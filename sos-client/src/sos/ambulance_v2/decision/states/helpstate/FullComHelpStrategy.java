package sos.ambulance_v2.decision.states.helpstate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.util.SOSGeometryTools;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;

/**
 * @author Reyhaneh
 */

public class FullComHelpStrategy extends HelpStrategy{

	private final short OFFSET_TIME_TO_BE_FREE = 5;
	private final short TIME_NEED_TO_lOAD_CIVILIAN = 1;
	private final short TIME_NEED_TO_UNlOAD_CIVILIAN = 1;
	private final short MAX_UNUPDATED_TIME = 3;
	/**************** Weights ***********************/	
	private final short BENEFIT_WEIGHT = 2;
	private final short IMPORTANCE_WEIGHT = 3;
	private final short LEFTTIME_WEIGHT = 1;
	private final short POPULATION_WEIGHT = 1;
	/************************************************/
	final private int FIRST_END_TIME = 30;
	final private int SECOND_START_TIME = 120;
	private AmbulanceTeamAgent ambulance = null;
	private AmbulanceDecision ambDecision;
	private ArrayList<Human> targets;
	private ArrayList<AmbulanceTeam> needHelpAmbulances;
	private ArrayList<HelpFactors> targetPoints;
	PriorityQueue<Pair<Human, Float>> maxPriorityTargets;
	
	public FullComHelpStrategy(AmbulanceInformationModel infoModel, SOSIState<AmbulanceInformationModel> state) {
		super(infoModel, state);
		ambulance = infoModel.getAmbulance();
		this.ambDecision = infoModel.getAmbulance().ambDecision;
	}
	
	
	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		if(infoModel.getTime() >FIRST_END_TIME && infoModel.getTime() < SECOND_START_TIME)
			return null;
		
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ HelpState $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambDecision.dclog.info("******************** HelpState  **********************");
		ambulance.lastState = " HelpState ";
		findValidHelpTargets();
		needHelpAmbulances();
		calculatePointFactors();
		setTargetPriority();
		Human bestHelpTarget =chooseTarget();
		
		if(bestHelpTarget != null){
			AmbulanceUtils.updateATtarget(bestHelpTarget,me(),state);
			infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + bestHelpTarget);
			ambDecision.dclog.info(" WorkTarget = " + me().getWork().getTarget());
			ambulance.currentSaveHumanTask = new SaveHumanTask(bestHelpTarget, infoModel.getTime());
				return ambulance.currentSaveHumanTask;
		}
		infoModel.getLog().info("$$$$$$$$$$ skipped from helpState $$$$$$$$$$");
		return null;
	}
	

	private void findValidHelpTargets() {
	ambDecision.dclog.info("*********** findValidHelpTargets **************");
		
		targets = new ArrayList<Human>();

		ArrayList<Human> allHumans = new ArrayList<Human>();
		allHumans = infoModel.getModel().humans();
		ambDecision.dclog.info("allHumans are " + allHumans);

		for (Human human : allHumans) {

			if (human.getRescueInfo().getIgnoredUntil() <= infoModel.getTime()) {
				ambDecision.humanUpdateLog.info("Human is ignored Until :" + human.getRescueInfo().getIgnoredUntil());
				human.getRescueInfo().setNotIgnored();
			}

			if (!AmbulanceUtils.isValidToDecideForCenter(human, ambDecision.humanUpdateLog))
				continue;

			if (infoModel.getAgent().messageSystem.type != Type.NoComunication) {
				int unUpdatedTime = Integer.MAX_VALUE;

				if (human instanceof Civilian)
					unUpdatedTime = infoModel.getTime() - ((Civilian) human).getLastReachableTime();
				else
					unUpdatedTime = infoModel.getTime() - human.updatedtime();

				if (unUpdatedTime < MAX_UNUPDATED_TIME) {
					ambDecision.humanUpdateLog.debug(human + " is invalid for agent because it is new sense and still 3 cycle doesn't pass");
					continue;
				}
			}
			targets.add(human);
		}

		ambDecision.dclog.debug("valid targets =" + targets);

		ambDecision.dclog.info("*******************************************");
	}


	private AmbulanceTeam me() {
		return (AmbulanceTeam) (infoModel.getAgent().me());
	}

	/************************************ chooseTarget *******************************************/
	public Human chooseTarget() {

		ambDecision.dclog.info("*********** chooseTarget ***************");
		
		ArrayList<AmbulanceTeam> ambulances=new ArrayList<AmbulanceTeam>();
		ArrayList<Human> checkedTargets=new ArrayList<Human>();
		ambulances =ambDecision.getAmbulances();
		
		if (needHelpAmbulances.isEmpty()) {
			ambDecision.dclog.info("targets is empty!!!!!!!!!");
			return null;
		}

		ambDecision.dclog.info("Tagets are " + maxPriorityTargets);
		
		while (!maxPriorityTargets.isEmpty()) {
			Pair<Human, Float> HumanAndCost = maxPriorityTargets.poll();
			if (HumanAndCost == null)
				continue;
			Human target = HumanAndCost.first();
			
			if(checkedTargets.contains(target))
				continue;

			ambDecision.dclog.logln("Target  = " + target + "    cost=" + HumanAndCost.second());

			AmbulanceTeam bestAT = getBestAmbulanceFor(target,ambulances);
			ambDecision.dclog.info("best AT for  = " + bestAT);

			if(bestAT == null)
				continue;
			
			if (bestAT.getID().equals(infoModel.getAgent().getID()) && target != null)
				return target;

			ambulances.remove(bestAT);
			checkedTargets.add(target);
		}
		ambDecision.dclog.info("*******************************************");
		return null;
	}

	/************************************************************************************************/
	/********************************** getBestAmbulanceFor *****************************************/
	private AmbulanceTeam getBestAmbulanceFor(final Human first,ArrayList<AmbulanceTeam> ambulances) {
		ambDecision.dclog.info("/////getBestAmbulanceFor////");
		ArrayList<AmbulanceTeam> ATs = new ArrayList<AmbulanceTeam>();
		ATs.addAll(ambulances);
		Collections.sort(ATs, new Comparator<AmbulanceTeam>() {

			@Override
			public int compare(AmbulanceTeam o1, AmbulanceTeam o2) {
				ClusterData o1Cluster = o1.model().searchWorldModel.getClusterData(o1);
				ClusterData o2Cluster = o2.model().searchWorldModel.getClusterData(o2);
				double o1s = SOSGeometryTools.distance(first.getPositionPoint().getX(),first.getPositionPoint().getY(), o1Cluster.getX(), o1Cluster.getY());
				double o2s = SOSGeometryTools.distance(first.getPositionPoint().getX(), first.getPositionPoint().getY(), o2Cluster.getX(), o2Cluster.getY());
				
				if (o1s > o2s)
					return 1; //o2s
				if (o1s < o2s)
					return -1; //o1s
				return 0;
			}
			
		});
		for (AmbulanceTeam AT : ATs) {
			if (AT.getWork() != null && AT.getWork().getTarget() != null && AmbulanceUtils.isReachableForAT(AT,true)) {
				ambDecision.dclog.info("skipped from AT =" + AT);
				continue;
			}

			return AT;
		}

		return null;
	}

	/*********************************************************************************************/
	/*********************************** needHelpAmbulances *************************************/
	private void needHelpAmbulances() {
		needHelpAmbulances = new ArrayList<AmbulanceTeam>(infoModel.getModel().ambulanceTeams().size());
		ambDecision.dclog.info("*********** needHelpAmbulances **************");
		for (AmbulanceTeam at : infoModel.getModel().ambulanceTeams()) {
			if (at.getWork() == null || at.getWork().getTarget() == null)
				continue;
			Human target = at.getWork().getTarget();
			if (!AmbulanceUtils.isValidToDecideIfSearchHaveNoTask(target,ambulance))
				continue;

			if (at.isReadyToAct())
				needHelpAmbulances.add(at);
		}

		ambDecision.dclog.debug("needHelp Ambulances=" + needHelpAmbulances);
		ambDecision.dclog.info("*********************************************");
	}

	/*********************************************************************************************/
	/****************************** assignCosttoAnotherAtsTargets *********************************/
	public void calculatePointFactors() {
		ambDecision.dclog.info("*********** calculatePointFactors ****************");
		targetPoints = new ArrayList<HelpFactors>();
		
		for (AmbulanceTeam AT : needHelpAmbulances) {

			Human helpTarget = AT.getWork().getTarget();

			int benefitForThatAT = getBenefitForThatAT(helpTarget);
			int probablityofImportance = probablityofImportance(helpTarget, AT);
			int timeItHas = helpTarget.getRescueInfo().getDeathTime()-infoModel.getTime()
					- (helpTarget.getBuriedness() / helpTarget.getRescueInfo().getNowWorkingOnMe().size()
							+ TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_lOAD_CIVILIAN + helpTarget.getRescueInfo().getTimeToRefuge());
			int numberOfCivilianInATCluster = numberOfCivilianInATCluster(AT);

			ambDecision.dclog.info("AT =" +AT +" target = " + helpTarget +" benefitForThatAT = "+benefitForThatAT+" probablityofImportance = "
			+ probablityofImportance+" timeItHas = "+ timeItHas+" numberOfCivilianInATCluster = "+ numberOfCivilianInATCluster );
			targetPoints.add(new HelpFactors(AT, benefitForThatAT, probablityofImportance, timeItHas, numberOfCivilianInATCluster));
		}
		ambDecision.dclog.info("*********************************************");
	}

	/*********************************************************************************************/
	/******************************** numberOfCivilianInATCluster ********************************/
	private int numberOfCivilianInATCluster(AmbulanceTeam AT) {
		int numberOfCivilianInATCluster = 0;
		ClusterData AtCluster = infoModel.getModel().searchWorldModel.getClusterData(AT);

		for (Human target : targets) {
			if (target.getRescueInfo().getNowWorkingOnMe().size() > 0)
				continue;
			if (AtCluster.getBuildings().contains(target.getAreaPosition()))
				numberOfCivilianInATCluster++;
		}
		return numberOfCivilianInATCluster;
	}

	/*********************************************************************************************/
	/*********************************** getImportanceOfThatAT ***********************************/
	private int probablityofImportance(Human helpTarget, AmbulanceTeam AT) {
		int taskDuration = taskDurationNowLeft(helpTarget);
		int MaxTimeToBeFree = infoModel.getTime() + taskDuration + OFFSET_TIME_TO_BE_FREE;
		ArrayList<AmbulanceTeam> ATsFreeWithThatAT = new ArrayList<AmbulanceTeam>();
		ArrayList<Human> withOutATtargets = new ArrayList<Human>();
		int helpTargetPoint = 0;

		for (AmbulanceTeam at : needHelpAmbulances) {

			if (at.getWork() == null || at.getWork().getTarget() == null)
				continue;

			Human target = at.getWork().getTarget();

			if ((infoModel.getTime() + taskDurationNowLeft(target)) <= MaxTimeToBeFree)
				ATsFreeWithThatAT.add(at);
		}
		ATsFreeWithThatAT.add(AT);
		ATsFreeWithThatAT.add(infoModel.getATEntity());

		for (Human target : targets) {
			if (target.getRescueInfo().getNowWorkingOnMe().size() > 0)
				continue;

			withOutATtargets.add(target);
		}

		for (final Human human : withOutATtargets) {

			Collections.sort(ATsFreeWithThatAT, new Comparator<AmbulanceTeam>() {
				@Override
				public int compare(AmbulanceTeam o1, AmbulanceTeam o2) {
					double o1s = SOSGeometryTools.distance(human.getPositionPoint().getX(), human.getPositionPoint().getY(), o1.getX(), o1.getY());
					double o2s = SOSGeometryTools.distance(human.getPositionPoint().getX(), human.getPositionPoint().getY(), o2.getX(), o2.getY());
					if (o1s > o2s)
						return 1; //o2s
					if (o1s < o2s)
						return -1; //o1s
					return 0;
				}
			});

			helpTargetPoint += getMyPointForHelpTarget(withOutATtargets);
		}
		return helpTargetPoint / (Math.max(1,withOutATtargets.size())*4);
	}

	/*********************************************************************************************/
	/********************************** getMyPointForHelpTarget **********************************/
	private int getMyPointForHelpTarget(ArrayList<Human> withOutATtargets) {
		int MAXpoint = 4;
		switch ((withOutATtargets.indexOf(infoModel.getATEntity()) + 1) % 4) {
		case 0:
			return MAXpoint;
		case 1:
			return MAXpoint - 1;
		case 2:
			return MAXpoint - 2;
		case 3:
			return MAXpoint - 3;
		default:
			break;

		}
		return 0;
	}

	/*********************************************************************************************/
	/************************************* taskDurationNowLeft ***********************************/
	private int taskDurationNowLeft(Human helpTarget) {
		int taskDuration = 0;
		taskDuration += Math.ceil(helpTarget.getBuriedness() / Math.max(1, (float) helpTarget.getRescueInfo().getNowWorkingOnMe().size()));
		taskDuration += ambDecision.getAverageMoveToTarget() / 2;

		if (helpTarget instanceof Civilian) {
			taskDuration += TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_UNlOAD_CIVILIAN;
			taskDuration += helpTarget.getRescueInfo().getTimeToRefuge();
		}
		return taskDuration;
	}

	/*********************************************************************************************/
	/************************************* getBenefitForThatAT ***********************************/
	private int getBenefitForThatAT(Human helpTarget) {
		int moveToTarget = ambDecision.getMoveToTarget(helpTarget);
		int nowWorkingOnHelpTarget = helpTarget.getRescueInfo().getNowWorkingOnMe().size();
		int benefitForThatAT = (helpTarget.getBuriedness() - (moveToTarget * nowWorkingOnHelpTarget)) / (nowWorkingOnHelpTarget + 1);
		return benefitForThatAT;
	}
	/*********************************************************************************************/
	/********************************** assignCostToTargets **************************************/
	private void setTargetPriority() {
		ambDecision.dclog.info("************** getMaxPriorityTarget ****************");
		maxPriorityTargets =new PriorityQueue<Pair<Human,Float>>(Math.max(targetPoints.size(),1),new Comparator<Pair<Human,Float>>() {

			@Override
			public int compare(Pair<Human, Float> o1, Pair<Human, Float> o2) {
				if(o1.second() < o2.second())
					return 1;
				else if( o1.second() > o2.second())
					return -1;
				else
					return 0;
			}
		});
		 int MaxbenefitForThatAT = 1;
		 int MaxtimeItHas = 1;
		 int MaxnumberOfCivilianInATCluster = 1;
		
		for(HelpFactors target: targetPoints){
			if(target.getBenefitForThatAT() > MaxbenefitForThatAT)
				MaxbenefitForThatAT = target.getBenefitForThatAT();
			if(target.getTimeItHas() > MaxtimeItHas)
				MaxtimeItHas = target.getTimeItHas();
			if(target.getNumberOfCivilianInATCluster() > MaxnumberOfCivilianInATCluster)
				MaxnumberOfCivilianInATCluster = target.getNumberOfCivilianInATCluster();
		}
		ambDecision.dclog.info("MaxbenefitForThatAT = " + MaxbenefitForThatAT + " MaxtimeItHas = " + MaxtimeItHas + " MaxnumberOfCivilianInATCluster " + MaxnumberOfCivilianInATCluster);

		for(HelpFactors Helpfactor : targetPoints){
			float priority = ( Helpfactor.getBenefitForThatAT() /MaxbenefitForThatAT ) * BENEFIT_WEIGHT
					+  ( Helpfactor.getProbablityofImportance() ) * IMPORTANCE_WEIGHT
					+  ( 1-( Helpfactor.getTimeItHas() / MaxtimeItHas) ) * LEFTTIME_WEIGHT
					+  ( Helpfactor.getNumberOfCivilianInATCluster() / MaxnumberOfCivilianInATCluster) * POPULATION_WEIGHT;
			ambDecision.dclog.info("AT =" + Helpfactor.getAmbulance() + " priority =" + priority);
			Human  target = Helpfactor.getAmbulance().getWork().getTarget();
			Pair<Human, Float> HumanAndPriority = new Pair<Human,Float>(target, priority);

			ambDecision.dclog.logln("target=" + target + " --> priority=" + HumanAndPriority.second());
			maxPriorityTargets.offer(HumanAndPriority);
		
		}
		ambDecision.dclog.info("*********************************************");
		
	}
	/*********************************************************************************************/
	/*********************************************************************************************/

}

package sos.ambulance_v2.decision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AmbulanceConstants;
import sos.ambulance_v2.base.AmbulanceConstants.CivilianState;
import sos.ambulance_v2.decision.controller.ImaginationShot;
import sos.ambulance_v2.tools.GraphUsage;
import sos.ambulance_v2.tools.MultiDitinctSourceCostInMM;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.entities.VirtualCivilian;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSGeometryTools;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.search_v2.tools.cluster.ClusterData;

/**
 * @author Reyhaneh
 */

public class LowCommunicationAmbulanceDecision {

	private SOSAgent<? extends StandardEntity> agent = null;
	/****************************************************/
	private final short MAX_UNUPDATED_TIME = 3;
	private final short TIME_NEED_TO_lOAD_CIVILIAN = 1;
	private final short TIME_NEED_TO_UNlOAD_CIVILIAN = 1;
	private final short StartMIDDLE_OF_SIMULATION = 75;
	private final short MIN_HIGH_LEVEL_BURIEDNESS = 30;
	private final short MIDDLE_OF_SIMULATION = 120;
	private final int MAX_PRIORITY = 10000000;
	private final short MIN_PRIORITY = 1;
	/****************************************************/
	public GraphUsage gu;
	public final SOSLoggerSystem lowdclog;

	/**************** center assigning map ******************/
	HashMap<AmbulanceTeam, ImaginationShot[]> assign;
	/******************* needed lists ***********************/
	ArrayList<AmbulanceTeam> ambulances;
	public ArrayList<VirtualCivilian> Virtualtargets;
	ArrayList<Human> humanTargets;

	PriorityQueue<Pair<VirtualCivilian, Float>> minCostTargets = new PriorityQueue<Pair<VirtualCivilian, Float>>(30, new VirtualCivilianCostComparator());

	int lastTimeUpdated = 0;
	int lastCycleIwasHere = 0;
	int lastCycleISetNumberOfATs = 0;

	public static int AVERAGE_MOVE_TO_TARGET = 4;
	public boolean hugeMap = false;
	public SOSLoggerSystem humanUpdateLog;
	public MultiDitinctSourceCostInMM costTable;

	public short[] ignoreBuildingUntil;

	public LowCommunicationAmbulanceDecision(SOSAgent<? extends StandardEntity> agent) {
		this.agent = agent;
		ignoreBuildingUntil = new short[agent.model().areas().size()];
		Arrays.fill(ignoreBuildingUntil, (short) 0);
		AVERAGE_MOVE_TO_TARGET = (agent.model().roads().size() < 1200 ? 6 : (agent.model().roads().size() < 2000 ? 9 : 12));
		lowdclog = new SOSLoggerSystem(agent.me(), "Agent/LowCommAmbulanceDecision", true, OutputType.File);
		lowdclog.setFullLoggingLevel();
		agent.sosLogger.addToAllLogType(lowdclog);

		humanUpdateLog = new SOSLoggerSystem(agent.me(), "Agent/ATHumanUpdate", true, OutputType.File, true);
		agent.sosLogger.addToAllLogType(humanUpdateLog);

		gu = new GraphUsage(agent);
		if (agent.getMapInfo().isBigMap()) {
			hugeMap = true;
		}
		lowdclog.logln("AVERAGE_MOVE_TO_TARGET=" + AVERAGE_MOVE_TO_TARGET + "  hugeMap=" + hugeMap);
	}

	/*********************************************************************************************/
	/************************************ findValidTargets ****************************************/
	public void findValidTargets() {

		lowdclog.info("*********** FindValidTargets **************");
		humanTargets = new ArrayList<Human>();
		ArrayList<Human> allHumans = new ArrayList<Human>();
		allHumans = agent.model().humans();
		AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) (agent));
		for (Human human : allHumans) {
			String infos = ambulance.xmlLog.addTag("Human", human.getID().getValue() + "");
			infos += ambulance.xmlLog.addTag("Type", human.toString());
			if (human.getRescueInfo().getIgnoredUntil() <= agent.time()) {
				humanUpdateLog.info(human + " is ignored Until :" + human.getRescueInfo().getIgnoredUntil() + "====>set Not Ignored");
				human.getRescueInfo().setNotIgnored();
			}

			if (!AmbulanceUtils.isValidToDecide(human, humanUpdateLog, (AmbulanceTeamAgent) agent))
				continue;

			int index = human.getAreaPosition().getAreaIndex();
			if (ignoreBuildingUntil[index] > agent.time()) {
				lowdclog.info("human = " + human + " in Building " + human.getAreaPosition() + " is ignored until = " + ignoreBuildingUntil[index]);
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", " in Building " + human.getAreaPosition() + " is ignored until = " + ignoreBuildingUntil[index]);
				ambulance.xmlLog.Info(infos);
				continue;
			}

			if (agent.messageSystem.type != Type.NoComunication && agent.messageSystem.type != Type.LowComunication) {
				int unUpdatedTime = Integer.MAX_VALUE;

				if (human instanceof Civilian)
					unUpdatedTime = agent.time() - ((Civilian) human).getLastReachableTime();
				else
					unUpdatedTime = agent.time() - human.updatedtime();

				if (unUpdatedTime < MAX_UNUPDATED_TIME) {
					humanUpdateLog.debug(human + " is invalid for agent because it is new sense and still 3 cycle doesn't pass");
					continue;
				}
			}

			humanTargets.add(human);

		}

		lowdclog.debug("valid Human targets =" + humanTargets);

		Virtualtargets = new ArrayList<VirtualCivilian>();
		ArrayList<VirtualCivilian> allVirtualCivilians = new ArrayList<VirtualCivilian>();
		allVirtualCivilians = agent.model().getVirtualCivilians();

		for (VirtualCivilian vc : allVirtualCivilians) {
			if (!AmbulanceUtils.isValidToDecide(vc, (AmbulanceTeam) (agent.me()), lowdclog, ambulance))
				continue;
			String infos = ambulance.xmlLog.addTag("VirtualCivilian", vc.toString());

			int index = vc.getPosition().getAreaIndex();
			if (ignoreBuildingUntil[index] > agent.time()) {
				lowdclog.info("VirtualCivilian " + vc + " in Building " + vc.getPosition() + " is ignored until = " + ignoreBuildingUntil[index]);
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", " in Building " + vc.getPosition() + " is ignored until = " + ignoreBuildingUntil[index]);
				ambulance.xmlLog.Info(infos);
				continue;
			}

			calculateRefugeInformation(vc);
			Virtualtargets.add(vc);
		}

		lowdclog.debug("all valid targets =" + Virtualtargets);

		lowdclog.info("*******************************************");
	}

	/*****************************************************************************************/
	/******************************* assignPrioritytoTargets *********************************/
	public void assignPrioritytoTargets() {

		lowdclog.info("************ assignPrioritytoTargets ***********");

		if (Virtualtargets.isEmpty())
			return;

		for (VirtualCivilian virtualCivilian : Virtualtargets) {

			if (getTargetCurrentState(virtualCivilian) == CivilianState.DEATH) {
				virtualCivilian.setRescuePriority(0);
				continue;
			}

			int priority = 0;

			if (virtualCivilian.isAgent())
				priority = getAgentPriority(virtualCivilian);
			else
				priority = getCivilianPriority(virtualCivilian);

			lowdclog.info("Human = " + virtualCivilian + "priority = " + priority);

			virtualCivilian.setRescuePriority(priority);
		}

		lowdclog.info("*******************************************");
	}

	/*************************************************************************************/
	/***************************** assignCosttoTargets ***********************************/
	public void assignCosttoTargets() {

		lowdclog.info("************ assignCosttoTargets ****************");

		minCostTargets = new PriorityQueue<Pair<VirtualCivilian, Float>>(Virtualtargets.size() + 5, new VirtualCivilianCostComparator());

		for (int i = 0; i < Virtualtargets.size(); i++) {
			VirtualCivilian virtualCivilian = Virtualtargets.get(i);

			//			if (agent.move.isReallyUnreachable(virtualCivilian.getPosition())) {
			//				lowdclog.logln(virtualCivilian + " is not reachable!");
			//				continue;
			//			}
			//			if (isEnoughATsWorkingOnHuman(virtualCivilian)) {
			//				lowdclog.logln(virtualCivilian + " has Enough At!");
			//				continue;
			//			}
			float cost = CostOfrescuingHuman(virtualCivilian);
			Pair<VirtualCivilian, Float> HumanAndCost = new Pair<VirtualCivilian, Float>(virtualCivilian, cost);

			lowdclog.logln("target=" + virtualCivilian + " --> cost=" + HumanAndCost.second());
			minCostTargets.offer(HumanAndCost);
		}

		lowdclog.logln("");
		lowdclog.info("*******************************************");
	}

	public void assignCosttoTargets2() {

		lowdclog.info("************ assignCosttoTargets ****************");

		minCostTargets = new PriorityQueue<Pair<VirtualCivilian, Float>>(Virtualtargets.size() + 5, new VirtualCivilianCostComparator());

		for (int i = 0; i < Virtualtargets.size(); i++) {
			VirtualCivilian virtualCivilian = Virtualtargets.get(i);

			//			if (agent.move.isReallyUnreachable(virtualCivilian.getPosition())) {
			//				lowdclog.logln(virtualCivilian + " is not reachable!");
			//				continue;
			//			}
			//			if (isEnoughATsWorkingOnHuman(virtualCivilian)) {
			//				lowdclog.logln(virtualCivilian + " has Enough At!");
			//				continue;
			//			}
			float cost = CostOfrescuingHuman2(virtualCivilian);
			Pair<VirtualCivilian, Float> HumanAndCost = new Pair<VirtualCivilian, Float>(virtualCivilian, cost);

			lowdclog.logln("target=" + virtualCivilian + " --> cost=" + HumanAndCost.second());
			minCostTargets.offer(HumanAndCost);
		}

		lowdclog.logln("");
		lowdclog.info("*******************************************");
	}

	/***************************************************************************************/
	/******************************** getMinCostTarget *************************************/

	public VirtualCivilian getMinCostTarget() {
		lowdclog.info("*********** getMinCostTarget ***************");

		Pair<VirtualCivilian, Float> HumanAndCost = minCostTargets.poll();

		if (Virtualtargets.isEmpty() || HumanAndCost == null) {
			lowdclog.info("***************** null *********************");
			return null;
		}

		lowdclog.logln("MinCostTarget result = " + HumanAndCost.first() + "    cost=" + HumanAndCost.second());
		lowdclog.info("*******************************************");
		return HumanAndCost.first();
	}

	private float CostOfrescuingHuman2(VirtualCivilian virtualCivilian) {
		lowdclog.info("********************* CostOfrescuingHuman ********************");
		float cost = 0;
		int duration = taskPerformingDurationForMe(virtualCivilian);
		try {
			int deathTime = virtualCivilian.getDeathTime();

			int distanceCost = getDistanceCost(virtualCivilian);

			int durationFactor = 0;
			int deathTimFactor = 0;

			switch (agent.time() / 100) {
			case 0:
				durationFactor = 2;
				deathTimFactor = 1;
				break;
			case 1:
				durationFactor = 1;
				deathTimFactor = 2;
				break;
			case 2:
				durationFactor = 0;
				deathTimFactor = 3;
				break;
			default:
				durationFactor = 1;
				deathTimFactor = 1;
				break;
			}

			//	cost = (duration*durationFactor)/100 + ((deathTime-agent.time())*deathTimFactor)/200 + ((distanceCost*2)/agent.model().ambulanceTeams().size());
			//for performance *1000
			cost = (duration * durationFactor) * 10 + ((deathTime - agent.time()) * deathTimFactor) * 5 + ((distanceCost * 20000) / agent.model().ambulanceTeams().size());
			float agentWeight = (virtualCivilian.isAgent()) ? getAgentWeight(virtualCivilian) : 1;
			cost /= 1000;
			cost *= agentWeight;

			lowdclog.info("VirtualCivilian =" + virtualCivilian + "taskPerformingDurationForMe =" + duration
					+ "deathTime = " + deathTime + " distanceCost = " + distanceCost + " time = " + agent.time() + " agentWeight= " + agentWeight);

		} catch (ArithmeticException ae) { //divided by zero
			cost = Integer.MAX_VALUE;
			System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
		}

		lowdclog.info("********************************************************");
		return cost;
	}

	/*****************************************************************************************/
	/************************* CostOfrescuingHuman ***************************************/
	private float CostOfrescuingHuman(VirtualCivilian virtualCivilian) {
		lowdclog.info("********************* CostOfrescuingHuman ********************");
		float cost = 0;
		int duration = taskPerformingDurationForMe(virtualCivilian);
		try {
			int deathTime = virtualCivilian.getDeathTime();

			int distanceCost = getDistanceCost(virtualCivilian);

			int durationFactor = 0;
			int deathTimFactor = 0;

			switch (agent.time() / 100) {
			case 0:
				durationFactor = 2;
				deathTimFactor = 1;
				break;
			case 1:
				durationFactor = 1;
				deathTimFactor = 2;
				break;
			case 2:
				durationFactor = 0;
				deathTimFactor = 3;
				break;
			default:
				durationFactor = 1;
				deathTimFactor = 1;
				break;
			}

			//	cost = (duration*durationFactor)/100 + ((deathTime-agent.time())*deathTimFactor)/200 + ((distanceCost*2)/agent.model().ambulanceTeams().size());
			//for performance *1000
			cost = (duration * durationFactor) * 10 + ((deathTime - agent.time()) * deathTimFactor) * 5 + ((distanceCost * 2000) / agent.model().ambulanceTeams().size());
			float agentWeight = (virtualCivilian.isAgent()) ? getAgentWeight(virtualCivilian) : 1;
			cost /= 1000;
			cost *= agentWeight;

			lowdclog.info("VirtualCivilian =" + virtualCivilian + "taskPerformingDurationForMe =" + duration
					+ "deathTime = " + deathTime + " distanceCost = " + distanceCost + " time = " + agent.time() + " agentWeight= " + agentWeight);

		} catch (ArithmeticException ae) { //divided by zero
			cost = Integer.MAX_VALUE;
			System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
		}

		lowdclog.info("********************************************************");
		return cost;
	}

	/*****************************************************************************************/
	/*********************************** getAgentWeight ***************************************/
	private float getAgentWeight(VirtualCivilian virtualCivilian) {

		if (agent.time() > 120)
			return 1.3f;
		if (virtualCivilian.getAgentType() == 0 || virtualCivilian.getAgentType() == 1)
			return 0.75f;

		ArrayList<FireBrigade> readyFireBrigades = getReadyFireBrigades();

		if (readyFireBrigades.size() < 15)
			return 0.33f;
		if (readyFireBrigades.size() < 25)
			return 0.5f;

		return 0.75f;
	}

	/*****************************************************************************************/
	/*
	 * private float CostOfrescuingHuman(VirtualCivilian virtualCivilian) {
	 * lowdclog.info("********************* CostOfrescuingHuman ********************");
	 * float cost = 0;
	 * int duration = taskPerformingDurationForMe(virtualCivilian);
	 * long priority = virtualCivilian.getRescuePriority();
	 * try {
	 * int deathTime = virtualCivilian.getDeathTime();
	 * int distanceCost =getDistanceCost(virtualCivilian);
	 * if(priority != 0)
	 * cost=(duration*100*distanceCost)/priority;
	 * else
	 * cost = Integer.MAX_VALUE;
	 * lowdclog.info("virtualCivilian = " + virtualCivilian + " taskPerformingDurationForMe =" + duration + " priority = " + priority
	 * + " deathTime = " + deathTime + " distanceCost = " + distanceCost);
	 * } catch (ArithmeticException ae) { //divided by zero
	 * cost = Integer.MAX_VALUE;
	 * System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
	 * }
	 * lowdclog.info("********************************************************");
	 * return cost;
	 * }
	 */
	/*****************************************************************************************/
	/*********************************** getDistanceCost *************************************/
	private int getDistanceCost(VirtualCivilian virtualCivilian) {
		ArrayList<ClusterData> clusters = new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
		final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();
		Collections.sort(clusters, new DistanceToMyClusterComparator(myCluster));

		for (ClusterData cluster : clusters) {
			if (cluster.getBuildings().contains(virtualCivilian.getPosition())) {
				return (clusters.indexOf(cluster) + 1);
			}
		}

		return 0;
	}

	/*****************************************************************************************/
	/**************************** taskPerformingDurationForMe *******************************/
	public int taskPerformingDurationForMe(VirtualCivilian virtualCivilian) {
		int cycle = 0;
		try {
			/* cycles needs to removing buridness */
			cycle += Math.ceil(virtualCivilian.getBuridness() / (float) virtualCivilian.getATneedToBeRescued());
			cycle += (virtualCivilian.getATneedToBeRescued() - 1);
			/* cycles needs to load and civilians to refuge */
			if (!virtualCivilian.isAgent()) {
				cycle++;
				cycle += virtualCivilian.getTimeToRefuge();
			}
			/* cycles needs to move to target */
			long moveWeight = agent.move.getWeightTo(virtualCivilian.getPosition(), StandardMove.class) * MoveConstants.DIVISION_UNIT_FOR_GET;
			cycle += gu.getFoolMoveTime(moveWeight);

		} catch (Exception ex) {
			cycle += virtualCivilian.getBuridness() + 2;
			cycle += (2 * AVERAGE_MOVE_TO_TARGET);
			ex.printStackTrace();
		}
		return cycle;
	}

	/*****************************************************************************************/
	/********************************* getAgentPriority **************************************/
	private int getAgentPriority(VirtualCivilian virtualCivilian) {

		lowdclog.info("********* getAgentPriority ***********");

		/* sinash 2013 - determination of ready forces */
		lowdclog.logln("SINA: In ready forces determination : ");
		ArrayList<FireBrigade> readyFireBrigades = getReadyFireBrigades();
		ArrayList<AmbulanceTeam> readyAmbulanceTeams = getReadyAmbulanceTeam();
		ArrayList<PoliceForce> readyPoliceForces = getReadyPoliceForce();
		lowdclog.logln("SINA: readyFireBrigades: " + readyFireBrigades.size() + " readyAmbulanceTeams: " + readyAmbulanceTeams.size() + " readyPoliceForces: " + readyPoliceForces.size());

		/* Added by Ali for distributing rescuing agent */
		int priority = agent.time() < 100 ? MAX_PRIORITY : MIN_PRIORITY;
		lowdclog.info("priority as cycle = " + priority);

		if (virtualCivilian.getAgentType() == 0)
			priority += (30 - readyAmbulanceTeams.size()) / 5;
		if (virtualCivilian.getAgentType() == 1)
			priority += (30 - readyPoliceForces.size()) / 5;
		if (virtualCivilian.getAgentType() == 3)
			priority += (30 - readyFireBrigades.size()) / 5;

		lowdclog.info("priority as kind of agent = " + priority);

		priority += Virtualtargets.indexOf(virtualCivilian) % 3;

		lowdclog.info("priority as distribution = " + priority);

		lowdclog.info("*****************************************");
		return priority;
	}

	/*****************************************************************************************/
	/******************************* getReadyFireBrigades ***********************************/

	private ArrayList<FireBrigade> getReadyFireBrigades() {
		ArrayList<FireBrigade> readyFireBrigades = new ArrayList<FireBrigade>(agent.model().fireBrigades().size()); //sinash

		for (FireBrigade fb : agent.model().fireBrigades()) {
			if (fb.isReadyToAct()) {
				readyFireBrigades.add(fb);
			}
		}
		readyFireBrigades.trimToSize();
		return readyFireBrigades;
	}

	/***************************************************************************************/
	/******************************* getReadyAmbulanceTeam **********************************/

	private ArrayList<AmbulanceTeam> getReadyAmbulanceTeam() {

		ArrayList<AmbulanceTeam> readyAmbulanceTeams = new ArrayList<AmbulanceTeam>(agent.model().ambulanceTeams().size()); //siansh
		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.isReadyToAct()) {
				readyAmbulanceTeams.add(at);
			}
		}
		readyAmbulanceTeams.trimToSize();

		return readyAmbulanceTeams;
	}

	/*****************************************************************************************/
	/******************************* getReadyPoliceForce *************************************/

	private ArrayList<PoliceForce> getReadyPoliceForce() {
		ArrayList<PoliceForce> readyPoliceForces = new ArrayList<PoliceForce>(agent.model().policeForces().size()); //sinash
		for (PoliceForce pf : agent.model().policeForces()) {
			if (pf.isReadyToAct()) {
				readyPoliceForces.add(pf);
			}
		}
		readyPoliceForces.trimToSize();

		return readyPoliceForces;
	}

	/*****************************************************************************************/
	/****************************** makePriorityListFromTargets ******************************/
	public void makePriorityListFromTargets() {

		lowdclog.info("******** makePriorityListFromTargets **************");
		Collections.sort(Virtualtargets, new VirtualCivilianPriorityComparator());

		if (!SOSConstant.IS_CHALLENGE_RUNNING)
			getPrirotyLog();
		lowdclog.info("*******************************************");
	}

	/*****************************************************************************************/
	/************************************* getPrirotyLog *************************************/
	private void getPrirotyLog() {

		lowdclog.log("PRIORITY::::::");
		for (VirtualCivilian virtualCivilian : Virtualtargets) {
			lowdclog.log("[" + virtualCivilian + " ->     RescuePriority:" + virtualCivilian.getRescuePriority() + "\tTarget CurrentState " + getTargetCurrentState(virtualCivilian) + "] ");
		}
		lowdclog.logln("");
	}

	/*****************************************************************************************/
	/******************************* getCivilianPriority ************************************/
	private int getCivilianPriority(VirtualCivilian virtualCivilian) {

		lowdclog.info("********* getCivilianPriority ***********");
		//old priority hm.getRescueInfo().setRescuePriority(((10000000 / (hm.getRescueInfo().getDeathTime() + 1 - agent.time())) * (320 - hm.getBuriedness()) / 300));
		int deathTime = virtualCivilian.getDeathTime();
		int priority = MAX_PRIORITY / (deathTime + 1 - agent.time());
		lowdclog.info("calculate priority from deathTime :" + "deathTime = " + deathTime
				+ "priority = " + priority);
		priority = (priority * (320 - virtualCivilian.getBuridness() * 2)) / 300;
		lowdclog.info("calculate priority from burriedness :" + "burriedness = " + virtualCivilian.getBuridness()
				+ "priority = " + priority);

		//		if (virtualCivilian.getBuridness() == 0 && virtualCivilian.getDamage() > 0) {
		//			priority *= 1.5; /* added by sinash for final day IranOpen 2013 */
		//			lowdclog.info("without buriedness and has Damage civilian" + " priority = " + priority);
		//		}

		if (!isHumanDeadTimeLessThanValidTime(virtualCivilian, AmbulanceConstants.VALID_DEATH_TIME_FOR_NO_REFUGE_MAP)) {
			priority /= 10;
			lowdclog.info("long life civilian " + " priority = " + priority);
		}

		lowdclog.info("*****************************************");
		return priority;
	}

	/*****************************************************************************************/
	/********************************* getTargetCurrentState *********************************/

	private CivilianState getTargetCurrentState(VirtualCivilian virtualCivilian) {
		int deathTime = virtualCivilian.getDeathTime();

		if (deathTime >= agent.time())
			return CivilianState.CRITICAL;

		return CivilianState.DEATH;

	}

	/*********************************************************************************************/
	/************************************* findValidAmbulances ***********************************/
	public void findValidAmbulances() {
		ambulances = new ArrayList<AmbulanceTeam>(agent.model().ambulanceTeams().size());
		lowdclog.info("*********** FindValidAmbulances **************");
		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.isReadyToAct())
				ambulances.add(at);
		}

		lowdclog.debug("valid Ambulances=" + ambulances);
		lowdclog.info("*********************************************");
	}

	/***********************************************************************************************/
	/***************************************** filterTargets ***************************************/
	public void filterTargets() {
		lowdclog.info("************* filterTargets ********************");
		if (humanTargets.isEmpty() && Virtualtargets.isEmpty()) {
			lowdclog.info("targets is empty");
			return;
		}
		removeCenterAssignListTarget();

		removeATsAssignedTargets();

		mergeTargets();

		removeAgentsTargetAfterMiddleOfSimulation();

		if (agent.model().refuges().isEmpty())
			withOutRefugeMapTargets();

		//	if (agent.time() < START_OF_SIMULATION)
		//		removeTooBuriedCivilians();

		removeUnrescueableTargets();

		if (agent.messageSystem.type == Type.LowComunication || agent.messageSystem.type == Type.NoComunication)
			removeOtherClusterTargets();

		lowdclog.info("*******************************************");

	}

	/***********************************************************************************************/
	/***************************************** filterTargets ***************************************/
	public void filterTargetsForAfterCivilianSearch() {
		lowdclog.info("************* filterTargets ********************");
		if (humanTargets.isEmpty() && Virtualtargets.isEmpty()) {
			lowdclog.info("targets is empty");
			return;
		}
		removeCenterAssignListTarget();

		removeATsAssignedTargets();

		mergeTargets();

		removeAgentsTargetAfterMiddleOfSimulation();

		if (agent.model().refuges().isEmpty())
			withOutRefugeMapTargets();

		removeUnrescueableTargets();

		removeFarestClusterCivilians();

		lowdclog.info("*******************************************");

	}

	/**************************************************************************************/
	/************************ removeFarestClusterCivilians ********************************/
	private void removeFarestClusterCivilians() {

		lowdclog.info("*********************** removeFarestClusterCivilians ********************");
		ArrayList<ClusterData> clusters = new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
		ArrayList<ClusterData> removed = new ArrayList<ClusterData>();
		for (ClusterData cd : clusters)
			if (cd.isCoverer())
				removed.add(cd);
		clusters.removeAll(removed);
		final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();

		Collections.sort(clusters, new Comparator<ClusterData>() {

			@Override
			public int compare(ClusterData o1, ClusterData o2) {

				float dis1 = SOSGeometryTools.distance(new Point2D(myCluster.getX(), myCluster.getY()), new Point2D(o1.getX(), o1.getY()));
				float dis2 = SOSGeometryTools.distance(new Point2D(myCluster.getX(), myCluster.getY()), new Point2D(o2.getX(), o2.getY()));
				if (dis1 < dis2)
					return -1;
				else
					return 1;
			}
		});
		int numb = 3;

		if (agent.time() > 160)
			numb = clusters.size() - 1;
		else if (agent.time() > 120)
			numb = 10;
		else
			numb = 5;

		ArrayList<VirtualCivilian> removes = new ArrayList<VirtualCivilian>();

		for (VirtualCivilian vc : Virtualtargets) {
			if (!(vc.getPosition() instanceof Building))
				continue;
			boolean check = false;
			for (int i = 0; i <= numb; i++)
			{
				if (clusters.get(i).getBuildings().contains(vc.getPosition())) {
					check = true;
					break;
				}
			}

			if(!check)
				removes.add(vc);
		}
		
		lowdclog.debug(" targets:" + Virtualtargets);
		Virtualtargets.removeAll(removes);
		lowdclog.debug("removed targets:" + removed);
		lowdclog.debug("current targets:" + Virtualtargets);
		lowdclog.info("*******************************************");
	}

	/**************************************************************************************/
	/************************ removeOtherClusterTargets ***********************************/
	public void removeOtherClusterTargets() {

		ArrayList<ClusterData> validClusters = new ArrayList<ClusterData>();

		final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();

		ArrayList<ClusterData> clusters = new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
		Collections.sort(clusters, new DistanceComparator(agent));

		validClusters.add(myCluster);

		if (!clusters.isEmpty()) {
			lowdclog.info("found nearest cluster");
			validClusters.add(clusters.get(0));
		}
		lowdclog.debug("removeOtherClusterTargetsInLowOrNoCommunication:current targets:" + Virtualtargets);

		ArrayList<VirtualCivilian> newTargets = new ArrayList<VirtualCivilian>();
		for (VirtualCivilian virtualCivilian : Virtualtargets) {
			if ((SOSGeometryTools.distance(virtualCivilian.getPosition().getPositionPoint(), agent.me().getPositionPoint())) < (agent.model().getBounds().getWidth() / 8)) {
				newTargets.add(virtualCivilian);
				continue;
			}
			for (ClusterData validCluster : validClusters) {
				if (validCluster.getBuildings().contains(virtualCivilian.getPosition()))
					newTargets.add(virtualCivilian);
			}
		}
		AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
		for (VirtualCivilian vc : Virtualtargets) {
			if (newTargets.contains(vc))
				continue;
			String infos = ambulance.xmlLog.addTag("VirtualCivilian", vc.toString());
			infos += ambulance.xmlLog.addTag("Validity", "invalid");
			infos += ambulance.xmlLog.addTag("stateReason", "not in my cluster or my current cluster!!!");
			ambulance.xmlLog.info(infos);
		}

		Virtualtargets = newTargets;
		lowdclog.debug("current Virtualtargets:" + Virtualtargets);
	}

	/**************************************************************************************/
	/***************************** removeUnrescueableTargets *****************************/
	public void removeUnrescueableTargets() {

		lowdclog.info("//////////// removeUnrescueableTargets ////////////////////");

		ArrayList<VirtualCivilian> remove = new ArrayList<VirtualCivilian>(5);

		for (VirtualCivilian virtualCivilian : Virtualtargets) {
			if (virtualCivilian.getATneedToBeRescued() > ambulances.size()) {
				lowdclog.logln(virtualCivilian + " needed AT bigger than ambulance sizes!");
				remove.add(virtualCivilian);
			} else if (AmbulanceUtils.taskAssigningExpireTime(virtualCivilian, virtualCivilian.getDeathTime()) - agent.time() < 0) {
				remove.add(virtualCivilian);
			}
			lowdclog.logln(virtualCivilian + " task AssignningExpireTime passed!" + "DeathTime =" + virtualCivilian.getDeathTime()
					+ " time = " + agent.time());

		}

		lowdclog.debug("remove unrescuable targets = " + remove);
		AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
		for (VirtualCivilian vc : remove) {
			String infos = ambulance.xmlLog.addTag("VirtualCivilian", vc.toString());
			infos += ambulance.xmlLog.addTag("Validity", "invalid");
			infos += ambulance.xmlLog.addTag("stateReason", "Unrescueable!!!");
			ambulance.xmlLog.info(infos);
		}
		Virtualtargets.removeAll(remove);
		lowdclog.debug("current Virtualtargets = " + Virtualtargets);

		lowdclog.info("///////////////////////////////////////////////////");
	}

	/***********************************************************************************************/
	/****************************** getNumberOfATNeedToRescue **************************************/
	public int getNumberOfATNeedToRescue(VirtualCivilian virtualCivilian, int time) {
		int limitTime = time;

		int TimeNeedToBeRescue = (agent.time() + virtualCivilian.getTimeToRefuge() + TIME_NEED_TO_lOAD_CIVILIAN +
				1 + (AVERAGE_MOVE_TO_TARGET - 1)) + TIME_NEED_TO_UNlOAD_CIVILIAN;

		int numOfATitNeeds = limitTime - TimeNeedToBeRescue;

		if (numOfATitNeeds == 0)
			return 1;
		if (numOfATitNeeds > 0) {
			numOfATitNeeds = (int) Math.floor(virtualCivilian.getBuridness() / numOfATitNeeds) + 1;
			return numOfATitNeeds;
		}

		return 100;
	}

	/*****************************************************************************************/
	/******************************* mergeTargets ******************************************/
	private void mergeTargets() {
		lowdclog.info("************* removeCenterAssignListTarget ********************");
		for (Human human : humanTargets) {
			VirtualCivilian vc = new VirtualCivilian(human.getAreaPosition(), human.getBuriedness()
					, human.getRescueInfo().getDeathTime(), AmbulanceUtils.isReachableForAT(human, true));
			vc.setATneedToBeRescued(human.getRescueInfo().getATneedToBeRescued());
			vc.setTimeToRefuge(human.getRescueInfo().getTimeToRefuge());

			if ((human instanceof AmbulanceTeam)) {
				vc.setAgent(true);
				vc.setAgentType(0);
			}
			else if ((human instanceof PoliceForce)) {
				vc.setAgent(true);
				vc.setAgentType(1);
			}
			else if ((human instanceof FireBrigade)) {
				vc.setAgent(true);
				vc.setAgentType(2);
			}

			Virtualtargets.add(vc);
		}
		lowdclog.debug("current targets:" + Virtualtargets);
	}

	/*****************************************************************************************/
	/**************************** removeCenterAssignListTarget *******************************/
	public void removeCenterAssignListTarget() {
		lowdclog.info("************* removeCenterAssignListTarget ********************");
		lowdclog.debug("removeCenterAssignListTarget:: current targets:" + humanTargets);

		humanTargets.removeAll(((AmbulanceTeamAgent) agent).centerAssignLists);
		lowdclog.debug("current human targets:" + humanTargets);
	}

	/***********************************************************************************************/
	/******************************** withOutRefugeMapTargets ***************************************/
	private void withOutRefugeMapTargets() {
		lowdclog.info("////////// withOutRefugeMapTargets ////////////////");
		ArrayList<VirtualCivilian> removes = new ArrayList<VirtualCivilian>();
		for (VirtualCivilian virtualCivilian : Virtualtargets) {

			if (virtualCivilian.isAgent())
				continue;
			if (AmbulanceUtils.isVirtualCivilianInFireBuilding(virtualCivilian)
					|| isHumanDeadTimeLessThanValidTime(virtualCivilian, AmbulanceConstants.VALID_DEATH_TIME_FOR_NO_REFUGE_MAP)
					|| !(virtualCivilian.getPosition() instanceof Building))
				removes.add(virtualCivilian);

		}
		lowdclog.debug("remove list because it is no refuge map=>" + removes);
		Virtualtargets.removeAll(removes);

		lowdclog.debug("current Virtualtargets:" + Virtualtargets);

		lowdclog.info("///////////////////////////////////////////////////");
	}

	/***********************************************************************************************/
	/***************************** isHumanDeadTimeLessThanValidTime *********************************/
	private boolean isHumanDeadTimeLessThanValidTime(VirtualCivilian virtualCivilian, int validTime) {
		if (virtualCivilian.getDeathTime() < validTime)
			return true;

		return false;
	}

	/***********************************************************************************************/
	/********************************** removeTooBuriedCivilians ***********************************/
	public void removeTooBuriedCivilians() {

		lowdclog.info("/////////// removeTooBuriedCivilians //////////////");

		ArrayList<VirtualCivilian> remove = new ArrayList<VirtualCivilian>();

		for (VirtualCivilian virtualCivilian : Virtualtargets) {
			if (virtualCivilian.getBuridness() > MIN_HIGH_LEVEL_BURIEDNESS)
				remove.add(virtualCivilian);

		}
		lowdclog.debug("removed civilians because too buridness in start of sim :" + remove);
		Virtualtargets.removeAll(remove);

		lowdclog.debug("current Virtualtargets:" + Virtualtargets);

		lowdclog.info("///////////////////////////////////////////////////");
	}

	/*****************************************************************************************/
	/****************** removeAgentsTargetAfterMiddleOfSimulation ****************************/
	public void removeAgentsTargetAfterMiddleOfSimulation() {
		lowdclog.info("************* removeAgentsTargetAfterMiddleOfSimulation ********************");
		if (StartMIDDLE_OF_SIMULATION > agent.time()) {
			lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: before middle");
			return;
		}
		lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after middle");

		ArrayList<VirtualCivilian> removed = new ArrayList<VirtualCivilian>();
		if (StartMIDDLE_OF_SIMULATION < agent.time() && MIDDLE_OF_SIMULATION > agent.time())
		{
			lowdclog.debug("removeAgentsTargetAfcterMiddleOfSimulation:: before middle(remove high levels)");
			for (VirtualCivilian virtualCivilian : Virtualtargets) {
				if (virtualCivilian.isAgent() && virtualCivilian.getBuridness() > MIN_HIGH_LEVEL_BURIEDNESS + 10)
					removed.add(virtualCivilian);
			}
		}
		else if (agent.time() < 200 && agent.model().getLastAfterShockTime() > 3) {
			lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after middle");
			lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after shock detected");
			for (VirtualCivilian virtualCivilian : Virtualtargets) {
				if (virtualCivilian.isAgent() && virtualCivilian.getBuridness() > MIN_HIGH_LEVEL_BURIEDNESS + 3)
					removed.add(virtualCivilian);
			}
		}
		else {
			lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: remove all agents");
			for (VirtualCivilian virtualCivilian : Virtualtargets) {
				if (virtualCivilian.isAgent() && virtualCivilian.getBuridness() > 12)
					removed.add(virtualCivilian);
			}
		}

		lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: current targets:" + Virtualtargets);
		lowdclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: removed targets:" + removed);

		AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
		for (VirtualCivilian vc : removed) {
			String infos = ambulance.xmlLog.addTag("VirtualCivilian", vc.toString());
			infos += ambulance.xmlLog.addTag("Validity", "invalid");
			infos += ambulance.xmlLog.addTag("stateReason", "remove agent!!!");
			ambulance.xmlLog.info(infos);
		}
		Virtualtargets.removeAll(removed);

		lowdclog.debug("current targets:" + Virtualtargets);
	}

	/**************************************************************************************/
	/************************** removeATsAssignedTargets **********************************/
	public void removeATsAssignedTargets() {

		lowdclog.info("///////////// removeATsAssignedTargets ///////////////");

		ArrayList<Human> removeTargetsBecauseOtherATGetIt = new ArrayList<Human>();

		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.getWork() != null && at.getWork().getTarget() != null) {
				Human human = at.getWork().getTarget();
				if (!isEnoughATsWorkingOnHuman(human)) {
					lowdclog.logln(human + " has'nt Enough At!");
					continue;
				}
				removeTargetsBecauseOtherATGetIt.add(at.getWork().getTarget());
			}
		}

		lowdclog.debug("removeTargetsBecauseOtherATGetIt:" + removeTargetsBecauseOtherATGetIt);

		humanTargets.removeAll(removeTargetsBecauseOtherATGetIt);

		lowdclog.debug("current humanTargets:" + humanTargets);

		lowdclog.info("///////////////////////////////////////////////////");
	}

	/*****************************************************************************************/
	/************************* isEnoughATsWorkingOnHuman *************************************/
	private boolean isEnoughATsWorkingOnHuman(Human human) {

		short numberOfATHumanNeedToBeRescued = human.getRescueInfo().getATneedToBeRescued();
		int numberOfATisWorkingOnHumanNow = human.getRescueInfo().getNowWorkingOnMe().size();

		if (doWeHaveEnoughTargets() && (numberOfATHumanNeedToBeRescued - numberOfATisWorkingOnHumanNow) <= 0)
			return true;

		return false;
	}

	/*****************************************************************************************/
	/*************************** doWeHaveEnoughTargets ***************************************/
	private boolean doWeHaveEnoughTargets() {
		if ((humanTargets.size() + Virtualtargets.size()) > ambulances.size() / 3)
			return true;

		return false;
	}

	/************************* calculateRefugeInformation ***********************************/
	public void calculateRefugeInformation(VirtualCivilian virtualCivilian) {
		if (!virtualCivilian.isPositionDefined() || virtualCivilian.getPosition() instanceof Refuge)
			return;
		if (agent.model().refuges().isEmpty())
			return;
		if (costTable == null) {//if is from hear
			virtualCivilian.setTimeToRefuge(AVERAGE_MOVE_TO_TARGET);
			return;
		}
		long minCost = Long.MAX_VALUE;
		Refuge best = null;
		for (Refuge refuge : agent.model().refuges()) {
			long cost = costTable.getCostFromTo(refuge, virtualCivilian.getPosition());
			if (cost < minCost) {
				best = refuge;
				minCost = cost;
			}
		}

		if (best != null && minCost < MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING) {
			virtualCivilian.setTimeToRefuge(gu.getFoolMoveTime(minCost));
		}

	}

	public void reset() {
		try {
			if (Virtualtargets != null)
				Virtualtargets.clear();
			if (minCostTargets != null)
				minCostTargets.clear();
			if (humanTargets != null)
				humanTargets.clear();
			if (ambulances != null)
				ambulances.clear();
		} catch (Exception e) {
			lowdclog.error(e);
		}
	}

}
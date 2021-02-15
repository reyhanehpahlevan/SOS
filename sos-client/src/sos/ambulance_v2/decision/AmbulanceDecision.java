package sos.ambulance_v2.decision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AmbulanceConstants;
import sos.ambulance_v2.base.AmbulanceConstants.CivilianState;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.decision.controller.AmbulanceDream;
import sos.ambulance_v2.decision.controller.ImaginationShot;
import sos.ambulance_v2.tools.GraphUsage;
import sos.ambulance_v2.tools.MultiDitinctSourceCostInMM;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.entities.VirtualCivilian;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSGeometryTools;
import sos.base.util.information_stacker.CycleInformations;
import sos.base.util.information_stacker.act.MoveAction;
import sos.base.util.information_stacker.act.StockMoveAction;
import sos.base.util.mapRecognition.MapRecognition.MapName;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.base.util.sosLogger.TableLogger;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceUtils;
import sos.search_v2.tools.cluster.ClusterData;

/**
 * @author Reyhaneh
 */

public class AmbulanceDecision {

	private SOSAgent<? extends StandardEntity> agent = null;
	/****************************************************/
	private final short MAX_UNUPDATED_TIME = 3;
	private final short TIME_NEED_TO_lOAD_CIVILIAN = 1;
	private final short TIME_NEED_TO_UNlOAD_CIVILIAN = 1;
	private final short START_OF_SIMULATION = 10;
	private final short MIN_HIGH_LEVEL_BURIEDNESS = 30;
	private final short MIDDLE_OF_SIMULATION = 120;
	private final short StartMIDDLE_OF_SIMULATION = 75;
	private final int MAX_PRIORITY = 10000000;
	private final short MIN_PRIORITY = 1;
	private boolean XMLLogging = false;
	/****************************************************/
	public GraphUsage gu;
	public final SOSLoggerSystem dclog;
	TableLogger updateHumanInfoTable;
	/**************** center assigning map ******************/
	HashMap<AmbulanceTeam, ImaginationShot[]> assign;
	/******************* needed lists ***********************/
	ArrayList<AmbulanceTeam> ambulances;
	ArrayList<Human> targets;

	PriorityQueue<Pair<Human, Float>> minCostTargets = new PriorityQueue<Pair<Human, Float>>(30, new CostComparator());

	int lastTimeUpdated = 0;
	int lastCycleIwasHere = 0;
	int lastCycleISetNumberOfATs = 0;

	public static int AVERAGE_MOVE_TO_TARGET = 4;
	public boolean hugeMap = false;
	public SOSLoggerSystem humanUpdateLog, msgLog;
	public MultiDitinctSourceCostInMM costTable;

	public AmbulanceDecision(SOSAgent<? extends StandardEntity> agent) {
		this.agent = agent;
		AVERAGE_MOVE_TO_TARGET = (agent.model().roads().size() < 1200 ? 6 : (agent.model().roads().size() < 2000 ? 9 : 12));
		dclog = new SOSLoggerSystem(agent.me(), "Agent/AmbulanceDecision", true, OutputType.File);
		dclog.setFullLoggingLevel();
		agent.sosLogger.addToAllLogType(dclog);

		humanUpdateLog = new SOSLoggerSystem(agent.me(), "Agent/ATHumanUpdate", true, OutputType.File, true);
		agent.sosLogger.addToAllLogType(humanUpdateLog);

		msgLog = new SOSLoggerSystem(agent.me(), "Agent/MeessageLog", true, OutputType.File);
		msgLog.setFullLoggingLevel();
		agent.sosLogger.addToAllLogType(msgLog);

		gu = new GraphUsage(agent);
		if (agent.getMapInfo().isBigMap()) {
			hugeMap = true;
		}
		dclog.logln("AVERAGE_MOVE_TO_TARGET=" + AVERAGE_MOVE_TO_TARGET + "  hugeMap=" + hugeMap);
	}

	/**
	 * @r@mik updating death and critical and average times and time to transport to nearest open refuge
	 */
	/***************************************************************************************/
	/******************************** updateHumansInfo **************************************/
	public void updateHumansInfo() {
		dclog.info("************* updateHumansInfo *******************");
		dclog.info("IN UPDATE_HUMANS_INFO .....time=" + agent.time() + " to more information take a look at human update log");

		updateHumanInfoTable = new TableLogger(20);
		updateHumanInfoTable.addColumn("DeadTime");
		if (lastTimeUpdated == agent.time())
			return;
		ArrayList<Area> srcs = new ArrayList<Area>();

		if (agent.getCenterActivities().isEmpty())
			srcs.add(agent.me().getAreaPosition());
		else {
			for (AmbulanceTeam at : agent.model().ambulanceTeams())
				srcs.add(at.getAreaPosition());
		}

		for (Refuge ref : agent.model().refuges()) {
			srcs.add(ref);
		}

		costTable = new MultiDitinctSourceCostInMM(agent.model(), srcs);
		lastTimeUpdated = agent.time();

		for (Human human : agent.model().humans()) {
			boolean result = updateHuman(human);
			if (!result)
				continue;

		}
		msgLog.info(updateHumanInfoTable.getTablarResult("DeadTime"));
		dclog.info("*******************************************");
	}

	/***************************************************************************************/

	public boolean updateHuman(Human hm) {
		humanUpdateLog.log(hm);
		//initial checks
		if (hm == null) {
			humanUpdateLog.logln(" --> NULL Human!");
			return false;
		}
		if (!hm.isBuriednessDefined()) {
			humanUpdateLog.logln(" --> Unknown State!");
			return false;
		}
		if (!(hm instanceof Civilian) && hm.getBuriedness() == 0) {
			humanUpdateLog.logln(" --> Not burried Agent!");
			return false;
		}
		if (!hm.isPositionDefined()) {
			humanUpdateLog.logln(" --> No position!");
			return false;
		}

		if (hm.getPosition() instanceof Refuge) {
			humanUpdateLog.logln(" --> in refuge!");
			return false;
		}
		if (hm.getPosition() instanceof AmbulanceTeam) {
			humanUpdateLog.logln(" --> in AT!");
			return false;
		}
		if (hm instanceof Civilian && hm.getPosition() instanceof Road && hm.getDamage() == 0 && hm.getHP() == 10000) {
			humanUpdateLog.logln(" --> in road Healthy Civilian");
			return false;
		}

		int deathTime = hm.getRescueInfo().getDeathTime();
		int numberOfATNeed = getNumberOfATNeedToRescue(hm, deathTime);
		hm.getRescueInfo().setATneedToBeRescued(numberOfATNeed);

		if (agent.model().refuges().isEmpty()) {
			hm.getRescueInfo().setTimeToRefuge(1);
			hm.getRescueInfo().setBestRefuge(null);
		} else {
			calculateRefugeInformation(hm);
		}

		updateHumanInfoTable.addScore(hm + "", "HP", hm.getHP() + "");
		updateHumanInfoTable.addScore(hm + "", "Damage", hm.getDamage() + "");
		updateHumanInfoTable.addScore(hm + "", "Time", hm.updatedtime() + "");
		updateHumanInfoTable.addScore(hm + "", "Buredness", hm.getBuriedness() + "");
		updateHumanInfoTable.addScore(hm + "", "DeadTime", hm.getRescueInfo().getDeathTime() + "");
		updateHumanInfoTable.addScore(hm + "", "position", hm.getPosition() + "");
		updateHumanInfoTable.addScore(hm + "", "Ignore", hm.getRescueInfo().isIgnored() + "");
		updateHumanInfoTable.addScore(hm + "", "IgnoreUntil", hm.getRescueInfo().getIgnoredUntil() + "");
		updateHumanInfoTable.addScore(hm + "", "Reason", hm.getRescueInfo().getIgnoreReason() + "");
		updateHumanInfoTable.addScore(hm + "", "NeededAT", numberOfATNeed + "");
		updateHumanInfoTable.addScore(hm + "", "Refuge", hm.getRescueInfo().getBestRefuge() + "");
		updateHumanInfoTable.addScore(hm + "", "RefugeTime", hm.getRescueInfo().getTimeToRefuge() + "");

		return true;
	}

	/***********************************************************************************************/
	public boolean isOldTaskValid() {
		dclog.info("**************** isOldTaskValid ******************");
		Human target = ((AmbulanceTeam) agent.me()).getWork().getTarget();
		if ((!AmbulanceUtils.isValidToRescue(target, dclog))) {
			dclog.logln("in isOldTaskValid  -> target=" + target);
			return false;
		}
		/*
		 * else if(canMakeIntrupt()){
		 * return false;
		 * }
		 */
		int deathTime = target.getRescueInfo().getDeathTime();
		dclog.info("deathTime = " + deathTime);
		dclog.info("ATs now working on target" + target.getRescueInfo().getNowWorkingOnMe() + "target need ATs = " + getNumberOfATNeedToRescue(target, deathTime));
		return true;
	}

	/************************************* canMakeIntrupt ****************************************/
	public boolean canMakeIntrupt() {
		boolean result = false;
		CycleInformations last = agent.informationStacker.getInformations(1);
		CycleInformations twoCycleAgo = agent.informationStacker.getInformations(2);
		if (last.getAct() instanceof MoveAction && !(last.getAct() instanceof StockMoveAction)) {
			if (twoCycleAgo.getAct() instanceof MoveAction && !(twoCycleAgo.getAct() instanceof StockMoveAction)) {
				dclog.info(this + "Checking if heavystock");
				int distanceLast = PoliceUtils.getDistance(agent.me().getPositionPair().second(), last.getPositionPair().second());
				int distanceTwoCycleAgo = PoliceUtils.getDistance(agent.me().getPositionPair().second(), twoCycleAgo.getPositionPair().second());

				dclog.trace("distanceToLastLocation:" + distanceLast + " distanceTo2CycleAgoLocation:" + distanceTwoCycleAgo);
				if (distanceLast < PoliceConstants.STOCK_DISTANCE && distanceTwoCycleAgo < PoliceConstants.STOCK_DISTANCE) {
					result = true;
				}
			} else if (twoCycleAgo.getAct() instanceof StockMoveAction) {
				dclog.info(this + "Checking if heavystock when twoCycleAgo.getAct() instanceof StockMoveAction");
				int distanceLast = PoliceUtils.getDistance(agent.me().getPositionPair().second(), last.getPositionPair().second());
				dclog.trace("distanceToLastLocation:" + distanceLast);
				if (distanceLast < PoliceConstants.STOCK_DISTANCE) {
					result = true;
				}
			}
		}
		dclog.trace(this + " can make intrupt?" + result);
		return result;
	}

	/********************************************************************************************/
	/************************************* FindValidTargets *************************************/
	/**
	 * preparing target list in both center and agent usage
	 * 
	 * @param isCenterDesiding
	 */
	public void findValidTargets(boolean isCenterDesiding) {

		if (isCenterDesiding)
			dclog.info("\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" +
					" Center is Desiding $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n");

		//		if (lastCycleIwasHere == agent.time()) {
		//			dclog.info("********** I am leader and I have made my lists before! **********");
		//			return;
		//		}
		//
		//		lastCycleIwasHere = agent.time();

		dclog.info("*********** FindValidTargets **************");
		targets = new ArrayList<Human>();

		ArrayList<Human> allHumans = new ArrayList<Human>();
		allHumans = agent.model().humans();
		dclog.info("allHumans are " + allHumans);

		for (Human human : allHumans) {
			if (human.getRescueInfo().getIgnoredUntil() <= agent.time()) {
				humanUpdateLog.info("Human is ignored Until :" + human.getRescueInfo().getIgnoredUntil());
				human.getRescueInfo().setNotIgnored();
			}

			if (isCenterDesiding && !AmbulanceUtils.isValidToDecideForCenter(human, humanUpdateLog))
				continue;

			if (!isCenterDesiding && !AmbulanceUtils.isValidToDecide(human, humanUpdateLog, (AmbulanceTeamAgent) agent))
				continue;

			if (!isCenterDesiding && (agent.messageSystem.type != Type.NoComunication) && (agent.messageSystem.type != Type.LowComunication)) {
				AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) (agent));
				String infos = ambulance.xmlLog.addTag("Human", human.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", human.toString());
				int unUpdatedTime = Integer.MAX_VALUE;

				if (human instanceof Civilian)
					unUpdatedTime = agent.time() - ((Civilian) human).getLastReachableTime();
				else
					unUpdatedTime = agent.time() - human.updatedtime();

				if (unUpdatedTime < MAX_UNUPDATED_TIME) {
					humanUpdateLog.debug(human + " is invalid for agent because it is new sense and still 3 cycle doesn't pass");
					infos += ambulance.xmlLog.addTag("Validity", "invalid");
					infos += ambulance.xmlLog.addTag("stateReason", " is invalid for agent because it is new sense and still 3 cycle doesn't pass");
					ambulance.xmlLog.Info(infos);
					continue;
				}
			}
			targets.add(human);
		}

		dclog.debug("valid targets =" + targets);

		dclog.info("*******************************************");
	}

	public void findValidVirtualCivilians() {
		ArrayList<VirtualCivilian> virtualCivilians = new ArrayList<VirtualCivilian>();
		virtualCivilians = agent.model().getVirtualCivilians();
	}

	/*********************************************************************************************/
	/************************************* findValidAmbulances ***********************************/
	public void findValidAmbulances() {
		ambulances = new ArrayList<AmbulanceTeam>(agent.model().ambulanceTeams().size());
		dclog.info("*********** FindValidAmbulances **************");
		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.isReadyToAct())
				ambulances.add(at);
		}

		dclog.debug("valid Ambulances=" + ambulances);
		dclog.info("*********************************************");
	}

	/***********************************************************************************************/

	private boolean isHumanDeadTimeLessThanValidTime(Human human, int validTime) {
		if (human.getRescueInfo().getInjuryDeathTime() < validTime)
			return true;

		return false;
	}

	/**
	 * @param hm
	 * @r@mik update nearest reachable refuge of Human and time to reaching that refuge
	 */
	/************************* calculateRefugeInformation ***********************************/
	public void calculateRefugeInformation(Human human) {
		if (!human.isPositionDefined() || human.getPosition() instanceof Refuge)
			return;
		if (!(human instanceof Civilian))
			return;
		if (agent.model().refuges().isEmpty() || human.getRescueInfo().getRefugeCalculatedTime() == agent.time())
			return;
		if (costTable == null) {//if is from hear
			human.getRescueInfo().setTimeToRefuge(AVERAGE_MOVE_TO_TARGET);
			return;
		}
		long minCost = Long.MAX_VALUE;
		Refuge best = null;
		for (Refuge refuge : agent.model().refuges()) {
			long cost = costTable.getCostFromTo(refuge, human.getAreaPosition());
			if (cost < minCost) {
				best = refuge;
				minCost = cost;
			}
		}

		if (best != null && minCost < MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING) {
			//TODO else nabayad null mikard?
			human.getRescueInfo().setBestRefuge(best);
			human.getRescueInfo().setTimeToRefuge(gu.getFoolMoveTime(minCost));
			
		}

		human.getRescueInfo().setRefugeCalculatedTime(agent.time());

	}

	/***********************************************************************************************/
	/****************************** calculateRefugeInformation_old *********************************/
	/**
	 * @param hm
	 * @r@mik update nearest reachable refuge of Human and time to reaching that refuge
	 */
	public void calculateRefugeInformation_old(Human hm) {
		if (!hm.isPositionDefined() || hm.getPosition() instanceof Refuge)
			return;
		if (!(hm instanceof Civilian))
			return;
		if (agent.model().refuges().isEmpty() || hm.getRescueInfo().getRefugeCalculatedTime() == agent.time())
			return;

		ArrayList<Pair<? extends Area, Point2D>> Positions = new ArrayList<Pair<? extends Area, Point2D>>(agent.model().ambulanceTeams().size());
		for (Refuge rf : agent.model().refuges())
			Positions.add(new Pair<Area, Point2D>(rf, new Point2D(rf.getX(), rf.getY())));
		int times[] = movingTime(hm.getPositionPair(), Positions);
		setMinTimeRefuge(hm, times);
	}

	/***********************************************************************************************/
	/************************************** movingTime *********************************************/
	public int[] movingTime(Pair<? extends Area, Point2D> from, ArrayList<Pair<? extends Area, Point2D>> targets) {
		int[] times = new int[targets.size()];
		if (hugeMap) {
			for (int i = 0; i < times.length; i++) {
				times[i] = gu.getFoolMoveTimeFromTo(from.first(), targets.get(i).first());
			}
			return times;
		}
		long[] lenghts = agent.move.getMMLenToTargets_notImportantPoint(from, targets);
		for (int i = 0; i < times.length; i++) {
			times[i] = (lenghts[i] >= MoveConstants.UNREACHABLE_COST_FOR_GRAPH_WEIGTHING) ? Integer.MAX_VALUE / 2 : gu.getFoolMoveTime(lenghts[i]);
		}
		return times;
	}

	/***********************************************************************************************/
	/********************************** setMinTimeRefuge *******************************************/
	protected void setMinTimeRefuge(Human hm, int[] times) {
		if (hm.getRescueInfo().getRefugeCalculatedTime() == agent.time() || times.length == 0 || agent.model().refuges().isEmpty())
			return;
		int refTimes[] = new int[agent.model().refuges().size()];
		if (times.length > agent.model().refuges().size()) {
			for (int i = 0; i < agent.model().refuges().size(); i++) {
				refTimes[refTimes.length - 1 - i] = times[times.length - 1 - i];
			}
		} else {
			for (int i = 0; i < refTimes.length; i++)
				refTimes[i] = times[i];
		}

		int min = Integer.MAX_VALUE;
		Refuge rf = null;
		for (int i = 0; i < refTimes.length; i++) {
			if (min > refTimes[i]) {
				min = refTimes[i];
				rf = agent.model().refuges().get(i);
			}
		}
		if (rf != null && min < Integer.MAX_VALUE / 2) {
			hm.getRescueInfo().setBestRefuge(rf);
			hm.getRescueInfo().setTimeToRefuge(min);
		}
		hm.getRescueInfo().setRefugeCalculatedTime(agent.time());
	}

	/***********************************************************************************************/
	/****************************** getNumberOfATNeedToRescue **************************************/
	public int getNumberOfATNeedToRescue(Human human, int time) {
		int limitTime = time;
		/*
		 * FireBrigades are very important in first 100 cycle and it's not important after cycle 160
		 */
		//		if (human instanceof FireBrigade) {
		//			limitTime = Math.min(limitTime, Math.max(60, agent.time() + 40));
		//			limitTime = Math.min(limitTime, 100);
		//		}

		int TimeNeedToBeRescueWithoutMove = agent.time() + human.getRescueInfo().getTimeToRefuge() + TIME_NEED_TO_lOAD_CIVILIAN +
				AmbulanceUtils.getCommunicationDelay(human) + TIME_NEED_TO_UNlOAD_CIVILIAN;
		int TimeNeedToBeRescueWithMove = agent.time() + human.getRescueInfo().getTimeToRefuge() + TIME_NEED_TO_lOAD_CIVILIAN +
				AmbulanceUtils.getCommunicationDelay(human) + (AVERAGE_MOVE_TO_TARGET - 1) + TIME_NEED_TO_UNlOAD_CIVILIAN;

		int numOfATitNeedsWithoutMove = limitTime - TimeNeedToBeRescueWithoutMove;
		int numOfATitNeedsWithMove = limitTime - TimeNeedToBeRescueWithMove;

		int numberOfAtWithoutMove = getNumberOfATNeeded(human.getBuriedness(), numOfATitNeedsWithoutMove);
		int numberOfAtWithMove = getNumberOfATNeeded(human.getBuriedness(), numOfATitNeedsWithMove);

		if (human instanceof FireBrigade && agent.time() < 100) {
			numberOfAtWithMove++;
			numberOfAtWithoutMove++;
		}

		if (numberOfAtWithoutMove > human.getRescueInfo().getNowWorkingOnMe().size())
			return numberOfAtWithMove;
		return numberOfAtWithoutMove;

	}

	/***********************************************************************************************/
	/***************************************** getNumberOfATNeeded *********************************/
	int getNumberOfATNeeded(int bureidness, int numOfATitNeeds) {
		if (numOfATitNeeds == 0)
			return 1;
		if (numOfATitNeeds > 0) {
			numOfATitNeeds = (int) Math.floor(bureidness / numOfATitNeeds) + 1;
			return numOfATitNeeds;
		}

		return 100;
	}

	/***********************************************************************************************/
	/***************************************** removeTooBuriedCivilians ****************************/

	public void removeTooBuriedCivilians() {

		dclog.info("/////////// removeTooBuriedCivilians //////////////");

		ArrayList<Human> remove = new ArrayList<Human>();

		for (Human target : targets) {
			if (target instanceof Civilian && target.getBuriedness() > MIN_HIGH_LEVEL_BURIEDNESS
					&& (target.getRescueInfo().getDeathTime() - agent.time() > 100))
				remove.add(target);

		}
		dclog.debug("removed civilians because too buridness in start of sim :" + remove);

		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : remove) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "tooBurried!!!");
				ambulance.xmlLog.info(infos);
			}
		}

		targets.removeAll(remove);

		dclog.debug("current targets:" + targets);

		dclog.info("///////////////////////////////////////////////////");
	}

	/**************************************************************************************/
	/************************** removeATsAssignedTargets **********************************/
	public void removeATsAssignedTargets() {

		dclog.info("///////////// removeATsAssignedTargets ///////////////");

		ArrayList<Human> removeTargetsBecauseOtherATGetIt = new ArrayList<Human>();

		for (AmbulanceTeam at : agent.model().ambulanceTeams()) {
			if (at.getWork() != null && at.getWork().getTarget() != null) {
				Human human = at.getWork().getTarget();
				if (!isEnoughATsWorkingOnHuman(human)) {
					dclog.logln(human + " has'nt Enough At!");
					continue;
				}
				removeTargetsBecauseOtherATGetIt.add(at.getWork().getTarget());
			}
		}

		dclog.debug("removeTargetsBecauseOtherATGetIt:" + removeTargetsBecauseOtherATGetIt);
		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : removeTargetsBecauseOtherATGetIt) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "Other AT get it!!!");
				ambulance.xmlLog.info(infos);
			}
		}

		targets.removeAll(removeTargetsBecauseOtherATGetIt);

		dclog.debug("current targets:" + targets);

		dclog.info("///////////////////////////////////////////////////");
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
			dclog.info("found nearest cluster");
			validClusters.add(clusters.get(0));
		}
		dclog.debug("removeOtherClusterTargetsInLowOrNoCommunication:current targets:" + targets);

		ArrayList<Human> newTargets = new ArrayList<Human>();
		for (Human target : targets) {
			if ((SOSGeometryTools.distance(target.getAreaPosition().getPositionPoint(), agent.me().getPositionPoint())) < (agent.model().getBounds().getWidth() / 8)) {
				newTargets.add(target);
				continue;
			}
			for (ClusterData validCluster : validClusters) {
				if (validCluster.getBuildings().contains(target.getAreaPosition()))
					newTargets.add(target);
			}
		}
		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : targets) {
				if (newTargets.contains(hu))
					continue;
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "not in my cluster or my current cluster!!!");
				ambulance.xmlLog.info(infos);
			}
		}
		targets = newTargets;
		dclog.debug("current targets:" + targets);
	}

	/**************************************************************************************/
	/***************************** removeUnrescueableTargets *****************************/
	public void removeUnrescueableTargets() {

		dclog.info("//////////// removeUnrescueableTargets ////////////////////");

		ArrayList<Human> remove = new ArrayList<Human>(5);

		for (Human human : targets) {
			if (human.getRescueInfo().getATneedToBeRescued() > ambulances.size()) {
				dclog.logln(human + " needed AT bigger than ambulance sizes!");
				remove.add(human);
			} else if (AmbulanceUtils.taskAssigningExpireTime(human, human.getRescueInfo().getDeathTime(), dclog) - agent.time() < 0) {
				remove.add(human);
				dclog.logln(human + " task AssignningExpireTime passed!" + "DeathTime =" + human.getRescueInfo().getDeathTime()
						+ " time = " + agent.time());
			}

		}

		dclog.debug("remove unrescuable targets = " + remove);
		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : remove) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "unrescueable!!!");
				ambulance.xmlLog.info(infos);
			}
		}
		targets.removeAll(remove);
		dclog.debug("current targets = " + targets);

		dclog.info("///////////////////////////////////////////////////");
	}

	/***************************************************************************************/
	/******************************** getMinCostTarget *************************************/

	public Human getMinCostTarget() {
		dclog.info("*********** getMinCostTarget ***************");

		Pair<Human, Float> HumanAndCost = minCostTargets.poll();

		if (targets.isEmpty() || HumanAndCost == null) {
			dclog.info("***************** null *********************");
			return null;
		}

		dclog.logln("MinCostTarget result = " + HumanAndCost.first() + "    cost=" + HumanAndCost.second());
		dclog.info("*******************************************");
		return HumanAndCost.first();
	}

	/*****************************************************************************************/
	/************************* CostOfrescuingHuman ***************************************/
	private float CostOfrescuingHuman(Human human) {
		dclog.info("********************* CostOfrescuingHuman ********************");
		float cost = 0;
		int duration = taskPerformingDurationForMe(human);
		try {
			int deathTime = human.getRescueInfo().getDeathTime();

			int distanceCost = getDistanceCost(human);

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

			cost = (duration * durationFactor) * 10 + ((deathTime - agent.time()) * deathTimFactor) * 5 + ((distanceCost * 2000) / agent.model().ambulanceTeams().size());
			cost /= 1000;
			float agentWeight = (human instanceof Civilian) ? 1 : getAgentWeight(human);
			cost *= agentWeight;

			dclog.info("Human =" + human + "taskPerformingDurationForMe =" + duration
					+ "deathTime = " + deathTime + " distanceCost = " + distanceCost + " time = " + agent.time() + " agentWeight= " + agentWeight);

		} catch (ArithmeticException ae) { //divided by zero
			cost = Integer.MAX_VALUE;
			System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
		}

		dclog.info("********************************************************");
		return cost;
	}

	/*****************************************************************************************/
	/*********************************** getAgentWeight ***************************************/
	private float getAgentWeight(Human human) {

		if (agent.time() > 120)
			return 1.33f;
		if (human instanceof AmbulanceTeam || human instanceof PoliceForce)
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
	 * private float CostOfrescuingHuman(Human human) {
	 * dclog.info("********************* CostOfrescuingHuman ********************");
	 * float cost = 0;
	 * int duration = taskPerformingDurationForMe(human);
	 * long priority = human.getRescueInfo().getRescuePriority();
	 * try {
	 * int deathTime = human.getRescueInfo().getDeathTime();
	 * int distanceCost =getDistanceCost(human);
	 * if(priority != 0)
	 * cost=(duration*100*distanceCost)/priority;
	 * else
	 * cost = Integer.MAX_VALUE;
	 * dclog.info("Human =" + human + "taskPerformingDurationForMe =" + duration + "priority = " + priority
	 * + "deathTime = " + deathTime + " distanceCost = " + distanceCost);
	 * } catch (ArithmeticException ae) { //divided by zero
	 * cost = Integer.MAX_VALUE;
	 * System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
	 * }
	 * dclog.info("********************************************************");
	 * return cost;
	 * }
	 */
	/*****************************************************************************************/
	/*********************************** getDistanceCost *************************************/
	private int getDistanceCost(Human human) {
		ArrayList<ClusterData> clusters = new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
		final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();
		Collections.sort(clusters, new DistanceToMyClusterComparator(myCluster));

		for (ClusterData cluster : clusters) {
			if (cluster.getBuildings().contains(human.getAreaPosition())) {
				return (clusters.indexOf(cluster) + 1);
			}
		}

		return 0;
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
	/************************* numberOfATHumanStillNeeded ************************************/
	private int numberOfATHumanStillNeeded(Human human) {

		short numberOfATHumanNeedToBeRescued = human.getRescueInfo().getATneedToBeRescued();
		int numberOfATisWorkingOnHumanNow = human.getRescueInfo().getNowWorkingOnMe().size();

		return (numberOfATHumanNeedToBeRescued - numberOfATisWorkingOnHumanNow);
	}

	/*****************************************************************************************/
	/*************************** doWeHaveEnoughTargets ***************************************/
	private boolean doWeHaveEnoughTargets() {
		if (targets.size() > ambulances.size() / 3)
			return true;

		return false;
	}

	/*****************************************************************************************/
	/**************************** taskPerformingDurationForMe *******************************/
	public int taskPerformingDurationForMe(Human target) {
		int cycle = 0;
		try {
			/* cycles needs to removing buridness */
			cycle += Math.ceil(target.getBuriedness() / (float) target.getRescueInfo().getATneedToBeRescued());
			cycle += (target.getRescueInfo().getATneedToBeRescued() - 1);
			/* cycles needs to load and civilians to refuge */
			if (target instanceof Civilian) {
				cycle++;
				cycle += target.getRescueInfo().getTimeToRefuge();
			}
			/* cycles needs to move to target */
			long moveWeight = agent.move.getWeightTo(target.getAreaPosition(), StandardMove.class) * MoveConstants.DIVISION_UNIT_FOR_GET;
			cycle += gu.getFoolMoveTime(moveWeight);

		} catch (Exception ex) {
			cycle += target.getBuriedness() + 2;
			cycle += (2 * AVERAGE_MOVE_TO_TARGET);
			ex.printStackTrace();
		}
		return cycle;
	}

	/*****************************************************************************************/
	/****************************** makePriorityListFromTargets ******************************/
	public void makePriorityListFromTargets() {

		dclog.info("******** makePriorityListFromTargets **************");
		Collections.sort(targets, new PriorityComparator());

		if (!SOSConstant.IS_CHALLENGE_RUNNING)
			getPrirotyLog();
		dclog.info("*******************************************");
	}

	/*****************************************************************************************/
	/************************************* getPrirotyLog *************************************/
	private void getPrirotyLog() {

		dclog.log("PRIORITY::::::");
		for (Human hum : targets) {
			dclog.log("[" + hum + " ->     RescuePriority:" + hum.getRescueInfo().getRescuePriority() + "\tTarget CurrentState " + getTargetCurrentState(hum) + "] ");
		}
		dclog.logln("");
	}

	/*****************************************************************************************/
	/******************************* assignPrioritytoTargets *********************************/
	public void assignPrioritytoTargets() {

		dclog.info("************ assignPrioritytoTargets ***********");

		if (targets.isEmpty())
			return;

		for (Human human : targets) {

			if (getTargetCurrentState(human) == CivilianState.DEATH) {
				human.getRescueInfo().setRescuePriority(0);
				continue;
			}
			//TODO sinash : inja begim ke harki buriednessesh 0 bood vali damage dasht ( va fireDamage nabood, yani faghat
			//	halati ke tu khiaboon unload shode bashe ya rescue shode bashe vali load nashode bashe) bishtarin priority e momken.

			int priority = 0;

			if (human instanceof Civilian)
				priority = getCivilianPriority(human);
			else
				priority = getAgentPriority(human);

			dclog.info("Human = " + human + "priority = " + priority);

			human.getRescueInfo().setRescuePriority(priority);
		}

		dclog.info("*******************************************");
	}

	/*****************************************************************************************/
	/********************************* getAgentPriority **************************************/
	private int getAgentPriority(Human human) {

		dclog.info("********* getAgentPriority ***********");

		/* sinash 2013 - determination of ready forces */
		dclog.logln("SINA: In ready forces determination : ");
		ArrayList<FireBrigade> readyFireBrigades = getReadyFireBrigades();
		ArrayList<AmbulanceTeam> readyAmbulanceTeams = getReadyAmbulanceTeam();
		ArrayList<PoliceForce> readyPoliceForces = getReadyPoliceForce();
		dclog.logln("SINA: readyFireBrigades: " + readyFireBrigades.size() + " readyAmbulanceTeams: " + readyAmbulanceTeams.size() + " readyPoliceForces: " + readyPoliceForces.size());

		/* Added by Ali for distributing rescuing agent */
		int priority = agent.time() < 100 ? MAX_PRIORITY : MIN_PRIORITY;
		dclog.info("priority as cycle = " + priority);

		if (human instanceof FireBrigade)
			priority += (30 - readyFireBrigades.size()) / 5;
		if (human instanceof AmbulanceTeam)
			priority += (30 - readyAmbulanceTeams.size()) / 5;
		if (human instanceof PoliceForce)
			priority += (30 - readyPoliceForces.size()) / 5;

		dclog.info("priority as kind of agent = " + priority);

		priority += human.getAgentIndex() % 3;

		dclog.info("priority as distribution = " + priority);

		dclog.info("*****************************************");
		return priority;
	}

	/*****************************************************************************************/
	/******************************* getCivilianPriority ************************************/
	private int getCivilianPriority(Human human) {

		dclog.info("********* getCivilianPriority ***********");
		//old priority hm.getRescueInfo().setRescuePriority(((10000000 / (hm.getRescueInfo().getDeathTime() + 1 - agent.time())) * (320 - hm.getBuriedness()) / 300));
		int deathTime = human.getRescueInfo().getDeathTime();
		int priority = MAX_PRIORITY / (deathTime + 1 - agent.time());
		dclog.info("calculate priority from deathTime :" + "deathTime = " + deathTime
				+ "priority = " + priority);
		priority = (priority * (320 - human.getBuriedness() * 2)) / 300;
		dclog.info("calculate priority from burriedness :" + "burriedness = " + human.getBuriedness()
				+ "priority = " + priority);

		if (human.getBuriedness() == 0 && human.getDamage() > 0) {
			priority *= 1.5; /* added by sinash for final day IranOpen 2013 */
			dclog.info("without buriedness and has Damage civilian" + " priority = " + priority);
		}

		if (human.getRescueInfo().longLife()) {
			priority /= 10;
			dclog.info("long life civilian " + " priority = " + priority);
		}

		dclog.info("*****************************************");
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
	/********************************* getTargetCurrentState *********************************/

	private CivilianState getTargetCurrentState(Human human) {
		int deathTime = human.getRescueInfo().getDeathTime();

		if (deathTime >= agent.time())
			return CivilianState.CRITICAL;

		return CivilianState.DEATH;

	}

	/*****************************************************************************************/
	/****************** removeAgentsTargetAfterMiddleOfSimulation ****************************/
	public void removeAgentsTargetAfterMiddleOfSimulation() {
		dclog.info("************* removeAgentsTargetAfterMiddleOfSimulation ********************");
		if (StartMIDDLE_OF_SIMULATION > agent.time()) {
			dclog.debug("removeAgentsTargetAfcterMiddleOfSimulation:: before startMiddle");
			return;
		}
		ArrayList<Human> removed = new ArrayList<Human>();
		if (StartMIDDLE_OF_SIMULATION < agent.time() && MIDDLE_OF_SIMULATION > agent.time())
		{
			dclog.debug("removeAgentsTargetAfcterMiddleOfSimulation:: before middle(remove high levels)");
			for (Human h : targets) {
				if (!(h instanceof Civilian) && h.getBuriedness() > MIN_HIGH_LEVEL_BURIEDNESS + 10)
					removed.add(h);
			}
		}
		else if (agent.time() < 200 && agent.model().getLastAfterShockTime() > 3) {
			dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after middle");
			dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after shock detected");
			for (Human h : targets) {
				if (!(h instanceof Civilian) && h.getBuriedness() > MIN_HIGH_LEVEL_BURIEDNESS + 3)
					removed.add(h);
			}
		}
		else {
			dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: after middle");
			dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: remove all agents");
			for (Human h : targets) {
				if (!(h instanceof Civilian) && h.getBuriedness() > 12)
					removed.add(h);
			}
		}

		dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: current targets:" + targets);
		dclog.debug("removeAgentsTargetAfterMiddleOfSimulation:: removed targets:" + removed);

		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : removed) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "remove agent!!!");
				ambulance.xmlLog.info(infos);
			}
		}
		targets.removeAll(removed);

		dclog.debug("current targets:" + targets);
	}

	/*****************************************************************************************/
	/**************************** removeCenterAssignListTarget *******************************/
	public void removeCenterAssignListTarget() {
		dclog.info("************* removeCenterAssignListTarget ********************");
		dclog.debug("removeCenterAssignListTarget:: current targets:" + targets);

		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : ambulance.centerAssignLists) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "center assigned list!!!");
				ambulance.xmlLog.info(infos);
			}
		}
		if (agent instanceof AmbulanceTeamAgent)
			targets.removeAll(((AmbulanceTeamAgent) agent).centerAssignLists);
		dclog.debug("current targets:" + targets);
	}

	/*****************************************************************************************/
	/************************************ filterTargets **************************************/

	public void filterTargets() {
		dclog.info("************* filterTargets ********************");
		if (targets.isEmpty()) {
			dclog.info("targets is empty");
			return;
		}

		if (agent.model().refuges().isEmpty())
			withOutRefugeMapTargets();

		if (agent.messageSystem.type != Type.NoComunication && agent.time() < START_OF_SIMULATION)
			removeTooBuriedCivilians();

		removeAgentsTargetAfterMiddleOfSimulation();

		removeATsAssignedTargets();

		removeCenterAssignListTarget();

		setNumberOfATsWhichTagetNeed();

		removeUnrescueableTargets();

		if (agent.messageSystem.type == Type.NoComunication && agent.time() < 150)
			removeMoreThanTwoAgentsNeedTargets();

		if (agent.messageSystem.type == Type.LowComunication || agent.messageSystem.type == Type.NoComunication)
			removeOtherClusterTargets();

		if (agent.messageSystem.type == Type.NoComunication)
			removeMyCurrentClusterTargetIfRequired();

		dclog.info("*******************************************");
	}

	private void removeMoreThanTwoAgentsNeedTargets() {

		dclog.info("//////////// removeUnrescueableTargets ////////////////////");
		ArrayList<Human> removed = new ArrayList<Human>();
		for (Human human : targets) {
			if (human.getRescueInfo().getATneedToBeRescued() > 2) {
				dclog.logln(human + " needed AT more than 2 !");
				removed.add(human);
			}
		}

		dclog.debug("remove unrescuable targets = " + removed);
		if (XMLLogging) {
			AmbulanceTeamAgent ambulance = ((AmbulanceTeamAgent) agent);
			for (Human hu : removed) {
				String infos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
				infos += ambulance.xmlLog.addTag("Type", hu.toString());
				infos += ambulance.xmlLog.addTag("Validity", "invalid");
				infos += ambulance.xmlLog.addTag("stateReason", "more than 2 in noCom!!!");
				ambulance.xmlLog.info(infos);
			}
		}
		targets.removeAll(removed);
		dclog.debug("current targets = " + targets);

		dclog.info("///////////////////////////////////////////////////");
	}

	private void removeMyCurrentClusterTargetIfRequired() {
		// Unfortunately, This function is only used in Kobe since is not tested in other maps.
		if (!agent.getMapInfo().getRealMapName().equals(MapName.Kobe))
			return;

		dclog.info("************** removeMyCurrentClusterTargetIfRequired *************");
		final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();

		ArrayList<Human> myCurrentClusterTargets = new ArrayList<Human>();
		for (Human target : targets) {

			if (!(target instanceof Civilian))
				continue;
			if (myCluster.getBuildings().contains(target.getAreaPosition()))
				continue;

			myCurrentClusterTargets.add(target);
		}

		if (myCurrentClusterTargets.isEmpty()) {
			dclog.info(" myCurrentClusterTargets is empty");
			return;
		}
		dclog.info(" myCurrentClusterTargets == " + myCurrentClusterTargets.size());

		if (myCurrentClusterTargets.size() == 1) {
			Human target = myCurrentClusterTargets.get(0);
			int timeItHas = target.getRescueInfo().getDeathTime() - agent.time()
					- (target.getBuriedness() / Math.max(1, target.getRescueInfo().getNowWorkingOnMe().size())
							+ TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_UNlOAD_CIVILIAN + target.getRescueInfo().getTimeToRefuge());
			dclog.info(" timeItHas = " + timeItHas + "(40)");
			if (timeItHas > 40) {
				dclog.info(" myCurrentClusterTargets removed");
				targets.remove(target);
			}
		}
		else if (myCurrentClusterTargets.size() == 2) {
			Human target1 = myCurrentClusterTargets.get(0);
			int timeItHas1 = target1.getRescueInfo().getDeathTime() - agent.time()
					- (target1.getBuriedness() / Math.max(1, target1.getRescueInfo().getNowWorkingOnMe().size())
							+ TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_UNlOAD_CIVILIAN + target1.getRescueInfo().getTimeToRefuge());
			Human target2 = myCurrentClusterTargets.get(1);
			int timeItHas2 = target2.getRescueInfo().getDeathTime() - agent.time()
					- (target2.getBuriedness() / Math.max(1, target2.getRescueInfo().getNowWorkingOnMe().size())
							+ TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_UNlOAD_CIVILIAN + target2.getRescueInfo().getTimeToRefuge());

			dclog.info("timeItHas target1 = " + timeItHas1 + "timeItHas target2 = " + timeItHas2);
			dclog.info("sum = " + (timeItHas1 + timeItHas2) + "min " + Math.min(timeItHas1, timeItHas2));
			if ((timeItHas1 + timeItHas2) > 90 && Math.min(timeItHas1, timeItHas2) > 40) {
				int notSearched = myCluster.getBuildings().size();
				for (Building building : myCluster.getBuildings())
					if (building.isSearchedForCivilian())
						notSearched--;

				float probablity = notSearched / Math.max(1, myCluster.getBuildings().size());
				dclog.info("notsearched = " + notSearched + " building num = " + myCluster.getBuildings().size()
						+ " probablity = " + probablity + " min = " + Math.min((agent.time() * 0.7) / Math.max(agent.time(), 350), 1));
				if (probablity > Math.min((agent.time() * 1.3) / Math.max(agent.time(), 350), 1)) {
					dclog.info("myCurrentClusterTargets removed");
					targets.removeAll(myCurrentClusterTargets);
				}

			}

		}
		else if (myCurrentClusterTargets.size() == 3) {

			int minTimeItHas = Integer.MAX_VALUE;
			for (Human target : myCurrentClusterTargets) {
				int timeItHas = target.getRescueInfo().getDeathTime() - agent.time()
						- (target.getBuriedness() / Math.max(1, target.getRescueInfo().getNowWorkingOnMe().size())
								+ TIME_NEED_TO_lOAD_CIVILIAN + TIME_NEED_TO_UNlOAD_CIVILIAN + target.getRescueInfo().getTimeToRefuge());
				if (timeItHas < minTimeItHas)
					minTimeItHas = timeItHas;
			}
			dclog.info(" minTimeItHas = " + minTimeItHas + "(70)");
			if (minTimeItHas > 70) {
				dclog.info(" myCurrentClusterTargets removed");
				targets.removeAll(myCurrentClusterTargets);
			}

		}
		dclog.info("****************************************************");

	}

	/*****************************************************************************************/
	/******************************** withOutRefugeMapTargets ********************************/
	/*
	 * If there is no refuge in map we don't rescue civilians that are in fireBuildings or they are
	 * not in the building
	 */
	private void withOutRefugeMapTargets() {
		dclog.info("////////// withOutRefugeMapTargets ////////////////");
		ArrayList<Human> removes = new ArrayList<Human>();
		for (Human human : targets) {
			if (!(human instanceof Civilian))
				continue;

			if (AmbulanceUtils.isHumaninFireBuilding(human)
					|| isHumanDeadTimeLessThanValidTime(human, AmbulanceConstants.VALID_DEATH_TIME_FOR_NO_REFUGE_MAP)
					|| !(human.getPosition() instanceof Building))
				removes.add(human);

		}
		dclog.debug("remove list because it is no refuge map=>" + removes);
		targets.removeAll(removes);

		dclog.debug("current targets:" + targets);

		dclog.info("///////////////////////////////////////////////////");
	}

	/*****************************************************************************************/
	/*************************** setNumberOfATsWhichTagetNeed ****************************/
	public void setNumberOfATsWhichTagetNeed() {

		if (lastCycleISetNumberOfATs == agent.time()) {
			dclog.info("********** I am leader and I have made my lists before! **********");
			return;
		}
		lastCycleISetNumberOfATs = agent.time();
		dclog.info("///////////// setNumberOfATsWhichTagetNeed ////////////////");

		dclog.logln("targets = ");

		for (Human human : targets) {
			int deathTime = human.getRescueInfo().getDeathTime();
			int numberOfATNeed = getNumberOfATNeedToRescue(human, deathTime);
			human.getRescueInfo().setATneedToBeRescued(numberOfATNeed);

			dclog.logln("    " + human + "--> b=" + human.getBuriedness() + " hp:" + human.getHP()
					+ " d=" + human.getDamage() + " ATneedToBeRescued=" + human.getRescueInfo().getATneedToBeRescued()
					+ "    working=" + human.getRescueInfo().getNowWorkingOnMe() + "  dTime=" + human.getRescueInfo().getDeathTime() + " "
					+ human.getRescueInfo().getBestRefuge() + "  refTime=" + human.getRescueInfo().getTimeToRefuge()
					+ " longLife=" + human.getRescueInfo().longLife() + " Reachable:" + AmbulanceUtils.isReachableForAT(human, true));
		}

		dclog.logln("");

		dclog.info("///////////////////////////////////////////////////");
	}

	/*************************************************************************************/
	/***************************** assignCosttoTargets ***********************************/
	public void assignCosttoTargets() {

		dclog.info("************ assignCosttoTargets ****************");

		minCostTargets = new PriorityQueue<Pair<Human, Float>>(targets.size() + 5, new CostComparator());

		for (int i = 0; i < targets.size(); i++) {
			Human human = targets.get(i);

			if (isEnoughATsWorkingOnHuman(human)) {
				dclog.logln(human + " has Enough At!");
				continue;
			}
			float cost = CostOfrescuingHuman(human);
			/*
			 * if (agent.messageSystem.type == Type.LowComunication || agent.messageSystem.type == Type.NoComunication)
			 * cost*=Math.max(numberOfATHumanStillNeeded(human),1);
			 * else
			 * cost/=Math.max(numberOfATHumanStillNeeded(human),1);
			 */
			Pair<Human, Float> HumanAndCost = new Pair<Human, Float>(human, cost);

			dclog.logln("target=" + human + " --> cost=" + HumanAndCost.second());
			minCostTargets.offer(HumanAndCost);
		}

		dclog.logln("");
		dclog.info("*******************************************");
	}

	/**
	 * **********************************************************************************************
	 * functions below are only for center assigning usage
	 * **********************************************************************************************
	 */

	/******************************** imagining ******************************************/
	public void imagining() {

		if (targets.isEmpty())
			return;

		dclog.info("IMAGINING START......");
		int costTimes[][] = new int[ambulances.size()][targets.size()];
		//        TreeSet<Integer> ignoredTargets = new TreeSet<Integer>();
		boolean[] ignoredTargets = new boolean[targets.size()];
		Arrays.fill(ignoredTargets, false);
		dclog.debug("Targets:" + targets);

		PriorityQueue<ImaginationShot> imaginationQueue = new PriorityQueue<ImaginationShot>(targets.size() + 3, new ImaginationShotPriorityComparator());
		ArrayList<ImaginationShot> listOfImages = new ArrayList<ImaginationShot>();
		ArrayList<AmbulanceDream> helpingDreams = new ArrayList<AmbulanceDream>();
		dclog.debug("Current Helping Dreams(Ready Ambulances)==>");
		for (AmbulanceTeam at : ambulances) {
			AmbulanceDream hd = new AmbulanceDream(at, at.getWork().getNextFreeTime());
			helpingDreams.add(hd);
			dclog.logln("\t" + hd);
		}

		for (int i = 0; i < targets.size(); i++) {
			Human hm = targets.get(i);
			ImaginationShot img = new ImaginationShot(hm, getTargetDesireState(hm, getTargetCurrentState(hm)), i);
			listOfImages.add(img);
			imaginationQueue.offer(img);
		}
		dclog.debug("Current Imagination shots=" + listOfImages);
		HashMap<Long, Integer> hm_MovingTime = new HashMap<Long, Integer>();
		
		while (true) {
			boolean first = true;
			////////////////////////////////////////////////////////////////////////////////////////////
			while (!imaginationQueue.isEmpty()) {

				ImaginationShot currentImage = imaginationQueue.poll();
				dclog.logln("");
				dclog.log("\tcurrent ImaginationShot = " + currentImage);
				if (ignoredTargets[currentImage.index]) { //not enough reachable AT to target or make penalty
					dclog.logln("-->ignored");
					continue;
				}
				dclog.logln("");

				//finding in position cost for AT
				/********************* set TimeCost for ATs to move to current target ****************************/
				if (first) {
					//TODO it is not correct
					ArrayList<Area> srcs = new ArrayList<Area>(agent.model().ambulanceTeams().size());
					for (int j = 0; j < helpingDreams.size(); j++)
						srcs.add(helpingDreams.get(j).at.getAreaPosition());
					for (Refuge rf : agent.model().refuges()) {
						srcs.add(rf);
					}
					int times[] = getMovingTime(srcs, currentImage.target.getAreaPosition());

					for (int i = 0; i < times.length; i++) {
						if (i < helpingDreams.size())//if it's not refuge
							costTimes[i][currentImage.index] = times[i];
						hm_MovingTime.put(getImageKey(currentImage, srcs.get(i).getPositionPair()), times[i]);
					}
					/************ found best refuge for current target **********/
					calculateRefugeInformation(currentImage.target);
				}
				/********************** LOG(TimeCost of Ats for current target *******************************/
				dclog.log("\t\t" + currentImage.target + " COSTS-> ");
				for (int z = 0; z < ambulances.size(); z++) {
					dclog.log(costTimes[z][currentImage.index] + " \t");
				}
				dclog.logln("");
				/************ calculate number of ATs that are suitable for rescue current target ****************/
				//Removing Not enough reachable AT to target;
				int count = 0;
				for (int j = 0; j < ambulances.size(); j++) {
					if (costTimes[j][currentImage.index] < AmbulanceConstants.LONG_TIME)
						count++;
				}
				/************* ignore target if there is not suitable enough good At to rescue him ****************/
				int numberOfAtsneedesNow = (currentImage.target.getRescueInfo().getATneedToBeRescued() - currentImage.target.getRescueInfo().getNowWorkingOnMe().size());
				if (count < numberOfAtsneedesNow) {
					ignoredTargets[currentImage.index] = true;
					dclog.logln("\t\tNot enough reachable AT to :" + currentImage.index);
					continue;
				}
				/************************** LOG ( (TimeCost + time_To_be_free ) for each AT ) *********************/
				dclog.log("\t\t COSTS +finishtime    -> ");
				for (int z = 0; z < ambulances.size(); z++) {
					dclog.log((costTimes[z][currentImage.index] + helpingDreams.get(z).time_To_be_free) + " \t");
				}
				dclog.logln("");
				/********************* filter ATs with filter of "who rescue current target sooner?" ***************/
				boolean[] ignoredATs = new boolean[ambulances.size()];
				Arrays.fill(ignoredATs, false);

				dclog.logln("\t\tnewAtNeed" + numberOfAtsneedesNow + " totalATneed:"
						+ currentImage.target.getRescueInfo().getATneedToBeRescued()
						+ " nowWorking:" + currentImage.target.getRescueInfo().getNowWorkingOnMe().size());

				for (int i = 0; i < numberOfAtsneedesNow; i++) {
					int min_at_index = minFreeTimeAmbulanceIndex(helpingDreams, costTimes, currentImage.index, ignoredATs, agent);

					dclog.logln("\t\tmin_at =" + helpingDreams.get(min_at_index).at
							+ " cost+finishtime=" + ((costTimes[min_at_index][currentImage.index]
							+ helpingDreams.get(min_at_index).time_To_be_free)));

					ignoredATs[min_at_index] = true;
					AmbulanceDream ad = helpingDreams.get(min_at_index);
					ad.addImagination(currentImage, costTimes[min_at_index][currentImage.index]);
				}

			} //end internal while

			/**************** found if there is any penalty for rescuing ************************/
			int penalty = 0;
			dclog.log("\tImagination shots after assigning=");
			long lastPenaltyPriority = 0;
			for (ImaginationShot is : listOfImages) {
				if (ignoredTargets[is.index])
					continue;
				if ((is.goalCondition != CivilianState.DEATH) && is.resultCondition == CivilianState.DEATH) {
					penalty += 1000;
					lastPenaltyPriority = is.target.getRescueInfo().getRescuePriority();
				}
				dclog.log(is + " ");
			}

			dclog.logln("");
			dclog.logln("[Penalty=" + penalty + "]");

			/********************** ignore target with maxATneedNow ************************/
			if (penalty >= 1000) {
				/*** get index of target with maxATneadNow **/
				int index = maxWorkNeedImageIndex(listOfImages, ignoredTargets, lastPenaltyPriority);

				/*** no one need AT now ****/
				if (index == -1) {
					dclog.logln("\tindex=-1");
					break;
				}

				dclog.logln("\tindex=" + index + "  maxWorkRemoving=" + listOfImages.get(index));
				ignoredTargets[index] = true;
				for (AmbulanceDream ad : helpingDreams)
					ad.reset();
				imaginationQueue.clear();
				for (ImaginationShot img : listOfImages) {
					img.reset();
					imaginationQueue.offer(img);
				}
			}
			else
				break;

		} //end of while

		dclog.logln("assigns ={{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{............");
		assign = new HashMap<AmbulanceTeam, ImaginationShot[]>();
		for (AmbulanceDream ad : helpingDreams) {
			ImaginationShot targets[] = new ImaginationShot[2];
			if (ad.performing.size() > 0)
				targets[0] = ad.performing.get(0);
			if (ad.performing.size() > 1)
				targets[1] = ad.performing.get(1);
			assign.put(ad.at, targets);

			dclog.debug(ad.at +
					"fct:" + ad.at.getWork().getNextFreeTime() +
					"  -->  target1:" + (targets[0] == null ? "null" : targets[0].target) +
					" , target2:" + (targets[1] == null ? null : targets[1].target) + " his current target=" +
					ad.at.getWork().getTarget());

		}
		dclog.logln("......................}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
	}

	/*******************************************************************************************/
	/******************************* getTargetDesireState **************************************/

	public CivilianState getTargetDesireState(Human hm, CivilianState current) {
		switch (current) {
		case HEALTHY:
		case AVERAGE:
		case CRITICAL:
			return CivilianState.CRITICAL;

		case DEATH:
		default:
			return CivilianState.DEATH;
		}
	}

	/********************************************************************************************/
	/*********************************** getMovingTime ******************************************/
	private int[] getMovingTime(ArrayList<Area> srcs, Area dst) {
		int[] times = new int[srcs.size()];
		for (int i = 0; i < srcs.size(); i++) {
			Area src = srcs.get(i);
			times[i] = gu.getFoolMoveTime(costTable.getCostFromTo(src, dst));
		}
		return times;
	}

	/*********************************************************************************************/
	/************************************* getImageKey *******************************************/
	private long getImageKey(ImaginationShot is, Pair<? extends Area, Point2D> ad) {
		return (long) ((ad.second().getX() + ad.second().getY()) / (ad.first().getAreaIndex() + 1) + (is.target.getX() + is.target.getY()) / (is.target.getPositionArea().getAreaIndex() + 1));
	}

	/*********************************************************************************************/
	/******************************** maxWorkNeedImageIndex **************************************/
	protected int maxWorkNeedImageIndex(ArrayList<ImaginationShot> list, boolean[] ignored, long priority) {
		ImaginationShot max = null;
		int max_val = 0;

		for (ImaginationShot is : list) {
			if (ignored[is.index] || is.target.getRescueInfo().getRescuePriority() < priority)
				continue;
			int numberOfAtsneedesNow = (is.target.getRescueInfo().getATneedToBeRescued() - is.target.getRescueInfo().getNowWorkingOnMe().size());
			if (numberOfAtsneedesNow > max_val) {
				max_val = numberOfAtsneedesNow;
				max = is;
			}
			else if (numberOfAtsneedesNow == max_val) {

				if (max != null && is.target.getBuriedness() > max.target.getBuriedness())
					max = is;
			}
		}
		dclog.debug("Ignoring " + max + " in maxWorkNeedImage");
		return max.index;
	}

	/*********************************************************************************************/
	/***************************** minFreeTimeAmbulanceIndex *************************************/
	protected int minFreeTimeAmbulanceIndex(ArrayList<AmbulanceDream> ADlist, int[][] cost, int index, boolean[] ignored, SOSAgent<?> ag) {

		int minIndex = 0;
		int minValue = Integer.MAX_VALUE;
		for (int i = 0; i < ADlist.size(); i++) {
			if (ignored[i])
				continue;

			AmbulanceDream ad = ADlist.get(i);
			int performFinishTime = ad.time_To_be_free + cost[i][index];

			if (minValue > performFinishTime) {
				minIndex = i;
				minValue = performFinishTime;
			}

		}

		return minIndex;
	}

	/*********************************************************************************************/
	/********************************** sendAssignMessages ***************************************/
	public void sendAssignMessages() {
		if (targets.isEmpty())
			return;
		for (Map.Entry<AmbulanceTeam, ImaginationShot[]> mapEntry : assign.entrySet()) {
			ImaginationShot[] img = mapEntry.getValue();

			if (img[0] == null || img[0].target == null/* || me.getKey().getWork().getNextFreeTime()-agent.time()>4 */) {
				dclog.info(" Assigned task was null or its target was null==========>");
				dclog.info("            " + img[0]);
				continue;
			}
			//It's not logical to send message to myself! :D so...
			if (agent instanceof AmbulanceTeamAgent && mapEntry.getKey().getAmbIndex() == ((AmbulanceTeamAgent) agent).me().getAmbIndex()) {
				((AmbulanceTeamAgent) agent).setCenterRecommendedTarget(img[0].target);
				continue;
			}
			int mlposIndex = img[0].target.getPositionArea().getAreaIndex();
			int LongLife = 0;
			if (img[0].target.getRescueInfo().longLife())
				LongLife = 1;

			dclog.trace("Sending assign msg-->" + mapEntry.getKey() + "--> target:" + img[0].target + " position:" + img[0].target.getPositionArea() + " longlife?" + img[0].target.getRescueInfo().longLife());
			agent.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_ASSIGN);
			agent.messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, mapEntry.getKey().getAmbIndex());
			agent.messageBlock.addData(MessageXmlConstant.DATA_ID, img[0].target.getID().getValue());
			agent.messageBlock.addData(MessageXmlConstant.DATA_AREA_INDEX, mlposIndex);
			agent.messageBlock.addData(MessageXmlConstant.DATA_LONG_LIFE, LongLife);
			agent.messageBlock.setResendOnNoise(false);
			agent.messages.add(agent.messageBlock);
		}
	}

	/*********************************************************************************************/
	/****************************** sendIgnoreMessages *******************************************/

	/**
	 * check to see if any ignore message should be sent, and send them.
	 * this method was created to solve a problem in which one or more ATs would spend lots of time on a target
	 * with no result ( the agent would die before the completion of rescue operations ).
	 * </br>Now, if a target needs <code>X</code> ATs but <code>Y</code> ATs are assigned to it ( <code>RescueInfo.getNowWorkingOnMe()</code> ) WHERE <b>X>Y</b> and no other ATs are availabe to be assigned to it, those ATs would ignore and abandon the target.
	 * </br><b>This method also manipulates <code>target</code>'s <code>RescueInfo</code></b>.
	 * 
	 * @author sinash
	 * @since Wednesday April 3rd, 2013
	 */

	public void sendIgnoreMessages() {

		if (targets.isEmpty()) {
			return;
		}

		for (Human target : targets) {
			if (target.getRescueInfo().getNowWorkingOnMe().size() > 0
					&& target.getRescueInfo().getATneedToBeRescued() > target.getRescueInfo().getNowWorkingOnMe().size()) {

				boolean isInNewAssigns = false;
				for (Map.Entry<AmbulanceTeam, ImaginationShot[]> mapEntry : assign.entrySet()) {

					ImaginationShot[] image = mapEntry.getValue();

					if (image[0] == null || image[0].target == null) {
						continue;
					}

					if (target.equals(image[0].target)) {
						isInNewAssigns = true;
					}

				}
				if (!isInNewAssigns) {

					target.getRescueInfo().setIgnoredUntil(IgnoreReason.WillDie, 1000);
					dclog.debug("SINA: target [" + target.getID() + "] ignored cause it's gonna die shortly. sending IgnoreMessages...");
					dclog.trace("Sending ignore msg-->" + target.getRescueInfo().getNowWorkingOnMe().toString() + "--> target:" + target + " position:" + target.getPositionArea() + " needs: " + target.getRescueInfo().getATneedToBeRescued() + " dt:" + target.getRescueInfo().getDeathTime());
					//tell myself if i'm center (rather than sending a message to myself, which is not logical btw :D )
					//age faghat center dare rush kar mikone (khodam centeram)
					if (!(agent instanceof AmbulanceTeamAgent
							&& target.getRescueInfo().getNowWorkingOnMe().contains(agent.getID().getValue())
							&& target.getRescueInfo().getNowWorkingOnMe().size() == 1)) {
						//send ignore message
						agent.messageBlock = new MessageBlock(MessageXmlConstant.HEADER_IGNORED_TARGET);
						agent.messageBlock.addData(MessageXmlConstant.DATA_ID, target.getID().getValue());
						agent.messageBlock.setResendOnNoise(true);
						agent.messages.add(agent.messageBlock);
					}
				}
			}
		}
	}

	/*********************************************************************************************/
	/************************************ chooseTarget *******************************************/
	public Human chooseTarget() {

		dclog.info("*********** chooseTarget ***************");

		if (targets.isEmpty()) {
			dclog.info("targets is empty!!!!!!!!!");
			return null;
		}

		dclog.info("Tagets are " + minCostTargets);

		while (!minCostTargets.isEmpty()) {
			Pair<Human, Float> HumanAndCost = minCostTargets.poll();
			if (HumanAndCost == null)
				continue;
			Human target = HumanAndCost.first();

			dclog.logln("Target  = " + target + "    cost=" + HumanAndCost.second());

			AmbulanceTeam bestAT = getBestAmbulanceFor(target);
			dclog.info("best AT for  = " + bestAT);

			if (bestAT == null)
				continue;

			if (bestAT.getID().equals(agent.getID()) && target != null)
				return target;

			ambulances.remove(bestAT);
		}
		dclog.info("*******************************************");
		return null;
	}

	/************************************************************************************************/
	/********************************** getBestAmbulanceFor *****************************************/
	private AmbulanceTeam getBestAmbulanceFor(Human first) {
		dclog.info("/////getBestAmbulanceFor////");
		ArrayList<AmbulanceTeam> ATs = new ArrayList<AmbulanceTeam>();
		ATs.addAll(ambulances);
		Collections.sort(ATs, new clusterComparitor(first));
		for (AmbulanceTeam AT : ATs) {

			if (AT.getWork() != null && AT.getWork().getTarget() != null && AmbulanceUtils.isReachableForAT(AT, true)) {
				dclog.info("skipped from AT =" + AT);
				continue;
			}

			return AT;
		}

		return null;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// Help state ///////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/*********************************************************************************************/
	/***************************************** reset ********************************************/
	public void reset() {
		targets.clear();
		minCostTargets.clear();
		lastCycleIwasHere = 0;
	}

	/*********************************************************************************************/
	/****************************** getAverageMoveToTarget ***************************************/
	public int getAverageMoveToTarget() {
		return AVERAGE_MOVE_TO_TARGET;
	}

	/*********************************************************************************************/
	/************************************ getMoveToTarget ***************************************/
	public int getMoveToTarget(Human target) {
		long moveWeight = agent.move.getWeightTo(target.getAreaPosition(), StandardMove.class) * MoveConstants.DIVISION_UNIT_FOR_GET;
		int moveToTarget = gu.getFoolMoveTime(moveWeight);

		return moveToTarget;
	}

	/*********************************************************************************************/
	/************************************** getAmbulances ****************************************/
	public ArrayList<AmbulanceTeam> getAmbulances() {
		findValidAmbulances();
		return ambulances;
	}

	/*********************************************************************************************/
	/********************************** ferociousFilterTargets ***********************************/
	public void ferociousFilterTargets() {
		if (targets.isEmpty()) {
			dclog.info("targets is empty");
			return;
		}

		if (agent.model().refuges().isEmpty())
			withOutRefugeMapTargets();

		removeAgentsTargetAfterMiddleOfSimulation();

		setNumberOfATsWhichTagetNeed();

		removeUnrescueableTargets();

		dclog.info("*******************************************");
	}

	/*********************************************************************************************/
	/**************************************** getTargets *****************************************/
	public ArrayList<Human> getTargets() {
		if (targets == null)
			targets = new ArrayList<Human>();
		return targets;
	}

	public boolean isXMLLogging() {
		return XMLLogging;
	}

	public void setXMLLogging(boolean xMLLogging) {
		XMLLogging = xMLLogging;
	}

}
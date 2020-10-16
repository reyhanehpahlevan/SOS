package sos.ambulance_v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;
import sos.ambulance_v2.base.AbstractAmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.decision.ATFeedbackFactory;
import sos.ambulance_v2.decision.AmbulanceCenterActivity;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.ambulance_v2.decision.AmbulanceDecisionMaker;
import sos.ambulance_v2.decision.LowCommunicationAmbulanceDecision;
import sos.ambulance_v2.decision.states.CenterAssignedTask;
import sos.ambulance_v2.decision.states.SelfTaskAssigningState;
import sos.ambulance_v2.tools.SimpleDeathTime;
import sos.base.SOSConstant;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.entities.VirtualCivilian;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.blocks.MessageBlock;
import sos.base.message.structure.channel.Channel;
import sos.base.move.Path;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.sosLogger.ATlogWriter;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.search_v2.agentSearch.AmbulanceSearch;
import sos.search_v2.tools.cluster.BuridBlockSearchCluster;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.decisionMaker.definitions.SOSInformationModel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;

/**
 * SOS ambulance team agent.
 */
public class AmbulanceTeamAgent extends AbstractAmbulanceTeamAgent {

	public Human target = null;
	public Human centerRecommendedTarget = null;
	public SaveHumanTask currentSaveHumanTask;
	public VirtualCivilian oldVirtualTarget;

	public boolean taskAssigner = false;
	public boolean isCenterAssigned = false;
	public AmbulanceDecision ambDecision;
	public LowCommunicationAmbulanceDecision lowComAmbDecision;
	public ArrayList<Human> centerAssignLists = new ArrayList<Human>();
	public long ambulanceThinkStart = 0;
	public String lastState = "";
	public SOSLoggerSystem abstractlog;
	public ATlogWriter xmlLog;
	private AmbulanceDecisionMaker ADK;
	SOSTask currentTask;
	private boolean isTargetsSet;
	public HashMap<ClusterData, Float> lowAndNocomClustersDis;
	
	/*** average move log ****/
	public int startMoveCycle = 0;
	public SOSLoggerSystem averageMoveLog;

	/******* stuck *****/
	public int stuckUntil=-1;
	
	
	@Override
	protected void preCompute() {
		super.preCompute();// it's better to call first!
		InitializeVariables();
		chooseTheAssigner();
		if (taskAssigner && messageSystem.type != Type.CenteralMiddleMan) {
			addCenterActivity(new AmbulanceCenterActivity(this));
		}
		if (ambDecision == null)
			ambDecision = new AmbulanceDecision(this);

		if (lowComAmbDecision == null)
			lowComAmbDecision = new LowCommunicationAmbulanceDecision(this);

		ADK = new AmbulanceDecisionMaker(this, new ATFeedbackFactory());

		for (Human a : model().agents()) {
			if (a.getAreaPosition() instanceof Building) {
				a.setBuriedness(20);
				a.setDamage(20);
				a.setHP(9000);
			}
		}

		SearchWorldModel<AmbulanceTeam> ss = new SearchWorldModel<AmbulanceTeam>(model());
		model().searchWorldModel = ss;
		newSearch = new AmbulanceSearch<AmbulanceTeam>(this, ss, new BuridBlockSearchCluster<AmbulanceTeam>(this, model().ambulanceTeams()));
		lowAndNocomClustersDis = new HashMap<ClusterData, Float>();
		if (messageSystem.type == Type.LowComunication || messageSystem.type == Type.NoComunication)
			calculateLowAndNocomClustersDis();
	}

	private void calculateLowAndNocomClustersDis() {

		ArrayList<ClusterData> clusters = new ArrayList<ClusterData>(model().searchWorldModel.getAllClusters());
		float max = Float.MIN_VALUE;

		final ClusterData myCluster = model().searchWorldModel.getClusterData();

		for (ClusterData cd : clusters) {
			float dis = (float) SOSGeometryTools.distance(myCluster.getX(), myCluster.getY(), cd.getX(), cd.getY());
			if (dis > max)
				max = dis;
		}
		for (ClusterData cd : clusters) {
			float dis = (float) SOSGeometryTools.distance(myCluster.getX(), myCluster.getY(), cd.getX(), cd.getY());
			lowAndNocomClustersDis.put(cd, (max - dis) / max);
		}
	}

	@Override
	protected void prepareForThink() {
		super.prepareForThink();
		//		handleHumanChange();
		if (time() + 5 > AmbulanceConstants.LONG_TIME)
			AmbulanceConstants.LONG_TIME += 10;

		//	if (messageSystem.type == Type.NoComunication || messageSystem.type == Type.LowComunication) {
		ambDecision.updateHumansInfo();

		//	}
		checkIgnored();
		if (!SOSConstant.IS_CHALLENGE_RUNNING) {
			printAllInfos();
		}
		try {
			currentTask = ADK.decidePreThink();
		} catch (SOSActionException e) {
			e.printStackTrace();
		}
	}

	protected void printAllInfos() {
		log().logln("AT INFOS : ");
		for (AmbulanceTeam at : model().ambulanceTeams()) {
			log().log(at + " --> " + at.getWork() + ", ");
		}
		log().logln("");
		log().logln("HUMAN INFOS : ");
		for (Human hu : model().humans()) {
			if (hu.isPositionDefined())
				log().log(hu + " --> " + hu.getRescueInfo() + ", ");
		}
		log().logln("");
	}

	protected void chooseTheAssigner() {
		if (messageSystem.type == Type.NoComunication || model().ambulanceTeams().size() == 1)
			return;
		if (messageSystem.type == Type.CenteralMiddleMan && messageSystem.getMine().isMiddleMan()) {
			this.taskAssigner = true;
		}

		if ((messageSystem.type == Type.NoMiddleMan || messageSystem.type == Type.WithMiddleMan) && me().getMessageWeightForSending() > 1) {
			this.taskAssigner = true;
		}
	}

	@Override
	protected void think() throws SOSActionException {
		super.think();

		xmlLog.startLog("Cycle", model().time() + "");
		addHumansInfoToXmlLog();
		// gets a task from decision maker
		if (currentTask == null)
			currentTask = ADK.decide();
		setTargetsSet(false);
		xmlLog.endLog("Cycle");

		if (currentTask != null) // there had been a task
			currentTask.execute(this);

		log().info("######## search ########");
		finishTasksState();
	}

	private void addHumansInfoToXmlLog() {

		xmlLog.startLog("State", "UpdateHumans");
		for (Human hm : model().humans()) {
			String infos = "";
			if (hm == null)
				continue;

			infos += xmlLog.addTag("Human", hm.getID().getValue() + "");
			infos += xmlLog.addTag("Type", hm.toString());

			if (!hm.isBuriednessDefined()) {
				infos += xmlLog.addTag("Buredness", "unDefined");
			}
			else if (!(hm instanceof Civilian) && hm.getBuriedness() == 0) {
				infos += xmlLog.addTag("Buredness", "0");
			}
			else if (!hm.isPositionDefined()) {
				infos += xmlLog.addTag("Position", "unDefined");
			}

			else if (hm.getPosition() instanceof Refuge) {
				infos += xmlLog.addTag("Position", "refuge");
			}
			else if (hm.getPosition() instanceof AmbulanceTeam) {
				infos += xmlLog.addTag("Position", "AT");
			}
			else if (hm instanceof Civilian && hm.getPosition() instanceof Road && hm.getDamage() == 0 && hm.getHP() == 10000) {
				infos += xmlLog.addTag("Damage", "0");
				infos += xmlLog.addTag("HP", "10000");
				infos += xmlLog.addTag("Position", hm.getPosition().toString());
			}
			else {
				int deathTime = hm.getRescueInfo().getDeathTime();
				int numberOfATNeed = ambDecision.getNumberOfATNeedToRescue(hm, deathTime);
				hm.getRescueInfo().setATneedToBeRescued(numberOfATNeed);

				infos += xmlLog.addTag("HP", hm.getHP() + "");
				infos += xmlLog.addTag("Damage", hm.getDamage() + "");
				infos += xmlLog.addTag("Time", hm.updatedtime() + "");
				infos += xmlLog.addTag("Buredness", hm.getBuriedness() + "");
				infos += xmlLog.addTag("DeadTime", hm.getRescueInfo().getDeathTime() + "");
				infos += xmlLog.addTag("Position", hm.getPosition() + "");
				infos += xmlLog.addTag("Ignore", hm.getRescueInfo().isIgnored() + "");
				infos += xmlLog.addTag("IgnoreUntil", hm.getRescueInfo().getIgnoredUntil() + "");
				infos += xmlLog.addTag("IgnoreReason", hm.getRescueInfo().getIgnoreReason() + "");
				infos += xmlLog.addTag("NeededAT", numberOfATNeed + "");
				infos += xmlLog.addTag("Refuge", hm.getRescueInfo().getBestRefuge() + "");
				infos += xmlLog.addTag("RefugeTime", hm.getRescueInfo().getTimeToRefuge() + "");
			}

			xmlLog.Info(infos);
		}
		xmlLog.endLog("State");
	}

	public void finishTasksState() throws SOSActionException {
		if (time() % 5 == 0)
			sendInfoMessage();

		lastState = " search ";
		search();

		lastState = " randomWalk ";
		randomWalk(true);

		lastState = " moveStandard ";
		if (model().refuges().isEmpty() && !model().centers().isEmpty())
			move.moveStandard(model().centers());
		else if (!model().refuges().isEmpty())
			move.moveStandard(model().refuges());
	}

	private void sendInfoMessage() {
		messageBlock = new MessageBlock(MessageXmlConstant.HEADER_AMBULANCE_INFO);
		messageBlock.addData(MessageXmlConstant.DATA_AMBULANCE_INDEX, me().getAmbIndex());
		messageBlock.addData(MessageXmlConstant.DATA_AT_STATE, 3);
		messageBlock.addData(MessageXmlConstant.DATA_ID, 0);
		messageBlock.addData(MessageXmlConstant.DATA_FINISH_TIME, time());
		messageBlock.setResendOnNoise(false);
		messages.add(messageBlock);
		sayMessages.add(messageBlock);

	}

	public void setCenterRecommendedTarget(Human target) {
		List<SOSIState> thinkStates = ADK.getThinkStates();
		for (SOSIState state : thinkStates) {
			if (state instanceof CenterAssignedTask) {
				((CenterAssignedTask) state).setCenterRecommendedTarget(target);
			}
		}
		thinkStates = ADK.getPreThinkStates();
		for (SOSIState<? extends SOSInformationModel> state : thinkStates) {
			if (state instanceof CenterAssignedTask) {
				((CenterAssignedTask) state).setCenterRecommendedTarget(target);
			}
		}
	}

	@Override
	protected void finalizeThink() {
		super.finalizeThink();

	}

	private void checkIgnored() {
		ArrayList<Human> lastCycleSensedHumans = getVisibleEntities(Human.class);
		for (Human hu : lastCycleSensedHumans)
			if (hu.isPositionDefined() && hu.getPosition() instanceof Road) {
				log().info("in checkIgnored function in AmbulanceTeamAgent class: " + hu);
				if (hu.getRescueInfo().isIgnored()
						&& (hu.getRescueInfo().getIgnoredUntil() >= time() + 5)
						&& hu.getRescueInfo().getIgnoredUntil() < 500) {
					log().info("it is still ignored");
					continue;
				}

				log().info(hu + "until = " + hu.getRescueInfo().getIgnoredUntil());
				hu.getRescueInfo().setNotIgnored();//TODO why???
			}
	}

	// *************************************************************************************************
	public boolean amIGoingToDieSoon() {
		if (me().getDamage() == 0)
			return false;
		if (SimpleDeathTime.getEasyLifeTime(me().getHP(), me().getDamage(), time()) < 4)
			return true;
		return false;
	}

	// *************************************************************************************************
	public boolean isItCriticalTogoRefuge() {
		if (me().getDamage() == 0)
			return false;
		if (me().getHP() == 0)
			return false;
		if (me().getRescueInfo().getInjuryDeathTime() - time() > 20 && me().getDamage() > 50)
			return true;
		if (me().getRescueInfo().getInjuryDeathTime() - time() < 20 && me().getDamage() > 25)
			return true;
		return false;
	}

	private HashMap<AmbulanceTeam, ArrayList<Human>> getSensedAmbulanceAndTargets() {
		ArrayList<Human> sensedHum = getVisibleEntities(Human.class);
		ArrayList<AmbulanceTeam> sensedAmb = getVisibleEntities(AmbulanceTeam.class);
		HashMap<AmbulanceTeam, ArrayList<Human>> amb_civs = new HashMap<AmbulanceTeam, ArrayList<Human>>();

		for (Human hum : sensedHum) {
			if (AmbulanceUtils.isValidToDecide(hum, log(), this)) {
				for (AmbulanceTeam ambulanceTeam : sensedAmb) {
					if (hum.getAreaPosition() instanceof Building && cansee(ambulanceTeam, ((Building) hum.getAreaPosition()).getSearchAreas())) {
						if (!amb_civs.containsKey(ambulanceTeam))
							amb_civs.put(ambulanceTeam, new ArrayList<Human>());
						amb_civs.get(ambulanceTeam).add(hum);
					}
				}
			}
		}
		return amb_civs;
	}

	private boolean cansee(AmbulanceTeam ambulanceTeam, ArrayList<ShapeInArea> searchAreas) {
		for (ShapeInArea shapeInArea : searchAreas) {
			if (shapeInArea.contains(ambulanceTeam.getPositionPoint().toGeomPoint()))
				return true;
		}
		return false;
	}

	//*************************************************************************************************
	/**
	 * @r@mik initializing humanoids properties
	 */
	private void InitializeVariables() {
		for (Human hm : model().humans()) {
			hm.getRescueInfo().updateProperties();
		}
		abstractlog = new SOSLoggerSystem(me(), "AmbulanceAbstract", true, OutputType.File);
		abstractlog.setFullLoggingLevel();
		sosLogger.addToAllLogType(abstractlog);

		xmlLog = new ATlogWriter(me(), "Agent/ATXmlLog", true, OutputType.File);

		averageMoveLog = new SOSLoggerSystem(null, "Agent/AverageMoveLog", true, OutputType.File);
		averageMoveLog.setFullLoggingLevel();

	}

	// *************************************************************************************************
	public int timeOfReachingMeToPlace(Pair<? extends Area, Point2D> to) {
		if (to == null || to.first() == null || to.second() == null)
			return AmbulanceDecision.AVERAGE_MOVE_TO_TARGET;
		ArrayList<Pair<? extends Area, Point2D>> b = new ArrayList<Pair<? extends Area, Point2D>>();
		b.add(to);
		Path pa = move.getPathToPoints(b, StandardMove.class);
		return ambDecision.gu.getFoolMoveTime(pa.getLenght());
	}

	// *************************************************************************************************
	public int myAgeFrom(ArrayList<AmbulanceTeam> ats) {
		int age = 0;
		for (Human ambulanceTeam : ats)
			if (ambulanceTeam.getID().getValue() < me().getID().getValue())
				age++;
		return age;
	}

	// *************************************************************************************************
	public boolean AmITheLoader1(Collection<Integer> ids) {
		int age = 0;
		for (Integer at : ids) {
			if (at < me().getID().getValue())
				age++;
		}
		if (age == 0)
			return true;
		return false;
	}

	public boolean AmITheLoader2(Collection<AmbulanceTeam> ats) {
		int age = 0;
		for (Human at : ats) {
			if (at.getID().getValue() < me().getID().getValue())
				age++;
		}
		if (age == 0)
			return true;
		return false;
	}

	// *************************************************************************************************
	public boolean ifContinueRescuing(int buried, int age) {
		return buried > age;
	}

	// *************************************************************************************************
	public int getMyBusyCycles(Human target, Pair<? extends Area, Point2D> place, ATstates currentState) {
		log().log("[Trace] calculating my busy time for " + target);
		int time = 0;
		switch (currentState) {
		case SEARCH:
			// for msg delay
			time++;
			log().log("\t" + currentState + " current time=" + time);
			break;
		case MOVE_TO_TARGET:
			if (this.target == null) {
				log().error("how target is null??");
				time++;
			} else {
				time += timeOfReachingMeToPlace(place);
				log().log("\t" + currentState + " time=" + time);
			}
		case RESCUE:
			if (this.target == null) {
				log().error("how target is null??");
				time++;
			} else {
				if (target.getBuriedness() != 0) {
					if (target.getRescueInfo().getNowWorkingOnMe().size() != 0) {
						time += target.getBuriedness() / target.getRescueInfo().getNowWorkingOnMe().size();
					} else {
						time += target.getBuriedness();
					}
					log().log("\t" + currentState + " time=" + time);
				}

				if (target instanceof Civilian) { // for load
					time++;
					log().log("\t" + currentState + "load time=" + time);
				}

			}

		case MOVE_TO_REFUGE:
			if (this.target == null) {
				log().error("how target is null??");
				time++;
			} else if (target instanceof Civilian) {
				boolean loader = AmITheLoader1(target.getRescueInfo().getNowWorkingOnMe());
				if (loader) {
					time += target.getRescueInfo().getTimeToRefuge();
					// for unload
					time++;
				}

				log().log("\t" + currentState + " time=" + time);
			}

		}
		return time;
	}

	//@r@mik**********************************************************************************start
	public ArrayList<Human> getLastCycleInMyPositionHumanoids() {
		ArrayList<Human> humans = new ArrayList<Human>();
		ArrayList<Human> lastCycleSensedHumans = getVisibleEntities(Human.class);
		for (Human hu : lastCycleSensedHumans) {
			if (hu.isPositionDefined() && hu.getPosition().getID().getValue() == location().getID().getValue()) {
				humans.add(hu);
			}
		}
		return humans;
	}

	public ArrayList<AmbulanceTeam> getLastCycleInMyPositionAmbulanceTeams() { // add self
		ArrayList<AmbulanceTeam> humans = new ArrayList<AmbulanceTeam>();
		ArrayList<Human> lastCycleSensedHumans = getVisibleEntities(Human.class);
		for (Human hu : lastCycleSensedHumans) {
			if (hu instanceof AmbulanceTeam && hu.getBuriedness() == 0 && hu.getHP() != 0) {
				if (hu.getPosition().getID().getValue() == location().getID().getValue()) {
					humans.add((AmbulanceTeam) hu);
				}
			}
		}
		return humans;
	}

	public ArrayList<Human> getLastCycleInMyPositionNeedHelpHumanoids() {// farz mikonim ke civiliane load shode need help nemibashad
		ArrayList<Human> humans = new ArrayList<Human>();
		ArrayList<Human> lastCycleSensedHumans = getVisibleEntities(Human.class);
		for (Human hu : lastCycleSensedHumans) {
			if (hu instanceof Civilian) {
				Civilian civ = (Civilian) hu;
				if (civ.getHP() != 0 /* && civ.damage()>0 */&& civ.isPositionDefined() && civ.getPosition().getID().getValue() == location().getID().getValue())
					humans.add(civ);
				if (civ.isPositionDefined() && !(civ.getPosition() instanceof Building) && civ.getDamage() == 0)
					humans.remove(civ);
			} else { //if it is agent
				if (hu.getHP() != 0 && hu.getBuriedness() != 0 && hu.getPosition().getID().getValue() == location().getID().getValue())
					humans.add(hu);
			}
		}
		return humans;
	}

	public ArrayList<Civilian> getLastCycleInMyPositionNeedHelpCivilians() {//farz mikonim ke civiliane load shode need help nemibashad
		log().info("getLastCycleInMyPositionNeedHelpCivilians");
		ArrayList<Civilian> humans = new ArrayList<Civilian>();
		ArrayList<Civilian> lastCycleSensedHumans = getVisibleEntities(Civilian.class);
		log().debug("lastCycleSensedHumans=" + lastCycleSensedHumans);
		for (Civilian hu : lastCycleSensedHumans) {
			Civilian civ = hu;
			if (civ.getHP() != 0
					&& civ.isPositionDefined()
					&& civ.getPosition().getID().getValue() == location().getID().getValue())
				humans.add(civ);
			if (civ.isPositionDefined() && !(civ.getPosition() instanceof Building) && civ.getDamage() == 0)
				humans.remove(civ);
		}
		log().debug("getLastCycleInMyPositionNeedHelpCivilians=" + humans);
		return humans;
	}

	@Override
	protected void thinkAfterExceptionOccured() throws SOSActionException {

	}

	public boolean haveTaskFromCenter() {
		return centerRecommendedTarget != null;
	}

	public boolean isCenterATaskAssigner() {
		return centerAssignLists.size() > 0;
	}

	// *************************************************************************************************
	/*
	 * Ali: Please keep it at the end!!!!(non-Javadoc)
	 */
	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		if (header.equalsIgnoreCase(HEADER_IGNORED_TARGET)) {
			log().info(header + " received for civilian " + data.get(DATA_ID));
			int id = data.get(DATA_ID);
			Human hu = (Human) me().model().getEntity(new EntityID(id));
			if (hu != null) {
				if (time() - hu.getLastSenseTime() < 10)
					return;
			}
		}
		super.hear(header, data, dynamicBitArray, sender, channel);
		ADK.hear(header, data, dynamicBitArray, sender, channel);//Added by Salim
		// ****************************************************************
		boolean civilianFlag = false;
		if (header.equalsIgnoreCase(HEADER_SENSED_CIVILIAN)) {
			Human h = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
			if (h == null) {
				civilianFlag = true;
			}
		}
		// ****************************************************************
		if (sender.equals(me()))
			return;//don't hear self message
		// ****************************************************************
		if (header.equalsIgnoreCase(HEADER_SENSED_CIVILIAN)) {

			if (sender.equals(me()))
				return;

			Human h = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
			if (h != null) {
				if (time() - h.getLastSenseTime() < 3)
					return;
				if (civilianFlag) {
					ambDecision.calculateRefugeInformation_old(h);
				}
			}
			if (h instanceof Civilian && h.getPosition() instanceof Road) {
				log().info("HEADER_SENSED_CIVILIAN in hear function in AmbulanceTeamAgent class");
				h.getRescueInfo().setNotIgnored();
			}
			//		ambDecision.updateHuman(h);
			log().logln("msg -> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);
		}
		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_SENSED_AGENT)) {
			if (sender.equals(me()))
				return;
			Human hu = model().agents().get(data.get(DATA_AGENT_INDEX));
			if (time() - hu.getLastSenseTime() < 3)
				return;
			ambDecision.updateHuman(hu);
		}
		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_STATUS)) { // usage only in say
			if (sender.equals(me()))
				return;
			if (time() - data.get(DATA_TIME) > 2)
				return;
			ATstates state = AmbulanceUtils.convertStateIndexToState(data.get(DATA_AT_STATE));
			log().logln("ambulance status --> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);

			AmbulanceTeam at = model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			if (at.getAmbIndex() == me().getAmbIndex())
				return;
			logToAmbDecision("ambulance status --> Header:" + header + " Data:" + data + " state=" + state + " From:" + sender);
			if (state != null)
				at.getWork().setCurrentState(state);
			if (state == ATstates.SEARCH) {
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(time());
			}
			if (data.get(DATA_ID) == 0)
				return;
			Human hmn = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
			Area position = model().areas().get(data.get(DATA_AREA_INDEX));
			if (hmn == null && position != null) {
				hmn = updater.newHuman(data.get(DATA_ID), time() - messageSystem.getNormalMessageDelay(), 9000, 20, 20, position);
				ambDecision.updateHuman(hmn);
			}
			if (position != null) {
				if (state == ATstates.RESCUE)
					hmn.setPosition(position.getID(), position.getX(), position.getY());
				at.setPosition(position.getID(), position.getX(), position.getY());
			}
			at.getWork().setTarget(hmn, null);
			//			hmn.getRescueInfo().addAT(at);
			at.getWork().setNeedHelpInSay(data.get(DATA_NEED_HELP) == 1);
		}
		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_INFO)) {
			ambDecision.dclog.info("@@@@@@@@@@@@@ in hear (HEADER_AMBULANCE_INFO) @@@@@@@@@@@@@@@");

			if (sender.equals(me()))
				return;
			ATstates state = AmbulanceUtils.convertStateIndexToState(data.get(DATA_AT_STATE));
			log().logln("Ambulance Info --> Header:" + header + " Data:" + data + " From:" + sender + " state=" + state);

			AmbulanceTeam at = model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			if (at.getAmbIndex() == me().getAmbIndex())
				return;

			logToAmbDecision("Ambulance Info --> Header:" + header + " Data:" + data + " state=" + state + " From:" + sender);
			if (state != null)
				at.getWork().setCurrentState(state);

			if (state != ATstates.SEARCH && data.get(DATA_ID) != 0) { //data.get(DATA_ID)==0 --> means no target
				Human hmn = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
				if (hmn == null) {
					hmn = updater.newHuman(data.get(DATA_ID), time() - messageSystem.getNormalMessageDelay(), 9000, 20, 20, null);
					ambDecision.updateHuman(hmn);
				}

				ambDecision.dclog.info("AT = " + at + "    target = " + hmn);
				hmn.getRescueInfo().addAT(at);
				at.getWork().setTarget(hmn, null);
				at.getWork().setNextFreeTime(data.get(DATA_FINISH_TIME));
				ambDecision.dclog.info("target set= " + at.getWork().getTarget());

				//  TODO it think it's better to comment because of some bugs:
				if (hmn.getRescueInfo().getATneedToBeRescued() - hmn.getRescueInfo().getNowWorkingOnMe().size() <= 0) {
					ambDecision.dclog.info("has more");
					if (hmn instanceof Civilian && loadingInjured() != null && hmn.getID().getValue() == loadingInjured().getID().getValue()) {
						ambDecision.dclog.info("nothing");
						//age khodesh load karde bood==>hichi
					} else if (underMissionTarget() != null && hmn.getID().getValue() == underMissionTarget().getID().getValue() && me().getAmbIndex() < at.getAmbIndex()) {
						//agar in civilian ro mikhasti nejat bedi! va indexet kamtar az index e sender e payam bood
						log().trace("NowWorkingOnMe:" + hmn.getRescueInfo().getNowWorkingOnMe());
						log().trace("AT need To Be Rescue:" + hmn.getRescueInfo().getATneedToBeRescued());
						if (!hmn.getRescueInfo().getNowWorkingOnMe().contains(me().getID().getValue())) {
							//agar man roosh kar nemikardam===>ignoresh kon be khatere inke have enoough AT hast
							log().trace(hmn + " has been ignored(have enough at) till" + (time() + 15));
							hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.HaveEnoughAT, time() + 15);//XXX check
						}
					} else if (hmn != underMissionTarget()) {
						ambDecision.dclog.info("not my mission");
						//agar loadesh nemikardi va mokhalefe target e khodet bood
						if (hmn.getRescueInfo().getIgnoreReason() != IgnoreReason.TargetLoadedByAnotherAT)
							hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.IsNotMyMissionTarget, time() + 15);
					}
					else {
						ambDecision.dclog.info("else");
					}
				}
				if (state == ATstates.MOVE_TO_REFUGE && hmn != null) {
					if (hmn.getRescueInfo().getBestRefuge() != null)
						hmn.setPosition(hmn.getRescueInfo().getBestRefuge().getID());
					else if (model().refuges().size() > 0)
						hmn.setPosition(model().refuges().get(0).getID());
					else {
						Area road = AmbulanceUtils.getRoadNeighbour(hmn.getAreaPosition());//TODO set position null!!!
						if (road != null)
							hmn.setPosition(road.getID());
					}
				}
			} else {
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(time());
			}

			ambDecision.dclog.info("target set= " + at.getWork().getTarget());
			ambDecision.dclog.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		}
		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_TASK_ACK)) {
			if (sender.equals(me()))
				return;
			log().logln("taskAck --> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);
			logToAmbDecision("taskAck --> Header:" + header + " Data:" + data + " From:" + sender);
			AmbulanceTeam at = model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			if (at.getAmbIndex() == me().getAmbIndex())
				return;
			Human hmn = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
			if (hmn == null) {
				hmn = updater.newHuman(data.get(DATA_ID), time() - messageSystem.getNormalMessageDelay(), 9000, 20, 20, null);
				ambDecision.updateHuman(hmn);
			}
			int meaning = data.get(DATA_ACK_TYPE); // 0-->accepted 1-->finish 2-->rejected
			switch (meaning) {
			case 0:
				hmn.getRescueInfo().addAT(at);
				at.getWork().setCurrentState(ATstates.MOVE_TO_TARGET);
				at.getWork().setTarget(hmn, null);//added in 2010

				//sinash - 2013
				//TODO alan farghe freeTime e loader ro ba baghie yeki dar nazar gereftim. bayad dorost beshe!!

				int nextFreeTime = time();
				nextFreeTime += (int) Math.ceil(hmn.getBuriedness() / (float) hmn.getRescueInfo().getATneedToBeRescued());
				nextFreeTime += (2 + hmn.getRescueInfo().getTimeToRefuge());//1 for load 1 for unload
				at.getWork().setNextFreeTime(nextFreeTime);

				//end sinash

				//  TODO UNKONWN
				if (hmn.getRescueInfo().getATneedToBeRescued() - hmn.getRescueInfo().getNowWorkingOnMe().size() <= 0) {
					if (hmn instanceof Civilian && loadingInjured() != null && hmn.getID().getValue() == loadingInjured().getID().getValue()) {
						//age khodesh load karde bood==>hichi
					} else if (underMissionTarget() != null && hmn.getID().getValue() == underMissionTarget().getID().getValue() && me().getAmbIndex() < at.getAmbIndex()) {
						//agar in civilian ro mikhasti nejat bedi! va indexet kamtar az index e sender e payam bood
						log().trace("NowWorkingOnMe:" + hmn.getRescueInfo().getNowWorkingOnMe());
						log().trace("AT need To Be Rescue:" + hmn.getRescueInfo().getATneedToBeRescued());
						if (!hmn.getRescueInfo().getNowWorkingOnMe().contains(me().getID().getValue())) {
							//agar man roosh kar nemikardam===>ignoresh kon be khatere inke have enoough AT hast
							log().trace(hmn + " has been ignored(have enough at) till" + (time() + 15));
							hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.ShouldCheck_Unknown, time() + 10);//XXX check
						}

					} else if (underMissionTarget() == null || hmn.getID().getValue() != underMissionTarget().getID().getValue()) {
						//agar loadesh nemikardi va mokhalefe target e khodet bood
						if (hmn.getRescueInfo().getIgnoreReason() != IgnoreReason.TargetLoadedByAnotherAT)
							hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.IsNotMyMissionTarget, time() + 15);
					}
				}

				checkValidityOfMyOldTask(hmn);
				break;
			case 1:
				at.getWork().setTarget(null, null);
				hmn.getRescueInfo().removeAT(at);
				if (hmn instanceof Civilian && hmn != loadingInjured()) {
					hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.FinishMessageReceived, time() + 10);

					if (messageSystem.type == Type.NoComunication) {
						hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.NoComunicationAndFinished, 260);
					}
				}
				break;
			case 2:
				if (centerAssignLists.contains(hmn))
					centerAssignLists.remove(hmn);
				hmn.getRescueInfo().removeAT(at);
				at.getWork().setTarget(null, null);
				if (!(hmn.getRescueInfo().getIgnoreReason() == IgnoreReason.IgnoredTargetMessageReceived || hmn.getRescueInfo().getIgnoreReason() == IgnoreReason.WillDie)) {
					log().info("HEADER_AMBULANCE_TASK_ACK in hear function in AmbulanceTeamAgent class");
					hmn.getRescueInfo().setNotIgnored();
				}
				break;
			default:
			}

		}
		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_ASSIGN)) {
			log().logln("assign message --> Header:" + header + " Data[" + data + "] From:" + sender + " Channel:" + channel);
			logToAmbDecision("assign message --> Header:" + header + " Data[" + data + "] From:" + sender);
			if (taskAssigner)
				return;

			AmbulanceTeam at = model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			Human hmn = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
			Area position = model().areas().get(data.get(DATA_AREA_INDEX));
			if (hmn == null && position != null) {
				hmn = updater.newHuman(data.get(DATA_ID), time() - messageSystem.getNormalMessageDelay(), 9000, 20, 20, position);
				ambDecision.updateHuman(hmn);
			}
			if (!hmn.isPositionDefined() || (!hmn.getPosition().equals(position) && hmn.updatedtime() - (time() - messageSystem.getNormalMessageDelay()) < 0)) {
				hmn.setPosition(position.getID(), position.getX(), position.getY());
			}
			if (at.getAmbIndex() == me().getAmbIndex() && hmn != null) {
				this.centerRecommendedTarget = hmn;
				hmn.getRescueInfo().setLongLife(data.get(DATA_LONG_LIFE) == 1);
			}
			if (hmn != null) {
				centerAssignLists.add(hmn);
				hmn.getRescueInfo().setLongLife(data.get(DATA_LONG_LIFE) == 1);
			}
			if (hmn.getLastSenseTime() < time() - 2) {//Added by Ali
				log().info("HEADER_AMBULANCE_ASSIGN in hear function in AmbulanceTeamAgent class");
				hmn.getRescueInfo().setNotIgnored();
			}
		}
	}

	private void checkValidityOfMyOldTask(Human hmn) {

		Human myTarget = me().getWork().getTarget();
		SOSIState myState = me().getWork().getState();
		if (myState == null || myTarget == null || hmn == null)
			return;

		if (messageSystem.type == Type.LowComunication || messageSystem.type == Type.NoComunication)
			return;
		if (!hmn.getID().equals(myTarget.getID()) || !(myState instanceof SelfTaskAssigningState))
			return;
		if (isLoadingInjured())
			return;
		int deathTime = myTarget.getRescueInfo().getDeathTime();
		int need = ambDecision.getNumberOfATNeedToRescue(hmn, deathTime);
		int current = myTarget.getRescueInfo().getNowWorkingOnMe().size();
		if (current <= need)
			return;
		AmbulanceUtils.sendRejectMessage(myTarget, this);
		myTarget.getRescueInfo().setIgnoredUntil(IgnoreReason.IwasExtraAT, time() + 10);
		me().getWork().setTarget(null, null);
		me().getWork().setNextFreeTime(0);
	}

	private void checkValidityOfMyOldTaskLowNoCom(Human hmn) {

		if (messageSystem.type == Type.LowComunication || messageSystem.type == Type.NoComunication) {

			Human myTarget = me().getWork().getTarget();
			SOSIState myState = me().getWork().getState();
			if (myState == null || myTarget == null || hmn == null)
				return;

			if (!hmn.getID().equals(myTarget.getID()))
				return;
			if (isLoadingInjured())
				return;
			int deathTime = myTarget.getRescueInfo().getDeathTime();
			int need = ambDecision.getNumberOfATNeedToRescue(hmn, deathTime);
			int current = myTarget.getRescueInfo().getNowWorkingOnMe().size();
			if (current <= (need + 1))
				return;

			lowComAmbDecision.findValidTargets();
			lowComAmbDecision.filterTargets();
			setTargetsSet(true);
			if (lowComAmbDecision.Virtualtargets.size() < 15)
				return;

			AmbulanceUtils.sendRejectMessage(myTarget, this);
			myTarget.getRescueInfo().setIgnoredUntil(IgnoreReason.IwasExtraAT, time() + 10);
			me().getWork().setTarget(null, null);
			me().getWork().setNextFreeTime(0);
		}
	}

	public void logToAmbDecision(String s) {
		ambDecision.dclog.logln("[Agent] " + s);
		abstractlog.logln("[Agent] " + s);
	}

	public boolean isTargetsSet() {
		return isTargetsSet;
	}

	public void setTargetsSet(boolean isTargetsSet) {
		this.isTargetsSet = isTargetsSet;
	}
}
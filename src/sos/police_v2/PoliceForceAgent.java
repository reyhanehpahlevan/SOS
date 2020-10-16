package sos.police_v2;

import java.util.ArrayList;
import java.util.HashMap;

import rescuecore2.worldmodel.EntityID;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.SOSActionException;
import sos.base.util.TimeNamayangar;
import sos.base.util.mapRecognition.MapRecognition.MapName;
import sos.police_v2.base.AbstractPoliceForceAgent;
import sos.police_v2.base.clearablePointToReachable.GeoClearPointReachablity;
import sos.police_v2.base.clearablePointToReachable.GeoDegreeClearPointToReachable;
import sos.police_v2.state.DummyPoliceState;
import sos.police_v2.state.LowComOpenCivilianState;
import sos.police_v2.state.NoCommunicationState;
import sos.police_v2.state.OpenCivilianState;
import sos.police_v2.state.OpenHealtyCivilianState;
import sos.police_v2.state.OpenHighWaysState2;
import sos.police_v2.state.OpenUnupdatedCivilianState;
import sos.police_v2.state.OtherAgentStockState;
import sos.police_v2.state.PoliceAbstractState;
import sos.police_v2.state.SearchState;
import sos.police_v2.state.TestState;
import sos.police_v2.state.UpdateClusterFireState;
import sos.police_v2.state.UpdateFireState;
import sos.police_v2.state.openIgnoredCivilian;
import sos.police_v2.state.afterShock.AfterShockClusterConnector;
import sos.police_v2.state.afterShock.AfterShockCriticalAmbulance;
import sos.police_v2.state.afterShock.AfterShockReachabler;
import sos.police_v2.state.intrupt.CheckProbabilityFire;
import sos.police_v2.state.intrupt.ClearEntrances;
import sos.police_v2.state.intrupt.ClearUnPlanedBlockadeIntruptState;
import sos.police_v2.state.intrupt.DamagedState;
import sos.police_v2.state.intrupt.FastOpeningAgentFromBuilding;
import sos.police_v2.state.intrupt.HeavyStockedHandlerState;
import sos.police_v2.state.intrupt.LockInBlockadeState;
import sos.police_v2.state.intrupt.LowBuriednessCivilianOpeningState;
import sos.police_v2.state.intrupt.NearCivilianOpenerState;
import sos.police_v2.state.intrupt.PoliceAbstractIntruptState;
import sos.police_v2.state.intrupt.ReachableHumanIntruptState;
import sos.police_v2.state.intrupt.SmallFireGardClearingInterupt;
import sos.police_v2.state.intrupt.StockHandlerState;
import sos.police_v2.state.preCompute.MSTState;
import sos.police_v2.state.preCompute.PrecomputeState;

/**
 * SOS police force agent.
 */
public class PoliceForceAgent extends AbstractPoliceForceAgent {
	private ArrayList<PoliceAbstractState> states = new ArrayList<PoliceAbstractState>();
	private ArrayList<PoliceAbstractIntruptState> intruptStates = new ArrayList<PoliceAbstractIntruptState>();
	private HashMap<AmbulanceTeam, Human> ambulanceTeamTarget;
	private PoliceAbstractState lastCycle;

	@Override
	protected void preCompute() {
		super.preCompute();// it's better to call first!
		PoliceConstants.STANDARD_OF_MAP = (int) model().getBounds().getWidth() / 200000;
		log.debug("Standard of Map:", PoliceConstants.STANDARD_OF_MAP);

		TimeNamayangar tm = new TimeNamayangar("Police PreCompute");
		model().makeIslands();
		model().makeHighways();
		clearableToPoint = new GeoClearPointReachablity(this);
		degreeClearableToPoint = new GeoDegreeClearPointToReachable(this);
		makeStates();
		makeIntruptsStates();
		doStatePrecomputes();
		sosLogger.base.consoleInfo("PolicePreCompute " + tm.stop());
		ambulanceTeamTarget = new HashMap<AmbulanceTeam, Human>();
		clearWidth = getConfig().getIntValue("clear.repair.rad", 1250);
	}

	private void makeStates() {
		//		states.add(new FireSearchState(this));//it is the most important state
		long start = System.currentTimeMillis();
		if (messageSystem.type == Type.NoComunication) {
			states.add(new NoCommunicationState(this));
			//			states.add(new SendDataOfNewFireZoneInMyClusterToFireAgent(this));
		}
		if (messageSystem.type != Type.NoComunication) {
			states.add(new AfterShockCriticalAmbulance(this));
		}
		states.add(new AfterShockReachabler(this));
		states.add(new AfterShockClusterConnector(this));
		if (model().policeForces().size() > 2) {
			states.add(new PrecomputeState(this));
			states.add(new MSTState(this));
		}
		//				states.add(new OpenHydrantState(this));
		states.add(new openIgnoredCivilian(this));
		states.add(new OpenHealtyCivilianState(this));

		states.add(new OtherAgentStockState(this));
		if (getMapInfo().getRealMapName() == MapName.Kobe || getMapInfo().getRealMapName() == MapName.VC)
			states.add(new OpenHighWaysState2(this));
		//			states.add(new OpenHighwayesState(this));
		states.add(new UpdateFireState(this));
		states.add(new UpdateClusterFireState(this));
		if (messageSystem.type == Type.LowComunication)
			states.add(new LowComOpenCivilianState(this));
		states.add(new OpenCivilianState(this));
		states.add(new OpenUnupdatedCivilianState(this));
		states.add(new SearchState(this));
		states.add(new TestState(this));
		states.add(new DummyPoliceState(this));
		long time = (System.currentTimeMillis() - start);
		log.info("police state making got:" + time + "ms", time > 5);
	}

	private void makeIntruptsStates() {
		long start = System.currentTimeMillis();
		intruptStates.add(new LockInBlockadeState(this));
		intruptStates.add(new StockHandlerState(this));
		intruptStates.add(new HeavyStockedHandlerState(this));
		intruptStates.add(new DamagedState(this));
		//		intruptStates.add(new UpdateFireIntruptState(this));
		intruptStates.add(new CheckProbabilityFire(this));//because it is more important than opening an agent. because when we open fire other reachable agent can extenguish fire.
		intruptStates.add(new SmallFireGardClearingInterupt(this));
		//		intruptStates.add(new ConnectFireBrigadeToFire(this));
		intruptStates.add(new ReachableHumanIntruptState(this));
		intruptStates.add(new FastOpeningAgentFromBuilding(this));
		intruptStates.add(new NearCivilianOpenerState(this));
		intruptStates.add(new LowBuriednessCivilianOpeningState(this));
		intruptStates.add(new ClearUnPlanedBlockadeIntruptState(this));
		intruptStates.add(new ClearEntrances(this));
		long time = (System.currentTimeMillis() - start);
		log.info("Police Interrupt state making  got:" + time + "ms", time > 5);
	}

	private void doStatePrecomputes() {
		ArrayList<PoliceAbstractState> allStates = new ArrayList<PoliceAbstractState>(intruptStates);
		allStates.addAll(states);
		for (PoliceAbstractState state : allStates) {
			long start = System.currentTimeMillis();
			state.precompute();
			long time = (System.currentTimeMillis() - start);
			boolean consoleLog = time > 10 || !(state instanceof PoliceAbstractIntruptState);
			log.info(state + " got:" + time + "ms", consoleLog);
		}

	}

	@Override
	protected void prepareForThink() {
		super.prepareForThink();
		//		if (getState(SendDataOfNewFireZoneInMyClusterToFireAgent.class) != null)
		//			if (messageSystem.type == Type.NoComunication && !getState(SendDataOfNewFireZoneInMyClusterToFireAgent.class).isStart) {
		//				checkInClusterFireZone();
		//			}
	}

	//	private void checkInClusterFireZone() {
	//		log.info("Chcek is any new fire zone in my clsuter");
	//		log.debug("model().getFireSites()=" + model().getFireSites());
	//		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfs : model().getFireSites())
	//		{
	//			SOSEstimatedFireZone fs = pfs.second();
	//			log.trace("sos estimated fire zone: " + fs + " isNewFireAndNotReported?" + !fs.isReported());
	//			if ((!fs.isReported()) && isInMyCluster(fs)) {
	//				log.debug(fs + " is not reported and in my cluster");
	//				getState(SendDataOfNewFireZoneInMyClusterToFireAgent.class).isStart = true;
	//				getState(SendDataOfNewFireZoneInMyClusterToFireAgent.class).fireZone = fs;
	//				getState(SendDataOfNewFireZoneInMyClusterToFireAgent.class).startTime = time();
	//				sosLogger.noComunication.info("exist unreported fire site " + fs);
	//
	//			}
	//		}
	//		log.info("No new fireZone in My cluster");
	//	}

	private boolean isInMyCluster(SOSEstimatedFireZone fs) {
		for (Building b : fs.getAllBuildings()) {
			if (getMyClusterData().getBuildings().contains(b))
				return true;
		}

		return false;
	}

	@Override
	protected void think() throws SOSActionException {
		//		sampleMove();
		super.think();
		//				goToCenterForNoCom();
		lastCycleState = lastState;
		doIntrupt();
		doStateAct();
		doBugState();
	}

	//	public void sampleMove(){
	//		Area area = (Area)model().getEntity(new EntityID(51745));
	//		long cost = SearchUtils.getWeightTo((Road) area, this);
	//		System.out.println("t"+time()+"----dist:"+ me().getTravelDistance()+" d2"+me().getLastCycleTraveledDistance()+" rt:"+(60-move.getMovingTimeFrom(cost))+" current cost:"+cost);
	//		move.movePolice(area);
	//	}

	private void doBugState() throws SOSActionException {
		log.warn("No Act done till here!!! WHY?");
		try {
			getState(SearchState.class).act();
		} catch (SOSActionException e1) {
			throw e1;
		} catch (Exception e) {
			sosLogger.agent.fatal(e);
		}
		try {
			getState(DummyPoliceState.class).act();
		} catch (SOSActionException e1) {
			throw e1;
		} catch (Exception e) {
			sosLogger.agent.fatal(e);
		}
		log.warn("NO ACT");
		problemRest("Exception occured");
	}

	private void doStateAct() throws SOSActionException {
		TimeNamayangar tm = new TimeNamayangar();
		for (PoliceAbstractState lastState : states) {
			log.info("acting as " + lastState + "==================================================================={{{{{{{");
			try {
				tm.reset();
				tm.start();
				super.lastState = lastState + "";
				lastState.act();
				tm.finish();
				sosLogger.act.debug(lastState + " do nothing but take " + tm);
			} catch (SOSActionException e) {
				abstractStateLogger.logln(time() + ":" + lastState.getClass().getSimpleName() + "\t\t\t : action=" + e.getMessage() + "\t\t\t" + tm);
				sosLogger.act.debug(lastState + "doing " + e.getMessage() + " take " + tm);
				lastCycle = lastState;
				throw e;
			} catch (Exception e) {
				sosLogger.error("State:" + lastState + " failed...");
				sosLogger.error(e);
			}
			log.info("state" + lastState + " finished}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}");
		}
	}

	private void doIntrupt() throws SOSActionException {
		for (PoliceAbstractIntruptState intruptState : intruptStates) {
			lastState = intruptState.getClass().getSimpleName();
			try {
				log.trace(intruptState + " checking for can intrupt?");
				if (intruptState.canMakeIntrupt()) {
					log.trace(intruptState + " can make intrupt");
					intruptState.act();
				}
			} catch (SOSActionException e) {
				sosLogger.act.debug(lastState + "doing " + e.getMessage());
				abstractStateLogger.logln(time() + ":" + lastState + "\t\t\t : action=" + e.getMessage() + "\t\t\t");
				lastCycle = intruptState;
				throw e;
			} catch (Exception e) {
				sosLogger.error("State:" + lastState + " failed...");
				sosLogger.error(e);
			}
		}
	}

	@Override
	protected void thinkAfterExceptionOccured() throws SOSActionException {
		log.warn("An unhandeled exception occured.... act as bug state");
		doBugState();
	}

	@Override
	protected void finalizeThink() {
		super.finalizeThink();
	}

	@SuppressWarnings("unchecked")
	public <T extends PoliceAbstractState> T getState(Class<T> stateClass) {
		for (PoliceAbstractState state : states) {
			if (stateClass.isInstance(state))
				return (T) state;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends PoliceAbstractState> T getInteruptState(Class<T> stateClass) {
		for (PoliceAbstractIntruptState state : intruptStates) {
			if (stateClass.isInstance(state))
				return (T) state;
		}
		return null;
	}

	public HashMap<AmbulanceTeam, Human> getAmbulanceTeamTarget() {
		return ambulanceTeamTarget;
	}

	public PoliceAbstractState getLastCycleState() {
		return lastCycle;
	}

	/*
	 * Ali: Please keep it at the end!!!!(non-Javadoc)
	 */
	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		super.hear(header, data, dynamicBitArray, sender, channel);
		for (PoliceAbstractState state : states) {
			state.hear(header, data, dynamicBitArray, sender, channel);
		}

		if (header.equalsIgnoreCase(HEADER_AMBULANCE_INFO)) {
			ATstates state = AmbulanceUtils.convertStateIndexToState(data.get(DATA_AT_STATE));
			AmbulanceTeam at = model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			if (state != ATstates.SEARCH && data.get(DATA_ID) != 0) {
				Human hmn = (Human) model().getEntity(new EntityID(data.get(DATA_ID)));
				getAmbulanceTeamTarget().put(at, hmn);
			}
		}

	}

}
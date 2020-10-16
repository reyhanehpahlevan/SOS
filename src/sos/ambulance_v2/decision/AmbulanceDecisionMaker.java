package sos.ambulance_v2.decision;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.decision.states.CenterAssignedTask;
import sos.ambulance_v2.decision.states.CivilianSearchState;
import sos.ambulance_v2.decision.states.DeadState;
import sos.ambulance_v2.decision.states.FireSearchState;
import sos.ambulance_v2.decision.states.HelpState;
import sos.ambulance_v2.decision.states.IAmHurtState;
import sos.ambulance_v2.decision.states.IAmStuckState;
import sos.ambulance_v2.decision.states.LockInBlockadeState;
import sos.ambulance_v2.decision.states.LowComAfterCivilianSearchState;
import sos.ambulance_v2.decision.states.LowCommunicationStartegyState;
import sos.ambulance_v2.decision.states.NoComState;
import sos.ambulance_v2.decision.states.RunAwayFromGassStationState;
import sos.ambulance_v2.decision.states.SelfTaskAssigningState;
import sos.ambulance_v2.decision.states.SpecialSelfTaskAssigningState;
import sos.ambulance_v2.decision.states.changeToBetterTarget;
import sos.base.util.SOSActionException;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.SOSStateBasedDecisionMaker;
import sos.tools.decisionMaker.implementations.stateBased.StateFeedbackFactory;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/**
 * @author Reyhaneh
 */

public class AmbulanceDecisionMaker extends SOSStateBasedDecisionMaker {

	private final AmbulanceTeamAgent agent;
	private AmbulanceDecision ambDecision;

	public AmbulanceDecisionMaker(AmbulanceTeamAgent agent, StateFeedbackFactory feedbackFactory) {
		super(agent, feedbackFactory, AmbulanceInformationModel.class);
		this.agent = agent;
	}

	@Override
	public void initiateStates() {
		getThinkStates().add(new DeadState(infoModel));
		getThinkStates().add(new IAmHurtState(getInfoModel()));
		getThinkStates().add(new IAmStuckState(getInfoModel()));
		getThinkStates().add(new LockInBlockadeState(getInfoModel()));
		getThinkStates().add(new RunAwayFromGassStationState(getInfoModel()));
		getThinkStates().add(new changeToBetterTarget(getInfoModel()));
		getThinkStates().add(new CenterAssignedTask(getInfoModel()));
		getThinkStates().add(new SpecialSelfTaskAssigningState(getInfoModel()));
		getThinkStates().add(new LowCommunicationStartegyState(getInfoModel()));
		getThinkStates().add(new SelfTaskAssigningState(getInfoModel()));
		getThinkStates().add(new FireSearchState(getInfoModel()));
		getThinkStates().add(new NoComState(getInfoModel()));
		getThinkStates().add(new CivilianSearchState(getInfoModel()));
		getThinkStates().add(new HelpState(getInfoModel()));
		getThinkStates().add(new LowComAfterCivilianSearchState(getInfoModel()));
		//		getThinkStates().add(new ferociousState(getInfoModel()));
	}

	public AmbulanceInformationModel getInfoModel() {
		return (AmbulanceInformationModel) infoModel;
	}

	@Override
	public SOSTask<?> decide() throws SOSActionException {
		return ATrunStates(thinkStates);
	}

	public SOSTask<?> ATrunStates(List<SOSIState> states) throws SOSActionException {
		SOSEventPool eventPool = new SOSEventPool();
		SOSTask<?> result = null;
		for (SOSIState state : states) {
			state.handleEvents(eventPool.removeEvents(state.getClass()));
			if (result != null) {
				state.overTaken();
				continue;
			}

			long t1 = System.currentTimeMillis();
			try {
				agent.xmlLog.startLog("State", state.getName());
				result = state.decide(eventPool);
				agent.xmlLog.endLog("State");
			} catch (Exception e) {
				if (e instanceof SOSActionException){			
					throw (SOSActionException) e;
				}
				infoModel.getLog().error(e);
			}
			long time = System.currentTimeMillis() - t1;
			state.giveFeedbacks(feedbackFactory.createFeedbacks(infoModel.getAgent(), state, result, time));
			if (result != null){
				state.taken();
				abstractStateLogger.logln(this.infoModel.getTime()+":"+state.getClass().getSimpleName()+"\t\t\t"+(System.currentTimeMillis() - t1)+"ms");
			}
			infoModel.getAgent().sosLogger.act.info(state + " : time " + time);
		}
		for (SOSIState state : states) {
			state.handleEvents(eventPool.removeEvents(state.getClass()));
		}
		//		}
		return result;
	}
}
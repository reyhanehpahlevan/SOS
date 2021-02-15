package sos.tools.decisionMaker.implementations.stateBased;

import java.util.ArrayList;
import java.util.List;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.tools.decisionMaker.definitions.SOSAbstractDecisionMaker;
import sos.tools.decisionMaker.definitions.SOSInformationModel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/**
 * State Based decision maker is used in state based strategies.<br>
 * It provides functionalities such as feedback system for states
 * 
 * @author Salim
 */
public abstract class SOSStateBasedDecisionMaker<E extends SOSInformationModel> extends SOSAbstractDecisionMaker<E> {
	protected List<SOSIState> thinkStates;
	private List<SOSIState> preThinkStates;
	protected StateFeedbackFactory feedbackFactory;
	protected SOSLoggerSystem abstractStateLogger;
	public SOSStateBasedDecisionMaker(SOSAgent<? extends Human> agent, StateFeedbackFactory feedbackFactory, Class<? extends SOSInformationModel> infoModelClass) {
		super(agent, infoModelClass);
		abstractStateLogger=agent.abstractStateLogger;
		this.feedbackFactory = feedbackFactory;
		setThinkStates(new ArrayList<SOSIState>());
		setPreThinkStates(new ArrayList<SOSIState>());

		initiateStates();
	}

	public SOSTask<?> decidePreThink() throws SOSActionException {
		return runStates(preThinkStates);
	}

	@Override
	public SOSTask<?> decide() throws SOSActionException {
		return runStates(thinkStates);
	}

	public SOSTask<?> runStates(List<SOSIState> states) throws SOSActionException {
		SOSEventPool eventPool = new SOSEventPool();
		SOSTask<?> result = null;
		for (SOSIState state : states) {
			state.handleEvents(eventPool.removeEvents(state.getClass()));
			if (result != null) {
				state.overTaken();
				continue;
			}
			/*
			 * if (!state.isValid()) {
			 * state.skipped();
			 * } else {
			 */
			long t1 = System.currentTimeMillis();
			try {
				result = state.decide(eventPool);
			} catch (Exception e) {
				if (e instanceof SOSActionException){
					abstractStateLogger.logln(this.infoModel.getTime()+":"+state.getClass().getSimpleName()+"\t\t\t : action="+e.getMessage()+"\t\t\t"+(System.currentTimeMillis() - t1)+"ms");
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

	public abstract void initiateStates();

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		for (SOSIState state : getPreThinkStates()) {
			state.hear(header, data, dynamicBitArray, sender, channel);
		}
		for (SOSIState state : getThinkStates()) {
			state.hear(header, data, dynamicBitArray, sender, channel);
		}
	}

	@SuppressWarnings("rawtypes")
	public List<SOSIState> getPreThinkStates() {
		return preThinkStates;
	}

	public void setPreThinkStates(List<SOSIState> states) {
		this.preThinkStates = states;
	}

	public List<SOSIState> getThinkStates() {
		return thinkStates;
	}

	public void setThinkStates(List<SOSIState> states) {
		this.thinkStates = states;
	}
}

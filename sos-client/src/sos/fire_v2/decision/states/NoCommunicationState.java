package sos.fire_v2.decision.states;

import java.util.List;

import sos.base.entities.FireBrigade;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.decision.FireInformationModel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.targets.EmptyTarget;
import sos.tools.decisionMaker.implementations.tasks.RestTask;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

public class NoCommunicationState extends SOSIState<FireInformationModel> {

	public NoCommunicationState(FireInformationModel infoModel) {
		super(infoModel);
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		if (infoModel.getAgent().messageSystem.type != Type.NoComunication)
			return null;
		infoModel.getAgent().sosLogger.agent.debug("No Commuincation handeler");

		if (shouldBeGoTOCenter()) {
			infoModel.getAgent().sosLogger.agent.debug("should speak with centerman!");
			if (((FireBrigade) infoModel.getAgent().me()).distance(infoModel.getAgent().newSearch.strategyChooser.noCommunication.getGatheringArea()) >= infoModel.getAgent().messageSystem.getMessageConfig().voiceChannels().get(0).getRange()) {
				infoModel.getAgent().sosLogger.agent.debug("Going to centerman");
				return new StandardMoveToAreaTask(new AreaTarget(infoModel.getAgent().newSearch.strategyChooser.noCommunication.getGatheringArea()), infoModel.getTime());
			}
			infoModel.setTimeInCenter(infoModel.getTimeInCenter() + 1);
			if (infoModel.getTimeInCenter() <= 2) {
				infoModel.getAgent().sosLogger.agent.debug("Resting to hear to centerman");
				return new RestTask(new EmptyTarget(), infoModel.getTime(), "Rest for Hear!");
			}
			infoModel.setLastVisitCenter(infoModel.getTime());
			infoModel.setTimeInCenter(0);

			return null;
		}
		infoModel.setTimeInCenter(0);

		//		if (((FireBrigade) infoModel.getAgent().me()).distance(infoModel.getAgent().newSearch.strategyChooser.noCommunication.getGatheringArea()) >= infoModel.getAgent().messageSystem.getMessageConfig().voiceChannels().get(0).getRange())
		//			infoModel.setTimeInCenter(infoModel.getTimeInCenter() + 1);

		return null;
	}

	private boolean shouldBeGoTOCenter() {
		return (infoModel.getModel().time() - infoModel.getLastVisitCenter()) > 40 && infoModel.getAgent().getMyClusterData().isCoverer();
	}

	@Override
	public void giveFeedbacks(List feedbacks) {
		// Not Used
	}

	@Override
	public void skipped() {
		// Not Used
	}

	@Override
	public void overTaken() {
		// Not Used
	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
		// Not Used
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		// Not Used
	}

	@Override
	public String getName() {
		return NoCommunicationState.class.getSimpleName();
	}

	@Override
	public void taken() {
		super.taken();
		((FireBrigadeAgent) infoModel.getAgent()).FDK.lastState = getName();
	}
}

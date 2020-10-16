package sos.ambulance_v2.decision.states;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.ambulance_v2.decision.LowCommunicationAmbulanceDecision;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.StandardEntity;
import sos.base.entities.VirtualCivilian;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

/* 
 * @author Reyhaneh
 */

public class LowCommunicationStartegyState extends SOSIState {

	private AmbulanceDecision ambDecision;
	public LowCommunicationAmbulanceDecision lowComAmbDecision;
	private AmbulanceTeamAgent ambulance = null;

	public LowCommunicationStartegyState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
		this.ambDecision = infoModel.getAmbulance().ambDecision;
		this.lowComAmbDecision = infoModel.getAmbulance().lowComAmbDecision;
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {

		if (infoModel.getAgent().messageSystem.type != Type.LowComunication)
			return null;

		VirtualCivilian target = null;
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ LowCommunicationStartegyState $$$$$$$$$$$$$$$$$$$$$$$$$");
		lowComAmbDecision.lowdclog.info("******************** LowCommunicationStartegyState  **********************");
		lowComAmbDecision.findValidAmbulances();
		if (!ambulance.isTargetsSet()) {
			lowComAmbDecision.findValidTargets();
			lowComAmbDecision.filterTargets();
		}
		lowComAmbDecision.assignPrioritytoTargets();
		lowComAmbDecision.makePriorityListFromTargets();
		lowComAmbDecision.assignCosttoTargets();

		target = lowComAmbDecision.getMinCostTarget();

		if (target == null) {
			infoModel.getLog().info("$$$$$ Skipped from LowCommunicationStartegyState $$$$$");
			return null;
		}
		ambulance.oldVirtualTarget = target;
		ambulance.lastState = " LowCommunicationStartegyState ";
		infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     VirtualTarget= =" + target);

		return new StandardMoveToAreaTask(new AreaTarget(ambulance.oldVirtualTarget.getPosition()), infoModel.getTime());
	}

	@Override
	public void giveFeedbacks(List feedbacks) {

	}

	@Override
	public void skipped() {

	}

	@Override
	public void overTaken() {

	}

	private AmbulanceTeam me() {
		return (AmbulanceTeam) (infoModel.getAgent().me());
	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
		// TODO Auto-generated method stub
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

}

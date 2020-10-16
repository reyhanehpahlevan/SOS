package sos.fire_v2.decision;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.fire_v2.Scape.NoCommScapeFire;
import sos.fire_v2.decision.states.ExtinguishFire;
import sos.fire_v2.decision.states.FireProbExtinguish;
import sos.fire_v2.decision.states.ImHurtState;
import sos.fire_v2.decision.states.MoveToHydrant;
import sos.fire_v2.decision.states.MoveToRefuge;
import sos.fire_v2.decision.states.NoCommunicationState;
import sos.fire_v2.decision.states.SearchUnExtinguishableFireZone;
import sos.tools.decisionMaker.implementations.stateBased.SOSStateBasedDecisionMaker;
import sos.tools.decisionMaker.implementations.stateBased.StateFeedbackFactory;

public class FireDecisionMaker extends SOSStateBasedDecisionMaker<FireInformationModel> {

	public String lastState = "Null";
	public String lastAct = "Null";

	public FireDecisionMaker(SOSAgent<? extends Human> agent, StateFeedbackFactory feedbackFactory) {
		super(agent, feedbackFactory, FireInformationModel.class);
	}

	@Override
	public void initiateStates() {
		///////PRE THINK
		//		getPreThinkStates().add(new UpdateInfoWorldMode(getInfoModel()));
		/////////Think
		getThinkStates().add(new ImHurtState(getInfoModel()));
		getThinkStates().add(new SearchUnExtinguishableFireZone(getInfoModel(), getInfoModel().getFireZoneSelector()));
		getThinkStates().add(new FireProbExtinguish(getInfoModel()));
		//fire prob
		getThinkStates().add(new MoveToRefuge(getInfoModel()));
		getThinkStates().add(new MoveToHydrant(getInfoModel()));
		//fire prob ex
	    getThinkStates().add(new NoCommScapeFire(getInfoModel()));
		getThinkStates().add(new ExtinguishFire(getInfoModel(), getInfoModel().getFireZoneSelector()));
		getThinkStates().add(new NoCommunicationState(getInfoModel()));
		//ex in block unreachable
	}

	public FireInformationModel getInfoModel() {
		return infoModel;
	}

}

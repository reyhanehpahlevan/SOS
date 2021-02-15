package sos.ambulance_v2.decision.states.helpstate;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/**
 * @author Reyhaneh
 */

public abstract class HelpStrategy {
	public abstract SOSTask<?> decide(SOSEventPool eventPool);

	protected AmbulanceInformationModel infoModel;
	protected SOSIState<AmbulanceInformationModel> state;

	public HelpStrategy(AmbulanceInformationModel infoModel, SOSIState<AmbulanceInformationModel> state) {
		this.state = state;
		this.infoModel = infoModel;
	}
}

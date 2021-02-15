package sos.ambulance_v2;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.tools.decisionMaker.definitions.SOSInformationModel;

public class AmbulanceInformationModel extends SOSInformationModel {

	public AmbulanceInformationModel(SOSAgent<? extends Human> agent) {
		super(agent);
	}

	public AmbulanceTeamAgent getAmbulance() {
		return (AmbulanceTeamAgent) getAgent();
	}

}
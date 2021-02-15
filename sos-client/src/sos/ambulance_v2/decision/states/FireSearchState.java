package sos.ambulance_v2.decision.states;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/* 
 * @author Reyhaneh
 */
public class FireSearchState extends SOSIState {
	private AmbulanceTeamAgent ambulance = null;

	@SuppressWarnings("unchecked")
	public FireSearchState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {

		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ FireSearchState $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambulance.lastState = " FireSearchState ";
		ambulance.newSearch.fireSearch();
		
		return null;
	}

	@Override
	public void giveFeedbacks(List feedbacks) {
		// TODO Auto-generated method stub

	}

	@Override
	public void skipped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void overTaken() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {

	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
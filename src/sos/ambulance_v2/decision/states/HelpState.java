package sos.ambulance_v2.decision.states;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.decision.states.helpstate.FullComHelpStrategy;
import sos.ambulance_v2.decision.states.helpstate.HelpStrategy;
import sos.ambulance_v2.decision.states.helpstate.NoComHelpStrategy;
import sos.ambulance_v2.decision.states.helpstate.lowComHelpStrategy;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/* 
 * @author Reyhaneh
 */
public class HelpState extends SOSIState {


	HelpStrategy strategy;
	
	public HelpState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		if(infoModel.getAgent().messageSystem.type == Type.LowComunication )
			strategy=new lowComHelpStrategy(infoModel, this);
		else if(infoModel.getAgent().messageSystem.type == Type.NoComunication )
			strategy= new NoComHelpStrategy(infoModel, this);
		else
			strategy=new FullComHelpStrategy(infoModel, this);
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		return strategy.decide(eventPool);
	}

	@Override
	public void giveFeedbacks(List feedbacks) {

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
		return this.getClass().getSimpleName();
	}


}

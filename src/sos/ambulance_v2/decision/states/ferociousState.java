package sos.ambulance_v2.decision.states;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;

/*
 * @author Reyhaneh
 */
public class ferociousState extends SOSIState {

	private AmbulanceDecision ambDecision;
	private AmbulanceTeamAgent ambulance = null;
	private boolean isCenterDesiding = false;
	
	public ferociousState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
		this.ambDecision = infoModel.getAmbulance().ambDecision;
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		
		if(infoModel.getTime() < 200){
			infoModel.getLog().info("$$$$$ Skipped from ferociousState  until cycle 200 $$$$$");
			return null;
		}
		
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ ferociousState $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambDecision.reset();
		ambDecision.setXMLLogging(true);
		ambDecision.findValidTargets(isCenterDesiding);
		ambDecision.ferociousFilterTargets();
		ambDecision.assignCosttoTargets();
		Human target = ambDecision.getMinCostTarget();

		if (target == null){
			infoModel.getLog().info("$$$$$ Skipped from ferociousState $$$$$");
			return null;
		}

		AmbulanceUtils.updateATtarget(target, infoModel.getATEntity(), this);
		infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + target);
		ambDecision.dclog.info(" WorkTarget = " + infoModel.getATEntity().getWork().getTarget());
		ambulance.lastState = " ferociousState ";

		ambulance.currentSaveHumanTask = new SaveHumanTask(target, infoModel.getTime());
		return ambulance.currentSaveHumanTask;
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

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
		
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	

}
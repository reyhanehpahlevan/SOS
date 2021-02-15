package sos.ambulance_v2.decision.states;

import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
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

public class SelfTaskAssigningState extends SOSIState {
	
	private AmbulanceDecision ambDecision;
	private boolean isCenterDesiding = false;
	private AmbulanceTeamAgent ambulance =null;
	final private int START_TIME = 7;
	
	public SelfTaskAssigningState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
		this.ambDecision=infoModel.getAmbulance().ambDecision;
		}
		

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {

		if(!(infoModel.getAgent().messageSystem.type == Type.LowComunication || infoModel.getAgent().messageSystem.type == Type.NoComunication) 
			&& infoModel.getTime() < START_TIME ) 
			return null;
		
		Human target = null;
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ SelfTaskAssigningState $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambDecision.dclog.info("******************** SelfTaskAssigningState  **********************");
		ambDecision.findValidAmbulances();
		ambDecision.setXMLLogging(true);
		ambDecision.findValidTargets(isCenterDesiding);
		ambDecision.filterTargets();
		ambDecision.assignPrioritytoTargets();
		ambDecision.makePriorityListFromTargets();
		ambDecision.assignCosttoTargets();
		
		if(infoModel.getAgent().messageSystem.type == Type.NoComunication)
			target = ambDecision.getMinCostTarget();
		else
			target = ambDecision.chooseTarget();
		
		if (target == null){
			infoModel.getLog().info("$$$$$ Skipped from SelfTaskAssigningState $$$$$");
			return null;
		}
		
		AmbulanceUtils.updateATtarget(target,me(),this);
			infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + target);
			ambDecision.dclog.info(" WorkTarget = " + me().getWork().getTarget());
			ambulance.lastState = " SelfTaskAssigning ";
			
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

	private AmbulanceTeam me(){
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

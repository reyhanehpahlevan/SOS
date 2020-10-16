package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.tools.Utils;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;

/* 
 * @author Reyhaneh
 */

public class changeToBetterTarget extends SOSIState {

	private AmbulanceTeamAgent ambulance = null;
	ArrayList<Human> validhums;

	public changeToBetterTarget(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
	}

	/**
	 * @author Salim,reyhaneh
	 * @throws SOSActionException
	 */
	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ changeToBetterTarget $$$$$$$$$$$$$$$$$$$$$$$$$");

		if (infoModel.getModel().refuges().isEmpty()) {
			infoModel.getLog().info("refuges is empty");
			infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ Skipp from changeToBetterTarget state$$$$$$$$$$$$$$$$$$$$$$$$$");
			return null;
		}

		if (ambulance.isLoadingInjured()) {
			infoModel.getLog().info("I've already loaded a target");
			infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ Skipp from changeToBetterTarget state$$$$$$$$$$$$$$$$$$$$$$$$$");
			return null;
		}
		ArrayList<Human> lastCycleSensedHumans = infoModel.getAgent().getVisibleEntities(Human.class);
		validhums = new ArrayList<Human>();

		String validInfos = "";
		ambulance.xmlLog.startLog("inValids", "");
		for (Human hu : lastCycleSensedHumans) {
			if (!isValid(hu))
				continue;
			infoModel.getLog().info("Human "+ hu+" is valid");
			validInfos += "<";
			validInfos = ambulance.xmlLog.addTag("Human", hu.getID().getValue() + "");
			validInfos += ambulance.xmlLog.addTag("Type", hu.toString());
			validInfos += ambulance.xmlLog.addTag("Validity", "valid");
			validInfos += "/>\n";
			validhums.add(hu);
		}
		ambulance.xmlLog.endLog("inValids");
		ambulance.xmlLog.addLog(validInfos, "Valids", "");

		if (validhums.isEmpty()) {
			infoModel.getLog().info("valid hums is empty");
			infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ Skipp from changeToBetterTarget state$$$$$$$$$$$$$$$$$$$$$$$$$");
			return null;
		}

		if (validhums.size() > 1) {
			Collections.sort(validhums, new Comparator<Human>() {

				@Override
				public int compare(Human o1, Human o2) {

					if (o1.getRescueInfo().getDeathTime() < o2.getRescueInfo().getDeathTime())
						return -1;
					if (o2.getRescueInfo().getDeathTime() < o1.getRescueInfo().getDeathTime())
						return 1;
					return 0;
				}
			});
		}

		if (shouldChange()) {
			ambulance.xmlLog.Info("BestTarget = " + validhums.get(0) + "    ShouldChange = " + "true");
			AmbulanceUtils.updateATtarget(validhums.get(0), infoModel.getATEntity(), this);
			infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + validhums.get(0));
			ambulance.ambDecision.dclog.info(" WorkTarget = " + infoModel.getATEntity().getWork().getTarget());
			ambulance.currentSaveHumanTask = new SaveHumanTask(validhums.get(0), infoModel.getTime());
			return ambulance.currentSaveHumanTask;
		}
		ambulance.xmlLog.Info("<BestTarget = " + validhums.get(0) + "    ShouldChange = " + "false" + "/>\n");
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ Skipp from changeToBetterTarget state$$$$$$$$$$$$$$$$$$$$$$$$$");
		return null;

	}

	private boolean shouldChange() throws SOSActionException {

		if (infoModel.getATEntity().getWork().getTarget() == null)
			return true;

		if (infoModel.getAgent().messageSystem.type == Type.LowComunication ||
				infoModel.getAgent().messageSystem.type == Type.NoComunication)
			return true;

		if (validhums.get(0).getRescueInfo().getDeathTime() < infoModel.getATEntity().getWork().getTarget().getRescueInfo().getDeathTime())
		{
			AmbulanceUtils.rejectTarget(infoModel.getATEntity().getWork().getTarget(), infoModel.getATEntity(), ambulance);
			return true;
		}

		return false;
	}

	private boolean isValid(Human hu) {
		if (hu == null)
			return false;
		if (!AmbulanceUtils.isValidToDecide(hu, infoModel.getLog(), ambulance))
			return false;
		if (hu.getBuriedness() != 0) {
			infoModel.getLog().info(hu + "burriedness is zero");
			return false;
		}
		return true;
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

	public int getTimeToNearestRefuge() {
		sos.base.move.Path path = infoModel.getAgent().move.getPathFromTo(java.util.Collections.singleton(infoModel.getEntity().getAreaPosition()), infoModel.getModel().refuges(), StandardMove.class);
		return Utils.getSampleTimeToTarget(path);
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
		return this.getClass().getName();
	}

}

package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.worldmodel.EntityID;
import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

/* 
 * @author Reyhaneh
 */
public class CenterAssignedTask extends SOSIState {

	private AmbulanceDecision ambDecision;
	private Human centerRecommendedTarget = null;
	private AmbulanceTeamAgent ambulance = null;

	public CenterAssignedTask(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
		this.ambDecision = infoModel.getAmbulance().ambDecision;

	}

	public boolean isInSeearchArea() {

		Area targetPosition = ambulance.oldVirtualTarget.getPosition();
		if (targetPosition == infoModel.getATEntity().getPosition())
			return true;
			
		if (!(targetPosition instanceof Building))
			return false;
		
		if(!ambulance.lineOfSightPerception.getThisCycleVisibleShapeFromMe().contains(targetPosition.getX(),targetPosition.getY()))
			return false;
		
		for (Civilian civ : ambulance.getVisibleEntities(Civilian.class)) {
			if(civ.getAreaPosition()==targetPosition)
				return true;
			
		}
		
//		Building b = (Building)(ambulance.oldVirtualTarget.getPosition());
//		for (ShapeInArea area : b.getSearchAreas())
//		{
//			if (area.contains(ambulance.me().getX(), ambulance.me().getY()))
//				return true;
//		}
		return false;
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ check Validity of old target $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambDecision.dclog.info("******************** check Validity of old target **********************");

		if (infoModel.getAgent().messageSystem.type == Type.LowComunication) {
			if (ambulance.oldVirtualTarget != null) {
				
				if (!isInSeearchArea()) {
					infoModel.getLog().info(" move to VirtualTarget");
					return new StandardMoveToAreaTask(new AreaTarget(ambulance.oldVirtualTarget.getPosition()), infoModel.getTime());
				}
				else {
					ArrayList<Human> allSensedHumans = new ArrayList<Human>();
					allSensedHumans = infoModel.getAgent().getVisibleEntities(Human.class);
					for (Human human : allSensedHumans) {
						if (AmbulanceUtils.isValidToRescue(human, infoModel.getLog())) {
							AmbulanceUtils.updateATtarget(human, me(), this);
							infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + human);
							ambDecision.dclog.info(" WorkTarget = " + me().getWork().getTarget());
							ambulance.currentSaveHumanTask = new SaveHumanTask(human, infoModel.getTime());
							ambulance.oldVirtualTarget.setIgnored(true);
							ambulance.oldVirtualTarget = null;
							return ambulance.currentSaveHumanTask;
						}
					}
					int index = ambulance.oldVirtualTarget.getPosition().getAreaIndex();
					
					ambulance.lowComAmbDecision.ignoreBuildingUntil[index] = (short) (1000);
					ambulance.oldVirtualTarget.setIgnored(true);
					ambulance.oldVirtualTarget = null;
				}
			}
		}

		Human oldTarget = me().getWork().getTarget();

		if (ambulance.isLoadingInjured()) {
			if (ambulance.currentSaveHumanTask == null) {
				infoModel.getLog().warn("in CenterAssignedTask,currentSaveHumanTask is null ");
				ambulance.currentSaveHumanTask = new SaveHumanTask(ambulance.loadingInjured(), infoModel.getTime());
			}
			return ambulance.currentSaveHumanTask;
		}

		if (ambDecision.isOldTaskValid()) {
			infoModel.getLog().info(" old target was valid ");
			infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + oldTarget);

			if (oldTarget != null &&
					!(!((AmbulanceTeamAgent) (infoModel.getAgent())).isLoadingInjured()
							&& oldTarget.getRescueInfo().longLife() && isValid() && !centerRecommendedTarget.getRescueInfo().longLife()
							&& (oldTarget.getRescueInfo().getDeathTime() - centerRecommendedTarget.getRescueInfo().getDeathTime()) > 40
							&& centerRecommendedTarget != oldTarget
							&& oldTarget instanceof Civilian)) {

				if (ambulance.currentSaveHumanTask == null) {
					infoModel.getLog().warn("in CenterAssignedTask,currentSaveHumanTask is null ");
					ambulance.currentSaveHumanTask = new SaveHumanTask(oldTarget, infoModel.getTime());
				}
				return ambulance.currentSaveHumanTask;
			}

			infoModel.getLog().info("old target was rejected because :\n" +
					"oldtarget is longlife = " + oldTarget.getRescueInfo().longLife() + " center target is valid = " +
					isValid() + " center target is not longlife = " + (!centerRecommendedTarget.getRescueInfo().longLife()));
		}
		else if (oldTarget != null) {
			if (!(oldTarget.getRescueInfo().isIgnored()))
				oldTarget.getRescueInfo().setIgnoredUntil(IgnoreReason.InvalidOldTarget, infoModel.getTime() + 10);
			AmbulanceUtils.rejectTarget(oldTarget, me(), ambulance);
		}

		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ CenterAssignedTask $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambDecision.dclog.info("******************** CenterAssignedTask  **********************");

		if (!isValid())
		{
			infoModel.getLog().info("in CenterAssignedTask class : Target = null ");
			infoModel.getLog().info("$$$$$ Skipped from CenterAssignedTaskState $$$$$");
			if (centerRecommendedTarget != null)
				AmbulanceUtils.sendRejectMessage(centerRecommendedTarget, ambulance);

			return null;
		}
		infoModel.getLog().info(" center target was valid ");
		ambulance.isCenterAssigned = true;
		infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + centerRecommendedTarget);

		AmbulanceUtils.updateATtarget(centerRecommendedTarget, me(), this);

		ambulance.lastState = " CenterAssignedTask ";
		ambDecision.dclog.info(" WorkTarget = " + me().getWork().getTarget());
		Human target = centerRecommendedTarget;
		centerRecommendedTarget = null;

		if (target == null)
			infoModel.getLog().error("in centerRecommendedTarget class Target = null ");

		ambulance.currentSaveHumanTask = new SaveHumanTask(target, infoModel.getTime());
		return ambulance.currentSaveHumanTask;

	}

	private boolean isValid() {
		if (centerRecommendedTarget == null)
			return false;
		else if (!AmbulanceUtils.isValidToRescue(centerRecommendedTarget, infoModel.getLog()))
			return false;

		return true;
	}

	private AmbulanceTeam me() {
		return (AmbulanceTeam) (infoModel.getAgent().me());
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

		if (header.equalsIgnoreCase(MessageXmlConstant.HEADER_AMBULANCE_ASSIGN)) {
			infoModel.getLog().logln("assign message --> Header:" + header + " Data[" + data + "] From:" + sender + " Channel:" + channel);
			if (ambulance.taskAssigner)
				return;

			AmbulanceTeam at = infoModel.getModel().ambulanceTeams().get(data.get(MessageXmlConstant.DATA_AMBULANCE_INDEX));
			Human human = (Human) infoModel.getModel().getEntity(new EntityID(data.get(MessageXmlConstant.DATA_ID)));
			Area position = infoModel.getModel().areas().get(data.get(MessageXmlConstant.DATA_AREA_INDEX));
			if (human == null && position != null) {
				human = infoModel.getAgent().updater.newHuman(data.get(MessageXmlConstant.DATA_ID), infoModel.getTime()
						- infoModel.getAgent().messageSystem.getNormalMessageDelay(), 9000, 20, 20, position);
				//	ambDecision.updateHuman(human);
			}
			if (!human.isPositionDefined() || (!human.getPosition().equals(position) && human.updatedtime()
					- (infoModel.getTime() - infoModel.getAgent().messageSystem.getNormalMessageDelay()) < 0)) {
				human.setPosition(position.getID(), position.getX(), position.getY());
			}
			if (at.getAmbIndex() == ambulance.me().getAmbIndex() && human != null) {
				this.centerRecommendedTarget = human;
				human.getRescueInfo().setLongLife(data.get(MessageXmlConstant.DATA_LONG_LIFE) == 1);
			}
			if (human != null) {
				ambulance.centerAssignLists.add(human);
				human.getRescueInfo().setLongLife(data.get(MessageXmlConstant.DATA_LONG_LIFE) == 1);
			}
			if (human.getLastSenseTime() < infoModel.getTime() - 2) {//Added by Ali
				infoModel.getLog().info("centerAssignedTask in hear ");
				human.getRescueInfo().setNotIgnored();
			}
		}

	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	public void setCenterRecommendedTarget(Human target) {
		centerRecommendedTarget = target;
	}

}
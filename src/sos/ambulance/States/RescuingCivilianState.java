package sos.ambulance.States;

import java.util.ArrayList;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.ambulance_v2.base.AmbulanceConstants.Rescue_Complete_status;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.tools.ParticleFilter;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class RescuingCivilianState extends AmbulanceGeneralState {
	private int firstHp0Time = 1000;
	protected Rescue_Complete_status status = null;

	public RescuingCivilianState(AmbulanceTeamAgent ownerAgent, Pair<? extends Area, Point2D> place, Human target) {
		super(ownerAgent, place, target);
		self.setUnderMissionTarget(target);
		setStatus(Rescue_Complete_status.MOVING_TO_TARGET);
		if (self.me().getWork().getTarget() != null)
			self.me().getWork().getTarget().getRescueInfo().removeAT(self.me());
		self.me().getWork().setTarget(target,null);
		target.getRescueInfo().addAT(self.me());
	}

	private void setStatus(Rescue_Complete_status st) {
		this.status = st;
		stateChanged = true;
	}

	//	private boolean isLoadedOnce = false;

	//-------------------------------------------------------------------------------------
	private boolean isReachableTarget() {
		return !self.move.isReallyUnreachableXY(place.first(), (int) place.second().getX(), (int) place.second().getY());
	}

	//-------------------------------------------------------------------------------------
	private boolean isReachableAnyRefuge() {
		if (self.model().refuges().isEmpty())
			return false;
		boolean isNotReachable = self.move.isReallyUnreachable(self.model().refuges());
		return !isNotReachable;
	}

	//-------------------------------------------------------------------------------------
	@Override
	public void act() throws SOSActionException {
		//		self.log().debug("Acting as " + this.getClass().getSimpleName() + " status=" + status);
		if (target.getHP() == 0)
			firstHp0Time = Math.min(firstHp0Time, self.time());

		switch (status) {
		case MOVING_TO_TARGET:
			moveToTarget();
			break;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		case RESCUING:
			rescue();
			break;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		case LOADING:
			load();
			break;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		case MOVING_TO_ROAD:
			moveToRoad();
			break;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		case MOVING_TO_REFUGE:
			moveToRefuge();
			break;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		case UNLOADING:
			unload();
		}
	}

	private void moveToTarget() throws SOSActionException {
		if (self.location().getAreaIndex() == place.first().getAreaIndex() 
				&& self.canSeeTheCenterOfBuilding(self.me().getPositionPoint(), self.me().getAreaPosition())) {
			self.log().logln("****I am in my target place!");
			ArrayList<Civilian> viewedCiv = self.getLastCycleInMyPositionNeedHelpCivilians();
			self.log().logln(" in position humanoids=" + viewedCiv);
			if (viewedCiv.contains(target)) { // I see the target now
				sendAckIfNotSent(0);
				if (target.getBuriedness() > 0) {
					setStatus(Rescue_Complete_status.RESCUING);
					this.act();
					return;
				} else {
					setStatus(Rescue_Complete_status.LOADING);
					this.act();
					return;
				}
			} else { //target out of my action
				isDone = true;
				ignoreTarget(IgnoreReason.TargetOutOfMyAction, self.time() + 10);
				sendTaskAckMsg(2);//rejected
				self.log().logln("****target out of my action");
				return;
			}
		} else {
			self.log().logln("****I am going to target " + target + "    " + place);
			if (!isReachableTarget() && !isReachablityBug()||isStockBug()) {
				self.log().logln("****NOT REACHABLE TARGET! from reachability");
				ignoreTarget(IgnoreReason.NotReachableToTarget, self.time() + 1);
				sendTaskAckMsg(2);//rejected
				target.getRescueInfo().removeAT(self.me());
				if (self.me().getWork().getTarget().equals(target))
					self.me().getWork().setTarget(null,null);
				self.me().getWork().setCurrentState(ATstates.SEARCH);
				isDone = true;
				return;
			}
			int busy = self.getMyBusyCycles(target, place, ATstates.MOVE_TO_TARGET);
			self.me().getWork().setCurrentState(ATstates.MOVE_TO_TARGET);
			self.me().getWork().setNextFreeTime( (self.time() + busy));

			sendAckIfNotSent(0);
			sendInfoMsgIfRequire(ATstates.MOVE_TO_TARGET, busy);
			sendStatueMsgBySay(ATstates.MOVE_TO_TARGET, false);
			//			self.move.moveStandardXY(place.first(), (int) place.second().getX(), (int) place.second().getY());
			self.moveTo(target);
		}
	}

	public void sendAckIfNotSent(int statusType) {
		if (!acknowledgeMsgSent) {
			acknowledgeMsgSent = true;
			sendTaskAckMsg(statusType);//accepted
		}
	}

	private void rescue() throws SOSActionException {
		self.log().logln("****I am in rescuing state");
		ArrayList<Civilian> viewedCivilian = self.getLastCycleInMyPositionNeedHelpCivilians();
		ArrayList<AmbulanceTeam> inPlaceAmbulanceTeams = self.getLastCycleInMyPositionAmbulanceTeams();

		self.log().logln(" in position humanoids=" + viewedCivilian);
		self.log().logln(" in position ATs=" + inPlaceAmbulanceTeams);

		if (viewedCivilian.contains(target)) { // I see the target now
			inPlaceAmbulanceTeams.remove(self.me());
			boolean loader = self.AmITheLoader2(inPlaceAmbulanceTeams);
			boolean rescueContinue = viewedCivilian.size() > 1 || self.ifContinueRescuing(target.getBuriedness(), self.myAgeFrom(inPlaceAmbulanceTeams));

			if (target.getBuriedness() > 0) {
				self.log().logln("*********buriedness=" + target.getBuriedness());
				if (loader || rescueContinue) {
					self.log().logln("*********primary and rescueContinue");
					int busy = self.getMyBusyCycles(target, place, ATstates.RESCUE);
					self.me().getWork().setCurrentState(ATstates.RESCUE);
					self.me().getWork().setNextFreeTime((self.time() + busy));

					sendInfoMsgIfRequire(ATstates.RESCUE, busy);
					sendRescueMsgBySay();

					self.rescue(target);
				} else {
					self.log().logln("*********my work is finished here!");
					isDone = true;
					ignoreTarget(IgnoreReason.FinishedWorkOnTarget, self.time() + 4);
				}
			} else {
				setStatus(Rescue_Complete_status.LOADING);
				this.act();
				return;
			}
		} else { //target out of my action
			if (self.getVisibleEntities(Civilian.class).contains(target)) {
				self.log().error("ambulance suddenly moved from server when it was in place of target " + target + "!!!!!moving to target again...");
				setStatus(Rescue_Complete_status.MOVING_TO_TARGET);
				this.act();
				return;
			}
			isDone = true;
			ignoreTarget(IgnoreReason.TargetOutOfMyAction, self.time() + 15);
			sendTaskAckMsg(2);//rejected
			return;
		}

	}

	private void sendRescueMsgBySay() {
		boolean needHelp = false;
		if (target.getRescueInfo().getATneedToBeRescued() - target.getRescueInfo().getNowWorkingOnMe().size() > 0)
			needHelp = true;
		sendStatueMsgBySay(ATstates.RESCUE, needHelp);
	}

	private void sendInfoMsgIfRequire(ATstates status, int busy) {
		self.log().trace("IsItReadyToSendInfoMSG?time=" + self.time() + " lastInfoSent=" + lastInfoSent + " statechanged?" + stateChanged);
		if (self.time() - lastInfoSent > 7 || stateChanged)
			sendInfoMsg(target.getID().getValue(), status, busy);
	}

	private void load() throws SOSActionException {
		if (self.isFull()) {
			self.log().logln("****I am full and gonna go to refuge!");
			setStatus(Rescue_Complete_status.MOVING_TO_REFUGE);
			act();
			return;
		}
		acknowledgeMsgSent = false;

		ArrayList<Civilian> viewedCivi = self.getLastCycleInMyPositionNeedHelpCivilians();
		ArrayList<AmbulanceTeam> ats = self.getLastCycleInMyPositionAmbulanceTeams();
		self.log().logln(" in position humanoids=" + viewedCivi);
		self.log().logln(" in position ATs=" + ats);
		ats.remove(self.me());
		boolean loader = self.AmITheLoader2(ats);
		if (viewedCivi.size() > 1 && (target.getRescueInfo().getATneedToBeRescued() == 1 || self.AmITheLoader1(target.getRescueInfo().getNowWorkingOnMe())))
			loader = true;
		if (loader && viewedCivi.contains(target)) {
			self.log().logln("************ loading " + target);
			self.load(target);
		} else {
			if (loader && self.getVisibleEntities(Civilian.class).contains(target)) {
				self.log().error("ambulance suddenly moved from server when it was in place of target " + target + "!!!!!moving to target again...");
				setStatus(Rescue_Complete_status.MOVING_TO_TARGET);
				this.act();
				return;
			}
			ignoreTarget(IgnoreReason.ImNotLoader, self.time() + 10);
			self.log().logln("Target ignored beacause i'm not loader of " + target);
			isDone = true;
			return;
		}

	}

	private void moveToRefuge() throws SOSActionException {
		self.log().logln("****Moving to refuge state!");
		if (self.location() instanceof Refuge) {
			self.log().logln("****I am in refuge!");
			if (self.isFull()) {
				ignoreTarget(IgnoreReason.InRefuge, self.time() + 5);
				setStatus(Rescue_Complete_status.UNLOADING);
				this.act();
				return;
			} else {
				self.log().error("Ambulance " + self.getID() + " is in refuge but nothing to unload!");
			}
		} else {
			boolean is_reachable_refuge = isReachableAnyRefuge();
			if (!is_reachable_refuge && !isReachablityBug()) {
				self.log().logln("****NOT REACHABLE ANY REFUGE!");
				sendTaskAckMsg(2);//rejected
				target.getRescueInfo().removeAT(self.me());
				if (!(self.location() instanceof Building)) {
					ignoreTarget(IgnoreReason.NotReachableToRefuge, self.time() + 5);
					setStatus(Rescue_Complete_status.UNLOADING);
					this.act();
					return;
				}

				//				self.move.moveStandard(self.model().roads());
				//				return;
			}
			if (!target.isAlive() && ParticleFilter.HP_PRECISION / target.getDamage() < self.time() - firstHp0Time) {
				self.log().logln("****Civilian has been dead!");
				ignoreTarget(IgnoreReason.DeadHuman, 1000);
				setStatus(Rescue_Complete_status.UNLOADING);
				this.act();
				return;
			}
			if (self.isFull())
				sendAckIfNotSent(1);//finished

			int busy = self.getMyBusyCycles(target, place, ATstates.MOVE_TO_REFUGE);
			self.me().getWork().setCurrentState(ATstates.MOVE_TO_REFUGE);
			self.me().getWork().setNextFreeTime((short) (self.time() + busy));
			sendInfoMsgIfRequire(ATstates.MOVE_TO_REFUGE, busy);
			sendStatueMsgBySay(ATstates.MOVE_TO_REFUGE, false);

			self.moveToRefuges();

		}
	}

	private void moveToRoad() throws SOSActionException {
		self.log().logln("****Moving to road state");

		int busy = 1;
		self.me().getWork().setNextFreeTime( (self.time() + busy));
		sendInfoMsgIfRequire(ATstates.MOVE_TO_REFUGE, busy);

		if (self.location() instanceof Road) {
			if (self.isFull()) {
				ignoreTarget(IgnoreReason.UnloadInRoad, self.time() + 5);
				if (self.model().refuges().isEmpty()) {
					target.getRescueInfo().setIgnoredUntil(IgnoreReason.NoRefuge, 1000);
				}
				setStatus(Rescue_Complete_status.UNLOADING);
				this.act();
				return;
			} else {
				self.log().error("Ambulance " + self.getID() + " is in Road but nothing to unload!");
			}
		} else {

			self.move.moveStandard(self.model().roads());
		}
	}

	private void unload() throws SOSActionException {
		if (self.isLoadingInjured())
			self.unload();
		isDone = true;
		if (self.me().getWork().getTarget().equals(target)){
			self.me().getWork().setTarget(null,null);
			self.me().getWork().setNextFreeTime(0);
		}
		self.me().getWork().setCurrentState(ATstates.SEARCH);

		self.log().logln("****Task completed successfully!****");
		self.setUnderMissionTarget(null);

	}

	//-------------------------------------------------------------------------------------
	@Override
	public boolean finished() {
		self.log().info("check if Rescuing Civilian State finished...");
		if (isDone) {
			self.log().debug(this.getClass() + " is done");
			return true;
		}

		if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.isPositionDefined() && !self.centerRecommendedTarget.getRescueInfo().isIgnored() && !self.centerRecommendedTarget.getRescueInfo().longLife() && target.getRescueInfo().longLife() && !self.isFull()) {
			self.log().debug("if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.isPositionDefined() && !self.centerRecommendedTarget.getRescueInfo().isIgnored() && !self.centerRecommendedTarget.getRescueInfo().longLife()&& target.getRescueInfo().longLife() && !self.isFull()) return true");
			ignoreTarget(IgnoreReason.CenterAssignMoreImportantTarget, self.time() + 3);
			sendTaskAckMsg(2);//rejected
			return true;
		}
		//		if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.isPositionDefined()
		//				&& !self.centerRecommendedTarget.getRescueInfo().isIgnored()
		//				&& !(self.centerRecommendedTarget instanceof Civilian) && !self.isFull()) {
		//			self.log().debug("if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.isPositionDefined() && !self.centerRecommendedTarget.getRescueInfo().isIgnored() && !(self.centerRecommendedTarget instanceof Civilian) && !self.isFull()) return true");
		//			ignoreTarget(self.time() + 3);
		//			sendTaskAckMsg(2);//rejected
		//			return true;
		//		}//TODO need???

		if (!target.isPositionDefined()) {
			self.log().debug("!target.isPositionDefined() return true");
			isDone = true;
			ignoreTarget(IgnoreReason.NoPosition, self.time() + 5);
			sendTaskAckMsg(1);//finished
		}
		if (status == Rescue_Complete_status.UNLOADING && self.location() instanceof Refuge && !self.isFull()) {
			self.log().debug("if (status == Rescue_Complete_status.UNLOADING && self.location() instanceof Refuge && !self.isFull()) return true");
			return true;
		}
		if(!self.isFull()&& ((self.target == null || !self.target.isPositionDefined() || self.target.getRescueInfo().isIgnored()) && target == self.target) ){
			self.log().debug("if ((self.target == null || !self.target.isPositionDefined() || self.target.getRescueInfo().isIgnored()) && target == self.target) return true");
			return true;
		}
		if (target.isPositionDefined() && target.getPosition() instanceof Road && !isReachableAnyRefuge()) {
			self.log().logln("Not reachable any refuge! in finished()");
			ignoreTarget(IgnoreReason.NotReachableToRefuge, self.time() + 2);
			return true;
		}
		if (target.getPosition() instanceof Refuge) {
			ignoreTarget(IgnoreReason.InRefuge, 1000);
			return true;
		}
		if (!target.getPosition().equals(self.me()) && target.getPosition() instanceof AmbulanceTeam) {
			ignoreTarget(IgnoreReason.InAmbulance, self.time() + 5);
			return true;
		}

		//sinash 2013
		if(!self.isFull()&&(target.getRescueInfo().getIgnoreReason() == IgnoreReason.IgnoredTargetMessageReceived) || (target.getRescueInfo().getIgnoreReason() == IgnoreReason.WillDie) ){
			self.log().debug("GONNA_DIE_IGNORED: target: " + target.getID().getValue()+ " cycle " + self.time() + 5 );
			return true;
		}

		return false;
	}

	@Override
	public void resetState() {
		if (status == Rescue_Complete_status.RESCUING || (status == Rescue_Complete_status.LOADING && !self.isFull()))
			status = Rescue_Complete_status.MOVING_TO_TARGET;
		if (self.loadingInjured() != null && self.loadingInjured().equals(target))
			status = Rescue_Complete_status.MOVING_TO_REFUGE;
		self.setUnderMissionTarget(target);
		acknowledgeMsgSent = false;
		lastInfoSent = 2;
		stateChanged = false;
	}

}
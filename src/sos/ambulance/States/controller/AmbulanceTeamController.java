package sos.ambulance.States.controller;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.util.SOSActionException;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class AmbulanceTeamController extends StateController {
	public AmbulanceTeamController(AmbulanceTeamAgent agent) {
		super(agent);
	}

	@Override
	public void act() throws SOSActionException {
		//		self.log().logln("");
		//		self.log().logln("***********************");
		//
		//		self.log().info("state=" + getState() + " my position=" + self.me().getPositionArea() + " \t " +
		//				"target=" + self.target + " NextTarget=" + self.nextTarget + " centerAssign=" + self.getCenterRecommendedTarget());
		//		/*************************** for reducing process *********************/
		//		if (self.me().getHP() == 0) {
		//			self.problemRest("i died!!!");
		//		}
		//		/***************************** checking health **********************/
		//		if (self.amIGoingToDieSoon()) {
		//			setInterrupt(new ImHurtState(self, true));
		//		} else if (self.isItCriticalTogoRefuge()) {
		//			setInterrupt(new ImHurtState(self, false));
		//		}
		//		/***************************** performing tasks *********************/
		//		//if (self.centerRecommendedTarget != null || (self.time() >= 8 && self.centerRecommendedTarget != null && self.nextTarget != null && self.centerAssignLists.size() == 0 || !self.target.isPositionDefined() || self.target.getRescueInfo().isIgnored()))
		//
		//		if (!self.isLoadingInjured() && (self.haveTaskFromCenter() || (self.time() >= 10 /* && !self.isCenterATaskAssigner() */))) {//XXX test beshe
		//		//			self.checkAndGetTasks(); @reyhaneh
		//		}
		//		if (!self.isLoadingInjured() && self.target == null) {//XXX test beshe
		//		//			self.specialSelfTaskAssigning();  @reyhaneh
		//		}
		//		if (getState() instanceof AbstractSearchState && self.target != null) {
		//			self.log().logln("current state is Search AmbulanceTeam ->setting to null--> ");
		//			getState().isDone = true;
		//			setState(null);
		//		}
		//		int targetChoosingTry = 0;
		//		while (self.target != null) {
		//			self.log().log("***current state:" + getState() + " target before doing =" + self.target + "   ");
		//			if (self.target != null)
		//				self.log().log(" target position:" + self.target.getPosition() + "  Ignored=" + self.target.getRescueInfo().getIgnoreReason() + " till" + self.target.getRescueInfo().getIgnoredUntil());
		//
		//			self.log().logln("");
		//
		//			actState();
		//			self.doNoActIfTimeIsFinished();
		//			self.log().log("current state:" + getState() + " target after doing =" + self.target + "   position=");
		//			if (self.target != null) {
		//				self.log().log(" target position:" + self.target.getPosition() + "  Ignored=" + self.target.getRescueInfo().getIgnoreReason() + " till" + self.target.getRescueInfo().getIgnoredUntil());
		//			}
		//			self.log().logln("");
		//
		//			if (!self.isLoadingInjured() && !AmbulanceUtils.isValidToRescue(self.target, self.log())) {
		//
		//				self.target = null;
		//				self.log().logln("Target" + self.target + " is invalid-->self.target=null");
		//				//				self.checkAndGetTasksSpecial(); @reyhaneh
		//			}
		//
		//			self.log().logln("set Rescuing State...");
		//			if (getState() == null && self.target != null && !self.target.getRescueInfo().isIgnored() && self.target.isPositionDefined()) {
		//				if (!(self.target instanceof Civilian))
		//					setState(new RescuingAgentState(self, self.target.getPositionPair(), self.target));
		//				else
		//					setState(new RescuingCivilianState(self, self.target.getPositionPair(), self.target));
		//
		//			}
		//			if (targetChoosingTry >= 10) {
		//				self.log().warn("Target choosing Try is bigger 10???why!!!!");
		//				break;
		//			}
		//			targetChoosingTry++;
		//		}
		//		/**************************** free Time Search ***********************/
		//		setState(new LowCommunicationSearchState(self));
		//		if (self.isTimeToActFinished())
		//			self.randomWalk();
		//		setState(new SearchForCivilianState(self));
		//
		//		self.log().logln("Nothing to search and get a target...Helping others");
		//		self.target = AmbulanceUtils.getBestCivilianWhenSearchHaveNoTask(self);
		//		if (self.target != null) {
		//			if (!(self.target instanceof Civilian))
		//				setState(new RescuingAgentState(self, self.target.getPositionPair(), self.target));
		//			else
		//				setState(new RescuingCivilianState(self, self.target.getPositionPair(), self.target));
		//		}
		//
		//		setState(new OtherClusterSearchState(self));
	}

}

package sos.ambulance.States;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants.Rescue_Hearsing_status;
import sos.base.entities.Area;
import sos.base.entities.Human;
import sos.base.util.SOSActionException;

/**
 * Created by IntelliJ IDEA.
 * User: faraz
 * Date: Jul 1, 2009
 * Time: 11:18:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class HearsingCivilianState extends AmbulanceGeneralState {
	public HearsingCivilianState(AmbulanceTeamAgent ownerAgent, Pair<? extends Area,Point2D> place, Human target) {
		super(ownerAgent, place, target);
        status = Rescue_Hearsing_status.MOVING_TO_TARGET;

// self.self().Target = target;
		// target.workInfo.addAT(self.self());
    }

    protected Rescue_Hearsing_status status = null;
//    private boolean isLoadedOnce = false;

// //-------------------------------------------------------------------------------------
	// private boolean isReachableTarget() {
	//
	// /* if(self.isReachable(self.myPosition(),place))
	// return true;
	// else*/
	// if (!self.dijkstraNew.isReallyUnreachable(self.myPosition(), Collections.singletonList(place))) {
	// return true;
	// }
	// return false;
	// }
	//
	// //-------------------------------------------------------------------------------------
	// private boolean isReachableAnyRefuge() {
	// if (ownerAgent.world.refuges.isEmpty())
	// return false;
	// boolean isNotReachable = self.dijkstraNew.isReallyUnreachable(self.myPosition(), self.world.refuges);
	// return !isNotReachable;
	// }
	//
	// //-------------------------------------------------------------------------------------
	// private Refuge findBestRefuge() { //albate age mogheye task assignng khode refuge moshakhas nashode bood ino estefade konim
	// if (target.nearestRefuge != null && !self.dijkstraNew.isReallyUnreachable(self.myPosition(),
	// Collections.singleton(target.nearestRefuge)))
	// return target.nearestRefuge;
	// ArrayList<MotionlessObject> ref = new ArrayList<MotionlessObject>();
	// for (Refuge r : self.world.refuges) {
	// if (self.isReachable(r)) {
	// ref.add(r);
	// }
	// }
	// if (ref.isEmpty()) {
	// for (Refuge r : self.world.refuges) {
	// if (!self.dijkstraNew.isReallyUnreachable(self.myPosition(), Collections.singleton(r))) {
	// ref.add(r);
	// }
	// }
	// }
	// if (ref.isEmpty())
	// return null;
	// if (ref.size() == 1)
	// return (Refuge) ref.iterator().next();
	// float time[] = new float[ref.size()];
	// float min = Integer.MAX_VALUE;
	// int index = 0;
	// for (int i = 0; i < time.length; i++) {
	// if(!Main.hugeMap)
	// time[i] = SOS.util.PathTimeEstimator.Estimator.floydShortestPath[self.getNodeIndex(self.myPosition())][self.getNodeIndex(ref.get(i))] + 5 - self.world.time() / 60;
	// else
	// time[i] = 4;
	// if (min > time[i]) {
	// min = time[i];
	// index = i;
	// }
	// }
	// return (Refuge) ref.get(index);
	// }

    //-------------------------------------------------------------------------------------
    @Override
	public void act() throws SOSActionException {

// switch (status) {
		// case MOVING_TO_TARGET:
		// if (self.myPosition() == place) {
		// self.logara.exportln("****I am in my target place!");
		// ArrayList<Civilian> viewedCiv = self.getLastCycleInMyPositionDeadCivilians();
		// self.logara.exportln(" in position dead humanoids=" + viewedCiv);
		//
		// if (viewedCiv.contains(target)) { // I see the target now
		// if (!acknowledgeMsgSent) {
		// acknowledgeMsgSent = true;
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 0}); //accepted msg
		// }
		// if (target.buriedness() > 0) {
		// status = Rescue_Hearsing_status.RESCUING;
		// this.act();
		// return;
		// } else {
		// status = Rescue_Hearsing_status.LOADING;
		// this.act();
		// return;
		// }
		// } else { //target out of my action
		// isDone = true;
		// //if(target.hp()!=0){ //target is loaded and rescued already
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 1}); //finished msg
		// self.logara.exportln("****target out of my action");
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 10;
		// //}
		// return;
		// }
		// } else {
		// self.logara.exportln("****I am going to target " + target.id + "    " + place.id);
		//
		// boolean is_reachable_target = isReachableTarget();
		// //log.exportln("****after isReachable");
		// if (!is_reachable_target) {
		// //canBeDone=false;
		// self.logara.exportln("****NOT REACHABLE TARGET! from reachability");
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 2}); //rejected msg
		// target.workInfo.removeAT(self.self());
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 1;
		// isDone = true;
		// return;
		// }
		// int busy = self.getMyBusyCycles(target, place, AmbulanceTeam.ATstates.MOVE_TO_TARGET);
		// self.self().currentState = AmbulanceTeam.ATstates.MOVE_TO_TARGET;
		// self.self().nextFreeTime = self.time() + busy;
		// if (!acknowledgeMsgSent) {
		// acknowledgeMsgSent = true;
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 0}); //accepted msg
		// }
		// self.addMessage("ambulance info", new int[]{self.ambIndex, 0, target.id, busy});
		// sendStatueMsgBySay(0, false);
		// self.move(place);
		// //log.exportln("****After move");
		// }
		// break;
		// //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		// case RESCUING:
		// self.logara.exportln("****I am in rescuing state");
		// ArrayList<Civilian> viewedCivilian = self.getLastCycleInMyPositionDeadCivilians();
		// ArrayList<Humanoid> inPlaceAmbulanceTeams = self.getLastCycleInMyPositionAmbulanceTeams();
		//
		// self.logara.exportln(" in position humanoids=" + viewedCivilian);
		// self.logara.exportln(" in position ATs=" + inPlaceAmbulanceTeams);
		// //remainTime=target.buriedness()/energyAssigned+1+normal_time_to_reach_refuge+1;
		// if (viewedCivilian.contains(target)) { // I see the target now
		//
		// inPlaceAmbulanceTeams.remove(self.self());
		// target.workInfo.isPrimaryForThisAgent = self.AmITheLoader(inPlaceAmbulanceTeams);
		// boolean rescueContinue = self.ifContinueRescuing(target.buriedness(), self.myAgeFrom(inPlaceAmbulanceTeams));
		// if (viewedCivilian.size() > 1)
		// rescueContinue = true;
		// if (target.buriedness() > 0) {
		// self.logara.exportln("*********buriedness=" + target.buriedness());
		//
		// if (target.workInfo.isPrimaryForThisAgent || rescueContinue) {
		// self.logara.exportln("*********primary and rescueContinue");
		// int busy = self.getMyBusyCycles(target, place, AmbulanceTeam.ATstates.RESCUE);
		// self.self().currentState = AmbulanceTeam.ATstates.RESCUE;
		// self.self().nextFreeTime = self.time() + busy;
		// self.addMessage("ambulance info", new int[]{self.ambIndex, 1, target.id, busy});
		// boolean needHelp = false;
		// // if(target.workInfo.pureEnergyNeededDeath-target.workInfo.energyWorking()>0)
		// // needHelp=true;
		// sendStatueMsgBySay(1, needHelp);
		// self.rescue(target);
		// } else {
		// self.logara.exportln("*********my work is finished here!");
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// isDone = true;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 4;
		// }
		// } else {
		// status = Rescue_Hearsing_status.LOADING;
		// this.act();
		// return;
		// }
		// } else { //target out of my action
		// isDone = true;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 1}); //finished msg
		// //if(target.hp()!=0){ //target is loaded and rescued already
		//
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 15;
		// //}
		// return;
		// }
		// break;
		// //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		// case LOADING:
		//
		// if (self.isFull()) {
		// //remainTime=normal_time_to_reach_refuge+1;
		// self.logara.exportln("****I am full and gonna go to refuge!");
		//
		// boolean is_reachable_refuge = isReachableAnyRefuge();
		// if (!is_reachable_refuge) {
		// self.logara.exportln("****NOT REACHABLE ANY REFUGE!");
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 2}); //rejected msg
		// target.workInfo.removeAT(self.self());
		// status = Rescue_Hearsing_status.MOVING_TO_ROAD;
		// act();
		//
		// } else {
		// status = Rescue_Hearsing_status.MOVING_TO_REFUGE;
		// act();
		//
		// }
		// return;
		// } else {
		// ArrayList<Civilian> viewedCivi = self.getLastCycleInMyPositionDeadCivilians();
		// ArrayList<Humanoid> ats = self.getLastCycleInMyPositionAmbulanceTeams();
		// self.logara.exportln(" in position humanoids=" + viewedCivi);
		// self.logara.exportln(" in position ATs=" + ats);
		// ats.remove(self.self());
		// target.workInfo.isPrimaryForThisAgent = self.AmITheLoader(ats);
		// if (viewedCivi.size() > 1 && (target.workInfo.pureEnergyNeededDeath <= 20 || self.AmITheLoader(target.workInfo.ambs)))//TODO update ambs
		// target.workInfo.isPrimaryForThisAgent = true;
		// if (!isLoadedOnce) {
		//
		// if (viewedCivi.contains(target) && target.workInfo.isPrimaryForThisAgent) {
		// isLoadedOnce = true;
		// self.logara.exportln("************ loading " + target);
		//
		// self.load(target);
		// } else {
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// isDone = true;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 10;
		// return;
		// }
		// } else { //cant load the civilian
		// self.logara.exportln("**cant load the civilian!");
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// isDone = true; //Todo maybe add finished msg
		// target.isIgnored = true;
		// target.ignoredUntil = 400;
		// return;
		// }
		// }
		// break;
		// //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		// case MOVING_TO_ROAD:
		// self.logara.exportln("****Moving to road state");
		// if (self.myPosition() instanceof Road || self.myPosition() instanceof Node) {
		// if (self.isFull()) {
		// status = Rescue_Hearsing_status.UNLOADING;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// isDone = true;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 5;
		// if (self.world.refuges.isEmpty()) {
		// target.ignoredUntil = 300;
		// }
		// self.unload();
		// } else {
		// System.out.println("Ambulance " + self.self().id + " is in Road but nothing to unload!");
		// }
		// } else {
		// self.move(self.world.roads);
		// }
		// break;
		// //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		// case MOVING_TO_REFUGE:
		//
		// self.logara.exportln("****Moving to refuge state!");
		// if (self.myPosition() instanceof Refuge) {
		// self.logara.exportln("****I am in refuge!");
		//
		// if (self.isFull()) {
		// status = Rescue_Hearsing_status.UNLOADING;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 5;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// self.unload();
		// } else {
		// System.out.println("Ambulance " + self.self().id + " is in refuge but nothing to unload!");
		// }
		// } else {
		//
		// boolean is_reachable_refuge = isReachableAnyRefuge();
		// if (!is_reachable_refuge) {
		// self.logara.exportln("****NOT REACHABLE ANY REFUGE!");
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 2}); //rejected msg
		// target.workInfo.removeAT(self.self());
		// if (!(self.myPosition() instanceof Building)) {
		// status = Rescue_Hearsing_status.UNLOADING;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 5;
		// isDone = true;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// self.unload();
		// }
		// return;
		// }
		// if (self.isFull()) {
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 1}); //finished msg
		// }
		// int busy = self.getMyBusyCycles(target, place, AmbulanceTeam.ATstates.MOVE_TO_REFUGE);
		// self.self().currentState = AmbulanceTeam.ATstates.MOVE_TO_REFUGE;
		// self.self().nextFreeTime = self.time() + busy;
		// self.addMessage("ambulance info", new int[]{self.ambIndex, 2, target.id, busy});
		// sendStatueMsgBySay(2, false);
		// Refuge bestRefuge = findBestRefuge();
		// self.move(Collections.singleton(bestRefuge), 4); //Transporting Move
		// }
		// break;
		// //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		// case UNLOADING:
		// self.logara.exportln("****Task completed successfully!****");
		// isDone = true;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(null);
		// }
    }

    //-------------------------------------------------------------------------------------
	@Override
	public boolean finished() {
		// if (isDone)
		// return true;
		//
		// if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.position() != null &&
		// !self.centerRecommendedTarget.isIgnored && !self.isFull()) {
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 3;
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 2});
		// return true;
		// }
		// if (self.centerRecommendedTarget != null && self.centerRecommendedTarget.position() != null &&
		// !self.centerRecommendedTarget.isIgnored && !(self.centerRecommendedTarget instanceof Civilian) &&
		// !self.isFull()) {
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 3;
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 2});
		// return true;
		// }
		//
		// if (target.position() == null) {
		// isDone = true;
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 5;
		// self.addMessage("taskAck", new int[]{self.ambIndex, target.id, 1}); //finished msg
		// }
		// if (status == Rescue_Hearsing_status.UNLOADING && self.myPosition() instanceof Refuge && !self.isFull())
		// return true;
		// if ((self.target == null || self.target.position() == null || self.target.isIgnored) && target == self.target)
		// return true;
		// if (target.position() != null && (target.position() instanceof Road || target.position() instanceof Node) && !isReachableAnyRefuge()) {
		// self.logara.exportln("Not reachable any refuge!");
		// target.isIgnored = true;
		// target.ignoredUntil = self.time() + 2;
		// return true;
		// }
        return false;
    }

    @Override
	public void resetState() {
		//
		// if (status == Rescue_Hearsing_status.RESCUING || (status == Rescue_Hearsing_status.LOADING && !self.isFull()))
		// status = Rescue_Hearsing_status.MOVING_TO_TARGET;
		// if (self.loadingInjured() == target)
		// status = Rescue_Hearsing_status.MOVING_TO_REFUGE;
		// ((AmbulanceTeamAgent) ownerAgent).setUnderMissionTarget(target);
		// acknowledgeMsgSent = false;
    }

}

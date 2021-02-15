package sos.ambulance.States;


import java.util.ArrayList;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.ambulance_v2.base.AmbulanceConstants.Rescue_Agent_status;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Human;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class RescuingAgentState extends AmbulanceGeneralState {
	
	public RescuingAgentState(AmbulanceTeamAgent ownerAgent, Pair<? extends Area,Point2D> place, Human target) {
		super(ownerAgent, place, target);
		self.setUnderMissionTarget(target);
		setStatus(Rescue_Agent_status.MOVING_TO_TARGET);

		self.me().getWork().setTarget(target,null);
		target.getRescueInfo().addAT(self.me());
    }

    private Rescue_Agent_status status = null;
    private void setStatus(Rescue_Agent_status st){
    	this.status=st;
    	stateChanged=true;
    }
    //-------------------------------------------------------------------------------------
    private boolean isReachableTarget() {
		return !self.move.isReallyUnreachableXY(place.first(),(int)place.second().getX(),(int)place.second().getY());
    }

    //-------------------------------------------------------------------------------------
    @Override
	public void act() throws SOSActionException {
        switch (status) {
            case MOVING_TO_TARGET:
            	if (self.location().getAreaIndex() == place.first().getAreaIndex() &&
            			SOSGeometryTools.distance(self.me().getPositionPoint(),place.second())<30000) {
            		self.log().logln("****I am in my target place!");
            		ArrayList<Human> viewedHum = self.getLastCycleInMyPositionNeedHelpHumanoids();
            		self.log().logln(" in position humanoids=" + viewedHum);
					if (viewedHum.contains(target)) { // I see the target now
						if (target.getBuriedness() > 0) {
							if (!acknowledgeMsgSent) {
								acknowledgeMsgSent = true;
								sendTaskAckMsg(0);//accept
							}
							setStatus(Rescue_Agent_status.RESCUING);
							this.act();
							return;
						} else {
							this.isDone = true;
							self.setUnderMissionTarget(null);
							sendTaskAckMsg(1);//finished
							return;
						}
					} else {                                  //target out of my action
						if(self.getVisibleEntities(Human.class).contains(target)){
	    					self.log().error("ambulance suddenly moved from server when it was in place of target "+target+"!!!!!moving to target again...");
	    					setStatus(Rescue_Agent_status.MOVING_TO_TARGET);
	    					this.act();
	    					return;
	    				}
	                    isDone = true;
						ignoreTarget(IgnoreReason.TargetOutOfMyAction,self.time() + 10);
						sendTaskAckMsg(2);//reject
						self.log().logln("****target out of my action");
	                    return;
	                }
            	} else {
					self.log().logln("****I am going to target " + target + "    " + place);
					boolean is_reachable_target = isReachableTarget();
					if (!is_reachable_target && !isReachablityBug()||isStockBug()) {
						self.log().logln("****NOT REACHABLE TARGET! from reachability");
						sendTaskAckMsg(2);//rejected msg
						target.getRescueInfo().removeAT(self.me());
						ignoreTarget(IgnoreReason.NotReachableToTarget,self.time() + 2);
						isDone = true;
						return;
					}
					int busy = self.getMyBusyCycles(target, place, ATstates.MOVE_TO_TARGET);
					self.me().getWork().setCurrentState(ATstates.MOVE_TO_TARGET);
					self.me().getWork().setNextFreeTime(self.time() + busy);
					if (!acknowledgeMsgSent) {
						acknowledgeMsgSent = true;
						sendTaskAckMsg(0);//accepted msg
					}
					if(self.time()-lastInfoSent>7 || stateChanged)
						sendInfoMsg(target.getID().getValue(),ATstates.MOVE_TO_TARGET,busy);
					sendStatueMsgBySay(ATstates.MOVE_TO_TARGET, false);
//					self.move.moveStandardXY(place.first(),(int)place.second().getX(),(int)place.second().getY());
					self.moveTo(target);
				}
                break;
                //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            case RESCUING:
            	self.log().logln("****I am in rescuing state");
            	ArrayList<Human> viewedHu = self.getLastCycleInMyPositionNeedHelpHumanoids();
            	ArrayList<AmbulanceTeam> inPlaceAmbulanceTeams = self.getLastCycleInMyPositionAmbulanceTeams();

            	self.log().logln(" in position humanoids=" + viewedHu);
            	self.log().logln(" in position ATs=" + inPlaceAmbulanceTeams);
                if (viewedHu.contains(target)) {         // I see the target now
					inPlaceAmbulanceTeams.remove(self.me());
	                boolean rescueContinue = true;
					if (target.getBuriedness() > 0) {
						 self.log().logln("*********buriedness=" + target.getBuriedness());
	                     if (rescueContinue) {
							 self.log().logln("*********primary and rescueContinue");
							 int busy = self.getMyBusyCycles(target, place, ATstates.RESCUE);
							 self.me().getWork().setCurrentState(ATstates.RESCUE);
							 self.me().getWork().setNextFreeTime(self.time() + busy);
							 if(self.time()-lastInfoSent>7 || stateChanged)
								 sendInfoMsg(target.getID().getValue(),ATstates.RESCUE,busy);
	                         boolean needHelp = false;
							 if (target.getRescueInfo().getATneedToBeRescued() - target.getRescueInfo().getNowWorkingOnMe().size() > 0)
	                                needHelp = true;
	                         sendStatueMsgBySay(ATstates.RESCUE, needHelp);
	                         self.rescue(target);
	                     } else {
							 self.log().logln("*********my work is finished here!");
	                         isDone = true;
							 ignoreTarget(IgnoreReason.FinishedWorkOnTarget,self.time() + 4);
	                     }
	                } else {

	                    this.isDone = true;
						self.setUnderMissionTarget(null);
						sendTaskAckMsg(1);//finished
	                    return;
	                    }
                } else {                                  //target out of my action
                	if(self.getVisibleEntities(Human.class).contains(target)){
    					self.log().error("ambulance suddenly moved from server when it was in place of target "+target+"!!!!!moving to target again...");
    					setStatus(Rescue_Agent_status.MOVING_TO_TARGET);
    					this.act();
    					return;
    				}
                    isDone = true;
                    ignoreTarget(IgnoreReason.TargetOutOfMyAction,1000);
                    sendTaskAckMsg(2);//rejected
                    return;
                }
                break;
        }
    }


	//-------------------------------------------------------------------------------------
	@Override
	public boolean finished() {
        if (isDone)
            return true;
		if (!target.isPositionDefined()) {
            isDone = true;
            ignoreTarget(IgnoreReason.NoPosition,self.time() + 5);
            sendTaskAckMsg(1);//finished
        }
		if (target.getBuriedness() == 0) {
			ignoreTarget(IgnoreReason.FinishedWorkOnTarget,1000);
            return true;
        }
		if ((self.target == null || !self.target.isPositionDefined() || self.target.getRescueInfo().isIgnored()) && target == self.target)
            return true;

        return false;
    }

    @Override
	public void resetState() {

        if (status == Rescue_Agent_status.RESCUING)
            status = Rescue_Agent_status.MOVING_TO_TARGET;
		self.setUnderMissionTarget(target);
        acknowledgeMsgSent = false;
        lastInfoSent=2;
    	stateChanged=false;
    }

}
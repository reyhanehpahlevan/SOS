package sos.ambulance.States.controller;

import sos.ambulance.States.AmbulanceGeneralState;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.CenterAgent;
import sos.base.SOSAgent;
import sos.base.util.SOSActionException;

public abstract class StateController {
	private AmbulanceGeneralState state = null;
	private AmbulanceGeneralState interrupt = null;
	protected final AmbulanceTeamAgent self;
	protected final CenterAgent center;
	
	public StateController(AmbulanceTeamAgent agent) {
		this.self = agent;
		this.center = null;
	}
	
	public StateController(SOSAgent<?> agent) {
		if(agent instanceof CenterAgent){
			this.center = (CenterAgent)agent;
			this.self = null;
		}else{
			this.self = (AmbulanceTeamAgent)agent;
			this.center = null;
		}
	}
	public void setInterrupt(AmbulanceGeneralState inter) throws SOSActionException {
		if (interrupt != null)
			return;
		if (state != null)
			state.resetState();
		interrupt = inter;
		actState();
	}
	
	public void setState(AmbulanceGeneralState state) throws SOSActionException {
		this.state = state;
		actState();
	}
	
	public AmbulanceGeneralState getState() {
		return state;
	}
	
	protected void actState() throws SOSActionException {
		self.log().info("Acting state interrupt:"+interrupt+" state:"+state);
		self.lastState=interrupt+"";

		// -------------------- INTERRUPT --------------------//
		if (interrupt != null && !interrupt.finished()) {
			self.log().logln("\tActing as interrupt:"+interrupt);
			try {
				self.log().logln("in actState act --> INTERRUPT ::::time got:"+(System.currentTimeMillis()-self.ambulanceThinkStart)+"ms");
				this.interrupt.act();
			}catch (SOSActionException e) {
					self.log().info("\t\tintrupt state "+state +" Acted in :"+(System.currentTimeMillis()-self.ambulanceThinkStart)+"ms , act="+e.getMessage());
					self.logToAmbDecision("\t\tintrupt state "+state +" Acted in :"+(System.currentTimeMillis()-self.ambulanceThinkStart)+"ms , act="+e.getMessage());
					throw e;
			} catch (Exception e) {
				self.log().fatal(e);
			}
		}
		self.log().debug("\tnothing to do in intrrupt:"+interrupt+" interrupt is now null");
		interrupt = null;
		self.lastState=state+"";
		// -------------------- Normal State --------------------//
		if (state != null && !this.state.finished()) {
			self.log().logln("\tActing normal state :"+state);
			try {
				self.log().logln("\tin actState act --> NORMAL "+state);
				this.state.act();
			} catch (SOSActionException e) {
				self.log().info("Normal state "+state +" Acted in :"+(System.currentTimeMillis()-self.ambulanceThinkStart)+"ms, act="+e.getMessage());
				self.logToAmbDecision("Normal state "+state +" Acted in :"+(System.currentTimeMillis()-self.ambulanceThinkStart)+"ms, act="+e.getMessage());
				throw e;
			} catch (Exception e) {
				self.log().fatal(e);
			}
			state.isDone = true;
			self.log().debug("nothing to do in normal state:"+state+" so state isdone=true");
		} else {
			self.log().logln("\tnormal state("+state+") is null or finished! so now normal state is null");
			this.state = null;
		}
		self.lastState=state+"";
	}
	
	public abstract void act() throws SOSActionException;
}
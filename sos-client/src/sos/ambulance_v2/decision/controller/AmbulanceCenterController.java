package sos.ambulance_v2.decision.controller;

import sos.ambulance.States.controller.StateController;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.SOSAgent;
import sos.base.util.SOSActionException;
import sos.base.util.sosLogger.SOSLoggerSystem;

/**
 * Created by IntelliJ IDEA.
 * To change this template use File | Settings | File Templates.
 */
public class AmbulanceCenterController extends StateController {
	private boolean isCenterDesiding = true;

	public AmbulanceCenterController(SOSAgent<?> ownerAgent) {
		super(ownerAgent);
	}

	private AmbulanceDecision ambDecision() {
		if (self != null)
			return self.ambDecision;
		return center.ambDecision;
	}

	private SOSLoggerSystem lg() {
		if (self != null)
			return self.log();
		return center.log();
	}

	@Override
	public void act() throws SOSActionException {

		lg().info("$$$$$$$$$$$$$$$$$$$ I am Leader $$$$$$$$$$$$$$$$$$$$$$$$$$");
		lg().logln("****************((((((( " + ((self != null) ? self.time() : center.time()) + " )))))))****************");
		lg().logln("$$$$$ Find Valid Ambulances $$$$$");
		ambDecision().setXMLLogging(false);
		ambDecision().findValidAmbulances();
		lg().logln("$$$$$ Find Valid targets $$$$$");
		ambDecision().findValidTargets(isCenterDesiding);
		lg().logln("$$$$$ set Number Of AT sWhich Taget Need $$$$$");
		ambDecision().setNumberOfATsWhichTagetNeed();
		lg().logln("$$$$$ remove Agents Target After Middle Of Simulation $$$$$");
		ambDecision().removeAgentsTargetAfterMiddleOfSimulation();
		lg().logln("$$$$$ Removing UnrescueableTargets $$$$$");
		ambDecision().removeUnrescueableTargets();
		lg().logln("$$$$$ assigning priority to tagets $$$$$");
		ambDecision().assignPrioritytoTargets();
		lg().logln("$$$$$ make PriorityList From Targets $$$$$");
		ambDecision().makePriorityListFromTargets();
		lg().logln("$$$$$ Imagining $$$$$");
		ambDecision().imagining();
		lg().logln("$$$$$ Sending Possible Ignore Messages $$$$$");
		ambDecision().sendIgnoreMessages(); //sinash 2013
		lg().logln("$$$$$ Assigning $$$$$");
		ambDecision().sendAssignMessages();
	}

}
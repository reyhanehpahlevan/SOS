package sos.police_v2.state.intrupt;

import sos.ambulance_v2.tools.SimpleDeathTime;
import sos.base.entities.Civilian;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;

public class ReachableCivilianState extends PoliceAbstractIntruptState {
	Civilian civil = null;

	public ReachableCivilianState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);

	}
	@Override
	public void precompute() {
		
	}

	@Override
	public void act() throws SOSActionException {
		log.info("acting as:" + this.getClass().getSimpleName());
		log.info("LOG IS NOT COMPLETED");

		makeReachableTo(civil);
	}

	@Override
	public boolean canMakeIntrupt() {
		if (civil != null) {
			if (!isReachableTo(civil.getPositionPair()))
				return true;
		}

		for (Civilian civilian : agent.getVisibleEntities(Civilian.class)) {

			if (thisCivilianIsUsefullToClear(civilian)) {
				civil = civilian;
				return true;
			}
		}
		return false;
	}

	private boolean thisCivilianIsUsefullToClear(Civilian civilian) {
		if (SimpleDeathTime.getEasyLifeTime(civilian.getHP(), civilian.getDamage(), model().time()) > 20)
			return true;
		return false;
	}

}

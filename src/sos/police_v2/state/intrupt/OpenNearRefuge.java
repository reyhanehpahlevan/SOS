package sos.police_v2.state.intrupt;

import java.util.ArrayList;

import sos.base.entities.Refuge;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;

public class OpenNearRefuge extends PoliceAbstractIntruptState {

	private Refuge target;

	public OpenNearRefuge(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public boolean canMakeIntrupt() {
		ArrayList<Refuge> sencedrefuges = agent.getVisibleEntities(Refuge.class);
		for (Refuge refuge : sencedrefuges) {
			if (isReachableTo(refuge))
				continue;
			target=refuge;
			return true;
		}
		return false;
	}

	@Override
	public void precompute() {
		
	}

	@Override
	public void act() throws SOSActionException {
		if (target != null) {
			makeReachableTo(target);
			target = null;
		}
	}

}

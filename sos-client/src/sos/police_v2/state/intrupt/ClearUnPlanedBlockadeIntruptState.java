package sos.police_v2.state.intrupt;

import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;

public class ClearUnPlanedBlockadeIntruptState extends PoliceAbstractIntruptState {
//TODO     agar baraye mesal ye 2 ta edge ro clear bokone raha va etefaghat khoobi miofte 
	public ClearUnPlanedBlockadeIntruptState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		
	}
	@Override
	public void precompute() {
		
	}

	@Override
	public boolean canMakeIntrupt() {
		
		return false;
	}

	@Override
	public void act() throws SOSActionException {
		
		
	}

}

package sos.ambulance.States;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.base.entities.Refuge;
import sos.base.util.SOSActionException;

/**
 * Created by IntelliJ IDEA.
 * User: ara
 * To change this template use File | Settings | File Templates.
 */
public class SearchForCivilianState extends AbstractSearchState {

	public SearchForCivilianState(AmbulanceTeamAgent ownerAgent) {
		super(ownerAgent);
	}

	@Override
	public boolean finished() {
		return isDone;
	}

	@Override
	public void act() throws SOSActionException {
		if (self.location() instanceof Refuge && self.isFull())
			self.unload();
		self.log().info("before acting as search think got:" + (System.currentTimeMillis() - self.ambulanceThinkStart) + "ms");
		if (self.time() - lastInfoSent > 5)
			sendInfoMsg(0, ATstates.SEARCH, self.getMyBusyCycles(null, null, ATstates.SEARCH));

		//		self.doNoActIfTimeIsFinished();
		self.search();

	}


	@Override
	public void resetState() {
	}

}

package sos.police_v2.state;

import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.tools.SearchTask;

public class FireSearchState extends PoliceAbstractState {

	public FireSearchState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}
	@Override
	public void precompute() {
		
	}

	@Override
	public void act() throws SOSActionException {

		log.info("acting as:" + this.getClass().getSimpleName());
		SearchTask task = agent.newSearch.fireSearchTask();
		handleTask(task);
	}

	public void handleTask(SearchTask task) throws SOSActionException {
		log.debug("Handeling task " + task);
		if (task == null) {
			return;
		} else {
			moveToShape(task.getArea());
		}
	}

}

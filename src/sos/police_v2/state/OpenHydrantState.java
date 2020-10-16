package sos.police_v2.state;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import sos.base.entities.Hydrant;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class OpenHydrantState extends PoliceAbstractState {

	private Shape clusterShape;
	private ArrayList<Hydrant> clusterHydrants;
	private ArrayList<Hydrant> isDone;
	private ArrayList<Hydrant> targets;
	private int lastAfterShock = 0;

	public OpenHydrantState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterShape = model().searchWorldModel.getClusterData().getConvexShape();
		isDone = new ArrayList<Hydrant>();
		setMyClusterHydrant();
	}

	private void setMyClusterHydrant() {
		clusterHydrants = new ArrayList<Hydrant>();
		for (Hydrant hydrant : model().Hydrants())
			if (clusterShape.contains(new Point2D.Double(hydrant.getX(), hydrant.getY())))
				clusterHydrants.add(hydrant);
		targets = new ArrayList<Hydrant>(clusterHydrants);
	}

	@Override
	public void act() throws SOSActionException {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return;
			}
		}
		if (lastAfterShock != model().getLastAfterShockTime())
			reset();
		if (targets.size() == 0)
			return;
		checkDone();
		makeReachableTo(targets);
	}

	private void reset() {
		lastAfterShock = model().getLastAfterShockTime();
		isDone.clear();
		targets.addAll(clusterHydrants);
	}

	private void checkDone() {
		for (Hydrant hydrant : targets) {
			if (isReachableTo(hydrant)) {
				isDone.add(hydrant);
				continue;
			}
			if (hydrant.isReallyReachable(true)) {
				isDone.add(hydrant);
				continue;
			}
		}
		targets.removeAll(isDone);
	}
}

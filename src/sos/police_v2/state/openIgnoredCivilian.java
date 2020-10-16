package sos.police_v2.state;

import java.awt.Point;
import java.util.ArrayList;

import sos.base.entities.Civilian;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;

public class openIgnoredCivilian extends PoliceAbstractState {
	ArrayList<Civilian> done;
	boolean isMoveOnCivilian = false;
	Civilian target;

	public openIgnoredCivilian(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		done = new ArrayList<Civilian>();
	}

	@Override
	public void precompute() {

	}

	@Override
	public void act() throws SOSActionException {
		if (model().refuges().size() == 0)
			return;
		ArrayList<Civilian> civilians = agent.getVisibleEntities(Civilian.class);
		ArrayList<Civilian> ignoredCivilians = new ArrayList<Civilian>();
		if (target == null) {
			for (Civilian civilian : civilians) {
				if (isIgnoredCivilian(civilian) && !(done.contains(civilian)))
					ignoredCivilians.add(civilian);
			}
			if (ignoredCivilians.size() > 0) {
				target = getNearCivilian(ignoredCivilians);
				log.debug(target + " is ignore target");
			}
		}
		if (target != null) {
			if (!isMoveOnCivilian) {
				log.debug("on move to civilian");
				if (isOnCivilian()) {
					isMoveOnCivilian = true;
				} else {
					moveToPoint(target.getPositionPair());
				}
			}
			if (isStillNearToCivilian() && !(agent.me().getPositionArea() instanceof Refuge)) {
				move(model().refuges());
			} else {
				done.add(target);
				target = null;
				isMoveOnCivilian = false;
			}
		}

	}

	private boolean isOnCivilian() {
		int dis = (int) Point.distance(target.getX(), target.getY(), agent.me().getX(), agent.me().getY());
		if (dis < 1000) {
			log.debug("i now on " + target);
			return true;
		}
		return false;
	}

	private boolean isStillNearToCivilian() {
		int dis = (int) Point.distance(target.getX(), target.getY(), agent.me().getX(), agent.me().getY());
		if (dis < agent.VIEW_DISTANCE)
			return true;
		return false;
	}

	private Civilian getNearCivilian(ArrayList<Civilian> ignoredCivilians) {
		Civilian result = null;
		int distance = Integer.MAX_VALUE;
		for (Civilian civi : ignoredCivilians) {
			int dis = (int) Point.distance(civi.getX(), civi.getY(), agent.me().getX(), agent.me().getY());
			if (distance > dis) {
				distance = dis;
				result = civi;
			}
		}
		return result;
	}

	private boolean isIgnoredCivilian(Civilian civilian) {
		if (!(civilian.getPosition() instanceof Road))
			return false;
		if (civilian.getPositionArea() instanceof Refuge)
			return false;
		if (civilian.getBuriedness() != 0)
			return false;
		if (civilian.getHP() == 0)
			return false;
		if (civilian.getDamage() == 0)
			return false;
		if (civilian.getPositionArea() instanceof Refuge)
			return false;
		return true;
	}

}

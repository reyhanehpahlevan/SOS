package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import sos.base.entities.Civilian;
import sos.base.entities.PoliceForce;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.state.preCompute.PrecomputeState;

public class NearCivilianOpenerState extends PoliceAbstractIntruptState {
	int remainedCivilian = 3;
	Civilian target = null;
	private PrecomputeState precomputeState;

	public NearCivilianOpenerState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		precomputeState = policeForceAgent.getState(PrecomputeState.class);
	}

	@Override
	public boolean canMakeIntrupt() {
		if (target != null) {
			updateTarget();
		}
		if (target != null) {
			return true;
		}
		if (remainedCivilian < 1)
			return false;
		if (precomputeState.isDone)
			return false;
		ArrayList<Civilian> nears = agent.getVisibleEntities(Civilian.class);
		for (Civilian civilian : nears) {
			if (!isReachableTo(civilian)) {
				if (civilian.getBuriedness() == 0)
					continue;
				if (!PoliceUtils.isValidCivilian(civilian, agent, true))
					continue;
				if (!amIClear(civilian)) {
					continue;
				}
				target = civilian;
				remainedCivilian--;
				return true;
			}
		}
		return false;
	}

	private boolean amIClear(Civilian civilian) {
		ArrayList<PoliceForce> forces = agent.getVisibleEntities(PoliceForce.class);
		Shape civilianSight = agent.lineOfSightPerception.findVisibleShape(civilian);
		for (PoliceForce force : forces) {
			if (force.getID().getValue() < agent.getID().getValue()) {
				if (civilianSight.contains(new Point2D.Float(force.getX(), force.getY()))) {
					log.debug(civilian + " passed to " + force + " to clear near civilian");
					return false;
				}
			}
		}
		return true;
	}

	private void updateTarget() {
		if ((agent.VIEW_DISTANCE * 1.5f) < Point.distance(agent.me().getX(), agent.me().getY(), target.getX(), target.getY())) {
			log.warn(target + " Near Civilian male ghabl bood dg fasele gereftim velesh kardim");
			target = null;
		} else {
			if (!amIClear(target)) {
				target = null;
				log.debug("no ke omad be bazar kohne mishe del azar yeki dg baghiasho clear mikone be man che ;D");
			}
		}

	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void act() throws SOSActionException {
		if (target != null) {
			makeReachableTo(target);
			target = null;
		}
	}
}

package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.util.ArrayList;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.police_v2.state.preCompute.Task;

public class FastOpeningAgentFromBuilding extends PoliceAbstractIntruptState {
	PrecomputeState state;
	Task<? extends StandardEntity> target = null;
	ArrayList<Task<? extends StandardEntity>> doneAgent;

	public FastOpeningAgentFromBuilding(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		state = policeForceAgent.getState(PrecomputeState.class);
		doneAgent = new ArrayList<Task<? extends StandardEntity>>();
	}

	@Override
	public boolean canMakeIntrupt() {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return false;
			}
		}
		if (state.isDone)
			return false;
		if (target != null)
			return true;
		setTarget();
		if (target != null) {
			log.debug("target entekhab shode " + target);
			return true;
		}

		return false;
	}

	private void setTarget() {
		log.info("set target called");
		ArrayList<Task<? extends StandardEntity>> tasks = state.getTasksOf(model().getPoliceTasks(agent.me()));
		for (Task<? extends StandardEntity> task : tasks) {
			if (!(task.getRealEntity() instanceof Human))
				continue;
			if (task.isDone())
				continue;
			if (doneAgent.contains(task))
				continue;
			Human human = (Human) task.getRealEntity();

			if (human.isBuriednessDefined())
				if (human.getBuriedness() != 0) {
					log.info(human + " is skiped becuase it have buriedness");
					continue;
				}
			if (!human.isPositionDefined()) {
				log.info(human + " is skiped becuase position is undefine");
				continue;
			}
			if (human.getAreaPosition() instanceof Refuge)
				continue;
			if (!(human.getAreaPosition() instanceof Building))
				continue;
			if (isInDangersPlace(human)) {
				target = task;
				log.info("target set shode =" + target);
				break;
			}
		}
	}

	private boolean isInDangersPlace(Human human) {
		int distance = getDistanceToFire(human);
		if (distance < (model().getDiagonalOfMap() / 10))
			return true;
		log.info("NOT IN DANGER ZONE " + human);
		return false;
	}

	private int getDistanceToFire(Human human) {
		int distance = Integer.MAX_VALUE;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : agent.model().getFireSites()) {
			if (pair.second().isDisable())
				continue;
			for (Building out : pair.second().getOuter()) {
				distance = (int) Math.min(distance, Point.distance(human.getX(), human.getY(), out.getX(), out.getY()));
			}
		}
		return distance;
	}

	@Override
	public void precompute() {

	}

	@Override
	public void act() throws SOSActionException {
		log.debug("acting as fastOpeningAgentFromFire => " + target);
		if (!isTargetStillValid()) {
			log.debug("agent skiped because it ");
			doneAgent.add(target);
			target = null;
			return;
		}
		if (target != null) {
			if (isDoneToClearAgent()) {
				log.debug("agent open for near in fire and stuck in building");
				doneAgent.add(target);
				target = null;
				return;
			}
			makeReachableTo(target.getRealEntity());
		}
	}

	private boolean isTargetStillValid() {
		if (target.isDone())
			return false;
		Human human = (Human) target.getRealEntity();

		if (human.isBuriednessDefined())
			if (human.getBuriedness() != 0) {
				log.info(human + " is skiped becuase it have buriedness");
				return false;
			}
		if (!human.isPositionDefined()) {
			log.info(human + " is skiped becuase position is undefine");
			return false;
		}
		if (human.getAreaPosition() instanceof Refuge)
			return false;
		if (!(human.getAreaPosition() instanceof Building))
			return false;
		if (doneAgent.contains(target))
			return false;
		return true;
	}

	private boolean isDoneToClearAgent() {
		if (isReachableTo(target.getRealEntity())) {
			log.info("agent in building is clear because isReachable method say its reachable ");
			return true;
		}
		if (agent.me().getAreaPosition().equals(target.getRealEntity())) {
			log.info("agent in building is clear because my area position is equal with it");
			return true;
		}
		if (target.getRealEntity().isReallyReachable(true)) {
			log.info("agent in building is clear because isReally Reachable method say its reachable ");
			return true;
		}
		return false;
	}

}

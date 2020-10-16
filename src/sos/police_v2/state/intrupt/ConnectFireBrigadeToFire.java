package sos.police_v2.state.intrupt;

import java.util.ArrayList;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.fire_v2.base.tools.FireStarZone;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.fire_v2.target.SOSSelectTarget.SelectStrategy;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.police_v2.state.preCompute.Task;
import sos.search_v2.tools.cluster.BuridBlockSearchCluster;

public class ConnectFireBrigadeToFire extends PoliceAbstractIntruptState {
	private Pair<Task<? extends StandardEntity>, Area> target = null;
	private PrecomputeState state;
	private SOSEstimatedFireZone selectedZone = null;
	private SOSFireZoneSelector fireSelector;

	public ConnectFireBrigadeToFire(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		state = policeForceAgent.getState(PrecomputeState.class);
		BuridBlockSearchCluster<FireBrigade> cluster = new BuridBlockSearchCluster<FireBrigade>(agent, model().fireBrigades());
		cluster.startClustering(agent.model());
		//		System.out.println(cluster.getClusterMap());
		fireSelector = new SOSFireZoneSelector(agent, cluster);

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
		if (target != null)
			return true;

		return false;
	}

	private void setTarget() {
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			SOSEstimatedFireZone zone = pair.second();
			if (zone.isReachableInInterupt)
				continue;
			if (zone.isDisable())
				continue;
			FireBrigade brigade = getFireBrigadeOfZone(zone);
			Area road = giveCenterRoadOfFireZone(zone);
			if (brigade == null || road == null)
				continue;
			Task<? extends StandardEntity> task = isFireBrigadeInMyTask(brigade);
			if (task != null) {
				log.debug("ACTING AS CONNECT FIRE TO FIRE BRIGADE = " + brigade + " to " + road);
				target = new Pair<Task<? extends StandardEntity>, Area>(task, road);
				selectedZone = zone;
				break;
			}
		}

	}

	private Task<? extends StandardEntity> isFireBrigadeInMyTask(FireBrigade brigade) {
		ArrayList<Task<? extends StandardEntity>> tasks = state.getTasksOf(model().getPoliceTasks(agent.me()));
		for (Task<? extends StandardEntity> task : tasks)
			if (task.getRealEntity().getID().equals(brigade.getID()))
				return task;
		return null;
	}

	private Area giveCenterRoadOfFireZone(SOSEstimatedFireZone fz) {
		for (Building b : fz.getOuter()) {
			for (ShapeInArea sh : b.fireSearchBuilding().sensibleAreasOfAreas()) {
				if (sh.getArea() instanceof Road)
					return sh.getArea();
			}
		}
		for (Building b : fz.getAllBuildings()) {
			for (ShapeInArea sh : b.fireSearchBuilding().sensibleAreasOfAreas()) {
				if (sh.getArea() instanceof Road)
					return sh.getArea();
			}
		}
		for (Building b : fz.getOuter()) {
			for (ShapeInArea sh : b.fireSearchBuilding().sensibleAreasOfAreas()) {
				return sh.getArea();
			}
		}
		for (Building b : fz.getAllBuildings()) {
			for (ShapeInArea sh : b.fireSearchBuilding().sensibleAreasOfAreas()) {
				return sh.getArea();
			}
		}
		log.error("Error !! couldnot find best road for reachable fire Zone " + fz);
		return null;
	}

	private FireBrigade getFireBrigadeOfZone(SOSEstimatedFireZone zone) {
		if (fireSelector.getStrategy() == SelectStrategy.NONE)
			return nearestFireBrigade(zone);
		try {
			for (Building b : zone.getAllBuildings()) {
				for (int i = 0; i < fireSelector.starCluster.getStarZones().length; i++) {
					FireStarZone fsz = fireSelector.starCluster.getStarZones()[i];
					if (fsz.getZoneBuildings().contains(b)) {
						return fireSelector.zone_FireBrigade.get(i).get(0);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nearestFireBrigade(zone);
	}

	private FireBrigade nearestFireBrigade(SOSEstimatedFireZone zone) {
		Integer min = Integer.MAX_VALUE;
		FireBrigade best = null;
		for (Building b : zone.getAllBuildings()) {
			for (FireBrigade f : agent.model().fireBrigades())
				if (min > b.distance(f)) {
					best = f;
				}
		}
		return best;
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void act() throws SOSActionException {
		log.debug("------Acting as connectFireBrigadeToFire");
		if (!target.first().isDone()) {
			makeReachableTo(target.first().getRealEntity());
			target.first().setDone(true);
		}
		else if (isDoneFireZoneClearing()) {
			log.debug("fire zone is opened in interupt");
			target = null;
			selectedZone.isReachableInInterupt = true;
			selectedZone = null;
			return;
		}
		makeReachableTo(target.second());
	}

	private boolean isDoneFireZoneClearing() {
		if (selectedZone.isDisable()) {
			log.info("fireZone is disabled so not need to connecting");
			return true;
		}
		if (isReachableTo(target.second())) {
			log.info("road is clear because isReachable method say its reachable ");
			return true;
		}
		if (agent.me().getAreaPosition().equals(target.second())) {
			log.info("road is clear because my area position is equal with it");
			return true;
		}
		if (target.second().isReallyReachable(true)) {
			log.info("road is clear because isReally Reachable method say its reachable ");
			return true;
		}
		return false;
	}
}

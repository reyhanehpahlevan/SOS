package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.PoliceForce;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.worldModel.SearchBuilding;

public class CheckProbabilityFire extends PoliceAbstractIntruptState {

	public ArrayList<Building> fireList;
	public ArrayList<Building> nearFireList;
	public SearchBuilding bestSearchBuilding;
	public Point2D avgOfBest;
	private Road target = null;
	private Road lastTarget = null;
	private PrecomputeState precomputeState;
	private int checkRang = 0;
	private int lastLock = -50;
	private int spentTime = 0;
	private SearchTask lastTask = null;

	public CheckProbabilityFire(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		precomputeState = agent.getState(PrecomputeState.class);
		checkRang = (int) (model().getDiagonalOfMap() / 5);
		log.warn("check rang for fire property= " + checkRang);
	}

	@Override
	public boolean canMakeIntrupt() {
		target = null;
		if (model().time() > lastLock + 20)
			spentTime = 0;
		if (model().time() < lastLock + 50) {
			log.debug("still in lock state");
			return false;
		}
		//		if (precomputeState.isDone)
		//			return false;// bekhater e inke fire probablity kheili moheme
		if (lastTarget != null)
			return true;
		fireList = agent.fireProbabilityChecker.getProbabilisticFieryBuilding();
		if (fireList.size() == 0) {
			return false;
		}
		//		setTarget();
		nearFireList = new ArrayList<Building>();
		if (agent.time() > 50) {
			for (Building select : fireList) {
				if (Point.distance(model().searchWorldModel.getClusterData().getX(), model().searchWorldModel.getClusterData().getY(), select.getX(), select.getY()) <= checkRang) {
					nearFireList.add(select);
				}
			}
		} else {
			for (Building select : fireList) {
				if (Point.distance(agent.me().getX(), agent.me().getY(), select.getX(), select.getY()) <= checkRang) {
					nearFireList.add(select);
				}
			}
		}
		if (nearFireList.size() > 0)
			return true;
		return false;
	}

	private void setTarget() {
		nearFireList = new ArrayList<Building>();
		long avgX = 0;
		long avgY = 0;
		for (Building select : fireList) {
			if (Point.distance(select.x(), select.y(), agent.me().getX(), agent.me().getY()) <= checkRang) {
				nearFireList.add(select);
				avgX += select.getX();
				avgY += select.getY();
			}
		}
		if (nearFireList.size() == 0) {
			log.debug("nearFireList size is zero");
			return;
		}
		avgX = avgX / nearFireList.size();
		avgY = avgY / nearFireList.size();
		log.debug("near to me = " + nearFireList + "  avg=" + avgX + "----" + avgY);
		ArrayList<Road> roads = new ArrayList<Road>(model().getObjectsInRange((int) avgX, (int) avgY, checkRang / 2, Road.class));
		if (roads.size() == 0) {
			log.debug("roads size to select center road is zero");
			return;
		}
		Road best = roads.get(0);
		int dis = Integer.MAX_VALUE;
		for (Road road : roads) {
			int temp = (int) Point.distance(road.getX(), road.getY(), avgX, avgY);
			if (dis > temp) {
				best = road;
				dis = temp;
			}
		}
		log.debug("best is " + best);
		target = best;
		lastTarget = best;
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void act() throws SOSActionException {
		spentTime++;
		log.debug("acting as checkProbabilityFire");
		if (spentTime > 10) {
			lastLock = model().time();
			spentTime = 0;
			log.debug("go to lock state");
			lastTarget = null;
			return;
		}
		//		nearFireList
		//		ArrayList<Building> res =agent.fireProbabilityChecker.getProbabilisticFieryBuilding();
		if (nearFireList.isEmpty())
			return;

		SearchBuilding best = agent.newSearch.getSearchWorld().getSearchBuilding(nearFireList.get(0));

		for (Building b : nearFireList) {
			SearchBuilding sb = agent.newSearch.getSearchWorld().getSearchBuilding(b);
			if (sb.isSpecialForFire() > best.isSpecialForFire())
				best = sb;
		}
		bestSearchBuilding = best;
		//		SearchTask task = agent.newSearch.fireSearchTask();
		SearchTask task = new SearchTask(best.getRealBuilding().fireSearchBuilding().sensibleAreasOfAreas());
		//		if (!precomputeState.isDone) {
		if (agent.time() > 50) {
			if (amICheckTask(task))
				handleTask(task);
		} else {
			if (amICheckTask(task))
				handleTask(task);
		}
		//		} else {
		//			handleTask(task);
		//		}
		//		if (target != null)
		//			makeReachableTo(target);
		//		if (lastTarget != null)
		//			makeReachableTo(lastTarget);
		//		lastTarget = null;
	}

	private boolean amICheckTask(SearchTask task) {
		long x = 0, y = 0;
		short index = 0;
		for (ShapeInArea inArea : task.getArea()) {
			x += inArea.getCenterX();
			y += inArea.getCenterY();
			index++;
		}
		x /= index;
		y /= index;
		avgOfBest = new Point2D(x, y);
		ArrayList<Pair<PoliceForce, Integer>> agents = new ArrayList<Pair<PoliceForce, Integer>>(3);
		agents.add(new Pair<PoliceForce, Integer>(null, Integer.MAX_VALUE));
		agents.add(new Pair<PoliceForce, Integer>(null, Integer.MAX_VALUE));
		agents.add(new Pair<PoliceForce, Integer>(null, Integer.MAX_VALUE));
		for (PoliceForce force : model().policeForces()) {
			if (model().searchWorldModel.getClusterData(force).isCoverer())
				continue;
			int dis;
			if (agent.time() > 50) {
				dis = (int) Point.distance(model().searchWorldModel.getClusterData(force).getX(), model().searchWorldModel.getClusterData(force).getY(), x, y);
			} else {
				dis = (int) Point.distance(force.getX(), force.getY(), x, y);
			}
			if (dis < agents.get(2).second()) {
				agents.set(2, new Pair<PoliceForce, Integer>(force, dis));
				Collections.sort(agents, new Comparator<Pair<PoliceForce, Integer>>() {

					@Override
					public int compare(Pair<PoliceForce, Integer> o1, Pair<PoliceForce, Integer> o2) {
						return o1.second().compareTo(o2.second());
					}
				});
			}
		}
		for (Pair<PoliceForce, Integer> pair : agents) {
			if (pair.first() == null)
				continue;
			if (pair.first().getID().getValue() == agent.me().getID().getValue())
				return true;
		}
		log.debug("i am not in 3 police that is near too fire so i pass to them" + agents);
		return false;
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

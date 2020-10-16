package sos.police_v2.state;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.types.PoliceMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.fire_v2.target.Tools;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class UpdateClusterFireState extends PoliceAbstractState {
	HashSet<Building> clusterBuilding;
	ArrayList<SOSEstimatedFireZone> fireZonesInCluster;
	public ArrayList<Building> allSafe;
	public ArrayList<Building> checkedGard;
	public Building lastTarget = null;

	public UpdateClusterFireState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterBuilding = model().searchWorldModel.getClusterData().getBuildings();
		fireZonesInCluster = new ArrayList<SOSEstimatedFireZone>();
		allSafe = new ArrayList<Building>();
		checkedGard = new ArrayList<Building>();
	}

	@Override
	public void act() throws SOSActionException {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return;
			}
		}
		updateZonesInMyCluster();
		if (fireZonesInCluster.size() > 0) {
			updateAllOuter();
			lastTarget = null;
			if (allSafe.size() > 0) {
				Building select = getBest();
				lastTarget = select;
				if (select != null) {
					if (agent.messageSystem.type == Type.NoComunication)
						sentDataToFireAgent();
					moveToShape(select.fireSearchBuilding().sensibleAreasOfAreas());
				}
			}
		}

	}

	private void sentDataToFireAgent() throws SOSActionException {
		boolean isMustSent = false;
		ArrayList<Integer> maxs = new ArrayList<Integer>(3);
		maxs.add(Integer.MIN_VALUE);
		maxs.add(Integer.MIN_VALUE);
		maxs.add(Integer.MIN_VALUE);
		for (FireBrigade fireBrigade : model().fireBrigades()) {
			maxs.set(0, Math.max(maxs.get(0), fireBrigade.updatedtime()));
			Collections.sort(maxs);
		}
		log.debug("updated fireBrigade " + maxs);
		if (maxs.get(0) < agent.time() - 15) {
			log.debug("must go and update fire agents data");
			isMustSent = true;
		}
		if (isMustSent) {
			ArrayList<Pair<Area, Integer>> agents = new ArrayList<Pair<Area, Integer>>();
			agents.add(new Pair<Area, Integer>(null, Integer.MAX_VALUE));
			agents.add(new Pair<Area, Integer>(null, Integer.MAX_VALUE));
			agents.add(new Pair<Area, Integer>(null, Integer.MAX_VALUE));
			for (FireBrigade brigade : model().fireBrigades()) {
				if (brigade.updatedtime() >= agent.time() - 15)
					continue;
				if (brigade.getBuriedness() > 0)
					continue;
				if (brigade.getHP() == 0)
					continue;
				if (brigade.getPositionArea().updatedtime() >= agent.time() - 15)
					continue;
				int dis = (int) Point.distance(brigade.getX(), brigade.getY(), lastTarget.getX(), lastTarget.getY());
				if (dis < agents.get(2).second()) {
					agents.set(2, new Pair<Area, Integer>(brigade.getAreaPosition(), dis));
					Collections.sort(agents, new Comparator<Pair<Area, Integer>>() {

						@Override
						public int compare(Pair<Area, Integer> o1, Pair<Area, Integer> o2) {
							return o1.second().compareTo(o2.second());
						}
					});
				}
			}
			log.debug("best agents position that are selected are : " + agents);
			ArrayList<Area> areas = new ArrayList<Area>();
			for (Pair<Area, Integer> pair : agents) {
				if (pair.first() == null)
					continue;
				areas.add(pair.first());
			}
			if (areas.size() > 0)
				move(areas);
		}

	}

	private Building getNearest() {
		Building result = null;
		int distance = Integer.MAX_VALUE;
		for (Building building : allSafe) {
			if (checkedGard.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				checkedGard.add(building);
				continue;
			}
			int dis = (int) Point.distance(agent.me().getX(), agent.me().getY(), building.getX(), building.getY());
			if (distance > dis) {
				distance = dis;
				result = building;
			}
		}
		return result;
	}

	private Building getBest() {
		Building result = null;
		int maxPrioty = Integer.MIN_VALUE;
		short num = 0;
		for (Building building : allSafe) {
			if (checkedGard.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				checkedGard.add(building);
				continue;
			}
			num++;
		}
		short filterSize = (short) (((num * 25) / 100) + 1);
		if (filterSize == 0) {
			log.warn("to update cluster fire size 30 % aval shode 0");
			return null;
		}
		updatePrioty();
		//FILTER TO 30 % Best//
		ArrayList<Pair<Building, Integer>> selectedBuilding = new ArrayList<Pair<Building, Integer>>(filterSize);
		for (int i = 0; i < filterSize; i++) {
			selectedBuilding.add(new Pair<Building, Integer>(null, Integer.MIN_VALUE));
		}
		for (Building building : allSafe) {
			if (checkedGard.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				continue;
			}

			if (selectedBuilding.get(0).second() < building.priority()) {
				selectedBuilding.set(0, new Pair<Building, Integer>(building, building.priority()));
				Collections.sort(selectedBuilding, new Comparator<Pair<Building, Integer>>() {
					@Override
					public int compare(Pair<Building, Integer> o1, Pair<Building, Integer> o2) {
						return o1.second().compareTo(o2.second());
					}
				});
			}
		}
		log.debug("filtered building : " + selectedBuilding);
		updatePriotyByDistanceCluster();
		//////////////////////
		for (Pair<Building, Integer> pair : selectedBuilding) {
			if (pair.first() == null)
				continue;
			if (pair.first().priority() > maxPrioty) {
				maxPrioty = pair.first().priority();
				result = pair.first();
			}
		}
		return result;
	}

	private void updatePrioty() {
		float unburnedPercent = getClusterUnbrunedPercent();
		for (Building building : allSafe) {
			building.resetPriority();
		}
		for (Building building : allSafe) {
			try {
				if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
					EP_setPriorityForSpread(10000, building.getEstimator(), building);
				else
					EP_setPriorityForSpread(1000, building.getEstimator(), building);
			} catch (Exception e) {
				// TODO: handle exception
			}

			EP_setPriorityForBuildingNotInMapSideBuildings(building, 1000000);
			EX_E_setNegativePriorityForUpdatetime(building, 1000);
			E_setPriorityForUnburnedNeighbours(building, 100);
			E_setPriorityForCLusterGard(building, (int) (4000 * unburnedPercent));
			if (unburnedPercent < 0.1f) {
				EP_setPriorityForDistance(building, -200);
				EX_E_setPriorityForRandombld(building, 100);
			}
		}

	}

	private void updatePriotyByDistanceCluster() {
		float unburnedPercent = getClusterUnbrunedPercent();
		for (Building building : allSafe) {
			building.resetPriority();
		}
		for (Building building : allSafe) {
			EP_setPriorityForDistanceToCluster(building);
		}

	}

	private float getClusterUnbrunedPercent() {
		int result = 0;
		for (Building building : clusterBuilding)
			if (building.getFieryness() != 8)
				result++;
		return ((float) result) / clusterBuilding.size();
	}

	private boolean isTargetSeenRecently(Building building) {
		if (building.updatedtime() < 5)
			return false;
		if (building.updatedtime() > agent.time() - 5)
			return true;
		return false;
	}

	private void updateAllOuter() {
		allSafe.clear();
		for (SOSEstimatedFireZone fireZone : fireZonesInCluster)
			for (Building building : fireZone.getSafeBuilding()) {
				//				if (!clusterBuilding.contains(building))
				//					continue;
				if (checkedGard.contains(building))
					continue;
				if (isTargetSeenRecently(building)) {
					checkedGard.add(building);
					continue;
				}
				allSafe.add(building);
			}
	}

	private void updateZonesInMyCluster() {
		fireZonesInCluster.clear();
		FIREZONES: for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			SOSEstimatedFireZone zone = pair.second();
			if (!zone.isDisable()) {
				for (Building safe : zone.getSafeBuilding()) {
					if (clusterBuilding.contains(safe)) {
						if (!haveFireInNear(safe))
							continue;
						fireZonesInCluster.add(zone);
						continue FIREZONES;
					}
				}
			}
		}
	}

	private boolean haveFireInNear(Building building) {
		for (Building near : building.realNeighbors_Building())
			if (near.getFieryness() > 0 && near.getFieryness() < 4)
				return true;
		return false;
	}

	protected void EP_setPriorityForSpread(int priority, SOSEstimatedFireZone site, Building b) {
		int SPREAD_ANGLE = 60;

		//		if (((FireWorldModel) agent.model()).getInnerOfMap().contains(site.getCenterX(), site.getCenterY()))
		//			SPREAD_ANGLE = 120;
		//		else
		//			SPREAD_ANGLE = 90;

		double x1, y1;
		site.computeSpread();
		Pair<Double, Double> spread = site.spread;
		x1 = spread.first();
		y1 = spread.second();

		double length = Math.hypot(x1, y1);
		priority = (int) (priority * length);

		double a3 = Tools.getAngleBetweenTwoVector(x1, y1, b.getX() - site.getCenterX(), b.getY() - site.getCenterY());
		int x = (int) (Math.abs(a3) / 30d);
		log.info("Building=" + b + "\t zone=" + site + "\tX=" + x + "\ta3=" + a3);
		int coef = 1;
		if (Tools.isBigFire(site))
			coef = 2;
		if (site.getOuter().size() > 30)
		{
			coef = 30;
		}
		//		tablelog.addScore(b.toString(), "SPREAD X", x);
		//		tablelog.addColumn("SPREAD");
		if (!(a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30))
			addPriority(b, (coef * priority / (x + 1)), ("SPREAD"));
		else
			addPriority(b, 0, ("SPREAD"));

		//		if (a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30) {
		//			//addPriority(b, -2 * coef * priority, "FILTER_SPREAD");
		//		}
	}

	protected void EP_setPriorityForBuildingNotInMapSideBuildings(Building b, int priority) {

		if (agent.messageSystem.type == Type.NoComunication) {
			if (!b.isMapSide())
				addPriority(b, priority, "MAP_SIDE");
			return;
		}
		//		if()
		else if (!b.isMapSide() || b.getFireBuilding().buildingBlock().isFireNewInBuildingBlock())
			addPriority(b, priority, "MAP_SIDE");

	}

	private void addPriority(Building b, int priority, String Comment) {
		//		tablelog.addScore(b.toString(), Comment, priority);
		b.addPriority(priority, Comment);
	}

	protected void EX_E_setNegativePriorityForUpdatetime(Building b, int priority) {
		addPriority(b, priority * b.getFieryness() * (agent.time() - getNearMaXUpdate(b)), "updatetimeScore");
	}

	protected void EP_setPriorityForDistance(Building b, int priority) {
		addPriority(b, agent.move.getMovingTime(agent.move.getPathTo(Arrays.asList(b), PoliceMove.class)) * priority, "Distance");
	}

	protected void EP_setPriorityForDistanceToCluster(Building b) {
		addPriority(b, (int) (Integer.MAX_VALUE - Point.distance(model().searchWorldModel.getClusterData().getX(), model().searchWorldModel.getClusterData().getY(), b.getX(), b.getY())), "Distance");
	}

	protected void E_setPriorityForUnburnedNeighbours(Building b, int i) {
		int num = 0;
		for (Building n : b.realNeighbors_Building()) {
			if (n.virtualData[0].getFieryness() == 0 || n.virtualData[0].getFieryness() == 4) {
				num++;
			}
		}
		addPriority(b, num * i, "UnBurned Neighbours");

	}

	protected void E_setPriorityForCLusterGard(Building b, int i) {
		if (clusterBuilding.contains(b))
			addPriority(b, i, "Cluster Builing");
	}

	private void EX_E_setPriorityForRandombld(Building b, int i) {
		int Random = agent.model().me().getID().getValue() + b.getID().getValue();
		int x = Random % 13;
		addPriority(b, i * x, "Random Score");
	}

	private int getNearMaXUpdate(Building b) {
		int max = b.updatedtime();
		for (Building building : b.realNeighbors_Building())
			max = Math.max(max, building.updatedtime());
		return max;
	}

}
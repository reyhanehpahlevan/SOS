package sos.police_v2.state.afterShock;

import java.awt.Point;
import java.util.ArrayList;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.PoliceAbstractState;
import sos.police_v2.state.preCompute.ClusterMST;
import sos.search_v2.tools.cluster.ClusterData;

public class AfterShockClusterConnector extends PoliceAbstractState {

	public ClusterMST clusterMST;
	private Road startRoad = null;
	private Road endRoad = null;
	private boolean isDone = true;
	private int lastAfterShock = 0;
	private ArrayList<Road> roads;

	public AfterShockClusterConnector(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterMST = new ClusterMST(model(), agent);
		setCenterRoadOfCluster();
	}

	private void setCenterRoadOfCluster() {
		Pair<Integer, Integer> result = clusterMST.myEdge;
		if (result == null) {
			log.debug("edge connect nadare");
			return;
		}
		if (clusterMST.clusterDatas.get(result.first()).equals(model().searchWorldModel.getClusterData())) {
			startRoad = getRoadOfClusterData(clusterMST.clusterDatas.get(result.first()));
			endRoad = getRoadOfClusterData(clusterMST.clusterDatas.get(result.second()));
		} else if (clusterMST.clusterDatas.get(result.second()).equals(model().searchWorldModel.getClusterData())) {
			startRoad = getRoadOfClusterData(clusterMST.clusterDatas.get(result.second()));
			endRoad = getRoadOfClusterData(clusterMST.clusterDatas.get(result.first()));
		}
		log.warn("Cluster Connector for afterShock startRoad= " + startRoad + " endRoad= " + endRoad);
	}

	private Road getRoadOfClusterData(ClusterData clusterData) {
		Building building = clusterData.getNearestBuildingToCenter();
		ArrayList<Road> roads = new ArrayList<Road>(model().getObjectsInRange(building.getID(), (int) (model().getDiagonalOfMap() / 10), Road.class));
		if (roads.size() == 0)
			return null;
		Road best = roads.get(0);
		int dis = Integer.MAX_VALUE;
		for (Road road : roads) {
			int temp = (int) Point.distance(road.getX(), road.getY(), building.getX(), building.getY());
			if (temp < dis) {
				best = road;
				dis = temp;
			}
		}
		return best;

	}

	@Override
	public void act() throws SOSActionException {
		if (startRoad == null || endRoad == null) {
			log.debug("not assigned connecting road to me");
			return;
		}
		if (model().getLastAfterShockTime() != lastAfterShock) {
			isDone = false;
			lastAfterShock = model().getLastAfterShockTime();
			roads = new ArrayList<Road>();
			roads.add(endRoad);
			roads.add(startRoad);
		}
		if (!isDone) {
			log.debug("acting as aftershock cluster Connector");
			checkDone(roads);
			if (roads.size() > 0)
				makeReachableTo(roads);
			isDone = true;
		}

	}

	private void checkDone(ArrayList<Road> list) {
		ArrayList<Road> removeList = new ArrayList<Road>();
		for (Road entity : list) {
			if (isReachableTo(entity))
				removeList.add(entity);
		}
		list.removeAll(removeList);
	}
}

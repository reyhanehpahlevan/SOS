package sos.police_v2.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Civilian;
import sos.base.entities.VirtualCivilian;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.search_v2.tools.cluster.ClusterData;

public class LowComOpenCivilianState extends PoliceAbstractState {
	ArrayList<VirtualCivilian> validCivilians;
	ArrayList<Area> isDone;
	VirtualCivilian target;
	private HashMap<ClusterData, ArrayList<VirtualCivilian>> clustersCivilians;
	private ArrayList<ClusterData> neighberClusters;
	private ArrayList<ClusterData> helpClusters;
	private int lastAfterShock = 0;

	public LowComOpenCivilianState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		validCivilians = new ArrayList<VirtualCivilian>();
		isDone = new ArrayList<Area>();
		clustersCivilians = new HashMap<ClusterData, ArrayList<VirtualCivilian>>();
		neighberClusters = PoliceUtils.getNeighberCluster(model(), model().searchWorldModel.getClusterData());
		helpClusters = new ArrayList<ClusterData>();
	}

	@Override
	public void act() throws SOSActionException {
		log.debug("acting as:" + this + " vir size=" + model().getVirtualCivilians().size());
		log.debug("assigned cilvilian=" + target);
		if (model().getLastAfterShockTime() != lastAfterShock) {
			isDone.clear();
			lastAfterShock = model().getLastAfterShockTime();
		}
		if (target != null)
			log.debug("mission com =" + missionComplete(target));
		if (target != null && missionComplete(target)) {
			log.info("missoin complete!!" + target + " opened!");
			isDone.add(target.getPosition());
			target = null;
		}
		if (!PoliceUtils.isValidVirtualCivilian(target, agent, true)) {
			if (target != null)
				isDone.add(target.getPosition());
			target = assignNewCivilian();
		}
		log.debug("current assigned vir civilian is:" + target);
		if (target != null) {
			Pair<Area, Point2D> ep = getEntrancePoint(target.getPosition());
			if (ep == null) {
				log.error("how?????");
				target = null;
				return;
			}
			moveToPoint(ep);//TODO TOO BUILDING MAGHSADESH NABASHE;)
		}

	}

	private VirtualCivilian assignNewCivilian() {
		helpClusters.clear();
		helpClusters.addAll(neighberClusters);
		//		ClusterData myCluster = agent.model().searchWorldModel.getClusterData();
		//		double myCLusterScore = agent.newSearch.getRemainingJobScorer().remainingJobScore(myCluster);

		ArrayList<ClusterData> openCivilianClusterList = makeListOfValidClusters();
		for (ClusterData cluster : openCivilianClusterList)
			validCivilians.addAll(clustersCivilians.get(cluster));
		log.info("all valid vir civilian for me= " + validCivilians);
		return getBestUnreachableCivilians(validCivilians);
	}

	private boolean missionComplete(VirtualCivilian select) {
		if (agent.me().getAreaPosition().equals(target.getPosition())) {
			log.info("vir civilian is reachabale because we are in same position");
			return true;
		}
		if (select.isReallyReachable()) {
			log.info("vir civlian is reachable because isReallyReachable say that");
			return true;
		}
		if (agent.isReachableTo(select.getPosition().getPositionPair())) {
			log.info("vir civlian is reachable because position is reachaable ..");
			return true;
		}
		if (select.getPosition().isReallyReachable(true)) {
			log.info("vir civlian is reachable because position is reachaable ..........");
			return true;
		}
		return false;
	}

	private VirtualCivilian getBestUnreachableCivilians(Collection<VirtualCivilian> cives) {
		log.info("getBestUnreachableCivilians");
		VirtualCivilian best = null;
		DistanceComparator comparator = new DistanceComparator();
		for (VirtualCivilian civilian : cives) {
			if (PoliceUtils.isValidVirtualCivilian(civilian, agent, true)) {
				if (isDone.contains(civilian.getPosition()))
					continue;
				if (best == null || comparator.compare(best, civilian) < 0)
					best = civilian;
			}

		}
		log.debug("best is =" + best);
		return best;
	}

	private class DistanceComparator implements java.util.Comparator<VirtualCivilian> {
		HashMap<VirtualCivilian, Integer> civ_cost = new HashMap<VirtualCivilian, Integer>();

		@Override
		public int compare(VirtualCivilian c1, VirtualCivilian c2) {
			Integer d1 = civ_cost.get(c1);
			if (d1 == null) {
				d1 = (int) (PoliceUtils.getDistance(agent.me(), c2.getPosition()) * (Math.random() * 25 + 75) / 100);
				civ_cost.put(c1, d1);
			}
			Integer d2 = civ_cost.get(c2);
			if (d2 == null) {
				d2 = (int) (PoliceUtils.getDistance(agent.me(), c2.getPosition()) * (Math.random() * 25 + 75) / 100);
				civ_cost.put(c2, d2);
			}
			if (d2 < d1)
				return -1;
			if (d2 > d1)
				return 1;
			return 0;
		}
	}

	private ArrayList<ClusterData> makeListOfValidClusters() {
		ArrayList<ClusterData> result = new ArrayList<ClusterData>();
		ClusterData mycluster = model().searchWorldModel.getClusterData();
		result.add(mycluster);
		clustersCivilians.clear();
		clustersCivilians.put(mycluster, new ArrayList<VirtualCivilian>());
		for (ClusterData cluster : helpClusters)
			clustersCivilians.put(cluster, new ArrayList<VirtualCivilian>());
		for (VirtualCivilian civilian : model().getVirtualCivilians()) {
			if (!PoliceUtils.isValidVirtualCivilian(civilian, agent, true))
				continue;
			if (isDone.contains(civilian.getPosition()))
				continue;
			if (mycluster.getBuildings().contains(civilian.getPosition())) {
				clustersCivilians.get(mycluster).add(civilian);
				continue;
			}
			for (ClusterData cluster : helpClusters) {
				if (cluster.getBuildings().contains(civilian.getPosition()))
					clustersCivilians.get(cluster).add(civilian);
			}
		}
		for (Civilian real : model().civilians()) {
			if (!PoliceUtils.isValidCivilian(real, agent, true))
				continue;
			if (isDone.contains(real.getAreaPosition()))
				continue;
			VirtualCivilian civilian = new VirtualCivilian(real.getPositionArea(), real.getBuriedness(), real.getRescueInfo().getDeathTime(), real.isReallyReachable(true));
			if (mycluster.getBuildings().contains(civilian.getPosition())) {
				clustersCivilians.get(mycluster).add(civilian);
				continue;
			}
			for (ClusterData cluster : helpClusters) {
				if (cluster.getBuildings().contains(civilian.getPosition()))
					clustersCivilians.get(cluster).add(civilian);
			}
		}
		log.debug("clustere khodam ------->" + clustersCivilians.get(model().searchWorldModel.getClusterData()).size());
		for (ClusterData cluster : helpClusters) {
			log.debug("cluster hamsaye ----> " + clustersCivilians.get(cluster).size());
			if (clustersCivilians.get(cluster).size() > 4)
				result.add(cluster);
		}
		return result;
	}
}

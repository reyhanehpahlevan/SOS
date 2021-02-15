package sos.search_v2.searchType;

import java.util.ArrayList;

import sos.base.SOSAgent;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.util.SOSGeometryTools;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.fire_v2.target.SOSFireZoneSelector.Task;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

public class BlockSearchNoComm<E extends Human> extends BlockSearch<E> {

	private int current = 0;
	private int mySearchClusterIndex = 0;
	public ArrayList<ClusterData> subStarClusters = null;

	public BlockSearchNoComm(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
		if (!(me instanceof FireBrigadeAgent))
			return;

	}

	int addNumberCheckCluster = 1;

	@Override
	public SearchTask searchTask() {
		if (!(me instanceof FireBrigadeAgent))
			return super.searchTask();
		precompute();

			
		
		
		if (hasBlock()||me.time()<40)
			return super.searchTask();

		for (int i = 1; i < subStarClusters.size(); i++) {
			me.sosLogger.search.info("mysubcluster has no block==> changing sub cluster:" + i + " ");

			current = (current + addNumberCheckCluster+subStarClusters.size()) % subStarClusters.size();
			if (current == mySearchClusterIndex) {
				addNumberCheckCluster *= -1;
			}

			setRegionCluster(getCurrentCluster());
			if (hasBlock())
				return super.searchTask();
		}
		me.sosLogger.search.info("no subcluster has block==>returning null");
		return null;
	}

	@Override
	public boolean hasBlock() {
		//		if(addNumberCheckCluster==1)
		if (getCheckedRegion().size() / (float) getCurrentCluster().getBuildings().size() > 0.9f)
			return false;
		return super.hasBlock();
	}

	private void setRegionCluster(ClusterData currentCluster) {
		me.sosLogger.search.info("set current cluster =" + currentCluster.getIndex());
		setRegion(currentCluster.getBuildings());
	}

	private void precompute() {
		if (this.subStarClusters != null)
			return;
		ArrayList<ClusterData> tmpstarClusters = new ArrayList<ClusterData>();
		SOSFireZoneSelector firezoneSelector = ((FireBrigadeAgent) me).FDK.getInfoModel().getFireZoneSelector();
		Task myTask = firezoneSelector.fireBrigade_Task.get(me.me());
		ArrayList<FireBrigade> zoneFirebrigades = firezoneSelector.zone_FireBrigade.get(myTask.getZoneIndex());
		for (int i = 0; i < zoneFirebrigades.size(); i++) {
			FireBrigade fireBrigade = zoneFirebrigades.get(i);
			tmpstarClusters.add(searchWorld.getClusterData(fireBrigade));
		}
		sortAndSet(tmpstarClusters);
		for (int i = 0; i < this.subStarClusters.size(); i++) {
			if (this.subStarClusters.get(i).equals(myClusterData)) {
				mySearchClusterIndex = i;
				break;
			}
		}

		current = mySearchClusterIndex;
		setRegionCluster(getCurrentCluster());

	}

	private void sortAndSet(ArrayList<ClusterData> clusters) {
		ClusterData cl = clusters.get(0);
		subStarClusters = new ArrayList<ClusterData>();
		subStarClusters.add(cl);
		clusters.remove(cl);
		while (!clusters.isEmpty()) {
			cl = getNearestCluster(cl, clusters);
			subStarClusters.add(cl);
			clusters.remove(cl);
		}
	}

	private ClusterData getNearestCluster(ClusterData cl, ArrayList<ClusterData> clusters) {
		double nearestDis = Integer.MAX_VALUE;
		ClusterData nearestCluster = null;
		for (ClusterData cluster : clusters) {
			double dist = SOSGeometryTools.distance(cluster.getX(), cluster.getY(), cl.getX(), cl.getY());
			if (dist < nearestDis) {
				nearestCluster = cluster;
				nearestDis = dist;
			}
		}
		return nearestCluster;
	}

	public ClusterData getCurrentCluster() {
		return subStarClusters.get(current);
	}
}

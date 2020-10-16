package sos.ambulance.States;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.entities.Building;
import sos.base.entities.Refuge;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.worldModel.SearchBuilding;

public class LowCommunicationSearchState extends AmbulanceGeneralState {

	public LowCommunicationSearchState(AmbulanceTeamAgent ownerAgent) {
		super(ownerAgent);
	}

	@Override
	public boolean finished() {
		return isDone;
	}

	@Override
	public void resetState() {

	}

	@Override
	public void act() throws SOSActionException {
		if (self.location() instanceof Refuge && self.isFull())
			self.unload();
		if (self.messageSystem.type == Type.LowComunication) {
			self.fireSearch();
			ArrayList<ClusterData> checked = new ArrayList<ClusterData>();
			self.log().info("checking low Communication Search!!!");
			int maximumAllowedClustersToSearch = (int) (Math.floor(self.time() / 25d) + 1);
			self.log().debug("maximumAllowedClustersToSearch " + maximumAllowedClustersToSearch);
			for (int i = 1; i <= Math.min(maximumAllowedClustersToSearch, self.model().searchWorldModel.getAllClusters().size()); i++) {
				ArrayList<ClusterData> clusters = getNearestClusters(i);
				clusters.removeAll(checked);
				self.log().debug("nearest (" + i + ") clusters to my cluster that has not been checked are:" + clusters);
				Collections.sort(clusters, new Comparator<ClusterData>() {

					@Override
					public int compare(ClusterData o1, ClusterData o2) {
						double o1s = SOSGeometryTools.distance(self.me().getX(), self.me().getY(), o1.getX(), o1.getY());
						double o2s = SOSGeometryTools.distance(self.me().getX(), self.me().getY(), o2.getX(), o2.getY());
						if (o1s > o2s)
							return 1;
						if (o1s < o2s)
							return -1;
						return 0;
					}
				});
				self.log().debug("sorted by current distance are:" + clusters);
				for (ClusterData clusterData : clusters) {
					viewInClusterCivilian(clusterData);
					checked.add(clusterData);
				}
			}
		}
		
	}

	private void viewInClusterCivilian(ClusterData clusterData) throws SOSActionException {
		final ClusterData myCluster = self.model().searchWorldModel.getClusterData();
		double distanceToMyCluster = SOSGeometryTools.distance(clusterData.getX(), clusterData.getY(), myCluster.getX(), myCluster.getY());
		double distanceCurrentPosition = SOSGeometryTools.distance(self.me().getX(), self.me().getY(), myCluster.getX(), myCluster.getY());
		self.log().trace("viewInClusterCivilian " + clusterData + " distance to my cluster:" + distanceToMyCluster + " distance to current position" + distanceCurrentPosition);

		Building best = null;
		long bestmove = Long.MAX_VALUE;
		for (Building b : clusterData.getBuildings()) {
			SearchBuilding s = self.model().searchWorldModel.getSearchBuilding(b);
			//			if (s.getRealBuilding().getLastSenseTime() > 3)
			//				continue;
			if (!s.isHasBeenSeenBySelf()) {
				for (ShapeInArea sa : b.getSearchAreas()) {
					if (sa.contains(self.me().getPositionPoint().toGeomPoint())) {
						b.setSearchedForCivilian(self.time());
						s.setHasBeenSeenBySelf(true);
						self.log().warn("Why has been seen didn't set till here?");
						break;
					}
				}
			}
			if (s.isHasBeenSeenBySelf()) {
				s.addScore("HasBeenSeenBySelf", -100);
				continue;
			}
			if (s.getValidCivilianCountInLowCom() == 0) {
				s.addScore("NoValidCivilianCount", -1000);
				continue;
			}
			if (s.isReallyUnReachableInLowCom(true)) {
				s.addScore("ReallyUnReachable", -10000);
				continue;
			}

			long tmpMoveScore = self.move.getWeightToLowProcess(s.getRealBuilding().getSearchAreas(), StandardMove.class);
			if (best == null || bestmove > tmpMoveScore) {
				best = s.getRealBuilding();
				bestmove = tmpMoveScore;
			}
		}
		if (best != null && !self.move.isReallyUnreachable(best)) {
			self.log().info("Acting as low Communication Search!!! " + best);
			self.move.moveToShape(best.getSearchAreas(), StandardMove.class);
		}
		self.log().info("No Valuable Building To Search!!! ");
	}

}

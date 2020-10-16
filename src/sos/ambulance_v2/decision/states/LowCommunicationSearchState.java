package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.entities.Building;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.worldModel.SearchBuilding;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

/**
 * @author Reyhaneh
 */

public class LowCommunicationSearchState extends SOSIState {
	
	private AmbulanceTeamAgent ambulance =null;

	public LowCommunicationSearchState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {
		
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ LowCommunicationSearchState $$$$$$$$$$$$$$$$$$$$$$$$$");
		ambulance.lastState = " LowCommunicationSearchState ";	

		if (infoModel.getAgent().messageSystem.type != Type.LowComunication)
			return null;
		
		ambulance.newSearch.fireSearchTask();
		ArrayList<ClusterData> checked = new ArrayList<ClusterData>();
		infoModel.getLog().info("checking low Communication Search!!!");
		int maximumAllowedClustersToSearch = (int) (Math.floor(infoModel.getTime() / 25d) + 1);
		infoModel.getLog().debug("maximumAllowedClustersToSearch " + maximumAllowedClustersToSearch);
		for (int i = 1; i <= Math.min(maximumAllowedClustersToSearch, infoModel.getModel().searchWorldModel.getAllClusters().size()); i++) {
			ArrayList<ClusterData> clusters = getNearestClusters(i);
			clusters.removeAll(checked);
			infoModel.getLog().debug("nearest (" + i + ") clusters to my cluster that has not been checked are:" + clusters);
			Collections.sort(clusters, new Comparator<ClusterData>(){
				@Override
				public int compare(ClusterData o1, ClusterData o2) {
					double o1s = SOSGeometryTools.distance(infoModel.getEntity().getPositionPoint().getX(),infoModel.getEntity().getPositionPoint().getY(), o1.getX(), o1.getY());
					double o2s = SOSGeometryTools.distance(infoModel.getEntity().getPositionPoint().getX(), infoModel.getEntity().getPositionPoint().getY(), o2.getX(), o2.getY());
					if (o1s > o2s)
						return 1; //o2s
					if (o1s < o2s)
						return -1; //o1s
					return 0;
				}
			});
			infoModel.getLog().debug("sorted by current distance are:" + clusters);
			for (ClusterData clusterData : clusters) {
				viewInClusterCivilian(clusterData);
				checked.add(clusterData);
			}
		}
		return null;
	}

	public ArrayList<ClusterData> getNearestClusters(int number){
		ArrayList<ClusterData> nearestCluster=new ArrayList<ClusterData>();
		final ClusterData myCluster =infoModel.getModel().searchWorldModel.getClusterData();

		ArrayList<ClusterData> cds = new ArrayList<ClusterData>(infoModel.getModel().searchWorldModel.getAllClusters());
		Collections.sort(cds, new Comparator<ClusterData>() {

			@Override
			public int compare(ClusterData o1, ClusterData o2) {
				double o1s = SOSGeometryTools.distance(myCluster.getX(),myCluster.getY(),o1.getX(),o1.getY());
				double o2s = SOSGeometryTools.distance(myCluster.getX(),myCluster.getY(),o2.getX(),o2.getY());
				if (o1s>o2s )
					return 1;
				if ( o1s<o2s)
					return -1;
				return 0;
			}
		});
		int i = 0;

		for (ClusterData cd : cds) {
			if (i >= number)
				break;
			i++;
			nearestCluster.add(cd);
		}
		return nearestCluster;

	}
	
	
	private void viewInClusterCivilian(ClusterData clusterData) throws SOSActionException  {
		final ClusterData myCluster = infoModel.getModel().searchWorldModel.getClusterData();
		double distanceToMyCluster = SOSGeometryTools.distance(clusterData.getX(), clusterData.getY(), myCluster.getX(), myCluster.getY());
		double distanceCurrentPosition = SOSGeometryTools.distance(infoModel.getATEntity().getX(), infoModel.getATEntity().getY(), myCluster.getX(), myCluster.getY());
		infoModel.getLog().trace("viewInClusterCivilian " + clusterData + " distance to my cluster:" + distanceToMyCluster + " distance to current position" + distanceCurrentPosition);

		Building best = null;
		long bestmove = Long.MAX_VALUE;
		for (Building b : clusterData.getBuildings()) {
			SearchBuilding s = infoModel.getModel().searchWorldModel.getSearchBuilding(b);
			//			if (s.getRealBuilding().getLastSenseTime() > 3)
			//				continue;
			if (!s.isHasBeenSeenBySelf()) {
				for (ShapeInArea sa : b.getSearchAreas()) {
					if (sa.contains(infoModel.getEntity().getPositionPoint().toGeomPoint())) {
						b.setSearchedForCivilian(infoModel.getTime());
						s.setHasBeenSeenBySelf(true);
						infoModel.getLog().warn("Why has been seen didn't set till here?");
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

			long tmpMoveScore = ambulance.move.getWeightToLowProcess(s.getRealBuilding().getSearchAreas(), StandardMove.class);
			if (best == null || bestmove > tmpMoveScore) {
				best = s.getRealBuilding();
				bestmove = tmpMoveScore;
			}
		}
		if (best != null && !ambulance.move.isReallyUnreachable(best)) {
			infoModel.getLog().info("Acting as low Communication Search!!! " + best);
			ambulance.move.moveToShape(best.getSearchAreas(), StandardMove.class);
		}
		infoModel.getLog().info("No Valuable Building To Search!!! ");
	}
	
	
	@Override
	public void skipped() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void overTaken() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
		
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void giveFeedbacks(List feedbacks) {
		// TODO Auto-generated method stub
		
	}

	

}
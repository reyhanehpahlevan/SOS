package sos.police_v2.state.preCompute;

import java.util.ArrayList;

import sos.ambulance_v2.tools.CostFromInMM;
import sos.base.SOSWorldModel;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.PoliceGraph;
import sos.search_v2.tools.cluster.ClusterData;

public class ClusterGraph extends PoliceGraph {
	public ArrayList<ClusterData> clusterDatas;
	PoliceForceAgent agent;
	CostFromInMM[] costTable;

	public ClusterGraph(SOSWorldModel model, PoliceForceAgent agent) {
		super(model.searchWorldModel.getAllClusters().size());
		clusterDatas = new ArrayList<ClusterData>(model.searchWorldModel.getAllClusters());
		this.agent = agent;
		costTable = new CostFromInMM[clusterDatas.size()];
		short index = 0;
		for (ClusterData clusterData : clusterDatas) {
			costTable[index] = new CostFromInMM(model, clusterData.getNearestBuildingToCenter());
			index++;
		}

	}

	@Override
	public short[] getConnectedNodesOf(short srcIndex) {
		short[] nodes = new short[clusterDatas.size()];
		for (short i = 0; i < clusterDatas.size(); i++) {
			nodes[i] = i;
		}
		return nodes;
	}

	@Override
	public double weight(int source, int destination) {
		if (source == destination)
			return Double.MAX_VALUE;
		return costTable[source].getCostTo(clusterDatas.get(destination).getNearestBuildingToCenter());

	}

	@Override
	public short[] getEdgesOf(short srcIndex) {
		short[] nodes = new short[clusterDatas.size()];
		for (short i = 0; i < clusterDatas.size(); i++) {
			nodes[i] = i;
		}
		return nodes;
	}

	@Override
	public short edgeIndexBetween(int u, int v) {
		System.err.println("omg ridam inja chi kar dare akhe");
		return 0;
	}

}

package sos.police_v2.state.preCompute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import rescuecore2.misc.Pair;
import sos.base.SOSWorldModel;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.MST;

public class ClusterMST {
	ArrayList<Pair<Integer, Integer>> result;
	short[] indexTable;
	public ArrayList<ClusterData> clusterDatas;
	private ClusterData myCluster;
	public Pair<Integer, Integer> myEdge;
	ClusterData nearCluster = null;

	public ClusterMST(SOSWorldModel model, PoliceForceAgent agent) {
		myCluster = model.searchWorldModel.getClusterData();
		ClusterGraph clusterGraph = new ClusterGraph(model, agent);
		clusterDatas = clusterGraph.clusterDatas;
		MST mst = new MST(clusterGraph.getNumberOfNodes(), model);
		try {
			mst.Run(clusterGraph, Arrays.asList(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result = mst.getMST();
		myEdge = findMyEdgeToClear();
		if (myEdge != null)
			nearCluster = clusterDatas.get(0);
	}

	private Pair<Integer, Integer> findMyEdgeToClear() {
		Pair<Integer, Integer> temp;
		indexTable = new short[clusterDatas.size()];
		ArrayList<Pair<Integer, Integer>> tempResult = (ArrayList<Pair<Integer, Integer>>) result.clone();
		while (tempResult.size() > 0) {
			clearIndexTable();
			for (Pair<Integer, Integer> p : tempResult) {
				indexTable[p.first()]++;
				indexTable[p.second()]++;
			}
			for (short i = 0; i < indexTable.length; i++) {
				if (indexTable[i] == 1) {
					temp = removeEdghFormListByIndexSrc(tempResult, i);
					if (temp != null) {
						if (clusterDatas.get(temp.first()).equals(myCluster) || clusterDatas.get(temp.second()).equals(myCluster))
							return temp;
					}
				}
			}
		}
		return null;
	}

	private Pair<Integer, Integer> removeEdghFormListByIndexSrc(ArrayList<Pair<Integer, Integer>> list, short srcIndex) {
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Pair<Integer, Integer> pair = (Pair<Integer, Integer>) iterator.next();
			if (pair.first() == srcIndex || pair.second() == srcIndex) {
				iterator.remove();
				return pair;
			}
		}
		return null;
	}

	private void clearIndexTable() {
		for (int i = 0; i < indexTable.length; i++)
			indexTable[i] = 0;
	}

	public ArrayList<Pair<Integer, Integer>> getResult() {
		return result;
	}

}

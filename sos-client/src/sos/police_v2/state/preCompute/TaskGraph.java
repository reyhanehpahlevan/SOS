package sos.police_v2.state.preCompute;

import java.util.ArrayList;

import sos.base.entities.StandardEntity;
import sos.base.util.sosLogger.SOSLogger;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.PoliceGraph;

public class TaskGraph extends PoliceGraph {

	private final short n;
	private final ArrayList<Task<? extends StandardEntity>> tasks;
	PoliceForceAgent agent;
	private final WorldTaskDijkstra worldTaskDijkstra;

	public TaskGraph(ArrayList<Task<? extends StandardEntity>> tasks2, PoliceForceAgent agent, WorldTaskDijkstra worldTaskDijkstra) {
		super(tasks2.size());
		this.agent = agent;

		this.tasks = tasks2;
		this.worldTaskDijkstra = worldTaskDijkstra;
		this.n = (short) tasks2.size();

	}

	@Override
	public short[] getConnectedNodesOf(short srcIndex) {
		short[] nodes = new short[n];
		for (short i = 0; i < nodes.length; i++) {
			nodes[i] = i;
		}
		return nodes;
	}

	@Override
	public double weight(int source, int destination) {

		long dijkstraWeight = worldTaskDijkstra.getDijkstraWeight(tasks.get(source), tasks.get(destination));
		//		long dijkstraWeight = PoliceUtils.getDistance(tasks.get(source), tasks.get(destination));
		long resultCost = dijkstraWeight + tasks.get(source).getWeight();
		//				log().trace("weight from(src) task " + tasks.get(source).task + " to des task = " + tasks.get(destination).task + " == " + dijkstraWeight + " tasks.get(src).getWeight: " + tasks.get(source).getWeight() + " resultcost=" + resultCost);

		return Math.max(resultCost, 0);

	}

	public void changePosition(PoliceForceTask from, Task<? extends StandardEntity> to) {
		long dijkstraWeight = worldTaskDijkstra.getDijkstraWeight(from, to);
		from.setTempPosition(to.getPositionPair());
		from.addJobDone(dijkstraWeight, false);
		if (to instanceof PoliceForceTask)
			((PoliceForceTask) to).addJobDone(from.getJobDone(), false);

		worldTaskDijkstra.changePosition(from, to);

	}

	public SOSLogger log() {
		return tasks.get(0).getRealEntity().getAgent().sosLogger;
	}

	@Override
	//foraramik
	@Deprecated
	public short edgeIndexBetween(int u, int v) {
		System.err.println("notImplimented");
		return 0;
	}

	@Override
	//foraramik
	@Deprecated
	public short[] getEdgesOf(short srcIndex) {
		System.err.println("notImplimented");
		return null;
	}

}

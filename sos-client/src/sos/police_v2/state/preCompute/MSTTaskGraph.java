package sos.police_v2.state.preCompute;

import java.util.ArrayList;

import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.base.PoliceGraph;

public class MSTTaskGraph extends PoliceGraph {

	private final ArrayList<Task<? extends StandardEntity>> tasks;
	private final ArrayList<PoliceForceTask> policeTasks;
	private final WorldTaskDijkstra worldTaskDijkstra;
	private final PoliceForceAgent agent;


	public MSTTaskGraph(PoliceForceAgent agent,int n, ArrayList<PoliceForceTask> arrayList, ArrayList<Task<? extends StandardEntity>> tasks2, WorldTaskDijkstra worldTaskDijkstra) {
		super(n);
		this.agent = agent;
		this.policeTasks = arrayList;
		this.tasks = tasks2;
		this.worldTaskDijkstra = worldTaskDijkstra;
		
	}

	@Override
	public short[] getConnectedNodesOf(short srcIndex) {
		if (tasks.get(srcIndex).getRealEntity() instanceof PoliceForce) {
			short[] cn = new short[policeTasks.size()];
			for (int i = 0; i < cn.length; i++) {
				cn[i] = policeTasks.get(i).getIndex();
			}
			return cn;
		} else
			return new short[0];
	}

	@Override
	public double weight(int source, int destination) {
		int minDistance = Integer.MAX_VALUE;
		Task<? extends StandardEntity> sourceTask = tasks.get(source);
		Task<? extends StandardEntity> destinationTask = tasks.get(destination);
		ArrayList<Task<? extends StandardEntity>> allofDstTask = agent.getState(PrecomputeState.class).agent_task.get(destinationTask);
		for (Task<?> mytask :allofDstTask ) {
			minDistance = Math.min(minDistance, PoliceUtils.getDistance(mytask, sourceTask));

		}
		
		ArrayList<Task<? extends StandardEntity>> allofSrcTask = agent.getState(PrecomputeState.class).agent_task.get(sourceTask);
		for (Task<?> mytask : allofSrcTask) {
			if (mytask.getRealEntity().equals(destinationTask.getRealEntity())) {
				sourceTask.getRealEntity().getAgent().sosLogger.agent.trace("weight from " + tasks.get(source) + "to " + tasks.get(destination) + ":"+0+"because it is in its task");
			}
		}
		
//		int w = minDistance;//+tasks.get(source).jobDone;
		int w=Math.min(minDistance,PoliceUtils.getDistance(destinationTask.getRealEntity(), sourceTask.getRealEntity()));//+sourceTask.jobDone;
		tasks.get(0).getRealEntity().getAgent().sosLogger.agent.trace("weight from " + tasks.get(source) + "to " + tasks.get(destination) + ":", w);
		if(w<0){
			tasks.get(0).getRealEntity().getAgent().sosLogger.agent.error("weight from " + tasks.get(source) + "to " + tasks.get(destination) + ":"+ w);
		}
		return w;
	}

	@Override
	public short[] getEdgesOf(short srcIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short edgeIndexBetween(int u, int v) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void updateWeight(int node, short childIndex) {
		if(tasks.get(node) instanceof PoliceForceTask)
		changePosition((PoliceForceTask) tasks.get(node),tasks.get(childIndex));
		else
			System.err.println("task not police-----------????????????");
	}

	private void changePosition(PoliceForceTask from, Task<? extends StandardEntity> to) {
		
			long dijkstraWeight = worldTaskDijkstra.getDijkstraWeight(from, to);
			from.setTempPosition(to.getPositionPair());
			from.addJobDone(dijkstraWeight,true);
		
		worldTaskDijkstra.changePosition(from,to);

	}

}

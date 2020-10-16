package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.police_v2.state.preCompute.Task;
import sos.police_v2.state.preCompute.WorldTaskDijkstra;

public class SelectedPrecomputeV3 extends SOSAbstractSelectedComponent<PoliceForce> {
	WorldTaskDijkstra worldTaskDijkstra;

	public SelectedPrecomputeV3() {
		super(PoliceForce.class);
	}

	@Override
	protected void paint(PoliceForce selectedObj, Graphics2D g, ScreenTransform transform) {
		short index = 0;
		//		System.err.println("task size="+entity.assignList);
		PrecomputeState state = ((PoliceForceAgent) model().sosAgent()).getState(PrecomputeState.class);
		worldTaskDijkstra = state.worldTaskDijkstra;
		//		System.err.println("javab size ="+state.assignList.size()+"  task size ="+state.tasks2.size());
		PoliceTaskes[] policeTaskes = new PoliceTaskes[((PoliceWorldModel) model()).policeForces().size()];
		for (int i = 0; i < policeTaskes.length; i++)
			policeTaskes[i] = new PoliceTaskes();
		///////////////////////////////////////
		for (short i = 0; i < state.assignList.size(); i++) {
			PoliceForce force = state.assignList.get(i);
			policeTaskes[force.getPoliceIndex()].policeForce = force;
			policeTaskes[force.getPoliceIndex()].taskList.add(state.tasks2.get(i));
		}
		for (short i = 0; i < policeTaskes.length; i++) {
			if (policeTaskes[i].policeForce == null)
				continue;
			policeTaskes[i].taskList.add(((PoliceWorldModel) model()).getPoliceTasks(policeTaskes[i].policeForce));
		}
		//		double t1 = System.currentTimeMillis();
		for (short i = 0; i < policeTaskes.length; i++) {
			if (policeTaskes[i].policeForce == null)
				continue;
			if (policeTaskes[i].policeForce == selectedObj)
				drawList(policeTaskes[i].taskList, g, transform);
		}
		//		int sumDistances = 0;
		//		for (int i = 0; i < agents.size(); i++) {
		//			Human fb = agents.get(i);
		//			ClusterData gen = representation.get(i);
		//			sumDistances += Utils.distance(gen.getX(), gen.getY(), fb.getX(), fb.getY());
		//		}
		//		return 1f / (sumDistances + 1);
		//		//System.out.println("Fitness time: " + (System.currentTimeMillis() - t1));

		//		for (PoliceForce force : state.assignList) {
		//			g.setColor(new Color((force.getPoliceIndex() * 40) % 250, (force.getPoliceIndex() * 20) % 250, (force.getPoliceIndex() * 100) % 250));
		//			g.setStroke(new BasicStroke(3));
		//			if (force != selectedObj) {
		//				index++;
		//				continue;
		//			}
		//			NamayangarUtils.drawLine(force.getX(), force.getY(), state.tasks2.get(index).getRealEntity().getPositionPoint().getIntX(), state.tasks2.get(index).getRealEntity().getPositionPoint().getIntY(), g, transform);
		//			NamayangarUtils.drawString(force.getPoliceIndex() + "", g, transform, force.getX(), force.getY());
		//			NamayangarUtils.drawString(force.getPoliceIndex() + "", g, transform, (state.tasks2.get(index).getRealEntity()).getPositionPoint().getIntX(), (state.tasks2.get(index).getRealEntity()).getPositionPoint().getIntY());
		//			g.setStroke(new BasicStroke(1));
		//			index++;
		//		}
	}

	private void drawList(ArrayList<Task<? extends StandardEntity>> taskList, Graphics2D g, ScreenTransform transform) {
		PoliceForce force = (PoliceForce) taskList.get(taskList.size() - 1).getRealEntity();

		if (taskList.size() < 2)
			return;

		long cost = 0;
		Task<? extends StandardEntity> currentTask = taskList.remove(taskList.size() - 1);
		Task<? extends StandardEntity> policeTask = currentTask;
		while (taskList.size() > 0) {
			Task<? extends StandardEntity> tempTask = null;
			long tempCost = Long.MAX_VALUE;
			for (Task<? extends StandardEntity> task : taskList) {
//				System.out.println("rize rize fitness " + currentTask + " be " + task + "---->" + worldTaskDijkstra.getDijkstraWeight(task, currentTask));
				if (worldTaskDijkstra.getDijkstraWeight(task, currentTask) < tempCost) {
					tempCost = worldTaskDijkstra.getDijkstraWeight(task, currentTask);
					tempTask = task;
				}
			}
			cost += tempCost;

			g.setStroke(new BasicStroke(3));
			g.setColor(new Color((force.getPoliceIndex() * 40) % 250, (force.getPoliceIndex() * 20) % 250, (force.getPoliceIndex() * 100) % 250));
			NamayangarUtils.drawLine(currentTask.getX(), currentTask.getY(), tempTask.getX(), tempTask.getY(), g, transform);
			g.setColor(Color.CYAN);
			g.setStroke(new BasicStroke(1));
			NamayangarUtils.drawString(cost + "(" + tempCost + ")", g, transform, tempTask.getX(), tempTask.getY());

			currentTask = tempTask;
			taskList.remove(tempTask);
		}
		long tempCost = worldTaskDijkstra.getDijkstraWeight(currentTask, model().searchWorldModel.getClusterData((Human) policeTask.getRealEntity()));
		cost += tempCost;

		g.setStroke(new BasicStroke(3));
		g.setColor(new Color((force.getPoliceIndex() * 40) % 250, (force.getPoliceIndex() * 20) % 250, (force.getPoliceIndex() * 100) % 250));
		NamayangarUtils.drawLine(currentTask.getX(), currentTask.getY(), model().searchWorldModel.getClusterData((Human) policeTask.getRealEntity()).getNearestBuildingToCenter().getX(), model().searchWorldModel.getClusterData((Human) policeTask.getRealEntity()).getNearestBuildingToCenter().getY(), g, transform);
		g.setColor(Color.cyan);
		g.setStroke(new BasicStroke(1));
		NamayangarUtils.drawString(cost + "(" + tempCost + ")", g, transform, model().searchWorldModel.getClusterData((Human) policeTask.getRealEntity()).getNearestBuildingToCenter().getX(), model().searchWorldModel.getClusterData((Human) policeTask.getRealEntity()).getNearestBuildingToCenter().getY());

	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return model() instanceof PoliceWorldModel;
	}

	private class PoliceTaskes {
		double fitness = 0;
		PoliceForce policeForce = null;
		ArrayList<Task<? extends StandardEntity>> taskList = new ArrayList<Task<? extends StandardEntity>>();
	}
}

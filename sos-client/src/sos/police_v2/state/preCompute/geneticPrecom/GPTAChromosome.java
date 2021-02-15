package sos.police_v2.state.preCompute.geneticPrecom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.genetics.InvalidRepresentationException;

import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.SOSGeometryTools;
import sos.base.util.genetic.SOSListChromsome;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.Task;
import sos.police_v2.state.preCompute.WorldTaskDijkstra;

/**
 * S.O.S chromosome class for running genetic on assigning agents to clusters.
 * 
 * @author Salim
 */
public class GPTAChromosome extends SOSListChromsome<PoliceForce> {
	WorldTaskDijkstra worldTaskDijkstra;
	private List<PoliceForce> polices;
	List<Task<? extends StandardEntity>> tasks;
	PoliceWorldModel model;
	public boolean print = false;
	private int CRITICAL_TASK;

	public GPTAChromosome(PoliceWorldModel model, WorldTaskDijkstra worldTaskDijkstra, List<Task<? extends StandardEntity>> tasks, PoliceForce[] representation, List<PoliceForce> polices) throws InvalidRepresentationException {
		super(representation);
		this.polices = polices;
		this.model = model;
		this.worldTaskDijkstra = worldTaskDijkstra;
		this.tasks = tasks;
		CRITICAL_TASK = (tasks.size() / polices.size()) ;
	}

	/**
	 * @author Yoosef
	 */
	@Override
	public double fitness() {
		List<PoliceForce> representation = getRepresentation();
		if (print)
			System.out.println(representation);
		//System.out.println("-----------------------");
		//		for (int i = 0; i < representation.size(); i++)
		//System.out.print(representation.get(i) + " , ");
		//System.out.println("*********************");
		//		for (int i = 0; i < representation.size(); i++)
		//System.out.print(tasks.get(i) + " , ");
		//System.out.println("*********************");

		if (representation == null)
			throw new Error("Chromosome is null in fitness function");
		PoliceTaskes[] policeTaskes = new PoliceTaskes[model.policeForces().size()];
		for (int i = 0; i < policeTaskes.length; i++)
			policeTaskes[i] = new PoliceTaskes();
		///////////////////////////////////////
		for (short i = 0; i < representation.size(); i++) {
			PoliceForce force = representation.get(i);
			policeTaskes[force.getPoliceIndex()].policeForce = force;
			policeTaskes[force.getPoliceIndex()].taskList.add(tasks.get(i));
		}
		for (short i = 0; i < policeTaskes.length; i++) {
			if (policeTaskes[i].policeForce == null)
				continue;
//			policeTaskes[i].taskList.add(model.getPoliceTasks(policeTaskes[i].policeForce));
		}
		double globalFitness = 0;
		//		double t1 = System.currentTimeMillis();
		for (short i = 0; i < policeTaskes.length; i++) {
			//			AntTsp anttsp = new AntTsp(worldTaskDijkstra, policeTaskes[i].taskList);
			//			anttsp.solve();
			//			policeTaskes[i].fitness = anttsp.bestTourLength;
			policeTaskes[i].fitness = getSampleFitness2(policeTaskes[i].policeForce,policeTaskes[i].taskList);
			if (print)
				System.out.println("i =" + i + " fitness" + policeTaskes[i].fitness);
			globalFitness += policeTaskes[i].fitness;

		}
		//		int sumDistances = 0;
		//		for (int i = 0; i < agents.size(); i++) {
		//			Human fb = agents.get(i);
		//			ClusterData gen = representation.get(i);
		//			sumDistances += Utils.distance(gen.getX(), gen.getY(), fb.getX(), fb.getY());
		//		}
		//		return 1f / (sumDistances + 1);
		//		//System.out.println("Fitness time: " + (System.currentTimeMillis() - t1));
//		System.err.println(globalFitness);
		return globalFitness;
	}

	private double getSampleFitness(ArrayList<Task<? extends StandardEntity>> taskList) {
		//System.out.println("rize fitness --> " + taskList);
		if (taskList.size() < 2)
			return 0;
		long cost = 0;
		Task<? extends StandardEntity> currentTask = taskList.remove(taskList.size() - 1);
		Task<? extends StandardEntity> policeTask = currentTask;
		
		while (taskList.size() > 0) {
			Task<? extends StandardEntity> tempTask = null;
			long tempCost = Long.MAX_VALUE;
			for (Task<? extends StandardEntity> task : taskList) {
				if (print)
					System.out.println("rize rize fitness " + currentTask + " be " + task + "---->" + worldTaskDijkstra.getDijkstraWeight(task, currentTask));
				long w =  (long) Math.pow(worldTaskDijkstra.getDijkstraWeight(task, currentTask),2);
				if (w < tempCost) {
					tempCost = w;
					tempTask = task;
				}
			}
			cost += tempCost;
			currentTask = tempTask;
			taskList.remove(tempTask);
		}
		cost += Math.pow(worldTaskDijkstra.getDijkstraWeight(currentTask, model.searchWorldModel.getClusterData((Human) policeTask.getRealEntity())),2);
		if (print) {
			System.out.println("az " + currentTask + " be " + "clusteresh" + policeTask + "=====>" + worldTaskDijkstra.getDijkstraWeight(currentTask, model.searchWorldModel.getClusterData((Human) policeTask.getRealEntity())));
			System.out.println("---------------------------------");
		}
		return cost;
	}
	private double getSampleFitness2(PoliceForce policeForce, ArrayList<Task<? extends StandardEntity>> taskList) {
		//System.out.println("rize fitness --> " + taskList);
		if(policeForce==null)
			return Math.pow(model.getBounds().getWidth()/8000,2);
		int cost = 0;
		double clusterx = model.searchWorldModel.getClusterData(policeForce).getX();
		double clustery = model.searchWorldModel.getClusterData(policeForce).getY();
		for (Task<? extends StandardEntity> task : taskList) {
			double w = Math.pow(SOSGeometryTools.distance(clusterx, clustery, task.getX(), task.getY())/1000,2);
			cost+= w;
		}
		if(taskList.size()>CRITICAL_TASK)
			cost+=Math.pow(model.getBounds().getWidth()/5000,2)*(taskList.size()-CRITICAL_TASK);
		
		return cost;
	}

	/**
	 * CHecks validity by checking occurances of ClusterDatas in lsit. No clsuter could be repeated
	 */
	@Override
	protected void checkValidity(List<PoliceForce> data) throws InvalidRepresentationException {
		polices = data.get(0).model().policeForces();

		int[] policeTask = new int[polices.size()];

		for (PoliceForce p : data) {
			if (policeTask[p.getPoliceIndex()] > GPCTaskAssigner.MAX_TASK) {
				throw new InvalidRepresentationException(new DummyLocalizable(" Not valid Data in GCAChromosome"), data);
			} else {
				policeTask[p.getPoliceIndex()]++;
			}
		}
	}

	/**
	 * this function simply changes the input to an [] and returns an instance of GCAChromosome
	 */
	@Override
	public GPTAChromosome newFixedLengthChromosome(List<PoliceForce> data) {
		PoliceForce[] tmp = new PoliceForce[data.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = data.get(i);
		}
		return new GPTAChromosome(model, worldTaskDijkstra, tasks, tmp, polices);
	}

	private class PoliceTaskes {
		double fitness = 0;
		PoliceForce policeForce = null;
		ArrayList<Task<? extends StandardEntity>> taskList = new ArrayList<Task<? extends StandardEntity>>();
	}

	public void printResult() {
		if (getRepresentation() == null)
			throw new Error("Chromosome is null in fitness function");
		PoliceTaskes[] policeTaskes = new PoliceTaskes[model.policeForces().size()];
		for (int i = 0; i < policeTaskes.length; i++)
			policeTaskes[i] = new PoliceTaskes();
		///////////////////////////////////////
		for (short i = 0; i < getRepresentation().size(); i++) {
			PoliceForce force = getRepresentation().get(i);
			policeTaskes[force.getPoliceIndex()].policeForce = force;
			policeTaskes[force.getPoliceIndex()].taskList.add(tasks.get(i));
		}
		for (short i = 0; i < policeTaskes.length; i++) {
			if (policeTaskes[i].policeForce == null)
				continue;
			policeTaskes[i].taskList.add(model.getPoliceTasks(policeTaskes[i].policeForce));
		}
		double globalFitness = 0;
		ArrayList<Double> list = new ArrayList<Double>();
		for (short i = 0; i < policeTaskes.length; i++) {
			policeTaskes[i].fitness = getSampleFitness(policeTaskes[i].taskList);
			if (policeTaskes[i].fitness > 0)
				list.add(policeTaskes[i].fitness);
			globalFitness += policeTaskes[i].fitness;
		}
		Collections.sort(list);
		System.out.println("genetic precom result = " + globalFitness + " <<<<< " + list.get(0) + "====" + list.get(list.size() / 4) + "====" + list.get(list.size() / 2) + "====" + list.get((list.size() * 3) / 4) + "====" + list.get(list.size() - 1) + ">>>>> avg:" + (globalFitness / list.size()));
		System.out.println(list);
	}
}

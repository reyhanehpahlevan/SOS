package sos.police_v2.state.preCompute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.GasStation;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.precompute.FileOperations;
import sos.base.precompute.PreCompute;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.state.PoliceAbstractState;
import sos.police_v2.state.preCompute.geneticPrecom.GPCTaskAssigner;
import sos.police_v2.state.preCompute.geneticPrecom.PrecomputeAssignFromFile;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.Dijkstra;

public class PrecomputeState extends PoliceAbstractState {
	ArrayList<Task<? extends StandardEntity>> tasks = new ArrayList<Task<? extends StandardEntity>>();
	HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>> agent_task = new HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>>();
	public WorldTaskDijkstra worldTaskDijkstra;
	public List<PoliceForce> assignList;
	public ArrayList<Task<? extends StandardEntity>> tasks2;
	public boolean isDone = false;
	private ArrayList<ClusterData> criticalCluster;

	public PrecomputeState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);

	}

	@Override
	public void precompute() {
		makeTask();
		setTasksValue();
		if (!canDoPrecomputeState())
			return;
		worldTaskDijkstra = new WorldTaskDijkstra(agent, tasks);
		taskAssigning_v3();
		criticalCluster = PoliceUtils.getNeighberCluster(model(), model().searchWorldModel.getClusterData());
		criticalCluster.add(model().searchWorldModel.getClusterData());
	}

	private boolean canDoPrecomputeState() {
		for (Task<? extends StandardEntity> task : tasks) {
			if (task.getFinalValue() == 0)
				return true;
		}
		return false;
	}

	private void taskAssigning_v3() {
		log.info("Assigning tasks...");

		ArrayList<PoliceForce> ignoredPolices = new ArrayList<PoliceForce>();
		ArrayList<PoliceForceTask> entrancePolice = new ArrayList<PoliceForceTask>();
		ArrayList<PoliceForceTask> inBuildingPolice = new ArrayList<PoliceForceTask>();

		for (PoliceForceTask p : model().getPoliceTasks()) {
			if (p.getAreaPosition() instanceof Building) {
				inBuildingPolice.add(p);
				continue;
			}
			if (p.getAreaPosition().getSOSGroundArea() < PoliceConstants.VERY_SMALL_ROAD_GROUND_IN_MM)
				for (Area n : p.getAreaPosition().getNeighbours()) {
					if (n instanceof Building) {
						entrancePolice.add(p);
						break;
					}
				}
		}
		int noneIgnoredPolice = model().policeForces().size();
		noneIgnoredPolice -= entrancePolice.size();// polices in the entrance of the building 
		noneIgnoredPolice -= inBuildingPolice.size();// polices in the building 
		noneIgnoredPolice--;//special task police

		for (PoliceForceTask policeForceTask : inBuildingPolice) {
			ignoredPolices.add(policeForceTask.getRealEntity());
		}
		if (noneIgnoredPolice > model().policeForces().size() * 0.8d) {
			for (PoliceForceTask policeForceTask : entrancePolice) {
				ignoredPolices.add(policeForceTask.getRealEntity());
			}
		}
		if (agent.messageSystem.type == Type.NoComunication && agent.newSearch.strategyChooser.noCommunication.getCenteralMan() instanceof PoliceForce) {
			PoliceForceTask ptask = model().getPoliceTasks((PoliceForce) agent.newSearch.strategyChooser.noCommunication.getCenteralMan());
			ignoredPolices.add(ptask.getRealEntity());
		}
		for (PoliceForceTask force : model().getPoliceForSpecialTask()) {
			if (!ignoredPolices.contains(force.getRealEntity()))
				ignoredPolices.add(force.getRealEntity());
		}
		ArrayList<PoliceForce> polices = new ArrayList<PoliceForce>(model().policeForces());
		polices.removeAll(ignoredPolices);

		tasks2 = new ArrayList<Task<? extends StandardEntity>>();
		for (Task<? extends StandardEntity> t : tasks) {
			if (t.getRealEntity() instanceof AmbulanceTeam || t.getRealEntity() instanceof FireBrigade)
				tasks2.add(t);
			else if (t.getRealEntity() instanceof Refuge) {
				tasks2.add(t);
			}
			else if (t.getRealEntity() instanceof PoliceForce) {
				if (ignoredPolices.contains(t.getRealEntity()))
					tasks2.add(t);
			}
			else if (t.getRealEntity() instanceof GasStation) {
				tasks2.add(t);
			}
		}
		//		System.err.println("IGNORE SIZE =" + ignoredPolices.size() + " entrance= " + entrancePolice.size() + " building" + inBuildingPolice.size() + " none=" + noneIgnoredPolice);
		////////////////
		PrecomputeAssignFromFile file = FileOperations.Read(PreCompute.getPreComputeFile("PolicePrecompute/"), PrecomputeAssignFromFile.class);
		if (file == null) {
			GPCTaskAssigner assigner = new GPCTaskAssigner(model(), worldTaskDijkstra, tasks2, polices);
			assignList = assigner.decide();
			file = new PrecomputeAssignFromFile();
			file.setAssign(assignList);
			FileOperations.Write(PreCompute.getPreComputeFile("PolicePrecompute/"), file);
		} else {
			assignList = file.getAssign();
		}
		/////////////////
		short index = -1;
		for (PoliceForce force : assignList) {
			index++;
			agent_task.get(model().getPoliceTasks(force)).add(tasks2.get(index));
		}
		log.debug("agent task->" + agent_task);
	}

	private void taskAssigning() {
		log.info("Assigning tasks...");
		TaskGraph taskgraph = new TaskGraph(tasks, agent, worldTaskDijkstra);

		Dijkstra myDij = new Dijkstra(tasks.size(), model());
		while (true) {
			ArrayList<Task<? extends StandardEntity>> destsTask = notSelectedSourcesTask();
			if (destsTask.size() == 0)
				break;

			log.trace("Destination ha barabare:", destsTask);

			Task<? extends StandardEntity> target = mostValueTask(destsTask);
			log.debug("Target ba bishtarin value  = " + target);

			log.info("Task assigning dijkstra is started");
			ArrayList<Integer> validPolices = removeFrom(model().getPoliceTasks(), target);
			myDij.Run(taskgraph, validPolices, getFirstCostOfPolices(validPolices, target));

			log.trace("destination costs: To Target '" + target + "'= " + myDij.getWeight(target.getIndex()) + " ; ");

			ArrayList<Integer> getpathArray = myDij.getpathArray(target.getIndex());
			StringBuilder sb = new StringBuilder("Task assigning dijkstra path = ");
			for (int i = 0; i < getpathArray.size(); i++) {
				sb.append(tasks.get(getpathArray.get(i)).getRealEntity() + " , ");
			}
			log.debug(sb);
			//			worldTaskDijkstra.debug(getpathArray);
			worldTaskDijkstra.saveMovePath(getpathArray);

			assign(getpathArray, taskgraph);
		}
		log.debug("agent task->" + agent_task);
	}

	private long[] getFirstCostOfPolices(ArrayList<Integer> validPolices, Task<? extends StandardEntity> task) {
		long[] costs = new long[tasks.size()];
		for (short i = 0; i < tasks.size(); i++)
			costs[i] = 0;
		for (int i : validPolices) {
			if (!(tasks.get(i).getRealEntity() instanceof PoliceForce)) {
				log.error("OMG WHY TASK IS NOT POLICE");
				continue;
			}
			PoliceForce force = (PoliceForce) tasks.get(i).getRealEntity();
			ClusterData clusterData = model().searchWorldModel.getClusterData(force);
			costs[i] = (long) (worldTaskDijkstra.getDijkstraWeight(task, clusterData) * 0.6f);
		}
		return costs;
	}

	private ArrayList<Integer> removeFrom(ArrayList<PoliceForceTask> arrayList, Task<?> removetask) {
		ArrayList<Integer> pt = new ArrayList<Integer>();

		for (PoliceForceTask task : arrayList) {
			if (task.getIndex() != removetask.getIndex()) {
				//nabayad har 2ta ye task ha too building bashan
				if (!(removetask.getRealEntity().getAreaPosition() instanceof Building && (task.getRealEntity().getAreaPosition() instanceof Building)))
					pt.add((int) task.getIndex());
			}
		}
		return pt;
	}

	private Task<? extends StandardEntity> mostValueTask(ArrayList<Task<? extends StandardEntity>> sources2) {
		if (sources2.size() == 0)
			return null;
		Task<? extends StandardEntity> mostValue = sources2.get(0);
		for (Task<? extends StandardEntity> task : sources2) {
			if (!task.isAssigned() && task.getFinalValue() > mostValue.getFinalValue())
				mostValue = task;
		}
		return (mostValue);
	}

	private ArrayList<Task<? extends StandardEntity>> notSelectedSourcesTask() {
		ArrayList<Task<? extends StandardEntity>> nss = new ArrayList<Task<? extends StandardEntity>>();
		for (Task<? extends StandardEntity> task : tasks) {
			if ((!task.isAssigned()) && task.getFinalValue() != 0)
				nss.add(task);
		}
		log.info("hanouz task assigne nashode darim " + nss.size());
		return nss;
	}

	private void assign(ArrayList<Integer> getpathArray, TaskGraph taskgraph) {

		PoliceForceTask policeToDoTasks = (PoliceForceTask) tasks.get(getpathArray.get(0));
		for (int i = 1; i < getpathArray.size(); i++) {
			int gpa = getpathArray.get(i);
			Task<? extends StandardEntity> targetTask = tasks.get(gpa);
			if (!tasks.get(gpa).isAssigned()) {
				agent_task.get(policeToDoTasks).add(targetTask);
				targetTask.setAssigner(policeToDoTasks);
				//				taskgraph.changePosition(tasks.get(getpathArray.get(0)), tasks.get(gpa));

			}
		}
		taskgraph.changePosition(policeToDoTasks, tasks.get(getpathArray.get(getpathArray.size() - 1)));
		log.debug(policeToDoTasks + " CurrentJobDone:" + policeToDoTasks.getJobDone());

	}

	//	public void changePosition(Task task, int x, int y) {
	//		if (task.task instanceof PoliceForce) {
	//			task.jobDone += PoliceUtils.getDistance(x, y, task.getX(), task.getY());//* 2/3;
	//		}
	//		task.setLocatin(x, y);
	//		task.setPosition(x, y);
	//		
	//		taskgraph.clearWeight(task.getIndex());
	//	}

	//	private Task getTask(StandardEntity se) {
	//		for (int i = 0; i < tasks.size(); i++) {
	//			if (tasks.get(i).task.getID().getValue() == se.getID().getValue())
	//				return tasks.get(i);
	//		}
	//		return null;
	//	}

	private void makeTask() {
		log.info("creation of task is started");
		short index = 0;
		for (PoliceForceTask policeForce : model().getPoliceTasks()) {
			tasks.add(policeForce);
			agent_task.put(policeForce, new ArrayList<Task<?>>());
			log.trace(policeForce + " default value=" + policeForce.defaultValue);
			index++;
		}
		log.info("polices added and task size is = " + tasks.size());
		for (FireBrigade fireBrigade : model().fireBrigades()) {
			Task<FireBrigade> task = new Task<FireBrigade>(fireBrigade, index++);
			task.setDefaultValue();
			tasks.add(task);
			log.trace(task + " default value=" + task.defaultValue);
		}
		log.info("fire brigaeds added and task size is = " + tasks.size());
		for (AmbulanceTeam ambulanceTeam : model().ambulanceTeams()) {
			Task<AmbulanceTeam> task = new Task<AmbulanceTeam>(ambulanceTeam, index++);
			task.setDefaultValue();
			tasks.add(task);
			log.trace(task + " default value=" + task.defaultValue);
		}
		log.info("amblances added and task size is = " + tasks.size());
		for (Refuge refuge : model().refuges()) {
			Task<Refuge> task = new Task<Refuge>(refuge, index++);
			task.setDefaultValue();
			tasks.add(task);
			log.trace(task + " default value=" + task.defaultValue);
		}
		log.info("refuges added and task size is = " + tasks.size());
		for (GasStation gasStation : model().GasStations()) {
			Task<GasStation> task = new Task<GasStation>(gasStation, index++);
			task.setDefaultValue();
			tasks.add(task);
			log.trace(task + " default value=" + task.defaultValue);
		}
		log.info("gasStation added and task size is = " + tasks.size());
		//		if (agent.messageSystem.type == Type.NoComunication && agent.newSearch.strategyChooser.starSearch != null) {
		//			log.info("No Communication ===>star search Gathering task is adding");
		//			for (Road r : agent.newSearch.strategyChooser.starSearch.getGatheringAreas()) {
		//				Task<Road> task = new Task<Road>(r, index++);
		//				task.setDefaultValue();
		//				tasks.add(task);
		//				log.trace(task + " default value=" + task.defaultValue);
		//			}
		//			log.info("star search added and task size=" + tasks.size());
		//		}
	}

	private void setTasksValue() {
		ArrayList<PoliceForceTask> entrancePolice = new ArrayList<PoliceForceTask>();
		for (PoliceForceTask p : model().getPoliceTasks()) {
			for (Area n : p.getAreaPosition().getNeighbours()) {
				if (n instanceof Building)
					entrancePolice.add(p);
			}
		}
		if (entrancePolice.size() < Math.min(6, model().policeForces().size() / 2)) {
			for (PoliceForceTask policeForceTask : entrancePolice) {
				policeForceTask.setDefaultValue(PoliceConstants.Value.PoliceForceInBuilding.getValue());
				policeForceTask.setJobDone(PoliceConstants.DEFAULT_JOB_DONE_FOR_POLICE_IN_BUILDING * PoliceConstants.STANDARD_OF_MAP, false);

			}
		}
		if (agent.messageSystem.type == Type.NoComunication && agent.newSearch.strategyChooser.noCommunication.getCenteralMan() instanceof PoliceForce) {
			PoliceForceTask ptask = model().getPoliceTasks((PoliceForce) agent.newSearch.strategyChooser.noCommunication.getCenteralMan());
			//		getPoliceForSpecialTask().add(ptask);
			ptask.setDefaultValue(PoliceConstants.Value.PoliceForSpecialTasks.getValue());
			ptask.setJobDone(10000, false);
			log.debug("SpecialPolice for no comm===>" + ptask);
		}
		for (Task<? extends StandardEntity> task : tasks) {
			task.setFinalValue(tasks);
		}

	}

	@Override
	public void act() throws SOSActionException {
		//		log.info("Acting as Precompute State....{{{{{{{{{{{{{{");
		sendDeadAgentTasks();
		sendBuriedAgentTasks();
		makeRechToTasks();
		//		log.info("precompute state finished... }}}}}}}}}}}}}}}");
		//		isFinished = true;
		isDone = true;
	}

	private void sendDeadAgentTasks() {
		log.info("Send dead agents tasks");

		for (PoliceForceTask policeTask : model().getPoliceTasks()) {
			PoliceForce police = policeTask.getRealEntity();
			if (police.isHPDefined() && police.getHP() < 5 && !agent_task.get(policeTask).isEmpty()) {
				log.debug("Oh:( " + police + " is died....");

				ArrayList<Task<? extends StandardEntity>> deadPoliceTasks = new ArrayList<Task<? extends StandardEntity>>(agent_task.get(policeTask));
				if (agent.messageSystem.type == Type.LowComunication || agent.messageSystem.type == Type.NoComunication) {
					PoliceForceTask bestPolice = getBestPoliceToSendTaskTo(policeTask);
					for (Task<? extends StandardEntity> ptask : deadPoliceTasks) {
						sendTask(policeTask, bestPolice, ptask);
					}
				} else {
					for (Task<? extends StandardEntity> ptask : deadPoliceTasks) {
						PoliceForceTask bestPolice = getBestPoliceToSendTaskTo(policeTask);
						sendTask(policeTask, bestPolice, ptask);
					}
				}

			}
		}
	}

	private void sendBuriedAgentTasks() {
		log.info("Send Buried agents tasks that in the start of simulation are not in buildings");

		for (PoliceForceTask policeTask : model().getPoliceTasks()) {
			PoliceForce police = policeTask.getRealEntity();
			if (policeTask.defaultValue == PoliceConstants.Value.PoliceForce.getValue()) {//if at first it wasn't in a building
				if (police.isBuriednessDefined() && police.getBuriedness() > 0 && !agent_task.get(policeTask).isEmpty()) {
					log.debug("Oh:( " + police + " have buridness " + police.getBuriedness() + "....but at the start of simulation is not in building");

					ArrayList<Task<? extends StandardEntity>> buriedPoliceTasks = agent_task.get(policeTask);
					PoliceForceTask bestPolice = getBestPoliceToSendTaskTo(policeTask);
					agent_task.get(bestPolice).add(0, policeTask);
					ArrayList<Task<?>> tmpburiedPoliceTasks = new ArrayList<Task<?>>(buriedPoliceTasks);

					for (Task<? extends StandardEntity> ptask : tmpburiedPoliceTasks) {
						if (!(agent.messageSystem.type == Type.NoComunication || agent.messageSystem.type == Type.LowComunication))
							bestPolice = getBestPoliceToSendTaskTo(policeTask);
						sendTask(policeTask, bestPolice, ptask);
					}
					log.debug(police + " have buriedness so he added to " + bestPolice.getRealEntity() + " task's!! now new " + bestPolice.getRealEntity() + " tasks are:" + agent_task.get(bestPolice));
				}
			}
		}
	}

	private PoliceForceTask getBestPoliceToSendTaskTo(PoliceForceTask deadPoliceTask) {
		log.info("getBestPoliceToSendTaskTo");
		if (deadPoliceTask.isAssigned()) {
			PoliceForce assighnerTask = deadPoliceTask.getAssigner().getRealEntity();
			if (!(assighnerTask.isHPDefined() && assighnerTask.getHP() < 5)) {
				log.debug("BestPoliceToSendTaskTo is:" + deadPoliceTask.getAssigner());
				return deadPoliceTask.getAssigner();
			}
			log.debug("AssignerPoliceToSendTaskTo is dead:" + deadPoliceTask.getAssigner());

		}

		PoliceForceTask minJobDonedPolice = model().getPoliceTasks().get(0);
		for (PoliceForceTask next : model().getPoliceTasks()) {
			PoliceForce nextPolice = next.getRealEntity();
			if (!(nextPolice.isHPDefined() && nextPolice.getHP() < 5) && next.getJobDone() < minJobDonedPolice.getJobDone())
				minJobDonedPolice = next;
		}
		log.debug("BestPoliceToSendTaskTo is:" + minJobDonedPolice);
		minJobDonedPolice.addJobDone(100, false);//TODO
		return minJobDonedPolice;

	}

	public void makeRechToTasks() throws SOSActionException {
		log.info("Reachabling to tasks=" + agent_task.get(model().meTask()));
		ArrayList<StandardEntity> shouldOpenTasks = new ArrayList<StandardEntity>();
		ArrayList<Task<? extends StandardEntity>> myTasks = agent_task.get(model().meTask());
		ArrayList<Building> buildingTask = new ArrayList<Building>();
		ArrayList<StandardEntity> criticalPrioty = new ArrayList<StandardEntity>();
		ArrayList<StandardEntity> highPrioty = new ArrayList<StandardEntity>();
		ArrayList<StandardEntity> lowPrioty = new ArrayList<StandardEntity>();
		ArrayList<Task<? extends StandardEntity>> criticalPriotyTask = new ArrayList<Task<? extends StandardEntity>>();
		ArrayList<Task<? extends StandardEntity>> highPriotyTask = new ArrayList<Task<? extends StandardEntity>>();
		ArrayList<Task<? extends StandardEntity>> lowPriotyTask = new ArrayList<Task<? extends StandardEntity>>();

		for (Task<? extends StandardEntity> task : myTasks) {
			if (!task.isDoneWithoutCalc() && (task.getRealEntity() instanceof Refuge)) {
				if (task.getAreaPosition().equals(agent.me().getAreaPosition())) {
					task.setDone(true);
				} else
					buildingTask.add((Building) task.getRealEntity());

				continue;
			}
			if (!task.isDone()) {
				if (isLowPriotyTask(task)) {
					lowPriotyTask.add(task);
					lowPrioty.add(task.getRealEntity());
				} else {
					if (isInCritical() && task.getRealEntity() instanceof FireBrigade) {
						criticalPriotyTask.add(task);
						criticalPrioty.add(task.getRealEntity());
					} else {
						highPriotyTask.add(task);
						highPrioty.add(task.getRealEntity());
					}
				}
				//				shouldOpenTasks.add(task.getRealEntity());
			}
		}
		

		
		if (!buildingTask.isEmpty()) {
			
			log.debug("should open buildingTasks are: " + shouldOpenTasks);

			ArrayList<Pair<? extends Area, Point2D>> list = new ArrayList<Pair<? extends Area, Point2D>>();
			for (Building dest : buildingTask) {
				list.add(getEntrancePoint(dest));
			}
			moveToPoint(list);

		}
		if (!criticalPrioty.isEmpty())
			log.warn("it is critical must reachable fire agent first " + criticalPrioty);
		log.debug("critical prioty open tasks are: " + criticalPrioty);
		log.debug("high prioty open tasks are: " + highPrioty);
		log.debug("low prioty open tasks are: " + lowPrioty);
		/////////////////////////////
		makeReachableTo(criticalPrioty);
		for (Task<? extends StandardEntity> task : criticalPriotyTask) {
			task.setDone(true);
		}
		log.debug("all critical prioty tasks are reachable...");

		/////////////////////////////
		makeReachableTo(highPrioty);
		for (Task<? extends StandardEntity> task : highPriotyTask) {
			task.setDone(true);
		}
		log.debug("all high prioty tasks are reachable...");
		/////////////////////////////
		makeReachableTo(lowPrioty);
		for (Task<? extends StandardEntity> task : lowPriotyTask) {
			task.setDone(true);
		}
		log.debug("all tasks are reachable");
		/////////////////////////////
	}

	private boolean isInCritical() {
		for (ClusterData clusterData : criticalCluster) {
			for (Building building : clusterData.getBuildings()) {
				if (building.isFierynessDefined())
					if (building.getFieryness() > 0 && building.getFieryness() < 4)
						return true;
			}
		}
		return false;
	}

	private boolean isLowPriotyTask(Task<? extends StandardEntity> task) {
		if (task.getRealEntity() instanceof Human && ((Human) task.getRealEntity()).isBuriednessDefined() && ((Human) task.getRealEntity()).getBuriedness() > 0)
			return true;
		if (task.getRealEntity() instanceof Human)
			if (!((Human) task.getRealEntity()).isBuriednessDefined())
				if (task.getRealEntity().getAreaPosition() instanceof Building)
					return true;
		return false;
	}

	public void sendTask(PoliceForceTask from, PoliceForceTask to, Task<? extends StandardEntity> ptask) {
		log.debug("sending " + ptask + " from " + from + " to " + to);
		log.debug(to + "'s old tasks=" + agent_task.get(to));
		log.debug(from + "'s old tasks=" + agent_task.get(from));

		agent_task.get(to).add(ptask);
		agent_task.get(from).remove(ptask);
		log.debug(to + "'s tasks=" + agent_task.get(to));
		log.debug(from + "'s tasks=" + agent_task.get(from));
	}

	public ArrayList<Task<? extends StandardEntity>> getTasksOf(PoliceForceTask policeForceTask) {
		return agent_task.get(policeForceTask);
	}
}

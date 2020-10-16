package sos.police_v2.state.preCompute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import rescuecore2.misc.Pair;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.PoliceAbstractState;
import sos.tools.MST;

public class MSTState extends PoliceAbstractState {
	public HashMap<PoliceForceTask, ArrayList<Task<?>>> msttasks = new HashMap<PoliceForceTask, ArrayList<Task<?>>>();

	public MSTState(PoliceForceAgent agent) {
		super(agent);
	}

	@Override
	public void precompute() {
		for (PoliceForceTask ptask : model().getPoliceTasks()) {
			msttasks.put(ptask, new ArrayList<Task<?>>());
		}
		MSTtaskAssigning();
	}

	private void MSTtaskAssigning() {
		log.info("MSTtaskAssigning started...");
		ArrayList<Task<? extends StandardEntity>> tasks = agent.getState(PrecomputeState.class).tasks;
		WorldTaskDijkstra worldTaskDijkstra = agent.getState(PrecomputeState.class).worldTaskDijkstra;
		MSTTaskGraph mstGraph = new MSTTaskGraph(agent, tasks.size(), model().getPoliceTasks(), tasks, worldTaskDijkstra);

		MST mst = new MST(mstGraph.getNumberOfNodes(), model());

		mst.Run(mstGraph, Arrays.asList((int) model().getPoliceTasks().get(0).getIndex()));//XXX chera fagaht ye src dare?

		ArrayList<Pair<Integer, Integer>> mstresult = mst.getMST();
		StringBuilder sb = new StringBuilder("MST Result=");

		for (Pair<Integer, Integer> pair : mstresult) {
			sb.append("from " + tasks.get(pair.first()) + " to " + tasks.get(pair.second()) + " , ");
			PoliceForceTask polFirst;
			Task<?> polsecond;
			//			if (tasks.get(pair.first()).jobDone < tasks.get(pair.second()).jobDone) {
			polFirst = (PoliceForceTask) tasks.get(pair.first());
			polsecond = tasks.get(pair.second());
			//			} else {
			//				polFirst = ((PoliceForce) tasks.get(pair.second()).task);
			//				polsecond = tasks.get(pair.first());
			//			}
			msttasks.get(polFirst).add(polsecond);

		}

		log.debug(sb);
		log.info("mst task assigning finished.");
	}

	private void makeRechToMSTTasks() throws SOSActionException {

		log.info("Reachabling to tasks=" + msttasks.get(model().meTask()));
		ArrayList<StandardEntity> shouldOpenTasks = new ArrayList<StandardEntity>();
		ArrayList<Task<? extends StandardEntity>> myTasks = msttasks.get(model().meTask());
		for (Task<? extends StandardEntity> task : myTasks) {
			if (!task.isDone()) {
				if (isReachableTo(task.getRealEntity())) {
					task.setDone(true);
					log.debug("task =" + task + " done shoood");
					continue;
				}
				shouldOpenTasks.add(task.getRealEntity());
			}
		}
		log.debug("should open tasks are: " + shouldOpenTasks);
		makeReachableTo(shouldOpenTasks);
		log.debug("all tasks are reachable...");

	}

	@Override
	public void act() throws SOSActionException {
		//		log.info("make rech to mst tasks started.....{{{{{{{{{{");
		sendDeadAgentTasks();
		sendBuriedAgentTasks();
		makeRechToMSTTasks();
		//		log.info("make rech to mst tasks finished}}}}}}}}}");
	}

	private void sendDeadAgentTasks() {
		log.info("Send dead agents tasks");
		for (PoliceForceTask policeTask : model().getPoliceTasks()) {
			PoliceForce police = policeTask.getRealEntity();
			if (police.isHPDefined() && police.getHP() < 5 && !msttasks.get(policeTask).isEmpty()) {
				log.debug("Oh:( " + police + " is died....");
				sendTask(policeTask, getBestPoliceToSendTaskTo(policeTask));
			}
		}
	}

	private void sendBuriedAgentTasks() {
		log.info("Send Buried agents tasks that in the start of simulation are not in buildings");

		for (PoliceForceTask policeTask : model().getPoliceTasks()) {
			PoliceForce police = policeTask.getRealEntity();
			if (policeTask.defaultValue == PoliceConstants.Value.PoliceForce.getValue()) {//if at first it wasn't in a building
				if (police.isBuriednessDefined() && police.getBuriedness() > 0 && !msttasks.get(policeTask).isEmpty()) {
					log.debug("Oh:( " + police + " have buridness " + police.getBuriedness() + "....but at the start of simulation is not in building");
					PoliceForceTask bestPolice = getBestPoliceToSendTaskTo(policeTask);
					sendTask(policeTask, bestPolice);

					msttasks.get(bestPolice).add(0, policeTask);
					log.debug(police + " have buriedness so he added to " + bestPolice.getRealEntity() + " task's!! now new " + bestPolice.getRealEntity() + " tasks are:" + msttasks.get(bestPolice));
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
		return minJobDonedPolice;

	}

	public void sendTask(PoliceForceTask from, PoliceForceTask to) {
		log.debug(from + " is sending its tasks to " + to);
		log.debug(to + "'s tasks=" + msttasks.get(to));
		log.debug(from + "'s tasks=" + msttasks.get(from));

		msttasks.get(to).addAll(msttasks.get(from));
		msttasks.get(from).clear();
		log.debug(to + "'s MSTtasks=" + msttasks.get(to));
		log.debug(from + "'s MSTtasks=" + msttasks.get(from));
	}

	public ArrayList<Task<? extends StandardEntity>> getTasksOf(PoliceForceTask policeTasks) {

		return msttasks.get(policeTasks);
	}

}

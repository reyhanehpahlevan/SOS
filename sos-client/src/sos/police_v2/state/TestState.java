package sos.police_v2.state;

import java.util.ArrayList;
import java.util.HashMap;

import sos.base.entities.Building;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.Task;

public class TestState extends PoliceAbstractState {
	ArrayList<Task<? extends StandardEntity>> alltasks = new ArrayList<Task<?>>();
	public HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>> agent_tasks = new HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>>();

	public TestState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		/*for (PoliceForceTask iterable_element : model().getPoliceTasks()) {
			agent_tasks.put(iterable_element, new ArrayList<Task<? extends StandardEntity>>());
		}
		int[][] lists = { { 352, 32737, 33815 }, { 33815, 31204, 1782 }, { 32765, 1976, 31204 }, { 1976, 352 }, { 32765, 32737 } };
		for (int i = 0; i < lists.length; i++) {
			PoliceForceTask police = getBestPoliceToSendTaskTo();

			for (int j : lists[i]) {
				Task<StandardEntity> t = new Task<StandardEntity>(model().getEntity(j), model().getEntity(j).getPositionPair());
				alltasks.add(t);
				agent_tasks.get(police).add(t);
			}
		}*/
	}

	/*
	private PoliceForceTask getBestPoliceToSendTaskTo() {
		PoliceForceTask minJobDonedPolice = model().getPoliceTasks().get(0);
		for (PoliceForceTask next : model().getPoliceTasks()) {
			PoliceForce nextPolice = next.getRealEntity();
			if (!(nextPolice.isHPDefined() && nextPolice.getHP() < 5) && next.getJobDone() < minJobDonedPolice.getJobDone())
				minJobDonedPolice = next;
		}
		log.debug("BestPoliceToSendTaskTo is:" + minJobDonedPolice);
		minJobDonedPolice.addJobDone(200, false);//TODO
		return minJobDonedPolice;
	}
	 */

	@Override
	public void act() throws SOSActionException {
		//	makeReachable((Area) model().getEntity((971)));
		
//		makeToTasks();
		if(!agent.model().refuges().isEmpty()){
			if(agent.me().getPositionArea()instanceof Refuge)
				agent.problemRest("Noting to do");
			move(agent.model().refuges());
		}else if(agent.me().getPositionArea()instanceof Building)
			move(agent.model().roads());
		
		agent.problemRest("Noting to do");
	}
	public void makeToTasks() throws SOSActionException {
		/*
		log.info("Reachabling to tasks=" + agent_tasks.get(model().meTask()));
		ArrayList<Area> shouldOpenTasks = new ArrayList<Area>();
		ArrayList<Task<? extends StandardEntity>> myTasks = agent_tasks.get(model().meTask());
		for (Task<? extends StandardEntity> task : myTasks) {
			if(model().me().getAreaPosition().equals(task.getRealEntity().getAreaPosition()))
				task.setDone(true);
			if (!task.isDone()) {
				shouldOpenTasks.add(task.getRealEntity().getAreaPosition());
			}
		}
		log.debug("should open tasks are: " + shouldOpenTasks);
		move(shouldOpenTasks);
		log.debug("all tasks are reachable...");
*/
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub
		
	}


}

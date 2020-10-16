package sos.police_v2.state.preCompute;

import java.util.ArrayList;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.FireBrigade;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;

public class Task<E extends StandardEntity> {
	private final E realEntity;

	protected int defaultValue = 0;
	protected int finalValue = 0;
	private boolean done = false;
	private final short index;

	private Pair<? extends Area, Point2D> areaPosition;

	private PoliceForceTask assigner = null;

	public Task(E task, short index) {
		this.realEntity = task;
		this.index = index;
		setPosition(task.getPositionPair());
	}

	public Task(E realEntity, Pair<? extends Area, Point2D> positionPair) {
		index = -1;
		setPosition(positionPair);
		this.realEntity = realEntity;

	}

	/*
	 * public Task(E task, PoliceForceAgent police, Pair<Integer, Integer> poss) {
	 * this.task =task;
	 * // this.agent = police.me();
	 * //TODO
	 * index=-1;
	 * setPosition(police.location());
	 * this.x = poss.first();
	 * this.y =poss.second();
	 * }
	 */

	/*
	 * public Task(Road road, sos.police.PoliceForceAgent agent2, Pair<Integer, Integer> pair) {
	 * this.task =road;
	 * index=-1;
	 * //TODO
	 * // this.agent = agent2.me();
	 * setPosition(agent2.location());
	 * this.x = pair.first();
	 * this.y = pair.second();
	 * }
	 * public Task(Area destination, sos.police.PoliceForceAgent agent2, Pair<Integer, Integer> pair) {
	 * this.task =destination;
	 * // this.agent = agent2.me();
	 * index=-1;
	 * setPosition(agent2.location());
	 * this.x = pair.first();
	 * this.y = pair.second();
	 * }
	 */
	protected void setDefaultValue() {
		if (realEntity instanceof Refuge) {
			this.defaultValue = PoliceConstants.Value.Refuge.getValue();
		} else if (realEntity instanceof FireBrigade) {
			this.defaultValue = PoliceConstants.Value.FireBrigade.getValue();
		} else if (realEntity instanceof AmbulanceTeam) {
			this.defaultValue = PoliceConstants.Value.AmbulanceTeam.getValue();
		} else if (realEntity instanceof Road) {
			this.defaultValue = PoliceConstants.Value.StarSearchGatheringRoad.getValue();
		} else {
			//			realEntity.getAgent().sosLogger.agent.error("Task Type Invalid type=" + realEntity.getClass());
			this.defaultValue = PoliceConstants.Value.Others.getValue();
		}

	}

	public int getX() {
		return (int) areaPosition.second().getX();
	}

	public int getY() {
		return (int) areaPosition.second().getY();
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public boolean isDoneWithoutCalc() {
		return done;
	}

	public boolean isDone() {
		if (done == false) {
			done = checkDone();
			return done;
		}
		return done;
	}

	private boolean checkDone() {
		PoliceForceAgent policeAgent = (PoliceForceAgent) getRealEntity().getAgent();
		if (!policeAgent.isReachableTo(realEntity.getPositionPair())) {
			return false;
		}
		//		if(!BlockadeEstimator.isActuallyMiddleBlockadesBiggerThanRealBlockades())
		//			this.done = true;
		return true;
	}

	public short getIndex() {
		return index;
	}

	/**
	 * agar police bood mosbat task done
	 * agar agent bood va asssign shode bood --->0
	 * agar agent select nashode bood -(value)
	 * 
	 * @return
	 */
	public int getWeight() {
		if (this.isAssigned()) {
			return 10000;
		}
		//		double jd = getRealEntity().model().sosAgent().newSearch.getRemainingJobScorer().getClusterJobDone(getAreaPosition());
		return (PoliceConstants.STANDARD_OF_MAP * PoliceConstants.DEFAULT_NEGATIVE_VALUE/** jd */
		);//TODO ba standarad map
		//		return -getFinalValue();
		//TODO

	}

	public void setFinalValue(ArrayList<Task<? extends StandardEntity>> tasks) {
		int value = this.defaultValue;

		for (Task<? extends StandardEntity> task : tasks) {

			value += (((defaultValue * task.defaultValue) / getDistanceTo2(task)));//Gravity rule (q*q)/(d^2)

		}

		realEntity.getAgent().sosLogger.trace("this task " + realEntity + "   have final value == " + value);
		setFinalValue(value);
	}

	private int getDistanceTo2(Task<? extends StandardEntity> task) {
		double dx = (this.getX() - task.getX()) / PoliceConstants.DISTANCE_UNIT;
		double dy = (this.getY() - task.getY()) / PoliceConstants.DISTANCE_UNIT;
		return Math.max(1, (int) (dx * dx + dy * dy));
	}

	protected void setFinalValue(int finalValue) {
		this.finalValue = finalValue;
	}

	public int getFinalValue() {
		return finalValue;
	}

	protected int distanceOf2Tasks(Task<StandardEntity> other) {
		double dx = this.getX() - other.getX();
		double dy = this.getY() - other.getY();
		return (int) Math.sqrt(dx * dx + dy * dy) / 1000;

	}

	public void setAssigner(PoliceForceTask policeToDoTasks) {
		assigner = policeToDoTasks;
	}

	public PoliceForceTask getAssigner() {
		return assigner;
	}

	public boolean isAssigned() {
		return assigner != null;
	}

	@Override
	public String toString() {
		return "[" + getRealEntity() + " index=" + index + "]";
	}

	public void setPosition(Pair<? extends Area, Point2D> pair) {
		this.areaPosition = pair;

	}

	public Pair<? extends Area, Point2D> getPositionPair() {
		return areaPosition;
	}

	public E getRealEntity() {
		return realEntity;
	}

	public Area getAreaPosition() {
		return getPositionPair().first();
	}

}

package sos.police_v2.state.preCompute;

import java.awt.Point;
import java.util.ArrayList;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.PoliceForce;
import sos.police_v2.PoliceConstants;

public class PoliceForceTask extends Task<PoliceForce> {
	private Pair<? extends Area, Point2D> temppositionPair;	
	private int jobDone=0;
	public ArrayList<Pair<Point,JobDone>> jobDoneHistory= new ArrayList<Pair<Point,JobDone>>();
	public PoliceForceTask(PoliceForce task, short index) {
		super(task, index);
	}

	@Override
	public int getWeight() {
		return jobDone;
	}
	public void setJobDone(int jobDone, boolean isMST) {
		this.jobDone = jobDone;
		jobDoneHistory.add(new Pair<Point, JobDone>(new Point(getX(), getY()), new JobDone(jobDone, isMST)));
	}
	public int getJobDone() {
		return jobDone;
	}

	public void addJobDone(long dijkstraWeight, boolean isMST) {
		setJobDone((int) (getJobDone()+dijkstraWeight),isMST);
	}
	
	@Override
	public void setDefaultValue() {
		if (getRealEntity().getPosition() instanceof Building){
			setDefaultValue(PoliceConstants.Value.PoliceForceInBuilding.getValue());
			setJobDone(PoliceConstants.DEFAULT_JOB_DONE_FOR_POLICE_IN_BUILDING*PoliceConstants.STANDARD_OF_MAP,false);
		}else
			setDefaultValue(PoliceConstants.Value.PoliceForce.getValue());
	}
	public void setDefaultValue(int value) {
		this.defaultValue=value;
	}
	public void setTempPosition(Pair<? extends Area, Point2D> positionPair) {
		this.temppositionPair = positionPair;
		
	}
	public class JobDone{
		private final boolean isMST;
		private final int jobDone2;

		public JobDone(int jobDone,boolean isMST) {
			this.jobDone2 = jobDone;
			this.isMST = isMST;
			// TODO Auto-generated constructor stub
		}

		public boolean isMST() {
			return isMST;
		}

		public int getJobDone() {
			return jobDone2;
		}
	}
	
	public Pair<? extends Area, Point2D> getTemppositionPair() {
		return temppositionPair;
	}

	public int getDefaultValue() {
		return defaultValue;
	}

	

}

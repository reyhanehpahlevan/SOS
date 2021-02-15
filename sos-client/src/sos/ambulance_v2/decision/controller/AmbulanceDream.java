package sos.ambulance_v2.decision.controller;

import java.util.ArrayList;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.base.AmbulanceConstants.CivilianState;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Civilian;
import sos.base.util.sosLogger.SOSLoggerSystem;

//***********************************************************************************************************
//***********************************************************************************************************
public class AmbulanceDream {
	public AmbulanceTeam at;
	public int time_To_be_free;
	public ArrayList<ImaginationShot> performing;
	public Pair<? extends Area, Point2D> lastPosition;
	private SOSLoggerSystem dclog;

	public AmbulanceDream(AmbulanceTeam at, int free_time) {
		this.at = at;
		this.dclog=at.getAgent().sosLogger.agent;
		this.time_To_be_free = free_time;
		if (time_To_be_free < at.model().time())
			time_To_be_free = at.model().time();
		if (at.getWork().getTarget() != null && at.getWork().getTarget().getRescueInfo().longLife())
			this.time_To_be_free = at.model().time();
		performing = new ArrayList<ImaginationShot>();
		lastPosition = at.getPositionPair();
	}

	public void reset() {
		this.time_To_be_free = at.getWork().getNextFreeTime();
		if (time_To_be_free < at.model().time())
			time_To_be_free = at.model().time();
		if (at.getWork().getTarget() != null && at.getWork().getTarget().getRescueInfo().longLife())
			this.time_To_be_free = at.model().time();
		performing.clear();
		lastPosition = at.getPositionPair();
	}

	public void addImagination(ImaginationShot is, int reachCost) {
		if (!performing.contains(is)) {
			boolean primary = false; //TODO edit and find the exact loader
			if (performing.isEmpty())
				primary = true;
			if (is.atAssigned.isEmpty())
				primary = true;
			performing.add(is);
			lastPosition = ambulanceLastPosition(primary);
			if (lastPosition == null)
				lastPosition = at.getPositionPair();

			int tp = (int) Math.ceil(is.target.getBuriedness() 
					/ (float) is.target.getRescueInfo().getATneedToBeRescued());
			
			tp += (is.target.getRescueInfo().getATneedToBeRescued() - 1);
			if (primary && is.target instanceof Civilian) {
				tp++; //for loading
				tp += is.target.getRescueInfo().getTimeToRefuge();
			}
			tp += reachCost;
			time_To_be_free += tp;
			dclog.logln("taskPerformingDuration  " + is.target + "   by " + at + " =" + tp);
			is.rescueMargin = is.conditionExpireTime - (time_To_be_free - 1); //for unload
			is.atAssigned.add(at);
			if (is.rescueMargin < 0) {
				switch (is.goalCondition) {
				case HEALTHY:
				case AVERAGE:
				case CRITICAL:
					is.resultCondition = CivilianState.DEATH;
				}
			} else {
				is.resultCondition = is.goalCondition;
			}
			dclog.logln("after adding " + is + " to --> " + this+" ==>ResultCond="+is.resultCondition);
		}
	}

	public Pair<? extends Area, Point2D> ambulanceLastPosition(boolean isPrimary) {
		if (performing.isEmpty()) {
			if (at.getWork().getTarget() == null || !at.getWork().getTarget().isPositionDefined() || at.getWork().getTarget().getRescueInfo().getBestRefuge() == null)
				return at.getPositionPair();
			if (at.getAgent().model().refuges().isEmpty())
				return at.getWork().getTarget().getPositionPair();
			if (isPrimary)
				return new Pair<Area, Point2D>(at.getWork().getTarget().getRescueInfo().getBestRefuge(), new Point2D(at.getWork().getTarget().getRescueInfo().getBestRefuge().getX(), at.getWork().getTarget().getRescueInfo().getBestRefuge().getY()));
			return at.getWork().getTarget().getPositionPair();
		}
		if (at.getAgent().model().refuges().isEmpty() || performing.get(performing.size() - 1).target.getRescueInfo().getBestRefuge() == null)
			return performing.get(performing.size() - 1).target.getPositionPair();
		return new Pair<Area, Point2D>(performing.get(performing.size() - 1).target.getRescueInfo().getBestRefuge(),
				new Point2D(performing.get(performing.size() - 1).target.getRescueInfo().getBestRefuge().getX()
						, performing.get(performing.size() - 1).target.getRescueInfo().getBestRefuge().getY()));
	}

	@Override
	public String toString() {
		return "[ " + at + " currentTarget:"+at.getWork().getTarget()+",FT=" + time_To_be_free + ",LP=" + lastPosition.first() + "->" + performing + " ]";
	}
}

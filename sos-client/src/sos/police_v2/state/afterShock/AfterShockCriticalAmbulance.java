package sos.police_v2.state.afterShock;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import rescuecore2.misc.Pair;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.PoliceAbstractState;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class AfterShockCriticalAmbulance extends PoliceAbstractState {
	Shape clusterShape;
	short lastAfterShock = 0;
	ArrayList<Pair<AmbulanceTeam, Integer>> isDone;
	boolean isCheckedAgain = true;
	boolean isMustOpenRefugeNow = false;

	ArrayList<Pair<AmbulanceTeam, Integer>> targetList;
	Pair<AmbulanceTeam, Integer> target;

	public AfterShockCriticalAmbulance(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterShape = model().searchWorldModel.getClusterData().getConvexShape();
		isDone = new ArrayList<Pair<AmbulanceTeam, Integer>>();
		targetList = new ArrayList<Pair<AmbulanceTeam, Integer>>();
	}

	@Override
	public void act() throws SOSActionException {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return;
			}
		}
		if (model().getLastAfterShockTime() != lastAfterShock) {
			log.warn("after shock shode dobare ali shirini bede " + model().getLastAfterShockTime());
			lastAfterShock = (short) model().getLastAfterShockTime();
			isDone.clear();
			setTarget();
			if (model().time() >= lastAfterShock + 3)
				isCheckedAgain = true;
		}
		if ((!isCheckedAgain) && model().time() >= lastAfterShock + 3) {
			setTarget();
			isCheckedAgain = true;
		}
		if (target != null) {
			if (targetIsDone(target)) {
				log.warn("at is reachable in aftershock ciritical at");
				isDone.add(target);
				target = null;
				isMustOpenRefugeNow = checkMust();
				if (!isMustOpenRefugeNow)
					setTarget();
			}
			if (target != null) {
				if (!stillValid()) {
					target = null;
					setTarget();
				}
			}
			if (target != null) {
				makeReachableTo(target.first());
			}
		}

	}

	private boolean stillValid() {
		if (target.first().isBuriednessDefined() && target.first().getBuriedness() != 0) {
			log.debug(target.first() + " skip to openning because it take buriedness");
			return false;
		}
		return true;
	}

	private boolean checkMust() {
		return false;
	}

	private boolean targetIsDone(Pair<AmbulanceTeam, Integer> select) {

		if (isReachableTo(select.first())) {
			log.info("at is reachable because isReachable method say its reachable ");
			return true;
		}
		if (agent.me().getAreaPosition().equals(select.first().getAreaPosition())) {
			log.info("at is reachable because my area position is equal with it");
			return true;
		}
		if (select.first().isReallyReachable(true)) {
			log.info("at is reachale because isReally Reachable method say its reachable ");
			return true;
		}
		return false;
	}

	private void setTarget() {
		targetList.clear();
		target = null;
		log.info("select target of AFTER SHOCK called");
		for (AmbulanceTeam at : model().ambulanceTeams()) {
			if (!clusterShape.contains(at.getX(), at.getY()))
				continue;
			if (!at.isBuriednessDefined())
				continue;
			if (at.getBuriedness() != 0)
				continue;
			int score = 0;
			if (at.getPositionArea() instanceof Building)
				score += numberOfAtInBuilding(at.getAreaPosition()) * 1000;

			Human atTarget = agent.getAmbulanceTeamTarget().get(at);
			if (isValidHuman(atTarget)) {
				int liveTime = atTarget.getRescueInfo().getDeathTime() - model().time();
				if (liveTime < 10) {
					log.debug(at + " skiped from at critical because target of it will die under 10 cycle after");
					continue;
				}
				score += (1000 - liveTime);
				if (atTarget.getAreaPosition().equals(at))
					score += 2000;

			} else
				continue;
			Pair<AmbulanceTeam, Integer> select = new Pair<AmbulanceTeam, Integer>(at, score);
			if (targetIsDone(select))
				isDone.add(select);
			if (!isDone.contains(select))
				targetList.add(select);
		}
		if (targetList.size() > 0) {
			Collections.sort(targetList, new atTargetComparator());
			log.warn("list of AT Target for aftershock = " + targetList);
			target = targetList.get(0);
		}
		log.warn("target that selected for after shock ciritical at =" + target);
	}

	private int numberOfAtInBuilding(Area areaPosition) {
		int index = 0;
		for (AmbulanceTeam at : model().ambulanceTeams()) {
			if (at.isPositionDefined() && at.getPositionArea().equals(areaPosition))
				index++;
		}
		return index;
	}

	private boolean isValidHuman(Human human) {
		if (human == null)
			return false;
		if (!human.isPositionDefined())
			return false;
		if (!human.isHPDefined())
			return false;
		if (human.getHP() < 100)
			return false;
		if (!human.isDamageDefined())
			return false;
		if (human.isBuriednessDefined() && human.getBuriedness() == 0)
			return false;

		return true;
	}

	private class atTargetComparator implements Comparator<Pair<AmbulanceTeam, Integer>> {

		@Override
		public int compare(Pair<AmbulanceTeam, Integer> o1, Pair<AmbulanceTeam, Integer> o2) {
			return o1.second().compareTo(o2.second()) * (-1);
		}

	}
}

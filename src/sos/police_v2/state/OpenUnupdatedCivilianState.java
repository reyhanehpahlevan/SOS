package sos.police_v2.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.tools.SimpleDeathTime;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.search_v2.tools.cluster.ClusterData;

public class OpenUnupdatedCivilianState extends PoliceAbstractState {

	public OpenUnupdatedCivilianState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {

	}

	Civilian assignCivil = null;

	@Override
	public void act() throws SOSActionException {
		log.info("acting as:" + this);
		log.debug("assigned cilvilian=" + assignCivil);
		//		clearEnterance();
		if (assignCivil != null && missionComplete(assignCivil)) {
			log.info("missoin complete!!" + assignCivil + " opened!");
			assignCivil.setIsReallyReachable(true);
			assignCivil = null;
		}
		if (!PoliceUtils.isValidCivilianWithOutDeadTimeCheck(assignCivil, agent, true)) {
			assignCivil = assignNewCivilian();
		} else if (isDeadTimeUpdated(assignCivil)) {
			assignCivil = assignNewCivilian();
		}
		log.debug("current assigned civilian is:" + assignCivil);
		if (assignCivil != null) {
			Pair<Area, Point2D> ep = getEntrancePoint(assignCivil.getAreaPosition());
			if (ep == null) {
				log.error("how?????");
				assignCivil = null;
				return;
			}
			moveToPoint(ep);//TODO TOO BUILDING MAGHSADESH NABASHE;)
		}

	}

	private void clearEnterance() throws SOSActionException {
		log.debug("clearing enterance for= " + assignCivil);
		if (assignCivil != null) {
			List<Area> enterances = assignCivil.getPositionArea().getNeighbours();
			log.debug("enterances " + enterances);
			for (Area area : enterances) {
				if (!isReachableTo(area.getPositionPair())) {
					log.debug("is not reachable to " + area);
					moveToPoint(area.getPositionPair());
				}
			}
		}
	}

	private boolean missionComplete(Civilian assignCivil) {
		if (agent.location().equals(assignCivil.getPosition()))
			return true;
		if (assignCivil.isReallyReachable(true))
			return true;
		if (assignCivil.getPosition().isReallyReachable(true))
			return true;
		if (agent.location().getNeighbours().contains(assignCivil.getPosition()) && agent.location().isBlockadesDefined() && agent.location().getBlockades().isEmpty())
			return true;
		if (isReallyOpen(assignCivil))
			return true;
		return false;
	}

	private boolean isReallyOpen(Civilian assignCivil2) {
		//		for (Area neighbour : assignCivil.getAreaPosition().getNeighbours()) {
		//			if(neighbour.getLastSenseTime()==agent.time()){
		//
		//			}
		//		}
		return false;
	}

	private Civilian assignNewCivilian() {
		//		PriorityQueue<Civilian> unreachableCivilians = getUnreachableCivilians();
		//		return unreachableCivilians.size()==0?null:unreachableCivilians.remove();
		Collection<Civilian> cives = new ArrayList<Civilian>();
		;
		if (agent.messageSystem.type == Type.LowComunication || agent.messageSystem.type == Type.NoComunication) {
			final ClusterData myCluster = agent.model().searchWorldModel.getClusterData();
			ArrayList<ClusterData> openCivilianClusterList = new ArrayList<ClusterData>();
			openCivilianClusterList.add(myCluster);
			ArrayList<ClusterData> cloneClusters = new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
			Collections.sort(cloneClusters, new Comparator<ClusterData>() {

				@Override
				public int compare(ClusterData o1, ClusterData o2) {
					double o1s = SOSGeometryTools.distance(myCluster.getX(), myCluster.getY(), o1.getX(), o1.getY());
					double o2s = SOSGeometryTools.distance(myCluster.getX(), myCluster.getY(), o2.getX(), o2.getY());
					if (o1s > o2s)
						return 1;
					if (o1s < o2s)
						return -1;
					return 0;

				}
			});
			int i = 0;
			for (ClusterData cd : cloneClusters) {
				openCivilianClusterList.add(cd);
				i++;
				if (i == 2)
					break;
			}
			for (Civilian civ : model().civilians()) {
				for (ClusterData clusterData : openCivilianClusterList) {
					if (civ.isPositionDefined() && clusterData.getBuildings().contains(civ.getPositionArea()))
						cives.add(civ);
				}
			}
			log.trace("all civilians are becase low comm ot no comm =" + cives);
		} else {
			//			if (model().civilians().size() < 70) {
			//				cives = getCivilianInRange((int) model().getBounds().getWidth() / 8);
			//			} else if (model().civilians().size() < 140) {
			//				cives = getCivilianInRange((int) model().getBounds().getWidth() / 4);
			//			} else {
			//				cives = model().civilians();
			//				log.trace("all civilians are becase civ size>140=" + cives);
			//			}
			ClusterData myCluster = agent.model().searchWorldModel.getClusterData();
			double myCLusterScore = agent.newSearch.getRemainingJobScorer().remainingJobScore(myCluster);
			ArrayList<ClusterData> openCivilianClusterList = new ArrayList<ClusterData>();
			openCivilianClusterList.add(myCluster);
			//			for (ClusterData cd : agent.model().searchWorldModel.getAllClusters()) {
			//				if (myCLusterScore < agent.newSearch.getRemainingJobScorer().remainingJobScore(cd))
			//					openCivilianClusterList.add(cd);
			//			}
			for (Civilian civ : model().civilians()) {
				for (ClusterData clusterData : openCivilianClusterList) {
					if (civ.isPositionDefined() && clusterData.getBuildings().contains(civ.getPositionArea()))
						cives.add(civ);
				}
			}
		}
		//		if (cives.isEmpty()) {
		//			HashSet<Building> clusterbuildings = agent.model().searchWorldModel.getClusterData().getBuildings();
		//			for (Civilian civ : model().civilians()) {
		//				if (civ.isPositionDefined() && clusterbuildings.contains(civ.getPositionArea()))
		//					cives.add(civ);
		//			}
		//		}
		Civilian best = getBestUnreachableCivilians(cives);
		//				log.warn("old select civilian " + getBestUnreachableCivilians(cives));
		//		log.warn("new select civilian with size =" + cives + " is" + best);

		return best;
	}

	/*
	 * private PriorityQueue<Civilian> getUnreachableCivilians() {
	 * PriorityQueue<Civilian> unreachableCivilians = new PriorityQueue<Civilian>(model().civilians().size(),new DistanceComparator());
	 * for (Civilian civilian : model().civilians()) {
	 * if(isValidCivilian(civilian))
	 * unreachableCivilians.add(civilian);
	 * }
	 * return unreachableCivilians;
	 * }
	 */
	private Civilian getBestUnreachableCivilians(Collection<Civilian> cives) {
		log.info("getBestUnreachableCivilians");
		Civilian best = null;
		DistanceComparator comparator = new DistanceComparator();
		for (Civilian civilian : cives) {
			if (PoliceUtils.isValidCivilianWithOutDeadTimeCheck(civilian, agent, true)) {
				if (!isDeadTimeUpdated(civilian)) {
					if (best == null || comparator.compare(best, civilian) < 0)
						best = civilian;
				} else
					log.debug(civilian + " is not valid because updated");
			}
			else
				log.debug(civilian + " valid nabod ");

		}
		//		log.info("BestUnreachableCivilian is:" + best);
		return best;
	}

	/*******************************
	 ** ADDED HESAM AKBARY
	 *******************************/
	private Civilian getBestUnreachableCivilians_V2(Collection<Civilian> cives) {
		log.info("getBestUnreachableCivilians v2");
		ArrayList<Civilian> sortByDistance = new ArrayList<Civilian>(cives);
		DistanceComparator comparator = new DistanceComparator();
		Collections.sort(sortByDistance, comparator);
		ArrayList<Civilian> sortByDeadTime = new ArrayList<Civilian>(cives);
		DeadTimeComparator comparator2 = new DeadTimeComparator();
		Collections.sort(sortByDeadTime, comparator2);
		//		System.err.println("DT:" + sortByDeadTime);
		//		System.err.println("DS:" + sortByDistance);

		Civilian best = null;
		int score = Integer.MAX_VALUE;
		for (Civilian civilian : cives) {
			if (!PoliceUtils.isValidCivilianWithOutDeadTimeCheck(civilian, agent, true))
				continue;
			if (isDeadTimeUpdated(civilian))
				continue;
			if (best == null) {
				best = civilian;
				score = sortByDistance.indexOf(civilian) + 2 * sortByDeadTime.indexOf(civilian);
				continue;
			}
			int temp = sortByDistance.indexOf(civilian) + 2 * sortByDeadTime.indexOf(civilian);
			if (temp < score) {
				best = civilian;
				score = temp;
			} else if (score == temp) {
				if (sortByDistance.indexOf(civilian) < sortByDistance.indexOf(best)) {
					best = civilian;
					score = temp;
				}
			}
		}
		log.info("BestUnreachableCivilian is(v2):" + best);
		return best;

	}

	@SuppressWarnings("unused")
	private Collection<Civilian> getCivilianInRange(int d) {
		ArrayList<Civilian> result = new ArrayList<Civilian>();
		for (Civilian civilian : model().civilians()) {
			if (civilian.isPositionDefined())
				if (PoliceUtils.getDistance(agent.me(), civilian) < d / PoliceConstants.DISTANCE_UNIT)
					result.add(civilian);
		}
		log.trace("civilians in range(" + d + ")=" + result);
		return result;
	}

	private class DistanceComparator implements java.util.Comparator<Civilian> {
		HashMap<Civilian, Integer> civ_cost = new HashMap<Civilian, Integer>();

		@Override
		public int compare(Civilian c1, Civilian c2) {
			Integer d1 = civ_cost.get(c1);
			if (d1 == null) {
				d1 = (int) (PoliceUtils.getDistance(agent.me(), c2) * (Math.random() * 25 + 75) / 100);
				civ_cost.put(c1, d1);
			}
			Integer d2 = civ_cost.get(c2);
			if (d2 == null) {
				d2 = (int) (PoliceUtils.getDistance(agent.me(), c2) * (Math.random() * 25 + 75) / 100);
				civ_cost.put(c2, d2);
			}

			double score1 = 1d;
			double score2 = 1d;
			if (c1.getPositionArea() instanceof Building)
				score1 = agent.newSearch.getRemainingJobScorer().getClusterRJS((Building) c1.getAreaPosition());
			if (c2.getPositionArea() instanceof Building)
				score2 = agent.newSearch.getRemainingJobScorer().getClusterRJS((Building) c2.getAreaPosition());

			double finalScore1 = score1 / (d1);
			double finalScore2 = score2 / (d2);
			if (finalScore2 < finalScore1)
				return -1;
			if (finalScore2 > finalScore1)
				return 1;

			return 0;
		}
	}

	private class DeadTimeComparator implements java.util.Comparator<Civilian> {
		HashMap<Civilian, Integer> civ_cost = new HashMap<Civilian, Integer>();

		@Override
		public int compare(Civilian c1, Civilian c2) {
			Integer d1 = civ_cost.get(c1);
			if (d1 == null) {
				d1 = c1.getRescueInfo().getDeathTime();
				civ_cost.put(c1, d1);
			}
			Integer d2 = civ_cost.get(c2);
			if (d2 == null) {
				d2 = c2.getRescueInfo().getDeathTime();
				civ_cost.put(c2, d2);
			}

			double score1 = 1d;
			double score2 = 1d;
			if (c1.getPositionArea() instanceof Building)
				score1 = agent.newSearch.getRemainingJobScorer().getClusterRJS((Building) c1.getAreaPosition());
			if (c2.getPositionArea() instanceof Building)
				score2 = agent.newSearch.getRemainingJobScorer().getClusterRJS((Building) c2.getAreaPosition());

			double finalScore1 = score1 / (d1);
			double finalScore2 = score2 / (d2);
			if (finalScore2 < finalScore1)
				return -1;
			if (finalScore2 > finalScore1)
				return 1;

			return 0;
		}
	}

	private boolean isDeadTimeUpdated(Civilian civilian) {
		int deadTime = SimpleDeathTime.getEasyLifeTime(civilian.getHP(), civilian.getDamage(), civilian.updatedtime());
		log.info("check for is civilian updated ot not  simpleDeadTime=" + deadTime + "   DeathTime=" + civilian.getRescueInfo().getDeathTime());
		if (civilian.getRescueInfo().getDeathTime() == deadTime)
			return false;
		return true;
	}
}
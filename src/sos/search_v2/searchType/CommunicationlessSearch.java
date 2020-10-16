package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sos.base.SOSAgent;
import sos.base.SOSWorldModel;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Edge;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.Utils;

/**
 * @author Salim Malakouti
 * @param <E>
 */
public class CommunicationlessSearch<E extends Human> extends SearchStrategy<E> {
	private Human centeralMan;
	public static final int CRITICAL_CENTERAL_MAN_TIME = 80;
	private Road gatheringArea = null;

	public CommunicationlessSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
		selectCenteralRoad();
		setCenteralMan();
	}

	private void selectCenteralRoad() {
		gatheringArea = selectGatheringArea(me.model());
	}

	public static Road selectGatheringArea(SOSWorldModel model) {
		Road gatheringArea = null;
		int cx = (int) model.mapCenter().getX();
		int cy = (int) model.mapCenter().getY();
		Collection<Road> validRoads = model.getObjectsInRange(cx, cy, (int) model.getBounds().getWidth() / 8, Road.class);
		double maxScore = Double.MIN_VALUE;
		double minDistance = 0;
		int totalX=0;
		int totalY=0;
		for (Building r : model.buildings()) {
			totalX += r.getX();
			totalY += r.getY();
		}
		totalX/=model.buildings().size();
		totalY/=model.buildings().size();
		for (Road r : validRoads) {

			if (haveBuildingNeighbor(r))
				continue;

			double distance = Utils.distance(r.getX(), r.getY(), totalX, totalY) / 50000;
			distance = Math.min(100, distance);
			double score = r.getNeighbours().size() * r.getNeighbours().size() / (distance);
			if (score > maxScore || (score == maxScore && Utils.distance(r.getX(), r.getY(), totalX, totalY) < minDistance)) {
				maxScore = score;
				gatheringArea = r;
				minDistance = Utils.distance(r.getX(), r.getY(), totalX, totalY);
			}
		}
		return gatheringArea;
	}

	private static boolean haveBuildingNeighbor(Road r) {
		if(r.getNeighbours().size()<=3)
			return true;
		for (Area neighbor : r.getNeighbours()) {
			if (neighbor instanceof Building)
				return true;
			for (Area nn : neighbor.getNeighbours()) {
				if (nn instanceof Building)
					return true;
			}

		}
		return false;
	}

	public static Road selectGatheringArea_old(SOSWorldModel model) {
		Road gatheringArea = null;
		double meanDistance = 0;
		double meanHeight = 0;
		int cx = (int) model.mapCenter().getX();
		int cy = (int) model.mapCenter().getY();

		for (Road r : model.roads()) {
			meanDistance += Utils.distance(r.getX(), r.getY(), cx, cy);
			meanHeight += getHeight(r);

		}
		meanHeight /= model.roads().size();
		meanDistance /= model.roads().size();

		double maxScore = Double.MIN_VALUE;
		double minDistance = 0;

		for (Road r : model.roads()) {
			if (r.getNeighbours().size() > 2) {
				double distance = Utils.distance(r.getX(), r.getY(), cx, cy);
				if (distance < meanDistance)
					distance = meanDistance;
				double height = getHeight(r);
				if (height > meanHeight * getNormalizationCoeficient())
					height = meanHeight * getNormalizationCoeficient();
				distance /= 1000;
				height /= 1000;
				double score = (height) / (distance * distance * distance);
				if (score > maxScore || (score == maxScore && Utils.distance(r.getX(), r.getY(), cx, cy) < minDistance)) {
					maxScore = score;
					gatheringArea = r;
					minDistance = Utils.distance(r.getX(), r.getY(), cx, cy);
				}
			}
		}
		return gatheringArea;
	}

	public static double getHeight(Area a) {
		double mean = 0;
		double min = Integer.MAX_VALUE;
		for (Edge e : a.getEdges()) {
			double d = Utils.distance(e.getStartX(), e.getStartY(), e.getEndX(), e.getEndY());
			mean += d;
			if (d < min) {
				min = d;
			}
		}
		mean = mean / a.getEdges().size();
		if (mean > min * getNormalizationCoeficient())
			mean = min * getNormalizationCoeficient();
		return mean;
	}

	public static int getNormalizationCoeficient() {
		return 2;
	}

	public double getArea(Road r) {
		return r.getGeomArea().getBounds2D().getWidth() * r.getGeomArea().getBounds2D().getHeight();
	}

	private static void addNotInBuildingHumanToList(List<Human> result, List<? extends Human> agents) {
		for (Human human : agents) {
			if (human.getAreaPosition() instanceof Building)
				continue;
			result.add(human);
		}
	}

	public static Human selectCenteralMan(SOSWorldModel model) {
		List<Human> agents = new ArrayList<Human>();
		addNotInBuildingHumanToList(agents, model.policeForces());
		if (agents.isEmpty())
			addNotInBuildingHumanToList(agents, model.fireBrigades());
		if (agents.isEmpty())
			addNotInBuildingHumanToList(agents, model.ambulanceTeams());
		Road ga = selectGatheringArea(model);

		double minD = Double.MAX_VALUE;
		Human best = null;
		for (Human h : agents) {
			double d = Utils.distance(ga.getLocation(), h.getLocation());
			if (d < minD) {
				minD = d;
				best = h;
			}
		}
		return best;
	}

	private void setCenteralMan() {
		centeralMan = selectCenteralMan(me.model());
		//		List<Human> agents = new ArrayList<Human>();
		//		if (me.model().policeForces().size() != 0) {
		//			if (!(me instanceof PoliceForceAgent))
		//				return;
		//			agents.addAll(me.model().policeForces());
		//		} else if (me.model().fireBrigades().size() != 0) {
		//			if (!(me instanceof FireBrigadeAgent))
		//				return;
		//			agents.addAll(me.model().fireBrigades());
		//		} else if (me.model().ambulanceTeams().size() != 0) {
		//			if (!(me instanceof AmbulanceTeamAgent))
		//				return;
		//			agents.addAll(me.model().ambulanceTeams());
		//		}
		//		//		int maxAgents = Math.max(me.model().policeForces().size(), Math.max(me.model().ambulanceTeams().size(), me.model().fireBrigades().size()));
		//		//		int minAgents = Math.min(me.model().policeForces().size(), Math.min(me.model().ambulanceTeams().size(), me.model().fireBrigades().size()));
		//		//
		//		//		if (maxAgents > 1.5 * minAgents) {
		//		//			if (maxAgents == me.model().policeForces().size()) {
		//		//				agents.addAll(me.model().policeForces());
		//		//			} else if (maxAgents == me.model().ambulanceTeams().size()) {
		//		//				agents.addAll(me.model().fireBrigades());
		//		//			} else if (maxAgents == me.model().fireBrigades().size()) {
		//		//				agents.addAll(me.model().ambulanceTeams());
		//		//			}
		//		//		} else {
		//		//			if (me.getMapInfo().isBigMap() || me.getMapInfo().isMediumMap()) {
		//		//				agents.addAll(me.model().policeForces());
		//		//			} else {
		//		//				agents.addAll(me.model().fireBrigades());
		//		//			}
		//		//		}
		//		double minD = Integer.MAX_VALUE;
		//		ClusterData best = null;
		//		FOR: for (ClusterData cd : me.model().searchWorldModel.getAllClusters()) {
		//			double d = Utils.distance(cd.getX(), cd.getY(), me.model().mapCenter().getX(), me.model().mapCenter().getY());
		//			if (d < minD) {
		//				for (Human human : agents) {
		//					ClusterData tmp = me.model().searchWorldModel.getClusterData(human);
		//					if (tmp != null) {
		//						if (tmp.equals(cd)) {
		//							if ((human.getAreaPosition() instanceof Building)) {
		//								continue FOR;
		//							}
		//						}
		//					}
		//				}
		//
		//				minD = d;
		//				best = cd;
		//			}
		//		}
		//		centeralClusterData = best;
		//
		//		for (Human human : agents) {
		//			ClusterData cd = me.model().searchWorldModel.getClusterData(human);
		//			if (cd != null) {
		//				if (cd.equals(centeralClusterData)) {
		//					centeralMan = human;
		//				}
		//			}
		//		}
		//		if (centeralMan == null) {
		//			minD = Integer.MAX_VALUE;
		//
		//			me.sosLogger.search.warn("No CenteralMan");
		//			for (Human human : agents) {
		//				double d = Utils.distance(human.getX(), human.getY(), me.model().mapCenter().getX(), me.model().mapCenter().getY());
		//				if (d < minD) {
		//					centeralMan = human;
		//					minD = d;
		//				}
		//
		//			}
		//
		//		}
		//		me.sosLogger.search.debug(centeralMan);

	}

	public boolean isNoCommunication() {
		return me.messageSystem.type == Type.NoComunication;
	}

	public SearchBuilding chooseTask() {
		//		if(imCenteral()){
		//			getCenteralManTask()
		//		}
		//		log("last target--->" + lasttarget2);
		//		long t1 = System.currentTimeMillis();
		//
		//		long weight = 0;
		//		if (lasttarget2 != null)
		//			weight = getWeightTo(lasttarget2);
		//
		//		log("Current weight: " + weight + " last weight:" + lastWeight + " lastWeight*coef:" + (searchTools.getWeightIncreaseCoef(me) * lastWeight) + "    time: " + (System.currentTimeMillis() - t1));
		//
		//		//		SearchBuilding toSearch = lasttarget;
		//		if (isTaskDone(lasttarget2, lastWeight, weight))
		//			lasttarget2 = searchTools.getBestCivilianSearchArea(input);
		//
		//		log("Search going for act time: " + (System.currentTimeMillis() - t1));
		//		log("checking building: " + lasttarget2 + " for civilian search.");
		//		if (lasttarget2 != null) {
		//			lastWeight = getWeightTo(lasttarget2);
		//			return lasttarget2;
		//		}
		//		log("civilian search had no task.  RETURNING!!!");
		return null;
	}

	private ArrayList<ShapeInArea> getCenteralManTask() {
		//		if (me.time() > CRITICAL_CENTERAL_MAN_TIME) {
		//			return ((ArrayList<ShapeInArea>) Collections.singleton(new ShapeInArea(gatheringArea.getApexList(), gatheringArea)));
		//		} else { //keep on doing you part on search
		//			if (me instanceof PoliceForceAgent) {
		//				return me.newSearch.communicationLessSearchTask();
		//			}
		//
		//		}
		return null;
	}

	private boolean imCenteral() {
		return centeralMan != null && me.getID().equals(centeralMan);
	}

	private boolean isTaskDone(SearchBuilding lasttarget, double lastWeight, double weight) {

		return (lasttarget == null || !lasttarget.needsToBeSearchedForCivilian() || weight > searchTools.getWeightIncreaseCoef(me) * lastWeight);
	}

	@Override
	public SearchTask searchTask() {
		//		log("Communicationless Search.......................................");
		//		if (lasttarget != null)
		//			removeBuggySearchArea(lasttarget.getRealBuilding());
		//		ArrayList<ShapeInArea> task = chooseTask(lasttarget, lastWeight, searchWorld.getClusterData().getBuildings());
		//		if (task == null)
		//			return null;
		//		return new SearchTask(task);
		return new SearchTask(null);

	}

	@Override
	public SearchType getType() {
		return SearchType.CivilianSearch;
	}

	public Human getCenteralMan() {
		return centeralMan;
	}

	public Road getGatheringArea() {
		return gatheringArea;
	}

}

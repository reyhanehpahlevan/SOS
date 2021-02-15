package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.HashSet;

import sos.ambulance_v2.tools.SimpleDeathTime;
import sos.base.SOSAgent;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.base.util.SOSGeometryTools;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.information_stacker.CycleInformations;
import sos.base.util.information_stacker.act.MoveAction;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.SearchUtils;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.Utils;

/**
 * @author Salim Malakouti
 * @param <E>
 */
public class CivilianUpdateSearch<E extends Human> extends SearchStrategy<E> {

	public CivilianUpdateSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	private long lastWeight = 0;
	private SearchBuilding lasttarget = null;
	private HashSet<Building> ignoredBuildings = new HashSet<Building>();
	private int startSearchLastTarget = -1;

	public ArrayList<ShapeInArea> chooseTask() {
		log("last target--->" + lasttarget);
		long t1 = System.currentTimeMillis();

		long weight = 0;
		if (lasttarget != null)
			weight = getWeightTo(lasttarget);

		log("Current weight/last weight: " + weight / Math.max(1, lastWeight) + "    time: " + (System.currentTimeMillis() - t1));

		SearchBuilding toSearch = lasttarget;
		if (lasttarget != null && isTaskDone(weight)) {
			ignoredBuildings.add(lasttarget.getRealBuilding());
		}
		if (isTaskDone(weight) || shouldChangeTask(weight))
			toSearch = getBestBuildingWithUpdatableCivilian();

		log("Search going for act time: " + (System.currentTimeMillis() - t1));
		log("checking building: " + toSearch + " for civilian search.");
		if (toSearch != null) {
			startSearchLastTarget = me.time();
			this.lasttarget = toSearch;
			this.lastWeight = getWeightTo(lasttarget);
			return toSearch.getRealBuilding().getSearchAreas();
		}
		log("civilian search had no task.  RETURNING!!!");
		this.lasttarget = null;
		startSearchLastTarget = -1;
		return null;
	}

	private SearchBuilding getBestBuildingWithUpdatableCivilian() {
		reset();
		ArrayList<Building> buildings = getProperBuildings();
		return chooseTarget(buildings);

	}

	private SearchBuilding chooseTarget(ArrayList<Building> buildings) {
		log("Scoring Buildings [count: " + buildings.size() + "] ...." + buildings);
		Building best = null;
		double bestScore = -1.0;
		for (Building b : buildings) {
			SearchBuilding sb = searchWorld.getSearchBuilding(b);
			double civilianScore = getCivilianScore(b);
			double cost = getWeightTo(sb);
			double rjs = getRemainingJobScorer().getClusterRJS(b);
			double score = (civilianScore * rjs) / cost;
			sb.setScore(score, log("Building:" + b + " civilainScore:" + civilianScore + " cost:" + cost + " rjs:" + rjs + " score:" + score));
			if (score > bestScore) {
				bestScore = score;
				best = b;
			}
		}
		log("Best: " + best + " Score:" + bestScore);
		if (best == null)
			return null;
		return getSearchWorldMode().getSearchBuilding(best);

	}

	public double getCivilianScore(Building b) {
		double sum = 0;
		int tt = SearchUtils.SIMULATION_TIME(me.time());
		ArrayList<Civilian> civilians = b.getCivilians();
		log("########## Computing civilian score of building: " + b);
		for (Civilian c : civilians) {
			int dt = SimpleDeathTime.getEasyLifeTime(c.getHP(), c.getDamage(), c.updatedtime());
			log("           Computing civilian [ " + c + " ] score of building: " + b + " total time: " + tt + " simple death time: " + dt + " score: " + (tt - dt));
			sum += (tt - dt);
		}
		log("computing civilian score: " + sum + " num:" + civilians.size());
		return Math.max(1, sum);
	}

	private void reset() {
		long t1 = System.currentTimeMillis();
		log("reseting ");
		for (Building b : searchWorld.model().buildings()) {
			searchWorld.getSearchBuilding(b).setScore(0);
		}
		log("reseting " + " Time=" + (System.currentTimeMillis() - t1));
	}

	private boolean needsToBeUpdated(Civilian c, boolean checkDistance, ClusterData cd, double range) {
		log("Civilian: " + c);
		log("me.time() - c.updatedtime(): " + (me.time() - c.updatedtime()) + " min update time:" + MIN_UPDATE_TIME(me));

		if (c.getPosition() == null)//Yoosef
			return false;
		if (c.getPosition() instanceof Road)//Yoosef
			return false;
		if (c.getPosition() instanceof AmbulanceTeam)//Yoosef
			return false;
		if (c.getPosition() instanceof Refuge)//Yoosef
			return false;
		if (!(c.getPosition() instanceof Building))
		{
			me.sosLogger.search.warn("Civilian Search Update : civilian has undefined position " + c + "  " + c.getPosition());
			return false;
		}
		SearchBuilding sb = searchWorld.getSearchBuilding(((Building) c.getPosition()));

		if (me.time() - c.updatedtime() < MIN_UPDATE_TIME(me)) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("Civilians where not old enough "));
			return false;
		}

		log("position: " + c.getPosition());
		if (c.getPosition() == null) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("Civilian's Position was null"));
			return false;
		}

		if (!(c.getPosition() instanceof Building)) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("Civilian's Position was not a building"));
			return false;
		}

		if (SearchUtils.isUpdatedCivlian(c)) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("Civilian was updated "));
			return false;
		}
		if (!isValidtoUpdate(c, me)) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("isValidtoUpdate civilian"));
			return false;
		}
		double d = Utils.distance(me.me().getX(), me.me().getY(), c.getX(), c.getY());
		boolean contains = cd.getBuildings().contains(c.getPosition());

		if (!(d < range || contains) && checkDistance) {
			sb.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("Not in range or in cluster [ distance: " + d + " range:" + range + " inRange:" + (d < range) + " contains:" + contains + " ]"));
			return false;
		}
		return true;

	}

	private ArrayList<Building> getProperBuildings() {
		log("Choosing buildings according to their civilians");
		ClusterData cd = me.model().searchWorldModel.getClusterData();
		ArrayList<Building> result = new ArrayList<Building>();
		double range = RANGE();
		log("----- range:" + range);
		for (Civilian c : me.model().civilians()) {
			if (needsToBeUpdated(c, true, cd, range)) {
				if (!result.contains(c.getPosition())) {
					log("Building added to list: " + c.getPosition());
					result.add((Building) c.getPosition());
				} else {
					log("NOT ADDED => was already in list: " + c.getPosition());
				}
			}
		}
		return result;
	}

	public static int MIN_UPDATE_TIME(SOSAgent<?> me) {
		if (me.getMapInfo().isBigMap())
			return 40;
		if (me.getMapInfo().isMediumMap())
			return 35;
		return 30;
	}

	private double RANGE() {
		int base = 6;
		if (me.getMapInfo().isBigMap())
			base = 10;
		if (me.getMapInfo().isMediumMap())
			base = 8;
		return (Math.max(me.model().getBounds().getWidth(), me.model().getBounds().getHeight()) / base);
	}

	private boolean isTaskDone(double weight) {
		if (lasttarget == null)
			return true;

		ArrayList<Civilian> civilians = lasttarget.getRealBuilding().getCivilians();
		if (ignoredBuildings.contains(lasttarget))
			return true;
		if (civilians.isEmpty())
			return true;
		if (civiliansAreUpdated(civilians))
			return true;
		if (lasttarget.getRealBuilding().getLastSearchedForCivilianTime() > me.model().time() - 10)
			return true;

		if (startSearchLastTarget > 0 && startSearchLastTarget < me.time() - 5) {
			CycleInformations cycle5Ago = me.informationStacker.getInformations(5);
			if (cycle5Ago.getAct() instanceof MoveAction) {
				if (cycle5Ago.getPositionPair().first().equals(me.me().getPositionArea()))
					return true;
				if (SOSGeometryTools.getDistance(me.me().getPositionPoint(), cycle5Ago.getPositionPair().second()) < 5000)
					return true;
			}
		}
		return false;
		//|| weight > searchTools.getWeightIncreaseCoef(me) * lastWeight
	}

	private boolean shouldChangeTask(double weight) {
		if (lasttarget == null)
			return true;
		return weight > searchTools.getWeightIncreaseCoef(me) * lastWeight;

	}

	private boolean civiliansAreUpdated(ArrayList<Civilian> civilians) {
		double range = RANGE();
		for (Civilian c : civilians) {
			if (needsToBeUpdated(c, false, me.model().searchWorldModel.getClusterData(), range))
				return false;
		}
		return true;
	}

	@Override
	public SearchTask searchTask() {
		log("Updating Civilians...............");
		ArrayList<ShapeInArea> task = chooseTask();
		log("Chosen task of update civilians: " + task);

		if (task == null)
			return null;
		return new SearchTask(task);

	}

	@Override
	public SearchType getType() {
		return SearchType.CivilianUpdateSearch;
	}

	public static boolean isValidtoUpdate(Civilian civ, SOSAgent<?> me) {
		if (civ == null) {
			return false;
		}
		if (civ.isUnkonwnCivilian()) {
			return false;
		}
		if (!civ.isPositionDefined()) {
			return false;
		}
		if (!(civ.getPosition() instanceof Building)) {
			return false;
		}
		//		if (civ.isReallyReachableSearch()) {
		//			return false;
		//		}
		if (civ.getHP() == 0) {
			return false;
		}
		if (civ.getDamage() > 200) {
			return false;
		}
//		if (SimpleDeathTime.getEasyLifeTime(civ.getHP(), civ.getDamage(), civ.updatedtime()) - me.model().time() < civ.getBuriedness() / 3 + 5) {
//			return false;
//		}
		return true;
	}
}

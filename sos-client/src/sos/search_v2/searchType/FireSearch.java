package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.Iterator;

import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.move.MoveConstants;
import sos.base.util.geom.ShapeInArea;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.SearchUtils;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;

/**
 * @author Yoosef Golshahi
 * @param <E>
 */
public class FireSearch<E extends Human> extends SearchStrategy<E> {

	public FireSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) throws InstantiationException, IllegalAccessException {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	@Override
	public String log(String st) {
		me.sosLogger.search.info(st);
		return st;
	}

	SearchBuilding target;
	int time = -1;
	ArrayList<Building> fire;

	public ArrayList<ShapeInArea> chooseTask() {
		long t1 = System.currentTimeMillis();
		log("search Time fire0 " + (System.currentTimeMillis() - t1));

		fire = searchTools.getBuildingWithFireProbability();
		log("buildings with fire probability: " + fire);

		log("search Time fire3 " + (System.currentTimeMillis() - t1));

		if (fire.size() == 0)
			return null;

		setBuidlingsPriority();
		log("fiery buidlings after scoring and fitlering: " + fire);
		SearchBuilding best = null;
		double bestSc = Integer.MIN_VALUE;
		for (Building b : fire) {
			SearchBuilding kk = searchWorld.getSearchBuilding(b);
			if (kk.getScore() > bestSc && getValidVisibleArea(b).size() > 0 && kk.getRealBuilding().getEstimator() == null) {
				best = kk;
				bestSc = kk.getScore();
			}
		}

		log("search Time fire3 " + (System.currentTimeMillis() - t1));
		if (best == null) {
			log("not best found");
			return null;
		}

		log("Best chosen as building: " + best.getRealBuilding() + " score:" + bestSc);
		time = me.model().time();
		target = best;
		best.setTarget(searchWorld.model().time());
		log("search Time fire4 " + (System.currentTimeMillis() - t1));
		return getValidVisibleArea(best.getRealBuilding());
	}

	private void setBuidlingsPriority() { //Salim
		log("Setting buildings priorities ");
		for (Iterator<Building> it = fire.iterator(); it.hasNext();) {
			Building b = it.next();
			reset(b);
			log("Building: " + b);
			if (SearchUtils.skipDueRJ(me, b)) {
				double rjs = me.newSearch.getRemainingJobScorer().remainingJobScore(me.model().searchWorldModel.getClusterData());
				double destRJS = me.newSearch.getRemainingJobScorer().getClusterRJS(b);
				me.newSearch.getSearchWorld().getSearchBuilding(b).setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, log("FILTERED because of RJ - rjs:" + rjs + " destRJS: " + destRJS));
				it.remove();
				continue;
			}
			if (!setOtherPriories(b)) {
				it.remove();
				continue;
			}
			log("Building's Score before RJS: " + searchWorld.getSearchBuilding(b).getScore());
			me.newSearch.getRemainingJobScorer().applyClusterJobDone(me.model().searchWorldModel.getSearchBuilding(b));
			log("Building's Score after RJS: " + searchWorld.getSearchBuilding(b).getScore());
		}
	}

	private boolean setOtherPriories(Building build) {
		SearchBuilding b = searchWorld.getSearchBuilding(build);
		long weight = getWeightTo(build.fireSearchBuilding().sensibleAreasOfAreas());
		if ((!(me instanceof PoliceForceAgent)) && weight>MoveConstants.UNREACHABLE_COST) {
			b.addScore("reachibility ", 1 * AgentSearchScore.SEARCH_FILLTER_SCORE);
			return false;
		}

//		b.addScore("build upate", SearchUtils.decimalScaleTime((me.model().time() - b.getRealBuilding().updatedtime()), 500) * scoreFunction.getLastUpdatedCoef());

		b.addScore("move cost", SearchUtils.decimalScaleCost(weight, me.getMapInfo(), me) * scoreFunction.getCostCoef());

		b.addScore("special for fire", b.isSpecialForFire() * scoreFunction.getSpecialForFireCoef());

		return true;
	}

	private void reset(Building b) {
		searchWorld.getSearchBuilding(b).setScore(0);
		searchWorld.getSearchBuilding(b).addScore("Fire Search", 0);
	}


	private ArrayList<ShapeInArea> getValidVisibleArea(Building building) {
		ArrayList<ShapeInArea> sensibleAreas = building.fireSearchBuilding().sensibleAreasOfAreas();
		return sensibleAreas;
//		ArrayList<ShapeInArea> newShapes = new ArrayList<ShapeInArea>();
//		for (ShapeInArea sh : building.getSearchAreas()) {
//			if (isValid(sh, building)) {
//				newShapes.add(sh);
//			}
//		}
//		if (newShapes.size() <= 1) {
//			for (ShapeInArea sh : building.fireSearchBuilding().sensibleAreasOfAreas()) {
//				if (isValid(sh, building)) {
//					newShapes.add(sh);
//				}
//			}
//		}
//		return newShapes;
	}

	private boolean isValid(ShapeInArea shape, Building building) {
		if (me instanceof PoliceForceAgent && shape.getArea(building.model()) instanceof Road)
			return true;
		else if ((shape.getArea(building.model()) instanceof Road) && !(me.move.isReallyUnreachable(shape)))
			return true;
		return false;
	}

	private boolean visible(SearchBuilding target2) {
		if (me.model().time() - target2.getRealBuilding().updatedtime() < 4)
			return true;
		return false;

	}

	@Override
	public SearchTask searchTask() {
		log("Fire Search.......................................");
		ArrayList<ShapeInArea> task = chooseTask();
		if (task == null)
			return null;
		return new SearchTask(task);
	}

	@Override
	public SearchType getType() {
		return SearchType.FireSearch;
	}
}

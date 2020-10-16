package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.move.MoveConstants;
import sos.base.util.SOSGeometryTools;
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
public class CombinedSearch<E extends Human> extends SearchStrategy<E> {

	public CombinedSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) throws InstantiationException, IllegalAccessException {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	private SearchBuilding target;
	private double lastWeight = 0;
	int time = -1;
	int numberOfTimeOut = 0;

	@Override
	public SearchTask searchTask() {
		log("Combine Search.......................................");

		double weight = 0;
		if (target != null)
			weight = getWeightTo(target);
		if (isTargetDone() || weight > searchTools.getWeightIncreaseCoef(me) * lastWeight) {
			target = chooseTarget();
			if (target == null)
				return null;
			lastWeight = getWeightTo(target);
		} else {
			lastWeight = weight;
		}
		log("last Target " + target);
		if (target.getRealBuilding().isSearchedForCivilian())
			return new SearchTask(target.getRealBuilding().fireSearchBuilding().sensibleAreasOfAreas());
		return new SearchTask(target.getRealBuilding().getSearchAreas());
	}

	public SearchBuilding chooseTarget() {
		reset();
		updateLastTargetScore();
		long t1 = System.currentTimeMillis();
		log("search Time Update last target score " + (System.currentTimeMillis() - t1));
		hearDistancePriority();
		log("search Time hearDistancePriority  " + (System.currentTimeMillis() - t1));
		visibleAreaPriority();
		log("search Time visibleAreaPriority " + (System.currentTimeMillis() - t1));
		visibleInSideAreaPriority();
		log("search Time visibleInSideAreaPriority " + (System.currentTimeMillis() - t1));
		//		updatedPriority();
		//		log("search Time updatedPriority " + (System.currentTimeMillis() - t1));
		updatedNeighbourPriority();
		log("search Time updatedNeighbourPriority " + (System.currentTimeMillis() - t1));
		PriorityQueue<SearchBuilding> selected = otherPriority();

		log("search Time Other Priority " + (System.currentTimeMillis() - t1));
		SearchBuilding best = null;
		double bestSc = Integer.MIN_VALUE;
		//		Collection<Building> buildings = me.model().buildings();//getObjectsInRange(me.me().getX(), me.me().getY(), 100000, Building.class);
		for (SearchBuilding b : selected) {
			if (b.getScore() > bestSc /*&& isReachable(b)*/) {
				best = b;
				bestSc = b.getScore();
			}
		}
		log("Search best is set, Time :" + (System.currentTimeMillis() - t1));
		target = best;
		if (best == null) {
			log("Nothing To choose (best was null)...");
			numberOfTimeOut--;// TODO What is it?
			return null;
		}
		time = me.model().time();
		log("New target " + target + " resons:" + target.reason);
		best.setTarget(searchWorld.model().time());
		log("Combine Search Choose Task done: " + (System.currentTimeMillis() - t1));
		if (me.isTimeToActFinished())
			numberOfTimeOut++;
		else
			numberOfTimeOut--;

		return best;
	}

	private void updateLastTargetScore() {
		if (me.model().time() == time + 1) {//FIXME it causes dangling.plus alaki switch mikone
			if (!isTargetDone()) /* && me.model().time() - target.getRealBuilding().updatedtime() > 3 */{
				target.addScore("Previous Task Score", 1 * scoreFunction.getLastTargetCoef());
			} else {
				target = null;
			}
		}
	}

	private boolean isTargetDone() {

		if (target == null) {
			log("DONE target was null");
			return true;
		}
		if (seeInSide(target)) {
			log("DONE: " + target + " see in side===>target done...");
			return true;
		}
		if (!(me instanceof PoliceForceAgent) && me.move.isReallyUnreachable(target.getRealBuilding().getSearchAreas())) {
			log("DONE: " + target + " I am not a police and i was not reachable");
			return true;
		}
		if (!target.needsToBeSearchedForCivilian() && target.isSearchedForFire()) {
			log("DONE: " + target + " is searched for civilian and searched for fire");
			return true;
		}
		log("Target: " + target + " was not done...");
		return false;
	}

	private boolean seeInSide(SearchBuilding t) {
		return t.isHasBeenSeen() && (t.getRealBuilding().getLastSenseTime() == me.time() || t.getRealBuilding().getLastSenseTime() == (me.time() - 1));
	}

	private void reset() {
		long t1 = System.currentTimeMillis();
		log("reseting ");
		for (Building b : searchWorld.model().buildings()) {
			searchWorld.getSearchBuilding(b).setScore(0);
		}
		log("reseting " + " Time=" + (System.currentTimeMillis() - t1));
	}

	public void hearDistancePriority() {
		long t1 = System.currentTimeMillis();
		log("Hear  Distance Priority");
		for (Building b : searchTools.getBuildingsInHearDistance()) {
			SearchBuilding temp = searchWorld.getSearchBuilding(b);
			temp.addScore("HearDistancePriority", 1 * scoreFunction.getHearingDistanceCoef());
		}
		log("Hear Distance Priority " + " Time=" + (System.currentTimeMillis() - t1));
	}

	public void visibleAreaPriority() {
		log("Visible Area Priority");
		for (Building b : searchTools.getVisibleBuilding()) {
			SearchBuilding temp = searchWorld.getSearchBuilding(b);
			temp.addScore("VisibleAreaPriority", 1 * scoreFunction.getVisibleAreaCoef());
		}
		log("Visible Area Priority ");
	}

	public void visibleInSideAreaPriority() {
		long t1 = System.currentTimeMillis();
		log("Visible In Side Area Priority");
		ArrayList<Building> visible = searchTools.getVisibleBuilding();
		for (Building b : visible) {
			SearchBuilding temp = searchWorld.getSearchBuilding(b);
			if (temp.isHasBeenSeen())
				temp.addScore("visibleInSide", AgentSearchScore.SEARCH_FILLTER_SCORE);
		}
		log("Visible In Side Priority " + " Time=" + (System.currentTimeMillis() - t1));
	}

	public void updatedNeighbourPriority() {
		long t1 = System.currentTimeMillis();
		log("Visible In Side Area Priority");
		for (Building b : searchTools.getUpdatedBuildingNeighbour()) {
			SearchBuilding temp = searchWorld.getSearchBuilding(b);
			temp.addScore("update neighbour priority", 1 * scoreFunction.getUpdatedNeighbourCoef());
		}
		log("Visible IN Side Priority " + " Time=" + (System.currentTimeMillis() - t1));
	}

	public PriorityQueue<SearchBuilding> otherPriority() {//FIXME too zone khodesh bashe biroon nemire.
		long t1 = System.currentTimeMillis();
		log("Other Priority");
		int distance = (int) (me.model().getBounds().getWidth() / 5);
		Collection<Building> buildings = me.model().getObjectsInRange(me.me().getX(), me.me().getY(), distance, Building.class);
		//Removing previous buildings
		HashSet<Building> all = new HashSet<Building>(searchWorld.getClusterData().getBuildings());
		all.addAll(buildings);
		if (target != null && !all.contains(target.getRealBuilding())) {
			all.add(target.getRealBuilding());
		}
		PriorityQueue<SearchBuilding> hesabBuilding = scoringList(all);
		if (hesabBuilding.isEmpty()) {
			ArrayList<Building> newList = new ArrayList<Building>(me.model().buildings());
			newList.removeAll(all);
			hesabBuilding = scoringList(newList);
		}
		log("OtherPriority " + " Time=" + (System.currentTimeMillis() - t1));
		return hesabBuilding;

	}

	public PriorityQueue<SearchBuilding> scoringList(Collection<Building> all) {
		long t1 = System.currentTimeMillis();
		log("Scoring List");
		PriorityQueue<SearchBuilding> hesabBuilding = new PriorityQueue<SearchBuilding>(50, new Comparator<SearchBuilding>() {
			@Override
			public int compare(SearchBuilding o1, SearchBuilding o2) {
				return (int) (o2.getScore() - o1.getScore());
			}
		});

		FOR: for (Building element : all) {
			SearchBuilding b = me.model().searchWorldModel.getSearchBuilding(element);

			if (b.getRealBuilding() instanceof Refuge) {
				b.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, "is Refuge");
				continue;
			}
			if (((me.time() - b.getRealBuilding().updatedtime()) < 20) && b.getRealBuilding().isSearchedForCivilian()) {
				b.setScore(AgentSearchScore.SEARCH_FILLTER_SCORE, "update tme <20");
				continue;
			}
			if (b.getRealBuilding().isEitherFieryOrBurnt()) {
				b.addScore("FILTER:firey Buildings:", AgentSearchScore.SEARCH_FILLTER_SCORE);
				continue FOR;
			}

			b.addScore("civilian probability", scoreFunction.getHearCoef() * b.getNormalizedCivProbability());
			ArrayList<Civilian> civilInBuildings = b.getRealBuilding().getCivilians();
			if (civilInBuildings.isEmpty() && !b.needsToBeSearchedForCivilian() && b.isSearchedForFire()) {
				b.addScore("civilInBuildings.isEmpty() && !b.needsToBeSearchedForCivilian()&&b.isSearchedForFire()", scoreFunction.getIsSearchedForFireAndCivilian());
			}

			b.addScore("civilian upate", SearchUtils.decimalScale((me.model().time() - b.getRealBuilding().updatedtime()) * b.getRealBuilding().getCivilians().size(), 1999) * scoreFunction.getCivilianUpdateCoef());

			long cost = getWeightTo(b); // For Police cost is always less than MoveConstants.UNREACHABLE_COST
			if (cost >= MoveConstants.UNREACHABLE_COST) {
				b.addScore("FILTER: not reachibility ", AgentSearchScore.SEARCH_FILLTER_SCORE);
				continue FOR;
			}
			if (b.getRealBuilding().isBrokennessDefined() && b.getRealBuilding().getBrokenness() > 0)
				b.addScore("Brokeness", scoreFunction.getBrokeNessCoef());
			if (b.getRealBuilding().isBrokennessDefined() && b.getRealBuilding().getBrokenness() == 0 && (!(me instanceof PoliceForceAgent) || !isReachable(b)))
				b.addScore("No Brokeness", scoreFunction.getNoBrokeNessCoef());
			b.addScore("move cost:", scoreFunction.getCostCoef() * SearchUtils.decimalScaleCost(cost, me.getMapInfo(), me));

			double distanceToCluster = SOSGeometryTools.distance(myClusterData.getX(),myClusterData.getY(), me.me().getX(),me.me().getY())/1000;

			b.addScore("distanceToCluster:", scoreFunction.getCostCoef() * SearchUtils.decimalScaleCost(distanceToCluster, me.getMapInfo(), me));

			b.addScore("build update", SearchUtils.decimalScaleTime((me.model().time() - b.getRealBuilding().updatedtime()), 500) * scoreFunction.getLastUpdatedCoef());

			if (!b.isHasBeenSeen())
				b.addScore("has not beean seen", scoreFunction.getNotBeenSeenCoef());
			else
				b.addScore("has been seen", scoreFunction.getHasBeenSeenCoef());

			if (searchWorld.getClusterData().getBuildings().contains(element))
				b.addScore("my cluster", scoreFunction.getMyClusterCoef());

			if (b.getRealBuilding().getEstimator() != null && b.getRealBuilding().virtualData[0].getTemperature() < 20)
				b.addScore("temp", scoreFunction.getLowTempertureEstimatedFiteSiteCoef());

			if (b.getRealBuilding().getEstimator() != null && b.getRealBuilding().virtualData[0].getTemperature() > 20) {
				b.addScore("burning", scoreFunction.getHighTempertureEstimatedFiteSiteCoef());
			}

			//			if(!b.getRealBuilding().isEitherFieryOrBurnt()){
			//				b.addScore("distance to cluster", SearchUtils.decimalScaleDistance(distanceToCluster, me.getMapInfo(), me) * scoreFunction.getDistanceToFunctionCoef());
			//			}
			if (b.getRealBuilding().isSearchedForCivilian() && b.getRealBuilding().getCivilians().isEmpty()) {
				b.addScore("search for civilian", scoreFunction.getNoCivilianCoef());
			}
			//			double distanceToCluster = Utils.distance(searchWorld.getClusterData().getX(), searchWorld.getClusterData().getY(), b.getRealBuilding().getX(), b.getRealBuilding().getY());
			//			b.addScore("distance to cluster", SearchUtils.decimalScaleDistance(distanceToCluster, me.getMapInfo(), me) * scoreFunction.getDistanceToFunctionCoef());

			//			if(!b.getRealBuilding().isf)
			//			double distanceToCluster = Utils.distance(searchWorld.getClusterData().getX(), searchWorld.getClusterData().getY(), b.getRealBuilding().getX(), b.getRealBuilding().getY());
			//			b.addScore("distance to cluster", SearchUtils.decimalScaleDistance(distanceToCluster, me.getMapInfo(), me) * scoreFunction.getDistanceToFunctionCoef());

			if (cost > 15)
				b.addScore("one cycle move", scoreFunction.getOneCycleMoveCoef() / ((cost / 25) +1));

			me.newSearch.getRemainingJobScorer().applyClusterJobDone(b);

			hesabBuilding.add(b);
		}
		log("Scoring List" + " Time=" + (System.currentTimeMillis() - t1));
		return hesabBuilding;
	}

	@Override
	public SearchType getType() {
		return SearchType.CombinedSearch;
	}

}

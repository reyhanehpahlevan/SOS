package sos.search_v2.searchType;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.SOSAgent;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Road;
import sos.base.move.types.PoliceMove;
import sos.base.move.types.StandardMove;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.fire_v2.FireBrigadeAgent;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.RemainingJobScorer;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.SearchUtils;
import sos.search_v2.tools.cluster.StarZone;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.Utils;

/**
 * @author Salim
 * @param <E>
 */

public class StarSearch<E extends Human> extends SearchStrategy<E> {
	public static int STAR_TALES = 8;
	public static double CLUSTER_SEARCH_THRESHOLD = 0.9;
	public static final int STAR_SUB_ZONES = 4;
	private StarZone[] starZones;
	private int startTimeOnCurrentCluster = 0;
	private int currentZone = -1;
	private int currentSubZone = -1;
	private int lastTimeOnSearch = 0;
	private int preComputeAssignedZone;
	private int preComputeAssignedSubZone;
	private StarSearchType type;
	private Road gatheringArea = null;
	private int timeAtGPZ = 0;
	private int timeAtGPM = 0;
	public static final int GHATHER_TIME = 80;
	public static final double RJS_SEARCHED_PERCENT = 0.3;
	private double[][] initialRJS;
	//---------------------------------
	private long lastWeight = 0;
	SearchBuilding lasttarget = null;

	enum StarSearchType {
		SEARCH_FOR_CIVILIAN, SEARCH_FOR_FIRE, TOTAL_SEARCH, RJS_THRESHOLD, ;
	}

	public StarSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, StarSearchType type, AgentSearchScore score, AgentSearch agentSearch) {
		super(me, searchWorld, score, agentSearch);
		this.type = type;
		createStarZones();
		assignInitialZone();
		setInitialRJS();
	}

	private void setInitialRJS() {
		initialRJS = new double[starZones.length][STAR_SUB_ZONES];
		for (StarZone st : starZones) {
			for (int i = 0; i < STAR_SUB_ZONES; i++) {
				initialRJS[st.getIndex()][i] = RemainingJobScorer.computeRJ(me, agentSearch, st.getSubZones()[i]);
			}
		}

	}

	public void chooseZone() {
		log("Checking of needs to choose a new zone");
		// If agent has been searching with out stop in between
		boolean isSearched = isSearched(getCurrentZone(), getCurrentSubZone());
		if (currentZone == -1 || currentSubZone == -1 || isSearched) {
			log("Choosing Zone [ currentZone: " + currentZone + "currentSubZone: " + currentSubZone + " isSearched: " + isSearched + "] ");
			Pair<Integer, Integer> zoneInfo = nextSubZone();
			currentZone = zoneInfo.first();
			currentSubZone = zoneInfo.second();
			log("newZone: " + currentZone + " newSubZone:" + currentSubZone);
		}
	}

	private int nextZone(int current) {
		return (current + 1 + STAR_TALES / 2) % STAR_TALES;
	}

	private Pair<Integer, Integer> nextSubZone() {
		int nextZone = getCurrentZone();
		int nextSubZone = getCurrentSubZone();
		int countOnZone = 1;
		while (true) {
			if (isSearched(nextZone, nextSubZone) || (nextSubZone == getCurrentSubZone() && nextZone == getCurrentZone())) {
				nextSubZone = (nextSubZone + 1 + STAR_SUB_ZONES / 2) % STAR_SUB_ZONES;
				countOnZone++;
			} else {
				return new Pair<Integer, Integer>(nextZone, nextSubZone);
			}
			if (countOnZone == STAR_SUB_ZONES) {
				countOnZone = 1;
				nextZone = (nextZone + STAR_TALES / 2) % STAR_TALES;
				nextSubZone = getBestSubZoneInZone(nextZone);
			}

		}
	}

	private int getBestSubZoneInZone(int zone) {
		double minDistance = Double.MAX_VALUE;
		int best = -1;
		for (int i = 0; i < STAR_SUB_ZONES; i++) {
			double d = Utils.distance(getStarZones()[zone].getCx(), getStarZones()[zone].getCy(), me.model().mapCenter().getX(), me.model().mapCenter().getY());
			if (d < minDistance) {
				minDistance = d;
				best = i;
			}
		}
		return best;
	}

	public int getPairCluster(int index) {
		return (index + STAR_TALES / 2) % STAR_TALES;
	}

	@Override
	public SearchTask searchTask() throws SOSActionException {
		log("Starting StarSearch......");
		Collection<ShapeInArea> task = chooseTask();
		if (task == null)
			return null;
		return new SearchTask(task);
	}
	private Collection<ShapeInArea> chooseTask() throws SOSActionException {

		log("Starting StarSearch on zone:" + currentZone + " subZone:" + currentSubZone);
		stayAtGatheringPoint();
		log("Agent didnt rest -------> checking if it sould go to GPM");
		if (isGatheringTimeOfMap())
			return Collections.singleton(new ShapeInArea(gatheringArea.getApexList(), gatheringArea));
		log("Agents didnt go to GPM  -------> checking if it sould go to GPZ ");
		if (isGatheringTimeOfZone())
			return Collections.singleton(new ShapeInArea(getGatheringPointOfZone().getApexList(), getGatheringPointOfZone()));
		log("Agents didnt go to GPZ  -------> Choose cluster");
		chooseZone();
		log("chooseZone() done  -------> chose: zone:" + currentZone + "  subZone:" + currentSubZone);
		return searchCurrentZone();
	}

	private void stayAtGatheringPoint() throws SOSActionException {
		Area gp = null;
		int stayTime = 0;
		if (isGatheringTimeOfMap()) {
			gp = gatheringArea;
			stayTime = timeAtGPM;
		} else if (isGatheringTimeOfZone()) {
			gp = getGatheringPointOfZone();
			stayTime = timeAtGPZ;
		}

		if (gp == null)
			return;

		double distanceToGP = Utils.distance(me.me().getPositionPoint().getX(), me.me().getPositionPoint().getY(), gp.getX(), gp.getY());
		log("distance to center ");
		if (gp != null && distanceToGP < 29100 && stayTime < STAYING_TIME()) {

			if (isGatheringTimeOfMap()) {
				timeAtGPM++;
			} else if (isGatheringTimeOfZone()) {
				timeAtGPZ++;
			}
			me.rest();
		}
	}

	public Road getGatheringPointOfZone() {
		return getStarZones()[getCurrentZone()].getGatheringRoad();
	}

	private boolean isGatheringTimeOfMap() {
		log("Checking isGhateringTimeOfMap");
		int moveTime = me.move.getMovingTimeFrom(SearchUtils.getWeightTo(gatheringArea, me));
		boolean isTime = (me.time() + moveTime >= nextGatheringTimeOfMap() && (nextGatheringTimeOfMap() - me.time() <= MAX_GHATHERING_TIME()));
		boolean canGo = (moveTime <= nextGatheringTimeOfMap() - me.time()) && moveTime < MAX_GHATHERING_TIME();
		log("Check gathering time of MAP [isTime: " + isTime + " canGo: " + canGo + "]");
		return isTime && canGo;
	}

	private boolean isGatheringTimeOfZone() {

		int moveTime = me.move.getMovingTimeFrom(SearchUtils.getWeightTo(getGatheringPointOfZone(), me));
		boolean isTime = (me.time() + moveTime >= nextGatheringTimeOfZone() && (nextGatheringTimeOfZone() - me.time() <= MAX_GHATHERING_TIME()));
		boolean canGo = (moveTime <= nextGatheringTimeOfZone() - me.time()) && moveTime < MAX_GHATHERING_TIME();
		log("Check gathering time of ZONE [isTime: " + isTime + " canGo: " + canGo + "]");
		return isTime && canGo;
	}

	private int nextGatheringTimeOfMap() {
		return ((me.time() / GATHERING_TIME_PERIOD_MAP()) + 1) * (GATHERING_TIME_PERIOD_MAP());
	}

	private int nextGatheringTimeOfZone() {
		return ((me.time() / GATHERING_TIME_PERIOD_ZONE()) + 1) * (GATHERING_TIME_PERIOD_ZONE());
	}

	private void goTo(Road r) throws SOSActionException {
		log("Moving to Road: " + r);
		if (me instanceof PoliceForceAgent)
			me.move.move(r, PoliceMove.class);
		else
			me.move.move(r, StandardMove.class);
	}

	private ArrayList<ShapeInArea> searchCurrentZone() {

		log(">>>>>>>>>> Searching CurrentZone: " + currentZone + " CurrentSubZone: " + currentSubZone);
		if (type.equals(StarSearchType.SEARCH_FOR_FIRE))
			return fireSearch();
		else if (type.equals(StarSearchType.SEARCH_FOR_CIVILIAN))
			return civilianSearch();
		else if (type.equals(StarSearchType.RJS_THRESHOLD))
			return civilianSearch();
		else
			return totalSearch();
	}


	private ArrayList<ShapeInArea> totalSearch() {
		throw new Error(" totalSearch not implemented");
	}

	private ArrayList<ShapeInArea> civilianSearch() {
		log(">>>>>>>>>> searching zone as civilianSearch");
		lasttarget = me.newSearch.strategyChooser.civilianSearch.chooseTask(lasttarget, lastWeight, getCurrentAssignedSubZone());
		log(">>>>>>>>>> new task is: " + lasttarget);
		if (lasttarget == null)
			return null;
		
		return lasttarget.getRealBuilding().getSearchAreas();
	}

	private ArrayList<ShapeInArea> fireSearch() {
		throw new Error(" totalSearch not implemented");
	}

	

	private SearchTask searchCurrentClusterTask() {
		if (type.equals(StarSearchType.SEARCH_FOR_FIRE))
			return fireSearchTask();
		else if (type.equals(StarSearchType.SEARCH_FOR_CIVILIAN))
			return civilianSearchTask();
		else
			return totalSearchTask();
	}

	private SearchTask totalSearchTask() {
		return null;
	}

	private SearchTask civilianSearchTask() {
		return null;
	}

	private SearchTask fireSearchTask() {
		return null;
	}

	//*******************************************************************************************
	//*******************************************************************************************
	//************************************* Pre-compute assignment **********************************
	//*******************************************************************************************
	//*******************************************************************************************
	public void assignInitialZone() {
		if (me instanceof PoliceForceAgent) {
			//			assignPolice();
		} else {
			boolean[][] policeSubZoneflags = assignPolice();
			if (policeSubZoneflags == null)
				policeSubZoneflags = getNoBlockMatrix();

			if (me instanceof FireBrigadeAgent) {
				assignFireBrigade(policeSubZoneflags);
			} else if (me instanceof AmbulanceTeamAgent) {
				assignAmbulanceTeam(policeSubZoneflags);
			}
		}
		currentSubZone = preComputeAssignedSubZone;
		currentZone = preComputeAssignedZone;
	}

	private boolean[][] getNoBlockMatrix() {
		boolean[][] policeSubZoneflags = new boolean[STAR_TALES][STAR_SUB_ZONES];
		for (int i = 0; i < policeSubZoneflags.length; i++) {
			for (int j = 0; j < policeSubZoneflags[i].length; j++) {
				policeSubZoneflags[i][j] = true;
			}
		}
		return policeSubZoneflags;
	}

	private boolean[][] assignPolice() {
		if (me.model().policeForces() == null && me.model().policeForces().size() == 0)
			return null;
		boolean[][] result = new boolean[STAR_TALES][STAR_SUB_ZONES];
		ArrayList<PoliceForce> validPolices = getValiPoliceForces();
		ArrayList<StarZone> zones = getStarZonesOrderedByImportance(validPolices);

		int totalAgents = validPolices.size();
		int handlableZones = totalAgents / agentsPerZone();
		handlableZones = Math.min(handlableZones, STAR_TALES);
		int agentPerZone = totalAgents / handlableZones;
		int leftOvers = totalAgents - handlableZones * agentPerZone;
		boolean[] flags = new boolean[me.model().policeForces().size()];

		for (int i = 0; i < handlableZones; i++) {
			int agentsForZone = agentPerZone;

			if (i == 0)
				agentsForZone += leftOvers;
			PoliceForce[] assigned = new PoliceForce[agentsForZone];
			PriorityQueue<Human> q = new PriorityQueue<Human>(totalAgents, new HumanComparator(zones.get(i).getCx(), zones.get(i).getCy()));
			for (int j = 0; j < totalAgents; j++) {
				if (!flags[j])
					q.add(validPolices.get(j));
			}

			for (int j = 0; j < agentsForZone; j++) {
				Human a = q.remove();
				assigned[j] = (PoliceForce) a;
				//marking as selected
				flags[((PoliceForce) a).getPoliceIndex()] = true;
				//checking if is me
				if (a.getID().equals(me.getID())) {
					setPreComputeAssignedZone(i);
				}
			}
			zones.get(i).setPolices(assigned);
			//Assigning SubZones
			PriorityQueue<Pair<Pair<Integer, Point2D>, Double>> queue = new PriorityQueue<Pair<Pair<Integer, Point2D>, Double>>(agentPerZone, new Comparator<Pair<Pair<Integer, Point2D>, Double>>() {
				@Override
				public int compare(Pair<Pair<Integer, Point2D>, Double> a1, Pair<Pair<Integer, Point2D>, Double> a2) {
					if (a1.second() > a2.second())
						return 1;
					else if (a1.second() < a2.second())
						return -1;
					return 0;
				}
			});
			for (int k = 0; k < zones.get(i).getSubZoneCount(); k++) {//TODO apply agent coordinations is a parameter
				queue.add(new Pair<Pair<Integer, Point2D>, Double>(new Pair<Integer, Point2D>(k, new Point2D(zones.get(i).getCx(), zones.get(i).getCy())), SearchUtils.minDistanceOf(zones.get(i).getSubZones()[k], zones.get(i).getCx(), zones.get(i).getCy())));
			}
			boolean[] cflags = new boolean[zones.get(i).getPolices().length];
			for (int k = 0; k < agentPerZone; k++) {
				Pair<Pair<Integer, Point2D>, Double> sz = queue.remove();
				double minDistance = Integer.MAX_VALUE;
				int best = -1;
				for (int j = 0; j < zones.get(i).getPolices().length; j++) {
					if (cflags[j])
						continue;
					double d = Utils.distance(zones.get(i).getPolices()[j].getX(), zones.get(i).getPolices()[j].getY(), sz.first().second().getX(), sz.first().second().getY());
					if (d < minDistance) {
						minDistance = d;
						best = j;
					}
				}
				//------
				result[i][sz.first().first()] = true;
				// preventing best to be chosen again
				cflags[best] = true;

				// checking if I am the best
				if (zones.get(i).getPolices()[best].getID().equals(me.getID())) {
					setPreComputeAssignedSubZone(sz.first().first());
				}
			}
		}
		return result;
	}

	private ArrayList<PoliceForce> getValiPoliceForces() {
		ArrayList<PoliceForce> result = new ArrayList<PoliceForce>(me.model().policeForces().size());
		for (PoliceForce pf : me.model().policeForces()) {
			if (pf.getPosition() instanceof Building)
				continue;
			result.add(pf);
		}
		return result;
	}

	private ArrayList<StarZone> getStarZonesOrderedByImportance(ArrayList<? extends Human> agents) {
		ArrayList<StarZone> sorted = new ArrayList<StarZone>();
		//		sorted.add(starZones[0]);
		for (int i = 1; i < starZones.length; i++) {
			starZones[i].setValue(0);
			double minDistance = starZones[i].minDistanceSum(agents);
			int size = starZones[i].size();
			starZones[i].setValue(size / (minDistance * minDistance));
			sorted.add(starZones[i]);
		}
		Collections.sort(sorted, new Comparator<StarZone>() {

			@Override
			public int compare(StarZone o1, StarZone o2) {
				if (o1.getValue() > o2.getValue())
					return -1;
				else if (o1.getValue() > o2.getValue())
					return 1;
				return 0;
			}
		});
		sorted.add(0, starZones[0]);
		return sorted;
	}

	private void assignAmbulanceTeam(boolean[][] policeSubZoneflags) {
		if (me.model().ambulanceTeams() == null && me.model().ambulanceTeams().size() == 0)
			return;

		int totalAgents = me.model().ambulanceTeams().size();
		int handlableZones = totalAgents / agentsPerZone();
		handlableZones = Math.min(handlableZones, STAR_TALES);
		int agentPerZone = totalAgents / handlableZones;
		int leftOvers = totalAgents - handlableZones * agentPerZone;
		boolean[] flags = new boolean[totalAgents];

		for (int i = 0; i < handlableZones; i++) {
			int agentsForZone = agentPerZone;
			if (i == 0)
				agentsForZone += leftOvers;
			AmbulanceTeam[] assigned = new AmbulanceTeam[agentsForZone];
			PriorityQueue<Human> q = new PriorityQueue<Human>(totalAgents, new HumanComparator(getStarZones()[i].getCx(), getStarZones()[i].getCy()));
			for (int j = 0; j < totalAgents; j++) {
				if (!flags[j])
					q.add(me.model().ambulanceTeams().get(j));
			}
			for (int j = 0; j < agentsForZone; j++) {
				Human a = q.remove();
				assigned[j] = (AmbulanceTeam) a;
				//marking as selected
				flags[((AmbulanceTeam) a).getAmbIndex()] = true;
				//checking if is me
				if (a.getID().equals(me.getID())) {
					setPreComputeAssignedZone(i);
				}
			}
			starZones[i].setAmbulances(assigned);
			//Assigning SubZones
			PriorityQueue<Pair<Pair<Integer, Point2D>, Double>> queue = new PriorityQueue<Pair<Pair<Integer, Point2D>, Double>>(agentPerZone, new Comparator<Pair<Pair<Integer, Point2D>, Double>>() {
				@Override
				public int compare(Pair<Pair<Integer, Point2D>, Double> a1, Pair<Pair<Integer, Point2D>, Double> a2) {
					if (a1.second() > a2.second())
						return 1;
					else if (a1.second() < a2.second())
						return -1;
					return 0;
				}
			});
			for (int k = 0; k < getStarZones()[i].getSubZoneCount(); k++) {//TODO apply agent coordinations is a parameter
				queue.add(new Pair<Pair<Integer, Point2D>, Double>(new Pair<Integer, Point2D>(k, new Point2D(getStarZones()[i].getCx(), getStarZones()[i].getCy())), SearchUtils.minDistanceOf(getStarZones()[i].getSubZones()[k], getStarZones()[i].getCx(), getStarZones()[i].getCy())));
			}
			boolean[] cflags = new boolean[getStarZones()[i].getAmbulances().length];
			for (int k = 0; k < agentPerZone; k++) {
				Pair<Pair<Integer, Point2D>, Double> sz = queue.remove();
				double minDistance = Integer.MAX_VALUE;
				int best = -1;
				for (int j = 0; j < getStarZones()[i].getAmbulances().length; j++) {
					if (cflags[j])
						continue;
					double d = Utils.distance(getStarZones()[i].getAmbulances()[j].getX(), getStarZones()[i].getAmbulances()[j].getY(), sz.first().second().getX(), sz.first().second().getY());
					if (d < minDistance) {
						minDistance = d;
						best = j;
					}
				}
				// preventing best to be chosen again
				cflags[best] = true;

				// checking if I am the best
				if (getStarZones()[i].getAmbulances()[best].getID().equals(me.getID())) {
					setPreComputeAssignedSubZone(sz.first().first());
				}
			}
		}

	}

	private void assignFireBrigade(boolean[][] policeSubZoneflags) {
		if (me.model().fireBrigades() == null && me.model().fireBrigades().size() == 0)
			return;

		int totalAgents = me.model().fireBrigades().size();
		int handlableZones = (int) Math.ceil((double) totalAgents / (double) agentsPerZone());
		handlableZones = Math.min(handlableZones, STAR_TALES);
		int agentPerZone = totalAgents / handlableZones;
		int leftOvers = totalAgents - handlableZones * agentPerZone;
		boolean[] flags = new boolean[totalAgents];

		for (int i = 0; i < handlableZones; i++) {
			int agentsForZone = agentPerZone;
			if (i == 0)
				agentsForZone += leftOvers;
			FireBrigade[] assigned = new FireBrigade[agentsForZone];

			PriorityQueue<Human> q = new PriorityQueue<Human>(totalAgents, new HumanComparator(getStarZones()[i].getCx(), getStarZones()[i].getCy()));
			for (int j = 0; j < totalAgents; j++) {

				if (!flags[j])
					q.add(me.model().fireBrigades().get(j));
			}
			for (int j = 0; j < agentsForZone; j++) {
				Human f = q.remove();
				assigned[j] = (FireBrigade) f;
				//marking as selected
				flags[((FireBrigade) f).getFireIndex()] = true;
				//checking if is me
				if (f.getID().equals(me.getID())) {
					setPreComputeAssignedZone(i);
					//Assigning SubZones
					int best = 0;
					double bestScore = -1;
					for (int k = 0; k < getStarZones()[i].getSubZoneCount(); k++) {//TODO apply agent coordinations is a parameter
						if (policeSubZoneflags[i][k]) {
							double minDistance = SearchUtils.minDistanceOf(getStarZones()[i].getSubZones()[k], getStarZones()[i].getCx(), getStarZones()[i].getCy());
							double score = 1 / (minDistance * minDistance);
							if (score > bestScore) {
								bestScore = score;
								best = k;
							}
						}
					}
					setPreComputeAssignedSubZone(best);

				}
			}
			getStarZones()[i].setFires(assigned);
		}

	}

	private int agentsPerZone() {
		if (me.getMapInfo().isBigMap())
			return Math.min(6, STAR_SUB_ZONES);
		else
			return Math.min(3, STAR_SUB_ZONES);
	}

	//*******************************************************************************************
	//*******************************************************************************************
	//************************************* Creating Zones and subs**********************************
	//*******************************************************************************************
	//*******************************************************************************************

	public Pair<Integer, Integer> getMeanCenter() {
		int cx = 0;
		int cy = 0;

		for (Building b : me.model().buildings()) {
			cx += b.getX();
			cy += b.getY();

		}
		return new Pair<Integer, Integer>(cx / me.model().buildings().size(), cy / me.model().buildings().size());
	}

	public double getMaxDistance(int cx, int cy) {
		double maxDistance = -1;
		for (Building b : me.model().buildings()) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			if (distance > maxDistance)
				maxDistance = distance;
		}
		return maxDistance;
	}

	public double getArea(Road r) {
		return r.getGeomArea().getBounds2D().getWidth() * r.getGeomArea().getBounds2D().getHeight();
	}

	public int getNormalizationCoeficient() {
		return 3;
	}

	public void getGatheringArea(ArrayList<Building>[] starAreas, int cx, int cy) {

		double meanDistance = 0;
		double meanArea = 0;
		double meanHeight = 0;
		double meanNeighbours = 0;
		double meanAreaScale = 0;
		int validNeighbourRoads = 0;

		for (Building b : starAreas[0]) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			meanDistance += distance;
		}
		meanDistance /= starAreas[0].size();

		for (Road r : me.model().roads()) {
			meanArea += getArea(r);
			if (me.model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount() > 2) {
				meanNeighbours += me.model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				validNeighbourRoads++;
			}
			meanHeight += r.getGeomArea().getBounds2D().getHeight();

		}
		for (Road r : me.model().roads()) {
			meanAreaScale += (getArea(r) / meanArea);

		}
		meanArea /= me.model().roads().size();
		meanHeight /= me.model().roads().size();
		meanNeighbours /= validNeighbourRoads;
		meanAreaScale /= me.model().roads().size();

		double maxScore = Double.MIN_VALUE;
		double minDistance = 0;
		for (Road r : me.model().roads()) {
			if (r.getNeighbours().size() > 2) {
				double distance = Utils.distance(r.getX(), r.getY(), cx, cy);
				if (distance < meanDistance)
					distance = meanDistance;

				double area = getArea(r);
				if (area > meanArea)
					area = meanArea;

				double nCount = me.model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				if (nCount > meanNeighbours * getNormalizationCoeficient())
					nCount = meanNeighbours * getNormalizationCoeficient();

				double height = r.getGeomArea().getBounds2D().getHeight();

				if (height > meanHeight * getNormalizationCoeficient())
					height = meanHeight * getNormalizationCoeficient();

				double width = r.getGeomArea().getBounds2D().getWidth();

				double areaScale = (height * width) / meanArea;
				if (areaScale > meanAreaScale)
					areaScale = meanAreaScale;

				double score = (height * areaScale * nCount) / (distance * distance * distance);
				if (score > maxScore || (score == maxScore && Utils.distance(r.getX(), r.getY(), cx, cy) < minDistance)) {
					maxScore = score;
					gatheringArea = r;
					minDistance = Utils.distance(r.getX(), r.getY(), cx, cy);
				}
			}
		}

	}

	public Road[] getGatheringAreas() {
		if (getStarZones() == null)
			return new Road[0];
		Road[] result = new Road[getStarZones().length + 1];

		result[0] = gatheringArea;
		for (int i = 0; i < getStarZones().length; i++) {
			result[i + 1] = getStarZones()[i].getGatheringRoad();
		}
		return result;
	}

	public Road getGatheringPointOfMap() {
		return gatheringArea;
	}

	private void createStarZones() {
		ArrayList<Building>[] starAreas = new ArrayList[STAR_TALES + 1];
		Point2D[] centPoints = new Point2D[STAR_TALES + 1];
		setStarZones(new StarZone[STAR_TALES + 1]);
		float taleAngle = 360 / STAR_TALES;
		//initializing starAreas
		Pair<Integer, Integer> meanCenter = getMeanCenter();
		int cx = meanCenter.first();
		int cy = meanCenter.second();

		for (int i = 0; i < starAreas.length; i++) {
			starAreas[i] = new ArrayList<Building>();
			centPoints[i] = new Point2D(0, 0);

		}
		double maxDistance = getMaxDistance(cx, cy);
		for (Building b : me.model().buildings()) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			if (distance < maxDistance / 3) {
				starAreas[0].add(b);
				centPoints[0].setCoordinations(centPoints[0].getX() + b.getX(), centPoints[0].getY() + b.getY());
			} else {
				int index = ((int) (SearchUtils.getAngle(b, cx, cy) / taleAngle)) + 1;
				starAreas[index].add(b);
				centPoints[index].setCoordinations(centPoints[index].getX() + b.getX(), centPoints[index].getY() + b.getY());
			}
		}

		// Finilizing centeral points
		for (int i = 0; i < centPoints.length; i++) {
			centPoints[i].setCoordinations(centPoints[i].getX() / starAreas[i].size(), centPoints[i].getY() / starAreas[i].size());
		}
		// setting centeral gather point of map
		getGatheringArea(starAreas, cx, cy);

		//creating subZones
		for (int i = 0; i < starAreas.length; i++) {
			getStarZones()[i] = new StarZone(STAR_SUB_ZONES, centPoints[i].getX(), centPoints[i].getY(), i);
			getStarZones()[i].createSubZones(starAreas[i], centPoints[i].getX(), centPoints[i].getY());
		}
		setGatheringAreaOfStarZones();

	}

	private void setGatheringAreaOfStarZones() {
		double[] maxScores = new double[getStarZones().length];
		Road[] gatheringAreas = new Road[getStarZones().length];
		for (Road r : me.model().roads()) {
			for (int j = 0; j < getStarZones().length; j++) {
				double distance = Math.max(Utils.distance(r.getX(), r.getY(), getStarZones()[j].getCx(), getStarZones()[j].getCy()), 1);
				double nCount = me.model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				double height = r.getGeomArea().getBounds2D().getHeight();
				double score = (height * nCount) / (distance * distance);
				if (score > maxScores[j]) {
					maxScores[j] = score;
					gatheringAreas[j] = r;
				}
			}
		}
		for (int i = 0; i < gatheringAreas.length; i++) {
			getStarZones()[i].setGatheringRoad(gatheringAreas[i]);
		}
	}

	//*******************************************************************************************
	//*******************************************************************************************
	//************************************* Zone status checks **********************************
	//*******************************************************************************************
	//*******************************************************************************************

	public boolean isTotalSearched(int zone, int subZone) {
		double df = 0;
		double dc = 0;
		for (Building building : getStarZones()[zone].getSubZones()[subZone]) {
			if (searchWorld.getSearchBuilding(building).isSearchedFrom(startTimeOnCurrentCluster))
				df++;
			if (building.isSearchedForCivilian())
				dc++;
		}
		log("IsSearchedForCivlian [ done_fire:" + df + " done_civ:" + dc + " zone:" + zone + " subZone:" + subZone + " size:" + getStarZones()[zone].getSubZones()[subZone].size());
		return df / getStarZones()[zone].getSubZones()[subZone].size() > CLUSTER_SEARCH_THRESHOLD && dc / getStarZones()[zone].getSubZones()[subZone].size() > CLUSTER_SEARCH_THRESHOLD;

	}

	public boolean isSearched(int zone, int subZone) {
		log("Checking isSearched for zone:" + zone + " subZone:" + subZone);
		if (type.equals(StarSearchType.SEARCH_FOR_CIVILIAN)) {
			log("searchType:" + type.name());
			if (isSearchedForCivilian(zone, subZone))
				return true;
		} else if (type.equals(StarSearchType.SEARCH_FOR_FIRE)) {
			log("searchType:" + type.name());
			if (isSearchedForFire(zone, subZone))
				return true;
		} else if (type.equals(StarSearchType.RJS_THRESHOLD)) {
			log("searchType:" + type.name());
			if (isSearchedByRJS(zone, subZone))
				return true;
		} else {
			log("searchType:" + type.name());
			if (isTotalSearched(zone, subZone))
				return true;
		}
		return false;

	}

	private boolean isSearchedByRJS(int zone, int subZone) {
		double rjs = RemainingJobScorer.computeRJ(me, me.newSearch, getStarZones()[zone].getSubZones()[subZone].iterator());
		double irjs = initialRJS[zone][subZone];
		boolean isSearched=rjs < RJS_SEARCHED_PERCENT * irjs;
		log("-------IsSearchedByRJS [ RJS:" + rjs + " IRJS:" + irjs + " zone: "+zone+" subZone:" + subZone + " size:" + getStarZones()[zone].getSubZones()[subZone].size()+" isSearched: "+isSearched+" ]");
		return isSearched;
	}

	public boolean isSearchedForCivilian(int zone, int subZone) {
		double done = 0;
		for (Building building : getStarZones()[zone].getSubZones()[subZone]) {
			if (building.isSearchedForCivilian())
				done++;
		}
		boolean searched = done / getStarZones()[zone].getSubZones()[subZone].size() > CLUSTER_SEARCH_THRESHOLD;
		log("IsSearchedForCivlian [ done:" + done + " zone:" + zone + " subZone:" + subZone + " size:" + getStarZones()[zone].getSubZones()[subZone].size() + " searched: " + searched + "]");
		return searched;
	}

	public boolean isSearchedForFire(int zone, int subZone) {
		double done = 0;
		for (Building building : getStarZones()[zone].getSubZones()[subZone]) {
			if (searchWorld.getSearchBuilding(building).isSearchedForFire())
				done++;
		}
		log("IsSearchedForFire [ done:" + done + " zone:" + zone + " subZone:" + subZone + " size:" + getStarZones()[zone].getSubZones()[subZone].size());
		return done / getStarZones()[zone].getSubZones()[subZone].size() > CLUSTER_SEARCH_THRESHOLD;
	}

	//	private double getAngle(Area area, int cx, int cy) {
	//		Vector2D scale = new Vector2D(0, 1);
	//		Vector2D v = new Vector2D(area.getX() - cx, area.getY() - cy);
	//		double a = GeometryTools2D.getAngleBetweenVectors(scale, v);
	//		if ((v.getX() < 0 && v.getY() < 0) || (v.getX() < 0 && v.getY() >= 0))
	//			a = 360 - a;
	//		return a;
	//	}

	public Shape getPrecomputeAssigedZoneArea() {
		return getStarZones()[getPreComputeAssignedZone()].getBounds();
	}

	public Shape getCurrentAssigenedZoneArea() {
		return getStarZones()[getCurrentZone()].getBounds();
	}

	public Shape getNextAssigenedZoneArea() {
		return getStarZones()[nextZone(currentZone)].getBounds();
	}

	public Shape getPrecomputeAssigenedSubZoneArea() {
		java.awt.geom.Area a = new java.awt.geom.Area();
		for (Building b : getStarZones()[getPreComputeAssignedZone()].getSubZones()[getPreComputeAssignedSubZone()]) {
			a.add(new java.awt.geom.Area(b.getShape()));
		}
		return a;
	}

	public ArrayList<Building> getCurrentAssignedSubZone() {
		return getStarZones()[getCurrentZone()].getSubZones()[getCurrentSubZone()];
	}

	public ArrayList<Building> getNextAssigenedSubZoneBuildings() {
		Pair<Integer, Integer> nextSubZone = nextSubZone();
		return getStarZones()[nextSubZone.first()].getSubZones()[nextSubZone.second()];
	}

	public boolean isReallyReachable(Building b) {
		if (!me.move.isReallyUnreachable(b)) //False or true
			return true;
		for (ShapeInArea a : b.getSearchAreas()) {
			if (a.getArea(me.model()) instanceof Road && !me.move.isReallyUnreachable(a.getArea(me.model()))) {//False or true
				return true;
			}
		}
		return false;
	}

	public StarZone[] getStarZones() {
		return starZones;
	}

	public void setStarZones(StarZone[] starZones) {
		this.starZones = starZones;
	}

	public int getPreComputeAssignedZone() {
		return preComputeAssignedZone;
	}

	public void setPreComputeAssignedZone(int preComputeAssignedZone) {
		this.preComputeAssignedZone = preComputeAssignedZone;
	}

	public int getPreComputeAssignedSubZone() {
		return preComputeAssignedSubZone;
	}

	public void setPreComputeAssignedSubZone(int preComputeAssignedSuZone) {
		this.preComputeAssignedSubZone = preComputeAssignedSuZone;
	}

	@Override
	public SearchType getType() {
		return SearchType.StarSearch;
	}

	public int getCurrentZone() {
		return currentZone;
	}

	public void setCurrentZone(int currentZone) {
		this.currentZone = currentZone;
	}

	public int getCurrentSubZone() {
		return currentSubZone;
	}

	public void setCurrenSubtZone(int zone) {
		this.currentSubZone = zone;
	}

	public int MAX_GHATHERING_TIME() {
		if (me.getMapInfo().isBigMap()) {
			return 34;
		} else if (me.getMapInfo().isMediumMap()) {
			return 25;
		} else
			return 13;
	}

	public int GATHERING_TIME_PERIOD_MAP() {
		if (me.getMapInfo().isBigMap()) {
			return 110;
		} else if (me.getMapInfo().isMediumMap()) {
			return 70;
		} else
			return 80;
	}

	public int STAYING_TIME() {
		if (me.getMapInfo().isBigMap()) {
			return 6;
		} else if (me.getMapInfo().isMediumMap()) {
			return 4;
		} else
			return 3;
	}

	public int GATHERING_TIME_PERIOD_ZONE() {
		if (me.getMapInfo().isBigMap()) {
			return 70;
		} else if (me.getMapInfo().isMediumMap()) {
			return 50;
		} else
			return 30;
	}
}

class HumanComparator implements Comparator<Human> {
	private final double cx;
	private final double cy;

	public HumanComparator(double cx, double cy) {
		this.cx = cx;
		this.cy = cy;
	}

	@Override
	public int compare(Human o1, Human o2) {
		double d1 = Utils.distance(cx, cy, o1.getX(), o1.getY());
		double d2 = Utils.distance(cx, cy, o2.getX(), o2.getY());
		if (d1 < d2)
			return -1;
		if (d1 > d2)
			return 1;
		return 0;
	}

}
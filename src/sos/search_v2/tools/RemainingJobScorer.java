package sos.search_v2.tools;

import java.util.Iterator;
import java.util.List;

import sos.base.SOSAgent;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Road;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.util.SOSGeometryTools;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.worldModel.SearchBuilding;
import sos.tools.Utils;

/**
 * @author Salim
 */
public class RemainingJobScorer {
	private double[] clusterDistancesToMyCluster;
	private final SOSAgent<?> me;
	private double minClusterDistance = -1;
	public static int MAX_CLUSTER_SCORE = 20;
	public static double MIN_RJS = 0.01;
	public static final float RJS_WIGHTED_MEAN_ALPHA = (float) 1.5;//Salim
	private double initialRJ;
	public static double REMAINING_JOB_THRESHOLD = 0.7;

	public RemainingJobScorer(SOSAgent<?> me) {
		this.me = me;
		computeDistances();
	}

	private void computeDistances() {
		int clusters = me.model().searchWorldModel.getAllClusters().size();
		clusterDistancesToMyCluster = new double[clusters];
		ClusterData myCluster = me.model().searchWorldModel.getClusterData();

		minClusterDistance = Integer.MAX_VALUE;
		for (ClusterData cd : me.model().searchWorldModel.getAllClusters()) {
			double d = Utils.distance(cd.getX(), cd.getY(), myCluster.getX(), myCluster.getY());
			clusterDistancesToMyCluster[cd.getIndex()] = Math.max(d, 1);
			if (d < minClusterDistance)
				minClusterDistance = Math.max(d, 1);
		}
	}

	public void applyClusterJobDone(SearchBuilding b) {
		for (ClusterData cd : me.model().searchWorldModel.getAllClusters()) {
			if (cd.getBuildings().contains(b.getRealBuilding())) {
				double score = b.getScore();
				double rjs = remainingJobScore(cd);
				double newScore = 0;
				if (score < 0) {
					newScore = score / Math.max(rjs, 0.01);
				} else {
					newScore = score * Math.max(rjs, 0.01);
				}
				b.setScore(newScore, "RemainingJobScore applied : jobScore:" + rjs + " preScore:" + score);
			}
		}

	}

	public double getClusterRJS(Area area) {
		if (area instanceof Building)
			return getClusterRJS((Building) area);
		if (area instanceof Road)
			return getClusterRJS((Road) area);
		throw new Error("no such Entity");
	}

	public double getClusterRJS(Building b) {
		for (ClusterData cd : me.model().searchWorldModel.getAllClusters()) {
			if (cd.getBuildings().contains(b)) {
				return remainingJobScore(cd);
			}
		}
		throw new Error("no such cluster for " + b);
	}

	public double getClusterRJS(Road b) {
		double minDist = Double.MAX_VALUE;
		ClusterData c = null;
		for (ClusterData cd : me.model().searchWorldModel.getAllClusters()) {
			double dist = SOSGeometryTools.distance2(cd.getX(), cd.getY(), b.getX(), b.getY());
			if (dist < minDist) {
				minDist = dist;
				c = cd;
			}
		}

		if (c != null)
			return remainingJobScore(c);

		throw new Error("no such cluster for " + b);
	}

	public double remainingJobScore(ClusterData cd) {
		if (isNoComm()) {
			return MAX_CLUSTER_SCORE * 1 * getDistanceCoef(cd);
		}
		return MAX_CLUSTER_SCORE * cd.getRemainingJob(me.time()) * getDistanceCoef(cd);
	}

	public double getDistanceCoef(ClusterData cd) {
		//		System.out.println("min: " + minClusterDistance + " dist: " + clusterDistancesToMyCluster[cd.getIndex()] + " index: " + cd.getIndex() + " me:" + me.getID());

		double h = (Math.hypot(me.model().getBounds().getWidth(), me.model().getBounds().getHeight()));
		double d = ((h - clusterDistancesToMyCluster[cd.getIndex()]) / h);
		//		System.out.println("min: " + minClusterDistance + " dist: " + clusterDistancesToMyCluster[cd.getIndex()] + " index: " + cd.getIndex() + " me:" + me.getID() + " h:" + h + " res:" + d);
		return d;

	}

	public static double computeRJ(SOSAgent<?> me, AgentSearch<?> agentSearch, Iterator<Building> buildings) {
		double totalArea = 0;//SA(CB) = sum of area (Cluster Buildings)
		double remainingArea = 0;//SA(PNSB) = sum of area (proper notsearched buildings)
		int notReachableCivilians = 0;
		int totalCivilian = 0;
		int withCivilianProbabilityBuildings = 0;
		int properNotSearchedBuildings = 0;
		while (buildings.hasNext()) {
			Building b = buildings.next();
			totalArea += b.getGroundArea();
			if ((b.isFierynessDefined() && !(b.getFieryness() > 0) || !b.isFierynessDefined()) && ((agentSearch.getSearchWorld().getSearchBuilding(b).needsToBeSearchedForCivilian()) || !(agentSearch.getSearchWorld().getSearchBuilding(b).isSearchedForFire()))) {
				remainingArea += b.getGroundArea();
				properNotSearchedBuildings++;
			}
			for (Civilian c : b.getCivilians()) {
				if (SearchUtils.isValidCivilian(c, me)) {
					notReachableCivilians++;
				}
				totalCivilian++;
			}
			if (agentSearch.getSearchWorld().getSearchBuilding(b).getCivProbability() > 0)
				withCivilianProbabilityBuildings++;
		}
		// handling arithmatic bad situations
		//		if (totalCivilian == 0)
		notReachableCivilians = Math.max(1, notReachableCivilians);//TODO check all conditions for this one
		totalCivilian = Math.max(1, totalCivilian);

		totalArea = Math.max(1, totalArea);
		remainingArea = Math.max(0, remainingArea);

		withCivilianProbabilityBuildings = Math.max(0, withCivilianProbabilityBuildings);
		properNotSearchedBuildings = Math.max(1, properNotSearchedBuildings);

		float sum_coefs = RemainingJobScorer.RJS_WIGHTED_MEAN_ALPHA + 1;
		double sum = withCivilianProbabilityBuildings / properNotSearchedBuildings + RemainingJobScorer.RJS_WIGHTED_MEAN_ALPHA * remainingArea / totalArea;
		return Math.max(Math.sqrt(((double) notReachableCivilians) / totalCivilian) * (sum / sum_coefs), MIN_RJS);
	}

	public static double computeRJ(SOSAgent<?> me, AgentSearch<?> agentSearch, List<Building> buildings) {
		double totalArea = 0;//SA(CB) = sum of area (Cluster Buildings)
		double remainingArea = 0;//SA(PNSB) = sum of area (proper notsearched buildings)
		int notReachableCivilians = 0;
		int totalCivilian = 0;
		int withCivilianProbabilityBuildings = 0;
		int properNotSearchedBuildings = 0;
		for (Building b : buildings) {
			totalArea += b.getGroundArea();
			if ((b.isFierynessDefined() && !(b.getFieryness() > 0) || !b.isFierynessDefined()) && ((agentSearch.getSearchWorld().getSearchBuilding(b).needsToBeSearchedForCivilian()) || !(agentSearch.getSearchWorld().getSearchBuilding(b).isSearchedForFire()))) {
				remainingArea += b.getGroundArea();
				properNotSearchedBuildings++;
			}
			for (Civilian c : b.getCivilians()) {
				if (SearchUtils.isValidCivilian(c, me)) {
					notReachableCivilians++;
				}
				totalCivilian++;
			}
			if (agentSearch.getSearchWorld().getSearchBuilding(b).getCivProbability() > 0)
				withCivilianProbabilityBuildings++;
		}
		// handling arithmatic bad situations
		//		if (totalCivilian == 0)
		notReachableCivilians = Math.max(1, notReachableCivilians);//TODO check all conditions for this one
		totalCivilian = Math.max(1, totalCivilian);

		totalArea = Math.max(1, totalArea);
		remainingArea = Math.max(0, remainingArea);

		withCivilianProbabilityBuildings = Math.max(0, withCivilianProbabilityBuildings);
		properNotSearchedBuildings = Math.max(1, properNotSearchedBuildings);

		float sum_coefs = RemainingJobScorer.RJS_WIGHTED_MEAN_ALPHA + 1;
		double sum = withCivilianProbabilityBuildings / properNotSearchedBuildings + RemainingJobScorer.RJS_WIGHTED_MEAN_ALPHA * remainingArea / totalArea;
		return Math.max(Math.sqrt(((double) notReachableCivilians) / totalCivilian) * (sum / sum_coefs), MIN_RJS);
	}

	public double getInitialRJ() {
		return initialRJ;
	}

	public void setInitialRJ(double initialRJ) {
		this.initialRJ = initialRJ;
	}

	protected boolean isNoComm() {
		return me.messageSystem.type == Type.NoComunication;
	}
}

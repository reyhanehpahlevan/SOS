package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.Utils;

/**
 * @author Salim Malakouti
 * @param <E>
 */
public class CivilianSearch<E extends Human> extends SearchStrategy<E> {
	public List<ClusterData> otherClusters = null;

	public CivilianSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	private ClusterData currentCluster;
	private long lastWeight = 0;
	SearchBuilding lasttarget = null;

	private SearchBuilding switchCluster() {
		SearchBuilding task = null;
		log("Sorting other clsuters....");
		Collections.sort(otherClusters, new Comparator<ClusterData>() {

			@Override
			public int compare(ClusterData o1, ClusterData o2) {
				double ownD1 = Utils.distance(o1.getX(), o1.getY(), me.model().me().getPositionPoint().getX(), me.model().me().getPositionPoint().getY());
				double mcD1 = Utils.distance(o1.getX(), o1.getY(), me.getMyClusterData().getX(), me.getMyClusterData().getY());//Distance to my cluster (original my cluster)
				double mean1 = (mcD1 + ownD1) / 2;
				double ownD2 = Utils.distance(o2.getX(), o2.getY(), me.model().me().getPositionPoint().getX(), me.model().me().getPositionPoint().getY());
				double mcD2 = Utils.distance(o2.getX(), o2.getY(), me.getMyClusterData().getX(), me.getMyClusterData().getY());//Distance to my cluster (original my cluster)
				double mean2 = (mcD2 + ownD2) / 2;
				if (mean1 < mean2) {
					return -1;
				}
				else if (mean1 > mean2) {
					return 1;
				} else
					return 0;
			}
		});
		//		me.sosLogger.search.warn("                 " + otherClusters);
		for (ClusterData cd : otherClusters) {
			task = chooseTask(lasttarget, lastWeight, cd);
			if (task != null) {
				currentCluster = cd;
				break;
			}
		}
		log("current cluster is chosen to be: " + currentCluster);
		log("Task on cluster is: " + task);

		return task;
	}

	private boolean shouldChangeCluster(SearchBuilding task) {
		if (currentCluster == null)
			return true;
		log("current cluster was null => shouldChangeCluster");
		if (task != null)
			return false;
		log("task was null => shouldChangeCluster");
		if (!isNoComm())
			return false;
		log("it is noComm => shouldChangeCluster");
		if (!(me instanceof AmbulanceTeamAgent))
			return false;
		log("I'm AmbulanceTeamAgent => shouldChangeCluster");
		return true;
	}

	public SearchBuilding chooseTask(SearchBuilding lasttarget2, double lastWeight, ClusterData cluster) {
		if (cluster == null)
			return null;
		else
			return chooseTask(lasttarget2, lastWeight, cluster.getBuildings());
	}

	public SearchBuilding chooseTask(SearchBuilding lasttarget2, double lastWeight, Collection<Building> input) {
		if (input == null)
			return null;
		log("last target--->" + lasttarget2);
		long t1 = System.currentTimeMillis();

		long weight = 0;
		if (lasttarget2 != null){
			lasttarget2.setScore(0);
			weight = getWeightTo(lasttarget2);
		}
		log("Current weight: " + weight + " last weight:" + lastWeight + " lastWeight*coef:" + (searchTools.getWeightIncreaseCoef(me) * lastWeight) + "    time: " + (System.currentTimeMillis() - t1));

		//		SearchBuilding toSearch = lasttarget;
		if (isTaskDone(lasttarget2, lastWeight, weight))
			lasttarget2 = searchTools.getBestCivilianSearchArea(input,myClusterData.isCoverer());

		log("Search going for act time: " + (System.currentTimeMillis() - t1) + "ms");
		log("building is checked: " + lasttarget2 + " for civilian search. ");
		this.lasttarget = lasttarget2;
		if (lasttarget2 != null) {
			//			lasttarget = toSearch;
			this.lastWeight = getWeightTo(lasttarget2);
			log("task choosed: " + lasttarget2 + " for civilian search. score="+lasttarget2.getScore()+" move weight="+lastWeight);
			log(lasttarget2+" scores reason:"+lasttarget2.reason);
			lasttarget2.setTarget(me.time());
			return lasttarget2;
		}
		log("civilian search had no task.  RETURNING!!!");
		return null;
	}

	private boolean isTaskDone(SearchBuilding lasttarget, double lastWeight, double weight) {

		return (lasttarget == null || !lasttarget.needsToBeSearchedForCivilian()
		|| weight > searchTools.getWeightIncreaseCoef(me) * lastWeight);
	}

	@Override
	public SearchTask searchTask() {
		if (otherClusters == null) {
			otherClusters = new ArrayList<ClusterData>(me.newSearch.getSearchWorld().getAllClusters());
			otherClusters.remove(me.getMyClusterData());
		}
		log("Civilian Search.......................................");

		SearchBuilding task = chooseTask(lasttarget, lastWeight, me.getMyClusterData());
		log("Task chosen from mycluster: " + task);
		if (isNoComm()) {
			if (task == null)
				task = chooseTask(lasttarget, lastWeight, currentCluster);
			log("Current cluster was: " + currentCluster);
			log("Task chosen from currrent cluster: " + task);
			if (shouldChangeCluster(task)){
				task = switchCluster();
				log("Task chosen from switch cluster: " + task);
			}
			
		}
		if (task == null)
			return null;
		return new SearchTask(task.getRealBuilding().getSearchAreas());

	}

	@Override
	public SearchType getType() {
		return SearchType.CivilianSearch;
	}
}

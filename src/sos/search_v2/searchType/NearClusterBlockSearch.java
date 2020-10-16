package sos.search_v2.searchType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import sos.base.SOSAgent;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.util.geom.ShapeInArea;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;
import sos.tools.Utils;

/**
 * @author Salim Malakouti
 * @param <E>
 */
public class NearClusterBlockSearch<E extends Human> extends SearchStrategy<E> {
	private ArrayList<Area> assignedRegion = null;
	private ArrayList<Area> checkedRegion = null;
	private int BLOCK_SEARCH_TIME_TRESH = 3;
	private int lastTimeOnBlockSearch = -1;

	public NearClusterBlockSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
		assignedRegion = new ArrayList<Area>();
		int cx = 0;
		int cy = 0;
		for (Building b : searchWorld.getClusterData().getBuildings()) {
			assignedRegion.add(b);
			cx += b.getX();
			cy += b.getY();
		}
		cx /= assignedRegion.size();
		cy /= assignedRegion.size();

		if ((me instanceof PoliceForceAgent)) {
			Collection<Road> roadsInRange = searchWorld.model().getObjectsInRange(cx, cy, 200000, Road.class);
			Road best = null;
			double maxScore = Integer.MIN_VALUE;
			for (Road r : roadsInRange) {
				double d = Utils.distance(cx, cy, r.getX(), r.getY());
				double score = r.getShape().getBounds().getHeight() / (d * d);
				if (score > maxScore) {
					maxScore = score;
					best = r;
				}

			}
			assignedRegion.add(best);
		}

		checkedRegion = new ArrayList<Area>();//Added By Salim
	}

	@Override
	public SearchTask searchTask() {
		log("Starting Block Search...............");
		if(myClusterData.isCoverer()){
			log("I have coverer cluster--->reutrn null");
			return null;
		}
		resetAssignedBuildings();
		ArrayList<ShapeInArea> blockSearch = blockSearch();
		if (blockSearch == null||notReachableAndNotPolice(blockSearch))
			return null;
		return new SearchTask(blockSearch);
	}

	/**
	 * Visits Blocks (Small Clusters of buildings) at early cycles.
	 */
	private ArrayList<ShapeInArea> blockSearch() { //Salim
		try {
			if (hasBlock())
				return checkBlocks();
		} catch (Exception e) {
			System.err.println("[EXCEPTION] Block Search had exception ");
		}

		return null;
	}

	public void resetAssignedBuildings() {

		if (assignedRegion.size() != 0)
			return;
		log("--------- assignedRegion.size() ==" + assignedRegion.size());
		if (me.time() < BLOCK_SEARCH_TIME_TRESH)
			return;
		log("assignedRegion.size() == 0");

		if (!(me.time() - lastTimeOnBlockSearch > TASK_RESET_TIME()))
			return;
		log("--------- me.time():" + me.time() + " BLOCK_SEARCH_TIME_TRESH:" + BLOCK_SEARCH_TIME_TRESH + " (me.time() - lastTimeOnBlockSearch > TASK_RESET_TIME())");
		for (int i = checkedRegion.size() - 1; i > -1; i--) {
			if (me.time() - checkedRegion.get(i).updatedtime() > INFORMATION_EXPIRE_TIME()) {
				if (getCheckedRegion().get(i) instanceof Building) {
					Building b = (Building) getCheckedRegion().get(i);
					if (b.getFieryness() != 0)
						continue;

					log("------------------ b: " + b);
					if (b.isEitherFieryOrBurnt()) {
						log("Skipped building because: temperature=" + b.getTemperature());
						continue;
					}
				}
				log("me.time:" + me.time() + " checkedRegion.get(i).updatedtime():" + checkedRegion.get(i).updatedtime());
				assignedRegion.add(checkedRegion.get(i));
				checkedRegion.remove(i);
			}
		}
		log("AssignedSize after reset:" + assignedRegion.size());
		log("Increasing BLOCK_SEARCH_TIME_TRESH from: " + BLOCK_SEARCH_TIME_TRESH + " to:" + me.time());
		BLOCK_SEARCH_TIME_TRESH = me.time();//FIXME!!! salim ghablesh bood BLOCK_SEARCH_TIME_TRESH += me.time(); dalili dasht?
	}

	/**
	 * Checks if there is any block to be searched
	 * 
	 * @return
	 */
	public boolean hasBlock() { //Salim
		log("Checking if there is any block left...");
		if (getAssignedRegion() == null) {

			log("[ERROR] There most be some error because assignedRegion is null");
			return false;
		} else {
			HashSet<Building> mayNotFieryBuildings = getNeighborOfNotFieryVisibleBuildings();
			for (int i = getAssignedRegion().size() - 1; i > -1; i--) {
				Area a = getAssignedRegion().get(i);
				log("--------- checking: " + a);
				if (a instanceof Building) {
					Building b = (Building) a;
					log("checking building: " + a + " b.updatedtime(): " + b.updatedtime() + " BLOCK_SEARCH_TIME_TRESH:" + BLOCK_SEARCH_TIME_TRESH + " (b.isSearchedForCivilian():" + (b.isSearchedForCivilian()));
					if (mayNotFieryBuildings.contains(b) || (b.updatedtime() > BLOCK_SEARCH_TIME_TRESH) || (b.isSearchedForCivilian()) || b.getFieryness() != 0) {
						log("added to CheckedRegion");
						getCheckedRegion().add(a);
						getAssignedRegion().remove(i);
					}
				} else if (a instanceof Road) {
					Road r = (Road) a;
					boolean isReachable = isReachable(r) || me.location().getID().equals(r.getID());
					log("checking road: " + a + " r.updatedtime():" + r.updatedtime() + " BLOCK_SEARCH_TIME_TRESH: " + BLOCK_SEARCH_TIME_TRESH + " isReachable:" + isReachable);
					if (r.updatedtime() > BLOCK_SEARCH_TIME_TRESH && isReachable) {
						log("is Reachable, removed from tasks for ever....");
						//						getCheckedRegion().add(r);
						getAssignedRegion().remove(i);
					}
				}
			}

			log("Number of remaining blocks: " + getAssignedRegion().size());
			return getAssignedRegion().size() != 0;
		}
	}

	@Override
	public void preSearch() {
		super.preSearch();
//		hasBlock();
	}

	public HashSet<Building> getNeighborOfNotFieryVisibleBuildings() {
		//		ArrayList<Building> visibleBuildings = SOSAgent.currentAgent().getVisibleEntities(Building.class);
		HashSet<Building> checked = new HashSet<Building>();
		for (Building building : me.model().buildings()) {
			if (building.updatedtime() > me.model().time() - 3 && building.isTemperatureDefined() && building.getTemperature() == 0) {
				for (Building neighbor : building.realNeighbors_Building()) {
					checked.add(neighbor);
				}
			}
		}
		return checked;
	}

	public ArrayList<Area> getAssignedRegion() {
		return assignedRegion;
	}

	public ArrayList<Area> getCheckedRegion() {
		return checkedRegion;
	}

	public void setCheckedRegion(ArrayList<Area> checkedRegion) {
		this.checkedRegion = checkedRegion;
	}

	/**
	 * Creates Search task in order to check blocks
	 */
	private ArrayList<ShapeInArea> checkBlocks() { //Salim
		lastTimeOnBlockSearch = me.time();
		List<Area> buildings = assignedRegion;
		log(" returning " + assignedRegion.size() + " as tasks to agents");
		ArrayList<ShapeInArea> shapes = new ArrayList<ShapeInArea>();
		for (Area a : buildings) {
			log("     added " + a);
			if (a instanceof Building) {
				shapes.addAll(((Building) a).getSearchAreas());
			} else if (a instanceof Road) {
				shapes.add(new ShapeInArea(a.getApexList(), a));
			}
		}
		return shapes;

	}

	@Override
	public SearchType getType() {
		return SearchType.BlockSearch;
	}

	public int INFORMATION_EXPIRE_TIME() {
		if (me.getMapInfo().isBigMap()) {
			return 60;
		} else if (me.getMapInfo().isMediumMap()) {
			return 40;
		} else
			return 30;
	}

	public int TASK_RESET_TIME() {
		if (me.getMapInfo().isBigMap()) {
			return 80;
		} else if (me.getMapInfo().isMediumMap()) {
			return 55;
		} else
			return 40;
	}
}

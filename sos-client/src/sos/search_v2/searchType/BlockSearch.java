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
import sos.fire_v2.FireBrigadeAgent;
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
public class BlockSearch<E extends Human> extends SearchStrategy<E> {
	private ArrayList<Area> assignedRegion = null;
	private ArrayList<Area> checkedRegion = null;
	private int BLOCK_SEARCH_TIME_TRESH = 3;
	private int lastTimeOnBlockSearch = -1;
	private HashSet<Building> checkedOtherRegion=new HashSet<Building>();

	public BlockSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
		setRegion(searchWorld.getClusterData().getBuildings());
		checkedRegion = new ArrayList<Area>();//Added By Salim
	}

	public void setRegion(Collection<Building> buildings){
		assignedRegion = new ArrayList<Area>();
		int cx = 0;
		int cy = 0;
		for (Building b : buildings) {
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

	}


	@Override
	public SearchTask searchTask() {
		if(me.time()<3)
			return null;
		if(me.time()>75 && me instanceof PoliceForceAgent)
			return null;
			
		log("Starting Block Search...............");
		if ((!isNoComm()) && myClusterData.isCoverer()) {
			log("I have coverer cluster--->reutrn null");
			return null;
		}
		
		resetAssignedBuildings();
		ArrayList<ShapeInArea> blockSearch = blockSearch();
		if (blockSearch == null || notReachableAndNotPolice(blockSearch)) {
			if (isNoComm() && me instanceof FireBrigadeAgent) {
				resetBlocks();
				blockSearch = blockSearch();
			}
			if (blockSearch == null || notReachableAndNotPolice(blockSearch)) {
				return null;
			}
		}
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
			me.sosLogger.search.error("[EXCEPTION] Block Search had exception ",e);
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
		
		if(me instanceof PoliceForceAgent)
			return;
		
		resetBlocks();
	}

	public void resetBlocks() {
		checkedOtherRegion.clear();
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
			getCheckedOtherRegion().addAll(mayNotFieryBuildings);
			for (int i = getAssignedRegion().size() - 1; i > -1; i--) {
				Area a = getAssignedRegion().get(i);
				log("--------- checking: " + a);
				if (a instanceof Building) {
					Building b = (Building) a;

					if (isFinishedBuilding(b, getCheckedOtherRegion())) {
						log("added to CheckedRegion");
						getCheckedRegion().add(a);
						getAssignedRegion().remove(i);
					}
				} else if (a instanceof Road) {
					Road r = (Road) a;
					boolean isReachable = isReachable(r);
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

	public boolean isFinishedBuilding(Building b, HashSet<Building> mayNotFieryBuildings) {
		log("checking " + b + " b.updatedtime(): " + b.updatedtime() + " BLOCK_SEARCH_TIME_TRESH:" + BLOCK_SEARCH_TIME_TRESH + " (b.isSearchedForCivilian():" + (b.isSearchedForCivilian()));
		if (mayNotFieryBuildings.contains(b)) {
			log("Finished  " + b + "==> may not fiery building...");
			return true;
		}
		if (b.updatedtime() > BLOCK_SEARCH_TIME_TRESH) {
			log("Finished " + b + "==> updatedtime(" + b.updatedtime() + ") > " + "BLOCK_SEARCH_TIME_TRESH(" + BLOCK_SEARCH_TIME_TRESH + ")");
			return true;
		}
		//		if (b.isSearchedForCivilian())
		//			return true;
		if (b.getFieryness() != 0 && b.getFieryness() != 4) {
			log("Finished  " + b + "==> is fiery building, fireness=" + b.getFieryness());
			return true;
		}
		return false;
	}

	@Override
	public void preSearch() {
		super.preSearch();
		//		hasBlock();
	}

	public HashSet<Building> getNeighborOfNotFieryVisibleBuildings() {
		ArrayList<Building> visibleBuildings = SOSAgent.currentAgent().getVisibleEntities(Building.class);

		HashSet<Building> checked = new HashSet<Building>();
		HashSet<Building> shouldNotRemove = new HashSet<Building>();
		for (Building building : visibleBuildings) {
			if (building.isTemperatureDefined() && building.getTemperature() == 0) {
				for (Building neighbor : building.realNeighbors_Building()) {
					if (building.distance(neighbor) < 50000)
						checked.add(neighbor);
				}
			}
			if (building.isTemperatureDefined() && building.getTemperature() != 0) {
				for (Building neighbor : building.realNeighbors_Building()) {
					shouldNotRemove.add(neighbor);
				}
			}
		}
		checked.removeAll(shouldNotRemove);
		return checked;
	}

	public ArrayList<Area> getAssignedRegion() {
		return assignedRegion;
	}

	public ArrayList<Area> getCheckedRegion() {
		return checkedRegion;
	}
	public HashSet<Building> getCheckedOtherRegion() {
		return checkedOtherRegion;
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

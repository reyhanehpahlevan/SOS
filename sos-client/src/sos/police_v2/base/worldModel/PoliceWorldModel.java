package sos.police_v2.base.worldModel;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.PriorityQueue;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.SOSWorldModel;
import sos.base.entities.Area;
import sos.base.entities.Blockade;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.PoliceForce;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.mapRecognition.MapRecognition.MapName;
import sos.fire_v2.base.tools.BuildingBlock;
import sos.fire_v2.base.worldmodel.FirePrecompute.Mode;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.search_v2.searchType.CommunicationlessSearch;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;

public class PoliceWorldModel extends SOSWorldModel {
	private ArrayList<PoliceForceTask> policeTasks = null;
	private PoliceForceTask meTask;
	private ArrayList<BuildingBlock> buildingBlocks = new ArrayList<BuildingBlock>();
	private ArrayList<BuildingBlock> island = new ArrayList<BuildingBlock>();

	//	private ArrayList<PoliceBuilding> policeBuildings;
	private HashSet<Area> highways = new HashSet<Area>();
	private HashSet<Area> junctionPoints;
	private ArrayList<PoliceForceTask> policeForSpecialTask = new ArrayList<PoliceForceTask>();

	//	private ConvexHull convexHull = new ConvexHull();
	//	private ArrayList<Point> convex = new ArrayList<Point>();

	public PoliceWorldModel(SOSAgent<? extends StandardEntity> sosAgent) {
		super(sosAgent);
	}

	@Override
	public void precompute() {
		super.precompute();
		setDefaultValueByScenarioCondition();
		makePoliceTasks();
		setSomePoliceForSpecialTasks();
	}

	private void setDefaultValueByScenarioCondition() {
		if (PoliceConstants.STANDARD_OF_MAP < 3) {
			if (fireBrigades().size() < 11)
				PoliceConstants.Value.FireBrigade.setValue(8);
			else
				PoliceConstants.Value.FireBrigade.setValue(6);

			if (ambulanceTeams().size() < 11)
				PoliceConstants.Value.AmbulanceTeam.setValue(7);
			else
				PoliceConstants.Value.AmbulanceTeam.setValue(5);

			if (refuges().size() < 2)
				PoliceConstants.Value.Refuge.setValue(10);
			else
				PoliceConstants.Value.Refuge.setValue(5);

		}
		if (PoliceConstants.STANDARD_OF_MAP >= 3) {
			if (fireBrigades().size() < 15)
				PoliceConstants.Value.FireBrigade.setValue(8);
			else
				PoliceConstants.Value.FireBrigade.setValue(6);

			if (ambulanceTeams().size() < 15)
				PoliceConstants.Value.AmbulanceTeam.setValue(7);
			else
				PoliceConstants.Value.AmbulanceTeam.setValue(5);

			if (refuges().size() < 3)
				PoliceConstants.Value.Refuge.setValue(7);
			else
				PoliceConstants.Value.Refuge.setValue(4);
		}
		PoliceConstants.Value.PoliceForce.setValue(0);
		if (policeForces().size() < 9)
			PoliceConstants.Value.PoliceForceInBuilding.setValue(10);
		else
			PoliceConstants.Value.PoliceForceInBuilding.setValue(8);

	}

	public void makeHighways() {
		if (!(sosAgent().getMapInfo().getRealMapName() == MapName.Kobe || sosAgent().getMapInfo().getRealMapName() == MapName.VC))
			return;
		ArrayList<ConvexHull_arr_New> convexes = new ArrayList<ConvexHull_arr_New>();
		long t = System.currentTimeMillis();
		for (BuildingBlock bblock : islands()) {
			convexes.add(new ConvexHull_arr_New(bblock.buildings()));
		}
		HashSet<Area> areahash = new HashSet<Area>();
		ArrayList<Area> roadsInIlandsList = new ArrayList<Area>();
		FOR: for (Road road : roads()) {
			for (ConvexHull_arr_New convexHullNew : convexes) {
				//				if(convexHullNew.isInConvex(road.getApexList()))
				if ((road.isNeighbourWithBuilding() && road.getSOSGroundArea() < PoliceConstants.VERY_SMALL_ROAD_GROUND_IN_MM))
					continue FOR;
				if (convexHullNew.contains(road.getX(), road.getY())) {
					roadsInIlandsList.add(road);
					continue FOR;
				}
			}
			areahash.add(road);
		}
		for (Area road : roadsInIlandsList) {
			int i = 0;
			for (Area neigh : road.getNeighbours()) {
				if (areahash.contains(neigh))
					i++;
			}
			if (i >= 2)
				areahash.add(road);
		}
		ArrayList<Area> removeList = new ArrayList<Area>();
		for (Area area : areahash) {
			int i = 0;
			for (Area neigh : area.getNeighbours()) {
				if (areahash.contains(neigh))
					i++;
			}
			if (i < 2)
				removeList.add(area);

		}

		areahash.removeAll(removeList);

		highways = areahash;
		sosAgent().log.consoleInfo("highways got:" + (System.currentTimeMillis() - t) + "ms");
		makeJunction();
	}

	private void makeJunction() {
		long t = System.currentTimeMillis();
		int LENGHT = 3;
		int VALIDCOUNT = 2;
		junctionPoints = new HashSet<Area>();
		FOR: for (Area road : highways) {
			if (road.getNeighbours().size() >= 2) {
				int validCount = 0;
				ArrayList<Area> tmp = new ArrayList<Area>();
				tmp.add(road);
				tmp.addAll(road.getNeighbours());
				for (Area area : road.getNeighbours()) {
					if (!highways.contains(area))
						continue FOR;
					if (getLenghtOfRoad(highways, tmp, area, LENGHT) >= LENGHT)
						validCount++;
				}
				if (validCount > VALIDCOUNT) {
					for (Area area : road.getNeighbours()) {
						if (junctionPoints.contains(area))
							continue FOR;
					}
					junctionPoints.add(road);
				}
			}
		}
		/**
		 * Filtering some junction becuase some of them is not correct and some near each other
		 */
		long avg = 0;
		short index = 0;
		Pair<Area, Long> select2 = null;
		for (Area select1 : junctionPoints) {
			select2 = getNearestJunction(select1);
			avg += select2.second();
			index++;
		}
		if (index != 0)
			avg = avg / index;
		long limit = (long) (avg * (0.7f));
//		System.err.println("avg=" + avg + "   limit=" + limit);
		ArrayList<Area> removelist = new ArrayList<Area>();
		for (Area select1 : junctionPoints) {
			select2 = getNearestJunction(select1);
			if (select2.second() < limit) {
				if (!removelist.contains(select1))
					removelist.add(select1);
				if (!removelist.contains(select2.first()))
					removelist.add(select2.first());
			}
		}

		junctionPoints.removeAll(removelist);

		/**
		 * back some removed junctionpoint that remove wrong
		 */
		if (removelist.size() > 0) {

			ArrayList<Area> addlist = new ArrayList<Area>();
			HashSet<Area> invalid = new HashSet<Area>(junctionPoints);
			for (Area n : junctionPoints) {
				Area neighbor = getHighwaysNeighbour(highways, removelist, invalid, n, n);
				if (neighbor != null) {
					//					temp.add(neighbor);
					if (!addlist.contains(neighbor))
						addlist.add(neighbor);
				}
			}
			junctionPoints.addAll(addlist);
		}

		sosAgent().log.consoleInfo("Junctions got:" + (System.currentTimeMillis() - t) + "ms");
	}

	private Pair<Area, Long> getNearestJunction(Area start) {
		Area result = null;
		long minDis = Long.MAX_VALUE;
		for (Area select : junctionPoints) {
			if (result == null) {
				result = select;
				continue;
			}
			long temp = (long) Point.distance(start.getX(), start.getY(), select.getX(), select.getY());
			if (temp == 0)
				continue;
			if (minDis > temp) {
				result = select;
				minDis = temp;

			}

		}
		return new Pair<Area, Long>(result, minDis);

	}

	private Area getHighwaysNeighbour(HashSet<Area> areahash, Collection<Area> junctionPoints2, HashSet<Area> OldPath, Area area, Area startTarget) {
		//		System.out.println(OldPath);
		if (junctionPoints2.contains(area)) {
			return area;
		}
		int mindist = Integer.MAX_VALUE;
		Area result = null;

		for (Area area2 : area.getNeighbours()) {
			if (OldPath.contains(area2))
				continue;
			if (area2.equals(startTarget))
				continue;
			if (!areahash.contains(area2))
				continue;
			HashSet<Area> newPath = OldPath;
			newPath.add(area2);
			Area a = getHighwaysNeighbour(areahash, junctionPoints2, newPath, area2, startTarget);
			if (a != null) {
				int min = PoliceUtils.getDistance(area, a);
				if (mindist > min) {
					result = a;
					mindist = min;
				}
			}
		}

		return result;
	}

	int getLenghtOfRoad(HashSet<Area> areahash, ArrayList<Area> OldPath, Area area, int max) {
		if (max == 0)
			return 0;
		int tmpmax = 0;

		for (Area area2 : area.getNeighbours()) {
			if (area2 instanceof Building)
				continue;
			if (!areahash.contains(area2))
				continue;
			if (OldPath.contains(area2))
				continue;
			//			ArrayList<Area> newPath = new ArrayList<Area>(OldPath);
			ArrayList<Area> newPath = OldPath;
			newPath.add(area2);
			tmpmax = Math.max(tmpmax, getLenghtOfRoad(areahash, newPath, area2, max - 1));
		}
		return tmpmax + 1;

	}

	public HashSet<Area> getJunctionPoints() {
		return junctionPoints;
	}

	public HashSet<Area> getHighways() {
		return highways;
	}

	private void makePoliceTasks() {
		policeTasks = new ArrayList<PoliceForceTask>();
		short tmpindex = 0;
		for (PoliceForce policeForce : policeForces()) {

			PoliceForceTask task = new PoliceForceTask(policeForce, tmpindex++);
			task.setDefaultValue();
			policeTasks.add(task);
			//			policeForce.selfToTask = task;
			if (me().equals(policeForce))
				meTask = task;
		}
	}

	private void setSomePoliceForSpecialTasks() {
		int num = 1;
		Human centeralMan = CommunicationlessSearch.selectCenteralMan(this);
		if (centeralMan instanceof PoliceForce && num > getPoliceForSpecialTask().size())
			getPoliceForSpecialTask().add(getPoliceTasks((PoliceForce) centeralMan));
		for (PoliceForceTask ptask : getPoliceTasks()) {
			if (getPoliceForSpecialTask().size() == num)
				return;
			if (ptask.getDefaultValue() == 0 && !getPoliceForSpecialTask().contains(ptask)) {
				getPoliceForSpecialTask().add(ptask);
				ptask.setDefaultValue(PoliceConstants.Value.PoliceForSpecialTasks.getValue());
				ptask.setJobDone(10000, false);
				log().debug("SpecialPolice===>" + ptask);
			}
		}
	}

	public PoliceForceTask meTask() {
		return meTask;
	}

	public void makeIslands() {
		if (!(sosAgent().getMapInfo().getRealMapName() == MapName.Kobe || sosAgent().getMapInfo().getRealMapName() == MapName.VC ))
			return;
		long start = System.currentTimeMillis();
		File islandFile = new File("SOSFiles/IslandsPolice/" + sosAgent().getMapInfo().getMapName() + ".txt");
		try {// if file exists
			FileInputStream fstream = new FileInputStream(islandFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String s = br.readLine();
			int numberOfIslands = Integer.parseInt(s);
			for (int i = 0; i < numberOfIslands; i++) {
				sos.fire_v2.base.tools.BuildingBlock bb = new BuildingBlock(sosAgent().model());
				s = br.readLine();
				int numberOfIslandBuildings = Integer.parseInt(s);
				ArrayList<Building> buildings = new ArrayList<Building>(numberOfIslandBuildings);
				for (int j = 0; j < numberOfIslandBuildings; j++) {
					s = br.readLine();
					int index = Integer.parseInt(s);
					buildings.add(sosAgent().model().buildings().get(index));
				}
				bb.makeByDistance(Mode.ReadFromFile, buildings);
				islands().add(bb);
			}
			in.close();
			for (BuildingBlock bb : islands()) {
				bb.setIslandNeighbors();
			}
		} catch (Exception e) {// if file dose not exist
			ArrayList<Building> allBuildings = new ArrayList<Building>(sosAgent().model().buildings());
			while (allBuildings.size() > 0) {
				BuildingBlock bb = new BuildingBlock(sosAgent().model());
				allBuildings = bb.makeByDistance(Mode.Compute, allBuildings);
				if (bb.buildings().size() > 0) {
					islands().add(bb);
				}
			}
			makeIslandsFile(islandFile);
		}
		sosAgent().log.consoleInfo("Police Islands got:" + (System.currentTimeMillis() - start) + "ms");
	}

	/** @author nima */
	private void makeIslandsFile(File file) {
		if (SOSConstant.IS_CHALLENGE_RUNNING)
			return;
		try {
			file.getParentFile().mkdirs();
			Formatter f = new Formatter(file);
			f.format("%d\n", islands().size());
			for (BuildingBlock bb : islands()) {
				f.format("%d\n", bb.buildings().size());
				for (Building b : bb.buildings())
					f.format("%d\n", b.getBuildingIndex());
			}
			f.flush();
			f.close();
		} catch (FileNotFoundException e) {
			log().error("Write Island File]" + e.getMessage());
			//			e.printStackTrace();
		}
	}

	public Point2D getCeterOfMap() {
		int x = getWorldBounds().first().first() + getWorldBounds().second().first();
		int y = getWorldBounds().first().second() + getWorldBounds().second().second();
		return new Point2D(x, y);
	}

	public PriorityQueue<Blockade> getBlockadesInRange(int x, int y, int range) {
		if (!indexed) {
			index();
		}
		final PriorityQueue<Blockade> result = new PriorityQueue<Blockade>(20, new DistanceComparator());
		Rectangle r = new Rectangle(x - range, y - range, x + range, y + range);
		index.intersects(r, new IntProcedure() {
			@Override
			public boolean execute(int id) {
				StandardEntity e = getEntity(new EntityID(id));
				if (e != null && e instanceof Road && ((Road) e).isBlockadesDefined()) {
					for (Blockade blockade : ((Road) e).getBlockades()) {
						if (PoliceUtils.isValid(blockade))
							result.add(blockade);
					}
				}
				return true;
			}
		});
		return result;
	}

	public ArrayList<BuildingBlock> buildingBlocks() {
		return buildingBlocks;
	}

	public void setBuildingBlocks(ArrayList<BuildingBlock> buildingBlocks) {
		this.buildingBlocks = buildingBlocks;
	}

	public ArrayList<BuildingBlock> islands() {
		return island;
	}

	public void setIsland(ArrayList<BuildingBlock> island) {
		this.island = island;
	}

	public ArrayList<PoliceForceTask> getPoliceTasks() {
		return policeTasks;
	}

	private class DistanceComparator implements java.util.Comparator<Blockade> {
		@Override
		public int compare(Blockade c1, Blockade c2) {
			return (int) (PoliceUtils.getBlockadeDistance(c1) - PoliceUtils.getBlockadeDistance(c2));
		}
	}

	public PoliceForceTask getPoliceTasks(PoliceForce o1) {//TODO behine tar beshe
		for (PoliceForceTask iterable_element : policeTasks) {
			if (iterable_element.getRealEntity().equals(o1))
				return iterable_element;
		}
		return null;
	}

	@Override
	public PoliceForceAgent sosAgent() {
		return (PoliceForceAgent) super.sosAgent();
	}

	public ArrayList<PoliceForceTask> getPoliceForSpecialTask() {
		return policeForSpecialTask;
	}
}

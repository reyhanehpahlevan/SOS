package sos.police_v2.state;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.StandardEntity;
import sos.base.move.types.PoliceMove;
import sos.base.move.types.SearchMove;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.HungarianAlgorithm;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.Task;
import sos.search_v2.tools.cluster.ClusterData;

public class OpenHighWaysState2 extends PoliceAbstractState {
	public HashMap<Area, ArrayList<Area>> yalBetweenJunctionPoints = new HashMap<Area, ArrayList<Area>>();
	public HashSet<Yal> yals = new HashSet<OpenHighWaysState2.Yal>();
	//	public HashMap<PoliceForce, ArrayList<Task>> tasks=new HashMap<PoliceForce, ArrayList<Task>>();
	//	ArrayList<Task<? extends StandardEntity>> alltasks = new ArrayList<Task<?>>();
	public HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>> agent_tasks = new HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>>();
	public HashMap<PoliceForceTask, ArrayList<Yal>> agent_yals = new HashMap<PoliceForceTask, ArrayList<Yal>>();
	public ArrayList<Yal> myYals = new ArrayList<Yal>();
	private ClusterData myClusterData;
	private ArrayList<Area> selectedYal = null;
	private ArrayList<Yal> notClearedYals;

	public OpenHighWaysState2(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}
	@Override
	public void precompute() {
		myClusterData = model().searchWorldModel.getClusterData();
		/*
		 * assigneHighways();
		 */
		
		/****************************
		 ** New assignTasks By Hesam**
		 ****************************/
		makeYals();
		assignTasks_new();
		notClearedYals = new ArrayList<OpenHighWaysState2.Yal>(myYals);
	}

	private void assignTasks_new() {
		ArrayList<ClusterData> clusterDatas = new ArrayList<ClusterData>(model().searchWorldModel.getAllClusters());
		ArrayList<Yal> remaindedYals = new ArrayList<Yal>(yals);
		while (remaindedYals.size() > 0) {
			HungarianAlgorithm hun = new HungarianAlgorithm(getCostList(remaindedYals, clusterDatas));
			int[] result = hun.execute();
			ArrayList<Yal> assignedYals = new ArrayList<OpenHighWaysState2.Yal>();
			for (int i = 0; i < result.length; i++) {
				if (result[i] >= 0) {
					assignedYals.add(remaindedYals.get(i));
					log.info("yale " + i + " om assign shoe be " + result[i]);
					if (clusterDatas.get(result[i]).equals(myClusterData))
						myYals.add(remaindedYals.get(i));
				}
			}
			remaindedYals.removeAll(assignedYals);
		}

	}

	private double[][] getCostList(ArrayList<Yal> yals, ArrayList<ClusterData> datas) {
		double[][] list = new double[yals.size()][datas.size()];
		for (short yalIndex = 0; yalIndex < yals.size(); yalIndex++) {
			for (short clusterIndex = 0; clusterIndex < datas.size(); clusterIndex++) {
				list[yalIndex][clusterIndex] = getCost(yals.get(yalIndex), datas.get(clusterIndex));
			}
		}
		return list;
	}

	private double getCost(Yal yal, ClusterData clusterData) {
		Point mid = new Point((yal.getHead().getX() + yal.getTail().getX()) / 2, (yal.getHead().getY() + yal.getTail().getY()) / 2);
		double cost1 = Point.distance(yal.getHead().getX(), yal.getHead().getY(), clusterData.getX(), clusterData.getY());
		double cost2 = Point.distance(mid.getX(), mid.getY(), clusterData.getX(), clusterData.getY());
		double cost3 = Point.distance(yal.getTail().getX(), yal.getTail().getY(), clusterData.getX(), clusterData.getY());
		return Math.min(Math.min(cost1, cost2), cost3);
	}

	private void makeYals() {
		long start = System.currentTimeMillis();
		ArrayList<Area> aList = new ArrayList<Area>();

		for (Area a : model().getJunctionPoints()) {
			for (Area area2 : a.getNeighbours()) {
				aList.add(area2);
			}

		}
		ConvexHull_arr_New chn = new ConvexHull_arr_New(aList);
		HashSet<Area> newAreaHash = new HashSet<Area>();

		for (Area area2 : model().getHighways()) {
			//			if(chn.isInConvex(new Point(area2.getX(), area2.getY())))
			//				newAreaHash.add(area2);
			if (chn.contains(area2.getX(), area2.getY()))
				newAreaHash.add(area2);
		}

		for (Area area : model().getJunctionPoints()) {
			HashSet<Area> tmp = new HashSet<Area>();

			tmp.addAll(area.getNeighbours());
			yalBetweenJunctionPoints.put(area, new ArrayList<Area>());
			for (Area a : area.getNeighbours()) {
				Area neighbor = getHighwaysNeighbour(newAreaHash, model().getJunctionPoints(), new HashSet<Area>(tmp), a, area);
				if (neighbor != null && !yalBetweenJunctionPoints.get(area).contains(neighbor)) {
					yalBetweenJunctionPoints.get(area).add(neighbor);
					Yal yal = new Yal(area, neighbor);
					if (!yals.contains(yal))
						yals.add(yal);
				}
			}
		}

		log.trace("road's yal= " + yalBetweenJunctionPoints);
		log.info("Making yals got:" + (System.currentTimeMillis() - start) + "ms");
	}

	private Area getHighwaysNeighbour(HashSet<Area> areahash, HashSet<Area> junctionPoints2, HashSet<Area> OldPath, Area area, Area startTarget) {
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

	private int startIndex = -1;

	@Override
	public void act() throws SOSActionException {
		if (selectedYal != null || notClearedYals.size() > 0) {
			if (selectedYal == null)
				selectedYal = selectNewYal(notClearedYals);
			selectedYal.remove(agent.me().getAreaPosition());
			if (selectedYal.size() == 0) {
				if (notClearedYals.size() > 0) {
					selectedYal = selectNewYal(notClearedYals);
				} else {
					selectedYal = null;
				}
			}
			if (selectedYal != null)
				move(selectedYal);
		}

	}

	private ArrayList<Area> selectNewYal(ArrayList<Yal> list) {
		if (list.size() > 0) {
			ArrayList<Area> result = new ArrayList<Area>();
			Yal temp = nearestYalToMe(list);
			list.remove(temp);
			result.add(temp.getHead());
			result.add(temp.getTail());
			return result;
		}
		return null;
	}

	private Yal nearestYalToMe(ArrayList<Yal> list) {
		Yal result = list.get(0);
		int dis = Integer.MAX_VALUE;
		for (Yal yal : list) {
			int temp = (int) Math.min(Point.distance(agent.me().getX(), agent.me().getY(), yal.getHead().getX(), yal.getHead().getY())
					, Point.distance(agent.me().getX(), agent.me().getY(), yal.getTail().getX(), yal.getTail().getY()));
			if (temp < dis) {
				result = yal;
				dis = temp;
			}
		}
		return result;
	}

	public void makeToTasks() throws SOSActionException {
		log.info("Reachabling to tasks=" + agent_tasks.get(model().meTask()));
		ArrayList<Area> shouldOpenTasks = new ArrayList<Area>();
		boolean resetStartIndex = true;
		ArrayList<Task<? extends StandardEntity>> myTasks = agent_tasks.get(model().meTask());
		for (int i = 0; i < myTasks.size(); i++) {
			Task<? extends StandardEntity> task = myTasks.get((i + startIndex) % myTasks.size());
			long policeMoveWeight = agent.move.getWeightTo(task.getAreaPosition(), PoliceMove.class);
			long searchMoveWeight = agent.move.getWeightTo(task.getAreaPosition(), SearchMove.class);
			if (model().me().getAreaPosition().equals(task.getRealEntity().getAreaPosition()) || Math.abs(policeMoveWeight - searchMoveWeight) < 100) {
				resetStartIndex = true;
				task.setDone(true);
			}
			if (!task.isDoneWithoutCalc()) {
				shouldOpenTasks.add(task.getRealEntity().getAreaPosition());
			}
		}
		if (resetStartIndex)
			startIndex = -1;
		//		Collections.sort(shouldOpenTasks,new Comparator<Area>() {
		//
		//			@Override
		//			public int compare(Area o1, Area o2) {
		//				for (AbstractFireSite fireSite : agent.model().getFireSites()) {
		//					ConvexHull_arr_New convex = new ConvexHull_arr_New(fireSite.getFires());
		//					if(convex.contains(o1.getX(), o2.getX()))
		//				}
		//				return 0;
		//			}
		//		});
		log.debug("should open tasks are: " + shouldOpenTasks);

		for (Area task : shouldOpenTasks) {

			move(task);
		}
		log.debug("all tasks are reachable...}}}}}}}}}}}}}");

	}

	public class Yal {
		private final Area head;
		private final Area tail;

		//	private PoliceForceTask assigner = null;

		public Yal(Area head, Area tail) {
			this.head = head;
			this.tail = tail;

		}

		/*
		 * public void setAssigner(PoliceForceTask policeForceTask) {
		 * this.assigner = policeForceTask;
		 * }
		 * public PoliceForceTask getAssigner() {
		 * return assigner;
		 * }
		 * public boolean isAssigned() {
		 * return assigner != null;
		 * }
		 */
		public Area getHead() {
			return head;
		}

		public Area getTail() {
			return tail;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Yal) {
				Yal yal = (Yal) obj;
				return (yal.head.equals(head) && yal.tail.equals(tail)) || (yal.head.equals(tail) && yal.tail.equals(head));

			}
			return false;
		}

		@Override
		public int hashCode() {
			//			if(tail.getID().getValue()<head.getID().getValue())
			//				return tail.getID().getValue()+head.getID().getValue();
			return tail.getID().getValue() + head.getID().getValue();

		}

		@Override
		public String toString() {
			return "[" + head + "," + tail + "]";
		}
	}
}
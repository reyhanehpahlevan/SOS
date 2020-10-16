package sos.police_v2.state;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Edge;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.move.types.PoliceMove;
import sos.base.move.types.SearchMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.SOSActionException;
import sos.base.util.blockadeEstimator.AliGeometryTools;
import sos.fire_v2.base.tools.FireUtill;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.state.preCompute.PoliceForceTask;
import sos.police_v2.state.preCompute.Task;

public class OpenHighwayesState extends PoliceAbstractState {
	public HashMap<Area, ArrayList<Area>> yalBetweenJunctionPoints = new HashMap<Area, ArrayList<Area>>();
	public HashSet<Yal> yals = new HashSet<OpenHighwayesState.Yal>();
	//	public HashMap<PoliceForce, ArrayList<Task>> tasks=new HashMap<PoliceForce, ArrayList<Task>>();
	//	ArrayList<Task<? extends StandardEntity>> alltasks = new ArrayList<Task<?>>();
	public HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>> agent_tasks = new HashMap<PoliceForceTask, ArrayList<Task<? extends StandardEntity>>>();
	public HashMap<PoliceForceTask, ArrayList<Yal>> agent_yals = new HashMap<PoliceForceTask, ArrayList<Yal>>();

	public OpenHighwayesState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}
	@Override
	public void precompute() {
		/*
		 * assigneHighways();
		 */
		makeYals();
		assignTasks();
		
	}


	private void assignTasks() {
		long start = System.currentTimeMillis();
		for (PoliceForceTask iterable_element : model().getPoliceTasks()) {
			agent_tasks.put(iterable_element, new ArrayList<Task<? extends StandardEntity>>());
			agent_yals.put(iterable_element, new ArrayList<Yal>());
		}
		log.debug("yals size:" + yals.size() + " yals=" + yals);

		int policeForAssignTasks = Math.min(model().policeForces().size() / 2, 6);
		log.debug("policeForAssignTasks count=" + policeForAssignTasks);
		int foreachPolice = (int) Math.floor((((double) yals.size()) / (double) policeForAssignTasks + 1));
		log.debug("foreachPolice =" + foreachPolice);
		ArrayList<Yal> remaindedYals = new ArrayList<Yal>(yals);
		for (int i = 0; i < policeForAssignTasks; i++) {
			log.debug("remainded yals =" + remaindedYals);
			PoliceForceTask police = getBestPoliceToSendTaskTo();
			Area lastArea = null;
			FOR: for (int k = 0; k < foreachPolice && !remaindedYals.isEmpty(); k++) {
				log.trace("LastArea is:" + lastArea);
				if (lastArea != null && !yalBetweenJunctionPoints.get(lastArea).isEmpty()) {
					log.trace("Yal between junction that is connected to last area:", yalBetweenJunctionPoints.get(lastArea));
					for (Area areatarget : yalBetweenJunctionPoints.get(lastArea)) {
						Yal yal = new Yal(lastArea, areatarget);
						if (remaindedYals.remove(yal)) {
							log.trace("This Yal" + yal + "has not been choosen yet!! and is connected to last area");
							Task<StandardEntity> t1 = new Task<StandardEntity>(yal.getHead(), yal.getHead().getPositionPair());
							Task<StandardEntity> t2 = new Task<StandardEntity>(yal.getTail(), yal.getTail().getPositionPair());
							//							alltasks.add(t2);
							agent_tasks.get(police).add(t1);
							agent_tasks.get(police).add(t2);
							agent_yals.get(police).add(yal);
							lastArea = yal.getTail();
							continue FOR;
						}

					}
				}
				log.debug("There is no yal that connected to current yals!!!! so pick a random yal!!!");//TODO

				Yal yal = remaindedYals.remove(remaindedYals.size() - 1);
				Task<StandardEntity> t1 = new Task<StandardEntity>(yal.getHead(), yal.getHead().getPositionPair());
				Task<StandardEntity> t2 = new Task<StandardEntity>(yal.getTail(), yal.getTail().getPositionPair());
				//				alltasks.add(t1);
				agent_tasks.get(police).add(t1);
				agent_tasks.get(police).add(t2);
				agent_yals.get(police).add(yal);
				lastArea = yal.getTail();
			}

		}
		log.debug("assigned" + agent_tasks);
		log.info("Highway task assign got:" + (System.currentTimeMillis() - start) + "ms");
	}

	private PoliceForceTask getBestPoliceToSendTaskTo() {

		PoliceForceTask minJobDonedPolice = model().getPoliceTasks().get(0);
		for (PoliceForceTask next : model().getPoliceTasks()) {
			PoliceForce nextPolice = next.getRealEntity();
			if (!(nextPolice.isHPDefined() && nextPolice.getHP() < 5) && next.getJobDone() < minJobDonedPolice.getJobDone())
				minJobDonedPolice = next;
		}
		log.debug("BestPoliceToSendTaskTo is:" + minJobDonedPolice);
		minJobDonedPolice.addJobDone(1000, false);//TODO
		return minJobDonedPolice;

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

		//	makeReachable((Area) model().getEntity((971)));
		if (startIndex < 0) {
			ArrayList<Task<? extends StandardEntity>> mytasks = agent_tasks.get(model().meTask());
			for (Yal yal : agent_yals.get(model().meTask())) {
				if (isInAFireZone(yal)) {
					startIndex = -1;
					for (int i = 0; i < mytasks.size(); i++) {
						if (mytasks.get(i).getRealEntity().equals(yal.getHead()))
							startIndex = i;
					}
				}
				if (startIndex >= 0)
					break;
			}
			if (startIndex < 0) {
				int minCost = Integer.MAX_VALUE;
				ArrayList<Task<? extends StandardEntity>> myTasks = agent_tasks.get(model().meTask());
				for (int i = 0; i < myTasks.size(); i++) {
					Task<? extends StandardEntity> task = myTasks.get(i);
					long cost = agent.move.getWeightTo(task.getAreaPosition(), task.getX(), task.getY(), PoliceMove.class);
					if (cost < minCost)
						startIndex = i;
				}
			}
		}
		makeToTasks();
		//		move(model().me().getAreaPosition());
	}

	private boolean isInAFireZone(Yal yal) {
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfs : model().getFireSites()) {
			SOSEstimatedFireZone fs = pfs.second();
			ConvexHull_arr_New convex = fs.getConvex().getScaleConvex(1.4f);
			Point p = null, a, b, c, d;
			a = new Point(yal.getHead().getX(), yal.getHead().getY());
			b = new Point(yal.getTail().getX(), yal.getTail().getY());
			List<Edge> edges = AliGeometryTools.getEdges(convex.getApexes());
			//				boolean roadSet = false;
			for (Edge edge : edges) {
				c = new Point(edge.getStartX(), edge.getStartY());
				d = new Point(edge.getEndX(), edge.getEndY());
				p = FireUtill.intersect(a, b, c, d);
				if (p != null) {
					//						neighbor.setIntersectedRoadIndex(r);
					//						neighbor.isIntersectedWithRoad = true;
					//						roadSet = true;
					//						break;
					return true;
				} else {
					p = FireUtill.intersectionZJU(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y);
					if (p.x != -1 && p.y != -1) {
						//							neighbor.setIntersectedRoadIndex(r);
						//							neighbor.isIntersectedWithRoad = true;
						return true;
						//							roadSet = true;
						//							break;
					}
				}
			}
			return false;

		}
		return false;
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
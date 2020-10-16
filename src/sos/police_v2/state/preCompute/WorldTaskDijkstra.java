package sos.police_v2.state.preCompute;

import java.awt.Color;
import java.util.ArrayList;

import rescuecore2.geometry.Line2D;
import sos.base.entities.Area;
import sos.base.entities.Edge;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.misc.gui.ShapeDebugFrame;
import sos.base.util.namayangar.misc.gui.ShapeDebugFrame.ShapeInfo;
import sos.base.worldGraph.Node;
import sos.base.worldGraph.WorldGraphEdge;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.PoliceTaskGraphWeight;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.Dijkstra;
import sos.tools.GraphEdge;

public class WorldTaskDijkstra {

	public final Dijkstra[] dijkstra;
	public final ArrayList<GraphEdge>[] allPathHistory;
	private final PoliceTaskGraphWeight policeMoveWeight;
	private final PoliceForceAgent agent;
	private final ArrayList<Task<? extends StandardEntity>> tasks;

	@SuppressWarnings("unchecked")
	public WorldTaskDijkstra(PoliceForceAgent agent, ArrayList<Task<? extends StandardEntity>> tasks2) {
		this.agent = agent;
		this.tasks = tasks2;
		policeMoveWeight = new PoliceTaskGraphWeight(agent.model(), agent.model().getWorldGraph());
		dijkstra = new Dijkstra[tasks2.size()];
		allPathHistory = new ArrayList[agent.model().policeForces().size()];

		for (Task<?> task : tasks2) {
			dijkstra[task.getIndex()] = new Dijkstra(agent.model().getWorldGraph().getEdgesSize(), agent.model());
			try {
				dijkstra[task.getIndex()].Run(agent.model().getWorldGraph(), policeMoveWeight, getOutsideNodes(task.getAreaPosition()), getCostsOfOutSidesFrom(task.getAreaPosition()));

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		//		setDebugBackground();
	}

	public long getDijkstraWeight(Task<?> source, Task<?> dest) {
		if (source.getPositionPair().first().equals(dest.getPositionPair().first()))
			return 0;

		long min = Long.MAX_VALUE;
		//		int minNode = -1;
		Area destArea = dest.getRealEntity().getAreaPosition();
		for (Edge ed : destArea.getPassableEdges()) {
			long dijkstraWeight = dijkstra[source.getIndex()].getWeight(ed.getNodeIndex());
			int inAreaWeight = policeMoveWeight.getWeightToXY(destArea, ed, dest.getPositionPair().second());
			long resultWeight = dijkstraWeight + inAreaWeight;
			if (resultWeight < min) {
				min = resultWeight;
				//				minNode = ed.getNodeIndex();
			}
		}
		return min;
	}

	public long getDijkstraWeight(Task<?> source, ClusterData clusterData) {
		if (source.getPositionPair().first().equals(clusterData.getNearestBuildingToCenter().getPositionPair().first()))
			return 0;

		long min = Long.MAX_VALUE;
		//		int minNode = -1;
		Area destArea = clusterData.getNearestBuildingToCenter();
		for (Edge ed : destArea.getPassableEdges()) {
			long dijkstraWeight = dijkstra[source.getIndex()].getWeight(ed.getNodeIndex());
			int inAreaWeight = policeMoveWeight.getWeightToXY(destArea, ed, clusterData.getNearestBuildingToCenter().getPositionPair().second());
			long resultWeight = dijkstraWeight + inAreaWeight;
			if (resultWeight < min) {
				min = resultWeight;
				//				minNode = ed.getNodeIndex();
			}
		}
		return min;
	}
	

	public long getDijkstraWeight(Task<?> source, Area area) {
		if (source.getPositionPair().first().equals(area.getPositionPair().first()))
			return 0;

		long min = Long.MAX_VALUE;
		//		int minNode = -1;
		Area destArea = area;
		for (Edge ed : destArea.getPassableEdges()) {
			long dijkstraWeight = dijkstra[source.getIndex()].getWeight(ed.getNodeIndex());
			int inAreaWeight = policeMoveWeight.getWeightToXY(destArea, ed, area.getPositionPair().second());
			long resultWeight = dijkstraWeight + inAreaWeight;
			if (resultWeight < min) {
				min = resultWeight;
				//				minNode = ed.getNodeIndex();
			}
		}
		return min;
	}

	protected ArrayList<Integer> getOutsideNodes(Area area) {
		ArrayList<Integer> result = new ArrayList<Integer>(5);
		for (Edge ed : area.getPassableEdges())
			result.add((int) ed.getNodeIndex());
		return result;
	}

	protected int[] getCostsOfOutSidesFrom(Area area) {
		int[] result = new int[area.getPassableEdges().length];
		for (int i = 0; i < area.getPassableEdges().length; i++) {
			Edge ed = area.getPassableEdges()[i];
			result[i] = policeMoveWeight.getWeightToXY(area, ed, area.getPositionPoint());
		}
		return result;
	}

	public void changePosition(PoliceForceTask from, Task<? extends StandardEntity> to) {
		dijkstra[from.getIndex()] = dijkstra[to.getIndex()];
	}

	public void saveMovePath(ArrayList<Integer> getpathArray) {
		if (getpathArray.isEmpty() && tasks.get(getpathArray.get(0)).getRealEntity() instanceof PoliceForce) {
			agent.log.error("path is illigal----in WorldTaskDijkstra.saveMovePath---");
			return;
		}
		short policeIndex = ((PoliceForce) tasks.get(getpathArray.get(0)).getRealEntity()).getPoliceIndex();
		if (allPathHistory[policeIndex] == null)
			allPathHistory[policeIndex] = new ArrayList<GraphEdge>();
		allPathHistory[policeIndex].addAll(getGraphEdgesOfPath(getpathArray));
		//		debug.show("allpath",getShape(allPathHistory[policeIndex].toArray(new GraphEdge[0])));
	}

	public ArrayList<GraphEdge> getGraphEdgesOfPath(ArrayList<Integer> getpathArray) {
		ArrayList<GraphEdge> graphEdges = new ArrayList<GraphEdge>();
		if (getpathArray.isEmpty())
			return graphEdges;

		Task<?> source = tasks.get(getpathArray.get(0));
		for (int i = 1; i < getpathArray.size(); i++) {
			Task<?> dest = tasks.get(getpathArray.get(i));

			int destNode = getDijkstraNode(source, dest);
			if (destNode == -1) {
				agent.log.warn("WorldTaskDijkstra dest node==src node", false);
				continue;
			}
			ArrayList<Integer> pathNodesIndx = dijkstra[source.getIndex()].getpathArray(destNode);
			Node[] pathNodes = new Node[pathNodesIndx.size()];
			for (int k = 0; k < pathNodesIndx.size(); ++k)
				pathNodes[k] = agent.model().nodes().get(pathNodesIndx.get(k));

			GraphEdge[] pathEdges = getEdgesByNodes(pathNodes);
			for (GraphEdge ge : pathEdges) {
				graphEdges.add(ge);
			}
			source = dest;
		}
		return graphEdges;
	}

	protected GraphEdge[] getEdgesByNodes(Node[] nodes) {
		GraphEdge[] pathEdges = new GraphEdge[nodes.length - 1];
		for (int i = 0, k = 1; k < nodes.length; ++i, ++k) {
			int edgeIndex = agent.model().getWorldGraph().edgeIndexBetween(nodes[i].getIndex(), nodes[k].getIndex());
			pathEdges[i] = agent.model().graphEdges().get(edgeIndex);
		}
		return pathEdges;
	}

	// -------------
	private int getDijkstraNode(Task<?> source, Task<?> dest) {
		if (source.getPositionPair().first().equals(dest.getPositionPair().first()))
			return -1;

		long min = Long.MAX_VALUE;
		int minNode = -1;
		Area destArea = dest.getPositionPair().first();
		for (Edge ed : destArea.getPassableEdges()) {
			long dijkstraWeight = dijkstra[source.getIndex()].getWeight(ed.getNodeIndex());
			int inAreaWeight = policeMoveWeight.getWeightToXY(destArea, ed, dest.getPositionPair().second());
			long resultWeight = dijkstraWeight + inAreaWeight;
			if (resultWeight < min) {
				min = resultWeight;
				minNode = ed.getNodeIndex();
			}
		}
		return minNode;
	}

	protected ArrayList<ShapeInfo> getShape(GraphEdge[] ge) {
		ArrayList<ShapeInfo> edgeShapes = new ArrayList<ShapeDebugFrame.ShapeInfo>();
		long resultWeight = 0;
		for (GraphEdge graphEdge : ge) {
			resultWeight += policeMoveWeight.getWeight(graphEdge);
			Node start = agent.model().nodes().get(graphEdge.getHeadIndex());
			Node end = agent.model().nodes().get(graphEdge.getTailIndex());
			Line2D line = new Line2D(start.getPosition().getX(), start.getPosition().getY(), end.getPosition().getX() - start.getPosition().getX(), end.getPosition().getY() - start.getPosition().getY());
			if (graphEdge instanceof WorldGraphEdge) {
				Color color;
				switch (graphEdge.getState()) {
				case Block:
					color = Color.RED;
					break;
				case Open:
					color = (Color.green.darker());
					break;
				case FoggyOpen:
					color = (Color.white).darker();
					break;
				case FoggyBlock:
					color = (Color.gray.darker());
					break;
				default:
					color = Color.LIGHT_GRAY;
				}
				edgeShapes.add(new ShapeDebugFrame.Line2DShapeInfo(line, "", color, false, true));
			}
		}
		edgeShapes.add(0, new ShapeDebugFrame.DetailInfo("currentWeight:" + resultWeight));
		return edgeShapes;
	}
	////////////////////////////////TEST/////////////////DEBUG////////////////////////////////

	//static ShapeDebugFrame debug = new ShapeDebugFrame();
	//private void setDebugBackground() {
	//ArrayList<AWTShapeInfo> back = new ArrayList<ShapeDebugFrame.AWTShapeInfo>();
	//for (Building b : agent.model().buildings()) {
	//back.add(new AWTShapeInfo(b.getShape(), b.toString(), Color.red, false));
	//}
	//for (Road r : agent.model().roads()) {
	//back.add(new AWTShapeInfo(r.getShape(), r.toString(), Color.gray, false));
	//}
	//debug.setBackground(back);
	//}

	//public void debug(ArrayList<Integer> getpathArray) {
	//Task<?> source = tasks.get(getpathArray.get(0));
	//Task<?> dest = tasks.get(getpathArray.get(getpathArray.size() - 1));
	//ArrayList<ShapeInfo> shapes = new ArrayList<ShapeDebugFrame.ShapeInfo>();
	//shapes.add(new AWTShapeInfo(source.getAreaPosition().getShape(), source.getAreaPosition().toString(), Color.yellow, false));
	//shapes.add(new AWTShapeInfo(dest.getAreaPosition().getShape(), dest.getAreaPosition().toString(), Color.green, false));
	//int d = getDijkstraNode(source, dest);
	//if (d == -1)
	//return;
	//ArrayList<Integer> pathNodesIndx = dijkstra[source.getIndex()].getpathArray(d);
	//Node[] pathNodes = new Node[pathNodesIndx.size()];
	//for (int k = 0; k < pathNodesIndx.size(); ++k)
	//pathNodes[k] = agent.model().nodes().get(pathNodesIndx.get(k));
	//
	//GraphEdge[] pathEdges = getEdgesByNodes(pathNodes);
	//shapes.add(new ShapeDebugFrame.DetailInfo("weight:" + getDijkstraWeight(source, dest)));
	//shapes.add(new ShapeDebugFrame.DetailInfo("distance:" + PoliceUtils.getDistance(source, dest)));
	//
	//shapes.addAll(getShape(pathEdges));
	//shapes.add(new ShapeDebugFrame.DetailInfo("weight:" + getDijkstraWeight(source, dest)));
	//
	//debug.show("movepath", shapes);
	//
	//}

}

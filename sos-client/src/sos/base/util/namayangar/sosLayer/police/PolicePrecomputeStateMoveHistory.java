package sos.base.util.namayangar.sosLayer.police;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.SOSWorldModel;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.base.worldGraph.Node;
import sos.base.worldGraph.WorldGraphEdge;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.police_v2.state.preCompute.Task;
import sos.tools.GraphEdge;

public class PolicePrecomputeStateMoveHistory extends SOSAbstractToolsLayer<PoliceForce> {

	private static final int SIZE = 5;

	public PolicePrecomputeStateMoveHistory() {
		super(PoliceForce.class);
		setVisible(false);
	}

	@Override
	protected void makeEntities() {
		setEntities(model().policeForces());

	}

	@Override
	protected Shape render(PoliceForce entity, Graphics2D g, ScreenTransform t) {
		PoliceForceAgent agent = (PoliceForceAgent) entity.getAgent();

		g.setStroke(new BasicStroke(5));
		
		Path2D path=new Path2D.Double();

		ArrayList<GraphEdge> graphEdges = agent.getState(PrecomputeState.class).worldTaskDijkstra.allPathHistory[entity.getPoliceIndex()];
		if (graphEdges != null) {
			
			for (GraphEdge graphEdge : graphEdges) {
				Node start = model().nodes().get(graphEdge.getHeadIndex());
				Node end = model().nodes().get(graphEdge.getTailIndex());
				Line2D line = new Line2D.Float(t.xToScreen(start.getPosition().getX()), t.yToScreen(start.getPosition().getY()), t.xToScreen(end.getPosition().getX()), t.yToScreen(end.getPosition().getY()));
				paintGraphEdge(graphEdge, line, g,entity);
				path.moveTo(line.getX1(), line.getY1());
				path.lineTo(line.getX2(), line.getY2());
				
			}
		}
		g.setStroke(new BasicStroke(1));
		int x=t.xToScreen(entity.getPositionPoint().getX());
		int y=t.yToScreen(entity.getPositionPoint().getY());

		Shape humshape = new Ellipse2D.Double(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
		g.setColor(Color.blue);
		g.fill(humshape);
		ArrayList<Task<? extends StandardEntity>> myTasks = agent.getState(PrecomputeState.class).getTasksOf(model().getPoliceTasks(entity));
		for (Task<?> task : myTasks) {
			int x1 = t.xToScreen(task.getRealEntity().getLocation().first());
			int y1 = t.yToScreen(task.getRealEntity().getLocation().second()) - 10;
			g.setColor(Color.white);
			g.setFont(new Font("Tahoma", Font.BOLD, 12));

			g.drawString("" + task.getFinalValue(), x1, y1);

		}

		return path;
	}

	Color[] c=new Color[]{Color.red,Color.yellow,Color.white,Color.green,Color.orange,Color.black,Color.blue,Color.CYAN,Color.magenta,Color.pink};
	protected void paintGraphEdge(GraphEdge e, Line2D line, Graphics2D g, PoliceForce entity) {
		g.setColor(c[entity.getID().getValue()%c.length]);
		if (e instanceof WorldGraphEdge)
			switch (e.getState()) {
			case Block:
				
//				g.setColor(Color.RED);
				break;
			case Open:
//				g.setColor(Color.green);
				break;
			case FoggyOpen:
//				g.setColor(Color.white);
				break;
			case FoggyBlock:
//				g.setColor(Color.gray);
				break;
			default:
				return;
			}
		
		g.drawLine((int) line.getX1(), (int) line.getY1(), (int) line.getX2(), (int) line.getY2());
	}

	@Override
	public PoliceWorldModel model() {
		return (PoliceWorldModel) world;
	}
	
	@Override
	public int getZIndex() {
		return 50;
	}


	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return ((SOSWorldModel)world).me() instanceof PoliceForce&&model().sosAgent().getState(PrecomputeState.class)!=null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(PoliceForce entity) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

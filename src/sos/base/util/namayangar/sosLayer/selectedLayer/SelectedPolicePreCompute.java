package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import sos.base.entities.PoliceForce;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.worldGraph.Node;
import sos.base.worldGraph.WorldGraphEdge;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.tools.GraphEdge;

public class SelectedPolicePreCompute extends SOSAbstractSelectedComponent<PoliceForce> {
	public SelectedPolicePreCompute() {
		super(PoliceForce.class);
		setVisible(true);
	}

	@Override
	protected void paint(PoliceForce entity, Graphics2D g, ScreenTransform t) {
		PoliceForceAgent agent = (PoliceForceAgent) entity.getAgent();

		g.setStroke(new BasicStroke(2));

		Path2D path = new Path2D.Double();

		ArrayList<GraphEdge> graphEdges = agent.getState(PrecomputeState.class).worldTaskDijkstra.allPathHistory[entity.getPoliceIndex()];
		if (graphEdges != null) {

			for (GraphEdge graphEdge : graphEdges) {
				Node start = model().nodes().get(graphEdge.getHeadIndex());
				Node end = model().nodes().get(graphEdge.getTailIndex());
				Line2D line = new Line2D.Float(t.xToScreen(start.getPosition().getX()), t.yToScreen(start.getPosition().getY()), t.xToScreen(end.getPosition().getX()), t.yToScreen(end.getPosition().getY()));
				paintGraphEdge(graphEdge, line, g, entity);
				path.moveTo(line.getX1(), line.getY1());
				path.lineTo(line.getX2(), line.getY2());

			}
		}
		g.setStroke(new BasicStroke(1));

	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel&&((PoliceForceAgent)model().sosAgent()).getState(PrecomputeState.class)!=null;
	}

	protected void paintGraphEdge(GraphEdge e, Line2D line, Graphics2D g, PoliceForce entity) {
		g.setColor(Color.yellow);
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

}

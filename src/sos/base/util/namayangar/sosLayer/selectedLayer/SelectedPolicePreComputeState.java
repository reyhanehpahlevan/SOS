package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.worldGraph.WorldGraphEdge;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.PrecomputeState;
import sos.police_v2.state.preCompute.Task;
import sos.tools.GraphEdge;

public class SelectedPolicePreComputeState extends SOSAbstractSelectedComponent<PoliceForce> {
	public SelectedPolicePreComputeState() {
		super(PoliceForce.class);
		setVisible(true);
	}

	@Override
	protected void paint(PoliceForce entity, Graphics2D g, ScreenTransform t) {
		PoliceForceAgent agent = (PoliceForceAgent) entity.getAgent();
		ArrayList<Task<? extends StandardEntity>> tasks = agent.getState(PrecomputeState.class).getTasksOf(model().getPoliceTasks(entity));
		g.setColor(Color.yellow);
		g.setStroke(new BasicStroke(2));
		for (Task<? extends StandardEntity> task : tasks) {
			NamayangarUtils.drawEntity(task.getRealEntity(), g, t);
		}
		g.setStroke(new BasicStroke(1));

	}
	@Override
	public PoliceWorldModel model() {
		// TODO Auto-generated method stub
		return (PoliceWorldModel) super.model();
	}

	@Override
	public boolean isValid() {
		return super.model() instanceof PoliceWorldModel&&model().sosAgent().getState(PrecomputeState.class)!=null;
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

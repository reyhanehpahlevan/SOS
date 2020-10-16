package sos.base.util.namayangar.sosLayer.police;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
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
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.MSTState;
import sos.police_v2.state.preCompute.Task;

public class PoliceMSTTaskAssignLayer extends SOSAbstractToolsLayer<PoliceForce> {

	public PoliceMSTTaskAssignLayer() {
		super(PoliceForce.class);
		setVisible(false);
	}

	@Override
	public PoliceWorldModel model() {
		return (PoliceWorldModel) world;
	}

	@Override
	protected Shape render(PoliceForce entity, Graphics2D g, ScreenTransform transform) {
		Path2D path2d = new Path2D.Double();
		int x = transform.xToScreen(entity.getPositionPoint().getX());
		int y = transform.yToScreen(entity.getPositionPoint().getY());
		path2d.moveTo(x, y);
		PoliceForceAgent agent = model().sosAgent();
				//		paintShape(entity, path2d, g);
		Path2D path = new Path2D.Double();
		path.moveTo(x, y);
		ArrayList<Task<? extends StandardEntity>> mstTasks = agent.getState(MSTState.class).getTasksOf(model().getPoliceTasks(entity));
		for (Task<?> t : mstTasks) {
			int x2 = transform.xToScreen(t.getRealEntity().getLocation().first());
			int y2 = transform.yToScreen(t.getRealEntity().getLocation().second());
			path.lineTo(x2, y2);
		}
		paintMSTTaskShape(entity, path, g);
		//		Shape humshape = new Ellipse2D.Double(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
		//		g.setColor(Color.blue);
		//		g.fill(humshape);

		return path;
	}

	private void paintMSTTaskShape(PoliceForce entity, Path2D shape, Graphics2D g) {
		g.setColor(Color.yellow);
		g.setStroke(new BasicStroke(2));
		Stroke a = g.getStroke();
		g.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 2, new float[] { 5.0f, 5.0f }, 0.0f));
		g.draw(shape);
		g.setStroke(a);

	}

	//	private static final int SIZE = 6;

	protected void paintShape(PoliceForce entity, Shape shape, Graphics2D g) {
		g.setColor(new Color((entity.getID().getValue() % 30) * 1000000));
		g.setStroke(new BasicStroke(2));
		g.draw(shape);
		g.setStroke(new BasicStroke(1));

	}

	@Override
	public int getZIndex() {
		return 50;
	}

	@Override
	protected void makeEntities() {
		setEntities(model().policeForces());
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		return ((SOSWorldModel) world).me() instanceof PoliceForce &&model().sosAgent().getState(MSTState.class)!=null;
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

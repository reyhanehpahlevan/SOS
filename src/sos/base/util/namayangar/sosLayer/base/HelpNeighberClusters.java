package sos.base.util.namayangar.sosLayer.base;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenCivilianState;
import sos.search_v2.tools.cluster.ClusterData;

public class HelpNeighberClusters extends SOSAbstractToolsLayer<ClusterData> {

	public HelpNeighberClusters() {
		super(ClusterData.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 900;
	}

	@Override
	protected void makeEntities() {
		ArrayList<ClusterData> list = ((PoliceForceAgent) (model().me().getAgent())).getState(OpenCivilianState.class).helpClusters;
		setEntities(list);
	}

	@Override
	protected Shape render(ClusterData entity, Graphics2D g, ScreenTransform transform) {
		if (entity != null) {
			Shape shape = entity.getConvexShape();
			g.setStroke(new BasicStroke(2));
			g.setColor(Color.magenta);
			NamayangarUtils.drawShape(shape, g, transform);
		}
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		return (model() instanceof PoliceWorldModel && ((PoliceForceAgent) model().me().getAgent()).getState(OpenCivilianState.class) != null);
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(ClusterData entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

package sos.base.util.namayangar.sosLayer.police;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.afterShock.AfterShockClusterConnector;
import sos.police_v2.state.preCompute.ClusterMST;

public class ClusterMSTGraphLayer extends SOSAbstractToolsLayer<ClusterMST> {

	public ClusterMSTGraphLayer() {
		super(ClusterMST.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 20;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) (model().me().getAgent())).getState(AfterShockClusterConnector.class).clusterMST);
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(ClusterMST entity) {
		return null;
	}

	@Override
	protected Shape render(ClusterMST entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(new Color(150, 100, 50));
		for (Pair<Integer, Integer> p : entity.getResult()) {
			Building a = entity.clusterDatas.get(p.first()).getNearestBuildingToCenter();
			Building b = entity.clusterDatas.get(p.second()).getNearestBuildingToCenter();
			NamayangarUtils.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), g, transform);
			//			g.drawLine(transform.xToScreen(a.getX()), transform.yToScreen(a.getY()), transform.xToScreen(b.getX()), transform.yToScreen(b.getY()));
		}
		if (entity.myEdge != null) {
			g.setColor(new Color(200, 100, 250));
			Building a = entity.clusterDatas.get(entity.myEdge.first()).getNearestBuildingToCenter();
			Building b = entity.clusterDatas.get(entity.myEdge.second()).getNearestBuildingToCenter();
			NamayangarUtils.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), g, transform);
		}
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

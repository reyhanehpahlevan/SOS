package sos.base.util.namayangar.sosLayer.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.cluster.ClusterData;

public class BlockSearchNoCommPriority extends SOSAbstractToolsLayer<ClusterData> {

	public BlockSearchNoCommPriority() {
		super(null);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 5;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void makeEntities() {
		setEntities(search().strategyChooser.blockSearchNoComm.subStarClusters);
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
	protected Shape render(ClusterData entity, Graphics2D g, ScreenTransform transform) {
		int c = model().sosAgent().newSearch.strategyChooser.blockSearchNoComm.subStarClusters.indexOf(entity);
		Color col = new Color(((c) * 100) % 255, ((c) * 140) % 255, ((c) * 30) % 255);
		g.setColor(col);
		for (Area b : entity.getBuildings()) {
			Shape shape = NamayangarUtils.transformShape(b, transform);
			g.fill(shape);
		}
		g.setFont(new Font("Arial", 10, 16));
		g.setColor(Color.BLACK);
		NamayangarUtils.drawString(""+c, g, transform, (int)entity.getX(), (int)entity.getY());
		return null;
	}

	private AgentSearch<?> search() {
		return model().sosAgent().newSearch;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(ClusterData entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

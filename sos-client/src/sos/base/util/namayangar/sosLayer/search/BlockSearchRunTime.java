package sos.base.util.namayangar.sosLayer.search;

import java.awt.Color;
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

public class BlockSearchRunTime extends SOSAbstractToolsLayer<ArrayList<Area>> {

	public BlockSearchRunTime() {
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
		setEntities(search().strategyChooser.blockSearch.getAssignedRegion());
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
	protected Shape render(ArrayList<Area> entity, Graphics2D g, ScreenTransform transform) {
		//		int c = model().sosAgent().newSearch.getBlockSearchClusters().indexOf(entity);
		g.setColor(Color.red.darker().darker());
		for (Area b : entity) {
			Shape shape = NamayangarUtils.transformShape(b, transform);
			g.fill(shape);
		}
		g.setColor(Color.GREEN.darker().darker());
		for (Area b : search().strategyChooser.blockSearch.getCheckedRegion()) {
			Shape shape = NamayangarUtils.transformShape(b, transform);
			g.fill(shape);
		}
		return null;
	}

	private AgentSearch<?> search() {
		return model().sosAgent().newSearch;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(ArrayList<Area> entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

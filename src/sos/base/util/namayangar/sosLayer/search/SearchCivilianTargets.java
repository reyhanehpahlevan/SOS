package sos.base.util.namayangar.sosLayer.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;

public class SearchCivilianTargets extends SOSAbstractToolsLayer<ShapeInArea> {

	public SearchCivilianTargets() {
		super(ShapeInArea.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 10;
	}

	@Override
	protected void makeEntities() {
		setEntities(model().sosAgent().newSearch.strategyChooser.civilianSearch.targets);
	}

	@Override
	protected Shape render(ShapeInArea entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.blue);
		Shape shape = NamayangarUtils.transformShape(entity, transform);
		g.draw(shape);
		return shape;
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
	public ArrayList<Pair<String, String>> sosInspect(ShapeInArea entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

package sos.base.util.namayangar.sosLayer.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.PoliceForce;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.search_v2.agentSearch.PoliceSearch;

public class BlockSearchRegions extends SOSAbstractToolsLayer<Area> {

	public BlockSearchRegions() {
		super(Area.class);
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

	private PoliceSearch<?> search() {
		return ((PoliceSearch<?>) model().sosAgent().newSearch);
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model().me() instanceof PoliceForce;
	}

	@Override
	protected Shape render(Area entity, Graphics2D g, ScreenTransform transform) {
		int c = entity.getID().getValue() % 255;
		g.setColor(new Color(Math.abs(c * 25) % 255, Math.abs(17 * c) % 255, Math.abs(34 * c) % 255));
		Shape shape = NamayangarUtils.transformShape(entity, transform);
		g.fill(shape);
		//////////////////////////////
		int x1=(int) model().me().getPositionPoint().getX();
		int y1=(int) model().me().getPositionPoint().getY();
		int x2=(int) model().sosAgent().newSearch.getSearchWorld().getClusterData().getX();
		int y2=(int) model().sosAgent().newSearch.getSearchWorld().getClusterData().getX();
		NamayangarUtils.drawLine(x1, y1, x2, y2, g, transform);
		
		
		
		return null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Area entity) {
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

package sos.base.util.namayangar.sosLayer.search;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.search_v2.tools.cluster.ClusterData;

public class RJSBoundLayer extends SOSAbstractToolsLayer<ClusterData> {

	public RJSBoundLayer() {
		super(ClusterData.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 5;
	}

	@Override
	protected void makeEntities() {
		setEntities(model().searchWorldModel.getAllClusters());
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
	protected Shape render(ClusterData cd, Graphics2D g, ScreenTransform transform) {

		g.setStroke(new BasicStroke(1));

//			Color c = NamayangarUtils.randomColor();
//			for (Building b : cd.getBuildings()) {
//				g.setColor(c);
//				g.draw(NamayangarUtils.transformShape(b.getShape(), transform));
//				if (((b.isFierynessDefined() && !(b.getFieryness() > 0))||!b.isFierynessDefined()) && ((model().me().getAgent().newSearch.getSearchWorld().getSearchBuilding(b).needsToBeSearchedForCivilian()) || !(model().me().getAgent().newSearch.getSearchWorld().getSearchBuilding(b).isSearchedForFire()))) {
//					if (model().me().getAgent().newSearch.getSearchWorld().getSearchBuilding(b).getCivProbability() > 0 || (b.getCivilians().size() > 0 && !b.isReallyReachableSearch())) {
//						g.setColor(Color.GREEN.brighter().brighter().brighter().brighter().brighter().brighter().brighter());
//						g.fill(NamayangarUtils.transformShape(b.getShape(), transform));
//					}
//				} else {
//					g.setColor(Color.BLACK.brighter());
//					g.fill(NamayangarUtils.transformShape(b.getShape(), transform));
//
//				}
//			}
			g.setColor(Color.white);
			g.setStroke(new BasicStroke(3));
			g.setFont(new Font("Arial", 10, 16));
			NamayangarUtils.drawString("RJS: " + model().sosAgent().newSearch.getRemainingJobScorer().remainingJobScore(cd),g, transform, (int) cd.getX(), (int) cd.getY());

		return null;
	}

//	public StarSearch<?> starSearch() {
//		return model().me().getAgent().newSearch.strategyChooser.starSearch;
//	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(ClusterData entity) {
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

package sos.base.util.namayangar.sosLayer.search;

 import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.search_v2.searchType.CommunicationlessSearch;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.Utils;
/**
 * 
 * @author Salim
 *
 */
public class NoCommunicationSearch extends SOSAbstractToolsLayer<SOSAgent<?>> {

	public NoCommunicationSearch() {
		super(null);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 50;
	}

	@Override
	protected void makeEntities() {
		setEntities(model().me().getAgent());

	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
//		return model().sosAgent().messageSystem.type==Type.NoComunication;
		return true;
	}

	@Override
	protected Shape render(SOSAgent<?> entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.MAGENTA);
//		Road gatheringArea = model().me().getAgent().newSearch.strategyChooser.noCommunication.getGatheringArea();
//		g.fill(NamayangarUtils.transformShape(gatheringArea, transform));
		double mapMaxDistance = Math.max(Utils.distance(model().getWorldBounds().first().first(), 0, model().getWorldBounds().second().first(), 0) / 2, Utils.distance(0, model().getWorldBounds().first().second(), 0, model().getWorldBounds().second().second()) / 2);
		//		System.out.println(mapMaxDistance+"============="+"1/3: "+1 / 3 * mapMaxDistance+" 2/3"+2 / 3 * mapMaxDistance);
		double cx = model().mapCenter().getX();
		double cy = model().mapCenter().getY();
		
//		
//		for (ClusterData cd : model().searchWorldModel.getAllClusters()) {
//			double d = Utils.distance(cd.getX(), cd.getY(), cx, cy);
//
//			if (d < (((double) 2 / 5) * mapMaxDistance)) {
//				g.setColor(Color.green);
//			} else if (d < (((double) 2 / 3) * mapMaxDistance)) {
//				g.setColor(Color.yellow);
//			} else {
//				double miNDistance = itSouldBeFilterred(cd, g, model().searchWorldModel.getAllClusters(), transform);
//				if (miNDistance > MIN_DISTANCE_THRESHOLD())
//					g.setColor(Color.black);
//				else
//					g.setColor(Color.red);
//			}
//
//			for (Building b : cd.getBuildings()) {
//				g.draw(NamayangarUtils.transformShape(b, transform));
//			}
//		}
		Road model = CommunicationlessSearch.selectGatheringArea(model());
		System.out.println(model);
		NamayangarUtils.fillEntity(model, g, transform);
		return null;
	}

	
	private double MIN_DISTANCE_THRESHOLD() {
		if (model().me().getAgent().getMapInfo().isBigMap())
			return 60000;
		if (model().me().getAgent().getMapInfo().isMediumMap())
			return 25000;
		else
			return 10000;
	}

	private double itSouldBeFilterred(ClusterData cd, Graphics2D g, Collection<ClusterData> values, ScreenTransform transform) {
		ArrayList<Pair<ClusterData, Double>> distances = new ArrayList<Pair<ClusterData, Double>>(values.size());
		for (ClusterData c : values) {
			double d = Utils.distance(c.getX(), c.getY(), cd.getX(), cd.getY());
			distances.add(new Pair<ClusterData, Double>(c, d));
		}
		Collections.sort(distances, new Comparator<Pair<ClusterData, Double>>() {

			@Override
			public int compare(Pair<ClusterData, Double> o1, Pair<ClusterData, Double> o2) {
				if (o1.second() < o2.second())
					return -1;
				if (o1.second() > o2.second())
					return 1;
				return 0;
			}
		});
		double maxDistance = -1;
		String tmp = "";
		for (int i = 1; i < Math.min(distances.size() - 1, 4); i++) {
			tmp += " " + distances.get(i).first().getIndex();
			double minDistance = Integer.MAX_VALUE;
			for (Building b : cd.getBuildings()) {
				for (Building b2 : distances.get(i).first().getBuildings()) {
					double d = Utils.distance(b.getX(), b.getY(), b2.getX(), b2.getY());
					if (d == 0)
						continue;
					if (d < minDistance)
						minDistance = d;
				}

			}
			if (minDistance > maxDistance)
				maxDistance += minDistance;

		}
		maxDistance = maxDistance / 3;
		g.setColor(Color.white);
		g.setFont(new Font("Arial", 10, 20));
		NamayangarUtils.drawString("i:" + cd.getIndex() + "[" + tmp + " ]" + (int) maxDistance, g, transform, (int) cd.getX(), (int) cd.getY());
		return maxDistance;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(SOSAgent<?> entity) {
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}
}

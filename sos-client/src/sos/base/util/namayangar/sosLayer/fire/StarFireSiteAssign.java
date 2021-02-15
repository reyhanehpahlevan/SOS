package sos.base.util.namayangar.sosLayer.fire;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.base.util.namayangar.tools.SOSSelectedObj;
import sos.base.util.namayangar.tools.SelectedObjectListener;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.tools.FireStarZone;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.search_v2.tools.cluster.ClusterData;

public class StarFireSiteAssign extends SOSAbstractToolsLayer<FireBrigade> implements SelectedObjectListener {

	public StarFireSiteAssign() {
		super(FireBrigade.class);
		setVisible(false);
	}

	@Override
	public void preCompute() {
		super.preCompute();
		getViewer().getViewerFrame().addSelectedObjectListener(this);
	}

	@Override
	public int getZIndex() {
		return 5;
	}

	ArrayList<FireBrigade> dd = new ArrayList<FireBrigade>();

	@Override
	protected void makeEntities() {
		setEntities(dd);
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof FireWorldModel;
	}

	@Override
	protected Shape render(FireBrigade entity, Graphics2D g, ScreenTransform transform) {
		SOSFireZoneSelector chooser = ((FireBrigadeAgent) entity.model().sosAgent()).FDK.getInfoModel().getFireZoneSelector();
		System.out.println("choooser " + chooser.fireBrigade_Task);
		int x1 = entity.getX();
		int y1 = entity.getY();

		try {
			sos.fire_v2.target.SOSFireZoneSelector.Task task = chooser.fireBrigade_Task.get(entity);
			int index = task.getZoneIndex();

			FireStarZone cd = chooser.starCluster.getStarZones()[index];
			if (cd == null) {
				System.out.println("null");
				return null;
			}
			int x2 = (int) cd.getCx();
			int y2 = (int) cd.getCy();

			/////////////////////////////////////////

			Color col = new Color(((cd.getIndex() + 1) * 100) % 255, ((cd.getIndex() + 1) * 140) % 255, ((cd.getIndex() + 1) * 30) % 255);
			g.setColor(col);
			g.setStroke(new BasicStroke(4));
			for (Building b : cd.getZoneBuildings()) {
				NamayangarUtils.drawShape(b.getShape(), g, transform);
			}
			NamayangarUtils.drawLine(x1, y1, x2, y2, g, transform);

			g.drawArc(transform.xToScreen(x2), transform.yToScreen(y2), 10, 10, 0, 360);

		} catch (Exception e) {
		}
		///////////////////////////////
		try {
			ClusterData mycd = entity.model().sosAgent().newSearch.getSearchWorld().getClusterData(entity);
			Color col = new Color(((mycd.getIndex() + 1) * 100) % 255, ((mycd.getIndex() + 1) * 140) % 255, ((mycd.getIndex() + 1) * 30) % 255);
			g.setColor(col);
			g.setStroke(new BasicStroke(2));
			for (Building b : mycd.getBuildings()) {
				NamayangarUtils.drawShape(b.getShape(), g, transform);
			}

			///////////////////////////////////
			g.setColor(Color.red);
			int x3 = (int) mycd.getX();
			int y3 = (int) mycd.getY();

			NamayangarUtils.drawLine(x1, y1, x3, y3, g, transform);
			g.drawArc(transform.xToScreen(x3), transform.yToScreen(y3), 10, 10, 0, 360);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	//	@Override
	//	protected Shape render(FireBrigade entity, Graphics2D g, ScreenTransform transform) {
	//		SOSFireSiteChooser2012_LargeMap chooser = (SOSFireSiteChooser2012_LargeMap) ((FireBrigadeAgent) entity.model().sosAgent()).fireSiteChooser;
	//		System.out.println("choooser " + chooser.fbZones);
	//		int index = chooser.fbZones.get(entity);
	//		StarZone cd = chooser.starClusters.getStarZones()[index];
	//		//chooser.starClusters.getClusterMap().get(model().fireBrigades().get(chooser.fbZones.get(entity)));
	//		if (cd == null) {
	//			System.out.println("null");
	//			return null;
	//		}
	//		/////////////////////////////////////////
	//		Color col = new Color(((cd.getIndex() + 1) * 100) % 255, ((cd.getIndex() + 1) * 140) % 255, ((cd.getIndex() + 1) * 30) % 255);
	//		g.setColor(col);
	//		g.setStroke(new BasicStroke(2));
	//		for (Building b : cd.getZoneBuildings()) {
	//			NamayangarUtils.drawShape(b.getShape(), g, transform);
	//		}
	//		///////////////////////////////
	//		///////////////////////////////////
	//		g.setColor(Color.red);
	//		int x1 = entity.getX();
	//		int y1 = entity.getY();
	//		int x2 = (int) cd.getCx();
	//		int y2 = (int) cd.getCy();
	//		NamayangarUtils.drawLine(x1, y1, x2, y2, g, transform);
	//		g.drawArc(transform.xToScreen(x2), transform.yToScreen(y2), 10, 10, 0, 360);
	//
	//		return null;
	//	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(FireBrigade entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Fire;
	}

	FireBrigade selected;

	@Override
	public void objectSelected(SOSSelectedObj sso) {
		dd.clear();
		if (sso != null) {
			if (sso.getObject() instanceof FireBrigade) {
				selected = (FireBrigade) sso.getObject();
				dd.add(selected);
			} else
				dd.clear();
		} else
			dd.clear();
	}

}

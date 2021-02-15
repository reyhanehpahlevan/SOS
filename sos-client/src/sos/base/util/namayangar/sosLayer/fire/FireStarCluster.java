package sos.base.util.namayangar.sosLayer.fire;

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
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.tools.FireStarZone;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.fire_v2.target.SOSFireZoneSelector;

public class FireStarCluster extends SOSAbstractToolsLayer<FireStarZone> {

	public FireStarCluster() {
		super(FireStarZone.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 1;
	}

	@Override
	protected void makeEntities() {
		SOSFireZoneSelector x = ((FireBrigadeAgent) model().sosAgent()).FDK.getInfoModel().getFireZoneSelector();
		setEntities(x.starCluster.getStarZones());
	}

	@Override
	protected Shape render(FireStarZone cd, Graphics2D g, ScreenTransform transform) {

		Color col = new Color(((cd.getIndex() + 1) * 100) % 255, ((cd.getIndex() + 1) * 140) % 255, ((cd.getIndex() + 1) * 30) % 255);
		g.setColor(col);
		for (Building b : cd.getZoneBuildings()) {
			NamayangarUtils.fillShape(b.getShape(), g, transform);
		}
		g.setColor(Color.black);
		NamayangarUtils.drawString("INDEX=" + cd.getIndex(), g, transform, (int) cd.getCx(), (int) cd.getCy());
		return null;
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
	public ArrayList<Pair<String, String>> sosInspect(FireStarZone entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Fire;
	}

}

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
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.UpdateClusterFireState;
import sos.police_v2.state.intrupt.SmallFireGardClearingInterupt;

public class SmallFireGardClearingInteruptLayer extends SOSAbstractToolsLayer<SmallFireGardClearingInterupt> {

	public SmallFireGardClearingInteruptLayer() {
		super(SmallFireGardClearingInterupt.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getZIndex() {
		// TODO Auto-generated method stub
		return 150;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) model().sosAgent()).getInteruptState(SmallFireGardClearingInterupt.class));
	}

	@Override
	protected Shape render(SmallFireGardClearingInterupt entity, Graphics2D g, ScreenTransform transform) {
		Shape shape;
		g.setColor(Color.red);
		for (Building b : entity.gardOfFire) {
			shape = NamayangarUtils.transformShape(b, transform);
			NamayangarUtils.drawString("safe gard", g, transform, b);
			g.draw(shape);
		}
		//		g.setColor(new Color(200, 50, 50));
		g.setColor(new Color(0, 200, 200));
		for (Building b : entity.checkedGard) {
			NamayangarUtils.drawString("done", g, transform, b);
			shape = NamayangarUtils.transformShape(b, transform);
			g.draw(shape);
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
		return model() instanceof PoliceWorldModel && ((PoliceForceAgent) model().sosAgent()).getState(UpdateClusterFireState.class) != null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(SmallFireGardClearingInterupt entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

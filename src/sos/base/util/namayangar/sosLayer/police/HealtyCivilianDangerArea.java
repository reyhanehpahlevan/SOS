package sos.base.util.namayangar.sosLayer.police;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenHealtyCivilianState;

public class HealtyCivilianDangerArea extends SOSAbstractToolsLayer<Shape> {

	public HealtyCivilianDangerArea() {
		super(Shape.class);
	}

	@Override
	public int getZIndex() {
		return 100;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) model().me().getAgent()).getState(OpenHealtyCivilianState.class).dangerZone);
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel;
	}

	@Override
	protected Shape render(Shape entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.white);
		if (entity != null)
			g.draw(NamayangarUtils.transformShape(entity, transform));
		return null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Shape entity) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

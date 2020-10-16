package sos.base.util.namayangar.sosLayer.base;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Civilian;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenCivilianState;

public class ValidCiviliansForOpening extends SOSAbstractToolsLayer<Civilian> {

	public ValidCiviliansForOpening() {
		super(Civilian.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 1100;
	}

	@Override
	public boolean isValid() {
		return (model() instanceof PoliceWorldModel && ((PoliceForceAgent) model().me().getAgent()).getState(OpenCivilianState.class) != null);
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}

	@Override
	protected void makeEntities() {
		ArrayList<Civilian> list = ((PoliceForceAgent) (model().me().getAgent())).getState(OpenCivilianState.class).validCivilians;
		setEntities(list);
	}

	@Override
	protected Shape render(Civilian entity, Graphics2D g, ScreenTransform transform) {
		if (entity != null) {
			g.setColor(Color.yellow);
			g.setColor(Color.cyan);
			NamayangarUtils.drawEntity(entity, g, transform);
		}
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Civilian entity) {
		// TODO Auto-generated method stub
		return null;
	}
}

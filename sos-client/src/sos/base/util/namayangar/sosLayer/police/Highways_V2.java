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
import sos.police_v2.state.OpenHighWaysState2;
import sos.police_v2.state.OpenHighWaysState2.Yal;

public class Highways_V2 extends SOSAbstractToolsLayer<Yal> {

	public Highways_V2() {
		super(Yal.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getZIndex() {
		return 100;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) (model().me().getAgent())).getState(OpenHighWaysState2.class).myYals);
	}

	@Override
	protected Shape render(Yal entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.magenta);
		NamayangarUtils.drawLine(entity.getHead(), entity.getTail(), g, transform);
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		return model()instanceof PoliceWorldModel&& ((PoliceForceAgent) (model().me().getAgent())).getState(OpenHighWaysState2.class)!=null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Yal entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}

}

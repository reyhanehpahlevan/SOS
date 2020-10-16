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
import sos.police_v2.state.OtherAgentStockState;

public class PoliceStockAreaInCluster extends SOSAbstractToolsLayer<Shape> {

	public PoliceStockAreaInCluster() {
		super(null);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 52;
	}

	@Override
	protected void makeEntities() {
		OtherAgentStockState hstate = ((PoliceForceAgent)model().sosAgent()).getState(OtherAgentStockState.class);

		setEntities(hstate.myClusterShape);
	}

	@Override
	protected Shape render(Shape entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.pink);

		Shape transformShape = NamayangarUtils.transformShape(entity, transform);
		g.draw(transformShape);
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel &&((PoliceForceAgent)model().sosAgent()).getState(OtherAgentStockState.class)!=null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Shape entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

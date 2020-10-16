package sos.base.util.namayangar.sosLayer.police;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Road;
import sos.base.move.types.PoliceMove;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.UpdateFireState;

public class PoliceUpdateFireZone extends SOSAbstractToolsLayer<UpdateFireState> {
	public PoliceUpdateFireZone() {
		super(UpdateFireState.class);
	}

	@Override
	public int getZIndex() {
		return 100;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) model().sosAgent()).getState(UpdateFireState.class));
	}

	@Override
	protected Shape render(UpdateFireState updateFireState, Graphics2D g, ScreenTransform transform) {
		if (updateFireState.roadsToClearNearFireSide != null) {
			if (updateFireState.roadsToClearNearFireSide != null) {
				int i = 0;
				for (Road element : updateFireState.roadsToClearNearFireSide) {
					g.setStroke(new BasicStroke(1));
					g.setColor(Color.green);
					NamayangarUtils.drawShape(element.getShape(), g, transform);
					g.setColor(Color.yellow);
					g.setStroke(new BasicStroke(2));
					NamayangarUtils.drawString(i++ + ":" + model().sosAgent().move.getWeightTo(element, element.getX(), element.getY(), PoliceMove.class) + "", g, transform, element.getX(), element.getY() + 1000);
				}
			}
			if (updateFireState.nearestToCenter != null) {
				g.setColor(Color.black);
				NamayangarUtils.fillShape(updateFireState.nearestToCenter.getShape(), g, transform);
			}
			if (updateFireState.fourBuilding != null) {
				g.setColor(Color.white);
				NamayangarUtils.fillShape(updateFireState.fourBuilding[0][0].getShape(), g, transform);
				NamayangarUtils.fillShape(updateFireState.fourBuilding[0][1].getShape(), g, transform);
				NamayangarUtils.fillShape(updateFireState.fourBuilding[1][0].getShape(), g, transform);
				NamayangarUtils.fillShape(updateFireState.fourBuilding[1][1].getShape(), g, transform);
			}

		}
		return null;
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
	public ArrayList<Pair<String, String>> sosInspect(UpdateFireState entity) {
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

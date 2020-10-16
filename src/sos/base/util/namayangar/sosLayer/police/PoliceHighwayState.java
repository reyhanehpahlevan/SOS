package sos.base.util.namayangar.sosLayer.police;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.OpenHighwayesState;

public class PoliceHighwayState extends SOSAbstractToolsLayer<Entry<Area, ArrayList<Area>>> {

	public PoliceHighwayState() {
		super(null);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 52;
	}

	@Override
	protected void makeEntities() {
		OpenHighwayesState hstate = ((PoliceForceAgent)model().sosAgent()).getState(OpenHighwayesState.class);
		
		setEntities(hstate.yalBetweenJunctionPoints.entrySet());
	}

	@Override
	protected Shape render(Entry<Area, ArrayList<Area>> entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.green);

		Shape transformShape = NamayangarUtils.transformShape(entity.getKey(), transform);
		g.fill(transformShape);
		g.setColor(Color.green);
		for (Area area : entity.getValue()) {
			int x1 = transform.xToScreen(area.getX());
			int y1 = transform.yToScreen(area.getY());
			int x2 = transform.xToScreen(entity.getKey().getX());
			int y2 = transform.yToScreen(entity.getKey().getY());
//			transformShape = NamayangarUtils.transformShape(l, transform);
//			g.fill(transformShape);
			g.drawLine(x1, y1, x2, y2);
		}
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel &&((PoliceForceAgent)model().sosAgent()).getState(OpenHighwayesState.class)!=null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Entry<Area, ArrayList<Area>> entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}

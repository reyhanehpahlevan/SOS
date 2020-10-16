package sos.base.util.namayangar.sosLayer.police;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.base.worldModel.PoliceWorldModel;

public class PoliceJunctions extends SOSAbstractToolsLayer<Area> {

	public PoliceJunctions() {
		super(Area.class);
	}

	@Override
	public int getZIndex() {
		return 2;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceWorldModel) model()).getJunctionPoints());
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
	protected Shape render(Area entity, Graphics2D g, ScreenTransform transform) {
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(2));

		Shape transformShape = NamayangarUtils.transformShape(entity, transform);
		g.setColor(Color.green);
		g.fill(transformShape);
		//		g.drawString(entity.getID().toString(), transform.xToScreen(entity.getX()), transform.yToScreen(entity.getY()));
		g.setStroke(new BasicStroke(1));

		return null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(Area entity) {
		return null;
	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
	}

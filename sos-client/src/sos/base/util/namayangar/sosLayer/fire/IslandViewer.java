package sos.base.util.namayangar.sosLayer.fire;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.fire_v2.base.tools.BuildingBlock;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.police_v2.base.worldModel.PoliceWorldModel;

public class IslandViewer extends SOSAbstractToolsLayer<BuildingBlock> {

	Random rand = new Random();

	public IslandViewer() {
		super(BuildingBlock.class);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 16;
	}

	@Override
	protected void makeEntities() {
		if (model() instanceof PoliceWorldModel)
			setEntities(((PoliceWorldModel) model()).islands());
		if (model() instanceof FireWorldModel)
			setEntities(((FireWorldModel) model()).islands());
	}

	@Override
	protected Shape render(BuildingBlock entity, Graphics2D g, ScreenTransform transform) {
		int c = 0;
		if (model() instanceof PoliceWorldModel)
			c = ((PoliceWorldModel) model()).islands().indexOf(entity);
		if (model() instanceof FireWorldModel)
			c = ((FireWorldModel) model()).islands().indexOf(entity);

		g.setColor(new Color(Math.abs(c * 25) % 255, Math.abs(17 * c) % 255, Math.abs(34 * c) % 255));
		Shape shape;
		for (Building b : entity.buildings()) {
			shape = NamayangarUtils.transformShape(b, transform);
//			g.draw(shape);
		}
		ConvexHull_arr_New can = new ConvexHull_arr_New(entity.buildings());
		
		g.draw(NamayangarUtils.transformShape(can.getShape(), transform));
		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel || model() instanceof FireWorldModel;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(BuildingBlock entity) {
		return null;
	}
	@Override
	public LayerType getLayerType() {
		return LayerType.Fire;
	}
}

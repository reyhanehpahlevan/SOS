package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

public class SaySelectedLayer extends SOSAbstractSelectedLayer {

	private int radius;

	public void initialise(Config config) {
		super.initialise(config);
		radius = config.getIntValue("comms.channels.0.range");
	}

	@Override
	public String getName() {
		return "Say Selected Layer";
	}

	public SaySelectedLayer() {
		entities = new ArrayList<Object>();
	}

	protected void paintShape(StandardEntity r, Shape shape, Graphics2D g) {

		g.setColor(Color.white);
		// g.fill(shape);
		// g.setColor(Color.BLACK);
		g.draw(shape);
	}

	@Override
	protected Shape render(Object obj, Graphics2D g, ScreenTransform t) {
		if(!(obj instanceof StandardEntity))
			return null;
		
		StandardEntity entity=world.getEntity(((StandardEntity) obj).getID());
		if(!(entity instanceof Human))
			return null;
		Pair<Integer, Integer> location = entity.getLocation(world);
		if (location == null) {
			return null;
		}
		int x = t.xToScreen(location.first());
		int y = t.yToScreen(location.second());

		double agentX = x;
		double agentY = y;
		double ellipseX1 = agentX - radius;
		double ellipseY1 = agentY + radius;
		double ellipseX2 = agentX + radius;
		double ellipseY2 = agentY - radius;

		int x1 = t.xToScreen(ellipseX1);
		int y1 = t.yToScreen(ellipseY1);
		int x2 = t.xToScreen(ellipseX2);
		int y2 = t.yToScreen(ellipseY2);
		int ellipseWidth = x2 - x1;
		int ellipseHeight = y2 - y1;

		Shape shape = new Ellipse2D.Double(x - ellipseWidth / 2, y - ellipseHeight / 2, ellipseWidth, ellipseHeight);
		paintShape(entity, shape, g);
		return null;
	}

}

package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collection;

import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;

public class AllHumanSayLayer extends SOSAbstractToolsLayer<StandardEntity> {

	
	
	private int radius;

	public void initialise(Config config) {
		super.initialise(config);
		radius = config.getIntValue("comms.channels.0.range");
	}

	@Override
	public String getName() {
		return "AllHumanSayLayer";
	}

	public AllHumanSayLayer() {
		entities = new ArrayList<StandardEntity>();
		setVisible(false);
	}

	protected void paintShape(StandardEntity r, Shape shape, Graphics2D g) {

		g.setColor(Color.white);
		// g.fill(shape);
		// g.setColor(Color.BLACK);
		g.draw(shape);
	}

	@Override
	protected Shape render(StandardEntity entity, Graphics2D g, ScreenTransform t) {
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

	@Override
	protected void paintShape(StandardEntity r, Polygon shape, Graphics2D g) {
		// TODO Auto-generated method stub

	}
	
	@Override
	protected void updateEntities() {
		if(timeStep==null)
			return;
		entities.clear();
		for (StandardEntity e : world.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_FORCE, StandardEntityURN.POLICE_OFFICE)) {
			Collection<Command> commands = timeStep.getCommands(e.getID());
			for (Command command : commands) {
				if(command instanceof AKSpeak){
					if(((AKSpeak) command).getChannel()==0)
						entities.add(e);
				}
			}
			
		}
	}
}

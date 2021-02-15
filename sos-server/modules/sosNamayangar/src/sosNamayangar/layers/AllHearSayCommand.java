package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Collection;

import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import sosNamayangar.NamayangarUtils;

public class AllHearSayCommand extends SOSAbstractToolsLayer<Pair<StandardEntity,AKSpeak>> {
	public AllHearSayCommand() {
		setVisible(false);
	}

	@Override
	protected Shape render(Pair<StandardEntity,AKSpeak>pair, Graphics2D g, ScreenTransform transform) {
		Shape shape = NamayangarUtils.transformEntity(pair.first(), transform);
		g.setColor(Color.orange);
		if(shape==null)
			System.out.println("why transformed shape is null?????");
		else{
			g.fill(shape);
			int x = transform.xToScreen(pair.first().getLocation(world).first());
			int y = transform.yToScreen(pair.first().getLocation(world).second());
			g.setColor(Color.BLUE);
			g.drawString("m:"+pair.second().getAgentID(), x, y);
		}
		return null;
	}

	@Override
	protected void updateEntities() {
		if (timeStep == null)
			return;
		entities.clear();
		for (StandardEntity e : world.getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.FIRE_STATION, StandardEntityURN.POLICE_FORCE, StandardEntityURN.POLICE_OFFICE)) {
			
			Collection<Command> commands = timeStep.getAgentHearing(e.getID());
			if (commands != null){
				for (Command command : commands) {
					if (command instanceof AKSpeak) {
						if (((AKSpeak) command).getChannel() == 0){
							entities.add(new Pair<StandardEntity, AKSpeak>(e, (AKSpeak) command));
						}
					}
				}
			}
		}
	}

	@Override
	protected void paintShape(Pair<StandardEntity, AKSpeak> r, Polygon shape,
			Graphics2D g) {
		// TODO Auto-generated method stub
		
	}

}

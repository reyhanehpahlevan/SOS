package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import sosNamayangar.estimators.SimpleDeathTime;

/**
 *
 * Date : Friday April 5th 2013.
 * @author sinash
 *
 */

public class HumanDeathTimeLayer extends SOSAbstractToolsLayer<StandardEntity> {
	public void initialise(Config config) {
		super.initialise(config);
	}

	@Override
	public String getName() {
		return "HumanDeathTime Layer";
	}

	public HumanDeathTimeLayer() {
		entities = new ArrayList<StandardEntity>();
		setVisible(false);
	}

	protected void paintShape(StandardEntity r, Shape shape, Graphics2D g) {

		
	}

	@Override
	protected Shape render(StandardEntity entity, Graphics2D g, ScreenTransform t) {
		Pair<Integer, Integer> location = entity.getLocation(world);
		if (location == null) {
			return null;
		}
		int x = t.xToScreen(location.first());
		int y = t.yToScreen(location.second());

		g.setColor(Color.yellow);
		// g.fill(shape);
		// g.setColor(Color.BLACK);
		String str = "";
			Human hu = (Human) entity;
			str += SimpleDeathTime.getEasyLifeTime(hu.getHP(), hu.getDamage(), timeStep.getTime());
		
		g.drawString(str,x,y );
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
		for (StandardEntity e : world.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE)) {
			Human hu = (Human)(e);
			if(hu.isDamageDefined() && hu.isHPDefined()){
				if(hu.getHP()>0 && hu.getDamage()>0){
					entities.add(e);
				}
			}
			}
			
		}
	}

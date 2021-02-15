package sosNamayangar.layers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Collection;
import java.util.HashMap;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.NamayangarUtils;

public class SenseCivilian extends SOSAbstractToolsLayer<EntityID> {

	HashMap<EntityID, Shape> civ_visibleShape=null;
	HashMap<EntityID, Integer> civ_senseTime=new HashMap<EntityID, Integer>();
	@Override
	protected Shape render(EntityID entityID, Graphics2D g, ScreenTransform transform) {
		StandardEntity entity = world.getEntity(entityID);
		
		if(((Civilian)entity).getPosition(world)instanceof Road)
			return null;
		if(((Civilian)entity).getPosition(world)instanceof Refuge)
			return null;
		if(/*civ_senseTime.get(entity)!=null&&*/timeStep.getTime()<=civ_senseTime.get(entityID))
			return null;
		
		g.setStroke(new BasicStroke(2));
		g.setColor(getColor(((Civilian)entity).getHP()));
		g.draw(NamayangarUtils.transformEntity(entity, transform));
		g.setStroke(new BasicStroke(1));
		return null;
	}
	
	private Color getColor(int hp) {
		if(hp>7000)
			return Color.green.darker();
		if(hp>4000)
			return Color.green.brighter();
		if(hp>2000)
			return Color.yellow;
		if(hp>1000)
			return Color.orange;
		if(hp>0)
			return Color.red;
		return Color.black;
	}

	@Override
	protected void updateEntities() {
		if (timeStep == null||timeStep.getTime()<2)
			return;
		
		if(timeStep.getTime()==2&&civ_visibleShape==null){
			civ_visibleShape=new HashMap<EntityID, Shape>();
			for (StandardEntity entity : world.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
				Collection<StandardEntity> nearby = world.getObjectsInRange((int)entity.getLocation(world).first(), (int)entity.getLocation(world).second(), (int) LineOfSight.viewDistance);	
				Shape shape =LineOfSight.findVisibleShape(entity, new Point2D(entity.getLocation(world).first(),entity.getLocation(world).second()), nearby);	
				civ_visibleShape.put(entity.getID(),shape);
			}
		}
		
		for (StandardEntity entity : world.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if(civ_visibleShape==null||civ_visibleShape.get(entity.getID())==null)
				continue;
			if(entities.contains(entity.getID()))
				continue;
			if(((Civilian)entity).getPosition(world)instanceof Road)
				continue;
			if(((Civilian)entity).getPosition(world)instanceof Refuge)
				continue;
			Point2D p1 = new Point2D(((Human)entity).getX(),((Human)entity).getY());
			for (StandardEntity standardEntity : world.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.FIRE_BRIGADE,StandardEntityURN.POLICE_FORCE)) {
				Point2D p2 = new Point2D(((Human)standardEntity).getX(),((Human)standardEntity).getY());
				if(GeometryTools2D.getDistance(p1, p2) >LineOfSight.viewDistance)
					continue;
				
				int x = ((Human)standardEntity).getX();
				int y = ((Human)standardEntity).getY();
				if(civ_visibleShape.get(entity.getID()).contains(x,y)){
					civ_senseTime.put(entity.getID(), timeStep.getTime()+0);
					entities.add(entity.getID());
				}
			}
		}
		super.updateEntities();
	}

	@Override
	protected void paintShape(EntityID entityID, Polygon shape, Graphics2D g) {
	}

}

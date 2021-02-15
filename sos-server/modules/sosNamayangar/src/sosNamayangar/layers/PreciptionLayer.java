package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.NamayangarUtils;

public class PreciptionLayer extends SOSAbstractSelectedLayer {

	@Override
	protected Shape render(Object obj, Graphics2D g, ScreenTransform transform) {
		if(!(obj instanceof StandardEntity))
			return null;
		StandardEntity entity=(StandardEntity) obj;
		if (timeStep == null)
			return null;
		ChangeSet preciption = timeStep.getAgentPerception(entity.getID());
		if (preciption != null) {
			for (EntityID changed : preciption.getChangedEntities()) {
				StandardEntity changedEntity = world.getEntity(changed);
				Shape shape = NamayangarUtils.transformEntity(changedEntity, transform);
				g.setColor(Color.magenta);
				if (shape != null) {
					if (changedEntity instanceof Human)
						g.fill(shape);
					else
						g.draw(shape);
				}
			}
		}
		return null;
	}


}

package sosNamayangar.layers;

 import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.StandardEntity;
import sosNamayangar.NamayangarUtils;

public class ShowSelectedLayer extends SOSAbstractSelectedLayer {

	@Override
	protected Shape render(Object obj, Graphics2D g, ScreenTransform transform) {
		if(!(obj instanceof StandardEntity))
			return null;
		
		StandardEntity entity=world.getEntity(((StandardEntity) obj).getID());
		g.setStroke(new BasicStroke(3));
		g.setColor(Color.yellow);
		g.draw(NamayangarUtils.transformEntity(entity, transform));
		g.setStroke(new BasicStroke(1));
		return null;
		
	}

	
}

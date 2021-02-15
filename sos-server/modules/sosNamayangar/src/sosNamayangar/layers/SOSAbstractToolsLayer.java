package sosNamayangar.layers;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;

import rescuecore2.Timestep;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

public abstract class SOSAbstractToolsLayer<T> extends StandardViewLayer {

	protected Collection<T> entities = new ArrayList<T>();
	protected Timestep timeStep;

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {
		updateEntities();
		synchronized (entities) {
			Collection<RenderedObject> result = new ArrayList<RenderedObject>();
			for (T next : entities) {
				RenderedObject r = new RenderedObject(next, render(next, g, transform));
				result.add(r);
			}
			return result;
		}
	}

	protected void updateEntities() {
	}

	protected abstract Shape render(T entity, Graphics2D g, ScreenTransform transform);

	protected void paintShape(T r, Polygon shape, Graphics2D g){
		
	}

	@Override
	public Rectangle2D view(Object... objects) {
		if (objects != null) {
			for (Object object : objects) {
				if (object instanceof Timestep)
					timeStep = (Timestep) object;
			}
		}
		return super.view(objects);

	}
}

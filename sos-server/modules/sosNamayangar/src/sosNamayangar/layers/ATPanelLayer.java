package sosNamayangar.layers;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;

public class ATPanelLayer extends SOSAbstractSelectedLayer {

	public ATPanelLayer() {
	}

	public Component getPanel() {
		return null;
	}

	@Override
	protected Shape render(Object obj, Graphics2D g, ScreenTransform transform) {
		if(!(obj instanceof Human))
			return null;
		Human human=(Human) obj;
		return null;
	}

}

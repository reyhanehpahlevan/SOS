package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.misc.gui.ShapeDebugFrame;
import sosNamayangar.NamayangarUtils;

public class CustomLayer extends SOSAbstractToolsLayer<Shape> {

	public CustomLayer(JPanel m_commandBox) {
		setVisible(true);
		JPanel jp = new JPanel(new GridLayout(1, 3));
		final JTextField x_text = new JTextField("1000");
		final JTextField y_text = new JTextField("1000");
		JButton show = new JButton("show");
		jp.add(x_text);
		jp.add(y_text);
		jp.add(show);
		show.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				entities.add(new PointShape(Integer.parseInt(x_text.getText()), Integer.parseInt(y_text.getText())));
				view();
			}
		});
		show.doClick();
		m_commandBox.add(jp);
	}

	@Override
	protected Shape render(Shape entity, Graphics2D g, ScreenTransform transform) {
		if (entity instanceof PointShape)
			NamayangarUtils.paintPoint2D(((PointShape) entity).x, ((PointShape) entity).y, transform, g);
		g.draw(NamayangarUtils.transformShape(entity, transform));
		return null;
	}

	private static class PointShape extends Area {

		public boolean square = false;
		private final int x;
		private final int y;

		public PointShape(int x, int y) {
			this.x = x;
			this.y = y;
		}

	}
}

package firesimulator;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.PanZoomListener;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

import firesimulator.sosTool.AliTool;
import firesimulator.world.Building;
import firesimulator.world.World;

public class FireSimulatorGui extends JPanel {
	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;
	private static final Color	HEATING				= new Color(176, 176, 56, 128);
	private static final Color	BURNING				= new Color(204, 122, 50, 128);
	private static final Color	INFERNO				= new Color(160, 52, 52, 128);

	private World				world;
	private ScreenTransform		transform;
	private StandardWorldModel	model;
	WorldView					view;

	public FireSimulatorGui() {
		super(new BorderLayout());
		view = new WorldView();
		this.add(view, BorderLayout.CENTER);
	}

	public void initialize(World world, StandardWorldModel model) {
		this.world = world;
		this.model = model;
		view.initialize();
	}

	private class WorldView extends JComponent {
		HashMap<Shape, Building>	shape_building	= new HashMap<Shape, Building>();
		Building					selectedBuilding;

		public void initialize() {
			Rectangle2D bounds = null;
			for (Building area : world.getBuildings()) {
				Rectangle2D r = area.getPolygon().getBounds2D();
				if (bounds == null) {
					bounds = new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
				} else {
					Rectangle2D.union(bounds, r, bounds);
				}
			}
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					Point p = e.getPoint();
					selectedBuilding = null;
					for (Map.Entry<Shape, Building> next : shape_building.entrySet()) {
						if (next.getKey().contains(p)) {
							selectedBuilding = next.getValue();
						}
					}
					repaint();
				}
			});
			transform = new ScreenTransform(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
			new PanZoomListener(this).setScreenTransform(transform);

		}

		@Override
		public void paintComponent(Graphics g) {
			int width = getWidth();
			int height = getHeight();
			Insets insets = getInsets();
			width -= insets.left + insets.right;
			height -= insets.top + insets.bottom;
			transform.rescale(width, height);
			Graphics2D copy = (Graphics2D) g.create(insets.left, insets.top, width, height);
			drawObjects(copy);

		}

		private void drawObjects(Graphics2D g) {
			drawBuildings((Graphics2D) g.create());
			drawCells((Graphics2D) g.create());
			drawSelected((Graphics2D) g.create());
			// addCoonectedValue((Graphics2D) g.create());
			drawCellTemprature((Graphics2D) g.create());
			drawAverageBuildingCellsTemprature((Graphics2D) g.create());
		}

		private void drawAverageBuildingCellsTemprature(Graphics2D g) {
			g.setColor(Color.blue);
			for(Building b:world.getBuildings()){
			    g.drawString((int)b.getTemperature()+"+"+b.getAverageCellTemprature(world), transform.xToScreen(b.getX()), transform.yToScreen(b.getY()));
			}
		}

		private void drawCellTemprature(Graphics2D g) {
			for (int x = 0; x < world.getAirTemp().length; x++)
				for (int y = 0; y < world.getAirTemp()[0].length; y++) {
				    int xv=x*world.SAMPLE_SIZE+world.getMinX();
	                int yv=y*world.SAMPLE_SIZE+world.getMinY();
	                int temp=(int) world.getAirCellTemp(x, y);
	                g.drawString(""+temp, transform.xToScreen(xv+2500), transform.yToScreen(yv+2500));

				}
		}

		private void drawSelected(Graphics2D g) {
			if (selectedBuilding == null)
				return;
			System.out.println("" + selectedBuilding.getID());
			List<Edge> edges = getEdges(selectedBuilding.getApexes());
			Edge e = edges.get(0);
			Path2D shape = new Path2D.Double();
			shape.moveTo(transform.xToScreen(e.getStartX()), transform.yToScreen(e.getStartY()));
			for (Edge edge : edges) {
				shape.lineTo(transform.xToScreen(edge.getEndX()), transform.yToScreen(edge.getEndY()));
			}

			g.setColor(Color.white);
			g.setStroke(new BasicStroke(2));
			g.draw(shape);
			g.setStroke(new BasicStroke(1));
			addCoonectedValue(g);
		}

		private void addCoonectedValue(Graphics2D g) {
			// for (Building b : world.getBuildings()) {
			// if (b.getFieryness() > 0) {
			Building b = selectedBuilding;
			Building[] bs = b.connectedBuilding;
			float[] vs = b.connectedValues;
			Shape expands = AliTool.createBlockages(getEdges(b.getApexes()));
			java.awt.Rectangle rectangle = expands.getBounds();
			Collection<StandardEntity> buildings = model.getObjectsInRectangle((int) rectangle.getMinX(), (int) rectangle.getMinY(), (int) rectangle.getMaxX(), (int) rectangle.getMaxY());
			Area area = new Area(expands);
			for (StandardEntity standardEntity : buildings) {
				if (standardEntity instanceof rescuecore2.standard.entities.Building)
					area.subtract(new Area(((rescuecore2.standard.entities.Area) standardEntity).getShape()));
			}
			List<Edge> edges = getEdges(AliTool.getApexes(expands));
			Edge e = edges.get(0);
			Path2D shape = new Path2D.Double();
			shape.moveTo(transform.xToScreen(e.getStartX()), transform.yToScreen(e.getStartY()));
			for (Edge edge : edges) {
				shape.lineTo(transform.xToScreen(edge.getEndX()), transform.yToScreen(edge.getEndY()));
			}
			// shape.
			g.setColor(Color.red);
			g.draw(shape);
			// g.drawArc(transform.xToScreen(b.getX()),
			// transform.yToScreen(b.getY()), width, height, startAngle,
			// arcAngle)
			for (int c = 0; c < vs.length; c++) {
				int x = transform.xToScreen(bs[c].getX());
				int y = transform.yToScreen(bs[c].getY());
				g.drawString("CV:" + vs[c], x, y);

			}
		}

		// public SOSArea expandArea(SOSArea sosArea){
		// return ExpandBlockade.expandBlock(sosArea, 50000);
		// }

		private void drawCells(Graphics2D g) {
			int ymin = transform.yToScreen(world.getMinY());
			int xmin = transform.xToScreen(world.getMinX());
			int ymax = transform.yToScreen(world.getMaxY());
			int xmax = transform.xToScreen(world.getMaxX());
			for (int x = 0; x < world.getAirTemp().length; x++) {
				int xv = x * world.SAMPLE_SIZE + world.getMinX();
				// int yv=y*world.SAMPLE_SIZE+world.getMinY();

				int xs = transform.xToScreen(xv);

				g.setColor(Color.green);
				g.drawLine(xs, ymin, xs, ymax);

			}
			for (int y = 0; y < world.getAirTemp()[0].length; y++) {
				int yv = y * world.SAMPLE_SIZE + world.getMinY();

				int ys = transform.yToScreen(yv);

				g.setColor(Color.green);
				g.drawLine(xmin, ys, xmax, ys);

			}
		}

		private void drawBuildings(Graphics2D g) {

			for (Building b : world.getBuildings()) {
				List<Edge> edges = getEdges(b.getApexes());
				Edge e = edges.get(0);
				Path2D shape = new Path2D.Double();
				shape.moveTo(transform.xToScreen(e.getStartX()), transform.yToScreen(e.getStartY()));
				for (Edge edge : edges) {
					shape.lineTo(transform.xToScreen(edge.getEndX()), transform.yToScreen(edge.getEndY()));
				}

				switch (b.getFieryness()) {
				case 1:
					g.setColor(HEATING);
					break;
				case 2:
					g.setColor(BURNING);
					break;
				case 3:
					g.setColor(INFERNO);
					break;

				default:
					g.setColor(new Color(135, 135, 135));
					break;
				}

				g.fill(shape);
				g.setColor(Color.black);
				g.draw(shape);
				shape_building.put(shape, b);
			}

		}
	}

	public static List<Edge> getEdges(int[] allApexes) {
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (int i = 0; i < allApexes.length; i += 2) {
			edges.add(new Edge(new Point2D(allApexes[i], allApexes[i + 1]),
					new Point2D(allApexes[(i + 2) % allApexes.length], allApexes[((i + 3)) % allApexes.length])));
		}
		return edges;
	}
}

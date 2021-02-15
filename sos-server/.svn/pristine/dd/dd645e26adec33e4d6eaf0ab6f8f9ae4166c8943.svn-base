package firesimulator.sosTool;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;

public class AliTool {

	public static int[] getApexes(Shape area) {

		List<Integer> apexes = new ArrayList<Integer>();
		// CHECKSTYLE:OFF:MagicNumber
		PathIterator it = area.getPathIterator(null, 100);
		double[] d = new double[6];
		int moveX = 0;
		int moveY = 0;
		int lastX = 0;
		int lastY = 0;
		boolean finished = false;
		while (/* !finished && */!it.isDone()) {
			int x = -1;
			int y = -1;
			switch (it.currentSegment(d)) {
			case PathIterator.SEG_MOVETO:
				x = (int) d[0];
				y = (int) d[1];
				moveX = x;
				moveY = y;
				// Logger.debug("Move to " + x + ", " + y);
				break;
			case PathIterator.SEG_LINETO:
				x = (int) d[0];
				y = (int) d[1];
				// Logger.debug("Line to " + x + ", " + y);
				if (x == moveX && y == moveY) {
					// finished = true;
				}
				break;
			case PathIterator.SEG_QUADTO:
				x = (int) d[2];
				y = (int) d[3];
				// Logger.debug("Quad to " + x + ", " + y);
				if (x == moveX && y == moveY) {
					// finished = true;
				}
				break;
			case PathIterator.SEG_CUBICTO:
				x = (int) d[4];
				y = (int) d[5];
				// Logger.debug("Cubic to " + x + ", " + y);
				if (x == moveX && y == moveY) {
					// finished = true;
				}
				break;
			case PathIterator.SEG_CLOSE:
				// Logger.debug("Close");
				finished = true;
				break;
			default:
				throw new RuntimeException("Unexpected result from PathIterator.currentSegment: " + it.currentSegment(d));
			}
			// Logger.debug(x + ", " + y);
			if (!finished && (x != lastX || y != lastY) && (x != -1 && y != -1)) {
				apexes.add(x);
				apexes.add(y);
			}
			lastX = x;
			lastY = y;
			it.next();
		}
		// CHECKSTYLE:ON:MagicNumber
		int[] result = new int[apexes.size()];
		int i = 0;
		for (Integer next : apexes) {
			result[i++] = next;
		}
		return result;
	}

	public static Shape createBlockages(List<Edge> list) {
		double d = 50000;
		// Place some blockages on surrounding roads
		List<java.awt.geom.Area> wallAreas = new ArrayList<java.awt.geom.Area>();
		// Project each wall out and build a list of wall areas
		for (Edge edge : list) {
			projectWall(edge, wallAreas, d);
		}
		java.awt.geom.Area fullArea = new java.awt.geom.Area();
		for (java.awt.geom.Area wallArea : wallAreas) {
			fullArea.add(wallArea);
		}
		return fullArea;
		/*
		 * new ShapeDebugFrame().show("Collapsed building", new
		 * ShapeDebugFrame.AWTShapeInfo(b.getShape(), "Original building area",
		 * Color.RED, true), new ShapeDebugFrame.AWTShapeInfo(fullArea,
		 * "Expanded building area (d = " + d + ")", Color.BLACK, false) );
		 */
	}

	private static void projectWall(Edge edge, Collection<java.awt.geom.Area> areaList, double d) {
		Line2D wallLine = new Line2D(edge.getStartX(), edge.getStartY(), edge.getEndX() - edge.getStartX(), edge.getEndY() - edge.getStartY());
		Vector2D wallDirection = wallLine.getDirection();
		Vector2D offset = wallDirection.getNormal().normalised().scale(-d);
		Path2D path = new Path2D.Double();
		Point2D first = wallLine.getOrigin();
		Point2D second = wallLine.getEndPoint();
		Point2D third = second.plus(offset);
		Point2D fourth = first.plus(offset);
		path.moveTo(first.getX(), first.getY());
		path.lineTo(second.getX(), second.getY());
		path.lineTo(third.getX(), third.getY());
		path.lineTo(fourth.getX(), fourth.getY());
		java.awt.geom.Area wallArea = new java.awt.geom.Area(path);
		areaList.add(wallArea);
		// Also add circles at each corner
		double radius = offset.getLength();
		Ellipse2D ellipse1 = new Ellipse2D.Double(first.getX() - radius, first.getY() - radius, radius * 2, radius * 2);
		Ellipse2D ellipse2 = new Ellipse2D.Double(second.getX() - radius, second.getY() - radius, radius * 2, radius * 2);
		areaList.add(new java.awt.geom.Area(ellipse1));
		areaList.add(new java.awt.geom.Area(ellipse2));
		// Logger.info("Edge from " + wallLine + " expanded to " + first + ", "
		// + second + ", " + third + ", " + fourth);
		// debug.show("Collapsed building",
		// new ShapeDebugFrame.AWTShapeInfo(buildingArea,
		// "Original building area", Color.RED, true),
		// new ShapeDebugFrame.Line2DShapeInfo(wallLine, "Wall edge",
		// Color.WHITE, true, true),
		// new ShapeDebugFrame.AWTShapeInfo(wallArea, "Wall area (d = " + d +
		// ")", Color.GREEN, false),
		// new ShapeDebugFrame.AWTShapeInfo(ellipse1, "Ellipse 1", Color.BLUE,
		// false),
		// new ShapeDebugFrame.AWTShapeInfo(ellipse2, "Ellipse 2", Color.ORANGE,
		// false)
		// );
	}

}

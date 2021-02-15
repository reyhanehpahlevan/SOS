package sosNamayangar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Icon;

import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

public class NamayangarUtils {

	/**
	 * @param shape
	 * @param t
	 * @return
	 */
	public static Shape transformShape(Shape shape, ScreenTransform t) {
		int[] apexes = getApexes(shape);
		int count = apexes.length / 2;
		int[] xs = new int[count];
		int[] ys = new int[count];
		for (int i = 0; i < count; ++i) {
			xs[i] = t.xToScreen(apexes[i * 2]);
			ys[i] = t.yToScreen(apexes[(i * 2) + 1]);
		}
		return new Polygon(xs, ys, count);

	}

	public static void paintPoint2D(Point2D p, ScreenTransform transform, Graphics2D g) {
		paintPoint2D(p.getX(), p.getY(), transform, g);
	}

	public static void paintPoint2D(double x, double y, ScreenTransform transform, Graphics2D g) {
		int SIZE = 3;
		int x1 = transform.xToScreen(x);
		int y1 = transform.yToScreen(y);
		g.drawLine(x1 - SIZE, y1 - SIZE, x1 + SIZE, y1 + SIZE);
		g.drawLine(x1 - SIZE, y1 + SIZE, x1 + SIZE, y1 - SIZE);
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages. <b>if it is extended from inheritedtype</b>
	 * 
	 * @param <T>
	 * @param packageName
	 *            The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<Class<? extends T>> getClasses(String packageName, Class<T> inheritedType) {
		ArrayList<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		try {
			classes = getClasses(packageName);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Class<?> next : classes) {
			Class<?> superClass = next.getSuperclass();
			while (!superClass.equals(Object.class)) {
				if (superClass.equals(inheritedType))
					result.add((Class<? extends T>) next);
				superClass = superClass.getSuperclass();
			}
		}
		return result;
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 * 
	 * @param packageName
	 *            The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static ArrayList<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}

		return classes;
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 * 
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	public static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}
		}
		return classes;
	}

	public static Shape transformEntity(StandardEntity entity, rescuecore2.misc.gui.ScreenTransform transform) {
		if (entity instanceof Area) {
			return transformShape(((Area) entity).getShape(), transform);
		} else if (entity instanceof Blockade) {
			return transformShape(((Blockade) entity).getShape(), transform);
		} else if (entity instanceof Human) {
			//		return	transformHumanActualSize((Human) entity, transform);
			return transformHuman((Human) entity, transform);
		} else {
			System.err.println("Unknown Type:"+entity);
			return null;
		}
	}

	private static Shape transformHuman(Human h, ScreenTransform t) {
		if(!h.isXDefined()||!h.isYDefined())
			return new Polygon();
		int SIZE = 10;
		int x = t.xToScreen(h.getX());
		int y = t.yToScreen(h.getY());
		Shape shape;
		shape = new Ellipse2D.Double(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);

		return shape;
	}

	private static Shape transformHumanActualSize(Human h, ScreenTransform t) {
		int RESCUE_AGENT_RADIUS = 500;
		int CIVILIAN_RADIUS = 200;
		Shape shape = null;
		Pair<Integer, Integer> location = null;
		try {
			location = h.getLocation(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (location == null) {
			return null;
		}
		int x = t.xToScreen(location.first());
		int y = t.yToScreen(location.second());
		int radius;
		if (h instanceof Civilian) {
			radius = CIVILIAN_RADIUS;
		} else {
			radius = RESCUE_AGENT_RADIUS;
		}
		double agentX = x;
		double agentY = y;
		double ellipseX1 = agentX - radius;
		double ellipseY1 = agentY + radius;
		double ellipseX2 = agentX + radius;
		double ellipseY2 = agentY - radius;

		int x1 = t.xToScreen(ellipseX1);
		int y1 = t.yToScreen(ellipseY1);
		int x2 = t.xToScreen(ellipseX2);
		int y2 = t.yToScreen(ellipseY2);
		int ellipseWidth = x2 - x1;
		int ellipseHeight = y2 - y1;

		shape = new Ellipse2D.Double(x - ellipseWidth / 2, y - ellipseHeight / 2, ellipseWidth, ellipseHeight);
		return shape;

	}

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

}

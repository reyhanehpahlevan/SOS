package sosNamayangar.layers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import sosNamayangar.NamayangarUtils;

public class LineOfSight extends SOSAbstractSelectedLayer{
	private static final IntersectionSorter INTERSECTION_SORTER = new IntersectionSorter();
	public static double viewDistance;
	public void initialise(Config config) {
		super.initialise(config);
		viewDistance = config.getIntValue("perception.los.max-distance");
	}
	@Override
	protected Shape render(Object obj, Graphics2D g, ScreenTransform transform) {
		if(!(obj instanceof StandardEntity))
			return null;
		StandardEntity entity=world.getEntity(((StandardEntity) obj).getID());
		Collection<StandardEntity> nearby = world.getObjectsInRange((int)entity.getLocation(world).first(), (int)entity.getLocation(world).second(), (int)viewDistance);
		Shape shape = findVisibleShape(entity, new Point2D(entity.getLocation(world).first(),entity.getLocation(world).second()), nearby);
		shape=NamayangarUtils.transformShape(shape, transform);
		g.setColor(Color.magenta);
		g.draw(shape);
		return null;
	}
	public static Shape findVisibleShape(StandardEntity agentEntity, Point2D location, Collection<StandardEntity> nearby) {
		Collection<LineInfo> lines = getAllLines(nearby);
		int rayCount = 72;
		double dAngle = Math.PI * 2 / rayCount;
		// CHECKSTYLE:ON:MagicNumber
		int[] xs = new int[rayCount];
		int[] ys = new int[rayCount];
		for (int i = 0; i < rayCount; ++i) {
			double angle = i * dAngle;
			Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(viewDistance);
			Ray ray = new Ray(new Line2D(location, vector), lines);

			Point2D p = ray.getRay().getPoint(ray.getVisibleLength());
			xs[i] = (int) p.getX();
			ys[i] = (int) p.getY();

		}

		return new Polygon(xs, ys, rayCount);
	}
	private static Collection<LineInfo> getAllLines(Collection<StandardEntity> entities) {
		Collection<LineInfo> result = new HashSet<LineInfo>();
		for (StandardEntity next : entities) {
			if (next instanceof Building) {
				for (Edge edge : ((Building) next).getEdges()) {
					Line2D line = edge.getLine();
					result.add(new LineInfo(line, next, !edge.isPassable()));
				}
			}
			if (next instanceof Road) {
				for (Edge edge : ((Road) next).getEdges()) {
					Line2D line = edge.getLine();
					result.add(new LineInfo(line, next, false));
				}
			} else if (next instanceof Blockade) {
				int[] apexes = ((Blockade) next).getApexes();
				List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
				List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
				for (Line2D line : lines) {
					result.add(new LineInfo(line, next, false));
				}
			} else {
				continue;
			}
		}
		return result;
	}

	private static class LineInfo {
		private Line2D line;
		private StandardEntity entity;
		private boolean blocking;

		public LineInfo(Line2D line, StandardEntity entity, boolean blocking) {
			this.line = line;
			this.entity = entity;
			this.blocking = blocking;
		}

		public Line2D getLine() {
			return line;
		}

		public StandardEntity getEntity() {
			return entity;
		}

		public boolean isBlocking() {
			return blocking;
		}
	}

	private static class IntersectionSorter implements Comparator<Pair<LineInfo, Double>>, java.io.Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Pair<LineInfo, Double> a, Pair<LineInfo, Double> b) {
			double d1 = a.second();
			double d2 = b.second();
			if (d1 < d2) {
				return -1;
			}
			if (d1 > d2) {
				return 1;
			}
			return 0;
		}
	}
	public static class Ray {
		/** The ray itself. */
		private Line2D ray;
		/** The visible length of the ray. */
		private double length;
		/** List of lines hit in order. */
		private List<LineInfo> hit;

		public Ray(Line2D ray, Collection<LineInfo> otherLines) {
			this.ray = ray;
			List<Pair<LineInfo, Double>> intersections = new ArrayList<Pair<LineInfo, Double>>();
			// Find intersections with other lines
			for (LineInfo other : otherLines) {
				double d1 = ray.getIntersection(other.getLine());
				double d2 = other.getLine().getIntersection(ray);
				if (d2 >= 0 && d2 <= 1 && d1 > 0 && d1 <= 1) {
					intersections.add(new Pair<LineInfo, Double>(other, d1));
				}
			}
			Collections.sort(intersections, INTERSECTION_SORTER);
			hit = new ArrayList<LineInfo>();
			length = 1;
			for (Pair<LineInfo, Double> next : intersections) {
				LineInfo l = next.first();
				hit.add(l);
				if (l.isBlocking()) {
					length = next.second();
					break;
				}
			}
		}

		public Line2D getRay() {
			return ray;
		}

		public double getVisibleLength() {
			return length;
		}

		public List<LineInfo> getLinesHit() {
			return Collections.unmodifiableList(hit);
		}
	}

}

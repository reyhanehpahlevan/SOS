package sos.base.util.namayangar.sosLayer.selectedLayer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.search_v2.agentSearch.PoliceSearch;
import sos.search_v2.tools.SearchUtils;
import sos.tools.Utils;

public class StarClusters extends SOSAbstractToolsLayer<StarZone[]> {

	public static int STAR_TALES = 8;
	public static final int GHATHER_TIME = 80;
	public static final int STAR_SUB_ZONES = 4;
	private StarZone[] starZones;
	private Road gatheringArea;

	public StarClusters() {
		super(null);
		setVisible(false);
	}

	@Override
	public int getZIndex() {
		return 5;
	}

	@Override
	protected void makeEntities() {
		createStarZones();
		setEntities(starZones);

	}

	private PoliceSearch<?> search() {
		return ((PoliceSearch<?>) model().sosAgent().newSearch);
	}

	@Override
	public JComponent getGUIComponent() {
		return null;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	protected Shape render(StarZone[] entity, Graphics2D g, ScreenTransform transform) {
		
		int c = 0;
		for (StarZone starZone : entity) {
			for (ArrayList<Building> arrayList : starZone.getSubZones()) {
				g.setColor(new Color(Math.abs(c * 25) % 255, Math.abs(17 * c) % 255, Math.abs(34 * c) % 255));
				for (Building r : arrayList) {
					Shape shape = NamayangarUtils.transformShape(r, transform);
					g.fill(shape);
				}
				c += 5;
			}
		}

		c = 230;
		g.setColor(new Color(Math.abs(c * 25) % 255, Math.abs(17 * c) % 255, Math.abs(34 * c) % 255));
		Shape shape = NamayangarUtils.transformShape(gatheringArea, transform);
		g.fill(shape);

		for (StarZone starZone : entity) {
			g.setColor(new Color(254, 254, 254));
			shape = NamayangarUtils.transformShape(starZone.getGatheringRoad(), transform);
			g.fill(shape);
		}
		return null;	
	}

	public Road[] getGatheringAreas() {
		Road[] result = new Road[starZones.length + 1];

		result[0] = gatheringArea;
		for (int i = 0; i < starZones.length; i++) {
			result[i + 1] = starZones[i].getGatheringRoad();
		}
		return result;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(StarZone[] entity) {
		return null;
	}

	public Pair<Integer, Integer> getMeanCenter() {
		int cx = 0;
		int cy = 0;

		for (Building b : model().buildings()) {
			cx += b.getX();
			cy += b.getY();

		}
		return new Pair<Integer, Integer>(cx / model().buildings().size(), cy / model().buildings().size());
	}

	public double getMaxDistance(int cx, int cy) {
		double maxDistance = -1;
		for (Building b : model().buildings()) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			if (distance > maxDistance)
				maxDistance = distance;
		}
		return maxDistance;
	}

	public double getArea(Road r) {
		return r.getGeomArea().getBounds2D().getWidth() * r.getGeomArea().getBounds2D().getHeight();
	}

	public int getNormalizationCoeficient() {
		return 3;
	}

	private void setGatheringAreaOfStarZones() {
		double[] maxScores = new double[starZones.length];
		Road[] gatheringAreas = new Road[starZones.length];
		for (Road r : model().roads()) {
			for (int j = 0; j < starZones.length; j++) {
				double distance = Math.max(Utils.distance(r.getX(), r.getY(), starZones[j].getCx(), starZones[j].getCy()), 1);
				double nCount = model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				double height = r.getGeomArea().getBounds2D().getHeight();
				double score = (height * nCount) / (distance * distance);
				if (score > maxScores[j]) {
					maxScores[j] = score;
					gatheringAreas[j] = r;
				}
			}
		}
		for (int i = 0; i < gatheringAreas.length; i++) {
			starZones[i].setGatheringRoad(gatheringAreas[i]);
		}
	}

	public void getGatheringArea(ArrayList<Building>[] starAreas, int cx, int cy) {

		double meanDistance = 0;
		double meanArea = 0;
		double meanHeight = 0;
		double meanNeighbours = 0;
		double meanAreaScale = 0;
		int validNeighbourRoads = 0;

		for (Building b : starAreas[0]) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			meanDistance += distance;
		}
		meanDistance /= starAreas[0].size();

		for (Road r : model().roads()) {
			meanArea += getArea(r);
			if (model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount() > 2) {
				meanNeighbours += model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				validNeighbourRoads++;
			}
			meanHeight += r.getGeomArea().getBounds2D().getHeight();

		}
		for (Road r : model().roads()) {
			meanAreaScale += (getArea(r) / meanArea);

		}
		meanArea /= model().roads().size();
		meanHeight /= model().roads().size();
		meanNeighbours /= validNeighbourRoads;
		meanAreaScale /= model().roads().size();

		System.err.println("Mean Height: " + meanHeight);
		System.err.println("Mean Area: " + meanArea);
		System.err.println("Mean Neig: " + meanNeighbours);
		double maxScore = Double.MIN_VALUE;
		double minDistance = 0;
		for (Road r : model().roads()) {
			if (r.getNeighbours().size() > 2) {
				double distance = Utils.distance(r.getX(), r.getY(), cx, cy);
				if (distance < meanDistance)
					distance = meanDistance;

				double area = getArea(r);
				if (area > meanArea)
					area = meanArea;

				double nCount = model().searchWorldModel.getSearchRoad(r).getNotEntranceNeighborCount();
				if (nCount > meanNeighbours * getNormalizationCoeficient())
					nCount = meanNeighbours * getNormalizationCoeficient();

				double height = r.getGeomArea().getBounds2D().getHeight();

				if (height > meanHeight * getNormalizationCoeficient())
					height = meanHeight * getNormalizationCoeficient();

				double width = r.getGeomArea().getBounds2D().getWidth();

				double areaScale = (height * width) / meanArea;
				if (areaScale > meanAreaScale)
					areaScale = meanAreaScale;

				double score = (height * areaScale * nCount) / (distance * distance * distance);
				//				System.out.println("-----" + r.getID());
				//				System.out.println(score);
				//				System.out.println(r.getGeomArea().getBounds2D().getHeight() + "-----" + r.getGeomArea().getBounds2D().getWidth());
				if (score > maxScore || (score == maxScore && Utils.distance(r.getX(), r.getY(), cx, cy) < minDistance)) {
					maxScore = score;
					gatheringArea = r;
					minDistance = Utils.distance(r.getX(), r.getY(), cx, cy);
				}
			}
		}
		System.out.println(gatheringArea.getID());

	}

	private void createStarZones() {
		ArrayList<Building>[] starAreas = new ArrayList[STAR_TALES + 1];
		Point2D[] centPoints = new Point2D[STAR_TALES + 1];
		starZones = new StarZone[STAR_TALES + 1];
		float taleAngle = 360 / STAR_TALES;
		//initializing starAreas
		Pair<Integer, Integer> meanCenter = getMeanCenter();
		int cx = meanCenter.first();
		int cy = meanCenter.second();

		for (int i = 0; i < starAreas.length; i++) {
			starAreas[i] = new ArrayList<Building>();
			centPoints[i] = new Point2D(0, 0);

		}
		double maxDistance = getMaxDistance(cx, cy);
		for (Building b : model().buildings()) {
			double distance = Utils.distance(b.getX(), b.getY(), cx, cy);
			if (distance < maxDistance / 3) {
				starAreas[0].add(b);
				centPoints[0].setCoordinations(centPoints[0].getX() + b.getX(), centPoints[0].getY() + b.getY());
			} else {
				int index = ((int) (SearchUtils.getAngle(b, cx, cy) / taleAngle)) + 1;
				starAreas[index].add(b);
				centPoints[index].setCoordinations(centPoints[index].getX() + b.getX(), centPoints[index].getY() + b.getY());
			}
		}

		// Finilizing centeral points
		for (int i = 0; i < centPoints.length; i++) {
			centPoints[i].setCoordinations(centPoints[i].getX() / starAreas[i].size(), centPoints[i].getY() / starAreas[i].size());
		}
		// setting centeral gather point of map
		getGatheringArea(starAreas, cx, cy);

		//creating subZones
		for (int i = 0; i < starAreas.length; i++) {
			starZones[i] = new StarZone(STAR_SUB_ZONES, centPoints[i].getX(), centPoints[i].getY());

			starZones[i].createSubZones(starAreas[i], centPoints[i].getX(), centPoints[i].getY());
		}
		setGatheringAreaOfStarZones();

	}

	@Override
	public LayerType getLayerType() {
		return LayerType.Search;
	}

}

class StarZone {
	private ArrayList<Building>[] subZones = null;
	private final int subZoneCount;
	private final int taleAngle;
	private Road gatheringRoad;
	private final double cy;
	private final double cx;

	public StarZone(int subZoneCount, double cx, double cy) {
		this.subZoneCount = subZoneCount;
		this.cx = cx;
		this.cy = cy;
		setSubZones(new ArrayList[subZoneCount]);
		taleAngle = 360 / subZoneCount;
		//newing array lists
		for (int i = 0; i < getSubZones().length; i++) {
			getSubZones()[i] = new ArrayList<Building>();
		}
	}

	public void createSubZones(ArrayList<Building> zoneBuildings, double cx, double cy) {
		for (Building b : zoneBuildings) {
			int index = ((int) (SearchUtils.getAngle(b, (int) cx, (int) cy) / taleAngle));
			getSubZones()[index].add(b);
		}
	}

	public ArrayList<Building>[] getSubZones() {
		return subZones;
	}

	public void setSubZones(ArrayList<Building>[] subZones) {
		this.subZones = subZones;
	}

	public double getCy() {
		return cy;
	}

	public double getCx() {
		return cx;
	}

	public int getSubZoneCount() {
		return subZoneCount;
	}

	public Road getGatheringRoad() {
		return gatheringRoad;
	}

	public void setGatheringRoad(Road gatheringRoad) {
		this.gatheringRoad = gatheringRoad;
	}
	
}

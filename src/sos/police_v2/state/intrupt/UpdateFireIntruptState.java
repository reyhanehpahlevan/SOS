package sos.police_v2.state.intrupt;
//
//import java.awt.Point;
//import java.awt.Shape;
//import java.util.ArrayList;
//
//import sos.base.entities.Building;
//import sos.base.entities.Road;
//import sos.base.sosFireZone.ConvexHull;
//import sos.base.sosFireZone.SOSFireZone;
//import sos.base.util.SOSActionException;
//import sos.base.util.blockadeEstimator.AliGeometryTools;
//import sos.police_v2.PoliceConstants;
//import sos.police_v2.PoliceForceAgent;
//
public class UpdateFireIntruptState{}
//public class UpdateFireIntruptState extends PoliceAbstractIntruptState {
//	private ConvexHull convexHull = new ConvexHull();
//	//	private short convexedTime=0;
//	private Shape convexOfFire;
//	private ArrayList<Road> roadsInRange;
//	private Road bestRoadToUpdate;
//	private ArrayList<SOSFireZone> firezones;
//
//	public UpdateFireIntruptState(PoliceForceAgent policeForceAgent) {
//		super(policeForceAgent);
//		firezones = new ArrayList<SOSFireZone>();
//	}
//
//	@Override
//	public void act() throws SOSActionException {
//		log.info("acting as:"+this.getClass().getSimpleName());
//		log.info("LOG IS NOT COMPLETED");
//		
//		for (SOSFireZone firezone : model().Fzones) {
//			if (firezone.allenvironmentalFireyBuildings.size() < 12) {
//				firezones.add(firezone);
//			}
//		}
//		SOSFireZone myFireZone = getBestFireZone();
//		if (myFireZone == null)
//			isFinished = true;
//		updateFireZone(myFireZone);
//
//	}
//
//	private void updateFireZone(SOSFireZone myFireZone) throws SOSActionException {
//		if (myFireZone == null)
//			return;
//		if (convexCondition()) {
//			makeConvexOfFireZone(myFireZone);
//			//			convexedTime=(short) model().time();TODO????WHY
//		}
//		setInFireZoneRoads();
//		bestRoadToUpdate = findBestRoad(roadsInRange);
//		if (bestRoadToUpdate != null)
//			move(bestRoadToUpdate);
//
//	}
//
//	private void setInFireZoneRoads() {
//		for (int i = 0; i < roadsInRange.size(); i++) {
//			roadsInRange.get(i).policeArea.setMoveCost((int) agent.move.getWeightTo(roadsInRange.get(i), roadsInRange.get(i).getX(), roadsInRange.get(i).getY()));
//			roadsInRange.get(i).policeArea.isInFireZone = false;
//			if (convexOfFire.contains(new Point(roadsInRange.get(i).getX(), roadsInRange.get(i).getY()))) {
//				roadsInRange.get(i).policeArea.isInFireZone = true;
//				roadsInRange.remove(roadsInRange.get(i));
//			}
//		}
//	}
//
//	private Road findBestRoad(ArrayList<Road> roads) {
//		Road maxValueRoad = roads.get(0);
//		for (Road road : roads) {
//			if (road.policeArea.getValue() > maxValueRoad.policeArea.getValue()) {
//				maxValueRoad = road;
//			}
//		}
//		return maxValueRoad;
//	}
//
//	private SOSFireZone getBestFireZone() {
//		if (firezones.size() > 0) {
//			SOSFireZone bestFireZone = firezones.get(0);
//			for (SOSFireZone fz : firezones) {
//				if (agent.move.getWeightTo(fz.buildings().get(0), fz.buildings().get(0).getX(), fz.buildings().get(0).getY()) < agent.move.getWeightTo(bestFireZone.buildings.get(0), bestFireZone.buildings.get(0).getX(), bestFireZone.buildings.get(0).getY()))//FIXME get(0)---> get best
//					bestFireZone = fz;
//			}
//			log.info("best fie zone ====  " + bestFireZone.buildings.get(0));
//			return bestFireZone;
//		}
//		return null;
//	}
//
//	private void makeConvexOfFireZone(SOSFireZone sosfz) {
//		ArrayList<java.awt.Point> convexpoints = makeConvexPoint(sosfz);
//		convexpoints = convexHull.makeQuickHull(convexpoints);
//		convexOfFire = AliGeometryTools.getShape(sos.police_v2.base.PoliceGeometryTools.getVertex2Ds(convexpoints));
//	}
//
//	private ArrayList<java.awt.Point> makeConvexPoint(SOSFireZone sosfz) {
//		ArrayList<java.awt.Point> points = new ArrayList<java.awt.Point>();
//		resetRoadsInRange();
//		for (Building building : sosfz.allenvironmentalBuildings) {
//			makeRoadsInRange(building, PoliceConstants.RANGE_OF_FIRE_UPDATE);
//			for (Building invBuilding : building.realNeighbors_Building()) {
//				if (!invBuilding.isFierynessDefined() || invBuilding.getFieryness() == 0)
//					if (model().time() - invBuilding.updatedtime() > 15)
//						points.add(new java.awt.Point(invBuilding.getX(), invBuilding.getY()));
//			}
//		}
//		return points;
//	}
//
//	private void resetRoadsInRange() {
//		roadsInRange = new ArrayList<Road>();
//
//	}
//
//	private void makeRoadsInRange(Building building, int rangeOfFireUpdate) {
//		roadsInRange = new ArrayList<Road>(model().getRoadsInRange(building.getX(), building.getY(), rangeOfFireUpdate));
//	}
//
//	private boolean convexCondition() {
//		//		if(policeForceAgent.model().time()-convexedTime>10)
//		//		return true; FIXME if takes time
//		//		return false;
//		return true;
//	}
//
//	@Override
//	public boolean canMakeIntrupt() {
//		boolean result = false;
//		for (SOSFireZone firezones : model().Fzones) {
//			log.debug("firezones.buildings.size():" + firezones.buildings.size() + " firezones.lastCycleAdded.size():" + firezones.lastCycleAdded.size());
//			if (firezones.buildings.size() == firezones.lastCycleAdded.size())
//				result = true;
//		}
//
//		log.trace(this + " can make intrupt?" + result);
//		return result;
//	}
//
//}

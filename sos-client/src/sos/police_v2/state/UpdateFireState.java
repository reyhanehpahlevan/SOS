package sos.police_v2.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.PoliceForce;
import sos.base.entities.Road;
import sos.base.move.types.PoliceMove;
import sos.base.sosFireZone.SOSAbstractFireZone;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.sosFireZone.util.ConvexHull_arr_New;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class UpdateFireState extends PoliceAbstractState {
	//	private short convexedTime=0;
	public ArrayList<Road> roadsToClearNearFireSide;
	private Road lasttarget;

	public UpdateFireState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}
	@Override
	public void precompute() {
		
	}

	@Override
	public void act() throws SOSActionException {
		if (!isValidForMe()) {
			log.debug(this + " is not valid for me!!!");
			lasttarget = null;
			return;
		}
		log.info("acting as:" + this);
		log.info("LOG IS NOT COMPLETED");
		ArrayList<SOSAbstractFireZone> firezones = new ArrayList<SOSAbstractFireZone>();
		log.debug("Current Firesites:" + agent.fireSiteManager.getFireSites());

		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfirezone : agent.fireSiteManager.getFireSites()) {
			SOSEstimatedFireZone firezone = pfirezone.second();
			if (firezone.getAllBuildings().size() < 100 && !firezone.isDisable()) {
				log.info("this fire zone is suitable by size..." + firezone + " albate sizesh ine== " + firezone.getAllBuildings().size());
				if (!getRoadsToClearNearFireSide(firezone).isEmpty()) {
					log.debug("in firezone road baraye clear kardan dare");
					firezones.add(firezone);
				}

			}
		}
		if (firezones.isEmpty()) {
			log.info("hich fire zoni ba size monase peda nashode.");
			lasttarget = null;
			return;
		}

		SOSAbstractFireZone myFireZone = getBestFireZone(firezones);

		if (myFireZone == null) {
			log.info("fire zon emonaseb bara update nist...");
			lasttarget = null;
			return;
		}

		updateFireZone(myFireZone);
	}

	private void updateFireZone(SOSAbstractFireZone myFireZone) throws SOSActionException {
		if (myFireZone == null)
			return;
		log.info("Updating FireZones..." + myFireZone);

		roadsToClearNearFireSide = getRoadsToClearNearFireSide(myFireZone);
		priorityRoads(roadsToClearNearFireSide);
		log.debug("roadsToClearNearFireSide :" + roadsToClearNearFireSide);
		//		if (roadsToClearNearFireSide.contains(agent.me().getAreaPosition())) {
		//			List<Blockade> blockades = agent.me().getAreaPosition().getBlockades();
		//			Blockade block = chooseBestBlockade((ArrayList<Blockade>) blockades);
		//			if (block != null)
		//				clear(block);
		//		}
		//		roadsToClearNearFireSide.remove(agent.me().getAreaPosition());
		//		if(!roadsToClearNearFireSide.isEmpty())
		if (lasttarget != null && roadsToClearNearFireSide.contains(lasttarget))
			makeReachableTo(lasttarget);
		for (Road road : roadsToClearNearFireSide) {
			lasttarget = road;
			makeReachableTo(road);
		}
		lasttarget = null;

	}

	private void priorityRoads(ArrayList<Road> roadsToClearNearFireSide) {
		Collections.sort(this.roadsToClearNearFireSide, new Comparator<Road>() {

			@Override
			public int compare(Road o1, Road o2) {
				if (model().getHighways().contains(o1) == model().getHighways().contains(o2)) {
					float weighto1 = agent.move.getWeightTo(o1, o1.getX(), o1.getY(), PoliceMove.class) * getRoadScore(o1);
					float weighto2 = agent.move.getWeightTo(o2, o2.getX(), o2.getY(), PoliceMove.class) * getRoadScore(o2);

					if (weighto1 < weighto2)
						return -1;
					if (weighto1 > weighto2)
						return 1;
					return 0;
				}
				if (model().getHighways().contains(o1))
					return -1;
				if (model().getHighways().contains(o2))
					return 1;
				return 0;
			}

			/**
			 * should return between .75 and 1
			 * 
			 * @param o1
			 * @return
			 */
			private float getRoadScore(Road road) {

				//score should between .75 and 1
				return 1;
			}
		});
	}

	//	private ArrayList<Road> getRoadsToClearNearFireSide(AbstractFireSite myFireZone) {
	//		
	//	}
	private ArrayList<Road> getRoadsToClearNearFireSide(SOSAbstractFireZone myFireZone) {

		//		getRoadsToClearNearFireSideWithShapePLUS(myFireZone);

		ConvexHull_arr_New fireZoneConvex = getConvexOfFireZone(myFireZone);

		ConvexHull_arr_New worldConvex = new ConvexHull_arr_New(model().buildings()).getScaleConvex(.8f);

		HashSet<Road> inFireZone = new HashSet<Road>();
		Collection<Road> roads = model().getObjectsInRectangle(fireZoneConvex.getShape().getBounds(), Road.class);
		for (Road road : roads)
			if (fireZoneConvex.contains(road.getX(), road.getY()))
				inFireZone.add(road);

		ConvexHull_arr_New fireZoneConvex_OutSide = fireZoneConvex.getScaleConvex(1.5f);
		ArrayList<Road> selectedRoads = new ArrayList<Road>();
		roads = model().getObjectsInRectangle(fireZoneConvex_OutSide.getShape().getBounds(), Road.class);

		HashSet<Road> mapsides = new HashSet<Road>();
		log.debug("IN side Fire Convex Road=" + inFireZone);
		FOR: for (Road road : roads) {
			if (!fireZoneConvex_OutSide.contains(road.getX(), road.getY()))
				continue;

			if (inFireZone.contains(road))
				continue;

			for (Area ne : road.getNeighbours()) {
				if (ne instanceof Building)
					continue FOR;
				if (road.getSOSGroundArea() < PoliceConstants.VERY_SMALL_ROAD_GROUND_IN_MM)
					continue FOR;
				if (isReachableTo(road.getPositionPair()))
					continue FOR;
			}
			if (worldConvex.contains(road.getX(), road.getY())) {
				mapsides.add(road);
				continue;
			}
			selectedRoads.add(road);

		}
		log.debug("OUTside-Inside Fire Convex Road=" + selectedRoads);
		if (selectedRoads.isEmpty()) {
			ConvexHull_arr_New fireZoneConvex_Inside = fireZoneConvex.getScaleConvex(.8f);
			for (Road road : inFireZone) {
				if (fireZoneConvex_Inside.contains(road.getX(), road.getY()))
					continue;
				if (worldConvex.contains(road.getX(), road.getY())) {
					mapsides.add(road);
					continue;
				}
				selectedRoads.add(road);

			}
			log.debug("Inside.8 Fire Convex Road=" + selectedRoads);
		}

		if (selectedRoads.isEmpty()) {
			selectedRoads.addAll(mapsides);
			log.debug("map side roads=" + mapsides);
		}

		return selectedRoads;
	}

	private boolean clearWithShapePLUSStart = false;
	public Building nearestToCenter;
	public Building[][] fourBuilding;

	@SuppressWarnings("unused")
	private void getRoadsToClearNearFireSideWithShapePLUS(SOSAbstractFireZone myFireZone) {
		log.info("getRoadsToClearNearFireSideWithShapePLUS " + myFireZone);
		if (myFireZone.getOuter().isEmpty()) {
			log.debug("myFireZone.getOuter()==0 -> nothing to return");
			return;
		}
		if ((myFireZone.getOuter().size() > 30 && !clearWithShapePLUSStart)) {
			log.debug("myFireZone.getOuter()>30 and clearWithShapePLUSStart==false -> nothing return");
			return;
		}
		if ((clearWithShapePLUSStart && myFireZone.getOuter().size() > 50)) {
			log.debug("myFireZone.getOuter()>50 and clearWithShapePLUSStart==true-> nothing return");
			return;
		}

		///Finding 4 Roads Around firesite
		nearestToCenter = myFireZone.getOuter().get(0);
		fourBuilding = new Building[2][2];
		fourBuilding[0][0] = myFireZone.getOuter().get(0);//Leftest
		fourBuilding[0][1] = myFireZone.getOuter().get(0);//Rightest
		fourBuilding[1][0] = myFireZone.getOuter().get(0);//Downest
		fourBuilding[1][1] = myFireZone.getOuter().get(0);//TOPEST
		for (Building b : myFireZone.getOuter()) {
			int xdiff = b.getX() - myFireZone.getCenterX();
			int ydiff = b.getY() - myFireZone.getCenterY();
			int i, j;
			if (Math.abs(xdiff) > Math.abs(ydiff)) {
				i = 0;
				if (xdiff > 0)
					j = 1;
				else
					j = 0;
				if (Math.abs(xdiff) > Math.abs((fourBuilding[i][j].getX() - myFireZone.getCenterX())))
					fourBuilding[i][j] = b;

			} else {
				i = 1;
				if (ydiff > 0)
					j = 1;
				else
					j = 0;
				if (Math.abs(ydiff) > Math.abs((fourBuilding[i][j].getY() - myFireZone.getCenterY())))
					fourBuilding[i][j] = b;
			}

			int centerDistance = SOSGeometryTools.distance(b.getX(), b.getY(), myFireZone.getCenterX(), myFireZone.getCenterY());
			if (centerDistance < SOSGeometryTools.distance(nearestToCenter.getX(), nearestToCenter.getY(), myFireZone.getCenterX(), myFireZone.getCenterY()))
				nearestToCenter = b;
		}

	}

	private SOSAbstractFireZone getBestFireZone(ArrayList<SOSAbstractFireZone> firezones) {
		for (SOSAbstractFireZone firezone : firezones) {
			if (firezone.getAllBuildings().size() < 100 && !firezone.isDisable()) {
				log.info("best fie zone ====  " + firezone);
				return firezone;
			}
		}
		return null;
	}

	private ConvexHull_arr_New getConvexOfFireZone(SOSAbstractFireZone myFireZone) {
		log.info("mkham convex doros konam");
		//		ArrayList<java.awt.Point> convexpoints = makeConvexPoint(myFireZone.getFires());
		ArrayList<Building> fiery = new ArrayList<Building>();
		for (Building b : myFireZone.getFires()) {
			if (b.virtualData[0].isBurning())
				fiery.add(b);
		}
		ConvexHull_arr_New convex = new ConvexHull_arr_New(fiery);
		return convex;
		//		convexOfFire = AliGeometryTools.getShape(sos.police_v2.base.PoliceGeometryTools.getVertex2Ds(convex.));
	}

	/*
	 * private boolean convexCondition() {
	 * // if(policeForceAgent.model().time()-convexedTime>10)
	 * // return true; FIXME if takes time
	 * // return false;
	 * return true;
	 * }
	 */
	public boolean isValidForMe() {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me()))
				return true;
		}
		int updaterState = model().policeForces().size() / 10;
		if (updaterState >= model().policeForces().size())
			return false;
		ArrayList<PoliceForce> clonepolices = new ArrayList<PoliceForce>(model().policeForces());
		Collections.sort(clonepolices, new Comparator<PoliceForce>() {

			@Override
			public int compare(PoliceForce o1, PoliceForce o2) {
				return model().getPoliceTasks(o1).getJobDone() - model().getPoliceTasks(o2).getJobDone();
			}
		});
		for (int i = 0; i < updaterState; i++) {
			if (clonepolices.get(i).equals(agent.me()))
				return true;
		}
		return false;
	}

}

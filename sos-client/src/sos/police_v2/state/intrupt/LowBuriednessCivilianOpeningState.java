package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.ConfigKey;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Blockade;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.move.Path;
import sos.base.move.types.PoliceMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.base.worldGraph.Node;
import sos.police_v2.PoliceConstants;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;
import sos.police_v2.base.clearablePointToReachable.ClearablePointToReachable;
import sos.police_v2.state.OpenHighwayesState;
import sos.police_v2.state.OtherAgentStockState;
import sos.police_v2.state.UpdateClusterFireState;
import sos.police_v2.state.preCompute.MSTState;
import sos.police_v2.state.preCompute.PrecomputeState;

public class LowBuriednessCivilianOpeningState extends PoliceAbstractIntruptState {
	public static int CHECK_RANGE_CIVILIAN = 80000;
	public int MapDiameter;
	ArrayList<Pair<Civilian, Integer>> civis;
	Civilian target = null;
	String lastState = "";
	boolean isRefugeReachable;
	boolean isATReachable;
	String string = "";
	int areaS;

	int distance = 0;
	int costEstimate = 0;
	int targetTime = 0;

	/**
	 * Baraye baz kardane civilian hayi ke az nazar buriedness nesbat be kole civilian haye map buriedness
	 * behtari darand va nazdike agent hsatand
	 */
	public LowBuriednessCivilianOpeningState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		MapDiameter = (int) Point.distance(0, 0, model().getBounds().getWidth(), model().getBounds().getHeight());
		civis = new ArrayList<Pair<Civilian, Integer>>();

	}

	@Override
	public boolean canMakeIntrupt() {
		if (!PoliceConstants.IS_NEW_CLEAR)
			return false;
		findNewCivilian();
		return target != null;
	}

	private void findNewCivilian() {
		if (target == null) {
			civis.clear();
			for (Civilian select : model().civilians()) {
				if (inCheckRange(select))
					if (PoliceUtils.isValidCivilian(select, agent, true)) {
						int time = select.getRescueInfo().getDeathTime();
						civis.add(new Pair<Civilian, Integer>(select, time));
					}
			}
			if (civis.size() > 0) {
				Collections.sort(civis, new Comparator<Pair<Civilian, Integer>>() {

					@Override
					public int compare(Pair<Civilian, Integer> o1, Pair<Civilian, Integer> o2) {
						if (o1.second() > o2.second())
							return 1;
						if (o1.second() < o2.second())
							return -1;
						return 0;

					}
				});
			}
			isRefugeReachable = false;
			for (Refuge refuge : model().refuges())
				if (refuge.isReallyReachable(true)) {
					isRefugeReachable = true;
					break;
				}
			isATReachable = false;
			for (AmbulanceTeam at : model().ambulanceTeams())
				if (at.isReallyReachable(true)) {
					isATReachable = true;
					break;
				}
			lastState = agent.lastCycleState;
			for (Pair<Civilian, Integer> temp : civis) {
				setSelect(temp.first(), temp.second() - model().time(), isRefugeReachable, isATReachable);
				if (target != null)
					break;
			}
		}

	}

	private boolean inCheckRange(Civilian select) {
		int dis = (int) Point.distance(agent.me().getX(), agent.me().getY(), select.getX(), select.getY());
		return dis < CHECK_RANGE_CIVILIAN;
	}

	private void setSelect(Civilian c, int deathTime, boolean isReachableToRefuge, boolean isReachableToAT) {
		int cost = getTimeToReachable(c);
		costEstimate = cost;
		int timeLimit = 0;
		int deathMaxLimit = Integer.MIN_VALUE;
		int deathMinLimit = Integer.MAX_VALUE;
		if (lastState.equals(UpdateClusterFireState.class.getSimpleName())) {
			// to vaziatike update search anst harchi civiliani ke mitone harekat kone ro to 2 cycle 
			if (c.getDamage() == 0 && c.getBuriedness() == 0) {
				//				log.warn(c + " is none damage when its in updatecluster");
				timeLimit = 3;
				Building b = (Building) c.getAreaPosition();
				if (b.getTemperature() > 27)
					timeLimit = 2;
				deathMaxLimit = Integer.MAX_VALUE;
				deathMinLimit = 0;
			}
			else {
				timeLimit = 0;
			}
		} else if ((lastState.equals(PrecomputeState.class.getSimpleName())
		|| lastState.equals(MSTState.class.getSimpleName()))) {
			boolean isInDangerZoneByFire = isInDangerZoneByFire(c);
			if (c.getDamage() == 0 && c.getBuriedness() == 0) {
				if (isInDangerZoneByFire) {
					//					log.warn(c + "(danger) is none damage when its in precom or MST");
					//TODO to check this time limit position
					timeLimit = 5;
					Building b = (Building) c.getAreaPosition();
					if (b.getTemperature() > 27 && b.getTemperature() < 37)
						timeLimit = 2;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				} else {
					//					log.warn(c + " is none damage when its in precom or MST");
					timeLimit = 3;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				}
			} else if (isReachableToAT && isReachableToRefuge && (!isInDangerZoneByFire)) {
				//				log.warn(c + " is have damage when its in precom or MST");
				timeLimit = 2;
				deathMaxLimit = c.getBuriedness() + 50;
				deathMinLimit = c.getBuriedness() + 10;
			}
		} else if (lastState.equals(OpenHighwayesState.class.getSimpleName())
				|| lastState.equals(OtherAgentStockState.class.getSimpleName())) {
			boolean isInDangerZoneByFire = isInDangerZoneByFire(c);
			if (c.getDamage() == 0 && c.getBuriedness() == 0) {
				if (isInDangerZoneByFire) {
					//					log.warn(c + " (danger) is none damage when its in fireserch open highway other agent");
					timeLimit = 4;
					Building b = (Building) c.getAreaPosition();
					if (b.getTemperature() > 27 && b.getTemperature() < 37)
						timeLimit = 2;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				} else {
					//					log.warn(c + " is none damage when its in fireserch open highway other agent");
					timeLimit = 3;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				}
			} else {
				if (isReachableToAT && isReachableToRefuge && (!isInDangerZoneByFire)) {
					//					log.warn(c + " is have damage when its in fireserch open highway other agent");
					timeLimit = 3;
					deathMaxLimit = c.getBuriedness() + 50;
					deathMinLimit = c.getBuriedness() + 10;
				}
			}
		} else {
			boolean isInDangerZoneByFire = isInDangerZoneByFire(c);
			if (c.getDamage() == 0 && c.getBuriedness() == 0) {
				if (isInDangerZoneByFire) {
					timeLimit = 4;
					Building b = (Building) c.getAreaPosition();
					if (b.getTemperature() > 27 && b.getTemperature() < 37)
						timeLimit = 3;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				} else {
					timeLimit = 3;
					deathMaxLimit = Integer.MAX_VALUE;
					deathMinLimit = 0;
				}
			} else {
				if (isReachableToAT && isReachableToRefuge && (!isInDangerZoneByFire)) {
					timeLimit = 4;
					deathMaxLimit = c.getBuriedness() + 50;
					deathMinLimit = c.getBuriedness() + 10;
				}
			}
		}
		if (c.getDamage() == 0 && c.getBuriedness() == 0) {
			timeLimit = Math.max(timeLimit, 2);
			if (worldIsInFire())
				timeLimit = Math.max(timeLimit, 4);
			deathMaxLimit = Integer.MAX_VALUE;
			deathMinLimit = 0;
		}
		//		if (true) {
		if (cost < timeLimit && deathTime > deathMinLimit && deathTime < deathMaxLimit) {
			Pair<Area, Point2D> ep = getEntrancePoint(c.getAreaPosition());
			ArrayList<Pair<? extends Area, Point2D>> dests = new ArrayList<Pair<? extends Area, Point2D>>();
			dests.add(ep);
			Path path = agent.move.getPathToPoints(dests, PoliceMove.class);
			areaS = path.getLenght();
			target = c;
			targetTime = model().time();
		}
	}

	private boolean worldIsInFire() {
		float worldFire = model().fieryBuildings().size() / model().buildings().size();
		if (worldFire >= 0.5f)
			return true;
		return false;
	}

	private boolean isInDangerZoneByFire(Civilian c) {

		SOSEstimatedFireZone zone;
		int dis = Integer.MAX_VALUE;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			zone = pair.second();
			int[] apexes = zone.getConvex().getApexes();
			for (int i = 0; i < apexes.length; i = i + 2) {
				dis = (int) Math.min(dis, Point.distance(c.getX(), c.getY(), apexes[i], apexes[i + 1]));
			}
		}
		//		log.warn("}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}} "+c+"  is in danger zone by fire ="+(dis < MapDiameter / 7));
		return dis < MapDiameter / 7;

	}

	private int getTimeToReachable(StandardEntity entity) {
		int cost = 0;
		Civilian civilian = (Civilian) entity;
		Pair<Area, Point2D> ep = getEntrancePoint(entity.getAreaPosition());
		ArrayList<Pair<? extends Area, Point2D>> dests = new ArrayList<Pair<? extends Area, Point2D>>();
		dests.add(ep);
		Path path = agent.move.getPathToPoints(dests, PoliceMove.class);
		cost += getTimeToTarget(path);
		if (cost > 1)
			return 100;
		ArrayList<Blockade> tempBlockades = reachableWithEdge.getBlockingBlockadeOfPath(path);
		Point point = agent.clearableToPoint.nextPointToClear(path, true, false);
		ArrayList<Blockade> targets = new ArrayList<Blockade>();
		for (Blockade blockade : tempBlockades)
			if (!targets.contains(blockade))
				targets.add(blockade);
		tempBlockades = new ArrayList<Blockade>(model().getBlockadesInRange(agent.me().getX(), agent.me().getY(), agent.clearDistance));
		//		System.err.println("size=" + targets.size());
		for (Blockade blockade : tempBlockades)
			if (!targets.contains(blockade))
				targets.add(blockade);
		int surface = getSurfaceOfWayPath(targets, path);
		cost += ((surface * 0.000001) / ConfigKey.getClearRepairRate()) + 1;
		//		log.warn("  en.x=" + ep.second().getIntX() + "  en.y" + ep.second().getIntY() + "  surface=" + surface + "  rate=" + ConfigKey.getClearRepairRate() + "  taghsim=" + ((surface * 0.000001) / ConfigKey.getClearRepairRate()));
		//		log.warn("bloackad in way size=" + targets.size() + "  with move cost=" + getTimeToTarget(path) + " to civilian " + entity.getID());
		return cost;
	}

	private int getSurfaceOfWayPath(ArrayList<Blockade> targets, Path path) {
		int surface = 0;
		Node[] allMoveNodes = path.getNodes();
		if (allMoveNodes == null) {
			Point p1 = agent.me().getPositionPoint().toGeomPoint();
			Point p2 = path.getDestination().second().toGeomPoint();
			java.awt.geom.Area clearArea = PoliceUtils.getClearAreaByPoint(p1, p2.x, p2.y, agent.clearDistance, agent.clearWidth);
			for (Blockade blockade : targets) {
				java.awt.geom.Area temp = new java.awt.geom.Area(blockade.getShape());
				temp.intersect(clearArea);
				surface += PoliceUtils.surface(temp);
			}
		} else {
			Point p1 = agent.me().getPositionPoint().toGeomPoint();
			Point p2;
			for (int i = 0; i < allMoveNodes.length; i += 2) {
				p2 = ClearablePointToReachable.getPoint(allMoveNodes[i]);
				java.awt.geom.Area clearArea = PoliceUtils.getClearAreaByPoint(p1, p2.x, p2.y, agent.clearDistance, agent.clearWidth);
				for (Blockade blockade : targets) {
					java.awt.geom.Area temp = new java.awt.geom.Area(blockade.getShape());
					temp.intersect(clearArea);
					surface += PoliceUtils.surface(temp);
				}
				p1 = p2;
			}
		}
		return surface;
	}

	@Override
	public void act() throws SOSActionException {
		//		System.err.println(lastState);
		log.info("acting as:" + this);
		log.debug("target cilvilian=" + target);
		if ((model().time() - targetTime) > costEstimate + 2) {
			log.debug("rafte jayi kare dg anjam bede nayad dg bara edame");
			target = null;
		}
		if (target != null && missionComplete(target)) {
			log.warn("Low Buriedness civilian opened " + target + "  estimate time = " + costEstimate + "   real time = " + (model().time() - targetTime) + "   path lenth=" + areaS);
			target.setIsReallyReachable(true);
			lastState = "";
			target = null;
			//			string="";
			//			sum=0;
		}
		if (!PoliceUtils.isValidCivilian(target, agent, true)) {
			lastState = "";
			target = null;
			findNewCivilian();
		}
		log.debug("current assigned civilian is:" + target);
		if (target != null) {
			Pair<Area, Point2D> ep = getEntrancePoint(target.getAreaPosition());
			if (ep == null) {
				log.error("how?????");
				target = null;
				return;
			}
			moveToPoint(ep);//TODO TOO BUILDING MAGHSADESH NABASHE;)
		}

	}

	private boolean missionComplete(Civilian assignCivil) {
		if (agent.location().equals(assignCivil.getPosition()))
			return true;
		if (assignCivil.isReallyReachable(true))
			return true;
		if (assignCivil.getPosition().isReallyReachable(true))
			return true;
		if (agent.location().getNeighbours().contains(assignCivil.getPosition()) && agent.location().isBlockadesDefined() && agent.location().getBlockades().isEmpty())
			return true;
		//		if (isReallyOpen(assignCivil))
		//			return true;
		return false;
	}

}

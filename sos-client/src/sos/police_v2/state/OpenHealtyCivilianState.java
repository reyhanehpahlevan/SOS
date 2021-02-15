package sos.police_v2.state;

import java.awt.Point;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;

import rescuecore2.geometry.Point2D;
import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSFireZoneManager;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;

public class OpenHealtyCivilianState extends PoliceAbstractState {
	//change think for map rule
	boolean haveAfterShock = false;
	SOSFireZoneManager zoneManager;
	public Shape dangerZone = null;
	public Shape ScapeShape = null;
	private HashSet<Building> myClusterBuilding;

	public OpenHealtyCivilianState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);

	}
	@Override
	public void precompute() {
		zoneManager = agent.fireSiteManager;
		myClusterBuilding = model().searchWorldModel.getClusterData().getBuildings();
		
	}


	Civilian assignCivil = null;

	@Override
	public void act() throws SOSActionException {
		log.info("acting as:" + this);
		log.debug("assigned cilvilian=" + assignCivil);
		if (assignCivil != null && missionComplete(assignCivil)) {
			log.info("missoin complete!!" + assignCivil + " opened!(Healty)");
			assignCivil.setIsReallyReachable(true);
			log.warn("civilian maked reachable " + assignCivil.getID());
			assignCivil = null;
		}

		if (!PoliceUtils.isValidHealtyCivilian(assignCivil, agent)) {
			if (assignCivil != null)
				log.warn("civilian is not valid more" + assignCivil.getID());
			assignCivil = assignNewCivilian();
		}
		log.debug("current assigned civilian is:" + assignCivil);
		if (assignCivil != null) {
			Pair<Area, Point2D> ep = getEntrancePoint(assignCivil.getAreaPosition());
			if (ep == null) {
				log.error("how?????");
				assignCivil = null;
				return;
			}
			moveToPoint(ep);//TODO TOO BUILDING MAGHSADESH NABASHE;)
		}

	}

	private Civilian assignNewCivilian() {
		ArrayList<Civilian> civis = new ArrayList<Civilian>();
		//		if (haveAfterShock)
		//			for (Civilian civilian : model().civilians()) {
		//				if (PoliceUtils.isValidHealtyCivilian(civilian, agent))
		//					if (getTimeToTarget(civilian) <= 2)
		//						return civilian;
		//			}
		if (agent.messageSystem.type == Type.NoComunication) {
			for (Civilian civilian : model().civilians()) {
				if (PoliceUtils.isValidHealtyCivilian(civilian, agent))
					if (myClusterBuilding.contains(civilian.getAreaPosition())) {
						civis.add(civilian);
					}
			}
			if (civis.size() > 0) {
				Civilian target = civis.get(0);
				for (Civilian civilian : civis) {
					if (target == civilian)
						continue;
					if (Point.distance(agent.me().getX(), agent.me().getY(), civilian.getX(), civilian.getY())
					< Point.distance(agent.me().getX(), agent.me().getY(), target.getX(), target.getY())) {
						target = civilian;
					}
				}
				log.warn("found ciritical civilian =" + target.getID());
				return target;
			}
		}
		else {
			// TODO age tempe sakhtemon ziad bashe bayad che kone bebine mirese ya na
			dangerZone = getDangersZone();
			for (Civilian civilian : model().civilians())
				if (PoliceUtils.isValidHealtyCivilian(civilian, agent))
					if (dangerZone.contains(civilian.getX(), civilian.getY()))
						if (!ScapeShape.contains(civilian.getX(), civilian.getY()))
							if (model().time() - civilian.updatedtime() < 10)
								civis.add(civilian);

			if (civis.size() > 0) {
				Civilian target = civis.get(0);
				for (Civilian civilian : civis) {
					if (target == civilian)
						continue;
					if (Point.distance(agent.me().getX(), agent.me().getY(), civilian.getX(), civilian.getY())
					< Point.distance(agent.me().getX(), agent.me().getY(), target.getX(), target.getY())) {
						target = civilian;
					}
				}
				log.warn("found ciritical civilian =" + target.getID());
				return target;
			}
		}
		return null;
	}

	private Shape getDangersZone() {
		java.awt.geom.Area Target = new java.awt.geom.Area();
		SOSEstimatedFireZone zone = null;
		if (zoneManager.getFireSites().size() > 0) {
			zone = zoneManager.getFireSites().get(0).second();
			int distance = Integer.MAX_VALUE;
			int tempdistance;
			for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> sites : zoneManager.getFireSites()) {
				SOSEstimatedFireZone temp = sites.second();
				if(temp.isDisable())
					continue;
				tempdistance = (int) Point.distance(temp.getCenterX(), temp.getCenterY(), agent.me().getX(), agent.me().getY());
				if (tempdistance < distance) {
					distance = tempdistance;
					zone = temp;
				}
			}
		}
		if (zone != null) {
			Target = new java.awt.geom.Area(zone.getConvex().getScaleConvex(1.5f).getShape());
			ScapeShape = new java.awt.geom.Area(zone.getConvex().getScaleConvex(1.1f).getShape());
		}
		return Target;
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
		return false;
	}

}

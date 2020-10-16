package sos.police_v2.state.intrupt;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.PoliceForce;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.tools.lists.ShortList;

public class SmallFireGardClearingInterupt extends PoliceAbstractIntruptState {
	private int checkRang = 0;
	public SOSEstimatedFireZone targetZone = null;
	public ArrayList<Building> checkedGard;
	private Building targetSelectedBuilding = null;
	private ArrayList<Building> selectedBuildings;
	private HashSet<Building> clusterBuilding;
	private ArrayList<Building> allFireZone;
	private ShortList allFireZoneTime;
	public ArrayList<Building> gardOfFire;

	public SmallFireGardClearingInterupt(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		checkRang = (int) (model().getDiagonalOfMap() / 10);
		checkedGard = new ArrayList<Building>();
		selectedBuildings = new ArrayList<Building>();
		allFireZone = new ArrayList<Building>();
		allFireZoneTime = new ShortList();
		clusterBuilding = model().searchWorldModel.getClusterData().getBuildings();
		gardOfFire = new ArrayList<Building>();
	}

	@Override
	public boolean canMakeIntrupt() {
		log.debug("targetSelectedBuilding : " + targetSelectedBuilding);
		updateAllFireZoneTime();
		if (targetSelectedBuilding == null) {
			setZone();
		} else {
			updateZone();
			if (targetZone != null && !amICheck(targetZone))
				setZone();
		}
		if (targetZone != null)
			if (!isSmallFire(targetZone)) {
				targetZone = null;
				targetSelectedBuilding = null;
			}
		if (targetZone != null)
			return true;
		targetSelectedBuilding = null;
		return false;
	}

	private boolean amICheck(SOSEstimatedFireZone selectZone) {
		ArrayList<PoliceForce> forces = agent.getVisibleEntities(PoliceForce.class);
		for (PoliceForce force : forces) {
			if (isZoneTheBestForForce(force, selectZone)) {
				if (force.getID().getValue() == agent.getID().getValue())
					continue;
				if (agent.messageSystem.type == Type.NoComunication) {

					if (isZoneInForceCluster(force, selectZone)) {
						log.debug("selected zone is in " + force + " cluster so i not attantion him");
						continue;
					}
					if (isZoneInForceCluster(agent.me(), selectZone)) {
						log.debug("selected zone is in my zone so i passed to" + force);
						return false;
					}
				} else {
					if (isZoneInForceCluster(force, selectZone)) {
						log.debug("selected zone is for " + force + " so i passed to him");
						return false;
					}
					if (isZoneInForceCluster(agent.me(), selectZone)) {
						log.debug("selected zone is in my zone so i must go ");
						return true;
					}
				}
				if (force.getID().getValue() < agent.getID().getValue()) {
					log.debug(selectZone + " is passed to force " + force);
					return false;
				}
			}
		}
		return true;
	}

	private boolean isZoneInForceCluster(PoliceForce force, SOSEstimatedFireZone zone) {
		for (Building building : zone.getAllBuildings()) {
			if (model().searchWorldModel.getClusterData(force).getBuildings().contains(building))
				return true;
		}
		return false;
	}

	private boolean isZoneTheBestForForce(PoliceForce force, SOSEstimatedFireZone selectZone) {
		int distance = Integer.MAX_VALUE;
		SOSEstimatedFireZone temp = null;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			if (isSmallFire(pair.second())) {
				SOSEstimatedFireZone zone = pair.second();
				if (!zone.isDisable()) {
					if (zone.getAllBuildings().size() > 0) {
						for (Building select : zone.getAllBuildings()) {
							int dis = (int) Point.distance(select.x(), select.y(), force.getX(), force.getY());
							if (dis <= checkRang) {
								if (distance > dis) {
									distance = dis;
									temp = zone;
								}
							}
						}
					}
				}
			}
		}
		if (temp != null && temp.getAllBuildings().size() > 0) {
			if (selectZone.getAllBuildings().contains(temp.getAllBuildings().get(0))) {
				log.debug("my target zone is best zone for " + force);
				return true;
			}
		}
		log.debug("my target zone is NOT best zone for " + force);
		return false;
	}

	private void updateAllFireZoneTime() {
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			if (!isSmallFire(pair.second()))
				continue;
			if (pair.second().getAllBuildings().size() == 0)
				continue;
			if (getZoneTime(pair.second()) == -1) {
				allFireZone.add(pair.second().getAllBuildings().get(0));
				allFireZoneTime.add((short) agent.time());
			}
		}
	}

	private short getZoneTime(SOSEstimatedFireZone fireZone) {
		short index = 0;
		for (Building building : allFireZone) {
			if (fireZone.getAllBuildings().contains(building))
				return allFireZoneTime.get(index);
			index++;
		}
		return -1;
	}

	private void updateZone() {
		targetZone = null;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			if (!isSmallFire(pair.second()))
				continue;
			if (pair.second().getAllBuildings().contains(targetSelectedBuilding)) {
				targetZone = pair.second();
				log.debug("zone updated " + targetZone);
				return;
			}
		}

	}

	private void setZone() {
		targetZone = null;
		int myCheckRnage = checkRang;
		int distance = Integer.MAX_VALUE;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			if (isSmallFire(pair.second())) {
				SOSEstimatedFireZone zone = pair.second();
				if (!isNewZone(zone))
					continue;
				if (!amICheck(zone)) {
					if (zone.getAllBuildings().size() > 0)
						selectedBuildings.add(zone.getAllBuildings().get(0));
					continue;
				}
				if (!IsZoneCheckedBefore(zone)) {
					if (!zone.isDisable()) {
						if (zone.getAllBuildings().size() > 0) {
							myCheckRnage = checkRang;
							for (Building select : zone.getAllBuildings()) {
								if (clusterBuilding.contains(zone))
									myCheckRnage = checkRang * 2;
								int dis = (int) Point.distance(select.x(), select.y(), agent.me().getX(), agent.me().getY());
								if (dis <= myCheckRnage) {
									if (distance > dis) {
										distance = dis;
										targetZone = zone;
									}
								}
							}
						}
					}
				}
			}
		}
		if (targetZone != null) {
			checkedGard.clear();
			targetSelectedBuilding = targetZone.getAllBuildings().get(0);
			selectedBuildings.add(targetSelectedBuilding);
		}
		log.debug("set zone " + targetZone);
	}

	private boolean isNewZone(SOSEstimatedFireZone zone) {
		if (agent.time() - getZoneTime(zone) < 21)
			return true;
		return false;
	}

	private boolean IsZoneCheckedBefore(SOSEstimatedFireZone zone) {
		for (Building building : selectedBuildings)
			if (zone.getAllBuildings().contains(building))
				return true;
		return false;
	}

	private boolean isSmallFire(SOSEstimatedFireZone fz) {
		if (agent.messageSystem.type == Type.NoComunication) {
			boolean isInMyCluster = false;
			for (Building building : fz.getAllBuildings()) {
				if (clusterBuilding.contains(building)) {
					isInMyCluster = true;
					break;
				}
			}
			if (isInMyCluster) {
				if (fz.getAllBuildings().size() > 3)
					return false;
			} else {
				if (fz.getAllBuildings().size() < 8)
					return true;
			}
		}
		if (fz.getAllBuildings().size() < 8)
			return true;
		return false;
	}

	private boolean isTargetSeenRecently(Building building) {
		if (building.updatedtime() < 5)
			return false;
		if (building.getLastSenseTime() > agent.time() - 5)
			return true;
		return false;
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub

	}

	@Override
	public void act() throws SOSActionException {
		Building select = getNearest();
		if (select != null) {
			moveToShape(select.fireSearchBuilding().sensibleAreasOfAreas());
		} else {
			targetZone = null;
			targetSelectedBuilding = null;
		}
	}

	private Building getNearest() {
		Building result = null;
		int distance = Integer.MAX_VALUE;
		getGardOfFireZone();
		for (Building building : gardOfFire) {
			if (checkedGard.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				checkedGard.add(building);
				continue;
			}
			int dis = (int) Point.distance(agent.me().getX(), agent.me().getY(), building.getX(), building.getY());
			if (distance > dis) {
				distance = dis;
				result = building;
			}
		}
		return result;
	}

	private void getGardOfFireZone() {
		gardOfFire.clear();
		for (Building outer : targetZone.getOuter()) {
			PriorityQueue<Pair<Building, Integer>> nears = new PriorityQueue<Pair<Building, Integer>>(11, new Comparator<Pair<Building, Integer>>() {

				@Override
				public int compare(Pair<Building, Integer> o1, Pair<Building, Integer> o2) {

					return o1.second().compareTo(o2.second());
				}
			});
			for (Building near : outer.realNeighbors_Building()) {
				if (near.isFierynessDefined())
					continue;
				nears.add(new Pair<Building, Integer>(near, (int) Point.distance(agent.me().getX(), agent.me().getY(), near.getX(), near.getY())));
			}
			if (nears.size() == 0)
				continue;
			short addNumber = (short) Math.max(1, nears.size() / 2);
			for (int i = 0; i < addNumber; i++)
				gardOfFire.add(nears.poll().first());
		}
	}
}

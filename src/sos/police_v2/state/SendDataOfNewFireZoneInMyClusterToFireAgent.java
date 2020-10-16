package sos.police_v2.state;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.move.types.PoliceMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.fire_v2.target.Tools;
import sos.police_v2.PoliceForceAgent;

public class SendDataOfNewFireZoneInMyClusterToFireAgent extends PoliceAbstractState {
	ArrayList<Building> doneBuilding;
	public int startTime;
	public boolean isStart = false;
	public boolean isFinished = false;
	HashSet<Building> clusterBuilding;
	ArrayList<SOSEstimatedFireZone> fireZonesInCluster;
	public SOSEstimatedFireZone fireZone = null;
	Building myZoneSelected = null;
	ArrayList<Building> checkedZoneSelected;
	public short MIN_FIRE_AGENT_NEEDED = 5;
	SOSEstimatedFireZone targetZone = null;
	short timeInZone = 0;

	public SendDataOfNewFireZoneInMyClusterToFireAgent(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
		checkedZoneSelected = new ArrayList<Building>();
		doneBuilding = new ArrayList<Building>();
		clusterBuilding = model().searchWorldModel.getClusterData().getBuildings();
	}

	@Override
	public void precompute() {

	}

	@Override
	public void act() throws SOSActionException {
		log.debug("act as send data");
		if (isFinished) {
			return;
		} else {
			if (getNumberOfFireBrigade() >= MIN_FIRE_AGENT_NEEDED) {
				log.debug("find enough fire agent to send data");
				finish();
			}
		}
		if (isStart) {
			if (myZoneSelected == null)
				myZoneSelected = fireZone.getAllBuildings().get(0);
			goAndSend();
		}
	}

	private void finish() {
		isFinished = true;
	}

	private void goAndSend() throws SOSActionException {
		log.debug("let go and send data to another fireZone");
		if (timeInZone > 10) {
			log.debug("TIME to this fire zone is finished must change fire zone");
			timeInZone = 0;
			targetZone = null;
		}
		if (targetZone != null && targetZone.getAllBuildings().size() == 0) {
			log.warn("SIZE E FIRE ZONE SEFRE YANI CHI ? ONAM INJA ?");
			targetZone = null;
		}
		if (targetZone != null)
			if (getNumOfUnCheckedBuilding(targetZone) == 0) {
				log.debug("all of fire zone that have " + fireZone.getAllBuildings().get(0) + " have no more building to check");
				targetZone = null;
			}
		if (targetZone == null) {
			log.debug("setting target zone");
			timeInZone = 0;
			int distance = Integer.MAX_VALUE;
			for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfs : model().getFireSites()) {
				SOSEstimatedFireZone zone = pfs.second();
				if (zone.isDisable())
					continue;
				if (zone.getAllBuildings().size() == 0)
					continue;
				if (isMineOrCheckedCluster(zone)) {
					log.debug(zone.getAllBuildings().get(0) + " zoni ke daratesh ro ghablan check shode ya zone khodame");
					continue;
				}
				int dis = (int) Point.distance(agent.me().getX(), agent.me().getY(), zone.getCenterX(), zone.getCenterY());
				if (distance > dis) {
					distance = dis;
					targetZone = zone;
				}
			}
		}
		if (targetZone != null) {
			checkedZoneSelected.add(targetZone.getAllBuildings().get(0));
			doneBuilding.clear();
			log.debug("targetZone is a zone that have " + targetZone.getAllBuildings().get(0));
			Building select = getBest();
			if (select != null) {
				if (doneBuilding.size() > 0)
					timeInZone++;
				moveToShape(select.fireSearchBuilding().sensibleAreasOfAreas());
			} else {
				log.debug("nabayad inja miomada ");
				targetZone = null;
			}
		} else {
			log.debug("not other fire zone to sent dada so this state must finish");
			finish();

		}

	}

	private int getNumOfUnCheckedBuilding(SOSEstimatedFireZone targetZone2) {
		int result = 0;
		log.debug("getNumOfUnCheckedBuilding");
		for (Building building : targetZone.getOuter()) {
			if (doneBuilding.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				log.debug(building + " is seen and updated");
				doneBuilding.add(building);
				continue;
			}
			result++;
		}
		log.debug("NumOfUnCheckedBuilding=" + result);
		return result;
	}

	private short getNumberOfFireBrigade() {
		short index = 0;
		for (FireBrigade brigade : model().fireBrigades()) {
			if (brigade.getLastSenseTime() > startTime) {
				if (brigade.getHP() == 0)
					continue;
				if (brigade.getBuriedness() > 0)
					continue;
				index++;
			}
		}
		return index;
	}

	private Building getBest() {
		Building result = null;
		int maxPrioty = Integer.MIN_VALUE;
		updatePrioty();
		for (Building building : targetZone.getOuter()) {
			if (doneBuilding.contains(building))
				continue;
			if (isTargetSeenRecently(building)) {
				log.debug(building + " is seen and updated");
				doneBuilding.add(building);
				continue;
			}
			int temp = building.priority();
			if (temp > maxPrioty) {
				maxPrioty = temp;
				result = building;
			}
		}
		return result;
	}

	private boolean isTargetSeenRecently(Building building) {
		if (building.updatedtime() < 5)
			return false;
		if (building.getLastSenseTime() > agent.time() - 5)
			return true;
		return false;
	}

	private void updatePrioty() {
		for (Building building : targetZone.getOuter()) {
			building.resetPriority();
		}
		for (Building building : targetZone.getOuter()) {
			try {
				if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
					EP_setPriorityForSpread(10000, building.getEstimator(), building);
				else
					EP_setPriorityForSpread(1000, building.getEstimator(), building);
			} catch (Exception e) {
				// TODO: handle exception
			}
			EX_E_setNegativePriorityForUpdatetime(building, 1000);
			E_setPriorityForUnburnedNeighbours(building, 100);
			E_setPriorityForCLusterGard(building, (4000));
		}

	}

	protected void EP_setPriorityForSpread(int priority, SOSEstimatedFireZone site, Building b) {
		int SPREAD_ANGLE = 60;

		//		if (((FireWorldModel) agent.model()).getInnerOfMap().contains(site.getCenterX(), site.getCenterY()))
		//			SPREAD_ANGLE = 120;
		//		else
		//			SPREAD_ANGLE = 90;

		double x1, y1;
		site.computeSpread();
		Pair<Double, Double> spread = site.spread;
		x1 = spread.first();
		y1 = spread.second();

		double length = Math.hypot(x1, y1);
		//		log.warn("LENT " + length);
		priority = (int) (priority * length);

		double a3 = Tools.getAngleBetweenTwoVector(x1, y1, b.getX() - site.getCenterX(), b.getY() - site.getCenterY());
		int x = (int) (Math.abs(a3) / 30d);
		//		log.info("Building=" + b + "\t zone=" + site + "\tX=" + x + "\ta3=" + a3);
		int coef = 1;
		if (Tools.isBigFire(site))
			coef = 2;
		if (site.getOuter().size() > 30)
		{
			coef = 30;
		}
		//		tablelog.addScore(b.toString(), "SPREAD X", x);
		//		tablelog.addColumn("SPREAD");
		if (!(a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30))
			addPriority(b, (coef * priority / (x + 1)), ("SPREAD"));
		else
			addPriority(b, 0, ("SPREAD"));

		//		if (a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30) {
		//			//addPriority(b, -2 * coef * priority, "FILTER_SPREAD");
		//		}
	}

	private boolean isMineOrCheckedCluster(SOSEstimatedFireZone zone) {
		if (zone.getAllBuildings().contains(myZoneSelected))
			return true;
		for (Building building : checkedZoneSelected) {
			if (zone.getAllBuildings().contains(building))
				return true;
		}
		return false;
	}

	private void addPriority(Building b, int priority, String Comment) {
		//		tablelog.addScore(b.toString(), Comment, priority);
		b.addPriority(priority, Comment);
	}

	protected void EX_E_setNegativePriorityForUpdatetime(Building b, int priority) {
		addPriority(b, priority * b.getFieryness() * (agent.time() - getNearMaXUpdate(b)), "updatetimeScore");
	}

	protected void EP_setPriorityForDistance(Building b, int priority) {
		addPriority(b, agent.move.getMovingTime(agent.move.getPathTo(Arrays.asList(b), PoliceMove.class)) * priority, "Distance");
	}

	protected void E_setPriorityForUnburnedNeighbours(Building b, int i) {
		int num = 0;
		for (Building n : b.realNeighbors_Building()) {
			if (n.virtualData[0].getFieryness() == 0 || n.virtualData[0].getFieryness() == 4) {
				num++;
			}
		}
		addPriority(b, num * i, "UnBurned Neighbours");

	}

	protected void E_setPriorityForCLusterGard(Building b, int i) {
		if (clusterBuilding.contains(b))
			addPriority(b, i, "Cluster Builing");
	}

	private void EX_E_setPriorityForRandombld(Building b, int i) {
		int Random = agent.model().me().getID().getValue() + b.getID().getValue();
		int x = Random % 13;
		addPriority(b, i * x, "Random Score");
	}

	private int getNearMaXUpdate(Building b) {
		int max = b.updatedtime();
		for (Building building : b.realNeighbors_Building())
			max = Math.max(max, building.updatedtime());
		return max;
	}
}

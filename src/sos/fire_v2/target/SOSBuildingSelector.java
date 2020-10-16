package sos.fire_v2.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import rescuecore2.misc.Pair;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.Building;
import sos.base.entities.Center;
import sos.base.entities.FireBrigade;
import sos.base.entities.GasStation;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.sosLogger.TableLogger;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.AbstractFireBrigadeAgent;
import sos.fire_v2.base.tools.BuildingBlock;
import sos.fire_v2.base.tools.FireStarCluster;
import sos.fire_v2.base.tools.FireStarZone;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.fire_v2.position.PositioningCostEvaluator;
import sos.fire_v2.target.SOSFireZoneSelector.Task;

public class SOSBuildingSelector extends SOSSelectTarget<Building> {

	public HashMap<FireBrigade, Task> fireBrigade_Task;
	public FireStarCluster starCluster;
	private PositioningCostEvaluator positioningcostEvaluator;
	public SOSBuildingSelector(@SuppressWarnings("rawtypes") SOSAgent agent, FireStarCluster starCluster, HashMap<FireBrigade, Task> fireBrigade_Task, SelectStrategy strategy)
	{
		super(agent, null);
		this.fireBrigade_Task = fireBrigade_Task;
		this.starCluster = starCluster;
		this.strategy = strategy;
		positioningcostEvaluator = ((FireBrigadeAgent) agent).positioning.getPositioningCostEvaluator();
	}

	@Override
	public void preCompute() {

	}

	@Override
	public Building getBestTarget(List<Building> validTarget) {
		double max = Integer.MIN_VALUE;
		Building best = null;

		for (Building e : validTarget) {
			if (e.priority() > max) {
				best = e;
				max = e.priority();
			}
		}
		log.info("get best Target " + best);

		return best;
	}

	@Override
	public void reset(List<Building> validTarget) {
		for (Building b : validTarget)
			b.resetPriority();
	}

	TableLogger tablelog;

	@Override
	public void setPriority(List<Building> validTarget) {
		tablelog = new TableLogger(30);
		tablelog.setPrintNull(false);
		tablelog.addColumn("Result");

		//increase score from 40 to 500 due to effect of new road site
		//**********************************************
		EP_setPriorityForBuildingsCoverRoadSite(50, validTarget);
		//**********************************************
		for (Building b : validTarget) {

			//EP_setPriorityForBuildingNotInMapSideBuildings(b, 1000000);//TODO KOBE VA VC BAYAD FARGH KONE

			if (agent.messageSystem.type == Type.NoComunication)
				EX_E_setPriorityForFireNess1(b, 2000);//set high score for fireness1 in Nocomm score
			if (agent.messageSystem.type != Type.NoComunication)
				EX_E_setPriorityForFireNess1(b, 1000);// set ordinary score for fireness1
			
			EX_E_setNegativePriorityForUpdatetime(b, -100);//Negative score for old building
		
			if (b instanceof Center)
				EP_setPriorityForCenters(1000, (Center) b);
			
			EP_setPriorityForDistance(b, -250);
			//EP_setPriorityForBigBuilding(b);//Negative Score
			//EP_setPriorityForUnBurnedIsLands(b, 1000);
			
			EP_setPriorityForUnBurnedRoadSite(b, 200);//FIXME KOBE VA VC
			try {
				if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
					EP_setPriorityForSpread(1000, b.getEstimator(), b);
				else
					EP_setPriorityForSpread(1000, b.getEstimator(), b);
			} catch (Exception e) {
				// TODO: handle exception
			}

			if (b.virtualData[0].isBurning()) {
				EX_setpriorityForBuildingNerarGasStation(b, 500);
				E_setPriorityForEarlyIgnitedBuildings(b, 500);
				E_setPriorityForUnburnedNeighbours(b, 50);
				E_setNegativeScoreForBigBuilding(b,-1000);
			} else {
				P_setPreExtinguishProrityForLargBuildingsNearSmallFireBuilding(b, 100);
				P_setPriorityForBigBuilding(b);
				P_setPriorityForCriticalTempratureBuildings(b, 200);
				P_setpriorityForBuildingNerarGasStation(b, 500);
				if (b instanceof GasStation)
					P_setPriorityForGasStation(1000, (GasStation) b);

			}

			EX_E_setPriorityForRandombld(b, 50);// select random building by sum of Building ID and Fire Brigade ID
			EX_E_setPriorityForZoneIndex(b, 4000, validTarget);//Set priority for building which are near to zone of Fire brigade

		}
		for (Building b : validTarget)
			tablelog.addScore(b.toString(), "Result", b.priority());
		log.logln("\n" + tablelog.getTablarResult("Result"));
	}
	private void P_setpriorityForBuildingNerarGasStation(Building b, int priority) {
		if (b.getTemperature() < 35)
			return;

		for (Building n : b.realNeighbors_Building()) {
			if (n instanceof GasStation) {
				addPriority(b, priority, "PreEx_NearGasStation");

			}
		}

	}

	private void EX_setpriorityForBuildingNerarGasStation(Building b, int priority) {
		for (Building n : b.realNeighbors_Building()) {
			if (n instanceof GasStation) {
				addPriority(b, priority, "Ex_NearGasStation");

			}
		}

	}

	private void EX_E_setPriorityForRandombld(Building b, int i) {
		int Random = agent.model().me().getID().getValue() + b.getID().getValue();
		int x = Random % 10;
		addPriority(b, i * x, "Random Score");
	}

	private void EX_E_setPriorityForZoneIndex(Building b, int priority, List<Building> buildings) {
		if (strategy != SelectStrategy.NONE)
		{
			if(isInMyCluster(b.getEstimator(), b))
				return;
			
			Task myTask = fireBrigade_Task.get(agent.me());
			int CenterX = (int) starCluster.getStarZones()[myTask.getZoneIndex()].getCx();
			int CenterY = (int) starCluster.getStarZones()[myTask.getZoneIndex()].getCy();
			double distance = (Math.hypot(b.getX() - CenterX, b.getY() - CenterY) / 1000);
			double ave = avgdistance(buildings, CenterX, CenterY);
			double sigma = varianse(buildings, CenterX, CenterY, ave);
			double norm = -(distance - ave);
			if (sigma != 0)
				norm /= sigma;
			addPriority(b, (int) (priority * norm), "Distance to cluster zone");
			//addPriority(b,(int) (norm * 1000), "Norm * 1000");

		}
	}

	// only for use of EX_E_setPriorityForZoneIndex
	private double avgdistance(List<Building> building, int CenterX, int CenterY) {
		double x = 0;
		for (Building b : building) {
			x += (Math.hypot(b.getX() - CenterX, b.getY() - CenterY) / 1000);
		}
		return (x / building.size());
	}

	// only for use of EX_E_setPriorityForZoneIndex
	private double varianse(List<Building> building, int CenterX, int CenterY, double avg) {
		int x = 0;
		for (Building b : building) {
			x += Math.pow(((Math.hypot(b.getX() - CenterX, b.getY() - CenterY) / 1000) - avg), 2);
		}
		return Math.sqrt(x);
	}

	private void EX_E_setPriorityForFireNess1(Building b, int i) {
		tablelog.addScore(b.toString(), "fireness", b.getFieryness());
		tablelog.addScore(b.toString(), "vfireness", b.virtualData[0].getFieryness());
		addPriority(b, -1, "FireNess=1");
		if (getCorrecterdFieryness(b) != 1)
			return;
		if (agent.time() - b.updatedtime() > 5)
			return;
		
		addPriority(b, i, "FireNess=1");

	}

	@Override
	public List<Building> getValidTask(Object link) {
		SOSEstimatedFireZone site = (SOSEstimatedFireZone) link;
		ArrayList<Building> res = new ArrayList<Building>();

		log.logln("Target From " + site + site.getSize());
		log.logln("bs: " + site.getOuter());
		log.logln("ns: " + site.getSafeBuilding());

		res.addAll(site.getOuter());
		res.addAll(site.getSafeBuilding());
		if (!SOSConstant.IS_CHALLENGE_RUNNING)
			reset(res);
		filterNeutral(res);
		filterRefugesAndCenters(res);
		filterUnReachableForExitnguish(res);

		return res;
	}

	private boolean canExtinguish(Building building) {
		return (sos.tools.Utils.distance(((Human) agent.me()).getX(), ((Human) agent.me()).getY(), building.x(), building.y()) <= AbstractFireBrigadeAgent.maxDistance);
	}

	protected void filterUnReachableForExitnguish(ArrayList<Building> buildings) {
		log.log("filterUnReachableForExitnguish : \t");

		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building b = iterator.next();

			boolean reachable = isReachable(b.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea(), b);
			if (reachable)
				continue;
			reachable = isReachable(b.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea(), b);
			if (reachable)
				continue;
			log.log(b.getID().getValue() + " \t");
			b.addPriority(0, "Filter Unreachable 1");
			iterator.remove();

			//			if (b.getFireBuilding().getExtinguishableArea().isReallyUnReachableCustom()) {
			//				log.log(b.getID().getValue() + " \t");
			//				b.resetPriority();
			//				b.addPriority(0, "Filter Unreachable 1");
			//				iterator.remove();
			//			}
			//			if (b.getFireBuilding().getExtinguishableArea().getBuildingsShapeInArea().isEmpty() && b.getFireBuilding().getExtinguishableArea().getRoadsShapeInArea().isEmpty()) {
			//				log.error("why it come here????" + b + " both ExtinguishableBuildings and ExtinguishableRoads are empty");
			//				b.resetPriority();
			//				b.addPriority(0, "Filter Unreachable 1");
			//				iterator.remove();
			//			}
		}
		log.logln("");
	}

	private boolean isReachable(ArrayList<ShapeInArea> shapes, Building building) {

		ArrayList<ShapeInArea> temp = new ArrayList<ShapeInArea>();
		for (ShapeInArea sh : shapes) {
//			log.info("\t\t Shape" + sh + "   Area : " + sh.getArea());
			long cost = 0;
			if (sh.getArea() instanceof Building && ((Building) sh.getArea()).virtualData[0].isBurning())
			{
//				log.info("\t\t\t burning building filter");
				continue;

			}
			if (((Human) agent.me()).getAreaPosition().equals(sh.getArea()) && canExtinguish(building)) {
				cost = 0;
			} else {
				temp.clear();
				temp.add(sh);
				cost = agent.move.getWeightTo(temp, StandardMove.class);
				if (cost >= MoveConstants.UNREACHABLE_COST) {
//					log.info("\t\t\t Unreachable");
					continue;
				}
			}
			if (cost < MoveConstants.UNREACHABLE_COST)
				return true;
		}
		return false;

	}

	protected void filterRefugesAndCenters(ArrayList<Building> buildings) {
		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building building = iterator.next();
			if ((building instanceof Refuge || building instanceof Center) && !building.isBurning())
			{
				building.resetPriority();
				building.addPriority(0, "Filter Ref Center");
				iterator.remove();
			}
		}

	}

	protected void filterNeutral(ArrayList<Building> buildings) {
		for (Iterator<Building> iterator = buildings.iterator(); iterator.hasNext();) {
			Building building = iterator.next();
			if (building instanceof GasStation)
			{
				if (building.virtualData[0].getTemperature() < 5)
				{
					building.resetPriority();
					building.addPriority(0, "Filter Neutral");
					iterator.remove();
				}
				continue;
			}
			if (building.virtualData[0].getTemperature() < 35)//bekhatere inke olaviyat migire va too filter spread kharrab mikone karo
			{
				building.resetPriority();
				building.addPriority(0, "Filter Neutral");
				iterator.remove();
				continue;
			}
		}
	}

	protected void EX_E_setNegativePriorityForUpdatetime(Building b, int priority) {
		tablelog.addScore(b.toString(), "MessageTime", b.getLastMsgTime());
		tablelog.addScore(b.toString(), "SenseTime", b.getLastSenseTime());
		
		if ((getCorrecterdFieryness(b) == 2 || getCorrecterdFieryness(b) == 3) && agent.time() - b.updatedtime() > 6) {
			addPriority(b, priority * getCorrecterdFieryness(b) * (agent.time() - b.updatedtime()), "updatetimeScore");
		}
		else if ((getCorrecterdFieryness(b) == 1) && agent.time() - b.updatedtime() > 10){
			addPriority(b, priority * getCorrecterdFieryness(b) * (agent.time() - b.updatedtime()), "updatetimeScore");
		}else if (agent.time() - b.updatedtime() > 5)
			addPriority(b, priority* 10, "updatetimeScore");
	}

	
	protected void P_setPriorityForBigBuilding(Building b) {
		if (b.virtualData[0].getTemperature() > 30) {		
			if (b.getTotalArea() > ((FireWorldModel)agent.model()).getBigBuildingArea()) {
				if (b.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance) {
					addPriority(b, 1000, "PreEx Area ");
				}
				else {
					addPriority(b, 700, "PreEx Area ");
				}
			}
		}

	}

	protected void EX_E_setPriorityForFireNess(Building b, int i) {
		ScoreConstant constant = new ScoreConstant();
		tablelog.addScore(b.toString(), "fireness", b.virtualData[0].getFieryness());
		addPriority(b, i * constant.fireness[b.virtualData[0].getFieryness()], "FireNessP");
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
	protected void E_setNegativeScoreForBigBuilding(Building b, int i){
		if (b.getTotalArea() < ((FireWorldModel)agent.model()).getBigBuildingArea())
			return;
		
		if (b.virtualData[0].getTemperature() < 500 )
			return;
				
		if (getCorrecterdFieryness(b)==1 || getCorrecterdFieryness(b)==2 )
			addPriority(b, i, "bigBuilding");
		else
			addPriority(b, i/2, "bigBuilding");
		
	}

	protected void P_setPreExtinguishProrityForLargBuildingsNearSmallFireBuilding(Building b, int priority) {
		double num = 0;
		if (b.getTotalArea()<((FireWorldModel)agent.model()).getBigBuildingArea())
			return;
		for (Building n : b.realNeighbors_Building()) {
			if (n.virtualData[0].isBurning()) {
				double d = b.getGroundArea()/ n.getGroundArea();
				if (d >= 3d) {
					num += d / 3;
				}
			}
		}
		addPriority(b, (int) (num * priority), "LARGE_BUILDING_NEAR_SMALL");
	}

	protected void EP_setPriorityForDistance(Building b, int priority) {
		//TODO position
		//		ShapeInArea pos = ((FireBrigadeAgent) agent).positioning.getPosition(b);
		//		if (pos != null) {
		//			long cost = agent.move.getWeightTo(((FireBrigadeAgent) agent).positioning.getPosition(b).getArea(), StandardMove.class);
		//			b.addPriority(agent.move.getMovingTimeFrom(cost) * priority, "Distance");
		//		} else
		//		{
		//			b.addPriority(100 * priority, "Null position");
		//		}
		addPriority(b, (int) (positioningcostEvaluator.PositioningTime(b) * priority), "Distance");

	}

	@SuppressWarnings("unchecked")
	protected void P_setPriorityForCriticalTempratureBuildings(Building n, int priority) {

		if (!(n.virtualData[0].getTemperature() > 30 && n.virtualData[0].getTemperature() < 50))
			return;

		if (n.isMapSide())
			return;

		if (n.virtualData[0].getTemperature() <= 40)
			return;

		if (!agent.getVisibleEntities(Building.class).contains(n))
			return;

		if (n.distance((FireBrigade) agent.me()) > AbstractFireBrigadeAgent.maxDistance)
			return;

		if (n.getTotalArea() > 3000)
			priority *= 2;

		addPriority(n, priority, "CRITICAL_TEMPERATURE");

	}

	protected void EP_setPriorityForCenters(int priority, Center c) {
		if (agent.messageSystem.isUsefulCenter(c))
			if (c.virtualData[0].getTemperature() >= 10) {
				addPriority(c, priority, "CENETER");
			}
	}

	
	
	protected void EP_setPriorityForGasStation(int priority, GasStation c) {
		if (c.virtualData[0].getTemperature() >= 10 && (c.getFieryness() == 0 || c.getFieryness() == 4)) {
			addPriority(c, priority, "GasStation");
		}
	}
	
	protected void P_setPriorityForGasStation(int priority, GasStation c) {
		if (c.virtualData[0].getTemperature() >= 20 && (c.getFieryness() == 0 || c.getFieryness() == 4)) {
			addPriority(c, priority, "GasStation");
		}
	}

	protected void EP_setPriorityForBuildingNotInMapSideBuildings(Building b, int priority) {

		if (agent.messageSystem.type == Type.NoComunication) {
			if (!b.isMapSide())
				addPriority(b, priority, "MAP_SIDE");
			return;
		}
		//		if()
		else if (!b.isMapSide() || b.getFireBuilding().buildingBlock().isFireNewInBuildingBlock())
			addPriority(b, priority, "MAP_SIDE");

	}
	private boolean isInMyCluster(SOSEstimatedFireZone site,Building b){
		Task myTask = fireBrigade_Task.get(agent.me());

		FireStarZone mycluster = starCluster.getStarZones()[myTask.getZoneIndex()];

		for (Building bb : site.getAllBuildings())
			if (mycluster.getZoneBuildings().contains(bb)) 
				return true;

		return false;
	}
	protected void EP_setPriorityForSpread(int priority, SOSEstimatedFireZone site, Building b) {
		// if fire zone is not in fire brigade zone, remove spread score for this fire brigade
		if (strategy != SelectStrategy.NONE)
		{
			if(!isInMyCluster(site, b))
				return;
		}
		//***************************************
		if (b.virtualData[0].getFieryness() == 3 && agent.model().time() - b.updatedtime() > 4)
			return;

		//******************************************
		int SPREAD_ANGLE = 60;

		//		if (((FireWorldModel) agent.model()).getInnerOfMap().contains(site.getCenterX(), site.getCenterY()))
		//			SPREAD_ANGLE = 120;
		//		else
		//			SPREAD_ANGLE = 90;

		double x1, y1;
		Pair<Double, Double> spread = site.spread;
		x1 = spread.first();
		y1 = spread.second();

		double length = Math.hypot(x1, y1);

		priority = (int) (priority * length);

		double a3 = Tools.getAngleBetweenTwoVector(x1, y1, b.getX() - site.getCenterX(), b.getY() - site.getCenterY());
		int x = (int) (Math.abs(a3) / 30d);
		log.info("Building=" + b + "\t zone=" + site + "\tX=" + x + "\ta3=" + a3);
		int coef = 1;
		if (Tools.isBigFire(site))
			coef = 2;
		if (site.getOuter().size() > 30)
		{
			coef = 30;
		}
		tablelog.addScore(b.toString(), "SPREAD X", x);
		//		tablelog.addColumn("SPREAD");
		if (!(a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30))
			addPriority(b, (coef * priority / (x + 1)), ("SPREAD"));
		else
			addPriority(b, 0, ("SPREAD"));

		//		if (a3 > 2 * SPREAD_ANGLE && site.getOuter().size() > 30) {
		//			//addPriority(b, -2 * coef * priority, "FILTER_SPREAD");
		//		}
	}

	public boolean isBigMap() {
		if (agent.getMapInfo().isBigMap() || agent.getMapInfo().isMediumMap())
			return true;
		return false;
	}

	protected void EP_setPriorityForBuildingsCoverRoadSite(int priority, List<Building> buildings) {
		for (BuildingBlock bb : ((FireWorldModel) agent.model()).buildingBlocks()) {
			if (bb.isFireNewInBuildingBlock())
				for (Building b : buildings) {
					if (bb.insideCoverBuildings().contains(b))
						addPriority(b, priority, "Cover Roadsite");
				}
		}
	}

	protected void EP_setPriorityForUnBurnedIsLands(Building b, int priority) {
		if (!b.getFireBuilding().island().isFireNewInIsland())
			return;
		double coef = 2;
		if (!b.getFireBuilding().island().insideCoverBuildings().contains(b))
			coef = 1.5;

		if (b.virtualData[0].getTemperature() < 20)
			priority /= 2;
		if (b.getFireBuilding().island().isImportant()) {
			addPriority(b, (int) (coef * priority), "UNBURNED_ISLAND");
		}
		else {
			addPriority(b, (int) (coef * priority / 2), "UNBURNED_ISLAND");
		}
	}

	protected void EP_setPriorityForUnBurnedRoadSite(Building b, int priority) {
		if (!b.virtualData[0].isBurning() || b.getTotalArea()<((FireWorldModel)agent.model()).getBigBuildingArea())
			return;
		if (!b.getFireBuilding().buildingBlock().isFireNewInBuildingBlock())
			return;
		double coef = 2;
		if (!b.getFireBuilding().buildingBlock().insideCoverBuildings().contains(b))
			coef = 1.5;

//		if (b.virtualData[0].getTemperature() < 20)
//			priority /= 2;
		
		if (b.getFireBuilding().buildingBlock().isImportant()) {
			addPriority(b, (int) (coef * priority), "UNBURNED_ROADSIDE");
		}
		else {
			addPriority(b, (int) (coef * priority / 2), "UNBURNED_ROADSIDE");
		}
	}

	private int getCorrecterdFieryness(Building b){
		if(b.updatedtime() >= (agent.time()-1))
			return b.getFieryness();
		
		return b.virtualData[0].getFieryness();
	}
	protected void E_setPriorityForEarlyIgnitedBuildings(Building b, int priority) {
		if (getCorrecterdFieryness(b) != 1)
			return;
		
		if (b.virtualData[0].isBurning() && b.virtualData[0].isExtinguishableInOneCycle(AbstractFireBrigadeAgent.maxPower)) {
			if (b.distance((FireBrigade) agent.me()) < AbstractFireBrigadeAgent.maxDistance) {
				addPriority(b, priority, "EARLY IGNITED");
			}
			else {
				addPriority(b, priority / 2, "EARLY IGNITED");
			}
		}
	}

	protected void computs() {
		((FireWorldModel) agent.model()).updateBuildingBlocks();
	}

	public static boolean isRighTurn(double x1, double y1, double x2, double y2) {
		double t = (x1 * y2) - (y1 * x2);
		return t < 0;
	}

	protected void EP_setPriorityForBigBuilding(Building b) {//TODO
		//		if (b.getGroundArea() > 3000) {
		//			if (b.virtualData[0].getTemperature() > 100)
		//				b.addPriority(-b.getGroundArea() * 2, "BigArea");
		//			if (b.virtualData[0].getTemperature() < 50)
		//				if (b.distance(model.owner().me()) < AbstractFireBrigadeAgent.maxDistance)
		//					b.addPriority(1300, "BigAreaMinTemp");
		//
		//		}
	}

	private void filterInConvexed(SOSEstimatedFireZone fireSite) {
		//		if (bs.size() > 10) {
		//			Shape convex;
		//			if (bs.size() < 25)
		//				convex = fireSite.getConvex().getScaleConvex(0.6f).getShape();
		//			else
		//				convex = fireSite.getConvex().getScaleConvex(0.8f).getShape();
		//			Building building;
		//			for (Iterator<Building> iterator = bs.iterator(); iterator.hasNext();) {
		//				building = iterator.next();
		//				if (convex.contains(building.getX(), building.getY()))
		//					iterator.remove();
		//			}
		//			for (Iterator<Building> iterator = ns.iterator(); iterator.hasNext();) {
		//				building = iterator.next();
		//				if (convex.contains(building.getX(), building.getY()))
		//					iterator.remove();
		//			}
		//
		//		}

	}

	private void addPriority(Building b, int priority, String Comment) {
		tablelog.addScore(b.toString(), Comment, priority);
		b.addPriority(priority, Comment);
	}
}

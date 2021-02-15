package sos.fire_v2.Scape;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.misc.Pair;
import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.move.MoveConstants;
import sos.base.move.types.StandardMove;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.SOSActionException;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.base.util.sosLogger.SOSLoggerSystem.OutputType;
import sos.fire_v2.decision.FireInformationModel;
import sos.fire_v2.target.SOSSelectTarget.SelectStrategy;
import sos.fire_v2.target.Tools;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.definitions.feedback.SOSFeedback;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

public class NoCommScapeFire extends SOSIState<FireInformationModel> {
	private boolean shouldescape = false;
	protected SOSAgent agent;
	protected SelectStrategy strategy = SelectStrategy.NONE;
	private int escapetime;
	private SOSLoggerSystem log;
	private ClusterData destination = null;
	private ArrayList<ClusterData> reachableClusters = new ArrayList<ClusterData>();
	private ArrayList<Road> roads = new ArrayList<Road>();

	public NoCommScapeFire(FireInformationModel infoModel) {
		super(infoModel);
		log = new SOSLoggerSystem(infoModel.self().me(), "marjanEscape", true, OutputType.File, true, true);
		infoModel.self().sosLogger.addToAllLogType(log);
		reachableClusters = (ArrayList<ClusterData>) infoModel.getModel().searchWorldModel.getAllClusters();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {

		if (infoModel.getAgent().messageSystem.type != Type.NoComunication) {
			return null;
		}
		if (infoModel.getTime() - escapetime < 25) {
			shouldescape = false;
			return null;
		}

		/////////******CENTER************/////////
		if (gatheringAreaDistance() < 50000) {
			log.info("near gathering area");
			if (escape()) {
				log.info("escape true");
				shouldescape = true;
			}
			if (shouldescape) {
				log.info("shouldescape true");

				if (stillescape() && destination == null) {
					log.info("stillescape true and have no destination");
					destination = getBestClusterForEscape();
					return getMoveTask();

				}

				if (stillescape() && destination != null) {
					log.info("stillescape true and has a destination");
					log.info("destination: " + destination.getNearestBuildingToCenter());
					return getMoveTask();
				}

				else if (!stillescape()) {
					escapetime = infoModel.getTime();
					log.info("finish escape");
					destination = null;
					return null;
				}

			}

		}
		/////////******NOT IN CENTER*********///////
		else {
			log.info("I am NOT near gathering area ");
			if (escape()) {
				log.info("	escape true");
				shouldescape = true;
			}
			if (shouldescape) {
				log.info("shouldescape true");
				if (stillescape() && destination == null) {
					log.info("stillescape true & have no destination");
					log.info("move to gathering area");
					return new StandardMoveToAreaTask(new AreaTarget(infoModel.self().newSearch.strategyChooser.noCommunication.getGatheringArea()), infoModel.getTime());
				}
				if (stillescape() && destination != null) {
					log.info("stillescape true and has a destination");
					log.info("destination: " + destination.getNearestBuildingToCenter());
					return getMoveTask();
				}

				else if (!stillescape()) {
					escapetime = infoModel.getTime();
					log.info("finish escape");
					destination = null;
					return null;
				}

			}

		}
		log.info("return null");
		return null;

	}

	private ClusterData getBestClusterForEscape() {
		double minSpread = Integer.MAX_VALUE;
		double spreadSum = -100;
		ClusterData bestCluster = null;
		for (ClusterData cl : infoModel.getModel().searchWorldModel.getAllClusters()) {
			spreadSum = -100;
			for (Building bl : cl.getBuildings()) {
				if (bl.getEstimator() != null)
					spreadSum += getSpread(bl, bl.getEstimator());
			}
			spreadSum = spreadSum / cl.getBuildings().size();
			log.info("Cluster: " + cl.getIndex() + " Building : " + cl.getNearestBuildingToCenter() + "  spreadSum: " + spreadSum);
			if (spreadSum < minSpread) {
				minSpread = spreadSum;
				bestCluster = cl;
			}
		}
		log.info("Best Cluster: " + bestCluster.getIndex() + " Building : " + bestCluster.getNearestBuildingToCenter() + "  minSpread: " + minSpread);
		return bestCluster;
	}

	private SOSTask<?> getMoveTask() {
		long cost = infoModel.self().move.getWeightTo(destination.getNearestBuildingToCenter(), StandardMove.class);
		log.info("cost: "+cost);
		if (cost < MoveConstants.UNREACHABLE_COST) {
			log.info("Destination: " + destination.getNearestBuildingToCenter() + "is reachable");
			if (!infoModel.self().getVisibleEntities(Building.class).contains(destination.getNearestBuildingToCenter()))

				return new StandardMoveToAreaTask(new AreaTarget(destination.getNearestBuildingToCenter().getAreaPosition()), infoModel.getTime());

			else if (stillescape()) {
				log.info("I am near my destination but stillescape is true");
				log.info("select another destination");
				double min_Spread = Integer.MAX_VALUE;
				double spread_Sum = 0;
				ClusterData best_Cluster = null;
				for (ClusterData cl : infoModel.getModel().searchWorldModel.getAllClusters()) {
					spread_Sum = 0;
					for (Building bl : cl.getBuildings()) {
						if (bl.isBurning() && bl.getEstimator() != null)
							spread_Sum += getSpread(bl, bl.getEstimator());
					}
					spread_Sum = spread_Sum / cl.getBuildings().size();
					log.info("Cluster : " + cl.getIndex() + "   spread_sum: " + spread_Sum);
					if (spread_Sum < min_Spread) {
						if (!cl.equals(destination)) {
							min_Spread = spread_Sum;
							best_Cluster = cl;
						}
					}
				}

				log.info("Best Cluster: " + best_Cluster.getIndex() + " Building : " + best_Cluster.getNearestBuildingToCenter() + "  minSpread: " + min_Spread);
				destination = best_Cluster;
				log.info("new Destination: " + destination.getNearestBuildingToCenter());
				return getMoveTask();
			}
		}
		else {
			log.info("destination is not reachable");
			double min_Spread = Integer.MAX_VALUE;
			double spread_Sum = 0;
			ClusterData best_Cluster = null;
			reachableClusters.remove(destination);
			if(reachableClusters.size()==0){
				escapetime=infoModel.getTime();
				log.info("escape finish");
				return null;
			}
			else{
				for (ClusterData cl : reachableClusters) {
					spread_Sum = 0;
					for (Building bl : cl.getBuildings()) {
						if (bl.isBurning() && bl.getEstimator() != null)
							spread_Sum += getSpread(bl, bl.getEstimator());
					}
					spread_Sum = spread_Sum / cl.getBuildings().size();
					log.info("Cluster : " + cl.getIndex() + "   spread_sum: " + spread_Sum);
					if (spread_Sum < min_Spread) {
						if (!cl.equals(destination)) {
							min_Spread = spread_Sum;
							best_Cluster = cl;
						}
					}
				}
				log.info("Best Cluster: " + best_Cluster.getIndex() + " Building : " + best_Cluster.getNearestBuildingToCenter() + "  minSpread: " + min_Spread);
				destination = best_Cluster;
				log.info("new Destination: " + destination.getNearestBuildingToCenter());
				return getMoveTask();
			
		}
		}
		log.info("getMoveTask return null");
		return null;

	}

	public int Distance(int ex, int ey, int x, int y) {
		return (int) Math.sqrt(Math.pow(ex - x, 2) + Math.pow(ey - y, 2));
	}

	private int gatheringAreaDistance() {
		int xDistance = (infoModel.self().me().getX()) - (infoModel.self().newSearch.strategyChooser.noCommunication.getGatheringArea().getX());
		int yDistance = (infoModel.self().me().getY()) - (infoModel.self().newSearch.strategyChooser.noCommunication.getGatheringArea().getY());
		int distance = (int) Math.sqrt(Math.pow(xDistance, 2) + Math.pow(yDistance, 2));
		return distance;
	}

	private boolean escape() {
		for (Building bl : infoModel.self().getVisibleEntities(Building.class)) {
			if (!(bl.getFieryness() == 2 || bl.getFieryness() == 3 || bl.getFieryness() == 8))
				return false;
		}

		return true;
	}

	private boolean stillescape() {
		boolean f0 = false;
		int unburn = 0;
		for (Building bl : infoModel.self().getVisibleEntities(Building.class)) {
			if (bl.getFieryness() == 0)
				unburn++;
		}
		if ((float)unburn / (float) infoModel.self().getVisibleEntities(Building.class).size() > 0.4)
			return false;
		return true;

	}

	public Pair<Double, Double> spread = new Pair<Double, Double>(0.0, 0.0);

	private int getSpread(Building b, SOSEstimatedFireZone site) {

		int SPREAD_ANGLE = 60;

		double x1, y1;
		site.computeSpread();
		Pair<Double, Double> spread = site.spread;
		x1 = spread.first();
		y1 = spread.second();

		double length = Math.sqrt(x1 * x1 + y1 * y1);

		double a3 = Tools.getAngleBetweenTwoVector(x1, y1, b.getX() - site.getCenterX(), b.getY() - site.getCenterY());
		int c = (int) (Math.abs(a3) / 30d);
		return c;
	}

	@Override
	public void giveFeedbacks(List<SOSFeedback> feedbacks) {
		// TODO Auto-generated method stub

	}

	@Override
	public void skipped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void overTaken() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
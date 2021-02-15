package sos.ambulance_v2.decision.states.helpstate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.decision.AmbulanceDecision;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.message.structure.MessageConstants.Type;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.SaveHumanTask;

/**
 * @author Reyhaneh
 */
public class NoComHelpStrategy extends HelpStrategy {

	private AmbulanceTeamAgent ambulance = null;
	private AmbulanceDecision ambDecision;
	ArrayList<ClusterData> clusters;
	HashMap<ClusterData, Vector<Human>> validCivPerCluster;
	boolean isCenterDesiding = false;
	int validCivilNumber = 0;

	public NoComHelpStrategy(AmbulanceInformationModel infoModel, SOSIState<AmbulanceInformationModel> state) {
		super(infoModel, state);
		ambulance = infoModel.getAmbulance();
		this.ambDecision = infoModel.getAmbulance().ambDecision;
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {

		infoModel.getLog().info("$$$$$ NoCommunicationHelpStartegyState $$$$$");
		clusters = new ArrayList<ClusterData>(infoModel.getModel().searchWorldModel.getAllClusters());
		ArrayList<ClusterData> removed = new ArrayList<ClusterData>();
		for (ClusterData cd : clusters)
			if (cd.isCoverer())
				removed.add(cd);
		clusters.removeAll(removed);

		Human target = null;
		validCivilNumber = 0;
		infoModel.getLog().info("----- find valid targets -----");
		findValidTargets();

		ClusterData best = getBestWeightCluster();
		infoModel.getLog().info("------- best cluster -------" + best);
		if (!validCivPerCluster.containsKey(best)) {
			infoModel.getLog().info("validCivPerCluster don't contains best cluster ");
			infoModel.getLog().info("$$$$$ Skipped from NoCommunicationHelpStartegyState $$$$$");
			return null;
		}

		ambDecision.getTargets().addAll(validCivPerCluster.get(best));
		ambDecision.assignPrioritytoTargets();
		ambDecision.makePriorityListFromTargets();
		ambDecision.assignCosttoTargets();

		if (infoModel.getAgent().messageSystem.type == Type.NoComunication)
			target = ambDecision.getMinCostTarget();

		if (target == null) {
			infoModel.getLog().info("$$$$$ Skipped from NoComHelpStrategyState $$$$$");
			return null;
		}

		AmbulanceUtils.updateATtarget(target, infoModel.getATEntity(), state);
		infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     target =" + target);
		ambDecision.dclog.info(" WorkTarget = " + infoModel.getATEntity().getWork().getTarget());
		ambulance.lastState = " NoComHelpStrategyState ";

		ambulance.currentSaveHumanTask = new SaveHumanTask(target, infoModel.getTime());
		return ambulance.currentSaveHumanTask;

	}

	private ClusterData getBestWeightCluster() {
		ArrayList<Pair<ClusterData, Float>> clusterWeights = new ArrayList<Pair<ClusterData, Float>>();
		for (Map.Entry<ClusterData, Float> e : infoModel.getAmbulance().lowAndNocomClustersDis.entrySet()) {
			infoModel.getLog().info("------------------------------------------------");
			float weight = e.getValue();
			if (validCivPerCluster.containsKey(e.getKey())) {
				int coef = validCivPerCluster.get(e.getKey()).size() / 10;
				coef = coef > 0 ? coef + 1 : coef;
				weight += (coef * (validCivPerCluster.get(e.getKey()).size() / (float) validCivilNumber));
				infoModel.getLog().info(e.getKey() + "  weight=  " + weight + "----> dis = " + e.getValue()
						+ "  validCivs =  " +
						validCivPerCluster.get(e.getKey()).size() + " coef = " + coef);
			}
			else
				infoModel.getLog().info(e.getKey() + "  weight=  " + weight + "----> dis = " + e.getValue()
						+ "  validCivs =  0");
			clusterWeights.add(new Pair<ClusterData, Float>(e.getKey(), weight));
		}

		Collections.sort(clusterWeights, new Comparator<Pair<ClusterData, Float>>() {

			@Override
			public int compare(Pair<ClusterData, Float> p1, Pair<ClusterData, Float> p2) {

				if (p1.second() > p2.second())
					return -1;
				else if (p1.second() < p2.second())
					return 1;
				return 0;
			}
		});

		return clusterWeights.get(0).first();
	}

	private void findValidTargets() {
		if (ambDecision.getAmbulances() == null)
			ambDecision.findValidAmbulances();
		ambDecision.setXMLLogging(true);
		ambDecision.findValidTargets(isCenterDesiding);
		ambDecision.ferociousFilterTargets();

		validCivilNumber = Math.max(1, ambDecision.getTargets().size());
		infoModel.getLog().info("validCivilNumber:" + validCivilNumber);
		validCivPerCluster = new HashMap<ClusterData, Vector<Human>>();
		for (Human human : ambDecision.getTargets()) {
			if (!(human.getAreaPosition() instanceof Building))
				continue;
			for (ClusterData cd : clusters) {
				if (cd.getBuildings().contains(human.getAreaPosition())) {
					Vector<Human> civs;
					if (validCivPerCluster.containsKey(cd)) {
						civs = validCivPerCluster.get(cd);
						validCivPerCluster.put(cd, validCivPerCluster.get(cd));
					}
					else
						civs = new Vector<Human>();

					civs.add(human);
					validCivPerCluster.put(cd, civs);
					infoModel.getLog().info("**********************************************\n CLUSTER" +
							cd.toString() + ":" + human.toString());
					//break;

				}

			}

		}
		ambDecision.getTargets().clear();

	}

}

package sos.ambulance_v2.decision.states.helpstate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import rescuecore2.misc.Pair;
import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.decision.LowCommunicationAmbulanceDecision;
import sos.base.entities.Building;
import sos.base.entities.VirtualCivilian;
import sos.search_v2.tools.cluster.ClusterData;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

/**
 * @author Reyhaneh
 */

public class lowComHelpStrategy extends HelpStrategy {

	private LowCommunicationAmbulanceDecision lowComAmbDecision;
	HashMap<ClusterData, Vector<VirtualCivilian>> validCivPerCluster;
	ArrayList<ClusterData> clusters;
	int validCivilNumber = 0;

	public lowComHelpStrategy(AmbulanceInformationModel infoModel, SOSIState<AmbulanceInformationModel> state) {
		super(infoModel, state);
		this.lowComAmbDecision = infoModel.getAmbulance().lowComAmbDecision;
		validCivPerCluster = new HashMap<ClusterData, Vector<VirtualCivilian>>();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {

		infoModel.getLog().info("$$$$$ LowCommunicationHelpStartegyState $$$$$");
		clusters = new ArrayList<ClusterData>(infoModel.getModel().searchWorldModel.getAllClusters());
		ArrayList<ClusterData> removed = new ArrayList<ClusterData>();
		for (ClusterData cd : clusters)
			if (cd.isCoverer())
				removed.add(cd);
		clusters.removeAll(removed);
		validCivilNumber = 0;
		infoModel.getLog().info("----- find valid targets -----");
		findValidTargets();

		ClusterData best = getBestWeightCluster();
		infoModel.getLog().info("------- best cluster -------" + best);
		if (!validCivPerCluster.containsKey(best)) {
			infoModel.getLog().info("validCivPerCluster don't contains best cluster ");
			infoModel.getLog().info("$$$$$ Skipped from LowCommunicationHelpStartegyState $$$$$");
			return null;
		}

		lowComAmbDecision.Virtualtargets.addAll(validCivPerCluster.get(best));

		lowComAmbDecision.assignPrioritytoTargets();
		lowComAmbDecision.makePriorityListFromTargets();
		lowComAmbDecision.assignCosttoTargets();

		VirtualCivilian target = lowComAmbDecision.getMinCostTarget();

		if (target == null) {
			infoModel.getLog().info("$$$$$ Skipped from LowCommunicationHelpStartegyState $$$$$");
			return null;
		}
		infoModel.getAmbulance().oldVirtualTarget = target;
		infoModel.getAmbulance().lastState = " LowCommunicationHelpStartegyState ";
		infoModel.getLog().info(" AT =" + infoModel.getAgent().getID() + "     VirtualTarget= =" + target);

		return new StandardMoveToAreaTask(new AreaTarget(infoModel.getAmbulance().oldVirtualTarget.getPosition()), infoModel.getTime());

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
		lowComAmbDecision.reset();
		lowComAmbDecision.findValidAmbulances();
		lowComAmbDecision.findValidTargets();
		lowComAmbDecision.filterTargetsForAfterCivilianSearch();
		validCivPerCluster = new HashMap<ClusterData, Vector<VirtualCivilian>>();
		validCivilNumber = Math.max(1, lowComAmbDecision.Virtualtargets.size());
		infoModel.getLog().info("validCivilNumber:" + validCivilNumber);

		for (VirtualCivilian vc : lowComAmbDecision.Virtualtargets) {
			if (!(vc.getPosition() instanceof Building))
				continue;
			for (ClusterData cd : clusters) {
				if (cd.getBuildings().contains(vc.getPosition())) {
					Vector<VirtualCivilian> civs;
					if (validCivPerCluster.containsKey(cd)) {
						civs = validCivPerCluster.get(cd);
						validCivPerCluster.put(cd, validCivPerCluster.get(cd));
					}
					else
						civs = new Vector<VirtualCivilian>();

					civs.add(vc);
					validCivPerCluster.put(cd, civs);
					infoModel.getLog().info("**********************************************\n CLUSTER" + cd.toString()
							+ ":" + vc.toString());
					//	break;
				}

			}

		}
		lowComAmbDecision.Virtualtargets.clear();

	}

}

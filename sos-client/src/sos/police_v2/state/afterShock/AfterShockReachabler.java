package sos.police_v2.state.afterShock;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.GasStation;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.PoliceAbstractState;
import sos.police_v2.state.preCompute.PoliceForceTask;

public class AfterShockReachabler extends PoliceAbstractState {

	private Shape clusterShape;
	private int lastAfterShock = 0;
	private boolean isDone = true;
	private ArrayList<Refuge> refugeInMyCluster;
	private ArrayList<Human> AgentInMyCluster;
	private ArrayList<GasStation> gasInMyCluster;

	public AfterShockReachabler(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		clusterShape = model().searchWorldModel.getClusterData().getConvexShape();
	}

	private ArrayList<GasStation> setGasInMyCluster() {
		ArrayList<GasStation> list = new ArrayList<GasStation>();
		for (Building building : model().searchWorldModel.getClusterData().getBuildings())
			if (building instanceof GasStation)
				list.add((GasStation) building);
		return list;
	}

	private ArrayList<Refuge> setRefugeInCluster() {
		ArrayList<Refuge> list = new ArrayList<Refuge>();
		for (Building building : model().searchWorldModel.getClusterData().getBuildings())
			if (building instanceof Refuge)
				list.add((Refuge) building);
		return list;
	}

	@Override
	public void act() throws SOSActionException {
		for (PoliceForceTask police : model().getPoliceForSpecialTask()) {
			if (police.getRealEntity().equals(agent.me())) {
				log.info(" i am special police so dont send act in updateClusteState");
				return;
			}
		}
		if (model().getLastAfterShockTime() != lastAfterShock) {
			isDone = false;
			lastAfterShock = model().getLastAfterShockTime();
			refugeInMyCluster = setRefugeInCluster();
			gasInMyCluster = setGasInMyCluster();
			if ((agent.messageSystem.type != Type.NoComunication) && (agent.messageSystem.type != Type.LowComunication))
				AgentInMyCluster = setAgentInCluster();
		}
		if (!isDone) {
			log.debug("acting as aftershockj reacchabler");
			checkDoneRefuge(refugeInMyCluster);
			if (refugeInMyCluster.size() > 0)
				makeReachableTo(refugeInMyCluster);
			checkDoneGas(gasInMyCluster);
			if (gasInMyCluster.size() > 0)
				makeReachableTo(gasInMyCluster);
			if ((agent.messageSystem.type != Type.NoComunication) && (agent.messageSystem.type != Type.LowComunication)) {
				checkDoneAgent(AgentInMyCluster);
				if (AgentInMyCluster.size() > 0)
					makeReachableTo(AgentInMyCluster);
			}
			isDone = true;
		}

	}

	private ArrayList<Human> setAgentInCluster() {
		ArrayList<Human> list = new ArrayList<Human>();
		for (AmbulanceTeam at : model().ambulanceTeams())
			if (clusterShape.contains(new Point2D.Double(at.getX(), at.getY())))
				list.add(at);
		for (FireBrigade fb : model().fireBrigades())
			if (clusterShape.contains(new Point2D.Double(fb.getX(), fb.getY())))
				list.add(fb);
		for (AmbulanceTeam at : model().ambulanceTeams())
			if (clusterShape.contains(new Point2D.Double(at.getX(), at.getY())))
				list.add(at);
		log.debug("list of agent that set for opening after shock =" + list);
		return list;
	}

	private void checkDoneRefuge(ArrayList<Refuge> list) {
		ArrayList<Refuge> removeList = new ArrayList<Refuge>();
		for (Refuge entity : list) {
			if (isReachableTo(entity))
				removeList.add(entity);
		}
		list.removeAll(removeList);
	}

	private void checkDoneGas(ArrayList<GasStation> list) {
		ArrayList<GasStation> removeList = new ArrayList<GasStation>();
		for (GasStation entity : list) {
			if (isReachableTo(entity))
				removeList.add(entity);
		}
		list.removeAll(removeList);
	}

	private void checkDoneAgent(ArrayList<Human> list) {
		ArrayList<Human> removeList = new ArrayList<Human>();
		for (Human entity : list) {
			if (isReachableTo(entity)) {
				removeList.add(entity);
				continue;
			}
			if (entity.isPositionDefined() && entity.getAreaPosition().equals(agent.me().getAreaPosition())) {
				removeList.add(entity);
				continue;
			}
		}
		list.removeAll(removeList);
	}

}

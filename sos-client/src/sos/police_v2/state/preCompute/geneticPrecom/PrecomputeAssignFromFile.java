package sos.police_v2.state.preCompute.geneticPrecom;

import java.util.ArrayList;
import java.util.List;

import sos.base.entities.PoliceForce;
import sos.base.precompute.PreComputeFile;
import sos.police_v2.PoliceForceAgent;

public class PrecomputeAssignFromFile implements PreComputeFile {
	private static final long serialVersionUID = 18646546518451324L;
	/**
	 * 
	 */
	int[] assign;

	public PrecomputeAssignFromFile() {
	}
	
	public void setAssign(List<PoliceForce> assignList) {
		assign = new int[assignList.size()];
		for (int i = 0; i < assignList.size(); i++) {
			assign[i] = assignList.get(i).getPoliceIndex();
		}
	}

	public List<PoliceForce> getAssign() {
		ArrayList<PoliceForce> result = new ArrayList<PoliceForce>();
		for (int i = 0; i < assign.length; i++) {
			PoliceForce force = PoliceForceAgent.currentAgent().model().policeForces().get(assign[i]);
			result.add(force);
		}
		return result;
	}

	@Override
	public boolean isValid() {

		return true;
	}
}

package sos.police_v2.state.intrupt;

import java.util.ArrayList;
import java.util.HashSet;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.police_v2.PoliceForceAgent;

public class SearchNearSmallFireIntrupt extends PoliceAbstractIntruptState {

	
	private SOSEstimatedFireZone target;
	private int startTime;

	
	//this intrupt because precompute task will took about more than 60 cycle! if we can extingouish fire in first steps we will the winner.
	public SearchNearSmallFireIntrupt(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public boolean canMakeIntrupt() {
		if (target != null && isSmallFire(target) && isSearched(target))
			return true;
		target = null;
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pair : model().getFireSites()) {
			SOSEstimatedFireZone zone = pair.second();
			if (zone.isDisable())
				continue;
			if (isSmallFire(zone))
				continue;

			HashSet<Building> visible = new HashSet<Building>(agent.getVisibleEntities(Building.class));
			for (Building b : zone.getAllBuildings()) {
				if (visible.contains(b)) {
					target = zone;
					startTime=agent.time();
					return true;
				}

			}
		}
		return false;
	}

	private boolean isSearched(SOSEstimatedFireZone zone) {
		int count = 0;
		for (Building safe : zone.getSafeBuilding()) {
			if (safe.updatedtime() > agent.time() - startTime) {
				count++;
				if (count > 10)
					return true;
			}
		}
		return false;

	}

	public static boolean isSmallFire(SOSEstimatedFireZone zone) {
		if (zone.getOuter().size() > 8)
			return false;
		return true;
	}

	@Override
	public void precompute() {

	}

	@Override
	public void act() throws SOSActionException {
		ArrayList<ShapeInArea> possibleTarget=new ArrayList<ShapeInArea>();
		for (Building safe : target.getSafeBuilding()) {
			if (safe.updatedtime() < agent.time() - startTime) {
				possibleTarget.addAll(safe.fireSearchBuilding().sensibleAreasOfAreas());
			}
		}
		moveToShape(possibleTarget);
	}
}

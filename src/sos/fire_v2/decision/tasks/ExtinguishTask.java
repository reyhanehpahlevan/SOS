package sos.fire_v2.decision.tasks;

import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.AbstractFireBrigadeAgent;
import sos.tools.decisionMaker.definitions.commands.SOSITarget;
import sos.tools.decisionMaker.definitions.commands.SOSTask;

public class ExtinguishTask extends SOSTask<SOSITarget> {

	public ExtinguishTask(Building target, ShapeInArea postition, int creatinTime) {
		super(new BuildingTarget(target, postition), creatinTime);
	}

	@Override
	public BuildingTarget getTarget() {
		return (BuildingTarget) target;
	}

	@Override
	public void execute(SOSAgent<? extends Human> agent) throws SOSActionException {
		Building b = ((BuildingTarget) target).getTarget();
		//		if (getTarget().getPosition() != null) {
		//			if (!getTarget().getPosition().contains(agent.me().getX(), agent.me().getY())) {
		//				agent.move.moveToShape(Arrays.asList(getTarget().getPosition()), StandardMove.class);
		//			}
		//		} else
		//		{
		//			if (!canExtinguish(getTarget().getTarget(),(FireBrigadeAgent) agent))
		//			{
		//				System.err.println("position null and i cant extinguish "+agent);
		//				agent.move.moveStandard(getTarget().getTarget());
		//			}
		//		}

		((FireBrigadeAgent) agent).positioning.newPsitioning(b);
		((FireBrigadeAgent) agent).extinguish(b, getEnoughWater(b, (FireBrigadeAgent) agent));
	}
	

	private boolean canExtinguish(Building building, FireBrigadeAgent me) {
		return (sos.tools.Utils.distance(me.me().getX(), me.me().getY(), building.x(), building.y()) <= AbstractFireBrigadeAgent.maxDistance);
	}

	public int getEnoughWater(Building b, FireBrigadeAgent agent) {
		
		
		
		return Math.min(AbstractFireBrigadeAgent.maxPower, agent.me().getWater());
	}

}

package sos.fire_v2.decision.states;

import java.util.List;

import sos.base.entities.Building;
import sos.base.entities.FireBrigade;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.base.worldmodel.FireWorldModel;
import sos.fire_v2.decision.FireInformationModel;
import sos.fire_v2.decision.tasks.ExtinguishTask;
import sos.fire_v2.target.SOSBuildingSelector;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.fire_v2.target.building.SOSTargetChooser2012_SmallMap;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

public class ExtinguishFire extends SOSIState<FireInformationModel> {

	private SOSFireZoneSelector fireZoneSelector;
	private SOSBuildingSelector buildingSelector;
	private SOSTargetChooser2012_SmallMap chooser;

	private SOSLoggerSystem log;
	boolean oldFire = false;

	public ExtinguishFire(FireInformationModel infoModel, SOSFireZoneSelector fireZoneSelector) {
		super(infoModel);
		this.fireZoneSelector = fireZoneSelector;
		if (oldFire)
			chooser = new SOSTargetChooser2012_SmallMap(infoModel.getAgent().fireSiteManager, (FireWorldModel) infoModel.getModel());
		else
			buildingSelector = new SOSBuildingSelector(infoModel.getAgent(), fireZoneSelector.starCluster,fireZoneSelector.fireBrigade_Task,fireZoneSelector.getStrategy());
		log = infoModel.extinguishFire;
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		if (((FireBrigade) infoModel.getAgent().me()).getWater() == 0)
			return null;
		SOSEstimatedFireZone fireZone = fireZoneSelector.decide(null);

		log.info("Select Fire Zone " + fireZone);

		if (fireZone == null)
		           	return null;
		if (fireZone.isDisable()) {
			log.warn("Selected Fire Zone is Disable " + fireZone);
			return null;
		}

		if (!fireZone.isExtinguishable()) {
			log.warn("Selected Fire Zone is UnExtinguishable " + fireZone);
			return null;
		}

		FireInformationModel inmodel = infoModel;

		inmodel.setLastSelectedFireZone(fireZone);
		inmodel.setSelectTime(inmodel.getTime());
		Building best;
		if (oldFire)
			best = chooser.getBestTarget(fireZone);
		else
			best = buildingSelector.decide(fireZone);

		inmodel.setLastSelectedBuilding(best);

		log.info("Target " + best);
		if (best == null)
			return null;

		//		ShapeInArea pos = ((FireBrigadeAgent) inmodel.getAgent()).positioning.getPosition(best);
		//		log.info("Position " + pos);
		return new ExtinguishTask(best, null, inmodel.getTime());
	}

	@Override
	public void skipped() {
	}

	@Override
	public void overTaken() {
		//		FireInformationModel inmodel = (FireInformationModel) infoModel;
		//		inmodel.setLastSelectedFireZone(null);
	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
	}

	@Override
	public String getName() {
		return "ExtinuishFire";
	}

	@Override
	public void giveFeedbacks(List feedbacks) {

	}

	@Override
	public void taken() {
		super.taken();
		((FireBrigadeAgent) infoModel.getAgent()).FDK.lastState = getName();
	}
}

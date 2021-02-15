package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.List;

import sos.ambulance_v2.AmbulanceInformationModel;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.base.entities.GasStation;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.base.util.SOSGeometryTools;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToListTask;

/*
 * @author Reyhaneh
 */
public class RunAwayFromGassStationState extends SOSIState {
	private AmbulanceTeamAgent ambulance = null;

	public RunAwayFromGassStationState(AmbulanceInformationModel infoModel) {
		super(infoModel);
		ambulance = infoModel.getAmbulance();
	}

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) throws SOSActionException {
		
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ RunAwayFromGassStationState $$$$$$$$$$$$$$$$$$$$$$$$$");
		
		for (GasStation gasStation : infoModel.getModel().GasStations()) {
			if(gasStation.distance((Human)(infoModel.getEntity())) < 50000 && gasStation.virtualData[0].getTemperature() > 35 &&
					( gasStation.getFieryness() == 0  || gasStation.getFieryness() == 4 )){
				ArrayList<Road> allRoads = new ArrayList<Road>();
				
				for(Road road:infoModel.getModel().roads()){
					if(SOSGeometryTools.distance(road.getAreaPosition(), gasStation.getAreaPosition()) > 60000)
						allRoads.add(road);
				}
				if(infoModel.getATEntity().getWork() !=  null &&  infoModel.getATEntity().getWork().getTarget() != null){
						Human oldTarget =  infoModel.getATEntity().getWork().getTarget();
						oldTarget.getRescueInfo().setIgnoredUntil(IgnoreReason.InvalidOldTarget,infoModel.getTime()+15);
						AmbulanceUtils.rejectTarget(oldTarget, infoModel.getATEntity(), ambulance);
				}
				ambulance.lastState = " RunAwayFromGassStationState ";
				infoModel.getLog().warn("Running away from Gass Station ....");
				return new StandardMoveToListTask(allRoads,infoModel.getTime());
			}
		}
		infoModel.getLog().info("$$$$$$$$$$$$$$ skipped from GassStationState $$$$$$$$$$$$$$$ ");
		
		return null;
	}

	@Override
	public void giveFeedbacks(List feedbacks) {

	}

	@Override
	public void skipped() {

	}

	@Override
	public void overTaken() {

	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {

	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {

	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

}
package sos.ambulance_v2.decision.states;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.misc.Pair;
import sos.base.PlatoonAgent;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.tools.Utils;
import sos.tools.decisionMaker.definitions.SOSInformationModel;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;
import sos.tools.decisionMaker.implementations.targets.AreaTarget;
import sos.tools.decisionMaker.implementations.targets.EmptyTarget;
import sos.tools.decisionMaker.implementations.tasks.RestTask;
import sos.tools.decisionMaker.implementations.tasks.StandardMoveToAreaTask;

/*
 * @author Reyhaneh
 */
public class NoComState extends SOSIState {

	public NoComState(SOSInformationModel infoModel) {
		super(infoModel);
	}

	private int timeInCenter = 0;
	
	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		if(infoModel.getAgent().messageSystem.type != Type.NoComunication)
			return null;
		
		infoModel.getLog().info("$$$$$$$$$$$$$$$$$$$$$$ NoComState $$$$$$$$$$$$$$$$$$$$$$$$$");
		infoModel.getLog().info("time in center : "+ timeInCenter);
		
		PlatoonAgent<? extends Human> agent = (PlatoonAgent<? extends Human>) infoModel.getAgent();
		if (shouldBeGoToCenter()) {
			if (distance(agent.newSearch.strategyChooser.noCommunication.getGatheringArea()) >= agent.messageSystem.getMessageConfig().voiceChannels().get(0).getRange()) {
				infoModel.getLog().debug("the distance from gathering area is more than hear range... move to gathering area...");
				return new StandardMoveToAreaTask(new AreaTarget(agent.newSearch.strategyChooser.noCommunication.getGatheringArea()), infoModel.getTime());
			}
			timeInCenter++;
			if (timeInCenter <= 2) {
				infoModel.getLog().debug("I reached the gathering area Waiting for hearing the messages from and sending to centeral man...");
				return new RestTask(new EmptyTarget(), infoModel.getTime(), "NOCOM-Agent");
			}
			for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> fs : infoModel.getModel().getFireSites())
				fs.second().setReportTime(infoModel.getModel().time());
			infoModel.getLog().info("$$$$$ Skipped from NoComState $$$$$");
			return null;
		}
		if (distance(agent.newSearch.strategyChooser.noCommunication.getGatheringArea()) >= agent.messageSystem.getMessageConfig().voiceChannels().get(0).getRange())
			timeInCenter = 0;
		
		infoModel.getLog().info("$$$$$ Skipped from NoComState $$$$$");
		return null;
	}

	private boolean shouldBeGoToCenter() {
		infoModel.getLog().info("should goto center???");
		infoModel.getLog().debug("model().getFireSites()=" + infoModel.getModel().getFireSites());
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfs : infoModel.getModel().getFireSites())
		{
			SOSEstimatedFireZone fs = pfs.second();
			infoModel.getLog().trace("sos estimated fire zone: " + fs + " isNewFireAndNotReported?" + !fs.isReported());
			if ((!fs.isReported()) && isInMyClusterOrTimeIsLessThan50AndSeenByMe(fs)) {

				infoModel.getAgent().sosLogger.noComunication.info("exist unreported fire site " + fs);
				return true;
			}
		}
		infoModel.getLog().info("Should not goto center!");
		return false;
	}
	
	private boolean isInMyClusterOrTimeIsLessThan50AndSeenByMe(SOSEstimatedFireZone fs) {
		for (Building b : fs.getAllBuildings()) {
			if (infoModel.getAgent().getMyClusterData().getBuildings().contains(b))
				return true;
			if (b.getLastSenseTime() > 3 && infoModel.getModel().time() < 50 && b.isBurning())
				return true;
		}

		return false;
	}
	
	public int distance(Area road) {
		return (int) Utils.distance(infoModel.getATEntity().getX(), infoModel.getATEntity().getY(), road.getX(),road.getY());
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
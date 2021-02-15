package sos.police_v2.state;

import java.util.ArrayList;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.sosFireZone.SOSRealFireZone;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;

/**
 * @author Yoosef
 */
public class NoCommunicationState extends PoliceAbstractState {

	public NoCommunicationState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {

	}

	private int timeInCenter = 0;

	@Override
	public void act() throws SOSActionException {

		actAsCenteralMan();
		actAsOtherAgents();
	}

	private void actAsOtherAgents() throws SOSActionException {
		log.info("Acting as Other Agent in No Communication");
		if (shouldBeGoToCenter()) {
			if (agent.me().distance(agent.newSearch.strategyChooser.noCommunication.getGatheringArea()) >= agent.messageSystem.getMessageConfig().voiceChannels().get(0).getRange()) {
				log.debug("the distance from gathering area is more than hear range... move to gathering area...");
				move(agent.newSearch.strategyChooser.noCommunication.getGatheringArea());
			}
			timeInCenter++;
			if (timeInCenter <= 2) {
				log.debug("I reached the gathering area Waiting for hearing the messages from and sending to centeral man...");
				agent.problemRest("NOCOM-Agent", false);
			}
			for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> fs : model().getFireSites())
				fs.second().setReportTime(model().time());
			return;
		}
		if (agent.me().distance(agent.newSearch.strategyChooser.noCommunication.getGatheringArea()) >= agent.messageSystem.getMessageConfig().voiceChannels().get(0).getRange())
			timeInCenter = 0;
	}

	private void actAsCenteralMan() throws SOSActionException {
		if (agent.me() == agent.newSearch.strategyChooser.noCommunication.getCenteralMan()) {
			agent.sosLogger.noComunication.info("i am centerMan " + agent.me());
			if (agent.newSearch.strategyChooser.noCommunication.getGatheringArea() == agent.me().getAreaPosition())
				agent.problemRest("NOCOM-Lead", false);
			move(agent.newSearch.strategyChooser.noCommunication.getGatheringArea());
		}

	}

	private boolean shouldBeGoToCenter() {
		log.info("should goto center???");
		log.debug("model().getFireSites()=" + model().getFireSites());
		for (Pair<ArrayList<SOSRealFireZone>, SOSEstimatedFireZone> pfs : model().getFireSites())
		{
			SOSEstimatedFireZone fs = pfs.second();
			log.trace("sos estimated fire zone: " + fs + " isNewFireAndNotReported?" + !fs.isReported());
			if ((!fs.isReported()) && isInMyClusterOrTimeIsLessThan50AndSeenByMe(fs)) {

				agent.sosLogger.noComunication.info("exist unreported fire site " + fs);
				//				for (Building b : fs.getAllBuildings()) {
				//					if (b.getLastMsgTime() > b.getLastSenseTime()) {
				//
				//					}
				//				}

				return true;
			}
		}
		log.info("Should not goto center!");
		return false;
	}

	private boolean isInMyClusterOrTimeIsLessThan50AndSeenByMe(SOSEstimatedFireZone fs) {
		for (Building b : fs.getAllBuildings()) {
			if (agent.getMyClusterData().getBuildings().contains(b))
				return true;
			if (b.getLastSenseTime() > 3 && model().time() < 50 && b.isBurning())
				return true;
		}

		return false;
	}

}

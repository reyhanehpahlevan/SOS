package sos.ambulance_v2.decision;

import rescuecore2.worldmodel.EntityID;
import sos.ambulance_v2.AmbulanceTeamAgent;
import sos.ambulance_v2.AmbulanceUtils;
import sos.ambulance_v2.base.AbstractAmbulanceCenterActivity;
import sos.ambulance_v2.base.AmbulanceConstants.ATstates;
import sos.ambulance_v2.base.RescueInfo.IgnoreReason;
import sos.ambulance_v2.decision.controller.AmbulanceCenterController;
import sos.base.CenterAgent;
import sos.base.SOSAgent;
import sos.base.SOSConstant;
import sos.base.entities.AmbulanceTeam;
import sos.base.entities.Area;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.base.util.sosLogger.SOSLoggerSystem;

/**
 * SOS centre agent.
 * 
 * @author Reyhaneh
 */

public class AmbulanceCenterActivity extends AbstractAmbulanceCenterActivity implements MessageXmlConstant {
	//	MoveConstants m;

	private AmbulanceCenterController acc;

	public AmbulanceCenterActivity(SOSAgent<? extends StandardEntity> sosAgent) {
		super(sosAgent);
		InitializeVariables();

		if ((sosAgent instanceof CenterAgent) && ((CenterAgent) sosAgent).ambDecision == null)
			((CenterAgent) sosAgent).ambDecision = new AmbulanceDecision(sosAgent);
		else if ((sosAgent instanceof AmbulanceTeamAgent) && ((AmbulanceTeamAgent) sosAgent).ambDecision == null)
			((AmbulanceTeamAgent) sosAgent).ambDecision = new AmbulanceDecision(sosAgent);

		acc = new AmbulanceCenterController(sosAgent);
	}

	private AmbulanceDecision ambDecision() {
		if (sosAgent instanceof AmbulanceTeamAgent)
			return ((AmbulanceTeamAgent) sosAgent).ambDecision;
		return ((CenterAgent) sosAgent).ambDecision;
	}

	private SOSLoggerSystem lg() {
		if (sosAgent instanceof AmbulanceTeamAgent)
			return ((AmbulanceTeamAgent) sosAgent).log();
		return ((CenterAgent) sosAgent).log();
	}

	private void InitializeVariables() {
		if (sosAgent instanceof AmbulanceTeamAgent)
			return;
		for (Human hm : sosAgent.model().humans()) {
			hm.getRescueInfo().updateProperties();
			//			hm.getRescueInfo().getPartileFilter().setDmg(hm.getDamage(), sosAgent.time());
			//			hm.getRescueInfo().getPartileFilter().setBury(hm.getBuriedness());
			//			hm.getRescueInfo().getPartileFilter().setHp(hm.getHP(), sosAgent.time());
		}
	}

	@Override
	protected void prepareForThink() throws Exception {
		if (sosAgent instanceof AmbulanceTeamAgent && sosAgent.model().ambulanceTeams().size() == 1)
			return;
		super.prepareForThink();
		//		handleHumanChange();
		ambDecision().updateHumansInfo();
		if (!SOSConstant.IS_CHALLENGE_RUNNING && !(sosAgent instanceof AmbulanceTeamAgent)) {
			printAllInfos();
		}
	}

	//	private void handleHumanChange() {
	//
	//		for (Human current :sosAgent.getVisibleEntities(Human.class)) {
	//				current.getRescueInfo().getPartileFilter().setDmg(current.getDamage(), sosAgent.time());
	//				current.getRescueInfo().getPartileFilter().setBury(current.getBuriedness());
	//				current.getRescueInfo().getPartileFilter().setHp(current.getHP(), sosAgent.time());
	//
	//			}
	//	}
	//
	protected void printAllInfos() {
		lg().logln("~~~~~~~~~~~~~~~~~~~~~~ AT INFOS ~~~~~~~~~~~~~~~~~~~~~~");
		for (AmbulanceTeam at : sosAgent.model().ambulanceTeams()) {
			lg().logln(at + " --> " + at.getWork());
		}
		lg().logln("~~~~~~~~~~~~~~~~~~~~~~ HUMAN INFOS ~~~~~~~~~~~~~~~~~~~~~~");
		for (Human hu : sosAgent.model().humans()) {
			if (!hu.isPositionDefined())
				continue;
			lg().logln(hu + " --> " + hu.getRescueInfo());
		}
		lg().logln("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}

	@Override
	protected void think() throws SOSActionException, Exception {
		if (sosAgent instanceof AmbulanceTeamAgent && sosAgent.model().ambulanceTeams().size() == 1)
			return;
		super.think();
		acc.act();
	}

	@Override
	protected void finalizeThink() throws Exception {
		if (sosAgent instanceof AmbulanceTeamAgent && sosAgent.model().ambulanceTeams().size() == 1)
			return;
		super.finalizeThink();
	}

	/*
	 * Ali: Please keep it at the end!!!!(non-Javadoc)
	 */

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {

		if (sosAgent instanceof AmbulanceTeamAgent)//TODO check the last todo for know if distribute or not
			return;
		boolean civilianFlag = false;
		if (header.equalsIgnoreCase(HEADER_SENSED_CIVILIAN)) {
			Human h = (Human) sosAgent.model().getEntity(new EntityID(data.get(DATA_ID)));
			if (h == null) {
				civilianFlag = true;
			}
		}
		// ****************************************************************
		super.hear(header, data, dynamicBitArray, sender, channel);
		// ****************************************************************
		if (header.equalsIgnoreCase(HEADER_SENSED_CIVILIAN)) {
			Human h = (Human) sosAgent.model().getEntity(new EntityID(data.get(DATA_ID)));
			if (h != null) {
				if (sosAgent.time() - h.getLastSenseTime() < 3)
					return;
				if (civilianFlag)
					ambDecision().calculateRefugeInformation_old(h);

			}
			if (h instanceof Civilian && h.getPosition() instanceof Road)
				h.getRescueInfo().setNotIgnored();
			//		ambDecision().updateHuman(h);
			lg().logln("msg -> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);
		}

		// ****************************************************************
		else if (header.equalsIgnoreCase(HEADER_SENSED_AGENT)) {
			Human hu = sosAgent.model().agents().get(data.get(DATA_AGENT_INDEX));
			if (sosAgent.time() - hu.getLastSenseTime() < 3)
				return;
			ambDecision().updateHuman(hu);
		}
		//****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_STATUS)) { // usage only in say
			if (sosAgent.time() - data.get(DATA_TIME) > 2)
				return;
			ATstates state = AmbulanceUtils.convertStateIndexToState(data.get(DATA_AT_STATE));
			lg().logln("ambulance status --> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);
			logToAmbDecision("ambulance status --> Header:" + header + " Data:" + data + " state=" + state + " From:" + sender);
			AmbulanceTeam at = sosAgent.model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));

			if (state != null)
				at.getWork().setCurrentState(state);
			if (state == ATstates.SEARCH) {
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(sosAgent.time());

			}
			if (data.get(DATA_ID) == 0)
				return;
			Human hmn = (Human) sosAgent.model().getEntity(new EntityID(data.get(DATA_ID)));
			Area position = sosAgent.model().areas().get(data.get(DATA_AREA_INDEX));
			if (hmn == null && position != null) {
				hmn = sosAgent.updater.newHuman(data.get(DATA_ID), sosAgent.time() - sosAgent.messageSystem.getNormalMessageDelay(), 9000, 20, 20, position);
				ambDecision().updateHuman(hmn);
			}
			if (position != null) {
				if (state == ATstates.RESCUE)
					hmn.setPosition(position.getID(), position.getX(), position.getY());
				at.setPosition(position.getID(), position.getX(), position.getY());
			}

			at.getWork().setTarget(hmn, null);
			hmn.getRescueInfo().addAT(at);
			at.getWork().setNeedHelpInSay(data.get(DATA_NEED_HELP) == 1);
		}
		//****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_INFO)) {
			ATstates state = AmbulanceUtils.convertStateIndexToState(data.get(DATA_AT_STATE));
			AmbulanceTeam at = sosAgent.model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));

			lg().logln("Ambulance Info --> Header:" + header + " Data:" + data + " state=" + state + " From:" + sender + " Channel:" + channel);
			logToAmbDecision("Ambulance Info --> Header:" + header + " Data:" + data + " state=" + state + " From:" + sender);
			if (state != null)
				at.getWork().setCurrentState(state);
			if (state != ATstates.SEARCH && data.get(DATA_ID) != 0) { //data.get(DATA_ID)==0 --> means no target
				Human hmn = (Human) sosAgent.model().getEntity(new EntityID(data.get(DATA_ID)));
				if (hmn == null) {
					hmn = sosAgent.updater.newHuman(data.get(DATA_ID), sosAgent.time() - sosAgent.messageSystem.getNormalMessageDelay(), 9000, 20, 20, null);
					ambDecision().updateHuman(hmn);
				}

				at.getWork().setTarget(hmn, null);
				at.getWork().setNextFreeTime(data.get(DATA_FINISH_TIME));
				hmn.getRescueInfo().addAT(at);

				//				if (hmn.getRescueInfo().getATneedToBeRescued() - hmn.getRescueInfo().getNowWorkingOnMe().size() <= 0) {
				//					hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.HaveEnoughAT, sosAgent.time() + 15);
				//				}
				if (state == ATstates.MOVE_TO_REFUGE && hmn != null) {
					if (hmn.getRescueInfo().getBestRefuge() != null)
						hmn.setPosition(hmn.getRescueInfo().getBestRefuge().getID());
					else if (sosAgent.model().refuges().size() > 0)
						hmn.setPosition(sosAgent.model().refuges().get(0).getID());
					else {
						Area road = AmbulanceUtils.getRoadNeighbour(hmn.getAreaPosition());//TODO set position null!!!
						if (road != null)
							hmn.setPosition(road.getID());
					}
				}
			} else {
				//				if (at.getWork().getTarget() != null)
				//					at.getWork().getTarget().getRescueInfo().removeAT(at);
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(sosAgent.time());
			}
		}
		//****************************************************************
		else if (header.equalsIgnoreCase(HEADER_AMBULANCE_TASK_ACK)) {
			lg().logln("taskAck --> Header:" + header + " Data:" + data + " From:" + sender + " Channel:" + channel);
			logToAmbDecision("taskAck --> Header:" + header + " Data:" + data + " From:" + sender);
			AmbulanceTeam at = sosAgent.model().ambulanceTeams().get(data.get(DATA_AMBULANCE_INDEX));
			Human hmn = (Human) sosAgent.model().getEntity(new EntityID(data.get(DATA_ID)));
			if (hmn == null) {
				hmn = sosAgent.updater.newHuman(data.get(DATA_ID), sosAgent.time() - sosAgent.messageSystem.getNormalMessageDelay(), 9000, 20, 20, null);
				ambDecision().updateHuman(hmn);
			}
			int meaning = data.get(DATA_ACK_TYPE); // 0-->accepted 1-->finish 2-->rejected
			switch (meaning) {
			case 0:
				at.getWork().setCurrentState(ATstates.MOVE_TO_TARGET);
				at.getWork().setTarget(hmn, null);
				hmn.getRescueInfo().addAT(at);

				//TODO alan farghe freeTime e loader ro ba baghie yeki dar nazar gereftim. bayad dorost beshe!!
				//sinash

				int nextFreeTime = sosAgent.time();
				nextFreeTime += (int) Math.ceil(hmn.getBuriedness() / (float) hmn.getRescueInfo().getATneedToBeRescued());
				nextFreeTime += (2 + hmn.getRescueInfo().getTimeToRefuge());//1 for load 1 for unload
				at.getWork().setNextFreeTime(nextFreeTime);

				if (!hmn.getRescueInfo().getNowWorkingOnMe().contains(sosAgent.getID().getValue())) {
					if (hmn.getRescueInfo().getATneedToBeRescued() - hmn.getRescueInfo().getNowWorkingOnMe().size() <= 0) {
						hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.HaveEnoughAT, sosAgent.time() + 15);
					}
				}
				break;
			case 1:
				hmn.getRescueInfo().setIgnoredUntil(IgnoreReason.FinishMessageReceived, sosAgent.time() + 10);
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(0);
				break;
			case 2:
				at.getWork().setTarget(null, null);
				at.getWork().setNextFreeTime(0);
				if (!(hmn.getRescueInfo().getIgnoreReason() == IgnoreReason.IgnoredTargetMessageReceived || hmn.getRescueInfo().getIgnoreReason() == IgnoreReason.WillDie)) { //sinash
					hmn.getRescueInfo().setNotIgnored();
				}
				break;
			default:
			}
			//	            if (meaning == 1)
			//	                addMessage("taskAck", new int[]{data[0], data[1], data[2]}); //TODO check and add if needed IMPORTANT
		}
	}

	public void logToAmbDecision(String s) {
		ambDecision().dclog.logln(s);
		abstractLog().logln(s);
	}

	public SOSLoggerSystem abstractLog() {
		if (sosAgent instanceof AmbulanceTeamAgent)
			return ((AmbulanceTeamAgent) sosAgent).abstractlog;
		return ((CenterAgent) sosAgent).abstractlog;
	}
}
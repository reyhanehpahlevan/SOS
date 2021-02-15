package sosNamayangar.message_decoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import sosNamayangar.SOSWorldModel;
import sosNamayangar.message_decoder.blocks.AbstractMessageBlock;
import sosNamayangar.message_decoder.blocks.DataArrayList;
import sosNamayangar.message_decoder.blocks.DynamicSizeMessageBlock;

public class MessageHandler implements MessageXmlConstant {

	private final SOSWorldModel model;
	private final EntityID agentid;

	public MessageHandler(SOSWorldModel model, EntityID agentid) {
		this.model = model;
		this.agentid = agentid;
	}

	public void updateByMessage(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, int meTime) {

		if (sender.getID().getValue() == agentid.getValue())
			return;
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		if (header.equalsIgnoreCase(HEADER_OPEN_ROAD)) {
			int index = data.get(DATA_ROAD_INDEX);
			Road road = model.roads().get(index);
			if (road.updatedtime() >= meTime - 2)
				return;
			road.setLastMsgTime(meTime - 2);
			road.setBlockades(new ArrayList<EntityID>());
		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_FIRE)) {
			int time = data.get(DATA_TIME);
			Building bu = model.buildings().get(data.get(DATA_BUILDING_INDEX));
			if (time <= bu.updatedtime())
				return;
			bu.setFieryness(data.get(DATA_FIERYNESS));
			bu.setTemperature(data.get(DATA_HEAT) * 3);
			bu.setLastMsgTime(time);
			// if (bu.getLastCycleUpdated() < time)
			// bu.setLastCycleUpdated(time);
		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_SENSED_CIVILIAN)) {
			int id = data.get(DATA_ID);
			Area pos = model.areas().get(data.get(DATA_AREA_INDEX));
			int hp = data.get(DATA_HP) * 200;
			int dmg = data.get(DATA_DAMAGE) * 10;
			Civilian civ = (Civilian) model.getEntity(new EntityID(id));
			if (civ == null) { // new civilian
				civ = new Civilian(new EntityID(id));
				model.addEntity(civ);
			}
			if (civ.updatedtime() >= data.get(DATA_TIME))
				return;
			civ.setBuriedness(data.get(DATA_BURIEDNESS));
			if (civ.getBuriedness() > 0 && dmg == 0)
				civ.setDamage(model.getConfig().getIntValue("perception.los.precision.damage") / 3);
			else
				civ.setDamage(dmg);
			boolean isReallyReachable = data.get(DATA_IS_REALLY_REACHABLE) == 1;
			// civ.setIsReallyReachable(isReallyReachable);
			civ.setHP(hp);
			civ.setLastMsgTime(data.get(DATA_TIME));
			civ.setPosition(pos.getID(), getRndpos(pos.getX(), 200), getRndpos(pos.getY(), 200));
			// if (pos instanceof Building)
			// ((Building) pos).setSearchedForCivilian(true);
		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_SENSED_AGENT)) {
			Area pos = model.areas().get(data.get(DATA_AREA_INDEX));
			int hp = data.get(DATA_HP) * 200;
			int dmg = data.get(DATA_DAMAGE) * 10;
			Human hu = model.agents().get(data.get(DATA_AGENT_INDEX));
			if (hu.getID().getValue() == agentid.getValue() || hu.updatedtime() >= data.get(DATA_TIME))
				return;
			hu.setBuriedness(data.get(DATA_BURIEDNESS));
			hu.setDamage(dmg);
			hu.setHP(hp);
			hu.setLastMsgTime(data.get(DATA_TIME));
			hu.setPosition(pos.getID(), pos.getX(), pos.getY());
		} else if (header.equalsIgnoreCase(HEADER_LOWCOM_FIRE)) {
			Building bu = model.buildings().get(data.get(DATA_BUILDING_INDEX));
			if (meTime - bu.updatedtime() < 2)
				return;
			bu.setFieryness(data.get(DATA_FIERYNESS));
			if (bu.getFieryness() == 1)
				bu.setTemperature(70);
			else if (bu.getFieryness() == 2)
				bu.setTemperature(170);

			else if (bu.getFieryness() == 3)
				bu.setTemperature(200);
			else
				bu.setTemperature(20);

			bu.setLastMsgTime(meTime - 2);
			// if (bu.getLastCycleUpdated() < model.time() - 2)
			// bu.setLastCycleUpdated(model.time() - 2);

		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_POSITION)) {
			Area ar = model.areas().get(data.get(DATA_AREA_INDEX));
			Human ag = model.agents().get(data.get(DATA_AGENT_INDEX));
			if (ag.updatedtime() >= meTime - 2)
				return;
			if (ar instanceof Building) {
				ag.setPosition(ar.getID(), ar.getX(), ar.getY());

				// ((Building) ar).setSearchedForCivilian(true);///TODO ADDED BY
				// ALI
			} else {
				Road rd = (Road) ar;
				ag.setPosition(ar.getID(), rd.getX(), rd.getY());
			}
		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_DEAD_AGENT)) {
			Human ag = model.agents().get(data.get(DATA_AGENT_INDEX));
			if (ag.updatedtime() >= meTime - 2)
				return;
			ag.setHP(0);
		}
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		// else if (header.equalsIgnoreCase(HEADER_IGNORED_TARGET)) {
		// int id = data.get(DATA_ID);
		// Human hu = (Human) model.getEntity(new EntityID(id));
		// if (hu != null) {
		// hu.getRescueInfo().setIgnoredUntil(1000);
		// hu.getRescueInfo().setIgnored(true);
		// }
		// }
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_FIRE_ZONE)) {
			int indx = data.get(DATA_BUILDING_INDEX);
			int fiery = data.get(DATA_FIERYNESS);
			Building b = model.buildings().get(indx);
			b.setFieryness(fiery);
		}/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
		else if (header.equalsIgnoreCase(HEADER_IM_HEALTHY_AND_CAN_ACT)) {
			int agentindx = data.get(DATA_AGENT_INDEX);
			Human agent = model.agents().get(agentindx);
			if (!agent.isHPDefined())
				agent.setHP(10000);
			if (!agent.isBuriednessDefined() || agent.getBuriedness() != 0)
				agent.setBuriedness(0);
			if (!agent.isDamageDefined())
				agent.setDamage(0);
		} else if (header.equalsIgnoreCase(HEADER_SEARCHED_FOR_CIVILIAN)) {
			int indx = data.get(DATA_BUILDING_INDEX);
			Building b = model.buildings().get(indx);
			// b.setSearchedForCivilian(true);
		} else if (header.equalsIgnoreCase(HEADER_LOWCOM_CIVILIAN)) {
			int indx = data.get(DATA_BUILDING_INDEX);
			int validCivilianCount = data.get(DATA_VALID_CIVILIAN_COUNT);
			boolean isReallyUnReachable = data.get(DATA_IS_REALLY_UNREACHABLE) == 1 ? true : false;
			Building b = model.buildings().get(indx);
			// SearchBuilding searchB =
			// model.searchWorldModel.getSearchBuilding(b);
			// searchB.setValidCivilianCountInLowCom(validCivilianCount,
			// isReallyUnReachable, meTime);
			/*
			 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			 * ~~~~~~~~~~~~~~~
			 */
		} else if (header.equalsIgnoreCase(HEADER_ROAD_STATE)) {
			Road road = model.roads().get(data.get(MessageXmlConstant.DATA_ROAD_INDEX));
			if (road.updatedtime() >= meTime - 2)
				return;
			if (road.isBlockadesDefined() && road.getBlockades().isEmpty())
				return;
			road.setLastMsgTime(meTime - 2);
		}
		// else if
		// (header.equalsIgnoreCase(HEADER_AGENT_TO_EDGES_REACHABLITY_STATE)) {
		// Road road =
		// model.roads().get(data.get(MessageXmlConstant.DATA_ROAD_INDEX));
		// Human humAgent =
		// model.agents().get(data.get(MessageXmlConstant.DATA_AGENT_INDEX));
		// for (int i = 0; i < dynamicBitArray.length(); i++) {
		// if (dynamicBitArray.get(i))
		// humAgent.addImReachableToEdge(road.getPassableEdges()[i]);
		//
		// }
		// }
		/*
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * ~~~~~~~~~~~
		 */
	}

	private int getRndpos(int base, int limit) {
		boolean sign = Math.random() > 0.5 ? true : false;
		if (sign)
			return ((int) (Math.random() * -10000) % limit) + base;
		else
			return ((int) (Math.random() * 10000) % limit) + base;
	}

	// **********************************************************************************************************/
	/**
	 * add new humanoid info getting from strategy msg
	 * 
	 * @param id
	 * @param time
	 * @param hp
	 * @param dmg
	 * @param bur
	 * @param mlpos
	 */
	public Human newHuman(int id, int time, int hp, int dmg, int bur, Area pos) {
		Human civ = new Civilian(new EntityID(id));
		model.addEntity(civ);
		civ.setBuriedness(bur);
		civ.setDamage(dmg);
		civ.setHP(hp);
		civ.setLastMsgTime(time);
		if (pos != null)
			civ.setPosition(pos.getID(), getRndpos(pos.getX(), 200), getRndpos(pos.getY(), 200));
		return civ;
	}

	private static final int HEADER_SIZE = 5;

	public void handleReceive(Collection<Command> heard, int time) {
		// mcLog.setOutputType(OutputType.Both);
		HashSet<Integer> recivedMessages = new HashSet<Integer>();
		try {
			for (Command command : heard) {
				try {

					if (command instanceof AKSpeak) {
						AKSpeak speak = (AKSpeak) command;
						StandardEntity sender = model.getEntity(command.getAgentID());
						if (!(sender instanceof Civilian || sender == null)) {
							SOSBitArray bitArray = new SOSBitArray(speak.getContent());

							int bitPosition = 0;

							while (bitPosition + HEADER_SIZE <= bitArray.length()) {
								int headerIndex = bitArray.get(bitPosition, HEADER_SIZE);
								if (headerIndex == 0)
									break;
								String header = ReadXml.indexToHeader(headerIndex);
//								System.err.println(header + ":"+headerIndex);
								if (header == null)
									break;
								bitPosition += HEADER_SIZE;

								DataArrayList xmlData = ReadXml.blocks.get(header).data();
								DataArrayList data = new DataArrayList(xmlData.size());

								for (int i = 0; i < xmlData.size(); i++) {
									data.put(xmlData.getKey(i), bitArray.get(bitPosition, xmlData.getValue(i)));
									bitPosition += xmlData.getValue(i);
								}
								int messageBlockHashcode = AbstractMessageBlock.getHash(header, data);
								SOSBitArray dynamicBitArray = null;
								int dynamicBitSize = DynamicSizeMessageBlock.getDynamicBitSize(header, data, model);
								if (dynamicBitSize > 0) {
									dynamicBitArray = bitArray.getBit(bitPosition, dynamicBitSize);
								}
								bitPosition += dynamicBitSize;
								// ReceiveMessageBlock rmb = new
								// ReceiveMessageBlock(sender, channel,
								// bitArray, bitPosition, tm4);
								if (recivedMessages.add(messageBlockHashcode)) {
									updateByMessage(header, data, dynamicBitArray, sender, time);
								}
								// bitPosition += ReadXml.getBlockSize(header);
								// receiveMessageList.add(rmb);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

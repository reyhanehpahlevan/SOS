package sos.police_v2.state;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;

import rescuecore2.misc.Pair;
import sos.base.entities.Area;
import sos.base.entities.Blockade;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.PoliceUtils;

public class OtherAgentStockState extends PoliceAbstractState {
	public ArrayList<Pair<Area, Short>> stockPositions = new ArrayList<Pair<Area, Short>>();
	public HashSet<Area> checkedArea = new HashSet<Area>();
	public Shape myClusterShape;

	public OtherAgentStockState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void precompute() {
		myClusterShape = model().searchWorldModel.getClusterData().getConvexShape();
	}

	@Override
	public void act() throws SOSActionException {
		//		if(true)
		//			return;
		log.debug("stock Position:" + stockPositions);
		//		for (Area stock : stockPositions) {
		//
		//			if (stock.equals(agent.me().getAreaPosition())) {
		//				checkedArea.add(stock);
		//			}
		//		}
		filterOldCycles();

		for (Pair<Area, Short> pair : stockPositions) {
			Area stock = pair.first();
			if (checkedArea.contains(stock))
				continue;
			if (!myClusterShape.contains(stock.getPositionPoint().toGeomPoint()))
				continue;
			if (stock.equals(agent.me().getAreaPosition())) {
				if (stock.isBlockadesDefined()) {

					for (Blockade block : stock.getBlockades()) {
						if (PoliceUtils.isValid(block))
							clear(block);
					}
					for (Blockade block : stock.getBlockades()) {
						//						if (!isReachableTo(block.getPositionPair())) {
						log.debug("going to:" + block);
						moveToPoint(block.getPositionPair());
						//						}
					}
				}
				log.debug(stock + "has been cleared!!!");

				checkedArea.add(stock);
			} else {
				log.debug("moving to " + stock);
				moveToPoint(stock.getPositionPair());
			}
		}
	}

	private void filterOldCycles() {
		ArrayList<Pair<Area, Short>> temp = new ArrayList<Pair<Area, Short>>();
		for (Pair<Area, Short> pair : stockPositions) {
			if (pair.second() + 10 >= agent.time())
				temp.add(pair);
		}
		stockPositions = temp;
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
		super.hear(header, data, dynamicBitArray, sender, channel);
		if (header.equalsIgnoreCase(MessageXmlConstant.HEADER_AGENT_STOCK)) {
			Area area = model().areas().get(data.get(MessageXmlConstant.DATA_AREA_INDEX));
			log.debug("heared stock message in " + area + " sender =  " + sender);
			if (!agent.getMyClusterData().isCoverer())
				stockPositions.add(new Pair<Area, Short>(area, (short) agent.time()));

		}
	}
}

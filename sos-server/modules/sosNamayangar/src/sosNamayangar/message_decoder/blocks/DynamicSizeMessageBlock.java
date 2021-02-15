package sosNamayangar.message_decoder.blocks;

import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import sosNamayangar.SOSWorldModel;
import sosNamayangar.message_decoder.MessageXmlConstant;


public class DynamicSizeMessageBlock {
	public static int getDynamicBitSize(String header,DataArrayList datas,SOSWorldModel model){
		if(header.equalsIgnoreCase(MessageXmlConstant.HEADER_ROAD_STATE)){
			Road road=model.roads().get(datas.get(MessageXmlConstant.DATA_ROAD_INDEX));
			int len = getPassableEdgesLength(road);
			return (short) ((len * (len - 1)) / 2);
		}
		if(header.equalsIgnoreCase(MessageXmlConstant.HEADER_AGENT_TO_EDGES_REACHABLITY_STATE)){
			Road road=model.roads().get(datas.get(MessageXmlConstant.DATA_ROAD_INDEX));
			return getPassableEdgesLength(road);
		}
		return 0;
	}

	private static int getPassableEdgesLength(Road road) {
		int len = 0;
		for (Edge e : road.getEdges()) {
			if(e.isPassable())
				len++;
		}
		return len;

	}

}

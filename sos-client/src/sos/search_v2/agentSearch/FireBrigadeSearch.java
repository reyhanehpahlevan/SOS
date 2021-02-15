package sos.search_v2.agentSearch;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.search_v2.tools.cluster.MapClusterType;
import sos.search_v2.tools.searchScore.FireBrigadeSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

/**
 * @author Yoosef Golshahi
 * @param <E>
 */
public class FireBrigadeSearch<E extends Human> extends AgentSearch<E> {
	public FireBrigadeSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, MapClusterType<E> clusterType) {
		super(me, searchWorld, clusterType, FireBrigadeSearchScore.class);
	}

	@Override
	public void hear(String header, DataArrayList data, StandardEntity sender, Channel channel) {
		super.hear(header, data, sender, channel);
	}

	@Override
	public void initSearchOrder() {
		searchTypes.add(strategyChooser.fireSearch);
		if(strategyChooser.noCommunication.isNoCommunication()){
			searchTypes.add(strategyChooser.blockSearchNoComm);
			searchTypes.add(strategyChooser.noCommGathering);
			searchTypes.add(strategyChooser.clusterCombinedSearch);
			searchTypes.add(strategyChooser.civilianSearch);
			searchTypes.add(strategyChooser.combinedSearch);
		}else{
			searchTypes.add(strategyChooser.blockSearch);
			searchTypes.add(strategyChooser.civilianSearch);
			searchTypes.add(strategyChooser.civilianUpdateSearch);
			searchTypes.add(strategyChooser.combinedSearch);
		}
		searchTypes.add(strategyChooser.dummySearch);
	}

}

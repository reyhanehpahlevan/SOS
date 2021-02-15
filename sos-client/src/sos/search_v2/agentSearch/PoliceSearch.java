package sos.search_v2.agentSearch;

import sos.base.SOSAgent;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.search_v2.tools.cluster.MapClusterType;
import sos.search_v2.tools.searchScore.PoliceSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

/**
 * @author Yoosef Golshahi
 * @param <E>
 */
public class PoliceSearch<E extends PoliceForce> extends AgentSearch<E> {

	public PoliceSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, MapClusterType<E> clusterType) {
		super(me, searchWorld, clusterType, PoliceSearchScore.class);

	}

	@Override
	public void hear(String header, DataArrayList data, StandardEntity sender, Channel channel) {
		super.hear(header, data, sender, channel);
		////////////////////////////////////////////////////////

	}

	@Override
	public void initSearchOrder() {
		if (strategyChooser.noCommunication.isNoCommunication()) {
//			searchTypes.add(strategyChooser.fireSearch);
			searchTypes.add(strategyChooser.blockSearch);
			searchTypes.add(strategyChooser.civilianSearch);
			//			searchTypes.add(strategyChooser.blockSearch);
			//			searchTypes.add(strategyChooser.civilianUpdateSearch);
			searchTypes.add(strategyChooser.combinedSearch);
		} else {
			//		searchTypes.add(strategyChooser.starSearch);
//			searchTypes.add(strategyChooser.fireSearch);
			searchTypes.add(strategyChooser.blockSearch);
			searchTypes.add(strategyChooser.civilianSearch);
			searchTypes.add(strategyChooser.civilianUpdateSearch);
			searchTypes.add(strategyChooser.combinedSearch);
		}
		searchTypes.add(strategyChooser.dummySearch);
	}

}

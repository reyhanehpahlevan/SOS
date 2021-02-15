package sos.search_v2.searchType;

import java.util.Arrays;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.base.entities.Road;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

public class NoCommGatheringSearch<E extends Human> extends SearchStrategy<E> {
	public NoCommGatheringSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	boolean isGatheringSearchDone = false;

	@Override
	public String log(String st) {
		me.sosLogger.search.info(st);
		return st;
	}

	@Override
	public SearchTask searchTask() throws SOSActionException {
		if (isGatheringSearchDone)
			return null;
		if (!me.newSearch.getSearchWorld().getClusterData().isCoverer())
			return null;

		Road gathering = me.newSearch.strategyChooser.noCommunication.getGatheringArea();
		if (me.me().getPositionArea().equals(gathering)) {
			agentSearch.strategyChooser.blockSearch.resetBlocks();
			isGatheringSearchDone = true;
			return null;
		}
		return new SearchTask(Arrays.asList(new ShapeInArea(gathering.getApexList(), gathering)));
	}

	@Override
	public SearchType getType() {
		return SearchType.NoCommGatheringSearch;
	}

}
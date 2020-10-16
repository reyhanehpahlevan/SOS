package sos.search_v2.searchType;

import java.util.ArrayList;

import sos.base.SOSAgent;
import sos.base.entities.Human;
import sos.base.move.Path;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.agentSearch.AgentSearch.SearchType;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

public class DummySearch<E extends Human> extends SearchStrategy<E> {

	public DummySearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, AgentSearchScore scoreFunction, AgentSearch<?> agentSearch) {
		super(me, searchWorld, scoreFunction, agentSearch);
	}

	@Override
	public SearchTask searchTask() throws SOSActionException {
		ArrayList<ShapeInArea> resultShapes = new ArrayList<ShapeInArea>();
//		if (!me.model().refuges().isEmpty())
//			resultShapes.add(new ShapeInArea(me.model().refuges().get(0).getApexes(), me.model().refuges().get(0)));
//		else
//			resultShapes.add(new ShapeInArea(me.model().roads().get(0).getApexList(), me.model().roads().get(0)));

		Path randomPath = me.move.getBfs().getDummyRandomWalkPath();
		resultShapes.add(new ShapeInArea(randomPath.getDestination().first().getApexList(), randomPath.getDestination().first()));
		return new SearchTask(resultShapes);
	}

	@Override
	public SearchType getType() {
		return SearchType.DummySearch;
	}
}
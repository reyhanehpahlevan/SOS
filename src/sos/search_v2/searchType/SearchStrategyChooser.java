package sos.search_v2.searchType;

import java.lang.reflect.InvocationTargetException;

import sos.base.SOSAgent;
import sos.base.entities.Area;
import sos.base.entities.Human;
import sos.base.util.geom.ShapeInArea;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchWorldModel;

/**
 * @author Yoosef Golshahi
 * @param <E>
 */
public class SearchStrategyChooser<E extends Human> {
	public CivilianSearch<E> civilianSearch;
	public CivilianUpdateSearch<E> civilianUpdateSearch;
	public FireSearch<E> fireSearch;
	public CombinedSearch<E> combinedSearch;
	public ClusterCombinedSearch<E> clusterCombinedSearch;
	public Area target;
	public ShapeInArea shapeTarget;
//	public StarSearch<E> starSearch;
	public CommunicationlessSearch<E> noCommunication;
	public BlockSearch<E> blockSearch;
	public DummySearch<E> dummySearch;
	public SearchStrategy<?> noCommGathering;
	public BlockSearchNoComm<E> blockSearchNoComm;

	public SearchStrategyChooser(SOSAgent<E> me, AgentSearch<?> agentSearch, SearchWorldModel<E> searchWorld, Class<? extends AgentSearchScore> scoreClass) {

		try {
			AgentSearchScore score = scoreClass.getConstructor(new Class[] { SOSAgent.class }).newInstance(me);
			civilianSearch = new CivilianSearch<E>(me, searchWorld, score, agentSearch);
			fireSearch = new FireSearch<E>(me, searchWorld, score, agentSearch);
			combinedSearch = new CombinedSearch<E>(me, searchWorld, score, agentSearch);
			clusterCombinedSearch= new ClusterCombinedSearch<E>(me, searchWorld, score, agentSearch);
//			starSearch = new StarSearch<E>(me, searchWorld, StarSearchType.RJS_THRESHOLD, score, agentSearch);

			blockSearch = new BlockSearch<E>(me, searchWorld, score, agentSearch);
			blockSearchNoComm = new BlockSearchNoComm<E>(me, searchWorld, score, agentSearch);
			civilianUpdateSearch = new CivilianUpdateSearch<E>(me, searchWorld, score, agentSearch);
			noCommunication=new CommunicationlessSearch<E>(me, searchWorld, score, agentSearch);
			dummySearch=new DummySearch<E>(me, searchWorld, score, agentSearch);
			noCommGathering=new NoCommGatheringSearch<E>(me, searchWorld, score, agentSearch);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public SearchStrategy<E> getBestStrategy() {
		return combinedSearch;//TODO
	}
}

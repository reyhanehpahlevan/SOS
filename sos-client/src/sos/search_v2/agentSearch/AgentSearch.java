package sos.search_v2.agentSearch;

import java.util.ArrayList;
import java.util.Collection;

import sos.base.SOSAgent;
import sos.base.entities.Building;
import sos.base.entities.Civilian;
import sos.base.entities.Human;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageXmlConstant;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.message.structure.channel.VoiceChannel;
import sos.base.move.types.SearchMove;
import sos.base.util.SOSActionException;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.police_v2.PoliceForceAgent;
import sos.search_v2.sampling.SearchSampler;
import sos.search_v2.searchType.SearchStrategy;
import sos.search_v2.searchType.SearchStrategyChooser;
import sos.search_v2.tools.RemainingJobScorer;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.cluster.MapClusterType;
import sos.search_v2.tools.searchScore.AgentSearchScore;
import sos.search_v2.worldModel.SearchBuilding;
import sos.search_v2.worldModel.SearchWorldModel;

/**
 * @author Yoosef Golshahi
 * @param <E>
 */
public abstract class AgentSearch<E extends Human> implements MessageXmlConstant {
	protected SOSAgent<E> me;
	public SearchStrategyChooser<E> strategyChooser;
	private SearchWorldModel<E> searchWorld;
	public SearchType searchType = SearchType.None;
	private SearchSampler searchSampler = new SearchSampler(); //Added By Salim
	protected ArrayList<SearchStrategy<?>> searchTypes;
	private RemainingJobScorer remainingJobScorer;

	public static final double CIV_HEAR_BASE_PROB_SCORE = 5;

	public enum SearchType {
		None, CombinedSearch, CivilianSearch, FireSearch, BlockSearch, StarSearch, CivilianUpdateSearch, ClusterCombinedSearch, DummySearch,NoCommGatheringSearch;
	}

	public AgentSearch(SOSAgent<E> me, SearchWorldModel<E> searchWorld, MapClusterType<E> clusterType, Class<? extends AgentSearchScore> score) {

		this.me = me;
		this.setSearchWorld(searchWorld);
		searchWorld.cluster(clusterType);
		setRemainingJobScorer(new RemainingJobScorer(me));//Salim
		getRemainingJobScorer().setInitialRJ(me.model().searchWorldModel.getClusterData().getRemainingJob(this, 0));
		strategyChooser = new SearchStrategyChooser<E>(me, this, searchWorld, score);
		//Salim
		searchTypes = new ArrayList<SearchStrategy<?>>();
		initSearchOrder();
	}

	public abstract void initSearchOrder();

	public void preSearch() {
		long start = System.currentTimeMillis();
		log("presearch started!");
		for (SearchStrategy<?> ss : searchTypes) {
			ss.preSearch();
		}
		log("presearch finished time:" + (System.currentTimeMillis() - start) + "ms");
	}

	public void search() throws SOSActionException {
		//		preSearch();
		doActs(searchTypes.toArray(new SearchStrategy<?>[0]));
		moveToASafeArea();
	}

	public void doActs(SearchStrategy<?>... all) throws SOSActionException {
		if (me instanceof PoliceForceAgent)
			log().error(new Error(me + " cant use this function"));
		SearchTask task = doTaskActs(all);
		if (task != null)
			moveToShapes(task.getArea());
	}

	public void moveToShapes(Collection<ShapeInArea> targets) throws SOSActionException {
		me.move.moveToShape(targets, SearchMove.class);
	}

	public SearchTask doTaskActs(SearchStrategy<?>... all) {
		//		if (!(me instanceof PoliceForceAgent))
		//			throw new Error(me + " cant use this function");
		me.sosLogger.act.info("=========Search==========" + all);
		for (SearchStrategy<?> ss : all) {
			long start = System.currentTimeMillis();
			log("search " + ss.getClass().getSimpleName() + " started!");
			try {
				searchType = ss.getType();
				SearchTask st = ss.searchTask();
				if (st != null) {
					me.abstractStateLogger.logln(me.time()+":"+ss.getClass().getSimpleName()+"\t\t\t : target="+st+"\t\t\t :" + (System.currentTimeMillis() - start) + "ms");
					me.sosLogger.act.info("search finished" + ss.getClass().getSimpleName() + " return:" + st + " time:" + (System.currentTimeMillis() - start) + "ms");
					log("finished search " + ss.getClass().getSimpleName() + " return:" + st + " time:" + (System.currentTimeMillis() - start) + "ms");
					return st;
				}
			} catch (Exception e) {
				me.sosLogger.error(e);
			}
			me.sosLogger.act.info("search do nothing " + ss.getClass().getSimpleName() + " time:" + (System.currentTimeMillis() - start) + "ms");
			log("search " + ss.getClass().getSimpleName() + " time:" + (System.currentTimeMillis() - start) + "ms");
		}
		return null;
	}

	public SearchTask searchTask() {
		//		preSearch();
		return doTaskActs(searchTypes.toArray(new SearchStrategy<?>[0]));
	}

	public void moveToASafeArea() throws SOSActionException {
		if (!me.model().refuges().isEmpty()) {
			if (me.me().getPositionArea() instanceof Refuge)
				me.rest();
			me.move.moveStandard(me.model().refuges());
		} else if (me.me().getPositionArea() instanceof Building)
			me.move.moveStandard(me.model().roads());
		me.rest();
	}

	protected SearchStrategy<E> getStrategy() {
		return strategyChooser.getBestStrategy();

	}

	public SOSLoggerSystem log() {
		return me.sosLogger.search;
	}

	public void log(String st) {
		me.sosLogger.search.info(st);
	}

	public void hear(String header, DataArrayList data, StandardEntity sender, Channel channel) {
		if (sender instanceof Civilian) {
			me.sosLogger.search.info("say shenidam " + sender);
			if (!((Civilian) sender).isPositionDefined()) {
				if (channel instanceof VoiceChannel) {
					Collection<Building> buildingsInRange = me.model().getObjectsInRange(me.me().getX(), me.me().getY(), ((VoiceChannel) channel).getRange(), Building.class);//TODO
					SearchBuilding temp;
					for (Building b : buildingsInRange) {
						temp = getSearchWorld().getSearchBuildings().get(b.getBuildingIndex());
						try{
						temp.addCivProbability((Civilian)sender,CIV_HEAR_BASE_PROB_SCORE / buildingsInRange.size());
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					me.sosLogger.search.warn(channel + " is not voice channel");
				}
			}
		}
	}

	public SearchTask fireSearchTask() {
		return doTaskActs(strategyChooser.fireSearch);
	}

	//Salim
	public SearchSampler getSearchSampler() {
		return searchSampler;
	}

	//Salim
	public void setSearchSampler(SearchSampler searchSampler) {
		this.searchSampler = searchSampler;
	}

	public SearchWorldModel<E> getSearchWorld() {
		return searchWorld;
	}

	public void setSearchWorld(SearchWorldModel<E> searchWorld) {
		this.searchWorld = searchWorld;
	}

	public void fireSearch() throws SOSActionException {
		doActs(strategyChooser.fireSearch);
	}

	public SearchTask blockSearchTask() {
		return doTaskActs(strategyChooser.blockSearch);
	}

	public void civilianSearch() throws SOSActionException {
		doActs(strategyChooser.civilianSearch);
	}

	public SearchTask combinedSearchTask() {
		return doTaskActs(strategyChooser.combinedSearch);
	}

	public SearchTask civilianSearchTask() {
		return doTaskActs(strategyChooser.civilianSearch);
	}

	public void combinedSearch() throws SOSActionException {
		doActs(strategyChooser.combinedSearch);
	}

	public RemainingJobScorer getRemainingJobScorer() {
		return remainingJobScorer;
	}

	public void setRemainingJobScorer(RemainingJobScorer remainingJobScorer) {
		this.remainingJobScorer = remainingJobScorer;
	}
}

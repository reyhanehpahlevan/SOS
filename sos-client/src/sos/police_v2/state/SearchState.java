package sos.police_v2.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rescuecore2.worldmodel.EntityID;
import sos.base.entities.Building;
import sos.base.entities.Road;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.move.types.PoliceMove;
import sos.base.util.SOSActionException;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.state.preCompute.Task;
import sos.search_v2.tools.SearchTask;
import sos.search_v2.tools.cluster.ClusterData;
import sos.search_v2.worldModel.SearchBuilding;

public class SearchState extends PoliceAbstractState {

	public SearchState(PoliceForceAgent policeForceAgent) {
		super(policeForceAgent);
	}

	@Override
	public void act() throws SOSActionException {
		log.info("acting as:" + this.getClass().getSimpleName());
		//		randomSearch();
		try{
			searchLowCommunicationHints();
		}catch (SOSActionException e) {
			throw e;
		}catch (Exception e) {
			log.error(e);
		}
		if(agent.isTimeToActFinished())
			randomSearch();
		SearchTask task = agent.newSearch.searchTask();
		handleTask(task);
		
		randomSearch();

		//handleTask(agent.mySearch.chooseStrategyAndSearch());
	}

	private void searchLowCommunicationHints() throws SOSActionException {
		if (agent.messageSystem.type == Type.LowComunication) {
			log.info("checking low Communication Search!!!");
			SearchTask task = agent.newSearch.fireSearchTask();
			if(task!=null&&!task.getArea().isEmpty())
				handleTask(task);
			Building best = openInClusterCivilian(agent.model().searchWorldModel.getClusterData());
			if (best != null){
				log.info("Acting as low Communication Search!!!"+best);
				move(best);
			}
			task = agent.newSearch.blockSearchTask();
			if(task!=null&&!task.getArea().isEmpty())
				handleTask(task);
			task = agent.newSearch.civilianSearchTask();
			if(task!=null&&!task.getArea().isEmpty())
				handleTask(task);
			
			ArrayList<ClusterData> cds=new ArrayList<ClusterData>(agent.model().searchWorldModel.getAllClusters());
			Collections.sort(cds,new Comparator<ClusterData>() {

				@Override
				public int compare(ClusterData o1, ClusterData o2) {
					double o1s = agent.newSearch.getRemainingJobScorer().remainingJobScore(o1);
					double o2s = agent.newSearch.getRemainingJobScorer().remainingJobScore(o2);
					if(o2s>o1s)
						return 1;
					if(o2s<o1s)
						return -1;
					return 0;
				}
			});
			int i=0;
			for (ClusterData cd : cds) {
				if(i++==5)
					break;
				openInClusterCivilian(cd);
			}
			
			
			task = agent.newSearch.combinedSearchTask();
			if(task!=null)
				handleTask(task);
			
			
		}
	}
	public Building openInClusterCivilian(ClusterData cd){
		Building best=null;
		long bestmove=Long.MAX_VALUE;
		for (Building b : cd.getBuildings()) {
			SearchBuilding s = agent.model().searchWorldModel.getSearchBuilding(b);
			if (s.getValidCivilianCountInLowCom() == 0)
				continue;
			if (!s.isReallyUnReachableInLowCom(true))
				continue;
			
			long tmpMoveScore = agent.move.getWeightTo(s.getRealBuilding(), s.getRealBuilding().getX(), s.getRealBuilding().getY(), PoliceMove.class);
			
			if (best == null || bestmove > tmpMoveScore) {
				best = s.getRealBuilding();
				bestmove = tmpMoveScore;
			}
			
		}	
		return best;
	}

	private void randomSearch() throws SOSActionException {
		log.info("policeRandomWalk");
		ArrayList<StandardEntity> result = new ArrayList<StandardEntity>();
		Collection<Road> roads = model().getObjectsInRange(agent.me(),(int) model().getBounds().getWidth()/5, Road.class);
		for (Road road : roads) {
			if (road.updatedtime() < 2)
				result.add(road);
		}
		log.debug("road that has not updated=" + result);
		if (result.isEmpty()) {
			Collection<Building> buildings= model().getObjectsInRange(agent.me(),(int) model().getBounds().getWidth()/5, Building.class);
			
			for (Building building : buildings) {
				if (!building.isSearchedForCivilian())
					result.add(building);
			}
			log.debug("unupdated roads are empty! building that has not updated=" + result);
		}
		StandardEntity dstEntity;
		if (result.isEmpty()) {
			log.debug("all entities are updated!!! now we are doing a dummy random walk");
			List<EntityID> a = agent.move.getBfs().getDummyRandomWalkPath().getIds();
			dstEntity = model().getEntity(a.get(a.size() - 1));
			result.add(dstEntity.getAreaPosition());
		}
		//		dstEntity = result.get(0);
		//		log.debug("Task "+dstEntity+" choosed...");
		if(!result.isEmpty())
			makeReachableTo(result.get(0));
	}

	public void handleTask(Task<? extends StandardEntity> task) throws SOSActionException {
		log.debug("Handeling task " + task);
		if (task == null) {
			log.warn("Noting to do in Search???");
			return;
		} else {
			makeReachableTo(task.getRealEntity());

			//			move(path)
		}

	}

	public void handleTask(SearchTask task) throws SOSActionException {
		log.debug("Handling task " + task);
		if (task == null) {
			log.warn("Noting to do in Search???");
			return;
		} else {
			moveToShape(task.getArea());
		}
	}

	@Override
	public void precompute() {
		// TODO Auto-generated method stub
		
	}

}

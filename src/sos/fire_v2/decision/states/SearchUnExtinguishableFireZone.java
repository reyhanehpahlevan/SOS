package sos.fire_v2.decision.states;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Center;
import sos.base.entities.FireBrigade;
import sos.base.entities.Refuge;
import sos.base.entities.StandardEntity;
import sos.base.message.structure.MessageConstants.Type;
import sos.base.message.structure.SOSBitArray;
import sos.base.message.structure.blocks.DataArrayList;
import sos.base.message.structure.channel.Channel;
import sos.base.sosFireZone.SOSEstimatedFireZone;
import sos.base.util.geom.ShapeInArea;
import sos.base.util.sosLogger.SOSLoggerSystem;
import sos.fire_v2.FireBrigadeAgent;
import sos.fire_v2.decision.FireInformationModel;
import sos.fire_v2.decision.tasks.ShapeSearchTask;
import sos.fire_v2.target.SOSFireZoneSelector;
import sos.fire_v2.target.SOSFireZoneSelector.TaskType;
import sos.fire_v2.target.SOSSelectTarget.SelectStrategy;
import sos.fire_v2.target.Tools;
import sos.tools.decisionMaker.definitions.commands.SOSTask;
import sos.tools.decisionMaker.implementations.stateBased.SOSEventPool;
import sos.tools.decisionMaker.implementations.stateBased.events.SOSEvent;
import sos.tools.decisionMaker.implementations.stateBased.states.SOSIState;

public class SearchUnExtinguishableFireZone extends SOSIState<FireInformationModel> {

	private final SOSFireZoneSelector fireZoneSelector;

	public SearchUnExtinguishableFireZone(FireInformationModel infoModel, SOSFireZoneSelector fireZoneSelector) {
		super(infoModel);
		this.fireZoneSelector = fireZoneSelector;
	}

	SOSLoggerSystem log;

	@Override
	public SOSTask<?> decide(SOSEventPool eventPool) {
		FireInformationModel infoM = infoModel;

		log = infoM.fireSearcher;

		log.info("Unextinguishable fire searcher");

		if (infoM.getAgent().me().getAreaPosition() instanceof Refuge && ((FireBrigade) infoM.getAgent().me()).getWater() < FireBrigadeAgent.maxWater)
		{
			log.info("Im in Refuge and filling water");
			return null;
		}
		//		if (!isNoComm() && fireZoneSelector.getMyTaskType((FireBrigade) infoM.getAgent().me()) != SOSFireZoneSelector.TaskType.SEARCHER)
		//			return null;
		//		log.info("I am Searcher   isNocomm::>"+isNoComm());
		//		//
		//		//		if (fireZoneSelector.getMyTaskType((FireBrigade) infoM.getAgent().me()) != SOSFireZoneSelector.TaskType.SEARCHER)
		//		//			return null;
		//		//
		//
		//		log.info("I am Searcher for this fire");

		SOSEstimatedFireZone ef = infoM.getLastSelectedFireZone();

		log.info("last selected fire zone " + ef);

		if ((ef == null || ef.isDisable()) && iAmSearcher()) {
			ef = fireZoneSelector.decide(null);
			log.info("select new fire zone " + ef);
		}

		if (ef == null)
			return null;

		log.info("is extinguishable " + ef.isExtinguishable());

		if (ef.isExtinguishable())
			return null;
		if (!canISearch(ef)) {
			log.info("I cant search");
			try{
				ef.getDangerBuildingForIgnit().clear();
			}catch(Exception e){
				e.printStackTrace();
			}
			return null;
		}

		log.info("before update building " + ef.getDangerBuildingForIgnit());

		update(ef.getDangerBuildingForIgnit());

		log.info("after update building " + ef.getDangerBuildingForIgnit());

		if (ef.getDangerBuildingForIgnit().size() == 0) {
			ef.setDisable(true, infoM.getTime(), true);
			return null;
		}
		ArrayList<ShapeInArea> x = searchTask(ef.getDangerBuildingForIgnit());
		if (x.size() == 0)
			return null;
		return new ShapeSearchTask(x, infoM.getTime());
	}

	private boolean canISearch(SOSEstimatedFireZone es) {

		try {
			if ((infoModel.getAgent().messageSystem.type == Type.NoComunication)){
				return true;
			}
			if (fireZoneSelector.getStrategy() == SelectStrategy.CLUSTER_GOOD) {
				if (fireZoneSelector.fireBrigade_Task.get(infoModel.getAgent().me()).getType() == TaskType.SEARCHER) {
					return true;
				}
				if (((FireBrigade)infoModel.getAgent().me()).distance(es) >  1 / 5d * infoModel.getModel().getBounds().getWidth()) {
					log.info("my distance is too ");
					return false;
				}
				if (fireZoneSelector.fireBrigade_Task.get(infoModel.getAgent().me()).getType() == TaskType.FREE) {
					return false;
				}
				if (isSearcherAbsent(es))
					if (isMyZone(es))
						return true;
			}
			if (fireZoneSelector.getStrategy() == SelectStrategy.CLUSTER_NORMAL) {
				if (fireZoneSelector.fireBrigade_Task.get(infoModel.getAgent().me()).getType() == TaskType.SEARCHER) {
					return true;
				}
				if (((FireBrigade)infoModel.getAgent().me()).distance(es) >  1 / 3d * infoModel.getModel().getBounds().getWidth()) {
					log.info("my distance is too ");
					return false;
				}
				if (fireZoneSelector.fireBrigade_Task.get(infoModel.getAgent().me()).getType() == TaskType.FREE) {
					return false;
				}
				if (isSearcherAbsent(es))
					if (isMyZone(es))
						return true;

			}
			if (fireZoneSelector.getStrategy() == SelectStrategy.NONE) {
				return true;
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean isMyZone(SOSEstimatedFireZone es) {
		ArrayList<Integer> x = Tools.getFireSiteLocation(es, fireZoneSelector.starCluster);
		for (int i : x) {
			try {
				if (fireZoneSelector.zone_FireBrigade.get(i).contains(infoModel.getAgent().me()))
				{
					log.info("fz is in my zone = true");
					return true;
				}
			} catch (Exception e) {
			}
		}
		log.info("fz is in my zone = false");
		return false;

	}

	private boolean isSearcherAbsent(SOSEstimatedFireZone sosEstimatedFireZone) {
		ArrayList<Integer> x = Tools.getFireSiteLocation(sosEstimatedFireZone, fireZoneSelector.starCluster);
		for (int i : x) {
			try {

				if (fireZoneSelector.zone_FireBrigade.get(i).get(0).getLastSenseTime() < 2) {
					log.info("searcher is absent --> i never have seen it "+fireZoneSelector.zone_FireBrigade.get(i).get(0));
					continue;
				}
				if (infoModel.getTime() - fireZoneSelector.zone_FireBrigade.get(i).get(0).updatedtime() < 15)
					if (fireZoneSelector.zone_FireBrigade.get(i).get(0).distance(sosEstimatedFireZone) > Math.min(200000, 1 / 6d * infoModel.getModel().getBounds().getWidth())) {
						log.info("searcher is absent --> i heared or saw it in last 15 cycle and its distance is much more .. "+fireZoneSelector.zone_FireBrigade.get(i).get(0));
						continue;
					}

				if (infoModel.getTime() - fireZoneSelector.zone_FireBrigade.get(i).get(0).updatedtime() > 13)
				{
					log.info("searcher is absent --> i didnot see or hear in last 13 "+fireZoneSelector.zone_FireBrigade.get(i).get(0));
					continue;
				}
				if (fireZoneSelector.zone_FireBrigade.get(i).get(0).getAreaPosition() instanceof Refuge) {
					log.info("searcher is absent --> it is in refuge "+fireZoneSelector.zone_FireBrigade.get(i).get(0));
					continue;
				}
				log.info("searcher is present   --> "+fireZoneSelector.zone_FireBrigade.get(i).get(0));
				
				return false;
			} catch (Exception e) {
			}
		}

		log.info(" searcher is absent");
		return true;
	}

	private boolean isNoComm() {
		return infoModel.getAgent().messageSystem.type == Type.NoComunication;
	}

	private boolean iAmSearcher() {
		return fireZoneSelector.getMyTaskType((FireBrigade) infoModel.getAgent().me()) == SOSFireZoneSelector.TaskType.SEARCHER;
	}

	//
	public ArrayList<ShapeInArea> searchTask(ArrayList<Building> buildings) {

		log.info("Execute Task  targets::> " + buildings);
		if (buildings.size() == 0)
			return null;
		ArrayList<ShapeInArea> toSearch = new ArrayList<ShapeInArea>();
		for (Area b : buildings) {
			for (ShapeInArea sh : ((Building) b).fireSearchBuilding().sensibleAreasOfAreas()) {
				if (!infoModel.getAgent().move.isReallyUnreachable(sh)) {
					toSearch.add(sh);
				}
			}
		}
		log.info("Sensible Area of Area::> " + toSearch);
		return toSearch;
	}

	private void update(ArrayList<Building> buildings) {
		buildings.removeAll(infoModel.getAgent().getVisibleEntities(Building.class));
		SOSLoggerSystem log = infoModel.fireSearcher;
		for (Iterator<Building> it = buildings.iterator(); it.hasNext();) {
			Building target = it.next();
			//			if (infoModel.getAgent().model().time() < 10 && target.updatedtime() < 4) {
			//				it.remove();
			//				log.info("\t\t remove updated   " + target);
			//				continue;
			//			}
			if (infoModel.getAgent().model().time() - target.updatedtime() < Math.min(6, infoModel.getTime() / 2) && target.virtualData[0].getTemperature() < 8) {
				it.remove();
				log.info("\t\t remove updated and low temp   " + target);
				continue;
			}
			if ((infoModel.getAgent().model().time() - target.updatedtime() < Math.min(4, infoModel.getTime() / 2) && target.virtualData[0].getTemperature() < 20)) {
				it.remove();
				log.info("\t\t remove update normal temp   " + target);
				continue;
			}
			if (target.getGroundArea() < 40) {
				it.remove();
				log.info("\t\t remove area   " + target);
				continue;
			}
			if (target.virtualData[0].getFieryness() >= 7) {
				it.remove();
				log.info("\t\t remove fierness  " + target);
				continue;
			}
			if (target instanceof Refuge || target instanceof Center) {
				it.remove();
				log.info("\t\t remove refuge or center   " + target);
				continue;
			}
		}

	}

	@Override
	public void giveFeedbacks(List feedbacks) {
	}

	@Override
	public void skipped() {
	}

	@Override
	public void overTaken() {

	}

	@Override
	protected void handleEvent(SOSEvent sosEvent) {
	}

	@Override
	public void hear(String header, DataArrayList data, SOSBitArray dynamicBitArray, StandardEntity sender, Channel channel) {
	}

	@Override
	public String getName() {
		return "SearchUnExtinguishableFireZone";
	}

	@Override
	public void taken() {
		super.taken();
		((FireBrigadeAgent) infoModel.getAgent()).FDK.lastState = getName();
	}
}

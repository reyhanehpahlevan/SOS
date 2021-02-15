package sosNamayangar;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.WorldModelListener;
import sosNamayangar.message_decoder.MessageHandler;
import sosNamayangar.message_decoder.ReadXml;

public class SOSWorldModel extends StandardWorldModel {

	// private final EntityID agentId;
	private MessageHandler messageHandler;

	public SOSWorldModel(EntityID agentId, Config config) {
		// this.agentId = agentId;

		addWorldModelListener(new entityAddRemoveListener());
		messageHandler = new MessageHandler(this, agentId);

	}

	/* ///////////////////S.O.S instants////////////////// */
	/* ////////////////////End of S.O.S/////////////////// */
	/* ////////////////////////BASE/////////////////////// */
	/**/private ArrayList<Civilian> civilians = new ArrayList<Civilian>(70);
	/**/private List<FireBrigade> fireBrigades = new ArrayList<FireBrigade>();
	/**/private List<PoliceForce> policeForces = new ArrayList<PoliceForce>();
	/**/private List<AmbulanceTeam> ambulanceTeams = new ArrayList<AmbulanceTeam>();
	/**/private ArrayList<Human> humans = new ArrayList<Human>(100);
	/**/private List<Human> agents = new ArrayList<Human>();
	/**/private List<Building> centers = new ArrayList<Building>();
	/**/private List<Building> buildings = new ArrayList<Building>(1500);
	/**/private List<Road> roads = new ArrayList<Road>(1500);
	/**/private ArrayList<Refuge> refuges = new ArrayList<Refuge>();
	/**/private ArrayList<Blockade> blockades = new ArrayList<Blockade>();
	/**/private List<Area> areas = new ArrayList<Area>(2000);// aramik
	private Point mapCenter = null;
	private boolean isPrecomputed = false;

	public void precompute() {
		if (isPrecomputed)
			return;
		numerizeRealObjects();
		isPrecomputed = true;
	}

	private void numerizeRealObjects() {
		// ******Sorted by their x
		Collections.sort(buildings, new SortComparator<Building>());
		buildings = Collections.unmodifiableList(buildings);
		Collections.sort(roads, new SortComparator<Road>());
		roads = Collections.unmodifiableList(roads);
		Collections.sort(areas, new SortComparator<Area>());
		areas = Collections.unmodifiableList(areas);
		// ******Sorted by their Id
		Collections.sort(fireBrigades, new SortComparator<FireBrigade>());
		fireBrigades = Collections.unmodifiableList(fireBrigades);
		Collections.sort(centers, new SortComparator<Building>());
		centers = Collections.unmodifiableList(centers);
		Collections.sort(ambulanceTeams, new SortComparator<AmbulanceTeam>());
		ambulanceTeams = Collections.unmodifiableList(ambulanceTeams);
		Collections.sort(policeForces, new SortComparator<PoliceForce>());
		policeForces = Collections.unmodifiableList(policeForces);
		Collections.sort(agents, new SortComparator<Human>());
		agents = Collections.unmodifiableList(agents);
		// ***************************************************Setting index to
		// objects
		// for (int i = 0; i < buildings().size(); i++)
		// buildings().get(i).setBuildingIndex((short) i);
		// for (int i = 0; i < roads().size(); i++)
		// roads().get(i).setRoadIndex((short) i);
		// for (int i = 0; i < areas().size(); i++)
		// areas().get(i).setAreaIndex((short) i);
		// // fire section
		// for (int i = 0; i < fireBrigades().size(); i++)
		// fireBrigades().get(i).setFireIndex((short) i);
		// // ambulance section
		// for (int i = 0; i < ambulanceTeams().size(); i++)
		// ambulanceTeams().get(i).setAmbIndex((short) i);
		// // police section
		// for (int i = 0; i < policeForces().size(); i++)
		// policeForces().get(i).setPoliceIndex((short) i);
		// // centers section
		// for (int i = 0; i < centers().size(); i++)
		// centers().get(i).setCenterIndex((short) i);
		// // ALL AGENTS
		// for (int i = 0; i < agents().size(); i++)
		// agents().get(i).setAgentIndex((short) i);
	}

	// ------------------------------------------------------------------------------------------------//
	public final class SortComparator<C extends StandardEntity> implements java.util.Comparator<C>, java.io.Serializable {
		private static final long serialVersionUID = -123456789123525L;

		@Override
		public int compare(C ro1, C ro2) {
			if (ro1 instanceof Human){
				if (ro1.getID().getValue() > ro2.getID().getValue())
					return 1;
			}else if (ro1 instanceof Area) {

				if (((Area) ro1).getX() > ((Area) ro2).getX() || ((Area) ro1).getX() == ((Area) ro2).getX() && ((Area) ro1).getY() > ((Area) ro2).getY())
					return 1;
			} else
				System.err.println("unsuported:"+ro1.getClass().getSimpleName());
			return -1;
		}
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	public ArrayList<Civilian> civilians() {
		return civilians;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	public ArrayList<Human> humans() {
		return humans;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link agent}
	 */
	public List<Human> agents() {
		return agents;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link FireBrigade}
	 */
	public List<FireBrigade> fireBrigades() {
		return fireBrigades;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link PoliceForce}
	 */
	public List<PoliceForce> policeForces() {
		return policeForces;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link AmbulanceTeam}
	 */
	public List<AmbulanceTeam> ambulanceTeams() {
		return ambulanceTeams;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link Center}
	 */
	public List<Building> centers() {
		return centers;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link Building}
	 */
	public List<Building> buildings() {
		return buildings;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link Road}
	 */
	public List<Road> roads() {
		return roads;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	public List<Refuge> refuges() {
		return refuges;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	public ArrayList<Blockade> blockades() {
		return blockades;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	/**
	 * 
	 * @return Unmodifiable {@link Area}
	 */
	public List<Area> areas() {
		return areas;
	}

	// DON'T ADD ANY instant or method HEAR!!!!!!
	private class entityAddRemoveListener implements WorldModelListener<StandardEntity> {
		// DON'T ADD ANY instant or method HEAR!!!!!!
		@Override
		public void entityAdded(WorldModel<? extends StandardEntity> model, StandardEntity e) {
			if (e instanceof Human) {
				humans.add((Human) e);
				if (!(e instanceof Civilian))
					agents.add((Human) e);

				if (e instanceof FireBrigade)
					fireBrigades.add((FireBrigade) e);
				else if (e instanceof PoliceForce)
					policeForces.add((PoliceForce) e);
				else if (e instanceof AmbulanceTeam)
					ambulanceTeams.add((AmbulanceTeam) e);
				else if (e instanceof Civilian) {
					civilians.add((Civilian) e);
				}
			} else {
				if (e instanceof Area)
					areas.add((Area) e);
				if (e instanceof Building) {
					buildings.add((Building) e);
					if (e instanceof Refuge)
						refuges.add((Refuge) e);
					else if (((Building) e).getStandardURN() == StandardEntityURN.AMBULANCE_CENTRE || ((Building) e).getStandardURN() == StandardEntityURN.FIRE_STATION
							|| ((Building) e).getStandardURN() == StandardEntityURN.POLICE_OFFICE)
						centers.add((Building) e);
				} else {
					if (e instanceof Road)
						roads.add((Road) e);
					else if (e instanceof Blockade)
						blockades.add((Blockade) e);
				}

			}
		}

		// DON'T ADD ANY instant or method HEAR!!!!!!
		@Override
		public void entityRemoved(WorldModel<? extends StandardEntity> model, StandardEntity e) {
			// TODO Auto-generated method stub
			if (e instanceof Human) {
				humans.remove(e);
				if (e instanceof FireBrigade)
					fireBrigades.remove(e);
				else if (e instanceof PoliceForce)
					policeForces.remove(e);
				else if (e instanceof AmbulanceTeam)
					ambulanceTeams.remove(e);
				else if (e instanceof Civilian)
					civilians.remove(e);
			} else {
				if (e instanceof Building) {
					buildings.remove(e);
					if (e instanceof Refuge)
						refuges.remove(e);
					else if (((Building) e).getStandardURN() == StandardEntityURN.AMBULANCE_CENTRE || ((Building) e).getStandardURN() == StandardEntityURN.FIRE_STATION
							|| ((Building) e).getStandardURN() == StandardEntityURN.POLICE_OFFICE)
						centers.remove(e);
				} else {
					if (e instanceof Road)
						roads.remove(e);
					else if (e instanceof Blockade)
						blockades.remove(e);
				}
			}
		}
	}

	/* /////////////////////END of BASE///////////////////// */
	/* ////////////////////S.O.S Methods////////////////// */

	// Ali
	public void removeBlockade(Blockade blockade) {
		if (blockade == null)
			return;
		removeStandardEntity(blockade, blockades());
		blockade.setRepairCost(-1);
		removeStandardEntity(blockade, ((Area) getEntity(blockade.getPosition())).getBlockades());
	}

	private void removeStandardEntity(StandardEntity se, List<EntityID> arr) {
		int index = -1;
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i).getValue() == se.getID().getValue()) {
				index = i;
				break;
			}
		}
		if (index == -1)
			return;
		arr.remove(index);
	}

	private void removeStandardEntity(StandardEntity se, ArrayList<? extends StandardEntity> arr) {
		int index = -1;
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i).getID().getValue() == se.getID().getValue()) {
				index = i;
				break;
			}
		}
		if (index == -1)
			return;
		arr.remove(index);
	}

	public Point mapCenter() {
		if (mapCenter == null)
			mapCenter = new Point((int) getBounds().getCenterX(), (int) getBounds().getCenterY());
		return mapCenter;
	}

	public void merge(ChangeSet changeSet, int time) {
		super.merge(changeSet);
		for (EntityID e : changeSet.getChangedEntities()) {
			Entity existingEntity = getEntity(e);
			if (existingEntity != null && existingEntity instanceof StandardEntity)
				((StandardEntity) existingEntity).setLastSenseTime(time);
		}
		ArrayList<Blockade> removeList=new ArrayList<Blockade>();
		for (StandardEntity blockadee : getEntitiesOfType(StandardEntityURN.BLOCKADE)) {
			Blockade blockade = (Blockade)blockadee;
			if(!((Area)getEntity(blockade.getPosition())).getBlockades().contains(blockade.getID())){
				removeList.add(blockade);
			}
		}
		for (Blockade blockade : removeList) {
			removeBlockade(blockade);
			removeEntity(blockade.getID());
			
		}
		
		precompute();
	}

	public void mergeWithHearing(Collection<Command> hearing, int time) {
		if (hearing.size() == 0)
			return;
		if (getAllEntities().size() < 100)
			return;
		new ReadXml(this);

		messageHandler.handleReceive(hearing, time);
	}

	public Config getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

}

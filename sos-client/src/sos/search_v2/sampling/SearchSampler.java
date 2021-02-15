package sos.search_v2.sampling;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import sos.base.SOSConstant;
import sos.base.SOSWorldModel;
import sos.base.entities.Area;
import sos.base.entities.Building;
import sos.base.entities.Center;
import sos.base.entities.Civilian;
import sos.base.entities.Refuge;
import sos.base.entities.Road;
import sos.search_v2.agentSearch.AgentSearch;
import sos.search_v2.tools.RegionAttribute;
import sos.search_v2.tools.cluster.ClusterData;

/**
 * @author Salim
 */
public class SearchSampler {
	private SearchDatabase database;
	public static final int TIME_PERIOD = 10;

	public SearchSampler() {
		try {
			if (SOSConstant.SEARCH_SAMPLING)
				database = new SearchDatabase(SOSConstant.MYSQL_URL, SOSConstant.MYSQL_USER, SOSConstant.MYSQL_PASS);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void sampleRegions(SOSWorldModel model, AgentSearch<?> search) {
		System.out.println("Should i sample?");
		if (!SOSConstant.SEARCH_SAMPLING)
			return;

		if (model.time() % TIME_PERIOD != 0)
			return;
		System.out.println("Sampling :D");
		ArrayList<RegionAttribute> attributes = null;

		for (ClusterData cluster : search.getSearchWorld().getAllClusters()) {

			attributes = countAttributes(model, search, cluster.getBuildings());
			try {
				database.addRegionInfo(attributes);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private ArrayList<RegionAttribute> countAttributes(SOSWorldModel model, AgentSearch<?> search, HashSet<Building> cluster) {
		ArrayList<RegionAttribute> attributes = new ArrayList<RegionAttribute>();

		int searched = 0;
		int refuges = 0;
		int centers = 0;
		int fiery = 0;
		int oldfieries = 0;
		int burnt = 0;
		int civilians = 0;
		int diedCivilians = 0;
		int buildingsWithCivilians = 0;
		int temperatureBuildings = 0;
		float temperatureSum = 0;
		double totalArea = 0;
		double fieryArea = 0;
		double burntArea = 0;
		double searchedArea = 0;
		int floors = 0;
		int brokenessSum = 0;
		int buriednessSum = 0;
		double hpSum = 0;
		int roads = 0;
		int entrances = 0;
		int recentSearchedBuildings = 0;
		double civProbabilitySum = 0;
		int rescuedCivilians = 0;
		int recentFoundCivilians = 0;

		for (Building building : cluster) {
			//Refuges
			if (building instanceof Refuge)
				refuges++;
			//Centers
			if (building instanceof Center)
				centers++;
			// Searched
			if (model.searchWorldModel.getSearchBuilding(building).wasLastBurning())
				oldfieries++;

			if (building.isSearchedForCivilian())
				searched++;
			if ((model.time() - (Math.max(building.getLastMsgTime(), building.getLastSenseTime())) > TIME_PERIOD))
				recentSearchedBuildings++;
			// Fiery
			if (building.isBurning())
				fiery++;
			// Burnt
			if (building.isFierynessDefined() && building.getFieryness() == Fieryness.BURNT_OUT.ordinal())
				burnt++;
			// Civilians
			civilians += building.getCivilians().size();
			// Civilians
			for (Civilian civ : building.getCivilians()) {
				if (!civ.isAlive())
					diedCivilians++;
				buriednessSum += civ.getBuriedness();
				hpSum += civ.getHP();
				civProbabilitySum += model.searchWorldModel.getSearchBuilding(building).getCivProbability();
			}

			// Buildings with Civilians
			if (building.getCivilians().size() > 0)
				buildingsWithCivilians++;
			// Temperature Buildings
			if (building.getTemperature() > 0)
				temperatureBuildings++;
			// Areas
			totalArea += building.getGroundArea();
			if (building.isBurning())
				fieryArea += building.getGroundArea();
			if (building.isFierynessDefined() && building.getFieryness() == Fieryness.BURNT_OUT.ordinal())
				burntArea += building.getGroundArea();
			if (building.isSearchedForCivilian())
				searchedArea += building.getGroundArea();

			//Floors
			floors += building.getFloors();
			//Damage
			brokenessSum += building.getBrokenness();
			//Temperature
			temperatureSum += building.getTemperature();

			//Roads
			roads += building.getRoadsInSight().size();
			for (Area r : building.getRoadsInSight()) {
				if (((Road) r).isEntrance())
					entrances++;
			}
			//----------------------------
			//----------------------------
			//Set attributes for next time
			setBuildingAttributes(model, building);
			for (Civilian c : model.civilians()) {
				if (c.getID().equals(building.getID())) {
					if ((model.time() - c.getFoundTime()) > TIME_PERIOD) {
						recentFoundCivilians++;
					}
					if (!c.getID().equals(c.getPosition().getID()))
						rescuedCivilians++;
				}
			}
		}

		// ----- Number of Buildings 
		attributes.add(new RegionAttribute("map", "'" + model.me().getAgent().getMapInfo().getRealMapName() + "'"));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("searched_buildings", "'" + searched + "'"));
		attributes.add(new RegionAttribute("fiery_buildings", "" + fiery + ""));
		attributes.add(new RegionAttribute("burnt_buildings", "" + burnt + ""));
		attributes.add(new RegionAttribute("civilians", "" + Math.max(civilians, 1) + ""));
		attributes.add(new RegionAttribute("buildings_with_civilians", "" + buildingsWithCivilians + ""));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("temperature_buildings", "" + temperatureBuildings + ""));
		attributes.add(new RegionAttribute("percent_fiery_area", "" + fieryArea / Math.max(totalArea, 1) + ""));
		attributes.add(new RegionAttribute("percent_burnt_area", "" + burntArea / Math.max(totalArea, 1) + ""));
		attributes.add(new RegionAttribute("percent_searched_area", "" + searchedArea / Math.max(totalArea, 1) + ""));
		attributes.add(new RegionAttribute("died_civilians", "" + diedCivilians / Math.max(civilians, 1) + ""));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("time", "" + model.time() + ""));
		attributes.add(new RegionAttribute("mean_floor", "" + floors / cluster.size() + ""));
		attributes.add(new RegionAttribute("mean_brokeness", "" + brokenessSum / cluster.size() + ""));
		attributes.add(new RegionAttribute("mean_area", "" + totalArea / cluster.size() + ""));
		attributes.add(new RegionAttribute("mean_buriedness", "" + buriednessSum / Math.max(civilians, 1) + ""));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("mean_hp", "" + hpSum / Math.max(civilians, 1) + ""));
		attributes.add(new RegionAttribute("mean_buildings_temperture", "" + temperatureSum / cluster.size() + ""));
		attributes.add(new RegionAttribute("roads", "" + roads + ""));
		attributes.add(new RegionAttribute("entrances", "" + entrances + ""));
		attributes.add(new RegionAttribute("buildings", "" + cluster.size()));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("recent_searched_buildings", "" + recentSearchedBuildings + ""));
		attributes.add(new RegionAttribute("prob_civilian_mean", "" + civProbabilitySum / Math.max(civilians, 1) + ""));
		attributes.add(new RegionAttribute("portion_cfb_pfb", "" + oldfieries / Math.max(fiery, 1) + ""));
		attributes.add(new RegionAttribute("refuges", "" + refuges + ""));
		attributes.add(new RegionAttribute("centers", "" + centers + ""));
		// =====================================================================================
		// =====================================================================================
		// =====================================================================================
		attributes.add(new RegionAttribute("rescued_civilians", "" + rescuedCivilians + ""));
		attributes.add(new RegionAttribute("recent_found_civilians", "" + recentFoundCivilians + ""));

		return attributes;
	}

	private void setBuildingAttributes(SOSWorldModel model, Building building) {
		model.searchWorldModel.getSearchBuilding(building).setLastFieryness();
	}
}

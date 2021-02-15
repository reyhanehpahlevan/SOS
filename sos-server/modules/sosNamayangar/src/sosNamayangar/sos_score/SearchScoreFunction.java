package sosNamayangar.sos_score;

import rescuecore2.score.AbstractScoreFunction;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.Timestep;

import java.util.HashMap;
import java.util.HashSet;

import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.PoliceForce;

/**
 * Score function that measures how quickly civilians are discovered by agents.
 */
public class SearchScoreFunction extends AbstractScoreFunction {
	//    private Set<EntityID> found;
	private HashMap<Integer, HashSet<EntityID>> found_time = new HashMap<Integer, HashSet<EntityID>>();

	/**
	 * Construct a DiscoveryScoreFunction.
	 */
	public SearchScoreFunction() {
		super("Search Score Function");
		//        found=new HashSet<>();
	}

	@Override
	public void initialise(WorldModel<? extends Entity> world, Config config) {
		//        found = new HashSet<EntityID>();
	}

	@Override
	public double score(WorldModel<? extends Entity> world, Timestep timestep) {
		if (!found_time.containsKey(timestep.getTime()))
			found_time.put(timestep.getTime(), new HashSet<EntityID>());
		// Look for agents that observed a civilian
		for (EntityID next : timestep.getAgentsWithUpdates()) {
			Entity agent = world.getEntity(next);
			// Only platoon agents can discover civilians
			if (!isPlatoonAgent(agent)) {
				continue;
			}
			ChangeSet perception = timestep.getAgentPerception(next);
			if (perception == null) {
				System.out.println("Why perception is null???");
				continue;
			}
			FOR: for (EntityID observedID : perception.getChangedEntities()) {
				// Is it already seen?
				for (int i = 0; i <= timestep.getTime(); i++) {
					if (found_time.get(i)!=null&&found_time.get(i).contains(observedID))
						continue FOR;
				}
				Entity e = world.getEntity(observedID);
				if (e instanceof Civilian && !perception.getChangedProperties(observedID).isEmpty()) {
					// Seen a new civilian with at least one updated property.
					//                    found.add(observedID);
					found_time.get(timestep.getTime()).add(observedID);
					//                    sum += timestep.getTime();
				}
			}
		}
		int count = 0;
		for (int i = 0; i <= timestep.getTime(); i++) {
			if(found_time.get(i)==null)
				found_time.put(i, new HashSet<EntityID>());
			count += found_time.get(i).size();

		}
		//        return sum;
		return count;
	}

	private boolean isPlatoonAgent(Entity e) {
		return e instanceof FireBrigade || e instanceof PoliceForce || e instanceof AmbulanceTeam;
	}
}

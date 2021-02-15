package rescuecore2.standard.score;

import rescuecore2.score.AbstractScoreFunction;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.Timestep;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.misc.AgentPath;

/**
   A score function that measures the distance travelled by all agents.
*/
public class DistanceTravelledScoreFunction extends AbstractScoreFunction {
    /**
       Construct a DistanceTravelledScoreFunction.
    */
    public DistanceTravelledScoreFunction() {
        super("Distance travelled");
    }

    double sum = 0;
    @Override
    public double score(WorldModel<? extends Entity> world, Timestep timestep) {
        StandardWorldModel model = StandardWorldModel.createStandardWorldModel(world);
        // Find out how far each agent moved
        for (Entity next : model) {
            if (next instanceof FireBrigade
                || next instanceof PoliceForce
                || next instanceof AmbulanceTeam) {
                Human h = (Human)next;
                AgentPath path = AgentPath.computePath(h, model);
                if (path != null) {
                    sum += path.getLength()/1000;
                }
            }
        }
//        System.out.println("Total distance travelled: " + sum);
        return sum;
    }
}

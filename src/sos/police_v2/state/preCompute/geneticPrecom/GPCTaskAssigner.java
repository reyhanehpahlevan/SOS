package sos.police_v2.state.preCompute.geneticPrecom;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.OnePointCrossover;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import sos.base.entities.Area;
import sos.base.entities.PoliceForce;
import sos.base.entities.StandardEntity;
import sos.base.util.genetic.ExchangeMutation;
import sos.base.util.genetic.SOSGeneticAlgorithm;
import sos.base.util.genetic.SOSListPopulation;
import sos.base.util.genetic.SOSTournamentSelection;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.preCompute.Task;
import sos.police_v2.state.preCompute.WorldTaskDijkstra;

/**
 * Genetic Assigner is using genetic to assign clusters to agent due to their distance and some other parameters to agents
 * 
 * @author Hesam
 */
public class GPCTaskAssigner {
	/**
	 * Max Task That one police can assign
	 */
	public static int MAX_TASK;

	/**
	 * The pointer to word model
	 */
	private PoliceWorldModel model;
	/**
	 * Population size of the genetic
	 */
	public int GENERATIONS = 10000;
	/**
	 * Population size of the genetic
	 */
	public int POPULATION_SIZE = 70;
	/**
	 * Max Time in milliseconds
	 */
	public static final int MAX_TIME = 200;
	/**
	 * The ratio that best chromsomes are passed to the next generation
	 */
	public static final double ELITISM_RATE = 0.2;
	/**
	 * Mutation rate shows the percent of mutated chromosomes
	 */
	public static final double MUTATION_RATE = 0.4;
	/**
	 * See mutation rate
	 */
	public static final double CROSSOVER_RATE = 0.0;
	/**
	 * agents that want to assign cluster
	 */
	public static final int TOURNOMENT_ARITY = 2;

	/**
	 * The rate of mutation for exchanging genes
	 */
	public static final float EXCHANGE_MUTATTION_RATE = 0.1f;
	/**
	 * The agents wants assign cluster
	 */
	private List<PoliceForce> agents;
	private RandomGenerator random;
	private Random random2=new Random(1);
	WorldTaskDijkstra worldTaskDijkstra;
	ArrayList<Task<? extends StandardEntity>> tasks;

	public GPCTaskAssigner(PoliceWorldModel model, WorldTaskDijkstra dijkstra, ArrayList<Task<? extends StandardEntity>> tasks, List<PoliceForce> agents) {
		this.model = model;
		this.agents = agents;
		this.tasks = tasks;
		this.worldTaskDijkstra = dijkstra;
		MAX_TASK = (tasks.size() / agents.size()) + 4;
		random = new JDKRandomGenerator();
		random.setSeed(1);
		

	}

	/**
	 * Assigns clusters to fire brigades. Each Cluster may be assigned to more than one fire brigade. Chromose's lenght is equal to number of the agents and shows that the ith agent is assigned to the ith cluaster of the lsit
	 * 
	 * @param zones
	 * @return
	 */
	public List<PoliceForce> decide() {
		long t1 = System.currentTimeMillis();
		GPTAChromosome decision = (GPTAChromosome) doGenetic();
		long t2 = System.currentTimeMillis();
		System.out.println("Genetic Time=" + (t2 - t1));
		decision.printResult();
		//		decision.print=true;
		//		System.out.println("result= "+ decision.fitness());
		return checkForErrors(agents, decision);
	}

	/**
	 * only checks errors in the result and returns the input list
	 * 
	 * @param agents
	 * @param decision
	 * @return
	 */
	private List<PoliceForce> checkForErrors(List<PoliceForce> agents, GPTAChromosome chromosome) {
		//		if (chromosome == null || agents.size() != chromosome.getRepresentation().size())
		//			throw new Error("There is some error in cluster assigning");
		return chromosome.getRepresentation();
	}

	/**
	 * Instantiates Aima genetic and runs the aglorithm
	 * 
	 * @param agents
	 * @param tasks
	 * @return
	 */
	private Chromosome doGenetic() {
		int length = tasks.size();
		SOSGeneticAlgorithm ga = new SOSGeneticAlgorithm(new OnePointCrossover<GPTAChromosome>(), CROSSOVER_RATE, new ExchangeMutation<PoliceForce>(EXCHANGE_MUTATTION_RATE, random), MUTATION_RATE, new SOSTournamentSelection(TOURNOMENT_ARITY, random), new GPSelectBest());
		ga.setRandomGenerator(random);
		Population bestPop = ga.evolve(createInitialPopulation(POPULATION_SIZE, length), new GCACondition(ga, agents, GENERATIONS));

		return bestPop.getFittestChromosome();
	}

	/**
	 * Initial population is generated using a random variable. Each ClusterData may be selected more than once and their possibility is equal.
	 * 
	 * @param size
	 * @param length
	 * @param agents
	 * @param zones
	 * @return
	 */
	private Population createInitialPopulation(int size, int length) {
		Population pop = new SOSListPopulation(size);
		for (int i = 0; i < size; i++) {
			PoliceForce tmp[] = new PoliceForce[length];
			ArrayList<PoliceForce> randomBaseArray = new ArrayList<PoliceForce>();
			for (short j = 0; j < MAX_TASK; j++)
				randomBaseArray.addAll(agents);
			for (int j = 0; j < length; j++) {
				short select=-1;
				Collections.shuffle(randomBaseArray,random2);
				Area task = tasks.get(j).getAreaPosition();
				for (short j2 = 0; j2 < randomBaseArray.size(); j2++) {
					PoliceForce police = randomBaseArray.get(j2);
					Shape convexShape = model.searchWorldModel.getClusterData(police).getConvexShape();
					if(convexShape.contains(task.getX(),task.getY()))
						select=j2;
				}
				if(select==-1){
					int nearest = Integer.MAX_VALUE;
					for (short j2 = 0; j2 < randomBaseArray.size(); j2++) {
						PoliceForce police = randomBaseArray.get(j2);
						int distance = police.distance(task);
						if(distance<nearest){
							select=j2;
							nearest=distance;
						}
						
					}	
				}
				tmp[j] = randomBaseArray.remove(select);
			}
			pop.addChromosome(new GPTAChromosome(model, worldTaskDijkstra, tasks, tmp, agents));
		}
		return pop;
	}

}

package sos.police_v2.state.preCompute.geneticPrecom;

import java.util.List;

import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.StoppingCondition;

import sos.base.entities.PoliceForce;
import sos.base.util.genetic.SOSGeneticAlgorithm;

public class GCACondition implements StoppingCondition {

	private final SOSGeneticAlgorithm geneticAlg;
	private final List<PoliceForce> agents;
	private int maxGeneration;

	public GCACondition(SOSGeneticAlgorithm ga, List<PoliceForce> agents, int maxGeneration) {
		this.geneticAlg = ga;
		this.agents = agents;
		this.maxGeneration = maxGeneration;
	}

	@Override
	public boolean isSatisfied(Population population) {

		return geneticAlg.getGenerationsEvolved() > maxGeneration;
	}

}

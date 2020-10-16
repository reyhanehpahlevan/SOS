package sos.ambulance_v2.decision.states.helpstate;

import sos.base.entities.AmbulanceTeam;

/**
 * @author Reyhaneh
 */

class HelpFactors {

	private AmbulanceTeam ambulance;
	private int benefitForThatAT = 0;
	private int probablityofImportance = 0;
	private int timeItHas = 0;
	private int numberOfCivilianInATCluster = 0;

	public HelpFactors(AmbulanceTeam ambulance, int benefitForThatAT, int probablityofImportance, int timeItHas, int numberOfCivilianInATCluster) {
		this.setAmbulance(ambulance);
		this.setBenefitForThatAT(benefitForThatAT);
		this.setProbablityofImportance(probablityofImportance);
		this.setTimeItHas(timeItHas);
		this.setNumberOfCivilianInATCluster(numberOfCivilianInATCluster);
	}

	public int getBenefitForThatAT() {
		return benefitForThatAT;
	}

	public void setBenefitForThatAT(int benefitForThatAT) {
		this.benefitForThatAT = benefitForThatAT;
	}

	public int getProbablityofImportance() {
		return probablityofImportance;
	}

	public void setProbablityofImportance(int probablityofImportance) {
		this.probablityofImportance = probablityofImportance;
	}

	public int getTimeItHas() {
		return timeItHas;
	}

	public void setTimeItHas(int timeItHas) {
		this.timeItHas = timeItHas;
	}

	public int getNumberOfCivilianInATCluster() {
		return numberOfCivilianInATCluster;
	}

	public void setNumberOfCivilianInATCluster(int numberOfCivilianInATCluster) {
		this.numberOfCivilianInATCluster = numberOfCivilianInATCluster;
	}

	public AmbulanceTeam getAmbulance() {
		return ambulance;
	}

	public void setAmbulance(AmbulanceTeam ambulance) {
		this.ambulance = ambulance;
	}

}
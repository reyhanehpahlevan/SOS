/*package sos.ambulance.decision;

import java.util.Comparator;

import rescuecore2.misc.Pair;
import sos.ambulance_v2.decision.controller.ImaginationShot;
import sos.base.entities.Human;
public class AmbComparator{
	
}
//***********************************************************************************************************
//***********************************************************************************************************
class DeathTimeComparator implements Comparator<Human> {
	@Override
	public int compare(Human h1, Human h2) {
		if (h1.getRescueInfo().getDeathTime() > h2.getRescueInfo().getDeathTime())
			return 1;
		else if (h1.getRescueInfo().getDeathTime() < h2.getRescueInfo().getDeathTime())
			return -1;
		return 0;
	}
}

//***********************************************************************************************************
//***********************************************************************************************************
class PriorityComparator implements Comparator<Human> {

	@Override
	public int compare(Human h1, Human h2) {
		if (h1.getRescueInfo().getRescuePriority() < h2.getRescueInfo().getRescuePriority())
			return 1;
		if (h1.getRescueInfo().getRescuePriority() > h2.getRescueInfo().getRescuePriority())
			return -1;
		if (h1.getBuriedness() < h2.getBuriedness())
			return 1;
		if (h1.getBuriedness() > h2.getBuriedness())
			return -1;
		return 0;
	}
}

//***********************************************************************************************************
//***********************************************************************************************************
class CostComparator implements Comparator<Pair<Human,Long>> {

	@Override
	public int compare(Pair<Human,Long> h1, Pair<Human,Long> h2) {//Target cost
		if (h1.second() < h2.second())
			return 1;
		else if (h1.second()> h2.second())
			return -1;

		return 0;
	}
}

//***********************************************************************************************************
//***********************************************************************************************************
class ImaginationShotPriorityComparator implements Comparator<ImaginationShot> {

	@Override
	public int compare(ImaginationShot h1, ImaginationShot h2) {
		if (h1.target.getRescueInfo().getRescuePriority() < h2.target.getRescueInfo().getRescuePriority())
			return 1;
		else if (h1.target.getRescueInfo().getRescuePriority() > h2.target.getRescueInfo().getRescuePriority())
			return -1;

		return 0;
	}
}

//***********************************************************************************************************
//***********************************************************************************************************
class IdComparator implements Comparator<Human> {

	@Override
	public int compare(Human h1, Human h2) {
		if (h1.getID().getValue() < h2.getID().getValue())
			return 1;
		else if (h1.getID().getValue() > h2.getID().getValue())
			return -1;

		return 0;
	}
}
*/
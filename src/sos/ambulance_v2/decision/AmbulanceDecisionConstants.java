package sos.ambulance_v2.decision;

/**
 * Constants used in AmbulanceDecision
 * 
 * @author Salim
 */
public interface AmbulanceDecisionConstants {
	public static final short MAX_UNUPDATED_TIME = 3;
	public static final short TIME_NEED_TO_lOAD_CIVILIAN = 1;
	public static final short TIME_NEED_TO_UNlOAD_CIVILIAN = 1;
	public static final short START_OF_SIMULATION = 15;
	public static final short MIN_HIGH_LEVEL_BURIEDNESS = 30;
	public static final short MIDDLE_OF_SIMULATION = 120;
	public static final int MAX_PRIORITY = 10000000;
	public static final short MIN_PRIORITY = 1;
	public static final int MAX_BURRIEDNESS_FOR_SPECIAL_TASKS = 30;
	public static final int VICINITY_DISTANCE_THRESHOLD=75000;
}

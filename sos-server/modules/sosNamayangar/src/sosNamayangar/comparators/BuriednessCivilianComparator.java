package sosNamayangar.comparators;

import java.util.Comparator;

import rescuecore2.standard.entities.Civilian;
/**
 * 
 * @author sinash
 *
 */
public class BuriednessCivilianComparator implements Comparator<Civilian> {

	@Override
	public int compare(Civilian cv1, Civilian cv2) {
		if (cv1.getBuriedness() < cv2.getBuriedness())
			return 1;
		if (cv1.getBuriedness() > cv2.getBuriedness())
			return -1;
		return 0;
	}
}
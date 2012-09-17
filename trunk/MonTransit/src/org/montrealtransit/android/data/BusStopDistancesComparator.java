package org.montrealtransit.android.data;

import java.util.Comparator;

/**
 * Sort the {@link ABusStop} by distance (closest first).
 * @author Mathieu Méa
 */
public class BusStopDistancesComparator implements Comparator<ABusStop> {

	@Override
	public int compare(ABusStop stop1, ABusStop stop2) {
		float d1 = stop1.getDistance();
		float d2 = stop2.getDistance();
		if (d1 > d2) {
			return +1;
		} else if (d1 < d2) {
			return -1;
		} else {
			return stop1.getLineNumber().compareTo(stop2.getLineNumber());
		}
	}

}

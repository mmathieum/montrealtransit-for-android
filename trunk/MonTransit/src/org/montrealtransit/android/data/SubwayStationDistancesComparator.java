package org.montrealtransit.android.data;

import java.util.Comparator;

/**
 * Sort the {@link ASubwayStation} by distance (closest first).
 * @author Mathieu Méa
 */
public class SubwayStationDistancesComparator implements Comparator<ASubwayStation> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compare(ASubwayStation station1, ASubwayStation station2) {

		float d1 = station1.getDistance();
		float d2 = station2.getDistance();

		if (d1 > d2) {
			return +1;
		} else if (d1 < d2) {
			return -1;
		} else {
			return 0;
		}
	}

}

package org.montrealtransit.android.data;

import java.util.Comparator;

public interface POI {

	public static final int ITEM_VIEW_TYPE_BUS = 0;
	public static final int ITEM_VIEW_TYPE_SUBWAY = 1;
	public static final int ITEM_VIEW_TYPE_BIKE = 2;

	/**
	 * @param distanceString the new distance string
	 */
	public void setDistanceString(String distanceString);

	public Double getLat();

	public Double getLng();

	public boolean hasLocation();

	/**
	 * @return the distance string
	 */
	public String getDistanceString();

	/**
	 * @param distance the new distance
	 */
	public void setDistance(float distance);

	/**
	 * @return the distance
	 */
	public float getDistance();

	/**
	 * @return unique identifier
	 */
	public String getUID();

	/**
	 * @return type
	 */
	public int getType();

	public class POIDistanceComparator implements Comparator<POI> {
		@Override
		public int compare(POI lhs, POI rhs) {
			if (lhs instanceof RouteTripStop && rhs instanceof RouteTripStop) {
				RouteTripStop alhs = (RouteTripStop) lhs;
				RouteTripStop arhs = (RouteTripStop) rhs;
				// IF same stop DO
				if (alhs.stop.id == arhs.stop.id) {
					// compare route shortName as integer
					try {
						return Integer.valueOf(alhs.route.shortName) - Integer.valueOf(arhs.route.shortName);
					} catch (NumberFormatException nfe) {
						// compare route short name as string
						return alhs.route.shortName.compareTo(arhs.route.shortName);
					}
				}
			} else if (lhs instanceof TripStop && rhs instanceof TripStop) {
				TripStop alhs = (TripStop) lhs;
				TripStop arhs = (TripStop) rhs;
				// IF same stop DO
				if (alhs.stop.id == arhs.stop.id) {
					// compare route ID (as good as any random sort)
					return alhs.trip.routeId - arhs.trip.routeId;
				}
			}
			float d1 = lhs.getDistance();
			float d2 = rhs.getDistance();
			if (d1 > d2) {
				return +1;
			} else if (d1 < d2) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
package org.montrealtransit.android;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.data.POI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * Location useful methods.
 * @author Mathieu Méa
 */
public class LocationUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = LocationUtils.class.getSimpleName();

	/**
	 * The minimum time between 2 locations updates (in milliseconds)
	 */
	public static final long MIN_TIME = 2000; // 2 second
	/**
	 * The minimum distance between 2 updates
	 */
	public static final float MIN_DISTANCE = 5; // 5 meters
	/**
	 * How long do we prefer accuracy over time? (in milliseconds)
	 */
	public static final long PREFER_ACCURACY_OVER_TIME = 30 * 1000; // 30 seconds
	/**
	 * Represent a significant improvement in the accuracy (in meters).
	 */
	public static final int SIGNIFICANT_ACCURACY_IN_METERS = 200;
	/**
	 * How long do we even consider the location? (in milliseconds)
	 */
	private static final long MAX_LAST_KNOW_LOCATION_TIME = 30 * 60 * 1000; // 30 minutes
	/**
	 * The minimum range of the location around.
	 */
	public static final double MIN_AROUND_DIFF = 0.02;
	/**
	 * The increment of the range of the location around.
	 */
	public static final double INC_AROUND_DIFF = 0.01;
	/**
	 * The maximum range of the location around.
	 */
	public static final double MAX_AROUND_DIFF = 0.10;
	/**
	 * The string formatter to truncate around location.
	 */
	private static final String AROUND_TRUNC = "%.4g";

	/**
	 * Utility class.
	 */
	private LocationUtils() {
	};

	/**
	 * @param activity the activity
	 * @return the providers matching the application requirement
	 */
	private static List<String> getProviders(Activity activity) {
		Criteria criteria = new Criteria();
		// criteria.setAccuracy(Criteria.ACCURACY_FINE); any accuracy
		criteria.setAltitudeRequired(false); // no altitude
		criteria.setBearingRequired(true); // now using compass
		criteria.setSpeedRequired(false); // no speed required
		boolean enabledOnly = true; // only enabled location providers
		List<String> providers = getLocationManager(activity).getProviders(criteria, enabledOnly);
		// MyLog.d(TAG, "nb location providers: %s", providers.size());
		return providers;
	}

	/**
	 * @param activity the activity
	 * @return the location manager service
	 */
	public static LocationManager getLocationManager(Activity activity) {
		return (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * @param activity the activity
	 * @return the best not too old last know location or <b>NULL</b>
	 */
	public static Location getBestLastKnownLocation(Activity activity) {
		MyLog.v(TAG, "getBestLastKnownLocation()");
		Location result = null;
		for (String provider : getProviders(activity)) {
			Location providerLocation = getLocationManager(activity).getLastKnownLocation(provider);
			// IF the last location is NULL (= location provider disabled) DO
			if (providerLocation == null) {
				// check next location provider
				continue;
			}
			// IF no last location candidate DO
			if (result == null) {
				// IF this location candidate is not way too old DO
				if (!isTooOld(providerLocation)) {
					result = providerLocation;
				}
			} else {
				// IF the new location candidate is more relevant DO
				if (isMoreRelevant(result, providerLocation)) {
					result = providerLocation;
				}
			}
		}
		if (result != null) {
			if (MyLog.isLoggable(android.util.Log.DEBUG)) {
				MyLog.d(TAG, "last know location: %s ", locationToString(result));
			}
		} else {
			MyLog.d(TAG, "no valid last location found!");
		}
		return result;
	}

	/**
	 * @param location the location
	 * @return a nice readable location string
	 */
	public static String locationToString(Location location) {
		return String.format("%s > %s,%s (%s) %s seconds ago", location.getProvider(), location.getLatitude(), location.getLongitude(), location.getAccuracy(),
				Utils.toTimestampInSeconds((System.currentTimeMillis() - location.getTime())));
	}

	/**
	 * @param location the location
	 * @return true if the location is way too "old" to be considered
	 */
	public static boolean isTooOld(Location location) {
		// MyLog.v(TAG, "isTooOld()");
		return isTooOld(location, MAX_LAST_KNOW_LOCATION_TIME);
	}

	public static boolean isTooOld(Location location, final long maxLastKnowLocationTime) {
		return location.getTime() + maxLastKnowLocationTime < System.currentTimeMillis();
	}

	/**
	 * Enable updates for an activity and a listener
	 * @param activity the activity
	 * @param listener the listener
	 */
	private static void enableLocationUpdates(final Activity activity, final LocationListener listener) {
		// MyLog.v(TAG, "enableLocationUpdates()");
		// enable location updates
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (String provider : getProviders(activity)) {
					getLocationManager(activity).requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, listener);
				}
			}
		});
	}

	public static boolean enableLocationUpdatesIfNecessary(Activity activity, LocationListener listener, boolean updatesEnabled, boolean paused) {
		// MyLog.v(TAG, "enableLocationUpdatesIfNecessary(%s,%s)", updatesEnabled, paused);
		if (!updatesEnabled && !paused) {
			enableLocationUpdates(activity, listener);
			MyLog.d(TAG, "Location updates ENABLED.");
			updatesEnabled = true;
		}
		return updatesEnabled;
	}

	/**
	 * Disable updates for an activity and a listener
	 * @param activity the activity
	 * @param listener the listener
	 */
	private static void disableLocationUpdates(Activity activity, LocationListener listener) {
		// MyLog.v(TAG, "disableLocationUpdates()");
		getLocationManager(activity).removeUpdates(listener);
	}

	public static boolean disableLocationUpdatesIfNecessary(Activity activity, LocationListener listener, boolean updatesEnabled) {
		MyLog.v(TAG, "disableLocationUpdatesIfNecessary(%s)", updatesEnabled);
		if (updatesEnabled) {
			disableLocationUpdates(activity, listener);
			MyLog.d(TAG, "Location updates DISABLED.");
			updatesEnabled = false;
		}
		return updatesEnabled;
	}

	/**
	 * @param lat the latitude
	 * @param lng the longitude
	 * @return the new location object
	 */
	@Deprecated
	public static Location getNewLocation(double lat, double lng) {
		Location newLocation = new Location("MonTransit");
		newLocation.setLatitude(lat);
		newLocation.setLongitude(lng);
		return newLocation;
	}

	public static float bearTo(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
		float[] results = new float[2];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
		float bearTo = results[1];
		return bearTo;
	}

	public static float distanceTo(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
		float[] results = new float[2];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
		float bearTo = results[0];
		return bearTo;
	}

	/**
	 * @param currentLocation the current location
	 * @param newLocation the new location
	 * @return true if the new location is more 'relevant'
	 * @author based on http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 */
	public static boolean isMoreRelevant(Location currentLocation, Location newLocation) {
		MyLog.v(TAG, "isMoreRelevant()");
		// MyLog.d(TAG, "current location: %s.", locationToString(currentLocation));
		// MyLog.d(TAG, "new location: %s.", locationToString(newLocation));
		return isMoreRelevant(currentLocation, newLocation, SIGNIFICANT_ACCURACY_IN_METERS, PREFER_ACCURACY_OVER_TIME);
	}

	public static boolean isMoreRelevant(Location currentLocation, Location newLocation, final int significantAccuracyInMeters,
			final long preferAccuracyOverTime) {
		MyLog.v(TAG, "isMoreRelevant(%s,%s)", significantAccuracyInMeters, preferAccuracyOverTime);
		// MyLog.d(TAG, "current location: %s.", locationToString(currentLocation));
		// MyLog.d(TAG, "new location: %s.", locationToString(newLocation));
		if (currentLocation == null) {
			// A new location is always better than no location
			MyLog.d(TAG, "New location is better than 'null'.");
			return true;
		}

		if (areTheSame(currentLocation, newLocation)) {
			// same location so new one is not more relevant
			// MyLog.d(TAG, "New location dropped... because it's the same location. (tag:%s)", tag);
			return false;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > preferAccuracyOverTime;
		boolean isSignificantlyOlder = timeDelta < -preferAccuracyOverTime;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			MyLog.d(TAG, "New location is significantly newer (user has moved).");
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			MyLog.d(TAG, "New location is significantly older (must be worse than current location).");
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > significantAccuracyInMeters;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation, currentLocation);

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			MyLog.d(TAG, "New location is more accurate.");
			return true;
		} else if (isNewer && !isLessAccurate) {
			MyLog.d(TAG, "New location is more recent and not less accurate.");
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			MyLog.d(TAG, "New location is more recent and not significantly less accurate and from the same provider.");
			return true;
		}
		MyLog.d(TAG, "New location dropped... keeping current location.");
		return false;
	}

	/**
	 * Checks whether two providers are the same.
	 * @param loc1 location 1
	 * @param loc2 location 2
	 * @return true if both location have the same provider
	 * @author http://developer.android.com/guide/topics/location/obtaining-user-location.html
	 */
	private static boolean isSameProvider(Location loc1, Location loc2) {
		if (loc1.getProvider() == null) {
			return loc2.getProvider() == null;
		}
		return loc1.getProvider().equals(loc2.getProvider());
	}

	/**
	 * @param context the context
	 * @param location the location
	 * @return the location address
	 */
	public static Address getLocationAddress(Context context, Location location) {
		MyLog.v(TAG, "getLocationAddress()");
		Address result = null;
		Geocoder geocoder = new Geocoder(context);
		try {
			int maxResults = 1;
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), maxResults);
			if (addresses != null && addresses.size() >= 1) {
				result = addresses.get(0);
				// MyLog.d(TAG, "Found address: %s", result.getAddressLine(0));
			}
		} catch (IOException ioe) {
			if (MyLog.isLoggable(android.util.Log.DEBUG)) {
				MyLog.w(TAG, ioe, "Can't find the adress of the current location!");
			} else {
				MyLog.w(TAG, "Can't find the adress of the current location!");
			}
		}
		return result;
	}

	/**
	 * @param context the context
	 * @param locationAddress the location address
	 * @param accuracy the accuracy
	 * @return the location string
	 */
	public static String getLocationString(Context context, int stringId, Address locationAddress, Float accuracy) {
		MyLog.v(TAG, "getLocationString()");
		StringBuilder sb = new StringBuilder();
		if (context == null) {
			return sb.toString();
		}
		sb.append(context.getString(stringId));
		if (locationAddress != null) {
			sb.append(" (");
			if (locationAddress.getAddressLine(0) != null) {
				// first line of the address (1234, street name)
				sb.append(locationAddress.getAddressLine(0));
			} else if (locationAddress.getThoroughfare() != null) {
				// street name only
				sb.append(locationAddress.getThoroughfare());
			} else if (locationAddress.getLocality() != null) {
				// city
				sb.append(", ").append(locationAddress.getLocality());
			}
			if (accuracy != null) {
				sb.append(" ± ").append(getDistanceStringUsingPref(context, accuracy, accuracy));
			}
			sb.append(")");
		}
		// MyLog.d(TAG, "text: " + sb.toString();
		return sb.toString();
	}

	/**
	 * @param loc the location (latitude/longitude)
	 * @return the truncated location
	 */
	public static double truncAround(String loc) {
		// return Double.parseDouble(String.format(Locale.US, AROUND_TRUNC, Double.parseDouble(loc)));
		return Double.parseDouble(truncAround(Double.parseDouble(loc)));
	}

	/**
	 * @param loc the location (latitude/longitude)
	 * @return the truncated location
	 */
	public static String truncAround(double loc) {
		return String.format(Locale.US, AROUND_TRUNC, loc);
	}

	/**
	 * Return the distance string matching the accuracy and the user settings.
	 * @param context the activity
	 * @param distanceInMeters the distance in meter
	 * @param accuracyInMeters the accuracy in meter
	 * @return the distance string.
	 */
	public static String getDistanceStringUsingPref(Context context, float distanceInMeters, float accuracyInMeters) {
		// MyLog.v(TAG, "getDistanceStringUsingPref(" + distanceInMeters + ", " + accuracyInMeters + ")");
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		return getDistanceString(distanceInMeters, accuracyInMeters, isDetailed, distanceUnit);
	}

	/**
	 * Return the distance string matching the accuracy and the user settings.
	 * @param context the activity
	 * @param distanceInMeters the distance in meter
	 * @param accuracyInMeters the accuracy in meter
	 * @param isDetailed true if detailed {@link UserPreferences#PREFS_DISTANCE}
	 * @param distanceUnit {@link UserPreferences#PREFS_DISTANCE_UNIT}
	 * @return the distance string.
	 */
	private static String getDistanceString(float distanceInMeters, float accuracyInMeters, boolean isDetailed, String distanceUnit) {
		// IF distance unit is Imperial DO
		if (distanceUnit.equals(UserPreferences.PREFS_DISTANCE_UNIT_IMPERIAL)) {
			float distanceInSmall = distanceInMeters * Constant.FEET_PER_M;
			float accuracyInSmall = accuracyInMeters * Constant.FEET_PER_M;
			return getDistance(distanceInSmall, accuracyInSmall, isDetailed, Constant.FEET_PER_MILE, 10, "ft", "mi");
		} else { // use Metric (default)
			return getDistance(distanceInMeters, accuracyInMeters, isDetailed, Constant.METER_PER_KM, 1, "m", "km");
		}
	}

	/**
	 * @param distance the distance
	 * @param accuracy the accuracy
	 * @param isDetailed true if the distance string must be detailed
	 * @param smallPerBig the number of small unit to make the big unit
	 * @param threshold the threshold between small and big
	 * @param smallUnit the small unit
	 * @param bigUnit the big unit
	 * @return the distance string
	 */
	private static String getDistance(final float distance, final float accuracy, final boolean isDetailed, final float smallPerBig, final int threshold,
			final String smallUnit, final String bigUnit) {
		StringBuilder sb = new StringBuilder();
		// IF the location is enough precise AND the accuracy is 10% or more of the distance DO
		if (isDetailed && accuracy < distance && accuracy / distance > 0.1) {
			final float shorterDistanceInSmallUnit = distance - accuracy / 2;
			final float longerDistanceInSmallUnit = distance + accuracy;
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (distance > (smallPerBig / threshold)) {
				// use "big unit"
				final float shorterDistanceInBigUnit = shorterDistanceInSmallUnit / smallPerBig;
				final float niceShorterDistanceInBigUnit = Integer.valueOf(Math.round(shorterDistanceInBigUnit * 10)).floatValue() / 10;
				final float longerDistanceInBigUnit = longerDistanceInSmallUnit / smallPerBig;
				final float niceLongerDistanceInBigUnit = Integer.valueOf(Math.round(longerDistanceInBigUnit * 10)).floatValue() / 10;
				sb.append(niceShorterDistanceInBigUnit).append(" - ").append(niceLongerDistanceInBigUnit).append(" ").append(bigUnit);
			} else {
				// use "small unit"
				final int niceShorterDistanceInSmallUnit = Math.round(shorterDistanceInSmallUnit);
				final int niceLongerDistanceInSmallUnit = Math.round(longerDistanceInSmallUnit);
				sb.append(niceShorterDistanceInSmallUnit).append(" - ").append(niceLongerDistanceInSmallUnit).append(" ").append(smallUnit);
			}
			// ELSE IF the accuracy of the location is more than the distance DO
		} else if (accuracy > distance) { // basically, the location is in the blue circle in Maps
			// use the accuracy as a distance
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (accuracy > (smallPerBig / threshold)) {
				// use "big unit"
				final float accuracyInBigUnit = accuracy / smallPerBig;
				final float niceAccuracyInBigUnit = Integer.valueOf(Math.round(accuracyInBigUnit * 10)).floatValue() / 10;
				sb.append("< ").append(niceAccuracyInBigUnit).append(" ").append(bigUnit);
			} else {
				// use "small unit"
				final int niceAccuracyInSmallUnit = Math.round(accuracy);
				sb.append("< ").append(getSimplerDistance(niceAccuracyInSmallUnit, accuracy, isDetailed)).append(" ").append(smallUnit);
			}
			// TODO ? ELSE if accuracy non-significant DO show the longer distance ?
		} else {
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (distance > (smallPerBig / threshold)) {
				// use "big unit"
				final float distanceInBigUnit = distance / smallPerBig;
				final float niceDistanceInBigUnit = Integer.valueOf(Math.round(distanceInBigUnit * 10)).floatValue() / 10;
				sb.append(niceDistanceInBigUnit).append(" ").append(bigUnit);
			} else {
				// use "small unit"
				final int niceDistanceInSmallUnit = Math.round(distance);
				sb.append(getSimplerDistance(niceDistanceInSmallUnit, accuracy, isDetailed)).append(" ").append(smallUnit);
			}
		}
		return sb.toString();
	}

	public static final int getSimplerDistance(int distance, float accuracyF, boolean isDetailed) {
		if (isDetailed) {
			return distance;
		}
		final int accuracy = (int) Math.round(accuracyF);
		final int simplerDistance = Math.round(distance / 10) * 10;
		if (Math.abs(simplerDistance - distance) < accuracy) {
			return simplerDistance;
		} else {
			return distance; // accuracy too good, have to keep real data
		}
	}

	public static float getAroundCoveredDistance(double lat, double lng, double AROUND_DIFF) {
		MyLog.v(TAG, "getAroundCoveredDistance(%s, %s)", lat, lng);
		// latitude
		double latTrunc = lat;
		double latBefore = Double.valueOf(truncAround(latTrunc - AROUND_DIFF));
		double latAfter = Double.valueOf(truncAround(latTrunc + AROUND_DIFF));
		// MyLog.d(TAG, "lat: " + latBefore + " - " + latAfter);
		// longitude
		double lngTrunc = lng;
		double lngBefore = Double.valueOf(truncAround(lngTrunc - AROUND_DIFF));
		double lngAfter = Double.valueOf(truncAround(lngTrunc + AROUND_DIFF));
		// MyLog.d(TAG, "lng: " + lngBefore + " - " + lngAfter);
		final float distanceToNorth = distanceTo(lat, lng, latAfter, lng);
		// MyLog.d(TAG, "north: " + distanceToNorth);
		final float distanceToSouth = distanceTo(lat, lng, latBefore, lng);
		// MyLog.d(TAG, "south: " + distanceToSouth);
		final float distanceToWest = distanceTo(lat, lng, lat, lngBefore);
		// MyLog.d(TAG, "west: " + distanceToWest);
		final float distanceToEast = distanceTo(lat, lng, lat, lngAfter);
		// MyLog.d(TAG, "east: " + distanceToEast);
		float[] distances = new float[] { distanceToNorth, distanceToSouth, distanceToWest, distanceToEast };
		Arrays.sort(distances);
		return distances[0]; // return the closest
	}

	/**
	 * @param lat latitude
	 * @param lng longitude
	 * @param latTableColumn latitude SQL table column
	 * @param lngTableColumn longitude SQL table column
	 * @return the SQL where clause
	 */
	public static String genAroundWhere(String lat, String lng, String latTableColumn, String lngTableColumn, double aroundDiff) {
		// MyLog.v(TAG, "genAroundWhere(%s, %s, %s, %s)", lat, lng, latTableColumn, lngTableColumn);
		StringBuilder qb = new StringBuilder();
		// latitude
		double latTrunc = truncAround(lat);
		String latBefore = truncAround(latTrunc - aroundDiff);
		String latAfter = truncAround(latTrunc + aroundDiff);
		// MyLog.d(TAG, "lat: " + latBefore + " - " + latAfter);
		qb.append(latTableColumn).append(" BETWEEN ").append(latBefore).append(" AND ").append(latAfter);
		qb.append(" AND ");
		// longitude
		double lngTrunc = truncAround(lng);
		String lngBefore = truncAround(lngTrunc - aroundDiff);
		String lngAfter = truncAround(lngTrunc + aroundDiff);
		// MyLog.d(TAG, "lng: " + lngBefore + " - " + lngAfter);
		qb.append(lngTableColumn).append(" BETWEEN ").append(lngBefore).append(" AND ").append(lngAfter);
		return qb.toString();
	}

	public static String genAroundWhere(double lat, double lng, String latTableColumn, String lngTableColumn, double aroundDiff) {
		return genAroundWhere(String.valueOf(lat), String.valueOf(lng), latTableColumn, lngTableColumn, aroundDiff);
	}

	public static String genAroundWhere(Location location, String latTableColumn, String lngTableColumn, double aroundDiff) {
		return genAroundWhere(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), latTableColumn, lngTableColumn, aroundDiff);
	}

	public static void updateDistance(Map<?, ? extends POI> pois, double lat, double lng) {
		if (pois == null) {
			return;
		}
		for (POI poi : pois.values()) {
			if (!poi.hasLocation()) {
				continue;
			}
			poi.setDistance(distanceTo(lat, lng, poi.getLat(), poi.getLng()));
		}
	}

	public static void updateDistanceWithString(Context context, Map<?, ? extends POI> pois, Location currentLocation) {
		if (pois == null || currentLocation == null) {
			return;
		}
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		float accuracyInMeters = currentLocation.getAccuracy();
		// update bus stops
		for (POI poi : pois.values()) {
			if (!poi.hasLocation()) {
				continue;
			}
			final float newDistance = distanceTo(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng());
			if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) {
				// MyLog.d(TAG, "skip distance");
				continue;
			}
			// update value
			poi.setDistance(newDistance);
			poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
		}
	}

	public static void updateDistanceWithString(final Context context, final List<? extends POI> pois, final Location currentLocation,
			final LocationTaskCompleted callback) {
		// MyLog.v(TAG, "updateDistance()");
		if (pois == null || currentLocation == null) {
			callback.onLocationTaskCompleted();
			return;
		}
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				updateDistanceWithString(context, pois, currentLocation);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				callback.onLocationTaskCompleted();
			}
		}.execute();
	}

	public static void updateDistanceWithString(Context context, List<? extends POI> pois, Location currentLocation) {
		if (pois == null || currentLocation == null) {
			return;
		}
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		float accuracyInMeters = currentLocation.getAccuracy();
		// update bus stops
		for (POI poi : pois) {
			if (!poi.hasLocation()) {
				continue;
			}
			final float newDistance = distanceTo(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng());
			if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) {
				// MyLog.d(TAG, "skip distance");
				continue;
			}
			// update value
			poi.setDistance(newDistance);
			poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
		}
	}

	public static void updateDistance(List<? extends POI> pois, double lat, double lng) {
		if (pois == null) {
			return;
		}
		for (POI poi : pois) {
			if (!poi.hasLocation()) {
				continue;
			}
			poi.setDistance(distanceTo(lat, lng, poi.getLat(), poi.getLng()));
		}
	}

	public static void updateDistanceWithString(final Context context, final POI poi, final Location currentLocation, final LocationTaskCompleted callback) {
		if (poi == null || currentLocation == null) {
			callback.onLocationTaskCompleted();
			return;
		}
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				updateDistanceWithString(context, poi, currentLocation);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				callback.onLocationTaskCompleted();
			}
		}.execute();
	}

	public static void updateDistanceWithString(Context context, POI poi, Location currentLocation) {
		if (poi == null || currentLocation == null) {
			return;
		}
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		float accuracyInMeters = currentLocation.getAccuracy();
		// update bus stops
		if (!poi.hasLocation()) {
			return;
		}
		final float newDistance = distanceTo(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng());
		if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) {
			// MyLog.d(TAG, "skip distance");
			return;
		}
		// update value
		poi.setDistance(newDistance);
		poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
	}

	public interface LocationTaskCompleted {
		void onLocationTaskCompleted();
	}

	public static boolean areTheSame(Location loc1, Location loc2) {
		if (loc1 == null) {
			return loc2 == null;
		}
		if (loc2 == null) {
			return false;
		}
		return areTheSame(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
	}

	/**
	 * @return true if both lat/lng are available (not null) and the same
	 */
	public static boolean areTheSame(Location loc1, double lat2, double lng2) {
		if (loc1 == null) {
			return false;
		}
		return areTheSame(loc1.getLatitude(), loc1.getLongitude(), lat2, lng2);
	}

	/**
	 * @return true if both lat/lng are the same
	 */
	public static boolean areTheSame(double lat1, double lng1, double lat2, double lng2) {
		return lat1 == lat2 && lng1 == lng2;
	}

	public static void showPOILocationInMap(Activity activity, POI poi) {
		MyLog.d(TAG, "showPOILocationInMap()");
		if (!poi.hasLocation()) {
			Utils.notifyTheUser(activity, activity.getString(R.string.poi_location_not_found));
			return;
		}
		Uri uri = Uri.parse(String.format("geo:%s,%s", poi.getLat(), poi.getLng()));
		final Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
		if (activity.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
			activity.startActivity(intent); // launch the map activity
		} else {
			Utils.notifyTheUser(activity, activity.getString(R.string.map_app_not_installed));
		}
	}

	public static void showPOILocationInRadar(Activity activity, POI poi) {
		MyLog.d(TAG, "showPOILocationInRadar()");
		if (!poi.hasLocation()) {
			Utils.notifyTheUser(activity, activity.getString(R.string.poi_location_not_found));
			return;
		}
		Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
		intent.putExtra("latitude", (double) poi.getLat());
		intent.putExtra("longitude", (double) poi.getLng());
		if (activity.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
			activity.startActivity(intent); // launch the radar activity
		} else {
			Utils.notifyTheUser(activity, activity.getString(R.string.no_radar_title));
		}
	}

	public static void removeTooFar(List<? extends POI> pois, float maxDistance) {
		MyLog.d(TAG, "removeTooFar(%s)", maxDistance);
		if (pois != null) {
			ListIterator<? extends POI> it = pois.listIterator();
			while (it.hasNext()) {
				POI poi = it.next();
				if (poi.getDistance() > maxDistance) {
					// MyLog.d(TAG, "removeTooFar() filtering on distance... (skiping %s)", poi.getUID());
					it.remove();
					continue;
					// } else {
					// MyLog.d(TAG, "removeTooFar() filtering on distance... (not skiping %s)", poi.getUID());
				}
			}
		}
	}
}

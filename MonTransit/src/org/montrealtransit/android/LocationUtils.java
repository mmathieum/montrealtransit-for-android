package org.montrealtransit.android;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

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
	private static final long MAX_LAST_KNOW_LOCATION_TIME = 2 * 60 * 1000; // 2 minutes
	/**
	 * The range of the location around.
	 */
	public static final double AROUND_DIFF = 0.02;
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
		criteria.setBearingRequired(false); // no compass... for now ;)
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
			if (MyLog.isLoggable(Log.DEBUG)) {
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
		return location.getTime() + MAX_LAST_KNOW_LOCATION_TIME < System.currentTimeMillis();
	}

	/**
	 * Enable updates for an activity and a listener
	 * @param activity the activity
	 * @param listener the listener
	 */
	public static void enableLocationUpdates(Activity activity, LocationListener listener) {
		MyLog.v(TAG, "enableLocationUpdates()");
		// enable location updates
		for (String provider : getProviders(activity)) {
			getLocationManager(activity).requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, listener);
		}
	}

	/**
	 * Disable updates for an activity and a listener
	 * @param activity the activity
	 * @param listener the listener
	 */
	public static void disableLocationUpdates(Activity activity, LocationListener listener) {
		MyLog.v(TAG, "disableLocationUpdates()");
		getLocationManager(activity).removeUpdates(listener);
	}

	/**
	 * @param lat the latitude
	 * @param lng the longitude
	 * @return the new location object
	 */
	public static Location getNewLocation(double lat, double lng) {
		Location newLocation = new Location("MonTransit");
		newLocation.setLatitude(lat);
		newLocation.setLongitude(lng);
		return newLocation;
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

		if (currentLocation == null) {
			// A new location is always better than no location
			MyLog.d(TAG, "New location is better than 'null'.");
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > PREFER_ACCURACY_OVER_TIME;
		boolean isSignificantlyOlder = timeDelta < -PREFER_ACCURACY_OVER_TIME;
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
		boolean isSignificantlyLessAccurate = accuracyDelta > SIGNIFICANT_ACCURACY_IN_METERS;

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
			if (MyLog.isLoggable(Log.DEBUG)) {
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
				sb.append(" ± ").append(Utils.getDistanceStringUsingPref(context, accuracy, 0));
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
	 * @param lat latitude
	 * @param lng longitude
	 * @param latTableColumn latitude SQL table column
	 * @param lngTableColumn longitude SQL table column
	 * @return the SQL where clause
	 */
	public static String genAroundWhere(String lat, String lng, String latTableColumn, String lngTableColumn) {
		StringBuilder qb = new StringBuilder();
		// latitude
		double latTrunc = truncAround(lat);
		String latBefore = truncAround(latTrunc - AROUND_DIFF);
		String latAfter = truncAround(latTrunc + AROUND_DIFF);
		qb.append(latTableColumn).append(" BETWEEN ").append(latBefore).append(" AND ").append(latAfter);
		qb.append(" AND ");
		// longitude
		double lngTrunc = truncAround(lng);
		String lngBefore = truncAround(lngTrunc - AROUND_DIFF);
		String lngAfter = truncAround(lngTrunc + AROUND_DIFF);
		qb.append(lngTableColumn).append(" BETWEEN ").append(lngBefore).append(" AND ").append(lngAfter);
		return qb.toString();
	}
}

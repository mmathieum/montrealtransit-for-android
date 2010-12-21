package org.montrealtransit.android;

import java.io.IOException;
import java.util.List;

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
	public static final long MIN_TIME = 1000; // 1 second
	/**
	 * The minimum distance between 2 updates
	 */
	public static final float MIN_DISTANCE = 2; // 2 meters
	/**
	 * How long do we prefer accuracy over time?
	 */
	public static final long PREFER_MORE_PRECISE_LAST_KNOWN_LOC_TIME = 5000; // 5 seconds
	/**
	 * The validity of a last know location (in milliseconds)
	 */
	private static final long MAX_LAST_KNOW_LOCATION_TIME = 120000; // 2 minutes

	/**
	 * Utility class.
	 */
	private LocationUtils() {
	};

	/**
	 * @param activity the activity
	 * @return the providers matching the application requirement
	 */
	public static List<String> getProviders(Activity activity) {
		Criteria criteria = new Criteria();
		// criteria.setAccuracy(Criteria.ACCURACY_FINE); any accuracy
		criteria.setAltitudeRequired(false); // no altitude
		criteria.setBearingRequired(false); // no compass... for now ;)
		criteria.setSpeedRequired(false); // no speed required
		boolean enabledOnly = true; // only enabled location providers
		List<String> providers = getLocationManager(activity).getProviders(criteria, enabledOnly);
		MyLog.v(TAG, "nb location providers: " + providers.size());
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
		Location result = null;
		for (String provider : getProviders(activity)) {
			Location lastLocation = getLocationManager(activity).getLastKnownLocation(provider);
			// IF the last location is NOT NULL (= location provider disabled) DO
			if (lastLocation != null) {
				// IF no last location candidate DO
				if (result == null) {
					// IF this location candidate is not too old DO
					if (!isTooOld(lastLocation)) {
						result = lastLocation;
					}
				} else {
					// IF the new location candidate is more recent DO
					if (lastLocation.getTime() > result.getTime() && isMorePrecise(lastLocation, result)) {
						result = lastLocation;
					}
					// TODO compare accuracy?
				}
			}
		}
		if (result != null) {
			if (MyLog.isLoggable(Log.VERBOSE)) {
				MyLog.v(TAG, "last know location:" + result.getProvider() + " > " + result.getLatitude() + ", "
				        + result.getLongitude() + "(" + result.getAccuracy() + ") "
				        + ((System.currentTimeMillis() - result.getTime()) / 1000) + " seconds ago.");
			}
		} else {
			MyLog.v(TAG, "no valid last location found!");
		}
		return result;
	}

	/**
	 * @param location the location
	 * @return true if the location is too "old"
	 */
	public static boolean isTooOld(Location location) {
		return location.getTime() + MAX_LAST_KNOW_LOCATION_TIME > System.currentTimeMillis();
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
	 */
	public static boolean isMorePrecise(Location currentLocation, Location newLocation) {
		if (newLocation.getAccuracy() < currentLocation.getAccuracy()) {
			// the new location is more precise
			return true;
		} else if (newLocation.getTime() - currentLocation.getTime() > PREFER_MORE_PRECISE_LAST_KNOWN_LOC_TIME) {
			// the new location is less precise but more recent
			return true;
		} else {
			return false;
		}
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
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),
			        maxResults);
			if (addresses != null && addresses.size() >= 1) {
				result = addresses.get(0);
				// MyLog.d(TAG, "Found address: %s", result.getAddressLine(0));
			}
		} catch (IOException e) {
			MyLog.w(TAG, "Can't find the adress of the current location!", e);
		}
		return result;
	}

	/**
	 * @param context the context
	 * @param locationAddress the location address
	 * @param accuracy the accuracy
	 * @return the location string
	 */
	public static String getLocationString(Context context, Address locationAddress, Float accuracy) {
		MyLog.v(TAG, "getLocationString()");
		String text = context.getString(R.string.closest_subway_stations);
		if (locationAddress != null) {
			text += " (";
			if (locationAddress.getAddressLine(0) != null) {
				// first line of the address (1234, street name)
				text += locationAddress.getAddressLine(0);
			} else if (locationAddress.getThoroughfare() != null) {
				// street name only
				text += locationAddress.getThoroughfare();
			} else if (locationAddress.getLocality() != null) {
				// city
				text += ", " + locationAddress.getLocality();
			}
			if (accuracy != null) {
				text += " ± " + Utils.getDistanceString(context, accuracy, 0);
			}
			text += ")";
		}
		// MyLog.d(TAG, "text: " + text);
		return text;
	}
}

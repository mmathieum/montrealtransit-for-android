package org.montrealtransit.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteStop;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmSubwayManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the utility class with a lot of useful methods to be used all across the application.
 * @author Mathieu Méa
 */
public class Utils {

	/**
	 * The log tag.
	 */
	private static final String TAG = Utils.class.getSimpleName();

	/**
	 * The date formatter.
	 */
	private static DateFormat dateFormatter;
	/**
	 * The current language/country.
	 */
	private static Locale currentLocale;
	/**
	 * The date formatter use to parse HH:mm into Date.
	 */
	private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
	/**
	 * The minimum between 2 {@link BaseAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	public static final int ADAPTER_NOTIFY_THRESOLD = 250; // 0.25 seconds
	/**
	 * The minimum number of item in a manual POI list.
	 */
	public static final int MIN_NEARBY_LIST = 10; // 0;
	/**
	 * The number of item in a manual POI list.
	 */
	public static final int NB_NEARBY_LIST = 20; // 10;
	/**
	 * The time after which a POIs list should be refresh if the user is not currently interacting with it (in milliseconds).
	 */
	public static final int CLOSEST_POI_LIST_TIMEOUT = 60 * 60 * 1000; // 1 hour
	/**
	 * How long do we prefer accuracy over time? (in milliseconds)
	 */
	public static final long CLOSEST_POI_LIST_PREFER_ACCURACY_OVER_TIME = 2 * 60 * 1000; // 2 minutes
	/**
	 * Vertical offset for top toast.
	 */
	public static final int TOAST_Y_OFFSET = 200;

	/**
	 * Read the input stream and write the stream to the output stream file.
	 * @param is the input stream
	 * @param os the output steam
	 * @param encoding encoding
	 */
	public static void getInputStreamToFile(InputStream is, FileOutputStream os, String encoding) {
		MyLog.v(TAG, "getInputStreamToFile(%s)", encoding);
		OutputStreamWriter writer = new OutputStreamWriter(os);
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				writer.write(new String(b, 0, n, encoding));
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, ioe, "Error while reading the input stream and writing into the file.");
		} finally {
			try {
				writer.flush();
				writer.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, ioe, "Error while finishing and closing the file.");
			}
		}
	}

	/**
	 * Read the input stream and return the content as a string
	 * @param is the input stream
	 * @param encoding the encoding
	 * @return the string
	 */
	public static String getInputStreamToString(InputStream is, String encoding) {
		MyLog.v(TAG, "getInputStreamToString(%s)", encoding);
		String result = "";
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				result += new String(b, 0, n, encoding);
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, ioe, "Error while reading the input stream and writing into a string.");
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, ioe, "Error while finishing and closing the file.");
			}
		}
		return result;
	}

	/**
	 * Read the input stream and return the content as a string
	 * @param is the input stream
	 * @return the string
	 */
	public static String getInputStreamToString(InputStream is) {
		MyLog.v(TAG, "getInputStreamToString()");
		String result = "";
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				result += new String(b, 0, n);
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, ioe, "Error while reading the input stream and writing into a string.");
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, ioe, "Error while finishing and closing the file.");
			}
		}
		return result;
	}

	/**
	 * Format the times according to the device settings.
	 * @param context the context used to get the device settings
	 * @param noFormatTime the original times string
	 * @return the formatted time string
	 */
	public static String formatTimes(Context context, String noFormatTime) {
		// MyLog.v(TAG, "formatTimes(%s)", noFormatTime);
		String result = "";
		try {
			result = getTimeFormatter(context).format(simpleDateFormatter.parse(noFormatTime.replace("h", ":")));
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while formatting '%s'.", noFormatTime);
			result = noFormatTime;
		}
		return result;
	}

	/**
	 * Format a date / time such that if the date is on same day as now, it shows just the time and if it's a different day, it shows just the date.
	 * @param date the date
	 * @return the formatted date
	 */
	public static CharSequence formatSameDayDate(Date date) {
		return formatSameDayDateInMs(date.getTime());
	}

	/**
	 * Format a date / time such that if the date is on same day as now, it shows just the time and if it's a different day, it shows just the date.
	 * @param dateInSec the date in seconds
	 * @return the formatted date
	 */
	public static CharSequence formatSameDayDateInSec(int dateInSec) {
		return formatSameDayDateInMs(((long) dateInSec) * 1000);
	}

	/**
	 * Format a date / time such that if the date is on same day as now, it shows just the time and if it's a different day, it shows just the date.
	 * @param dateInMs the date in milliseconds
	 * @return the formatted date
	 */
	public static CharSequence formatSameDayDateInMs(long dateInMs) {
		return DateUtils.formatSameDayTime(dateInMs, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT);
	}

	/**
	 * Use a static field to store the DateFormatter for improve the performance.
	 * @param context the context used to get the device settings
	 * @return the date formatter matching the device settings
	 */
	public static DateFormat getTimeFormatter(Context context) {
		// IF no current local OR no current data formatter OR the country/language has changed DO
		if (currentLocale == null || dateFormatter == null || currentLocale != context.getResources().getConfiguration().locale) {
			// get the current language/country
			currentLocale = context.getResources().getConfiguration().locale;
			// get the current date formatter
			dateFormatter = android.text.format.DateFormat.getTimeFormat(context);
		}
		return dateFormatter;
	}

	/**
	 * @param collection the collection
	 * @return the size of the collection (0 or more => no NPE!)
	 */
	public static int getCollectionSize(Collection<?> collection) {
		if (collection == null) {
			return 0;
		}
		return collection.size();
	}

	public static int getCollectionSize(SparseArray<?> sparseArray) {
		if (sparseArray == null) {
			return 0;
		}
		return sparseArray.size();
	}

	/**
	 * @param map the map
	 * @return the size of the map (0 or more => no NPE!)
	 */
	public static int getMapSize(Map<?, ?> map) {
		if (map == null) {
			return 0;
		}
		return map.size();
	}

	/**
	 * Return a string representing the content of a list of string.
	 * @param list the list
	 * @return the string
	 */
	public static String toStringListOfString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int i = 0;
		for (String string : list) {
			if (sb.length() > 2) {
				sb.append("|");
			}
			sb.append(i).append(":").append(string);
			i++;
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Return a string representing the content of the map of string,string.
	 * @param map the map
	 * @return the string
	 */
	public static String toStringMapOfStringString(Map<String, String> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String mapKey : map.keySet()) {
			if (sb.length() > 2) {
				sb.append("|");
			}
			sb.append(":").append(mapKey).append(">").append(map.get(mapKey));
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Return a saved string value from the intent that call the activity.
	 * @param intent the intent
	 * @param savedInstanceState the saved instance state
	 * @param id the string ID
	 * @return the saved string value
	 */
	public static String getSavedStringValue(Intent intent, Bundle savedInstanceState, String id) {
		MyLog.v(TAG, "getSavedStringValue(%s)", id);
		String result = savedInstanceState != null && savedInstanceState.containsKey(id) ? savedInstanceState.getString(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the string
			result = extras != null && extras.containsKey(id) ? extras.getString(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved string value for ID '%s' (returned null)", id);
		}
		return result;
	}

	public static Integer getSavedIntValue(Intent intent, Bundle savedInstanceState, String id) {
		MyLog.v(TAG, "getSavedIntValue(%s)", id);
		Integer result = savedInstanceState != null && savedInstanceState.containsKey(id) ? savedInstanceState.getInt(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the int
			result = extras != null && extras.containsKey(id) ? extras.getInt(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved int value for ID '%s' (returned null)", id);
		}
		return result;
	}

	public static float getSavedFloatValue(Intent intent, Bundle savedInstanceState, String id) {
		MyLog.v(TAG, "getSavedFloatValue(%s)", id);
		Float result = savedInstanceState != null && savedInstanceState.containsKey(id) ? savedInstanceState.getFloat(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the line id
			result = extras != null && extras.containsKey(id) ? extras.getFloat(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved float value for ID '%s' (returned null)", id);
		}
		return result;
	}

	/**
	 * @param calendar the date and time
	 * @return the hour formatted for the DB
	 */
	public static String getTimeOfTheDay(Calendar calendar) {
		String hours = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
		if (hours.length() < 2) {
			hours = "0" + hours;
		}
		String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
		if (minutes.length() < 2) {
			minutes = "0" + hours;
		}
		String result = hours + ":" + minutes + ":00";
		// MyLog.d(TAG, "hour:" + result);
		return result;
	}

	/**
	 * Return the date string from the JulianDay date
	 * @param jd the JulianDay date
	 * @return the date string.
	 */
	public static String getHourMinFromJD(double jd) {
		String result;
		double jd0 = jd + 0.5;
		double z = Math.floor(jd0);
		double f = jd0 - z;
		double a = 0.0;
		double alp = 0.0;
		if (z < 2299161) {
			a = z;
		} else {
			alp = Math.floor((z - 1867216.25) / 36524.25);
			a = z + 1.0 + alp - Math.floor(alp / 4.0);
		}
		double b = a + 1524;
		double c = Math.floor((b - 122.1) / 365.25);
		double d = Math.floor(365.25 * c);
		double e = Math.floor((b - d) / 30.6001);
		double day = b - d - Math.floor(30.6001 * e) + f;
		int mon = 0;
		if (e < 13.5) {
			mon = (int) e - 1;
		} else {
			mon = (int) e - 13;
		}
		int yr = 0;
		if (mon > 2.5) {
			yr = (int) c - 4716;
		} else {
			yr = (int) c - 4715;
		}
		String yearS = String.valueOf(yr);
		String monthS = String.valueOf(mon);
		String dayS = String.valueOf(Math.floor(day));

		int uth = (int) Math.floor(24.0 * (day - Math.floor(day)));
		int utm = (int) Math.floor(1440.0 * (day - Math.floor(day) - uth / 24.0));
		double uts = 86400.0 * (day - Math.floor(day) - uth / 24.0 - utm / 1440.0);
		// TODO remove unnecessary code.
		MyLog.v(TAG, "[" + yearS + "]-[" + monthS + "]-[" + dayS + "] " + "[" + uth + "]:[" + utm + "]:[" + String.valueOf(uts) + "].");
		String hourS = String.valueOf(uth);
		String minuteS = String.valueOf(utm);
		while (minuteS.length() < 2) {
			minuteS = "0" + minuteS;
		}
		result = hourS + " h " + minuteS;
		// MyLog.d(TAG, "result>" + result);
		return result;
	}

	/**
	 * Simple method to display a <b>short</b> message (toast) to the user. {@link Toast}
	 * @param context the activity displaying the message
	 * @param message the message to display.
	 */
	public static void notifyTheUser(Context context, String message) {
		final Toast makeText = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		makeText.show();
	}

	public static void notifyTheUserTop(Context context, String message) {
		final Toast makeText = Toast.makeText(context, message, Toast.LENGTH_SHORT);
		makeText.setGravity(Gravity.TOP, 0, TOAST_Y_OFFSET);
		makeText.show();
	}

	/**
	 * Print the application version in the log.
	 */
	public static void logAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PKG, 0);
			String versionName = packageInfo.versionName;
			int versionCode = packageInfo.versionCode;
			MyLog.i(TAG, "%s \"%s\" (v%s)", context.getString(R.string.app_name), versionName, versionCode);
		} catch (NameNotFoundException e) {
			MyLog.w(TAG, String.format("No VERSION for %s!", context.getString(R.string.app_name)), e);
		}
	}

	public static boolean isPackageExists(Context context, String targetPackage) {
		try {
			context.getPackageManager().getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	public static boolean isConnectedOrConnecting(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
		if (activeNetworkInfo == null) {
			return false;
		}
		return activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * @return the user language (fr/en/...)
	 */
	public static String getUserLanguage() {
		return Locale.getDefault().getLanguage();
	}

	/**
	 * @return the supported user language (fr or en)
	 */
	public static String getSupportedUserLocale() {
		if (getUserLanguage().equals(Locale.FRENCH.toString())) {
			return Locale.FRENCH.toString();
		} else {
			return Locale.ENGLISH.toString(); // default
		}
	}

	/**
	 * Extract the subway station IDs from the favorite subway station list
	 * @param subwayStationFavList the favorite subway station list
	 * @return the subway station IDs string
	 */
	public static String extractSubwayStationIDsFromFavList(List<Fav> subwayStationFavList) {
		StringBuilder sb = new StringBuilder();
		for (Fav subwayStationpFav : subwayStationFavList) {
			if (sb.length() > 0) {
				sb.append("+");
			}
			sb.append(subwayStationpFav.getFkId());
		}
		return sb.toString();
	}

	/**
	 * Extract bike stations terminal name from favorite list.
	 * @param bikeStationFavList the bike stations favorite list
	 * @return the bike stations terminal name
	 */
	public static String extractBikeStationTerminNamesFromFavList(List<Fav> bikeStationFavList) {
		StringBuilder sb = new StringBuilder();
		for (Fav stationFav : bikeStationFavList) {
			if (stationFav.getFkId() == null) { // need check because of previous issue corrupting favorites
				continue;
			}
			if (sb.length() > 0) {
				sb.append("+");
			}
			sb.append(stationFav.getFkId());
		}
		return sb.toString();
	}

	/**
	 * Show an about dialog.
	 * @param activity the activity asking for the dialog
	 */
	public static void showAboutDialog(Activity activity) {
		String versionName = "";
		String versionCode = "";
		try {
			PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(Constant.PKG, 0);
			versionName = packageInfo.versionName;
			versionCode = String.valueOf(packageInfo.versionCode);
		} catch (NameNotFoundException e) {
		}
		View view = activity.getLayoutInflater().inflate(R.layout.about, null, false);
		// set the version
		TextView versionTv = (TextView) view.findViewById(R.id.version);
		versionTv.setText(activity.getString(R.string.about_version, versionName, versionCode));

		new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.app_name)).setIcon(android.R.drawable.ic_dialog_info).setView(view)
				.setPositiveButton(activity.getString(android.R.string.ok), null).setCancelable(true).create().show();
	}

	/**
	 * @param version {@link Build#VERSION_CODES} value
	 * @return true if the version is older than the current version
	 */
	@SuppressWarnings("deprecation")
	// TODO use Build.VERSION.SDK_INT
	public static boolean isVersionOlderThan(int version) {
		return Integer.parseInt(Build.VERSION.SDK) < version;
	}

	/**
	 * Show soft keyboard (if no hardware keyboard).
	 * @param context the context
	 * @param view the focused view
	 */
	public static void showKeyboard(Activity context, View view) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	/**
	 * Hide soft keyboard (if no hardware keyboard).
	 * @param context the context
	 * @param view the focused view
	 */
	public static void hideKeyboard(Activity context, View view) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	/**
	 * Return the number of line of a text file.
	 * @param textFileIS the text file input stream
	 * @return the number of lines
	 * @throws IOException an I/O exception occurs
	 */
	public static int countNumberOfLine(InputStream textFileIS) throws IOException {
		InputStream is = new BufferedInputStream(textFileIS, 8192);
		byte[] c = new byte[1024];
		int count = 0;
		int readChars = 0;
		while ((readChars = is.read(c)) != -1) {
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n')
					++count;
			}
		}
		is.close();
		return count;
	}

	/**
	 * Clean favorites linking to old non-existing bus stops or subway stations. Use after STM DB updates.
	 * @param contentResolver the content resolver
	 */
	@SuppressWarnings("deprecation")
	public static void cleanFavorites(Context context) {
		MyLog.v(TAG, "cleanFavorites()");
		final ContentResolver contentResolver = context.getContentResolver();
		try {
			// bus stops
			List<Fav> busStopFavs = DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_BUS_STOP);
			List<RouteTripStop> routeTripStops = StmBusManager.findRouteTripStops(context, busStopFavs, false);
			for (Fav busStopFav : busStopFavs) {
				boolean stillInTheDB = false;
				for (RouteTripStop routeTripStop : routeTripStops) {
					if (busStopFav.getFkId().equals(routeTripStop.stop.id) && busStopFav.getFkId2().equals(routeTripStop.route.id)) {
						stillInTheDB = true;
					}
				}
				if (!stillInTheDB) {
					DataManager.deleteFav(contentResolver, busStopFav.getId());
				}
			}
			// subway stations
			List<Fav> subwayFavs = DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			for (Fav subwayFav : subwayFavs) {
				Stop stop = StmSubwayManager.findStopWithId(context, Integer.valueOf(subwayFav.getFkId()));
				if (stop == null) {
					DataManager.deleteFav(contentResolver, subwayFav.getId());
				}
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "Unknow error while cleaning favorite.");
		}
	}

	/**
	 * Update favorites bus stops into route stops.
	 */
	@SuppressWarnings("deprecation")
	public static void updateBusStopsToRouteStops(Context context) {
		MyLog.v(TAG, "updateBusStopsToRouteStops()");
		final ContentResolver contentResolver = context.getContentResolver();
		List<Fav> busStopFavs = DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_BUS_STOP);
		MyLog.d(TAG, "Favorite bus stops to upgrade: %s", (busStopFavs == null ? null : busStopFavs.size()));
		if (busStopFavs != null) {
			for (Fav busStopFav : busStopFavs) {
				try {
					final int stopId = Integer.valueOf(busStopFav.getFkId());
					final int routeId = Integer.valueOf(busStopFav.getFkId2());
					final String uid = RouteStop.getUID(StmBusManager.AUTHORITY, stopId, routeId);
					Fav newRouteStopFav = new Fav();
					newRouteStopFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
					newRouteStopFav.setFkId(uid);
					final boolean alreadyFavorite = DataManager.findFav(contentResolver, Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP, uid) != null;
					if (alreadyFavorite) {
						MyLog.d(TAG, "Favorite bus stop %s already migrated.", busStopFav);
					} else {
						final boolean added = DataManager.addFav(contentResolver, newRouteStopFav) != null;
						if (!added) {
							MyLog.d(TAG, "Favorite bus stop %s not converted to route stop!", busStopFav);
							continue; // don't remove not migrated
						}
					}
					final boolean deleted = DataManager.deleteFav(contentResolver, busStopFav.getId());
					if (!deleted) {
						MyLog.d(TAG, "Old favorite bus stop %s migrated but not deleted!", busStopFav);
					}
				} catch (Throwable t) {
					MyLog.w(TAG, t, "Error while migrating favorite bus stop %s to route stop!", busStopFav);
				}
			}
		}
		MyLog.d(TAG, "updateBusStopsToRouteStops() > DONE");
	}

	/**
	 * Update favorites subway stations into route stops.
	 */
	@SuppressWarnings("deprecation")
	public static void updateSubwayStationsToRouteStops(Context context) {
		MyLog.v(TAG, "updateSubwayStationsToRouteStops()");
		final ContentResolver contentResolver = context.getContentResolver();
		List<Fav> subwayStationFavs = DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
		MyLog.d(TAG, "Favorite subway stations to upgrade: %s", (subwayStationFavs == null ? null : subwayStationFavs.size()));
		if (subwayStationFavs != null) {
			for (Fav subwayStationFav : subwayStationFavs) {
				try {
					boolean allStopRoutesMigrated = true;
					final int stopId = Integer.valueOf(subwayStationFav.getFkId());
					List<Route> stopRoutes = StmSubwayManager.findRoutesWithStopIdList(context, stopId);
					if (stopRoutes == null || stopRoutes.size() == 0) {
						MyLog.d(TAG, "Favorite subway station %s route(s) not found!", subwayStationFav);
						allStopRoutesMigrated = false; // no stop routes!
					}
					if (stopRoutes != null) {
						for (Route stopRoute : stopRoutes) {
							final int routeId = stopRoute.id;
							final String uid = RouteStop.getUID(StmSubwayManager.AUTHORITY, stopId, routeId);
							Fav newRouteStopFav = new Fav();
							newRouteStopFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
							newRouteStopFav.setFkId(uid);
							final boolean alreadyFavorite = DataManager.findFav(contentResolver, Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP, uid) != null;
							if (alreadyFavorite) {
								MyLog.d(TAG, "Favorite subway station %s already migrated.", newRouteStopFav);
							} else {
								final boolean added = DataManager.addFav(contentResolver, newRouteStopFav) != null;
								if (!added) {
									MyLog.d(TAG, "Favorite subway station %s not converted to route stop!", subwayStationFav);
									allStopRoutesMigrated = false;
								}
							}
						}

					}
					if (!allStopRoutesMigrated) {
						MyLog.d(TAG, "Favorite subway station %s not converted to route stop!", subwayStationFav);
						continue; // don't remove not migrated
					}
					final boolean deleted = DataManager.deleteFav(contentResolver, subwayStationFav.getId());
					if (!deleted) {
						MyLog.d(TAG, "Old favorite subway station %s migrated but not deleted!", subwayStationFav);
					}
				} catch (Throwable t) {
					MyLog.w(TAG, t, "Error while migrating favorite subway station %s to route stop!", subwayStationFav);
				}
			}
		}
		MyLog.d(TAG, "updateSubwayStationsToRouteStops() > DONE");
	}

	/**
	 * Update favorites bus lines to match January 2012 bus lines number changes.
	 * @param contentResolver the content resolver
	 */
	@SuppressWarnings("deprecation")
	public static void updateFavoritesJan2012(ContentResolver contentResolver) {
		MyLog.v(TAG, "updateFavoritesJan2012()");
		try {
			Map<String, String> update = new HashMap<String, String>();
			update.put("77", "444");
			update.put("120", "495");
			update.put("132", "136");
			update.put("143", "440");
			update.put("148", "448");
			update.put("159", "469");
			// update.put("167", "777"); no bus stops
			// update.put("167", "767"); no bus stops
			// update.put("169", "769"); no bus stops
			update.put("173", "496");
			update.put("182", "486");
			update.put("184", "487");
			update.put("190", "491");
			update.put("194", "449");
			update.put("199", "432");
			update.put("210", "419");
			update.put("214", "409");
			update.put("221", "411");
			update.put("251", "212");
			update.put("261", "401");
			update.put("265", "407");
			update.put("268", "468");
			update.put("480", "178");
			update.put("505", "439");
			update.put("506", "406");
			update.put("515", "715");
			update.put("535", "435");
			List<Fav> busStopFavs = DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_BUS_STOP);
			for (Fav busStopFav : busStopFavs) {
				String busStopLineNumber = busStopFav.getFkId2();
				// IF the bus stop line number need to be updated DO
				if (update.keySet().contains(busStopLineNumber)) {
					// delete the old favorite
					DataManager.deleteFav(contentResolver, busStopFav.getId());
					// update the bus line number
					busStopFav.setFkId2(update.get(busStopLineNumber));
					// add the new favorite
					DataManager.addFav(contentResolver, busStopFav);
				}
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "Unknow error while updating favorites.");
		}
	}

	/**
	 * Set the DEMO mode with favorites and other settings...
	 * @param context the context
	 */
	public static void setDemoMode(Context context) {
		// set favorites
		Fav newFav = new Fav();
		newFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
		newFav.setFkId(RouteStop.getUID(StmBusManager.AUTHORITY, 54321, 10));
		DataManager.addFav(context.getContentResolver(), newFav);
		newFav.setFkId(RouteStop.getUID(StmBusManager.AUTHORITY, 52509, 24));
		DataManager.addFav(context.getContentResolver(), newFav);
		newFav.setFkId(RouteStop.getUID(StmBusManager.AUTHORITY, 55140, 48));
		DataManager.addFav(context.getContentResolver(), newFav);
		newFav.setFkId(RouteStop.getUID(StmBusManager.AUTHORITY, 11, 1)); // Berri-UQAM - Verte
		DataManager.addFav(context.getContentResolver(), newFav);
		newFav.setFkId(RouteStop.getUID(StmBusManager.AUTHORITY, 9, 2)); // Mont-Royal - Orange
		DataManager.addFav(context.getContentResolver(), newFav);
		newFav.setType(Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
		newFav.setFkId("6415"); // Wilson / Sherbrooke
		DataManager.addFav(context.getContentResolver(), newFav);
		SupportFactory.get().backupManagerDataChanged(context);
	}

	/**
	 * @param context the context
	 * @return {@link android.R.attr.textColorPrimary}.
	 */
	public static int getTextColorPrimary(Context context) {
		return getThemeAttribute(context, android.R.attr.textColorPrimary);
	}

	/**
	 * @param context the context
	 * @return {@link android.R.attr.textColorSecondary}.
	 */
	public static int getTextColorSecondary(Context context) {
		return getThemeAttribute(context, android.R.attr.textColorSecondary);
	}

	/**
	 * @param context the context
	 * @param resId the resource ID
	 * @return the theme attribute
	 */
	public static int getThemeAttribute(Context context, int resId) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(resId, tv, true);
		return context.getResources().getColor(tv.resourceId);
	}

	/**
	 * @return the available space for the application (/data/data/...)
	 */
	public static long getAvailableSize() {
		MyLog.v(TAG, "getAvailableSize()");
		StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
		return SupportFactory.get().getStatFsAvailableBlocksLong(stat) * SupportFactory.get().getStatFsBlockSizeLong(stat);
	}

	/**
	 * @param timestampInMillis time-stamp in milliseconds
	 * @return time-stamp in seconds
	 */
	public static int toTimestampInSeconds(Long timestampInMillis) {
		if (timestampInMillis == null) {
			return 0;
		}
		return (int) (timestampInMillis.longValue() / 1000);
	}

	/**
	 * @return the local system time in seconds
	 */
	public static int currentTimeSec() {
		return toTimestampInSeconds(System.currentTimeMillis());
	}

	/**
	 * Clean a bike station name.
	 * @param uncleanStation the source station string
	 * @return the cleaned bike station
	 */
	public static String cleanBikeStationName(String uncleanStation) {
		String result = uncleanStation;
		result = BusUtils.cleanBusStopPlace(result);
		// clean "/" => " / "
		result = result.replaceAll("(\\w)(/)(\\w)", "$1 / $3");
		return result;
	}

	/**
	 * Sleep for x seconds.
	 * @param timeInSec the number of seconds to sleep.
	 */
	public static void sleep(int timeInSec) {
		try {
			Thread.sleep(timeInSec * 1000);
		} catch (InterruptedException e) {
		}
	}

	public static String getJson(URLConnection urlc) throws UnsupportedEncodingException, IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(urlc.getInputStream(), "UTF-8"), 8);
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while reading json!");
		} finally {
			reader.close();
		}
		return sb.toString();
	}

	static Map<String, Integer> colorMap = new HashMap<String, Integer>();

	/**
	 * @param color RRGGBB
	 * @return color-integer
	 * @see Color#parseColor(String)
	 */
	public static int parseColor(String color) {
		if (!colorMap.containsKey(color)) {
			// Logger.d(TAG, "new color parsed: " + color);
			colorMap.put(color, Color.parseColor("#" + color));
		}
		return colorMap.get(color);
	}

	public static Uri newContentUri(String authority) {
		return Uri.parse("content://" + authority + "/");
	}

	public static boolean isContentProviderAvailable(Context context, String authority) {
		return context.getPackageManager().resolveContentProvider(authority, 0) != null;
	}

	public static boolean isAppInstalled(Context context, String providerAppPackage) {
		return Utils.isPackageExists(context, providerAppPackage);
	}

	public static final long RECENT_IN_MILLIS = 1000 * 60 * 60; // 1 hour

	/**
	 * @return time-stamp for now minus some time considered as recent (ex: 1 hour)
	 */
	public static long recentTimeMillis() {
		return System.currentTimeMillis() - RECENT_IN_MILLIS;
	}

	public static long currentTimeToTheMinuteMillis() {
		long currentTime = System.currentTimeMillis();
		currentTime -= currentTime % (60 * 1000);
		return currentTime;
	}
}

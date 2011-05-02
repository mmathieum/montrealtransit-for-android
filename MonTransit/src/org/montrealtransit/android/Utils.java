package org.montrealtransit.android;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
	private static final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("HH:mm");

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
	 * Format the hours according to the device settings.
	 * @param context the context used to get the device settings
	 * @param noFormatHour the original hours string
	 * @return the formatted hour string
	 */
	public static String formatHours(Context context, String noFormatHour) {
		// MyLog.v(TAG, "formatHours(%s)", noFormatHour);
		String result = "";
		try {
			result = getTimeFormatter(context).format(simpleDateFormatter.parse(noFormatHour.replace("h", ":")));
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while formatting '%s'.", noFormatHour);
			result = noFormatHour;
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
		if (currentLocale == null || dateFormatter == null
		        || currentLocale != context.getResources().getConfiguration().locale) {
			// get the current language/country
			currentLocale = context.getResources().getConfiguration().locale;
			// get the current date formatter
			dateFormatter = android.text.format.DateFormat.getTimeFormat(context);
		}
		return dateFormatter;
	}

	/**
	 * Format a string containing 2 hours strings.
	 * @param context the context use by the {@link Utils#formatHours(Context, String)} method
	 * @param noFormatHours the 2 hours string to format
	 * @param splitBy the separator between the 2 hours string in the source string
	 * @return the formatted 2 hours string
	 */
	public static String getFormatted2Hours(Context context, String noFormatHours, String splitBy) {
		// MyLog.v(TAG, "getFormatted2Hours(%s, %s)", noFormatHour, splitBy);
		try {
			int indexOfH = noFormatHours.indexOf(splitBy);
			String noFormatHour1 = noFormatHours.substring(0, indexOfH);
			String noFormatHour2 = noFormatHours.substring(indexOfH + splitBy.length());
			return Utils.formatHours(context, noFormatHour1) + " - " + Utils.formatHours(context, noFormatHour2);
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while formatting '%s'.", noFormatHours);
			return noFormatHours;
		}
	}

	/**
	 * 0 or more (no NPE)
	 * @param list the list
	 * @return The size of the list
	 */
	public static int getListSize(List<?> list) {
		int result = 0;
		if (list != null) {
			result = list.size();
		}
		return result;
	}

	/**
	 * Return a string representing the content of a list of string.
	 * @param list the list
	 * @return the string
	 */
	public static String toStringListOfString(List<String> list) {
		String result = "[";
		int i = 0;
		for (String string : list) {
			if (result.length() > 2) {
				result += "|";
			}
			result += i + ":" + string;
			i++;
		}
		result += "]";
		return result;
	}

	/**
	 * Return a string representing the content of the map of string,string.
	 * @param map the map
	 * @return the string
	 */
	public static String toStringMapOfStringString(Map<String, String> map) {
		String result = "[";
		for (String mapKey : map.keySet()) {
			if (result.length() > 2) {
				result += "|";
			}
			result += ":" + mapKey + ">" + map.get(mapKey);
		}
		result += "]";
		return result;
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
		String result = savedInstanceState != null ? savedInstanceState.getString(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the line id
			result = extras != null ? extras.getString(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved string value for string ID '%s' (returned null)", id);
		}
		return result;
	}

	public static float getSavedFloatValue(Intent intent, Bundle savedInstanceState, String id) {
		MyLog.v(TAG, "getSavedFloatValue(%s)", id);
		Float result = savedInstanceState != null ? savedInstanceState.getFloat(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the line id
			result = extras != null ? extras.getFloat(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved float value for string ID '%s' (returned null)", id);
		}
		return result;
	}

	/**
	 * @return the day of the week value in the DB
	 */
	public static String getDayOfTheWeek() {
		return getDayOfTheWeek(Calendar.getInstance());
	}

	/**
	 * @param calendar the date
	 * @return the day of the week value in the DB
	 */
	public static String getDayOfTheWeek(Calendar calendar) {
		switch (calendar.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SATURDAY:
			return StmStore.SubwayLine.FREQUENCES_K_DAY_SATURDAY;
		case Calendar.SUNDAY:
			return StmStore.SubwayLine.FREQUENCES_K_DAY_SUNDAY;
		default:
			return StmStore.SubwayLine.FREQUENCES_K_DAY_WEEK;
		}
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
		MyLog.v(TAG,
		        "[" + yearS + "]-[" + monthS + "]-[" + dayS + "] " + "[" + uth + "]:[" + utm + "]:["
		                + String.valueOf(uts) + "].");
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
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Simple method to display a <b>long</b> message (toast) to the user. {@link Toast}
	 * @param context the activity displaying the message
	 * @param message the message to display.
	 */
	public static void notifyTheUserLong(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	public static void saveSharedPreferences(Context context, String prefKey, String newValue) {
		MyLog.v(TAG, "saveSharedPreferences(%s, %s)", prefKey, newValue);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(prefKey, newValue);
		SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	public static void saveSharedPreferences(Context context, String prefKey, boolean newValue) {
		MyLog.v(TAG, "saveSharedPreferences(%s, %s)", prefKey, newValue);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(prefKey, newValue);
		SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
	}

	/**
	 * Return a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value
	 * @return the preference value
	 */
	public static String getSharedPreferences(Context context, String prefKey, String defaultValue) {
		// MyLog.v(TAG, "getSharedPreferences(%s, %s)", prefKey, defaultValue);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getString(prefKey, defaultValue);
	}

	/**
	 * Return a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value
	 * @return the preference value
	 */
	public static boolean getSharedPreferences(Context context, String prefKey, boolean defaultValue) {
		// MyLog.v(TAG, "getSharedPreferencesBoolean(%s, %s)", prefKey, defaultValue);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		return settings.getBoolean(prefKey, defaultValue);
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

	/**
	 * Indicates whether the specified action can be used as an intent. This method queries the package manager for installed packages that can respond to an
	 * intent with the specified action. If no suitable package is found, this method returns false.
	 * 
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
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
	 * Extract the bus stop IDs (bus stop code - bus line number) from the favorite bus stop list
	 * @param busStopFavList the favorite bus stop list
	 * @return the bus stop IDs string
	 */
	public static String extractBusStopIDsFromFavList(List<Fav> busStopFavList) {
		String result = "";
		for (Fav busStopFav : busStopFavList) {
			if (result.length() > 0) {
				result += "+";
			}
			result += busStopFav.getFkId() + "-" + busStopFav.getFkId2();
		}
		return result;
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

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(activity.getString(R.string.app_name));
		builder.setIcon(android.R.drawable.ic_dialog_info);
		builder.setView(view);
		builder.setPositiveButton(activity.getString(android.R.string.ok), null);
		builder.setCancelable(true);

		builder.create();
		builder.show();
	}

	/**
	 * Return the distance string matching the accuracy and the user settings.
	 * @param context the activity
	 * @param distanceInMeters the distance in meter
	 * @param accuracyInMeters the accuracy in meter
	 * @return the distance string.
	 */
	public static String getDistanceString(Context context, float distanceInMeters, float accuracyInMeters) {
		// MyLog.v(TAG, "getDistanceString(" + distanceInMeters + ", " + accuracyInMeters + ")");
		boolean isDetailed = getSharedPreferences(context, UserPreferences.PREFS_DISTANCE,
		        UserPreferences.PREFS_DISTANCE_DEFAULT).equals(UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = getSharedPreferences(context, UserPreferences.PREFS_DISTANCE_UNIT,
		        UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		// IF distance unit is Imperial DO
		if (distanceUnit.equals(UserPreferences.PREFS_DISTANCE_UNIT_IMPERIAL)) {
			float distanceInFeet = distanceInMeters * Constant.FEET_PER_M;
			float accuracyInFeet = accuracyInMeters * Constant.FEET_PER_M;
			return getDistance(distanceInFeet, accuracyInFeet, isDetailed, Constant.FEET_PER_MILE, 10, "ft", "mi");
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
	private static String getDistance(float distance, float accuracy, boolean isDetailed, float smallPerBig,
	        int threshold, String smallUnit, String bigUnit) {
		String result = "";
		// IF the location is enough precise AND the accuracy is 10% or more of the distance DO
		if (isDetailed && accuracy < distance && accuracy / distance > 0.1) {
			float shorterDistanceInFeet = distance - accuracy / 2;
			float longerDistanceInFeet = distance + accuracy;
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (distance > (smallPerBig / threshold)) {
				// use "big unit"
				float shorterDistanceInMile = shorterDistanceInFeet / smallPerBig;
				float niceShorterDistanceInMile = ((Integer) Math.round(shorterDistanceInMile * 10)).floatValue() / 10;
				float longerDistanceInMile = longerDistanceInFeet / smallPerBig;
				float niceLongerDistanceInMile = ((Integer) Math.round(longerDistanceInMile * 10)).floatValue() / 10;
				result = niceShorterDistanceInMile + " - " + niceLongerDistanceInMile + " " + bigUnit;
			} else {
				// use "small unit"
				int niceShorterDistanceInFeet = Math.round(shorterDistanceInFeet);
				int niceLongerDistanceInFeet = Math.round(longerDistanceInFeet);
				result = niceShorterDistanceInFeet + " - " + niceLongerDistanceInFeet + " " + smallUnit;
			}
			// ELSE IF the accuracy of the location is more than the distance DO
		} else if (accuracy > distance) { // basically, the location is in the blue circle in Maps
			// use the accuracy as a distance
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (accuracy > (smallPerBig / threshold)) {
				// use "big unit"
				float accuracyInMile = accuracy / smallPerBig;
				float niceAccuracyInMile = ((Integer) Math.round(accuracyInMile * 10)).floatValue() / 10;
				result += "< " + niceAccuracyInMile + " " + bigUnit;
			} else {
				// use "small unit"
				int niceAccuracyInFeet = Math.round(accuracy);
				result += "< " + niceAccuracyInFeet + " " + smallUnit;
			}
			// TODO ? ELSE if accuracy non-significant DO show the longer distance ?
		} else {
			// IF distance in "small unit" is big enough to fit in "big unit" DO
			if (distance > (smallPerBig / threshold)) {
				// use "big unit"
				float distanceInMile = distance / smallPerBig;
				float niceDistanceInMile = ((Integer) Math.round(distanceInMile * 10)).floatValue() / 10;
				result += niceDistanceInMile + " " + bigUnit;
			} else {
				// use "small unit"
				int niceDistanceInFeet = Math.round(distance);
				result += niceDistanceInFeet + " " + smallUnit;
			}
		}
		return result;
	}

	/**
	 * @param version {@link Build#VERSION_CODES} value
	 * @return true if the version is older than the current version
	 */
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
		InputStream is = new BufferedInputStream(textFileIS);
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
	public static void cleanFavorites(ContentResolver contentResolver) {
		MyLog.v(TAG, "cleanFavorites()");
		try {
			List<Fav> busStopFavs = DataManager.findFavsByTypeList(contentResolver,
			        DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			List<BusStop> busStops = StmManager.findBusStopsExtendedList(contentResolver,
			        Utils.extractBusStopIDsFromFavList(busStopFavs));
			for (Fav busStopFav : busStopFavs) {
				boolean stillInTheDB = false;
				for (BusStop busStop : busStops) {
					if (busStopFav.getFkId().equals(busStop.getCode())
					        && busStopFav.getFkId2().equals(busStop.getLineNumber())) {
						stillInTheDB = true;
					}
				}
				if (!stillInTheDB) {
					DataManager.deleteFav(contentResolver, busStopFav.getId());
				}
			}
			List<Fav> subwayFavs = DataManager.findFavsByTypeList(contentResolver,
			        DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			for (Fav subwayFav : subwayFavs) {
				SubwayStation station = StmManager.findSubwayStation(contentResolver, subwayFav.getFkId());
				if (station == null) {
					DataManager.deleteFav(contentResolver, subwayFav.getId());
				}
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "Unknow error while cleaning favorite.");
		}
	}
}
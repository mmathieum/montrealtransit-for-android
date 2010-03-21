package org.montrealtransit.android;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
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
	 * Read the input stream and write the stream to the output stream file.
	 * @param is the input stream
	 * @param os the output steam
	 */
	public static void getInputStreamToFile(InputStream is, FileOutputStream os) {
		MyLog.v(TAG, "getInputStreamToFile()");
		OutputStreamWriter writer = new OutputStreamWriter(os);
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				writer.write(new String(b, 0, n));
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, "Error while reading the input stream and writing into the file.", ioe);
		} finally {
			try {
				writer.flush();
				writer.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while finishing and closing the file.", ioe);
			}
		}
	}

	/**
	 * Return a list of 2 R.string.<ID> for the bus line direction string.
	 * @param busLineDirectionString the direction string
	 * @return the 2 R.string.<ID>
	 */
	public static List<Integer> getBusLineDirectionStringIdFromId(String busLineDirectionString) {
		MyLog.v(TAG, "getBusLineDirectionStringIdFromId(" + busLineDirectionString + ")");
		String lineId = busLineDirectionString.substring(0, busLineDirectionString.length() - 3);
		String otherChar = busLineDirectionString.substring(busLineDirectionString.length() - 3, busLineDirectionString.length() - 1);
		String lastChar = busLineDirectionString.substring(busLineDirectionString.length() - 1);

		List<Integer> results = new ArrayList<Integer>();
		if (lastChar.equalsIgnoreCase("N")) {
			results.add(R.string.north);
		} else if (lastChar.equalsIgnoreCase("S")) {
			results.add(R.string.south);
		} else if (lastChar.equalsIgnoreCase("E")) {
			results.add(R.string.east);
		} else if (lastChar.equalsIgnoreCase("O")) {
			results.add(R.string.west);
		} else {
			results.add(0);
		}
		int lineNumber = Integer.valueOf(lineId);
		switch (lineNumber) {
		case 11:
			if (otherChar.equals("SO")) {
				results.add(R.string.post_9pm_route);
			}
			break;
		case 15:
			if (busLineDirectionString.equals("15SOE")) {
				results.add(R.string.evening_route);
			}
			break;
		case 33:
			if (otherChar.equals("SO")) {
				results.add(R.string.evening_route);
			} else if (otherChar.equals("JO")) {
				results.add(R.string.morning_route);
			}
			break;
		case 46:
			if (busLineDirectionString.startsWith("46AB")) {
				results.add(R.string.regular_route);
			} else if (busLineDirectionString.equals("46AC0")) {
				results.add(R.string.route_by_way_of_fairmount);
			}
			break;
		case 52:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 68:
			if (otherChar.equals("AB")) {
				results.add(R.string.regular_route);
			} else if (otherChar.equals("AC")) {
				results.add(R.string.route_leading_to_timberlea);
			}
			break;
		case 70:
			if (otherChar.equals("AB")) {
				results.add(R.string.saturday_and_sunday_bus_route);
			} else if (otherChar.equals("AC")) {
				results.add(R.string.weekday_bus_route);
			}
			break;
		case 103:
			if (busLineDirectionString.equals("103ACO")) {
				results.add(R.string.route_by_way_of_westhill);
			} else if (busLineDirectionString.equals("103JOO")) {
				results.add(R.string.regular_route);
			}
			break;
		case 115:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 131:
			if (busLineDirectionString.equals("131HPS") || busLineDirectionString.equals("131HPN")) {
				results.add(R.string.monday_to_friday_between_9am_and_330pm);
			}
			break;
		case 135:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 143:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 146:
			if (otherChar.equals("SW")) {
				results.add(R.string.route_leading_to_henri_bourassa_station);
			}
			break;
		case 148:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 166:
			if (otherChar.equals("L2")) {
				results.add(R.string.route_after_8pm_by_way_of_ridgewood);
			}
			break;
		case 182:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 184:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 188:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 194:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route_peak_period);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route_peak_period);
			} else if (otherChar.equals("HC")) {
				results.add(R.string.route_off_peak_periods);
			}
			break;
		case 197:
			if (otherChar.equals("JO")) {
				results.add(R.string.regular_route);
			} else if (busLineDirectionString.equals("197HPE")) {
				results.add(R.string.route_by_way_of_pepiniere);
			}
			break;
		case 199:
			if (otherChar.equals("PM")) {
				results.add(R.string.pm_route_peak_period);
			} else if (busLineDirectionString.equals("199AMN")) {
				results.add(R.string.am_route_peak_period);
			} else if (busLineDirectionString.equals("199AMS")) {
				results.add(R.string.am_route_peak_period_and_off_peaks_periods);
			} else if (busLineDirectionString.equals("199HCN")) {
				results.add(R.string.route_off_peak_periods);
			}
			break;
		case 204:
			if (otherChar.equals("JO")) {
				results.add(R.string.rush_hour_route);
			} else if (otherChar.equals("HP")) {
				results.add(R.string.route_during_off_peak_periods);
			}
			break;
		case 214:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 261:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 410:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 460:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("PM")) {
				results.add(R.string.pm_route);
			}
			break;
		case 505:
			if (otherChar.equals("AM")) {
				results.add(R.string.am_route);
			} else if (otherChar.equals("TP")) {
				results.add(R.string.pm_route);
			}
			break;
		case 515:
			if (busLineDirectionString.equals("51501E")) {
				results.add(R.string.to_the_old_montreal);
			} else if (busLineDirectionString.equals("51501O")) {
				results.add(R.string.to_the_old_port_of_montreal);
			}
			break;
		default:
			break;
		}
		return results;
	}

	/**
	 * Return the R.drawable ID of the image matching the bus line type.
	 * @param type the bus line type
	 * @return the image ID
	 */
	public static int getBusLineTypeImgFromType(String type) {
		MyLog.v(TAG, "getBusLineTypeImgFromType(" + type + ")");
		if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE)) {
			return R.drawable.bus_type_soleil;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RUSH_HOUR_SERVICE)) {
			return R.drawable.bus_type_hot;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_METROBUS_SERVICE)) {
			return R.drawable.bus_type_mbus;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_TRAINBUS)) {
			return R.drawable.bus_type_tbus;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE)) {
			return R.drawable.bus_type_snuit;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_EXPRESS_SERVICE)) {
			return R.drawable.bus_type_express;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RESERVED_LANE_SERVICE)) {
			return R.drawable.bus_type_voieres;
		} else {
			MyLog.w(TAG, "Unknown bus line type \"" + type + "\".");
			return android.R.drawable.ic_dialog_alert;
		}
	}

	/**
	 * Return the bus line type string ID from the bus line type code.
	 * @param type the bus line type code
	 * @return the bus line type string ID
	 */
	public static int getBusStringFromType(String type) {
		MyLog.v(TAG, "getBusStringFromType(" + type + ")");
		if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE)) {
			return R.string.bus_type_soleil;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RUSH_HOUR_SERVICE)) {
			return R.string.bus_type_hot;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_METROBUS_SERVICE)) {
			return R.string.bus_type_mbus;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_TRAINBUS)) {
			return R.string.bus_type_tbus;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE)) {
			return R.string.bus_type_snuit;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_EXPRESS_SERVICE)) {
			return R.string.bus_type_express;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RESERVED_LANE_SERVICE)) {
			return R.string.bus_type_voieres;
		} else {
			MyLog.w(TAG, "Unknown bus line type \"" + type + "\".");
			return R.string.error;
		}
	}

	/**
	 * Clean the bus stop place.
	 * @param uncleanStopPlace the original bus stop place
	 * @return the cleaned bus stop place
	 */
	public static String cleanBusStopPlace(String uncleanStopPlace) {
		MyLog.v(TAG, "cleanBusStopPlace(" + uncleanStopPlace + ")");
		String result = uncleanStopPlace;
		if (result.startsWith(Constant.PLACE_CHAR_DE)) {
			result = result.substring(Constant.PLACE_CHAR_DE_LENGTH);
		} else if (result.startsWith(Constant.PLACE_CHAR_DES)) {
			result = result.substring(Constant.PLACE_CHAR_DES_LENGTH);
		} else if (result.startsWith(Constant.PLACE_CHAR_DU)) {
			result = result.substring(Constant.PLACE_CHAR_DU_LENGTH);
		}

		if (result.contains(Constant.PLACE_CHAR_IN_DE)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DE, Constant.PLACE_CHAR_IN);
		} else if (result.contains(Constant.PLACE_CHAR_IN_DES)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DES, Constant.PLACE_CHAR_IN);
		} else if (result.contains(Constant.PLACE_CHAR_IN_DU)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DU, Constant.PLACE_CHAR_IN);
		}

		if (result.contains(Constant.PLACE_CHAR_PARENTHESE_STATION)) {
			result = result.replace(Constant.PLACE_CHAR_PARENTHESE_STATION, Constant.PLACE_CHAR_PARENTHESE);
		}
		// TODO MORE ?
		return result;
	}

	/**
	 * Format the hours according to the device settings.
	 * @param context the context used to get the device settings
	 * @param shour the original hours string
	 * @return the formatted hour string
	 */
	public static String formatHours(Context context, String shour) {
		// MyLog.v(TAG, "formatHours(" + shour + ")");
		// TODO propose an option to specify the format in the application
		// because this global settings may not works sometimes
		// CyanogenMod 4.2.x & CursorSenceROM => French (Canada) => format is 24
		// but value:null ???
		String hourSetting = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
		// TODO MyLog.d(TAG, "hour setting:" + hourSetting + ".");
		String result = "";
		if (hourSetting != null && hourSetting.equals("24")) {
			// 24
			String hour = shour.substring(0, shour.indexOf("h")).trim();
			String minute = shour.substring(shour.indexOf("h") + 1).trim();
			result = hour + ":" + minute;
		} else {
			// 12
			boolean isPM = false;
			String minuteS = shour.substring(shour.indexOf("h") + 1).trim();
			int hour = Integer.valueOf(shour.substring(0, shour.indexOf("h")).trim());
			if (hour > 12) {
				isPM = true;
				hour = hour - 12;
			}
			if (!isPM && hour == 0) {
				hour = 12;
			}
			String hourS = String.valueOf(hour);
			String ampm = isPM ? "PM" : "AM";
			result = hourS + ":" + minuteS + " " + ampm;
		}
		return result;
	}

	/**
	 * Format a string containing 2 hours strings.
	 * @param context the context use by the {@link Utils#formatHours(Context, String)} method
	 * @param shours the 2 hours string to format
	 * @param splitBy the separator between the 2 hours string in the source string
	 * @return the formatted 2 hours string
	 */
	public static String getFormatted2Hours(Context context, String shours, String splitBy) {
		// MyLog.v(TAG, "getFormatted2Hours(" + shours + ", " + splitBy + ")");
		String startHour = shours.substring(0, shours.indexOf(splitBy)).trim();
		String endHour = shours.substring(shours.indexOf(splitBy) + 1).trim();
		return Utils.formatHours(context, startHour) + " - " + Utils.formatHours(context, endHour);
	}

	/**
	 * Return the size of the cursor. Return 0 is the cursor is null.
	 * @param cursor the cursor
	 * @return the cursor size
	 */
	public static int getCursorSize(Cursor cursor) {
		MyLog.v(TAG, "getCursorSize()");
		int result = 0;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result++;
			}
			while (cursor.moveToNext()) {
				result++;
			}
		}
		return result;
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
	 * Return the subway line name string ID from the subway line number.
	 * @param subwayLineNumber the subway line number
	 * @return the subway line name string ID
	 */
	public static int getSubwayLineName(int number) {
		MyLog.v(TAG, "getSubwayLineName(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.string.green_line;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.string.orange_line;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.string.yellow_line;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.string.blue_line;
		default:
			MyLog.w(TAG, "Unknown subway line number \"" + number + "\".");
			return R.string.error;
		}
	}

	/**
	 * Return the subway line image ID for the subway line number
	 * @param number the subway line number
	 * @return the subway line image ID
	 */
	public static int getSubwayLineImg(int number) {
		MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.drawable.green;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.drawable.orange;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.drawable.yellow;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.drawable.blue;
		default:
			MyLog.w(TAG, "Unknown image for subway line number \"" + number + "\".");
			return R.drawable.icon;
		}
	}

	/**
	 * Return the subway line color ID from the subway line number
	 * @param number the subway line number
	 * @return the subway line color ID
	 */
	public static int getSubwayLineColor(int number) {
		MyLog.v(TAG, "getSubwayLineColor(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return Color.GREEN;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return Color.rgb(255, 165, 0); // Orange
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return Color.YELLOW;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return Color.BLUE;
		default:
			MyLog.w(TAG, "Unknown color for subway line number \"" + number + "\".");
			return Color.BLACK;
		}
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
		MyLog.v(TAG, "getSavedStringValue(" + id + ")");
		String result = savedInstanceState != null ? savedInstanceState.getString(id) : null;
		// IF the activity was already launch, get the last id
		if (result == null) {
			// get the extras bundles from the intent
			Bundle extras = intent.getExtras();
			// IF there is extras, get the line id
			result = extras != null ? extras.getString(id) : null;
		}
		if (result == null) {
			MyLog.w(TAG, "Can't find the saved string value for string ID \"" + id + "\" (returned null)");
		}
		return result;
	}

	/**
	 * Return the day of the week
	 * @return the day of the week
	 */
	public static String getDayOfTheWeek() {
		Calendar rightNow = Calendar.getInstance();
		if (rightNow.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			return StmStore.SubwayLine.FREQUENCES_K_DAY_SATURDAY;
		} else if (rightNow.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			return StmStore.SubwayLine.FREQUENCES_K_DAY_SUNDAY;
		} else {
			return StmStore.SubwayLine.FREQUENCES_K_DAY_WEEK;
		}
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
		MyLog.v(TAG, "result>" + result);
		return result;
	}

	/**
	 * Simple method to display a message (toast) to the user. {@link Toast}
	 * @param context the activity displaying the message
	 * @param message the message to display.
	 */
	public static void notifyTheUser(Context context, String message) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	/**
	 * Parse the subway line list to extract the subway line numbers.
	 * @param subwayLines the subway line list
	 * @return the subway line numbers list
	 */
	public static List<String> extractSubwayLineNumbers(List<StmStore.SubwayLine> subwayLines) {
		List<String> result = new ArrayList<String>();
		for (StmStore.SubwayLine subwayLine : subwayLines) {
			result.add(String.valueOf(subwayLine.getNumber()));
		}
		return result;
	}

	/**
	 * Parse the bus line list to extract the bus line numbers.
	 * @param otherBusLine the bus line list
	 * @return the bus line numbers list
	 */
	public static List<String> extractBusLineNumbersFromBusLine(List<BusLine> otherBusLine) {
		List<String> result = new ArrayList<String>();
		for (BusLine busLine : otherBusLine) {
			result.add(busLine.getNumber());
		}
		return result;
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	public static void saveSharedPreferences(Context context, String prefKey, String newValue) {
		MyLog.v(TAG, "saveSharedPreferences(" + prefKey + ", " + newValue + ")");
		SharedPreferences settings = context.getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(prefKey, newValue);
		editor.commit();
	}

	/**
	 * Return a new preference value.
	 * @param context the context calling the method.
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value.
	 * @return the preference value
	 */
	public static String getSharedPreferences(Context context, String prefKey, String defaultValue) {
		MyLog.v(TAG, "saveSharedPreferences(" + prefKey + ", " + defaultValue + ")");
		SharedPreferences settings = context.getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE);
		return settings.getString(prefKey, defaultValue);
	}

	/**
	 * Print the application version in the log.
	 */
	public static void logAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constant.PKG, 0);
			String versionName = packageInfo.versionName;
			int versionCode = packageInfo.versionCode;
			MyLog.i(TAG, context.getResources().getString(R.string.app_name) + " \"" + versionName + "\" (v" + versionCode + ")");
		} catch (NameNotFoundException e) {
			MyLog.w(TAG, "No VERSION for " + context.getResources().getString(R.string.app_name) + "!", e);
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
}

package org.montrealtransit.android;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.BusLine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
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
		MyLog.v(TAG, "getInputStreamToFile(" + encoding + ")");
		OutputStreamWriter writer = new OutputStreamWriter(os);
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				String string = new String(b, 0, n, encoding);
				writer.write(string);
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
	 * Read the input stream and return the content as a string
	 * @param is the input stream
	 * @param encoding the encoding
	 * @return the string
	 */
	public static String getInputStreamToString(InputStream is, String encoding) {
		MyLog.v(TAG, "getInputStreamToString(" + encoding + ")");
		String result = "";
		byte[] b = new byte[4096];
		try {
			for (int n; (n = is.read(b)) != -1;) {
				String string = new String(b, 0, n, encoding);
				result += string;
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, "Error while reading the input stream and writing into a string.", ioe);
		} finally {
			try {
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while finishing and closing the file.", ioe);
			}
		}
		return result;
	}

	/**
	 * Return a list of 2 R.string.<ID> for the bus line direction string.
	 * @param directionId the direction string
	 * @return the 2 R.string.<ID>
	 */
	public static List<Integer> getBusLineDirectionStringIdFromId(String directionId) {
		MyLog.v(TAG, "getBusLineDirectionStringIdFromId(" + directionId + ")");
		int directionIdLength = directionId.length();
		String mainDirection = directionId.substring(directionIdLength - 1);

		List<Integer> results = new ArrayList<Integer>();
		if (mainDirection.equalsIgnoreCase("N")) {
			results.add(R.string.north);
		} else if (mainDirection.equalsIgnoreCase("S")) {
			results.add(R.string.south);
		} else if (mainDirection.equalsIgnoreCase("E")) {
			results.add(R.string.east);
		} else if (mainDirection.equalsIgnoreCase("O")) {
			results.add(R.string.west);
		} else {
			results.add(0);
		}
		if (directionIdLength > 1) {
			String extraDirectionInfo = directionId.substring(directionIdLength - 3, directionIdLength - 1);
			int lineNumber = Integer.valueOf(directionId.substring(0, directionIdLength - 3));
			switch (lineNumber) {
			case 11:
				if (extraDirectionInfo.equals("SO")) {
					results.add(R.string.post_9pm_route);
				}
				break;
			case 15:
				if (directionId.equals("15SOE")) {
					results.add(R.string.evening_route);
				}
				break;
			case 33:
				if (extraDirectionInfo.equals("SO")) {
					results.add(R.string.evening_route);
				} else if (extraDirectionInfo.equals("JO")) {
					results.add(R.string.morning_route);
				}
				break;
			case 46:
				if (extraDirectionInfo.equals("AC")) {
					results.add(R.string.route_by_way_of_fairmount);
				}
				break;
			case 52:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 68:
				if (extraDirectionInfo.equals("AC")) {
					results.add(R.string.route_leading_to_timberlea);
				}
				break;
			case 70:
				if (extraDirectionInfo.equals("AB")) {
					results.add(R.string.saturday_and_sunday_bus_route);
				} else if (extraDirectionInfo.equals("AC")) {
					results.add(R.string.weekday_bus_route);
				}
				break;
			case 103:
				if (directionId.equals("103ACO")) {
					results.add(R.string.route_by_way_of_westhill);
				}
				break;
			case 115:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 131:
				if (directionId.equals("131HPS") || directionId.equals("131HPN")) {
					results.add(R.string.monday_to_friday_between_9am_and_330pm);
				}
				break;
			case 135:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 143:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 146:
				if (extraDirectionInfo.equals("SW")) {
					results.add(R.string.route_leading_to_henri_bourassa_station);
				}
				break;
			case 148:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 166:
				if (extraDirectionInfo.equals("L2")) {
					results.add(R.string.route_after_8pm_by_way_of_ridgewood);
				}
				break;
			case 182:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 184:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 188:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 194:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route_peak_period);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route_peak_period);
				} else if (extraDirectionInfo.equals("HC")) {
					results.add(R.string.route_off_peak_periods);
				}
				break;
			case 197:
				if (directionId.equals("197HPE")) {
					results.add(R.string.route_by_way_of_pepiniere);
				}
				break;
			case 199:
				if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route_peak_period);
				} else if (directionId.equals("199AMN")) {
					results.add(R.string.am_route_peak_period);
				} else if (directionId.equals("199AMS")) {
					results.add(R.string.am_route_peak_period_and_off_peaks_periods);
				} else if (directionId.equals("199HCN")) {
					results.add(R.string.route_off_peak_periods);
				}
				break;
			case 204:
				if (extraDirectionInfo.equals("JO")) {
					results.add(R.string.rush_hour_route);
				} else if (extraDirectionInfo.equals("HP")) {
					results.add(R.string.route_during_off_peak_periods);
				}
				break;
			case 214:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 261:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 410:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 460:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("PM")) {
					results.add(R.string.pm_route);
				}
				break;
			case 505:
				if (extraDirectionInfo.equals("AM")) {
					results.add(R.string.am_route);
				} else if (extraDirectionInfo.equals("TP")) {
					results.add(R.string.pm_route);
				}
				break;
			case 515:
				if (directionId.equals("51501E")) {
					results.add(R.string.to_the_old_montreal);
				} else if (directionId.equals("51501O")) {
					results.add(R.string.to_the_old_port_of_montreal);
				}
				break;
			default:
				break;
			}
		}
		return results;
	}

	/**
	 * Return the R.drawable ID of the image matching the bus line type.
	 * @param type the bus line type
	 * @return the image ID
	 */
	public static int getBusLineTypeImgFromType(String type) {
		// MyLog.v(TAG, "getBusLineTypeImgFromType(" + type + ")");
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
	 * @param type the bus line type
	 * @return the color ID matching the bus line type
	 */
	public static int getBusLineTypeBgColorFromType(String type) {
		// MyLog.v(TAG, "getBusLineTypeImgFromType(" + type + ")");
		if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE)) {
			return Color.rgb(0, 96, 170); // BLUE;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RUSH_HOUR_SERVICE)) {
			return Color.RED;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_METROBUS_SERVICE)) {
			return Color.rgb(0, 115, 57); // GREEN
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_TRAINBUS)) {
			return Color.rgb(0, 156, 33); // GREEN light
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE)) {
			return Color.BLACK;
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_EXPRESS_SERVICE)) {
			return Color.rgb(0, 115, 57); // GREEN
		} else if (type.equalsIgnoreCase(StmStore.BusLine.LINE_TYPE_RESERVED_LANE_SERVICE)) {
			return Color.rgb(0, 115, 57); // GREEN
		} else {
			MyLog.w(TAG, "Unknown bus line type \"" + type + "\".");
			return Color.GRAY;
		}
	}

	/**
	 * Return the bus line type string ID from the bus line type code.
	 * @param type the bus line type code
	 * @return the bus line type string ID
	 */
	public static int getBusStringFromType(String type) {
		// MyLog.v(TAG, "getBusStringFromType(" + type + ")");
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

		if (result.startsWith(Constant.PLACE_CHAR_L)) {
			result = result.substring(Constant.PLACE_CHAR_L_LENGTH);
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
	 * @param noFormatHour the original hours string
	 * @return the formatted hour string
	 */
	public static String formatHours(Context context, String noFormatHour) {
		// MyLog.v(TAG, "formatHours(" + noFormatHour + ")");
		String result = "";
		try {
			result = getDateFormatter(context).format(simpleDateFormatter.parse(noFormatHour.replace("h", ":")));
		} catch (Exception e) {
			MyLog.w(TAG, "Error while formatting '" + noFormatHour + "'.", e);
			result = noFormatHour;
		}
		return result;
	}

	/**
	 * Use a static field to store the DateFormatter for improve the performance.
	 * @param context the context used to get the device settings
	 * @return the date formatter matching the device settings
	 */
	public static DateFormat getDateFormatter(Context context) {
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
		// MyLog.v(TAG, "getFormatted2Hours(" + noFormatHour + ", " + splitBy + ")");
		try {
			int indexOfH = noFormatHours.indexOf(splitBy);
			String noFormatHour1 = noFormatHours.substring(0, indexOfH);
			String noFormatHour2 = noFormatHours.substring(indexOfH + splitBy.length());
			return Utils.formatHours(context, noFormatHour1) + " - " + Utils.formatHours(context, noFormatHour2);
		} catch (Exception e) {
			MyLog.w(TAG, "Error while formatting '" + noFormatHours + "'.");
			return noFormatHours;
		}
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
	 * Return the subway line short name string ID from the subway line number.
	 * @param subwayLineNumber the subway line number
	 * @return the subway line short name string ID
	 */
	public static int getSubwayLineNameShort(int number) {
		MyLog.v(TAG, "getSubwayLineNameShort(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.string.green_line_short;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.string.orange_line_short;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.string.yellow_line_short;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.string.blue_line_short;
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
	public static int getSubwayLineImgId(int number) {
		//MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
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
			return R.drawable.yellow;
		}
	}
	
	/**
	 * @param the subway line number
	 * @return the subway line list image
	 */
	public static int getSubwayLineImgListId(int number) {
		//MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.drawable.green_list;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.drawable.orange_list;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.drawable.yellow_list;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.drawable.blue_list;
		default:
			MyLog.w(TAG, "Unknown image for subway line number \"" + number + "\".");
			return R.drawable.yellow;
		}
	}
	
	/**
	 * @param the subway line number
	 * @return the subway line list top image
	 */
	public static int getSubwayLineImgListTopId(int number) {
		//MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.drawable.green_list_top;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.drawable.orange_list_top;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.drawable.yellow_list_top;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.drawable.blue_list_top;
		default:
			MyLog.w(TAG, "Unknown image for subway line number \"" + number + "\".");
			return R.drawable.yellow;
		}
	}
	
	/**
	 * @param the subway line number
	 * @return the subway line list middle image
	 */
	public static int getSubwayLineImgListMiddleId(int number) {
		//MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.drawable.green_list_middle;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.drawable.orange_list_middle;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.drawable.yellow_list_middle;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.drawable.blue_list_middle;
		default:
			MyLog.w(TAG, "Unknown image for subway line number \"" + number + "\".");
			return R.drawable.yellow;
		}
	}
	
	/**
	 * @param the subway line number
	 * @return the subway line list bottom image
	 */
	public static int getSubwayLineImgListBottomId(int number) {
		//MyLog.v(TAG, "getSubwayLineImg(" + number + ")");
		switch (number) {
		case StmStore.SubwayLine.GREEN_LINE_NUMBER:
			return R.drawable.green_list_bottom;
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return R.drawable.orange_list_bottom;
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return R.drawable.yellow_list_bottom;
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return R.drawable.blue_list_bottom;
		default:
			MyLog.w(TAG, "Unknown image for subway line number \"" + number + "\".");
			return R.drawable.yellow;
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
			return Color.rgb(0, 148, 52);// green
		case StmStore.SubwayLine.ORANGE_LINE_NUMBER:
			return Color.rgb(236, 127, 0); // Orange
		case StmStore.SubwayLine.YELLOW_LINE_NUMBER:
			return Color.rgb(255, 227, 1); // yellow
		case StmStore.SubwayLine.BLUE_LINE_NUMBER:
			return Color.rgb(0, 157, 224); // blue
		default:
			MyLog.w(TAG, "Unknown color for subway line number \"" + number + "\".");
			return Color.WHITE;
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
			hours = "0"+ hours;
		}
		String minutes = String.valueOf(calendar.get(Calendar.MINUTE));
		if (minutes.length() < 2) {
			minutes = "0"+ hours;
		}
		String result = hours + ":" + minutes + ":00";
		//String result = hours + "h" + minutes;
		MyLog.d(TAG, "hour:"+result);
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
		MyLog.v(TAG, "[" + yearS + "]-[" + monthS + "]-[" + dayS + "] " + "[" + uth + "]:[" + utm + "]:["
		        + String.valueOf(uts) + "].");
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
	 * Simple method to display a <b>short</b> message (toast) to the user. {@link Toast}
	 * @param context the activity displaying the message
	 * @param message the message to display.
	 */
	public static void notifyTheUser(Context context, String message) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	/**
	 * Simple method to display a <b>long</b> message (toast) to the user. {@link Toast}
	 * @param context the activity displaying the message
	 * @param message the message to display.
	 */
	public static void notifyTheUserLong(Context context, String message) {
		int duration = Toast.LENGTH_LONG;
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
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.edit().putString(prefKey, newValue).commit();
	}

	/**
	 * Return a new preference value.
	 * @param context the context calling the method.
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value.
	 * @return the preference value
	 */
	public static String getSharedPreferences(Context context, String prefKey, String defaultValue) {
		//MyLog.v(TAG, "getSharedPreferences(" + prefKey + ", " + defaultValue + ")");
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
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
			MyLog.i(TAG, context.getString(R.string.app_name) + " \"" + versionName + "\" (v"
			        + versionCode + ")");
		} catch (NameNotFoundException e) {
			MyLog.w(TAG, "No VERSION for " + context.getString(R.string.app_name) + "!", e);
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
	public static String getUserLocale() {
		return Locale.getDefault().getLanguage();
	}

	/**
	 * Extract the bus stop IDs (bus stop code - bus line number) from the favorite list
	 * @param favList the favorite list
	 * @return the bus stop IDs string
	 */
	public static String extractBusStopIDsFromFavList(List<Fav> favList) {
		String favIdsS = "";
		for (DataStore.Fav favId : favList) {
			if (favIdsS.length() > 0) {
				favIdsS += "+";
			}
			favIdsS += favId.getFkId() + "-" + favId.getFkId2();
		}
		return favIdsS;
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
	 * Check if a bus stop code is in the database.
	 * @param context the activity
	 * @param stopCode the bus stop code
	 * @return true if the bus stop code exist
	 */
	public static boolean isStopCodeValid(Context context, String stopCode) {
		return StmManager.findBusStop(context.getContentResolver(), stopCode) != null;
	}

	/**
	 * Check if a bus line number is in the database.
	 * @param context the activity
	 * @param lineNumber the bus line number
	 * @return true if the bus line exist
	 */
	public static boolean isBusLineNumberValid(Context context, String lineNumber) {
		return StmManager.findBusLine(context.getContentResolver(), lineNumber) != null;
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
}

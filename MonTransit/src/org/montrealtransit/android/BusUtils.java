package org.montrealtransit.android;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.content.Context;
import android.graphics.Color;

/**
 * Some useful method for buses.
 * @author Mathieu Méa
 */
public class BusUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusUtils.class.getSimpleName();

	/**
	 * Return 1 R.string.<ID> for the bus line simple/main direction string.
	 * @param directionId the direction string
	 * @return the 1 R.string.<ID>
	 */
	public static int getBusLineSimpleDirection(String directionId) {
		MyLog.v(TAG, "getBusLineSimpleDirection(%s)", directionId);
		if (directionId.endsWith("N")) {
			return R.string.north;
		} else if (directionId.endsWith("S")) {
			return R.string.south;
		} else if (directionId.endsWith("E")) {
			return R.string.east;
		} else if (directionId.endsWith("O")) {
			return R.string.west;
		} else {
			MyLog.w(TAG, "INTERNAL ERROR: unknow simple direction for '%s'!", directionId);
			return 0;
		}
	}

	/**
	 * Return a list of 2 R.string.<ID> for the bus line direction string.
	 * @param directionId the direction string
	 * @return the 2 R.string.<ID>
	 */
	public static List<Integer> getBusLineDirectionStringIdFromId(String directionId) {
		MyLog.v(TAG, "getBusLineDirectionStringIdFromId(%s)", directionId);
		int directionIdLength = directionId.length();

		List<Integer> results = new ArrayList<Integer>();
		results.add(getBusLineSimpleDirection(directionId));
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
			case 21:
				if (directionId.equals("21AMS")) {
					results.add(R.string.place_du_commerce);
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
			case 48:
				if (extraDirectionInfo.equals("GC")) {
					results.add(R.string.perras_via_gouin_ozias_leduc);
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
			MyLog.w(TAG, "Unknown bus line type '%s'.", type);
			return android.R.drawable.ic_dialog_alert;
		}
	}

	/**
	 * @param type the bus line type
	 * @return the color ID matching the bus line type
	 */
	public static int getBusLineTypeBgColorFromType(String type) {
		// MyLog.v(TAG, "getBusLineTypeImgFromType(" + type + ")");
		// TODO use R.colors!!
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
			MyLog.w(TAG, "Unknown bus line type '%s'.", type);
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
			MyLog.w(TAG, "Unknown bus line type '%s'.", type);
			return R.string.error;
		}
	}

	/**
	 * Clean the bus stop place.
	 * @param uncleanStopPlace the original bus stop place
	 * @return the cleaned bus stop place
	 */
	public static String cleanBusStopPlace(String uncleanStopPlace) {
		MyLog.v(TAG, "cleanBusStopPlace(%s)", uncleanStopPlace);
		String result = uncleanStopPlace;
		// if (result.startsWith(Constant.PLACE_CHAR_DE_LA)) {
		// result = result.substring(Constant.PLACE_CHAR_DE_LA_LENGTH);
		// } else
		if (result.startsWith(Constant.PLACE_CHAR_DE)) {
			result = result.substring(Constant.PLACE_CHAR_DE_LENGTH);
		} else if (result.startsWith(Constant.PLACE_CHAR_DES)) {
			result = result.substring(Constant.PLACE_CHAR_DES_LENGTH);
		} else if (result.startsWith(Constant.PLACE_CHAR_DU)) {
			result = result.substring(Constant.PLACE_CHAR_DU_LENGTH);
		}
		if (result.startsWith(Constant.PLACE_CHAR_LA)) {
			result = result.substring(Constant.PLACE_CHAR_LA_LENGTH);
		} else if (result.startsWith(Constant.PLACE_CHAR_L)) {
			result = result.substring(Constant.PLACE_CHAR_L_LENGTH);
		}

		// if (result.contains(Constant.PLACE_CHAR_IN_DE_LA)) {
		// result = result.replace(Constant.PLACE_CHAR_IN_DE_LA, Constant.PLACE_CHAR_IN);
		// } else
		if (result.contains(Constant.PLACE_CHAR_IN_DE)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DE, Constant.PLACE_CHAR_IN);
		} else if (result.contains(Constant.PLACE_CHAR_IN_DES)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DES, Constant.PLACE_CHAR_IN);
		} else if (result.contains(Constant.PLACE_CHAR_IN_DU)) {
			result = result.replace(Constant.PLACE_CHAR_IN_DU, Constant.PLACE_CHAR_IN);
		}
		if (result.contains(Constant.PLACE_CHAR_IN_LA)) {
			result = result.replace(Constant.PLACE_CHAR_IN_LA, Constant.PLACE_CHAR_IN);
		} else if (result.contains(Constant.PLACE_CHAR_IN_L)) {
			result = result.replace(Constant.PLACE_CHAR_IN_L, Constant.PLACE_CHAR_IN);
		}

		if (result.contains(Constant.PLACE_CHAR_PARENTHESE_STATION)) {
			result = result.replace(Constant.PLACE_CHAR_PARENTHESE_STATION, Constant.PLACE_CHAR_PARENTHESE);
		}
		// TODO MORE ?
		return result;
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

}

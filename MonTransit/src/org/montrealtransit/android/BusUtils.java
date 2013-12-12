package org.montrealtransit.android;


/**
 * Some useful method for buses.
 * @author Mathieu Méa
 */
public class BusUtils {

	/**
	 * The log tag.
	 */
	public static final String TAG = BusUtils.class.getSimpleName();

	/**
	 * The cache is too old to be useful, don't display it.
	 */
	public static final int CACHE_NOT_USEFUL_IN_SEC = 60 * 60; // 60 minutes

	/**
	 * The validity of the cache (in seconds), try to refresh it (display it).
	 */
	public static final int CACHE_TOO_OLD_IN_SEC = 1 * 60; // 1 minute

	/**
	 * TODO still useful? Clean the bus stop place.
	 * @param uncleanStopPlace the original bus stop place
	 * @return the cleaned bus stop place
	 */
	public static String cleanBusStopPlace(String uncleanStopPlace) {
		// MyLog.v(TAG, "cleanBusStopPlace(%s)", uncleanStopPlace);
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
		} else if (result.startsWith(Constant.PLACE_CHAR_D)) {
			result = result.substring(Constant.PLACE_CHAR_D_LENGTH);
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
		} else if (result.contains(Constant.PLACE_CHAR_IN_D)) {
			result = result.replace(Constant.PLACE_CHAR_IN_D, Constant.PLACE_CHAR_IN);
		}

		if (result.contains(Constant.PLACE_CHAR_PARENTHESE_STATION)) {
			result = result.replace(Constant.PLACE_CHAR_PARENTHESE_STATION, Constant.PLACE_CHAR_PARENTHESE);
		}
		if (result.contains(Constant.PLACE_CHAR_PARENTHESE_STATION_BIG)) {
			result = result.replace(Constant.PLACE_CHAR_PARENTHESE_STATION_BIG, Constant.PLACE_CHAR_PARENTHESE);
		}
		// TODO MORE ?
		return result;
	}

}

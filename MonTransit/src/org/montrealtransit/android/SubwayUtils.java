package org.montrealtransit.android;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Some useful method for subways.
 * @author Mathieu Méa
 */
public class SubwayUtils {

	/**
	 * The log tag.
	 */
	public static final String TAG = SubwayUtils.class.getSimpleName();

	/**
	 * The STM map (FR).
	 */
	private static final String STM_SUBWAY_MAP_URL_FR = "http://stm.info/metro/images/plan-metro.jpg";
	/**
	 * The STM map (EN).
	 */
	private static final String STM_SUBWAY_MAP_URL_EN = "http://stm.info/English/metro/images/plan-metro.jpg";
	/**
	 * The cache is too old to be useful, don't display it.
	 */
	public static final int STATUS_NOT_USEFUL_IN_SEC = 30 * 60; // 30 minutes
	/**
	 * The validity of the current status (in seconds).
	 */
	public static final int STATUS_TOO_OLD_IN_SEC = 10 * 60; // 10 minutes

	/**
	 * Show the STM subway map.
	 * @param v the view (not used)
	 */
	public static void showSTMSubwayMap(Context context) {
		// TODO store the map on the SD card the first time and then re-open it
		// TODO add a menu to reload the map from the web site in the image viewer?
		String url;
		if (Utils.getUserLanguage().equals(Locale.FRENCH.toString())) {
			url = STM_SUBWAY_MAP_URL_FR;
		} else {
			url = STM_SUBWAY_MAP_URL_EN;
		}
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	/**
	 * Return the subway line short name string ID from the subway line number.
	 * @param subwayLineNumber the subway line number
	 * @return the subway line short name string ID
	 */
	@Deprecated
	public static int getSubwayLineNameShort(Integer number) {
		MyLog.v(TAG, "getSubwayLineNameShort(%s)", number);
		if (number == null) {
			MyLog.w(TAG, "Unknown subway line number '%s'.", number);
			return R.string.error;
		}
		switch (number) {
		case 1:
			return R.string.green_line_short;
		case 2:
			return R.string.orange_line_short;
		case 4:
			return R.string.yellow_line_short;
		case 5:
			return R.string.blue_line_short;
		default:
			MyLog.w(TAG, "Unknown subway line number '%s'.", number);
			return R.string.error;
		}
	}
}

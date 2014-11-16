package org.montrealtransit.android.provider.stmbus.schedule;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.StmBusScheduleManager;
import org.montrealtransit.android.provider.common.AbstractScheduleProvider;

import android.content.UriMatcher;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;

public class StmBusLiveScheduleProvider extends AbstractScheduleProvider {

	public static final String TAG = StmBusLiveScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.live.stmbus";

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	/**
	 * The source name
	 */
	public static final String SOURCE_NAME = "www.stm.info";

	/**
	 * The URL.
	 */
	// http://i-www.stm.info/en/lines/97/stops/52084/arrivals?direction=E&limit=5
	// d=20130702&t=1002
	public static final String URL_PART_1_BEFORE_LANG = "http://i-www.stm.info/";
	public static final String URL_PART_2_BEFORE_ROUTE_ID = "/lines/";
	public static final String URL_PART_3_BEFORE_STOP_CODE = "/stops/";
	public static final String URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE = "/arrivals?direction=";
	public static final String URL_PART_5_BEFORE_LIMIT = "&limit=";
	public static final String URL_PART_6_BEFORE_DATE = "&d=";
	public static final String URL_PART_7_BEFORE_TIME = "&t=";

	private static final String URL_DATE_FORMAT_PATTERN = "yyyyMMdd";
	private static final String URL_TIME_FORMAT_PATTERN = "HHmm";
	// NOT THREAD SAFE
	private static final SimpleDateFormat URL_DATE_FORMAT = new SimpleDateFormat(URL_DATE_FORMAT_PATTERN);
	// NOT THREAD SAFE
	private static final SimpleDateFormat URL_TIME_FORMAT = new SimpleDateFormat(URL_TIME_FORMAT_PATTERN);

	@Override
	public String getAUTHORITY() {
		return AUTHORITY;
	}

	@Override
	public void ping() {
		// remove this app icon
		// SplashScreen.removeLauncherIcon(getContext());
	}

	@Override
	public Cursor getDeparture(RouteTripStop routeTripStop, Calendar now, Cache cache, String cacheUUID) {
		try {
			final Date nowDate = now.getTime();
			final String urlDateS = URL_DATE_FORMAT.format(nowDate);
			final String urlTimeS = URL_TIME_FORMAT.format(nowDate);
			// setup error messages
			String noInternetMsg = getContext().getString(R.string.no_internet);
			String noOfflineSchedule = null;
			if (!StmBusScheduleManager.isContentProviderAvailable(getContext())) {
				if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					noOfflineSchedule = getContext().getString(R.string.no_offline_schedule_or_sd_card_not_mounted);
				} else {
					noOfflineSchedule = getContext().getString(R.string.no_offline_schedule);
				}
			}
			// setup results object
			JSONArray jMessages = new JSONArray();
			String errorMessage = null;
			List<Long> allTimestamps = new ArrayList<Long>();
			// try to load from www
			try {
				URL url = new URL(getUrlStringWithDateAndTime(routeTripStop, urlDateS, urlTimeS));
				URLConnection urlc = url.openConnection();
				MyLog.d(TAG, "URL created: '%s'", url.toString());
				HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
				switch (httpUrlConnection.getResponseCode()) {
				case HttpURLConnection.HTTP_OK:
					MyLog.d(TAG, "HttpURLConnection.HTTP_OK");
					String json = Utils.getJson(urlc);
					// MyLog.d(TAG, "json:%s", json);
					AnalyticsUtils.dispatch(getContext()); // while we are connected, send the analytics data
					final JSONObject jResponse = new JSONObject(json);
					JSONArray jResponseResults = jResponse.getJSONArray("result");
					if (jResponseResults.length() > 0) {
						Long previousTime = -1L;
						for (int i = 0; i < jResponseResults.length(); i++) {
							JSONObject jResponseResult = jResponseResults.getJSONObject(i);
							// TODO String note = jResponseResult.getString("note");
							previousTime = parseTime(urlDateS, jResponseResult.getString("time"), previousTime);
							allTimestamps.add(previousTime);
						}
						// not necessary to set previous time now, query should ask for now minus the previous duration they want to have
						JSONArray jResponseMessages = jResponse.getJSONArray("messages");
						for (int i = 0; i < jResponseMessages.length(); i++) {
							final String jResponseMessageText = jResponseMessages.getJSONObject(i).getString("text");
							if (!TextUtils.isEmpty(jResponseMessageText)) {
								jMessages.put(jResponseMessageText);
							}
						}
					} else { // IF no result DO
						// look for provider error(s)
						JSONObject jStatus = jResponse.getJSONObject("status");
						if ("Error".equalsIgnoreCase(jStatus.getString("level"))) {
							String code = jStatus.getString("code");
							MyLog.d(TAG, "%s error: %s", SOURCE_NAME, code);
							if ("NoResultsDate".equalsIgnoreCase(code)) {
								errorMessage = getContext().getString(R.string.bus_stop_no_results_date, routeTripStop.route.shortName);
							} else if ("LastStop".equalsIgnoreCase(code)) {
								errorMessage = getContext().getString(R.string.descent_only);
							}
							AnalyticsUtils.trackEvent(getContext(), AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_STOP_SOURCE_ERROR,
									routeTripStop.getUUID(), routeTripStop.stop.id);
						}
						if (TextUtils.isEmpty(errorMessage)) {
							// no information => use default error message
							errorMessage = getContext().getString(R.string.bus_stop_no_info_and_source, routeTripStop.route.shortName, SOURCE_NAME);
							AnalyticsUtils.trackEvent(getContext(), AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_STOP_SOURCE_ERROR,
									routeTripStop.getUUID(), getContext().getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
						}
					}
					break;
				// return hours;
				case HttpURLConnection.HTTP_INTERNAL_ERROR:
					errorMessage = getContext().getString(R.string.error_http_500_and_source);
				case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
					errorMessage = getContext().getString(R.string.error_http_504_and_source);
				default:
					MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
							httpUrlConnection.getResponseMessage());
					AnalyticsUtils.trackEvent(getContext(), AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_HTTP_ERROR, SOURCE_NAME,
							httpUrlConnection.getResponseCode());
					errorMessage = getContext().getString(R.string.error);
					break;
				}
			} catch (UnknownHostException uhe) {
				MyLog.w(TAG, uhe, "No Internet Connection!");
				if (!TextUtils.isEmpty(noOfflineSchedule)) {
					jMessages.put(noOfflineSchedule);
				}
				jMessages.put(noInternetMsg);
			} catch (SocketException se) {
				MyLog.w(TAG, se, "No Internet Connection!");
				if (!TextUtils.isEmpty(noOfflineSchedule)) {
					jMessages.put(noOfflineSchedule);
				}
				jMessages.put(noInternetMsg);
			} catch (Exception e) {
				MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
				errorMessage = getContext().getString(R.string.error);
			}
			// IF we had cache AND no new data DO use cache instead
			if (cache != null && allTimestamps.size() == 0) {
				// at this point, loaded cache is valid because it would have been deleted if too old before calling this provider
				try {
					return getDepartureCursor(new JSONObject(cache.getObject()));
				} catch (JSONException jsone) {
					MyLog.w(TAG, jsone, "Error while parsing JSON from cache!");
					// cache not valid, returning error from www
				}
			}
			// create JSON
			final JSONObject jResult = new JSONObject();
			jResult.put("realtime", true); // not actually real-time but close enough (better than planned schedule)
			jResult.put("source", SOURCE_NAME);
			final JSONArray jTimestamps = new JSONArray();
			// MyLog.d(TAG, "jTimestamps: %s", allTimestamps.size());
			Collections.sort(allTimestamps);
			for (Long t : allTimestamps) {
				jTimestamps.put(t);
			}
			jResult.put("timestamps", jTimestamps);
			jResult.put("messages", jMessages);
			if (!TextUtils.isEmpty(errorMessage)) {
				jResult.put("error", errorMessage);
			}
			// MyLog.d(TAG, "jResult: %s", jResult);
			// save to cache
			saveToCache(cacheUUID, jResult);
			// return result
			return getDepartureCursor(jResult);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", routeTripStop);
			return null;
		}
	}

	@Override
	public int getCACHE_MAX_VALIDITY_IN_SEC() {
		return CACHE_MAX_VALIDITY_IN_SEC;
	}

	@Override
	public int getCACHE_NOT_REFRESHED_IN_SEC() {
		return CACHE_NOT_REFRESHED_IN_SEC;
	}

	/**
	 * How long (in seconds) before the cache is too old to be useful (don't display it).
	 */
	public static final int CACHE_MAX_VALIDITY_IN_SEC = 2 * 60 * 60; // 2 hours

	/**
	 * How long (in seconds) before refreshing the cache (display it).
	 */
	public static final int CACHE_NOT_REFRESHED_IN_SEC = 5 * 60; // 5 minutes (since it's not actually real time)

	private static final String SOURCE_FORMAT_PATTERN = "HHmm";
	// NOT THREAD SAFE
	public static final SimpleDateFormat SOURCE_FORMAT = new SimpleDateFormat(SOURCE_FORMAT_PATTERN);
	// NOT THREAD SAFE
	public static final SimpleDateFormat TO_TIMESTAMP_FORMAT = new SimpleDateFormat(URL_DATE_FORMAT_PATTERN + SOURCE_FORMAT_PATTERN);

	public static final int STM_DAYS_ENDS_AT_ON_THE_NEXT_DAY = 500; // 05:00 AM
	private static final long ONE_DAY_IN_MILLIS = 1 * 24 * 60 * 60 * 1000;

	private Long parseTime(String urlDateS, String timeToParse, long previousTimestamp) {
		// MyLog.d(TAG, "parseTime(%s,%s)", urlDateS, timeToParse);
		try {
			int timeInt = Integer.valueOf(timeToParse);
			long timestamp = TO_TIMESTAMP_FORMAT.parse(urlDateS + timeToParse).getTime();
			if (timeInt <= STM_DAYS_ENDS_AT_ON_THE_NEXT_DAY || timestamp < previousTimestamp) {
				timestamp += ONE_DAY_IN_MILLIS;
			}
			return timestamp;
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s %s!", urlDateS, timeToParse);
			return null;
		}
	}

	// @SuppressWarnings("unused")
	// private static String getUrlString(RouteTripStop routeTripStop) {
	// return new StringBuilder() //
	// .append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en") // lang
	// .append(URL_PART_2_BEFORE_ROUTE_ID).append(routeTripStop.route.id) // line number
	// .append(URL_PART_3_BEFORE_STOP_CODE).append(routeTripStop.stop.id) // stop code
	// .append(URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(routeTripStop.trip.headsignValue) // line direction
	// .append(URL_PART_5_BEFORE_LIMIT).append(100) // without limit, return all schedule for the day
	// .toString();
	// }

	private static String getUrlStringWithDateAndTime(RouteTripStop routeTripStop, String urlDateS, String urlTimeS) {
		return new StringBuilder() //
				.append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en") // lang
				.append(URL_PART_2_BEFORE_ROUTE_ID).append(routeTripStop.route.id) // line number
				.append(URL_PART_3_BEFORE_STOP_CODE).append(routeTripStop.stop.id) // stop code
				.append(URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(routeTripStop.trip.headsignValue) // line direction
				.append(URL_PART_5_BEFORE_LIMIT).append(100) // without limit, return all schedule for the day
				.append(URL_PART_6_BEFORE_DATE).append(urlDateS) // date 20100602
				.append(URL_PART_7_BEFORE_TIME).append(urlTimeS) // time 2359
				.toString();
	}

	@Override
	public UriMatcher getURIMATCHER() {
		return URI_MATCHER;
	}

}

package org.montrealtransit.android.services.nextstop;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.StopTimes;
import org.montrealtransit.android.provider.StmBusScheduleManager;

import android.content.Context;
import android.os.Environment;
import android.util.SparseArray;

public class IStmInfoTask extends AbstractNextStopProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = IStmInfoTask.class.getSimpleName();

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
	public static final String URL_PART_6_BEFORE_TIME = "&t=";

	public IStmInfoTask(Context context, NextStopListener from, RouteTripStop busStop) {
		super(context, from, busStop);
	}

	@Override
	protected SparseArray<StopTimes> doInBackground(Void... params) {
		if (this.routeTripStop == null) {
			return null;
		}
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		SparseArray<StopTimes> hours = new SparseArray<StopTimes>();
		// if (!Utils.isConnectedOrConnecting(this.context)) {
		// MyLog.d(TAG, "No Internet Connection!");
		// publishProgress(this.context.getString(R.string.no_internet));
		// hours.put(this.busStop.getLineNumber(), new StopTimes(SOURCE_NAME, this.context.getString(R.string.no_internet)));
		// return hours;
		// }
		String noInternetMsg = this.context.getString(R.string.no_internet);
		String noOfflineSchedule = null;
		if (!StmBusScheduleManager.isContentProviderAvailable(this.context)) {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				noOfflineSchedule = this.context.getString(R.string.no_offline_schedule_or_sd_card_not_mounted);
			} else {
				noOfflineSchedule = this.context.getString(R.string.no_offline_schedule);
			}
		}
		try {
			publishProgress(context.getString(R.string.downloading_data_from_and_source, SOURCE_NAME));
			URL url = new URL(getUrlString());
			URLConnection urlc = url.openConnection();
			MyLog.d(TAG, "URL created: '%s'", url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String json = Utils.getJson(urlc);
				AnalyticsUtils.dispatch(context); // while we are connected, send the analytics data
				publishProgress(this.context.getResources().getString(R.string.processing_data));
				JSONObject jResponse = new JSONObject(json);
				JSONArray jResults = jResponse.getJSONArray("result");
				if (jResults.length() > 0) {
					StopTimes stopTimes = new StopTimes(SOURCE_NAME);
					for (int i = 0; i < jResults.length(); i++) {
						JSONObject jResult = jResults.getJSONObject(i);
						// TODO String note = jResult.getString("note");
						stopTimes.addSTime(formatHour(jResult.getString("time")));
					}
					stopTimes.setPreviousTime(tryToLoadPreviousHour(jResults.getJSONObject(0).getString("time"), stopTimes.getSTimes()));
					JSONArray jMessages = jResponse.getJSONArray("messages");
					if (jMessages.length() > 0) {
						stopTimes.addMessageString(jMessages.getJSONObject(0).getString("text"));
						if (jMessages.length() > 1) {
							stopTimes.addMessage2String(jMessages.getJSONObject(1).getString("text"));
						}
					}
					hours.put(this.routeTripStop.route.id, stopTimes);
				} else {
					// provider error
					JSONObject jStatus = jResponse.getJSONObject("status");
					if ("Error".equalsIgnoreCase(jStatus.getString("level"))) {
						String code = jStatus.getString("code");
						MyLog.d(TAG, "%s error: %s", SOURCE_NAME, code);
						if ("NoResultsDate".equalsIgnoreCase(code)) {
							errorMessage = this.context.getString(R.string.bus_stop_no_results_date, this.routeTripStop.route.shortName);
							publishProgress(errorMessage);
							hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, errorMessage));
						} else if ("LastStop".equalsIgnoreCase(code)) {
							errorMessage = this.context.getString(R.string.descent_only);
							publishProgress(errorMessage);
							hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, errorMessage));
						}
						AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_STOP_SOURCE_ERROR, this.routeTripStop.getUID(),
								this.routeTripStop.stop.id);
					}
					if (hours.size() == 0) {
						// no information
						errorMessage = this.context.getString(R.string.bus_stop_no_info_and_source, this.routeTripStop.route.shortName, SOURCE_NAME);
						publishProgress(errorMessage);
						hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, errorMessage));
						AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_STOP_SOURCE_ERROR, this.routeTripStop.getUID(),
								context.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
					}
				}
				return hours;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500_and_source);
			case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
				errorMessage = this.context.getString(R.string.error_http_504_and_source);
			default:
				MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_HTTP_ERROR, SOURCE_NAME,
						httpUrlConnection.getResponseCode());
				hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, errorMessage));
				return hours;
			}
		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, uhe, "No Internet Connection!");
			publishProgress(noInternetMsg);
			hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, noOfflineSchedule, noInternetMsg));
			return hours;
		} catch (SocketException se) {
			MyLog.w(TAG, se, "No Internet Connection!");
			publishProgress(noInternetMsg);
			hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, noOfflineSchedule, noInternetMsg));
			return hours;
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(errorMessage);
			hours.put(this.routeTripStop.route.id, new StopTimes(SOURCE_NAME, this.context.getString(R.string.error)));
			return hours;
		}
	}

	private String tryToLoadPreviousHour(String nextHour, List<String> sHours) {
		MyLog.v(TAG, "tryToLoadPreviousHour(nextHour)", nextHour);
		try {
			final int nextHourInt = Integer.parseInt(nextHour);
			if (nextHourInt < 100) {
				return null;
			}
			int oneHourAgo = nextHourInt - 100;
			// MyLog.d(TAG, "oneHourAgo: " + oneHourAgo);
			final String urlS = getUrlStringWithHour(oneHourAgo);
			// MyLog.d(TAG, "urlS: " + urlS);
			URL url = new URL(urlS);
			URLConnection urlc = url.openConnection();
			// MyLog.d(TAG, "URL created: '%s'", url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				JSONArray jResults = new JSONObject(Utils.getJson(urlc)).getJSONArray("result");
				if (jResults.length() > 0) {
					boolean firstCommonHourFound = false;
					for (int i = jResults.length() - 1; i >= 0; i--) {
						final String fHour = formatHour(jResults.getJSONObject(i).getString("time"));
						// MyLog.d(TAG, "fHour: " + fHour);
						if (sHours != null && sHours.contains(fHour)) {
							firstCommonHourFound = true;
							continue;
						}
						if (firstCommonHourFound && sHours != null && !sHours.contains(fHour)) {
							// MyLog.d(TAG, "fHour previous: " + fHour);
							return fHour;
						}
					}
				}
				break;
			default:
				MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
			}
		} catch (Throwable t) {
			MyLog.e(TAG, t, "Error while loading previous hour!");
		}
		return null;
	}

	private String formatHour(String time) {
		// MyLog.v(TAG, "formatHour(%s)", time);
		try {
			// if (Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString())) {
			// return FR_FORMAT.format(SOURCE_FORMAT.parse(time));
			// } else {
			// // else EN - default
			// return EN_FORMAT.format(SOURCE_FORMAT.parse(time));
			// }
			return OUTPUT_FORMAT.format(SOURCE_FORMAT.parse(time));
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s!", time);
			return time;
		}
	}

	public static final SimpleDateFormat SOURCE_FORMAT = new SimpleDateFormat("HHmm");
	public static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("HH'h'mm");

	public static final SimpleDateFormat EN_FORMAT = new SimpleDateFormat("hh:mm a");
	public static final SimpleDateFormat FR_FORMAT = new SimpleDateFormat("HH'h'mm");

	public String getUrlString() {
		return new StringBuilder() //
				.append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en") // lang
				.append(URL_PART_2_BEFORE_ROUTE_ID).append(this.routeTripStop.route.id) // line number
				.append(URL_PART_3_BEFORE_STOP_CODE).append(this.routeTripStop.stop.id) // stop code
				.append(URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(this.routeTripStop.trip.headsignValue) // line direction
				.append(URL_PART_5_BEFORE_LIMIT).append(100) // without limit, return all schedule for the day
				.toString();
	}

	public String getUrlStringWithHour(int time) {
		return new StringBuilder() //
				.append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en") // lang
				.append(URL_PART_2_BEFORE_ROUTE_ID).append(this.routeTripStop.route.id) // line number
				.append(URL_PART_3_BEFORE_STOP_CODE).append(this.routeTripStop.stop.id) // stop code
				.append(URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(this.routeTripStop.trip.headsignValue) // line direction
				.append(URL_PART_5_BEFORE_LIMIT).append(100) // without limit, return all schedule for the day
				.append(URL_PART_6_BEFORE_TIME).append(time) // time
				.toString();
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String getSourceName() {
		return SOURCE_NAME;
	}
}

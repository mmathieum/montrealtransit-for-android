package org.montrealtransit.android.services.nextstop;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore;

import android.content.Context;
import android.text.TextUtils;

/**
 * Next stop provider implementation for the http://www.stm.info/ web site.
 * @author Mathieu Méa
 */
public class StmInfoTask extends AbstractNextStopProvider {

	/**
	 * @see AbstractNextStopProvider#AbstractNextStopProvider(NextStopListener, Context)
	 */
	public StmInfoTask(NextStopListener from, Context context) {
		super(from, context);
	}

	/**
	 * The source name
	 */
	public static final String SOURCE_NAME = "www.stm.info";
	/**
	 * The URL.
	 */
	private static final String URL_PART_1_BEFORE_LANG = "http://www2.stm.info/horaires/frmResult.aspx?Langue=";
	private static final String URL_PART_2_BEFORE_BUS_STOP = "&Arret=";
	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoTask.class.getSimpleName();

	@Override
	protected Map<String, BusStopHours> doInBackground(StmStore.BusStop... busStops) {
		MyLog.v(TAG, "doInBackground()");
		Utils.logAppVersion(this.context);
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		Map<String, BusStopHours> hours = new HashMap<String, BusStopHours>();
		if (busStops == null || busStops.length == 0) {
			return null;
		}
		String stopCode = busStops[0].getCode();
		String lineNumber = busStops[0].getLineNumber();
		try {
			publishProgress(context.getString(R.string.downloading_data_from_and_source, StmInfoTask.SOURCE_NAME));
			URL url = new URL(getUrlString(stopCode));
			URLConnection urlc = url.openConnection();
			// MyLog.d(TAG, "URL created: '%s'", url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String html = Utils.getInputStreamToString(urlc.getInputStream(), "utf-8");
				AnalyticsUtils.dispatch(context); // while we are connected, send the analytics data
				publishProgress(this.context.getResources().getString(R.string.processing_data));
				// FOR each bus line DO
				for (String line : findAllBusLines(html)) {
					hours.put(line, getBusStopHoursFromString(html, line));
				}
				// IF the targeted bus line was there DO
				if (hours.keySet().contains(lineNumber)) {
					publishProgress(this.context.getResources().getString(R.string.done));
				} else {
					// no information
					errorMessage = this.context.getString(R.string.bus_stop_no_info_and_source, lineNumber, SOURCE_NAME);
					publishProgress(errorMessage);
					hours.put(lineNumber, new BusStopHours(SOURCE_NAME, errorMessage));
					AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BUS_STOP_REMOVED, busStops[0].getUID(), context
							.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
				}
				return hours;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500_and_source, this.context.getString(R.string.select_next_stop_data_source));
			default:
				MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				hours.put(lineNumber, new BusStopHours(SOURCE_NAME, errorMessage));
				return hours;
			}
		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, uhe, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			hours.put(lineNumber, new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet)));
			return hours;
		} catch (SocketException se) {
			MyLog.w(TAG, se, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			hours.put(lineNumber, new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet)));
			return hours;
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(errorMessage);
			hours.put(lineNumber, new BusStopHours(SOURCE_NAME, this.context.getString(R.string.error)));
			return hours;
		}
	}

	/**
	 * @param stopCode the bus stop code
	 * @return the URL of the bus stop page on m.stm.info
	 */
	public String getUrlString(String stopCode) {
		return new StringBuilder().append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en")
				.append(URL_PART_2_BEFORE_BUS_STOP).append(stopCode).toString();
	}

	/**
	 * The pattern to extract the bus line number from the HTML source.
	 */
	private static final Pattern PATTERN_REGEX_LINE_NUMBER = Pattern.compile("<td[^>]*>(<b[^>]*>)?([0-9]{1,3})(</b>)?</td>");

	/**
	 * @param html the HTML source
	 * @return the bus line number included in the HTML source
	 */
	private Set<String> findAllBusLines(String html) {
		MyLog.v(TAG, "findAllBusLines(%s)", html.length());
		Set<String> result = new HashSet<String>();
		Matcher matcher = PATTERN_REGEX_LINE_NUMBER.matcher(html);
		while (matcher.find()) {
			result.add(matcher.group(2));
		}
		return result;
	}

	/**
	 * The pattern for the hours.
	 */
	private static final Pattern PATTERN_REGEX_FOR_HOURS = Pattern.compile("[0-9]{1,2}[h|:][0-9]{1,2}");

	/**
	 * Extract bus stops hours + messages from HTML code.
	 * @param html the HTML code
	 * @param lineNumber the line number
	 */
	private BusStopHours getBusStopHoursFromString(String html, String lineNumber) {
		MyLog.v(TAG, "getBusStopHoursFromString(%s, %s)", html.length(), lineNumber);
		BusStopHours result = new BusStopHours(SOURCE_NAME);
		String interestingPart = getInterestingPart(html, lineNumber);
		// MyLog.d(TAG, "interestingPart:" + interestingPart);
		if (interestingPart != null) {
			// find hours
			Matcher matcher = PATTERN_REGEX_FOR_HOURS.matcher(interestingPart);
			while (matcher.find()) {
				// considering 00h00 the standard (instead of the 00:00 provided by m.stm.info in English)
				String hour = matcher.group().replaceAll(":", "h");
				// MyLog.d(TAG, "hour:" + hour);
				result.addSHour(hour);
			}
			// find main message
			String message = findMessage(interestingPart);
			if (!TextUtils.isEmpty(message)) {
				result.addMessageString(message);
			}
			// find onClick message
			Map<String, List<String>> onClickMessages = findOnClickMessages(interestingPart);
			if (onClickMessages.size() > 0) {
				if (onClickMessages.size() == 1 && onClickMessages.values().iterator().next().size() == result.getSHours().size()) {
					// only one message concerning all bus stops
					result.addMessage2String(onClickMessages.keySet().iterator().next());
				} else {
					String msg = "";
					for (String onClickMessage : onClickMessages.keySet()) {
						List<String> hours = onClickMessages.get(onClickMessage);
						if (hours != null & hours.size() > 0) {
							String concernedBusStops = "";
							for (String hour : hours) {
								if (concernedBusStops.length() > 0) {
									concernedBusStops += " ";
								}
								concernedBusStops += hour;
							}
							msg = context.getString(R.string.next_bus_stops_note, concernedBusStops, onClickMessage);
						} else {
							msg = onClickMessage;
						}
						if (result.getMessage().length() <= 0) {
							result.addMessageString(msg);
						} else {
							result.addMessage2String(msg);
						}
					}
				}
			}
		} else {
			MyLog.w(TAG, "Can't find the next bus stops for line %s!", lineNumber);
			// try to find error API
			String errorAPIMsg = getErrorAPIMsg(html);
			if (!TextUtils.isEmpty(errorAPIMsg)) {
				result.setError(errorAPIMsg);
			}
		}
		// MyLog.d(TAG, "result:" + result.serialized());
		return result;
	}

	/**
	 * The pattern used to extract API error message from HTML code.
	 */
	private static final Pattern PATTERN_REGEX_ERROR_API = Pattern
			.compile("<div id=\"panErreurApi\">[\\s]*<span id=\"lblErreurApi\"[^>]*>(<b[^>]*>)?(<font[^>]*>)?(([^<])*)(</font>)?(</b>)?</span>");

	/**
	 * Extract API error message from HTML code.
	 * @param html the HTML code
	 * @return the API error message or <b>NULL</b>
	 */
	private String getErrorAPIMsg(String html) {
		MyLog.v(TAG, "getErrorAPIMsg(%s)", html.length());
		String result = null;
		Matcher matcher = PATTERN_REGEX_ERROR_API.matcher(html);
		if (matcher.find()) {
			result = matcher.group(3);
		}
		return result;
	}

	/**
	 * The pattern used to extract stops message.
	 */
	private static final Pattern PATTERN_REGEX_ONCLICK_MESSAGE = Pattern.compile("<a href\\=javascript\\:void\\(0\\) onclick=\""
			+ "AfficherMessage\\('(([^'])*)'\\)" + "\">(([^<])*)</a>"); // "AfficherMessage\\('(([^'])*)'\\)");

	/**
	 * Extract stops message from interesting part of HTML code.
	 * @param interestingPart the interesting part of HTML code
	 * @return a map of message and related stops
	 */
	private Map<String, List<String>> findOnClickMessages(String interestingPart) {
		MyLog.v(TAG, "findOnClickMessage(%s)", interestingPart.length());
		Map<String, List<String>> res = new HashMap<String, List<String>>();
		Matcher matcher = PATTERN_REGEX_ONCLICK_MESSAGE.matcher(interestingPart);
		String message;
		String hour;
		while (matcher.find()) {
			message = matcher.group(1);
			hour = Utils.formatHours(context, matcher.group(3).replaceAll(":", "h"));
			if (!res.containsKey(message)) {
				res.put(message, new ArrayList<String>());
			}
			res.get(message).add(hour);
		}
		return res;
	}

	/**
	 * The pattern used for stop global message.
	 */
	private static final Pattern PATTERN_REGEX_NOTE_MESSAGE = Pattern.compile("<tr>[\\s]*" + "<td[^>]*>(<IMG[^>]*>)?[^<]*</td>" + "<td[^>]*>(<b[^>]*>)?[^<]*"
			+ "(</b>)?</td>" + "<td[^>]*>(<b[^>]*>)?[^<]*(</b>)?</td>" + "<td[^>]*>(([^<])*)</td>[\\s]*</tr>");

	/**
	 * Find the global bus stop in the interesting part of HTML code.
	 * @param interestingPart the interesting part of HTML code
	 * @return the message or <b>NULL</b>
	 */
	private String findMessage(String interestingPart) {
		MyLog.v(TAG, "findMessage(%s)", interestingPart.length());
		String result = null;
		Matcher matcher = PATTERN_REGEX_NOTE_MESSAGE.matcher(interestingPart);
		if (matcher.find()) {
			result = matcher.group(6);
		}
		return result;
	}

	/**
	 * Find the interesting part of HTML code related to a specific bus line number.
	 * @param html the HTML code
	 * @param lineNumber the bus line number
	 * @return the interesting part of HTML code
	 */
	private String getInterestingPart(String html, String lineNumber) {
		MyLog.v(TAG, "getInterestingPart(%s, %s)", html.length(), lineNumber);
		String result = null;
		String regex = "<tr>[\\s]*" + "<td[^>]*>(<IMG[^>]*>)?[^<]*</td>" + "<td[^>]*>(<b[^>]*>)?" + lineNumber + "(</b>)?</td>"
				+ "<td[^>]*>(<b[^>]*>)?[^<]*(</b>)?</td>" + "(" + "<td[^>]*>[<a[^>]*>]?[^<]*[</a>]?[^<]*</td>" + "){0,6}[\\s]*</tr>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	@Override
	public String getTag() {
		return TAG;
	}
}

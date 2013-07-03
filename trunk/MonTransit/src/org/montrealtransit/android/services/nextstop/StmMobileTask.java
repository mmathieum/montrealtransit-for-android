package org.montrealtransit.android.services.nextstop;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
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
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.content.Context;
import android.text.TextUtils;

/**
 * This task retrieve next bus stop from the http://m.stm.info/ web site.
 * @author Mathieu Méa
 * @deprecated completely broken by completely new web site design.
 */
@Deprecated
public class StmMobileTask extends AbstractNextStopProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmMobileTask.class.getSimpleName();

	/**
	 * @see AbstractNextStopProvider#AbstractNextStopProvider(NextStopListener, Context)
	 */
	public StmMobileTask(Context context, NextStopListener from, BusStop busStop) {
		super(context, from, busStop);
	}

	/**
	 * The source name.
	 */
	public static final String SOURCE_NAME = "m.stm.info";
	/**
	 * The URL part 1 with the bus stop code
	 */
	private static final String URL_PART_1_BEFORE_STOP_CODE = "http://m.stm.info/bus/arret/";

	/**
	 * The URL part 2 with the UI language.
	 */
	private static final String URL_PART_2_BEFORE_LANG = "?lang=";

	@Override
	protected Map<String, BusStopHours> doInBackground(Void... params) {
		MyLog.v(TAG, "doInBackground()");
		Map<String, BusStopHours> hours = new HashMap<String, BusStopHours>();
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		if (this.busStop == null) {
			return null; // TODO return error message?
		}
		String stopCode = this.busStop.getCode();
		String lineNumber = this.busStop.getLineNumber();
		String urlString = getUrlString(stopCode);
		try {
			publishProgress(context.getString(R.string.downloading_data_from_and_source, SOURCE_NAME));
			URL url = new URL(urlString);
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
					// try to find a generic error message
					Pair<String, String> errors = findServiceNotAvailable(html);
					if (errors != null && !TextUtils.isEmpty(errors.first)) {
						publishProgress(errors.first);
						BusStopHours hour = new BusStopHours(SOURCE_NAME);
						if (!TextUtils.isEmpty(errors.second)) {
							hour.addMessage2String(errors.first);
							hour.addMessageString(errors.second);
						} else {
							hour.addMessageString(errors.first);
						}
						hours.put(lineNumber, hour);
					} else {
						// no info on m.stm.info about this bus stop
						errorMessage = this.context.getString(R.string.bus_stop_no_info_and_source, lineNumber, SOURCE_NAME);
						publishProgress(errorMessage);
						hours.put(lineNumber, new BusStopHours(SOURCE_NAME, errorMessage));
						AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BUS_STOP_REMOVED, this.busStop.getUID(),
								context.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
					}
					AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BUS_STOP_NO_INFO, this.busStop.getUID(), context
							.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
				}
				return hours;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500_and_source, this.context.getString(R.string.select_next_stop_data_source));
			default:
				MyLog.w(TAG, "Error: HTTP URL-Connection Response Code:%s (Message: %s)", httpUrlConnection.getResponseCode(),
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
			publishProgress(this.context.getString(R.string.error));
			hours.put(lineNumber, new BusStopHours(SOURCE_NAME, this.context.getString(R.string.error)));
			return hours;
		}
	}

	/**
	 * The pattern to extract the bus line number(s) from the HTML source.
	 */
	private static final Pattern PATTERN_REGEX_LINE_NUMBER = Pattern.compile("<p class=\"route-desc\">[\\s]*"
			+ "<a href=\"/bus/arrets/(([0-9]{1,3}))\" class=\"stm-link\">");

	/**
	 * @param html the HTML source
	 * @return the bus line number included in the HTML source
	 */
	private Set<String> findAllBusLines(String html) {
		MyLog.v(TAG, "findAllBusLines(%s)", html.length());
		Set<String> result = new HashSet<String>();
		Matcher matcher = PATTERN_REGEX_LINE_NUMBER.matcher(html);
		while (matcher.find()) {
			result.add(matcher.group(1));
		}
		return result;
	}

	/**
	 * The pattern to extract the bus line number from the HTML source.
	 */
	private static final Pattern PATTERN_REGEX_SERVICE_NOT_AVAILABLE = Pattern.compile("<div id=\"main\">[\\s]*" + "<h2 id=\"main-title\">([^<]*)</h2>[\\s]*"
			+ "<p>([^<]*)</p>[\\s]*" + "</div>");

	/**
	 * @param html the HTML source
	 * @return the bus line number included in the HTML source
	 */
	private Pair<String, String> findServiceNotAvailable(String html) {
		MyLog.v(TAG, "findServiceNotAvailable(%s)", html.length());
		Pair<String, String> result = null;
		Matcher matcher = PATTERN_REGEX_SERVICE_NOT_AVAILABLE.matcher(html);
		while (matcher.find()) {
			result = new Pair<String, String>(matcher.group(1), matcher.group(2));
		}
		return result;
	}

	/**
	 * @param stopCode the bus stop code
	 * @return the URL of the bus stop page on m.stm.info
	 */
	@Deprecated
	public static String getUrlString(String stopCode) {
		return new StringBuilder().append(URL_PART_1_BEFORE_STOP_CODE).append(stopCode).append(URL_PART_2_BEFORE_LANG)
				.append(Utils.getUserLanguage().equals("fr") ? "fr" : "en").toString();
	}

	/**
	 * The pattern for the hours.
	 */
	private static final Pattern PATTERN_REGEX_FOR_HOURS = Pattern.compile("[0-9]{1,2}[h|:][0-9]{1,2}");

	/**
	 * @param html the HTML source
	 * @param lineNumber the bus line number
	 * @return the bus stop hours
	 */
	private BusStopHours getBusStopHoursFromString(String html, String lineNumber) {
		MyLog.v(TAG, "getBusStopHoursFromString(%s, %s)", html.length(), lineNumber);
		BusStopHours result = new BusStopHours(SOURCE_NAME);
		String interestingPart = getInterestingPart(html, lineNumber);
		if (interestingPart != null) {
			// find hours
			Matcher matcher = PATTERN_REGEX_FOR_HOURS.matcher(getRouteSchedule(interestingPart));
			while (matcher.find()) {
				// considering 00h00 the standard (instead of the 00:00 provided by m.stm.info in English)
				result.addSHour(matcher.group().replaceAll(":", "h"));
			}
			// find 'notes'
			String notesPart = findNotesPart(interestingPart, lineNumber);
			if (!TextUtils.isEmpty(notesPart)) {
				String noteMessage = findMessage(notesPart);
				if (!TextUtils.isEmpty(noteMessage)) {
					String concernedBusStops = findNoteStops(notesPart);
					if (!TextUtils.isEmpty(concernedBusStops)) {
						result.addMessageString(this.context.getString(R.string.next_bus_stops_note, concernedBusStops, noteMessage));
					} else {
						result.addMessageString(noteMessage);
					}
				}
			}
			// find 'mips'
			String mipsPart = findMipsPart(interestingPart, lineNumber);
			if (!TextUtils.isEmpty(mipsPart)) {
				String mipsMessage = findMessage(mipsPart);
				if (!TextUtils.isEmpty(mipsMessage)) {
					result.addMessage2String(mipsMessage);
				}
			}
			// find 'div'
			String divMessage = findDivMessage(interestingPart, lineNumber);
			if (!TextUtils.isEmpty(divMessage)) {
				result.addMessage2String(divMessage);
			}
		} else {
			MyLog.w(TAG, "Can't find the next bus stops for line %s!", lineNumber);
		}
		return result;
	}

	/**
	 * The pattern for the route schedule.
	 */
	private static final Pattern PATTERN_REGEX_FOR_ROUTE_SCHEDULE = Pattern.compile("<p class=\"route-schedules\">[^<]*</p>[\\s]*");

	/**
	 * @param interestingPart the bus line part
	 * @return the bus line route schedule part
	 */
	private String getRouteSchedule(String interestingPart) {
		String result = null;
		Matcher matcher = PATTERN_REGEX_FOR_ROUTE_SCHEDULE.matcher(interestingPart);
		if (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	/**
	 * Find 'notes' part from the HTML code.
	 * @param interestingPart the HTML code
	 * @param lineNumber the concerned line number
	 * @return the 'notes' part or <b>NULL</b>
	 */
	private String findNotesPart(String interestingPart, String lineNumber) {
		MyLog.v(TAG, "findNotesPart(%s)", interestingPart.length(), lineNumber);
		String result = null;
		String regex = "<div class=\"notes\">[\\s]*" + "<div class=\"wrapper\">[\\s]*" + "(" + "<div class=\"heure\">[^d]*div>[\\s]*" + "|"
				+ "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*" + ")" + "<div class=\"message\">[^<]*</div>[\\s]*"
				+ "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(interestingPart);
		while (matcher.find()) {
			result = matcher.group(0);
		}
		return result;

	}

	/**
	 * Find the 'mips' part of the HTML code.
	 * @param interestingPart the HTML code
	 * @param lineNumber the line number
	 * @return the 'mips' part or <b>NULL</b>
	 */
	private String findMipsPart(String interestingPart, String lineNumber) {
		MyLog.v(TAG, "findMipsPart(%s)", interestingPart.length(), lineNumber);
		String result = null;
		String regex = "<div class=\"mips\">[\\s]*" + "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*"
				+ "<div class=\"message\">[^</]*</div>[\\s]*" + "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(interestingPart);
		while (matcher.find()) {
			result = matcher.group(0);
		}
		return result;
	}

	/**
	 * Find the 'div' message of the HTML code.
	 * @param interestingPart the HTML code
	 * @param lineNumber the line number
	 * @return the 'div' message or <b>NULL</b>
	 */
	private String findDivMessage(String interestingPart, String lineNumber) {
		MyLog.v(TAG, "findDivMessage(%s)", interestingPart.length(), lineNumber);
		String result = null;
		String regex = "<a href=\"/bus/arrets/" + lineNumber + "\" [^>]*" + "[^<]*</a>[^<]*" + "<div>([^<]*)</div>[\\s]*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(interestingPart);
		while (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	/**
	 * The pattern used for stops note.
	 */
	private static final Pattern PATTERN_REGEX_NOTE_MESSAGE = Pattern.compile("<div class=\"message\">(([^<])*)</div>");

	/**
	 * Find the message
	 * @param interestingPart the HTML code where the message is
	 * @return the message or <b>NULL</b>
	 */
	private String findMessage(String interestingPart) {
		MyLog.v(TAG, "findMessage(%s)", interestingPart.length());
		String result = null;
		Matcher matcher = PATTERN_REGEX_NOTE_MESSAGE.matcher(interestingPart);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	/**
	 * Find the stops concerned by the note message.
	 * @param interestingPart the HTML code where note is supposed to be
	 * @return the concerned stop or <B>NULL</b>
	 */
	private String findNoteStops(String interestingPart) {
		MyLog.v(TAG, "findNoteStops(%s)", interestingPart.length());
		String result = null;
		String noteHourPart = findNoteHourPart(interestingPart);
		if (!TextUtils.isEmpty(noteHourPart)) {
			Matcher matcher = PATTERN_REGEX_FOR_HOURS.matcher(noteHourPart);
			while (matcher.find()) {
				if (result == null) {
					result = "";
				} else { // if (result.length() > 0) {
					result += " ";
				}
				result += Utils.formatHours(context, matcher.group().replaceAll(":", "h"));
			}
		}
		return result;

	}

	/**
	 * The pattern used for stops note.
	 */
	private static final Pattern PATTERN_REGEX_HOUR_PART = Pattern.compile("<div class=\"heure\">[^\bdiv\b]*");

	/**
	 * Find the part of the note containing the stops hours.
	 * @param all the HTML code supposed to contains the stops hours
	 * @return the part of the HTML code containing the stops hours or <B>NULL</B>
	 */
	private String findNoteHourPart(String all) {
		MyLog.v(TAG, "findNoteHourPart(%s)", all.length());
		String result = null;
		Matcher matcher = PATTERN_REGEX_HOUR_PART.matcher(all);
		while (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	/**
	 * @param html the HTML source
	 * @param lineNumber the bus line number
	 * @return the interesting part of the HTML source matching the bus line number
	 */
	private String getInterestingPart(String html, String lineNumber) {
		MyLog.v(TAG, "getInterestingPart(%s, %s)", html.length(), lineNumber);
		String result = null;
		String regex = "<div class=\"route\">[\\s]*" + "<p class=\"route-desc\">[\\s]*" + "<a href=\"/bus/arrets/" + lineNumber + "\" [^>]*[^<]*</a>[^<]*"
				+ "(<div>[^<]*</div>[^<]*)?" + "</p>[^<]*" + "<p class=\"route-schedules\">[^<]*</p>[\\s]*" + "(" + "<div class=\"notes\">[\\s]*"
				+ "<div class=\"wrapper\">[\\s]*" + "<div class=\"heure\">[^d]*div>[\\s]*" + "<div class=\"message\">[^<]*</div>[\\s]*"
				+ "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "(" + "<div class=\"notes\">[\\s]*"
				+ "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*" + "<div class=\"message\">[^<]*</div>[\\s]*"
				+ "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "(" + "<div class=\"mips\">[\\s]*"
				+ "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*" + "<div class=\"message\">[^</]*</div>[\\s]*"
				+ "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "</div>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		while (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	@Override
	public String getTag() {
		return TAG;
	}
}

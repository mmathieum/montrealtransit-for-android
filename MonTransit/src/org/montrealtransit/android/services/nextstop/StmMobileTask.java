package org.montrealtransit.android.services.nextstop;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore;

import android.content.Context;
import android.text.TextUtils;

/**
 * This task retrieve next bus stop from the m.stm.info web site.
 * @author Mathieu Méa
 */
public class StmMobileTask extends AbstractNextStopProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmMobileTask.class.getSimpleName();

	/**
	 * @see AbstractNextStopProvider#AbstractNextStopProvider(NextStopListener, Context)
	 */
	public StmMobileTask(NextStopListener from, Context context) {
		super(from, context);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BusStopHours doInBackground(StmStore.BusStop... stopInfo) {
		String stopCode = stopInfo[0].getCode();
		String lineNumber = stopInfo[0].getLineNumber();
		String URLString = getUrlString(stopCode);
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		try {
			publishProgress(context.getString(R.string.downloading_data_from_and_source, StmMobileTask.SOURCE_NAME));
			URL url = new URL(URLString);
			URLConnection urlc = url.openConnection();
			MyLog.v(TAG, "URL created: '%s'", url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String html = Utils.getInputStreamToString(urlc.getInputStream(), "utf-8");
				publishProgress(this.context.getResources().getString(R.string.processing_data));
				BusStopHours hours = getBusStopHoursFromString(html, lineNumber);
				publishProgress(this.context.getResources().getString(R.string.done));
				return hours;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500_and_source,
				        this.context.getString(R.string.select_next_stop_data_source));
			default:
				MyLog.w(TAG, "Error: HTTP URL-Connection Response Code:%s (Message: %s)",
				        httpUrlConnection.getResponseCode(), httpUrlConnection.getResponseMessage());
				return new BusStopHours(SOURCE_NAME, errorMessage);
			}
		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, "No Internet Connection!", uhe);
			publishProgress(this.context.getString(R.string.no_internet));
			return new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet));
		} catch (SocketException se) {
			MyLog.w(TAG, "No Internet Connection!", se);
			publishProgress(this.context.getString(R.string.no_internet));
			return new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet));
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
			publishProgress(this.context.getString(R.string.error));
			return new BusStopHours(SOURCE_NAME, this.context.getString(R.string.error));
		}
	}

	/**
	 * @param stopCode the bus stop code
	 * @return the URL of the bus stop page on m.stm.info
	 */
	public static String getUrlString(String stopCode) {
		String result = URL_PART_1_BEFORE_STOP_CODE + stopCode;
		if (Utils.getUserLocale().equals("fr")) {
			result += URL_PART_2_BEFORE_LANG + "fr";
		} else {
			result += URL_PART_2_BEFORE_LANG + "en";
		}
		return result;
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
			Matcher matcher = PATTERN_REGEX_FOR_HOURS.matcher(interestingPart);
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
						result.addMessageString(this.context.getString(R.string.next_bus_stops_note, concernedBusStops,
						        noteMessage));
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
		} else {
			MyLog.w(TAG, "Can't find the next bus stops for line %s!", lineNumber);
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
		String regex = "<div class=\"notes\">[\\s]*" + "<div class=\"wrapper\">[\\s]*" + "("
		        + "<div class=\"heure\">[^d]*div>[\\s]*" + "|" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*"
		        + ")" + "<div class=\"message\">[^<]*</div>[\\s]*" + "<div class=\"clearfloat\">[^<]*</div>[\\s]*"
		        + "</div>[\\s]*" + "</div>[\\s]*";
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
		String regex = "<div class=\"mips\">[\\s]*" + "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">"
		        + lineNumber + "</div>[\\s]*" + "<div class=\"message\">[^</]*</div>[\\s]*"
		        + "<div class=\"clearfloat\">[^<]*</div>[\\s]*" + "</div>[\\s]*" + "</div>[\\s]*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(interestingPart);
		while (matcher.find()) {
			result = matcher.group(0);
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
		while (matcher.find()) {
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
		String regex = "<div class=\"route\">[\\s]*" + "<p class=\"route-desc\">[\\s]*" + "<a href=\"/bus/arrets/"
		        + lineNumber + "\" class=\"stm-link\">[^</]*</a>[^<]*" + "</p>[^<]*"
		        + "<p class=\"route-schedules\">[^<]*</p>[\\s]*" + "(" + "<div class=\"notes\">[\\s]*"
		        + "<div class=\"wrapper\">[\\s]*" + "<div class=\"heure\">[^d]*div>[\\s]*"
		        + "<div class=\"message\">[^<]*</div>[\\s]*" + "<div class=\"clearfloat\">[^<]*</div>[\\s]*"
		        + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "(" + "<div class=\"notes\">[\\s]*"
		        + "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*"
		        + "<div class=\"message\">[^<]*</div>[\\s]*" + "<div class=\"clearfloat\">[^<]*</div>[\\s]*"
		        + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "(" + "<div class=\"mips\">[\\s]*"
		        + "<div class=\"wrapper\">[\\s]*" + "<div class=\"ligne\">" + lineNumber + "</div>[\\s]*"
		        + "<div class=\"message\">[^</]*</div>[\\s]*" + "<div class=\"clearfloat\">[^<]*</div>[\\s]*"
		        + "</div>[\\s]*" + "</div>[\\s]*" + ")?" + "</div>";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);
		while (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTag() {
		return TAG;
	}
}

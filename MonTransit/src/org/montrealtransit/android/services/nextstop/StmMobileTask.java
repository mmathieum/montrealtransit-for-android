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
			MyLog.v(TAG, "URL created:" + url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String html = Utils.getInputStreamToString(urlc.getInputStream(), "utf-8");
				publishProgress(this.context.getResources().getString(R.string.processing_data));
				BusStopHours hours = getBusStopHoursFromString(html, lineNumber);
				publishProgress(this.context.getResources().getString(R.string.done));
				return hours;
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500);
			default:
				MyLog.w(TAG, "Error: HTTP URL-Connection Response Code:" + httpUrlConnection.getResponseCode()
				        + "(Message: " + httpUrlConnection.getResponseMessage() + ")");
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
		return URL_PART_1_BEFORE_STOP_CODE + stopCode;
	}

	/**
	 * The pattern for the hours.
	 */
	private static final Pattern PATTERN_REGEX_FOR_HOURS = Pattern.compile("[0-9]{1,2}h[0-9]{1,2}");

	/**
	 * @param html the HTML source
	 * @param lineNumber the bus line number
	 * @return the bus stop hours
	 */
	private BusStopHours getBusStopHoursFromString(String html, String lineNumber) {
		MyLog.v(TAG, "getBusStopHoursFromString(" + html.length() + ", " + lineNumber + ")");
		BusStopHours result = new BusStopHours(SOURCE_NAME);
		String interestingPart = getInterestingPart(html, lineNumber);
		if (interestingPart != null) {
			Matcher matcher = PATTERN_REGEX_FOR_HOURS.matcher(interestingPart);
			while (matcher.find()) {
				result.addSHour(matcher.group());
			}
		}
		return result;
	}

	/**
	 * @param html the HTML source
	 * @param lineNumber the bus line number
	 * @return the interesting part of the HTML source matching the bus line number
	 */
	private String getInterestingPart(String html, String lineNumber) {
		MyLog.v(TAG, "getInterestingPart(" + html.length() + ", " + lineNumber + ")");
		String result = null;
		String regex = "<p class=\"route-desc\">[\\s]*<a href=\"/bus/arrets/"
		        + lineNumber
		        + "\" class=\"stm-link\">[^</]*</a>[^<]*</p>[^<]*<p class=\"route-schedules\">[(((([0-9]{1,2}h[0-9]{1,2}[\\s]*]*</p>";
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

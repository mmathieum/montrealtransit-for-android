package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;

import android.content.Context;
import android.os.AsyncTask;

/**
 * This task get the STM info status from stminfo.com
 * @author Mathieu Méa
 */
public class StmInfoStatusApiReader extends AsyncTask<String, String, String> {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoStatusApiReader.class.getSimpleName();

	/**
	 * The source string.
	 */
	public static final String SOURCE = "stm.info";

	/**
	 * The stm.info XMX URL.
	 */
	private static final String URL_PART_1_BEFORE_LANG = "http://www.stm.info/";
	private static final String URL_PART_2 = "/ajax/etats-du-service";

	/**
	 * The context executing the task.
	 */
	private Context context;

	/**
	 * The class that will handle the answer.
	 */
	private WeakReference<StmInfoStatusReaderListener> from;

	/**
	 * The default constructor.
	 * @param from the class that will handle the answer
	 * @param context context executing the task
	 */
	public StmInfoStatusApiReader(StmInfoStatusReaderListener from, Context context) {
		this.from = new WeakReference<StmInfoStatusReaderListener>(from);
		this.context = context;
	}

	@Override
	protected String doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground()");
		try {
			URL url = new URL(getUrlString());
			URLConnection urlc = url.openConnection();
			// MyLog.d(TAG, "URL created: '%s'", url.toString());
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String json = Utils.getJson(urlc);
				AnalyticsUtils.dispatch(this.context); // while we are connected, send the analytics data
				publishProgress(this.context.getResources().getString(R.string.processing_data));
				JSONObject jResponse = new JSONObject(json);
				List<ServiceStatus> allServiceStatus = new ArrayList<ServiceStatus>();
				JSONObject jMetro = jResponse.getJSONObject("metro");
				JSONArray jMetroNames = jMetro.names();
				if (jMetroNames.length() > 0) {
					int now = Utils.currentTimeSec();
					for (int i = 0; i < jMetroNames.length(); i++) {
						String jMetroName = jMetroNames.getString(i);
						JSONObject jLine = jMetro.getJSONObject(jMetroName);
						JSONObject jLineData = jLine.getJSONObject("data");
						String jLineDataText = jLineData.getString("text");
						ServiceStatus serviceStatus = new ServiceStatus();
						int type = extractServiceStatus(jLineDataText);
						serviceStatus.setType(type);
						serviceStatus.setMessage(jLineDataText);
						serviceStatus.setLanguage(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? ServiceStatus.STATUS_LANG_FRENCH
								: ServiceStatus.STATUS_LANG_ENGLISH);
						// date
						serviceStatus.setReadDate(now);
						serviceStatus.setPubDate(now);
						// source name
						serviceStatus.setSourceName("stminfoetatsduservice");
						allServiceStatus.add(serviceStatus);
					}
				}
				// publishProgress(this.context.getString(R.string.done));
				// MyLog.d(TAG, "new service statuses :" + (allServiceStatus == null ? null : allServiceStatus.size()));
				// delete existing status
				DataManager.deleteAllServiceStatus(this.context.getContentResolver());
				// add new status (all language & all status type)
				for (ServiceStatus serviceStatus : allServiceStatus) {
					// MyLog.d(TAG, "new service status (" + serviceStatus.getReadDate() + "):" + serviceStatus.getMessage());
					DataManager.addServiceStatus(this.context.getContentResolver(), serviceStatus);
				}
				return null;
			default:
				MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				publishProgress(this.context.getString(R.string.error));
				return this.context.getString(R.string.error);
			}

		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, uhe, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			return this.context.getString(R.string.no_internet);
		} catch (SocketException se) {
			MyLog.w(TAG, se, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			return this.context.getString(R.string.no_internet);
		} catch (Exception e) {
			// Unknown error
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(this.context.getString(R.string.error));
			return this.context.getString(R.string.error);
		}
	}

	public String getUrlString() {
		return new StringBuilder() //
				.append(URL_PART_1_BEFORE_LANG).append(Utils.getSupportedUserLocale().equals(Locale.FRENCH.toString()) ? "fr" : "en") // lang
				.append(URL_PART_2) //
				.toString();
	}

	@Override
	protected void onPostExecute(String errorMessage) {
		MyLog.v(TAG, "onPostExecute(%s)", errorMessage);
		StmInfoStatusReaderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onStmInfoStatusesLoaded(errorMessage);
		}
		super.onPostExecute(errorMessage);
	}

	private static final String STATUS_GREEN_FR = "Service normal";
	private static final String STATUS_GREEN_EN = "Normal m";

	private static final String STATUS_YELLOW_FR = "Reprise";
	private static final String STATUS_YELLOW_EN = "Service gradually";

	private static final String STATUS_RED_FR = "Interruption de service";
	private static final String STATUS_RED_EN = "Service disrupt";

	/**
	 * Extract the service status from the Twitter status.
	 * @param statusText the Twitter status
	 * @return the service status
	 */
	public static int extractServiceStatus(String statusText) {
		// if (statusText.contains(" VE ") || statusText.contains(" VF ")) {
		// return ServiceStatus.STATUS_TYPE_GREEN;
		// } else if (statusText.contains(" JE ") || statusText.contains(" JF ")) {
		// return ServiceStatus.STATUS_TYPE_YELLOW;
		// } else if (statusText.contains(" RE ") || statusText.contains(" RF ")) {
		// return ServiceStatus.STATUS_TYPE_RED;
		// } else {
		// try keyword detection
		if (statusText.startsWith(STATUS_GREEN_EN) || statusText.startsWith(STATUS_GREEN_FR)) {
			return ServiceStatus.STATUS_TYPE_GREEN;
		} else if (statusText.startsWith(STATUS_YELLOW_EN) || statusText.startsWith(STATUS_YELLOW_FR)) {
			return ServiceStatus.STATUS_TYPE_YELLOW;
		} else if (statusText.startsWith(STATUS_RED_EN) || statusText.startsWith(STATUS_RED_FR)) {
			return ServiceStatus.STATUS_TYPE_RED;
		} else {
			return ServiceStatus.STATUS_TYPE_DEFAULT;
		}
		// }
	}

	/**
	 * Extract the message language from the Twitter status.
	 * @param statusText the Twitter status
	 * @return the message language
	 */
	@Deprecated
	public static String extractMessageLanguage(String statusText) {
		// if (statusText.contains(" VE ") || statusText.contains(" JE ") || statusText.contains(" RE ")) {
		// return ServiceStatus.STATUS_LANG_ENGLISH;
		// } else if (statusText.contains(" VF ") || statusText.contains(" JF ") || statusText.contains(" RF ")) {
		// return ServiceStatus.STATUS_LANG_FRENCH;
		// } else {
		// try keyword detection
		if (statusText.startsWith(STATUS_GREEN_EN) || statusText.startsWith(STATUS_YELLOW_EN) || statusText.startsWith(STATUS_RED_EN)) {
			return ServiceStatus.STATUS_LANG_ENGLISH;
		} else if (statusText.startsWith(STATUS_GREEN_FR) || statusText.startsWith(STATUS_YELLOW_FR) || statusText.startsWith(STATUS_RED_FR)) {
			return ServiceStatus.STATUS_LANG_FRENCH;
		} else {
			return ServiceStatus.STATUS_LANG_UNKNOWN;
		}
		// }
	}

	public static class ServiceStatusTypeComparator implements Comparator<ServiceStatus> {
		@Override
		public int compare(ServiceStatus lhs, ServiceStatus rhs) {
			return (lhs.getType() > rhs.getType() ? -1 : (lhs.getType() == rhs.getType() ? 0 : 1));
		}
	}

}

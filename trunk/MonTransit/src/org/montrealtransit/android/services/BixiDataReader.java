package org.montrealtransit.android.services;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.os.AsyncTask;

/**
 * This task retrieve the Bixi data from montreal.bixi.com.
 * @author Mathieu Méa
 */
public class BixiDataReader extends AsyncTask<String, String, List<BikeStation>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = BixiDataReader.class.getSimpleName();
	/**
	 * The source string.
	 */
	public static final String SOURCE = "montreal.bixi.com";
	/**
	 * The montreal.bixi.com XML URL.
	 */
	public static final String XML_SOURCE = "https://montreal.bixi.com/data/bikeStations.xml";

	/**
	 * The context executing the task.
	 */
	private Context context;
	/**
	 * The class that will handle the answer.
	 */
	private BixiDataReaderListener from;
	/**
	 * The new update date.
	 */
	private int newUpdate;
	/**
	 * The last update date.
	 */
	private int lastUpdate;
	/**
	 * Time to wait before actually refreshing the data (in seconds)
	 */
	private int waitFor = 0; // 0 = no wait

	/**
	 * The default constructor.
	 * @param context context executing the task
	 * @param from the class that will handle the answer
	 * @param waitFor time to wait before actually refreshing the data (in seconds)
	 */
	public BixiDataReader(Context context, BixiDataReaderListener from, int waitFor) {
		this.from = from;
		this.context = context;
		this.waitFor = waitFor;
	}

	@Override
	protected List<BikeStation> doInBackground(String... bikeStationTerminalNames) {
		MyLog.v(TAG, "doInBackground()");
		if (this.waitFor > 0) {
			MyLog.d(TAG, "Waiting for %s seconds before loading new Bixi data from server...", this.waitFor);
			try { // wait for it...
				Thread.sleep(this.waitFor * 1000);
			} catch (InterruptedException ie) {
				MyLog.d(TAG, "Error while waiting!", ie);
			}
		}
		List<BikeStation> updatedBikeStations = BixiDataReader.doInForeground(this.context, this.from, Arrays.asList(bikeStationTerminalNames), 0);
		// IF no result OR no specific bike stations to return DO
		if (updatedBikeStations == null || bikeStationTerminalNames == null || bikeStationTerminalNames.length == 0) {
			// just return all (or none!)
			return updatedBikeStations;
		}
		// ELSE filter to only returns the required bike station(s)
		List<BikeStation> result = new ArrayList<BikeStation>();
		for (BikeStation updatedBikeStation : updatedBikeStations) {
			for (String bikeStationTerminalName : bikeStationTerminalNames) {
				if (bikeStationTerminalName.equals(updatedBikeStation.getTerminalName())) {
					result.add(updatedBikeStation);
					break;
				}
			}
			if (result.size() == bikeStationTerminalNames.length) {
				break; // found all
			}
		}
		return result;
	}

	/**
	 * The maximum number of retries for loading data.
	 */
	private static final int MAX_RETRY = 1;

	/**
	 * Synchronous {@link #doInBackground(String...)} for access from another {@link AsyncTask}.
	 */
	public static List<BikeStation> doInForeground(Context context, BixiDataReaderListener from, final List<String> forceDBUpdateTerminalNames, int tried) {
		// MyLog.v(TAG, "doInForeground(%s,%s)", forceDBUpdate, Utils.getCollectionSize(forceDBUpdateTerminalNames));
		try {
			URL url = new URL(XML_SOURCE);
			URLConnection urlc = url.openConnection();
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				publishProgress(from, context.getString(R.string.downloading_data_from_and_source, BixiDataReader.SOURCE));
				AnalyticsUtils.dispatch(context); // while we are connected, send the analytics data
				// Get a SAX Parser from the SAX Parser Factory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				// Get the XML Reader of the SAX Parser we created
				XMLReader xr = sp.getXMLReader();
				// Create a new ContentHandler and apply it to the XML-Reader
				BixiBikeStationsDataHandler handler = new BixiBikeStationsDataHandler();
				xr.setContentHandler(handler);
				// MyLog.d(TAG, "Parsing data...");
				xr.parse(new InputSource(urlc.getInputStream()));
				// MyLog.d(TAG, "Parsing data... DONE");
				publishProgress(from, context.getString(R.string.processing));
				updateDatabaseAll(context, handler.getBikeStations());
				// save new last update
				UserPreferences.savePrefLcl(context, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, handler.getLastUpdate());
				if (tried > 0) { // didn't work on 1st try but worked on retry
					AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BIXI_DATA_LOADING_FAIL, "Success after X retry.",
							tried);
				}
				return handler.getBikeStations();
			default:
				MyLog.w(TAG, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpsUrlConnection.getResponseCode(),
						httpsUrlConnection.getResponseMessage());
				publishProgress(from, context.getString(R.string.error));
				AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BIXI_DATA_LOADING_FAIL,
						tried + httpsUrlConnection.getResponseMessage(), httpsUrlConnection.getResponseCode());
				if (tried < MAX_RETRY) {
					return doInForeground(context, from, forceDBUpdateTerminalNames, ++tried);
				} else {
					return null;
				}
			}
		} catch (SSLHandshakeException sslhe) {
			MyLog.w(TAG, sslhe, "SSL error!");
			publishProgress(from, context.getString(R.string.error_ssl_and_url, SOURCE));
			AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BIXI_DATA_LOADING_FAIL, tried + sslhe.getMessage(), 0);
			if (tried < MAX_RETRY) {
				return doInForeground(context, from, forceDBUpdateTerminalNames, ++tried);
			} else {
				return null;
			}
		} catch (UnknownHostException uhe) {
			if (MyLog.isLoggable(android.util.Log.DEBUG)) {
				MyLog.w(TAG, uhe, "No Internet Connection!");
			} else {
				MyLog.w(TAG, "No Internet Connection!");
			}
			publishProgress(from, context.getString(R.string.no_internet));
			return null;
		} catch (SocketException se) {
			MyLog.w(TAG, se, "No Internet Connection!");
			publishProgress(from, context.getString(R.string.no_internet));
			return null;
		} catch (Exception e) {
			// Unknown error
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(from, context.getString(R.string.error));
			AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BIXI_DATA_LOADING_FAIL, e.getMessage(), 0);
			if (tried < MAX_RETRY) {
				return doInForeground(context, from, forceDBUpdateTerminalNames, ++tried);
			} else {
				return null;
			}
		}
	}

	/**
	 * Disable SSL certificate validation. WARNING: security issue!
	 */
	public static void disableCertificateValidation() {
		// create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				// do nothing
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				// do nothing
			}
		} };
		// ignore differences between given host-name and certificate host-name
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
		}
	}

	/**
	 * Update the database.
	 * @param context the context
	 * @param newBikeStations the new bike stations to put in the database
	 * @param forceDBUpdateTerminalNames the list of bike station terminal names to be updated in the database or null
	 * @deprecated not useful anymore. Use {@link #updateDatabaseAll(Context, List, boolean)} directly.
	 */
	@Deprecated
	public static synchronized void updateDatabase(final Context context, final List<BikeStation> newBikeStations, List<String> forceDBUpdateTerminalNames) {
		// MyLog.v(TAG, "updateDatabase(%s,%s,%s)", Utils.getCollectionSize(newBikeStations), forceDBUpdate, forceDBUpdateTerminalNames);
		// IF no bike stations terminal names were provided DO
		if (Utils.getCollectionSize(forceDBUpdateTerminalNames) == 0) {
			// synchronously update all
			updateDatabaseAll(context, newBikeStations);
		} else {
			int updated = 0;
			// synchronously update these bike stations
			for (BikeStation newBikeStation : newBikeStations) {
				if (forceDBUpdateTerminalNames.contains(newBikeStation.getTerminalName())) {
					BixiManager.updateBikeStation(context.getContentResolver(), newBikeStation, newBikeStation.getTerminalName());
					updated++;
					// TODO remove updated bike station from list
				}
				if (forceDBUpdateTerminalNames.size() == updated) {
					break; // all forced updates done
				}
			}
			// asynchronously update the rest
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					updateDatabaseAll(context, newBikeStations);
					return null;
				}
			}.execute();
		}
	}

	/**
	 * Update all the database.
	 * @param context the context
	 * @param newBikeStations the new bike stations
	 */
	public static void updateDatabaseAll(Context context, List<BikeStation> newBikeStations) {
		MyLog.v(TAG, "updateDatabaseAll(%s)", Utils.getCollectionSize(newBikeStations));
		// delete all existing bike stations
		BixiManager.deleteAllBikeStations(context.getContentResolver());
		// add all new bike stations
		BixiManager.addBikeStations(context.getContentResolver(), newBikeStations);
	}

	/**
	 * Publish progress.
	 * @param from the listener
	 * @param values the progress messages
	 */
	private static void publishProgress(BixiDataReaderListener from, String... values) {
		if (from != null) {
			from.onBixiDataProgress(values[0]);
		}
	}

	@Override
	protected void onPostExecute(List<BikeStation> newBikeStations) {
		// MyLog.v(TAG, "onPostExecute(%s)", Utils.getCollectionSize(newBikeStations));
		this.from.onBixiDataLoaded(newBikeStations, (newUpdate > lastUpdate));
	}

	@Override
	protected void onProgressUpdate(String... values) {
		MyLog.v(TAG, "onProgressUpdate(%s)", values[0]);
		this.from.onBixiDataProgress(values[0]);
	}

	/**
	 * {@link BixiDataReader} listener.
	 */
	public interface BixiDataReaderListener {

		/**
		 * @param progress new progress message
		 */
		void onBixiDataProgress(String progress);

		/**
		 * Task completed.
		 * @param newBikeStations new bike stations
		 * @param isNew true if new data
		 */
		void onBixiDataLoaded(List<BikeStation> newBikeStations, boolean isNew);

	}
}

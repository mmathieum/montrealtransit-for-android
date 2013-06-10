package org.montrealtransit.android.services;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

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
	public static final String XML_SOURCE = "http://www.stm.info/alertesmetro/esm.xml";

	/**
	 * The context executing the task.
	 */
	private Context context;

	/**
	 * The class that will handle the answer.
	 */
	private StmInfoStatusReaderListener from;

	/**
	 * The default constructor.
	 * @param from the class that will handle the answer
	 * @param context context executing the task
	 */
	public StmInfoStatusApiReader(StmInfoStatusReaderListener from, Context context) {
		this.from = from;
		this.context = context;
	}

	@Override
	protected String doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground()");
		try {
			URL url = new URL(XML_SOURCE);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				AnalyticsUtils.dispatch(this.context); // while we are connected, send the analytics data
				// Get a SAX Parser from the SAX Parser Factory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				// Get the XML Reader of the SAX Parser we created
				XMLReader xr = sp.getXMLReader();
				// Create a new ContentHandler and apply it to the XML-Reader
				StmInfoStatusApiHandler handler = new StmInfoStatusApiHandler();
				xr.setContentHandler(handler);
				// MyLog.d(TAG, "Parsing data ...");
				xr.parse(new InputSource(urlc.getInputStream()));
				// MyLog.d(TAG, "Parsing data... DONE");
				publishProgress(this.context.getString(R.string.done));
				List<ServiceStatus> allServiceStatus = handler.getServiceStatus();
				// delete existing status
				DataManager.deleteAllServiceStatus(this.context.getContentResolver());
				// add new status (all language & all status type)
				for (ServiceStatus serviceStatus : allServiceStatus) {
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

	@Override
	protected void onPostExecute(String errorMessage) {
		MyLog.v(TAG, "onPostExecute(%s)", errorMessage);
		this.from.onStmInfoStatusesLoaded(errorMessage);
		super.onPostExecute(errorMessage);
	}

	private static final String STATUS_GREEN_FR = "Service normal";
	private static final String STATUS_GREEN_EN = "Normal m";

	private static final String STATUS_YELLOW_FR = "Reprise";
	private static final String STATUS_YELLOW_EN = "Service gradually";

	private static final String STATUS_RED_FR = "Arrêt";
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

	/**
	 * XML Handler.
	 */
	private class StmInfoStatusApiHandler extends DefaultHandler implements ContentHandler {

		/**
		 * <Root> XML tag.
		 */
		private static final String ROOT = "Root";
		/**
		 * <msgFrancais> XML tag.
		 */
		private static final String MSGFRANCAIS = "msgFrancais";
		/**
		 * <msgAnglais> XML tag.
		 */
		private static final String MSGANGLAIS = "msgAnglais";

		/**
		 * The current element name.
		 */
		private String currentLocalName = ROOT;
		/**
		 * True if in {@link #MSGANGLAIS} or {@value #MSGFRANCAIS}.
		 */
		private boolean isMsg = false;

		/**
		 * The service statuses.
		 */
		private List<ServiceStatus> status = new ArrayList<ServiceStatus>();

		/**
		 * @return the orderer service statuses
		 */
		public List<ServiceStatus> getServiceStatus() {
			Collections.sort(status, new ServiceStatusTypeComparator());
			return status;
		}

		@Override
		public void startDocument() throws SAXException {
			// MyLog.v(TAG, "startDocument()");
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			// MyLog.v(TAG, "startDocument(%s,%s,%s)", uri, localName, qName);
			currentLocalName = localName;
			isMsg = localName.equalsIgnoreCase(MSGFRANCAIS) || localName.equalsIgnoreCase(MSGANGLAIS);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			// MyLog.v(TAG, "characters(%s)", new String(ch, start, length));
			if (isMsg) {
				String string = new String(ch, start, length).trim();
				int type = extractServiceStatus(string);
				ServiceStatus serviceStatus = new ServiceStatus();
				serviceStatus.setLanguage(currentLocalName.equals(MSGFRANCAIS) ? ServiceStatus.STATUS_LANG_FRENCH : ServiceStatus.STATUS_LANG_ENGLISH);
				serviceStatus.setMessage(string);
				serviceStatus.setType(type);
				// date
				int now = Utils.currentTimeSec();
				serviceStatus.setReadDate(now);
				serviceStatus.setPubDate(now);
				// source name
				serviceStatus.setSourceName("stminfoalertesmetro");
				// IF 1st status OR more important DO
				status.add(serviceStatus);
			}
			super.characters(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// MyLog.v(TAG, "endElement(%s,%s,%s)", uri, localName, qName);
			isMsg = false;
		}

		@Override
		public void endDocument() throws SAXException {
			// MyLog.v(TAG, "endDocument()");
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			MyLog.v(TAG, "error()");
			exception.printStackTrace();
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			MyLog.v(TAG, "fatalError()");
			exception.printStackTrace();
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			MyLog.v(TAG, "warning()");
			exception.printStackTrace();
		}

	}

	public static class ServiceStatusTypeComparator implements Comparator<ServiceStatus> {
		@Override
		public int compare(ServiceStatus lhs, ServiceStatus rhs) {
			return (lhs.getType() > rhs.getType() ? -1 : (lhs.getType() == rhs.getType() ? 0 : 1));
		}
	}

}

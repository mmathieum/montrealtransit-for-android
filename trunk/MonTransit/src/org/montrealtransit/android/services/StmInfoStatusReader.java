package org.montrealtransit.android.services;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.StmInfoStatus;
import org.montrealtransit.android.data.StmInfoStatuses;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.os.AsyncTask;

/**
 * This task get the STM info status from twitter.com/stminfo
 * @author Mathieu Méa
 */
public class StmInfoStatusReader extends AsyncTask<String, String, StmInfoStatuses> {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoStatusReader.class.getSimpleName();

	/**
	 * The Twitter RSS feed.
	 */
	private static final String RSS = "http://twitter.com/statuses/user_timeline/54692326.rss";

	/**
	 * The source string.
	 */
	public static final String SOURCE = "twitter.com/stminfo";

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
	public StmInfoStatusReader(StmInfoStatusReaderListener from, Context context) {
		this.from = from;
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected StmInfoStatuses doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground()");
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		try {
			URL url = new URL(RSS);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				Utils.getInputStreamToFile(urlc.getInputStream(), this.context.openFileOutput(Constant.FILE1,
				        Context.MODE_WORLD_READABLE), "UTF-8");
				cleanHTMLCodes(this.context.openFileInput(Constant.FILE1), this.context.openFileOutput(Constant.FILE2,
				        Context.MODE_WORLD_READABLE));
				// Get a SAX Parser from the SAX PArser Factory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				// Get the XML Reader of the SAX Parser we created
				XMLReader xr = sp.getXMLReader();
				// Create a new ContentHandler and apply it to the XML-Reader
				StmInfoStatusHandler contentHandler = new StmInfoStatusHandler();
				xr.setContentHandler(contentHandler);
				InputSource inputSource = new InputSource(this.context.openFileInput(Constant.FILE2));
				xr.parse(inputSource);
				return contentHandler.getStatuses();
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500);
				break;
			case HttpURLConnection.HTTP_BAD_REQUEST:
				errorMessage = this.context.getString(R.string.error_http_400_twitter);
				//TODO read from twitter.com/stminfo HTML?
				//TODO read from another Twitter client (twidroyd?)?
				//TODO read from stm.info HTML?
				break;
			}
			MyLog.w(TAG, "Error: HTTP URL-Connection Response Code:" + httpUrlConnection.getResponseCode()
			        + "(Message: " + httpUrlConnection.getResponseMessage() + ")");
			return new StmInfoStatuses(errorMessage);
		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, "No Internet Connection!", uhe);
			publishProgress(this.context.getString(R.string.no_internet));
			return new StmInfoStatuses(this.context.getString(R.string.no_internet));
		} catch (SocketException se) {
			MyLog.w(TAG, "No Internet Connection!", se);
			publishProgress(this.context.getString(R.string.no_internet));
			return new StmInfoStatuses(this.context.getString(R.string.no_internet));
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
			publishProgress(this.context.getString(R.string.error));
			return new StmInfoStatuses(this.context.getString(R.string.error));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(StmInfoStatuses result) {
		from.onStmInfoStatusesLoaded(result);
		super.onPostExecute(result);
	}

	/**
	 * Clean the code
	 * @param is the original file
	 * @param os the cleaned file
	 */
	private void cleanHTMLCodes(FileInputStream is, FileOutputStream os) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is), 4096);
		OutputStreamWriter writer = new OutputStreamWriter(os);
		try {
			String line = reader.readLine();
			while (line != null) {
				writer.write(removeHref(line.trim()));
				line = reader.readLine();
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, "Error while removing useless code.", ioe);
		} finally {
			try {
				writer.flush();
				writer.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while closing the file.", ioe);
			}
		}
	}

	/**
	 * Remove unwanted HTML code.
	 * @param string the string the clean
	 * @return the cleaned string
	 */
	private String removeHref(String string) {
		if (string.contains(Constant.HTML_CODE_EACUTE_2)) {
			string = string.replace(Constant.HTML_CODE_EACUTE_2, "é");
		}
		if (string.contains(Constant.HTML_CODE_ECIRC_2)) {
			string = string.replace(Constant.HTML_CODE_ECIRC_2, "ê");
		}
		if (string.contains(Constant.HTML_CODE_OCIRC)) {
			string = string.replace(Constant.HTML_CODE_OCIRC, "ô");
		}
		return string;
	}

	/**
	 * Handle the processing of the STM info RSS feed (XML).
	 * @author Mathieu Méa
	 */
	private class StmInfoStatusHandler extends DefaultHandler implements ContentHandler {

		private static final String TWITTER_RSS_FEED_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss ZZZZ";

		/**
		 * The STM statuses.
		 */
		private StmInfoStatuses statuses = new StmInfoStatuses();

		/**
		 * The current status.
		 */
		private StmInfoStatus currentStatus;

		/**
		 * The RSS XML elements.
		 */
		// protected static final String RSS = "rss";
		// protected static final String CHANNEL = "channel";
		protected static final String TITLE = "title";
		// protected static final String LINK = "link";
		// protected static final String DESCRIPTION = "description";
		// protected static final String LANGUAGE = "language";
		// protected static final String TTL = "ttl";
		protected static final String ITEM = "item";
		protected static final String PUB_DATE = "pubDate";
		// protected static final String GUID = "guid";

		/**
		 * True if in 'title' element.
		 */
		private boolean inTitle = false;
		/**
		 * True if in 'pubDate' element.
		 */
		private boolean inPubDate = false;

		/**
		 * True if the message is tagged as French.
		 */
		private boolean isInFrench = false;
		/**
		 * True if the message is tagged as English.
		 */
		private boolean isInEnglish = false;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			// MyLog.v(TAG, "startElement(" + localName + ")");
			if (TITLE.equals(localName)) {
				inTitle = true;
			} else if (PUB_DATE.equals(localName)) {
				inPubDate = true;
			} else if (ITEM.equals(localName)) {
				this.currentStatus = null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String string = new String(ch, start, length).trim();
			// MyLog.v(TAG, "characters(" + string + ").");
			if (string.length() > 0) {
				if (inTitle) {
					isInFrench = string.endsWith("F");
					isInEnglish = string.endsWith("E");
					if (Utils.getUserLocale().equals("fr")) {
						if (isInFrench) {
							this.currentStatus = new StmInfoStatus(string);
						}
					} else if (isInEnglish) {
						this.currentStatus = new StmInfoStatus(string);
					}
				} else if (inPubDate) {
					if (this.currentStatus != null) {
						SimpleDateFormat formatter = new SimpleDateFormat(TWITTER_RSS_FEED_DATE_FORMAT, Locale.ENGLISH);
						try {
							Date date = formatter.parse(string);
							this.currentStatus.setDate(date);
						} catch (ParseException pe) {
							MyLog.w(TAG, "Error while parsing the date from Twitter RSS feed!", pe);
						}
					}
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			// MyLog.v(TAG, "endElement(" + localName + ")");
			if (TITLE.equals(localName)) {
				inTitle = false;
			} else if (PUB_DATE.equals(localName)) {
				inPubDate = false;
			} else if (ITEM.equals(localName)) {
				if (this.currentStatus != null) {
					this.statuses.add(this.currentStatus);
				}
			}
		}

		/**
		 * @return the statuses
		 */
		public StmInfoStatuses getStatuses() {
			return statuses;
		}

	}
}

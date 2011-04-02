package org.montrealtransit.android.services.nextstop;

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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;

/**
 * Next stop provider implementation for the http://www.stm.info web site.
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
	private static final String URL_PART_1_BEFORE_BUS_STOP = "http://www2.stm.info/horaires/frmResult.aspx?Arret=";
	private static final String URL_PART_2_BEFORE_LANG = "&Langue=";
	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoTask.class.getSimpleName();

	/**
	 * The HTML codes to remove.
	 */
	private static final CharSequence HTML_IMG_NIGHT = "<IMG alt=\"\" src=\"image/Nuit.gif\">";
	private static final CharSequence HTML_IMG_METROBUS = "<IMG alt=\"\" src=\"image/Métrobus.gif\">";
	private static final CharSequence HTML_IMG_TRAINBUS = "<IMG alt=\"\" src=\"image/Trainbus.gif\">";
	private static final CharSequence HTML_IMG_EXPRESS = "<IMG alt=\"\" src=\"image/Express.gif\">";
	private static final CharSequence HTML_IMG_HOT = "<IMG alt=\"\" src=\"image/hot.gif\">";
	private static final CharSequence HTML_IMG_RESERVED = "<IMG alt=\"\" src=\"image/voieres.gif\">";
	private static final CharSequence HTML_A = "<a";
	private static final String HTML_A_REGEX = "<a.[^>]*>";
	private static final CharSequence HTML_A_close = "</a>";

	/**
	 * Some HTML codes marks.
	 */
	private static final String TD_B_1 = "<td width=\"30\"><b>";
	private static final String TD_B_2 = "</b></td>";
	private static final String TABLE_WEB_GRILLE = "<table cellspacing=\"0\" border=\"0\" id=\"webGrille\" width=\"450\">";
	private static final String TABLE_WEB_GRILLE_MES = "<table cellspacing=\"0\" border=\"0\" id=\"webGrilleMes\" width=\"450\">";
	private static final String TABLE_WEB_GRILLE_NUIT = "<table cellspacing=\"0\" border=\"0\" id=\"webGrilleNuit\" width=\"450\">";
	private static final String TABLE_WEB_GRILLE_NUIT_MES = "<table cellspacing=\"0\" border=\"0\" id=\"webGrilleNuitMes\" width=\"450\">";
	private static final String DOCTYPE_TAG = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BusStopHours doInBackground(StmStore.BusStop... busStops) {
		String stopCode = busStops[0].getCode();
		String lineNumber = busStops[0].getLineNumber();
		Utils.logAppVersion(this.context);
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		try {
			publishProgress(context.getString(R.string.downloading_data_from_and_source, StmInfoTask.SOURCE_NAME));
			String URLString = URL_PART_1_BEFORE_BUS_STOP + stopCode;
			if (Utils.getUserLanguage().equals("fr")) {
				URLString += URL_PART_2_BEFORE_LANG + "Fr";
			} else {
				URLString += URL_PART_2_BEFORE_LANG + "En";
			}
			URL url = new URL(URLString + stopCode);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				// download the the page.
				Utils.getInputStreamToFile(urlc.getInputStream(), this.context.openFileOutput(Constant.FILE1,
				        Context.MODE_WORLD_READABLE), "iso-8859-1");
				AnalyticsUtils.dispatch(context); // while we are connected, sent the analytics data
				publishProgress(this.context.getString(R.string.processing_data));
				// remove useless code from the page
				cleanHtmlCodes(this.context.openFileInput(Constant.FILE1), this.context.openFileOutput(Constant.FILE2,
				        Context.MODE_WORLD_READABLE), lineNumber);
				// Get a SAX Parser from the SAX PArser Factory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				// Get the XML Reader of the SAX Parser we created
				XMLReader xr = sp.getXMLReader();
				// Create a new ContentHandler and apply it to the XML-Reader
				StmInfoHandler busStopHandler = new StmInfoHandler(lineNumber);
				xr.setContentHandler(busStopHandler);
				MyLog.v(TAG, "Parsing data ...");
				InputSource inputSource = new InputSource(this.context.openFileInput(Constant.FILE2));
				xr.parse(inputSource);
				MyLog.v(TAG, "Parsing data... DONE");
				publishProgress(this.context.getString(R.string.done));
				return busStopHandler.getHours();
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				errorMessage = this.context.getString(R.string.error_http_500_and_source, this.context.getString(R.string.select_next_stop_data_source));
			default:
				MyLog.w(TAG, "Error: HTTP URL-Connection Response Code:" + httpUrlConnection.getResponseCode()
				        + "(Message: " + httpUrlConnection.getResponseMessage() + ")");
				return new BusStopHours(SOURCE_NAME, errorMessage);
			}
		} catch (UnknownHostException uhe) {
			MyLog.w(TAG, uhe, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			return new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet));
		} catch (SocketException se) {
			MyLog.w(TAG, se, "No Internet Connection!");
			publishProgress(this.context.getString(R.string.no_internet));
			return new BusStopHours(SOURCE_NAME, this.context.getString(R.string.no_internet));
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(errorMessage);
			return new BusStopHours(SOURCE_NAME, errorMessage);
		}
	}

	/**
	 * Clean the HTML code.
	 * @param is the source file
	 * @param os the result file
	 * @param lineNumber the bus line number used to clean the code.
	 */
	public static void cleanHtmlCodes(FileInputStream is, FileOutputStream os, String lineNumber) {
		MyLog.v(TAG, "cleanHtmlCodes(" + lineNumber + ")");
		boolean isIn = false;
		boolean isOK = false;
		String mustInclude = StmInfoTask.TD_B_1 + lineNumber + StmInfoTask.TD_B_2;
		String startString = StmInfoTask.TABLE_WEB_GRILLE;
		String startString2 = StmInfoTask.TABLE_WEB_GRILLE_MES;
		String startString3 = StmInfoTask.TABLE_WEB_GRILLE_NUIT;
		String startString4 = StmInfoTask.TABLE_WEB_GRILLE_NUIT_MES;
		String stopString = Constant.HTML_TABLE_END;
		BufferedReader reader = new BufferedReader(new InputStreamReader(is), 4096);
		OutputStreamWriter writer = new OutputStreamWriter(os);
		try {
			writer.write(StmInfoTask.DOCTYPE_TAG);
			writer.write(Constant.NEW_LINE);
			writer.write(Constant.HTML_TAG);
			String line = reader.readLine();
			while (line != null) {
				if (line.contains(startString)) {
					isIn = true;
				}
				if (!isOK && line.contains(startString2)) {
					isIn = true;
				}
				if (!isOK && line.contains(startString3)) {
					isIn = true;
				}
				if (!isOK && line.contains(startString4)) {
					isIn = true;
				}
				if (line.contains(stopString)) {
					if (isIn) {
						writer.write(stopString);
						writer.write(Constant.NEW_LINE);
					}
					isIn = false;
				}
				if (isIn) {
					writer.write(removeHref(line.trim()));
					writer.write(Constant.NEW_LINE);
				}
				if (line.contains(mustInclude)) {
					isOK = true;
				}
				line = reader.readLine();
			}
			writer.write(Constant.HTML_TAG_END);
		} catch (IOException ioe) {
			MyLog.e(TAG, ioe, "Error while removing useless code.");
		} finally {
			try {
				writer.flush();
				writer.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, ioe, "Error while closing the file.");
			}
		}
	}

	/**
	 * Remove HTML useless codes from the string
	 * @param string the string
	 * @return the cleaned string
	 */
	public static String removeHref(String string) {
		// MyLog.v(TAG, "removeHref(" + string + ")");
		if (string.contains(Constant.HTML_CODE_SPACE)) {
			string = string.replace(Constant.HTML_CODE_SPACE, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_NIGHT)) {
			string = string.replace(StmInfoTask.HTML_IMG_NIGHT, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_METROBUS)) {
			string = string.replace(StmInfoTask.HTML_IMG_METROBUS, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_TRAINBUS)) {
			string = string.replace(StmInfoTask.HTML_IMG_TRAINBUS, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_EXPRESS)) {
			string = string.replace(StmInfoTask.HTML_IMG_EXPRESS, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_RESERVED)) {
			string = string.replace(StmInfoTask.HTML_IMG_RESERVED, " ");
		}
		if (string.contains(StmInfoTask.HTML_IMG_HOT)) {
			string = string.replace(StmInfoTask.HTML_IMG_HOT, " ");
		}
		if (string.contains(StmInfoTask.HTML_A)) {
			string = string.replaceAll(StmInfoTask.HTML_A_REGEX, "");
			// TODO use the extra information about the bus line stop ?
		}
		if (string.contains(StmInfoTask.HTML_A_close)) {
			string = string.replace(StmInfoTask.HTML_A_close, "");
			// TODO use the extra information about the bus line stop ?
		}
		return string;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTag() {
		return TAG;
	}
}

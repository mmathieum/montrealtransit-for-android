package org.montrealtransit.android.services.nextstop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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
	private static final String SOURCE_NAME = "stm.info";
	/**
	 * The URL.
	 */
	private static final String URL = "http://www2.stm.info/horaires/frmResult.aspx?Langue=En&Arret=";
	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoTask.class.getSimpleName();

	/**
	 * The HTML codes to remove.
	 */
	public static final CharSequence HTML_IMG_NIGHT = "<IMG alt=\"\" src=\"image/Nuit.gif\">";
	public static final CharSequence HTML_IMG_METROBUS = "<IMG alt=\"\" src=\"image/Métrobus.gif\">";
	public static final CharSequence HTML_IMG_TRAINBUS = "<IMG alt=\"\" src=\"image/Trainbus.gif\">";
	public static final CharSequence HTML_IMG_EXPRESS = "<IMG alt=\"\" src=\"image/Express.gif\">";
	public static final CharSequence HTML_IMG_HOT = "<IMG alt=\"\" src=\"image/hot.gif\">";
	public static final CharSequence HTML_IMG_RESERVED = "<IMG alt=\"\" src=\"image/voieres.gif\">";
	public static final CharSequence HTML_A_HREF_JS_VOID = "<a href=javascript:void(0)";
	/**
	 * Some HTML codes marks.
	 */
	public static final String TABLE_WEB_GRILLE = "<table cellspacing=\"0\" border=\"0\" id=\"webGrille\" width=\"450\">";
	public static final String TD_B_1 = "<td width=\"30\"><b>";
	public static final String TD_B_2 = "</b></td>";
	public static final String TABLE_WEB_GRILLE_NUIT = "<table cellspacing=\"0\" border=\"0\" id=\"webGrilleNuit\" width=\"450\">";
	public static final String DOCTYPE_TAG = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BusStopHours doInBackground(StmStore.BusStop... busStops) {
		String stopCode = busStops[0].getCode();
		String lineNumber = busStops[0].getLineNumber();
		Utils.logAppVersion(this.context);
		try {
			publishProgress(this.context.getResources().getString(R.string.downloading_data_from) + " " + StmInfoTask.SOURCE_NAME
			        + this.context.getResources().getString(R.string.ellipsis));
			URL url = new URL(URL + stopCode);
			// download the the page.
			Utils.getInputStreamToFile(url.openStream(), this.context.openFileOutput(FILE1, Context.MODE_WORLD_READABLE));
			publishProgress(this.context.getResources().getString(R.string.processing_data));
			// remove useless code from the page
			removeUselessCode(this.context.openFileInput(FILE1), this.context.openFileOutput(FILE2, Context.MODE_WORLD_READABLE), lineNumber);
			// remove useless HTML codes from the page
			removeHtmlCodes(this.context.openFileInput(FILE2), this.context.openFileOutput(FILE3, Context.MODE_WORLD_READABLE));
			// Get a SAX Parser from the SAX PArser Factory
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			// Get the XML Reader of the SAX Parser we created
			XMLReader xr = sp.getXMLReader();
			// Create a new ContentHandler and apply it to the XML-Reader
			BusStopHandler busStopHandler = new BusStopHandler(lineNumber);
			xr.setContentHandler(busStopHandler);
			MyLog.v(TAG, "Parsing data ...");
			InputSource inputSource = new InputSource(this.context.openFileInput(FILE3));
			xr.parse(inputSource);
			MyLog.v(TAG, "Parsing data... DONE");
			publishProgress(this.context.getResources().getString(R.string.done));
			return busStopHandler.getHours();
		} catch (MalformedURLException e) {
			MyLog.e(TAG, "INTERNAL ERROR: Malformed URL Exception", e);
		} catch (ParserConfigurationException e) {
			MyLog.e(TAG, "INTERNAL ERROR: Parser Configuration Exception", e);
		} catch (SAXException e) {
			MyLog.e(TAG, "INTERNAL ERROR: SAX Exception", e);
		} catch (IOException e) {
			MyLog.e(TAG, "INTERNAL ERROR: I/O Exception", e);
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
		}
		publishProgress(this.context.getResources().getString(R.string.error));
		return null;
	}

	/**
	 * Read the input stream and remove HTML useless code and copy it to the output stream.
	 * @param is the input steam
	 * @param os the output stream
	 */
	public static void removeHtmlCodes(InputStream is, OutputStream os) {
		MyLog.v(TAG, "removeHtmlCodes()");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
		try {
			String line = reader.readLine();
			while (line != null) {
				writer.write(removeHref(line));
				line = reader.readLine();
			}
		} catch (IOException ioe) {
			MyLog.e(TAG, "Error while removing the useless HTML code from the file.", ioe);
		} finally {
			try {
				writer.flush();
				writer.close();
				os.flush();
				os.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while finishing and closing the input/output stream files.", ioe);
			}
		}
	}

	/**
	 * Remove HTML useless codes from the string
	 * @param string the string
	 * @return the cleaned string
	 */
	public static String removeHref(String string) {
		MyLog.v(TAG, "removeHref(" + string + ")");
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
		if (string.contains(StmInfoTask.HTML_A_HREF_JS_VOID)) {
			string = string.replace(StmInfoTask.HTML_A_HREF_JS_VOID, Constant.HTML_A_1);
			// TODO use the extra information about the bus line stop ?
		}
		return string;
	}

	/**
	 * Remove useless code from the input stream and write the useful code in the output stream. Use the line number to determine the useful code.
	 * @param is the input stream
	 * @param os the output stream
	 * @param lineNumber the bus line number
	 */
	public static void removeUselessCode(FileInputStream is, FileOutputStream os, String lineNumber) {
		boolean isIn = false;
		boolean isOK = false;
		String startString = StmInfoTask.TABLE_WEB_GRILLE;
		String mustInclude = StmInfoTask.TD_B_1 + lineNumber + StmInfoTask.TD_B_2;
		String startString2 = StmInfoTask.TABLE_WEB_GRILLE_NUIT;
		String stopString = Constant.HTML_TABLE_END;
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
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
				if (isIn) {
					writer.write(line);
					writer.newLine();
				}
				if (line.contains(mustInclude)) {
					isOK = true;
				}
				if (line.contains(stopString)) {
					isIn = false;
				}
				line = reader.readLine();
			}
			writer.write("</html>");
		} catch (IOException ioe) {
			MyLog.e(TAG, "Error while removing useless code.", ioe);
		} finally {
			try {
				writer.flush();
				writer.close();
				os.flush();
				os.close();
				is.close();
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while closing the file.", ioe);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getTag() {
		return TAG;
	}
}

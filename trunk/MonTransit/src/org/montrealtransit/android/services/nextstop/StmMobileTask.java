package org.montrealtransit.android.services.nextstop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
 * <b>NOT WORKING</b> This task retrieve next bus stop from the m.stm.info web site.
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
	private static final String URL_PART_1_BEFORE_STOP_CODE = "http://m.stm.info/stm/bus/schedule?stop_code=";
	/**
	 * The URL part 2 with the bus line number.
	 */
	private static final String URL_PART_2_BEFORE_LINE_NUMBER = "&line_number=";
	/**
	 * The URL part 3 with the UI language.
	 */
	private static final String URL_PART_3_BEFORE_LANG = "&lang=";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BusStopHours doInBackground(StmStore.BusStop... stopInfo) {
		String stopCode = stopInfo[0].getCode();
		String lineNumber = stopInfo[0].getLineNumber();
		String URLString = URL_PART_1_BEFORE_STOP_CODE + stopCode;
		if (lineNumber != null && lineNumber.length() > 0) {
			URLString += URL_PART_2_BEFORE_LINE_NUMBER + lineNumber;
		}
		if (Utils.getUserLocale().equals("fr")) {
			URLString += URL_PART_3_BEFORE_LANG + "fr";
		} else {
			URLString += URL_PART_3_BEFORE_LANG + "en";
		}
		try {
			publishProgress(this.context.getResources().getString(R.string.downloading_data_from) + " "
			        + StmMobileTask.SOURCE_NAME + this.context.getResources().getString(R.string.ellipsis));
			URL url = new URL(URLString);
			// faking a real browser
			URLConnection urlc = url.openConnection();
			urlc.setRequestProperty("Accept", "application/xhtml+xml");
			urlc.setRequestProperty("Accept-Charset", "iso-8859-1");
			urlc.setRequestProperty("Accept-Encoding", "gzip");
			urlc.setRequestProperty("Accept-Language", "en-CA, en-US");
			urlc.setRequestProperty("User-Agent", "Android");
			MyLog.v(TAG, "URL created:" + url.toString());
			Utils.getInputStreamToFile(urlc.getInputStream(), this.context.openFileOutput(Constant.FILE1,
			        Context.MODE_WORLD_READABLE), "iso-8859-1");
			publishProgress(this.context.getResources().getString(R.string.processing_data));
			// remove useless code from the page
			cleanHtmlCodes(this.context.openFileInput(Constant.FILE1), this.context.openFileOutput(Constant.FILE2,
			        Context.MODE_WORLD_READABLE));
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			// MyTrace.d(TAG, "SAX parser initiate");
			// Get the XMLReader of the SAXParser we created.
			XMLReader xmlReader = parser.getXMLReader();
			// Create a new ContentHandler and apply it to the XML-Reader
			StmMobileHandler stmMobileHandler = new StmMobileHandler();
			xmlReader.setContentHandler(stmMobileHandler);
			// MyTrace.d(TAG, "content handler instantiate");
			MyLog.d(TAG, "Parsing data ...");
			InputSource inputSource = new InputSource(this.context.openFileInput(Constant.FILE2));
			xmlReader.parse(inputSource);
			MyLog.d(TAG, "Parsing data ... OK");
			publishProgress(this.context.getResources().getString(R.string.done));
			return stmMobileHandler.getHours();
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
			publishProgress(this.context.getResources().getString(R.string.error));
			return new BusStopHours(StmInfoTask.SOURCE_NAME, true);
		}
	}

	/**
	 * Clean the HTML code.
	 * @param is the source file.
	 * @param os the result file.
	 */
	private void cleanHtmlCodes(FileInputStream is, FileOutputStream os) {
		MyLog.v(TAG, "removeHtmlCodes()");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
		try {
			String line = reader.readLine();
			while (line != null) {
				writer.write(removeHref(line.trim())+ " ");
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
	 * Remove unreadable HREF code.
	 * @param string the string to clean
	 * @return the cleaned string
	 */
	private String removeHref(String string) {
		//MyLog.v(TAG, "removeHref(" + string + ")");
		if (string.contains(Constant.HTML_CODE_EACUTE)) {
			string = string.replaceAll(Constant.HTML_CODE_EACUTE, "é");
		}
		if (string.contains(Constant.HTML_CODE_ECIRC)) {
			string = string.replaceAll(Constant.HTML_CODE_ECIRC, "ê");
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

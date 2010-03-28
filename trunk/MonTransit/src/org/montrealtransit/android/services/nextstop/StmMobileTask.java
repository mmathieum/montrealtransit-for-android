package org.montrealtransit.android.services.nextstop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
		try {
			publishProgress(this.context.getResources().getString(R.string.downloading_data_from) + " "
			        + StmMobileTask.SOURCE_NAME + this.context.getResources().getString(R.string.ellipsis));
			URL url = new URL(URLString);
			// use an URLConnection to fake a real browser
			URLConnection urlc = url.openConnection();
			urlc.setRequestProperty("Accept", "application/xhtml+xml");
			urlc.setRequestProperty("Accept-Charset", "iso-8859-1");
			urlc.setRequestProperty("Accept-Encoding", "gzip");
			urlc.setRequestProperty("Accept-Language", "en-CA, en-US");
			urlc.setRequestProperty("User-Agent", "Android");
			MyLog.v(TAG, "URL created:" + url.toString());
			Utils.getInputStreamToFile(urlc.getInputStream(), this.context.openFileOutput(Constant.FILE1,
			        Context.MODE_WORLD_READABLE)/* , this.from */);
			publishProgress(this.context.getResources().getString(R.string.processing_data));
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
			InputSource inputSource = new InputSource(this.context.openFileInput(Constant.FILE1));
			xmlReader.parse(inputSource);
			MyLog.d(TAG, "Parsing data ... OK");
			publishProgress(this.context.getResources().getString(R.string.done));
			return stmMobileHandler.getHours();
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
		}
		publishProgress(this.context.getResources().getString(R.string.error));
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTag() {
		return TAG;
	}

	/**
	 * To convert the InputStream to String we use the BufferedReader.readLine() method. We iterate until the BufferedReader return null which means there's no
	 * more data to read. Each line will appended to a StringBuilder and returned as String. Taken from
	 * http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
	 */
	public static String ConvertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}

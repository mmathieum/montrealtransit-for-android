package org.montrealtransit.android.services.nextstop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
	private static final String SOURCE_NAME = "m.stm.info";
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
			/*
			 * HttpParams httpparams = new BasicHttpParams(); HttpConnectionParams.setConnectionTimeout(httpparams, 60*1000);
			 * HttpConnectionParams.setSoTimeout(httpparams, 60*1000); DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
			 * 
			 * // Make request HttpGet httpget = new HttpGet(URLString); HttpResponse response = httpclient.execute(httpget);
			 * 
			 * // Read JSON response InputStream instream = response.getEntity().getContent(); String result = convertStreamToString(instream);
			 * 
			 * MyTrace.d(TAG, "result:"+result.length());
			 */

			publishProgress(this.context.getResources().getString(R.string.downloading_data_from) + " " + StmMobileTask.SOURCE_NAME
			        + this.context.getResources().getString(R.string.ellipsis));
			URL url = new URL(URLString);
			MyLog.v(TAG, "URL created:" + url.toString());
			Utils.getInputStreamToFile(url.openStream(), this.context.openFileOutput(FILE1, Context.MODE_WORLD_READABLE)/* , this.from */);
			publishProgress(this.context.getResources().getString(R.string.processing_data));

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			// MyTrace.d(TAG, "sax parser instiate");
			// Get the XMLReader of the SAXParser we created.
			XMLReader xr = sp.getXMLReader();
			// Create a new ContentHandler and apply it to the XML-Reader
			BusStopHandler busStopHandler = new BusStopHandler(lineNumber);
			xr.setContentHandler(busStopHandler);
			// MyTrace.d(TAG, "content handler instanciate");
			MyLog.d(TAG, "Parsing data ...");
			InputSource inputSource = new InputSource(this.context.openFileInput(FILE1));
			xr.parse(inputSource);
			MyLog.d(TAG, "Parsing data ... OK");
			publishProgress(this.context.getResources().getString(R.string.done));
			return busStopHandler.getHours();

		} catch (Exception e) {
			// TODO: handle exception
		}

		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getTag() {
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

package org.montrealtransit.android.services.nextstop;

import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Abstract task for next bus stop services.
 * @author Mathieu Méa
 */
public abstract class AbstractNextStopProvider extends AsyncTask<Void, String, Map<String, BusStopHours>> {

	/**
	 * The class that will handle the response.
	 */
	protected NextStopListener from;
	/**
	 * The class asking for the info.
	 */
	protected Context context;
	/**
	 * The bus stop.
	 */
	protected BusStop busStop;

	/**
	 * Default constructor.
	 * @param context the context
	 * @param from the class asking for the info
	 */
	public AbstractNextStopProvider(Context context, NextStopListener from, BusStop busStop) {
		this.context = context;
		this.from = from;
		this.busStop = busStop;
	}

	/**
	 * @return the log tag for the implementation.
	 */
	public abstract String getTag();

	@Override
	protected void onPostExecute(Map<String, BusStopHours> results) {
		MyLog.v(getTag(), "onPostExecute()");
		if (results != null && this.from != null) {
			this.from.onNextStopsLoaded(results);
		}
	}

	@Override
	protected void onProgressUpdate(String... values) {
		MyLog.v(getTag(), "onProgressUpdate(%s)", values[0]);
		if (this.from != null) {
			this.from.onNextStopsProgress(values[0]);
		}
		super.onProgressUpdate(values);
	}
}

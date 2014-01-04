package org.montrealtransit.android.services.nextstop;

import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.StopTimes;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Abstract task for next stop services.
 * @author Mathieu Méa
 */
public abstract class AbstractNextStopProvider extends AsyncTask<Void, String, Map<String,StopTimes>> {

	/**
	 * The class that will handle the response.
	 */
	protected NextStopListener from;
	/**
	 * The class asking for the info.
	 */
	protected Context context;
	/**
	 * The stop.
	 */
	protected RouteTripStop routeTripStop;

	/**
	 * Default constructor.
	 * @param context the context
	 * @param from the class asking for the info
	 */
	public AbstractNextStopProvider(Context context, NextStopListener from, RouteTripStop routeTripStop) {
		this.context = context;
		this.from = from;
		this.routeTripStop = routeTripStop;
	}

	/**
	 * @return the log tag for the implementation.
	 */
	public abstract String getTag();

	public abstract String getSourceName();

	@Override
	protected void onPostExecute(Map<String,StopTimes> results) {
		MyLog.v(getTag(), "onPostExecute()");
		// MyLog.d(getTag(), "results null?: " + (results == null));
		// if (results == null) {
		// MyLog.v(getTag(), "onPostExecute() > no result!");
		// return;
		// }
		if (this.from != null) {
			this.from.onNextStopsLoaded(results);
		} else {
			MyLog.d(getTag(), "onPostExecute() > no listener!");
		}
	}

	@Override
	protected void onProgressUpdate(String... values) {
		MyLog.v(getTag(), "onProgressUpdate()");
		if (values.length <= 0) {
			return;
		}
		if (this.from != null) {
			this.from.onNextStopsProgress(values[0]);
		}
		super.onProgressUpdate(values);
	}
}

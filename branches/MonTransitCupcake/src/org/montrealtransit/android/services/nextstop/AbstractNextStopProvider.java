package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Abstract task for next bus stop services.
 * @author Mathieu Méa
 */
public abstract class AbstractNextStopProvider extends AsyncTask<StmStore.BusStop, String, BusStopHours> {

	/**
	 * The class asking for the info
	 */
	protected NextStopListener from;
	/**
	 * The context.
	 */
	protected Context context;

	/**
	 * Default constructor.
	 * @param from the class asking for the info
	 * @param context the context
	 */
	public AbstractNextStopProvider(NextStopListener from, Context context) {
		this.context = context;
		this.from = from;
	}

	/**
	 * @return the log tag for the implementation.
	 */
	public abstract String getTag();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(BusStopHours result) {
		MyLog.v(getTag(), "onPostExecute()");
		if (result != null) {
			this.from.onPostExectute(result);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		this.from.updateProgress(values[0]);
		MyLog.v(getTag(), "Progress: " + values[0] + ".");
	}
}

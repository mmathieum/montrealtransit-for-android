package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * Find the closest {@link RouteTripStop}.
 * @author Mathieu Méa
 */
public class ClosestRouteTripStopsFinderTask extends AsyncTask<Location, String, ClosestPOI<RouteTripStop>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestRouteTripStopsFinderTask.class.getSimpleName();

	/**
	 * The maximum number of closest stops in the list.
	 */
	private static final int MAX_CLOSEST_STOPS_LIST_SIZE = 100;
	/**
	 * The maximum number of results (0 = no limit).
	 */
	private int maxResult = MAX_CLOSEST_STOPS_LIST_SIZE;
	/**
	 * The context.
	 */
	private Context context;
	/**
	 * The class handling the result and progress.
	 */
	private WeakReference<ClosestRouteTripStopsFinderListener> from;

	private Uri contentUri;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 */
	public ClosestRouteTripStopsFinderTask(ClosestRouteTripStopsFinderListener from, Context context, Uri contentUri, int maxResult) {
		this.from = new WeakReference<ClosestRouteTripStopsFinderTask.ClosestRouteTripStopsFinderListener>(from);
		this.context = context;
		this.contentUri = contentUri;
		this.maxResult = maxResult;
	}

	@Override
	protected ClosestPOI<RouteTripStop> doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<RouteTripStop> result = null;
		// read last (not too old) location
		Location location = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (location != null) {
			publishProgress(this.context.getString(R.string.processing));
			result = new ClosestPOI<RouteTripStop>(location);
			// create a list of all stops with lines and location
			List<RouteTripStop> stopsWithOtherLines = AbstractManager.findRouteTripStopsWithLocationList(this.context, this.contentUri, location, true);
			// set stops distance
			LocationUtils.updateDistanceWithString(this.context, stopsWithOtherLines, location);
			// sort by distance
			Collections.sort(stopsWithOtherLines, new POI.POIDistanceComparator());
			if (Utils.getCollectionSize(stopsWithOtherLines) > maxResult) {
				result.setPoiList(stopsWithOtherLines.subList(0, maxResult));
			} else {
				result.setPoiList(stopsWithOtherLines);
			}
		}
		return result;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length <= 0) {
			return;
		}
		// MyLog.v(TAG, "onProgressUpdate(%s)", values[0]);
		ClosestRouteTripStopsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStopsProgress(values[0]);
		} else {
			MyLog.v(TAG, "onProgressUpdate() listener null!");
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(ClosestPOI<RouteTripStop> result) {
		MyLog.v(TAG, "onPostExecute()");
		ClosestRouteTripStopsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStopsDone(result);
		} else {
			MyLog.v(TAG, "onClosestStopsDone() listener null!");
		}
		super.onPostExecute(result);
	}

	/**
	 * Contract for handling {@link ClosestRouteTripStopsFinderTask}.
	 */
	public interface ClosestRouteTripStopsFinderListener {

		/**
		 * Called to share task execution progress
		 * @param message the progress
		 */
		void onClosestStopsProgress(String message);

		/**
		 * Call when the task is completed.
		 * @param result the result of the task
		 */
		void onClosestStopsDone(ClosestPOI<RouteTripStop> result);
	}
}
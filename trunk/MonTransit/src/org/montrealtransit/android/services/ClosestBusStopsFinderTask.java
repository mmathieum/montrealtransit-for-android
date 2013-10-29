package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.StmBusManager;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

/**
 * Find the closest bus stops.
 * @author Mathieu Méa
 */
public class ClosestBusStopsFinderTask extends AsyncTask<Location, String, ClosestPOI<RouteTripStop>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestBusStopsFinderTask.class.getSimpleName();

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
	private WeakReference<ClosestBusStopsFinderListener> from;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 */
	public ClosestBusStopsFinderTask(ClosestBusStopsFinderListener from, Context context, int maxResult) {
		this.from = new WeakReference<ClosestBusStopsFinderTask.ClosestBusStopsFinderListener>(from);
		this.context = context;
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
			// read location accuracy
			// create a list of all stops with lines and location
			List<RouteTripStop> stopsWithOtherLines = getAllStopsWithLines(location);
			Collections.sort(stopsWithOtherLines, new POI.POIDistanceComparator());
			if (Utils.getCollectionSize(stopsWithOtherLines) > maxResult) {
				result.setPoiList(stopsWithOtherLines.subList(0, maxResult));
			} else {
				result.setPoiList(stopsWithOtherLines);
			}
		}
		return result;
	}

	/**
	 * Create a list of all stops including their distance to the location and all bus lines.
	 * @param location the location
	 * @return the list of localized stops
	 */
	private List<RouteTripStop> getAllStopsWithLines(Location location) {
		MyLog.v(TAG, "getAllStopsWithLines()");
		// try the short way with location hack
		// MyLog.v(TAG, "getAllStopsWithLines() loading some data...");
		List<RouteTripStop> allBusStopsWithLoc = StmBusManager.findRouteTripStopsWithLocationList(context, location);
		// MyLog.v(TAG, "getAllStopsWithLines() loading some data... DONE (%s)", (allBusStopsWithLoc == null ? null : allBusStopsWithLoc.size()));
		if (Utils.getCollectionSize(allBusStopsWithLoc) == 0) { // if no value return
			// do it the hard long way
			// MyLog.v(TAG, "getAllStopsWithLines() loading all data...");
			allBusStopsWithLoc = StmBusManager.findRouteTripStopsList(context);
			// MyLog.v(TAG, "getAllStopsWithLines() loading all data... DONE");
		}
		// MyLog.v(TAG, "getAllStopsWithLines() filtering on UID...");
		if (allBusStopsWithLoc != null) {
			Set<String> uids = new HashSet<String>();
			ListIterator<RouteTripStop> it = allBusStopsWithLoc.listIterator();
			while (it.hasNext()) {
				RouteTripStop busStop = it.next();
				if (uids.contains(busStop.getUID())) {
					// MyLog.d(TAG, "getAllStopsWithLines() filtering on UID... (skiping %s)", busStop.getUID());
					it.remove();
					continue;
				}
				uids.add(busStop.getUID());
			}
		}
		// MyLog.v(TAG, "getAllStopsWithLines() filtering on UID... DONE");
		// MyLog.v(TAG, "getAllStopsWithLines() updating distance...");
		LocationUtils.updateDistanceWithString(context, allBusStopsWithLoc, location);
		// MyLog.v(TAG, "getAllStopsWithLines() updating distance... DONE");
		return allBusStopsWithLoc;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length <= 0) {
			return;
		}
		// MyLog.v(TAG, "onProgressUpdate(%s)", values[0]);
		ClosestBusStopsFinderListener fromWR = this.from == null ? null : this.from.get();
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
		ClosestBusStopsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStopsDone(result);
		} else {
			MyLog.v(TAG, "onClosestStopsDone() listener null!");
		}
		super.onPostExecute(result);
	}

	/**
	 * Contract for handling {@link ClosestBusStopsFinderTask}.
	 */
	public interface ClosestBusStopsFinderListener {

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
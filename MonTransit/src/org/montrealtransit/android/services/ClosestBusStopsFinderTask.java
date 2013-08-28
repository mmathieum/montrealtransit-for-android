package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.BusStopDistancesComparator;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

/**
 * Find the closest bus stops.
 * @author Mathieu Méa
 */
public class ClosestBusStopsFinderTask extends AsyncTask<Location, String, ClosestPOI<ABusStop>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestBusStopsFinderTask.class.getSimpleName();

	// /**
	// * The minimum number of closest stops in the list.
	// */
	// private static final int MIN_CLOSEST_STOPS_LIST_SIZE = 10;
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
	protected ClosestPOI<ABusStop> doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<ABusStop> result = null;
		// read last (not too old) location
		Location location = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (location != null) {
			publishProgress(this.context.getString(R.string.processing));
			result = new ClosestPOI<ABusStop>(location);
			// read location accuracy
			// create a list of all stops with lines and location
			List<ABusStop> stopsWithOtherLines = getAllStopsWithLines(location);
			Collections.sort(stopsWithOtherLines, new BusStopComparator());
			if (Utils.getCollectionSize(stopsWithOtherLines) > maxResult) {
				result.setPoiList(stopsWithOtherLines.subList(0, maxResult));
			} else {
				result.setPoiList(stopsWithOtherLines);
			}
		}
		return result;
	}

	/**
	 * Sort bus stops by distance and line number.
	 */
	private class BusStopComparator extends BusStopDistancesComparator {
		@Override
		public int compare(ABusStop lhs, ABusStop rhs) {
			// IF same bus stop code DO
			if (lhs.getCode().equals(rhs.getCode())) {
				// compare line number
				return Integer.valueOf(lhs.getLineNumber()) - Integer.valueOf(rhs.getLineNumber());
			}
			// ELSE compare distance
			return super.compare(lhs, rhs);
		}
	}

	/**
	 * Create a list of all stops including their distance to the location and all bus lines.
	 * @param location the location
	 * @return the list of localized stops
	 */
	private List<ABusStop> getAllStopsWithLines(Location location) {
		MyLog.v(TAG, "getAllStopsWithLines()");
		Map<String, ABusStop> aresult = new HashMap<String, ABusStop>();
		// try the short way with location hack
		List<BusStop> allBusStopsWithLoc = StmManager.findAllBusStopLocationList(context.getContentResolver(), location);
		// MyLog.d(TAG, "1st try: " + Utils.getCollectionSize(allBusStopsWithLoc));
		if (Utils.getCollectionSize(allBusStopsWithLoc) == 0) { // if no value return
			// do it the hard long way
			allBusStopsWithLoc = StmManager.findAllBusStopLocationList(context.getContentResolver());
		}
		if (allBusStopsWithLoc != null) {
			for (BusStop busStop : allBusStopsWithLoc) {
				if (aresult.containsKey(busStop.getUID())) {
					continue;
				}
				aresult.put(busStop.getUID(), new ABusStop(busStop)); // filters on UID
			}
		}
		LocationUtils.updateDistance(context, aresult, location);
		return new ArrayList<ABusStop>(aresult.values());
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length <= 0) {
			return;
		}
		ClosestBusStopsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStopsProgress(values[0]);
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(ClosestPOI<ABusStop> result) {
		MyLog.v(TAG, "onPostExecute()");
		ClosestBusStopsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStopsDone(result);
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
		void onClosestStopsDone(ClosestPOI<ABusStop> result);
	}
}
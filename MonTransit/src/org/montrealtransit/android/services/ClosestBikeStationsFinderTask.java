package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.services.BixiDataReader.BixiDataReaderListener;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

public class ClosestBikeStationsFinderTask extends AsyncTask<Double, String, ClosestPOI<ABikeStation>> implements BixiDataReaderListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestBikeStationsFinderTask.class.getSimpleName();
	/**
	 * True if forcing update from the web (DEBUG).
	 */
	private static final boolean FORCE_UPDATE_FROM_WEB = false;
	/**
	 * The validity of the current bike stations list (in seconds).
	 */
	private static final int BIKE_STATION_LIST_TOO_OLD_IN_SEC = 7 * 24 * 60 * 60; // 1 week
	/**
	 * Represents no limit in the number of returned result.
	 */
	public static final int NO_LIMIT = 0;

	/**
	 * The context.
	 */
	private Context context;
	/**
	 * The class handling the result and progress.
	 */
	private WeakReference<ClosestBikeStationsFinderListener> from;
	/**
	 * The last Bixi data message.
	 */
	private String lastBixiDataMessage;
	/**
	 * The maximum number of results (0 = no limit).
	 */
	private int maxResult = NO_LIMIT;
	/**
	 * True if forcing update from the web (when DB empty...)
	 */
	private boolean forceUpdateFromWeb = false;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 * @param maxResult the maximum number of result or {@link #NO_LIMIT}
	 */
	public ClosestBikeStationsFinderTask(ClosestBikeStationsFinderListener from, Context context, int maxResult, boolean forceUpdateFromWeb) {
		this.from = new WeakReference<ClosestBikeStationsFinderTask.ClosestBikeStationsFinderListener>(from);
		this.context = context;
		this.maxResult = maxResult;
		this.forceUpdateFromWeb = forceUpdateFromWeb;
	}

	@Override
	protected ClosestPOI<ABikeStation> doInBackground(Double... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<ABikeStation> result = null;
		// IF location available DO
		if (params.length == 2 && params[0] != null && params[1] != null) {
			result = new ClosestPOI<ABikeStation>();
			// IF the local cache is too old DO
			if (FORCE_UPDATE_FROM_WEB
					|| forceUpdateFromWeb
					|| Utils.currentTimeSec() >= UserPreferences.getPrefLcl(this.context, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0)
							+ BIKE_STATION_LIST_TOO_OLD_IN_SEC) {
				publishProgress(this.context.getString(R.string.downloading_data_from_and_source, BixiDataReader.SOURCE));
				// look for new data
				BixiDataReader.doInForeground(this.context, new WeakReference<BixiDataReaderListener>(this), null, 0);
			}
			publishProgress(this.context.getString(R.string.processing));
			// get the closest bike station from database or NULL
			List<BikeStation> bikeStations = getAllBikeStations(params[0], params[1]);
			if (!TextUtils.isEmpty(this.lastBixiDataMessage)) {
				result.setErrorMessage(this.lastBixiDataMessage);
			}
			if (bikeStations != null) { // bike stations
				result.setPoiList(getABikeStations(bikeStations, params[0], params[1], this.maxResult));
			} else if (TextUtils.isEmpty(this.lastBixiDataMessage)) { // no bike stations
				result.setPoiList(new ArrayList<ABikeStation>());
			}
		}
		return result;
	}

	public List<BikeStation> getAllBikeStations(double lat, double lng) {
		MyLog.v(TAG, "getAllBikeStations()");
		// try the short way with location hack
		List<BikeStation> allBikeStationsWithLoc = BixiManager.findAllBikeStationsLocationList(context.getContentResolver(), lat, lng);
		// MyLog.d(TAG, "1st try: " + Utils.getCollectionSize(allBikeStationsWithLoc));
		if (Utils.getCollectionSize(allBikeStationsWithLoc) == 0) { // if no value return
			// do it the hard long way
			allBikeStationsWithLoc = BixiManager.findAllBikeStationsList(this.context.getContentResolver(), true);
			// MyLog.d(TAG, "2nd try: " + Utils.getCollectionSize(allBikeStationsWithLoc));
		}
		return allBikeStationsWithLoc;
	}

	/**
	 * Remove not installed station
	 * @param bikeStations the bike stations list to update
	 */
	public void removeNotInstalled(List<BikeStation> bikeStations) {
		if (bikeStations != null) {
			Iterator<BikeStation> it = bikeStations.iterator();
			while (it.hasNext()) {
				BikeStation bikeStation = (BikeStation) it.next();
				if (!bikeStation.isInstalled()) {
					it.remove();
				}
			}
		}
	}

	/**
	 * Converts {@link BikeStation} list to {@link ABikeStation} list and add distance, sort by distance and set distance string.
	 * @param bikeStations the {@link BikeStation} list
	 * @param maxResult the maximum number of result or {@link #NO_LIMIT}
	 * @return the {@link ABikeStation} list
	 */
	private static List<ABikeStation> getABikeStations(List<BikeStation> bikeStations, double lat, double lng, int maxResult) {
		// MyLog.v(TAG, "getABikeStations(%s, %s)", Utils.getCollectionSize(bikeStations), currentLocation);
		List<ABikeStation> aresult = new ArrayList<ABikeStation>();
		for (BikeStation bikeStation : bikeStations) {
			ABikeStation astation = new ABikeStation(bikeStation);
			// add location => distance
			astation.setDistance(LocationUtils.distanceTo(lat, lng, bikeStation.getLat(), bikeStation.getLng()));
			aresult.add(astation);
		}
		// sort the bike stations
		sortBikeStations(aresult);
		if (maxResult > 0) {
			maxResult = aresult.size() < maxResult ? aresult.size() : maxResult; // use size if max result too big
			aresult = aresult.subList(0, maxResult);
		}
		return aresult;
	}

	/**
	 * Sort {@link ABikeStation} list by distance.
	 */
	public static void sortBikeStations(List<ABikeStation> aresult) {
		Collections.sort(aresult, new Comparator<ABikeStation>() {
			@Override
			public int compare(ABikeStation lhs, ABikeStation rhs) {
				float d1 = lhs.getDistance();
				float d2 = rhs.getDistance();
				if (d1 > d2) {
					return +1;
				} else if (d1 < d2) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}

	@Override
	public void onBixiDataProgress(String progress) {
		MyLog.v(TAG, "onBixiDataProgress(%s)", progress);
		this.lastBixiDataMessage = progress;
		// this.publishProgress(progress);
	}

	@Override
	public void onBixiDataLoaded(List<BikeStation> newBikeStations, boolean isNew) {
		// MyLog.v(TAG, "onBixiDataLoaded(%s, %s)", Utils.getCollectionSize(newBikeStations), isNew);
		// already handled
	}

	@Override
	protected void onProgressUpdate(String... values) {
		MyLog.v(TAG, "onProgressUpdate()");
		if (values.length <= 0) {
			return;
		}
		ClosestBikeStationsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestBikeStationsProgress(values[0]);
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(ClosestPOI<ABikeStation> result) {
		MyLog.v(TAG, "onPostExecute()");
		ClosestBikeStationsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestBikeStationsDone(result);
		}
		super.onPostExecute(result);
	}

	/**
	 * Contract for handling {@link ClosestBikeStationsFinderTask}.
	 */
	public interface ClosestBikeStationsFinderListener {

		/**
		 * Called to share task execution progress
		 * @param message the progress
		 */
		void onClosestBikeStationsProgress(String message);

		/**
		 * Call when the task is completed.
		 * @param result the result of the task
		 */
		void onClosestBikeStationsDone(ClosestPOI<ABikeStation> result);
	}

}

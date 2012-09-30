package org.montrealtransit.android.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
import android.location.Location;
import android.os.AsyncTask;

public class ClosestBikeStationsFinderTask extends AsyncTask<Location, String, ClosestPOI<ABikeStation>> implements BixiDataReaderListener {

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
	 * The context.
	 */
	private Context context;
	/**
	 * The class handling the result and progress.
	 */
	private ClosestBikeStationsFinderListener from;

	/**
	 * The last Bixi data message.
	 */
	private String lastBixiDataMessage;

	/**
	 * The maximum number of results (0 = no limit).
	 */
	private int maxResult = NO_LIMIT;

	/**
	 * Represents no limit in the number of returned result.
	 */
	public static final int NO_LIMIT = 0;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 * @param maxResult the maximum number of result or {@link #NO_LIMIT}
	 */
	public ClosestBikeStationsFinderTask(ClosestBikeStationsFinderListener from, Context context, int maxResult) {
		this.from = from;
		this.context = context;
		this.maxResult = maxResult;
	}

	@Override
	protected ClosestPOI<ABikeStation> doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<ABikeStation> result = null;
		// read last (not too old) location
		Location currentLocation = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (currentLocation != null) {
			result = new ClosestPOI<ABikeStation>();
			// IF the local cache is too old DO
			if (FORCE_UPDATE_FROM_WEB
					|| Utils.currentTimeSec() >= UserPreferences.getPrefLcl(this.context, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0)
							+ BIKE_STATION_LIST_TOO_OLD_IN_SEC) {
				publishProgress(this.context.getString(R.string.downloading_data_from_and_source, BixiDataReader.SOURCE));
				// look for new data
				BixiDataReader.doInForeground(this.context, this, true, null, 0);
			}
			publishProgress(this.context.getString(R.string.processing));
			// get the closest bike station from database or NULL
			List<BikeStation> bikeStations = getAllBikeStations(currentLocation);
			if (bikeStations != null) {
				result.setPoiList(getABikeStations(this.context, bikeStations, currentLocation, this.maxResult));
			} else {
				result.setErrorMessage(this.lastBixiDataMessage);
			}
		}
		return result;
	}

	public List<BikeStation> getAllBikeStations(Location currentLocation) {
		MyLog.v(TAG, "getAllBikeStations()");
		// try the short way with location hack
		List<BikeStation> allBikeStationsWithLoc = BixiManager.findAllBikeStationsLocationList(context.getContentResolver(), currentLocation, false);
		MyLog.d(TAG, "1st try: " + Utils.getCollectionSize(allBikeStationsWithLoc));
		if (Utils.getCollectionSize(allBikeStationsWithLoc) == 0) { // if no value return
			// do it the hard long way
			allBikeStationsWithLoc = BixiManager.findAllBikeStationsList(this.context.getContentResolver(), false);
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
	 * @param context the context
	 * @param bikeStations the {@link BikeStation} list
	 * @param maxResult the maximum number of result or {@link #NO_LIMIT}
	 * @return the {@link ABikeStation} list
	 */
	private static List<ABikeStation> getABikeStations(Context context, List<BikeStation> bikeStations, Location currentLocation, int maxResult) {
		// MyLog.v(TAG, "getABikeStations(%s, %s)", Utils.getCollectionSize(bikeStations), currentLocation);
		List<ABikeStation> aresult = new ArrayList<ABikeStation>();
		float accuracy = currentLocation.getAccuracy();
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		for (BikeStation bikeStation : bikeStations) {
			ABikeStation astation = new ABikeStation(bikeStation);
			// add location => distance
			astation.setDistance(currentLocation.distanceTo(bikeStation.getLocation()));
			aresult.add(astation);
		}
		// sort the bike stations
		sortBikeStations(aresult);
		if (maxResult > 0) {
			maxResult = aresult.size() < maxResult ? aresult.size() : maxResult; // use size if max result too big
			aresult = aresult.subList(0, maxResult);
		}
		// set distance string for the displayed station only
		for (ABikeStation astation : aresult) {
			astation.setDistanceString(Utils.getDistanceString(astation.getDistance(), accuracy, isDetailed, distanceUnit));
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
		if (values.length > 0) {
			from.onClosestBikeStationsProgress(values[0]);
			super.onProgressUpdate(values);
		}
	}

	@Override
	protected void onPostExecute(ClosestPOI<ABikeStation> result) {
		MyLog.v(TAG, "onPostExecute()");
		from.onClosestBikeStationsDone(result);
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

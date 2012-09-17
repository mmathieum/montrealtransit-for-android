package org.montrealtransit.android.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.data.SubwayStationDistancesComparator;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

/**
 * Find the closest subway stations.
 * @author Mathieu Méa
 */
public class ClosestSubwayStationsFinderTask extends AsyncTask<Location, String, ClosestPOI<ASubwayStation>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestSubwayStationsFinderTask.class.getSimpleName();

	// /**
	// * The minimum number of closest stations in the list.
	// */
	// private static final int MIN_CLOSEST_STATIONS_LIST_SIZE = 3;
	// /**
	// * The maximum number of closest stations in the list.
	// */
	// private static final int MAX_CLOSEST_STATIONS_LIST_SIZE = 5;
	/**
	 * The context.
	 */
	private Context context;
	/**
	 * The class handling the result and progress.
	 */
	private ClosestSubwayStationsFinderListener from;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 */
	public ClosestSubwayStationsFinderTask(ClosestSubwayStationsFinderListener from, Context context) {
		this.from = from;
		this.context = context;
	}

	@Override
	protected ClosestPOI<ASubwayStation> doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<ASubwayStation> result = null;
		// read last (not too old) location
		Location currentLocation = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (currentLocation != null) {
			publishProgress(this.context.getString(R.string.processing));
			result = new ClosestPOI<ASubwayStation>();
			// create a list of all stations with lines and location
			List<ASubwayStation> stationsWithOtherLines = getAllStationsWithLines(currentLocation);
			// order the stations list by distance (closest first)
			Collections.sort(stationsWithOtherLines, new SubwayStationDistancesComparator());
			result.setPoiList(stationsWithOtherLines);
		}
		return result;
	}

	/**
	 * Create a list of all stations including their distance to the location and all subway lines.
	 * @param currentLocation the location
	 * @return the list of localized stations
	 */
	private List<ASubwayStation> getAllStationsWithLines(Location currentLocation) {
		Map<String, ASubwayStation> aresult = new HashMap<String, ASubwayStation>();
		float accuracy = currentLocation.getAccuracy();
		boolean isDetailed = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		// FOR each subway line + station combinations DO
		for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayStationsAndLinesList(context.getContentResolver())) {
			// read subway line number
			SubwayLine subwayLine = lineStation.first;
			// read subway station ID
			SubwayStation subwayStation = lineStation.second;
			// IF this is the 1st line of the station DO
			if (!aresult.containsKey(subwayStation.getId())) {
				ASubwayStation station = new ASubwayStation(subwayStation);
				// location
				station.setDistance(currentLocation.distanceTo(subwayStation.getLocation()));
				station.setDistanceString(Utils.getDistanceString(station.getDistance(), accuracy, isDetailed, distanceUnit));
				aresult.put(subwayStation.getId(), station);
			}
			// add the subway line number
			aresult.get(subwayStation.getId()).addOtherLineId(subwayLine.getNumber());
		}
		return new ArrayList<ASubwayStation>(aresult.values());
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length > 0) {
			from.onClosestStationsProgress(values[0]);
			super.onProgressUpdate(values);
		}
	}

	@Override
	protected void onPostExecute(ClosestPOI<ASubwayStation> result) {
		MyLog.v(TAG, "onPostExecute()");
		from.onClosestStationsDone(result);
		super.onPostExecute(result);
	}

	/**
	 * Contract for handling {@link ClosestSubwayStationsFinderTask}.
	 */
	public interface ClosestSubwayStationsFinderListener {

		/**
		 * Called to share task execution progress
		 * @param message the progress
		 */
		void onClosestStationsProgress(String message);

		/**
		 * Call when the task is completed.
		 * @param result the result of the task
		 */
		void onClosestStationsDone(ClosestPOI<ASubwayStation> result);
	}
}
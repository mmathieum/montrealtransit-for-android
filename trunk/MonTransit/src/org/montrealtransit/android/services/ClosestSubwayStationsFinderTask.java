package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.Pair;
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
	private WeakReference<ClosestSubwayStationsFinderListener> from;

	private int maxResult = 10;

	/**
	 * The default constructor.
	 * @param from the class handling the result and progress
	 * @param context the context
	 */
	public ClosestSubwayStationsFinderTask(ClosestSubwayStationsFinderListener from, Context context, int maxResult) {
		this.from = new WeakReference<ClosestSubwayStationsFinderTask.ClosestSubwayStationsFinderListener>(from);
		this.context = context;
		this.maxResult = maxResult;
	}

	@Override
	protected ClosestPOI<ASubwayStation> doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<ASubwayStation> result = null;
		// read last (not too old) location
		Location location = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (location != null) {
			publishProgress(this.context.getString(R.string.processing));
			result = new ClosestPOI<ASubwayStation>(location);
			// create a list of all stations with lines and location
			List<ASubwayStation> stationsWithOtherLines = getAllStationsWithLines(context, location, maxResult);
			// order the stations list by distance (closest first)
			Collections.sort(stationsWithOtherLines, new POI.POIDistanceComparator());
			result.setPoiList(stationsWithOtherLines);
		}
		return result;
	}

	/**
	 * Create a list of all stations including their distance to the location and all subway lines.
	 * @param currentLocation the location
	 * @return the list of localized stations
	 */
	public static List<ASubwayStation> getAllStationsWithLines(Context context, Location location, int maxResult) {
		Map<String, ASubwayStation> aresult = new HashMap<String, ASubwayStation>();
		// try the short way with location hack
		List<Pair<SubwayLine, SubwayStation>> stationsWithLoc = StmManager.findAllSubwayStationsAndLinesLocationList(context, location.getLatitude(),
				location.getLongitude());
		// MyLog.d(TAG, "1st try: " + Utils.getCollectionSize(subwayStationsWithLoc));
		if (Utils.getCollectionSize(stationsWithLoc) == 0) { // if no value return
			// do it the hard long way
			stationsWithLoc = StmManager.findSubwayStationsAndLinesList(context);
		}
		// FOR each subway line + station combinations DO
		for (Pair<SubwayLine, SubwayStation> lineStation : stationsWithLoc) {
			// read subway line number
			SubwayLine subwayLine = lineStation.first;
			// read subway station ID
			SubwayStation subwayStation = lineStation.second;
			// IF this is the 1st line of the station DO
			if (!aresult.containsKey(subwayStation.getId())) {
				aresult.put(subwayStation.getId(), new ASubwayStation(subwayStation));
			}
			// add the subway line number
			aresult.get(subwayStation.getId()).addOtherLineId(subwayLine.getNumber());
		}
		List<ASubwayStation> lresult = new ArrayList<ASubwayStation>(aresult.values());
		if (maxResult > 0 && aresult.size() > maxResult) {
			lresult = lresult.subList(0, maxResult);
		}
		LocationUtils.updateDistanceWithString(context, lresult, location);
		return lresult;
	}

	public static Collection<ASubwayStation> getAllStationsWithLines(Context context, double lat, double lng) {
		// try the short way with location hack
		List<Pair<SubwayLine, SubwayStation>> stationsWithLoc = StmManager.findAllSubwayStationsAndLinesLocationList(context, lat, lng);
		// MyLog.d(TAG, "1st try: " + Utils.getCollectionSize(subwayStationsWithLoc));
		if (Utils.getCollectionSize(stationsWithLoc) == 0) { // if no value return
			// do it the hard long way
			stationsWithLoc = StmManager.findSubwayStationsAndLinesList(context);
		}
		Map<String, ASubwayStation> aresult = new HashMap<String, ASubwayStation>();
		// FOR each subway line + station combinations DO
		for (Pair<SubwayLine, SubwayStation> lineStation : stationsWithLoc) {
			// read subway line number
			SubwayLine subwayLine = lineStation.first;
			// read subway station ID
			SubwayStation subwayStation = lineStation.second;
			// IF this is the 1st line of the station DO
			if (!aresult.containsKey(subwayStation.getId())) {
				aresult.put(subwayStation.getId(), new ASubwayStation(subwayStation));
			}
			// add the subway line number
			aresult.get(subwayStation.getId()).addOtherLineId(subwayLine.getNumber());
		}
		return aresult.values();
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (values.length <= 0) {
			return;
		}
		ClosestSubwayStationsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStationsProgress(values[0]);
		} else {
			MyLog.d(TAG, "listener null!");
		}
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(ClosestPOI<ASubwayStation> result) {
		MyLog.v(TAG, "onPostExecute()");
		ClosestSubwayStationsFinderListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onClosestStationsDone(result);
		} else {
			MyLog.d(TAG, "listener null!");
		}
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
package org.montrealtransit.android.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestSubwayStations;
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
public class ClosestSubwayStationsFinderTask extends AsyncTask<Location, String, ClosestSubwayStations> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ClosestSubwayStationsFinderTask.class.getSimpleName();

	/**
	 * The minimum number of closest stations in the list.
	 */
	private static final int MIN_CLOSEST_STATIONS_LIST_SIZE = 3;
	/**
	 * The maximum number of closest stations in the list.
	 */
	private static final int MAX_CLOSEST_STATIONS_LIST_SIZE = 5;
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
	protected ClosestSubwayStations doInBackground(Location... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestSubwayStations result = null;
		// read last (not too old) location
		Location currentLocation = params[0];
		// MyLog.d(TAG, "currentLocation:" + currentLocation);
		// IF location available DO
		if (currentLocation != null) {
			publishProgress(this.context.getString(R.string.processing));
			result = new ClosestSubwayStations();
			result.setLocationAndAddress(currentLocation, this.context);
			// read location accuracy
			float currentAccuracy = currentLocation.getAccuracy();
			// MyLog.d(TAG, "currentAccuracy: " + currentAccuracy);
			// create a list of all stations with lines and location
			List<ASubwayStation> stationsWithOtherLines = getAllStationsWithLines(currentLocation);
			// order the stations list by distance (closest first)
			SubwayStationDistancesComparator comparator = new SubwayStationDistancesComparator();
			Collections.sort(stationsWithOtherLines, comparator);
			// select only the firsts stations
			// FOR each ordered station DO
			for (ASubwayStation station : stationsWithOtherLines) {
				// IF minimum station not reached DO
				if (result.getStations().size() < MIN_CLOSEST_STATIONS_LIST_SIZE) {
					// add the station
					result.getStations().add(station);
					// ELSE ID maximum stations not reached AND location is too bad DO
				} else if (result.getStations().size() < MAX_CLOSEST_STATIONS_LIST_SIZE
				        && station.getDistance() < currentAccuracy) {
					// add the station
					result.getStations().add(station);
				} else {
					// it's over
					break;
				}
			}
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
		// FOR each subway line + station combinations DO
		for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayStationsAndLinesList(context
		        .getContentResolver())) {
			// read subway line number
			SubwayLine subwayLine = lineStation.first;
			// read subway station ID
			SubwayStation subwayStation = lineStation.second;
			// IF this is the first line of the station DO
			if (!aresult.containsKey(subwayStation.getId())) {
				ASubwayStation station = new ASubwayStation(subwayStation);
				// location
				Location stationLocation = LocationUtils.getNewLocation(subwayStation.getLat(), subwayStation.getLng());
				station.setDistance(currentLocation.distanceTo(stationLocation));
				String distanceString = Utils.getDistanceString(this.context, station.getDistance(),
				        currentLocation.getAccuracy());
				station.setDistanceString(distanceString);
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
	protected void onPostExecute(ClosestSubwayStations result) {
		MyLog.v(TAG, "onPostExecute()");
		from.onClosestStationsDone(result);
		super.onPostExecute(result);
	}
}
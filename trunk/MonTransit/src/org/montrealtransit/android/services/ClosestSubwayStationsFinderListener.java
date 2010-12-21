package org.montrealtransit.android.services;

import org.montrealtransit.android.data.ClosestSubwayStations;

/**
 * Contract for handling {@link ClosestSubwayStationsFinderTask}.
 * @author Mathieu Méa
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
	void onClosestStationsDone(ClosestSubwayStations result);
}

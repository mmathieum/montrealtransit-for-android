package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.data.StopTimes;

import android.util.SparseArray;

/**
 * This interface have to be implemented by any class that want handle next services response.
 * @author Mathieu Méa
 */
public interface NextStopListener {

	/**
	 * Update the progress status.
	 * @param progress the progress message
	 */
	void onNextStopsProgress(String progress);

	/**
	 * Methods calls after the execution of the task.
	 * @param results the results
	 */
	void onNextStopsLoaded(SparseArray<StopTimes> results);

}

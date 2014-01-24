package org.montrealtransit.android.services.nextstop;

import java.util.Map;

import org.montrealtransit.android.data.StopTimes;

/**
 * This interface have to be implemented by any class that want handle next services response.
 * @author Mathieu Méa
 */
public interface NextStopListener {

	/**
	 * Update the progress status.
	 * @param authority the schedule provider authority
	 * @param progress the progress message
	 */
	void onNextStopsProgress(String authority, String progress);

	/**
	 * Methods calls after the execution of the task.
	 * @param authority the schedule provider authority
	 * @param results the results
	 */
	void onNextStopsLoaded(String authority, Map<String,StopTimes> results);

}

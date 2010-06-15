package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.data.BusStopHours;

/**
 * This interface have to be implemented by any class that want handle next bus services response.
 * @author Mathieu Méa
 */
public interface NextStopListener {

	/**
	 * Update the progress status.
	 * @param progress the progress message 
	 */
	void updateProgress(String progress);
	
	/**
	 * Methods calls after the execution of the task.
	 * @param result the result
	 */
	void onPostExectute(BusStopHours result);

	/**
	 * Methods calls when the task is cancelled.
	 */
	void onCancelled();
}

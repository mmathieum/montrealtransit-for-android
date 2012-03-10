package org.montrealtransit.android.services;

/**
 * The contract for handling {@link StmInfoStatusReader} or {@link StmInfoStatusApiReader}.
 * @author Mathieu Méa
 */
public interface StmInfoStatusReaderListener {

	/**
	 * Called when the task is completed.
	 * @param errorMessage the error message or <b>NULL</b>
	 * @param serviceStatus the new service status or <b>NULL</b>
	 */
	void onStmInfoStatusesLoaded(String errorMessage);
	// TODO return the current status so the listener doesn't need to get it from the DB

}

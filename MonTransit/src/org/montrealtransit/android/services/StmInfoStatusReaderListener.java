package org.montrealtransit.android.services;

/**
 * The contract for handling {@link StmInfoStatusReader}.
 * @author Mathieu Méa
 */
public interface StmInfoStatusReaderListener {

	/**
	 * Called when the task is completed.
	 * @param errorMessage the error message or <b>NULL</b>
	 */
	void onStmInfoStatusesLoaded(String errorMessage);

}

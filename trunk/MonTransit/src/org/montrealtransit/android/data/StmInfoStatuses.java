package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of {@link StmInfoStatus} + error message.
 * @author Mathieu Méa
 */
public class StmInfoStatuses {

	/**
	 * The list of statuses.
	 */
	private List<StmInfoStatus> statuses = new ArrayList<StmInfoStatus>();
	/**
	 * The error message.
	 */
	private String error = null;

	/**
	 * The default constructor.
	 */
	public StmInfoStatuses() {
	}

	/**
	 * Constructor
	 * @param error the error message
	 */
	public StmInfoStatuses(String error) {
		this.error = error;
	}

	/**
	 * @param status the new status
	 */
	public void add(StmInfoStatus status) {
		this.statuses.add(status);
	}

	/**
	 * @param location the status location in the list
	 * @return the status
	 */
	public StmInfoStatus get(int location) {
		return this.statuses.get(location);
	}

	/**
	 * @return the number of statuses
	 */
	public int size() {
		return this.statuses.size();
	}

	/**
	 * @return the error message
	 */
	public String getError() {
		return error;
	}
}

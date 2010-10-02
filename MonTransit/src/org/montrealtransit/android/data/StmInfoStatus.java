package org.montrealtransit.android.data;

import java.util.Date;

import org.montrealtransit.android.MyLog;

/**
 * Represent the status of the subway.
 * @author Mathieu Méa
 */
public class StmInfoStatus {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoStatus.class.getSimpleName();

	/**
	 * The message.
	 */
	private String message;
	/**
	 * The date.
	 */
	private Date date;
	/**
	 * The status.
	 */
	private Status status;

	/**
	 * The available statuses.
	 */
	public enum Status {
		GREEN, YELLOW, RED
	}

	/**
	 * Default constructor.
	 * @param message the message
	 */
	public StmInfoStatus(String message) {
		setMessage(message);
	}

	/**
	 * Set the message (cleaned) and the status.
	 * @param message
	 */
	public void setMessage(String message) {
		MyLog.v(TAG, "setMessage(" + message + ")");
		// extract the subway status from the code
		String statusChar = message.substring(message.length() - 2, message.length() - 1);
		if (statusChar.equals("V")) {
			setStatus(Status.GREEN);
		} else if (statusChar.equals("J")) {
			setStatus(Status.YELLOW);
		} else if (statusChar.equals("R")) {
			setStatus(Status.RED);
		}
		// clean message (remove 'stminfo: ' and ' #STM XY')
		message = message.substring(9, message.length() - 8);
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param status the new status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * @param date the new date
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

}

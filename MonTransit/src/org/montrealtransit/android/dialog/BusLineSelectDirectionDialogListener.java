package org.montrealtransit.android.dialog;

/**
 * This interface must be implemented by all the classes that want to be able to manage the BusSelectDirection dialog.
 * @author Mathieu Méa
 */
public interface BusLineSelectDirectionDialogListener {

	/**
	 * Show the selected bus line (direction).
	 * @param lineNumber the line number
	 * @param lineName the line name or null (not required)
	 * @param directionId the line direction
	 */
	void showNewLine(String lineNumber, String lineName, String directionId);

}

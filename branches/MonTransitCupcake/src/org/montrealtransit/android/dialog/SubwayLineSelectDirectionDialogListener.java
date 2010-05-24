package org.montrealtransit.android.dialog;

/**
 * This interface must be implemented by all the classes that want to be able to manage the BusSelectDirection dialog.
 * @author Mathieu Méa
 */
public interface SubwayLineSelectDirectionDialogListener {
	
	/**
	 * show the selected subway line (direction).
	 * @param lineNumber the line number
	 * @param orderId the order use to display the stations
	 */
	 void showNewSubway(int lineNumber, String orderId);

}

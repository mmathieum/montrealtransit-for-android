package org.montrealtransit.android.activity;

import android.content.Context;

/**
 * This class manage the selection of the bus line direction from a bus line number.
 * @author Mathieu Méa
 */
@Deprecated
public class BusLineSelectDirection extends org.montrealtransit.android.dialog.BusLineSelectDirection {

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param lineNumber the bus line number
	 */
	public BusLineSelectDirection(Context context, String lineNumber) {
		super(context, lineNumber);
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param lineNumber the line number
	 * @param listener the dialog listener
	 */
	public BusLineSelectDirection(Context context, String lineNumber, BusLineSelectDirectionDialogListener listener) {
		super(context, lineNumber, listener);
	}
}

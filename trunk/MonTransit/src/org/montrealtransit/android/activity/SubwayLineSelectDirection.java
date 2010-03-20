package org.montrealtransit.android.activity;

import android.content.Context;

/**
 * This class handle the subway line direction selection.
 * @author Mathieu Méa
 */
@Deprecated
public class SubwayLineSelectDirection extends org.montrealtransit.android.dialog.SubwayLineSelectDirection {

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param subwayLineId the bus line number
	 */
	public SubwayLineSelectDirection(Context context, int subwayLineId) {
		super(context,subwayLineId);
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param subwayLineId the line number
	 * @param listener the dialog listener
	 */
	public SubwayLineSelectDirection(Context context, int subwayLineId, SubwayLineSelectDirectionDialogListener listener) {
		super(context, subwayLineId, listener);
	}
}

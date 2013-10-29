package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusLineInfo;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.provider.StmBusManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;

/**
 * This class manage the selection of the bus line direction from a bus line number.
 * @author Mathieu Méa
 */
public class BusLineSelectDirection implements View.OnClickListener, BusLineSelectDirectionDialogListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineSelectDirection.class.getSimpleName();
	/**
	 * The bus line number.
	 */
	private String lineNumber;
	/**
	 * The bus line name or null.
	 */
	private String lineName;
	private String lineColor;
	private String lineTextColor;
	/**
	 * The activity context calling the selector.
	 */
	private Context context;
	/**
	 * The listener that will receive the action after the selection.
	 */
	private BusLineSelectDirectionDialogListener listener;
	/**
	 * The selected bus line directions ID.
	 */
	private List<Trip> selectedBusLineDirectionIds;
	/**
	 * The simple directions ID.
	 */
	private List<Integer> simpleDirectionsIds;
	/**
	 * True if the last dialog shown was the second dialog.
	 */
	protected boolean secondDialog = false;
	/**
	 * The current direction ID or null.
	 */
	private String currentDirectionId = null;

	public BusLineSelectDirection(Context context, String lineNumber, String lineName, String lineColor, String lineTextColor, String currentDirectionId) {
		this.context = context;
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.lineColor = lineColor;
		this.lineTextColor = lineTextColor;
		this.currentDirectionId = currentDirectionId;
		this.listener = this;
	}

	public BusLineSelectDirection(Context context, Route line) {
		this.context = context;
		this.lineNumber = line.shortName;
		this.lineName = line.longName;
		this.lineColor = line.color;
		this.lineTextColor = line.textColor;
		this.listener = this;
	}

	public BusLineSelectDirection(Context context, Route line, String currentDirectionId, BusLineSelectDirectionDialogListener listener) {
		this.context = context;
		this.lineNumber = line.shortName;
		this.lineName = line.longName;
		this.lineColor = line.color;
		this.lineTextColor = line.textColor;
		this.currentDirectionId = currentDirectionId;
		this.listener = listener;
	}

	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick()");
		showDialog();
	}

	/**
	 * Actually show the bus line direction selector (dialog).
	 */
	public void showDialog() {
		try {
			showFirstAlertDialog();
		} catch (WrongBusLineNumberException e) {
			Utils.notifyTheUser(this.context, this.context.getString(R.string.wrong_line_number_and_number, this.lineNumber));
		} catch (OutOfMemoryError oome) { // occurs on some low end devices (like Samsung Galaxy 551)
			if (this.simpleDirectionsIds == null || this.simpleDirectionsIds.isEmpty()) {
				getFirstItems();
			}
			// show the 1st direction
			String directionId = getDirectionId(BusLineSelectDirection.this.simpleDirectionsIds.get(0));
			String number = BusLineSelectDirection.this.lineNumber;
			BusLineSelectDirection.this.listener.showNewLine(number, directionId);
		}
	}

	/**
	 * Return the alert dialog.
	 * @return the alert dialog
	 * @throws WrongBusLineNumberException @see {@link BusLineSelectDirection#getItems()}
	 */
	private void showFirstAlertDialog() throws WrongBusLineNumberException {
		MyLog.v(TAG, "getFirstAlertDialog()");
		String[] firstItems = getFirstItems(); // initialize simpleDirectionsIds
		int indexOf = -1;
		if (this.currentDirectionId != null && this.simpleDirectionsIds != null) {
			indexOf = this.simpleDirectionsIds.indexOf(this.currentDirectionId);
		}
		new AlertDialog.Builder(this.context).setTitle(this.context.getString(R.string.select_bus_line_direction_and_number, this.lineNumber))
				.setSingleChoiceItems(firstItems, indexOf, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// FIRST DIALOG (simple directions)
						// show the bus line direction
						String directionId = getDirectionId(BusLineSelectDirection.this.simpleDirectionsIds.get(which));
						if (directionId != null && !TextUtils.isEmpty(directionId)) {
							String number = BusLineSelectDirection.this.lineNumber;
							BusLineSelectDirection.this.listener.showNewLine(number, directionId);
							dialog.dismiss(); // CLOSE
						}
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel(); // CANCEL
					}
				}).create().show();
	}

	/**
	 * @return the first direction items (simple directions)
	 */
	private String[] getFirstItems() {
		MyLog.v(TAG, "getFirstItems()");
		List<String> items = new ArrayList<String>();
		this.simpleDirectionsIds = new ArrayList<Integer>();
		for (Trip directionId : getSelectedBusLineDirectionIds()) {
			if (!this.simpleDirectionsIds.contains(directionId.id)) {
				this.simpleDirectionsIds.add(directionId.id);
				items.add(directionId.getHeading(context));
			}
		}
		return items.toArray(new String[] {});
	}

	/**
	 * @return the direction for the selected bus line (number)
	 */
	private List<Trip> getSelectedBusLineDirectionIds() {
		if (this.selectedBusLineDirectionIds == null) {
			this.selectedBusLineDirectionIds = StmBusManager.findTripsWithRouteIdList(this.context, this.lineNumber);
		}
		return this.selectedBusLineDirectionIds;
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the first direction ID matching the simple direction ID
	 */
	private String getDirectionId(Integer simpleDirectionId) {
		for (Trip direction : getSelectedBusLineDirectionIds()) {
			if (direction.id == simpleDirectionId) {
				return String.valueOf(direction.id);
			}
		}
		return null;
	}

	@Override
	public void showNewLine(String lineNumber, String directionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", lineNumber, directionId);
		this.context.startActivity(BusLineInfo.newInstance(this.context, lineNumber, this.lineName, this.lineColor, this.lineTextColor, directionId));
	}

	/**
	 * This custom exception is raised when a submitted bus line number doesn't exist.
	 * @author Mathieu Méa
	 */
	public class WrongBusLineNumberException extends Exception {
		/**
		 * The default serial ID.
		 */
		private static final long serialVersionUID = 1L;
	}
}

package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusLineInfo;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLineDirection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

/**
 * This class manage the selection of the bus line direction from a bus line number.
 * @author Mathieu Méa
 */
public class BusLineSelectDirection implements android.view.View.OnClickListener,
        android.content.DialogInterface.OnClickListener, BusLineSelectDirectionDialogListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineSelectDirection.class.getSimpleName();
	/**
	 * The bus line number.
	 */
	private String lineNumber;
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
	private List<BusLineDirection> selectedBusLineDirectionIds;
	/**
	 * The simple directions ID.
	 */
	private List<String> simpleDirectionsId;
	/**
	 * The detailed directions ID.
	 */
	private List<String> detailDirectionsId;
	/**
	 * True if the last dialog shown was the second dialog.
	 */
	private boolean secondDialog = false;

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param lineNumber the bus line number
	 */
	public BusLineSelectDirection(Context context, String lineNumber) {
		this.lineNumber = lineNumber;
		this.context = context;
		this.listener = this;
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param lineNumber the line number
	 * @param listener the dialog listener
	 */
	public BusLineSelectDirection(Context context, String lineNumber, BusLineSelectDirectionDialogListener listener) {
		this.lineNumber = lineNumber;
		this.context = context;
		this.listener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
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
			getFirstAlertDialog().show();
		} catch (WrongBusLineNumberException e) {
			String message = this.context.getResources().getString(R.string.wrong_line_number_before) + this.lineNumber
			        + this.context.getResources().getString(R.string.wrong_line_number_after);
			Utils.notifyTheUser(this.context, message);
		}
	}

	/**
	 * Return the alert dialog.
	 * @return the alert dialog
	 * @throws WrongBusLineNumberException @see {@link BusLineSelectDirection#getItems()}
	 */
	private AlertDialog getFirstAlertDialog() throws WrongBusLineNumberException {
		MyLog.v(TAG, "getFirstAlertDialog()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setTitle(context.getResources().getString(R.string.line) + " " + this.lineNumber + " - "
		        + context.getResources().getString(R.string.select_bus_direction));
		builder.setItems(getFirstItems(), this);
		builder.setNegativeButton(R.string.cancel, this);
		AlertDialog alert = builder.create();
		return alert;
	}

	/**
	 * @return the first direction items (simple directions)
	 */
	private String[] getFirstItems() {
		MyLog.v(TAG, "getFirstItems()");
		this.simpleDirectionsId = new ArrayList<String>();
		for (StmStore.BusLineDirection directionId : getSelectedBusLineDirectionIds()) {
			if (!this.simpleDirectionsId.contains(directionId.getSimpleId())) {
				this.simpleDirectionsId.add(directionId.getSimpleId());
			}
		}
		List<String> items = new ArrayList<String>();
		for (String itemId : this.simpleDirectionsId) {
			items.add(this.context.getResources().getString(Utils.getBusLineDirectionStringIdFromId(itemId).get(0)));
		}
		return items.toArray(new String[0]);
	}

	/**
	 * @return the direction for the selected bus line (number)
	 */
	private List<StmStore.BusLineDirection> getSelectedBusLineDirectionIds() {
		if (this.selectedBusLineDirectionIds == null) {
			this.selectedBusLineDirectionIds = StmManager.findBusLineDirections(this.context.getContentResolver(),
			        this.lineNumber);
		}
		return this.selectedBusLineDirectionIds;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(" + which + ")");
		if (!this.secondDialog) {
			// FIRST DIALOG (simple directions)
			if (which == -2) {
				dialog.dismiss(); // CANCEL
			} else {
				// IF more than one detail directions for this simple direction DO
				if (isMoreThanOneDirectionFor(this.simpleDirectionsId.get(which))) {
					// show the second dialog
					showSecondDialog(this.simpleDirectionsId.get(which));
				} else {
					// show the bus line direction
					String directionId = getDirectionId(this.simpleDirectionsId.get(which));
					if (!TextUtils.isEmpty(directionId)) {
						this.listener.showNewLine(this.lineNumber, directionId);
					}
				}
			}
		} else {
			// SECOND DIALOG (direction details)
			if (which == -2) {
				dialog.cancel();// CANCEL
				this.secondDialog = false;
				showDialog(); // show first dialog
			} else {
				// show the bus line direction
				this.listener.showNewLine(this.lineNumber, this.detailDirectionsId.get(which));
			}
		}
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the first direction ID matching the simple direction ID
	 */
	private String getDirectionId(String simpleDirectionId) {
		for (StmStore.BusLineDirection direction : getSelectedBusLineDirectionIds()) {
			if (direction.getSimpleId().equalsIgnoreCase(simpleDirectionId)) {
				return direction.getId();
			}
		}
		return null;
	}

	/**
	 * Show the second dialog (detail directions)
	 * @param simpleDirectionId the simple direction ID
	 */
	public void showSecondDialog(String simpleDirectionId) {
		this.secondDialog = true;
		getSecondAlertDialog(simpleDirectionId).show();
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the dialog of detail directions ID
	 */
	private AlertDialog getSecondAlertDialog(String simpleDirectionId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setTitle(context.getResources().getString(R.string.line) + " " + this.lineNumber + " - "
		        + context.getResources().getString(R.string.select_bus_direction) + " "
		        + context.getString(Utils.getBusLineDirectionStringIdFromId(simpleDirectionId).get(0)));
		builder.setItems(getSecondItems(simpleDirectionId), this);
		builder.setNegativeButton(R.string.cancel, this);
		AlertDialog alert = builder.create();
		return alert;
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the list of details direction ID
	 */
	private String[] getSecondItems(String simpleDirectionId) {
		this.detailDirectionsId = new ArrayList<String>();
		for (StmStore.BusLineDirection directionId : getSelectedBusLineDirectionIds()) {
			if (directionId.getSimpleId().equalsIgnoreCase(simpleDirectionId)) {
				if (!this.detailDirectionsId.contains(directionId.getId())) {
					this.detailDirectionsId.add(directionId.getId());
				}
			}
		}
		List<String> items = new ArrayList<String>();
		for (String itemId : this.detailDirectionsId) {
			List<Integer> stringIds = Utils.getBusLineDirectionStringIdFromId(itemId);
			int stringId;
			if (stringIds.size() >= 2) {
				stringId = stringIds.get(1);
			} else {
				stringId = R.string.regular_route;
			}
			items.add(this.context.getResources().getString(stringId));
		}
		return items.toArray(new String[0]);
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return true if the there are more than one direction ID matching the simple direction ID
	 */
	private boolean isMoreThanOneDirectionFor(String simpleDirectionId) {
		int nb = 0;
		for (StmStore.BusLineDirection directionId : getSelectedBusLineDirectionIds()) {
			if (directionId.getSimpleId().equalsIgnoreCase(simpleDirectionId)) {
				nb++;
			}
		}
		return nb > 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewLine(String lineNumber, String directionId) {
		Intent mIntent = new Intent(this.context, BusLineInfo.class);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, lineNumber);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_DIRECTION_ID, directionId);
		this.context.startActivity(mIntent);
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

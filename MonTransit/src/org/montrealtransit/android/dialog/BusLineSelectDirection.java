package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.BusUtils;
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
	protected boolean secondDialog = false;

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param lineNumber the bus line number
	 */
	public BusLineSelectDirection(Context context, String lineNumber, String lineName) {
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.context = context;
		this.listener = this;
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param lineNumber the line number
	 * @param lineName the line name or null
	 * @param listener the dialog listener
	 */
	public BusLineSelectDirection(Context context, String lineNumber, String lineName, BusLineSelectDirectionDialogListener listener) {
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.context = context;
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
			Utils.notifyTheUser(this.context, context.getString(R.string.wrong_line_number_and_number, lineNumber));
		}
	}

	/**
	 * Return the alert dialog.
	 * @return the alert dialog
	 * @throws WrongBusLineNumberException @see {@link BusLineSelectDirection#getItems()}
	 */
	private void showFirstAlertDialog() throws WrongBusLineNumberException {
		MyLog.v(TAG, "getFirstAlertDialog()");
		new AlertDialog.Builder(this.context).setTitle(this.context.getString(R.string.select_bus_line_direction_and_number, this.lineNumber))
				.setItems(getFirstItems(), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// FIRST DIALOG (simple directions)
						// IF more than one detail directions for this simple direction DO
						if (isMoreThanOneDirectionFor(BusLineSelectDirection.this.simpleDirectionsId.get(which))) {
							// show the second dialog
							showSecondDialog(BusLineSelectDirection.this.simpleDirectionsId.get(which));
						} else {
							// show the bus line direction
							String directionId = getDirectionId(BusLineSelectDirection.this.simpleDirectionsId.get(which));
							if (!TextUtils.isEmpty(directionId)) {
								String number = BusLineSelectDirection.this.lineNumber;
								String name = BusLineSelectDirection.this.lineName;
								BusLineSelectDirection.this.listener.showNewLine(number, name, directionId);
							}
						}
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss(); // CANCEL
					}
				}).create().show();
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
			items.add(this.context.getString(BusUtils.getBusLineDirectionStringIdFromId(itemId).get(0)));
		}
		return items.toArray(new String[0]);
	}

	/**
	 * @return the direction for the selected bus line (number)
	 */
	private List<StmStore.BusLineDirection> getSelectedBusLineDirectionIds() {
		if (this.selectedBusLineDirectionIds == null) {
			this.selectedBusLineDirectionIds = StmManager.findBusLineDirections(this.context.getContentResolver(), this.lineNumber);
		}
		return this.selectedBusLineDirectionIds;
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
		showSecondAlertDialog(simpleDirectionId);
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the dialog of detail directions ID
	 */
	private void showSecondAlertDialog(String simpleDirectionId) {
		String title = this.context.getString(R.string.select_bus_line_detail_direction_and_number_and_direction, this.lineNumber,
				this.context.getString(BusUtils.getBusLineDirectionStringIdFromId(simpleDirectionId).get(0)));
		new AlertDialog.Builder(this.context).setTitle(title).setItems(getSecondItems(simpleDirectionId), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// show the bus line direction
				String number = BusLineSelectDirection.this.lineNumber;
				String name = BusLineSelectDirection.this.lineName;
				String directionId = BusLineSelectDirection.this.detailDirectionsId.get(which);
				BusLineSelectDirection.this.listener.showNewLine(number, name, directionId);
			}
		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();// CANCEL
				BusLineSelectDirection.this.secondDialog = false;
				showDialog(); // show first dialog
			}
		}).create().show();
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
			List<Integer> stringIds = BusUtils.getBusLineDirectionStringIdFromId(itemId);
			int stringId;
			if (stringIds.size() >= 2) {
				stringId = stringIds.get(1);
			} else {
				stringId = R.string.regular_route;
			}
			items.add(this.context.getString(stringId));
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

	@Override
	public void showNewLine(String lineNumber, String lineName, String directionId) {
		Intent mIntent = new Intent(this.context, BusLineInfo.class);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, lineNumber);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_NAME, lineNumber);
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

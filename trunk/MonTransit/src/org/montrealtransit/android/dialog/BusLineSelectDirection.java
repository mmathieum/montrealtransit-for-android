package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusLineInfo;
import org.montrealtransit.android.api.SupportFactory;
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
	 * The bus line type or null.
	 */
	private String lineType;
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
	private List<String> simpleDirectionsIds;
	/**
	 * The detailed directions ID.
	 */
	private List<String> detailDirectionsIds;
	/**
	 * True if the last dialog shown was the second dialog.
	 */
	protected boolean secondDialog = false;
	/**
	 * The current direction ID or null.
	 */
	private String currentDirectionId = null;

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param lineNumber the bus line number
	 */
	public BusLineSelectDirection(Context context, String lineNumber, String lineName, String lineType) {
		this.context = context;
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.lineType = lineType;
		this.listener = this;
	}

	public BusLineSelectDirection(Context context, String lineNumber, String lineName, String lineType, String currentDirectionId) {
		this.context = context;
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.lineType = lineType;
		this.currentDirectionId = currentDirectionId;
		this.listener = this;
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param lineNumber the line number
	 * @param lineName the line name or null
	 * @param listener the dialog listener
	 */
	public BusLineSelectDirection(Context context, String lineNumber, String lineName, String lineType, BusLineSelectDirectionDialogListener listener) {
		this(context, lineNumber, lineName, lineType, null, listener);
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param lineNumber the line number
	 * @param lineName the line name or null
	 * @param listener the dialog listener
	 */
	public BusLineSelectDirection(Context context, String lineNumber, String lineName, String lineType, String currentDirectionId,
			BusLineSelectDirectionDialogListener listener) {
		this.context = context;
		this.lineNumber = lineNumber;
		this.lineName = lineName;
		this.lineType = lineType;
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
			indexOf = this.simpleDirectionsIds.indexOf(BusLineDirection.toSimpleDirectionId(this.currentDirectionId));
		}
		new AlertDialog.Builder(this.context).setTitle(this.context.getString(R.string.select_bus_line_direction_and_number, this.lineNumber))
				.setSingleChoiceItems(firstItems, indexOf, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// FIRST DIALOG (simple directions)
						// IF more than 1 detail directions for this simple direction DO
						if (isMoreThanOneDirectionFor(BusLineSelectDirection.this.simpleDirectionsIds.get(which))) {
							// show the 2nd dialog
							showSecondDialog(BusLineSelectDirection.this.simpleDirectionsIds.get(which), dialog);
						} else {
							// show the bus line direction
							String directionId = getDirectionId(BusLineSelectDirection.this.simpleDirectionsIds.get(which));
							if (!TextUtils.isEmpty(directionId)) {
								String number = BusLineSelectDirection.this.lineNumber;
								BusLineSelectDirection.this.listener.showNewLine(number, directionId);
								dialog.dismiss(); // CLOSE
							}
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
		this.simpleDirectionsIds = new ArrayList<String>();
		for (StmStore.BusLineDirection directionId : getSelectedBusLineDirectionIds()) {
			if (!this.simpleDirectionsIds.contains(directionId.getSimpleId())) {
				this.simpleDirectionsIds.add(directionId.getSimpleId());
			}
		}
		List<String> items = new ArrayList<String>();
		for (String simpleDirectionId : this.simpleDirectionsIds) {
			items.add(this.context.getString(BusUtils.getBusLineDirectionStringIdFromId(simpleDirectionId).get(0)));
		}
		return items.toArray(new String[] {});
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
	 * @param firstDialog 1st dialog
	 */
	public void showSecondDialog(String simpleDirectionId, DialogInterface firstDialog) {
		this.secondDialog = true;
		showSecondAlertDialog(simpleDirectionId, firstDialog);
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the dialog of detail directions ID
	 */
	private void showSecondAlertDialog(String simpleDirectionId, final DialogInterface firstDialog) {
		String title = this.context.getString(R.string.select_bus_line_detail_direction_and_number_and_direction, this.lineNumber,
				this.context.getString(BusUtils.getBusLineDirectionStringIdFromId(simpleDirectionId).get(0)));
		String[] secondItems = getSecondItems(simpleDirectionId); // initialize detailDirectionsIds
		int indexOf = -1;
		if (this.currentDirectionId != null && this.detailDirectionsIds != null) {
			indexOf = this.detailDirectionsIds.indexOf(this.currentDirectionId);
		}
		new AlertDialog.Builder(this.context).setTitle(title).setSingleChoiceItems(secondItems, indexOf, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// show the bus line direction
				String number = BusLineSelectDirection.this.lineNumber;
				String directionId = BusLineSelectDirection.this.detailDirectionsIds.get(which);
				BusLineSelectDirection.this.listener.showNewLine(number, directionId);
				dialog.dismiss(); // CLOSE
				firstDialog.dismiss(); // also close 1st dialog
			}
		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel(); // CANCEL
				BusLineSelectDirection.this.secondDialog = false;
				// 1st dialog is still open
			}
		}).create().show();
	}

	/**
	 * @param simpleDirectionId the simple direction ID
	 * @return the list of details direction ID
	 */
	private String[] getSecondItems(String simpleDirectionId) {
		this.detailDirectionsIds = new ArrayList<String>();
		for (StmStore.BusLineDirection directionId : getSelectedBusLineDirectionIds()) {
			if (directionId.getSimpleId().equalsIgnoreCase(simpleDirectionId)) {
				if (!this.detailDirectionsIds.contains(directionId.getId())) {
					this.detailDirectionsIds.add(directionId.getId());
				}
			}
		}
		List<String> items = new ArrayList<String>();
		for (String defailDirectionId : this.detailDirectionsIds) {
			List<Integer> stringIds = BusUtils.getBusLineDirectionStringIdFromId(defailDirectionId);
			int stringId;
			if (stringIds.size() >= 2) {
				stringId = stringIds.get(1);
			} else {
				stringId = R.string.regular_route;
			}
			items.add(this.context.getString(stringId));
		}
		return items.toArray(new String[] {});
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
	public void showNewLine(String lineNumber, String directionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", lineNumber, directionId);
		Intent mIntent = new Intent(this.context, SupportFactory.getInstance(this.context).getBusLineInfoClass());
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, lineNumber);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_NAME, this.lineName);
		mIntent.putExtra(BusLineInfo.EXTRA_LINE_TYPE, this.lineType);
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

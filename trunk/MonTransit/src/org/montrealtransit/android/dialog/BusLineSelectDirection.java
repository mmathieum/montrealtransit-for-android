package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusLineInfo;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

/**
 * This class manage the selection of the bus line direction from a bus line number.
 * @author Mathieu Méa
 */
public class BusLineSelectDirection implements android.view.View.OnClickListener, android.content.DialogInterface.OnClickListener,
        BusLineSelectDirectionDialogListener {

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
	 * the array of ordered directions ID strings.
	 */
	private String[] orderedItemsDirectionId;
	/**
	 * The listener that will receive the action after the selection.
	 */
	private BusLineSelectDirectionDialogListener listener;

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
		MyLog.v(TAG, "onListItemClick()");
		showDialog();
	}

	/**
	 * Actually show the bus line direction selector (dialog).
	 */
	public void showDialog() {
		try {
			getAlertDialog().show();
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
	private AlertDialog getAlertDialog() throws WrongBusLineNumberException {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setTitle(context.getResources().getString(R.string.line) + " " + this.lineNumber + " - "
		        + context.getResources().getString(R.string.select_bus_direction));
		builder.setItems(getItems(), this);
		builder.setNegativeButton(R.string.cancel, this);
		AlertDialog alert = builder.create();
		return alert;
	}

	/**
	 * Return the items (directions) for the dialog.
	 * @return the items (directions)
	 * @throws WrongBusLineNumberException throw if the bus line number doesn't match an existing line number
	 */
	private String[] getItems() throws WrongBusLineNumberException {
		List<StmStore.BusLineDirection> selectedDirectionIds = StmManager.findBusLineDirections(this.context.getContentResolver(), this.lineNumber);

		if (selectedDirectionIds != null && selectedDirectionIds.size() > 0) {
			BusDirections busDirections = getBusDirections(selectedDirectionIds);
			String[] items = new String[selectedDirectionIds.size()];
			this.orderedItemsDirectionId = new String[items.length];
			int i = 0;
			for (int mainDistinctDirectionStringCode : busDirections.getMainDistinctDirections()) {
				String direction = this.context.getResources().getString(mainDistinctDirectionStringCode);

				for (FullBusDirection subDirection : busDirections.getSubDirections(mainDistinctDirectionStringCode)) {
					items[i] = direction + " - " + this.context.getResources().getString(subDirection.getSubDirectionId());
					this.orderedItemsDirectionId[i] = subDirection.getDirection();
					i++;
				}
			}
			return items;
		} else {
			throw new WrongBusLineNumberException();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(" + which + ")");
		if (which == -2) {
			dialog.dismiss(); // CANCEL
		} else {
			this.listener.showNewLine(this.lineNumber, this.orderedItemsDirectionId[which]);
		}
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
	 * Return the bus line directions.
	 * @param directionIds the bus line directions IDs
	 * @return the bus line directions
	 */
	private BusDirections getBusDirections(final List<StmStore.BusLineDirection> directionIds) {
		// TODO Still need BusDirections class ?
		BusDirections busDirections = new BusDirections();
		for (StmStore.BusLineDirection directionId : directionIds) {
			List<Integer> busLineDirectionStringCodes = Utils.getBusLineDirectionStringIdFromId(directionId.getId());
			// Main Direction (North/South/West/East)
			int mainDirectionStringCode = busLineDirectionStringCodes.get(0);
			if (!busDirections.containsMainDirectionId(mainDirectionStringCode)) {
				busDirections.addMainDirectionId(mainDirectionStringCode);
			}
			// Sub Direction
			if (busLineDirectionStringCodes.size() >= 2) {
				int subDirectionStringCode = busLineDirectionStringCodes.get(1);
				busDirections.putSubDirectionId(mainDirectionStringCode, subDirectionStringCode, directionId.getId());
			} else {
				// add regular
				int subDirectionStringCode = R.string.regular_route;
				busDirections.putSubDirectionId(mainDirectionStringCode, subDirectionStringCode, directionId.getId());
			}
		}
		return busDirections;
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

	/**
	 * Use to manage the direction (TODO better ?)
	 * @author Mathieu Méa
	 */
	private class BusDirections {

		/**
		 * The main (distinct) bus line directions.
		 */
		Map<Integer, List<FullBusDirection>> mainDistinctDirections;

		/**
		 * @param mainDirectionId the main direction ID
		 * @return true if this direction is already in the list
		 */
		public boolean containsMainDirectionId(int mainDirectionId) {
			return getDirections().keySet().contains(mainDirectionId);
		}

		/**
		 * @return the bus line directions
		 */
		private Map<Integer, List<FullBusDirection>> getDirections() {
			if (this.mainDistinctDirections == null) {
				this.mainDistinctDirections = new HashMap<Integer, List<FullBusDirection>>();
			}
			return this.mainDistinctDirections;
		}

		/**
		 * Add a main bus line direction.
		 * @param mainDirectionId the main direction ID to add
		 */
		public void addMainDirectionId(int mainDirectionId) {
			getDirections().put(mainDirectionId, new ArrayList<FullBusDirection>());
		}

		/**
		 * Add a sub direction to the main direction.
		 * @param mainDirectionId the main direction ID
		 * @param subDirectionId the sub direction ID
		 * @param direction the direction string.
		 */
		public void putSubDirectionId(int mainDirectionId, int subDirectionId, String direction) {
			if (!containsMainDirectionId(mainDirectionId)) {
				this.addMainDirectionId(mainDirectionId);
			}
			if (!this.getDirections().get(mainDirectionId).contains(subDirectionId)) {
				this.getDirections().get(mainDirectionId).add(new FullBusDirection(subDirectionId, direction));
			}
		}

		/**
		 * @return the main (distinct) bus line directions.
		 */
		public Set<Integer> getMainDistinctDirections() {
			return this.getDirections().keySet();
		}

		/**
		 * Return the sub directions of a main direction.
		 * @param mainDirectionId the main direction ID
		 * @return the sub directions
		 */
		public List<FullBusDirection> getSubDirections(int mainDirectionId) {
			if (!containsMainDirectionId(mainDirectionId)) {
				this.addMainDirectionId(mainDirectionId);
			}
			return this.getDirections().get(mainDirectionId);
		}
	}

	/**
	 * This class represent a complete bus line direction.
	 * @author Mathieu Méa
	 */
	private class FullBusDirection {

		/**
		 * The sub direction ID
		 */
		private int subDirectionId;
		/**
		 * The direction name.
		 */
		private String direction;

		/**
		 * Default constructor.
		 * @param subDirectionId the sub direction ID
		 * @param direction the direction string
		 */
		public FullBusDirection(int subDirectionId, String direction) {
			this.subDirectionId = subDirectionId;
			this.direction = direction;
		}

		/**
		 * @return the sub direction ID
		 */
		public int getSubDirectionId() {
			return subDirectionId;
		}

		/**
		 * @return the direction string
		 */
		public String getDirection() {
			return direction;
		}
	}
}

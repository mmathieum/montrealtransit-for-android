package org.montrealtransit.android.dialog;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.SubwayLineInfo;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

/**
 * This class handle the subway line direction selection.
 * @author Mathieu Méa
 */
public class SubwayLineSelectDirection implements View.OnClickListener, SubwayLineSelectDirectionDialogListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineSelectDirection.class.getSimpleName();
	/**
	 * The context asking for the dialog.
	 */
	private Context context;
	/**
	 * The class who's going to handle the dialog response.
	 */
	private SubwayLineSelectDirectionDialogListener listener;
	/**
	 * The direction preferences.
	 */
	private String[] orderPref;
	/**
	 * The subway line.
	 */
	private StmStore.SubwayLine subwayLine;

	/**
	 * Default constructor that will launch a new activity.
	 * @param context the caller context
	 * @param subwayLineId the bus line number
	 */
	public SubwayLineSelectDirection(Context context, int subwayLineId) {
		MyLog.v(TAG, "SubwayLineSelectDirection(%s)", subwayLineId);
		this.context = context;
		this.listener = this;
		this.subwayLine = StmManager.findSubwayLine(context.getContentResolver(), subwayLineId);
	}

	/**
	 * This constructor allow the caller to specify which class will manage the answer of the dialog.
	 * @param context the caller context
	 * @param subwayLineId the line number
	 * @param listener the dialog listener
	 */
	public SubwayLineSelectDirection(Context context, int subwayLineId, SubwayLineSelectDirectionDialogListener listener) {
		MyLog.v(TAG, "SubwayLineSelectDirection(%s, listener)", subwayLineId);
		this.subwayLine = StmManager.findSubwayLine(context.getContentResolver(), subwayLineId);
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
	 * Show the dialog.
	 */
	public void showDialog() {
		MyLog.v(TAG, "showDialog()");
		getAlertDialog().show();
	}

	/**
	 * @return the dialog.
	 */
	private AlertDialog getAlertDialog() {
		MyLog.v(TAG, "getAlertDialog()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		String lineName = context.getString(SubwayUtils.getSubwayLineName(this.subwayLine.getNumber()));
		builder.setTitle(context.getString(R.string.select_subway_direction_and_name, lineName));
		builder.setSingleChoiceItems(getItems(), getCheckedItemFromPref(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MyLog.v(TAG, "onClick(%s)", which);
				dialog.dismiss(); // close the dialog
				int lineNumber = SubwayLineSelectDirection.this.subwayLine.getNumber();
				String orderPref = SubwayLineSelectDirection.this.orderPref[which];
				SubwayLineSelectDirection.this.listener.showNewSubway(lineNumber, orderPref);
			}
		});
		// CANCEL
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MyLog.v(TAG, "onClick(%s)", which);
				dialog.dismiss(); // close the dialog (do nothing)
			}
		});
		AlertDialog alert = builder.create();
		return alert;
	}

	/**
	 * @return the id of the checked choice
	 */
	private int getCheckedItemFromPref() {
		String sharedPreferences = Utils.getSharedPreferences(context,
		        UserPreferences.getPrefsSubwayStationsOrder(this.subwayLine.getNumber()),
		        UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
		if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)) {
			return 1;
		} else if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return 2;
		} else {
			return 0;
		}
	}

	/**
	 * @return the items to be displayed
	 */
	private String[] getItems() {
		MyLog.v(TAG, "getItems()");
		StmStore.SubwayStation firstSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(
		        this.context.getContentResolver(), this.subwayLine.getNumber(),
		        StmStore.SubwayStation.NATURAL_SORT_ORDER);
		// MyTrace.d(TAG, "First station: " + firstSubwayStationDirection.getName());
		StmStore.SubwayStation lastSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(
		        this.context.getContentResolver(), this.subwayLine.getNumber(),
		        StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC);
		// MyTrace.d(TAG, "Last station: " + lastSubwayStationDirection.getName());

		String[] items = new String[3];
		orderPref = new String[3];

		orderPref[0] = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_AZ;
		items[0] = this.context.getString(R.string.alphabetical_order);
		orderPref[1] = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL;
		items[1] = firstSubwayStationDirection.getName();
		orderPref[2] = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC;
		items[2] = lastSubwayStationDirection.getName();
		return items;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewSubway(int subwayLineId, String orderPref) {
		MyLog.v(TAG, "showNewSubway(%s, %s)", subwayLineId, orderPref);
		Intent mIntent = new Intent(this.context, SubwayLineInfo.class);
		mIntent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, String.valueOf(subwayLineId));
		mIntent.putExtra(SubwayLineInfo.EXTRA_ORDER_PREF, orderPref);
		this.context.startActivity(mIntent);
	}
}

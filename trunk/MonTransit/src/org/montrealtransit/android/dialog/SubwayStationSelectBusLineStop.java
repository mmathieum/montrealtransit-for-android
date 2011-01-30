package org.montrealtransit.android.dialog;

import java.util.List;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * This class manage the selection from a subway station bus line to help the user select the bus stop.
 * @author Mathieu Méa
 */
public class SubwayStationSelectBusLineStop implements View.OnClickListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayStationSelectBusLineStop.class.getSimpleName();

	/**
	 * The activity context calling the selector.
	 */
	private Context context;
	/**
	 * The subway station ID.
	 */
	private String subwayStationId;
	/**
	 * The bus line number.
	 */
	private String busLineNumber;
	/**
	 * The cursor use by the dialog.
	 */
	private Cursor cursor;
	/**
	 * The dialog.
	 */
	private AlertDialog dialog;

	/**
	 * Default constructor.
	 * @param context the activity context
	 * @param subwayStationId the subway station ID
	 * @param busLineNumber the bus line number
	 */
	public SubwayStationSelectBusLineStop(Context context, String subwayStationId, String busLineNumber) {
		this.context = context;
		this.subwayStationId = subwayStationId;
		this.busLineNumber = busLineNumber;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick()");
		List<StmStore.BusStop> busStops = StmManager.findSubwayStationBusLineStopsList(
		        this.context.getContentResolver(), this.subwayStationId, this.busLineNumber);
		// IF there is only 1 bus stop DO
		if (busStops.size() == 1) {
			// show the bus stop
			showBusStop(busStops.get(0).getCode(), this.busLineNumber);
		} else {
			// show the select dialog
			showDialog();
		}
	}

	/**
	 * Show the dialog.
	 */
	public void showDialog() {
		MyLog.v(TAG, "showDialog()");
		getAlertDialog().show();
	}

	/**
	 * @return the dialog
	 */
	private AlertDialog getAlertDialog() {
		MyLog.v(TAG, "getAlertDialog()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		// title
		String title = this.context.getString(R.string.select_bus_line_stop_and_number, this.busLineNumber);
		builder.setTitle(title);
		// bus stops list view
		ListView listView = new ListView(this.context);
		listView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		listView.setAdapter(getAdapter());
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				if (id > 0) {
					showBusStop(String.valueOf(id), SubwayStationSelectBusLineStop.this.busLineNumber);
				}
			}
		});
		builder.setView(listView);
		// cancel button
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MyLog.v(TAG, "onClick(%s)", which);
				// CANCEL
				dialog.dismiss();
				closeCursor();
			}
		});
		this.dialog = builder.create();
		return dialog;
	}

	/**
	 * @return the bus stops list adapter
	 */
	private SimpleCursorAdapter getAdapter() {
		this.cursor = StmManager.findSubwayStationBusLineStops(this.context.getContentResolver(), this.subwayStationId,
		        this.busLineNumber);
		String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE,
		        StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, };
		int[] to = new int[] { R.id.stop_code, R.id.label, R.id.direction_main };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this.context, R.layout.dialog_bus_stop_select_list_item,
		        cursor, from, to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				MyLog.v(TAG, "setViewValue(%s, %s)", view.getId(), columnIndex);
				switch (view.getId()) {
				case R.id.label:
					String busStopPlace = BusUtils.cleanBusStopPlace(cursor.getString(columnIndex));
					((TextView) view).setText(busStopPlace);
					return true;
				case R.id.direction_main:
					int simpleBusLineDirectionId = BusUtils.getBusLineDirectionStringIdFromId(
					        cursor.getString(columnIndex)).get(0);
					((TextView) view).setText(simpleBusLineDirectionId);
					return true;
				default:
					return false;
				}
			}
		});
		return adapter;
	}

	/**
	 * Show the bus stop
	 * @param stopCode the bus stop code
	 * @param busLineNumber the bus stop line number
	 */
	private void showBusStop(String stopCode, String busLineNumber) {
		if (this.dialog != null) {
			this.dialog.dismiss();
		}
		closeCursor();
		Intent intent = new Intent(this.context, BusStopInfo.class);
		intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, stopCode);
		intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busLineNumber);
		this.context.startActivity(intent);
	}

	/**
	 * Close the cursor.
	 */
	private void closeCursor() {
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
	}
}

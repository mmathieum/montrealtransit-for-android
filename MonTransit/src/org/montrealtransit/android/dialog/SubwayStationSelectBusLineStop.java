package org.montrealtransit.android.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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
	private Activity context;
	/**
	 * The subway station ID.
	 */
	private String subwayStationId;
	/**
	 * The bus line number.
	 */
	private String busLineNumber;
	/**
	 * The bus line name.
	 */
	private String busLineName;
	/**
	 * The bus line type.
	 */
	private String busLineType;
	/**
	 * The dialog.
	 */
	private AlertDialog dialog;
	/**
	 * The current list of bus stops.
	 */
	private List<BusStop> busStops;

	/**
	 * Default constructor.
	 * @param context the activity context
	 * @param subwayStationId the subway station ID
	 * @param busLineNumber the bus line number
	 */
	public SubwayStationSelectBusLineStop(Activity context, String subwayStationId, String busLineNumber, String busLineName, String busLineType) {
		MyLog.v(TAG, "SubwayStationSelectBusLineStop(%s, %s)", subwayStationId, busLineNumber);
		this.context = context;
		this.subwayStationId = subwayStationId;
		this.busLineNumber = busLineNumber;
		this.busLineName = busLineName;
		this.busLineType = busLineType;
	}

	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick()");
		this.busStops = StmManager.findSubwayStationBusLineStopsList(this.context.getContentResolver(), this.subwayStationId, this.busLineNumber);
		// IF there is not bus stop DO
		if (this.busStops == null || this.busStops.size() == 0) {
			// TODO show error message?
			// IF there is only 1 bus stop DO
		} else if (this.busStops.size() == 1) {
			// show the bus stop
			showBusStop(this.busStops.get(0).getCode(), this.busStops.get(0).getPlace(), this.busLineNumber, this.busLineName, this.busLineType);
		} else {
			// show the select dialog
			showAlertDialog();
		}
	}

	/**
	 * @return the dialog
	 */
	private void showAlertDialog() {
		MyLog.v(TAG, "getAlertDialog()");
		// title
		String title = this.context.getString(R.string.select_bus_line_stop_and_number, this.busLineNumber);
		// bus stops list view
		ListView listView = new ListView(this.context);
		listView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		listView.setAdapter(getListAdapter());
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				if (SubwayStationSelectBusLineStop.this.busStops != null) {
					BusStop busStop = SubwayStationSelectBusLineStop.this.busStops.get(position);
					if (busStop != null) {
						showBusStop(busStop.getCode(), busStop.getPlace(), SubwayStationSelectBusLineStop.this.busLineNumber,
								SubwayStationSelectBusLineStop.this.busLineName, SubwayStationSelectBusLineStop.this.busLineType);
					}
				}
			}
		});
		this.dialog = new AlertDialog.Builder(this.context).setTitle(title).setView(listView)
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyLog.v(TAG, "onClick(%s)", which);
						// CANCEL
						dialog.dismiss();
					}
				}).create();
		this.dialog.show();
	}

	/**
	 * @return the bus stops list adapter
	 */
	private ListAdapter getListAdapter() {
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		for (BusStop busStop : this.busStops) {
			Map<String, String> busStopMap = new HashMap<String, String>();
			busStopMap.put(StmStore.BusStop.STOP_CODE, busStop.getCode());
			busStopMap.put(StmStore.BusStop.STOP_PLACE, BusUtils.cleanBusStopPlace(busStop.getPlace()));
			String direction = this.context.getString(BusUtils.getBusLineSimpleDirection(busStop.getDirectionId()));
			busStopMap.put(StmStore.BusStop.STOP_DIRECTION_ID, direction);
			data.add(busStopMap);
		}
		String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_DIRECTION_ID };
		int[] to = new int[] { R.id.stop_code, R.id.label, R.id.direction_main };
		return new SimpleAdapter(this.context, data, R.layout.dialog_bus_stop_select_list_item, from, to);
	}

	/**
	 * Show the bus stop
	 * @param stopCode the bus stop code
	 * @param busLineNumber the bus stop line number
	 */
	private void showBusStop(String stopCode, String stopPlace, String busLineNumber, String busLineName, String busLineType) {
		MyLog.v(TAG, "showBusStop(%s,%s,%s,%s,%s)", stopCode, stopPlace, busLineNumber, busLineName, busLineType);
		if (this.dialog != null) {
			this.dialog.dismiss();
		}
		Intent intent = new Intent(this.context, BusStopInfo.class);
		intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, stopCode);
		intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, stopPlace);
		intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busLineNumber);
		intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busLineName);
		intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busLineType);
		this.context.startActivity(intent);
	}
}

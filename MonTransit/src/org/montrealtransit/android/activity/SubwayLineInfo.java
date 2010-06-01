package org.montrealtransit.android.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.dialog.SubwayLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, OnItemClickListener,
        ViewBinder, FilterQueryProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineInfo.class.getSimpleName();

	/**
	 * The extra for the subway line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra for the subway station display order.
	 */
	public static final String EXTRA_ORDER_ID = "extra_order_id";
	/**
	 * The subway line.
	 */
	private StmStore.SubwayLine subwayLine;
	/**
	 * The subway line direction.
	 */
	private StmStore.SubwayStation lastSubwayStation;
	/**
	 * The subway station list order ID.
	 */
	private String orderId;

	/**
	 * Store the other subway line for the subway stations
	 */
	private Map<String, List<String>> subwayStationOtherLines;

	/**
	 * The cursor used to display the subway station.
	 */
	private Cursor cursor;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_info);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
		((ListView) findViewById(R.id.list)).setOnItemClickListener(this);
		// get info from the intent.
		int subwayLineId = Integer.valueOf(Utils.getSavedStringValue(this.getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_LINE_NUMBER));
		this.orderId = Utils.getSavedStringValue(this.getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_ORDER_ID);
		this.subwayLine = StmManager.findSubwayLine(getContentResolver(), subwayLineId);
		// refresh the UI
		refreshSubwayLineInfo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
		refreshSubwayStationsList();
	}

	/**
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		refreshSubwayLineInfo();
		refreshSubwayStationsList();
	}

	/**
	 * Refresh the subway line info.
	 */
	private void refreshSubwayLineInfo() {
		// subway line name
		((TextView) findViewById(R.id.line_name)).setText(Utils.getSubwayLineName(subwayLine.getNumber()));
		((ImageView) findViewById(R.id.subway_img)).setImageResource(Utils.getSubwayLineImg(subwayLine.getNumber()));

		// subway line direction
		this.lastSubwayStation = StmManager.findSubwayLineLastSubwayStation(this.getContentResolver(), this.subwayLine.getNumber(), this.orderId);
		String separatorText = this.getResources().getString(R.string.subway_stations) + " (" + getDirectionText() + ")";
		((TextView) findViewById(R.id.subway_line_station_string)).setText(separatorText);
		SubwayLineSelectDirection selectSubwayStationOrder = new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this);
		((TextView) findViewById(R.id.subway_line_station_string)).setOnClickListener(selectSubwayStationOrder);
	}

	/**
	 * @return the direction test (the direction(station) or the A-Z order)
	 */
	private String getDirectionText() {
		if (this.orderId.equals(StmStore.SubwayStation.NATURAL_SORT_ORDER) || this.orderId.equals(StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC)) {
			return this.getResources().getString(R.string.direction) + " " + this.lastSubwayStation.getName();
		} else {
			// DEFAULT : StmStore.SubwayLine.DEFAULT_SORT_ORDER A-Z order
			this.orderId = StmStore.SubwayStation.DEFAULT_SORT_ORDER;
			return this.getResources().getString(R.string.alphabetical_order);
		}
	}

	/**
	 * Refresh the subway stations list.
	 */
	private void refreshSubwayStationsList() {
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter());
		// store other subway lines IDs for the the subway stations with multiple subway lines
		this.subwayStationOtherLines = new HashMap<String, List<String>>();
		List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(this.getContentResolver(), this.subwayLine.getNumber(), this.orderId);
		for (SubwayStation subwayStation : subwayStationsList) {
			List<String> otherSubwayLinesIds = Utils.extractSubwayLineNumbers(StmManager.findSubwayStationLinesList(getContentResolver(), subwayStation.getId()));
			if (otherSubwayLinesIds!=null) {
				otherSubwayLinesIds.remove(String.valueOf(this.subwayLine.getNumber()));
				this.subwayStationOtherLines.put(subwayStation.getId(), otherSubwayLinesIds);
			}
		}
	}
	
	/**
	 * @return the subway station list adapter.
	 */
	private SimpleCursorAdapter getAdapter() {
		MyLog.v(TAG, "getAdapter()");
		this.cursor = StmManager.findSubwayLineStations(this.getContentResolver(), this.subwayLine.getNumber(), this.orderId);
		String[] from = new String[] { StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_ID,
		        StmStore.SubwayStation.STATION_NAME };
		int[] to = new int[] { R.id.subway_img_1, R.id.subway_img_2, R.id.subway_img_3, R.id.station_name };
		SimpleCursorAdapter subwayStations = new SimpleCursorAdapter(this, R.layout.subway_line_info_stations_list_item, this.cursor, from, to);
		subwayStations.setViewBinder(this);
		subwayStations.setFilterQueryProvider(this);
		return subwayStations;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor runQuery(CharSequence constraint) {
		MyLog.v(TAG, "runQuery(" + constraint + ")");
		return StmManager.searchSubwayLineStations(this.getContentResolver(), this.subwayLine.getNumber(),
		        this.orderId, constraint.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		MyLog.v(TAG, "setViewValue(" + view.getId() + ", " + columnIndex + ")");
		if (view.getId() == R.id.subway_img_1 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			((ImageView) view).setImageResource(Utils.getSubwayLineImg(this.subwayLine.getNumber()));
			return true;
		} else if (view.getId() == R.id.subway_img_2 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			if (this.subwayStationOtherLines.get(subwayStationID).size() > 0) {
				((ImageView) view).setVisibility(View.VISIBLE);
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(Integer.valueOf(this.subwayStationOtherLines.get(subwayStationID).get(0))));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.subway_img_3 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			if (this.subwayStationOtherLines.get(subwayStationID).size() > 1) {
				((ImageView) view).setVisibility(View.VISIBLE);
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(Integer.valueOf(this.subwayStationOtherLines.get(subwayStationID).get(1))));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Menu for changing the direction of the bus line.
	 */
	private static final int MENU_CHANGE_DIRECTION = Menu.FIRST;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 1;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuDirection = menu.add(0, MENU_CHANGE_DIRECTION, 0, R.string.change_direction);
		menuDirection.setIcon(android.R.drawable.ic_menu_compass);
		MenuItem menuPref = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menuPref.setIcon(android.R.drawable.ic_menu_preferences);
		MenuItem menuAbout = menu.add(0, MENU_ABOUT, Menu.NONE, R.string.menu_about);
		menuAbout.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_CHANGE_DIRECTION:
			SubwayLineSelectDirection select = new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this);
			select.showDialog();
			return true;
		case MENU_PREFERENCES:
            startActivity(new Intent(this, UserPreferences.class));
            return true;
		case MENU_ABOUT:
        	Utils.showAboutDialog(this);
        	return true;
		default:
			MyLog.d(TAG, "Unknow menu id: " + item.getItemId() + ".");
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		if (id > 0) {
			Intent intent = new Intent(this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, String.valueOf(id));
			startActivity(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewSubway(int newLineNumber, String newOrderId) {
		MyLog.v(TAG, "showNewSubway(" + newLineNumber + ", " + newOrderId + ")");
		if (!(this.subwayLine.getNumber() == newLineNumber) || !this.orderId.equals(newOrderId)) {
			MyLog.v(TAG, "new subway");
			this.orderId = newOrderId;
			this.subwayLine = StmManager.findSubwayLine(getContentResolver(), newLineNumber);
			refreshAll();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor!=null) {this.cursor.close(); }
	    super.onDestroy();
	}
}

package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.BusLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
 * This activity display information about a bus line.
 * @author Mathieu Méa
 */
public class BusLineInfo extends Activity implements ViewBinder, BusLineSelectDirectionDialogListener,
        OnItemClickListener, FilterQueryProvider {

	/**
	 * The current bus line.
	 */
	private StmStore.BusLine busLine;
	/**
	 * The bus line direction.
	 */
	private StmStore.BusLineDirection busLineDirection;
	/**
	 * The cursor used to display the bus line stops.
	 */
	private Cursor cursor;
	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineInfo.class.getSimpleName();
	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra ID for the bus line direction ID.
	 */
	public static final String EXTRA_LINE_DIRECTION_ID = "extra_line_direction_id";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
		((ListView) findViewById(R.id.list)).setOnItemClickListener(this);
		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(this.getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NUMBER);
		String lineDirectionId = Utils.getSavedStringValue(this.getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_DIRECTION_ID);
		showNewLine(lineNumber, lineDirectionId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewLine(String newLineNumber, String newDirectionId) {
		MyLog.v(TAG, "showNewLine(" + newLineNumber + ", " + newDirectionId + ")");
		if ((this.busLine == null || this.busLineDirection == null)
		        || (!this.busLine.getNumber().equals(newLineNumber) || !this.busLineDirection.getId().equals(newDirectionId))) {
			MyLog.v(TAG, "show new bus line.");
			this.busLine = StmManager.findBusLine(this.getContentResolver(), newLineNumber);
			this.busLineDirection = StmManager.findBusLineDirection(this.getContentResolver(), newDirectionId);
			refreshAll();
		}
	}

	/**
	 * Refresh ALL the UI.
	 */
	private void refreshAll() {
		refreshBusLineInfo();
		refreshBusStopList();
	}

	/**
	 * Refresh the bus line info UI.
	 */
	private void refreshBusLineInfo() {
		// bus line number
		((TextView) findViewById(R.id.line_number)).setText(this.busLine.getNumber());

		// bus line name
		((TextView) findViewById(R.id.line_name)).setText(this.busLine.getName());

		// bus line type
		((ImageView) findViewById(R.id.bus_type)).setImageResource(Utils.getBusLineTypeImgFromType(this.busLine.getType()));
		
		// bus line direction
		BusLineSelectDirection selectBusLineDirection = new BusLineSelectDirection(this, this.busLine.getNumber(), this);
		((TextView) findViewById(R.id.direction_string)).setOnClickListener(selectBusLineDirection);
		List<Integer> busLineDirection = Utils.getBusLineDirectionStringIdFromId(this.busLineDirection.getId());
		((TextView) findViewById(R.id.direction_main)).setText(getResources().getString(busLineDirection.get(0)));
		((TextView) findViewById(R.id.direction_main)).setOnClickListener(selectBusLineDirection);
		// bus line direction details
		if (busLineDirection.size() >= 2) {
			((TextView) findViewById(R.id.direction_detail)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.direction_detail)).setText(getResources().getString(busLineDirection.get(1)));
			((TextView) findViewById(R.id.direction_detail)).setOnClickListener(selectBusLineDirection);
		}
	}

	/**
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopList() {
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter());
	}

	/**
	 * Return the bus stops list for this bus line number and direction.
	 * @return the bus stops list adapter.
	 */
	private SimpleCursorAdapter getAdapter() {
		this.cursor = StmManager.findBusLineStops(this.getContentResolver(), this.busLine.getNumber(),
		        this.busLineDirection.getId());
		String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE,
				StmStore.BusStop.STATION_NAME, StmStore.BusStop.STOP_SUBWAY_STATION_ID };
		int[] to = new int[] { R.id.stop_code, R.id.place, R.id.station_name, R.id.subway_img };
		SimpleCursorAdapter busStops = new SimpleCursorAdapter(this, R.layout.bus_line_info_stops_list_item,
		        this.cursor, from, to);
		busStops.setViewBinder(this);
		busStops.setFilterQueryProvider(this);
		return busStops;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor runQuery(CharSequence constraint) {
		return StmManager.searchBusLineStops(this.getContentResolver(), this.busLine.getNumber(), this.busLineDirection
		        .getId(), constraint.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (view.getId() == R.id.subway_img) {
			if (cursor.getInt(cursor.getColumnIndex(StmStore.BusStop.STOP_SUBWAY_STATION_ID)) != 0) {
				((ImageView) view).setVisibility(View.VISIBLE);
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.place) {
			((TextView) view).setText(Utils.cleanBusStopPlace(cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE))));
			return true;
		} else {
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
			Intent intent = new Intent(this, BusStopInfo.class);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, String.valueOf(id));
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, this.busLine.getNumber());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_DIRECTION, this.busLineDirection.getId());
			startActivity(intent);
		}
	}

	/**
	 * The menu item to show the map
	 */
	private static final int MENU_SEE_MAP = Menu.FIRST;
	/**
	 * The menu item to select the bus line direction.
	 */
	private static final int MENU_CHANGE_DIRECTION = Menu.FIRST + 1;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 2;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 3;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuMap = menu.add(0, MENU_SEE_MAP, 0, R.string.see_bus_line_plan);
		menuMap.setIcon(R.drawable.planibu);
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
		case MENU_SEE_MAP:
			String url = "http://www.stm.info/bus/images/PLAN/lign-" + this.busLine.getNumber() + ".gif";
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			break;
		case MENU_CHANGE_DIRECTION:
			BusLineSelectDirection select = new BusLineSelectDirection(this, this.busLine.getNumber(), this);
			select.showDialog();
			break;
		case MENU_PREFERENCES:
            startActivity(new Intent(this, UserPreferences.class));
	        break;
		case MENU_ABOUT:
        	Utils.showAboutDialog(this);
        	break;
		}
		return false;
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

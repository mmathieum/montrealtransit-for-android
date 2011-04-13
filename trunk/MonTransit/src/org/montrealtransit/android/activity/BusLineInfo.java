package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
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

/**
 * This activity display information about a bus line.
 * @author Mathieu Méa
 */
public class BusLineInfo extends Activity implements BusLineSelectDirectionDialogListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BusLine";

	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra ID for the bus line direction ID.
	 */
	public static final String EXTRA_LINE_DIRECTION_ID = "extra_line_direction_id";

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
	 * The line stops list view.
	 */
	private ListView list;
	/**
	 * The line number text view.
	 */
	private TextView lineNumberTv;
	/**
	 * The line name text view.
	 */
	private TextView lineNameTv;
	/**
	 * The line type image.
	 */
	private ImageView lineTypeImg;
	/**
	 * The line stops title text view.
	 */
	private TextView lineStopsTv;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);

		this.list = (ListView) findViewById(R.id.list);
		this.lineNumberTv = (TextView) findViewById(R.id.line_number);
		this.lineNameTv = (TextView) findViewById(R.id.line_name);
		this.lineTypeImg = (ImageView) findViewById(R.id.bus_type);
		this.lineStopsTv = (TextView) findViewById(R.id.bus_line_stop_string);

		this.list.setEmptyView(findViewById(R.id.list_empty));
		this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				// MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
				if (id > 0) {
					Intent intent = new Intent(BusLineInfo.this, BusStopInfo.class);
					intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, String.valueOf(id));
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, BusLineInfo.this.busLine.getNumber());
					startActivity(intent);
				}
			}
		});
		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NUMBER);
		String lineDirectionId = Utils.getSavedStringValue(getIntent(), savedInstanceState,
		        BusLineInfo.EXTRA_LINE_DIRECTION_ID);
		showNewLine(lineNumber, lineDirectionId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewLine(String newLineNumber, String newDirectionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", newLineNumber, newDirectionId);
		if ((this.busLine == null || this.busLineDirection == null)
		        || (!this.busLine.getNumber().equals(newLineNumber) || !this.busLineDirection.getId().equals(
		                newDirectionId))) {
			MyLog.d(TAG, "show new bus line.");
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
		this.lineNumberTv.setText(this.busLine.getNumber());
		// bus line name
		this.lineNameTv.setText(this.busLine.getName());
		// bus line type
		this.lineTypeImg.setImageResource(BusUtils.getBusLineTypeImgFromType(this.busLine.getType()));

		// bus line direction
		this.lineStopsTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSelectDirectionDialog(null);
			}
		});
		List<Integer> busLineDirection = BusUtils.getBusLineDirectionStringIdFromId(this.busLineDirection.getId());
		String direction = getString(busLineDirection.get(0));
		if (busLineDirection.size() >= 2) {
			direction += " " + getString(busLineDirection.get(1));
		}
		this.lineStopsTv.setText(getString(R.string.bus_stops_short_and_direction, direction));
	}

	/**
	 * Show the bus line dialog to select direction.
	 */
	public void showSelectDirectionDialog(View v) {
		new BusLineSelectDirection(this, this.busLine.getNumber(), this).showDialog();
	}

	/**
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopList() {
		this.list.setAdapter(getAdapter());
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
		busStops.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()) {
				case R.id.subway_img:
					view.setVisibility(cursor.getInt(columnIndex) != 0 ? View.VISIBLE : View.GONE);
					return true;
				case R.id.place:
					String cleanBusStopPlace = BusUtils.cleanBusStopPlace(cursor.getString(columnIndex));
					((TextView) view).setText(cleanBusStopPlace);
					return true;
				default:
					return false;
				}
			}
		});
		busStops.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				String lineNumber = BusLineInfo.this.busLine.getNumber();
				String directionID = BusLineInfo.this.busLineDirection.getId();
				return StmManager.searchBusLineStops(BusLineInfo.this.getContentResolver(), lineNumber, directionID,
				        constraint.toString());
			}
		});
		startManagingCursor(cursor);
		return busStops;
	}

	/**
	 * Show STM bus line map.
	 * @param v the view (not used)
	 */
	public void showSTMBusLineMap(View v) {
		String url = "http://www.stm.info/bus/images/PLAN/lign-" + this.busLine.getNumber() + ".gif";
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_line_info_menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map:
			showSTMBusLineMap(null);
			return true;
		case R.id.direction:
			showSelectDirectionDialog(null);
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		super.onDestroy();
	}
}

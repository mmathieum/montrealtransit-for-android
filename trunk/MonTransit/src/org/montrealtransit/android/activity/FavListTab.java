package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.DataStore.Fav;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * This activity list the favorite bus stops.
 * @author Mathieu Méa
 */
public class FavListTab extends Activity implements ViewBinder, OnItemClickListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = FavListTab.class.getSimpleName();
	
	/**
	 * The favorite bus stops list.
	 */
	private List<DataStore.Fav> lastFavList;
	
	/**
	 * The cursor used to display the bus stops.
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
		setContentView(R.layout.fav_list_tab);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
		((ListView) findViewById(R.id.list)).setOnItemClickListener(this);
		((ListView) findViewById(R.id.list)).setOnCreateContextMenuListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
		refreshDataIfNecessary();
	}
	
	/**
	 * Refresh the data If the favorite list have change.
	 */
	private void refreshDataIfNecessary() {
		// check if favorite list have changed.
		// TODO check more than the size
		if (this.lastFavList == null || this.lastFavList.size() != Utils.getCursorSize(DataManager.findAllFavs(this.getContentResolver()))) {
			// update the list
			forceRefresh();
		}
	}

	/**
	 * Return the favorite bus stops adapter.
	 * @param favList the favorite elements
	 * @return the favorite bus stops.
	 */
	private ListAdapter getAdapter(List<DataStore.Fav> favList) {
		MyLog.v(TAG, "getAdapter(" + Utils.getListSize(favList) + ")");
		if (Utils.getListSize(favList) > 0) {
			this.cursor = StmManager.findBusStopsExtended(this.getContentResolver(), Utils.extractBusStopIDsFromFavList(favList));
			String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_LINE_NUMBER,
			        StmStore.BusStop.LINE_NAME, StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID };
			int[] to = new int[] { R.id.stop_code, R.id.label, R.id.line_number, R.id.line_name, R.id.line_direction };
			SimpleCursorAdapter busStops = new SimpleCursorAdapter(this, R.layout.fav_list_tab_bus_stop_item, this.cursor, from, to);
			busStops.setViewBinder(this);
			return busStops;
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (view.getId() == R.id.line_direction && columnIndex == cursor.getColumnIndex(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID)) {
			String simpleDirectionId = cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID));
			((TextView) view).setText(Utils.getBusLineDirectionStringIdFromId(simpleDirectionId).get(0));
			return true;
		} else if (view.getId() == R.id.label && columnIndex == cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE)) {
			String busStopPlace = cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE));
			((TextView) view).setText(Utils.cleanBusStopPlace(busStopPlace));
			return true;
		}
		return false;
	}

	/**
	 * The activity to show the bus stop info.
	 */
	private static final int ACTIVITY_VIEW_BUS_STOP = 1;
	
	/**
	 * The line number index in the the view.
	 */
	private static final int LINE_NUMBER_VIEW_INDEX = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		if (id > 0) {
			Intent i = new Intent(this, BusStopInfo.class);
			TextView lineNumberTextView = (TextView) ((RelativeLayout) v).getChildAt(LINE_NUMBER_VIEW_INDEX);
			i.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, lineNumberTextView.getText().toString());
			i.putExtra(BusStopInfo.EXTRA_STOP_CODE, String.valueOf(id));
			startActivityForResult(i, ACTIVITY_VIEW_BUS_STOP);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		MyLog.v(TAG, "onCreateContextMenu(" + v.getId() + ")");
		menu.setHeaderTitle(R.string.favorite); // TODO add favorite info in the title
		menu.add(Menu.NONE, 0, Menu.NONE, R.string.remove_fav);
		// TODO see bus line / directions ? / open in m.stm.info
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		// the stop code
		String stopCode = String.valueOf(menuInfo.id);
		// find the selected favorite layout
		RelativeLayout selectedFavoriteLayout = (RelativeLayout) ((ListView) findViewById(R.id.list)).getChildAt(menuInfo.position);
		// find the line number text view
		TextView lineNumberTextView = (TextView) selectedFavoriteLayout.getChildAt(LINE_NUMBER_VIEW_INDEX);
		// find the line number
		String lineNumber = lineNumberTextView.getText().toString();
		// find the favorite to delete
		Fav findFav = DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, stopCode, lineNumber);
		// delete the favorite
		DataManager.deleteFav(this.getContentResolver(), findFav.getId());
		// refresh the UI
		forceRefresh();
		return super.onContextItemSelected(item);
	}

	/**
	 * Force the refresh of the favorite bus stops list.
	 */
	private void forceRefresh() {
		this.lastFavList = DataManager.findAllFavsList(this.getContentResolver());
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter(this.lastFavList));
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

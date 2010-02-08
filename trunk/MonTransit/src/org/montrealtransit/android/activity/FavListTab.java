package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

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
	 * Return the favorite bus stops adapter.
	 * @param favList the favorite elements
	 * @return the favorite bus stops.
	 */
	private ListAdapter getAdapter(List<DataStore.Fav> favList) {
		MyLog.v(TAG, "getAdapter(" + Utils.getListSize(favList) + ")");
		if (Utils.getListSize(favList) > 0) {
			Cursor cursor = StmManager.findBusStopsExtended(this.getContentResolver(), favList);
			String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_LINE_NUMBER,
			        StmStore.BusStop.LINE_NAME, StmStore.BusStop.STOP_DIRECTION_ID };
			int[] to = new int[] { R.id.stop_code, R.id.label, R.id.line_number, R.id.line_name, R.id.line_direction };
			SimpleCursorAdapter busStops = new SimpleCursorAdapter(this, R.layout.fav_list_tab_bus_stop_item, cursor, from, to);
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
		if (view.getId() == R.id.line_direction && columnIndex == cursor.getColumnIndex(StmStore.BusStop.STOP_DIRECTION_ID)) {
			((TextView) view).setText(getResources().getString(
			        Utils.getBusLineDirectionStringIdFromId(cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_DIRECTION_ID))).get(0)));
			return true;
		} else if (view.getId() == R.id.label && columnIndex == cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE)) {
			((TextView) view).setText(Utils.cleanBusStopPlace(cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE))));
			return true;
		}
		return false;
	}

	/**
	 * The activity to show the bus stop info.
	 */
	private static final int ACTIVITY_VIEW_BUS_STOP = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		if (id > 0) {
			Intent i = new Intent(this, BusStopInfo.class);
			i.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, ((TextView) ((RelativeLayout) v).getChildAt(2)).getText().toString());
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
		menu.setHeaderTitle(R.string.favorite); // TODO add fav info in the title
		menu.add(Menu.NONE, 0, Menu.NONE, R.string.remove_fav);
		// TODO see bus line / directions ? / open in m.stm.info
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		String stopCode = String.valueOf(menuInfo.id);
		String lineNumber = ((TextView) ((RelativeLayout) ((ListView) findViewById(R.id.list)).getChildAt(menuInfo.position)).getChildAt(2)).getText()
		        .toString();
		DataManager.deleteFav(this.getContentResolver(), DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, stopCode,
		        lineNumber).getId());
		forceRefresh();
		return super.onContextItemSelected(item);
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
	 * Force the refresh of the favorite bus stops list.
	 */
	private void forceRefresh() {
		this.lastFavList = DataManager.findAllFavsList(this.getContentResolver());
		final ListView lvFavs = (ListView) findViewById(R.id.list);
		lvFavs.setAdapter(getAdapter(this.lastFavList));
	}
}

package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * This activity shows the search results.
 * @author Mathieu Méa
 */
public class SearchResult extends ListActivity {

	/**
	 * The log tag.
	 */
	private static final String TAG = SearchResult.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SearchResult";

	/**
	 * The bus line number index in the the view.
	 */
	private static final int LINE_NUMBER_VIEW_INDEX = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.search_result);

		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				if (id > 0) {
					Intent intent = new Intent(SearchResult.this, BusStopInfo.class);
					TextView lineNumberTextView = (TextView) ((RelativeLayout) v).getChildAt(LINE_NUMBER_VIEW_INDEX);
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, lineNumberTextView.getText().toString());
					intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, String.valueOf(id));
					startActivity(intent);
				}
			}
		});
		processIntent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		setIntent(intent);
		processIntent();
		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * Process the intent of the activity.
	 */
	private void processIntent() {
		if (getIntent() != null) {
			if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
				// MyLog.d(TAG, "ACTION_VIEW");
				// from click on search results
				showBusStop(getIntent().getData().getPathSegments().get(0));
				finish();
			} else if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
				// MyLog.d(TAG, "ACTION_SEARCH");
				// an actual search
				String searchTerm = getIntent().getStringExtra(SearchManager.QUERY);
				// MyLog.d(TAG, "search: " + searchTerm);
				setTitle(getString(R.string.search_result_for_and_keyword, searchTerm));
				// setListAdapter(getAdapter(searchTerm));
				getListView().setAdapter(null);
				new LoadSearchResultTask().execute(searchTerm);
			}
		}
	}

	/**
	 * This task create load the search results cursor.
	 * @author Mathieu Méa
	 */
	private class LoadSearchResultTask extends AsyncTask<String, String, Cursor> {
		@Override
		protected Cursor doInBackground(String... arg0) {
			return StmManager.searchResult(SearchResult.this.getContentResolver(), arg0[0]);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			setAdapter(result);
			super.onPostExecute(result);
		}

	}

	/**
	 * Set the list view adapter.
	 * @param cursor the cursor used to create the adapter
	 */
	private void setAdapter(Cursor cursor) {
		String[] from = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.LINE_NAME,
				StmStore.BusStop.STOP_DIRECTION_ID };
		int[] to = new int[] { R.id.stop_code, R.id.label, R.id.line_number, R.id.line_name, R.id.line_direction };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.search_result_bus_stop_item, cursor, from, to, 0);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()) {
				case R.id.line_direction:
					String simpleDirectionId = cursor.getString(columnIndex);
					((TextView) view).setText(BusUtils.getBusLineSimpleDirection(simpleDirectionId));
					return true;
				case R.id.label:
					String busStopPlace = cursor.getString(cursor.getColumnIndex(StmStore.BusStop.STOP_PLACE));
					((TextView) view).setText(BusUtils.cleanBusStopPlace(busStopPlace));
					return true;
				default:
					return false;
				}
			}
		});
		getListView().setAdapter(adapter);
	}

	/**
	 * Launch the view bus stop info activity.
	 * @param selectedSearchResultId the selected search result
	 */
	private void showBusStop(String selectedSearchResultId) {
		MyLog.v(TAG, "showBusStop(%s)", selectedSearchResultId);
		String busStopCode = BusStop.getCodeFromUID(selectedSearchResultId);
		String busLineNumber = BusStop.getLineNumberFromUID(selectedSearchResultId);
		if (busStopCode != null && busLineNumber != null) {
			Intent intent = new Intent(this, BusStopInfo.class);
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busLineNumber);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStopCode);
			startActivity(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.createMainMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}

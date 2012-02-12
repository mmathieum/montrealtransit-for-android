package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * This activity display a search text box for entering bus stop code. The user can also enter a bus line number. In the future, this activity will have the
 * same functionalities as a search box for almost everything.
 * @author Mathieu Méa
 */
public class BusStopCodeTab extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusStopCodeTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BusStopCode";

	/**
	 * The search field.
	 */
	private AutoCompleteTextView searchField;

	/**
	 * The history list.
	 */
	private ListView historyList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_code_tab);
		this.searchField = (AutoCompleteTextView) findViewById(R.id.field);
		this.historyList = (ListView) findViewById(R.id.list);

		this.searchField.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				MyLog.v(TAG, "onKey(%s, %s)", v.getId(), keyCode);
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					searchFor(BusStopCodeTab.this.searchField.getText().toString(), true);
					return true;
				}
				return false;
			}
		});
		this.searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					Utils.showKeyboard(BusStopCodeTab.this, v);
				} else {
					Utils.hideKeyboard(BusStopCodeTab.this, v);
				}
			}
		});
		this.searchField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				searchFor((((TextView) v).getText()).toString(), true);
			}
		});
		((ImageButton) findViewById(R.id.ok)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyLog.v(TAG, "onClick(%s)", v.getId());
				searchFor(BusStopCodeTab.this.searchField.getText().toString(), true);
			}
		});
		this.historyList.setEmptyView(findViewById(R.id.list_empty));
		this.historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				searchFor((((TextView) v).getText()).toString(), position != 0);
			}
		});
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
		// refresh the adapters
		setSearchAutoCompleteAdapter();
		setHistoryAdapter();
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
	}

	/**
	 * Set the auto complete adapter.
	 */
	private void setSearchAutoCompleteAdapter() {
		if (this.searchField.getAdapter() == null) {
			new GetAllNumbersTask().execute();
		}
	}

	/**
	 * This task get all bus stop codes and all bus lines numbers.
	 */
	private class GetAllNumbersTask extends AsyncTask<Void, String, List<String>> {

		@Override
		protected List<String> doInBackground(Void... params) {
			List<String> numbers = StmManager.findAllBusStopCodeList(BusStopCodeTab.this.getContentResolver());
			numbers.addAll(StmManager.findAllBusLinesNumbersList(BusStopCodeTab.this.getContentResolver()));
			return numbers;
		}

		@Override
		protected void onPostExecute(List<String> result) {
			BusStopCodeTab.this.searchField.setAdapter(new ArrayAdapter<String>(BusStopCodeTab.this,
			        android.R.layout.simple_dropdown_item_1line, result));
		}
	}

	/**
	 * Set the history adapter. Since it's created from the cursor, it will be updated automatically.
	 */
	private void setHistoryAdapter() {
		if (this.historyList.getAdapter() == null) {
			this.historyList.setAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, DataManager
			        .findAllHistory(this.getContentResolver()), new String[] { DataStore.History.VALUE },
			        new int[] { android.R.id.text1 }));
		}
	}

	/**
	 * Search for a match for the search text. Could redirect the user to a bus stop or a bus line for now.
	 * @param search the search text.
	 * @param saveToHistory true if adding the new search text to the history
	 */
	private void searchFor(String search, boolean saveToHistory) {
		MyLog.v(TAG, "searchFor(%s, %s)", search, saveToHistory);
		if (search == null || search.length() == 0) {
			// please enter a number
			Utils.notifyTheUser(this, getString(R.string.please_enter_a_stop_code));
		} else {
			if (search.length() <= 3) {
				// search for a bus line number
				if (BusUtils.isBusLineNumberValid(this, search)) {
					if (saveToHistory) {
						addToHistory(search);
					}
					new BusLineSelectDirection(this, search).showDialog();
				} else {
					Utils.notifyTheUserLong(this, getString(R.string.wrong_line_number_and_number, search));
				}
			} else if (search.length() == 5) {
				// search for a bus stop code
				if (BusUtils.isStopCodeValid(this, search)) {
					if (saveToHistory) {
						addToHistory(search);
					}
					showBusStopInfo(search);
				} else {
					Utils.notifyTheUserLong(this, getString(R.string.wrong_stop_code_and_code, search));
				}
			}
		}
	}

	/**
	 * Add a value to the history.
	 * @param search the search string.
	 */
	private void addToHistory(String search) {
		// save to the history
		DataManager.addHistory(getContentResolver(), new DataStore.History(search));
	}

	/**
	 * Redirect the user to the bus stop info activity.
	 * @param stopCode the bus stop code
	 */
	private void showBusStopInfo(String stopCode) {
		Intent intent = new Intent(this, BusStopInfo.class);
		intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, stopCode);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_stop_code_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.history:
			DataManager.deleteAllHistory(getContentResolver());
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}

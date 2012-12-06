package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
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
	 * The cursor used to store the history.
	 */
	private Cursor historyCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_code_tab);

		setupSearchField((AutoCompleteTextView) findViewById(R.id.field));
		setupOkButton((ImageButton) findViewById(R.id.ok));
		setupHistoryList((ListView) findViewById(R.id.list));
	}

	/**
	 * Setup 'ok' button.
	 * @param okButton the ok button
	 */
	public void setupOkButton(ImageButton okButton) {
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyLog.v(TAG, "onClick(%s)", v.getId());
				searchFor(((AutoCompleteTextView) findViewById(R.id.field)).getText().toString(), true);
			}
		});
	}

	/**
	 * Setup history list.
	 * @param historyList the history list
	 */
	private void setupHistoryList(ListView historyList) {
		historyList.setEmptyView(findViewById(R.id.list_empty));
		historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				searchFor((((TextView) v).getText()).toString(), position != 0);
			}
		});
	}

	/**
	 * Setup search field
	 * @param searchField the search field
	 */
	public void setupSearchField(AutoCompleteTextView searchField) {
		searchField.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				MyLog.v(TAG, "onKey(%s, %s)", v.getId(), keyCode);
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					searchFor(((AutoCompleteTextView) findViewById(R.id.field)).getText().toString(), true);
					return true;
				}
				return false;
			}
		});
		searchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					Utils.showKeyboard(BusStopCodeTab.this, v);
				} else {
					Utils.hideKeyboard(BusStopCodeTab.this, v);
				}
			}
		});
		searchField.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				searchFor((((TextView) v).getText()).toString(), true);
			}
		});
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
		// refresh the adapters
		setSearchAutoCompleteAdapterFromDB();
		setHistoryAdapterFromDB();
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
	}

	/**
	 * Set the auto complete adapter.
	 */
	private void setSearchAutoCompleteAdapterFromDB() {
		if (((AutoCompleteTextView) findViewById(R.id.field)).getAdapter() == null) {
			new AsyncTask<Void, String, List<String>>() {
				@Override
				protected List<String> doInBackground(Void... params) {
					List<String> numbers = StmManager.findAllBusStopCodeList(BusStopCodeTab.this.getContentResolver());
					numbers.addAll(StmManager.findAllBusLinesNumbersList(BusStopCodeTab.this.getContentResolver()));
					return numbers;
				}

				@Override
				protected void onPostExecute(List<String> result) {
					((AutoCompleteTextView) findViewById(R.id.field)).setAdapter(new ArrayAdapter<String>(BusStopCodeTab.this,
							android.R.layout.simple_dropdown_item_1line, result));
				}

			}.execute();
		}
	}

	/**
	 * Set the history adapter. Since it's created from the cursor, it will be updated automatically.
	 */
	private void setHistoryAdapterFromDB() {
		if (((ListView) findViewById(R.id.list)).getAdapter() == null) {
			// TODO show loading wheel while loading
			new AsyncTask<Void, String, Cursor>() {
				@Override
				protected Cursor doInBackground(Void... params) {
					return DataManager.findAllHistory(BusStopCodeTab.this.getContentResolver());
				}

				@SuppressWarnings("deprecation")
				// TODO use {@link android.app.LoaderManager} with a {@link android.content.CursorLoader}
				@Override
				protected void onPostExecute(Cursor result) {
					BusStopCodeTab.this.historyCursor = result;
					((ListView) findViewById(R.id.list)).setAdapter(new SimpleCursorAdapter(BusStopCodeTab.this, android.R.layout.simple_list_item_1, BusStopCodeTab.this.historyCursor,
							new String[] { DataStore.History.VALUE }, new int[] { android.R.id.text1 }));

				}
			}.execute();
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
						// save to the history
						new AsyncTask<String, Void, Void>() {
							@Override
							protected Void doInBackground(String... params) {
								DataManager.addHistory(getContentResolver(), new DataStore.History(params[0]));
								return null;
							}
						}.execute(search);
					}
					Intent intent = new Intent(this, SupportFactory.getInstance(this).getBusLineInfoClass());
					intent.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, search);
					startActivity(intent);
				} else {
					Utils.notifyTheUserLong(this, getString(R.string.wrong_line_number_and_number, search));
				}
			} else if (search.length() == 5) {
				// search for a bus stop code
				if (BusUtils.isStopCodeValid(this, search)) {
					if (saveToHistory) {
						// save to the history
						new AsyncTask<String, Void, Void>() {
							@Override
							protected Void doInBackground(String... params) {
								DataManager.addHistory(getContentResolver(), new DataStore.History(params[0]));
								return null;
							}
						}.execute(search);
					}
					showBusStopInfo(search);
				} else {
					Utils.notifyTheUserLong(this, getString(R.string.wrong_stop_code_and_code, search));
				}
			}
		}
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
			// clear history
			new AsyncTask<String, Void, Void>() {
				@Override
				protected Void doInBackground(String... params) {
					DataManager.deleteAllHistory(getContentResolver());
					return null;
				}
			}.execute();
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}
	
	@Override
	protected void onDestroy() {
		if (this.historyCursor != null && !this.historyCursor.isClosed()) {
			this.historyCursor.close();
		}
		super.onDestroy();
	}
}

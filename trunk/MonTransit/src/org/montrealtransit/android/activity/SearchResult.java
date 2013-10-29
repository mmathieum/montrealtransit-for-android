package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * This activity shows the search results.
 * @author Mathieu Méa
 */
public class SearchResult extends ListActivity implements LocationListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SearchResult.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SearchResult";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.search_result);

		this.adapter = new POIArrayAdapter(this);
		getListView().setOnItemClickListener(this.adapter);
		getListView().setOnScrollListener(this.adapter);
		getListView().setAdapter(this.adapter);

		processIntent();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		setIntent(intent);
		processIntent();
		super.onNewIntent(intent);
	}

	/**
	 * Process the intent of the activity.
	 */
	private void processIntent() {
		// MyLog.v(TAG, "processIntent()");
		if (getIntent() != null) {
			if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
				// MyLog.d(TAG, "ACTION_VIEW");
				// from click on search results
				String uid = getIntent().getData().getPathSegments().get(0);
				startActivity(BusStopInfo.newInstance(this, RouteTripStop.getStopCodeFromUID(uid), RouteTripStop.getRouteShortNameFromUID(uid)));
				finish();
			} else if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
				// MyLog.d(TAG, "ACTION_SEARCH");
				// an actual search
				String searchTerm = getIntent().getStringExtra(SearchManager.QUERY);
				// MyLog.d(TAG, "search: " + searchTerm);
				setTitle(getString(R.string.search_result_for_and_keyword, searchTerm));
				((TextView) findViewById(R.id.main_msg)).setText(R.string.please_wait);
				this.adapter.setPois(null);
				new LoadSearchResultTask().execute(searchTerm);
			}
		}
	}

	/**
	 * The bus stops list adapter.
	 */
	private POIArrayAdapter adapter;

	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;

	/**
	 * This task create load the search results cursor.
	 * @author Mathieu Méa
	 */
	private class LoadSearchResultTask extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... arg0) {
			// MyLog.v(TAG, "LoadSearchResultTask>doInBackground()");
			final String searchTerm = arg0[0];
			List<RouteTripStop> searchRouteTripStopList = StmBusManager.searchRouteTripStopList(SearchResult.this, searchTerm);
			if (searchRouteTripStopList == null) {
				searchRouteTripStopList = new ArrayList<RouteTripStop>(); // null == loading
			}
			SearchResult.this.adapter.setPois(searchRouteTripStopList);
			SearchResult.this.adapter.updateDistancesNow(getLocation());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// MyLog.v(TAG, "LoadSearchResultTask>onPostExecute()");
			if (SearchResult.this.adapter.getPois() == null) {
				((TextView) findViewById(R.id.main_msg)).setText(R.string.please_wait);
			} else {
				((TextView) findViewById(R.id.main_msg)).setText(R.string.search_no_result);
			}
			refreshFavoriteUIDsFromDB();
			SearchResult.this.adapter.notifyDataSetChanged(true);
		}

	}

	/**
	 * Find favorites bus stops UIDs.
	 */
	private void refreshFavoriteUIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				SearchResult.this.adapter.setFavs(result);
			};
		}.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.createMainMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	@Override
	public void onProviderEnabled(String provider) {
		// MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	@Override
	public void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		this.adapter.onPause();
		super.onPause();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		MyLog.v(TAG, "onWindowFocusChanged(%s)", hasFocus);
		// IF the activity just regained the focus DO
		if (!this.hasFocus && hasFocus) {
			onResumeWithFocus();
		}
		this.hasFocus = hasFocus;
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		this.paused = false;
		// IF the activity has the focus DO
		if (this.hasFocus) {
			onResumeWithFocus();
		}
		super.onResume();
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		// IF location updates should be enabled DO
		if (!this.locationUpdatesEnabled) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					// MyLog.v(TAG, "onResumeWithFocus() > doInBackground()");
					return LocationUtils.getBestLastKnownLocation(SearchResult.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "onResumeWithFocus() > onPostExecute(%s)", result);
					// IF there is a valid last know location DO
					if (result != null) {
						// set the new distance
						setLocation(result);
					}
					// re-enable
					SearchResult.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(SearchResult.this, SearchResult.this,
							SearchResult.this.locationUpdatesEnabled, SearchResult.this.paused);

				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		refreshFavoriteUIDsFromDB();
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		// MyLog.v(TAG, "getLocation()");
		if (this.location == null) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					// MyLog.v(TAG, "getLocation() > doInBackground()");
					return LocationUtils.getBestLastKnownLocation(SearchResult.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "getLocation() > onPostExecute(%s)", result);
					if (result != null) {
						setLocation(result);
					}
					// enable location updates if necessary
					SearchResult.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(SearchResult.this, SearchResult.this,
							SearchResult.this.locationUpdatesEnabled, SearchResult.this.paused);
				}

			}.execute();
		}
		return this.location;
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		MyLog.v(TAG, "setLocation()");
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.adapter.setLocation(this.location);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
	}

}

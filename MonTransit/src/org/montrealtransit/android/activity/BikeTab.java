package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BikeUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Display a list of bike stations.
 * @author Mathieu Méa
 */
public class BikeTab extends Activity implements LocationListener, ClosestBikeStationsFinderTask.ClosestBikeStationsFinderListener, SensorEventListener,
		ShakeListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BikeTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BikeStations";

	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass update enabled?
	 */
	private boolean shakeUpdatesEnabled = false;
	/**
	 * The location used to generate the closest bike stations list.
	 */
	private Location closestBikeStationsLocation;
	/**
	 * The location address used to generate the closest bike stations list;
	 */
	private String closestBikeStationsLocationAddress;
	/**
	 * The bike stations list adapter.
	 */
	private POIArrayAdapter adapter;
	/**
	 * The task used to load the closest bike stations.
	 */
	private ClosestBikeStationsFinderTask closestBikeStationsTask;
	/**
	 * The acceleration apart from gravity.
	 */
	private float lastSensorAcceleration = 0.00f;
	/**
	 * The last acceleration including gravity.
	 */
	private float lastSensorAccelerationIncGravity = SensorManager.GRAVITY_EARTH;
	/**
	 * The last sensor update time-stamp.
	 */
	private long lastSensorUpdate = -1;
	/**
	 * True if the share was already handled (should be reset in {@link #onResume()}).
	 */
	private boolean shakeHandled = false;
	/**
	 * Last try (device time).
	 */
	private int lastForcedRefresh = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bike_station_tab);
		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		ListView list = (ListView) findViewById(R.id.closest_bike_stations_list);
		this.adapter = new POIArrayAdapter(this);
		this.adapter.setShakeEnabled(true);
		this.adapter.setShowData(true);
		this.adapter.setListView(list);
		showClosestBikeStations();
	}

	/**
	 * {@link #onCreate(Bundle)} method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		findViewById(R.id.closest_bike_stations_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshClosestStations(v);
			}
		});
	}

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;

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
		UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_TAB, 4);
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
					return LocationUtils.getBestLastKnownLocation(BikeTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "onResumeWithFocus()>onPostExecute(%s)", result);
					// IF there is a valid last know location DO
					if (result != null) {
						if (BikeTab.this.closestBikeStationsLocation != null) {
							if (LocationUtils.isMoreRelevant(BikeTab.this.closestBikeStationsLocation, result, LocationUtils.SIGNIFICANT_ACCURACY_IN_METERS,
									Utils.CLOSEST_POI_LIST_PREFER_ACCURACY_OVER_TIME)
									&& LocationUtils.isTooOld(BikeTab.this.closestBikeStationsLocation, Utils.CLOSEST_POI_LIST_TIMEOUT)) {
								// MyLog.d(TAG, "onResumeWithFocus()>onPostExecute()> force closest stations refresh!");
								BikeTab.this.adapter.setPois(null);
							}
						}
						// set the new distance
						setLocation(result);
					}
					BikeTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(BikeTab.this, BikeTab.this,
							BikeTab.this.locationUpdatesEnabled, BikeTab.this.paused);
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		refreshFavoriteTerminalNamesFromDB();
		if (this.closestBikeStationsLocation != null) {
			refreshDataIfTooOld();
		}
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		BikeTab.this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.shakeUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.shakeUpdatesEnabled = false;
		}
		this.adapter.onPause();
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(event, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onShake() {
		MyLog.v(TAG, "onShake()");
		showClosestStation();
	}

	/**
	 * Show the closest subway line station (if possible).
	 */
	private void showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		if (this.hasFocus && !this.shakeHandled && this.adapter.hasClosestPOI()) {
			Toast.makeText(this, R.string.shake_closest_bike_station_selected, Toast.LENGTH_SHORT).show();
			// show bike station view
			this.adapter.showClosestPOI();
			this.shakeHandled = true;
		}
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		MyLog.v(TAG, "setLocation(%s)", newLocation);
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.adapter.setLocation(this.location);
				if (!this.shakeUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.shakeUpdatesEnabled = true;
					this.shakeHandled = false;
				}
				// updateDistancesWithNewLocation(this.location);
				// IF no closest bike stations AND new location DO
				if (this.adapter.getPois() == null && this.location != null) {
					// start refreshing if not running.
					refreshClosestBikeStations(false);
				}
			}
		}
	}

	/**
	 * Set the closest stations as not loading.
	 */
	private void setClosestStationsNotLoading() {
		MyLog.v(TAG, "setClosestStationsNotLoading()");
		View closestStationsTitle = findViewById(R.id.closest_bike_stations_title);
		// show refresh icon instead of loading
		closestStationsTitle.findViewById(R.id.closest_bike_stations_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		closestStationsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.INVISIBLE);
	}

	/**
	 * Show the new closest bike stations.
	 */
	private void showNewClosestBikeStations(final boolean forceRefresh) {
		MyLog.v(TAG, "showNewClosestBikeStations(forceRefresh:%s)", forceRefresh);
		if (this.adapter.getPois() != null) {
			// set the closest station title
			showNewClosestBikeStationsTitle();
			// hide loading
			findViewById(R.id.closest_bike_stations_list_loading).setVisibility(View.GONE); // hide
			// show stations list
			this.adapter.notifyDataSetChanged(true);
			ListView closestStationsListView = (ListView) findViewById(R.id.closest_bike_stations_list);
			if (this.forceRefresh) {
				SupportFactory.get().listViewScrollTo(closestStationsListView, 0, 0);
			}
			this.forceRefresh = false;
			closestStationsListView.setVisibility(View.VISIBLE);
			setClosestStationsNotLoading();
			new AsyncTask<Void, Void, Integer>() {
				@Override
				protected Integer doInBackground(Void... params) {
					return UserPreferences.getPrefLcl(BikeTab.this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0);
				}

				@Override
				protected void onPostExecute(Integer result) {
					BikeTab.this.adapter.setLastSuccessfulRefresh(result);
					if (forceRefresh) {
						forceRefresh();
					} else {
						refreshDataIfTooOld();
					}
				};
			}.execute();
		}
	}

	private boolean refreshDataIfTooOld() {
		MyLog.v(TAG, "refreshDataIfTooOld()");
		if (isDataTooOld()) {
			// IF last try too recent DO
			if (isDataTooRecent()) {
				MyLog.d(TAG, "refreshDataIfTooOld() > last refresh too recent, not refreshing");
				return false; // skip refresh
			}
			// ELSE refresh from www
			forceRefresh();
			return true;
		}
		return false;
	}

	private boolean isDataTooOld() {
		// MyLog.v(TAG, " isDataTooOld()");
		return Utils.currentTimeSec() - BikeUtils.CACHE_TOO_OLD_IN_SEC - getLastUpdateTime() > 0;
	}

	private void forceRefresh() {
		MyLog.v(TAG, "forceRefresh()");
		// cancel current task
		if (this.closestBikeStationsTask != null && this.closestBikeStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			try { // wait for 1 seconds
				this.closestBikeStationsTask.get(1, TimeUnit.SECONDS);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Error while waiting to task to complete!");
			}
		}
		// start new task w/ force refresh
		startClosestStationsTask(true);
	}

	private boolean isDataTooRecent() {
		// MyLog.v(TAG, " isDataTooRecent()");
		if (this.lastForcedRefresh < 0) {
			return false; // never forced refresh
		}
		return Utils.currentTimeSec() - BikeUtils.CACHE_TOO_OLD_IN_SEC - this.lastForcedRefresh < 0;
	}

	/**
	 * @return the last update time
	 */
	public int getLastUpdateTime() {
		if (this.adapter.getLastSuccessfulRefresh() < 0) {
			this.adapter.setLastSuccessfulRefresh(UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, -1));
		}
		if (this.adapter.getLastSuccessfulRefresh() < 0) {
			return Utils.currentTimeSec(); // use device time
		}
		return this.adapter.getLastSuccessfulRefresh();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		MyLog.v(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
		adaptToScreenSize(newConfig);
	}

	private void adaptToScreenSize(Configuration configuration) {
		if (SupportFactory.get().isScreenHeightSmall(configuration)) {
			// HIDE AD
			if (findViewById(R.id.ad_layout) != null) {
				findViewById(R.id.ad_layout).setVisibility(View.GONE); // not enough space on phone
			}
		} else {
			// SHOW AD
			AdsUtils.setupAd(this);
		}
	}

	/**
	 * set the closest station title
	 */
	public void showNewClosestBikeStationsTitle() {
		if (this.closestBikeStationsLocationAddress != null && this.closestBikeStationsLocation != null) {
			((TextView) findViewById(R.id.closest_bike_stations_title).findViewById(R.id.closest_bike_stations_title_text))
					.setText(this.closestBikeStationsLocationAddress);
		}
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStationsError(String errorMessage) {
		MyLog.v(TAG, "setClosestStationsError(%s)", errorMessage);
		// IF there are already stations DO
		if (Utils.getCollectionSize(this.adapter.getPois()) > 0) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUserTop(this, errorMessage);
		} else {
			// show the error message
			View loadingLayout = findViewById(R.id.closest_bike_stations_list_loading);
			TextView mainMsgTv = (TextView) loadingLayout.findViewById(R.id.main_msg);
			TextView detailMsgTv = (TextView) loadingLayout.findViewById(R.id.detail_msg);
			mainMsgTv.setText(R.string.error);
			detailMsgTv.setText(errorMessage);
			detailMsgTv.setVisibility(View.VISIBLE);
		}
		setClosestStationsNotLoading();
	}

	/**
	 * Set the closest stations as loading.
	 */
	private void setClosestStationsLoading(String detailMsg) {
		MyLog.v(TAG, "setClosestStationsLoading(%s)", detailMsg);
		View closestStationsTitle = findViewById(R.id.closest_bike_stations_title);
		if (Utils.getCollectionSize(this.adapter.getPois()) == 0) {
			// set the loading message
			// remove last location from the list divider
			((TextView) closestStationsTitle.findViewById(R.id.closest_bike_stations_title_text)).setText(R.string.closest_bike_stations);
			if (findViewById(R.id.closest_bike_stations_list) != null) { // IF inflated/present DO
				// hide the list
				findViewById(R.id.closest_bike_stations_list).setVisibility(View.GONE);
			}
			// show loading
			View loadingLayout = findViewById(R.id.closest_bike_stations_list_loading);
			TextView mainMsgTv = (TextView) loadingLayout.findViewById(R.id.main_msg);
			TextView detailMsgTv = (TextView) loadingLayout.findViewById(R.id.detail_msg);
			mainMsgTv.setText(R.string.please_wait);
			if (detailMsg == null) { // show waiting for location
				detailMsg = getString(R.string.waiting_for_location_fix);
			}
			detailMsgTv.setText(detailMsg);
			detailMsgTv.setVisibility(View.VISIBLE);
			loadingLayout.setVisibility(View.VISIBLE);
			// } else { just notify the user ?
		}
		// show stop icon instead of refresh
		closestStationsTitle.findViewById(R.id.closest_bike_stations_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		closestStationsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.VISIBLE);
	}

	/**
	 * Show the closest stations UI.
	 */
	public void showClosestBikeStations() {
		MyLog.v(TAG, "showClosestBikeStations()");
		// enable location updates
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		// IF there is no closest bike stations DO
		if (Utils.getCollectionSize(this.adapter.getPois()) == 0) {
			// generate the closest bike stations list
			refreshClosestBikeStations(false);
		} else {
			// show the closest stations
			showNewClosestBikeStations(false);
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestBikeStationsLocation)) {
				// start refreshing
				refreshClosestBikeStations(true);
			}
		}
	}

	private boolean forceRefresh = false;

	/**
	 * Refresh the closest stations if not running.
	 * @param v a view (not used)
	 */
	public void refreshOrStopRefreshClosestStations(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshClosestStations()");
		this.forceRefresh = true;
		if (!isDataTooRecent()) {
			this.adapter.setPois(null); // refresh list 1st, then data from www
		}
		refreshClosestBikeStations(false);
	}

	/**
	 * Refresh the closest bike stations list.
	 */
	private void refreshClosestBikeStations(boolean forceUpdateFromWeb) {
		MyLog.v(TAG, "refreshClosestBikeStations(%s)", forceUpdateFromWeb);
		// cancel current if forcing
		if (forceUpdateFromWeb && this.closestBikeStationsTask != null) {
			this.closestBikeStationsTask.cancel(true);
			this.closestBikeStationsTask = null;
		}
		// IF the task is NOT already running DO
		if (this.closestBikeStationsTask == null || !this.closestBikeStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// IF location found DO
			Location locationUsed = getLocation();
			// MyLog.d(TAG, "refreshClosestBikeStations() locationUsed: " + locationUsed);
			if (locationUsed != null) {
				// find the closest stations
				this.closestBikeStationsLocation = locationUsed;
				startClosestStationsTask(forceUpdateFromWeb);
				new AsyncTask<Location, Void, String>() {

					@Override
					protected String doInBackground(Location... locations) {
						// MyLog.v(TAG, "refreshClosestBikeStations() > doInBackground()");
						Address address = LocationUtils.getLocationAddress(BikeTab.this, locations[0]);
						if (address == null || BikeTab.this.closestBikeStationsLocation == null) {
							return null;
						}
						return LocationUtils.getLocationString(BikeTab.this, R.string.closest_bike_stations, address,
								BikeTab.this.closestBikeStationsLocation.getAccuracy());
					}

					@Override
					protected void onPostExecute(String result) {
						// MyLog.v(TAG, "refreshClosestBikeStations() > onPostExecute(%s)", result);
						boolean refreshRequired = BikeTab.this.closestBikeStationsLocationAddress == null;
						BikeTab.this.closestBikeStationsLocationAddress = result;
						if (refreshRequired) {
							showNewClosestBikeStationsTitle();
						}
					}

				}.execute(this.closestBikeStationsLocation);
			} else { // ELSE wait for location...
				setClosestStationsLoading(null);
			}
			// } else {
			// MyLog.d(TAG, "refreshClosestBikeStations() > already running");
		}
	}

	private void startClosestStationsTask(boolean forceUpdateFromWeb) {
		// MyLog.v(TAG, "startClosestStationsTask(%s)", forceUpdateFromWeb);
		if (forceUpdateFromWeb) {
			this.lastForcedRefresh = Utils.currentTimeSec();
		}
		setClosestStationsLoading(null);
		this.closestBikeStationsTask = new ClosestBikeStationsFinderTask(this, this, SupportFactory.get().getNbClosestPOIDisplay(), forceUpdateFromWeb);
		this.closestBikeStationsTask.execute(this.closestBikeStationsLocation.getLatitude(), this.closestBikeStationsLocation.getLongitude());
	}

	@Override
	public void onClosestBikeStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestBikeStationsProgress(%s)", progress);
		setClosestStationsLoading(progress);
	}

	@Override
	public void onClosestBikeStationsDone(ClosestPOI<ABikeStation> result) {
		// MyLog.v(TAG, "onClosestBikeStationsDone()");
		// MyLog.d(TAG, "Error: " + (result == null ? null : result.getErrorMessage()));
		if (result == null || result.getPoiListOrNull() == null) {
			// show the error
			setClosestStationsError(result == null ? null : result.getErrorMessage());
		} else {
			// IF first refresh and no POI DO
			if (this.adapter.getPois() == null && result.getPoiListSize() == 0) {
				// get the result
				this.adapter.setPois(new ArrayList<ABikeStation>()); // important for only forcing refresh
																	 // once
				// force refresh from server
				refreshClosestBikeStations(true);
			} else { // ELSE
				boolean forceRefresh = false;
				if (this.adapter.getPois() == null && isDataTooOld() && !isDataTooRecent()) {
					// MyLog.d(TAG, "onClosestBikeStationsDone() > forceRefresh = true");
					forceRefresh = true;
				}
				// get the result
				BikeTab.this.adapter.setPois(result.getPoiList());
				// set location
				BikeTab.this.adapter.updateDistancesNow(getLocation());
				// set compass
				BikeTab.this.adapter.updateCompassNow();
				// refresh favorites
				refreshFavoriteTerminalNamesFromDB();
				// shot the result
				showNewClosestBikeStations(forceRefresh);
			}
			// notify the error message
			if (!TextUtils.isEmpty(result.getErrorMessage())) {
				setClosestStationsError(result.getErrorMessage());
			}
		}
	}

	/**
	 * Find favorites bike stations terminal names.
	 */
	private void refreshFavoriteTerminalNamesFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				BikeTab.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		if (this.location == null) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					// MyLog.v(TAG, "getLocation() > doInBackground()");
					return LocationUtils.getBestLastKnownLocation(BikeTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "getLocation() > onPostExecute(%s)", result);
					if (result != null) {
						BikeTab.this.setLocation(result);
					}
					// enable location updates if necessary
					BikeTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(BikeTab.this, BikeTab.this,
							BikeTab.this.locationUpdatesEnabled, BikeTab.this.paused);
				}

			}.execute();
		}
		return this.location;
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bike_tab_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.closestBikeStationsTask != null) {
			this.closestBikeStationsTask.cancel(true);
			if (this.adapter != null) {
				this.adapter.setPois(null);
			}
		}
		AdsUtils.setupAd(this);
		super.onDestroy();
	}
}

package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.services.BixiDataReader;
import org.montrealtransit.android.services.BixiDataReader.BixiDataReaderListener;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask.ClosestBikeStationsFinderListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BikeStationInfo extends Activity implements BixiDataReaderListener, ClosestBikeStationsFinderListener, LocationListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BikeStationInfo.class.getSimpleName();

	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BikeStation";

	/**
	 * The extra ID for the bike station terminal name.
	 */
	public static final String EXTRA_STATION_TERMINAL_NAME = "extra_bike_station_terminal_name";
	/**
	 * The extra ID for the bike station name (optional).
	 */
	public static final String EXTRA_STATION_NAME = "extra_bike_station_name";

	/**
	 * The validity of the cache (in seconds).
	 */
	private static final int CACHE_TOO_OLD_IN_SEC = 10 * 60; // 10 minutes

	/**
	 * The bike station.
	 */
	private BikeStation bikeStation;

	/**
	 * The terminal name of the bike station for which the current closest station are.
	 */
	private String closestBikeStationTerminalName;
	/**
	 * The other bus lines.
	 */
	protected List<ABikeStation> closestBikeStations;
	/**
	 * The task used to load the closest bike stations.
	 */
	private ClosestBikeStationsFinderTask closestBikeStationsTask;

	/**
	 * The number of closest bike station displayed.
	 */
	private static final int NB_CLOSEST_BIKE_STATIONS = 4;

	/**
	 * The task used to load the new bike station info.
	 */
	private BixiDataReader task;

	/**
	 * Is the location updates should be enabled?
	 */
	private boolean locationUpdatesEnabled = false;

	/**
	 * The device location.
	 */
	private Location location;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bike_station_info);
		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
	}

	/**
	 * {@link #onCreate(Bundle)} method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		findViewById(R.id.next_stops_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshStatus(v);
			}
		});
		findViewById(R.id.star).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addOrRemoveFavorite(v);
			}
		});
	}

	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onStop();
	}

	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onRestart();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		setBikeStationFromIntent(getIntent(), null);
		super.onResume();
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		Location currentLocation = getLocation();
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.bikeStation != null) {
			// distance & accuracy
			((TextView) findViewById(R.id.distance)).setText(Utils.getDistanceStringUsingPref(this,
					currentLocation.distanceTo(LocationUtils.getNewLocation(this.bikeStation.getLat(), this.bikeStation.getLng())),
					currentLocation.getAccuracy()));
		}
		// update other stations
		if (this.closestBikeStations != null && currentLocation != null) {
			// update the list distances
			setClosestStationsDistances(currentLocation);
			// update the view
			refreshClosestSubwayStationsDistancesList();
		}
	}

	/**
	 * Refresh the closest subway stations <b>distances</b> ONLY in the list.
	 */
	private void refreshClosestSubwayStationsDistancesList() {
		MyLog.v(TAG, "refreshClosestSubwayStationsDistancesList()");
		findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
		int i = 1;
		for (ABikeStation station : this.closestBikeStations) {
			View stationView = findViewById(R.id.closest_stations).findViewWithTag(getStationViewTag(i++));
			if (stationView != null && !TextUtils.isEmpty(station.getDistanceString())) {
				((TextView) stationView.findViewById(R.id.distance)).setText(station.getDistanceString());
			}
		}
	}

	/**
	 * @param i the station view index
	 * @return the station view tab with this index
	 */
	private static String getStationViewTag(int i) {
		return "station" + i;
	}

	/**
	 * Set closest stations distances with the location
	 * @param location the location (or null to reset)
	 */
	public void setClosestStationsDistances(Location location) {
		boolean isDetailed = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
				UserPreferences.PREFS_DISTANCE_DETAILED);
		String distanceUnit = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
		for (ABikeStation station : this.closestBikeStations) {
			// distance
			if (location != null) {
				station.setDistance(location.distanceTo(LocationUtils.getNewLocation(station.getLat(), station.getLng())));
				station.setDistanceString(Utils.getDistanceString(station.getDistance(), location.getAccuracy(), isDetailed, distanceUnit));
			} else {
				station.setDistance(null);
				station.setDistanceString(null);
			}
		}
	}

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <B>NULL</b>
	 */
	private Location getLocation() {
		if (this.location == null) {
			Location bestLastKnownLocationOrNull = LocationUtils.getBestLastKnownLocation(this);
			if (bestLastKnownLocationOrNull != null) {
				this.setLocation(bestLastKnownLocationOrNull);
			}
			// enable location updates if necessary
			if (!this.locationUpdatesEnabled) {
				LocationUtils.enableLocationUpdates(this, this);
				this.locationUpdatesEnabled = true;
			}
		}
		return this.location;
	}

	/**
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: '%s'.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistancesWithNewLocation();
	}

	@Override
	public void onProviderEnabled(String provider) {
		MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	private void setBikeStationFromIntent(Intent intent, Bundle savedInstanceState) {
		MyLog.v(TAG, "setBusStopFromIntent()");
		if (intent != null) {
			String bikeStationTerminalName;
			String bikeStationName = null;
			// TODO support NFC
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				bikeStationTerminalName = intent.getData().getPathSegments().get(1);
			} else {
				bikeStationTerminalName = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STATION_TERMINAL_NAME);
				bikeStationName = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STATION_NAME);
			}
			showNewBikeStation(bikeStationTerminalName, bikeStationName);
		}
	}

	/**
	 * Refresh status.
	 * @param v the view (not used)
	 */
	public void refreshStatus(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshStatus()");
		// IF the task is running DO
		if (this.task != null && this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.task.cancel(true);
			this.task = null;
		} else {
			setStatusAsLoading();
			// find the next bus stop
			this.task = new BixiDataReader(this, this, false);
			this.task.execute(this.bikeStation.getTerminalName());
		}
	}

	@Override
	public void onBixiDataProgress(String progress) {
		MyLog.v(TAG, "onBixiDataProgress(%s)", progress);
		// nothing
	}

	@Override
	public void onBixiDataLoaded(List<BikeStation> newBikeStations, boolean isNew) {
		// MyLog.v(TAG, "onBixiDataLoaded(%s,%s)", Utils.getCollectionSize(newBikeStations), isNew);
		// IF the bike station was returned as expected DO
		if (Utils.getCollectionSize(newBikeStations) == 1) {
			this.bikeStation = newBikeStations.get(0);
			showNewBikeStationStatus();
			setStatusNotLoading();
		} else {
			setStatusNotLoading();
			setStatusError();
		}

	}

	/**
	 * Show error.
	 */
	private void setStatusError() {
		MyLog.v(TAG, "setStatusError()");
		Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show status not loading.
	 */
	private void setStatusNotLoading() {
		MyLog.v(TAG, "setStatusNotLoading()");
		// show refresh icon instead of loading
		findViewById(R.id.availability_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		findViewById(R.id.availability_title_progress_bar).setVisibility(View.INVISIBLE);
	}

	/**
	 * Show status as loading.
	 */
	private void setStatusAsLoading() {
		MyLog.v(TAG, "setStatusAsLoading()");
		// hide refresh icon instead of loading
		findViewById(R.id.availability_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		findViewById(R.id.availability_title_progress_bar).setVisibility(View.VISIBLE);
	}

	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		if (this.bikeStation == null) {
			return;
		}
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_BIKE_STATIONS, this.bikeStation.getTerminalName(), null);
		// IF the favorite exist DO
		if (findFav != null) {
			// delete the favorite
			DataManager.deleteFav(getContentResolver(), findFav.getId());
			Utils.notifyTheUser(this, getString(R.string.favorite_removed));
		} else {
			// add the favorite
			Fav newFav = new Fav();
			newFav.setType(Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
			newFav.setFkId(this.bikeStation.getTerminalName());
			DataManager.addFav(getContentResolver(), newFav);
			Utils.notifyTheUser(this, getString(R.string.favorite_added));
			UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_IS_FAV, true);
		}
		SupportFactory.getInstance(this).backupManagerDataChanged();
		setTheStar(); // TODO is remove useless?
	}

	/**
	 * Show the new bike station information.
	 * @param newBikeStationTerminalName the new bike station terminal name MANDATORY
	 * @param newBikeStationName the new bike station name or null (optional)
	 */
	public void showNewBikeStation(String newBikeStationTerminalName, String newBikeStationName) {
		MyLog.v(TAG, "showNewBikeStation(%s, %s)", newBikeStationTerminalName, newBikeStationName);
		// temporary set UI
		if (!TextUtils.isEmpty(newBikeStationName)) {
			((TextView) findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(newBikeStationName));
		}
		findViewById(R.id.star).setVisibility(View.INVISIBLE);

		if (BikeStationInfo.this.bikeStation == null || !BikeStationInfo.this.bikeStation.getTerminalName().equals(newBikeStationTerminalName)) {
			findViewById(R.id.availability).setVisibility(View.GONE);
			findViewById(R.id.availability_loading).setVisibility(View.VISIBLE);
			new AsyncTask<String, Void, BikeStation>() {

				@Override
				protected BikeStation doInBackground(String... params) {
					return BixiManager.findBikeStation(getContentResolver(), params[0]);
				}

				@Override
				protected void onPostExecute(BikeStation result) {
					BikeStationInfo.this.bikeStation = result;
					if (BikeStationInfo.this.task != null) {
						BikeStationInfo.this.task.cancel(true);
						BikeStationInfo.this.task = null;
					}
					setUpUI();
				}
			}.execute(newBikeStationTerminalName);
		}
	}

	/**
	 * Set up all the UI.
	 */
	private void setUpUI() {
		MyLog.v(TAG, "setUpUI()");
		refreshBikeStationInfo();
		showBikeStationStatus();
		refreshClosestBikeStations();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistancesWithNewLocation();
		}
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Refresh closest bike stations.
	 */
	private void refreshClosestBikeStations() {
		// IF refresh required DO
		if (Utils.getCollectionSize(this.closestBikeStations) == 0 || this.closestBikeStationTerminalName == null
				|| !this.closestBikeStationTerminalName.equals(this.bikeStation.getTerminalName())) {
			// stop current task is running
			if (this.closestBikeStationsTask != null) {
				this.closestBikeStationsTask.cancel(true);
				this.closestBikeStations = null;
			}
			setClosestStationsAsLoading(); // set as loading
			this.closestBikeStationsTask = new ClosestBikeStationsFinderTask(this, this, NB_CLOSEST_BIKE_STATIONS + 1);
			this.closestBikeStationsTask.execute(LocationUtils.getNewLocation(this.bikeStation.getLat(), this.bikeStation.getLng()));
		}
	}

	/**
	 * Set closest stations as loading.
	 */
	public void setClosestStationsAsLoading() {
		findViewById(R.id.closest_stations).setVisibility(View.GONE);
		findViewById(R.id.closest_stations_loading).setVisibility(View.VISIBLE);
	}

	/**
	 * Set closest stations as not loading.
	 */
	public void setClosestStationsAsNotLoading() {
		findViewById(R.id.closest_stations_loading).setVisibility(View.GONE);
		findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
	}

	@Override
	public void onClosestBikeStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress(%s)", progress);
		// do nothing
	}

	@Override
	public void onClosestBikeStationsDone(ClosestPOI<ABikeStation> result) {
		MyLog.v(TAG, "onClosestBikeStationsDone()");
		if (result != null) {
			this.closestBikeStations = result.getPoiList().subList(1, result.getPoiList().size());
			// set location
			setClosestStationsDistances(getLocation());
			// shot the result
			showNewClosestStations();
		}
	}

	/**
	 * Show new closest stations.
	 */
	private void showNewClosestStations() {
		MyLog.v(TAG, "showNewClosestStations()");
		LinearLayout closestStationsLayout = (LinearLayout) findViewById(R.id.closest_stations);
		// clear the previous list
		closestStationsLayout.removeAllViews();
		// hide loading
		setClosestStationsAsNotLoading();
		// show stations list
		int i = 1;
		for (ABikeStation station : this.closestBikeStations) {
			// list view divider
			if (closestStationsLayout.getChildCount() > 0) {
				closestStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
			}
			// create view
			View view = getLayoutInflater().inflate(R.layout.bike_station_tab_closest_stations_list_item, null);
			view.setTag(getStationViewTag(i++));
			// bike station name
			((TextView) view.findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(station.getName()));
			// bike station distance
			if (!TextUtils.isEmpty(station.getDistanceString())) {
				((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
			}
			// add click listener
			final String terminalName = station.getTerminalName();
			final String name = station.getName();
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(%s)", v.getId());
					// Intent intent = new Intent(BikeStationInfo.this, BikeStationInfo.class);
					// intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, terminalName);
					// intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, name);
					// startActivity(intent);
					showNewBikeStation(terminalName, name);
				}
			});
			closestStationsLayout.addView(view);
		}
	}

	/**
	 * Refresh bike station info.
	 */
	private void refreshBikeStationInfo() {
		MyLog.v(TAG, "refreshBikeStationInfo()");
		MyLog.d(TAG, "this.bikeStatio null? " + (this.bikeStation == null));
		// set bike station name
		((TextView) findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(this.bikeStation.getName()));
		// set the favorite icon
		setTheStar();

		showNewBikeStationStatus();
	}

	/**
	 * Show bike station status (start refreshing if necessary).
	 */
	private void showBikeStationStatus() {
		MyLog.v(TAG, "showBikeStationStatus()");
		// compute the too old date
		int tooOld = Utils.currentTimeSec() - CACHE_TOO_OLD_IN_SEC;
		if (tooOld >= getLastUpdateTime()) {
			refreshStatus(null); // asynchronously start refreshing
		}
		showNewBikeStationStatus();
	}

	/**
	 * @return the last update time
	 */
	public int getLastUpdateTime() {
		int timestamp = 0;
		timestamp = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0);
		if (timestamp == 0 && this.bikeStation != null) {
			 timestamp = this.bikeStation.getLatestUpdateTime();
		}
		if (timestamp == 0) {
			timestamp =  Utils.currentTimeSec(); // use device time
		}
		return timestamp;
	}

	/**
	 * Show new bike station status.
	 */
	public void showNewBikeStationStatus() {
		MyLog.v(TAG, "showNewBikeStationStatus()");
		RelativeLayout availabilityLayout = (RelativeLayout) findViewById(R.id.availability);
		// bikes #
		String bikeString;
		switch (this.bikeStation.getNbBikes()) {
		case 0:
			bikeString = getString(R.string.bike_no);
			break;
		case 1:
			bikeString = getString(R.string.bike_and_nb, this.bikeStation.getNbBikes());
			break;
		default:
			bikeString = getString(R.string.bikes_and_nb, this.bikeStation.getNbBikes());
			break;
		}
		TextView bikeTv = (TextView) availabilityLayout.findViewById(R.id.progress_bike);
		bikeTv.setText(bikeString);
		bikeTv.setTypeface(this.bikeStation.getNbBikes() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		// dock #
		String emptyDocksString;
		switch (this.bikeStation.getNbEmptyDocks()) {
		case 0:
			emptyDocksString = getString(R.string.dock_no);
			break;
		case 1:
			emptyDocksString = getString(R.string.dock_and_nb, this.bikeStation.getNbEmptyDocks());
			break;
		default:
			emptyDocksString = getString(R.string.docks_and_nb, this.bikeStation.getNbEmptyDocks());
			break;
		}
		TextView emptyDockTv = (TextView) availabilityLayout.findViewById(R.id.progress_dock);
		emptyDockTv.setText(emptyDocksString);
		emptyDockTv.setTypeface(this.bikeStation.getNbEmptyDocks() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		// progress bar
		ProgressBar progressBar = (ProgressBar) availabilityLayout.findViewById(R.id.progress_bar);
		progressBar.setMax(this.bikeStation.getNbTotalDocks());
		progressBar.setProgress(this.bikeStation.getNbBikes());
		progressBar.setIndeterminate(false);
		// show last update time
		int timestamp = getLastUpdateTime();
		if (timestamp != 0) {
			CharSequence readTime = Utils.formatSameDayDateInSec(timestamp);
			final String sectionTitle = getString(R.string.bike_station_status_hour, readTime);
			((TextView) findViewById(R.id.availability_title).findViewById(R.id.availability_title_string)).setText(sectionTitle);
		} else {
			((TextView) findViewById(R.id.availability_title).findViewById(R.id.availability_title_string)).setText(R.string.bike_station_status);
		}

		findViewById(R.id.availability_loading).setVisibility(View.GONE);
		findViewById(R.id.availability).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(this.getContentResolver(), Fav.KEY_TYPE_VALUE_BIKE_STATIONS, this.bikeStation.getTerminalName(), null);
		((CheckBox) findViewById(R.id.star)).setChecked(findFav != null);
		findViewById(R.id.star).setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bike_station_info_menu);
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
			this.closestBikeStationsTask = null;
			this.closestBikeStations = null;
		}
		if (this.task != null) {
			this.task.cancel(false);
			this.task = null;
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}

}

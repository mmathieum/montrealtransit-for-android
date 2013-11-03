package org.montrealtransit.android.activity;

import java.util.List;
import java.util.Map;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BikeUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.services.BixiDataReader;
import org.montrealtransit.android.services.BixiDataReader.BixiDataReaderListener;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask.ClosestBikeStationsFinderListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class BikeStationInfo extends Activity implements BixiDataReaderListener, ClosestBikeStationsFinderListener, LocationListener, SensorEventListener,
		CompassListener {

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
	private static final String EXTRA_STATION_TERMINAL_NAME = "extra_bike_station_terminal_name";
	/**
	 * The extra ID for the bike station name (optional).
	 */
	private static final String EXTRA_STATION_NAME = "extra_bike_station_name";
	/**
	 * The bike station.
	 */
	private ABikeStation bikeStation;
	/**
	 * The terminal name of the bike station for which the current closest station are.
	 */
	private String closestBikeStationTerminalName;
	/**
	 * The task used to load the closest bike stations.
	 */
	private ClosestBikeStationsFinderTask closestBikeStationsTask;
	/**
	 * The task used to load the new bike station info.
	 */
	private BixiDataReader bixiDataReaderTask;
	/**
	 * The last message from the {@link BixiDataReader}.
	 */
	private String lastBixiDataMessage;
	/**
	 * Is the location updates should be enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass updates should be enabled?
	 */
	private boolean compassUpdatesEnabled = false;
	/**
	 * The device location.
	 */
	private Location location;
	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];
	/**
	 * The last compass degree.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;

	private POIArrayAdapter adapter;

	public static Intent newInstance(Context context, String stationTerminalName) {
		return newInstance(context, stationTerminalName, null);
	}

	public static Intent newInstance(Context context, BikeStation bikeStation) {
		return newInstance(context, bikeStation.getTerminalName(), bikeStation.getName());
	}

	public static Intent newInstance(Context context, String stationTerminalName, String stationName) {
		Intent intent = new Intent(context, BikeStationInfo.class);
		intent.putExtra(EXTRA_STATION_TERMINAL_NAME, stationTerminalName);
		if (stationName != null) {
			intent.putExtra(EXTRA_STATION_NAME, stationName);
		}
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bike_station_info);

		this.adapter = new POIArrayAdapter(this);
		this.adapter.setShowData(true);
		this.adapter.setManualLayout((ViewGroup) findViewById(R.id.nearby_list));
		this.adapter.setManualScrollView((ScrollView) findViewById(R.id.scrollview));

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
	}

	/**
	 * {@link #onCreate(Bundle)} method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		findViewById(R.id.availability_refresh).setOnClickListener(new View.OnClickListener() {
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
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		setIntent(intent);
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
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		if (!this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		setBikeStationFromIntent(getIntent(), null);
		setIntent(null); // set intent as processed
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onResume()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.compassUpdatesEnabled = false;
		}
		this.adapter.onPause();
		super.onPause();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged(%s)", accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForCompass(this, event, this.accelerometerValues, this.magneticFieldValues, this);
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s, %s)", orientation, force);
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							if (BikeStationInfo.this.bikeStation != null) {
								BikeStationInfo.this.lastCompassInDegree = (int) orientation;
								BikeStationInfo.this.lastCompassChanged = now;
							}
							if (BikeStationInfo.this.bikeStation != null) {
								ImageView compassImg = (ImageView) findViewById(R.id.compass);
								if (location.getAccuracy() <= bikeStation.getDistance()) {
									float compassRotation = SensorUtils.getCompassRotationInDegree(location, bikeStation, lastCompassInDegree,
											locationDeclination);
									SupportFactory.get().rotateImageView(compassImg, compassRotation, BikeStationInfo.this);
									compassImg.setVisibility(View.VISIBLE);
								} else {
									compassImg.setVisibility(View.INVISIBLE);
								}
							}
						}
					}
				});
	}

	private float locationDeclination;

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistancesWithNewLocation(Location currentLocation) {
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.bikeStation != null) {
			// distance & accuracy
			LocationUtils.updateDistanceWithString(this, this.bikeStation, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					TextView distanceTv = (TextView) findViewById(R.id.distance);
					distanceTv.setText(BikeStationInfo.this.bikeStation.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				}
			});
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
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
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
				this.adapter.setLocation(this.location);
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
				}
				updateDistancesWithNewLocation(this.location);
			}
		}
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

	private void setBikeStationFromIntent(Intent intent, Bundle savedInstanceState) {
		// MyLog.v(TAG, "setBikeStationFromIntent()");
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
		MyLog.v(TAG, "refreshStatus()");
		// IF the task is running DO
		if (this.bixiDataReaderTask != null && this.bixiDataReaderTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// do NOT cancel, useful
			// stopping the task
			// this.task.cancel(true);
			// this.task = null;
			return;
		} // else
		if (this.bikeStation != null) {
			setStatusAsLoading();
			// check if it's not too soon
			int waitFor = getLastUpdateTime() + BikeUtils.CACHE_TOO_FRESH_IN_SEC - Utils.currentTimeSec();
			// load new bike station data
			this.bixiDataReaderTask = new BixiDataReader(this, this, waitFor > 0 ? waitFor : 0);
			this.bixiDataReaderTask.execute(this.bikeStation.getTerminalName());
		}
	}

	@Override
	public void onBixiDataProgress(String progress) {
		MyLog.v(TAG, "onBixiDataProgress(%s)", progress);
		BikeStationInfo.this.lastBixiDataMessage = progress;
		// nothing
	}

	@Override
	public void onBixiDataLoaded(List<BikeStation> newBikeStations, boolean isNew) {
		MyLog.v(TAG, "onBixiDataLoaded(%s,%s)", Utils.getCollectionSize(newBikeStations), isNew);
		// IF the bike station was returned as expected DO
		if (Utils.getCollectionSize(newBikeStations) == 1) {
			this.adapter.setLastSuccessfulRefresh(UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, -1));
			this.bikeStation = new ABikeStation(newBikeStations.get(0));
			updateDistancesWithNewLocation(getLocation());
			showNewBikeStationStatus();
			showNewClosestBikeStationsStatus();
			setStatusNotLoading();
		} else {
			setStatusNotLoading();
			setStatusError();
		}
	}

	private void showNewClosestBikeStationsStatus() {
		MyLog.v(TAG, "showNewClosestBikeStationsStatus()");
		if (this.adapter.getPois() == null) {
			return;
		}
		// load new closest stations data
		Map<String, BikeStation> newStations = BixiManager.findBikeStationsMap(getContentResolver(), getTerminalNamesFromPOI(this.adapter.getPois()));
		if (newStations == null) {
			return;
		}
		for (POI poi : this.adapter.getPois()) {
			if (poi instanceof ABikeStation) {
				ABikeStation station = (ABikeStation) poi;
				BikeStation newStation = newStations.get(station.getTerminalName());
				if (newStation == null) {
					continue;
				}
				// update data
				station.setLatestUpdateTime(newStation.getLatestUpdateTime());
				station.setNbBikes(newStation.getNbBikes());
				station.setNbEmptyDocks(newStation.getNbEmptyDocks());
				// TODO more ?
			}
		}
		this.adapter.notifyDataSetChanged(true);
	}

	/**
	 * @param bikeStations the bike stations
	 * @return terminal names
	 */
	public static String getTerminalNames(List<? extends BikeStation> bikeStations) {
		StringBuilder sb = new StringBuilder();
		for (BikeStation station : bikeStations) {
			if (sb.length() > 0) {
				sb.append("+");
			}
			sb.append(station.getTerminalName());
		}
		return sb.toString();
	}

	public static String getTerminalNamesFromPOI(List<? extends POI> pois) {
		StringBuilder sb = new StringBuilder();
		for (POI poi : pois) {
			if (poi instanceof ABikeStation) {
				if (sb.length() > 0) {
					sb.append("+");
				}
				sb.append(((ABikeStation) poi).getTerminalName());
			}
		}
		return sb.toString();
	}

	/**
	 * Show error.
	 */
	private void setStatusError() {
		MyLog.v(TAG, "setStatusError()");
		String errorMsg = getString(R.string.error);
		if (!TextUtils.isEmpty(this.lastBixiDataMessage)) {
			errorMsg = this.lastBixiDataMessage;
		}
		Toast toast = Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP, 0, Constant.TOAST_Y_OFFSET);
		toast.show();
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
		SupportFactory.get().backupManagerDataChanged(this);
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
		findViewById(R.id.nearby_title).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.GONE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		this.adapter.setPois(null);

		if (BikeStationInfo.this.bikeStation == null || !BikeStationInfo.this.bikeStation.getTerminalName().equals(newBikeStationTerminalName)) {
			findViewById(R.id.availability).setVisibility(View.GONE);
			findViewById(R.id.availability_loading).setVisibility(View.VISIBLE);
			new AsyncTask<String, Void, BikeStation>() {

				@Override
				protected BikeStation doInBackground(String... params) {
					BikeStation result = null;
					boolean success = false;
					do {
						try {
							result = BixiManager.findBikeStation(getContentResolver(), params[0]);
							success = true;
						} catch (Exception e) {
							success = false;
							MyLog.d(TAG, "Error... wait 3 seconds and retry", e);
							try {
								Thread.sleep(3 * 1000);
							} catch (InterruptedException ie) {
							}
						}
					} while (!success);
					return result;
				}

				@Override
				protected void onPostExecute(BikeStation result) {
					// do NOT cancel, useful for everyone
					// if (BikeStationInfo.this.task != null) {
					// BikeStationInfo.this.task.cancel(true);
					// BikeStationInfo.this.task = null;
					// }
					// IF no bike station found (bike station removed ...) DO
					if (result == null) {
						BikeStationInfo.this.finish(); // close the activity
					} else {
						BikeStationInfo.this.bikeStation = new ABikeStation(result);
						setUpUI();
					}
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
		refreshNearby(false);
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh closest bike stations.
	 */
	private void refreshNearby(boolean force) {
		MyLog.v(TAG, "refreshNearby(%s)", force);
		// IF refresh required DO
		if (force || BikeStationInfo.this.adapter.getPoisCount() == 0 || this.closestBikeStationTerminalName == null
				|| !this.closestBikeStationTerminalName.equals(this.bikeStation.getTerminalName())) {
			// stop current task is running
			if (this.closestBikeStationsTask != null) {
				this.closestBikeStationsTask.cancel(true);
				this.adapter.setPois(null);
			}
			findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
			findViewById(R.id.nearby_list).setVisibility(View.GONE);
			findViewById(R.id.nearby_loading).setVisibility(View.VISIBLE);
			this.closestBikeStationsTask = new ClosestBikeStationsFinderTask(this, this, Utils.NB_NEARBY_LIST + 1, force);
			this.closestBikeStationsTask.execute(this.bikeStation.getLat(), this.bikeStation.getLng());
		}
	}

	@Override
	public void onClosestBikeStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress(%s)", progress);
		// do nothing
	}

	@Override
	public void onClosestBikeStationsDone(ClosestPOI<ABikeStation> result) {
		MyLog.v(TAG, "onClosestBikeStationsDone()");
		if (result != null && result.getPoiListSize() > 0) {
			// MyLog.d(TAG, "onClosestBikeStationsDone() > result not empty");
			this.adapter.setPois(result.getPoiList().subList(1, result.getPoiList().size()));
			this.adapter.setLastSuccessfulRefresh(UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, -1));
			refreshFavoriteTerminalNamesFromDB();
			// set location
			this.adapter.updateDistancesNow(getLocation());
			this.adapter.initManual();
			findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
			findViewById(R.id.nearby_loading).setVisibility(View.GONE);
			findViewById(R.id.nearby_list).setVisibility(View.VISIBLE);
			// } else {
			// MyLog.d(TAG, "onClosestBikeStationsDone() > result null or empty");
		}
	}

	/**
	 * Find favorites bike stations terminal names.
	 */
	private void refreshFavoriteTerminalNamesFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				MyLog.v(TAG, "refreshFavoriteTerminalNamesFromDB()>onPostExecute()");
				BikeStationInfo.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * @param station the bike station
	 * @param view the view containing the status
	 * @param lastUpdate last update time (in second)
	 */
	public void setBikeStationStatus(ABikeStation station, View view, final int lastUpdate) {
		MyLog.v(TAG, "setBikeStationStatus(%s)", lastUpdate);
		ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		TextView dockTv = (TextView) view.findViewById(R.id.progress_dock);
		TextView bikeTv = (TextView) view.findViewById(R.id.progress_bike);
		int waitFor = Utils.currentTimeSec() - BikeUtils.CACHE_NOT_USEFUL_IN_SEC - lastUpdate;
		if (waitFor < 0) {
			// MyLog.d(TAG, "Cache useful (closest).");
			if (!station.isInstalled()) {
				bikeTv.setText(R.string.bike_station_not_installed);
				bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
				dockTv.setVisibility(View.GONE);
			} else if (station.isLocked()) {
				bikeTv.setText(R.string.bike_station_locked);
				bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
				dockTv.setVisibility(View.GONE);
			} else {
				// bikes #
				bikeTv.setText(getResources().getQuantityString(R.plurals.bikes_nb, station.getNbBikes(), station.getNbBikes()));
				bikeTv.setTypeface(station.getNbBikes() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				bikeTv.setVisibility(View.VISIBLE);
				// dock #
				dockTv.setText(getResources().getQuantityString(R.plurals.docks_nb, station.getNbEmptyDocks(), station.getNbEmptyDocks()));
				dockTv.setTypeface(station.getNbEmptyDocks() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				dockTv.setVisibility(View.VISIBLE);
			}
			// progress bar
			progressBar.setIndeterminate(false);
			progressBar.setMax(station.getNbTotalDocks());
			progressBar.setProgress(station.getNbBikes());
		} else { // loading...
			// MyLog.d(TAG, "Cache NOT useful (closest).");
			bikeTv.setText(R.string.ellipsis);
			bikeTv.setTypeface(Typeface.DEFAULT);
			dockTv.setText(R.string.ellipsis);
			dockTv.setTypeface(Typeface.DEFAULT);
			progressBar.setIndeterminate(true);
		}
	}

	/**
	 * Refresh bike station info.
	 */
	private void refreshBikeStationInfo() {
		MyLog.v(TAG, "refreshBikeStationInfo()");
		// MyLog.d(TAG, "this.bikeStatio null? %s", this.bikeStation == null);
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
		if (Utils.currentTimeSec() - BikeUtils.CACHE_TOO_OLD_IN_SEC - getLastUpdateTime() > 0) {
			refreshStatus(null); // asynchronously start refreshing
		}
		showNewBikeStationStatus();
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

	/**
	 * Show new bike station status.
	 */
	private void showNewBikeStationStatus() {
		MyLog.v(TAG, "showNewBikeStationStatus()");
		RelativeLayout availabilityLayout = (RelativeLayout) findViewById(R.id.availability);
		ProgressBar progressBar = (ProgressBar) availabilityLayout.findViewById(R.id.progress_bar);
		TextView bikeTv = (TextView) availabilityLayout.findViewById(R.id.progress_bike);
		TextView dockTv = (TextView) availabilityLayout.findViewById(R.id.progress_dock);
		boolean cacheUseful = Utils.currentTimeSec() - BikeUtils.CACHE_NOT_USEFUL_IN_SEC - getLastUpdateTime() < 0;
		if (cacheUseful) {
			// MyLog.d(TAG, "Cache useful.");
			if (!this.bikeStation.isInstalled()) {
				bikeTv.setText(R.string.bike_station_not_installed);
				bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
				dockTv.setVisibility(View.GONE);
			} else if (this.bikeStation.isLocked()) {
				bikeTv.setText(R.string.bike_station_locked);
				bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
				dockTv.setVisibility(View.GONE);
			} else {
				// bikes #
				bikeTv.setText(getResources().getQuantityString(R.plurals.bikes_nb, this.bikeStation.getNbBikes(), this.bikeStation.getNbBikes()));
				bikeTv.setTypeface(this.bikeStation.getNbBikes() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				bikeTv.setVisibility(View.VISIBLE);
				// dock #
				dockTv.setText(getResources().getQuantityString(R.plurals.docks_nb, this.bikeStation.getNbEmptyDocks(), this.bikeStation.getNbEmptyDocks()));
				dockTv.setTypeface(this.bikeStation.getNbEmptyDocks() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				dockTv.setVisibility(View.VISIBLE);
			}
			// progress bar
			progressBar.setIndeterminate(false);
			progressBar.setMax(this.bikeStation.getNbTotalDocks());
			progressBar.setProgress(this.bikeStation.getNbBikes());
			// show last update time
			int timestamp = getLastUpdateTime();
			if (timestamp != 0) {
				CharSequence readTime = Utils.formatSameDayDateInSec(timestamp);
				final String sectionTitle = getString(R.string.bike_station_status_hour, readTime);
				((TextView) findViewById(R.id.availability_title).findViewById(R.id.availability_title_string)).setText(sectionTitle);
			} else {
				((TextView) findViewById(R.id.availability_title).findViewById(R.id.availability_title_string)).setText(R.string.bike_station_status);
			}
		} else { // loading...
			// MyLog.d(TAG, "Cache NOT useful.");
			bikeTv.setText(R.string.ellipsis);
			bikeTv.setTypeface(Typeface.DEFAULT);
			dockTv.setText(R.string.ellipsis);
			dockTv.setTypeface(Typeface.DEFAULT);
			progressBar.setIndeterminate(true);
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
		new AsyncTask<Void, Void, Fav>() {
			@Override
			protected Fav doInBackground(Void... params) {
				return DataManager.findFav(BikeStationInfo.this.getContentResolver(), Fav.KEY_TYPE_VALUE_BIKE_STATIONS,
						BikeStationInfo.this.bikeStation.getTerminalName(), null);
			}

			protected void onPostExecute(Fav result) {
				final CheckBox starCb = (CheckBox) findViewById(R.id.star);
				starCb.setChecked(result != null);
				starCb.setVisibility(View.VISIBLE);
			};
		}.execute();
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
			if (this.adapter != null) {
				this.adapter.setPois(null);
			}
		}
		if (this.bixiDataReaderTask != null) {
			this.bixiDataReaderTask.cancel(false);
			this.bixiDataReaderTask = null;
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}

}

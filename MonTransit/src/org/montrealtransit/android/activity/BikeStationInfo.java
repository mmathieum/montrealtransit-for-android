package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.Pair;
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
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
	public static final String EXTRA_STATION_TERMINAL_NAME = "extra_bike_station_terminal_name";
	/**
	 * The extra ID for the bike station name (optional).
	 */
	public static final String EXTRA_STATION_NAME = "extra_bike_station_name";

	/**
	 * The validity of the cache (in seconds).
	 */
	private static final int CACHE_TOO_OLD_IN_SEC = 2 * 60; // 2 minutes
	/**
	 * The cache is too old to be useful, don't display it.
	 */
	private static final int CACHE_NOT_USEFUL_IN_SEC = 10 * 60; // 10 minutes
	/**
	 * No DDOS on the server!
	 */
	private static final int CACHE_TOO_FRESH_IN_SEC = 1 * 60; // 1 minute
	/**
	 * The number of closest bike station displayed.
	 */
	private static final int NB_CLOSEST_BIKE_STATIONS = 7;

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
	 * The device location.
	 */
	private Location location;

	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass degree.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The favorites bike station terminal names.
	 */
	private List<String> favTerminalNames;

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

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;

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
				updateDistancesWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		setBikeStationFromIntent(getIntent(), null);
		setIntent(null); // set intent as processed
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onResume()");
		LocationUtils.disableLocationUpdates(this, this);
		this.locationUpdatesEnabled = false;
		SensorUtils.unregisterSensorListener(this, this);
		super.onPause();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged(%s)", accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		// SensorUtils.checkForCompass(event, this.accelerometerValues, this.magneticFieldValues, this);
		checkForCompass(event, this);
	}

	/**
	 * @see SensorUtils#checkForCompass(SensorEvent, float[], float[], CompassListener)
	 */
	public void checkForCompass(SensorEvent event, CompassListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerValues = event.values;
			if (magneticFieldValues != null) {
				listener.onCompass();
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magneticFieldValues = event.values;
			if (accelerometerValues != null) {
				listener.onCompass();
			}
			break;
		}
	}

	@Override
	public void onCompass() {
		// MyLog.v(TAG, "onCompass()");
		if (this.accelerometerValues != null && this.magneticFieldValues != null) {
			updateCompass(SensorUtils.calculateOrientation(this, this.accelerometerValues, this.magneticFieldValues));
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	private void updateCompass(float[] orientation) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update bike station compass
				if (this.bikeStation != null) {
					Matrix matrix = new Matrix();
					matrix.postRotate(SensorUtils.getCompassRotationInDegree(this, currentLocation, this.bikeStation.getLocation(), orientation,
							getLocationDeclination()), getArrowDimLight().first / 2, getArrowDimLight().second / 2);
					ImageView compassImg = (ImageView) findViewById(R.id.compass);
					compassImg.setImageMatrix(matrix);
					compassImg.setVisibility(View.VISIBLE);
				}
				// update closest bike stations compass
				if (this.closestBikeStations != null) {
					final View closestStations = findViewById(R.id.closest_stations);
					for (ABikeStation station : this.closestBikeStations) {
						station.getCompassMatrix().reset();
						station.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this, currentLocation, station.getLocation(), orientation, getLocationDeclination()),
								getArrowDim().first / 2, getArrowDim().second / 2);
						ImageView compassImg = (ImageView) closestStations.findViewWithTag(getStationViewTag(station)).findViewById(R.id.compass);
						compassImg.setImageMatrix(station.getCompassMatrix());
						compassImg.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}

	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	private Pair<Integer, Integer> arrowDim;

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	private Pair<Integer, Integer> arrowDimLight;

	public Pair<Integer, Integer> getArrowDimLight() {
		if (this.arrowDimLight == null) {
			this.arrowDimLight = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow_light);
		}
		return this.arrowDimLight;
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		Location currentLocation = getLocation();
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.bikeStation != null) {
			// distance & accuracy
			TextView distanceTv = (TextView) findViewById(R.id.distance);
			distanceTv.setText(Utils.getDistanceStringUsingPref(this, currentLocation.distanceTo(bikeStation.getLocation()), currentLocation.getAccuracy()));
			distanceTv.setVisibility(View.VISIBLE);
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
		for (ABikeStation station : this.closestBikeStations) {
			View stationView = findViewById(R.id.closest_stations).findViewWithTag(getStationViewTag(station));
			if (stationView != null && !TextUtils.isEmpty(station.getDistanceString())) {
				TextView distanceTv = (TextView) stationView.findViewById(R.id.distance);
				distanceTv.setText(station.getDistanceString());
				distanceTv.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * @param station the station (for terminal name)
	 * @return the station view tab with this index
	 */
	private static String getStationViewTag(BikeStation station) {
		return "station" + station.getTerminalName();
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
				station.setDistance(location.distanceTo(station.getLocation()));
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
				SensorUtils.registerCompassListener(this, this);
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
		MyLog.v(TAG, "refreshStatus()");
		// IF the task is running DO
		if (this.bixiDataReaderTask != null && this.bixiDataReaderTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// do NOT cancel, useful
			// stopping the task
			// this.task.cancel(true);
			// this.task = null;
			return;
		}// else
		if (this.bikeStation != null) {
			setStatusAsLoading();
			// check if it's not too soon
			int waitFor = getLastUpdateTime() + CACHE_TOO_FRESH_IN_SEC - Utils.currentTimeSec();
			// find the next bus stop
			this.bixiDataReaderTask = new BixiDataReader(this, this, waitFor > 0 ? waitFor : 0);
			this.bixiDataReaderTask.execute(this.bikeStation.getTerminalName());
		}
	}

	@Override
	public void onBixiDataProgress(String progress) {
		MyLog.v(TAG, "onBixiDataProgress(%s)", progress);
		this.lastBixiDataMessage = progress;
		// nothing
	}

	@Override
	public void onBixiDataLoaded(List<BikeStation> newBikeStations, boolean isNew) {
		// MyLog.v(TAG, "onBixiDataLoaded(%s,%s)", Utils.getCollectionSize(newBikeStations), isNew);
		// IF the bike station was returned as expected DO
		if (Utils.getCollectionSize(newBikeStations) == 1) {
			this.bikeStation = newBikeStations.get(0);
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
		if (this.closestBikeStations == null) {
			return;
		}
		// load new closest stations data
		Map<String, BikeStation> newStations = BixiManager.findBikeStationsMap(getContentResolver(), getTerminalNames(this.closestBikeStations));
		if (newStations == null) {
			return;
		}
		LinearLayout closestStationsLayout = (LinearLayout) findViewById(R.id.closest_stations);
		int timestamp = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0);
		ListIterator<ABikeStation> it = this.closestBikeStations.listIterator();
		while (it.hasNext()) {
			ABikeStation station = it.next();
			BikeStation newStation = newStations.get(station.getTerminalName());
			if (newStation != null) {
				// update data
				station.setLatestUpdateTime(newStation.getLatestUpdateTime());
				station.setNbBikes(newStation.getNbBikes());
				station.setNbEmptyDocks(newStation.getNbEmptyDocks());
				// TODO more ?
				// update UI
				View stationView = closestStationsLayout.findViewWithTag(getStationViewTag(station));
				if (stationView != null) {
					final int lastUpdate = timestamp != 0 ? timestamp : station.getLatestUpdateTime();
					setBikeStationStatus(station, stationView, lastUpdate);
				}
			}
		}
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

	/**
	 * Show error.
	 */
	private void setStatusError() {
		MyLog.v(TAG, "setStatusError()");
		String errorMsg = getString(R.string.error);
		if (!TextUtils.isEmpty(this.lastBixiDataMessage)) {
			errorMsg = this.lastBixiDataMessage;
		}
		Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
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
					BikeStationInfo.this.bikeStation = result;
					// do NOT cancel, useful for everyone
					// if (BikeStationInfo.this.task != null) {
					// BikeStationInfo.this.task.cancel(true);
					// BikeStationInfo.this.task = null;
					// }
					// IF no bike station found (bike station removed ...) DO
					if (BikeStationInfo.this.bikeStation == null) {
						BikeStationInfo.this.finish(); // close the activity
					} else {
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
		refreshClosestBikeStations(false);
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
	private void refreshClosestBikeStations(boolean force) {
		MyLog.v(TAG, "refreshClosestBikeStations(%s)", force);
		// IF refresh required DO
		if (Utils.getCollectionSize(this.closestBikeStations) == 0 || this.closestBikeStationTerminalName == null
				|| !this.closestBikeStationTerminalName.equals(this.bikeStation.getTerminalName())) {
			// stop current task is running
			if (this.closestBikeStationsTask != null) {
				this.closestBikeStationsTask.cancel(true);
				this.closestBikeStations = null;
			}
			setClosestStationsAsLoading(); // set as loading
			this.closestBikeStationsTask = new ClosestBikeStationsFinderTask(this, this, NB_CLOSEST_BIKE_STATIONS + 1, force);
			this.closestBikeStationsTask.execute(this.bikeStation.getLocation());
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
		if (result != null && result.getPoiListSize() > 0) {
			this.closestBikeStations = result.getPoiList().subList(1, result.getPoiList().size());
			refreshFavoriteTerminalNamesFromDB();
			// set location
			setClosestStationsDistances(getLocation());
			// shot the result
			showNewClosestStations();
			// hide loading
			setClosestStationsAsNotLoading();
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
				boolean newFav = false; // don't trigger update if favorites are the same
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BikeStationInfo.this.favTerminalNames)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavTerminalNames = new ArrayList<String>();
				for (Fav bikeStationFav : result) {
					if (BikeStationInfo.this.favTerminalNames == null || !BikeStationInfo.this.favTerminalNames.contains(bikeStationFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavTerminalNames.add(bikeStationFav.getFkId()); // store terminal name
				}
				BikeStationInfo.this.favTerminalNames = newfavTerminalNames;
				// trigger change if necessary
				if (newFav) {
					// notifyDataSetChanged(true);
					final View closestStations = findViewById(R.id.closest_stations);
					for (ABikeStation station : BikeStationInfo.this.closestBikeStations) {
						View stationView = closestStations.findViewWithTag(getStationViewTag(station));
						if (stationView != null) {
							if (BikeStationInfo.this.favTerminalNames != null && BikeStationInfo.this.favTerminalNames.contains(station.getTerminalName())) {
								stationView.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
							} else {
								stationView.findViewById(R.id.fav_img).setVisibility(View.GONE);
							}
						}
					}
				}
			};
		}.execute();
	}

	/**
	 * Show new closest stations.
	 */
	private void showNewClosestStations() {
		MyLog.v(TAG, "showNewClosestStations()");
		LinearLayout closestStationsLayout = (LinearLayout) findViewById(R.id.closest_stations);
		// clear the previous list
		closestStationsLayout.removeAllViews();
		int timestamp = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0);
		// show stations list
		for (ABikeStation station : this.closestBikeStations) {
			// list view divider
			if (closestStationsLayout.getChildCount() > 0) {
				closestStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, closestStationsLayout, false));
			}
			// create view
			View view = getLayoutInflater().inflate(R.layout.bike_station_info_closest_stations_list_item, closestStationsLayout, false);
			view.setTag(getStationViewTag(station));
			// bike station name
			((TextView) view.findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(station.getName()));
			// status (not installed, locked..)
			if (!station.isInstalled() || station.isLocked()) {
				((TextView) view.findViewById(R.id.station_name)).setTextColor(Utils.getTextColorSecondary(BikeStationInfo.this));
			} else {
				((TextView) view.findViewById(R.id.station_name)).setTextColor(Utils.getTextColorPrimary(BikeStationInfo.this));
			}
			// bike station distance
			TextView distanceTv = (TextView) view.findViewById(R.id.distance);
			if (!TextUtils.isEmpty(station.getDistanceString())) {
				distanceTv.setText(station.getDistanceString());
				distanceTv.setVisibility(View.VISIBLE);
			} else {
				distanceTv.setVisibility(View.GONE);
				distanceTv.setText(null);
			}
			// compass
			view.findViewById(R.id.compass).setVisibility(View.GONE);
			// favorite
			if (BikeStationInfo.this.favTerminalNames != null && BikeStationInfo.this.favTerminalNames.contains(station.getTerminalName())) {
				view.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.fav_img).setVisibility(View.GONE);
			}
			// status
			final int lastUpdate = timestamp != 0 ? timestamp : station.getLatestUpdateTime();
			setBikeStationStatus(station, view, lastUpdate);
			// add click listener
			final String terminalName = station.getTerminalName();
			final String name = station.getName();
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(%s)", v.getId());
					showNewBikeStation(terminalName, name);
				}
			});
			closestStationsLayout.addView(view);
		}
	}

	/**
	 * @param station the bike station
	 * @param view the view containing the status
	 * @param lastUpdate last update time (in second)
	 */
	public void setBikeStationStatus(ABikeStation station, View view, final int lastUpdate) {
		ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		TextView dockTv = (TextView) view.findViewById(R.id.progress_dock);
		TextView bikeTv = (TextView) view.findViewById(R.id.progress_bike);
		int waitFor = Utils.currentTimeSec() - CACHE_NOT_USEFUL_IN_SEC - lastUpdate;
		if (waitFor < 0) {
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
		if (Utils.currentTimeSec() - CACHE_TOO_OLD_IN_SEC - getLastUpdateTime() > 0) {
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
			timestamp = Utils.currentTimeSec(); // use device time
		}
		return timestamp;
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
		int waitFor = Utils.currentTimeSec() - CACHE_NOT_USEFUL_IN_SEC - getLastUpdateTime();
		if (waitFor < 0) {
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
		if (this.bixiDataReaderTask != null) {
			this.bixiDataReaderTask.cancel(false);
			this.bixiDataReaderTask = null;
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}

}

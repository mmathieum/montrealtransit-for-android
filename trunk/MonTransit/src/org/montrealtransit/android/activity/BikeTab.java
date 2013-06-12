package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BikeUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask;
import org.montrealtransit.android.services.ClosestBikeStationsFinderTask.ClosestBikeStationsFinderListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Display a list of bike stations.
 * @author Mathieu Méa
 */
public class BikeTab extends Activity implements LocationListener, ClosestBikeStationsFinderListener, SensorEventListener, ShakeListener, OnItemClickListener,
		CompassListener, OnScrollListener {

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
	private boolean compassUpdatesEnabled = false;
	/**
	 * The closest bike stations (with distance => ordered).
	 */
	private List<ABikeStation> closestStations;
	/**
	 * The location used to generate the closest bike stations list.
	 */
	private Location closestBikeStationsLocation;
	/**
	 * The location address used to generate the closest bike stations list;
	 */
	private Address closestBikeStationsLocationAddress;
	/**
	 * The bike stations list adapter.
	 */
	private ArrayAdapter<ABikeStation> adapter;
	/**
	 * The closest bike station ID by distance.
	 */
	private String closestStationTerminalName;
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
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;
	/**
	 * The list scroll state.
	 */
	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;
	/**
	 * The favorites bike station terminal names.
	 */
	private List<String> favTerminalNames;
	/**
	 * The time-stamp of the last data refresh from www.
	 */
	private int lastSuccessfulRefresh = 0;
	/**
	 * Last try (device time).
	 */
	private int lastForcedRefresh = 0;

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
		list.setOnItemClickListener(this);
		list.setOnScrollListener(this);
		this.adapter = new ArrayAdapterWithCustomView(this, R.layout.bike_station_tab_closest_stations_list_item);
		list.setAdapter(this.adapter);
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
					return LocationUtils.getBestLastKnownLocation(BikeTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// IF there is a valid last know location DO
					if (result != null) {
						// set the new distance
						setLocation(result);
						updateDistancesWithNewLocation();
					}
					// re-enable
					LocationUtils.enableLocationUpdates(BikeTab.this, BikeTab.this);
					BikeTab.this.locationUpdatesEnabled = true;
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		refreshFavoriteTerminalNamesFromDB();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		this.locationUpdatesEnabled = false;
		SensorUtils.unregisterSensorListener(this, this);
		this.compassUpdatesEnabled = false;
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(event, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onShake() {
		MyLog.v(TAG, "onShake()");
		showClosestStation();
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
		// MyLog.v(TAG, "updateCompass(%s)", orientation[0]);
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			long now = System.currentTimeMillis();
			if (io != 0 && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE && (now - this.lastCompassChanged) > SensorUtils.COMPASS_UPDATE_THRESOLD
					&& Math.abs(this.lastCompassInDegree - io) > SensorUtils.COMPASS_DEGREE_UPDATE_THRESOLD) {
				// update closest bike stations compass
				if (this.closestStations != null) {
					this.lastCompassInDegree = io;
					this.lastCompassChanged = now;
					for (ABikeStation station : this.closestStations) {
						station.getCompassMatrix().reset();
						station.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this, currentLocation, station.getLocation(), orientation, getLocationDeclination()),
								getArrowDim().first / 2, getArrowDim().second / 2);
					}
					// update the view
					notifyDataSetChanged(false);
				}
			}
		}
	}

	/**
	 * The minimum between 2 {@link ArrayAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	private static final int ADAPTER_NOTIFY_THRESOLD = 150; // 0.15 seconds

	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	private Pair<Integer, Integer> arrowDim;
	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (view == findViewById(R.id.list)) {
			this.scrollState = scrollState;
		}
	}

	/**
	 * Show the closest subway line station (if possible).
	 */
	private void showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		if (this.hasFocus && !this.shakeHandled && !TextUtils.isEmpty(this.closestStationTerminalName)) {
			Toast.makeText(this, R.string.shake_closest_bike_station_selected, Toast.LENGTH_SHORT).show();
			// show bike station view
			Intent intent = new Intent(this, BikeStationInfo.class);
			intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, this.closestStationTerminalName);
			intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, findStationName(this.closestStationTerminalName));
			startActivity(intent);
			this.shakeHandled = true;
		}
	}

	/**
	 * @param terminalName a bike station terminal name
	 * @return a bike station name or null
	 */
	private String findStationName(String terminalName) {
		if (this.closestStations != null) {
			for (BikeStation bikeStation : this.closestStations) {
				if (bikeStation.getTerminalName().equals(terminalName)) {
					return bikeStation.getName();
				}
			}
		}
		return null;
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.compassUpdatesEnabled = true;
					this.shakeHandled = false;
				}
			}
		}
	}

	/**
	 * Update the bike stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		// IF no closest bike stations AND new location DO
		Location currentLocation = getLocation();
		if (this.closestStations == null && currentLocation != null) {
			// start refreshing if not running.
			refreshClosestBikeStations(false);
			return;
		}
		// ELSE IF there are closest stations AND new location DO
		if (Utils.getCollectionSize(this.closestStations) > 0 && currentLocation != null) {
			// update the list distances
			boolean isDetailed = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
					UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			float accuracyInMeters = currentLocation.getAccuracy();
			for (ABikeStation station : this.closestStations) {
				// distance
				station.setDistance(currentLocation.distanceTo(station.getLocation()));
				station.setDistanceString(Utils.getDistanceString(station.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			String previousClosest = this.closestStationTerminalName;
			generateOrderedStationsIds();
			notifyDataSetChanged(this.closestStationTerminalName == null ? false : this.closestStationTerminalName.equals(previousClosest));
		}
	}

	/**
	 * Generate the ordered subway line station IDs.
	 */
	public void generateOrderedStationsIds() {
		MyLog.v(TAG, "generateOrderedStationsIds()");
		// IF no station DO
		if (Utils.getCollectionSize(this.closestStations) == 0) {
			this.closestStationTerminalName = null;
			return;
		}
		// ELSE IF stations DO
		List<ABikeStation> orderedStations = new ArrayList<ABikeStation>(this.closestStations);
		// order the stations list by distance (closest first)
		Collections.sort(orderedStations, new Comparator<ABikeStation>() {
			@Override
			public int compare(ABikeStation lhs, ABikeStation rhs) {
				float d1 = lhs.getDistance();
				float d2 = rhs.getDistance();
				if (d1 > d2) {
					return +1;
				} else if (d1 < d2) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		this.closestStationTerminalName = orderedStations.get(0).getTerminalName();
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
	private void showNewClosestBikeStations() {
		MyLog.v(TAG, "showNewClosestBikeStations()");
		// if (Utils.getCollectionSize(this.closestStations) > 0) {
		if (this.closestStations != null) {
			// set the closest station title
			showNewClosestBikeStationsTitle();
			// hide loading
			findViewById(R.id.closest_bike_stations_list_loading).setVisibility(View.GONE); // hide
			// show stations list
			this.lastSuccessfulRefresh = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_BIXI_LAST_UPDATE, 0);
			notifyDataSetChanged(true);
			findViewById(R.id.closest_bike_stations_list).setVisibility(View.VISIBLE);
			setClosestStationsNotLoading();
			refreshDataIfTooOld();
		}
	}

	private void refreshDataIfTooOld() {
		MyLog.v(TAG, "refreshDataIfTooOld()");
		if (Utils.currentTimeSec() - BikeUtils.CACHE_TOO_OLD_IN_SEC - getLastUpdateTime() > 0) {
			// IF last try too recent DO
			if (Utils.currentTimeSec() - BikeUtils.CACHE_TOO_OLD_IN_SEC - this.lastForcedRefresh < 0) {
				MyLog.d(TAG, "refreshDataIfTooOld() > last refresh too recent, not refreshing");
				return; // skip refresh
			}
			// ELSE refresh from www
			if (this.closestBikeStationsTask != null && this.closestBikeStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
				try { // wait for 1 seconds
					this.closestBikeStationsTask.get(1, TimeUnit.SECONDS);
				} catch (Exception e) {
					MyLog.w(TAG, e, "Error while waiting to task to complete!");
				}
			}
			startClosestStationsTask(true);
		}
	}

	/**
	 * @return the last update time
	 */
	public int getLastUpdateTime() {
		int timestamp = 0;
		timestamp = this.lastSuccessfulRefresh;
		// if (timestamp == 0 && this.bikeStation != null) {
		// timestamp = this.bikeStation.getLatestUpdateTime();
		// }
		if (timestamp == 0) {
			timestamp = Utils.currentTimeSec(); // use device time
		}
		return timestamp;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s, %s,%s,%s)", parent.getId(), view.getId(), position, id);
		if (this.closestStations != null && position < this.closestStations.size() && this.closestStations.get(position) != null) {
			Intent intent = new Intent(this, BikeStationInfo.class);
			BikeStation selectedStation = this.closestStations.get(position);
			intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, selectedStation.getTerminalName());
			intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, selectedStation.getName());
			startActivity(intent);
		}
	}

	/**
	 * set the closest station title
	 */
	public void showNewClosestBikeStationsTitle() {
		if (this.closestBikeStationsLocationAddress != null && this.closestBikeStationsLocation != null) {
			((TextView) findViewById(R.id.closest_bike_stations_title).findViewById(R.id.closest_bike_stations_title_text)).setText(LocationUtils
					.getLocationString(this, R.string.closest_bike_stations, this.closestBikeStationsLocationAddress,
							this.closestBikeStationsLocation.getAccuracy()));
		}
	}

	static class ViewHolder {
		TextView placeTv;
		TextView stationNameTv;
		TextView distanceTv;
		ImageView subwayImg;
		ImageView favImg;
		ImageView compassImg;
		ProgressBar progressBar;
		TextView dockTv;
		TextView bikeTv;
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	private class ArrayAdapterWithCustomView extends ArrayAdapter<ABikeStation> {

		/**
		 * The layout inflater.
		 */
		private LayoutInflater layoutInflater;
		/**
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 */
		public ArrayAdapterWithCustomView(Context context, int viewId) {
			super(context, viewId);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return BikeTab.this.closestStations == null ? 0 : BikeTab.this.closestStations.size();
		}

		@Override
		public int getPosition(ABikeStation item) {
			return BikeTab.this.closestStations.indexOf(item);
		}

		@Override
		public ABikeStation getItem(int position) {
			return BikeTab.this.closestStations.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.placeTv = (TextView) convertView.findViewById(R.id.place);
				holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				holder.subwayImg = (ImageView) convertView.findViewById(R.id.subway_img);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				holder.progressBar = (ProgressBar) convertView.findViewById(R.id.availability).findViewById(R.id.progress_bar);
				holder.dockTv = (TextView) convertView.findViewById(R.id.availability).findViewById(R.id.progress_dock);
				holder.bikeTv = (TextView) convertView.findViewById(R.id.availability).findViewById(R.id.progress_bike);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ABikeStation bikeStation = getItem(position);
			if (bikeStation != null) {
				// bike station name
				holder.stationNameTv.setText(Utils.cleanBikeStationName(bikeStation.getName()));
				// favorite
				if (BikeTab.this.favTerminalNames != null && BikeTab.this.favTerminalNames.contains(bikeStation.getTerminalName())) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// status (not installed, locked..)
				if (!bikeStation.isInstalled() || bikeStation.isLocked()) {
					holder.stationNameTv.setTextColor(Utils.getTextColorSecondary(getContext()));
				} else {
					holder.stationNameTv.setTextColor(Utils.getTextColorPrimary(getContext()));
				}
				// availability
				final int lastUpdate = BikeTab.this.lastSuccessfulRefresh != 0 ? BikeTab.this.lastSuccessfulRefresh : bikeStation.getLatestUpdateTime();
				int waitFor = Utils.currentTimeSec() - BikeUtils.CACHE_NOT_USEFUL_IN_SEC - lastUpdate;
				if (waitFor < 0) {
					if (!bikeStation.isInstalled()) {
						holder.bikeTv.setText(R.string.bike_station_not_installed);
						holder.bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
						holder.dockTv.setVisibility(View.GONE);
					} else if (bikeStation.isLocked()) {
						holder.bikeTv.setText(R.string.bike_station_locked);
						holder.bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
						holder.dockTv.setVisibility(View.GONE);
					} else {
						// bikes #
						holder.bikeTv.setText(getResources().getQuantityString(R.plurals.bikes_nb, bikeStation.getNbBikes(), bikeStation.getNbBikes()));
						holder.bikeTv.setTypeface(bikeStation.getNbBikes() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
						holder.bikeTv.setVisibility(View.VISIBLE);
						// dock #
						holder.dockTv.setText(getResources()
								.getQuantityString(R.plurals.docks_nb, bikeStation.getNbEmptyDocks(), bikeStation.getNbEmptyDocks()));
						holder.dockTv.setTypeface(bikeStation.getNbEmptyDocks() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
						holder.dockTv.setVisibility(View.VISIBLE);
					}
					// progress bar
					holder.progressBar.setIndeterminate(false);
					holder.progressBar.setMax(bikeStation.getNbTotalDocks());
					holder.progressBar.setProgress(bikeStation.getNbBikes());
				} else {
					holder.bikeTv.setText(R.string.ellipsis);
					holder.bikeTv.setTypeface(Typeface.DEFAULT);
					holder.dockTv.setText(R.string.ellipsis);
					holder.dockTv.setTypeface(Typeface.DEFAULT);
					holder.progressBar.setIndeterminate(true);
				}
				// distance
				if (!TextUtils.isEmpty(bikeStation.getDistanceString())) {
					holder.distanceTv.setText(bikeStation.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE); // never hide once shown
					holder.distanceTv.setText(null);
				}
				// compass
				if (bikeStation.getCompassMatrixOrNull() != null) {
					holder.compassImg.setImageMatrix(bikeStation.getCompassMatrix());
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE); // never hide once shown
				}
				// closest bike station
				int index = -1;
				if (!TextUtils.isEmpty(BikeTab.this.closestStationTerminalName)) {
					index = bikeStation.getTerminalName().equals(BikeTab.this.closestStationTerminalName) ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
					break;
				default:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow);
					break;
				}
			}
			return convertView;
		}
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStationsError(String errorMessage) {
		MyLog.v(TAG, "setClosestStationsError(%s)", errorMessage);
		// IF there are already stations DO
		if (Utils.getCollectionSize(this.closestStations) > 0) {
			// notify the user but keep showing the old stations
			Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP, 0, Constant.TOAST_Y_OFFSET);
			toast.show();
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
		if (Utils.getCollectionSize(this.closestStations) == 0) {
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
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
		// IF there is no closest bike stations DO
		if (Utils.getCollectionSize(this.closestStations) == 0) {
			// generate the closest bike stations list
			refreshClosestBikeStations(false);
		} else {
			// show the closest stations
			showNewClosestBikeStations();
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestBikeStationsLocation)) {
				// start refreshing
				refreshClosestBikeStations(true);
			}
		}
	}

	/**
	 * Refresh the closest stations if not running.
	 * @param v a view (not used)
	 */
	public void refreshOrStopRefreshClosestStations(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshClosestStations()");
		refreshClosestBikeStations(true);
	}

	/**
	 * Refresh the closest bike stations list.
	 */
	private void refreshClosestBikeStations(boolean force) {
		MyLog.v(TAG, "refreshClosestBikeStations(%s)", force);
		// cancel current if forcing
		if (force && this.closestBikeStationsTask != null) {
			this.closestBikeStationsTask.cancel(true);
			this.closestBikeStationsTask = null;
		}
		// IF the task is NOT already running DO
		if (this.closestBikeStationsTask == null || !this.closestBikeStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// IF location found DO
			Location locationUsed = getLocation();
			if (locationUsed != null) {
				// find the closest stations
				this.closestBikeStationsLocation = locationUsed;
				startClosestStationsTask(force);
				new AsyncTask<Location, Void, Address>() {

					@Override
					protected Address doInBackground(Location... locations) {
						return LocationUtils.getLocationAddress(BikeTab.this, locations[0]);
					}

					@Override
					protected void onPostExecute(Address result) {
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
		}
	}

	private void startClosestStationsTask(boolean force) {
		// MyLog.v(TAG, "startClosestStationsTask(%s)", force);
		if (force) {
			this.lastForcedRefresh = Utils.currentTimeSec();
		}
		setClosestStationsLoading(null);
		this.closestBikeStationsTask = new ClosestBikeStationsFinderTask(this, this, SupportFactory.getInstance(this).getNbClosestPOIDisplay(), force);
		this.closestBikeStationsTask.execute(this.closestBikeStationsLocation);
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
			if (this.closestStations == null && result.getPoiListSize() == 0) {
				// get the result
				this.closestStations = new ArrayList<ABikeStation>(); // important for only forcing refresh once
				// force refresh from server
				refreshClosestBikeStations(true);
			} else { // ELSE
				// get the result
				this.closestStations = result.getPoiList();
				generateOrderedStationsIds();
				refreshFavoriteTerminalNamesFromDB();
				// shot the result
				showNewClosestBikeStations();
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
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false; // don't trigger update if favorites are the same
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BikeTab.this.favTerminalNames)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavTerminalNames = new ArrayList<String>();
				for (Fav bikeStationFav : result) {
					if (BikeTab.this.favTerminalNames == null || !BikeTab.this.favTerminalNames.contains(bikeStationFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavTerminalNames.add(bikeStationFav.getFkId()); // store terminal name
				}
				BikeTab.this.favTerminalNames = newfavTerminalNames;
				// trigger change if necessary
				if (newFav) {
					notifyDataSetChanged(true);
				}
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
					return LocationUtils.getBestLastKnownLocation(BikeTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					if (result != null) {
						BikeTab.this.setLocation(result);
					}
					// enable location updates if necessary
					if (!BikeTab.this.locationUpdatesEnabled) {
						LocationUtils.enableLocationUpdates(BikeTab.this, BikeTab.this);
						BikeTab.this.locationUpdatesEnabled = true;
					}
				}

			}.execute();
		}
		return this.location;
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
			this.closestStations = null;
		}
		AdsUtils.setupAd(this);
		super.onDestroy();
	}
}

package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.PrefetchingUtils;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABikeStation;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

/**
 * This activity list the favorite bus stops.
 * @author Mathieu Méa
 */
public class FavListTab extends Activity implements LocationListener, SensorEventListener, CompassListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = FavListTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/FavList";

	/**
	 * The favorite bike station list.
	 */
	private List<DataStore.Fav> currentBikeStationFavList;
	/**
	 * The bike stations.
	 */
	private List<ABikeStation> bikeStations;
	/**
	 * The favorite subway stations list.
	 */
	private List<DataStore.Fav> currentSubwayStationFavList;
	/**
	 * The subway stations.
	 */
	private List<ASubwayStation> subwayStations;
	/**
	 * The favorite bus stops list.
	 */
	private List<DataStore.Fav> currentBusStopFavList;
	/**
	 * The bus stops.
	 */
	private List<ABusStop> busStops;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass updates enabled?
	 */
	private boolean compassUpdatesEnabled = false;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.fav_list_tab);
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
		this.paused = false;
		// IF the activity has the focus DO
		if (this.hasFocus) {
			onResumeWithFocus();
		}
		super.onResume();
		UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_TAB, 0);
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
					return LocationUtils.getBestLastKnownLocation(FavListTab.this);
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
					FavListTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(FavListTab.this, FavListTab.this,
							FavListTab.this.locationUpdatesEnabled, FavListTab.this.paused);
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		setUpUI();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		checkForCompass(se, this);
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
			updateCompass(SensorUtils.calculateOrientation(this, this.accelerometerValues, this.magneticFieldValues), false);
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	private void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		final long now = System.currentTimeMillis();
		// if (this.busStops != null) {
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							if (FavListTab.this.busStops != null && FavListTab.this.subwayStations != null && FavListTab.this.bikeStations != null) {
								FavListTab.this.lastCompassInDegree = (int) orientation;
								FavListTab.this.lastCompassChanged = now;
							}
							// update bus stops compass
							if (FavListTab.this.busStops != null) {
								View busStopsLayout = findViewById(R.id.bus_stops_list);
								for (ABusStop busStop : FavListTab.this.busStops) {
									if (busStop == null) {
										continue;
									}
									View stopView = busStopsLayout.findViewWithTag(getBusStopViewTag(busStop));
									if (stopView == null) { // || TextUtils.isEmpty(busStop.getDistanceString())) {
										continue;
									}
									ImageView compassImg = (ImageView) stopView.findViewById(R.id.compass);
									float compassRotation = SensorUtils.getCompassRotationInDegree(location, busStop, lastCompassInDegree, locationDeclination);
									SupportFactory.get().rotateImageView(compassImg, compassRotation, FavListTab.this);
									compassImg.setVisibility(View.VISIBLE);
								}
							}
							// update subway stations compass
							if (FavListTab.this.subwayStations != null) {
								View subwayStationsLayout = findViewById(R.id.subway_stations_list);
								for (ASubwayStation subwayStation : FavListTab.this.subwayStations) {
									if (subwayStation == null) {
										continue;
									}
									View stationView = subwayStationsLayout.findViewWithTag(getSubwayStationViewTag(subwayStation));
									if (stationView == null) { // || TextUtils.isEmpty(subwayStation.getDistanceString())) {
										continue;
									}
									ImageView compassImg = (ImageView) stationView.findViewById(R.id.compass);
									float compassRotation = SensorUtils.getCompassRotationInDegree(location, subwayStation, lastCompassInDegree,
											locationDeclination);
									SupportFactory.get().rotateImageView(compassImg, compassRotation, FavListTab.this);
									compassImg.setVisibility(View.VISIBLE);
								}
							}
							// update bike stations compass
							if (FavListTab.this.bikeStations != null) {
								View bikeStationsLayout = findViewById(R.id.bike_stations_list);
								for (ABikeStation bikeStation : FavListTab.this.bikeStations) {
									if (bikeStation == null) {
										continue;
									}
									View stationView = bikeStationsLayout.findViewWithTag(getBikeStationViewTag(bikeStation));
									if (stationView == null) { // || TextUtils.isEmpty(bikeStation.getDistanceString())) {
										continue;
									}
									ImageView compassImg = (ImageView) stationView.findViewById(R.id.compass);
									float compassRotation = SensorUtils.getCompassRotationInDegree(location, bikeStation, lastCompassInDegree,
											locationDeclination);
									SupportFactory.get().rotateImageView(compassImg, compassRotation, FavListTab.this);
									compassImg.setVisibility(View.VISIBLE);
								}
							}
						}
					}
				});
	}

	private boolean paused = false;
	private float locationDeclination;

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	protected void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.compassUpdatesEnabled = false;
		}
		super.onPause();
	}

	/**
	 * Refresh all the UI.
	 */
	private void setUpUI() {
		MyLog.v(TAG, "setUpUI()");
		loadFavoritesFromDB();
	}

	private void loadFavoritesFromDB() {
		MyLog.v(TAG, "loadFavoritesFromDB()");
		new AsyncTask<Void, Void, Void>() {

			private List<DataStore.Fav> newBusStopFavList;
			private List<BusStop> busStopsExtendedList;
			private List<DataStore.Fav> newSubwayFavList;
			private List<SubwayStation> subwayStations;
			private Map<String, List<SubwayLine>> otherSubwayLines;
			private List<DataStore.Fav> newBikeFavList;
			private Map<String, BikeStation> bikeStations;

			@Override
			protected Void doInBackground(Void... params) {
				MyLog.v(TAG, "loadFavoritesFromDB() > doInBackground()");
				// BUS STOPs
				this.newBusStopFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
				if (FavListTab.this.currentBusStopFavList == null || !Fav.listEquals(FavListTab.this.currentBusStopFavList, this.newBusStopFavList)) {
					if (Utils.getCollectionSize(this.newBusStopFavList) > 0) {
						MyLog.d(TAG, "Loading bus stop favorites from DB...");
						this.busStopsExtendedList = StmManager.findBusStopsExtendedList(getContentResolver(),
								Utils.extractBusStopIDsFromFavList(this.newBusStopFavList));
						MyLog.d(TAG, "Loading bus stop favorites from DB... DONE");
					}
				}
				// SUBWAY STATIONs
				this.newSubwayFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
				if (FavListTab.this.currentSubwayStationFavList == null || !Fav.listEquals(FavListTab.this.currentSubwayStationFavList, this.newSubwayFavList)) {
					MyLog.d(TAG, "Loading subway station favorites from DB...");
					this.subwayStations = StmManager.findSubwayStationsList(getContentResolver(),
							Utils.extractSubwayStationIDsFromFavList(this.newSubwayFavList));
					this.otherSubwayLines = new HashMap<String, List<SubwayLine>>();
					if (Utils.getCollectionSize(this.subwayStations) > 0) {
						for (SubwayStation station : this.subwayStations) {
							if (station != null) {
								this.otherSubwayLines.put(station.getId(), StmManager.findSubwayStationLinesList(getContentResolver(), station.getId()));
							}
						}
					}
					MyLog.d(TAG, "Loading subway station favorites from DB... DONE");
				}
				// BIKE STATIONs
				this.newBikeFavList = DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
				if (FavListTab.this.currentBikeStationFavList == null || !Fav.listEquals(FavListTab.this.currentBikeStationFavList, this.newBikeFavList)) {
					if (Utils.getCollectionSize(this.newBikeFavList) > 0) {
						MyLog.d(TAG, "Loading bike station favorites from DB...");
						this.bikeStations = BixiManager.findBikeStationsMap(getContentResolver(),
								Utils.extractBikeStationTerminNamesFromFavList(this.newBikeFavList));
						MyLog.d(TAG, "Loading bike station favorites from DB... DONE");
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				MyLog.v(TAG, "loadFavoritesFromDB() > onPostExecute()");
				if (newBusStopFavList != null) { // IF favorite bus stop list was refreshed DO update the UI
					refreshBusStopsUI(this.newBusStopFavList, this.busStopsExtendedList);
				}
				if (newSubwayFavList != null) { // IF favorite subway station list was refreshed DO update the UI
					refreshSubwayStationsUI(this.newSubwayFavList, this.subwayStations, this.otherSubwayLines);
				}
				if (newBikeFavList != null) { // IF favorite bike station list was refreshed DO update the UI
					refreshBikeStationsUI(this.newBikeFavList, this.bikeStations);
				}
				updateDistancesWithNewLocation(); // show distance if location found
				updateCompass(FavListTab.this.lastCompassInDegree, true); // show compass if available
				showEmptyFav();
				UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV, isThereAtLeastOneFavorite());
			}

		}.execute();
	}

	public boolean isThereAtLeastOneFavorite() {
		return Utils.getCollectionSize(FavListTab.this.currentBusStopFavList) > 0 || Utils.getCollectionSize(FavListTab.this.currentSubwayStationFavList) > 0
				|| Utils.getCollectionSize(FavListTab.this.currentBikeStationFavList) > 0;
	}

	/**
	 * Show 'no favorite' view if necessary.
	 */
	private void showEmptyFav() {
		MyLog.v(TAG, "showEmptyFav()");
		findViewById(R.id.loading).setVisibility(View.GONE);
		if (!isThereAtLeastOneFavorite()) {
			findViewById(R.id.lists).setVisibility(View.GONE);
			findViewById(R.id.empty).setVisibility(View.VISIBLE);
		} else { // at least 1 favorite
			findViewById(R.id.empty).setVisibility(View.GONE);
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			// IF there is no favorite bus stops DO
			if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() == 0) {
				findViewById(R.id.fav_bus_stops).setVisibility(View.GONE);
				findViewById(R.id.bus_stops_list).setVisibility(View.GONE);
			}
			// IF there is no favorite subway stations DO
			if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() == 0) {
				findViewById(R.id.fav_subway_stations).setVisibility(View.GONE);
				findViewById(R.id.subway_stations_list).setVisibility(View.GONE);
			}
			// IF there is no favorite bike stations DO
			if (this.currentBikeStationFavList == null || this.currentBikeStationFavList.size() == 0) {
				findViewById(R.id.fav_bike_stations).setVisibility(View.GONE);
				findViewById(R.id.bike_stations_list).setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Refresh the favorite bus stops UI
	 * @param newBusStopFavList the new favorite bus stops
	 * @param busStopsExtendedList the bus stops (extended)
	 */
	private void refreshBusStopsUI(List<DataStore.Fav> newBusStopFavList, List<BusStop> busStopsExtendedList) {
		// MyLog.v(TAG, "refreshBusStopsUI(%s,%s)", Utils.getCollectionSize(newBusStopFavList), Utils.getCollectionSize(busStopsExtendedList));
		if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() != newBusStopFavList.size()) {
			// remove all favorite bus stop views
			LinearLayout busStopsLayout = (LinearLayout) findViewById(R.id.bus_stops_list);
			busStopsLayout.removeAllViews();
			// use new favorite bus stops
			this.currentBusStopFavList = newBusStopFavList;
			this.busStops = new ArrayList<ABusStop>();
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_bus_stops).setVisibility(View.VISIBLE);
			busStopsLayout.setVisibility(View.VISIBLE);
			// FOR EACH bus stop DO
			if (busStopsExtendedList != null) {
				for (final BusStop busStop : busStopsExtendedList) {
					// list view divider
					if (busStopsLayout.getChildCount() > 0) {
						busStopsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, busStopsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bus_stop_item, busStopsLayout, false);
					view.setTag(getBusStopViewTag(busStop));
					// bus stop code
					((TextView) view.findViewById(R.id.stop_code)).setText(busStop.getCode());
					// bus stop place
					String busStopPlace = BusUtils.cleanBusStopPlace(busStop.getPlace());
					((TextView) view.findViewById(R.id.label)).setText(busStopPlace);
					// bus stop line number
					TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
					lineNumberTv.setText(busStop.getLineNumber());
					int color = BusUtils.getBusLineTypeBgColor(busStop.getLineTypeOrNull(), busStop.getLineNumber());
					lineNumberTv.setBackgroundColor(color);
					// bus stop line direction
					int busLineDirection = BusUtils.getBusLineSimpleDirection(busStop.getDirectionId());
					((TextView) view.findViewById(R.id.line_direction)).setText(getString(busLineDirection).toUpperCase(Locale.getDefault()));
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
							intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
							intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
							intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
							intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
							intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
							startActivity(intent);
						}
					});
					// add context menu
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							final View theViewToDelete = v;
							new AlertDialog.Builder(FavListTab.this)
									.setTitle(getString(R.string.bus_stop_and_line_short, busStop.getCode(), busStop.getLineNumber()))
									.setItems(
											new CharSequence[] { getString(R.string.view_bus_stop), getString(R.string.view_bus_stop_line),
													getString(R.string.remove_fav) }, new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case 0:
														Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
														intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
														intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
														intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
														startActivity(intent);
														break;
													case 1:
														Intent intent2 = new Intent(FavListTab.this, SupportFactory.get().getBusLineInfoClass());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, busStop.getLineNumber());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_NAME, busStop.getLineNameOrNull());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_TYPE, busStop.getLineTypeOrNull());
														intent2.putExtra(BusLineInfo.EXTRA_LINE_DIRECTION_ID, busStop.getDirectionId());
														startActivity(intent2);
														break;
													case 2:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.bus_stops_list)).removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentBusStopFavList.iterator();
														while (it.hasNext()) {
															DataStore.Fav fav = (DataStore.Fav) it.next();
															if (fav.getFkId().equals(busStop.getCode()) && fav.getFkId2().equals(busStop.getLineNumber())) {
																it.remove();
																break;
															}
														}
														// refresh empty
														showEmptyFav();
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// find the favorite to delete
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, busStop.getCode(), busStop.getLineNumber());
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
														SupportFactory.get().backupManagerDataChanged(FavListTab.this);
														break;
													default:
														break;
													}
												}
											}).create().show();
							return true;
						}
					});
					this.busStops.add(new ABusStop(busStop));
					busStopsLayout.addView(view);
					SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(FavListTab.this, busStop, null, true, false),
							PrefetchingUtils.getExecutor());
				}
			}
		}
	}

	// private Executor executor = new ;

	private String getBusStopViewTag(BusStop busStop) {
		return "busStop" + busStop.getUID();
	}

	/**
	 * Refresh subway station UI.
	 * @param newSubwayFavList the new favorite subway stations list
	 * @param stations the new favorite subway stations
	 * @param otherLines the new favorite subway stations "other lines"
	 */
	private void refreshSubwayStationsUI(List<DataStore.Fav> newSubwayFavList, List<SubwayStation> stations, Map<String, List<SubwayLine>> otherLines) {
		// MyLog.v(TAG, "refreshSubwayStationsUI()", Utils.getCollectionSize(newSubwayFavList), Utils.getMapSize(stations), Utils.getMapSize(otherLines));
		if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() != newSubwayFavList.size()) {
			LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
			// remove all subway station views
			subwayStationsLayout.removeAllViews();
			// use new favorite subway station
			this.currentSubwayStationFavList = newSubwayFavList;
			this.subwayStations = new ArrayList<ASubwayStation>();
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_subway_stations).setVisibility(View.VISIBLE);
			subwayStationsLayout.setVisibility(View.VISIBLE);
			// FOR EACH favorite subway DO
			if (stations != null) {
				for (final SubwayStation subwayStation : stations) {
					List<SubwayLine> otherLinesId = otherLines.get(subwayStation.getId());
					// list view divider
					if (subwayStationsLayout.getChildCount() > 0) {
						subwayStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, subwayStationsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_subway_station_item, subwayStationsLayout, false);
					view.setTag(getSubwayStationViewTag(subwayStation));
					// subway station name
					((TextView) view.findViewById(R.id.station_name)).setText(subwayStation.getName());
					// station lines color
					if (otherLinesId != null && otherLinesId.size() > 0) {
						int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(0).getNumber());
						((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImg1);
						if (otherLinesId.size() > 1) {
							int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(1).getNumber());
							((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImg2);
							if (otherLinesId.size() > 2) {
								int subwayLineImg3 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(2).getNumber());
								((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImg3);
							} else {
								view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
							}
						} else {
							view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
							view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
						}
					} else {
						view.findViewById(R.id.subway_img_1).setVisibility(View.GONE);
						view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
						view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
					}
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
							intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStation.getId());
							intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStation.getName());
							startActivity(intent);
						}
					});
					// add context menu
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							final View theViewToDelete = v;
							new AlertDialog.Builder(FavListTab.this)
									.setTitle(getString(R.string.subway_station_with_name_short, subwayStation.getName()))
									.setItems(new CharSequence[] { getString(R.string.view_subway_station), getString(R.string.remove_fav) },
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case 0:
														Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStation.getId());
														intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStation.getName());
														startActivity(intent);
														break;
													case 1:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.subway_stations_list)).removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentSubwayStationFavList.iterator();
														while (it.hasNext()) {
															DataStore.Fav fav = (DataStore.Fav) it.next();
															if (fav.getFkId().equals(subwayStation.getId())) {
																it.remove();
																break;
															}
														}
														// refresh empty
														showEmptyFav();
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// delete the favorite
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION, subwayStation.getId(), null);
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
														SupportFactory.get().backupManagerDataChanged(FavListTab.this);
														break;
													default:
														break;
													}
												}
											}).create().show();
							return true;
						}
					});
					this.subwayStations.add(new ASubwayStation(subwayStation));
					subwayStationsLayout.addView(view);
				}
			}
		}
	}

	private String getSubwayStationViewTag(SubwayStation subwayStation) {
		return "subwayStation" + subwayStation.getId();
	}

	/**
	 * Refresh bike station UI.
	 * @param newBikeFavList the new favorite bike stations list
	 * @param bikeStations the new favorite bike stations
	 */
	private void refreshBikeStationsUI(List<DataStore.Fav> newBikeFavList, Map<String, BikeStation> bikeStations) {
		// MyLog.v(TAG, "refreshBikeStationsUI(%s,%s)", Utils.getCollectionSize(newBikeFavList), Utils.getMapSize(bikeStations));
		if (this.currentBikeStationFavList == null || this.currentBikeStationFavList.size() != newBikeFavList.size()) {
			LinearLayout bikeStationsLayout = (LinearLayout) findViewById(R.id.bike_stations_list);
			// remove all bike station views
			bikeStationsLayout.removeAllViews();
			// use new favorite bike station
			this.currentBikeStationFavList = newBikeFavList;
			this.bikeStations = new ArrayList<ABikeStation>();
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_bike_stations).setVisibility(View.VISIBLE);
			bikeStationsLayout.setVisibility(View.VISIBLE);
			// FOR EACH favorite bike DO
			if (bikeStations != null) {
				for (final BikeStation bikeStation : bikeStations.values()) {
					// list view divider
					if (bikeStationsLayout.getChildCount() > 0) {
						bikeStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, bikeStationsLayout, false));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bike_station_item, bikeStationsLayout, false);
					view.setTag(getBikeStationViewTag(bikeStation));
					// subway station name
					((TextView) view.findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(bikeStation.getName()));
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, BikeStationInfo.class);
							intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, bikeStation.getTerminalName());
							intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, bikeStation.getName());
							startActivity(intent);
						}
					});
					// add context menu
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							final View theViewToDelete = v;
							new AlertDialog.Builder(FavListTab.this)
									.setTitle(bikeStation.getName())
									.setItems(new CharSequence[] { getString(R.string.view_bike_station), getString(R.string.remove_fav) },
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int item) {
													MyLog.v(TAG, "onClick(%s)", item);
													switch (item) {
													case 0:
														Intent intent = new Intent(FavListTab.this, BikeStationInfo.class);
														intent.putExtra(BikeStationInfo.EXTRA_STATION_TERMINAL_NAME, bikeStation.getTerminalName());
														intent.putExtra(BikeStationInfo.EXTRA_STATION_NAME, bikeStation.getName());
														startActivity(intent);
														break;
													case 1:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.bike_stations_list)).removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentBikeStationFavList.iterator();
														while (it.hasNext()) {
															DataStore.Fav fav = (DataStore.Fav) it.next();
															if (fav.getFkId().equals(bikeStation.getTerminalName())) {
																it.remove();
																break;
															}
														}
														// refresh empty
														showEmptyFav();
														UserPreferences.savePrefLcl(FavListTab.this, UserPreferences.PREFS_LCL_IS_FAV,
																isThereAtLeastOneFavorite());
														// delete the favorite
														Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
																DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS, bikeStation.getTerminalName(), null);
														// delete the favorite
														if (findFav != null) {
															DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
														}
														SupportFactory.get().backupManagerDataChanged(FavListTab.this);
														break;
													default:
														break;
													}
												}
											}).create().show();
							return true;
						}
					});
					this.bikeStations.add(new ABikeStation(bikeStation));
					bikeStationsLayout.addView(view);
				}
			}
		}
	}

	private String getBikeStationViewTag(BikeStation bikeStation) {
		return "bikeStation" + bikeStation.getTerminalName();
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		final Location currentLocation = getLocation();
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", currentLocation);
		if (currentLocation == null) {
			return;
		}
		// update bus stops
		LocationUtils.updateDistance(FavListTab.this, FavListTab.this.busStops, currentLocation, new LocationTaskCompleted() {

			@Override
			public void onLocationTaskCompleted() {
				if (FavListTab.this.busStops != null) {
					View busStopsLayout = findViewById(R.id.bus_stops_list);
					for (ABusStop busStop : FavListTab.this.busStops) {
						if (!busStop.hasLocation()) {
							continue;
						}
						// update view
						View stopView = busStopsLayout.findViewWithTag(getBusStopViewTag(busStop));
						if (stopView != null && !TextUtils.isEmpty(busStop.getDistanceString())) {
							TextView distanceTv = (TextView) stopView.findViewById(R.id.distance);
							distanceTv.setText(busStop.getDistanceString());
							distanceTv.setVisibility(View.VISIBLE);
						}
					}
				}
			}
		});
		// update subway stations
		LocationUtils.updateDistance(FavListTab.this, FavListTab.this.subwayStations, currentLocation, new LocationTaskCompleted() {

			@Override
			public void onLocationTaskCompleted() {
				if (FavListTab.this.subwayStations != null) {
					View subwayStationsLayout = findViewById(R.id.subway_stations_list);
					for (ASubwayStation subwayStation : FavListTab.this.subwayStations) {
						// update view
						View stationView = subwayStationsLayout.findViewWithTag(getSubwayStationViewTag(subwayStation));
						if (stationView != null && !TextUtils.isEmpty(subwayStation.getDistanceString())) {
							TextView distanceTv = (TextView) stationView.findViewById(R.id.distance);
							distanceTv.setText(subwayStation.getDistanceString());
							distanceTv.setVisibility(View.VISIBLE);
						}
					}
				}
			}
		});
		// update bike stations
		LocationUtils.updateDistance(FavListTab.this, FavListTab.this.bikeStations, currentLocation, new LocationTaskCompleted() {

			@Override
			public void onLocationTaskCompleted() {
				if (FavListTab.this.bikeStations != null) {
					View bikeStationsLayout = findViewById(R.id.bike_stations_list);
					for (ABikeStation bikeStation : FavListTab.this.bikeStations) {
						// update view
						View stationView = bikeStationsLayout.findViewWithTag(getBikeStationViewTag(bikeStation));
						if (stationView != null && !TextUtils.isEmpty(bikeStation.getDistanceString())) {
							TextView distanceTv = (TextView) stationView.findViewById(R.id.distance);
							distanceTv.setText(bikeStation.getDistanceString());
							distanceTv.setVisibility(View.VISIBLE);
						}
					}
				}
			}
		});
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
				}
			}
		}
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
					return LocationUtils.getBestLastKnownLocation(FavListTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "getLocation() > onPostExecute()");
					if (result != null) {
						FavListTab.this.setLocation(result);
					}
					// enable location updates if necessary
					FavListTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(FavListTab.this, FavListTab.this,
							FavListTab.this.locationUpdatesEnabled, FavListTab.this.paused);
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
		return MenuUtils.createMainMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}

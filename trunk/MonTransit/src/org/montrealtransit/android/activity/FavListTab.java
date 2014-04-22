package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.AnalyticsUtils;
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
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.BixiManager;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmSubwayManager;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
	private List<Fav> currentBikeStationFavList;
	/**
	 * The bike stations.
	 */
	private List<ABikeStation> bikeStations;
	/**
	 * The favorite subway stations list.
	 */
	private List<Fav> currentSubwayStationFavList;
	/**
	 * The favorite bus stops list.
	 */
	private List<Fav> currentBusStopFavList;
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
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];
	/**
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;
	private boolean paused = false;
	private float locationDeclination;
	private POIArrayAdapter subwayAdapter;
	private POIArrayAdapter busAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.fav_list_tab);

		this.busAdapter = new POIArrayAdapter(this);
		this.busAdapter.setManualLayout((ViewGroup) findViewById(R.id.bus_stops_list));
		this.busAdapter.setManualScrollView((ScrollView) findViewById(R.id.lists));

		this.subwayAdapter = new POIArrayAdapter(this);
		this.subwayAdapter.setManualLayout((ViewGroup) findViewById(R.id.subway_stations_list));
		this.subwayAdapter.setManualScrollView((ScrollView) findViewById(R.id.lists));
		// TODO POIArrayAdapter for favorite bikes
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
		SensorUtils.checkForCompass(this, se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		final long now = System.currentTimeMillis();
		// if (this.busStops != null) {
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							if (FavListTab.this.bikeStations != null) {
								FavListTab.this.lastCompassInDegree = (int) orientation;
								FavListTab.this.lastCompassChanged = now;
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
									if (location.getAccuracy() <= bikeStation.getDistance()) {
										float compassRotation = SensorUtils.getCompassRotationInDegree(location, bikeStation, lastCompassInDegree,
												locationDeclination);
										SupportFactory.get().rotateImageView(compassImg, compassRotation, FavListTab.this);
										compassImg.setVisibility(View.VISIBLE);
									} else {
										compassImg.setVisibility(View.GONE);
									}
								}
							}
						}
					}
				});
	}

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
		this.subwayAdapter.onPause();
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

			private List<RouteTripStop> busStopsList;
			private List<RouteTripStop> subwayStationsList;
			private Map<String, BikeStation> bikeStations;

			@Override
			protected Void doInBackground(Void... params) {
				MyLog.v(TAG, "loadFavoritesFromDB() > doInBackground()");
				List<Fav> newRouteStopFavList = DataManager.findFavsByTypeList(getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
				List<Fav> newBusStopFavList = new ArrayList<Fav>();
				List<Fav> newSubwayFavList = new ArrayList<Fav>();
				for (Fav newRouteStopFav : newRouteStopFavList) {
					String authority = TripStop.getAuthorityFromUID(newRouteStopFav.getFkId());
					if (TextUtils.isEmpty(authority)) {
						continue;
					}
					if (authority.contains("bus")) {
						newBusStopFavList.add(newRouteStopFav);
					} else if (authority.contains("subway")) {
						newSubwayFavList.add(newRouteStopFav);
					}
				}
				// BUS STOPs
				if (FavListTab.this.currentBusStopFavList == null || !Fav.listEquals(FavListTab.this.currentBusStopFavList, newBusStopFavList)) {
					if (Utils.getCollectionSize(newBusStopFavList) > 0) {
						MyLog.d(TAG, "Loading bus stop favorites from DB...");
						this.busStopsList = AbstractManager.findRouteTripStops(FavListTab.this, StmBusManager.CONTENT_URI, newBusStopFavList, true);
						MyLog.d(TAG, "Loading bus stop favorites from DB... DONE");
					}
					if (this.busStopsList == null) {
						this.busStopsList = new ArrayList<RouteTripStop>(); // empty but updated
					}
				}
				// SUBWAY STATIONs
				if (FavListTab.this.currentSubwayStationFavList == null || !Fav.listEquals(FavListTab.this.currentSubwayStationFavList, newSubwayFavList)) {
					if (Utils.getCollectionSize(newBusStopFavList) > 0) {
						MyLog.d(TAG, "Loading subway station favorites from DB...");
						this.subwayStationsList = AbstractManager.findRouteTripStops(FavListTab.this, StmSubwayManager.CONTENT_URI, newSubwayFavList, true);
						MyLog.d(TAG, "Loading subway station favorites from DB... DONE");
					}
					if (this.subwayStationsList == null) {
						this.subwayStationsList = new ArrayList<RouteTripStop>(); // empty but updated
					}
				}
				// BIKE STATIONs
				List<Fav> newBikeFavList = DataManager.findFavsByTypeList(getContentResolver(), Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
				if (FavListTab.this.currentBikeStationFavList == null || !Fav.listEquals(FavListTab.this.currentBikeStationFavList, newBikeFavList)) {
					if (Utils.getCollectionSize(newBikeFavList) > 0) {
						MyLog.d(TAG, "Loading bike station favorites from DB...");
						this.bikeStations = BixiManager.findBikeStationsMap(getContentResolver(),
								Utils.extractBikeStationTerminNamesFromFavList(newBikeFavList));
						MyLog.d(TAG, "Loading bike station favorites from DB... DONE");
					}
					if (this.bikeStations == null) {
						this.bikeStations = new HashMap<String, BikeStation>(); // empty but updated
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				MyLog.v(TAG, "loadFavoritesFromDB() > onPostExecute()");
				if (this.busStopsList != null) { // IF favorite bus stop list was refreshed DO update the UI
					refreshBusStopsUI(this.busStopsList);
				}
				if (this.subwayStationsList != null) { // IF favorite subway station list was refreshed DO update the UI
					refreshSubwayStationsUI(this.subwayStationsList);
				}
				if (this.bikeStations != null) { // IF favorite bike station list was refreshed DO update the UI
					refreshBikeStationsUI(this.bikeStations);
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
	 * @param busStops the bus stops (extended)
	 */
	private void refreshBusStopsUI(List<RouteTripStop> busStops) {
		MyLog.v(TAG, "refreshBusStopsUI(%s)", Utils.getCollectionSize(busStops));
		List<Fav> newBusStopFavList = new ArrayList<Fav>();
		if (busStops != null) {
			for (RouteTripStop busStop : busStops) {
				Fav newBusStopFav = new Fav();
				newBusStopFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
				newBusStopFav.setFkId(busStop.getUID());
				newBusStopFavList.add(newBusStopFav);
			}
		}
		if (this.currentBusStopFavList == null || this.currentBusStopFavList.size() != newBusStopFavList.size()) {
			this.currentBusStopFavList = newBusStopFavList;
			this.busAdapter.setPois(busStops);
			this.busAdapter.updateDistancesNow(getLocation());
			this.busAdapter.initManual();
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_bus_stops).setVisibility(View.VISIBLE);
			findViewById(R.id.bus_stops_list).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Refresh subway station UI.
	 * @param stations the new favorite subway stations
	 */
	private void refreshSubwayStationsUI(List<RouteTripStop> stations) {
		MyLog.v(TAG, "refreshSubwayStationsUI(%s)", Utils.getCollectionSize(stations));
		List<Fav> newSubwayFavList = new ArrayList<Fav>();
		if (stations != null) {
			for (RouteTripStop station : stations) {
				Fav newSubwayFav = new Fav();
				newSubwayFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
				newSubwayFav.setFkId(station.getUID());
				newSubwayFavList.add(newSubwayFav);
			}
		}
		if (this.currentSubwayStationFavList == null || this.currentSubwayStationFavList.size() != newSubwayFavList.size()) {
			this.currentSubwayStationFavList = newSubwayFavList;
			this.subwayAdapter.setPois(stations);
			this.subwayAdapter.updateDistancesNow(getLocation());
			this.subwayAdapter.initManual();
			findViewById(R.id.lists).setVisibility(View.VISIBLE);
			findViewById(R.id.fav_subway_stations).setVisibility(View.VISIBLE);
			findViewById(R.id.subway_stations_list).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Refresh bike station UI.
	 * @param bikeStations the new favorite bike stations
	 */
	private void refreshBikeStationsUI(Map<String, BikeStation> bikeStations) {
		MyLog.v(TAG, "refreshBikeStationsUI(%s)", Utils.getMapSize(bikeStations));
		List<Fav> newBikeFavList = new ArrayList<Fav>();
		if (bikeStations != null) {
			for (BikeStation bikeStation : bikeStations.values()) {
				Fav newBikeFav = new Fav();
				newBikeFav.setType(Fav.KEY_TYPE_VALUE_BIKE_STATIONS);
				newBikeFav.setFkId(bikeStation.getTerminalName());
				newBikeFavList.add(newBikeFav);
			}
		}
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
					// bike station name
					((TextView) view.findViewById(R.id.station_name)).setText(Utils.cleanBikeStationName(bikeStation.getName()));
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							startActivity(BikeStationInfo.newInstance(FavListTab.this, bikeStation));
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
														startActivity(BikeStationInfo.newInstance(FavListTab.this, bikeStation));
														break;
													case 1:
														// remove the view from the UI
														((LinearLayout) findViewById(R.id.bike_stations_list)).removeView(theViewToDelete);
														// remove the favorite from the current list
														Iterator<Fav> it = FavListTab.this.currentBikeStationFavList.iterator();
														while (it.hasNext()) {
															Fav fav = (Fav) it.next();
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
																Fav.KEY_TYPE_VALUE_BIKE_STATIONS, bikeStation.getTerminalName());
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
		// update bike stations
		LocationUtils.updateDistanceWithString(FavListTab.this, FavListTab.this.bikeStations, currentLocation, new LocationTaskCompleted() {

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
				this.busAdapter.setLocation(this.location);
				this.subwayAdapter.setLocation(this.location);
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

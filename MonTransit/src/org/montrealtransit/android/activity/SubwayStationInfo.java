package org.montrealtransit.android.activity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import org.montrealtransit.android.activity.v4.SubwayLineInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Show information about a subway station.
 * @author Mathieu Méa
 */
public class SubwayStationInfo extends Activity implements LocationListener, SensorEventListener, CompassListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayStationInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SubwayStation";
	/**
	 * Extra for the subway station ID.
	 */
	public static final String EXTRA_STATION_ID = "station_id";
	/**
	 * Extra for the subway station name (not required).
	 */
	public static final String EXTRA_STATION_NAME = "station_name";
	/**
	 * The subway station.
	 */
	private ASubwayStation subwayStation;
	/**
	 * The subway lines.
	 */
	private List<SubwayLine> subwayLines;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the location updates enabled?
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_station_info);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}

		// show the subway station
		String stationId = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_STATION_ID);
		String stationName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_STATION_NAME);
		showNewSubwayStation(stationId, stationName);
	}

	/**
	 * onCreate() method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		((CheckBox) findViewById(R.id.star)).setOnClickListener(new View.OnClickListener() {
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
		// IF location updates should be enabled DO
		if (!this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistanceWithNewLocation();
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		SensorUtils.unregisterSensorListener(this, this);
		this.compassUpdatesEnabled = false;
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
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		if (this.subwayStation == null) {
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							SubwayStationInfo.this.lastCompassInDegree = (int) orientation;
							SubwayStationInfo.this.lastCompassChanged = now;
							// update the view
							ImageView compassImg = (ImageView) findViewById(R.id.compass);
							if (location.getAccuracy() <= subwayStation.getDistance()) {
								float compassRotation = SensorUtils.getCompassRotationInDegree(location, subwayStation, lastCompassInDegree,
										locationDeclination);
								SupportFactory.get().rotateImageView(compassImg, compassRotation, SubwayStationInfo.this);
								compassImg.setVisibility(View.VISIBLE);
							} else {
								compassImg.setVisibility(View.INVISIBLE);
							}
						}
					}
				});
	}

	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		new AsyncTask<String, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(String... params) {
				// try to find the existing favorite
				Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_SUBWAY_STATION, params[0], null);
				// IF the station is already a favorite DO
				if (findFav != null) {
					// delete the favorite
					DataManager.deleteFav(SubwayStationInfo.this.getContentResolver(), findFav.getId());
					SupportFactory.get().backupManagerDataChanged(SubwayStationInfo.this);
					return false;
				} else {
					// add the favorite
					Fav newFav = new Fav();
					newFav.setType(Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
					newFav.setFkId(SubwayStationInfo.this.subwayStation.getId());
					newFav.setFkId2(null);
					DataManager.addFav(SubwayStationInfo.this.getContentResolver(), newFav);
					UserPreferences.savePrefLcl(SubwayStationInfo.this, UserPreferences.PREFS_LCL_IS_FAV, true);
					SupportFactory.get().backupManagerDataChanged(SubwayStationInfo.this);
					return true;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_added));
				} else {
					Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_removed));
				}
				setTheStar(); // TODO is remove useless?
			};

		}.execute(this.subwayStation.getId());
	}

	/**
	 * Show a new subway station
	 * @param newStationId the new subway station ID
	 */
	private void showNewSubwayStation(String newStationId, String newStationName) {
		MyLog.v(TAG, "showNewSubwayStation(%s, %s)", newStationId, newStationName);
		// temporary set station name
		((TextView) findViewById(R.id.station_name)).setText(newStationName);
		findViewById(R.id.star).setVisibility(View.INVISIBLE);
		if (this.subwayStation == null || !this.subwayStation.getId().equals(newStationId)) {
			new AsyncTask<String, Void, Pair<SubwayStation, List<SubwayLine>>>() {
				@Override
				protected Pair<SubwayStation, List<SubwayLine>> doInBackground(String... params) {
					// MyLog.d(TAG, "display a new subway station");
					SubwayStation findSubwayStation = StmManager.findSubwayStation(getContentResolver(), params[0]);
					List<SubwayLine> findSubwayStationLinesList = StmManager.findSubwayStationLinesList(getContentResolver(), findSubwayStation.getId());
					return new Pair<SubwayStation, List<SubwayLine>>(findSubwayStation, findSubwayStationLinesList);
				}

				@Override
				protected void onPostExecute(Pair<SubwayStation, List<SubwayLine>> result) {
					SubwayStationInfo.this.subwayStation = new ASubwayStation(result.first);
					SubwayStationInfo.this.subwayLines = result.second;
					refreshAll();
				};
			}.execute(newStationId);
		}
	}

	/**
	 * Refresh all the UI based on the subway station.
	 */
	private void refreshAll() {
		refreshSubwayStationInfo();
		refreshSchedule();
		refreshBusLines();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistanceWithNewLocation();
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh the subway station schedule.
	 */
	private void refreshSchedule() {
		findViewById(R.id.hours_loading).setVisibility(View.VISIBLE);
		findViewById(R.id.hours_list).setVisibility(View.GONE);
		new AsyncTask<String, Void, Void>() {
			private List<StmStore.SubwayLine> subwayLines;
			private SparseArray<SubwayStation> firstStationDirectionss;
			private SparseArray<String> firstStationDirFreq;
			private SparseArray<Pair<String, String>> firstStationDirDeps;
			private SparseArray<SubwayStation> lastStationDirections;
			private SparseArray<String> lastStationDirFreq;
			private SparseArray<Pair<String, String>> lastStationDirDeps;

			@Override
			protected Void doInBackground(String... params) {
				subwayLines = StmManager.findSubwayStationLinesList(SubwayStationInfo.this.getContentResolver(), params[0]);
				firstStationDirectionss = new SparseArray<SubwayStation>();
				firstStationDirFreq = new SparseArray<String>();
				firstStationDirDeps = new SparseArray<Pair<String, String>>();
				lastStationDirections = new SparseArray<SubwayStation>();
				lastStationDirFreq = new SparseArray<String>();
				lastStationDirDeps = new SparseArray<Pair<String, String>>();

				// find day and hour (minus 2 hours)
				Calendar now = Calendar.getInstance();
				now.add(Calendar.HOUR, -2);
				String dayOfTheWeek = Utils.getDayOfTheWeek(now);
				String time = Utils.getTimeOfTheDay(now);
				// FOR EACH subway line DO
				for (StmStore.SubwayLine subwayLine : subwayLines) {
					// FIRST direction
					StmStore.SubwayStation firstSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(getContentResolver(),
							subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER);
					firstStationDirectionss.put(subwayLine.getNumber(), firstSubwayStationDirection);
					firstStationDirFreq.put(subwayLine.getNumber(),
							StmManager.findSubwayDirectionFrequency(getContentResolver(), firstSubwayStationDirection.getId(), dayOfTheWeek, time));
					firstStationDirDeps.put(subwayLine.getNumber(), StmManager.findSubwayStationDepartures(getContentResolver(),
							SubwayStationInfo.this.subwayStation.getId(), firstSubwayStationDirection.getId(), dayOfTheWeek));
					// SECOND direction
					StmStore.SubwayStation lastSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(getContentResolver(),
							subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC);
					lastStationDirections.put(subwayLine.getNumber(), lastSubwayStationDirection);
					lastStationDirFreq.put(subwayLine.getNumber(),
							StmManager.findSubwayDirectionFrequency(getContentResolver(), lastSubwayStationDirection.getId(), dayOfTheWeek, time));
					lastStationDirDeps.put(subwayLine.getNumber(), StmManager.findSubwayStationDepartures(getContentResolver(),
							SubwayStationInfo.this.subwayStation.getId(), lastSubwayStationDirection.getId(), dayOfTheWeek));
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// FOR EACH subway line DO
				for (StmStore.SubwayLine subwayLine : subwayLines) {
					LinearLayout hoursListLayout = (LinearLayout) findViewById(R.id.hours_list);
					// FIRST direction
					// IF the direction is not the subway station DO
					if (!SubwayStationInfo.this.subwayStation.getId().equals(firstStationDirectionss.get(subwayLine.getNumber()).getId())) {
						// list divider
						if (hoursListLayout.getChildCount() > 0) {
							hoursListLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, hoursListLayout, false));
						}
						// list view
						hoursListLayout.addView(getDirectionView(subwayLine, firstStationDirectionss.get(subwayLine.getNumber()), hoursListLayout,
								firstStationDirFreq.get(subwayLine.getNumber()), firstStationDirDeps.get(subwayLine.getNumber())));
					}
					// SECOND direction
					// IF the direction is not the subway station DO
					if (!SubwayStationInfo.this.subwayStation.getId().equals(lastStationDirections.get(subwayLine.getNumber()).getId())) {
						// list divider
						if (hoursListLayout.getChildCount() > 0) {
							hoursListLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, hoursListLayout, false));
						}
						// list view
						hoursListLayout.addView(getDirectionView(subwayLine, lastStationDirections.get(subwayLine.getNumber()), hoursListLayout,
								lastStationDirFreq.get(subwayLine.getNumber()), lastStationDirDeps.get(subwayLine.getNumber())));
					}
				}
				findViewById(R.id.hours_loading).setVisibility(View.GONE);
				findViewById(R.id.hours_list).setVisibility(View.VISIBLE);
			};

			/**
			 * @param subwayLine the subway line
			 * @param subwayStationDir the direction
			 * @return the direction view for the subway line and the direction
			 */
			private View getDirectionView(SubwayLine subwayLine, SubwayStation subwayStationDir, LinearLayout root, String frequency, Pair<String, String> deps) {
				// MyLog.v(TAG, "getDirectionView(" + subwayLine.getNumber() + ", " + subwayStationDir.getId() + ")");
				// create view
				View dView = getLayoutInflater().inflate(R.layout.subway_station_info_line_schedule_direction, root, false);
				// SUBWAY LINE color
				int subwayLineImgId = SubwayUtils.getSubwayLineImgId(subwayLine.getNumber());
				((ImageView) dView.findViewById(R.id.subway_img)).setImageResource(subwayLineImgId);
				// DIRECTION - SUBWAY STATION name
				((TextView) dView.findViewById(R.id.direction_station)).setText(subwayStationDir.getName());
				// FIRST LAST DEPARTURE
				String firstCleanDep = Utils.formatHours(SubwayStationInfo.this, deps.first);
				String secondCleanDep = Utils.formatHours(SubwayStationInfo.this, deps.second);
				((TextView) dView.findViewById(R.id.direction_hours)).setText(firstCleanDep + " - " + secondCleanDep);
				// FREQUENCY
				if (frequency != null) {
					((TextView) dView.findViewById(R.id.direction_frequency)).setText(getString(R.string.minutes_and_minutes, frequency));
				} else {
					((TextView) dView.findViewById(R.id.direction_frequency)).setText(R.string.no_service);
				}
				return dView;
			}

		}.execute(this.subwayStation.getId());
	}

	/**
	 * Refresh the subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		// subway station name
		((TextView) findViewById(R.id.station_name)).setText(this.subwayStation.getName());
		// set the favorite icon
		setTheStar();
		// subway lines colors
		if (this.subwayLines.size() > 0) {
			ImageView lineTypeImg1 = (ImageView) findViewById(R.id.subway_line_1);
			lineTypeImg1.setVisibility(View.VISIBLE);
			int subwayLineImg = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(0).getNumber());
			lineTypeImg1.setImageResource(subwayLineImg);
			lineTypeImg1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(%s)", v.getId());
					Intent intent = new Intent(SubwayStationInfo.this, SupportFactory.get().getSubwayLineInfoClass());
					String subwayLineNumber = String.valueOf(SubwayStationInfo.this.subwayLines.get(0).getNumber());
					intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
					intent.putExtra(SubwayLineInfo.EXTRA_STATION_ID, SubwayStationInfo.this.subwayStation.getId());
					startActivity(intent);
				}
			});
			if (this.subwayLines.size() > 1) {
				ImageView lineTypeImg2 = (ImageView) findViewById(R.id.subway_line_2);
				lineTypeImg2.setVisibility(View.VISIBLE);
				int subwayLineImg2 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(1).getNumber());
				lineTypeImg2.setImageResource(subwayLineImg2);
				lineTypeImg2.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", v.getId());
						Intent intent = new Intent(SubwayStationInfo.this, SupportFactory.get().getSubwayLineInfoClass());
						String subwayLineNumber = String.valueOf(SubwayStationInfo.this.subwayLines.get(1).getNumber());
						intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
						intent.putExtra(SubwayLineInfo.EXTRA_STATION_ID, SubwayStationInfo.this.subwayStation.getId());
						startActivity(intent);
					}
				});
				if (this.subwayLines.size() > 2) {
					ImageView lineTypeImg3 = (ImageView) findViewById(R.id.subway_line_3);
					lineTypeImg3.setVisibility(View.VISIBLE);
					int subwayLineImg3 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(2).getNumber());
					lineTypeImg3.setImageResource(subwayLineImg3);
					lineTypeImg3.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(SubwayStationInfo.this, SupportFactory.get().getSubwayLineInfoClass());
							int lineNumber = SubwayStationInfo.this.subwayLines.get(2).getNumber();
							String subwayLineNumber = String.valueOf(lineNumber);
							intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
							intent.putExtra(SubwayLineInfo.EXTRA_STATION_ID, SubwayStationInfo.this.subwayStation.getId());
							startActivity(intent);
						}
					});
				}
			}
		}
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistanceWithNewLocation() {
		Location currentLocation = getLocation();
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.subwayStation != null) {
			// distance & accuracy
			LocationUtils.updateDistance(this, this.subwayStation, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					TextView distanceTv = (TextView) findViewById(R.id.distance);
					distanceTv.setText(SubwayStationInfo.this.subwayStation.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;
	private float locationDeclination;

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
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
				}
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistanceWithNewLocation();
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

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		MyLog.v(TAG, "setTheStar()");
		// try to find the existing favorite
		new AsyncTask<Void, Void, Fav>() {
			@Override
			protected Fav doInBackground(Void... params) {
				return DataManager.findFav(SubwayStationInfo.this.getContentResolver(), Fav.KEY_TYPE_VALUE_SUBWAY_STATION,
						SubwayStationInfo.this.subwayStation.getId(), null);
			}

			protected void onPostExecute(Fav result) {
				final CheckBox starCb = (CheckBox) findViewById(R.id.star);
				starCb.setChecked(result != null);
				starCb.setVisibility(View.VISIBLE);
			};
		}.execute();
	}

	/**
	 * Refresh the bus lines.
	 */
	private void refreshBusLines() {
		MyLog.v(TAG, "refreshBusLines()");
		new AsyncTask<String, Void, List<BusStop>>() {
			@Override
			protected List<BusStop> doInBackground(String... params) {
				return StmManager.findSubwayStationBusStopsWithLineList(SubwayStationInfo.this.getContentResolver(), params[0]);
			}

			@Override
			protected void onPostExecute(List<BusStop> result) {
				LinearLayout busLinesLayout = (LinearLayout) findViewById(R.id.bus_line_list);
				busLinesLayout.removeAllViews();
				Set<String> busLinesNumberDirection = new HashSet<String>();
				// IF there is one or more bus lines DO
				if (Utils.getCollectionSize(result) > 0) {
					findViewById(R.id.bus_line).setVisibility(View.VISIBLE);
					busLinesLayout.setVisibility(View.VISIBLE);
					// FOR EACH bus line DO
					for (final BusStop busStop : result) {
						// check if same bus line number & direction not already in the list
						if (busLinesNumberDirection.contains(busStop.getLineNumber() + BusUtils.getBusLineSimpleDirection(busStop.getDirectionId()))) {
							continue;
						}
						// create view
						View view = getLayoutInflater().inflate(R.layout.subway_station_info_bus_line_list_item, busLinesLayout, false);
						// bus line number
						((TextView) view.findViewById(R.id.line_number)).setText(busStop.getLineNumber());
						int color = BusUtils.getBusLineTypeBgColor(busStop.getLineTypeOrNull(), busStop.getLineNumber());
						((TextView) view.findViewById(R.id.line_number)).setBackgroundColor(color);
						// bus line direction
						int busLineDirection = BusUtils.getBusLineSimpleDirection(busStop.getDirectionId());
						((TextView) view.findViewById(R.id.line_direction)).setText(getString(busLineDirection).toUpperCase(Locale.getDefault()));
						// add click listener
						view.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(SubwayStationInfo.this, BusStopInfo.class);
								intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
								intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStop.getPlace());
								intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
								intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, busStop.getLineNameOrNull());
								intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, busStop.getLineTypeOrNull());
								startActivity(intent);
							}
						});
						view.setOnLongClickListener(new View.OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								new BusLineSelectDirection(SubwayStationInfo.this, busStop.getLineNumber(), busStop.getLineNameOrNull(), busStop
										.getLineTypeOrNull()).showDialog();
								return true;
							}
						});
						busLinesLayout.addView(view);
						busLinesNumberDirection.add(busStop.getLineNumber() + BusUtils.getBusLineSimpleDirection(busStop.getDirectionId()));
						SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(SubwayStationInfo.this, busStop, null, true, false),
								PrefetchingUtils.getExecutor());
					}
				} else {
					findViewById(R.id.bus_line).setVisibility(View.GONE);
					busLinesLayout.setVisibility(View.GONE);
				}
			};
		}.execute(this.subwayStation.getId());
	}

	/**
	 * Show the station in a radar-enabled application.
	 * @param v the view (not used)
	 */
	public void showStationInRadar(View v) {
		// IF the a radar activity is available DO
		if (!Utils.isIntentAvailable(this, "com.google.android.radar.SHOW_RADAR")) {
			// tell the user he needs to install a radar library.
			new NoRadarInstalled(this).showDialog();
		} else {
			// Launch the radar activity
			Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
			intent.putExtra("latitude", (double) this.subwayStation.getLat());
			intent.putExtra("longitude", (double) this.subwayStation.getLng());
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException ex) {
				MyLog.w(TAG, "Radar activity not found.");
				new NoRadarInstalled(this).showDialog();
			}
		}
	}

	/**
	 * Show the station location in a maps-enabled app.
	 * @param v the view (not used)
	 */
	public void showStationLocationInMaps(View v) {
		Uri uri = Uri.parse(String.format("geo:%s,%s", this.subwayStation.getLat(), this.subwayStation.getLng()));
		startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.subway_station_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map:
			showStationLocationInMaps(null);
			return true;
		case R.id.radar:
			showStationInRadar(null);
			return true;
		default:
			return MenuUtils.handleCommonMenuActions(this, item);
		}
	}
}

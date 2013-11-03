package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.v4.SubwayLineInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
	private static final String EXTRA_STATION_ID = "station_id";
	/**
	 * Extra for the subway station name (not required).
	 */
	private static final String EXTRA_STATION_NAME = "station_name";
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

	private POIArrayAdapter adapter;

	public static Intent newInstance(Context context, String stationId) {
		return newInstance(context, stationId, null);
	}

	public static Intent newInstance(Context context, SubwayStation subwayStation) {
		return newInstance(context, subwayStation.getId(), subwayStation.getName());
	}

	public static Intent newInstance(Context context, String stationId, String stationName) {
		Intent intent = new Intent(context, SubwayStationInfo.class);
		intent.putExtra(EXTRA_STATION_ID, stationId);
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
		setContentView(R.layout.subway_station_info);

		this.adapter = new POIArrayAdapter(this);
		this.adapter.setManualLayout((ViewGroup) findViewById(R.id.nearby_list));
		this.adapter.setManualScrollView((ScrollView) findViewById(R.id.scrollview));

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
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		refreshFavoriteIDsFromDB();
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
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
		findViewById(R.id.nearby_title).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.GONE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		if (this.subwayStation == null || !this.subwayStation.getId().equals(newStationId)) {
			new AsyncTask<String, Void, Pair<SubwayStation, List<SubwayLine>>>() {
				@Override
				protected Pair<SubwayStation, List<SubwayLine>> doInBackground(String... params) {
					// MyLog.d(TAG, "display a new subway station");
					SubwayStation findSubwayStation = StmManager.findSubwayStation(SubwayStationInfo.this, params[0]);
					List<SubwayLine> findSubwayStationLinesList = StmManager.findSubwayStationLinesList(SubwayStationInfo.this, findSubwayStation.getId());
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
		// refreshNearby();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
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
			private SparseArray<SubwayStation> firstStationDirectionss;
			private SparseArray<String> firstStationDirFreq;
			private SparseArray<Pair<String, String>> firstStationDirDeps;
			private SparseArray<SubwayStation> lastStationDirections;
			private SparseArray<String> lastStationDirFreq;
			private SparseArray<Pair<String, String>> lastStationDirDeps;

			@Override
			protected Void doInBackground(String... params) {
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
				if (SubwayStationInfo.this.subwayLines != null) {
					for (StmStore.SubwayLine subwayLine : SubwayStationInfo.this.subwayLines) {
						// FIRST direction
						StmStore.SubwayStation firstSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(SubwayStationInfo.this,
								subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER);
						firstStationDirectionss.put(subwayLine.getNumber(), firstSubwayStationDirection);
						firstStationDirFreq.put(subwayLine.getNumber(),
								StmManager.findSubwayDirectionFrequency(SubwayStationInfo.this, firstSubwayStationDirection.getId(), dayOfTheWeek, time));
						firstStationDirDeps.put(subwayLine.getNumber(), StmManager.findSubwayStationDepartures(SubwayStationInfo.this,
								SubwayStationInfo.this.subwayStation.getId(), firstSubwayStationDirection.getId(), dayOfTheWeek));
						// SECOND direction
						StmStore.SubwayStation lastSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(SubwayStationInfo.this,
								subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC);
						lastStationDirections.put(subwayLine.getNumber(), lastSubwayStationDirection);
						lastStationDirFreq.put(subwayLine.getNumber(),
								StmManager.findSubwayDirectionFrequency(SubwayStationInfo.this, lastSubwayStationDirection.getId(), dayOfTheWeek, time));
						lastStationDirDeps.put(subwayLine.getNumber(), StmManager.findSubwayStationDepartures(SubwayStationInfo.this,
								SubwayStationInfo.this.subwayStation.getId(), lastSubwayStationDirection.getId(), dayOfTheWeek));
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// FOR EACH subway line DO
				refreshScheduleUI();
				refreshNearby();
			};

			private void refreshScheduleUI() {
				if (subwayLines != null) {
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
				}
			}

		}.execute(this.subwayStation.getId());
	}

	/**
	 * @param subwayLine the subway line
	 * @param subwayStationDir the direction
	 * @return the direction view for the subway line and the direction
	 */
	private View getDirectionView(SubwayLine subwayLine, SubwayStation subwayStationDir, LinearLayout root, String frequency, Pair<String, String> deps) {
		// MyLog.v(TAG, "getDirectionView(%s, %s), subwayLine.getNumber(), subwayStationDir.getId());
		// create view
		View dView = getLayoutInflater().inflate(R.layout.subway_station_info_line_schedule_direction, root, false);
		// SUBWAY LINE color
		int subwayLineImgId = SubwayUtils.getSubwayLineImgId(subwayLine.getNumber());
		((ImageView) dView.findViewById(R.id.subway_img)).setImageResource(subwayLineImgId);
		// DIRECTION - SUBWAY STATION name
		((TextView) dView.findViewById(R.id.direction_station)).setText(subwayStationDir.getName());
		// FIRST LAST DEPARTURE
		String firstCleanDep = Utils.formatHours(this, deps.first);
		String secondCleanDep = Utils.formatHours(this, deps.second);
		((TextView) dView.findViewById(R.id.direction_hours)).setText(firstCleanDep + " - " + secondCleanDep);
		// FREQUENCY
		if (frequency != null) {
			((TextView) dView.findViewById(R.id.direction_frequency)).setText(getString(R.string.minutes_and_minutes, frequency));
		} else {
			((TextView) dView.findViewById(R.id.direction_frequency)).setText(R.string.no_service);
		}
		return dView;
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
		if (this.subwayLines != null && this.subwayLines.size() > 0) {
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
	private void updateDistanceWithNewLocation(Location currentLocation) {
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.subwayStation != null) {
			// distance & accuracy
			LocationUtils.updateDistanceWithString(this, this.subwayStation, currentLocation, new LocationTaskCompleted() {

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
				this.adapter.setLocation(this.location);
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
				}
				updateDistanceWithNewLocation(this.location);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		setLocation(location);
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

	private void refreshNearby() {
		MyLog.v(TAG, "refreshNearby()");
		findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.VISIBLE);
		new AsyncTask<String, Void, List<POI>>() {
			@Override
			protected List<POI> doInBackground(String... params) {
				// MyLog.v(TAG, "refreshNearby()>doInBackground()");
				List<POI> result = new ArrayList<POI>();
				final Double lat = SubwayStationInfo.this.subwayStation.getLat();
				final Double lng = SubwayStationInfo.this.subwayStation.getLng();
				// TODO asynchronous from all content providers
				final Collection<ASubwayStation> stations = ClosestSubwayStationsFinderTask.getAllStationsWithLines(SubwayStationInfo.this, lat, lng);
				Iterator<ASubwayStation> it = stations.iterator();
				while (it.hasNext()) {
					ASubwayStation station = it.next();
					if (station.getId().equals(SubwayStationInfo.this.subwayStation.getId())) {
						it.remove();
					}
				}
				result.addAll(stations);
				result.addAll(StmBusManager.findRouteTripStopsWithLatLngList(SubwayStationInfo.this, lat, lng));
				// TODO ? result.addAll(ClosestBikeStationsFinderTask.getABikeStations(BixiManager.findAllBikeStationsLocationList(contentResolver, lat, lng)));
				LocationUtils.updateDistance(result, lat, lng);
				Collections.sort(result, new POI.POIDistanceComparator());
				if (Utils.NB_NEARBY_LIST > 0 && result.size() > Utils.NB_NEARBY_LIST) {
					return result.subList(0, Utils.NB_NEARBY_LIST);
				} else {
					return result;
				}
			}

			@Override
			protected void onPostExecute(List<POI> result) {
				// MyLog.v(TAG, "refreshNearby()>onPostExecute()");
				SubwayStationInfo.this.adapter.setPois(result);
				SubwayStationInfo.this.adapter.updateDistancesNow(getLocation());
				refreshFavoriteIDsFromDB();
				// show the result
				SubwayStationInfo.this.adapter.initManual();
				findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
				findViewById(R.id.nearby_loading).setVisibility(View.GONE);
				findViewById(R.id.nearby_list).setVisibility(View.VISIBLE);
			}

		}.execute();
	}

	private void refreshFavoriteIDsFromDB() {
		// MyLog.v(TAG, "refreshFavoriteIDsFromDB()");
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				// MyLog.v(TAG, "refreshFavoriteIDsFromDB()>doInBackground()");
				return DataManager.findAllFavsList(getContentResolver());
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				// MyLog.v(TAG, "refreshFavoriteIDsFromDB()>onPostExecute()");
				SubwayStationInfo.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * Show the station in a radar-enabled application.
	 * @param v the view (not used)
	 */
	public void showStationInRadar(View v) {
		LocationUtils.showPOILocationInRadar(this, this.subwayStation);
	}

	/**
	 * Show the station location in a maps-enabled app.
	 * @param v the view (not used)
	 */
	public void showStationLocationInMaps(View v) {
		LocationUtils.showPOILocationInMap(this, this.subwayStation);
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

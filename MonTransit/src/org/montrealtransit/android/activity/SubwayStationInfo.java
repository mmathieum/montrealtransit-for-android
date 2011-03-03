package org.montrealtransit.android.activity;

import java.util.Calendar;
import java.util.List;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.dialog.SubwayStationSelectBusLineStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Show information about a subway station.
 * @author Mathieu Méa
 */
public class SubwayStationInfo extends Activity implements LocationListener {

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
	 * The subway station.
	 */
	private StmStore.SubwayStation subwayStation;
	/**
	 * The subway lines.
	 */
	private List<SubwayLine> subwayLines;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_station_info);
		((CheckBox) findViewById(R.id.star)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// manage favorite star click
				DataStore.Fav fav = DataManager
				        .findFav(SubwayStationInfo.this.getContentResolver(),
				                DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION, SubwayStationInfo.this.subwayStation
				                        .getId(), null);
				// IF the station is already a favorite DO
				if (fav != null) {
					// delete the favorite
					DataManager.deleteFav(SubwayStationInfo.this.getContentResolver(), fav.getId());
					Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_removed));
				} else {
					// add the favorite
					DataStore.Fav newFav = new DataStore.Fav();
					newFav.setType(DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
					newFav.setFkId(SubwayStationInfo.this.subwayStation.getId());
					newFav.setFkId2(null);
					DataManager.addFav(SubwayStationInfo.this.getContentResolver(), newFav);
					Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_added));
				}
				setTheStar(); // TODO is remove useless?
			}
		});
		// show the subway station
		showNewSubwayStation(Utils.getSavedStringValue(this.getIntent(), savedInstanceState, EXTRA_STATION_ID));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistanceWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onRestart();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		super.onResume();
	}

	/**
	 * Show a new subway station
	 * @param newStationId the new subway station ID
	 */
	private void showNewSubwayStation(String newStationId) {
		MyLog.v(TAG, "showNewSubwayStation(%s)", newStationId);
		if (this.subwayStation == null || !this.subwayStation.getId().equals(newStationId)) {
			MyLog.v(TAG, "display a new subway station");
			this.subwayStation = StmManager.findSubwayStation(getContentResolver(), newStationId);
			this.subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), this.subwayStation.getId());
			refreshAll();
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
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Refresh the subway station schedule.
	 */
	private void refreshSchedule() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.hours_list);
		List<StmStore.SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(),
		        this.subwayStation.getId());
		// FOR EACH subway line DO
		for (StmStore.SubwayLine subwayLine : subwayLines) {
			// FIRST direction
			StmStore.SubwayStation firstSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(
			        getContentResolver(), subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER);
			// IF the direction is not the subway station DO
			if (!this.subwayStation.getId().equals(firstSubwayStationDirection.getId())) {
				// list divider
				if (layout.getChildCount() > 0) {
					layout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// list view
				layout.addView(getDirectionView(subwayLine, firstSubwayStationDirection));
			}
			// SECOND direction
			StmStore.SubwayStation lastSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(
			        getContentResolver(), subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC);
			// IF the direction is not the subway station DO
			if (!this.subwayStation.getId().equals(lastSubwayStationDirection.getId())) {
				// list divider
				if (layout.getChildCount() > 0) {
					layout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// list view
				layout.addView(getDirectionView(subwayLine, lastSubwayStationDirection));
			}

		}
	}

	/**
	 * @param subwayLine the subway line
	 * @param subwayStationDir the direction
	 * @return the direction view for the subway line and the direction
	 */
	private View getDirectionView(SubwayLine subwayLine, SubwayStation subwayStationDir) {
		// MyLog.v(TAG, "getDirectionView(" + subwayLine.getNumber() + ", " + subwayStationDir.getId() + ")");
		// find day and hour (minus 2 hours)
		Calendar now = Calendar.getInstance();
		now.add(Calendar.HOUR, -2);
		String dayOfTheWeek = Utils.getDayOfTheWeek(now);
		String time = Utils.getTimeOfTheDay(now);
		// create view
		View dView = getLayoutInflater().inflate(R.layout.subway_station_info_line_schedule_direction, null);
		// SUBWAY LINE color
		ImageView ivSubwayLineColor1 = (ImageView) dView.findViewById(R.id.subway_img);
		ivSubwayLineColor1.setImageResource(SubwayUtils.getSubwayLineImgId(subwayLine.getNumber()));
		// DIRECTION - SUBWAY STATION name
		TextView tvSubwayStation1 = (TextView) dView.findViewById(R.id.direction_station);
		tvSubwayStation1.setText(subwayStationDir.getName());
		// FIRST LAST DEPARTURE
		Pair<String, String> deps = StmManager.findSubwayStationDepartures(getContentResolver(), this.subwayStation
		        .getId(), subwayStationDir.getId(), dayOfTheWeek);
		TextView tvHours1 = (TextView) dView.findViewById(R.id.direction_hours);
		String firstCleanDep = Utils.formatHours(this, deps.first);
		String secondCleanDep = Utils.formatHours(this, deps.second);
		tvHours1.setText(firstCleanDep + " - " + secondCleanDep);
		// FREQUENCY
		String frequency = StmManager.findSubwayDirectionFrequency(getContentResolver(), subwayStationDir.getId(),
		        dayOfTheWeek, time);
		TextView tvDirFreq1 = (TextView) dView.findViewById(R.id.direction_frequency);
		tvDirFreq1.setText(getString(R.string.minutes_and_minutes, frequency));
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
		if (this.subwayLines.size() > 0) {
			findViewById(R.id.subway_line_1).setVisibility(View.VISIBLE);
			int subwayLineImg = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(0).getNumber());
			((ImageView) findViewById(R.id.subway_line_1)).setImageResource(subwayLineImg);
			((ImageView) findViewById(R.id.subway_line_1)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(%s)", v.getId());
					Intent intent = new Intent(SubwayStationInfo.this, SubwayLineInfo.class);
					String subwayLineNumber = String.valueOf(SubwayStationInfo.this.subwayLines.get(0).getNumber());
					intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
					startActivity(intent);
				}
			});
			if (this.subwayLines.size() > 1) {
				findViewById(R.id.subway_line_2).setVisibility(View.VISIBLE);
				int subwayLineImg2 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(1).getNumber());
				((ImageView) findViewById(R.id.subway_line_2)).setImageResource(subwayLineImg2);
				((ImageView) findViewById(R.id.subway_line_2)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", v.getId());
						Intent intent = new Intent(SubwayStationInfo.this, SubwayLineInfo.class);
						String subwayLineNumber = String.valueOf(SubwayStationInfo.this.subwayLines.get(1).getNumber());
						intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
						startActivity(intent);
					}
				});
				if (this.subwayLines.size() > 2) {
					findViewById(R.id.subway_line_3).setVisibility(View.VISIBLE);
					int subwayLineImg3 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(2).getNumber());
					((ImageView) findViewById(R.id.subway_line_3)).setImageResource(subwayLineImg3);
					((ImageView) findViewById(R.id.subway_line_3)).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(SubwayStationInfo.this, SubwayLineInfo.class);
							int lineNumber = SubwayStationInfo.this.subwayLines.get(2).getNumber();
							String subwayLineNumber = String.valueOf(lineNumber);
							intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
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
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", getLocation());
		if (getLocation() != null && this.subwayStation != null) {
			// distance & accuracy
			Location stationLocation = LocationUtils.getNewLocation(subwayStation.getLat(), subwayStation.getLng());
			float distanceInMeters = getLocation().distanceTo(stationLocation);
			float accuracyInMeters = getLocation().getAccuracy();
			// MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
			String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
			((TextView) findViewById(R.id.distance)).setText(distanceString);
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;

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
			MyLog.v(TAG, String.format("new location: '%s' %s,%s (%s)", newLocation.getProvider(), newLocation
			        .getLatitude(), newLocation.getLongitude(), newLocation.getAccuracy()));
			if (this.location == null || LocationUtils.isMorePrecise(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistanceWithNewLocation();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderEnabled(String provider) {
		MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderDisabled(String provider) {
		MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		MyLog.v(TAG, "setTheStar()");
		Fav fav = DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION,
		        this.subwayStation.getId(), null);
		((CheckBox) findViewById(R.id.star)).setChecked(fav != null);
	}

	/**
	 * Refresh the bus lines.
	 */
	private void refreshBusLines() {
		MyLog.v(TAG, "refreshBusLines()");
		List<StmStore.BusLine> busLinesList = StmManager.findSubwayStationBusLinesList(getContentResolver(),
		        this.subwayStation.getId());
		LinearLayout busLinesLayout = (LinearLayout) findViewById(R.id.bus_line_list);
		busLinesLayout.removeAllViews();
		// IF there is one or more bus lines DO
		if (busLinesList != null && busLinesList.size() > 0) {
			findViewById(R.id.bus_line).setVisibility(View.VISIBLE);
			busLinesLayout.setVisibility(View.VISIBLE);
			// FOR EACH bus line DO
			for (StmStore.BusLine busLine : busLinesList) {
				// create view
				View view = getLayoutInflater().inflate(R.layout.subway_station_info_bus_line_list_item, null);
				// bus line number
				final String lineNumber = busLine.getNumber();
				((TextView) view.findViewById(R.id.line_number)).setText(lineNumber);
				int color = BusUtils.getBusLineTypeBgColorFromType(busLine.getType());
				((TextView) view.findViewById(R.id.line_number)).setBackgroundColor(color);
				// add click listener
				view.setOnClickListener(new SubwayStationSelectBusLineStop(this, subwayStation.getId(), lineNumber));
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						MyLog.d(TAG, "bus line number: %s", lineNumber);
						BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(
						        SubwayStationInfo.this, lineNumber);
						busLineSelectDirection.showDialog();
						return true;
					}
				});
				busLinesLayout.addView(view);
			}
		} else {
			findViewById(R.id.bus_line).setVisibility(View.GONE);
			busLinesLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Menu for showing the subway station in Maps.
	 */
	private static final int MENU_SHOW_SUBWAY_STATION_IN_MAPS = Menu.FIRST;
	/**
	 * Menu for using a radar to get to the subway station.
	 */
	private static final int MENU_USE_RADAR_TO_THE_SUBWAY_STATION = Menu.FIRST + 1;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuShowMaps = menu.add(0, MENU_SHOW_SUBWAY_STATION_IN_MAPS, Menu.NONE, R.string.show_in_map);
		menuShowMaps.setIcon(android.R.drawable.ic_menu_mapmode);
		MenuItem menuUseRadar = menu.add(0, MENU_USE_RADAR_TO_THE_SUBWAY_STATION, Menu.NONE, R.string.use_radar);
		menuUseRadar.setIcon(android.R.drawable.ic_menu_compass);
		MenuItem menuPref = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menuPref.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_SUBWAY_STATION_IN_MAPS:
			try {
				Uri uri = Uri.parse(String.format("geo:%s,%s", subwayStation.getLat(), subwayStation.getLng()));
				startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
				return true;
			} catch (Exception e) {
				MyLog.e(TAG, e, "Error while launching map");
				return false;
			}
		case MENU_USE_RADAR_TO_THE_SUBWAY_STATION:
			// IF the a radar activity is available DO
			if (!Utils.isIntentAvailable(this, "com.google.android.radar.SHOW_RADAR")) {
				// tell the user he needs to install a radar library.
				NoRadarInstalled noRadar = new NoRadarInstalled(this);
				noRadar.showDialog();
			} else {
				// Launch the radar activity
				Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
				intent.putExtra("latitude", (float) this.subwayStation.getLat());
				intent.putExtra("longitude", (float) this.subwayStation.getLng());
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException ex) {
					MyLog.w(TAG, "Radar activity not found.");
					NoRadarInstalled noRadar = new NoRadarInstalled(this);
					noRadar.showDialog();
				}
			}
			return true;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			return true;
		default:
			MyLog.d(TAG, "Unknow menu id: %s.", item.getItemId());
			return false;
		}
	}
}

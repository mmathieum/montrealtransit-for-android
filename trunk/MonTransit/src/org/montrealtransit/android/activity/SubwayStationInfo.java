package org.montrealtransit.android.activity;

import java.util.Calendar;
import java.util.List;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.dialog.SubwayStationSelectBusLineStop;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Show information about a subway station.
 * @author Mathieu Méa
 */
public class SubwayStationInfo extends Activity implements OnClickListener, LocationListener {

	/**
	 * Extra for the subway station ID.
	 */
	public static final String EXTRA_STATION_ID = "station_id";
	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayStationInfo.class.getSimpleName();
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
		((ImageView) findViewById(R.id.subway_line_1)).setOnClickListener(this);
		((ImageView) findViewById(R.id.subway_line_2)).setOnClickListener(this);
		((ImageView) findViewById(R.id.subway_line_3)).setOnClickListener(this);
		// show the subway station
		showNewSubwayStation(Utils.getSavedStringValue(this.getIntent(), savedInstanceState,
		        SubwayStationInfo.EXTRA_STATION_ID));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onResume();
	}

	/**
	 * Show a new subway station
	 * @param newStationId the new subway station ID
	 */
	private void showNewSubwayStation(String newStationId) {
		MyLog.v(TAG, "showNewSubwayStation(" + newStationId + ")");
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
			updateDistanceWithNewLocation();
		}
		// IF location updates is not already enabled DO
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
		MyLog.v(TAG, "getDirectionView(" + subwayLine.getNumber() + ", " + subwayStationDir.getId() + ")");
		// find day and hour (minus 2 hours)
		Calendar now = Calendar.getInstance();
		now.add(Calendar.HOUR, -2);
		String dayOfTheWeek = Utils.getDayOfTheWeek(now);
		String time = Utils.getTimeOfTheDay(now);
		// create view
		View dView = getLayoutInflater().inflate(R.layout.subway_station_info_line_schedule_direction, null);
		// SUBWAY LINE color
		ImageView ivSubwayLineColor1 = (ImageView) dView.findViewById(R.id.subway_img);
		ivSubwayLineColor1.setImageResource(Utils.getSubwayLineImg(subwayLine.getNumber()));
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
		tvDirFreq1.setText(frequency + " " + getResources().getString(R.string.minutes));
		return dView;
	}

	/**
	 * Refresh the subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		// subway station name
		((TextView) findViewById(R.id.station_name)).setText(this.subwayStation.getName());
		// subway lines colors
		if (this.subwayLines.size() > 0) {
			((ImageView) findViewById(R.id.subway_line_1)).setVisibility(View.VISIBLE);
			int subwayLineImg = Utils.getSubwayLineImg(this.subwayLines.get(0).getNumber());
			((ImageView) findViewById(R.id.subway_line_1)).setImageResource(subwayLineImg);
			if (this.subwayLines.size() > 1) {
				((ImageView) findViewById(R.id.subway_line_2)).setVisibility(View.VISIBLE);
				int subwayLineImg2 = Utils.getSubwayLineImg(this.subwayLines.get(1).getNumber());
				((ImageView) findViewById(R.id.subway_line_2)).setImageResource(subwayLineImg2);
				if (this.subwayLines.size() > 2) {
					((ImageView) findViewById(R.id.subway_line_3)).setVisibility(View.VISIBLE);
					int subwayLineImg3 = Utils.getSubwayLineImg(this.subwayLines.get(2).getNumber());
					((ImageView) findViewById(R.id.subway_line_3)).setImageResource(subwayLineImg3);
				}
			}
		}
		// TODO bus line colors ?
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistanceWithNewLocation() {
		MyLog.v(TAG, "updateDistanceWithNewLocation(" + getLocation() + ")");
		if (getLocation() != null && this.subwayStation != null) {
			// distance & accuracy
			Location stationLocation = LocationUtils.getNewLocation(subwayStation.getLat(), subwayStation.getLng());
			float distanceInMeters = getLocation().distanceTo(stationLocation);
			float accuracyInMeters = getLocation().getAccuracy();
			MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
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
	 * @param location the new location
	 */
	public void setLocation(Location location) {
		if (location != null) {
			MyLog.v(TAG, "setLocation(" + location.getProvider() + ", " + location.getLatitude() + ", "
			        + location.getLongitude() + ", " + location.getAccuracy() + ")", this, MyLog.SHOW_LOCATION);
		}
		this.location = location;
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
		MyLog.v(TAG, "onProviderEnabled(" + provider + ")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderDisabled(String provider) {
		MyLog.v(TAG, "onProviderDisabled(" + provider + ")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		MyLog.v(TAG, "onStatusChanged(" + provider + ", " + status + ")");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick(" + v.getId() + ")");
		if (this.subwayLines.size() == 1) {
			Intent intent = new Intent(this, SubwayLineInfo.class);
			String subwayLineNumber = String.valueOf(this.subwayLines.get(0).getNumber());
			intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumber);
			startActivity(intent);
		} else {
			// TODO show subway lines selector?
		}
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
			((TextView) findViewById(R.id.bus_line)).setVisibility(View.VISIBLE);
			busLinesLayout.setVisibility(View.VISIBLE);
			// FOR EACH bus line DO
			for (StmStore.BusLine busLine : busLinesList) {
				// list view divider
				if (busLinesLayout.getChildCount() > 0) {
					busLinesLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// create view
				View view = getLayoutInflater().inflate(R.layout.subway_station_info_bus_line_list_item, null);
				// bus line type image
				int busLineTypeImg = Utils.getBusLineTypeImgFromType(busLine.getType());
				((ImageView) view.findViewById(R.id.line_type)).setImageResource(busLineTypeImg);
				// bus line number
				((TextView) view.findViewById(R.id.line_number)).setText(busLine.getNumber());
				// bus line name
				((TextView) view.findViewById(R.id.line_name)).setText(busLine.getName());
				// bus line hours
				String formattedHours = Utils.getFormatted2Hours(this, busLine.getHours(), "-");
				((TextView) view.findViewById(R.id.hours)).setText(formattedHours);
				// add click listener
				view.setOnClickListener(new SubwayStationSelectBusLineStop(this, this.subwayStation.getId(), busLine
				        .getNumber()));
				busLinesLayout.addView(view);
			}
		} else {
			((TextView) findViewById(R.id.bus_line)).setVisibility(View.GONE);
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
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 3;

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
		MenuItem menuAbout = menu.add(0, MENU_ABOUT, Menu.NONE, R.string.menu_about);
		menuAbout.setIcon(android.R.drawable.ic_menu_info_details);
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
				Uri uri = Uri.parse("geo:" + this.subwayStation.getLat() + "," + this.subwayStation.getLng());
				startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
				return true;
			} catch (Exception e) {
				MyLog.e(TAG, "Error while launching map", e);
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
		case MENU_ABOUT:
			Utils.showAboutDialog(this);
			return true;
		default:
			MyLog.d(TAG, "Unknow menu id: " + item.getItemId() + ".");
			return false;
		}
	}
}

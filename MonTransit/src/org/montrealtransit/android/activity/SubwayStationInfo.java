package org.montrealtransit.android.activity;

import java.util.Calendar;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.dialog.SubwayStationSelectBusLineStop;
import org.montrealtransit.android.provider.DataManager;
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
import android.os.Build;
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
	 * The favorite star check box.
	 */
	private CheckBox startCb;
	/**
	 * The connecting bus lines layout.
	 */
	private LinearLayout busLinesLayout;
	/**
	 * The connecting bus lines view.
	 */
	private View busLineTitleView;
	/**
	 * The hours list layout.
	 */
	private LinearLayout hoursListLayout;
	/**
	 * The station name text view.
	 */
	private TextView stationNameTv;
	/**
	 * The first subway line type image.
	 */
	private ImageView lineTypeImg1;
	/**
	 * The second subway line type image.
	 */
	private ImageView lineTypeImg2;
	/**
	 * The third subway line type image.
	 */
	private ImageView lineTypeImg3;
	/**
	 * The distance text view.
	 */
	private TextView distanceTv;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_station_info);

		this.startCb = (CheckBox) findViewById(R.id.star);
		this.busLinesLayout = (LinearLayout) findViewById(R.id.bus_line_list);
		this.busLineTitleView = findViewById(R.id.bus_line);
		this.hoursListLayout = (LinearLayout) findViewById(R.id.hours_list);
		this.stationNameTv = (TextView) findViewById(R.id.station_name);
		this.lineTypeImg1 = (ImageView) findViewById(R.id.subway_line_1);
		this.lineTypeImg2 = (ImageView) findViewById(R.id.subway_line_2);
		this.lineTypeImg3 = (ImageView) findViewById(R.id.subway_line_3);
		this.distanceTv = (TextView) findViewById(R.id.distance);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}

		// show the subway station
		showNewSubwayStation(Utils.getSavedStringValue(this.getIntent(), savedInstanceState, EXTRA_STATION_ID));
	}

	/**
	 * onCreate() method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		this.startCb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addOrRemoveFavorite(v);
			}
		});
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
		super.onResume();
	}

	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_SUBWAY_STATION,
		        this.subwayStation.getId(), null);
		// IF the station is already a favorite DO
		if (findFav != null) {
			// delete the favorite
			DataManager.deleteFav(SubwayStationInfo.this.getContentResolver(), findFav.getId());
			Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_removed));
		} else {
			// add the favorite
			Fav newFav = new Fav();
			newFav.setType(Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			newFav.setFkId(SubwayStationInfo.this.subwayStation.getId());
			newFav.setFkId2(null);
			DataManager.addFav(SubwayStationInfo.this.getContentResolver(), newFav);
			Utils.notifyTheUser(SubwayStationInfo.this, getString(R.string.favorite_added));
		}
		setTheStar(); // TODO is remove useless?
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
				if (this.hoursListLayout.getChildCount() > 0) {
					this.hoursListLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// list view
				this.hoursListLayout.addView(getDirectionView(subwayLine, firstSubwayStationDirection));
			}
			// SECOND direction
			StmStore.SubwayStation lastSubwayStationDirection = StmManager.findSubwayLineLastSubwayStation(
			        getContentResolver(), subwayLine.getNumber(), StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC);
			// IF the direction is not the subway station DO
			if (!this.subwayStation.getId().equals(lastSubwayStationDirection.getId())) {
				// list divider
				if (this.hoursListLayout.getChildCount() > 0) {
					this.hoursListLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// list view
				this.hoursListLayout.addView(getDirectionView(subwayLine, lastSubwayStationDirection));
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
		int subwayLineImgId = SubwayUtils.getSubwayLineImgId(subwayLine.getNumber());
		((ImageView) dView.findViewById(R.id.subway_img)).setImageResource(subwayLineImgId);
		// DIRECTION - SUBWAY STATION name
		((TextView) dView.findViewById(R.id.direction_station)).setText(subwayStationDir.getName());
		// FIRST LAST DEPARTURE
		Pair<String, String> deps = StmManager.findSubwayStationDepartures(getContentResolver(),
		        this.subwayStation.getId(), subwayStationDir.getId(), dayOfTheWeek);
		String firstCleanDep = Utils.formatHours(this, deps.first);
		String secondCleanDep = Utils.formatHours(this, deps.second);
		((TextView) dView.findViewById(R.id.direction_hours)).setText(firstCleanDep + " - " + secondCleanDep);
		// FREQUENCY
		String frequency = StmManager.findSubwayDirectionFrequency(getContentResolver(), subwayStationDir.getId(),
		        dayOfTheWeek, time);
		String dirFreq = getString(R.string.minutes_and_minutes, frequency);
		((TextView) dView.findViewById(R.id.direction_frequency)).setText(dirFreq);
		return dView;
	}

	/**
	 * Refresh the subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		// subway station name
		this.stationNameTv.setText(this.subwayStation.getName());
		// set the favorite icon
		setTheStar();
		// subway lines colors
		if (this.subwayLines.size() > 0) {
			this.lineTypeImg1.setVisibility(View.VISIBLE);
			int subwayLineImg = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(0).getNumber());
			this.lineTypeImg1.setImageResource(subwayLineImg);
			this.lineTypeImg1.setOnClickListener(new OnClickListener() {
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
				this.lineTypeImg2.setVisibility(View.VISIBLE);
				int subwayLineImg2 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(1).getNumber());
				this.lineTypeImg2.setImageResource(subwayLineImg2);
				this.lineTypeImg2.setOnClickListener(new OnClickListener() {
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
					this.lineTypeImg3.setVisibility(View.VISIBLE);
					int subwayLineImg3 = SubwayUtils.getSubwayLineImgListMiddleId(this.subwayLines.get(2).getNumber());
					this.lineTypeImg3.setImageResource(subwayLineImg3);
					this.lineTypeImg3.setOnClickListener(new OnClickListener() {
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
			// MyLog.d(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
			String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
			this.distanceTv.setText(distanceString);
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
			MyLog.d(TAG, "new location: '%s'.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
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
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_SUBWAY_STATION,
		        this.subwayStation.getId(), null);
		this.startCb.setChecked(findFav != null);
	}

	/**
	 * Refresh the bus lines.
	 */
	private void refreshBusLines() {
		MyLog.v(TAG, "refreshBusLines()");
		List<StmStore.BusLine> busLinesList = StmManager.findSubwayStationBusLinesList(getContentResolver(),
		        this.subwayStation.getId());
		this.busLinesLayout.removeAllViews();
		// IF there is one or more bus lines DO
		if (busLinesList != null && busLinesList.size() > 0) {
			this.busLineTitleView.setVisibility(View.VISIBLE);
			this.busLinesLayout.setVisibility(View.VISIBLE);
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
						new BusLineSelectDirection(SubwayStationInfo.this, lineNumber).showDialog();
						return true;
					}
				});
				this.busLinesLayout.addView(view);
			}
		} else {
			this.busLineTitleView.setVisibility(View.GONE);
			this.busLinesLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Show the station in a radar-enabled app.
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
			intent.putExtra("latitude", (float) this.subwayStation.getLat());
			intent.putExtra("longitude", (float) this.subwayStation.getLng());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.subway_station_info_menu);
	}

	/**
	 * {@inheritDoc}
	 */
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

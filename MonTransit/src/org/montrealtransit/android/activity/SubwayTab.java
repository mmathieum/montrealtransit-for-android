package org.montrealtransit.android.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.data.SubwayStationDistancesComparator;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Display a list of subway lines.
 * @author Mathieu Méa
 */
public class SubwayTab extends Activity implements LocationListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayTab.class.getSimpleName();

	/**
	 * The cursor used to display the subway lines.
	 */
	private Cursor cursor;

	/**
	 * Store the device location.
	 */
	private Location location;

	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;

	/**
	 * The address used for the closest stations.
	 */
	public Address locationAddress = null;

	/**
	 * The accuracy of the address used for the closest stations.
	 */
	public Float locationAdressAccuracy = null;

	/**
	 * The location used for the closest stations.
	 */
	private Location closestStationsLocation = null;

	/**
	 * The closest stations.
	 */
	private ASubwayStation[] closestStations;

	/**
	 * The minimum number of closest stations in the list.
	 */
	private static final int MIN_CLOSEST_STATIONS_LIST_SIZE = 3;
	/**
	 * The maximum number of closest stations in the list.
	 */
	private static final int MAX_CLOSEST_STATIONS_LIST_SIZE = 5;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_tab);
		refreshAll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onResume();
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
	 * Refresh all the UI.
	 */
	private void refreshAll() {
		refreshSubwayLines();
		refreshClosestSubwayStations();
	}

	/**
	 * Refresh the subway lines UI.
	 */
	private void refreshSubwayLines() {
		LinearLayout subwayLinesLayout = (LinearLayout) findViewById(R.id.subway_line_list);
		List<SubwayLine> subwayLines = StmManager.findAllSubwayLinesList(this.getContentResolver());
		int i = 0;
		for (SubwayLine subwayLine : subwayLines) {
			// create view
			View view = subwayLinesLayout.getChildAt(i++);
			// subway line type image
			String subwayLineName = getResources().getString(Utils.getSubwayLineNameShort(subwayLine.getNumber()));
			((TextView) view.findViewById(R.id.line_name)).setText(subwayLineName);
			// subway line colors
			((ImageView) view.findViewById(R.id.subway_img)).setBackgroundColor(Utils.getSubwayLineColor(subwayLine
			        .getNumber()));

			final String subwayLineNumberS = String.valueOf(subwayLine.getNumber());
			// add click listener
			view.setOnClickListener(new View.OnClickListener() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(" + v.getId() + ")");
					Intent intent = new Intent(SubwayTab.this, SubwayLineInfo.class);
					intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumberS);
					startActivity(intent);
				}
			});
			final int subwayLineNumberI = subwayLine.getNumber();
			view.setOnLongClickListener(new View.OnLongClickListener() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public boolean onLongClick(View v) {
					MyLog.v(TAG, "onLongClick(" + v.getId() + ")");
					SubwayLineSelectDirection subwayLineSelectDirection = new SubwayLineSelectDirection(SubwayTab.this,
					        subwayLineNumberI);
					subwayLineSelectDirection.showDialog();
					return true;
				}
			});
		}
	}

	/**
	 * Refresh the closest stations UI.
	 */
	private void refreshClosestSubwayStations() {
		// clean the list divider
		TextView tv = (TextView) findViewById(R.id.closest_subway_stations);
		tv.setText(R.string.closest_subway_stations);
		LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
		// clean the list
		subwayStationsLayout.removeAllViews();
		if (LocationUtils.getProviders(this).size() == 0) {
			// no location providers available
			MyLog.w(TAG, "no location provider available");
			// set the loading message
			TextView noLocationProviderTv = new TextView(this);
			noLocationProviderTv
			        .setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			noLocationProviderTv.setText(R.string.no_location_provider_available);
			subwayStationsLayout.addView(noLocationProviderTv);
		} else {
			if (this.closestStationsLocation == null
			        || this.closestStationsLocation.getTime() == getLocation().getTime()) {
				// set the loading message
				TextView loadingTv = new TextView(this);
				loadingTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				loadingTv.setText(R.string.waiting_for_location_fix);
				subwayStationsLayout.addView(loadingTv);
				// use last know location if available
				if (LocationUtils.getBestLastKnownLocation(this) != null) {
					// set the distance before showing the list
					setLocation(LocationUtils.getBestLastKnownLocation(this));
					updateDistancesWithNewLocation();
				}
			} else {
				// use the last location
				this.closestStations = null;
				updateDistancesWithNewLocation();
			}
			// enable location updates
			// IF location updates are not already enabled DO
			if (!this.locationUpdatesEnabled) {
				// enable
				LocationUtils.enableLocationUpdates(this, this);
				this.locationUpdatesEnabled = true;
			}
		}
	}

	/**
	 * Find the closest subway stations.
	 * @author Mathieu Méa
	 */
	private class FindClosestSubwayStationsTask extends AsyncTask<String, String, ASubwayStation[]> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected ASubwayStation[] doInBackground(String... params) {
			SubwayTab.this.closestStationsLocation = SubwayTab.this.getLocation();
			if (SubwayTab.this.closestStationsLocation != null) {
				SubwayTab.this.locationAdressAccuracy = SubwayTab.this.closestStationsLocation.getAccuracy();

				publishProgress(getString(R.string.loading));

				// reverse geocoding of the location
				SubwayTab.this.locationAddress = null;
				Geocoder geocoder = new Geocoder(SubwayTab.this);
				try {
					int maxResults = 1;
					List<Address> addresses = geocoder.getFromLocation(SubwayTab.this.closestStationsLocation
					        .getLatitude(), SubwayTab.this.closestStationsLocation.getLongitude(), maxResults);
					if (addresses != null && addresses.size() >= 1) {
						SubwayTab.this.locationAddress = addresses.get(0);
						MyLog.d(TAG, "address:" + SubwayTab.this.locationAddress.getAddressLine(0));
					}
				} catch (IOException e) {
					MyLog.e(TAG, "Can't find the adress of the current location", e);
				}

				Map<String, Set<Integer>> stationsWithOtherLines = new HashMap<String, Set<Integer>>();
				for (Pair<SubwayLine, SubwayStation> lineStation : StmManager
				        .findSubwayStationsAndLinesList(SubwayTab.this.getContentResolver())) {
					int subwayLineNumber = lineStation.first.getNumber();
					String subwayStationId = lineStation.second.getId();
					if (stationsWithOtherLines.get(subwayStationId) == null) {
						stationsWithOtherLines.put(subwayStationId, new HashSet<Integer>());
					}
					stationsWithOtherLines.get(subwayStationId).add(subwayLineNumber);
				}

				List<SubwayStation> subwayStationsList = StmManager.findAllSubwayStationsList(getContentResolver());
				List<ASubwayStation> stations = new ArrayList<ASubwayStation>();
				for (SubwayStation subwayStation : subwayStationsList) {
					ASubwayStation station = new ASubwayStation();
					station.setId(subwayStation.getId());
					station.setName(subwayStation.getName());
					station.setLat(subwayStation.getLat());
					station.setLng(subwayStation.getLng());
					// add other subway lines
					if (stationsWithOtherLines.containsKey(station.getId())) {
						station.addOtherLinesId(stationsWithOtherLines.get(station.getId()));
					}
					// location
					Location stationLocation = LocationUtils.getNewLocation(subwayStation.getLat(), subwayStation
					        .getLng());
					station.setDistance(SubwayTab.this.closestStationsLocation.distanceTo(stationLocation));
					String distanceString = Utils.getDistanceString(SubwayTab.this, station.getDistance(),
					        locationAdressAccuracy);
					station.setDistanceString(distanceString);
					stations.add(station);
				}
				// order the stations list by distance (closest first)
				SubwayStationDistancesComparator comparator = new SubwayStationDistancesComparator();
				Collections.sort(stations, comparator);

				// select only the firsts stations
				List<ASubwayStation> displayedStations = new ArrayList<ASubwayStation>();
				for (ASubwayStation station : stations) {
					if ((station.getDistance() < locationAdressAccuracy && displayedStations.size() < MAX_CLOSEST_STATIONS_LIST_SIZE)
					        || displayedStations.size() < MIN_CLOSEST_STATIONS_LIST_SIZE) {
						displayedStations.add(station);
					} else {
						break;
					}
				}
				return displayedStations.toArray(new ASubwayStation[0]);
			} else {
				return null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onProgressUpdate(String... values) {
			if (values.length > 0) {
				LinearLayout subwayStationsLayout = (LinearLayout) SubwayTab.this
				        .findViewById(R.id.subway_stations_list);
				// clean the list
				subwayStationsLayout.removeAllViews();
				// set the loading message
				TextView loadingTv = new TextView(SubwayTab.this);
				loadingTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				loadingTv.setText(values[0]);
				subwayStationsLayout.addView(loadingTv);
				super.onProgressUpdate(values);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(ASubwayStation[] result) {
			SubwayTab.this.closestStations = result;
			refreshClosestSubwayStationsList();
			super.onPostExecute(result);
		}
	}

	/**
	 * Refresh the closest subway stations list.
	 */
	private void refreshClosestSubwayStationsList() {
		if (this.closestStations != null) {
			TextView tv = (TextView) findViewById(R.id.closest_subway_stations);
			String text = "" + getResources().getText(R.string.closest_subway_stations);
			if (this.locationAddress != null) {
				text += " (";
				if (this.locationAddress.getThoroughfare() != null) {
					text += this.locationAddress.getThoroughfare();
				}
				if (this.locationAddress.getLocality() != null) {
					text += ", " + this.locationAddress.getLocality();
				}
				if (this.locationAdressAccuracy != null) {
					text += " ± " + Utils.getDistanceString(this, this.locationAdressAccuracy, 0);
				}
				text += ")";
			}
			tv.setText(text);

			LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
			subwayStationsLayout.removeAllViews();
			int i = 1;
			for (ASubwayStation station : this.closestStations) {
				// list view divider
				if (subwayStationsLayout.getChildCount() > 0) {
					subwayStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// create view
				View view = getLayoutInflater().inflate(R.layout.subway_tab_subway_stations_list_item, null);
				view.setTag("station" + i++);
				// subway station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				// station lines color
				if (station.getOtherLinesId() != null && station.getOtherLinesId().size() > 0) {
					int subwayLineImg1 = Utils.getSubwayLineImg(station.getOtherLinesId().get(0));
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImg1);
					if (station.getOtherLinesId().size() > 1) {
						int subwayLineImg2 = Utils.getSubwayLineImg(station.getOtherLinesId().get(1));
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImg2);
						if (station.getOtherLinesId().size() > 2) {
							int subwayLineImg3 = Utils.getSubwayLineImg(station.getOtherLinesId().get(2));
							((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImg3);
						} else {
							((ImageView) view.findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
						}
					} else {
						((ImageView) view.findViewById(R.id.subway_img_2)).setVisibility(View.GONE);
						((ImageView) view.findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
					}
				} else {
					((ImageView) view.findViewById(R.id.subway_img_1)).setVisibility(View.GONE);
					((ImageView) view.findViewById(R.id.subway_img_2)).setVisibility(View.GONE);
					((ImageView) view.findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
				}
				// station distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
				}
				// add click listener
				final String subwayStationId = station.getId();
				view.setOnClickListener(new View.OnClickListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(" + v.getId() + ")");
						Intent intent = new Intent(SubwayTab.this, SubwayStationInfo.class);
						intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
						startActivity(intent);
					}
				});
				subwayStationsLayout.addView(view);
			}
		}
	}

	/**
	 * Refresh the closest subway stations distances in the list.
	 */
	private void refreshClosestSubwayStationsDistancesList() {
		LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
		int i = 1;
		for (ASubwayStation station : this.closestStations) {
			View stationView = subwayStationsLayout.findViewWithTag("station" + i++);
			if (stationView != null && !TextUtils.isEmpty(station.getDistanceString())) {
				((TextView) stationView.findViewById(R.id.distance)).setText(station.getDistanceString());
			}
		}
	}

	/**
	 * Update the subway stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		if (getLocation() != null && this.closestStations == null) {
			// create the closest stations list
			new FindClosestSubwayStationsTask().execute();
		} else if (getLocation() != null && this.closestStations != null) {
			// update the list distances
			float accuracyInMeters = getLocation().getAccuracy();
			for (ASubwayStation station : this.closestStations) {
				// distance
				Location stationLocation = LocationUtils.getNewLocation(station.getLat(), station.getLng());
				float distanceInMeters = getLocation().distanceTo(stationLocation);
				// MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
				String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
				station.setDistanceString(distanceString);
			}
			// update the view
			refreshClosestSubwayStationsDistancesList();
		}
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			MyLog.v(TAG, "new location: " + newLocation);
			if (this.location == null || LocationUtils.isMorePrecise(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
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
	 * {@inheritDoc}
	 */
	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistancesWithNewLocation();
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
	 * Menu to reload the closest subway stations.
	 */
	private static final int MENU_RELOAD_CLOSEST_STATIONS = Menu.FIRST;
	/**
	 * Menu to show the subway map from the STM.info Web Site.
	 */
	private static final int MENU_SHOW_MAP_ON_THE_STM_WEBSITE = Menu.FIRST + 1;
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
		MenuItem menuReload = menu.add(0, MENU_RELOAD_CLOSEST_STATIONS, Menu.NONE, R.string.reload_closest_stations);
		menuReload.setIcon(R.drawable.ic_menu_refresh);

		menu.add(0, MENU_SHOW_MAP_ON_THE_STM_WEBSITE, 0, R.string.show_map_from_stm_website);

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
		case MENU_SHOW_MAP_ON_THE_STM_WEBSITE:
			String url = "http://www.stm.info/metro/images/plan-metro.jpg";
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			break;
		case MENU_RELOAD_CLOSEST_STATIONS:
			refreshClosestSubwayStations();
			break;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			break;
		case MENU_ABOUT:
			Utils.showAboutDialog(this);
			break;
		default:
			MyLog.d(TAG, "Unknow menu action:" + item.getItemId() + ".");
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		super.onDestroy();
	}
}

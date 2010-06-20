package org.montrealtransit.android.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.dialog.SubwayLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, OnItemClickListener,
        ViewBinder, FilterQueryProvider, LocationListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineInfo.class.getSimpleName();

	/**
	 * The extra for the subway line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra for the subway station display order.
	 */
	public static final String EXTRA_ORDER_PREF = "extra_order_pref";
	/**
	 * The subway line.
	 */
	private StmStore.SubwayLine subwayLine;
	/**
	 * The subway line direction.
	 */
	private StmStore.SubwayStation lastSubwayStation;
	/**
	 * The subway station list order ID.
	 */
	private String orderPref;
	/**
	 * Store the other subway line for the subway stations
	 */
	private Map<String, List<String>> subwayStationOtherLines;
	/**
	 * The cursor used to display the subway station.
	 */
	private Cursor cursor;
	/**
	 * Store the subway station locations.
	 */
	private HashMap<String, Pair<Double, Double>> subwayStationLocations;
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
		setContentView(R.layout.subway_line_info);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
		((ListView) findViewById(R.id.list)).setOnItemClickListener(this);
		// get info from the intent.
		int newLineNumber = Integer.valueOf(Utils.getSavedStringValue(this.getIntent(), savedInstanceState,
		        SubwayLineInfo.EXTRA_LINE_NUMBER));
		String newOrderPref = Utils.getSavedStringValue(this.getIntent(), savedInstanceState,
		        SubwayLineInfo.EXTRA_ORDER_PREF);
		showNewSubway(newLineNumber, newOrderPref);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showNewSubway(int newLineNumber, String newOrderPref) {
		MyLog.v(TAG, "showNewSubway(" + newLineNumber + ", " + newOrderPref + ")");
		if ((this.subwayLine == null || this.subwayLine.getNumber() != newLineNumber)
		        || (this.orderPref != null && !this.orderPref.equals(newOrderPref))) {
			MyLog.v(TAG, "new subway line / stations order");
			this.subwayLine = StmManager.findSubwayLine(getContentResolver(), newLineNumber);
			if (newOrderPref == null) {
				newOrderPref = Utils.getSharedPreferences(this, UserPreferences
				        .getPrefsSubwayStationsOrder(this.subwayLine.getNumber()),
				        UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
			} else {
				Utils.saveSharedPreferences(this, UserPreferences
				        .getPrefsSubwayStationsOrder(this.subwayLine.getNumber()),
						newOrderPref);
			}
			this.orderPref = newOrderPref;
			refreshAll();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
		refreshSubwayStationsList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		if (locationUpdatesEnabled) {
			this.getLocationManager().removeUpdates(this);
		}
		super.onPause();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		if (this.locationUpdatesEnabled) {
			enableLocationUpdates();
		}
		super.onResume();
	}

	/**
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		refreshSubwayLineInfo();
		refreshSubwayStationsList();
	}

	/**
	 * Refresh the subway line info.
	 */
	private void refreshSubwayLineInfo() {
		// subway line name
		((TextView) findViewById(R.id.line_name)).setText(Utils.getSubwayLineName(subwayLine.getNumber()));
		((ImageView) findViewById(R.id.subway_img)).setImageResource(Utils.getSubwayLineImg(subwayLine.getNumber()));

		// subway line direction
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		this.lastSubwayStation = StmManager.findSubwayLineLastSubwayStation(this.getContentResolver(), this.subwayLine
		        .getNumber(), orderId);
		String separatorText = this.getResources().getString(R.string.subway_stations) + " (" + getDirectionText()
		        + ")";
		((TextView) findViewById(R.id.subway_line_station_string)).setText(separatorText);
		SubwayLineSelectDirection selectSubwayStationOrder = new SubwayLineSelectDirection(this, this.subwayLine
		        .getNumber(), this);
		((TextView) findViewById(R.id.subway_line_station_string)).setOnClickListener(selectSubwayStationOrder);
	}

	/**
	 * @return  the sort order from the order preference
	 */
	private String getSortOrderFromOrderPref(int subwayLineNumber) {
		String prefsSubwayStationsOrder = UserPreferences.getPrefsSubwayStationsOrder(subwayLineNumber);
		String sharedPreferences = Utils.getSharedPreferences(this, prefsSubwayStationsOrder,
		        UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
		if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER;
		} else if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC;
		} else {
			return StmStore.SubwayStation.DEFAULT_SORT_ORDER; // DEFAULT (A-Z order)
		}
	}

	/**
	 * @return the direction test (the direction(station) or the A-Z order)
	 */
	private String getDirectionText() {
		if (this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)
		        || this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return this.getResources().getString(R.string.direction) + " " + this.lastSubwayStation.getName();
		} else {
			// DEFAULT: A-Z order
			this.orderPref = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT;
			return this.getResources().getString(R.string.alphabetical_order);
		}
	}

	/**
	 * Refresh the subway stations list.
	 */
	private void refreshSubwayStationsList() {
		// 1 - store some useful subway stations info
		this.subwayStationLocations = new HashMap<String, Pair<Double, Double>>();
		this.subwayStationOtherLines = new HashMap<String, List<String>>();
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(this.getContentResolver(),
		        this.subwayLine.getNumber(), orderId);
		for (SubwayStation subwayStation : subwayStationsList) {
			// store the station location
			this.subwayStationLocations.put(subwayStation.getId(), new Pair<Double, Double>(subwayStation.getLat(),
			        subwayStation.getLng()));
			// store the other subway station lines
			List<String> otherSubwayLinesIds = Utils.extractSubwayLineNumbers(StmManager.findSubwayStationLinesList(
			        getContentResolver(), subwayStation.getId()));
			if (otherSubwayLinesIds != null) {
				otherSubwayLinesIds.remove(String.valueOf(this.subwayLine.getNumber()));
				this.subwayStationOtherLines.put(subwayStation.getId(), otherSubwayLinesIds);
			}
		}
		// 2 - set the list adapter
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter());
	}

	/**
	 * @return the subway station list adapter.
	 */
	private SimpleCursorAdapter getAdapter() {
		MyLog.v(TAG, "getAdapter()");
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		this.cursor = StmManager
		        .findSubwayLineStations(this.getContentResolver(), this.subwayLine.getNumber(), orderId);
		String[] from = new String[] { StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_ID,
		        StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_NAME,
		        StmStore.SubwayStation.STATION_ID };
		int[] to = new int[] { R.id.subway_img_1, R.id.subway_img_2, R.id.subway_img_3, R.id.station_name,
		        R.id.distance };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.subway_line_info_stations_list_item,
		        this.cursor, from, to);
		adapter.setViewBinder(this);
		adapter.setFilterQueryProvider(this);
		return adapter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor runQuery(CharSequence constraint) {
		MyLog.v(TAG, "runQuery(" + constraint + ")");
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		return StmManager.searchSubwayLineStations(this.getContentResolver(), this.subwayLine.getNumber(), orderId,
		        constraint.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		MyLog.v(TAG, "setViewValue(" + view.getId() + ", " + columnIndex + ")");
		if (view.getId() == R.id.subway_img_1
		        && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			((ImageView) view).setImageResource(Utils.getSubwayLineImg(this.subwayLine.getNumber()));
			return true;
		} else if (view.getId() == R.id.subway_img_2
		        && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			if (this.subwayStationOtherLines.get(subwayStationID).size() > 0) {
				((ImageView) view).setVisibility(View.VISIBLE);
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(Integer.valueOf(this.subwayStationOtherLines
				        .get(subwayStationID).get(0))));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.subway_img_3
		        && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			if (this.subwayStationOtherLines.get(subwayStationID).size() > 1) {
				((ImageView) view).setVisibility(View.VISIBLE);
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(Integer.valueOf(this.subwayStationOtherLines
				        .get(subwayStationID).get(1))));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.distance
		        && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			// location
			if (getLocation() != null) {
				String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
				// subway station info
				Location subwayStationLocation = new Location("MonTransit");
				subwayStationLocation.setLatitude(this.subwayStationLocations.get(subwayStationID).first);
				subwayStationLocation.setLongitude(this.subwayStationLocations.get(subwayStationID).second);
				// distance
				float distanceInMeters = getLocation().distanceTo(subwayStationLocation);
				float accuracyInMeters = getLocation().getAccuracy();
				MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
				String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
				((TextView) view).setText(distanceString);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;

	/**
	 * Initialize if necessary.
	 * @return the location
	 */
	private Location getLocation() {
		if (this.location == null) {
			this.location = getLastKnownLocation();
			// enable location updates if necessary
			if (!this.locationUpdatesEnabled) {
				enableLocationUpdates();
			}
		}
		return this.location;
	}

	/**
	 * @param location the new location
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * @return the best last know location (GPS if available, if not NETWORK)
	 */
	private Location getLastKnownLocation() {
		Location lastGPSLocation = getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastNetworkLocation = getLocationManager().getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		return lastGPSLocation != null ? lastGPSLocation : lastNetworkLocation;
	}

	/**
	 * Enable location updates.
	 */
	public void enableLocationUpdates() {
		MyLog.v(TAG, "enableLocationUpdates()");
		// enable location updates
		long minTime = 2000; // 2 seconds
		float minDistance = 5; // 5 meters
		// use both location providers because GPS is better and NETWORK is useful when no GPS.
		this.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
		this.getLocationManager().requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
		this.locationUpdatesEnabled = true;
	}

	/**
	 * The location manager.
	 */
	private LocationManager locationManager;

	/**
	 * @return the location manager
	 */
	public LocationManager getLocationManager() {
		MyLog.v(TAG, "getLocationManager()");
		if (this.locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		return this.locationManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()"); // TODO update list view update? how? (maybe use provider ?)
		this.setLocation(location);
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
	 * Menu for changing the direction of the bus line.
	 */
	private static final int MENU_CHANGE_DIRECTION = Menu.FIRST;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 1;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuDirection = menu.add(0, MENU_CHANGE_DIRECTION, 0, R.string.change_direction);
		menuDirection.setIcon(android.R.drawable.ic_menu_compass);
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
		case MENU_CHANGE_DIRECTION:
			SubwayLineSelectDirection select = new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this);
			select.showDialog();
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		if (id > 0) {
			Intent intent = new Intent(this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, String.valueOf(id));
			startActivity(intent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null) {
			this.cursor.close();
		}
		super.onDestroy();
	}
}

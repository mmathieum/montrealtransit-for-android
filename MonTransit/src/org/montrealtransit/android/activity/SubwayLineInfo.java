package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.dialog.SubwayLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, OnItemClickListener,
        FilterQueryProvider, LocationListener {

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
	 * The subway station list order ID.
	 */
	private String orderPref;
	/**
	 * Is the location updates should be enabled?
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
				Utils.saveSharedPreferences(this, UserPreferences.getPrefsSubwayStationsOrder(this.subwayLine
				        .getNumber()), newOrderPref);
			}
			this.orderPref = newOrderPref;
			refreshAll();
		}
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
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		refreshSubwayLineInfo();
		refreshSubwayStationsList();
		// IF there is a valid last know location DO
		if (LocationUtils.getLocationManager(this) != null) {
			// set the distance before showing the list
			updateDistancesWithNewLocation();
		}
		// IF location updates is not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Refresh the subway stations list.
	 */
	private void refreshSubwayStationsList() {
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter());
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
		SubwayStation lastStation = StmManager.findSubwayLineLastSubwayStation(getContentResolver(), subwayLine
		        .getNumber(), orderId);
		String separatorText = this.getResources().getString(R.string.subway_stations) + " ("
		        + getDirectionText(lastStation) + ")";
		((TextView) findViewById(R.id.subway_line_station_string)).setText(separatorText);
		SubwayLineSelectDirection select = new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this);
		((TextView) findViewById(R.id.subway_line_station_string)).setOnClickListener(select);
	}

	/**
	 * @return the sort order from the order preference
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
	 * @param lastSubwayStation the last subway station
	 * @return the direction test (the direction(station) or the A-Z order)
	 */
	private String getDirectionText(SubwayStation lastSubwayStation) {
		if (this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)
		        || this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return this.getResources().getString(R.string.direction) + " " + lastSubwayStation.getName();
		} else {
			// DEFAULT: A-Z order
			this.orderPref = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT;
			return this.getResources().getString(R.string.alphabetical_order);
		}
	}

	/**
	 * Update the distances with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		if (getLocation() != null) {
			for (ASubwayStation station : stations) {
				// distance
				Location stationLocation = LocationUtils.getNewLocation(station.getLat(), station.getLng());
				float distanceInMeters = getLocation().distanceTo(stationLocation);
				float accuracyInMeters = getLocation().getAccuracy();
				// MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
				String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
				station.setDistanceString(distanceString);
			}
			this.adapter.notifyDataSetChanged();
		}
	}

	/**
	 * The array adapter.
	 */
	private ArrayAdapter<ASubwayStation> adapter;

	/**
	 * The subway stations.
	 */
	private ASubwayStation[] stations;

	/**
	 * @return the adapter
	 */
	private ArrayAdapter<ASubwayStation> getAdapter() {
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(this.getContentResolver(),
		        this.subwayLine.getNumber(), orderId);
		// preparing other stations lines data
		Map<String, Set<Integer>> stationsWithOtherLines = new HashMap<String, Set<Integer>>();
		for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayLineStationsWithOtherLinesList(this
		        .getContentResolver(), this.subwayLine.getNumber())) {
			int subwayLineNumber = lineStation.first.getNumber();
			String subwayStationId = lineStation.second.getId();
			if (stationsWithOtherLines.get(subwayStationId) == null) {
				stationsWithOtherLines.put(subwayStationId, new HashSet<Integer>());
			}
			stationsWithOtherLines.get(subwayStationId).add(subwayLineNumber);
		}

		// creating the list of the subways stations object
		this.stations = new ASubwayStation[subwayStationsList.size()];
		int i = 0;
		for (SubwayStation station : subwayStationsList) {
			ASubwayStation aStation = new ASubwayStation();
			aStation.setId(station.getId());
			aStation.setName(station.getName());
			aStation.setLat(station.getLat());
			aStation.setLng(station.getLng());
			aStation.setLineId(this.subwayLine.getNumber());
			// add other subway lines
			if (stationsWithOtherLines.containsKey(aStation.getId())) {
				aStation.addOtherLinesId(stationsWithOtherLines.get(aStation.getId()));
			}
			this.stations[i++] = aStation;
		}
		this.adapter = new ArrayAdapterWithCustomView(this, R.layout.subway_line_info_stations_list_item, stations);
		return this.adapter;
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	private class ArrayAdapterWithCustomView extends ArrayAdapter<ASubwayStation> {

		/**
		 * The layout inflater.
		 */
		private LayoutInflater layoutInflater;
		/**
		 * The stations.
		 */
		private ASubwayStation[] stations;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param textViewResourceId the text view resource ID (not used)
		 * @param objects the stations
		 */
		public ArrayAdapterWithCustomView(Context context, int textViewResourceId, ASubwayStation[] stations) {
			super(context, textViewResourceId, stations);
			this.stations = stations;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MyLog.v(TAG, "getView(" + position + ")");
			View view;
			if (convertView == null) {
				view = this.layoutInflater.inflate(R.layout.subway_line_info_stations_list_item, parent, false);
			} else {
				view = convertView;
			}
			ASubwayStation station = this.stations[position];
			if (station != null) {
				MyLog.v(TAG, "station:" + station.getId());
				// station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				// station lines color
				if (station.getOtherLinesId().size() > 0) {
					int lastIndex = station.getOtherLinesId().size() - 1;
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(Utils.getSubwayLineImg(station
					        .getOtherLinesId().get(lastIndex)));
				} else {
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(Utils.getSubwayLineImg(station
					        .getLineId()));
				}
				if (station.getOtherLinesId().size() > 0) {
					((ImageView) view.findViewById(R.id.subway_img_2)).setVisibility(View.VISIBLE);
					if (station.getOtherLinesId().size() == 1) {
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(Utils
						        .getSubwayLineImg(station.getLineId()));
					} else {
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(Utils
						        .getSubwayLineImg(station.getOtherLinesId().get(0)));
					}
				} else {
					((ImageView) view.findViewById(R.id.subway_img_2)).setVisibility(View.GONE);
				}

				if (station.getOtherLinesId().size() > 1) {
					((ImageView) view.findViewById(R.id.subway_img_3)).setVisibility(View.VISIBLE);
					if (station.getOtherLinesId().size() == 2) {
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(Utils
						        .getSubwayLineImg(station.getLineId()));
					} else {
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(Utils
						        .getSubwayLineImg(station.getOtherLinesId().get(1)));
					}
				} else {
					((ImageView) view.findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
				}
				// station distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
				}
			}
			return view;
		}
	}

	/**
	 * A subway station.
	 */
	private class ASubwayStation extends SubwayStation {

		/**
		 * The current main subway line ID.
		 */
		private Integer lineId;
		/**
		 * the other subway lines ID.
		 */
		private List<Integer> otherLinesId;
		/**
		 * The distance string.
		 */
		private String distanceString;

		/**
		 * @param lineId the new current subway line ID
		 */
		public void setLineId(int lineId) {
			this.lineId = lineId;
		}

		/**
		 * @return the current subway line ID.
		 */
		public int getLineId() {
			return lineId;
		}

		/**
		 * @param newLineNumber a new other subway line ID
		 */
		public void addOtherLinesId(Integer newLineNumber) {
			if (!this.getOtherLinesId().contains(newLineNumber)) {
				this.getOtherLinesId().add(newLineNumber);
			}
		}

		/**
		 * @param newLinesNumber the new other subway lines ID
		 */
		public void addOtherLinesId(Set<Integer> newLinesNumber) {
			for (Integer newLineNumber : newLinesNumber) {
				addOtherLinesId(newLineNumber);
			}
		}

		/**
		 * @return the other subway lines ID
		 */
		private List<Integer> getOtherLinesId() {
			if (this.otherLinesId == null) {
				this.otherLinesId = new ArrayList<Integer>();
			}
			return this.otherLinesId;
		}

		/**
		 * @param distanceString the new distance string
		 */
		public void setDistanceString(String distanceString) {
			this.distanceString = distanceString;
		}

		/**
		 * @return the distance string
		 */
		public String getDistanceString() {
			return distanceString;
		}
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
	 * Store the device location.
	 */
	private Location location;

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <b>NULL</b>
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
		if (this.stations != null && position < this.stations.length && this.stations[position] != null) {
			Intent intent = new Intent(this, SubwayStationInfo.class);
			String subwayStationId = this.stations[position].getId();
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
			startActivity(intent);
		}
	}
}

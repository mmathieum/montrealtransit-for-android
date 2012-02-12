package org.montrealtransit.android.activity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, LocationListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SubwayLine";

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
	 * The line stops list.
	 */
	private ListView list;
	/**
	 * The line name text view.
	 */
	private TextView lineNameTv;
	/**
	 * The line type image.
	 */
	private ImageView lineTypeImg;
	/**
	 * The line direction text view.
	 */
	private TextView lineDirectionTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_info);

		this.list = (ListView) findViewById(R.id.list);
		this.lineNameTv = (TextView) findViewById(R.id.line_name);
		this.lineTypeImg = (ImageView) findViewById(R.id.subway_img);
		this.lineDirectionTv = (TextView) findViewById(R.id.subway_line_station_string);

		this.list.setEmptyView(findViewById(R.id.list_empty));
		this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				if (SubwayLineInfo.this.stations != null && position < SubwayLineInfo.this.stations.length
				        && SubwayLineInfo.this.stations[position] != null) {
					Intent intent = new Intent(SubwayLineInfo.this, SubwayStationInfo.class);
					String subwayStationId = SubwayLineInfo.this.stations[position].getId();
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
					startActivity(intent);
				}
			}
		});
		// get info from the intent.
		int newLineNumber = Integer.valueOf(Utils.getSavedStringValue(getIntent(), savedInstanceState,
		        SubwayLineInfo.EXTRA_LINE_NUMBER));
		String newOrderPref = Utils.getSavedStringValue(getIntent(), savedInstanceState,
		        SubwayLineInfo.EXTRA_ORDER_PREF);
		showNewSubway(newLineNumber, newOrderPref);
	}

	@Override
	public void showNewSubway(int newLineNumber, String newOrderPref) {
		MyLog.v(TAG, "showNewSubway(%s, %s)", newLineNumber, newOrderPref);
		if ((this.subwayLine == null || this.subwayLine.getNumber() != newLineNumber)
		        || (this.orderPref != null && !this.orderPref.equals(newOrderPref))) {
			MyLog.d(TAG, "new subway line / stations order");
			this.subwayLine = StmManager.findSubwayLine(getContentResolver(), newLineNumber);
			if (newOrderPref == null) {
				newOrderPref = UserPreferences.getPrefDefault(this,
				        UserPreferences.getPrefsSubwayStationsOrder(this.subwayLine.getNumber()),
				        UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
			} else {
				UserPreferences.savePrefDefault(this,
				        UserPreferences.getPrefsSubwayStationsOrder(this.subwayLine.getNumber()), newOrderPref);
			}
			this.orderPref = newOrderPref;
			refreshAll();
		}
	}

	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onStop();
	}

	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
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
		super.onRestart();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		refreshSubwayLineInfo();
		refreshSubwayStationsList();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistancesWithNewLocation();
		}
		// IF location updates are not already enabled DO
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
		this.list.setAdapter(getAdapter());
	}

	/**
	 * Refresh the subway line info.
	 */
	private void refreshSubwayLineInfo() {
		// subway line name
		this.lineNameTv.setText(SubwayUtils.getSubwayLineName(subwayLine.getNumber()));
		this.lineTypeImg.setImageResource(SubwayUtils.getSubwayLineImgId(subwayLine.getNumber()));

		// subway line direction
		String orderId = getSortOrderFromOrderPref(this.subwayLine.getNumber());
		SubwayStation lastStation = StmManager.findSubwayLineLastSubwayStation(getContentResolver(),
		        subwayLine.getNumber(), orderId);
		String separatorText = getString(R.string.stations_and_order, getDirectionText(lastStation));
		this.lineDirectionTv.setText(separatorText);
		this.lineDirectionTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSelectDirectionDialog(v);
			}
		});
	}

	/**
	 * Show the subway line select direction dialog.
	 * @param v the view (not used)
	 */
	public void showSelectDirectionDialog(View v) {
		new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this).showDialog();
	}

	/**
	 * @return the sort order from the order preference
	 */
	private String getSortOrderFromOrderPref(int subwayLineNumber) {
		String prefsSubwayStationsOrder = UserPreferences.getPrefsSubwayStationsOrder(subwayLineNumber);
		String sharedPreferences = UserPreferences.getPrefDefault(this, prefsSubwayStationsOrder,
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
			return getString(R.string.direction_and_string, lastSubwayStation.getName());
		} else {
			// DEFAULT: A-Z order
			this.orderPref = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT;
			return getString(R.string.alphabetical_order);
		}
	}

	/**
	 * Update the distances with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		if (getLocation() != null) {
			float accuracyInMeters = getLocation().getAccuracy();
			for (ASubwayStation station : stations) {
				// distance
				Location stationLocation = LocationUtils.getNewLocation(station.getLat(), station.getLng());
				float distanceInMeters = getLocation().distanceTo(stationLocation);
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
		for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayLineStationsWithOtherLinesList(
		        this.getContentResolver(), this.subwayLine.getNumber())) {
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
			// MyLog.v(TAG, "getView(" + position + ")");
			View view;
			if (convertView == null) {
				view = this.layoutInflater.inflate(R.layout.subway_line_info_stations_list_item, parent, false);
			} else {
				view = convertView;
			}
			ASubwayStation station = this.stations[position];
			if (station != null) {
				// station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				// station lines color
				List<Integer> otherLines = station.getOtherLinesId();
				// 1 - find the station line image
				int subwayLineImgId = SubwayUtils.getSubwayLineImgId(station.getLineId());
				if (!SubwayLineInfo.this.getSortOrderFromOrderPref(station.getLineId()).equals(
				        StmStore.SubwayStation.DEFAULT_SORT_ORDER)) {
					if (position == 0) {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListTopId(station.getLineId());
					} else if (position == this.stations.length - 1) {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListBottomId(station.getLineId());
					} else {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListMiddleId(station.getLineId());
					}
				} else {
					subwayLineImgId = SubwayUtils.getSubwayLineImgListId(station.getLineId());
				}
				// 2 - set the images to the right image view
				// color 1 (on the right, closer to the text)
				if (otherLines.size() == 0) {
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImgId);
				} else {
					int lastIndex = otherLines.size() - 1;
					int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(lastIndex));
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(otherLineImg);
				}
				// color 2 (on the middle)
				if (otherLines.size() < 1) {
					view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.subway_img_2).setVisibility(View.VISIBLE);
					if (otherLines.size() == 1) {
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(0));
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(otherLineImg);
					}
				}
				// color 3 (on the left, closer to the border)
				if (otherLines.size() < 2) {
					view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.subway_img_3).setVisibility(View.VISIBLE);
					if (otherLines.size() == 2) {
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(1));
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(otherLineImg);
					}
				}
				// station distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
				}
			}
			return view;
		}
	}

	// TODO enable list filtering
	// @Override
	// public Cursor runQuery(CharSequence constraint) {
	// MyLog.v(TAG, "runQuery(" + constraint + ")");
	// int lineNumber = this.subwayLine.getNumber();
	// String orderId = getSortOrderFromOrderPref(lineNumber);
	// return StmManager.searchSubwayLineStations(getContentResolver(), lineNumber, orderId, constraint.toString());
	// }

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
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
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
		return MenuUtils.inflateMenu(this, menu, R.menu.subway_line_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.direction:
			showSelectDirectionDialog(null);
			return true;
		default:
			return MenuUtils.handleCommonMenuActions(this, item);
		}
	}
}

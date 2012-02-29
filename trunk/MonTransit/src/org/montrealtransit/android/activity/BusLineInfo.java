package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.BusLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.services.GeocodingTask;
import org.montrealtransit.android.services.GeocodingTaskListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
 * This activity display information about a bus line.
 * @author Mathieu Méa
 */
public class BusLineInfo extends Activity implements BusLineSelectDirectionDialogListener, LocationListener, OnSharedPreferenceChangeListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BusLine";

	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * Only used to display initial bus line name.
	 */
	public static final String EXTRA_LINE_NAME = "extra_line_name";
	/**
	 * The extra ID for the bus line direction ID.
	 */
	public static final String EXTRA_LINE_DIRECTION_ID = "extra_line_direction_id";

	/**
	 * The current bus line.
	 */
	private StmStore.BusLine busLine;
	/**
	 * The bus line direction.
	 */
	private StmStore.BusLineDirection busLineDirection;
	/**
	 * The cursor used to display the bus line stops.
	 */
	private Cursor cursor;

	/**
	 * The line stops list view.
	 */
	private ListView list;
	/**
	 * The line number text view.
	 */
	private TextView lineNumberTv;
	/**
	 * The line name text view.
	 */
	private TextView lineNameTv;
	/**
	 * The line type image.
	 */
	private ImageView lineTypeImg;
	/**
	 * The line stops title text view.
	 */
	private TextView lineStopsTv;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates should be enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * The list of bus stops.
	 */
	private ABusStop[] busStops = new ABusStop[0];
	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<ABusStop> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);

		this.list = (ListView) findViewById(R.id.list);
		this.lineNumberTv = (TextView) findViewById(R.id.line_number);
		this.lineNameTv = (TextView) findViewById(R.id.line_name);
		this.lineTypeImg = (ImageView) findViewById(R.id.bus_type);
		this.lineStopsTv = (TextView) findViewById(R.id.bus_line_stop_text);

		this.list.setEmptyView(findViewById(R.id.list_empty));
		this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				// MyLog.v(TAG, "onItemClick(%s, %s, %s ,%s)", l.getId(), v.getId(), position, id);
				if (BusLineInfo.this.busStops != null && position < BusLineInfo.this.busStops.length && BusLineInfo.this.busStops[position] != null
						&& !TextUtils.isEmpty(BusLineInfo.this.busStops[position].getCode())) {
					Intent intent = new Intent(BusLineInfo.this, BusStopInfo.class);
					String busStopCode = BusLineInfo.this.busStops[position].getCode();
					String busStopPlace = BusLineInfo.this.busStops[position].getPlace();
					intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStopCode);
					intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, busStopPlace);
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, BusLineInfo.this.busLine.getNumber());
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, BusLineInfo.this.busLine.getName());
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, BusLineInfo.this.busLine.getType());
					startActivity(intent);
				}
			}
		});

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NUMBER);
		this.lineNumberTv.setText(lineNumber); // temporary show the line number
		String lineName = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NAME);
		this.lineNameTv.setText(lineName); // temporary show the line name
		String lineDirectionId = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_DIRECTION_ID);
		showNewLine(lineNumber, lineName, lineDirectionId);
	}

	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		if (isShowingBusStopLocation()) {
			LocationUtils.disableLocationUpdates(this, this);
		}
		super.onStop();
	}

	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		// IF location updates should be enabled DO
		if (isShowingBusStopLocation() && this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation(null);
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

	@Override
	public void showNewLine(final String newLineNumber, final String newLineName, final String newDirectionId) {
		MyLog.v(TAG, "showNewLine(%s, %s, %s)", newLineNumber, newLineName, newDirectionId);
		if ((this.busLine == null || this.busLineDirection == null)
				|| (!this.busLine.getNumber().equals(newLineNumber) || !this.busLineDirection.getId().equals(newDirectionId))) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					BusLineInfo.this.busLine = StmManager.findBusLine(BusLineInfo.this.getContentResolver(), newLineNumber);
					BusLineInfo.this.busLineDirection = StmManager.findBusLineDirection(BusLineInfo.this.getContentResolver(), newDirectionId);
					return null;
				}

				protected void onPostExecute(Void result) {
					refreshAll();
				};
			}.execute();
		}
	}

	/**
	 * Refresh ALL the UI.
	 */
	private void refreshAll() {
		refreshBusLineInfo();
		refreshBusStopList();
		if (isShowingBusStopLocation()) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the distance before showing the list
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation(null);
			}
			// IF location updates are not already enabled DO
			if (!this.locationUpdatesEnabled) {
				// enable
				LocationUtils.enableLocationUpdates(this, this);
				this.locationUpdatesEnabled = true;
			}
		}
	}

	/**
	 * Refresh the bus line info UI.
	 */
	private void refreshBusLineInfo() {
		// bus line number
		this.lineNumberTv.setText(this.busLine.getNumber());
		int color = BusUtils.getBusLineTypeBgColorFromType(this.busLine.getType());
		this.lineNumberTv.setBackgroundColor(color);
		// bus line name
		this.lineNameTv.setText(this.busLine.getName());
		this.lineNameTv.requestFocus();
		// bus line type
		this.lineTypeImg.setImageResource(BusUtils.getBusLineTypeImgFromType(this.busLine.getType()));

		// bus line direction
		this.lineStopsTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSelectDirectionDialog(null);
			}
		});
		List<Integer> busLineDirection = BusUtils.getBusLineDirectionStringIdFromId(this.busLineDirection.getId());
		String direction = getString(busLineDirection.get(0));
		if (busLineDirection.size() >= 2) {
			direction += " " + getString(busLineDirection.get(1));
		}
		this.lineStopsTv.setText(getString(R.string.bus_stops_short_and_direction, direction));
	}

	/**
	 * Show the bus line dialog to select direction.
	 */
	public void showSelectDirectionDialog(View v) {
		// TODO use single choice items to show the current direction
		new BusLineSelectDirection(this, this.busLine.getNumber(), this.busLine.getName(), this).showDialog();
	}

	/**
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopList() {
		new AsyncTask<Void, Void, ABusStop[]>() {
			@Override
			protected ABusStop[] doInBackground(Void... params) {
				return getBusStops();
			}

			@Override
			protected void onPostExecute(ABusStop[] result) {
				BusLineInfo.this.busStops = result;
				BusLineInfo.this.adapter = new ArrayAdapterWithCustomView(BusLineInfo.this, R.layout.subway_line_info_stations_list_item,
						BusLineInfo.this.busStops);
				BusLineInfo.this.list.setAdapter(BusLineInfo.this.adapter);
				startFindBusStopLocation();
			};

		}.execute();
	}

	/**
	 * The index of the last bus stop location task started.
	 */
	private int lastStartedBusStopLocationTaskIndex;

	/**
	 * This method asynchronously find all bus stops location.
	 */
	private void startFindBusStopLocation() {
		MyLog.v(TAG, "startFindBusStopLocation()");
		if (isShowingBusStopLocation()) {
			this.lastStartedBusStopLocationTaskIndex = -1;
			if (this.busStops != null && this.busStops.length > 0) {
				for (int i = 0; i < this.busStops.length && i < SupportFactory.getInstance(this).getASyncTaskCapacity(); i++) {
					findNextBusStopLocation();
				}
			}
		} else {
			// remove all bus stop locations
			for (ABusStop busStop : this.busStops) {
				busStop.setLat(null);
				busStop.setLng(null);
				busStop.setDistance(0);
				busStop.setDistanceString(null);
			}
			this.adapter.notifyDataSetChanged();
		}
	}

	/**
	 * @return the bus stop location preference.
	 */
	private boolean isShowingBusStopLocation() {
		return UserPreferences.getPrefDefault(this, UserPreferences.PREFS_BUS_STOP_LOCATION, UserPreferences.PREFS_BUS_STOP_LOCATION_DEFAULT);
	}

	/**
	 * Schedule the next bus stop location task based on the last started bus stop location task.
	 */
	private synchronized void findNextBusStopLocation() {
		MyLog.v(TAG, "findNextBusStopLocation(%s)", this.lastStartedBusStopLocationTaskIndex);
		if (isShowingBusStopLocation()) {
			this.lastStartedBusStopLocationTaskIndex++;
			if (this.lastStartedBusStopLocationTaskIndex < this.busStops.length) {
				final int persitedIndex = this.lastStartedBusStopLocationTaskIndex;
				final String persistedCode = this.busStops[persitedIndex].getCode(); // empty code is rare enough to be use as a check
				final String persistedSubwayStationId = this.busStops[persitedIndex].getSubwayStationId();
				final Double persistedSubwayStationLat = this.busStops[persitedIndex].getSubwayStationLatOrNull();
				final Double persistedSubwayStationLng = this.busStops[persitedIndex].getSubwayStationLngOrNull();
				new GeocodingTask(this, 1, false, new GeocodingTaskListener() {
					@Override
					public void processLocation(List<Address> addresses) {
						MyLog.v(TAG, "processLocation()");
						// 1 - apply result to the bus stop
						if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
							// use the first location result as the bus stop location
							if (BusLineInfo.this.busStops != null && BusLineInfo.this.busStops.length > persitedIndex) {
								// IF still the same bus stop
								if (BusLineInfo.this.busStops[persitedIndex] != null
										&& BusLineInfo.this.busStops[persitedIndex].getCode().equals(persistedCode)) {
									BusLineInfo.this.busStops[persitedIndex].setLat(addresses.get(0).getLatitude());
									BusLineInfo.this.busStops[persitedIndex].setLng(addresses.get(0).getLongitude());
									updateDistancesWithNewLocation(persitedIndex); // force update
								}
							}
						} else if (!TextUtils.isEmpty(persistedSubwayStationId) && persistedSubwayStationLat != null && persistedSubwayStationLng != null) {
							// use the subway station location as a second choice
							BusLineInfo.this.busStops[persitedIndex].setLat(persistedSubwayStationLat);
							BusLineInfo.this.busStops[persitedIndex].setLng(persistedSubwayStationLng);
							updateDistancesWithNewLocation(persitedIndex); // force update
						}
						// 2 - start next bus stop location search
						BusLineInfo.this.findNextBusStopLocation();
					}
				}).execute(this.busStops[persitedIndex].getPlace());
			}
		}
	}

	private ABusStop[] getBusStops() {
		List<BusStop> busStopList = StmManager.findBusLineStopsList(this.getContentResolver(), this.busLine.getNumber(), this.busLineDirection.getId());
		// creating the list of the subways stations object
		ABusStop[] busStops = new ABusStop[busStopList.size()];
		int i = 0;
		for (BusStop busStop : busStopList) {
			ABusStop aBusStop = new ABusStop();
			aBusStop.setCode(busStop.getCode());
			aBusStop.setDirectionId(busStop.getDirectionId());
			String cleanBusStopPlace = BusUtils.cleanBusStopPlace(busStop.getPlace());
			aBusStop.setPlace(cleanBusStopPlace);
			aBusStop.setSubwayStationId(busStop.getSubwayStationId());
			aBusStop.setSubwayStationName(busStop.getSubwayStationNameOrNull());
			aBusStop.setLineNumber(busStop.getLineNumber());
			aBusStop.setLineNumber(busStop.getLineNameOrNull());
			aBusStop.setLineType(busStop.getLineTypeOrNull());
			busStops[i] = aBusStop;
			i++;
		}
		return busStops;
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	private class ArrayAdapterWithCustomView extends ArrayAdapter<ABusStop> {

		/**
		 * The layout inflater.
		 */
		private LayoutInflater layoutInflater;
		/**
		 * The bus stops.
		 */
		private ABusStop[] busStops;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param textViewResourceId the text view resource ID (not used)
		 * @param objects the stations
		 */
		public ArrayAdapterWithCustomView(Context context, int textViewResourceId, ABusStop[] busStops) {
			super(context, textViewResourceId, busStops);
			this.busStops = busStops;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			View view;
			if (convertView == null) {
				view = this.layoutInflater.inflate(R.layout.bus_line_info_stops_list_item, parent, false);
			} else {
				view = convertView;
			}

			ABusStop busStop = this.busStops[position];
			if (busStop != null) {
				// bus stop code
				((TextView) view.findViewById(R.id.stop_code)).setText(busStop.getCode());
				// bus stop place
				((TextView) view.findViewById(R.id.place)).setText(busStop.getPlace());
				// bus stop subway station
				if (!TextUtils.isEmpty(busStop.getSubwayStationNameOrNull())) {
					((TextView) view.findViewById(R.id.station_name)).setText(busStop.getSubwayStationNameOrNull());
				} else {
					((TextView) view.findViewById(R.id.station_name)).setText("");
				}
				view.findViewById(R.id.subway_img).setVisibility(TextUtils.isEmpty(busStop.getSubwayStationId()) ? View.GONE : View.VISIBLE);
				// bus stop distance
				if (isShowingBusStopLocation() && !TextUtils.isEmpty(busStop.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(busStop.getDistanceString());
					view.findViewById(R.id.distance).setVisibility(View.VISIBLE);
				} else {
					((TextView) view.findViewById(R.id.distance)).setText(null);
					view.findViewById(R.id.distance).setVisibility(View.GONE);
				}
			}
			return view;
		}

	}

	/**
	 * Show STM bus line map.
	 * @param v the view (not used)
	 */
	public void showSTMBusLineMap(View v) {
		String url = "http://www.stm.info/bus/images/PLAN/lign-" + this.busLine.getNumber() + ".gif";
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	/**
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (isShowingBusStopLocation() && newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
	}

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <b>NULL</b>
	 */
	private Location getLocation() {
		if (isShowingBusStopLocation() && this.location == null) {
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
	 * Update the distances with the latest device location.
	 * @param busStopIndex the bus stops list index to update or <b>NULL</b> for updating all bus stops
	 */
	private void updateDistancesWithNewLocation(Integer busStopIndex) {
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", busStopIndex);
		if (isShowingBusStopLocation() && getLocation() != null) {
			float accuracyInMeters = getLocation().getAccuracy();
			for (int i = 0; i < this.busStops.length; i++) {
				if (busStopIndex == null || busStopIndex.intValue() == i) {
					ABusStop busStop = this.busStops[i];
					// IF the bus stop location is known DO
					if (busStop.getLat() != null && busStop.getLng() != null) {
						Location busStopLocation = LocationUtils.getNewLocation(busStop.getLat(), busStop.getLng());
						busStop.setDistance(getLocation().distanceTo(busStopLocation));
						String distanceString = Utils.getDistanceString(this, busStop.getDistance(), accuracyInMeters);
						busStop.setDistanceString(distanceString);
					}
				}
			}
			if (this.adapter != null) {
				this.adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		if (isShowingBusStopLocation()) {
			this.setLocation(location);
			updateDistancesWithNewLocation(null);
		}
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
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_line_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map:
			showSTMBusLineMap(null);
			return true;
		case R.id.direction:
			showSelectDirectionDialog(null);
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		MyLog.v(TAG, "onSharedPreferenceChanged(%s)", key);
		if (key.equals(UserPreferences.PREFS_BUS_STOP_LOCATION)) {
			startFindBusStopLocation();
		}
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
}

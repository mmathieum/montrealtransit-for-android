package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.BusLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
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
 * This activity display information about a bus line.
 * @author Mathieu Méa
 */
public class BusLineInfo extends Activity implements BusLineSelectDirectionDialogListener, LocationListener/* , OnSharedPreferenceChangeListener */{

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
	 * Only used to display initial bus line type color.
	 */
	public static final String EXTRA_LINE_TYPE = "extra_line_type";
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
	/**
	 * The bus line stops codes ordered by distance (closest first).
	 */
	private List<String> orderedStopCodes;
	/**
	 * The favorite bus stops codes.
	 */
	private List<String> favStopCodes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);

		setupList((ListView) findViewById(R.id.list));
		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NUMBER);
		String lineType = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_TYPE);
		String lineName = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_NAME);
		String lineDirectionId = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_DIRECTION_ID);
		// temporary show the line name & number
		setLineNumberAndName(lineNumber, lineType, lineName);
		// show bus line
		showNewLine(lineNumber, lineDirectionId);
	}

	/**
	 * Setup the bus stops list.
	 * @param list the bus stops list
	 */
	public void setupList(ListView list) {
		list.setEmptyView(findViewById(R.id.list_empty));
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
		// refresh favorites
		refreshFavoriteStopCodesFromDB();
		super.onResume();
	}

	@Override
	public void showNewLine(final String newLineNumber, final String newDirectionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", newLineNumber, newDirectionId);
		if ((this.busLine == null || this.busLineDirection == null)
				|| (!this.busLine.getNumber().equals(newLineNumber) || !this.busLineDirection.getId().equals(newDirectionId))) {
			// show loading layout
			((ListView) findViewById(R.id.list)).setAdapter(null);
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
		refreshBusStopListFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(BusLineInfo.this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(BusLineInfo.this));
			updateDistancesWithNewLocation(null);
		}
		// IF location updates are not already enabled DO
		if (!BusLineInfo.this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(BusLineInfo.this, BusLineInfo.this);
			BusLineInfo.this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Refresh the bus line info UI.
	 */
	private void refreshBusLineInfo() {
		// bus line number & name
		setLineNumberAndName(this.busLine.getNumber(), this.busLine.getType(), this.busLine.getName());
		// bus line type
		((ImageView) findViewById(R.id.bus_type)).setImageResource(BusUtils.getBusLineTypeImgFromType(this.busLine.getType()));
		// bus line direction
		setupBusLineDirection((TextView) findViewById(R.id.bus_line_stop_text));
	}

	/**
	 * Setup bus line direction.
	 * @param lineStopsTv the bus line direction {@link TextView}
	 */
	private void setupBusLineDirection(TextView lineStopsTv) {
		lineStopsTv.setOnClickListener(new View.OnClickListener() {
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
		lineStopsTv.setText(getString(R.string.bus_stops_short_and_direction, direction));
	}

	/**
	 * Refresh the bus line number UI.
	 */
	public void setLineNumberAndName(String lineNumber, String lineType, String lineName) {
		((TextView) findViewById(R.id.line_number)).setText(lineNumber);
		findViewById(R.id.line_number).setBackgroundColor(BusUtils.getBusLineTypeBgColorFromType(lineType));
		((TextView) findViewById(R.id.line_name)).setText(lineName);
		findViewById(R.id.line_name).requestFocus();
	}

	/**
	 * Show the bus line dialog to select direction.
	 */
	public void showSelectDirectionDialog(View v) {
		// TODO use single choice items to show the current direction
		new BusLineSelectDirection(this, this.busLine.getNumber(), this.busLine.getName(), this.busLine.getType(), this.busLineDirection.getId(), this)
				.showDialog();
	}

	/**
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopListFromDB() {
		new AsyncTask<Void, Void, ABusStop[]>() {
			@Override
			protected ABusStop[] doInBackground(Void... params) {
				List<BusStop> busStopList = StmManager.findBusLineStopsList(BusLineInfo.this.getContentResolver(), BusLineInfo.this.busLine.getNumber(),
						BusLineInfo.this.busLineDirection.getId());
				// creating the list of the subways stations object
				ABusStop[] busStops = new ABusStop[busStopList.size()];
				int i = 0;
				for (BusStop busStop : busStopList) {
					ABusStop aBusStop = new ABusStop();
					aBusStop.setCode(busStop.getCode());
					aBusStop.setDirectionId(busStop.getDirectionId());
					aBusStop.setPlace(BusUtils.cleanBusStopPlace(busStop.getPlace()));
					aBusStop.setSubwayStationId(busStop.getSubwayStationId());
					aBusStop.setSubwayStationName(busStop.getSubwayStationNameOrNull());
					aBusStop.setLineNumber(busStop.getLineNumber());
					aBusStop.setLineNumber(busStop.getLineNameOrNull());
					aBusStop.setLineType(busStop.getLineTypeOrNull());
					aBusStop.setLat(busStop.getLat());
					aBusStop.setLng(busStop.getLng());
					busStops[i] = aBusStop;
					i++;
				}
				return busStops;
			}

			@Override
			protected void onPostExecute(ABusStop[] result) {
				BusLineInfo.this.busStops = result;
				refreshFavoriteStopCodesFromDB();
				BusLineInfo.this.adapter = new ArrayAdapterWithCustomView(BusLineInfo.this, R.layout.bus_line_info_stops_list_item, BusLineInfo.this.busStops);
				((ListView) findViewById(R.id.list)).setAdapter(BusLineInfo.this.adapter);
				updateDistancesWithNewLocation(null); // force update all bus stop with location
			};

		}.execute();
	}

	/**
	 * Find favorites bus stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB() {
		BusLineInfo.this.favStopCodes = new ArrayList<String>(); // clear list
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				// TODO filter by fkid2 (bus line number)
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				for (Fav busStopFav : result) {
					if (BusLineInfo.this.busLine != null && BusLineInfo.this.busLine.getNumber().equals(busStopFav.getFkId2())) { // compare line number
						BusLineInfo.this.favStopCodes.add(busStopFav.getFkId()); // store stop code
					}
				}
				// trigger change
				if (BusLineInfo.this.adapter != null) {
					BusLineInfo.this.adapter.notifyDataSetChanged();
				}
			};
		}.execute();
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
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 * @param objects the stations
		 */
		public ArrayAdapterWithCustomView(Context context, int viewId, ABusStop[] busStops) {
			super(context, viewId, busStops);
			this.viewId = viewId;
			this.busStops = busStops;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			View view;
			if (convertView == null) {
				view = this.layoutInflater.inflate(this.viewId, parent, false);
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
				if (!TextUtils.isEmpty(busStop.getSubwayStationId()) && !TextUtils.isEmpty(busStop.getSubwayStationNameOrNull())) {
					view.findViewById(R.id.subway_img).setVisibility(View.VISIBLE);
					view.findViewById(R.id.station_name).setVisibility(View.VISIBLE);
					((TextView) view.findViewById(R.id.station_name)).setText(busStop.getSubwayStationNameOrNull());
				} else {
					view.findViewById(R.id.station_name).setVisibility(View.GONE);
					view.findViewById(R.id.subway_img).setVisibility(View.GONE);
					((TextView) view.findViewById(R.id.station_name)).setText(null);
				}
				// bus stop distance
				if (!TextUtils.isEmpty(busStop.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(busStop.getDistanceString());
					view.findViewById(R.id.distance).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.distance).setVisibility(View.GONE);
					((TextView) view.findViewById(R.id.distance)).setText(null);
				}
				// favorite
				if (BusLineInfo.this.favStopCodes != null && BusLineInfo.this.favStopCodes.contains(busStop.getCode())) {
					view.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.fav_img).setVisibility(View.GONE);
				}
				// set style for closest bus stop
				int index = -1;
				if (BusLineInfo.this.orderedStopCodes != null) {
					index = BusLineInfo.this.orderedStopCodes.indexOf(busStop.getCode());
				}
				switch (index) {
				case 0:
					((TextView) view.findViewById(R.id.place)).setTypeface(Typeface.DEFAULT_BOLD);
					((TextView) view.findViewById(R.id.distance)).setTypeface(Typeface.DEFAULT_BOLD);
					((TextView) view.findViewById(R.id.distance)).setTextColor(Utils.getTextColorPrimary(getContext()));
					break;
				default:
					((TextView) view.findViewById(R.id.place)).setTypeface(Typeface.DEFAULT);
					((TextView) view.findViewById(R.id.distance)).setTypeface(Typeface.DEFAULT);
					((TextView) view.findViewById(R.id.distance)).setTextColor(Utils.getTextColorSecondary(getContext()));
					break;
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
		if (newLocation != null) {
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
	 * Update the distances with the latest device location.
	 * @param busStopIndex the bus stops list index to update or <b>NULL</b> for updating all bus stops
	 */
	private void updateDistancesWithNewLocation(Integer busStopIndex) {
		MyLog.v(TAG, "updateDistancesWithNewLocation(%s)", busStopIndex);
		if (getLocation() != null) {
			float accuracyInMeters = getLocation().getAccuracy();
			for (int i = 0; i < this.busStops.length; i++) {
				if (busStopIndex == null || busStopIndex.intValue() == i) {
					ABusStop busStop = this.busStops[i];
					// IF the bus stop location is known DO
					if (busStop.getLat() != null && busStop.getLng() != null) {
						busStop.setDistance(getLocation().distanceTo(LocationUtils.getNewLocation(busStop.getLat(), busStop.getLng())));
						busStop.setDistanceString(Utils.getDistanceString(this, busStop.getDistance(), accuracyInMeters));
					}
				}
			}

			List<ABusStop> orderedStops = new ArrayList<ABusStop>(Arrays.asList(this.busStops));
			// order the stations list by distance (closest first)
			Collections.sort(orderedStops, new Comparator<ABusStop>() {
				@Override
				public int compare(ABusStop lhs, ABusStop rhs) {
					float d1 = lhs.getDistance();
					float d2 = rhs.getDistance();

					if (d1 > d2) {
						return +1;
					} else if (d1 < d2) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			this.orderedStopCodes = new ArrayList<String>();
			for (BusStop orderedStop : orderedStops) {
				this.orderedStopCodes.add(orderedStop.getCode());
			}
			if (this.adapter != null) {
				this.adapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistancesWithNewLocation(null);
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
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		super.onDestroy();
	}
}

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
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.Pair;
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
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity display information about a bus line.
 * @author Mathieu Méa
 */
public class BusLineInfo extends Activity implements BusLineSelectDirectionDialogListener, LocationListener, SensorEventListener, ShakeListener,
		CompassListener, OnScrollListener {

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
	private List<ABusStop> busStops = new ArrayList<ABusStop>(); // new ABusStop[0];
	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<ABusStop> adapter;
	/**
	 * The closest bus line stop code by distance.
	 */
	private String closestStopCode;
	/**
	 * The favorite bus stops codes.
	 */
	private List<String> favStopCodes;
	/**
	 * The acceleration apart from gravity.
	 */
	private float lastSensorAcceleration = 0.00f;
	/**
	 * The last acceleration including gravity.
	 */
	private float lastSensorAccelerationIncGravity = SensorManager.GRAVITY_EARTH;
	/**
	 * The last sensor update time-stamp.
	 */
	private long lastSensorUpdate = -1;
	/**
	 * True if the share was already handled (should be reset in {@link #onResume()}).
	 */
	private boolean shakeHandled = false;
	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass (in degree).
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The scroll state of the list.
	 */
	private int scrollState = SCROLL_STATE_IDLE;

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
				if (BusLineInfo.this.busStops != null && position < BusLineInfo.this.busStops.size() && BusLineInfo.this.busStops.get(position) != null
						&& !TextUtils.isEmpty(BusLineInfo.this.busStops.get(position).getCode())) {
					Intent intent = new Intent(BusLineInfo.this, BusStopInfo.class);
					String busStopCode = BusLineInfo.this.busStops.get(position).getCode();
					String busStopPlace = BusLineInfo.this.busStops.get(position).getPlace();
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

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;

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
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// refresh favorites
		refreshFavoriteStopCodesFromDB();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		SensorUtils.unregisterSensorListener(this, this);
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(se, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
		// SensorUtils.checkForCompass(event, this.accelerometerValues, this.magneticFieldValues, this);
		checkForCompass(se, this);
	}

	/**
	 * @see SensorUtils#checkForCompass(SensorEvent, float[], float[], CompassListener)
	 */
	public void checkForCompass(SensorEvent event, CompassListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerValues = event.values;
			if (magneticFieldValues != null) {
				listener.onCompass();
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magneticFieldValues = event.values;
			if (accelerometerValues != null) {
				listener.onCompass();
			}
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onShake() {
		MyLog.v(TAG, "onShake()");
		showClosestStop();
	}

	@Override
	public void onCompass() {
		// MyLog.v(TAG, "onCompass()");
		if (this.accelerometerValues != null && this.magneticFieldValues != null) {
			updateCompass(SensorUtils.calculateOrientation(this, this.accelerometerValues, this.magneticFieldValues));
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	private void updateCompass(float[] orientation) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update closest bike stations compass
				if (this.busStops != null) {
					for (ABusStop busStop : this.busStops) {
						busStop.getCompassMatrix().reset();
						busStop.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this, currentLocation, busStop.getLocation(), orientation, getLocationDeclination()),
								getArrowDim().first / 2, getArrowDim().second / 2);
					}
					// update the view
					notifyDataSetChanged();
				}
			}
		}
	}

	/**
	 * The minimum between 2 {@link ArrayAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	private static final int ADAPTER_NOTIFY_THRESOLD = 150; // 0.15 seconds

	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;

	/**
	 * {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged() {
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE && (now - this.lastNotifyDataSetChanged) > ADAPTER_NOTIFY_THRESOLD) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	private Pair<Integer, Integer> arrowDim;

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (view == findViewById(R.id.list)) {
			this.scrollState = scrollState;// == OnScrollListener.SCROLL_STATE_FLING);
		}
	}

	/**
	 * Show the closest subway line station (if possible).
	 */
	private void showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.hasFocus && !this.shakeHandled && !TextUtils.isEmpty(this.closestStopCode)) {
			Toast.makeText(this, R.string.shake_closest_bus_line_stop_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, BusStopInfo.class);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, this.closestStopCode);
			intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, findStopPlace(this.closestStopCode));
			if (this.busLine != null) {
				intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, this.busLine.getNumber());
				intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, this.busLine.getName());
				intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, this.busLine.getType());
			}
			startActivity(intent);
			this.shakeHandled = true;
		}
	}

	/**
	 * @param stopCode a bus stop code
	 * @return a bus stop place or null
	 */
	private String findStopPlace(String stopCode) {
		if (this.busStops == null) {
			return null;
		}
		for (BusStop busStop : this.busStops) {
			if (busStop.getCode().equals(stopCode)) {
				return busStop.getPlace();
			}
		}
		return null;
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
			updateDistancesWithNewLocation();
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
		// MyLog.v(TAG, "setLineNumberAndName(%s, %s, %s)", lineNumber, lineType, lineName);
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
				BusLineInfo.this.busStops = Arrays.asList(result);
				refreshFavoriteStopCodesFromDB();
				BusLineInfo.this.adapter = new ArrayAdapterWithCustomView(BusLineInfo.this, R.layout.bus_line_info_stops_list_item);
				((ListView) findViewById(R.id.list)).setAdapter(BusLineInfo.this.adapter);
				((ListView) findViewById(R.id.list)).setOnScrollListener(BusLineInfo.this);
				updateDistancesWithNewLocation(); // force update all bus stop with location
			};

		}.execute();
	}

	/**
	 * Find favorites bus stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB() {
		this.favStopCodes = new ArrayList<String>(); // clear list
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
				notifyDataSetChanged();
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
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 */
		public ArrayAdapterWithCustomView(Context context, int viewId) {
			super(context, viewId);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return BusLineInfo.this.busStops == null ? 0 : BusLineInfo.this.busStops.size();
		}

		@Override
		public int getPosition(ABusStop item) {
			return BusLineInfo.this.busStops.indexOf(item);
		}

		@Override
		public ABusStop getItem(int position) {
			return BusLineInfo.this.busStops.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
			}
			ABusStop busStop = getItem(position);
			if (busStop != null) {
				// bus stop code
				((TextView) convertView.findViewById(R.id.stop_code)).setText(busStop.getCode());
				// bus stop place
				TextView placeTv = (TextView) convertView.findViewById(R.id.place);
				placeTv.setText(busStop.getPlace());
				// bus stop subway station
				TextView stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				if (!TextUtils.isEmpty(busStop.getSubwayStationId()) && !TextUtils.isEmpty(busStop.getSubwayStationNameOrNull())) {
					convertView.findViewById(R.id.subway_img).setVisibility(View.VISIBLE);
					stationNameTv.setText(busStop.getSubwayStationNameOrNull());
					stationNameTv.setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.subway_img).setVisibility(View.GONE);
					stationNameTv.setVisibility(View.GONE);
				}
				// favorite
				if (BusLineInfo.this.favStopCodes != null && BusLineInfo.this.favStopCodes.contains(busStop.getCode())) {
					convertView.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.fav_img).setVisibility(View.GONE);
				}
				// bus stop distance
				TextView distanceTv = (TextView) convertView.findViewById(R.id.distance);
				if (!TextUtils.isEmpty(busStop.getDistanceString())) {
					distanceTv.setText(busStop.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				} else {
					distanceTv.setVisibility(View.GONE);
				}
				// bus stop compass
				ImageView compassTv = (ImageView) convertView.findViewById(R.id.compass);
				if (busStop.getCompassMatrixOrNull() != null) {
					compassTv.setImageMatrix(busStop.getCompassMatrix());
					compassTv.setVisibility(View.VISIBLE);
				} else {
					compassTv.setVisibility(View.GONE);
				}
				// set style for closest bus stop
				int index = -1;
				if (!TextUtils.isEmpty(BusLineInfo.this.closestStopCode)) {
					index = busStop.getCode().equals(BusLineInfo.this.closestStopCode) ? 0 : 999;
				}
				switch (index) {
				case 0:
					placeTv.setTypeface(Typeface.DEFAULT_BOLD);
					distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					break;
				default:
					placeTv.setTypeface(Typeface.DEFAULT);
					distanceTv.setTypeface(Typeface.DEFAULT);
					distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					break;
				}
			}
			return convertView;
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
				SensorUtils.registerShakeAndCompassListener(this, this);
				this.shakeHandled = false;
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
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			float accuracyInMeters = currentLocation.getAccuracy();
			boolean isDetailed = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
					UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			for (ABusStop busStop : this.busStops) {
				// IF the bus stop location is known DO
				busStop.setDistance(currentLocation.distanceTo(busStop.getLocation()));
				busStop.setDistanceString(Utils.getDistanceString(busStop.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			generateOrderedStopCodes();
			notifyDataSetChanged();
		}
	}

	/**
	 * Generate the ordered bus line stops codes.
	 */
	public void generateOrderedStopCodes() {
		if (this.busStops.size() == 0) {
			return;
		}
		List<ABusStop> orderedStops = new ArrayList<ABusStop>(this.busStops);
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
		this.closestStopCode = orderedStops.get(0).getCode();
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

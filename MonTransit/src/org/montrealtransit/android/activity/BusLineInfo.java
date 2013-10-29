package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.BusLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
@TargetApi(3)
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
	 * The extra ID for the bus line number (required).
	 */
	public static final String EXTRA_ROUTE_SHORT_NAME = "extra_line_number";
	/**
	 * Only used to display initial bus line name.
	 */
	public static final String EXTRA_ROUTE_LONG_NAME = "extra_line_name";

	public static final String EXTRA_ROUTE_COLOR = "extra_line_color";
	public static final String EXTRA_ROUTE_TEXT_COLOR = "extra_line_text_color";
	/**
	 * The extra ID for the bus line direction ID (optional).
	 */
	public static final String EXTRA_LINE_TRIP_ID = "extra_line_direction_id"; // TODO switch to integer
	/**
	 * The extra ID for the bus stop code (optional)
	 */
	public static final String EXTRA_LINE_STOP_CODE = "extra_line_stop_code";

	/**
	 * The current bus line.
	 */
	private Route busLine;
	/**
	 * The bus line direction.
	 */
	private Trip busLineDirection;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass updates enabled?
	 */
	private boolean compassUpdatesEnabled = false;
	/**
	 * The list of bus stops.
	 */
	private List<TripStop> busStops = new ArrayList<TripStop>();
	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<TripStop> adapter;
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
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];
	/**
	 * The last compass (in degree).
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;
	/**
	 * The scroll state of the list.
	 */
	private int scrollState = SCROLL_STATE_IDLE;

	public static Intent newInstance(Context context, Route route, String tripId, String stopCode) {
		Intent intent = new Intent(context, SupportFactory.get().getBusLineInfoClass());
		if (route != null) {
			intent.putExtra(BusLineInfo.EXTRA_ROUTE_SHORT_NAME, route.shortName);
			intent.putExtra(BusLineInfo.EXTRA_ROUTE_LONG_NAME, route.longName);
			intent.putExtra(BusLineInfo.EXTRA_ROUTE_COLOR, route.color);
			intent.putExtra(BusLineInfo.EXTRA_ROUTE_TEXT_COLOR, route.textColor);
		}
		intent.putExtra(BusLineInfo.EXTRA_LINE_TRIP_ID, tripId);
		intent.putExtra(BusLineInfo.EXTRA_LINE_STOP_CODE, stopCode);
		return intent;
	}

	public static Intent newInstance(Context context, String routeShortName, String routeLongName, String routeColor, String routeTextColor, String tripId) {
		Intent intent = new Intent(context, SupportFactory.get().getBusLineInfoClass());
		intent.putExtra(BusLineInfo.EXTRA_ROUTE_SHORT_NAME, routeShortName);
		intent.putExtra(BusLineInfo.EXTRA_ROUTE_LONG_NAME, routeLongName);
		intent.putExtra(BusLineInfo.EXTRA_ROUTE_COLOR, routeColor);
		intent.putExtra(BusLineInfo.EXTRA_ROUTE_TEXT_COLOR, routeTextColor);
		intent.putExtra(BusLineInfo.EXTRA_LINE_TRIP_ID, tripId);
		return intent;
	}

	public static Intent newInstance(Context context, String routeShortName) {
		Intent intent = new Intent(context, SupportFactory.get().getBusLineInfoClass());
		intent.putExtra(BusLineInfo.EXTRA_ROUTE_SHORT_NAME, routeShortName);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);

		setupList((ListView) findViewById(R.id.list));
		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_ROUTE_SHORT_NAME);
		String lineName = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_ROUTE_LONG_NAME);
		String lineColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_COLOR);
		String lineTextColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
		String lineDirectionId = Utils.getSavedStringValue(getIntent(), savedInstanceState, BusLineInfo.EXTRA_LINE_TRIP_ID);
		// temporary show the line name & number
		setLineNumberAndName(lineNumber, lineName, lineColor, lineTextColor);
		// show bus line
		showNewLine(lineNumber, lineDirectionId);
	}

	/**
	 * Setup the bus stops list.
	 * @param list the bus stops list
	 */
	public void setupList(ListView list) {
		list.setEmptyView(findViewById(R.id.list_empty));
		list.setOnScrollListener(this);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				// MyLog.v(TAG, "onItemClick(%s, %s, %s ,%s)", l.getId(), v.getId(), position, id);
				if (BusLineInfo.this.busStops != null && position < BusLineInfo.this.busStops.size() && BusLineInfo.this.busStops.get(position) != null
						&& !TextUtils.isEmpty(BusLineInfo.this.busStops.get(position).stop.code)) {
					// IF last bus stop, show descent only
					if (position + 1 == BusLineInfo.this.busStops.size()) {
						Toast toast = Toast.makeText(BusLineInfo.this, R.string.descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						// return; why not?
					}
					Intent intent = BusStopInfo.newInstance(BusLineInfo.this, BusLineInfo.this.busStops.get(position).stop, BusLineInfo.this.busLine);
					startActivity(intent);
				}
			}
		});
		this.adapter = new ArrayAdapterWithCustomView(BusLineInfo.this, R.layout.bus_line_info_stops_list_item);
		list.setAdapter(this.adapter);
	}

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;

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
		this.paused = false;
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
		if (!this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation();
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// refresh favorites
		refreshFavoriteStopCodesFromDB();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		SensorUtils.unregisterSensorListener(this, this);
		this.compassUpdatesEnabled = false;
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(se, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
		SensorUtils.checkForCompass(this, se, this.accelerometerValues, this.magneticFieldValues, this);
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

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		if (this.busStops == null) {
			// MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							BusLineInfo.this.lastCompassInDegree = (int) orientation;
							BusLineInfo.this.lastCompassChanged = now;
							// update the view
							notifyDataSetChanged(false);
						}

					}
				});
	}

	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;

	private float locationDeclination;

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > Utils.ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
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
	 * Show the closest bus line station (if possible).
	 */
	private void showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.hasFocus && !this.shakeHandled && !TextUtils.isEmpty(this.closestStopCode)) {
			Toast.makeText(this, R.string.shake_closest_bus_line_stop_selected, Toast.LENGTH_SHORT).show();
			startActivity(BusStopInfo.newInstance(this, findStop(this.closestStopCode).stop, this.busLine));
			this.shakeHandled = true;
		}
	}

	/**
	 * @param stopCode a bus stop code
	 * @return a bus stop place or null
	 */
	private TripStop findStop(String stopCode) {
		if (this.busStops == null) {
			return null;
		}
		for (TripStop busStop : this.busStops) {
			if (busStop.stop.code.equals(stopCode)) {
				return busStop;
			}
		}
		return null;
	}

	@Override
	public void showNewLine(final String newLineNumber, final String newDirectionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", newLineNumber, newDirectionId);
		if ((this.busLine == null || this.busLineDirection == null)
				|| (!this.busLine.shortName.equals(newLineNumber) || !String.valueOf(this.busLineDirection.id).equals(newDirectionId))) {
			// show loading layout
			this.busStops = null;
			notifyDataSetChanged(true);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					BusLineInfo.this.busLine = StmBusManager.findRoute(BusLineInfo.this, newLineNumber);
					if (newDirectionId == null) { // use the 1st one
						BusLineInfo.this.busLineDirection = StmBusManager.findTripsWithRouteIdList(BusLineInfo.this, newLineNumber).get(0);
					} else {
						BusLineInfo.this.busLineDirection = StmBusManager.findTrip(BusLineInfo.this, String.valueOf(newDirectionId));
					}
					return null;
				}

				@Override
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
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh the bus line info UI.
	 */
	private void refreshBusLineInfo() {
		// bus line number & name
		setLineNumberAndName(this.busLine.shortName, this.busLine.longName, this.busLine.color, this.busLine.textColor);
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
		lineStopsTv.setText(getString(R.string.bus_stops_short_and_direction, this.busLineDirection.getHeading(this)));
	}

	/**
	 * Refresh the bus line number UI.
	 */
	public void setLineNumberAndName(String shortName, String longName, String color, String textColor) {
		// MyLog.v(TAG, "setLineNumberAndName(%s, %s, %s, %s)", shortName, longName, color, textColor);
		final TextView routeShortNameTv = (TextView) findViewById(R.id.line_number);
		routeShortNameTv.setText(shortName);
		if (!TextUtils.isEmpty(textColor)) {
			routeShortNameTv.setTextColor(Utils.parseColor(textColor));
		}
		if (!TextUtils.isEmpty(color)) {
			routeShortNameTv.setBackgroundColor(Utils.parseColor(color));
		}
		((TextView) findViewById(R.id.line_name)).setText(longName);
		findViewById(R.id.line_name).requestFocus();
	}

	/**
	 * Show the bus line dialog to select direction.
	 */
	public void showSelectDirectionDialog(View v) {
		// TODO use single choice items to show the current direction
		new BusLineSelectDirection(this, this.busLine, String.valueOf(this.busLineDirection.id), this).showDialog();
	}

	/**
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopListFromDB() {
		new AsyncTask<Void, Void, List<TripStop>>() {
			@Override
			protected List<TripStop> doInBackground(Void... params) {
				return StmBusManager.findStopsWithTripIdList(BusLineInfo.this, String.valueOf(BusLineInfo.this.busLineDirection.id));
			}

			@Override
			protected void onPostExecute(List<TripStop> result) {
				BusLineInfo.this.busStops = result;
				generateOrderedStopCodes();
				refreshFavoriteStopCodesFromDB();
				notifyDataSetChanged(true);
				updateDistancesWithNewLocation(); // force update all bus stop with location
			};

		}.execute();
	}

	/**
	 * Find favorites bus stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				// TODO filter by fkid2 (bus line number)
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false;
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BusLineInfo.this.favStopCodes)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavStopCodes = new ArrayList<String>();
				for (Fav busStopFav : result) {
					if (BusLineInfo.this.busLine != null && BusLineInfo.this.busLine.shortName.equals(busStopFav.getFkId2())) { // check line number
						if (BusLineInfo.this.favStopCodes == null || !BusLineInfo.this.favStopCodes.contains(busStopFav.getFkId())) {
							newFav = true; // new favorite
						}
						newfavStopCodes.add(busStopFav.getFkId()); // store stop code
					}
				}
				BusLineInfo.this.favStopCodes = newfavStopCodes;
				// trigger change
				if (newFav) {
					notifyDataSetChanged(true);
				}
			};
		}.execute();
	}

	@TargetApi(3)
	static class ViewHolder {
		TextView stopCodeTv;
		TextView placeTv;
		TextView stationNameTv;
		TextView distanceTv;
		ImageView subwayImg;
		ImageView favImg;
		ImageView compassImg;
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	@TargetApi(3)
	private class ArrayAdapterWithCustomView extends ArrayAdapter<TripStop> {

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
		public int getPosition(TripStop item) {
			return BusLineInfo.this.busStops.indexOf(item);
		}

		@Override
		public TripStop getItem(int position) {
			return BusLineInfo.this.busStops.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
				holder.placeTv = (TextView) convertView.findViewById(R.id.place);
				holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				holder.subwayImg = (ImageView) convertView.findViewById(R.id.subway_img);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			TripStop busStop = getItem(position);
			if (busStop != null) {
				// bus stop code
				holder.stopCodeTv.setText(busStop.stop.code);
				// bus stop place
				holder.placeTv.setText(busStop.stop.name);
				// bus stop subway station
				holder.subwayImg.setVisibility(View.GONE);
				holder.stationNameTv.setVisibility(View.GONE);
				// favorite
				if (BusLineInfo.this.favStopCodes != null && BusLineInfo.this.favStopCodes.contains(busStop.stop.code)) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// bus stop distance
				if (!TextUtils.isEmpty(busStop.getDistanceString())) {
					holder.distanceTv.setText(busStop.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE);
				}
				// set style for closest bus stop
				int index = -1;
				if (!TextUtils.isEmpty(BusLineInfo.this.closestStopCode)) {
					index = busStop.stop.code.equals(BusLineInfo.this.closestStopCode) ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.placeTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
					break;
				default:
					holder.placeTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow);
					break;
				}
				// bus stop compass
				if (location != null && lastCompassInDegree != 0 && location.getAccuracy() <= busStop.getDistance()) {
					float compassRotation = SensorUtils.getCompassRotationInDegree(location, busStop, lastCompassInDegree, locationDeclination);
					SupportFactory.get().rotateImageView(holder.compassImg, compassRotation, BusLineInfo.this);
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE);
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
		String url = "http://www.stm.info/bus/images/PLAN/lign-" + this.busLine.shortName + ".gif";
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	/**
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.compassUpdatesEnabled = true;
					this.shakeHandled = false;
				}
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
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
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
			LocationUtils.updateDistance(this, this.busStops, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					String previousClosest = BusLineInfo.this.closestStopCode;
					generateOrderedStopCodes();
					notifyDataSetChanged(BusLineInfo.this.closestStopCode == null ? false : BusLineInfo.this.closestStopCode.equals(previousClosest));
				}
			});
		}
	}

	/**
	 * Generate the ordered bus line stops codes.
	 */
	public void generateOrderedStopCodes() {
		if (this.busStops == null || this.busStops.size() == 0) {
			return;
		}
		List<TripStop> orderedStops = new ArrayList<TripStop>(this.busStops);
		Collections.sort(orderedStops, new POI.POIDistanceComparator());
		this.closestStopCode = orderedStops.get(0).getDistance() > 0 ? orderedStops.get(0).stop.code : null;
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
}

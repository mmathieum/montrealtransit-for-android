package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.services.ClosestBusStopsFinderTask;
import org.montrealtransit.android.services.ClosestBusStopsFinderTask.ClosestBusStopsFinderListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Display the buses.
 * @author Mathieu Méa
 */
public class BusTab extends Activity implements LocationListener, ClosestBusStopsFinderListener, SensorEventListener, CompassListener, OnScrollListener,
		OnItemClickListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Buses";

	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * The location used to generate the closest stops.
	 */
	private Location closestStopsLocation;
	/**
	 * The location address used to generate the closest stops.
	 */
	protected Address closestStopsLocationAddress;
	/**
	 * The closest stops.
	 */
	private List<ABusStop> closestStops;

	/**
	 * The task used to find the closest stations.
	 */
	private ClosestBusStopsFinderTask closestStopsTask;

	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The favorites bus stops UIDs.
	 */
	private List<String> favUIDs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bus_tab);
		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		showAll();
	}

	/**
	 * onCreate() method only for Android version older than Android 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		findViewById(R.id.closest_bus_stops_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshClosestStops(v);
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
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					return LocationUtils.getBestLastKnownLocation(BusTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// IF there is a valid last know location DO
					if (result != null) {
						// set the new distance
						setLocation(result);
						updateDistancesWithNewLocation();
					}
					// re-enable
					LocationUtils.enableLocationUpdates(BusTab.this, BusTab.this);
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		refreshFavoriteUIDsFromDB();
		adaptToScreenSize(getResources().getConfiguration());
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
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
				if (this.closestStops != null) {
					for (ABusStop stop : this.closestStops) {
						stop.getCompassMatrix().reset();
						stop.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this, currentLocation, stop.getLocation(), orientation, getLocationDeclination()),
								getArrowDim().first / 2, getArrowDim().second / 2);
					}
					// update the view
					notifyDataSetChanged();
				}
			}
		}
	}

	private Pair<Integer, Integer> arrowDim;
	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		SensorUtils.unregisterSensorListener(this, this);
		super.onPause();
	}

	public void showAll() {
		MyLog.v(TAG, "showAll()");
		refreshBusLinesFromDB();
		showClosestStops();
	}

	/**
	 * Show the closest stops UI.
	 */
	public void showClosestStops() {
		MyLog.v(TAG, "showClosestStops()");
		// enable location updates
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
		// IF there is no closest stops DO
		if (this.closestStops == null) {
			// look for the closest stops
			refreshClosestStops();
		} else {
			// show the closest stops
			showNewClosestStops();
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestStopsLocation)) {
				// start refreshing
				refreshClosestStops();
			}
		}
	}

	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<ABusStop> adapter;

	/**
	 * Show the new closest stops.
	 */
	private void showNewClosestStops() {
		MyLog.v(TAG, "showNewClosestStops()");
		if (this.closestStops != null) {
			// set the closest stop title
			showNewClosestStopsTitle();
			// hide loading
			if (findViewById(R.id.closest_stops_loading) != null) { // IF inflated/present DO
				findViewById(R.id.closest_stops_loading).setVisibility(View.GONE); // hide
			}
			if (findViewById(R.id.closest_stops) == null) { // IF NOT present/inflated DO
				((ViewStub) findViewById(R.id.closest_stops_stub)).inflate(); // inflate
			}
			ListView closestStopsLayout = (ListView) findViewById(R.id.closest_stops);
			// show stops list
			closestStopsLayout.setVisibility(View.VISIBLE);
			ABusStop[] array = this.closestStops.toArray(new ABusStop[] {});
			this.adapter = new ArrayAdapterWithCustomView(this, R.layout.bus_tab_closest_stops_list_item, array);
			closestStopsLayout.setAdapter(this.adapter);
			closestStopsLayout.setOnItemClickListener(this);
			closestStopsLayout.setOnScrollListener(this);
			setClosestStopsNotLoading();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s, %s,%s,%s)", parent.getId(), view.getId(), position, id);
		if (this.closestStops != null && position < this.closestStops.size() && this.closestStops.get(position) != null) {
			Intent intent = new Intent(this, BusStopInfo.class);
			BusStop selectedStop = this.closestStops.get(position);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, selectedStop.getCode());
			intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, selectedStop.getPlace());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, selectedStop.getLineNumber());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, selectedStop.getLineNameOrNull());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, selectedStop.getLineTypeOrNull());
			startActivity(intent);
		}
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
		 * @param busStops the stops
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
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
			}

			ABusStop stop = this.busStops[position];
			if (stop != null) {
				// bus stop code
				((TextView) convertView.findViewById(R.id.stop_code)).setText(stop.getCode());
				// bus stop place
				((TextView) convertView.findViewById(R.id.label)).setText(BusUtils.cleanBusStopPlace(stop.getPlace()));
				// bus stop line number
				TextView lineNumberTv = (TextView) convertView.findViewById(R.id.line_number);
				lineNumberTv.setText(stop.getLineNumber());
				lineNumberTv.setBackgroundColor(BusUtils.getBusLineTypeBgColorFromType(stop.getLineTypeOrNull()));
				// bus stop line direction
				int busLineDirection = BusUtils.getBusLineSimpleDirection(stop.getDirectionId());
				((TextView) convertView.findViewById(R.id.line_direction)).setText(busLineDirection);
				// distance
				TextView distanceTv = (TextView) convertView.findViewById(R.id.distance);
				if (!TextUtils.isEmpty(stop.getDistanceString())) {
					distanceTv.setText(stop.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				} else {
					distanceTv.setVisibility(View.GONE);
					distanceTv.setText(null);
				}
				// compass
				ImageView compassImg = (ImageView) convertView.findViewById(R.id.compass);
				if (stop.getCompassMatrixOrNull() != null) {
					compassImg.setImageMatrix(stop.getCompassMatrix());
					compassImg.setVisibility(View.VISIBLE);
				} else {
					compassImg.setVisibility(View.GONE);
				}
				// favorite
				if (BusTab.this.favUIDs != null && BusTab.this.favUIDs.contains(stop.getUID())) {
					convertView.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.fav_img).setVisibility(View.GONE);
				}
				// // closest bike station
				// int index = -1;
				// if (BikeTab.this.orderedStationsIds != null) {
				// index = BikeTab.this.orderedStationsIds.indexOf(bikeStation.getTerminalName());
				// }
				// switch (index) {
				// case 0:
				// ((TextView) convertView.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT_BOLD);
				// distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
				// distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
				// break;
				// default:
				// ((TextView) convertView.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT);
				// distanceTv.setTypeface(Typeface.DEFAULT);
				// distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
				// break;
				// }
			}
			return convertView;
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
	 * The list scroll state.
	 */
	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (view == findViewById(R.id.list)) {
			this.scrollState = scrollState;
		}
	}

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

	/**
	 * Start the refresh closest stops tasks if necessary.
	 */
	private void refreshClosestStops() {
		MyLog.v(TAG, "refreshClosestStops()");
		// IF the task is NOT already running DO
		if (this.closestStopsTask == null || !this.closestStopsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setClosestStopsLoading();
			// IF location found DO
			Location currentLocation = getLocation();
			if (currentLocation == null) {
				MyLog.d(TAG, "no location yet...");
				return;
			}
			// find the closest stations
			this.closestStopsTask = new ClosestBusStopsFinderTask(this, this);
			this.closestStopsTask.execute(currentLocation);
			this.closestStopsLocation = currentLocation;
			new AsyncTask<Location, Void, Address>() {

				@Override
				protected Address doInBackground(Location... locations) {
					return LocationUtils.getLocationAddress(BusTab.this, locations[0]);
				}

				@Override
				protected void onPostExecute(Address result) {
					boolean refreshRequired = BusTab.this.closestStopsLocationAddress == null;
					BusTab.this.closestStopsLocationAddress = result;
					if (refreshRequired) {
						showNewClosestStopsTitle();
					}
				}

			}.execute(this.closestStopsLocation);
			// ELSE wait for location...
		}
	}

	/**
	 * Show new closest stops title.
	 */
	public void showNewClosestStopsTitle() {
		if (this.closestStopsLocationAddress != null && this.closestStopsLocation != null) {
			String text = LocationUtils.getLocationString(this, R.string.closest_bus_stops, this.closestStopsLocationAddress,
					this.closestStopsLocation.getAccuracy());
			((TextView) findViewById(R.id.closest_stops_title).findViewById(R.id.closest_bus_stops)).setText(text);
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;

	/**
	 * @return the location
	 */
	public Location getLocation() {
		if (this.location == null) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					// MyLog.v(TAG, "doInBackground()");
					return LocationUtils.getBestLastKnownLocation(BusTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "onPostExecute()");
					if (result != null) {
						BusTab.this.setLocation(result);
					}
					// enable location updates if necessary
					if (!BusTab.this.locationUpdatesEnabled) {
						LocationUtils.enableLocationUpdates(BusTab.this, BusTab.this);
						BusTab.this.locationUpdatesEnabled = true;
					}
				}

			}.execute();
		}
		return this.location;
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				SensorUtils.registerShakeAndCompassListener(this, this);
				// SensorUtils.registerCompassListener(this, this);
				// this.shakeHandled = false;
			}
		}
	}

	/**
	 * Set the closest stops as loading.
	 */
	private void setClosestStopsLoading() {
		MyLog.v(TAG, "setClosestStationsLoading()");
		View closestStopsTitle = findViewById(R.id.closest_stops_title);
		if (this.closestStops == null) {
			// set the BIG loading message
			// remove last location from the list divider
			((TextView) closestStopsTitle.findViewById(R.id.closest_bus_stops)).setText(R.string.closest_bus_stops);
			if (findViewById(R.id.closest_stops) != null) { // IF inflated/present DO
				// hide the list
				findViewById(R.id.closest_stops).setVisibility(View.GONE);
				// clean the list (useful ?)
				((LinearLayout) findViewById(R.id.closest_stops)).removeAllViews();
			}
			// show loading
			if (findViewById(R.id.closest_stops_loading) == null) { // IF NOT inflated/present DO
				((ViewStub) findViewById(R.id.closest_stops_loading_stub)).inflate(); // inflate
			}
			findViewById(R.id.closest_stops_loading).setVisibility(View.VISIBLE);
			// show waiting for location
			TextView detailMsgTv = (TextView) findViewById(R.id.closest_stops_loading).findViewById(R.id.detail_msg);
			detailMsgTv.setText(R.string.waiting_for_location_fix);
			detailMsgTv.setVisibility(View.VISIBLE);
			// } else { just notify the user ?
		}
		// show stop icon instead of refresh
		closestStopsTitle.findViewById(R.id.closest_bus_stops_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		closestStopsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the closest stations as not loading.
	 */
	private void setClosestStopsNotLoading() {
		MyLog.v(TAG, "setClosestStopsNotLoading()");
		View closestStopsTitle = findViewById(R.id.closest_stops_title);
		// show refresh icon instead of loading
		closestStopsTitle.findViewById(R.id.closest_bus_stops_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		closestStopsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStopsError() {
		MyLog.v(TAG, "setClosestStopsError()");
		// IF there are already stations DO
		if (this.closestStops != null) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUser(this, getString(R.string.closest_bus_stops_error));
		} else {
			// show the BIG message
			TextView cancelMsgTv = new TextView(this);
			cancelMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cancelMsgTv.setText(getString(R.string.closest_bus_stops_error));
			((LinearLayout) findViewById(R.id.closest_stops)).addView(cancelMsgTv);
			// hide loading
			findViewById(R.id.closest_stops_loading).setVisibility(View.GONE);
			findViewById(R.id.closest_stops).setVisibility(View.VISIBLE);
		}
		setClosestStopsNotLoading();
	}

	/**
	 * Refresh bus lines from database.
	 */
	private void refreshBusLinesFromDB() {
		MyLog.v(TAG, "refreshBusLinesFromDB()");
		new AsyncTask<Void, Void, List<BusLine>>() {
			@Override
			protected List<BusLine> doInBackground(Void... params) {
				return StmManager.findAllBusLinesList(getContentResolver());
			}

			protected void onPostExecute(List<BusLine> result) {
				LinearLayout busLinesLayout = (LinearLayout) findViewById(R.id.bus_line_list);
				for (BusLine busLine : result) {
					// the view
					View view = getLayoutInflater().inflate(R.layout.bus_tab_bus_line_list_item, null);// 70% of the time
					TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
					final String lineNumber = busLine.getNumber();
					final String lineName = busLine.getName();
					final String lineType = busLine.getType();
					lineNumberTv.setText(lineNumber);
					int color = BusUtils.getBusLineTypeBgColorFromType(busLine.getType());
					lineNumberTv.setBackgroundColor(color);
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick()");
							new BusLineSelectDirection(BusTab.this, lineNumber, lineName, lineType).showDialog();
						}
					});
					// view.setOnLongClickListener(new View.OnLongClickListener() {
					// @Override
					// public boolean onLongClick(View v) {
					// MyLog.v(TAG, "onLongClick()");
					// new BusLineSelectDirection(BusTab.this, lineNumber, lineName, lineType).showDialog();
					// return true;
					// }
					// });
					busLinesLayout.addView(view);
				}
				findViewById(R.id.bus_lines_loading).setVisibility(View.GONE);
				findViewById(R.id.bus_lines).setVisibility(View.VISIBLE);
			}

		}.execute();
	}

	/**
	 * Refresh or stop refresh the closest stops depending if running.
	 * @param v a view (not used)
	 */
	public void refreshOrStopRefreshClosestStops(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshClosestStops()");
		// IF the task is running DO
		if (this.closestStopsTask != null && this.closestStopsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.closestStopsTask.cancel(true);
			this.closestStopsTask = null;
		} else {
			// refreshSubwayStatus();
			refreshClosestStops();
		}
	}

	@Override
	public void onClosestStopsProgress(String message) {
		MyLog.v(TAG, "onClosestStopsProgress(%s)", message);
		// do nothing
	}

	@Override
	public void onClosestStopsDone(ClosestPOI<ABusStop> result) {
		MyLog.v(TAG, "onClosestStopsDone(%s)", result == null ? null : result.getPoiListSize());
		if (result == null || result.getPoiListOrNull() == null) {
			// show the error
			setClosestStopsError();
		} else {
			// get the result
			this.closestStops = result.getPoiList();
			refreshFavoriteUIDsFromDB();
			// shot the result
			showNewClosestStops();
			setClosestStopsNotLoading();
		}
	}

	/**
	 * Find favorites bus stops UIDs.
	 */
	private void refreshFavoriteUIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false; // don't trigger update if favorites are the same
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BusTab.this.favUIDs)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavUIDs = new ArrayList<String>();
				for (Fav busStopFav : result) {
					String UID = BusStop.getUID(busStopFav.getFkId(), busStopFav.getFkId2());
					if (BusTab.this.favUIDs == null || !BusTab.this.favUIDs.contains(UID)) {
						newFav = true; // new favorite
					}
					newfavUIDs.add(UID); // store UID
				}
				BusTab.this.favUIDs = newfavUIDs;
				// trigger change if necessary
				if (newFav) {
					notifyDataSetChanged();
				}
			};
		}.execute();
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		// TODO Auto-generated method stub
		this.setLocation(location);
		updateDistancesWithNewLocation();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		MyLog.v(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
		adaptToScreenSize(newConfig);
	}

	/**
	 * Adapt to the screen size
	 * @param configuration configuration
	 */
	private void adaptToScreenSize(Configuration configuration) {
		if (Utils.isScreenHeightSmall(this, configuration)) {
			// HIDE LINE TITLE
			findViewById(R.id.bus_line).setVisibility(View.GONE);
		} else {
			// SHOW LINE TITLE
			findViewById(R.id.bus_line).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Update the bike stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		// IF no closest bike stations AND new location DO
		Location currentLocation = getLocation();
		if (this.closestStops == null && currentLocation != null) {
			// start refreshing if not running.
			refreshClosestStops();
			return;
		}
		// ELSE IF there are closest stations AND new location DO
		if (this.closestStops != null && currentLocation != null) {
			// update the list distances
			boolean isDetailed = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
					UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			float accuracyInMeters = currentLocation.getAccuracy();
			for (ABusStop stop : this.closestStops) {
				// distance
				stop.setDistance(currentLocation.distanceTo(stop.getLocation()));
				stop.setDistanceString(Utils.getDistanceString(stop.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			// update the view
			// generateOrderedStationsIds();
			notifyDataSetChanged();
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
		return MenuUtils.inflateMenu(this, menu, R.menu.main_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}
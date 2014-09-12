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
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.common.AbstractManager;

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
 * This activity display information about a route.
 * @author Mathieu Méa
 */
@TargetApi(3)
public class RouteInfo extends Activity implements LocationListener, SensorEventListener, ShakeListener, CompassListener, OnScrollListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = RouteInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Route";

	/**
	 * The extra ID for the authority (required).
	 */
	public static final String EXTRA_AUTHORITY = "extra_authority";

	/**
	 * The extra ID for the route ID (required).
	 */
	public static final String EXTRA_ROUTE_ID = "extra_route_id";
	/**
	 * The extra ID for the route short name (optional).
	 */
	public static final String EXTRA_ROUTE_SHORT_NAME = "extra_route_short_name";
	/**
	 * Only used to display initial route long name.
	 */
	public static final String EXTRA_ROUTE_LONG_NAME = "extra_route_long_name";

	public static final String EXTRA_ROUTE_COLOR = "extra_route_color";
	public static final String EXTRA_ROUTE_TEXT_COLOR = "extra_route_text_color";
	/**
	 * The extra ID for the route trip ID (optional).
	 */
	public static final String EXTRA_TRIP_ID = "extra_trip_id";
	/**
	 * The extra ID for the route trip stop code (optional)
	 */
	public static final String EXTRA_STOP_ID = "extra_stop_id";

	/**
	 * The current route.
	 */
	private Route route;
	/**
	 * The route trip.
	 */
	private Trip trip;
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
	 * The list of stops.
	 */
	private List<TripStop> stops = new ArrayList<TripStop>();
	/**
	 * The stops list adapter.
	 */
	private ArrayAdapter<TripStop> adapter;
	/**
	 * The closest stop ID by distance.
	 */
	private Integer closestStopId;
	/**
	 * The favorite stops IDs.
	 */
	private List<Integer> favStopIds;
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

	public static Intent newInstance(Context context, String authority, RouteTripStop routeTripStop) {
		Intent intent = new Intent(context, SupportFactory.get().getRouteInfoClass());
		intent.putExtra(EXTRA_AUTHORITY, authority);
		if (routeTripStop.route != null) {
			intent.putExtra(EXTRA_ROUTE_ID, routeTripStop.route.id);
			intent.putExtra(EXTRA_ROUTE_SHORT_NAME, routeTripStop.route.shortName);
			intent.putExtra(EXTRA_ROUTE_LONG_NAME, routeTripStop.route.longName);
			intent.putExtra(EXTRA_ROUTE_COLOR, routeTripStop.route.color);
			intent.putExtra(EXTRA_ROUTE_TEXT_COLOR, routeTripStop.route.textColor);
		}
		if (routeTripStop.trip != null) {
			intent.putExtra(EXTRA_TRIP_ID, routeTripStop.trip.id);
		}
		if (routeTripStop.stop != null) {
			intent.putExtra(EXTRA_STOP_ID, routeTripStop.stop.id);
		}
		return intent;
	}

	public static Intent newInstance(Context context, String authority, Route route, Integer tripId, Integer stopId) {
		Intent intent = new Intent(context, SupportFactory.get().getRouteInfoClass());
		intent.putExtra(EXTRA_AUTHORITY, authority);
		if (route != null) {
			intent.putExtra(EXTRA_ROUTE_ID, route.id);
			intent.putExtra(EXTRA_ROUTE_SHORT_NAME, route.shortName);
			intent.putExtra(EXTRA_ROUTE_LONG_NAME, route.longName);
			intent.putExtra(EXTRA_ROUTE_COLOR, route.color);
			intent.putExtra(EXTRA_ROUTE_TEXT_COLOR, route.textColor);
		}
		if (tripId != null) {
			intent.putExtra(EXTRA_TRIP_ID, tripId);
		}
		if (stopId != null) {
			intent.putExtra(EXTRA_STOP_ID, stopId);
		}
		return intent;
	}

	public static Intent newInstance(Context context, String authority, Integer routeId, Integer tripId, Integer stopId) {
		Intent intent = new Intent(context, SupportFactory.get().getRouteInfoClass());
		intent.putExtra(EXTRA_AUTHORITY, authority);
		if (routeId != null) {
			intent.putExtra(EXTRA_ROUTE_ID, routeId);
		}
		if (tripId != null) {
			intent.putExtra(EXTRA_TRIP_ID, tripId);
		}
		if (stopId != null) {
			intent.putExtra(EXTRA_STOP_ID, stopId);
		}
		return intent;
	}

	public static Intent newInstance(Context context, String authority, int routeId, String routeShortName, String routeLongName, String routeColor,
			String routeTextColor, Integer tripId) {
		Intent intent = new Intent(context, SupportFactory.get().getRouteInfoClass());
		intent.putExtra(EXTRA_AUTHORITY, authority);
		intent.putExtra(EXTRA_ROUTE_ID, routeId);
		intent.putExtra(EXTRA_ROUTE_SHORT_NAME, routeShortName);
		intent.putExtra(EXTRA_ROUTE_LONG_NAME, routeLongName);
		intent.putExtra(EXTRA_ROUTE_COLOR, routeColor);
		intent.putExtra(EXTRA_ROUTE_TEXT_COLOR, routeTextColor);
		intent.putExtra(EXTRA_TRIP_ID, tripId);
		return intent;
	}

	public static Intent newInstance(Context context, String authority, Integer routeId) {
		Intent intent = new Intent(context, SupportFactory.get().getRouteInfoClass());
		intent.putExtra(EXTRA_AUTHORITY, authority);
		intent.putExtra(EXTRA_ROUTE_ID, routeId);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.route_info);

		setupList((ListView) findViewById(R.id.list));
		// get the route ID and trip ID from the intent.
		String authority = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_AUTHORITY);
		Integer routeId = Utils.getSavedIntValue(getIntent(), savedInstanceState, EXTRA_ROUTE_ID);
		String routeShortName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_SHORT_NAME);
		String routeLongName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_LONG_NAME);
		String routeColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_COLOR);
		String routeTextColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
		Integer tripId = Utils.getSavedIntValue(getIntent(), savedInstanceState, EXTRA_TRIP_ID);
		// temporary show the route information
		setRouteInformation(routeShortName, routeLongName, routeColor, routeTextColor);
		// show route
		showNewRoute(authority, routeId, tripId);
	}

	/**
	 * Setup the stops list.
	 * @param list the stops list
	 */
	public void setupList(ListView list) {
		list.setEmptyView(findViewById(R.id.list_empty));
		list.setOnScrollListener(this);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				// MyLog.v(TAG, "onItemClick(%s, %s, %s ,%s)", l.getId(), v.getId(), position, id);
				if (RouteInfo.this.stops != null && position < RouteInfo.this.stops.size() && RouteInfo.this.stops.get(position) != null
						&& !TextUtils.isEmpty(RouteInfo.this.stops.get(position).stop.code)) {
					// IF last stop, show descent only
					if (position + 1 == RouteInfo.this.stops.size()) {
						Toast toast = Toast.makeText(RouteInfo.this, R.string.descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						// return; why not?
					}
					Intent intent = StopInfo.newInstance(RouteInfo.this, RouteInfo.this.contentUri.getAuthority(), RouteInfo.this.stops.get(position).stop,
							RouteInfo.this.route);
					startActivity(intent);
				}
			}
		});
		this.adapter = new ArrayAdapterWithCustomView(RouteInfo.this, R.layout.route_info_stops_list_item);
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
		if (this.stops == null) {
			MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							RouteInfo.this.lastCompassInDegree = (int) orientation;
							MyLog.d(TAG, "updateCompass() > new lastCompassInDegree: %s", lastCompassInDegree);
							RouteInfo.this.lastCompassChanged = now;
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

	private Uri contentUri;

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
	 * Show the closest stop (if possible).
	 */
	private void showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.hasFocus && !this.shakeHandled && this.closestStopId != null) {
			Toast.makeText(this, R.string.shake_closest_stop_selected, Toast.LENGTH_SHORT).show();
			// FIXME RouteStopInfo
			// startActivity(BusStopInfo.newInstance(this, findStop(this.closestStopId).stop, this.route));
			startActivity(StopInfo.newInstance(RouteInfo.this, RouteInfo.this.contentUri.getAuthority(), findStop(this.closestStopId).stop,
					RouteInfo.this.route));
			this.shakeHandled = true;
		}
	}

	/**
	 * @param stopCode a stop code
	 * @return a stop name or null
	 */
	private TripStop findStop(Integer stopId) {
		if (this.stops == null) {
			return null;
		}
		for (TripStop tripStop : this.stops) {
			if (tripStop.stop.id == stopId) {
				return tripStop;
			}
		}
		return null;
	}

	// @Override
	public void showNewRoute(final String authority, final Integer newRouteId, final Integer newTripId) {
		MyLog.v(TAG, "showNewRoute(%s, %s, %s)", authority, newRouteId, newTripId);
		if ((this.route == null || this.trip == null) || (this.route.id != newRouteId || this.trip.id != newTripId)) {
			// show loading layout
			this.stops = null;
			notifyDataSetChanged(true);
			this.contentUri = Utils.newContentUri(authority);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					RouteInfo.this.route = AbstractManager.findRoute(RouteInfo.this, RouteInfo.this.contentUri, newRouteId);
					if (newTripId == null) { // use the 1st one
						RouteInfo.this.trip = AbstractManager.findTripsWithRouteIdList(RouteInfo.this, RouteInfo.this.contentUri, newRouteId).get(0);
					} else {
						RouteInfo.this.trip = AbstractManager.findTrip(RouteInfo.this, RouteInfo.this.contentUri, newTripId);
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
		refreshRouteAndTripInfo();
		refreshStopListFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(RouteInfo.this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(RouteInfo.this));
			updateDistancesWithNewLocation();
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh the route info UI.
	 */
	private void refreshRouteAndTripInfo() {
		// route info
		setRouteInformation(this.route.shortName, this.route.longName, this.route.color, this.route.textColor);
		// route direction info
		setupTrip((TextView) findViewById(R.id.stop_text));
	}

	/**
	 * Setup trip.
	 * @param routeStopsTv the route direction {@link TextView}
	 */
	private void setupTrip(TextView routeStopsTv) {
		routeStopsTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSelectDirectionDialog(null);
			}
		});
		routeStopsTv.setText(getString(R.string.stops_short_and_direction, this.trip.getHeading(this)));
	}

	/**
	 * Refresh the route information.
	 */
	public void setRouteInformation(String shortName, String longName, String color, String textColor) {
		// MyLog.v(TAG, "setRouteInformation(%s, %s, %s, %s)", shortName, longName, color, textColor);
		final TextView routeShortNameTv = (TextView) findViewById(R.id.route_short_name);
		if (TextUtils.isEmpty(shortName)) {
			// TODO show subway
		} else {
			routeShortNameTv.setText(shortName);
		}
		if (!TextUtils.isEmpty(textColor)) {
			routeShortNameTv.setTextColor(Utils.parseColor(textColor));
		}
		if (!TextUtils.isEmpty(color)) {
			routeShortNameTv.setBackgroundColor(Utils.parseColor(color));
		}
		final TextView routeLongNameTv = (TextView) findViewById(R.id.route_long_name);
		routeLongNameTv.setText(longName);
		routeLongNameTv.requestFocus();
	}

	/**
	 * Show the route dialog to select trip.
	 */
	public void showSelectDirectionDialog(View v) {
		// TODO use single choice items to show the current direction
		// TODO new RouteSelectTrip(this, this.route, String.valueOf(this.trip.id), this).showDialog();
	}

	/**
	 * Refresh the stops list UI.
	 */
	private void refreshStopListFromDB() {
		new AsyncTask<Void, Void, List<TripStop>>() {
			@Override
			protected List<TripStop> doInBackground(Void... params) {
				return AbstractManager.findStopsWithTripIdList(RouteInfo.this, RouteInfo.this.contentUri, RouteInfo.this.trip.id);
			}

			@Override
			protected void onPostExecute(List<TripStop> result) {
				RouteInfo.this.stops = result;
				generateOrderedStopCodes();
				refreshFavoriteStopCodesFromDB();
				notifyDataSetChanged(true);
				updateDistancesWithNewLocation(); // force update all stop with location
			};

		}.execute();
	}

	/**
	 * Find favorites stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				// return DataManager.findFavsByTypeList(getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP);
				return DataManager.findFavsByTypeList(getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false;
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(RouteInfo.this.favStopIds)) {
					newFav = true; // different size => different favorites
				}
				List<Integer> newfavStopIds = new ArrayList<Integer>();
				for (Fav stopFav : result) {
					// if (RouteInfo.this.route != null && RouteInfo.this.route.shortName.equals(stopFav.getFkId2())) { // check line number
					if (RouteInfo.this.favStopIds == null || !RouteInfo.this.favStopIds.contains(stopFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavStopIds.add(Integer.valueOf(stopFav.getFkId())); // store stop code
					// }
				}
				RouteInfo.this.favStopIds = newfavStopIds;
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
		TextView stopNameTv;
		// TextView stationNameTv;
		TextView distanceTv;
		// ImageView subwayImg;
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
			return RouteInfo.this.stops == null ? 0 : RouteInfo.this.stops.size();
		}

		@Override
		public int getPosition(TripStop item) {
			return RouteInfo.this.stops.indexOf(item);
		}

		@Override
		public TripStop getItem(int position) {
			return RouteInfo.this.stops.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
				holder.stopNameTv = (TextView) convertView.findViewById(R.id.stop_name);
				// holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				// holder.subwayImg = (ImageView) convertView.findViewById(R.id.subway_img);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			TripStop tripStop = getItem(position);
			if (tripStop != null) {
				// stop code
				holder.stopCodeTv.setText(tripStop.stop.code);
				// stop name
				holder.stopNameTv.setText(tripStop.stop.name);
				// // stop subway station
				// holder.subwayImg.setVisibility(View.GONE);
				// holder.stationNameTv.setVisibility(View.GONE);
				// favorite
				if (RouteInfo.this.favStopIds != null && RouteInfo.this.favStopIds.contains(tripStop.stop.id)) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// stop distance
				if (!TextUtils.isEmpty(tripStop.getDistanceString())) {
					holder.distanceTv.setText(tripStop.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE);
				}
				// set style for closest stop
				int index = -1;
				if (RouteInfo.this.closestStopId != null) {
					index = tripStop.stop.id == RouteInfo.this.closestStopId ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.stopNameTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
					break;
				default:
					holder.stopNameTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow);
					break;
				}
				// stop compass
				if (location != null && lastCompassInDegree != 0 && location.getAccuracy() <= tripStop.getDistance()) {
					float compassRotation = SensorUtils.getCompassRotationInDegree(location, tripStop, lastCompassInDegree, locationDeclination);
					SupportFactory.get().rotateImageView(holder.compassImg, compassRotation, RouteInfo.this);
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE);
				}
			}
			return convertView;
		}
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
			LocationUtils.updateDistanceWithString(this, this.stops, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					Integer previousClosest = RouteInfo.this.closestStopId;
					generateOrderedStopCodes();
					notifyDataSetChanged(RouteInfo.this.closestStopId == null ? false : RouteInfo.this.closestStopId.equals(previousClosest));
				}
			});
		}
	}

	/**
	 * Generate the ordered stops codes.
	 */
	public void generateOrderedStopCodes() {
		if (this.stops == null || this.stops.size() == 0) {
			return;
		}
		List<TripStop> orderedStops = new ArrayList<TripStop>(this.stops);
		Collections.sort(orderedStops, POI.POI_DISTANCE_COMPARATOR);
		this.closestStopId = orderedStops.get(0).getDistance() > 0 ? orderedStops.get(0).stop.id : null;
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
		return MenuUtils.inflateMenu(this, menu, R.menu.route_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.direction:
			showSelectDirectionDialog(null);
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}

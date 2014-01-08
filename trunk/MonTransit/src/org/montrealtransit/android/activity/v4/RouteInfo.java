package org.montrealtransit.android.activity.v4;

import java.util.List;
import java.util.Locale;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.dialog.RouteSelectTripDialog;
import org.montrealtransit.android.dialog.RouteSelectTripDialogListener;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.viewpagerindicator.TitlePageIndicator;

@TargetApi(4)
public class RouteInfo extends FragmentActivity implements LocationListener, SensorEventListener, ShakeListener, OnPageChangeListener,
		RouteSelectTripDialogListener {

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

	public static final String EXTRA_ROUTE_ID = org.montrealtransit.android.activity.RouteInfo.EXTRA_ROUTE_ID;
	/**
	 * The extra ID for the route short name.
	 */
	public static final String EXTRA_ROUTE_SHORT_NAME = org.montrealtransit.android.activity.RouteInfo.EXTRA_ROUTE_SHORT_NAME;
	/**
	 * Only used to display initial route long name.
	 */
	public static final String EXTRA_ROUTE_LONG_NAME = org.montrealtransit.android.activity.RouteInfo.EXTRA_ROUTE_LONG_NAME;

	public static final String EXTRA_ROUTE_COLOR = org.montrealtransit.android.activity.RouteInfo.EXTRA_ROUTE_COLOR;

	public static final String EXTRA_ROUTE_TEXT_COLOR = org.montrealtransit.android.activity.RouteInfo.EXTRA_ROUTE_TEXT_COLOR;
	/**
	 * The extra ID for the trip ID.
	 */
	public static final String EXTRA_TRIP_ID = org.montrealtransit.android.activity.RouteInfo.EXTRA_TRIP_ID;
	/**
	 * The extra ID for the stop ID (optional)
	 */
	public static final String EXTRA_STOP_ID = org.montrealtransit.android.activity.RouteInfo.EXTRA_STOP_ID;

	/**
	 * The current route.
	 */
	private Route route;
	/**
	 * The trips.
	 */
	private List<Trip> routeTrips;
	/**
	 * The trips title.
	 */
	private SparseArray<String> routeTripsStrings;
	/**
	 * The current direction ID.
	 */
	private int currentTripId = -1;
	/**
	 * The route trip adapter.
	 */
	private RouteTripAdapter adapter;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the shake updates enabled?
	 */
	private boolean shakeUpdatesEnabled = false;
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
	 * The selected stop ID (or null).
	 */
	private Integer currentStopId;

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;
	private Uri contentUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.route_info);

		// get the route ID and trip ID from the intent.
		String authority = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_AUTHORITY);
		Integer routeId = Utils.getSavedIntValue(getIntent(), savedInstanceState, EXTRA_ROUTE_ID);
		String routeShortName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_SHORT_NAME);
		String routeLongName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_LONG_NAME);
		String routeColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_COLOR);
		String routeTextColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
		Integer tripId = Utils.getSavedIntValue(getIntent(), savedInstanceState, EXTRA_TRIP_ID);
		this.currentStopId = Utils.getSavedIntValue(getIntent(), savedInstanceState, EXTRA_STOP_ID);
		// temporary show the route information
		setRouteInformation(routeShortName, routeLongName, routeColor, routeTextColor);
		// show route
		showNewRoute(authority, routeId, tripId);
	}

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
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// resume fragments
		if (this.adapter != null) {
			for (int i = 0; i < this.adapter.getCount(); i++) {
				Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
				if (f != null) {
					RouteTripFragment df = (RouteTripFragment) f;
					df.onResumeWithFocus(this);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.shakeUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.shakeUpdatesEnabled = false;
		}
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(se, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
		// TODO broadcast new sensor data to fragments?
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
	 * Show the closest route stop (if possible).
	 */
	private void showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.hasFocus && !this.shakeHandled) {
			Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + currentTripIndex());
			if (f != null) {
				RouteTripFragment df = (RouteTripFragment) f;
				this.shakeHandled = df.showClosestStop();
			}
		}
	}

	public void showNewRoute(final String newAuthority, final Integer newRouteId, final Integer newTripId) {
		MyLog.v(TAG, "showNewRoute(%s, %s, %s)", newAuthority, newRouteId, newTripId);
		this.currentTripId = newTripId == null ? -1 : newTripId.intValue();
		if (this.route == null || this.route.id != newRouteId) {
			// show loading layout
			showLoading();
			this.contentUri = Utils.newContentUri(newAuthority);
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					MyLog.v(TAG, "showNewRoute()>doInBackground()");
					RouteInfo.this.route = AbstractManager.findRoute(RouteInfo.this, RouteInfo.this.contentUri, newRouteId);
					// MyLog.d(TAG, "showNewRoute()>doInBackground() > route null? %s", (route == null));
					RouteInfo.this.routeTrips = AbstractManager.findTripsWithRouteIdList(RouteInfo.this, RouteInfo.this.contentUri, newRouteId);
					// MyLog.d(TAG, "showNewRoute()>doInBackground() > routeTrips? %s", (routeTrips == null ? null : routeTrips.size()));
					RouteInfo.this.routeTripsStrings = new SparseArray<String>();
					if (RouteInfo.this.routeTrips != null) {
						for (Trip direction : RouteInfo.this.routeTrips) {
							RouteInfo.this.routeTripsStrings.put(direction.id, direction.getHeading(RouteInfo.this).toUpperCase(Locale.getDefault()));
						}
					}
					// MyLog.d(TAG, "showNewRoute()>doInBackground() > routeTripsStrings? %s", (routeTripsStrings == null ? null : routeTripsStrings.size()));
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					refreshAll();
				};

			}.execute();
		}
	}

	@Override
	public void showNewRouteTrip(String newAuthority, Route newRoute, Trip newTrip) {
		MyLog.v(TAG, "showNewRoute(%s, %s, %s)", newAuthority, newRoute, newTrip);
		final int newRouteId = newRoute == null ? -1 : newRoute.id;
		final int newTripId = newTrip == null ? -1 : newTrip.id;
		final String currentAuthority = this.contentUri == null ? null : this.contentUri.getAuthority();
		// MyLog.d(TAG, "currentAuthority: %s", currentAuthority);
		// MyLog.d(TAG, "this.route: %s", this.route);
		// MyLog.d(TAG, "this.currentTripId: %s", this.currentTripId);
		final boolean isEmptyView = TextUtils.isEmpty(currentAuthority) || this.route == null;
		if (isEmptyView || !currentAuthority.equals(newAuthority) || this.route.id != newRouteId || this.currentTripId != newTripId) {
			// something is new
			if (this.route != null && this.route.id == newRouteId) {
				// same route, just switch trip if different
				if (this.currentTripId != newTripId) {
					this.currentTripId = newTripId;
					final TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
					indicator.setCurrentItem(currentTripIndex());
				}
			} else {
				// load new route
				showNewRoute(newAuthority, newRouteId, newTripId);
			}
		}
	}

	/**
	 * Show as loading.
	 */
	private void showLoading() {
		findViewById(R.id.route_stops).setVisibility(View.GONE);
		findViewById(R.id.route_stops_loading).setVisibility(View.VISIBLE);
	}

	/**
	 * Show as NOT loading.
	 */
	private void hideLoading() {
		findViewById(R.id.route_stops_loading).setVisibility(View.GONE);
		findViewById(R.id.route_stops).setVisibility(View.VISIBLE);
	}

	/**
	 * Refresh ALL the UI.
	 */
	private void refreshAll() {
		refreshRouteInfo();
		refreshStopListsFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(this));
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh the route info UI.
	 */
	private void refreshRouteInfo() {
		// route short name and long name
		if (this.route == null) {
			return;
		}
		setRouteInformation(this.route.shortName, this.route.longName, this.route.color, this.route.textColor);
	}

	/**
	 * Refresh the route UI.
	 */
	public void setRouteInformation(String shortName, String longName, String color, String textColor) {
		MyLog.v(TAG, "setRouteInformation(%s, %s, %s, %s)", shortName, longName, color, textColor);
		final View routeFL = findViewById(R.id.route);
		final View routeImg = findViewById(R.id.route_type_img);
		final TextView routeShortNameTv = (TextView) findViewById(R.id.route_short_name);
		final TextView routeLongNameTv = (TextView) findViewById(R.id.route_long_name);
		if (TextUtils.isEmpty(shortName)) {
			routeShortNameTv.setVisibility(View.GONE);
			routeImg.setVisibility(View.VISIBLE);
		} else {
			routeImg.setVisibility(View.GONE);
			routeShortNameTv.setText(shortName);
			routeShortNameTv.setVisibility(View.VISIBLE);
		}
		if (!TextUtils.isEmpty(textColor)) {
			routeShortNameTv.setTextColor(Utils.parseColor(textColor));
		}
		if (!TextUtils.isEmpty(color)) {
			routeFL.setBackgroundColor(Utils.parseColor(color));
		}
		routeLongNameTv.setText(longName);
		routeLongNameTv.requestFocus();
	}

	/**
	 * Refresh the stops lists UI.
	 */
	private void refreshStopListsFromDB() {
		MyLog.v(TAG, "refreshStopListsFromDB()");
		// instantiate view pager...
		ViewPager viewpager = (ViewPager) findViewById(R.id.viewpager);
		TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		this.adapter = new RouteTripAdapter(getSupportFragmentManager());
		viewpager.setAdapter(this.adapter);
		indicator.setViewPager(viewpager);
		indicator.setCurrentItem(currentTripIndex());
		indicator.setOnPageChangeListener(this);
		hideLoading();
	}

	private int currentTripIndex() {
		MyLog.v(TAG, "currentTripIndex()");
		// MyLog.d(TAG, "this.currentTripId: " + this.currentTripId);
		int index = 0;
		if (this.routeTrips != null && this.currentTripId > 0) {
			for (int i = 0; i < this.routeTrips.size(); i++) {
				if (this.routeTrips.get(i).id == this.currentTripId) {
					index = i;
				}
			}
		}
		// MyLog.d(TAG, "index: " + index);
		return index;
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		MyLog.v(TAG, "onPageSelected(%s)", position);
		if (this.routeTrips != null && position < this.routeTrips.size()) {
			this.currentTripId = this.routeTrips.get(position).id;
		} else {
			this.currentTripId = -1;
		}
		// MyLog.d(TAG, "this.currentTripId: " + this.currentTripId);
	}

	/**
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				if (!this.shakeUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.shakeUpdatesEnabled = true;
					this.shakeHandled = false;
				}
				if (this.adapter != null) {
					for (int i = 0; i < this.adapter.getCount(); i++) {
						Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
						if (f != null) {
							RouteTripFragment df = (RouteTripFragment) f;
							df.setLocation(this.location);
						}
					}
				}
			}
		}
	}

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <b>NULL</b>
	 */
	public Location getLocation() {
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
	 * @return the route or null
	 */
	public Route getRoute() {
		return this.route;
	}

	@Override
	public void onLocationChanged(Location location) {
		// MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
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

	/**
	 * Show the route dialog to select trip.
	 */
	public void showSelectDirectionDialog(View v) {
		MyLog.v(TAG, "showSelectDirectionDialog()");
		new RouteSelectTripDialog(this, this.contentUri.getAuthority(), this.route, this.currentTripId, this).showDialog();
	}

	/**
	 * The route trip view pager adapter.
	 */
	private class RouteTripAdapter extends FragmentPagerAdapter {

		/**
		 * Default constructor.
		 * @param fm fragment manager
		 */
		public RouteTripAdapter(FragmentManager fm) {
			super(fm);
			// MyLog.v(TAG, "RouteTripAdapter()");
		}

		@Override
		public CharSequence getPageTitle(int position) {
			// MyLog.v(TAG, "getPageTitle(%s)", position);
			return RouteInfo.this.routeTripsStrings.get(RouteInfo.this.routeTrips.get(position).id);
		}

		@Override
		public int getCount() {
			// MyLog.v(TAG, "getCount()");
			return RouteInfo.this.routeTrips == null ? 0 : RouteInfo.this.routeTrips.size();
		}

		@Override
		public Fragment getItem(int position) {
			MyLog.v(TAG, "getItem(%s)", position);
			return RouteTripFragment.newInstance(RouteInfo.this.contentUri.getAuthority(), RouteInfo.this.route, RouteInfo.this.routeTrips.get(position),
					RouteInfo.this.currentStopId);
		}

	}
}

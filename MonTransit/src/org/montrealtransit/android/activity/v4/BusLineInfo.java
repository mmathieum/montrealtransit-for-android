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
import org.montrealtransit.android.provider.StmBusManager;

import android.annotation.TargetApi;
import android.content.Intent;
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
public class BusLineInfo extends FragmentActivity implements LocationListener, SensorEventListener, ShakeListener, /* CompassListener, */OnPageChangeListener {

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
	public static final String EXTRA_ROUTE_SHORT_NAME = org.montrealtransit.android.activity.BusLineInfo.EXTRA_ROUTE_SHORT_NAME;
	/**
	 * Only used to display initial bus line name.
	 */
	public static final String EXTRA_ROUTE_LONG_NAME = org.montrealtransit.android.activity.BusLineInfo.EXTRA_ROUTE_LONG_NAME;

	public static final String EXTRA_ROUTE_COLOR = org.montrealtransit.android.activity.BusLineInfo.EXTRA_ROUTE_COLOR;

	public static final String EXTRA_ROUTE_TEXT_COLOR = org.montrealtransit.android.activity.BusLineInfo.EXTRA_ROUTE_TEXT_COLOR;
	/**
	 * The extra ID for the bus line direction ID.
	 */
	public static final String EXTRA_LINE_TRIP_ID = org.montrealtransit.android.activity.BusLineInfo.EXTRA_LINE_TRIP_ID; // TODO switch to integer
	/**
	 * The extra ID for the bus stop code (optional)
	 */
	public static final String EXTRA_LINE_STOP_CODE = org.montrealtransit.android.activity.BusLineInfo.EXTRA_LINE_STOP_CODE;

	/**
	 * The current bus line.
	 */
	private Route busLine;
	/**
	 * The bus line directions.
	 */
	private List<Trip> busLineDirections;
	/**
	 * The bus line directions title.
	 */
	private SparseArray<String> busLineDirectionsString;
	/**
	 * The current direction ID.
	 */
	private String currentBusLineDirectionId;
	/**
	 * The bus line direction adapter.
	 */
	private BusLineDirectionAdapter adapter;
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
	 * The selected stop code (or null).
	 */
	private String currentStopCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_info);

		// get the bus line ID and bus line direction ID from the intent.
		String lineNumber = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_SHORT_NAME);
		String lineName = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_LONG_NAME);
		String lineColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_COLOR);
		String lineTextColor = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
		String lineDirectionId = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_LINE_TRIP_ID);
		this.currentStopCode = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_LINE_STOP_CODE);
		// temporary show the line name & number
		setLineNumberAndName(lineNumber, lineName, lineColor, lineTextColor);
		// show bus line
		showNewLine(lineNumber, lineDirectionId);
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
					BusLineDirectionFragment df = (BusLineDirectionFragment) f;
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
	 * Show the closest bus line station (if possible).
	 */
	private void showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.hasFocus && !this.shakeHandled/* && !TextUtils.isEmpty(this.closestStopCode) */) {
			Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + currentDirectionIndex());
			if (f != null) {
				BusLineDirectionFragment df = (BusLineDirectionFragment) f;
				this.shakeHandled = df.showClosestStop();
			}
		}
	}

	public void showNewLine(final String newLineNumber, final String newDirectionId) {
		MyLog.v(TAG, "showNewLine(%s, %s)", newLineNumber, newDirectionId);
		this.currentBusLineDirectionId = newDirectionId;
		if (this.busLine == null || !this.busLine.shortName.equals(newLineNumber)) {
			// show loading layout
			showLoading();
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					BusLineInfo.this.busLine = StmBusManager.findRoute(BusLineInfo.this, newLineNumber);
					BusLineInfo.this.busLineDirections = StmBusManager.findTripsWithRouteIdList(BusLineInfo.this, newLineNumber);
					BusLineInfo.this.busLineDirectionsString = new SparseArray<String>();
					for (Trip direction : BusLineInfo.this.busLineDirections) {
						BusLineInfo.this.busLineDirectionsString.put(direction.id, direction.getHeading(BusLineInfo.this).toUpperCase(Locale.getDefault()));
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
	 * Show as loading.
	 */
	private void showLoading() {
		findViewById(R.id.bus_line_stops).setVisibility(View.GONE);
		findViewById(R.id.bus_line_stops_loading).setVisibility(View.VISIBLE);
	}

	/**
	 * Show as NOT loading.
	 */
	private void hideLoading() {
		findViewById(R.id.bus_line_stops_loading).setVisibility(View.GONE);
		findViewById(R.id.bus_line_stops).setVisibility(View.VISIBLE);
	}

	/**
	 * Refresh ALL the UI.
	 */
	private void refreshAll() {
		refreshBusLineInfo();
		refreshBusStopListsFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(this));
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
	 * Refresh the bus stops lists UI.
	 */
	private void refreshBusStopListsFromDB() {
		MyLog.v(TAG, "refreshBusStopListsFromDB()");
		// instantiate view pager...
		ViewPager viewpager = (ViewPager) findViewById(R.id.viewpager);
		TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		this.adapter = new BusLineDirectionAdapter(getSupportFragmentManager());
		viewpager.setAdapter(this.adapter);
		indicator.setViewPager(viewpager);
		indicator.setCurrentItem(currentDirectionIndex());
		indicator.setOnPageChangeListener(this);
		hideLoading();
	}

	private int currentDirectionIndex() {
		MyLog.v(TAG, "currentDirectionIndex()");
		// MyLog.d(TAG, "this.currentBusLineDirectionId: " + this.currentBusLineDirectionId);
		int index = 0;
		if (this.busLineDirections != null) {
			for (int i = 0; i < this.busLineDirections.size(); i++) {
				if (String.valueOf(this.busLineDirections.get(i).id).equals(this.currentBusLineDirectionId)) {// TODO burk
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
		if (this.busLineDirections != null && this.busLineDirections.size() > position) {
			this.currentBusLineDirectionId = String.valueOf(this.busLineDirections.get(position).id);
		} else {
			this.currentBusLineDirectionId = null;
		}
		// MyLog.d(TAG, "this.currentBusLineDirectionId: " + this.currentBusLineDirectionId);
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
				if (!this.shakeUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.shakeUpdatesEnabled = true;
					this.shakeHandled = false;
				}
				if (this.adapter != null) {
					for (int i = 0; i < this.adapter.getCount(); i++) {
						Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
						if (f != null) {
							BusLineDirectionFragment df = (BusLineDirectionFragment) f;
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
	 * @return the bus line or null
	 */
	public Route getBusLine() {
		return this.busLine;
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
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_line_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map:
			showSTMBusLineMap(null);
			return true;
		case R.id.direction:
			// showSelectDirectionDialog(null); TODO
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	// /**
	// * Show the bus line dialog to select direction.
	// */
	// public void showSelectDirectionDialog(View v) {
	// // TODO use single choice items to show the current direction
	// new BusLineSelectDirection(this, this.busLine.getNumber(), this.busLine.getName(), this.busLine.getType(), this.busLineDirection.getId(), this)
	// .showDialog();
	// }

	/**
	 * The bus line directions view pager adapter.
	 */
	private class BusLineDirectionAdapter extends FragmentPagerAdapter {

		/**
		 * Default constructor.
		 * @param fm fragment manager
		 */
		public BusLineDirectionAdapter(FragmentManager fm) {
			super(fm);
			// MyLog.v(TAG, "BusLineDirectionAdapter()");
		}

		@Override
		public CharSequence getPageTitle(int position) {
			// MyLog.v(TAG, "getPageTitle(%s)", position);
			return BusLineInfo.this.busLineDirectionsString.get(BusLineInfo.this.busLineDirections.get(position).id);
		}

		@Override
		public int getCount() {
			// MyLog.v(TAG, "getCount()");
			return BusLineInfo.this.busLineDirections.size();
		}

		@Override
		public Fragment getItem(int position) {
			// MyLog.v(TAG, "getItem(%s)", position);
			return BusLineDirectionFragment.newInstance(BusLineInfo.this.busLine, BusLineInfo.this.busLineDirections.get(position),
					BusLineInfo.this.currentStopCode);
		}

	}
}

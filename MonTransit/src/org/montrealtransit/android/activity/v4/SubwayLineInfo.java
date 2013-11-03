package org.montrealtransit.android.activity.v4;

import java.util.Locale;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.TitlePageIndicator;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class SubwayLineInfo extends FragmentActivity implements LocationListener, SensorEventListener, ShakeListener, CompassListener, OnPageChangeListener {

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
	public static final String EXTRA_LINE_NUMBER = org.montrealtransit.android.activity.SubwayLineInfo.EXTRA_LINE_NUMBER;
	/**
	 * The extra for the subway station display order.
	 */
	public static final String EXTRA_ORDER_PREF = org.montrealtransit.android.activity.SubwayLineInfo.EXTRA_ORDER_PREF;
	/**
	 * The extra for the subway station ID (optional)
	 */
	public static final String EXTRA_STATION_ID = org.montrealtransit.android.activity.SubwayLineInfo.EXTRA_STATION_ID;
	/**
	 * The subway line directions.
	 */
	private static final String[] subwayLineDirections = new String[] { UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL,
			UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC };

	private SubwayStation[] subwayLineDirectionsStations = new SubwayStation[subwayLineDirections.length];
	/**
	 * The subway line.
	 */
	private SubwayLine subwayLine;
	/**
	 * The subway line direction adapter.
	 */
	private SubwayLineDirectionAdapter adapter;
	/**
	 * The subway station list direction ID (order preference).
	 */
	private String currentSubwayLineDirectionId;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass updates enabled?
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
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];
	/**
	 * The selected station ID (or null)
	 */
	public String currentStationId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_info);

		int lineNumber = Integer.valueOf(Utils.getSavedStringValue(getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_LINE_NUMBER)).intValue();
		String newOrderPref = Utils.getSavedStringValue(getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_ORDER_PREF);
		this.currentStationId = Utils.getSavedStringValue(getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_STATION_ID);
		showNewSubway(lineNumber, newOrderPref);
	}

	/**
	 * Show new subway.
	 * @param newLineNumber new subway line number
	 * @param newOrderPref new subway order preference
	 */
	public void showNewSubway(int newLineNumber, final String newOrderPref) {
		MyLog.v(TAG, "showNewSubway(%s, %s)", newLineNumber, newOrderPref);
		if ((this.subwayLine == null || this.subwayLine.getNumber() != newLineNumber)/* || (this.orderPref != null && !this.orderPref.equals(newOrderPref)) */) {
			// temporary show the subway line name
			((TextView) findViewById(R.id.line_name)).setText(SubwayUtils.getSubwayLineName(newLineNumber));
			((ImageView) findViewById(R.id.subway_img)).setImageResource(SubwayUtils.getSubwayLineImgId(newLineNumber));
			// show loading layout
			showLoading();
			new AsyncTask<Integer, Void, Void>() {
				@Override
				protected Void doInBackground(Integer... params) {
					if (SubwayLineInfo.this.subwayLine != null && SubwayLineInfo.this.subwayLine.getNumber() == params[0].intValue()) {
						return null;
					}
					SubwayLineInfo.this.subwayLine = StmManager.findSubwayLine(SubwayLineInfo.this, params[0]);
					if (newOrderPref == null) {
						SubwayLineInfo.this.currentSubwayLineDirectionId = UserPreferences.getPrefDefault(SubwayLineInfo.this,
								UserPreferences.getPrefsSubwayStationsOrder(SubwayLineInfo.this.subwayLine.getNumber()),
								UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
					} else {
						UserPreferences.savePrefDefault(SubwayLineInfo.this,
								UserPreferences.getPrefsSubwayStationsOrder(SubwayLineInfo.this.subwayLine.getNumber()), newOrderPref);
						SubwayLineInfo.this.currentSubwayLineDirectionId = newOrderPref;
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					refreshAll();
				};
			}.execute(newLineNumber);
		}
	}

	/**
	 * Show as loading.
	 */
	private void showLoading() {
		findViewById(R.id.subway_line_stations).setVisibility(View.GONE);
		findViewById(R.id.subway_line_stations_loading).setVisibility(View.VISIBLE);
	}

	/**
	 * Show as NOT loading.
	 */
	private void hideLoading() {
		findViewById(R.id.subway_line_stations_loading).setVisibility(View.GONE);
		findViewById(R.id.subway_line_stations).setVisibility(View.VISIBLE);
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
		// resume fragments
		if (this.adapter != null) {
			for (int i = 0; i < this.adapter.getCount(); i++) {
				Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
				if (f != null) {
					SubwayLineDirectionFragment df = (SubwayLineDirectionFragment) f;
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
		SensorUtils.checkForCompass(this, se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onShake() {
		MyLog.v(TAG, "onShake()");
		showClosestStation();
	}

	/**
	 * Show the closest subway line station (if possible).
	 */
	private void showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		if (this.hasFocus && !this.shakeHandled) {
			Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + currentDirectionIndex());
			if (f != null) {
				SubwayLineDirectionFragment df = (SubwayLineDirectionFragment) f;
				this.shakeHandled = df.showClosestStation();
			}
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(float orientation, boolean force) {
		// MyLog.v(TAG, "onCompass()");
		if (this.adapter != null && this.accelerometerValues != null && this.magneticFieldValues != null) {
			for (int i = 0; i < this.adapter.getCount(); i++) {
				Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
				if (f != null) {
					SubwayLineDirectionFragment df = (SubwayLineDirectionFragment) f;
					df.updateCompass(orientation, false);
				}
			}
		}
	}

	/**
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		MyLog.v(TAG, "refreshAll()");
		refreshSubwayLineInfo();
		refreshSubwayStationsListsFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistancesWithNewLocation();
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Refresh the subway stations lists.
	 */
	private void refreshSubwayStationsListsFromDB() {
		MyLog.v(TAG, "refreshSubwayStationsListsFromDB()");
		// instantiate view pager...
		ViewPager viewpager = (ViewPager) findViewById(R.id.viewpager);
		TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		this.adapter = new SubwayLineDirectionAdapter(getSupportFragmentManager());
		viewpager.setAdapter(this.adapter);
		indicator.setViewPager(viewpager);
		indicator.setCurrentItem(currentDirectionIndex());
		indicator.setOnPageChangeListener(this);
		hideLoading();
	}

	/**
	 * @return the current direction index
	 */
	private int currentDirectionIndex() {
		// MyLog.v(TAG, "currentDirectionIndex()");
		int index = 0;
		if (subwayLineDirections != null) {
			for (int i = 0; i < subwayLineDirections.length; i++) {
				if (subwayLineDirections[i].equals(this.currentSubwayLineDirectionId)) {
					index = i;
				}
			}
		}
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
		// MyLog.v(TAG, "onPageSelected(%s)", position);
		if (subwayLineDirections != null && subwayLineDirections.length > position) {
			this.currentSubwayLineDirectionId = subwayLineDirections[position];
		} else {
			this.currentSubwayLineDirectionId = null;
		}
	}

	/**
	 * Refresh the subway line info.
	 */
	private void refreshSubwayLineInfo() {
		// subway line name
		((TextView) findViewById(R.id.line_name)).setText(SubwayUtils.getSubwayLineName(this.subwayLine.getNumber()));
		((ImageView) findViewById(R.id.subway_img)).setImageResource(SubwayUtils.getSubwayLineImgId(this.subwayLine.getNumber()));
		// subway line direction
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				for (int i = 0; i < subwayLineDirections.length; i++) {
					subwayLineDirectionsStations[i] = StmManager.findSubwayLineLastSubwayStation(SubwayLineInfo.this,
							SubwayLineInfo.this.subwayLine.getNumber(), getSortOrderFromOrderPref(subwayLineDirections[i]));
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// refresh view pager title
				((TitlePageIndicator) findViewById(R.id.indicator)).notifyDataSetChanged();
			};
		}.execute();
	}

	/**
	 * @param sharedPreferences preference
	 * @return actual {@link StmStore} sort order
	 */
	private String getSortOrderFromOrderPref(String sharedPreferences) {
		if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER;
		} else if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC;
		} else {
			// return StmStore.SubwayStation.DEFAULT_SORT_ORDER; // DEFAULT (A-Z order)
			return StmStore.SubwayStation.NATURAL_SORT_ORDER; // DEFAULT (ASC)
		}
	}

	/**
	 * Update the distances with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		// MyLog.v(TAG, "updateDistancesWithNewLocation()");
		if (this.adapter != null) {
			for (int i = 0; i < this.adapter.getCount(); i++) {
				Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + i);
				if (f != null) {
					SubwayLineDirectionFragment df = (SubwayLineDirectionFragment) f;
					df.updateDistancesWithNewLocation(this);
				}
			}
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;
	private float locationDeclination;

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <b>NULL</b>
	 */
	public Location getLocation() {
		if (this.location == null) {
			Location bestLastKnownLocationOrNull = LocationUtils.getBestLastKnownLocation(this);
			if (bestLastKnownLocationOrNull != null) {
				setLocation(bestLastKnownLocationOrNull);
			}
			// enable location updates if necessary
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
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
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.shakeUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.shakeUpdatesEnabled = true;
					this.shakeHandled = false;
				}
			}
		}
	}

	public float getLocationDeclination() {
		return locationDeclination;
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		setLocation(location);
		updateDistancesWithNewLocation();
	}

	@Override
	public void onProviderEnabled(String provider) {
		// MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.createMainMenu(this, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	/**
	 * The subway line directions view pager adapter.
	 */
	private class SubwayLineDirectionAdapter extends FragmentPagerAdapter {

		/**
		 * Default constructor.
		 * @param fm fragment manager
		 */
		public SubwayLineDirectionAdapter(FragmentManager fm) {
			super(fm);
			// MyLog.v(TAG, "SubwayLineDirectionAdapter()");
		}

		@Override
		public CharSequence getPageTitle(int position) {
			// MyLog.v(TAG, "getPageTitle(%s)", position);
			if (subwayLineDirectionsStations != null && position < subwayLineDirectionsStations.length && subwayLineDirectionsStations[position] != null) {
				return subwayLineDirectionsStations[position].getName().toUpperCase(Locale.getDefault());
			}
			return getString(R.string.ellipsis);
		}

		@Override
		public int getCount() {
			// MyLog.v(TAG, "getCount()");
			return subwayLineDirections.length;
		}

		@Override
		public Fragment getItem(int position) {
			// MyLog.v(TAG, "getItem(%s)", position);
			return SubwayLineDirectionFragment.newInstance(SubwayLineInfo.this.subwayLine.getNumber(), subwayLineDirections[position],
					SubwayLineInfo.this.currentStationId);
		}

	}
}

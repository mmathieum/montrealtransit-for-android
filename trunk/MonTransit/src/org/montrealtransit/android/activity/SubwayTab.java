package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask.ClosestSubwayStationsFinderListener;
import org.montrealtransit.android.services.StmInfoStatusApiReader;
import org.montrealtransit.android.services.StmInfoStatusReaderListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
import android.widget.Toast;

/**
 * Display a list of subway lines.
 * @author Mathieu Méa
 */
public class SubwayTab extends Activity implements LocationListener, StmInfoStatusReaderListener, ClosestSubwayStationsFinderListener, SensorEventListener,
		ShakeListener, CompassListener, OnItemClickListener, OnScrollListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Subways";
	/**
	 * The validity of the current status (in seconds).
	 */
	private static final int STATUS_TOO_OLD_IN_SEC = 20 * 60; // 20 minutes

	/**
	 * The cursor used to display the subway lines.
	 */
	private Cursor cursor;
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
	 * The closest stations.
	 */
	private List<ASubwayStation> closestStations;
	/**
	 * The location used to generate the closest stations.
	 */
	private Location closestStationsLocation;
	/**
	 * The location address used to generate the closest stations.
	 */
	protected String closestStationsLocationAddress;
	/**
	 * The task used to load the subway status.
	 */
	private StmInfoStatusApiReader statusTask;
	/**
	 * The task used to find the closest stations.
	 */
	private ClosestSubwayStationsFinderTask closestStationsTask;
	/**
	 * The current service status.
	 */
	private List<ServiceStatus> serviceStatuses = null;
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
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;
	/**
	 * The favorites subway stations IDs.
	 */
	private List<String> favStationIds;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_tab);

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
		findViewById(R.id.subway_status_section_refresh_or_stop_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshStatus(v);
			}
		});
		findViewById(R.id.closest_subway_stations_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshClosestStations(v);
			}
		});
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
		UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_TAB, 3);
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		// IF location updates should be enabled DO
		if (!this.locationUpdatesEnabled) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					return LocationUtils.getBestLastKnownLocation(SubwayTab.this);
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
					SubwayTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(SubwayTab.this, SubwayTab.this,
							SubwayTab.this.locationUpdatesEnabled, SubwayTab.this.paused);
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		refreshFavoriteIDsFromDB();
		adaptToScreenSize(getResources().getConfiguration());
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
	public void onCompass() {
		// MyLog.v(TAG, "onCompass()");
		if (this.accelerometerValues != null && this.magneticFieldValues != null) {
			updateCompass(SensorUtils.calculateOrientation(this, this.accelerometerValues, this.magneticFieldValues), false);
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	private void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation[0]);
		if (this.closestStations == null) {
			// MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							SubwayTab.this.lastCompassInDegree = (int) orientation;
							SubwayTab.this.lastCompassChanged = now;
							// update the view
							notifyDataSetChanged(false);
						}
					}
				});
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
		if (this.hasFocus && !this.shakeHandled && !TextUtils.isEmpty(this.closestStationId)) {
			Toast.makeText(this, R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.closestStationId);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, findStationName(this.closestStationId));
			startActivity(intent);
			this.shakeHandled = true;
		}
	}

	/**
	 * @param stationId a subway station ID
	 * @return a subway station name or null
	 */
	private String findStationName(String stationId) {
		if (this.closestStations == null) {
			return null;
		}
		for (SubwayStation subwayStation : this.closestStations) {
			if (subwayStation.getId().equals(stationId)) {
				return subwayStation.getName();
			}
		}
		return null;
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.compassUpdatesEnabled = false;
		}
		super.onPause();
	}

	/**
	 * Refresh all the UI.
	 */
	private void showAll() {
		if (findViewById(R.id.closest_stations) == null) { // IF NOT present/inflated DO
			((ViewStub) findViewById(R.id.closest_stations_stub)).inflate(); // inflate
		}
		ListView closestStationsListView = (ListView) findViewById(R.id.closest_stations);
		closestStationsListView.setOnItemClickListener(this);
		closestStationsListView.setOnScrollListener(this);
		this.adapter = new ArrayAdapterWithCustomView(this, R.layout.subway_tab_closest_stations_list_item);
		closestStationsListView.setAdapter(this.adapter);
		refreshSubwayLinesFromDB();
		showStatusFromDB();
		showClosestStations();
	}

	/**
	 * Refresh the subway lines.
	 */
	private void refreshSubwayLinesFromDB() {
		new AsyncTask<Void, Void, List<SubwayLine>>() {
			@Override
			protected List<SubwayLine> doInBackground(Void... params) {
				return StmManager.findAllSubwayLinesList(SubwayTab.this.getContentResolver());
			}

			@Override
			protected void onPostExecute(List<SubwayLine> result) {
				LinearLayout subwayLinesLayout = (LinearLayout) findViewById(R.id.subway_lines);
				int i = 0;
				for (SubwayLine subwayLine : result) {
					// create view
					View view = subwayLinesLayout.getChildAt(i++);
					// subway line type image
					final int lineNumber = subwayLine.getNumber();
					// subway line colors
					int color = SubwayUtils.getSubwayLineColor(lineNumber);
					view.findViewById(R.id.subway_img_bg).setBackgroundColor(color);

					final String subwayLineNumberS = String.valueOf(lineNumber);
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(SubwayTab.this, SupportFactory.get().getSubwayLineInfoClass());
							intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, subwayLineNumberS);
							startActivity(intent);
						}
					});
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							new SubwayLineSelectDirection(SubwayTab.this, lineNumber).showDialog();
							return true;
						}
					});
				}
				findViewById(R.id.subway_lines_loading).setVisibility(View.GONE);
				findViewById(R.id.subway_lines).setVisibility(View.VISIBLE);
			}
		}.execute();
	}

	/**
	 * Show the subway status. Try to load the subway status if not exist
	 */
	public void showStatusFromDB() {
		MyLog.v(TAG, "showStatusFromDB()");
		new AsyncTask<Void, Void, List<ServiceStatus>>() {
			@Override
			protected List<ServiceStatus> doInBackground(Void... params) {
				// get the latest service status in the local database or NULL
				return DataManager.findLatestServiceStatuses(getContentResolver(), Utils.getSupportedUserLocale());
			}

			@Override
			protected void onPostExecute(List<ServiceStatus> result) {
				// MyLog.v(TAG, "showStatusFromDB()>onPostExecute()");
				SubwayTab.this.serviceStatuses = result;
				// IF there is no service status DO
				if (SubwayTab.this.serviceStatuses == null) {
					// look for new service status
					refreshStatus();
				} else {
					// show latest service status
					showNewStatus();
					// check service age
					// IF the latest service is too old DO
					if (SubwayTab.this.serviceStatuses.size() > 0) {
						int statusTooOldInSec = STATUS_TOO_OLD_IN_SEC;
						if (SubwayTab.this.serviceStatuses.get(0).getType() != ServiceStatus.STATUS_TYPE_GREEN) { // IF status not OK
							statusTooOldInSec /= 2; // check twice as often
						}
						if (Utils.currentTimeSec() >= SubwayTab.this.serviceStatuses.get(0).getReadDate() + statusTooOldInSec) {
							// look for new service status
							refreshStatus();
						}
					}
				}
			}

		}.execute();
	}

	/**
	 * Show the new status.
	 */
	private void showNewStatus() {
		MyLog.v(TAG, "showNewStatus()");
		// hide loading (progress bar)
		if (findViewById(R.id.subway_status_loading) != null) { // IF present/inflated DO
			findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
		}

		if (this.serviceStatuses != null && this.serviceStatuses.size() > 0) {
			if (findViewById(R.id.subway_status) == null) { // IF NOT present/inflated DO
				((ViewStub) findViewById(R.id.subway_status_stub)).inflate(); // inflate
			}
			View statusLayout = findViewById(R.id.subway_status);
			TextView statusTv = (TextView) statusLayout.findViewById(R.id.subway_status_message);
			ImageView statusImg = (ImageView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section_logo);
			// set the status title with the date
			CharSequence readTime = Utils.formatSameDayDateInSec(this.serviceStatuses.get(0).getReadDate());
			final String sectionTitle = getString(R.string.subway_status_hour, readTime);
			((TextView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section)).setText(sectionTitle);
			// show message
			statusLayout.setVisibility(View.VISIBLE);
			// set the status message text
			SpannableStringBuilder sb = new SpannableStringBuilder();
			boolean allGreen = true;
			for (ServiceStatus status : this.serviceStatuses) {
				if (allGreen || status.getType() != ServiceStatus.STATUS_TYPE_GREEN) {
					if (sb.length() > 0) {
						sb.append("\n");
					}
					sb.append(status.getMessage().replaceAll("\\.\\ ", ".\n"));
				}
				if (status.getType() == ServiceStatus.STATUS_TYPE_GREEN) {
					break; // only 1 green
				} else if (status.getType() != ServiceStatus.STATUS_TYPE_GREEN) {
					allGreen = false; // only non-green
				}
			}
			final CharSequence message = colorize(sb);
			statusTv.setText(message);
			if (allGreen) {
				statusImg.setImageResource(R.drawable.ic_btn_info_details);
			} else {
				statusImg.setImageResource(R.drawable.ic_btn_alert);
			}
		}
	}

	private CharSequence colorize(CharSequence message) {
		SpannableStringBuilder sb = new SpannableStringBuilder(message);
		String messageS = message.toString().toLowerCase(Locale.ENGLISH);
		String green = getString(R.string.green_line_short).toLowerCase(Locale.ENGLISH);
		String orange = getString(R.string.orange_line_short).toLowerCase(Locale.ENGLISH);
		String yellow = getString(R.string.yellow_line_short).toLowerCase(Locale.ENGLISH);
		String blue = getString(R.string.blue_line_short).toLowerCase(Locale.ENGLISH);
		if (messageS.contains(green)) {
			final ForegroundColorSpan fcs = new ForegroundColorSpan(SubwayUtils.getSubwayLineColor(SubwayUtils
					.findSubwayLineNumberFromShortName(R.string.green_line_short)));
			int start = messageS.indexOf(green);
			int end = start + green.length();
			sb.setSpan(fcs, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		}
		if (messageS.contains(orange)) {
			final ForegroundColorSpan fcs = new ForegroundColorSpan(SubwayUtils.getSubwayLineColor(SubwayUtils
					.findSubwayLineNumberFromShortName(R.string.orange_line_short)));
			int start = messageS.indexOf(orange);
			int end = start + orange.length();
			sb.setSpan(fcs, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		}
		if (messageS.contains(yellow)) {
			final ForegroundColorSpan fcs = new ForegroundColorSpan(SubwayUtils.getSubwayLineColor(SubwayUtils
					.findSubwayLineNumberFromShortName(R.string.yellow_line_short)));
			int start = messageS.indexOf(yellow);
			int end = start + yellow.length();
			sb.setSpan(fcs, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		}
		if (messageS.contains(blue)) {
			final ForegroundColorSpan fcs = new ForegroundColorSpan(SubwayUtils.getSubwayLineColor(SubwayUtils
					.findSubwayLineNumberFromShortName(R.string.blue_line_short)));
			int start = messageS.indexOf(blue);
			int end = start + blue.length();
			sb.setSpan(fcs, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		}
		return sb;
	}

	/**
	 * Start the refresh status task if not running.
	 */
	public void refreshStatus() {
		MyLog.v(TAG, "refreshStatus()");
		// IF the task is NOT already running DO
		if (this.statusTask == null || !this.statusTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setStatusLoading();
			// read the subway status
			this.statusTask = new StmInfoStatusApiReader(this, this);
			this.statusTask.execute();
		}
	}

	/**
	 * Refresh or not refresh the status depending of the task status.
	 * @param v
	 */
	public void refreshOrStopRefreshStatus(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshStatus()");
		// IF the task is running DO
		if (this.statusTask != null && this.statusTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.statusTask.cancel(true);
			this.statusTask = null;
			setStatusCancelled();
		} else {
			refreshStatus();
		}
	}

	/**
	 * Set the status as loading.
	 */
	private void setStatusLoading() {
		MyLog.v(TAG, "setStatusLoading()");
		if (this.serviceStatuses == null) {
			// set the BIG loading message
			if (findViewById(R.id.subway_status) != null) { // IF present/inflated DO
				// hide the status layout
				findViewById(R.id.subway_status).setVisibility(View.GONE);
			}
			// clean the status
			((TextView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section)).setText(R.string.subway_status);
			// show the loading layout
			if (findViewById(R.id.subway_status_loading) == null) { // IF NOT present/inflated DO
				((ViewStub) findViewById(R.id.subway_status_loading_stub)).inflate(); // inflate
			}
			findViewById(R.id.subway_status_loading).setVisibility(View.VISIBLE);
			// set the progress bar
			TextView progressBarLoading = (TextView) findViewById(R.id.subway_status_loading).findViewById(R.id.detail_msg);
			String loadingMsg = getString(R.string.downloading_data_from_and_source, StmInfoStatusApiReader.SOURCE);
			progressBarLoading.setText(loadingMsg);
			progressBarLoading.setVisibility(View.VISIBLE);
			// } else { // just notify the user ?
		}
		// show stop icon instead of refresh
		findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section_refresh_or_stop_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		findViewById(R.id.subway_status_title).findViewById(R.id.progress_bar_status).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the status as not loading.
	 */
	private void setStatusNotLoading() {
		MyLog.v(TAG, "setStatusNotLoading()");
		// show refresh icon instead of loading
		findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section_refresh_or_stop_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		findViewById(R.id.subway_status_title).findViewById(R.id.progress_bar_status).setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the status as cancelled.
	 */
	private void setStatusCancelled() {
		MyLog.v(TAG, "setStatusCancelled()");
		// IF there is already a status DO
		if (this.serviceStatuses != null) {
			// notify the user but keep showing the old status ?
			Utils.notifyTheUser(this, getString(R.string.subway_status_loading_cancelled));
		} else {
			showStatusMessage(getString(R.string.subway_status_loading_cancelled));
			// hide loading (progress bar)
			if (findViewById(R.id.subway_status_loading) != null) { // IF present/inflated DO
				findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
			}
		}
		setStatusNotLoading();
	}

	private void showStatusMessage(String message) {
		if (findViewById(R.id.subway_status) == null) { // IF NOT present/inflated DO
			((ViewStub) findViewById(R.id.subway_status_stub)).inflate(); // inflate
		}
		View statusLayout = findViewById(R.id.subway_status);
		TextView statusTv = (TextView) statusLayout.findViewById(R.id.subway_status_message);
		statusTv.setText(message);
		statusLayout.setVisibility(View.VISIBLE);
	}

	/**
	 * Set the status as error.
	 * @param errorMessage the error message
	 */
	private void setStatusError(String errorMessage) {
		MyLog.v(TAG, "setStatusError(%s)", errorMessage);
		// IF there is already a status DO
		if (this.serviceStatuses != null) {
			// notify the user but keep showing the old status
			Utils.notifyTheUser(this, errorMessage);
		} else {
			// show the BIG error message
			// hide loading (progress bar)
			if (findViewById(R.id.subway_status_loading) != null) { // IF present/inflated DO
				findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
			}
			// show message
			showStatusMessage(errorMessage);
		}
		setStatusNotLoading();
	}

	@Override
	public void onStmInfoStatusesLoaded(String errorMessage) {
		MyLog.v(TAG, "onStmInfoStatusesLoaded(%s)", errorMessage);
		// IF there is an error message DO
		if (errorMessage != null) {
			// update the BIG message
			setStatusError(errorMessage);
		} else {
			// notify the user ?
			new AsyncTask<Void, Void, List<ServiceStatus>>() {
				@Override
				protected List<ServiceStatus> doInBackground(Void... params) {
					// get the latest service status in the local database or NULL
					return DataManager.findLatestServiceStatuses(getContentResolver(), Utils.getSupportedUserLocale());
				}

				@Override
				protected void onPostExecute(List<ServiceStatus> result) {
					// MyLog.v(TAG, "onStmInfoStatusesLoaded()>onPostExecute()");
					// get the latest service status in the local database or NULL
					SubwayTab.this.serviceStatuses = result;
					// show latest service status
					showNewStatus();
					setStatusNotLoading();
				}
			}.execute();
		}
	}

	/**
	 * Show the subway status info dialog.
	 * @param v useless - can be null
	 */
	public void showSubwayStatusInfoDialog(View v) {
		MyLog.v(TAG, "showSubwayStatusInfoDialog()");
		TextView messageTv = new TextView(this);
		messageTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		messageTv.setPadding(15, 15, 15, 15); // TODO custom dialog
		messageTv.setText(getString(R.string.subway_status_message));
		// Linkify.addLinks(messageTv, Linkify.WEB_URLS);
		new AlertDialog.Builder(this).setTitle(getString(R.string.subway_status)).setIcon(R.drawable.ic_btn_info_details).setView(messageTv)
				.setPositiveButton(getString(android.R.string.ok), null).setCancelable(true).create().show();
	}

	/**
	 * Show the closest stations UI.
	 */
	public void showClosestStations() {
		MyLog.v(TAG, "showClosestStations()");
		// enable location updates
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		// IF there is no closest stations DO
		if (this.closestStations == null) {
			// look for the closest stations
			refreshClosestStations();
		} else {
			// show the closest stations
			showNewClosestStations();
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestStationsLocation)) {
				// start refreshing
				refreshClosestStations();
			}
		}
	}

	/**
	 * The subway station list adapter.
	 */
	private ArrayAdapter<ASubwayStation> adapter;

	/**
	 * Show the new closest stations.
	 */
	private void showNewClosestStations() {
		MyLog.v(TAG, "showNewClosestStations()");
		if (this.closestStations != null) {
			// set the closest station title
			showNewClosestStationsTitle();
			// hide loading
			if (findViewById(R.id.closest_stations_loading) != null) { // IF inflated/present DO
				findViewById(R.id.closest_stations_loading).setVisibility(View.GONE); // hide
			}
			if (findViewById(R.id.closest_stations) == null) { // IF NOT present/inflated DO
				((ViewStub) findViewById(R.id.closest_stations_stub)).inflate(); // inflate
			}
			// show stations list
			notifyDataSetChanged(true);
			findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
			setClosestStationsNotLoading();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s, %s,%s,%s)", parent.getId(), view.getId(), position, id);
		if (this.closestStations != null && position < this.closestStations.size() && this.closestStations.get(position) != null) {
			Intent intent = new Intent(this, SubwayStationInfo.class);
			SubwayStation selectedStation = this.closestStations.get(position);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, selectedStation.getId());
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, selectedStation.getName());
			startActivity(intent);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		MyLog.v(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
		adaptToScreenSize(newConfig);
	}

	private void adaptToScreenSize(Configuration configuration) {
		if (SupportFactory.get().isScreenHeightSmall(configuration)) {
			// HIDE AD
			if (findViewById(R.id.ad_layout) != null) {
				findViewById(R.id.ad_layout).setVisibility(View.GONE); // not enough space on phone
			}
			// HIDE STATUS
			findViewById(R.id.subway_status_title).setVisibility(View.GONE);
			// if (findViewById(R.id.subway_status) != null) { // IF present/inflated DO
			// ((TextView) findViewById(R.id.subway_status)).setSingleLine(true);
			// }
			// HIDE LINE TITLE
			findViewById(R.id.subway_line).setVisibility(View.GONE);
		} else {
			// SHOW AD
			AdsUtils.setupAd(this);
			// SHOW STATUS
			findViewById(R.id.subway_status_title).setVisibility(View.VISIBLE);
			if (findViewById(R.id.subway_status) == null) { // IF NOT present/inflated DO
				((ViewStub) findViewById(R.id.subway_status_stub)).inflate(); // inflate
			}
			// if (this.serviceStatuses != null && this.serviceStatuses.size() > 0 && this.serviceStatuses.get(0).getType() != ServiceStatus.STATUS_TYPE_GREEN)
			// {
			// ((TextView) findViewById(R.id.subway_status)).setMaxLines(2);
			// }
			// SHOW LINE TITLE
			findViewById(R.id.subway_line).setVisibility(View.VISIBLE);
		}
	}

	static class ViewHolder {
		TextView placeTv;
		TextView stationNameTv;
		TextView distanceTv;
		ImageView subwayImg1;
		ImageView subwayImg2;
		ImageView subwayImg3;
		ImageView favImg;
		ImageView compassImg;
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
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 * @param subwayStations the stations
		 */
		public ArrayAdapterWithCustomView(Context context, int viewId) {
			super(context, viewId);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return SubwayTab.this.closestStations == null ? 0 : SubwayTab.this.closestStations.size();
		}

		@Override
		public int getPosition(ASubwayStation item) {
			return SubwayTab.this.closestStations.indexOf(item);
		}

		@Override
		public ASubwayStation getItem(int position) {
			return SubwayTab.this.closestStations.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.placeTv = (TextView) convertView.findViewById(R.id.place);
				holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				holder.subwayImg1 = (ImageView) convertView.findViewById(R.id.subway_img_1);
				holder.subwayImg2 = (ImageView) convertView.findViewById(R.id.subway_img_2);
				holder.subwayImg3 = (ImageView) convertView.findViewById(R.id.subway_img_3);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ASubwayStation station = getItem(position);
			if (station != null) {
				// subway station name
				holder.stationNameTv.setText(station.getName());
				// station lines color
				if (station.getOtherLinesId() != null && station.getOtherLinesId().size() > 0) {
					int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(0));
					holder.subwayImg1.setVisibility(View.VISIBLE);
					holder.subwayImg1.setImageResource(subwayLineImg1);
					if (station.getOtherLinesId().size() > 1) {
						int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(1));
						holder.subwayImg2.setVisibility(View.VISIBLE);
						holder.subwayImg2.setImageResource(subwayLineImg2);
						if (station.getOtherLinesId().size() > 2) {
							int subwayLineImg3 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(2));
							holder.subwayImg3.setVisibility(View.VISIBLE);
							holder.subwayImg3.setImageResource(subwayLineImg3);
						} else {
							holder.subwayImg3.setVisibility(View.GONE);
						}
					} else {
						holder.subwayImg2.setVisibility(View.GONE);
						holder.subwayImg3.setVisibility(View.GONE);
					}
				} else {
					holder.subwayImg1.setVisibility(View.GONE);
					holder.subwayImg2.setVisibility(View.GONE);
					holder.subwayImg3.setVisibility(View.GONE);
				}
				// distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					holder.distanceTv.setText(station.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE);
					holder.distanceTv.setText(null);
				}
				// favorite
				if (SubwayTab.this.favStationIds != null && SubwayTab.this.favStationIds.contains(station.getId())) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// closest bike station
				int index = -1;
				if (!TextUtils.isEmpty(SubwayTab.this.closestStationId)) {
					index = station.getId().equals(SubwayTab.this.closestStationId) ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
					break;
				default:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow);
					break;
				}
				// compass
				if (location != null && lastCompassInDegree != 0) {
					float compassRotation = SensorUtils.getCompassRotationInDegree(location, station, lastCompassInDegree, locationDeclination);
					SupportFactory.get().rotateImageView(holder.compassImg, compassRotation, SubwayTab.this);
					// holder.compassImg.setImageMatrix(station.getCompassMatrix());
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE);
				}
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
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	/**
	 * Show new closest stations title.
	 */
	public void showNewClosestStationsTitle() {
		if (this.closestStationsLocationAddress != null && this.closestStationsLocation != null) {
			((TextView) findViewById(R.id.closest_stations_title).findViewById(R.id.closest_subway_stations)).setText(this.closestStationsLocationAddress);
		}
	}

	/**
	 * Start the refresh closest stations tasks if necessary.
	 */
	private void refreshClosestStations() {
		MyLog.v(TAG, "refreshClosestStations()");
		// IF the task is NOT already running DO
		if (this.closestStationsTask == null || !this.closestStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setClosestStationsLoading();
			// IF location found DO
			Location currentLocation = getLocation();
			if (currentLocation != null) {
				// find the closest stations
				this.closestStationsTask = new ClosestSubwayStationsFinderTask(this, this, SupportFactory.get().getNbClosestPOIDisplay());
				this.closestStationsTask.execute(currentLocation);
				this.closestStationsLocation = currentLocation;
				new AsyncTask<Location, Void, String>() {

					@Override
					protected String doInBackground(Location... locations) {
						Address address = LocationUtils.getLocationAddress(SubwayTab.this, locations[0]);
						if (address == null || SubwayTab.this.closestStationsLocation == null) {
							return null;
						}
						return LocationUtils.getLocationString(SubwayTab.this, R.string.closest_subway_stations, address,
								SubwayTab.this.closestStationsLocation.getAccuracy());
					}

					@Override
					protected void onPostExecute(String result) {
						boolean refreshRequired = SubwayTab.this.closestStationsLocationAddress == null;
						SubwayTab.this.closestStationsLocationAddress = result;
						if (refreshRequired) {
							showNewClosestStationsTitle();
						}
					}

				}.execute(this.closestStationsLocation);
			}
			// ELSE wait for location...
		}
	}

	/**
	 * Refresh or stop refresh the closest stations depending if running.
	 * @param v a view (not used)
	 */
	public void refreshOrStopRefreshClosestStations(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshClosestStations()");
		// IF the task is running DO
		if (this.closestStationsTask != null && this.closestStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.closestStationsTask.cancel(true);
			this.closestStationsTask = null;
			setClosestStationsCancelled();
		} else {
			// refreshSubwayStatus();
			refreshClosestStations();
		}
	}

	/**
	 * Set the closest stations as loading.
	 */
	private void setClosestStationsLoading() {
		MyLog.v(TAG, "setClosestStationsLoading()");
		View closestStationsTitle = findViewById(R.id.closest_stations_title);
		if (this.closestStations == null) {
			// set the BIG loading message
			// remove last location from the list divider
			((TextView) closestStationsTitle.findViewById(R.id.closest_subway_stations)).setText(R.string.closest_subway_stations);
			if (findViewById(R.id.closest_stations) != null) { // IF inflated/present DO
				// hide the list
				findViewById(R.id.closest_stations).setVisibility(View.GONE);
				// clean the list (useful ?)
				// ((ListView) findViewById(R.id.closest_stations)).removeAllViews();
			}
			// show loading
			if (findViewById(R.id.closest_stations_loading) == null) { // IF NOT inflated/present DO
				((ViewStub) findViewById(R.id.closest_stations_loading_stub)).inflate(); // inflate
			}
			findViewById(R.id.closest_stations_loading).setVisibility(View.VISIBLE);
			// show waiting for location
			TextView detailMsgTv = (TextView) findViewById(R.id.closest_stations_loading).findViewById(R.id.detail_msg);
			detailMsgTv.setText(R.string.waiting_for_location_fix);
			detailMsgTv.setVisibility(View.VISIBLE);
			// } else { just notify the user ?
		}
		// show stop icon instead of refresh
		closestStationsTitle.findViewById(R.id.closest_subway_stations_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		closestStationsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the closest stations as not loading.
	 */
	private void setClosestStationsNotLoading() {
		MyLog.v(TAG, "setClosestStationsNotLoading()");
		View closestStationsTitle = findViewById(R.id.closest_stations_title);
		// show refresh icon instead of loading
		closestStationsTitle.findViewById(R.id.closest_subway_stations_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		closestStationsTitle.findViewById(R.id.progress_bar_closest).setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the closest stations as cancelled.
	 */
	private void setClosestStationsCancelled() {
		MyLog.v(TAG, "setClosestStationsCancelled()");
		if (this.closestStations == null) {
			// update the BIG cancel message
			TextView cancelMsgTv = new TextView(this);
			cancelMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cancelMsgTv.setText(getString(R.string.closest_subway_stations_cancelled));
			// ((ListView) findViewById(R.id.closest_stations)).addView(cancelMsgTv);
			// hide loading
			findViewById(R.id.closest_stations_loading).setVisibility(View.GONE);
			findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
		}
		setClosestStationsNotLoading();
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStationsError() {
		MyLog.v(TAG, "setClosestStationsError()");
		// IF there are already stations DO
		if (this.closestStations != null) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUser(this, getString(R.string.closest_subway_stations_error));
		} else {
			// show the BIG message
			TextView cancelMsgTv = new TextView(this);
			cancelMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cancelMsgTv.setText(getString(R.string.closest_subway_stations_error));
			// ((ListView) findViewById(R.id.closest_stations)).addView(cancelMsgTv);
			// hide loading
			findViewById(R.id.closest_stations_loading).setVisibility(View.GONE);
			findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
		}
		setClosestStationsNotLoading();
	}

	@Override
	public void onClosestStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress(%s)", progress);
		// if (this.closestStations != null) {notify the user ?
		if (this.closestStations == null) {
			// update the BIG message
			findViewById(R.id.closest_stations_loading).setVisibility(View.VISIBLE);
			TextView detailMsgTv = (TextView) findViewById(R.id.closest_stations_loading).findViewById(R.id.detail_msg);
			detailMsgTv.setText(progress);
			detailMsgTv.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onClosestStationsDone(ClosestPOI<ASubwayStation> result) {
		MyLog.v(TAG, "onClosestStationsDone()");
		if (result == null || result.getPoiListOrNull() == null) {
			// show the error
			setClosestStationsError();
		} else {
			// get the result
			this.closestStations = result.getPoiList();
			updateCompass(this.lastCompassInDegree, true);
			generateOrderedStationsIds();
			refreshFavoriteIDsFromDB();
			// shot the result
			showNewClosestStations();
			setClosestStationsNotLoading();
		}
	}

	/**
	 * Find favorites subways stations IDs.
	 */
	private void refreshFavoriteIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false; // don't trigger update if favorites are the same
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(SubwayTab.this.favStationIds)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavUIDs = new ArrayList<String>();
				for (Fav subwayStationFav : result) {
					if (SubwayTab.this.favStationIds == null || !SubwayTab.this.favStationIds.contains(subwayStationFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavUIDs.add(subwayStationFav.getFkId()); // store station ID
				}
				SubwayTab.this.favStationIds = newfavUIDs;
				// trigger change if necessary
				if (newFav) {
					notifyDataSetChanged(true);
				}
			};
		}.execute();
	}

	/**
	 * Update the subway stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		Location currentLocation = getLocation();
		if (currentLocation == null) {
			return;
		}
		// IF no closest stations AND new location DO
		if (this.closestStations == null) {
			// start refreshing if not running.
			refreshClosestStations();
			return;
		}
		// ELSE IF there are closest stations AND new location DO
		if (this.closestStations != null) {
			// update the list distances
			LocationUtils.updateDistance(this, this.closestStations, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					// update the view
					String previousClosest = SubwayTab.this.closestStationId;
					generateOrderedStationsIds();
					notifyDataSetChanged(SubwayTab.this.closestStationId == null ? false : SubwayTab.this.closestStationId.equals(previousClosest));
				}
			});
		}
	}

	/**
	 * The subway stations IDs ordered by distance (closest first).
	 */
	private String closestStationId;
	private float locationDeclination;

	/**
	 * Generate the ordered subway line station IDs.
	 */
	public void generateOrderedStationsIds() {
		MyLog.v(TAG, "generateOrderedStationsIds()");
		List<ASubwayStation> orderedStations = new ArrayList<ASubwayStation>(this.closestStations);
		// order the stations list by distance (closest first)
		Collections.sort(orderedStations, new Comparator<ASubwayStation>() {
			@Override
			public int compare(ASubwayStation lhs, ASubwayStation rhs) {
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
		this.closestStationId = orderedStations.get(0).getDistance() > 0 ? orderedStations.get(0).getId() : null;
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
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
	 * @return the location
	 */
	public Location getLocation() {
		if (this.location == null) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					// MyLog.v(TAG, "doInBackground()");
					return LocationUtils.getBestLastKnownLocation(SubwayTab.this);
				}

				@Override
				protected void onPostExecute(Location result) {
					// MyLog.v(TAG, "onPostExecute()");
					if (result != null) {
						SubwayTab.this.setLocation(result);
					}
					// enable location updates if necessary
					SubwayTab.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(SubwayTab.this, SubwayTab.this,
							SubwayTab.this.locationUpdatesEnabled, SubwayTab.this.paused);
				}

			}.execute();
		}
		return this.location;
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

	// /**
	// * Login or Logout from Twitter.
	// * @param v the view (not used)
	// */
	// public void twitterLoginOrLogout(View v) {
	// MyLog.v(TAG, "twitterLoginOrLogout()");
	// if (TwitterUtils.isConnected(this)) {
	// // disconnect
	// TwitterUtils.logout(this);
	// } else {
	// // try to connect
	// TwitterUtils.getInstance().startLoginProcess(this);
	// }
	// }

	/**
	 * Show the STM subway map.
	 * @param v the view (not used)
	 */
	public void showSTMSubwayMap(View v) {
		SubwayUtils.showSTMSubwayMap(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.subway_tab_menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (super.onPrepareOptionsMenu(menu)) {
			// // TWITTER disabled for now (not required to get subway status anymore)
			// menu.findItem(R.id.twitter).setTitle(TwitterUtils.isConnected(this) ? R.string.menu_twitter_logout : R.string.menu_twitter_login);
			return true;
		} else {
			MyLog.w(TAG, "Error in onPrepareOptionsMenu().");
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.twitter: disabled for now (not required to get subway status anymore)
		// twitterLoginOrLogout(null);
		// return true;
		case R.id.stm_map:
			showSTMSubwayMap(null);
			return true;
		default:
			return MenuUtils.handleCommonMenuActions(this, item);
		}
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.statusTask != null) {
			this.statusTask.cancel(true);
			this.statusTask = null;
		}
		if (this.closestStationsTask != null) {
			this.closestStationsTask.cancel(true);
			this.closestStations = null;
		}
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}
}

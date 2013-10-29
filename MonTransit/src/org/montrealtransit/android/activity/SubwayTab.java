package org.montrealtransit.android.activity;

import java.util.List;
import java.util.Locale;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask.ClosestSubwayStationsFinderListener;
import org.montrealtransit.android.services.StmInfoStatusApiReader;
import org.montrealtransit.android.services.StmInfoStatusReaderListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
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
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Display a list of subway lines.
 * @author Mathieu Méa
 */
public class SubwayTab extends Activity implements LocationListener, StmInfoStatusReaderListener, ClosestSubwayStationsFinderListener, SensorEventListener,
		ShakeListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Subways";

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
	private boolean shakeUpdatesEnabled = false;
	/**
	 * The subway station list adapter.
	 */
	private POIArrayAdapter adapter;
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
						if (SubwayTab.this.closestStationsLocation != null) {
							if (LocationUtils.isMoreRelevant(SubwayTab.this.closestStationsLocation, result, LocationUtils.SIGNIFICANT_ACCURACY_IN_METERS,
									Utils.CLOSEST_POI_LIST_PREFER_ACCURACY_OVER_TIME)
									&& LocationUtils.isTooOld(SubwayTab.this.closestStationsLocation, Utils.CLOSEST_POI_LIST_TIMEOUT)) {
								SubwayTab.this.adapter.setPois(null); // force refresh
							}
						}
						// set the new distance
						setLocation(result);
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
		if (this.hasFocus && !this.shakeHandled && this.adapter.hasClosestPOI()) {
			Toast.makeText(this, R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			this.adapter.showClosestPOI();
			this.shakeHandled = true;
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
		this.adapter.onPause();
		super.onPause();
	}

	/**
	 * Refresh all the UI.
	 */
	private void showAll() {
		this.adapter = new POIArrayAdapter(this);
		this.adapter.setShakeEnabled(true);
		this.adapter.setManualLayout((ViewGroup) findViewById(R.id.closest_stations));
		this.adapter.setManualScrollView((ScrollView) findViewById(R.id.scrollview));
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
				return StmManager.findAllSubwayLinesList(SubwayTab.this);
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
				// IF there is no service status OR service status is way too old to be useful DO
				if (result == null || Utils.currentTimeSec() >= result.get(0).getReadDate() + SubwayUtils.STATUS_NOT_USEFUL_IN_SEC) {
					setStatusLoading();
					// look for new service status
					refreshStatus();
					return;
				}
				SubwayTab.this.serviceStatuses = result;
				// show latest service status
				showNewStatus();
				// check service age
				// IF the latest service is too old DO
				if (SubwayTab.this.serviceStatuses.size() > 0) {
					int statusTooOldInSec = SubwayUtils.STATUS_TOO_OLD_IN_SEC;
					if (SubwayTab.this.serviceStatuses.get(0).getType() != ServiceStatus.STATUS_TYPE_GREEN) { // IF status not OK
						statusTooOldInSec /= 3; // check more often
					}
					if (Utils.currentTimeSec() >= SubwayTab.this.serviceStatuses.get(0).getReadDate() + statusTooOldInSec) {
						// look for new service status
						refreshStatus();
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
			// // set the BIG loading message
			// if (findViewById(R.id.subway_status) != null) { // IF present/inflated DO
			// // hide the status layout
			// findViewById(R.id.subway_status).setVisibility(View.GONE);
			// }
			// // clean the status
			// ((TextView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section)).setText(R.string.subway_status);
			// // show the loading layout
			// if (findViewById(R.id.subway_status_loading) == null) { // IF NOT present/inflated DO
			// ((ViewStub) findViewById(R.id.subway_status_loading_stub)).inflate(); // inflate
			// }
			// findViewById(R.id.subway_status_loading).setVisibility(View.VISIBLE);
			// // set the progress bar
			// TextView progressBarLoading = (TextView) findViewById(R.id.subway_status_loading).findViewById(R.id.detail_msg);
			// String loadingMsg = getString(R.string.downloading_data_from_and_source, StmInfoStatusApiReader.SOURCE);
			// progressBarLoading.setText(loadingMsg);
			// progressBarLoading.setVisibility(View.VISIBLE);
			showStatusMessage(getString(R.string.downloading_data_from_and_source, StmInfoStatusApiReader.SOURCE));
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
		if (this.adapter.getPois() == null) {
			// look for the closest stations
			refreshClosestStations();
		} else {
			// show the closest stations
			showNewClosestStations(false);
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestStationsLocation)) {
				// start refreshing
				refreshClosestStations();
			}
		}
	}

	/**
	 * Show the new closest stations.
	 */
	private void showNewClosestStations(boolean scroll) {
		MyLog.v(TAG, "showNewClosestStations(%s)", scroll);
		if (this.adapter.getPois() != null) {
			// set the closest station title
			showNewClosestStationsTitle();
			// hide loading
			if (findViewById(R.id.closest_stations_loading) != null) { // IF inflated/present DO
				findViewById(R.id.closest_stations_loading).setVisibility(View.GONE); // hide
			}
			// show stations list
			this.adapter.initManual();
			if (scroll) {
				this.adapter.scrollManualScrollViewTo(0, 0);
			}
			findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
			setClosestStationsNotLoading();
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

	@Override
	public void onClosestStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress(%s)", progress);
		if (this.adapter.getPois() == null) { // notify the user ?
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
			// this.closestStations = result.getPoiList();
			this.adapter.setPois(result.getPoiList());
			this.adapter.updateClosestPoi();
			this.adapter.updateCompassNow();
			// updateCompass(this.lastCompassInDegree, true);
			// generateOrderedStationsIds();
			refreshFavoriteIDsFromDB();
			// show the result
			showNewClosestStations(LocationUtils.areTheSame(this.closestStationsLocation, result.getLat(), result.getLng()));
			setClosestStationsNotLoading();
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
		if (this.adapter.getPois() == null) {
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
		if (this.adapter.getPois() == null) {
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
		if (this.adapter.getPois() != null) {
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
				SubwayTab.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.adapter.setLocation(this.location);
				if (!this.shakeUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(this, this);
					this.shakeHandled = false;
				}
				if (this.adapter.getPois() == null) {
					// start refreshing if not running.
					refreshClosestStations();
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
			if (this.adapter != null) {
				this.adapter.setPois(null);
			}
		}
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}
}

package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.ClosestSubwayStations;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderListener;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask;
import org.montrealtransit.android.services.StmInfoStatusApiReader;
import org.montrealtransit.android.services.StmInfoStatusReaderListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Display a list of subway lines.
 * @author Mathieu Méa
 */
public class SubwayTab extends Activity implements LocationListener, StmInfoStatusReaderListener, ClosestSubwayStationsFinderListener {

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
	 * The closest stations.
	 */
	private ClosestSubwayStations closestStations;

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
	private ServiceStatus serviceStatus = null;

	/**
	 * The validity of the current status (in seconds).
	 */
	private static final int STATUS_TOO_OLD_IN_SEC = 20 * 60; // 20 minutes

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_tab);
		showAll();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
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
					LocationUtils.enableLocationUpdates(SubwayTab.this, SubwayTab.this);
				};

			}.execute();
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onPause();
	}

	/**
	 * Refresh all the UI.
	 */
	private void showAll() {
		refreshSubwayLinesFromDB();
		showStatusFromDB();
		showClosestStations();
	}

	/**
	 * Refresh the subway lines.
	 */
	private void refreshSubwayLinesFromDB() { // not asynchronous > more consistent
		List<SubwayLine> result = StmManager.findAllSubwayLinesList(SubwayTab.this.getContentResolver());
		LinearLayout subwayLinesLayout = (LinearLayout) findViewById(R.id.subway_line_list);
		int i = 0;
		for (SubwayLine subwayLine : result) {
			// create view
			View view = subwayLinesLayout.getChildAt(i++);
			// subway line type image
			final int lineNumber = subwayLine.getNumber();
			String subwayLineName = getString(SubwayUtils.getSubwayLineNameShort(lineNumber));
			((TextView) view.findViewById(R.id.line_name)).setText(subwayLineName);
			// subway line colors
			int color = SubwayUtils.getSubwayLineColor(lineNumber);
			((RelativeLayout) view.findViewById(R.id.subway_img_bg)).setBackgroundColor(color);

			final String subwayLineNumberS = String.valueOf(lineNumber);
			// add click listener
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick(%s)", v.getId());
					Intent intent = new Intent(SubwayTab.this, SubwayLineInfo.class);
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
	}

	/**
	 * Show the subway status. Try to load the subway status if not exist
	 */
	public void showStatusFromDB() {
		MyLog.v(TAG, "showStatusFromDB()");
		new AsyncTask<Void, Void, ServiceStatus>() {
			@Override
			protected ServiceStatus doInBackground(Void... params) {
				// get the latest service status in the local database or NULL
				return DataManager.findLatestServiceStatus(getContentResolver(), Utils.getSupportedUserLocale());
			}

			@Override
			protected void onPostExecute(ServiceStatus result) {
				SubwayTab.this.serviceStatus = result;
				// IF there is no service status DO
				if (SubwayTab.this.serviceStatus == null) {
					// look for new service status
					refreshStatus();
				} else {
					// show latest service status
					showNewStatus();
					// check service age
					int nowInSec = (int) (System.currentTimeMillis() / 1000);
					// IF the latest service is too old DO
					if (nowInSec >= SubwayTab.this.serviceStatus.getReadDate() + STATUS_TOO_OLD_IN_SEC) {
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
		findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
		if (this.serviceStatus != null) {
			View statusLayout = findViewById(R.id.subway_status);
			ImageView statusImg = (ImageView) statusLayout.findViewById(R.id.subway_status_img);
			// set the status title with the date
			CharSequence readTime = Utils.formatSameDayDateInSec(this.serviceStatus.getReadDate());
			final String sectionTitle = getString(R.string.subway_status_hour, readTime);
			((TextView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section)).setText(sectionTitle);
			// show message
			statusLayout.setVisibility(View.VISIBLE);
			// set the status message text
			((TextView) statusLayout.findViewById(R.id.subway_status_message)).setText(this.serviceStatus.getMessage());
			// set the status image (or not)
			int statusImgDrawable = android.R.drawable.ic_dialog_info;
			switch (this.serviceStatus.getType()) {
			case ServiceStatus.STATUS_TYPE_RED:
				statusImg.setVisibility(View.VISIBLE);
				statusImg.setImageResource(R.drawable.status_red);
				statusImgDrawable = R.drawable.status_red;
				break;
			case ServiceStatus.STATUS_TYPE_YELLOW:
				statusImg.setVisibility(View.VISIBLE);
				statusImg.setImageResource(R.drawable.status_yellow);
				statusImgDrawable = R.drawable.status_yellow;
				break;
			case ServiceStatus.STATUS_TYPE_GREEN:
				statusImg.setVisibility(View.VISIBLE);
				statusImg.setImageResource(R.drawable.status_green);
				statusImgDrawable = R.drawable.status_green;
				break;
			default:
				statusImg.setVisibility(View.GONE);
				break;
			}
			final int statusImgDrawable2 = statusImgDrawable;
			statusLayout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick()");
					new AlertDialog.Builder(SubwayTab.this).setIcon(statusImgDrawable2).setTitle(sectionTitle)
							.setMessage(SubwayTab.this.serviceStatus.getMessage()).setCancelable(true).setPositiveButton(getString(android.R.string.ok), null)
							.create().show();
				}
			});
		}
	}

	/**
	 * Start the refresh status task if not running.
	 */
	public void refreshStatus() {
		MyLog.v(TAG, "refreshStatus()");
		// IF the task is NOT already running DO
		if (this.statusTask == null || !this.statusTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setStatusLoading();
			// read the subway status from http://twitter.com/stminfo
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
		if (this.serviceStatus == null) {
			// set the BIG loading message
			// hide the status layout
			findViewById(R.id.subway_status).setVisibility(View.GONE);
			// clean the status
			((TextView) findViewById(R.id.subway_status_title).findViewById(R.id.subway_status_section)).setText(R.string.subway_status);
			// show the loading layout
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
		if (this.serviceStatus != null) {
			// notify the user but keep showing the old status ?
			Utils.notifyTheUser(this, getString(R.string.subway_status_loading_cancelled));
		} else {
			// show the BIG cancel message
			findViewById(R.id.subway_status).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.subway_status).findViewById(R.id.subway_status_message)).setText(getString(R.string.subway_status_loading_cancelled));
			// hide loading (progress bar)
			findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
		}
		setStatusNotLoading();
	}

	/**
	 * Set the status as error.
	 * @param errorMessage the error message
	 */
	private void setStatusError(String errorMessage) {
		MyLog.v(TAG, "setStatusError(%s)", errorMessage);
		// IF there is already a status DO
		if (this.serviceStatus != null) {
			// notify the user but keep showing the old status
			Utils.notifyTheUser(this, errorMessage);
		} else {
			// show the BIG error message
			// hide loading (progress bar)
			findViewById(R.id.subway_status_loading).setVisibility(View.GONE);
			// show message
			findViewById(R.id.subway_status).setVisibility(View.VISIBLE); // inflate ViewStub
			((TextView) findViewById(R.id.subway_status).findViewById(R.id.subway_status_message)).setText(errorMessage);
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
			new AsyncTask<Void, Void, ServiceStatus>() {
				@Override
				protected ServiceStatus doInBackground(Void... params) {
					// get the latest service status in the local database or NULL
					return DataManager.findLatestServiceStatus(getContentResolver(), Utils.getSupportedUserLocale());
				}

				@Override
				protected void onPostExecute(ServiceStatus result) {
					MyLog.v(TAG, "onPostExecute()");
					// get the latest service status in the local database or NULL
					SubwayTab.this.serviceStatus = result;
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
		Linkify.addLinks(messageTv, Linkify.WEB_URLS);
		new AlertDialog.Builder(this).setTitle(getString(R.string.subway_status)).setIcon(R.drawable.ic_btn_info_details).setView(messageTv)
				.setPositiveButton(getString(android.R.string.ok), null).setCancelable(true).create().show();
	}

	/**
	 * Show the closest stations UI.
	 */
	public void showClosestStations() {
		MyLog.v(TAG, "showClosestStations()");
		// enable location updates
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
		// IF there is no closest stations DO
		if (this.closestStations == null) {
			// look for the closest stations
			refreshClosestStations();
		} else {
			// show the closest stations
			showNewClosestStations();
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestStations.getLocation())) {
				// start refreshing
				refreshClosestStations();
			}
		}
	}

	/**
	 * Show the new closest stations.
	 */
	private void showNewClosestStations() {
		MyLog.v(TAG, "showNewClosestStations()");
		if (this.closestStations != null) {
			// set the closest station title
			String text = LocationUtils.getLocationString(this, this.closestStations.getLocationAddress(), this.closestStations.getLocation().getAccuracy());
			((TextView) findViewById(R.id.closest_stations_title).findViewById(R.id.closest_subway_stations)).setText(text);
			// hide loading
			findViewById(R.id.closest_stations_loading).setVisibility(View.GONE);
			// clear the previous list
			LinearLayout closestStationsLayout = (LinearLayout) findViewById(R.id.closest_stations);
			closestStationsLayout.removeAllViews();
			// show stations list
			closestStationsLayout.setVisibility(View.VISIBLE);
			int i = 1;
			for (ASubwayStation station : this.closestStations.getStations()) {
				// list view divider
				if (closestStationsLayout.getChildCount() > 0) {
					closestStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// create view
				View view = getLayoutInflater().inflate(R.layout.subway_tab_subway_closest_stations_list_item, null);
				view.setTag("station" + i++);
				// subway station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				ImageView subwayImg1 = (ImageView) view.findViewById(R.id.subway_img_1);
				ImageView subwayImg2 = (ImageView) view.findViewById(R.id.subway_img_2);
				ImageView subwayImg3 = (ImageView) view.findViewById(R.id.subway_img_3);
				// station lines color
				if (station.getOtherLinesId() != null && station.getOtherLinesId().size() > 0) {
					int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(0));
					subwayImg1.setImageResource(subwayLineImg1);
					if (station.getOtherLinesId().size() > 1) {
						int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(1));
						subwayImg2.setImageResource(subwayLineImg2);
						if (station.getOtherLinesId().size() > 2) {
							int subwayLineImg3 = SubwayUtils.getSubwayLineImgId(station.getOtherLinesId().get(2));
							subwayImg3.setImageResource(subwayLineImg3);
						} else {
							subwayImg3.setVisibility(View.GONE);
						}
					} else {
						subwayImg2.setVisibility(View.GONE);
						subwayImg3.setVisibility(View.GONE);
					}
				} else {
					subwayImg1.setVisibility(View.GONE);
					subwayImg2.setVisibility(View.GONE);
					subwayImg3.setVisibility(View.GONE);
				}
				// station distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
				}
				// add click listener
				final String subwayStationId = station.getId();
				final String subwayStationName = station.getName();
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", v.getId());
						Intent intent = new Intent(SubwayTab.this, SubwayStationInfo.class);
						intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
						intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStationName);
						startActivity(intent);
					}
				});
				closestStationsLayout.addView(view);
			}
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
			if (getLocation() != null) {
				// find the closest stations
				this.closestStationsTask = new ClosestSubwayStationsFinderTask(this, this);
				this.closestStationsTask.execute(getLocation());
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
			// hide the list
			findViewById(R.id.closest_stations).setVisibility(View.GONE);
			// clean the list
			((LinearLayout) findViewById(R.id.closest_stations)).removeAllViews(); // useful ?
			// show loading
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
			((LinearLayout) findViewById(R.id.closest_stations)).addView(cancelMsgTv);
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
			((LinearLayout) findViewById(R.id.closest_stations)).addView(cancelMsgTv);
			// hide loading
			findViewById(R.id.closest_stations_loading).setVisibility(View.GONE);
			findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
		}
		setClosestStationsNotLoading();
	}

	@Override
	public void onClosestStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress()");
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
	public void onClosestStationsDone(ClosestSubwayStations result) {
		MyLog.v(TAG, "onClosestStationsDone()");
		if (result == null) {
			// show the error
			setClosestStationsError();
		} else {
			// get the result
			this.closestStations = result;
			// shot the result
			showNewClosestStations();
			setClosestStationsNotLoading();
		}
	}

	/**
	 * Update the subway stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		// IF no closest stations AND new location DO
		if (this.closestStations == null && getLocation() != null) {
			// start refreshing if not running.
			refreshClosestStations();
			return;
		}
		// ELSE IF there are closest stations AND new location DO
		if (this.closestStations != null && getLocation() != null) {
			// update the list distances
			float accuracyInMeters = getLocation().getAccuracy();
			for (ASubwayStation station : this.closestStations.getStations()) {
				// distance
				Location stationLocation = LocationUtils.getNewLocation(station.getLat(), station.getLng());
				float distanceInMeters = getLocation().distanceTo(stationLocation);
				// MyLog.v(TAG, "distance in meters: " + distanceInMeters + " (accuracy: " + accuracyInMeters + ").");
				String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
				station.setDistanceString(distanceString);
			}
			// update the view
			refreshClosestSubwayStationsDistancesList();
		}
	}

	/**
	 * Refresh the closest subway stations <b>distances</b> ONLY in the list.
	 */
	private void refreshClosestSubwayStationsDistancesList() {
		MyLog.v(TAG, "refreshClosestSubwayStationsDistancesList()");
		findViewById(R.id.closest_stations).setVisibility(View.VISIBLE);
		int i = 1;
		for (ASubwayStation station : this.closestStations.getStations()) {
			View stationView = findViewById(R.id.closest_stations).findViewWithTag("station" + i++);
			if (stationView != null && !TextUtils.isEmpty(station.getDistanceString())) {
				((TextView) stationView.findViewById(R.id.distance)).setText(station.getDistanceString());
			}
		}
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
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
					if (!SubwayTab.this.locationUpdatesEnabled) {
						LocationUtils.enableLocationUpdates(SubwayTab.this, SubwayTab.this);
						SubwayTab.this.locationUpdatesEnabled = true;
					}
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

	/**
	 * Login or Logout from Twitter.
	 * @param v the view (not used)
	 */
	public void twitterLoginOrLogout(View v) {
		MyLog.v(TAG, "twitterLoginOrLogout()");
		if (TwitterUtils.isConnected(this)) {
			// disconnect
			TwitterUtils.logout(this);
		} else {
			// try to connect
			TwitterUtils.getInstance().startLoginProcess(this);
		}
	}

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

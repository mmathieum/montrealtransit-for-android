package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
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
import org.montrealtransit.android.services.StmInfoStatusReader;
import org.montrealtransit.android.services.StmInfoStatusReaderListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
public class SubwayTab extends Activity implements LocationListener, StmInfoStatusReaderListener,
        ClosestSubwayStationsFinderListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayTab.class.getSimpleName();

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
	private StmInfoStatusReader statusTask;

	/**
	 * The task used to find the closest stations.
	 */
	private ClosestSubwayStationsFinderTask closestStationsTask;

	/**
	 * Refresh or not refresh status image.
	 */
	private ImageView statusRefreshOrNotImg;
	/**
	 * Refresh of not refresh closest stations image.
	 */
	private ImageView closestStationsRefreshOrNorImg;
	/**
	 * Closest stations progress bar.
	 */
	private View closestStationsProgressBarView;
	/**
	 * Status progress bar.
	 */
	private View statusProgressBarView;
	/**
	 * Status layout.
	 */
	private RelativeLayout statusLayout;
	/**
	 * Status title text view.
	 */
	private TextView statusTitleTv;
	/**
	 * Status loading layout.
	 */
	private RelativeLayout statusLoadingLayout;
	/**
	 * Status message text view.
	 */
	private TextView statusMsgTv;
	/**
	 * Closest stations title text view.
	 */
	private TextView closestStationsTitleTv;
	/**
	 * Closest stations layout.
	 */
	private LinearLayout closestStationsLayout;
	/**
	 * Closest stations loading layout.
	 */
	private RelativeLayout closestStationsLoadingLayout;

	/**
	 * The current service status.
	 */
	private ServiceStatus serviceStatus = null;

	/**
	 * The validity of the current status (in seconds).
	 */
	private static final int STATUS_TOO_OLD_IN_SEC = 20 * 60; // 20 minutes

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_tab);
		// find the views.
		this.statusRefreshOrNotImg = (ImageView) findViewById(R.id.subway_status_section_refresh_or_stop_refresh);
		this.closestStationsRefreshOrNorImg = (ImageView) findViewById(R.id.closest_subway_stations_refresh);
		View statusTitleSectionView = findViewById(R.id.subway_status_title);
		this.statusProgressBarView = statusTitleSectionView.findViewById(R.id.progress_bar);
		this.closestStationsProgressBarView = findViewById(R.id.closest_stations_title).findViewById(R.id.progress_bar);
		this.statusLayout = (RelativeLayout) findViewById(R.id.subway_status);
		this.statusTitleTv = (TextView) statusTitleSectionView.findViewById(R.id.subway_status_section);
		this.statusLoadingLayout = (RelativeLayout) findViewById(R.id.subway_status_loading);
		this.statusMsgTv = (TextView) this.statusLayout.findViewById(R.id.subway_status_message);
		this.closestStationsTitleTv = (TextView) findViewById(R.id.closest_subway_stations);
		this.closestStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
		this.closestStationsLoadingLayout = (RelativeLayout) findViewById(R.id.closest_stations_loading);

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
		this.statusRefreshOrNotImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshStatus(v);
			}
		});
		View statusInfoView = findViewById(R.id.subway_status_section_info);
		statusInfoView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubwayStatusInfoDialog(v);
			}
		});
		this.closestStationsRefreshOrNorImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshClosestStations(v);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
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
		super.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
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
		refreshSubwayLines();
		showStatus();
		showClosestStations();
	}

	/**
	 * Refresh the subway lines UI.
	 */
	private void refreshSubwayLines() {
		LinearLayout subwayLinesLayout = (LinearLayout) findViewById(R.id.subway_line_list);
		List<SubwayLine> subwayLines = StmManager.findAllSubwayLinesList(this.getContentResolver());
		int i = 0;
		for (SubwayLine subwayLine : subwayLines) {
			// create view
			View view = subwayLinesLayout.getChildAt(i++);
			// subway line type image
			final int lineNumber = subwayLine.getNumber();
			String subwayLineName = getString(Utils.getSubwayLineNameShort(lineNumber));
			((TextView) view.findViewById(R.id.line_name)).setText(subwayLineName);
			// subway line colors
			int color = Utils.getSubwayLineColor(lineNumber);
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
					SubwayLineSelectDirection selectDialog = new SubwayLineSelectDirection(SubwayTab.this, lineNumber);
					selectDialog.showDialog();
					return true;
				}
			});
		}
	}

	/**
	 * Show the subway status. Try to load the subway status if not exist
	 */
	public void showStatus() {
		MyLog.v(TAG, "showStatus()");
		// get the latest service status in the local database or NULL
		this.serviceStatus = DataManager.findLatestServiceStatus(getContentResolver(), Utils.getUserLocale());
		// IF there is no service status DO
		if (this.serviceStatus == null) {
			// look for new service status
			refreshStatus();
		} else {
			// show latest service status
			showNewStatus();
			// check service age
			int nowInSec = (int) (System.currentTimeMillis() / 1000);
			// IF the latest service is too old DO
			if (nowInSec >= this.serviceStatus.getReadDate() + STATUS_TOO_OLD_IN_SEC) {
				// look for new service status
				refreshStatus();
			}
		}
	}

	/**
	 * Show the new status.
	 */
	private void showNewStatus() {
		ImageView statusImg = (ImageView) this.statusLayout.findViewById(R.id.subway_status_img);
		// hide loading (progress bar)
		this.statusLoadingLayout.setVisibility(View.GONE);
		// set the status title with the date
		CharSequence readTime = Utils.formatSameDayDateInSec(this.serviceStatus.getReadDate());
		this.statusTitleTv.setText(getString(R.string.subway_status_hour, readTime));
		// set the status message text
		this.statusMsgTv.setText(this.serviceStatus.getMessage());
		// set the status image (or not)
		switch (this.serviceStatus.getType()) {
		case ServiceStatus.STATUS_TYPE_RED:
			statusImg.setVisibility(View.VISIBLE);
			statusImg.setImageResource(R.drawable.status_red);
			break;
		case ServiceStatus.STATUS_TYPE_YELLOW:
			statusImg.setVisibility(View.VISIBLE);
			statusImg.setImageResource(R.drawable.status_yellow);
			break;
		case ServiceStatus.STATUS_TYPE_GREEN:
			statusImg.setVisibility(View.VISIBLE);
			statusImg.setImageResource(R.drawable.status_green);
			break;
		default:
			statusImg.setVisibility(View.GONE);
			break;
		}
		// show message
		this.statusLayout.setVisibility(View.VISIBLE);
	}

	/**
	 * Start the refresh status task if not running.
	 */
	public void refreshStatus() {
		MyLog.v(TAG, "refreshStatus()");
		// IF the task is NOT already running DO
		if (this.statusTask == null || !this.statusTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setStatusLoading();
			// read the subway status from twitter.com/stminfo
			this.statusTask = new StmInfoStatusReader(this, this);
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
			this.statusLayout.setVisibility(View.GONE);
			// clean the status
			this.statusTitleTv.setText(R.string.subway_status);
			// show the loading layout
			this.statusLoadingLayout.setVisibility(View.VISIBLE);
			// set the progress bar
			TextView progressBarLoading = (TextView) this.statusLoadingLayout.findViewById(R.id.detail_msg);
			String loadingMsg = getString(R.string.downloading_data_from_and_source, StmInfoStatusReader.SOURCE);
			progressBarLoading.setText(loadingMsg);
			progressBarLoading.setVisibility(View.VISIBLE);
		} else {
			// just notify the user ?
		}
		// show stop icon instead of refresh
		this.statusRefreshOrNotImg.setImageResource(R.drawable.ic_btn_stop);
		// show progress bar
		this.statusProgressBarView.setVisibility(View.VISIBLE);
	}

	/**
	 * Set the status as not loading.
	 */
	private void setStatusNotLoading() {
		MyLog.v(TAG, "setStatusNotLoading()");
		// show refresh icon instead of loading
		this.statusRefreshOrNotImg.setImageResource(R.drawable.ic_btn_refresh);
		// hide progress bar
		this.statusProgressBarView.setVisibility(View.GONE);
	}

	/**
	 * Set the status as cancelled.
	 */
	private void setStatusCancelled() {
		MyLog.v(TAG, "setStatusCancelled()");
		if (this.serviceStatus != null) {
			// notify the user but keep showing the old status ?
			Utils.notifyTheUser(this, getString(R.string.subway_status_loading_cancelled));
		} else {
			// show the BIG cancel message
			this.statusMsgTv.setText(getString(R.string.subway_status_loading_cancelled));
			this.statusLayout.setVisibility(View.VISIBLE);
			// hide loading (progress bar)
			this.statusLoadingLayout.setVisibility(View.GONE);
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
			this.statusLoadingLayout.setVisibility(View.GONE);
			// show message
			this.statusLayout.setVisibility(View.VISIBLE);
			this.statusMsgTv.setText(errorMessage);
		}
		setStatusNotLoading();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStmInfoStatusesLoaded(String errorMessage) {
		MyLog.v(TAG, "onStmInfoStatusesLoaded(%s)", errorMessage);
		// IF there is an error message DO
		if (errorMessage != null) {
			// update the BIG message
			setStatusError(errorMessage);
		} else {
			// notify the user ?
			// get the latest service status in the local database or NULL
			this.serviceStatus = DataManager.findLatestServiceStatus(getContentResolver(), Utils.getUserLocale());
			// show latest service status
			showNewStatus();
			setStatusNotLoading();
		}
	}

	/**
	 * Show the subway status info dialog.
	 * @param v useless - can be null
	 */
	public void showSubwayStatusInfoDialog(View v) {
		MyLog.v(TAG, "showSubwayStatusInfoDialog()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.subway_status));
		builder.setIcon(R.drawable.ic_btn_info_details);
		TextView messageTv = new TextView(this);
		messageTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		messageTv.setPadding(15, 15, 15, 15); // TODO custom dialog
		messageTv.setText(getString(R.string.subway_status_message));
		Linkify.addLinks(messageTv, Linkify.WEB_URLS);
		builder.setView(messageTv);
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.setCancelable(true);
		builder.create();
		builder.show();
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
			String text = LocationUtils.getLocationString(this, this.closestStations.getLocationAddress(),
			        this.closestStations.getLocation().getAccuracy());
			// MyLog.d(TAG, "text:" + text);
			this.closestStationsTitleTv.setText(text);
			// hide loading
			this.closestStationsLoadingLayout.setVisibility(View.GONE);
			// clear the previous list
			this.closestStationsLayout.removeAllViews();
			// show stations list
			this.closestStationsLayout.setVisibility(View.VISIBLE);
			int i = 1;
			for (ASubwayStation station : this.closestStations.getStations()) {
				// list view divider
				if (this.closestStationsLayout.getChildCount() > 0) {
					this.closestStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				// create view
				View view = getLayoutInflater().inflate(R.layout.subway_tab_subway_stations_list_item, null);
				view.setTag("station" + i++);
				// subway station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				ImageView subwayImg1 = (ImageView) view.findViewById(R.id.subway_img_1);
				ImageView subwayImg2 = (ImageView) view.findViewById(R.id.subway_img_2);
				ImageView subwayImg3 = (ImageView) view.findViewById(R.id.subway_img_3);
				// station lines color
				if (station.getOtherLinesId() != null && station.getOtherLinesId().size() > 0) {
					int subwayLineImg1 = Utils.getSubwayLineImgId(station.getOtherLinesId().get(0));
					subwayImg1.setImageResource(subwayLineImg1);
					if (station.getOtherLinesId().size() > 1) {
						int subwayLineImg2 = Utils.getSubwayLineImgId(station.getOtherLinesId().get(1));
						subwayImg2.setImageResource(subwayLineImg2);
						if (station.getOtherLinesId().size() > 2) {
							int subwayLineImg3 = Utils.getSubwayLineImgId(station.getOtherLinesId().get(2));
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
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", v.getId());
						Intent intent = new Intent(SubwayTab.this, SubwayStationInfo.class);
						intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
						startActivity(intent);
					}
				});
				this.closestStationsLayout.addView(view);
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
		if (this.closestStations == null) {
			// set the BIG loading message
			// remove last location from the list divider
			this.closestStationsTitleTv.setText(R.string.closest_subway_stations);
			// hide the list
			this.closestStationsLayout.setVisibility(View.GONE);
			// clean the list
			this.closestStationsLayout.removeAllViews(); // useful ?
			// show loading
			this.closestStationsLoadingLayout.setVisibility(View.VISIBLE);
			// show waiting for location
			TextView detailMsgTv = (TextView) this.closestStationsLoadingLayout.findViewById(R.id.detail_msg);
			detailMsgTv.setText(R.string.waiting_for_location_fix);
			detailMsgTv.setVisibility(View.VISIBLE);
		} else {
			// just notify the user ?
		}
		// show stop icon instead of refresh
		this.closestStationsRefreshOrNorImg.setImageResource(R.drawable.ic_btn_stop);
		// show progress bar
		this.closestStationsProgressBarView.setVisibility(View.VISIBLE);
	}

	/**
	 * Set the closest stations as not loading.
	 */
	private void setClosestStationsNotLoading() {
		MyLog.v(TAG, "setClosestStationsNotLoading()");
		// show refresh icon instead of loading
		this.closestStationsRefreshOrNorImg.setImageResource(R.drawable.ic_btn_refresh);
		// hide progress bar
		this.closestStationsProgressBarView.setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the closest stations as cancelled.
	 */
	private void setClosestStationsCancelled() {
		MyLog.v(TAG, "setClosestStationsCancelled()");
		if (this.closestStations != null) {
			// notify the user ?
			// Utils.notifyTheUser(this, getString(R.string.closest_subway_stations_cancelled));
		} else {
			// update the BIG cancel message
			TextView cancelMsgTv = new TextView(this);
			cancelMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cancelMsgTv.setText(getString(R.string.closest_subway_stations_cancelled));
			this.closestStationsLayout.addView(cancelMsgTv);
			// hide loading
			this.closestStationsLoadingLayout.setVisibility(View.GONE);
			this.closestStationsLayout.setVisibility(View.VISIBLE);
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
			this.closestStationsLayout.addView(cancelMsgTv);
			// hide loading
			this.closestStationsLoadingLayout.setVisibility(View.GONE);
			this.closestStationsLayout.setVisibility(View.VISIBLE);
		}
		setClosestStationsNotLoading();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClosestStationsProgress(String progress) {
		MyLog.v(TAG, "onClosestStationsProgress()");
		if (this.closestStations != null) {
			// notify the user ?
		} else {
			// update the BIG message
			this.closestStationsLoadingLayout.setVisibility(View.VISIBLE);
			TextView detailMsgTv = (TextView) this.closestStationsLoadingLayout.findViewById(R.id.detail_msg);
			detailMsgTv.setText(progress);
			detailMsgTv.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
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
		this.closestStationsLayout.setVisibility(View.VISIBLE);
		int i = 1;
		for (ASubwayStation station : this.closestStations.getStations()) {
			View stationView = this.closestStationsLayout.findViewWithTag("station" + i++);
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
			MyLog.d(TAG, "new location: %s", newLocation);
			if (this.location == null || LocationUtils.isMorePrecise(this.location, newLocation)) {
				this.location = newLocation;
			}
		}
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
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
	 * {@inheritDoc}
	 */
	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistancesWithNewLocation();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderEnabled(String provider) {
		MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProviderDisabled(String provider) {
		MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	/**
	 * Menu to login/logout of Twitter API.
	 */
	private static final int MENU_LOGIN_TWITTER_API = Menu.FIRST;
	/**
	 * Menu to reload the subway status.
	 */
	private static final int MENU_RELOAD_STATUS = Menu.FIRST + 1;
	/**
	 * Menu to reload the closest subway stations.
	 */
	private static final int MENU_RELOAD_CLOSEST_STATIONS = Menu.FIRST + 2;
	/**
	 * Menu to show the subway map from the STM.info Web Site.
	 */
	private static final int MENU_SHOW_MAP_ON_THE_STM_WEBSITE = Menu.FIRST + 3;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 4;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 5;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuTwitterApi = menu.add(0, MENU_LOGIN_TWITTER_API, Menu.NONE, R.string.menu_twitter_login);
		menuTwitterApi.setIcon(R.drawable.ic_menu_twitter);

		MenuItem menuReloadStatus = menu.add(0, MENU_RELOAD_STATUS, Menu.NONE, R.string.reload_subway_status);
		menuReloadStatus.setIcon(R.drawable.ic_menu_refresh);

		MenuItem menuClosestStations = menu.add(0, MENU_RELOAD_CLOSEST_STATIONS, Menu.NONE,
		        R.string.reload_closest_stations);
		menuClosestStations.setIcon(R.drawable.ic_menu_refresh);

		menu.add(0, MENU_SHOW_MAP_ON_THE_STM_WEBSITE, 0, R.string.show_map_from_stm_website);

		MenuItem menuPref = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menuPref.setIcon(android.R.drawable.ic_menu_preferences);

		MenuItem menuAbout = menu.add(0, MENU_ABOUT, Menu.NONE, R.string.menu_about);
		menuAbout.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (super.onPrepareOptionsMenu(menu)) {
			// TWITTER
			if (TwitterUtils.isConnected(this)) {
				// CANCEL REFRESH
				MenuItem menuTwitterApi = menu.findItem(MENU_LOGIN_TWITTER_API);
				// TODO menuTwitterApi.setIcon(R.drawable.ic_menu_twitter);
				menuTwitterApi.setTitle(R.string.menu_twitter_logout);
			} else {
				// REFRESH
				MenuItem menuTwitterApi = menu.findItem(MENU_LOGIN_TWITTER_API);
				// TODO menuTwitterApi.setIcon(R.drawable.ic_menu_twitter);
				menuTwitterApi.setTitle(R.string.menu_twitter_login);
			}
			// SERVICE STATUS REFRESH
			if (this.statusTask != null && this.statusTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
				// CANCEL REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_RELOAD_STATUS);
				menuRefresh.setIcon(R.drawable.ic_menu_stop); // not in SDK 1.5!
			} else {
				// REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_RELOAD_STATUS);
				menuRefresh.setIcon(R.drawable.ic_menu_refresh); // not in SDK 1.5!
			}
			// CLOSEST STATIONs REFRESH
			if (this.closestStationsTask != null
			        && this.closestStationsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
				// CANCEL REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_RELOAD_CLOSEST_STATIONS);
				menuRefresh.setIcon(R.drawable.ic_menu_stop); // not in SDK 1.5!
			} else {
				// REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_RELOAD_CLOSEST_STATIONS);
				menuRefresh.setIcon(R.drawable.ic_menu_refresh); // not in SDK 1.5!
			}
			return true;
		} else {
			MyLog.w(TAG, "Error in onPrepareOptionsMenu().");
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_LOGIN_TWITTER_API:
			if (TwitterUtils.isConnected(this)) {
				// disconnect
				TwitterUtils.logout(this);
			} else {
				// try to connect
				TwitterUtils.getInstance().startLoginProcess(this);
			}
			break;
		case MENU_RELOAD_STATUS:
			refreshOrStopRefreshStatus(null);
			break;
		case MENU_RELOAD_CLOSEST_STATIONS:
			refreshOrStopRefreshClosestStations(null);
			break;
		case MENU_SHOW_MAP_ON_THE_STM_WEBSITE:
			String url = "http://www.stm.info/metro/images/plan-metro.jpg";
			// TODO store the map on the SD card the first time and then re-open it
			// TODO add a menu to reload the map from the web site in the image viewer?
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			break;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			break;
		case MENU_ABOUT:
			Utils.showAboutDialog(this);
			break;
		default:
			MyLog.d(TAG, "Unknow menu action: %s.", item.getItemId());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
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
		super.onDestroy();
	}
}

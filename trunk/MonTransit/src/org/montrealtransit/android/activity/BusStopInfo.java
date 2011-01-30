package org.montrealtransit.android.activity;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.BusLineDirection;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.services.GeocodingTask;
import org.montrealtransit.android.services.GeocodingTaskListener;
import org.montrealtransit.android.services.nextstop.NextStopListener;
import org.montrealtransit.android.services.nextstop.StmInfoTask;
import org.montrealtransit.android.services.nextstop.StmMobileTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Address;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements NextStopListener, DialogInterface.OnClickListener,
        OnSharedPreferenceChangeListener {

	/**
	 * The extra ID for the bus stop code.
	 */
	public static final String EXTRA_STOP_CODE = "extra_stop_code";
	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_STOP_LINE_NUMBER = "extra_line_number";
	/**
	 * The log tag.
	 */
	private static final String TAG = BusStopInfo.class.getSimpleName();
	/**
	 * The bus stop.
	 */
	private StmStore.BusStop busStop;
	/**
	 * The bus line.
	 */
	private StmStore.BusLine busLine;
	/**
	 * Store the current hours (including messages).
	 */
	private BusStopHours hours;
	/**
	 * The task used to load the next bus stops.
	 */
	private AsyncTask<StmStore.BusStop, String, BusStopHours> task;

	/**
	 * The refresh/stop refresh image.
	 */
	private ImageView nextStopsRefreshOrStopRefreshImg;
	/**
	 * The favorite check box.
	 */
	private CheckBox starCb;
	/**
	 * The subway station layout.
	 */
	private RelativeLayout theSubwayStationLayout;
	/**
	 * The loading view.
	 */
	private View loadingView;
	/**
	 * The first message text view.
	 */
	private TextView message1Tv;
	/**
	 * The second message text view.
	 */
	private TextView message2Tv;
	/**
	 * The next stop group view.
	 */
	private View nextStopsGroupView;
	/**
	 * The progress bar view.
	 */
	private View progressBarView;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_info);
		// find the views
		this.nextStopsRefreshOrStopRefreshImg = (ImageView) findViewById(R.id.next_stops_refresh);
		this.starCb = (CheckBox) findViewById(R.id.star);
		this.theSubwayStationLayout = (RelativeLayout) findViewById(R.id.the_subway_station);
		this.loadingView = findViewById(R.id.next_stops_loading);
		this.message1Tv = (TextView) findViewById(R.id.next_stops_msg);
		this.message2Tv = (TextView) findViewById(R.id.next_stops_msg2);
		this.nextStopsGroupView = findViewById(R.id.next_stops_group);
		this.progressBarView = findViewById(R.id.next_stops_title).findViewById(R.id.progress_bar);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		setBusStopFromIntent(getIntent(), savedInstanceState);
	}

	/**
	 * onCreate() method only for Android version older than Android 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		this.nextStopsRefreshOrStopRefreshImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshNextStops(v);
			}
		});
		((ImageView) findViewById(R.id.next_stops_section_info)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showNextStopsInfoDialog(v);
			}
		});
		this.starCb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addOrRemoveFavorite(v);
			}
		});
		this.theSubwayStationLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubwayStation(v);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		setBusStopFromIntent(intent, null);
	}

	/**
	 * Retrieve the bus stop from the Intent or from the Bundle.
	 * @param intent the intent
	 * @param savedInstanceState the saved instance state (Bundle)
	 */
	private void setBusStopFromIntent(Intent intent, Bundle savedInstanceState) {
		MyLog.v(TAG, "setBusStopFromIntent()");
		if (intent != null) {
			String stopCode;
			String lineNumber;
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				String pathSegment = intent.getData().getPathSegments().get(1);
				stopCode = pathSegment.substring(0, 5);
				lineNumber = pathSegment.substring(5);
			} else {
				stopCode = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_CODE);
				lineNumber = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_LINE_NUMBER);
			}
			showNewBusStop(stopCode, lineNumber);
		}
	}

	/**
	 * Set the bus stop in the UI.
	 * @param stopCode the bus stop code
	 */
	private void setStopCode(String stopCode) {
		MyLog.v(TAG, "setStopCode(%s)", stopCode);
		((TextView) findViewById(R.id.stop_code)).setText(stopCode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		// save the current hours
		return this.hours != null ? this.hours : null;
	}

	/**
	 * Show the next stops info dialog
	 * @param v useless - can be null
	 */
	public void showNextStopsInfoDialog(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.next_bus_stops));
		builder.setIcon(R.drawable.ic_btn_info_details);
		String source;
		if (this.hours != null) {
			source = this.hours.getSourceName();
		} else {
			if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				source = StmInfoTask.SOURCE_NAME;
			} else if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				source = StmMobileTask.SOURCE_NAME;
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", getNextStopProviderFromPreferences());
				source = StmMobileTask.SOURCE_NAME; // default stm mobile
			}
		}
		builder.setMessage(getString(R.string.next_bus_stops_message_and_source, source));
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.setCancelable(true);

		builder.create();
		builder.show();
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		MyLog.v(TAG, "setTheStar()");
		Fav fav = DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP,
		        this.busStop.getCode(), this.busStop.getLineNumber());
		this.starCb.setChecked(fav != null);
	}

	/**
	 * Set the bus stop info basic UI.
	 */
	private void refreshBusStopInfo() {
		MyLog.v(TAG, "refreshBusStopInfo()");
		setStopCode(this.busStop.getCode());
		// set bus stop place name
		((TextView) findViewById(R.id.bus_stop_place)).setText(BusUtils.cleanBusStopPlace(this.busStop.getPlace()));
		// set the favorite icon
		setTheStar();
		// set bus line number
		TextView lineNumberTv = (TextView) findViewById(R.id.line_number);
		lineNumberTv.setText(this.busLine.getNumber());
		BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(this, this.busLine.getNumber());
		lineNumberTv.setOnClickListener(busLineSelectDirection);
		// set bus line name
		TextView lineNameTv = (TextView) findViewById(R.id.line_name);
		lineNameTv.setText(this.busLine.getName());
		lineNameTv.setOnClickListener(busLineSelectDirection);
		// bus line type
		((ImageView) findViewById(R.id.line_type)).setImageResource(BusUtils.getBusLineTypeImgFromType(this.busLine
		        .getType()));
		// set bus line direction
		String directionId = this.busStop.getDirectionId();
		BusLineDirection busLineDirection = StmManager.findBusLineDirection(this.getContentResolver(), directionId);
		List<Integer> busLineDirectionIds = BusUtils.getBusLineDirectionStringIdFromId(busLineDirection.getId());
		String directionText = getString(R.string.direction_and_string, getString(busLineDirectionIds.get(0)));
		((TextView) findViewById(R.id.direction_main)).setText(directionText);
	}

	/**
	 * Set the bus stop subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		View subwayStationView = findViewById(R.id.subway_station);
		if (!TextUtils.isEmpty(this.busStop.getSubwayStationId())) {
			// MyLog.d(TAG, "SubwayStationId: %s.", this.busStop.getSubwayStationId());
			subwayStationView.setVisibility(View.VISIBLE);
			this.theSubwayStationLayout.setVisibility(View.VISIBLE);
			StmStore.SubwayStation subwayStation = StmManager.findSubwayStation(this.getContentResolver(),
			        this.busStop.getSubwayStationId());
			((TextView) findViewById(R.id.station_name)).setText(subwayStation.getName());

			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(),
			        subwayStation.getId());
			ImageView subwayImg1 = (ImageView) findViewById(R.id.subway_img_1);
			ImageView subwayImg2 = ((ImageView) findViewById(R.id.subway_img_2));
			ImageView subwayImg3 = ((ImageView) findViewById(R.id.subway_img_3));
			if (subwayLines != null && subwayLines.size() > 0) {
				int subwayLineImg0 = SubwayUtils.getSubwayLineImgId(subwayLines.get(0).getNumber());
				subwayImg1.setImageResource(subwayLineImg0);
				if (subwayLines.size() > 1) {
					int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(subwayLines.get(1).getNumber());
					subwayImg2.setImageResource(subwayLineImg1);
					if (subwayLines.size() > 2) {
						int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(subwayLines.get(2).getNumber());
						subwayImg3.setImageResource(subwayLineImg2);
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
		} else {
			subwayStationView.setVisibility(View.GONE);
			this.theSubwayStationLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Set the other bus lines using this bus stop.
	 */
	private void refreshOtherBusLinesInfo() {
		MyLog.v(TAG, "refreshOtherBusLinesInfo()");
		List<BusLine> allBusLines = StmManager.findBusStopLinesList(this.getContentResolver(), this.busStop.getCode());
		// remove all bus lines with the same line number
		ListIterator<BusLine> it = allBusLines.listIterator();
		Set<String> buslinesNumber = new HashSet<String>();
		while (it.hasNext()) {
			BusLine busLine = it.next();
			// IF the bus line number is the same as the current bus stop DO
			if (busLine.getNumber().equals(this.busLine.getNumber())) {
				it.remove();
				// ELSE IF the bus line number is already in the list DO
			} else if (buslinesNumber.contains(busLine.getNumber())) {
				it.remove();
			} else {
				buslinesNumber.add(busLine.getNumber());
			}
		}
		LinearLayout otherBusLinesLayout = (LinearLayout) findViewById(R.id.other_bus_line_list);
		otherBusLinesLayout.removeAllViews();
		View otherBusLinesView = findViewById(R.id.other_bus_line);
		if (allBusLines.size() > 0) {
			otherBusLinesView.setVisibility(View.VISIBLE);
			otherBusLinesLayout.setVisibility(View.VISIBLE);
			for (StmStore.BusLine busLine : allBusLines) {
				// the view
				View view = getLayoutInflater().inflate(R.layout.bus_stop_info_bus_line_list_item, null);
				TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
				final String lineNumber = busLine.getNumber();
				lineNumberTv.setText(lineNumber);
				int color = BusUtils.getBusLineTypeBgColorFromType(busLine.getType());
				lineNumberTv.setBackgroundColor(color);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)");
						// MyLog.d(TAG, "bus line number: %s", lineNumber);
						Intent intent = new Intent(BusStopInfo.this, BusStopInfo.class);
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, lineNumber);
						intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, BusStopInfo.this.busStop.getCode());
						startActivity(intent);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						// MyLog.d(TAG, "bus line number: %s", lineNumber);
						BusLineSelectDirection busLineSelDir = new BusLineSelectDirection(BusStopInfo.this, lineNumber);
						busLineSelDir.showDialog();
						return true;
					}
				});
				otherBusLinesLayout.addView(view);
			}
		} else {
			otherBusLinesView.setVisibility(View.GONE);
			otherBusLinesLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newLineNumber the new bus line number (optional)
	 */
	public void showNewBusStop(String newStopCode, String newLineNumber) {
		MyLog.v(TAG, "showNewBusStop(%s, %s)", newStopCode, newLineNumber);
		if ((this.busStop == null)
		        || (!this.busStop.getCode().equals(newStopCode) || !this.busStop.getLineNumber().equals(newLineNumber))) {
			MyLog.v(TAG, "New bus stop '%s' line '%s'.", newStopCode, newLineNumber);
			setStopCode(newStopCode);
			checkStopCode(newStopCode);
			if (newLineNumber == null) {
				// get the bus lines for this bus stop
				List<BusLine> busLines = StmManager.findBusStopLinesList(getContentResolver(), newStopCode);
				if (busLines == null) {
					// no bus line found
					// TODO handle unknown bus stop code
					String message = getString(R.string.wrong_stop_code_and_code, newStopCode);
					Utils.notifyTheUser(this, message);
					this.finish();
				} else {
					// at least one bus line found
					if (busLines.size() == 1) {
						// use the only bus line available for this bus stop
						newLineNumber = busLines.get(0).getNumber();
					} else {
						// TODO show a bus line selector to the user
						// for now, select the first result
						newLineNumber = busLines.get(0).getNumber();
					}
				}
			}
			if (newStopCode != null && newLineNumber != null) {
				this.busStop = StmManager.findBusLineStop(this.getContentResolver(), newStopCode, newLineNumber);
				this.busLine = StmManager.findBusLine(this.getContentResolver(), this.busStop.getLineNumber());
				this.hours = null;
				if (this.task != null) {
					this.task.cancel(true);
					this.task = null;
				}
				refreshAll();
			}
		}
	}

	/**
	 * Check if the bus stop is in the current app database.
	 * @param stopCode the bus stop code
	 */
	private void checkStopCode(String stopCode) {
		if (StmManager.findBusStop(this.getContentResolver(), stopCode) == null) {
			showAlertDialog(stopCode);
		}
	}

	/**
	 * Show the dialog about the unknown bus stop id.
	 * @param wrongStopCode
	 */
	private void showAlertDialog(String wrongStopCode) {
		MyLog.v(TAG, "showAlertDialog()");
		MyLog.w(TAG, "Wrong bus stop code '%s'?", wrongStopCode);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning);
		String message = getString(R.string.wrong_stop_code_and_code, wrongStopCode) + "\n"
		        + getString(R.string.wrong_stop_code_internet);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(%s)", which);
		if (which == -2) {
			dialog.dismiss();// CANCEL
			this.finish(); // close the activity.
		} else {
			// try to load the next stop from the web.
			refreshNextStops();
		}
	}

	/**
	 * Refresh all the UI (based on the bus stop).
	 */
	private void refreshAll() {
		showNextBusStops();
		refreshBusStopInfo();
		refreshOtherBusLinesInfo();
		refreshSubwayStationInfo();
		setFocusToTheBusStopPlace();
	}

	/**
	 * Show the next bus stops (or launch refresh next bus stops task).
	 */
	private void showNextBusStops() {
		if (this.hours == null) {
			refreshNextStops();
		} else {
			showNewNextStops(this.hours);
		}

	}

	/**
	 * Show the new next bus stops.
	 * @param hours the new next bus stops
	 */
	private void showNewNextStops(BusStopHours hours) {
		MyLog.v(TAG, "showNewNextStops(%s)");
		if (hours != null) {
			String nextBusStop = getString(R.string.next_bus_stops_and_source, hours.getSourceName());
			((TextView) findViewById(R.id.next_stops_string)).setText(nextBusStop);
			// IF there next stops found DO
			if (hours.getSHours().size() > 0) {
				// hide loading + messages
				this.loadingView.setVisibility(View.GONE);
				this.message1Tv.setVisibility(View.GONE);
				this.message2Tv.setVisibility(View.GONE);
				// show next bus stop group
				this.nextStopsGroupView.setVisibility(View.VISIBLE);
				List<String> fHours = hours.getFormattedHours(this);
				// clear the last value
				TextView theNextStopTv = (TextView) findViewById(R.id.the_next_stop);
				TextView theSecondNextStopTv = (TextView) findViewById(R.id.the_second_next_stop);
				TextView otherStopsTv = (TextView) findViewById(R.id.other_stops);
				// theNextStopTv.setText(null);
				theSecondNextStopTv.setText(null);
				otherStopsTv.setText(null);
				// show the bus stops
				theNextStopTv.setText(fHours.get(0));
				if (fHours.size() > 1) {
					theSecondNextStopTv.setText(fHours.get(1));
					if (fHours.size() > 2) {
						String hoursS = "";
						for (int i = 2; i < fHours.size(); i++) {
							if (hoursS.length() > 0) {
								hoursS += " ";
							}
							hoursS += fHours.get(i);
						}
						otherStopsTv.setText(hoursS);
					}
				}
				// show message
				if (!TextUtils.isEmpty(hours.getMessage())) {
					this.message1Tv.setVisibility(View.VISIBLE);
					this.message1Tv.setText(hours.getMessage());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
				}
			}
		}
	}

	/**
	 * Start the next bus stops refresh task if not running.
	 */
	private void refreshNextStops() {
		MyLog.v(TAG, "refreshNextStops()");
		// IF the task is NOT already running DO
		if (this.task == null || !this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setNextStopsLoading();
			// find the closest stations
			if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				this.task = new StmInfoTask(this, this);
				this.task.execute(this.busStop);
			} else if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				this.task = new StmMobileTask(this, this);
				this.task.execute(this.busStop);
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", getNextStopProviderFromPreferences());
				this.task = new StmMobileTask(this, this); // default stm mobile
				this.task.execute(this.busStop);
			}
		}
	}

	/**
	 * Refresh or stop refresh the next bus stop depending on the current status of the task.
	 * @param v the view (not used)
	 */
	public void refreshOrStopRefreshNextStops(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshNextStops()");
		// IF the task is running DO
		if (this.task != null && this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.task.cancel(true);
			this.task = null;
			setNextStopsCancelled();
		} else {
			refreshNextStops();
		}
	}

	/**
	 * Set the next stops view as loading.
	 */
	private void setNextStopsLoading() {
		MyLog.v(TAG, "setNextStopsLoading()");
		if (this.hours == null) {
			// set the BIG loading message
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message1Tv.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.VISIBLE);
		} else {
			// notify the user ?
		}
		// show stop icon instead of refresh
		this.nextStopsRefreshOrStopRefreshImg.setImageResource(R.drawable.ic_btn_stop);
		// show progress bar
		this.progressBarView.setVisibility(View.VISIBLE);

	}

	/**
	 * Set the next stop view as not loading.
	 */
	private void setNextStopsNotLoading() {
		MyLog.v(TAG, "setNextStopsNotLoading()");
		// show refresh icon instead of loading
		this.nextStopsRefreshOrStopRefreshImg.setImageResource(R.drawable.ic_btn_refresh);
		// hide progress bar
		this.progressBarView.setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the next stops view as cancelled.
	 */
	private void setNextStopsCancelled() {
		MyLog.v(TAG, "setClosestStationsCancelled()");
		if (this.hours != null) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUser(this, getString(R.string.next_bus_stop_load_cancelled));
		} else {
			// show the BIG cancel message
			// hide loading + message 2 + next stops group
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.GONE);
			// show message 1
			this.message1Tv.setVisibility(View.VISIBLE);
			this.message1Tv.setText(R.string.next_bus_stop_load_cancelled);
			this.message1Tv.setVisibility(View.VISIBLE);
		}
		setNextStopsNotLoading();
	}

	/**
	 * Set the next stops view as error.
	 * @param hours the error
	 */
	private void setNextStopsError(BusStopHours hours) {
		MyLog.v(TAG, "setNextStopsError()");
		// IF there are hours to show DO
		if (this.hours != null) {
			// notify the user but keep showing the old stations
			if (!TextUtils.isEmpty(hours.getError())) {
				Utils.notifyTheUser(this, hours.getError());
			} else if (!TextUtils.isEmpty(hours.getMessage())) {
				Utils.notifyTheUser(this, hours.getMessage());
			} else if (!TextUtils.isEmpty(hours.getMessage2())) {
				Utils.notifyTheUser(this, hours.getMessage2());
			} else {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
				Utils.notifyTheUser(this, defaultMessage);
			}
		} else {
			// show the BIG cancel message
			// hide loading + message 2 + next stops group
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.GONE);
			// show message 1
			this.message1Tv.setVisibility(View.VISIBLE);
			// IF an error occurs during the process DO
			if (!TextUtils.isEmpty(hours.getError())) {
				this.message1Tv.setText(hours.getError());
			} else {
				// IF there is a secondary message from the STM DO
				if (!TextUtils.isEmpty(hours.getMessage2())) {
					this.message1Tv.setText(hours.getMessage2());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
					// IF there is also an error message from the STM DO
					if (!TextUtils.isEmpty(hours.getMessage())) {
						this.message2Tv.setVisibility(View.VISIBLE);
						Linkify.addLinks(this.message2Tv, Linkify.ALL);
					}
					// ELSE IF there is only an error message from the STM DO
				} else if (!TextUtils.isEmpty(hours.getMessage())) {
					this.message1Tv.setText(hours.getMessage());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
					// ELSE
				} else {
					MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
					// DEFAULT MESSAGE > no more bus stop for this bus line
					String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
					this.message1Tv.setText(defaultMessage);
				}
			}
		}
		setNextStopsNotLoading();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNextStopsProgress(String progress) {
		MyLog.v(TAG, "updateProgress(%s)", progress);
		if (!TextUtils.isEmpty(progress)) {
			if (this.hours != null) {
				// notify the user  ?
			} else {
				// update the BIG message
				TextView detailMsgTv = (TextView) this.loadingView.findViewById(R.id.detail_msg);
				detailMsgTv.setText(progress);
				detailMsgTv.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNextStopsLoaded(BusStopHours result) {
		MyLog.v(TAG, "onNextStopsLoaded()");
		// IF error DO
		if (result == null || result.getSHours().size() <= 0) {
			// process the error
			setNextStopsError(result);
		} else {
			// get the result
			this.hours = result;
			// show the result
			showNewNextStops(this.hours);
			setNextStopsNotLoading();
		}
	}

	/**
	 * Set the focus to the bus stop place.
	 */
	private void setFocusToTheBusStopPlace() {
		// TODO doesn't work!!!
		((TextView) findViewById(R.id.bus_stop_place)).requestFocus();
	}
	
	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		// manage favorite star click
		if (DataManager.findFav(BusStopInfo.this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP,
		        BusStopInfo.this.busStop.getCode(), BusStopInfo.this.busStop.getLineNumber()) != null) {
			// delete the favorite
			DataManager.deleteFav(
			        BusStopInfo.this.getContentResolver(),
			        DataManager.findFav(BusStopInfo.this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP,
			                BusStopInfo.this.busStop.getCode(), BusStopInfo.this.busStop.getLineNumber()).getId());
			Utils.notifyTheUser(BusStopInfo.this, getString(R.string.favorite_removed));
		} else {
			// add the favorite
			DataStore.Fav newFav = new DataStore.Fav();
			newFav.setType(DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			newFav.setFkId(BusStopInfo.this.busStop.getCode());
			newFav.setFkId2(BusStopInfo.this.busLine.getNumber());
			DataManager.addFav(BusStopInfo.this.getContentResolver(), newFav);
			Utils.notifyTheUser(BusStopInfo.this, getString(R.string.favorite_added));
		}
		setTheStar(); // TODO is remove useless?
	}

	/**
	 * Show the subway station.
	 * @param v a view (not used)
	 */
	public void showSubwayStation(View v) {
		// IF there is a subway station DO
		String subwayStationId = BusStopInfo.this.busStop.getSubwayStationId();
		if (!TextUtils.isEmpty(subwayStationId)) {
			// show subway station info
			Intent intent = new Intent(BusStopInfo.this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
			startActivity(intent);
		}
	}

	/**
	 * The menu item for refreshing the next bus stops.
	 */
	private static final int MENU_REFRESH_NEXT_STOP = Menu.FIRST;
	/**
	 * The menu item for showing the m.stm.info page of this bus stop.
	 */
	private static final int MENU_SHOW_STM_MOBILE_WEBSITE = Menu.FIRST + 1;
	/**
	 * Menu for showing the bus stop in Maps.
	 */
	private static final int MENU_SHOW_IN_MAPS = Menu.FIRST + 2;
	/**
	 * Menu for using a radar to get to the bus stop.
	 */
	private static final int MENU_USE_RADAR = Menu.FIRST + 3;
	/**
	 * Menu for selecting the next stop data provider.
	 */
	private static final int MENU_SELECT_NEXT_STOP_PROVIDER_GROUP = Menu.FIRST + 4;
	/**
	 * Menu for selecting the next stop data provider.
	 */
	private static final int MENU_SELECT_NEXT_STOP_PROVIDER = Menu.FIRST + 5;
	/**
	 * Menu for selecting the next stop data provider.
	 */
	private static final int MENU_SELECT_NEXT_STOP_PROVIDER_STM_MOBILE = Menu.FIRST + 6;
	/**
	 * Menu for selecting the next stop data provider.
	 */
	private static final int MENU_SELECT_NEXT_STOP_PROVIDER_STM_INFO = Menu.FIRST + 7;
	/**
	 * The menu used to show the search UI.
	 */
	private static final int MENU_SEARCH = Menu.FIRST + 8;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 9;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// MyLog.v(TAG, "onCreateOptionsMenu()");
		MenuItem menuRefresh = menu.add(0, MENU_REFRESH_NEXT_STOP, 0, R.string.refresh_next_bus_stop);
		menuRefresh.setIcon(R.drawable.ic_menu_refresh); // not in SDK 1.5!
		menuRefresh.setAlphabeticShortcut(Constant.SHORTCUT_REFRESH);
		MenuItem menuStmMobile = menu.add(0, MENU_SHOW_STM_MOBILE_WEBSITE, 0, R.string.see_in_stm_mobile_web_site);
		menuStmMobile.setIcon(R.drawable.ic_menu_stmmobile);
		menuStmMobile.setAlphabeticShortcut(Constant.SHORTCUT_STM_MOBILE);
		MenuItem menuMaps = menu.add(0, MENU_SHOW_IN_MAPS, 0, R.string.show_in_map_exp);
		menuMaps.setIcon(android.R.drawable.ic_menu_mapmode);
		menuMaps.setAlphabeticShortcut(Constant.SHORTCUT_MAPS);
		MenuItem menuRadar = menu.add(0, MENU_USE_RADAR, 0, R.string.use_radar);
		menuRadar.setIcon(android.R.drawable.ic_menu_compass);

		SubMenu subMenu = menu.addSubMenu(MENU_SELECT_NEXT_STOP_PROVIDER_GROUP, MENU_SELECT_NEXT_STOP_PROVIDER, 0,
		        R.string.select_next_stop_data_source);
		subMenu.setIcon(android.R.drawable.ic_menu_preferences);
		subMenu.add(MENU_SELECT_NEXT_STOP_PROVIDER_GROUP, MENU_SELECT_NEXT_STOP_PROVIDER_STM_MOBILE, 0,
		        StmMobileTask.SOURCE_NAME);
		subMenu.add(MENU_SELECT_NEXT_STOP_PROVIDER_GROUP, MENU_SELECT_NEXT_STOP_PROVIDER_STM_INFO, 0,
		        StmInfoTask.SOURCE_NAME);
		subMenu.setGroupCheckable(MENU_SELECT_NEXT_STOP_PROVIDER_GROUP, true, true);

		MenuItem menuSearch = menu.add(0, MENU_SEARCH, Menu.NONE, R.string.menu_search);
		menuSearch.setIcon(android.R.drawable.ic_menu_search);
		MenuItem menuPref = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menuPref.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// MyLog.v(TAG, "onPrepareOptionsMenu()");
		if (super.onPrepareOptionsMenu(menu)) {
			SubMenu subMenu = menu.findItem(MENU_SELECT_NEXT_STOP_PROVIDER).getSubMenu();
			for (int i = 0; i < subMenu.size(); i++) {
				if (subMenu.getItem(i).getItemId() == MENU_SELECT_NEXT_STOP_PROVIDER_STM_INFO
				        && getNextStopProviderFromPreferences().equals(
				                UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
					subMenu.getItem(i).setChecked(true);
					break;
				} else if (subMenu.getItem(i).getItemId() == MENU_SELECT_NEXT_STOP_PROVIDER_STM_MOBILE
				        && getNextStopProviderFromPreferences().equals(
				                UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
					subMenu.getItem(i).setChecked(true);
					break;
				}
			}
			if (this.task != null && this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
				// CANCEL REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_REFRESH_NEXT_STOP);
				menuRefresh.setIcon(R.drawable.ic_menu_stop); // not in SDK 1.5!
				menuRefresh.setTitle(R.string.stop_refresh_next_bus_stop);
			} else {
				// REFRESH
				MenuItem menuRefresh = menu.findItem(MENU_REFRESH_NEXT_STOP);
				menuRefresh.setIcon(R.drawable.ic_menu_refresh); // not in SDK 1.5!
				menuRefresh.setTitle(R.string.refresh_next_bus_stop);
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
		case MENU_SHOW_STM_MOBILE_WEBSITE:
			String url = StmMobileTask.getUrlString(busStop.getCode());
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			return true;
		case MENU_REFRESH_NEXT_STOP:
			refreshOrStopRefreshNextStops(null);
			return true;
		case MENU_SHOW_IN_MAPS:
			try {
				// Finding the location of the bus stop
				new GeocodingTask(this, 1, new GeocodingTaskListener() {
					@Override
					public void processLocation(List<Address> addresses) {
						if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
							double lat = addresses.get(0).getLatitude();
							double lng = addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:%s, lng:%s", lat, lng);
							// Launch the map activity
							Uri uri = Uri.parse(String.format("geo:%s,%s", lat, lng)); // geo:0,0?q="+busStop.getPlace()
							startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
						} else {
							Utils.notifyTheUser(BusStopInfo.this, getString(R.string.bus_stop_location_not_found));
						}
					}

				}).execute(this.busStop.getPlace());
				return true;
			} catch (Exception e) {
				MyLog.e(TAG, "Error while launching map", e);
				return false;
			}
		case MENU_USE_RADAR:
			// IF the a radar activity is available DO
			if (!Utils.isIntentAvailable(this, "com.google.android.radar.SHOW_RADAR")) {
				// tell the user he needs to install a radar library.
				NoRadarInstalled noRadar = new NoRadarInstalled(this);
				noRadar.showDialog();
			} else {
				// Finding the location of the bus stop
				new GeocodingTask(this, 1, new GeocodingTaskListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void processLocation(List<Address> addresses) {
						if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
							float lat = (float) addresses.get(0).getLatitude();
							float lng = (float) addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:%s,lng:%s", lat, lng);
							// Launch the radar activity
							Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
							intent.putExtra("latitude", (float) lat);
							intent.putExtra("longitude", (float) lng);
							try {
								startActivity(intent);
							} catch (ActivityNotFoundException ex) {
								MyLog.w(TAG, "Radar activity not found.");
								NoRadarInstalled noRadar = new NoRadarInstalled(BusStopInfo.this);
								noRadar.showDialog();
							}
						} else {
							Utils.notifyTheUser(BusStopInfo.this, getString(R.string.bus_stop_location_not_found));
						}
					}

				}).execute(this.busStop.getPlace());
			}
			return true;
		case MENU_SELECT_NEXT_STOP_PROVIDER_STM_INFO:
			if (!getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				Utils.saveSharedPreferences(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER,
				        UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO);
				// reloadNextBusStops();
			}
			return true;
		case MENU_SELECT_NEXT_STOP_PROVIDER_STM_MOBILE:
			if (!getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				Utils.saveSharedPreferences(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER,
				        UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE);
				// reloadNextBusStops();
			}
			return true;
		case MENU_SEARCH:
			return this.onSearchRequested();
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			return true;
		default:
			MyLog.d(TAG, "Unknow menu action: %s.", item.getItemId());
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER)) {
			// TODO reload if error?
		}
	}

	/**
	 * @return the bus list "group by" preference.
	 */
	private String getNextStopProviderFromPreferences() {
		return Utils.getSharedPreferences(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER,
		        UserPreferences.PREFS_NEXT_STOP_PROVIDER_DEFAULT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.task != null) {
			this.task.cancel(true);
			this.task = null;
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
}

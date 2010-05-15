package org.montrealtransit.android.activity;

import java.util.List;
import java.util.ListIterator;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.BusLineDirection;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.services.ReverseGeocodeTask;
import org.montrealtransit.android.services.ReverseGeocodeTaskListener;
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
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements NextStopListener, View.OnClickListener,
        DialogInterface.OnClickListener, OnSharedPreferenceChangeListener {

	/**
	 * The extra ID for the bus stop code.
	 */
	public static final String EXTRA_STOP_CODE = "extra_stop_code";
	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_STOP_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra ID for the bus line direction.
	 */
	public static final String EXTRA_STOP_LINE_DIRECTION = "extra_line_direction";
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
	 * The cursor used to display the subway station(s).
	 */
	private Cursor cursorSubwayStations;
	/**
	 * The cursor used to display the bus lines.
	 */
	private Cursor cursorBusLines;
	/**
	 * The task used to load the next bus stops.
	 */
	private AsyncTask<StmStore.BusStop, String, BusStopHours> task;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_info);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		getBusStopFromIntent(savedInstanceState);
	}

	/**
	 * Retrieve the bus stop from the Intent or from the Bundle.
	 * @param savedInstanceState the saved instance state (Bundle)
	 */
	private void getBusStopFromIntent(Bundle savedInstanceState) {
		MyLog.v(TAG, "getBusStopFromIntent()");
		final Intent intent = getIntent();
		if (intent != null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				List<String> pathSegments = intent.getData().getPathSegments();
				String stopCode = pathSegments.get(1).substring(0, 5);
				String lineNumber = pathSegments.get(1).substring(5);
				showNewBusStop(stopCode, lineNumber);
			} else {
				String stopCode = Utils.getSavedStringValue(this.getIntent(), savedInstanceState,
				        BusStopInfo.EXTRA_STOP_CODE);
				String lineNumber = Utils.getSavedStringValue(this.getIntent(), savedInstanceState,
				        BusStopInfo.EXTRA_STOP_LINE_NUMBER);
				showNewBusStop(stopCode, lineNumber);
			}
		}
	}

	/**
	 * Set the bus stop in the UI.
	 * @param stopCode the bus stop code
	 */
	private void setStopCode(String stopCode) {
		MyLog.v(TAG, "setStopCode(" + stopCode + ")");
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
	 * Reload the next bus stop date (based on the current bus stop).
	 */
	private void reloadNextBusStops() {
		MyLog.v(TAG, "reloadNextBusStop()");
		((TextView) findViewById(R.id.next_stops_string)).setText(R.string.next_bus_stops);
		// try to retrieve the last configuration instance
		final Object data = getLastNonConfigurationInstance();
		if (data != null) {
			this.hours = (BusStopHours) data;
			setNextStops();
		} else {
			showProgressBar();
			if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				this.task = new StmInfoTask(this, this.getApplicationContext());
				this.task.execute(this.busStop);
			} else if (getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				this.task = new StmMobileTask(this, this.getApplicationContext());
				this.task.execute(this.busStop);
			} else {
				MyLog.w(TAG, "Unknow next stop provider \"" + getNextStopProviderFromPreferences() + "\"");
				this.task = new StmMobileTask(this, this.getApplicationContext()); // default stm mobile
				this.task.execute(this.busStop);
			}
		}
	}

	/**
	 * Show the progress bar and hide the next bus stop text views.
	 */
	private void showProgressBar() {
		((TextView) findViewById(R.id.the_next_stop)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.the_second_next_stop)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.other_stops)).setVisibility(View.GONE);

		((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.progress_bar_please_wait)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.progress_bar_please_wait)).setText(R.string.please_wait);
		((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.VISIBLE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateProgress(String progress) {
		MyLog.v(TAG, "updateProgress(" + progress + ")");
		if (progress != null && progress.length() > 0) {
			((TextView) findViewById(R.id.progress_bar_text)).setText(progress);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPostExectute(BusStopHours result) {
		MyLog.v(TAG, "onPostExectute()");
		this.hours = result;
		setNextStops();
	}

	/**
	 * Hide the progress bar and show the next bus stop text views.
	 */
	private void showNextBusStop() {
		((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.progress_bar_please_wait)).setVisibility(View.GONE);

		((TextView) findViewById(R.id.the_next_stop)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.the_second_next_stop)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.other_stops)).setVisibility(View.VISIBLE);
	}

	/**
	 * Hide the progress bar and show the next bus stop text views.
	 */
	private void showNextBusStopMessage() {
		((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.the_next_stop)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.the_second_next_stop)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.other_stops)).setVisibility(View.GONE);

		((TextView) findViewById(R.id.progress_bar_please_wait)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.GONE);
	}

	/**
	 * Set the next bus stops hours.
	 */
	private void setNextStops() {
		MyLog.v(TAG, "setNextStops(" + hours.getSHours() + ")");
		String nextBusStop = getResources().getString(R.string.next_bus_stops) + " ("
		        + getResources().getString(R.string.data_source) + getResources().getString(R.string.colon)
		        + this.hours.getSourceName() + ")";
		((TextView) findViewById(R.id.next_stops_string)).setText(nextBusStop);
		if (hours.getSHours().size() > 0) {
			List<String> fHours = hours.getFormattedHours(this);
			showNextBusStop();
			// clear the last value
			((TextView) findViewById(R.id.the_next_stop)).setText(null);
			((TextView) findViewById(R.id.other_stops)).setText(null);
			((TextView) findViewById(R.id.the_second_next_stop)).setText(null);
			// show the bus stops
			((TextView) findViewById(R.id.the_next_stop)).setText(fHours.get(0));
			if (fHours.size() > 1) {
				((TextView) findViewById(R.id.the_second_next_stop)).setText(fHours.get(1));
				if (fHours.size() > 2) {
					String hoursS = "";
					for (int i = 2; i < fHours.size(); i++) {
						if (hoursS.length() > 0) {
							hoursS += " ";
						}
						hoursS += fHours.get(i);
					}
					((TextView) findViewById(R.id.other_stops)).setText(hoursS);
				}
			}
		} else {
			showNextBusStopMessage();
			// clear the last value
			((TextView) findViewById(R.id.progress_bar_please_wait)).setText(null);
			((TextView) findViewById(R.id.progress_bar_text)).setText(null);
			// IF an error occurs during the process DO
			if (hours.isError()) {
				((TextView) findViewById(R.id.progress_bar_please_wait)).setText(R.string.error);
			} else {
				// IF there is a secondary message from the STM DO
				if (!TextUtils.isEmpty(this.hours.getMessage2())) {
					((TextView) findViewById(R.id.progress_bar_please_wait)).setText(hours.getMessage2());
					Linkify.addLinks((TextView) findViewById(R.id.progress_bar_please_wait), Linkify.ALL);
					// IF there is also an error message from the STM DO
					if (!TextUtils.isEmpty(this.hours.getMessage())) {
						((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.VISIBLE);
						((TextView) findViewById(R.id.progress_bar_text)).setText(hours.getMessage());
						Linkify.addLinks((TextView) findViewById(R.id.progress_bar_please_wait), Linkify.ALL);
					}
				// ELSE IF there is only an error message from the STM DO
				} else if (!TextUtils.isEmpty(this.hours.getMessage())) {
					((TextView) findViewById(R.id.progress_bar_please_wait)).setText(hours.getMessage());
				// ELSE
				} else {
					// DEFAULT MESSAGE > no more bus stop for this bus line
					String defaultMessage = getResources().getString(R.string.no_more_stops_for_this_bus_line) + " "
					        + this.busLine.getNumber();
					((TextView) findViewById(R.id.progress_bar_please_wait)).setText(defaultMessage);
					((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.GONE);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(" + which + ")");
		if (which == -2) {
			dialog.dismiss();// CANCEL
			this.finish(); // close the activity.
		} else {
			// try to load the next stop from the web.
			reloadNextBusStops();
		}
	}

	/**
	 * The line number index in the the view.
	 */
	private static final int LINE_NUMBER_VIEW_INDEX = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick(" + v.getId() + ")");
		switch (v.getId()) {
		case R.id.star:
			// manage favorite star click
			if (DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop
			        .getCode(), this.busStop.getLineNumber()) != null) {
				// delete the favorite
				DataManager.deleteFav(this.getContentResolver(), DataManager.findFav(this.getContentResolver(),
				        DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(), this.busStop.getLineNumber())
				        .getId());
				Utils.notifyTheUser(this, getResources().getString(R.string.favorite_removed));
			} else {
				// add the favorite
				DataStore.Fav newFav = new DataStore.Fav();
				newFav.setType(DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
				newFav.setFkId(this.busStop.getCode());
				newFav.setFkId2(this.busLine.getNumber());
				DataManager.addFav(this.getContentResolver(), newFav);
				Utils.notifyTheUser(this, getResources().getString(R.string.favorite_added));
			}
			setTheStar(); // TODO is remove useless?
			break;
		case R.id.the_subway_station:
			// IF there is a subway station DO
			if (!TextUtils.isEmpty(this.busStop.getSubwayStationId())) {
				// show subway station info
				Intent intent = new Intent(this, SubwayStationInfo.class);
				intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.busStop.getSubwayStationId());
				startActivity(intent);
			}
			break;
		case R.id.bus_line_view:
			TextView lineNumberTextView = (TextView) ((RelativeLayout) v).getChildAt(LINE_NUMBER_VIEW_INDEX);
			String lineNumber = lineNumberTextView.getText().toString();
			MyLog.d(TAG, "bus line number:" + lineNumber);
			showNewBusStop(this.busStop.getCode(), lineNumber);
			break;
		default:
			MyLog.d(TAG, "Unknown view ID " + v.getId() + " (" + v.getClass().getSimpleName() + ")");
			break;
		}
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		MyLog.v(TAG, "setTheStar()");
		((CheckBox) findViewById(R.id.star)).setChecked(DataManager.findFav(this.getContentResolver(),
		        DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(), this.busStop.getLineNumber()) != null);
	}

	/**
	 * Set the bus stop info basic UI.
	 */
	private void refreshBusStopInfo() {
		MyLog.v(TAG, "refreshBusStopInfo()");
		setStopCode(this.busStop.getCode());
		// set bus stop place name
		((TextView) findViewById(R.id.bus_stop_place)).setText(Utils.cleanBusStopPlace(this.busStop.getPlace()));
		// set the favorite icon
		((CheckBox) findViewById(R.id.star)).setOnClickListener(this);
		// set bus line number
		((TextView) findViewById(R.id.line_number)).setText(this.busLine.getNumber());
		BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(this, this.busLine.getNumber());
		((TextView) findViewById(R.id.line_number)).setOnClickListener(busLineSelectDirection);
		// set bus line name
		((TextView) findViewById(R.id.line_name)).setText(this.busLine.getName());
		((TextView) findViewById(R.id.line_name)).setOnClickListener(busLineSelectDirection);
		// set bus line direction
		String directionId = this.busStop.getDirectionId();
		BusLineDirection busLineDirection = StmManager.findBusLineDirection(this.getContentResolver(), directionId);
		List<Integer> busLineDirectionIds = Utils.getBusLineDirectionStringIdFromId(busLineDirection.getId());
		((TextView) findViewById(R.id.direction_main)).setText(getResources().getString(busLineDirectionIds.get(0)));
		// set the favorite star
		setTheStar();
	}

	/**
	 * Set the bus stop subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "setSubwayStations()");
		if (!TextUtils.isEmpty(this.busStop.getSubwayStationId())) {
			MyLog.d(TAG, "SubwayStationId:" + this.busStop.getSubwayStationId() + ".");
			((TextView) findViewById(R.id.subway_station)).setVisibility(View.VISIBLE);
			((RelativeLayout) findViewById(R.id.the_subway_station)).setVisibility(View.VISIBLE);
			((RelativeLayout) findViewById(R.id.the_subway_station)).setOnClickListener(this);
			((RelativeLayout) findViewById(R.id.the_subway_station)).setFocusable(true);

			StmStore.SubwayStation subwayStation = StmManager.findSubwayStation(this.getContentResolver(), this.busStop
			        .getSubwayStationId());
			((TextView) findViewById(R.id.station_name)).setText(subwayStation.getName());

			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), subwayStation
			        .getId());
			if (subwayLines != null && subwayLines.size() > 0) {
				int subwayLineImg0 = Utils.getSubwayLineImg(subwayLines.get(0).getNumber());
				((ImageView) findViewById(R.id.subway_img_1)).setImageResource(subwayLineImg0);
				if (subwayLines.size() > 1) {
					int subwayLineImg1 = Utils.getSubwayLineImg(subwayLines.get(1).getNumber());
					((ImageView) findViewById(R.id.subway_img_2)).setImageResource(subwayLineImg1);
					if (subwayLines.size() > 2) {
						int subwayLineImg2 = Utils.getSubwayLineImg(subwayLines.get(2).getNumber());
						((ImageView) findViewById(R.id.subway_img_3)).setImageResource(subwayLineImg2);
					} else {
						((ImageView) findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
					}
				} else {
					((ImageView) findViewById(R.id.subway_img_2)).setVisibility(View.GONE);
					((ImageView) findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
				}
			} else {
				((ImageView) findViewById(R.id.subway_img_1)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.subway_img_2)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.subway_img_3)).setVisibility(View.GONE);
			}
		} else {
			((TextView) findViewById(R.id.subway_station)).setVisibility(View.GONE);
			((RelativeLayout) findViewById(R.id.the_subway_station)).setVisibility(View.GONE);
		}
	}

	/**
	 * Set the other bus lines using this bus stop.
	 */
	private void refreshOtherBusLinesInfo() {
		MyLog.v(TAG, "setOtherBusLines()");
		List<BusLine> allBusLines = StmManager.findBusStopLinesList(this.getContentResolver(), this.busStop.getCode());
		// remove all bus line with the same line number
		ListIterator<BusLine> it = allBusLines.listIterator();
		while (it.hasNext()) {
			BusLine busLine = it.next();
			if (busLine.getNumber().equals(this.busLine.getNumber())) {
				it.remove();
			}
		}
		LinearLayout otherBusLinesList = (LinearLayout) findViewById(R.id.other_bus_line_list);
		otherBusLinesList.removeAllViews();
		if (allBusLines.size() > 0) {
			((TextView) findViewById(R.id.other_bus_line)).setVisibility(View.VISIBLE);
			otherBusLinesList.setVisibility(View.VISIBLE);
			for (StmStore.BusLine busLine : allBusLines) {
				if (otherBusLinesList.getChildCount() > 0) {
					otherBusLinesList.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
				}
				View view = getLayoutInflater().inflate(R.layout.bus_stop_info_bus_line_list_item, null);
				int busLineTypeImg = Utils.getBusLineTypeImgFromType(busLine.getType());
				((ImageView) view.findViewById(R.id.line_type)).setImageResource(busLineTypeImg);
				((TextView) view.findViewById(R.id.line_number)).setText(busLine.getNumber());
				((TextView) view.findViewById(R.id.line_name)).setText(busLine.getName());
				String formattedHours = Utils.getFormatted2Hours(this, busLine.getHours(), "-");
				((TextView) view.findViewById(R.id.hours)).setText(formattedHours);
				view.setOnClickListener(this);
				otherBusLinesList.addView(view);
			}
		} else {
			((TextView) findViewById(R.id.other_bus_line)).setVisibility(View.GONE);
			otherBusLinesList.setVisibility(View.GONE);
		}
	}

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newLineNumber the new bus line number (optional)
	 */
	public void showNewBusStop(String newStopCode, String newLineNumber) {
		MyLog.v(TAG, "showNewBusStop(" + newStopCode + ", " + newLineNumber + ")");
		if ((this.busStop == null)
		        || (!this.busStop.getCode().equals(newStopCode) || !this.busStop.getLineNumber().equals(newLineNumber))) {
			MyLog.v(TAG, "New bus stop '" + newStopCode + "' line '" + newLineNumber + "'.");
			setStopCode(newStopCode);
			checkStopCode(newStopCode);

			if (newLineNumber == null) {
				// get the bus lines for this bus stop
				List<BusLine> busLines = StmManager.findBusStopLinesList(getContentResolver(), newStopCode);
				if (busLines == null) {
					// no bus line found
					// TODO HERE handle unknown bus stop code
					String message = getResources().getString(R.string.wrong_stop_code_before) + newStopCode
					        + getResources().getString(R.string.wrong_stop_code_after);
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
		MyLog.w(TAG, "Wrong StopCode '" + wrongStopCode + "'?");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning);
		String message = getResources().getString(R.string.wrong_stop_code_before) + wrongStopCode
		        + getResources().getString(R.string.wrong_stop_code_after) + "\n"
		        + getResources().getString(R.string.wrong_stop_code_after_internet);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Refresh all the UI (based on the bus stop).
	 */
	private void refreshAll() {
		reloadNextBusStops();
		refreshBusStopInfo();
		refreshOtherBusLinesInfo();
		refreshSubwayStationInfo();
		setFocusToTheBusStopPlace();
	}

	/**
	 * Set the focus to the bus stop place.
	 */
	private void setFocusToTheBusStopPlace() {
		// TODO doesn't work!!!
		((TextView) findViewById(R.id.bus_stop_place)).requestFocus();
	}

	/**
	 * The menu item for refreshing the next bus stops.
	 */
	private static final int MENU_SHOW_REFRESH_NEXT_STOP = Menu.FIRST;
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
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 8;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 9;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// MyLog.v(TAG, "onCreateOptionsMenu()");
		MenuItem menuRefresh = menu.add(0, MENU_SHOW_REFRESH_NEXT_STOP, 0, R.string.refresh_next_bus_stop);
		menuRefresh.setIcon(R.drawable.ic_menu_refresh); // TODO use refresh icon from android.R.drawable (bug SDK 1.5)
		menuRefresh.setAlphabeticShortcut('r');
		MenuItem menuStmMobile = menu.add(0, MENU_SHOW_STM_MOBILE_WEBSITE, 0, R.string.see_in_stm_mobile_web_site);
		menuStmMobile.setIcon(R.drawable.stmmobile);
		menuStmMobile.setAlphabeticShortcut('s');
		MenuItem menuMaps = menu.add(0, MENU_SHOW_IN_MAPS, 0, R.string.show_in_map_exp);
		menuMaps.setIcon(android.R.drawable.ic_menu_mapmode);
		menuMaps.setAlphabeticShortcut('m');
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
		// MyLog.v(TAG, "onPrepareOptionsMenu()");
		if (super.onPrepareOptionsMenu(menu)) {
			SubMenu subMenu = menu.findItem(MENU_SELECT_NEXT_STOP_PROVIDER).getSubMenu();
			for (int i = 0; i < subMenu.size(); i++) {
				if (subMenu.getItem(i).getItemId() == MENU_SELECT_NEXT_STOP_PROVIDER_STM_INFO
				        && getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
					subMenu.getItem(i).setChecked(true);
					break;
				} else if (subMenu.getItem(i).getItemId() == MENU_SELECT_NEXT_STOP_PROVIDER_STM_MOBILE
				        && getNextStopProviderFromPreferences().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
					subMenu.getItem(i).setChecked(true);
					break;
				}
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
			String url = "http://m.stm.info/stm/bus/schedule?stop_code=" + this.busStop.getCode();
			if (!TextUtils.isEmpty(this.busStop.getLineNumber())) {
				url += "&line_number=" + this.busStop.getLineNumber();
			}
			if (Utils.getUserLocale().equals("fr")) {
				url += "&lang=" + "fr";
			} else {
				url += "&lang=" + "en";
			}
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			return true;
		case MENU_SHOW_REFRESH_NEXT_STOP:
			reloadNextBusStops();
			return true;
		case MENU_SHOW_IN_MAPS:
			try {
				// Finding the location of the bus stop
				new ReverseGeocodeTask(this, 1, new ReverseGeocodeTaskListener() {
					@Override
					public void processLocation(List<Address> addresses) {
						if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
							double lat = addresses.get(0).getLatitude();
							double lng = addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:" + lat + ", lng:" + lng);
							// Launch the map activity
							Uri uri = Uri.parse("geo:" + lat + "," + lng + ""); // geo:0,0?q="+busStop.getPlace()
							startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
						} else {
							Utils.notifyTheUser(BusStopInfo.this, getResources().getString(
							        R.string.bus_stop_location_not_found));
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
				new ReverseGeocodeTask(this, 1, new ReverseGeocodeTaskListener() {
					@Override
					public void processLocation(List<Address> addresses) {
						if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
							float lat = (float) addresses.get(0).getLatitude();
							float lng = (float) addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:" + lat + ", lng:" + lng);
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
							Utils.notifyTheUser(BusStopInfo.this, getResources().getString(
							        R.string.bus_stop_location_not_found));
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
		case MENU_PREFERENCES:
            startActivity(new Intent(this, UserPreferences.class));
	        return true;
		case MENU_ABOUT:
        	Utils.showAboutDialog(this);
        	return true;
		default:
			MyLog.d(TAG, "Unknow menu action:" + item.getItemId() + ".");
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    if (key.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER)) {
	    	// reloadNextBusStops();
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
		}
		if (this.cursorBusLines != null) {
			this.cursorBusLines.close();
		}
		if (this.cursorSubwayStations != null) {
			this.cursorSubwayStations.close();
		}
		super.onDestroy();
	}
}

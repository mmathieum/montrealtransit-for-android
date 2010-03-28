package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements OnClickListener, NextStopListener, android.content.DialogInterface.OnClickListener, OnItemClickListener,
        ViewBinder {

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
	 * The subway station close to the bus stop (if there is one).
	 */
	private StmStore.SubwayStation subwayStation;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_info);
		((ListView) findViewById(R.id.other_bus_line_list)).setEmptyView(findViewById(R.id.empty_other_bus_line_list));
		((ListView) findViewById(R.id.subway_station_list)).setEmptyView(findViewById(R.id.empty_subway_station_list));

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
				String stopCode = Utils.getSavedStringValue(this.getIntent(), savedInstanceState, BusStopInfo.EXTRA_STOP_CODE);
				String lineNumber = Utils.getSavedStringValue(this.getIntent(), savedInstanceState, BusStopInfo.EXTRA_STOP_LINE_NUMBER);
				showNewBusStop(stopCode, lineNumber);
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
	 * Set the bus stop in the UI.
	 * @param stopCode the bus stop code
	 */
	private void setStopCode(String stopCode) {
		MyLog.v(TAG, "setStopCode(" + stopCode + ")");
		((TextView) findViewById(R.id.stop_code)).setText(stopCode);
	}

	/**
	 * Reload the next bus stop date (based on the current bus stop).
	 */
	private void reloadNextBusStops() {
		MyLog.v(TAG, "reloadNextBusStop()");
		showProgressBar();
		new StmInfoTask(this, this.getApplicationContext()).execute(this.busStop);
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
		hideProgressBar();
		setNextStops(result.getFormattedHours(this));
	}

	/**
	 * Hide the progress bar and show the next bus stop text views.
	 */
	private void hideProgressBar() {
		((ProgressBar) findViewById(R.id.progress_bar)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.progress_bar_text)).setVisibility(View.GONE);
		((TextView) findViewById(R.id.progress_bar_please_wait)).setVisibility(View.GONE);

		((TextView) findViewById(R.id.the_next_stop)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.the_second_next_stop)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.other_stops)).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the next bus stops hours.
	 * @param hours the next bus stop hours.
	 */
	private void setNextStops(List<String> hours) {
		MyLog.v(TAG, "setNextStops(" + Utils.toStringListOfString(hours) + ")");
		// clear the last value
		((TextView) findViewById(R.id.the_next_stop)).setText(null);
		((TextView) findViewById(R.id.other_stops)).setText(null);
		((TextView) findViewById(R.id.the_second_next_stop)).setText(null);
		if (hours.size() > 0) {
			// show the bus stops
			((TextView) findViewById(R.id.the_next_stop)).setText(hours.get(0));
			if (hours.size() > 1) {
				((TextView) findViewById(R.id.the_second_next_stop)).setText(hours.get(1));
				if (hours.size() > 2) {
					String hoursS = "";
					for (int i = 2; i < hours.size(); i++) {
						if (hoursS.length() > 0) {
							hoursS += ", ";
						}
						hoursS += hours.get(i);
					}
					((TextView) findViewById(R.id.other_stops)).setText(hoursS);
				}
			}
		} else {
			// no more bus stop for this bus line
			((TextView) findViewById(R.id.the_second_next_stop)).setText(getResources().getString(R.string.no_more_stops_for_this_bus_line) + " "
			        + this.busLine.getNumber());
			// TODO, show the message from stm.info
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
	 * The activity to show subway station info.
	 */
	private static final int ACTIVITY_VIEW_STATION_INFO = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		MyLog.v(TAG, "onClick(" + v.getId() + ")");
		if (v.getId() == R.id.station_name) {
			// show subway station info
			Intent i = new Intent(this, SubwayStationInfo.class);
			i.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.subwayStation.getId());
			startActivityForResult(i, ACTIVITY_VIEW_STATION_INFO);
		} else {
			// manage favorite star click
			if (DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(), this.busStop.getLineNumber()) != null) {
				// delete the favorite
				DataManager.deleteFav(this.getContentResolver(), DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP,
				        this.busStop.getCode(), this.busStop.getLineNumber()).getId());
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
		}
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		MyLog.v(TAG, "setTheStar()");
		((CheckBox) findViewById(R.id.star)).setChecked(DataManager.findFav(this.getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop
		        .getCode(), this.busStop.getLineNumber()) != null);
	}

	/**
	 * Show the dialog about the unknown bus stop id.
	 * @param wrongStopCode
	 */
	private void showAlertDialog(String wrongStopCode) {
		MyLog.v(TAG, "showAlertDialog()");
		MyLog.w(TAG, "StopPlaceName not found. Wrong StopCode '" + wrongStopCode + "'?");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning);
		String message = getResources().getString(R.string.wrong_stop_code_before) + wrongStopCode + getResources().getString(R.string.wrong_stop_code_after);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		AlertDialog alert = builder.create();
		alert.show();
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
		((TextView) findViewById(R.id.line_number)).setOnClickListener(new BusLineSelectDirection(this, this.busLine.getNumber()));
		// set bus line name
		((TextView) findViewById(R.id.line_name)).setText(this.busLine.getName());
		((TextView) findViewById(R.id.line_name)).setOnClickListener(new BusLineSelectDirection(this, this.busLine.getNumber()));
		// set bus line direction
		BusLineDirection busLineDirection = StmManager.findBusLineDirection(this.getContentResolver(), this.busStop.getDirectionId());
		List<Integer> busLineDirectionIds = Utils.getBusLineDirectionStringIdFromId(busLineDirection.getId());
		((TextView) findViewById(R.id.direction_main)).setText(getResources().getString(busLineDirectionIds.get(0)));
		if (busLineDirectionIds.size() >= 2) {
			TextView tvDirectionDetails = (TextView) findViewById(R.id.direction_detail);
			tvDirectionDetails.setVisibility(View.VISIBLE);
			tvDirectionDetails.setText(getResources().getString(busLineDirectionIds.get(1)));
		}
		// set the favorite star
		setTheStar();
	}

	/**
	 * Set the bus stop subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "setSubwayStations()");
		if (this.busStop.getSubwayStationId() != null && this.busStop.getSubwayStationId().length() > 0) {
			MyLog.d(TAG, "SubwayStationId:" + this.busStop.getSubwayStationId() + ".");
			((TextView) findViewById(R.id.subway_station)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.empty_subway_station_list)).setVisibility(View.VISIBLE);
			((ListView) findViewById(R.id.subway_station_list)).setVisibility(View.VISIBLE);
			((ListView) findViewById(R.id.subway_station_list)).setOnItemClickListener(this);
			List<String> subwayStationIds = new ArrayList<String>();
			subwayStationIds.add(this.busStop.getSubwayStationId());
			((ListView) findViewById(R.id.subway_station_list)).setAdapter(getSubwayListAdapter(subwayStationIds));
		} else {
			((TextView) findViewById(R.id.empty_subway_station_list)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.subway_station)).setVisibility(View.GONE);
			((ListView) findViewById(R.id.subway_station_list)).setVisibility(View.GONE);
		}
	}

	/**
	 * Return the subway list adapter (should contain maximum one subway station).
	 * @param subwayStationsId the subway stations ID (should contains only one station ID).
	 * @return the subway list adapter
	 */
	private ListAdapter getSubwayListAdapter(List<String> subwayStationsId) {
		MyLog.v(TAG, "getSubwayListAdapter(" + Utils.toStringListOfString(subwayStationsId) + ")");
		Cursor cursor = StmManager.findSubwayStations(this.getContentResolver(), subwayStationsId);
		String[] from = new String[] { StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_ID,
		        StmStore.SubwayStation.STATION_NAME /* , StmDbHelper.SUBWAY_HOUR_KEY_HOUR */};
		int[] to = new int[] { R.id.subway_img_1, R.id.subway_img_2, R.id.subway_img_3, R.id.station_name /* , R.id.hours */};
		SimpleCursorAdapter subwayStations = new SimpleCursorAdapter(this, R.layout.bus_stop_info_subway_line_list_item, cursor, from, to);
		subwayStations.setViewBinder(this);
		return subwayStations;
	}

	/**
	 * Set the other bus lines using this bus stop.
	 */
	private void refreshOtherBusLinesInfo() {
		MyLog.v(TAG, "setOtherBusLines()");
		List<String> otherBusLines = Utils.extractBusLineNumbersFromBusLine(StmManager.findBusStopLinesList(this.getContentResolver(), this.busStop.getCode()));
		otherBusLines.remove(this.busLine.getNumber());
		if (otherBusLines.size() > 0) {
			((TextView) findViewById(R.id.other_bus_line)).setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.empty_other_bus_line_list)).setVisibility(View.VISIBLE);
			((ListView) findViewById(R.id.other_bus_line_list)).setVisibility(View.VISIBLE);
			((ListView) findViewById(R.id.other_bus_line_list)).setOnItemClickListener(this);
			((ListView) findViewById(R.id.other_bus_line_list)).setAdapter(getBusLineAdapter(otherBusLines));
		} else {
			((TextView) findViewById(R.id.other_bus_line)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.empty_other_bus_line_list)).setVisibility(View.GONE);
			((ListView) findViewById(R.id.other_bus_line_list)).setVisibility(View.GONE);
		}
	}

	/**
	 * Return the bus lines list adapter for other bus lines.
	 * @param otherBusLineNumbers the other bus lines number.
	 * @return the bus lines list adapter
	 */
	private ListAdapter getBusLineAdapter(List<String> otherBusLineNumbers) {
		MyLog.v(TAG, "getBusLineAdapter(" + Utils.toStringListOfString(otherBusLineNumbers) + ")");
		Cursor cursor = StmManager.findBusLines(this.getContentResolver(), otherBusLineNumbers);
		String[] from = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME, StmStore.BusLine.LINE_HOURS, StmStore.BusLine.LINE_TYPE };
		int[] to = new int[] { R.id.line_number, R.id.line_name, R.id.hours, R.id.line_type };
		SimpleCursorAdapter busLines = new SimpleCursorAdapter(this, R.layout.bus_stop_info_bus_line_list_item, cursor, from, to);
		busLines.setViewBinder(this);
		return busLines;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		MyLog.v(TAG, "setViewValue(" + view.getId() + ", " + columnIndex + ")");
		if (view.getId() == R.id.line_type && columnIndex == cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE)) {
			String type = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE));
			((ImageView) view).setImageResource(Utils.getBusLineTypeImgFromType(type));
			return true;
		} else if (view.getId() == R.id.hours && columnIndex == cursor.getColumnIndex(StmStore.BusLine.LINE_HOURS)) {
			String result = Utils.getFormatted2Hours(this, cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_HOURS)), "-");
			((TextView) view).setText(result);
			return true;
		} else if (view.getId() == R.id.subway_img_1 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), subwayStationID);
			if (subwayLines != null && subwayLines.size() > 0) {
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(subwayLines.get(0).getNumber()));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.subway_img_2 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), subwayStationID);
			if (subwayLines.size() > 1) {
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(subwayLines.get(1).getNumber()));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else if (view.getId() == R.id.subway_img_3 && columnIndex == cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID)) {
			String subwayStationID = cursor.getString(cursor.getColumnIndex(StmStore.SubwayStation.STATION_ID));
			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), subwayStationID);
			if (subwayLines.size() > 2) {
				((ImageView) view).setImageResource(Utils.getSubwayLineImg(subwayLines.get(2).getNumber()));
			} else {
				((ImageView) view).setVisibility(View.GONE);
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + parent.getId() + "," + view.getId() + ", " + position + ", " + id + ")");
		if (parent.getId() == R.id.subway_station_list) {
			if (id > 0) {
				Intent i = new Intent(this, SubwayStationInfo.class);
				i.putExtra(SubwayStationInfo.EXTRA_STATION_ID, String.valueOf(id));
				startActivityForResult(i, ACTIVITY_VIEW_STATION_INFO);
			}
		} else if (parent.getId() == R.id.other_bus_line_list) {
			showNewBusStop(this.busStop.getCode(), String.valueOf(id));
		} else {
			MyLog.w(TAG, "unknown parent.ID:" + parent.getId());
		}
	}

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newLineNumber the new bus line number (optional)
	 */
	public void showNewBusStop(String newStopCode, String newLineNumber) {
		MyLog.v(TAG, "showNewBusStop(" + newStopCode + ", " + newLineNumber + ")");
		if ((this.busStop == null) || (!this.busStop.getCode().equals(newStopCode) || !this.busStop.getLineNumber().equals(newLineNumber))) {
			MyLog.d(TAG, "new bus stop");
			if (newLineNumber == null) {
				// get the bus lines for this bus stop
				List<BusLine> busLines = StmManager.findBusStopLinesList(getContentResolver(), newStopCode);
				if (busLines == null) {
					// TODO HERE handle unknown bus stop code
				} else if (busLines.size() == 1) {
					// use the only bus line available for this bus stop
					newLineNumber = busLines.get(0).getNumber();
				} else {
					// TODO show a bus line selector to the user
					// for now, select the first result
					newLineNumber = busLines.get(0).getNumber();
				}
			}

			setStopCode(newStopCode);
			checkStopCode(newStopCode);
			this.busStop = StmManager.findBusLineStop(this.getContentResolver(), newStopCode, newLineNumber);
			this.busLine = StmManager.findBusLine(this.getContentResolver(), this.busStop.getLineNumber());
			refreshAll();
		}
	}

	/**
	 * Refresh all the UI (based on the bus stop).
	 */
	private void refreshAll() {
		refreshBusStopInfo();
		refreshOtherBusLinesInfo();
		refreshSubwayStationInfo();
		reloadNextBusStops();
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
	private static final int MENU_SHOW_REFRESH_NEXT_STOP = 1;
	/**
	 * The menu item for showing the m.stm.info page of this bus stop.
	 */
	private static final int MENU_SHOW_STM_MOBILE_WEBSITE = 2;
	/**
	 * Menu for showing the bus stop in Maps.
	 */
	private static final int MENU_SHOW_SUBWAY_STATION_IN_MAPS = 3;
	/**
	 * Menu for using a radar to get to the subway station.
	 */
	private static final int MENU_USE_RADAR_TO_THE_SUBWAY_STATION = 4;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO use refresh icon from android.R.drawable... => bug Android 1.5 SDK !!!
		menu.add(0, MENU_SHOW_REFRESH_NEXT_STOP, 0, R.string.refresh_next_bus_stop).setIcon(R.drawable.ic_menu_refresh);
		menu.add(0, MENU_SHOW_STM_MOBILE_WEBSITE, 0, R.string.see_in_stm_mobile_web_site).setIcon(R.drawable.stmmobile);
		menu.add(0, MENU_SHOW_SUBWAY_STATION_IN_MAPS, 0, R.string.show_in_map_exp).setIcon(android.R.drawable.ic_menu_mapmode);
		menu.add(0, MENU_USE_RADAR_TO_THE_SUBWAY_STATION, 0, R.string.use_radar).setIcon(android.R.drawable.ic_menu_compass);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_STM_MOBILE_WEBSITE:
			String url = "http://m.stm.info/stm/bus/schedule?stop_code=" + this.busStop.getCode() + "&line_number=" + this.busStop.getLineNumber();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			break;
		case MENU_SHOW_REFRESH_NEXT_STOP:
			reloadNextBusStops();
			break;
		case MENU_SHOW_SUBWAY_STATION_IN_MAPS:
			try {
				// Finding the location of the bus stop
				new ReverseGeocodeTask(this, 1, new ReverseGeocodeTaskListener() {
					@Override
                    public void processLocation(List<Address> addresses) {
						if (addresses != null && addresses.size() > 0 && addresses.get(0)!=null) {
							double lat = addresses.get(0).getLatitude();
							double lng = addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:"+lat+", lng:"+lng);
							// Launch the map activity
							Uri uri = Uri.parse("geo:"+lat+","+lng+""); // geo:0,0?q="+busStop.getPlace()
							startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
						} else {
							Utils.notifyTheUser(BusStopInfo.this, getResources().getString(R.string.bus_stop_location_not_found));
						}
                    }
					
				}).execute(this.busStop.getPlace());
				return true;
			} catch (Exception e) {
				MyLog.e(TAG, "Error while launching map", e);
				return false;
			}
		case MENU_USE_RADAR_TO_THE_SUBWAY_STATION:
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
						if (addresses != null && addresses.size() > 0 && addresses.get(0)!=null) {
							float lat = (float) addresses.get(0).getLatitude();
							float lng = (float) addresses.get(0).getLongitude();
							MyLog.d(TAG, "Bus stop GPS > lat:"+lat+", lng:"+lng);
							// Launch the radar activity
					        Intent i = new Intent("com.google.android.radar.SHOW_RADAR");
					        i.putExtra("latitude", (float) lat);
					        i.putExtra("longitude", (float) lng);
					        try {
					            startActivity(i);
					        } catch (ActivityNotFoundException ex) {
					        	MyLog.w(TAG, "Radar activity not found.");
					        	NoRadarInstalled noRadar = new NoRadarInstalled(BusStopInfo.this);
								noRadar.showDialog();
					        }
						} else {
							Utils.notifyTheUser(BusStopInfo.this, getResources().getString(R.string.bus_stop_location_not_found));
						}
                    }
					
				}).execute(this.busStop.getPlace());
			}
            return true;
		default:
			MyLog.d(TAG, "Unknow menu action:" + item.getItemId() + ".");
		}
		return false;
	}
}

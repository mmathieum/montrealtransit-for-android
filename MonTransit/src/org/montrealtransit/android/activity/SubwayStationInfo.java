package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.SubwayLine;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;

/**
 * Show information about a subway station.
 * @author Mathieu Méa
 */
public class SubwayStationInfo extends Activity implements OnChildClickListener, OnClickListener {

	/**
	 * Extra for the subway station ID.
	 */
	public static final String EXTRA_STATION_ID = "station_id";
	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayStationInfo.class.getSimpleName();
	/**
	 * The subway station.
	 */
	private StmStore.SubwayStation subwayStation;
	/**
	 * The subway lines.
	 */
	private List<SubwayLine> subwayLines;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_station_info);
		((ExpandableListView) findViewById(R.id.bus_line_list)).setEmptyView(findViewById(R.id.empty_bus_line_list));
		((ExpandableListView) findViewById(R.id.bus_line_list)).setOnChildClickListener(this);
		((TextView) findViewById(R.id.subway_line_string)).setOnClickListener(this);
		((ImageView) findViewById(R.id.subway_line_1)).setOnClickListener(this);
		((ImageView) findViewById(R.id.subway_line_2)).setOnClickListener(this);
		((ImageView) findViewById(R.id.subway_line_3)).setOnClickListener(this);

		showNewSubwayStation(Utils.getSavedStringValue(this.getIntent(), savedInstanceState, SubwayStationInfo.EXTRA_STATION_ID));
	}

	/**
	 * Show a new subway station
	 * @param newStationId the new subway station ID
	 */
	private void showNewSubwayStation(String newStationId) {
		MyLog.v(TAG, "showNewSubwayStation(" + newStationId + ")");
		if (this.subwayStation == null || !this.subwayStation.getId().equals(newStationId)) {
			MyLog.v(TAG, "display a new subway station");
			this.subwayStation = StmManager.findSubwayStation(getContentResolver(), newStationId);
			this.subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(), this.subwayStation.getId());
			refreshAll();
		}
	}

	/**
	 * Refresh all the UI based on the subway station.
	 */
	private void refreshAll() {
		refreshSubwayStationInfo();
		refreshBusLines();
	}

	/**
	 * Refresh the subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		((TextView) findViewById(R.id.station_name)).setText(this.subwayStation.getName());
		if (this.subwayLines.size() > 0) {
			((TextView) findViewById(R.id.subway_line_string)).setText(R.string.subway_line);
			((ImageView) findViewById(R.id.subway_line_1)).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.subway_line_1)).setImageResource(Utils.getSubwayLineImg(this.subwayLines.get(0).getNumber()));
			if (this.subwayLines.size() > 1) {
				((TextView) findViewById(R.id.subway_line_string)).setText(R.string.subway_lines);
				((ImageView) findViewById(R.id.subway_line_2)).setVisibility(View.VISIBLE);
				((ImageView) findViewById(R.id.subway_line_2)).setImageResource(Utils.getSubwayLineImg(this.subwayLines.get(1).getNumber()));
				if (this.subwayLines.size() > 2) {
					((ImageView) findViewById(R.id.subway_line_3)).setVisibility(View.VISIBLE);
					((ImageView) findViewById(R.id.subway_line_3)).setImageResource(Utils.getSubwayLineImg(this.subwayLines.get(2).getNumber()));
				}
			}
		}
		// TODO bus line colors ?
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		if (this.subwayLines.size() == 1) {
			Intent intent = new Intent(this, SubwayLineInfo.class);
			intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, String.valueOf(this.subwayLines.get(0).getNumber()));
			intent.putExtra(SubwayLineInfo.EXTRA_ORDER_ID, StmStore.SubwayLine.DEFAULT_SORT_ORDER);
			startActivity(intent);
		} else {
			//TODO show subway lines selector
		}
	}

	/**
	 * Refresh the bus lines.
	 */
	private void refreshBusLines() {
		MyLog.v(TAG, "refreshBusLines()");
		((ExpandableListView) findViewById(R.id.bus_line_list)).setAdapter(getBusStopsEAdapter());
	}

	/**
	 * The current bus stops (group by bus line).
	 */
	private List<List<Map<String, String>>> currentChildData;
	/**
	 * The current bus lines.
	 */
	private List<Map<String, StmStore.BusLine>> currentGroupData;
	/**
	 * The bus line string.
	 */
	private static final String BUS_LINE = "line";
	/**
	 * The bus line number string.
	 */
	private static final String BUS_LINE_NUMBER = "lineNumber";

	/**
	 * @return the bus stops adapter.
	 */
	private ExpandableListAdapter getBusStopsEAdapter() {
		MyLog.v(TAG, "getBusStopsEAdapter()");
		List<StmStore.BusStop> busStopList = StmManager.findSubwayStationBusStopsExtendedList(getContentResolver(), this.subwayStation.getId());

		if (busStopList != null && busStopList.size() > 0) {

			List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
			this.currentGroupData = new ArrayList<Map<String, StmStore.BusLine>>();
			this.currentChildData = new ArrayList<List<Map<String, String>>>();

			String currentLine = null;
			List<Map<String, String>> currrentChildren = null;
			for (StmStore.BusStop busStop : busStopList) {
				// IF the bus stop isn't a terminus (has a stop code) DO
				if (!TextUtils.isEmpty(busStop.getCode())) {
					// IF this is a bus stop of a new bus line DO
					if (!busStop.getLineNumber().equals(currentLine)) {
						// create a new group for this bus line
						Map<String, StmStore.BusLine> curGroupBusLineMap = new HashMap<String, StmStore.BusLine>();
						Map<String, String> curGroupMap = new HashMap<String, String>();
						// save the current bus line
						currentLine = busStop.getLineNumber();
						BusLine busLine = new BusLine();
						busLine.setNumber(busStop.getLineNumber());
						busLine.setName(busStop.getLineNameOrNull());
						//busLine.setHours(busStop.getLineHoursOrNull());
						busLine.setType(busStop.getLineTypeOrNull());
						curGroupBusLineMap.put(BUS_LINE, busLine);
						curGroupMap.put(BUS_LINE_NUMBER, busLine.getNumber());
						this.currentGroupData.add(curGroupBusLineMap);
						groupData.add(curGroupMap);
						// create the children list
						currrentChildren = new ArrayList<Map<String, String>>();
						this.currentChildData.add(currrentChildren);
	
					}
					Map<String, String> curChildMap = new HashMap<String, String>();
					curChildMap.put(StmStore.BusStop.STOP_CODE, busStop.getCode());
					curChildMap.put(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, busStop.getSimpleDirectionId());
					curChildMap.put(StmStore.BusStop.STOP_LINE_NUMBER, busStop.getLineNumber());
					curChildMap.put(StmStore.BusStop.STOP_PLACE, busStop.getPlace());
					curChildMap.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, busStop.getSubwayStationId());
					curChildMap.put(StmStore.BusStop.LINE_NAME, busStop.getLineNameOrNull());
					currrentChildren.add(curChildMap);
				}
			}

			String[] groupFrom = new String[] { BUS_LINE_NUMBER, BUS_LINE_NUMBER, BUS_LINE_NUMBER };
			int[] groupTo = new int[] { R.id.line_number, R.id.line_name, R.id.line_type };
			String[] childFrom = new String[] { StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID };
			int[] childTo = new int[] { R.id.stop_code, R.id.label, R.id.direction_main };

			MySimpleExpandableListAdapter mAdapter = new MySimpleExpandableListAdapter(this, groupData, R.layout.subway_station_info_bus_stop_list_group_item,
			        groupFrom, groupTo, this.currentChildData, R.layout.subway_station_info_bus_stop_list_item, childFrom, childTo);
			return mAdapter;
		} else {
			return null;
		}
	}

	/**
	 * Simple expandable list adapter to customize the expandable list view for bus line and bus stops.
	 * @author Mathieu Méa
	 */
	private class MySimpleExpandableListAdapter extends SimpleExpandableListAdapter {

		/**
		 * Default constructor @see SimpleExpandableListAdapter
		 */
		public MySimpleExpandableListAdapter(SubwayStationInfo subwayStationInfo, List<Map<String, String>> groupData, int simpleExpandableListItem1,
		        String[] strings, int[] is, List<List<Map<String, String>>> childData, int busLineListItem, String[] childFrom, int[] childTo) {
			super(subwayStationInfo, groupData, simpleExpandableListItem1, strings, is, childData, busLineListItem, childFrom, childTo);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			MyLog.v(TAG, "getChildView(" + groupPosition + "," + childPosition + "," + isLastChild + ")");
			View v;
			if (convertView == null) {
				v = newChildView(isLastChild, parent);
			} else {
				v = convertView;
			}
			bindView(v, currentChildData.get(groupPosition).get(childPosition));
			return v;
		}

		/**
		 * Bind the child view.
		 * @param view the child view
		 * @param data the child data
		 */
		private void bindView(View view, Map<String, String> data) {
			((TextView) view.findViewById(R.id.stop_code)).setText(data.get(StmStore.BusStop.STOP_CODE));
			String busStopPlace = Utils.cleanBusStopPlace(data.get(StmStore.BusStop.STOP_PLACE));
			((TextView) view.findViewById(R.id.label)).setText(busStopPlace);
			Integer simpleBusLineDirectionId = Utils.getBusLineDirectionStringIdFromId(data.get(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID)).get(0);
			((TextView) view.findViewById(R.id.direction_main)).setText(simpleBusLineDirectionId);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = newGroupView(isExpanded, parent);
			} else {
				v = convertView;
			}
			StmStore.BusLine busLine = currentGroupData.get(groupPosition).get(BUS_LINE);
			((TextView) v.findViewById(R.id.line_number)).setText(busLine.getNumber());
			((TextView) v.findViewById(R.id.line_name)).setText(busLine.getName());
			((ImageView) v.findViewById(R.id.line_type)).setImageResource(Utils.getBusLineTypeImgFromType(busLine.getType()));
			return v;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		MyLog.v(TAG, "onChildClick(" + parent.getId() + "," + v.getId() + "," + groupPosition + "," + childPosition + "," + id + ")");
		String busLineNumber = this.currentChildData.get(groupPosition).get(childPosition).get(StmStore.BusStop.STOP_LINE_NUMBER);
		String busStopCode = this.currentChildData.get(groupPosition).get(childPosition).get(StmStore.BusStop.STOP_CODE);
		if (busStopCode != null && busStopCode.length() > 0) {
			Intent intent = new Intent(this, BusStopInfo.class);
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busLineNumber);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStopCode);
			startActivity(intent);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Menu for showing the subway station in Maps.
	 */
	private static final int MENU_SHOW_SUBWAY_STATION_IN_MAPS = Menu.FIRST;
	/**
	 * Menu for using a radar to get to the subway station.
	 */
	private static final int MENU_USE_RADAR_TO_THE_SUBWAY_STATION = Menu.FIRST + 1;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 2;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 3;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuShowMaps = menu.add(0, MENU_SHOW_SUBWAY_STATION_IN_MAPS, Menu.NONE, R.string.show_in_map);
		menuShowMaps.setIcon(android.R.drawable.ic_menu_mapmode);
		MenuItem menuUseRadar = menu.add(0, MENU_USE_RADAR_TO_THE_SUBWAY_STATION, Menu.NONE, R.string.use_radar);
		menuUseRadar.setIcon(android.R.drawable.ic_menu_compass);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_SUBWAY_STATION_IN_MAPS:
			try {
				// + "?q=Station " + this.subwayStation.getName()
				Uri uri = Uri.parse("geo:" + this.subwayStation.getLat() + "," + this.subwayStation.getLng());
				startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
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
				// Launch the radar activity
	            Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
	            intent.putExtra("latitude", (float) this.subwayStation.getLat());
	            intent.putExtra("longitude", (float) this.subwayStation.getLng());
	            try {
	                startActivity(intent);
	            } catch (ActivityNotFoundException ex) {
	            	MyLog.w(TAG, "Radar activity not found.");
	            	NoRadarInstalled noRadar = new NoRadarInstalled(this);
					noRadar.showDialog();
	            }
			}
            return true;
		case MENU_PREFERENCES:
            startActivity(new Intent(this, UserPreferences.class));
	        return true;
		case MENU_ABOUT:
        	Utils.showAboutDialog(this);
        	return true;
		default:
			MyLog.d(TAG, "Unknow menu id: " + item.getItemId() + ".");
			return false;
		}
	}
}

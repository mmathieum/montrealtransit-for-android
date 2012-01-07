package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

/**
 * This activity show a list of the bus lines. They can be grouped by number or name.
 * @author Mathieu Méa
 */
public class BusLineListTab extends Activity implements OnSharedPreferenceChangeListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineListTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BusLines";

	/**
	 * The cursor used to display the bus lines list (in no group mode).
	 */
	private Cursor cursor;

	/**
	 * the adapter used for the expandable list (in group by number)
	 */
	private ExpandableListAdapter adapterByNumber;
	/**
	 * The current group data for the expandable list (in group by number).
	 */
	private ArrayList<List<Map<String, String>>> currentChildDataByNumber;

	/**
	 * the adapter used for the expandable list (in group by type)
	 */
	private ExpandableListAdapter adapterByType;
	/**
	 * The current group data for the expandable list (in group by type).
	 */
	private List<Map<String, String>> currentGroupDataByType;
	/**
	 * The current bus line data (in grouped mode).
	 */
	private List<List<Map<String, String>>> currentChildDataByType;

	/**
	 * the adapter used for the expandable list (in group by day/night)
	 */
	private ExpandableListAdapter adapterByDayNight;
	/**
	 * The current group data for the expandable list (in group by day/night).
	 */
	private List<Map<String, String>> currentGroupDataByDayNight;
	/**
	 * The current bus line data (in grouped mode).
	 */
	private List<List<Map<String, String>>> currentChildDataByDayNight;

	/**
	 * The expandable list view.
	 */
	private ExpandableListView elist;
	/**
	 * The list view.
	 */
	private ListView list;
	/**
	 * The empty view for the expandable list.
	 */
	private View elistEmpty;
	/**
	 * The empty view for the list.
	 */
	private View listEmpty;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_list_tab);

		this.elist = (ExpandableListView) findViewById(R.id.elist);
		this.elistEmpty = findViewById(R.id.elist_empty);
		this.list = (ListView) findViewById(R.id.list);
		this.listEmpty = findViewById(R.id.list_empty);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		this.elist.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				MyLog.v(TAG, "onChildClick(%s, %s, %s, %s, %s)", parent.getId(), v.getId(), groupPosition,
				        childPosition, id);
				if (parent.getId() == R.id.elist) {
					String lineNumber = null;
					if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
						if (BusLineListTab.this.currentChildDataByType != null) {
							lineNumber = BusLineListTab.this.currentChildDataByType.get(groupPosition)
							        .get(childPosition).get(StmStore.BusLine.LINE_NUMBER);
						}
					} else if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
						if (BusLineListTab.this.currentChildDataByDayNight != null) {
							lineNumber = BusLineListTab.this.currentChildDataByDayNight.get(groupPosition)
							        .get(childPosition).get(StmStore.BusLine.LINE_NUMBER);
						}
					} else if (BusLineListTab.this.currentChildDataByNumber != null) {
						lineNumber = BusLineListTab.this.currentChildDataByNumber.get(groupPosition).get(childPosition)
						        .get(StmStore.BusLine.LINE_NUMBER);
					}
					// MyLog.d(TAG, "bus line number: %s.", lineNumber);
					if (lineNumber == null) {
						return false;
					}
					new BusLineSelectDirection(BusLineListTab.this, lineNumber).showDialog();
					return true;
				} else {
					MyLog.w(TAG, "unknown view id: %s", parent.getId());
					return false;
				}
			}
		});
		this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s,%s,%s)", l.getId(), v.getId(), position, id);
				if (l.getId() == R.id.list) {
					// MyLog.d(TAG, "lineNumber: %s", lineNumber);
					new BusLineSelectDirection(BusLineListTab.this, String.valueOf(id)).showDialog();
				} else {
					MyLog.w(TAG, "unknown view id: %s", v.getId());
				}
			}
		});
		// refresh the bus list
		setUpUI();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)
		        || getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)
		        || getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
			showEListView();
		} else {
			showListView();
		}
		super.onRestart();
	}

	/**
	 * Use preferences to set the bus line list.
	 */
	public void setUpUI() {
		MyLog.v(TAG, "setUpUI()");
		if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)
		        || getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)
		        || getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
			// expendable list
			showEListView();
			new SetBusEListTask().execute(getGroupByPref());
		} else {
			// list
			showListView();
			this.list.setAdapter(getAdapterFromSettings(getGroupByPref()));
		}
	}

	/**
	 * Show the expandable list view and hide the list view.
	 */
	private void showEListView() {
		MyLog.v(TAG, "showEListView()");
		this.elist.setVisibility(View.VISIBLE);
		this.elistEmpty.setVisibility(View.VISIBLE);
		this.elist.setEmptyView(this.elistEmpty);

		this.list.setVisibility(View.GONE);
		this.listEmpty.setVisibility(View.GONE);
	}

	/**
	 * show the list view and hide the expandable list view.
	 */
	private void showListView() {
		MyLog.v(TAG, "showListView()");
		this.elist.setVisibility(View.GONE);
		this.elistEmpty.setVisibility(View.GONE);

		this.list.setVisibility(View.VISIBLE);
		this.listEmpty.setVisibility(View.VISIBLE);
		this.list.setEmptyView(this.listEmpty);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// MyLog.v(TAG, "onSharedPreferenceChanged(%s)", key);
		if (key.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY)) {
			setUpUI();
		}
	}

	/**
	 * @return the bus list "group by" preference.
	 */
	private String getGroupByPref() {
		return Utils.getSharedPreferences(this, UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY,
		        UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DEFAULT);
	}

	/**
	 * Return the expandable list adapter from the bus list "group by" preference.</br> <b>WARING:</b> use {@link BusLineListTab#getAdapterFromSettings(String)}
	 * for the list view.
	 * @param busListGroupBy the bus list "group by" preference.
	 * @return the expandable list adapter.
	 */
	private ExpandableListAdapter getEAdapterFromSettings(String busListGroupBy) {
		MyLog.v(TAG, "getEAdapterFromSettings(%s)", busListGroupBy);
		if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)) {
			return getAdapterByNumber();
		} else if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
			return getAdapterByType();
		} else if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
			return getAdapterByDayNight();
		} else {
			MyLog.w(TAG, "Unknow exandable list adapter '%s'", busListGroupBy);
			return getAdapterByNumber(); // default group by (expandable)
		}
	}

	/**
	 * Return the list adapter from the bus list "group by" preference.</br> <b>WARING:</b> use {@link BusLineListTab#getEAdapterFromSettings(String)} for the
	 * expandable list view.
	 * @param busListGroupBy the bus list "group by" preference.
	 * @return the list adapter.
	 */
	private ListAdapter getAdapterFromSettings(String busListGroupBy) {
		MyLog.v(TAG, "getAdapterFromSettings(%s)", busListGroupBy);
		if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP)) {
			return getAdapterNoGroupBy();
		} else {
			MyLog.w(TAG, "Unknow list adapter '%s'", busListGroupBy);
			return getAdapterNoGroupBy(); // default group by
		}
	}

	/**
	 * Return the list adapter for the bus lines list (no group by).
	 * @return the list adapter
	 */
	private ListAdapter getAdapterNoGroupBy() {
		MyLog.v(TAG, "getAdapterNoGroupBy()");
		this.cursor = StmManager.findAllBusLines(this.getContentResolver());
		String[] from = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
		        StmStore.BusLine.LINE_TYPE };
		int[] to = new int[] { R.id.line_number, R.id.line_name, R.id.line_type };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.bus_line_list_item, this.cursor, from, to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()) {
				case R.id.line_type:
					String type = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE));
					((ImageView) view).setImageResource(BusUtils.getBusLineTypeImgFromType(type));
					return true;
				case R.id.line_number:
					String number = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_NUMBER));
					((TextView) view).setText(number);
					String type2 = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE));
					((TextView) view).setBackgroundColor(BusUtils.getBusLineTypeBgColorFromType(type2));
					return true;
				default:
					return false;
				}
			}
		});
		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				MyLog.v(TAG, "runQuery(%s)", constraint);
				return StmManager.searchAllBusLines(BusLineListTab.this.getContentResolver(), constraint.toString());
			}
		});
		startManagingCursor(cursor);
		return adapter;
	}

	/**
	 * Return the expandable list adapter for the bus lines list group by line number.
	 * @return the expandable list adapter
	 */
	private ExpandableListAdapter getAdapterByNumber() {
		MyLog.v(TAG, "getAdapterByNumber()");
		if (this.adapterByNumber == null) {
			List<StmStore.BusLine> busLineList = StmManager.findAllBusLinesList(this.getContentResolver());

			List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
			this.currentChildDataByNumber = new ArrayList<List<Map<String, String>>>();

			int currentGroup = -1;
			List<Map<String, String>> currrentChildren = null;
			for (StmStore.BusLine busLine : busLineList) {
				// IF this is a line of a new group DO
				if (getBusLineGroup(busLine.getNumber()) != currentGroup) {
					// create a new group
					Map<String, String> curGroupMap = new HashMap<String, String>();
					currentGroup = getBusLineGroup(busLine.getNumber());
					int currentGroupI = Integer.valueOf(String.valueOf(currentGroup) + "00");
					curGroupMap.put("lines", getString(R.string.bus_line_string) + " " + currentGroupI + " "
					        + getString(R.string.to) + " " + (currentGroupI + 99));
					groupData.add(curGroupMap);
					// create the children list
					currrentChildren = new ArrayList<Map<String, String>>();
					this.currentChildDataByNumber.add(currrentChildren);

				}
				Map<String, String> curChildMap = new HashMap<String, String>();
				curChildMap.put(StmStore.BusLine.LINE_NUMBER, busLine.getNumber());
				curChildMap.put(StmStore.BusLine.LINE_NAME, busLine.getName());
				curChildMap.put(StmStore.BusLine.LINE_TYPE, busLine.getType());
				currrentChildren.add(curChildMap);
			}

			String[] fromGroup = new String[] { "lines" };
			int[] toGroup = new int[] { android.R.id.text1 };
			String[] fromChild = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
			        StmStore.BusLine.LINE_TYPE };
			int[] toChild = new int[] { R.id.line_number, R.id.line_name, R.id.line_type };
			adapterByNumber = new MySimpleExpandableListAdapter(this, groupData,
			        android.R.layout.simple_expandable_list_item_1, fromGroup, toGroup, this.currentChildDataByNumber,
			        R.layout.bus_line_list_item, fromChild, toChild);
		}
		return this.adapterByNumber;
	}

	/**
	 * Use to know the group of the bus line (when group by number).
	 * @param number the bus line number
	 * @return the group
	 */
	private int getBusLineGroup(String number) {
		if (number.length() < 3) {
			return 0;
		} else {
			return Integer.valueOf(number.substring(0, 1));
		}
	}

	/**
	 * The bus type constant.
	 */
	private static final String BUS_TYPE = "type";

	/**
	 * Return the expandable list adapter for the bus list group by type.
	 * @return the expandable list adapter
	 */
	private ExpandableListAdapter getAdapterByType() {
		MyLog.v(TAG, "getAdapterByType()");
		if (this.adapterByType == null) {
			List<StmStore.BusLine> busLineList = StmManager.findAllBusLinesList(this.getContentResolver());

			List<String> busLineType = new ArrayList<String>();
			busLineType.add(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE);
			busLineType.add(StmStore.BusLine.LINE_TYPE_RUSH_HOUR_SERVICE);
			busLineType.add(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE);
			busLineType.add(StmStore.BusLine.LINE_TYPE_EXPRESS_SERVICE);

			this.currentGroupDataByType = new ArrayList<Map<String, String>>();
			this.currentChildDataByType = new ArrayList<List<Map<String, String>>>();
			Map<String, Integer> childrenId = new HashMap<String, Integer>();

			// create group data (bus line type)
			int id = 0;
			for (String type : busLineType) {
				// create a new group
				Map<String, String> curGroupMap = new HashMap<String, String>();
				curGroupMap.put(BUS_TYPE, type);
				this.currentGroupDataByType.add(curGroupMap);
				List<Map<String, String>> children = new ArrayList<Map<String, String>>();
				childrenId.put(type, id++);
				this.currentChildDataByType.add(children);
			}

			for (StmStore.BusLine busLine : busLineList) {
				Map<String, String> curChildMap = new HashMap<String, String>();
				curChildMap.put(StmStore.BusLine.LINE_NUMBER, busLine.getNumber());
				curChildMap.put(StmStore.BusLine.LINE_NAME, busLine.getName());
				curChildMap.put(StmStore.BusLine.LINE_TYPE, busLine.getType());
				this.currentChildDataByType.get(childrenId.get(busLine.getType())).add(curChildMap);
			}

			String[] fromGroup = new String[] { BUS_TYPE, BUS_TYPE };
			int[] toGroup = new int[] { R.id.bus_type_string, R.id.bus_type_img };
			String[] fromChild = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
			        StmStore.BusLine.LINE_TYPE };
			int[] toChild = new int[] { R.id.line_number, R.id.line_name, R.id.line_type };

			this.adapterByType = new MySimpleExpandableListAdapterType(this, this.currentGroupDataByType,
			        R.layout.bus_line_list_group_item_type, fromGroup, toGroup, this.currentChildDataByType,
			        R.layout.bus_line_list_item, fromChild, toChild);
		}
		return this.adapterByType;
	}

	/**
	 * Return the expandable list adapter for the bus list group by day/night.
	 * @return the expandable list adapter
	 */
	private ExpandableListAdapter getAdapterByDayNight() {
		MyLog.v(TAG, "getAdapterByDayNight()");
		if (this.adapterByDayNight == null) {
			List<StmStore.BusLine> busLines = StmManager.findAllBusLinesList(this.getContentResolver());

			List<String> busLineTypes = new ArrayList<String>();
			busLineTypes.add(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE);
			busLineTypes.add(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE);

			this.currentGroupDataByDayNight = new ArrayList<Map<String, String>>();
			this.currentChildDataByDayNight = new ArrayList<List<Map<String, String>>>();
			Map<String, Integer> childrenId = new HashMap<String, Integer>();

			// create group data (bus line type)
			int id = 0;
			for (String type : busLineTypes) {
				// create a new group
				Map<String, String> curGroupMap = new HashMap<String, String>();
				curGroupMap.put(BUS_TYPE, type);
				this.currentGroupDataByDayNight.add(curGroupMap);
				List<Map<String, String>> children = new ArrayList<Map<String, String>>();
				childrenId.put(type, id++);
				this.currentChildDataByDayNight.add(children);
			}

			for (StmStore.BusLine busLine : busLines) {
				Map<String, String> curChildMap = new HashMap<String, String>();
				curChildMap.put(StmStore.BusLine.LINE_NUMBER, busLine.getNumber());
				curChildMap.put(StmStore.BusLine.LINE_NAME, busLine.getName());
				curChildMap.put(StmStore.BusLine.LINE_TYPE, busLine.getType());
				String type = busLine.getType().equals(StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE) ? StmStore.BusLine.LINE_TYPE_NIGHT_SERVICE
				        : StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE;
				this.currentChildDataByDayNight.get(childrenId.get(type)).add(curChildMap);
			}

			String[] fromGroup = new String[] { BUS_TYPE, BUS_TYPE };
			int[] toGroup = new int[] { R.id.bus_type_string, R.id.bus_type_img };
			String[] fromChild = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
			        StmStore.BusLine.LINE_TYPE };
			int[] toChild = new int[] { R.id.line_number, R.id.line_name, R.id.line_type };

			this.adapterByDayNight = new MySimpleExpandableListAdapterDayNight(this, this.currentGroupDataByDayNight,
			        R.layout.bus_line_list_group_item_type, fromGroup, toGroup, this.currentChildDataByDayNight,
			        R.layout.bus_line_list_item, fromChild, toChild);
		}
		return this.adapterByDayNight;
	}

	/**
	 * Switch to line list (no group).
	 * @param v the view (not used)
	 */
	public void switchToList(View v) {
		switchToListType(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP);
	}

	/**
	 * Switch to line list group by type.
	 * @param v the view (not used)
	 */
	public void switchToListGroupByType(View v) {
		switchToListType(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE);
	}

	/**
	 * Switch to line list group by day/night.
	 * @param v the view (not used)
	 */
	public void switchToListGroupByDayNight(View v) {
		switchToListType(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT);
	}

	/**
	 * Switch to line list group by number.
	 * @param v the view (not used)
	 */
	public void switchToListGroupByNumber(View v) {
		switchToListType(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER);
	}

	/**
	 * Switch to a new line list view type.
	 * @param typePref the new type (preference).
	 */
	private void switchToListType(String typePref) {
		if (!getGroupByPref().equals(typePref)) {
			Utils.saveSharedPreferences(this, UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY, typePref);
			setUpUI();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_line_list_menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MyLog.v(TAG, "onPrepareOptionsMenu()");
		if (super.onPrepareOptionsMenu(menu)) {
			if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)) {
				menu.findItem(R.id.group_by_number).setChecked(true);
			} else if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
				menu.findItem(R.id.group_by_type).setChecked(true);
			} else if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
				menu.findItem(R.id.group_by_daynight).setChecked(true);
			} else {
				menu.findItem(R.id.group_by_no).setChecked(true);
			}
			return true;
		} else {
			MyLog.w(TAG, "Error in onPrepareOptionsMenu().");
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.group_by_no:
			switchToList(null);
			return true;
		case R.id.group_by_type:
			switchToListGroupByType(null);
			return true;
		case R.id.group_by_daynight:
			switchToListGroupByDayNight(null);
			return true;
		case R.id.group_by_number:
			switchToListGroupByNumber(null);
			return true;
		default:
			return MenuUtils.handleCommonMenuActions(this, item);
		}
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	/**
	 * This task create the expandable list adapter in a other thread.
	 */
	private class SetBusEListTask extends AsyncTask<String, String, ExpandableListAdapter> {

		@Override
		protected ExpandableListAdapter doInBackground(String... arg0) {
			String busListGroupBy = arg0[0];
			// load the adapter
			return getEAdapterFromSettings(busListGroupBy);
		}

		@Override
		protected void onPostExecute(ExpandableListAdapter result) {
			MyLog.v(TAG, "onPostExecute()");
			BusLineListTab.this.elist.setAdapter(result);
			super.onPostExecute(result);
		}
	}

	/**
	 * A simple expandable list adapter based on {@link SimpleExpandableListAdapter}. Add the customization of the child view (line type img).
	 */
	private class MySimpleExpandableListAdapter extends SimpleExpandableListAdapter {

		/**
		 * @see {@link SimpleExpandableListAdapter}
		 */
		public MySimpleExpandableListAdapter(BusLineListTab expandableBusListTab, List<Map<String, String>> groupData,
		        int simpleExpandableListItem1, String[] strings, int[] is, List<List<Map<String, String>>> childData,
		        int busLineListItem, String[] childFrom, int[] childTo) {
			super(expandableBusListTab, groupData, simpleExpandableListItem1, strings, is, childData, busLineListItem,
			        childFrom, childTo);
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
		        ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = newChildView(isLastChild, parent);
			} else {
				v = convertView;
			}
			if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
				bindView(v, currentChildDataByType.get(groupPosition).get(childPosition));
			} else if (getGroupByPref().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT)) {
				bindView(v, currentChildDataByDayNight.get(groupPosition).get(childPosition));
			} else {
				bindView(v, currentChildDataByNumber.get(groupPosition).get(childPosition));
			}
			return v;
		}

		/**
		 * Bind the view with the data values.
		 * @param view the view
		 * @param data the data values
		 */
		private void bindView(View view, Map<String, String> data) {
			((TextView) view.findViewById(R.id.line_number)).setText(data.get(StmStore.BusLine.LINE_NUMBER));
			int color = BusUtils.getBusLineTypeBgColorFromType(data.get(StmStore.BusLine.LINE_TYPE));
			((TextView) view.findViewById(R.id.line_number)).setBackgroundColor(color);
			((TextView) view.findViewById(R.id.line_name)).setText(data.get(StmStore.BusLine.LINE_NAME));
			int busImg = BusUtils.getBusLineTypeImgFromType(data.get(StmStore.BusLine.LINE_TYPE));
			((ImageView) view.findViewById(R.id.line_type)).setImageResource(busImg);
		}
	}

	/**
	 * A custom expandable list adapter based on {@link MySimpleExpandableListAdapter}. Add the group view customization.
	 */
	private class MySimpleExpandableListAdapterType extends MySimpleExpandableListAdapter {

		/**
		 * @see MySimpleExpandableListAdapter#MySimpleExpandableListAdapter(BusLineListTab, List, int, String[], int[], List, int, String[], int[])
		 */
		public MySimpleExpandableListAdapterType(BusLineListTab expandableBusListTab,
		        List<Map<String, String>> groupData, int simpleExpandableListItem1, String[] strings, int[] is,
		        List<List<Map<String, String>>> childData, int busLineListItem, String[] childFrom, int[] childTo) {
			super(expandableBusListTab, groupData, simpleExpandableListItem1, strings, is, childData, busLineListItem,
			        childFrom, childTo);
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = newGroupView(isExpanded, parent);
			} else {
				v = convertView;
			}
			String type = currentGroupDataByType.get(groupPosition).get(BUS_TYPE);
			((TextView) v.findViewById(R.id.bus_type_string)).setText(BusUtils.getBusStringFromType(type));
			((ImageView) v.findViewById(R.id.bus_type_img)).setImageResource(BusUtils.getBusLineTypeImgFromType(type));
			return v;
		}
	}

	/**
	 * A custom expandable list adapter based on {@link MySimpleExpandableListAdapter}. Add the group view customization.
	 */
	private class MySimpleExpandableListAdapterDayNight extends MySimpleExpandableListAdapter {

		/**
		 * @see MySimpleExpandableListAdapter#MySimpleExpandableListAdapter(BusLineListTab, List, int, String[], int[], List, int, String[], int[])
		 */
		public MySimpleExpandableListAdapterDayNight(BusLineListTab expandableBusListTab,
		        List<Map<String, String>> groupData, int simpleExpandableListItem1, String[] strings, int[] is,
		        List<List<Map<String, String>>> childData, int busLineListItem, String[] childFrom, int[] childTo) {
			super(expandableBusListTab, groupData, simpleExpandableListItem1, strings, is, childData, busLineListItem,
			        childFrom, childTo);
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = newGroupView(isExpanded, parent);
			} else {
				v = convertView;
			}
			String type = currentGroupDataByDayNight.get(groupPosition).get(BUS_TYPE);
			int serviceString = type.equals(StmStore.BusLine.LINE_TYPE_REGULAR_SERVICE) ? R.string.bus_line_service_day
			        : R.string.bus_line_service_night;
			((TextView) v.findViewById(R.id.bus_type_string)).setText(serviceString);
			((ImageView) v.findViewById(R.id.bus_type_img)).setImageResource(BusUtils.getBusLineTypeImgFromType(type));
			return v;
		}
	}
}

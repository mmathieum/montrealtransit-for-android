package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
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
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_line_list_tab);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		((ExpandableListView) findViewById(R.id.elist))
		        .setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			        @Override
			        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
			                int childPosition, long id) {
				        MyLog.v(TAG, "onChildClick(" + parent.getId() + "," + v.getId() + "," + groupPosition + ","
				                + childPosition + "," + id + ")");
				        if (parent.getId() == R.id.elist) {
					        String lineNumber;
					        if (getBusListGroupByFromPreferences().equals(
					                UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
						        lineNumber = BusLineListTab.this.currentChildDataByType.get(groupPosition).get(
						                childPosition).get(StmStore.BusLine.LINE_NUMBER);
					        } else {
						        lineNumber = BusLineListTab.this.currentChildDataByNumber.get(groupPosition).get(
						                childPosition).get(StmStore.BusLine.LINE_NUMBER);
					        }
					        MyLog.v(TAG, "bus line number:" + lineNumber + ".");
					        BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(
					                BusLineListTab.this, lineNumber);
					        busLineSelectDirection.showDialog();
					        return true;
				        } else {
					        MyLog.w(TAG, "unknown view id:" + parent.getId());
					        return false;
				        }
			        }
		        });
		((ListView) findViewById(R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(" + l.getId() + "," + v.getId() + "," + position + "," + id + ")");
				if (l.getId() == R.id.list) {
					String lineNumber = String.valueOf(id);
					MyLog.v(TAG, "lineNumber:" + lineNumber);
					BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(BusLineListTab.this,
					        lineNumber);
					busLineSelectDirection.showDialog();
				} else {
					MyLog.w(TAG, "unknown view id:" + v.getId());
				}
			}
		});
		// refresh the bus list
		refreshAll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		if (getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)
		        || getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
			showEListView();
		} else {
			showListView();
		}
		super.onRestart();
	}

	/**
	 * Use preferences to set the bus line list.
	 */
	private void refreshAll() {
		MyLog.v(TAG, "refreshAll()");
		if (getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)
		        || getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
			showEListView();
			new SetBusEListTask().execute(getBusListGroupByFromPreferences());
		} else {
			showListView();
			((ListView) findViewById(R.id.list)).setAdapter(getAdapterFromSettings(getBusListGroupByFromPreferences()));
		}
	}

	/**
	 * This task create the expandable list adapter in a other thread.
	 * @author Mathieu Méa
	 */
	private class SetBusEListTask extends AsyncTask<String, String, ExpandableListAdapter> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected ExpandableListAdapter doInBackground(String... arg0) {
			String busListGroupBy = arg0[0];
			// load the adapter
			return getEAdapterFromSettings(busListGroupBy);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(ExpandableListAdapter result) {
			MyLog.v(TAG, "onPostExecute()");
			((ExpandableListView) findViewById(R.id.elist)).setAdapter(result);
			super.onPostExecute(result);
		}

	}

	/**
	 * Show the expandable list view and hide the list view.
	 */
	private void showEListView() {
		MyLog.v(TAG, "showEListView()");
		findViewById(R.id.elist).setVisibility(View.VISIBLE);
		findViewById(R.id.elist_empty).setVisibility(View.VISIBLE);
		((ExpandableListView) findViewById(R.id.elist)).setEmptyView(findViewById(R.id.elist_empty));

		findViewById(R.id.list).setVisibility(View.GONE);
		findViewById(R.id.list_empty).setVisibility(View.GONE);
	}

	/**
	 * show the list view and hide the expandable list view.
	 */
	private void showListView() {
		MyLog.v(TAG, "showListView()");
		findViewById(R.id.elist).setVisibility(View.GONE);
		findViewById(R.id.elist_empty).setVisibility(View.GONE);

		findViewById(R.id.list).setVisibility(View.VISIBLE);
		findViewById(R.id.list_empty).setVisibility(View.VISIBLE);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
	}

	/**
	 * The menu group for the group by setting.
	 */
	private static final int MENU_GROUP_BY_GROUP = Menu.FIRST;
	/**
	 * The menu for the group by setting.
	 */
	private static final int MENU_GROUP_BY = Menu.FIRST;
	/**
	 * The menu item to show the list without group by.
	 */
	private static final int MENU_GROUP_BY_NO_GROUP_BY = Menu.FIRST + 1;
	/**
	 * The menu item to show the list group by type.
	 */
	private static final int MENU_GROUP_BY_TYPE = Menu.FIRST + 2;
	/**
	 * The menu item to show the list group by number.
	 */
	private static final int MENU_GROUP_BY_NUMBER = Menu.FIRST + 3;
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
		SubMenu subMenu = menu.addSubMenu(MENU_GROUP_BY_GROUP, MENU_GROUP_BY, 0, R.string.select_group_by);
		subMenu.setIcon(android.R.drawable.ic_menu_view);
		subMenu.add(MENU_GROUP_BY_GROUP, MENU_GROUP_BY_NUMBER, Menu.NONE, R.string.group_by_bus_line_number);
		subMenu.add(MENU_GROUP_BY_GROUP, MENU_GROUP_BY_TYPE, Menu.NONE, R.string.group_by_bus_line_type);
		subMenu.add(MENU_GROUP_BY_GROUP, MENU_GROUP_BY_NO_GROUP_BY, Menu.NONE, R.string.group_by_bus_line_no_group);
		subMenu.setGroupCheckable(MENU_GROUP_BY_GROUP, true, true);

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
			SubMenu subMenu = menu.findItem(MENU_GROUP_BY).getSubMenu();
			for (int i = 0; i < subMenu.size(); i++) {
				if (subMenu.getItem(i).getItemId() == MENU_GROUP_BY_NO_GROUP_BY
				        && getBusListGroupByFromPreferences().equals(
				                UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP)) {
					subMenu.getItem(i).setChecked(true);
					break;
				} else if (subMenu.getItem(i).getItemId() == MENU_GROUP_BY_TYPE
				        && getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
					subMenu.getItem(i).setChecked(true);
					break;
				} else if (subMenu.getItem(i).getItemId() == MENU_GROUP_BY_NUMBER
				        && getBusListGroupByFromPreferences().equals(
				                UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)) {
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
		case MENU_GROUP_BY_GROUP:
			return false;
		case MENU_GROUP_BY_NO_GROUP_BY:
			if (!getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP)) {
				Utils.saveSharedPreferences(this, UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY,
				        UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP);
				refreshAll();
			}
			return true;
		case MENU_GROUP_BY_TYPE:
			if (!getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
				Utils.saveSharedPreferences(this, UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY,
				        UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE);
				refreshAll();
			}
			return true;
		case MENU_GROUP_BY_NUMBER:
			if (!getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)) {
				Utils.saveSharedPreferences(this, UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY,
				        UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER);
				refreshAll();
			}
			return true;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			return true;
		case MENU_ABOUT:
			Utils.showAboutDialog(this);
			return true;
		default:
			MyLog.w(TAG, "Unknow menu action:" + item.getItemId() + ".");
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		MyLog.d(TAG, "onSharedPreferenceChanged(" + key + ")");
		if (key.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY)) {
			refreshAll();
		}
	}

	/**
	 * @return the bus list "group by" preference.
	 */
	private String getBusListGroupByFromPreferences() {
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
		MyLog.v(TAG, "getEAdapterFromSettings(" + busListGroupBy + ")");
		if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER)) {
			return getAdapterByNumber();
		} else if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
			return getAdapterByType();
		} else {
			MyLog.w(TAG, "Unknow exandable list adapter \"" + busListGroupBy + "\"");
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
		MyLog.v(TAG, "getAdapterFromSettings(" + busListGroupBy + ")");
		if (busListGroupBy.equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP)) {
			return getAdapterNoGroupBy();
		} else {
			MyLog.w(TAG, "Unknow list adapter \"" + busListGroupBy + "\"");
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
		        StmStore.BusLine.LINE_HOURS, StmStore.BusLine.LINE_TYPE };
		int[] to = new int[] { R.id.line_number, R.id.line_name, R.id.hours, R.id.line_type };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.bus_line_list_item, this.cursor, from, to);
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				switch (view.getId()) {
				case R.id.line_type:
					String type = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE));
					((ImageView) view).setImageResource(Utils.getBusLineTypeImgFromType(type));
					return true;
				case R.id.hours:
					String shours = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_HOURS));
					((TextView) view).setText(Utils.getFormatted2Hours(BusLineListTab.this, shours, "-"));
					return true;
				case R.id.line_number:
					String number = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_NUMBER));
					((TextView) view).setText(number);
					String type2 = cursor.getString(cursor.getColumnIndex(StmStore.BusLine.LINE_TYPE));
					((TextView) view).setBackgroundColor(Utils.getBusLineTypeBgColorFromType(type2));
					return true;
				default:
					return false;
				}
			}
		});
		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				MyLog.v(TAG, "runQuery(" + constraint + ")");
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
				curChildMap.put(StmStore.BusLine.LINE_HOURS, busLine.getHours());
				currrentChildren.add(curChildMap);
			}

			String[] fromGroup = new String[] { "lines" };
			int[] toGroup = new int[] { android.R.id.text1 };
			String[] fromChild = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
			        StmStore.BusLine.LINE_HOURS, StmStore.BusLine.LINE_TYPE };
			int[] toChild = new int[] { R.id.line_number, R.id.line_name, R.id.hours, R.id.line_type };
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
	 * A simple expandable list adapter based on {@link SimpleExpandableListAdapter}. Add the customization of the child view (line type img).
	 * @author Mathieu Méa
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

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
		        ViewGroup parent) {
			View v;
			if (convertView == null) {
				v = newChildView(isLastChild, parent);
			} else {
				v = convertView;
			}
			if (getBusListGroupByFromPreferences().equals(UserPreferences.PREFS_BUS_LINE_LIST_GROUP_BY_TYPE)) {
				bindView(v, currentChildDataByType.get(groupPosition).get(childPosition));
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
			int color = Utils.getBusLineTypeBgColorFromType(data.get(StmStore.BusLine.LINE_TYPE));
			((TextView) view.findViewById(R.id.line_number)).setBackgroundColor(color);
			((TextView) view.findViewById(R.id.line_name)).setText(data.get(StmStore.BusLine.LINE_NAME));
			String hours = Utils.getFormatted2Hours(BusLineListTab.this, data.get(StmStore.BusLine.LINE_HOURS), "-");
			((TextView) view.findViewById(R.id.hours)).setText(hours);
			int busImg = Utils.getBusLineTypeImgFromType(data.get(StmStore.BusLine.LINE_TYPE));
			((ImageView) view.findViewById(R.id.line_type)).setImageResource(busImg);
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
			busLineType.add(StmStore.BusLine.LINE_TYPE_METROBUS_SERVICE);
			busLineType.add(StmStore.BusLine.LINE_TYPE_TRAINBUS);
			busLineType.add(StmStore.BusLine.LINE_TYPE_EXPRESS_SERVICE);
			busLineType.add(StmStore.BusLine.LINE_TYPE_RESERVED_LANE_SERVICE);

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
				curChildMap.put(StmStore.BusLine.LINE_HOURS, busLine.getHours());
				this.currentChildDataByType.get(childrenId.get(busLine.getType())).add(curChildMap);
			}

			String[] fromGroup = new String[] { BUS_TYPE, BUS_TYPE };
			int[] toGroup = new int[] { R.id.bus_type_string, R.id.bus_type_img };
			String[] fromChild = new String[] { StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME,
			        StmStore.BusLine.LINE_HOURS, StmStore.BusLine.LINE_TYPE };
			int[] toChild = new int[] { R.id.line_number, R.id.line_name, R.id.hours, R.id.line_type };

			this.adapterByType = new MySimpleExpandableListAdapterType(this, this.currentGroupDataByType,
			        R.layout.bus_line_list_group_item_type, fromGroup, toGroup, this.currentChildDataByType,
			        R.layout.bus_line_list_item, fromChild, toChild);
		}
		return this.adapterByType;
	}

	/**
	 * A custom expandable list adapter based on {@link MySimpleExpandableListAdapter}. Add the group view customization.
	 * @author Mathieu Méa
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
			String type = currentGroupDataByType.get(groupPosition).get(BUS_TYPE);
			((TextView) v.findViewById(R.id.bus_type_string)).setText(Utils.getBusStringFromType(type));
			((ImageView) v.findViewById(R.id.bus_type_img)).setImageResource(Utils.getBusLineTypeImgFromType(type));
			return v;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
}

package org.montrealtransit.android.activity;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * Display a list of subway line.
 * @author Mathieu Méa
 */
public class SubwayLinesListTab extends Activity implements ViewBinder, OnItemClickListener, OnItemLongClickListener,
        FilterQueryProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLinesListTab.class.getSimpleName();
	
	/**
	 * The cursor used to display the subway lines.
	 */
	private Cursor cursor;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_list_tab);
		((ListView) findViewById(R.id.list)).setEmptyView(findViewById(R.id.list_empty));
		((ListView) findViewById(R.id.list)).setOnItemClickListener(this);
		((ListView) findViewById(R.id.list)).setOnItemLongClickListener(this);
		((ListView) findViewById(R.id.list)).setAdapter(getAdapter());
	}

	/**
	 * @return the subway line list adapter
	 */
	private ListAdapter getAdapter() {
		this.cursor = StmManager.findAllSubwayLines(this.getContentResolver());
		String[] from = new String[] { StmStore.SubwayLine.LINE_NUMBER };
		int[] to = new int[] { R.id.line_name };
		SimpleCursorAdapter subwayLines = new SimpleCursorAdapter(this, R.layout.subway_line_list_tab_item, this.cursor, from, to);
		subwayLines.setViewBinder(this);
		subwayLines.setFilterQueryProvider(this);
		return subwayLines;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor runQuery(CharSequence constraint) {
	    return StmManager.searchAllSubwayLines(this.getContentResolver(), constraint.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if (view.getId() == R.id.line_name && columnIndex == cursor.getColumnIndex(StmStore.SubwayLine.LINE_NUMBER)) {
			int subwayLineId = cursor.getInt(cursor.getColumnIndex(StmStore.SubwayLine.LINE_NUMBER));
			((TextView) view).setTextColor(Utils.getSubwayLineColor(subwayLineId));
			((TextView) view).setText(getResources().getString(Utils.getSubwayLineName(subwayLineId)));
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		// show the subway station in default (A-Z) older by default
		Intent intent = new Intent(this, SubwayLineInfo.class);
		intent.putExtra(SubwayLineInfo.EXTRA_LINE_NUMBER, String.valueOf(id));
		intent.putExtra(SubwayLineInfo.EXTRA_ORDER_ID, StmStore.SubwayLine.DEFAULT_SORT_ORDER);
		startActivity(intent);
		// TODO ? use user settings to save the last order choose by the user for each subway lines ?
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {
		MyLog.v(TAG, "onItemLongClick(" + v.getId() + "," + v.getId() + "," + position + "," + id + ")");
		SubwayLineSelectDirection subwayLineSelectDirection = new SubwayLineSelectDirection(this, Integer.valueOf(String.valueOf(id)));
		subwayLineSelectDirection.showDialog();
		return true;
	}

	/**
	 * Menu to show the subway map from the STM.info Web Site.
	 */
	private static final int MENU_SHOW_MAP_ON_THE_STM_WEBSITE = Menu.FIRST;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 1;
	/**
	 * The menu used to show the about screen.
	 */
	private static final int MENU_ABOUT = Menu.FIRST + 2;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_MAP_ON_THE_STM_WEBSITE:
			String url = "http://www.stm.info/metro/images/plan-metro.jpg";
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			break;
		case MENU_PREFERENCES:
            startActivity(new Intent(this, UserPreferences.class));
	        break;
		case MENU_ABOUT:
        	Utils.showAboutDialog(this);
        	break;
		default:
			MyLog.d(TAG, "Unknow menu action:" + item.getItemId() + ".");
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.cursor!=null) {this.cursor.close(); }
	    super.onDestroy();
	}
}

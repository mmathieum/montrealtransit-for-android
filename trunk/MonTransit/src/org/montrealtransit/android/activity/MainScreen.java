package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost.TabSpec;

/**
 * This class is the first screen displayed by the application. It contains, 4 tabs.
 * @author Mathieu Méa
 */
// TODO offer the options to just show a list of the "tabs" == Dashboard UI
public class MainScreen extends TabActivity {

	/**
	 * The log tag.
	 */
	private static final String TAG = MainScreen.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Main";
	/**
	 * The favorite tab ID.
	 */
	private static final String TAB_FAV = "tab_fav";
	/**
	 * The bus stop code search tab ID.
	 */
	private static final String TAB_STOP_CODE = "tab_stop_code";
	/**
	 * The bus lines list tab ID.
	 */
	private static final String TAB_BUS = "tab_bus";
	/**
	 * The subway lines list tab ID.
	 */
	private static final String TAB_SUBWAY = "tab_subway";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utils.logAppVersion(this);
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI.
		setContentView(R.layout.main_screen);

		// the favorites list
		TabSpec favListTab = getTabHost().newTabSpec(TAB_FAV);
		favListTab.setIndicator(getString(R.string.favorite), getResources().getDrawable(R.drawable.ic_tab_starred));
		favListTab.setContent(new Intent(this, FavListTab.class));
		getTabHost().addTab(favListTab);
		// the bus stop code
		TabSpec busStopCodeTab = getTabHost().newTabSpec(TAB_STOP_CODE);
		busStopCodeTab.setIndicator(getString(R.string.stop_code), getResources().getDrawable(
		        R.drawable.ic_tab_stop_code));
		busStopCodeTab.setContent(new Intent(this, BusStopCodeTab.class));
		getTabHost().addTab(busStopCodeTab);
		// the bus lines list
		TabSpec busLinesListTab = getTabHost().newTabSpec(TAB_BUS);
		busLinesListTab.setIndicator(getString(R.string.bus), getResources().getDrawable(R.drawable.ic_tab_bus));
		busLinesListTab.setContent(new Intent(this, BusLineListTab.class));
		getTabHost().addTab(busLinesListTab);
		// the subway lines list
		TabSpec subwayLinesListTab = getTabHost().newTabSpec(TAB_SUBWAY);
		subwayLinesListTab.setIndicator(getString(R.string.subway), getResources()
		        .getDrawable(R.drawable.ic_tab_subway));
		subwayLinesListTab.setContent(new Intent(this, SubwayTab.class));
		getTabHost().addTab(subwayLinesListTab);
		try {
			// IF there is one or more favorites DO
			if (Utils.getListSize(DataManager.findAllFavsList(this.getContentResolver())) > 0) {
				getTabHost().setCurrentTab(0); // show favorite tab
			} else {
				getTabHost().setCurrentTab(1); // show bus stop code search tab
			}
		} catch (Exception e) {
			MyLog.w(TAG, "Error while determing the select tab", e);
		}
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
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		if (TwitterUtils.isTwitterCallback(intent)) {
			TwitterUtils.getInstance().login(this, intent.getData());
		}
	    super.onNewIntent(intent);
	}
}
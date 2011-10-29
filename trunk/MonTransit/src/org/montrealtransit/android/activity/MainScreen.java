package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

/**
 * This class is the main screen displayed by the application. It contains, 4 tabs.
 * @author Mathieu Méa
 */
// TODO offer the options to just show a list of the "tabs" == Dashboard UI
@SuppressWarnings("deprecation") // TODO use Fragment
public class MainScreen extends ActivityGroup {

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI.
		setContentView(R.layout.main_screen);

		// add the tab host
		TabHost tabHost = (TabHost) getLayoutInflater().inflate(R.layout.main_screen_tab_host, null);
		tabHost.setup(this.getLocalActivityManager());
		// the favorites list
		TabSpec favListTab = tabHost.newTabSpec(TAB_FAV);
		favListTab.setIndicator(getString(R.string.favorite), getResources().getDrawable(R.drawable.ic_tab_starred));
		favListTab.setContent(new Intent(this, FavListTab.class));
		tabHost.addTab(favListTab);
		// the bus stop code
		TabSpec busStopCodeTab = tabHost.newTabSpec(TAB_STOP_CODE);
		busStopCodeTab.setIndicator(getString(R.string.stop_code),
		        getResources().getDrawable(R.drawable.ic_tab_stop_code));
		busStopCodeTab.setContent(new Intent(this, BusStopCodeTab.class));
		tabHost.addTab(busStopCodeTab);
		// the bus lines list
		TabSpec busLinesListTab = tabHost.newTabSpec(TAB_BUS);
		busLinesListTab.setIndicator(getString(R.string.bus), getResources().getDrawable(R.drawable.ic_tab_bus));
		busLinesListTab.setContent(new Intent(this, BusLineListTab.class));
		tabHost.addTab(busLinesListTab);
		// the subway lines list
		TabSpec subwayLinesListTab = tabHost.newTabSpec(TAB_SUBWAY);
		subwayLinesListTab.setIndicator(getString(R.string.subway), getResources()
		        .getDrawable(R.drawable.ic_tab_subway));
		subwayLinesListTab.setContent(new Intent(this, SubwayTab.class));
		tabHost.addTab(subwayLinesListTab);
		try {
			// IF there is one or more favorites DO
			if (Utils.getSharedPreferences(this, UserPreferences.PREFS_IS_FAV, UserPreferences.PREFS_IS_FAV_DEFAULT)) {
				tabHost.setCurrentTab(0); // show favorite tab
			} else {
				tabHost.setCurrentTab(1); // show bus stop code search tab
			}
		} catch (Exception e) {
			MyLog.w(TAG, "Error while determining the select tab", e);
		}
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
		        RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		layoutParams.addRule(RelativeLayout.ABOVE, R.id.ad_layout);
		((RelativeLayout) findViewById(R.id.main)).addView(tabHost, layoutParams);
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		if (TwitterUtils.isTwitterCallback(intent)) {
			TwitterUtils.getInstance().login(this, intent.getData());
		}
		super.onNewIntent(intent);
	}
}
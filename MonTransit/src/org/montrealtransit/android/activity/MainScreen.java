package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.api.SupportFactory;

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
@SuppressWarnings("deprecation")
// TODO use Fragment
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
	/**
	 * The bike stations list tab ID.
	 */
	private static final String TAB_BIKE = "tab_bike";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI.
		setContentView(R.layout.main_screen);

		RelativeLayout main = (RelativeLayout) findViewById(R.id.main);
		// add the tab host
		TabHost tabHost = (TabHost) getLayoutInflater().inflate(R.layout.main_screen_tab_host, main, false);
		tabHost.setup(this.getLocalActivityManager());
		// the favorites list
		TabSpec favListTab = tabHost.newTabSpec(TAB_FAV);
		favListTab.setIndicator(getString(R.string.favorite), getResources().getDrawable(R.drawable.ic_tab_starred));
		favListTab.setContent(new Intent(this, FavListTab.class));
		tabHost.addTab(favListTab);
		// the bus stop code
		TabSpec busStopCodeTab = tabHost.newTabSpec(TAB_STOP_CODE);
		busStopCodeTab.setIndicator(getString(R.string.stop_code), getResources().getDrawable(R.drawable.ic_tab_stop_code));
		busStopCodeTab.setContent(new Intent(this, BusStopCodeTab.class));
		tabHost.addTab(busStopCodeTab);
		// the bus lines list
		TabSpec busTab = tabHost.newTabSpec(TAB_BUS);
		busTab.setIndicator(getString(R.string.bus), getResources().getDrawable(R.drawable.ic_tab_bus));
		busTab.setContent(new Intent(this, SupportFactory.get().getBusTabClass()));
		tabHost.addTab(busTab);
		// the subway lines list
		TabSpec subwayLinesListTab = tabHost.newTabSpec(TAB_SUBWAY);
		subwayLinesListTab.setIndicator(getString(R.string.subway), getResources().getDrawable(R.drawable.ic_tab_subway));
		subwayLinesListTab.setContent(new Intent(this, SubwayTab.class));
		tabHost.addTab(subwayLinesListTab);
		// the bike stations list
		TabSpec bikeStationsListTab = tabHost.newTabSpec(TAB_BIKE);
		bikeStationsListTab.setIndicator(getString(R.string.bike), getResources().getDrawable(R.drawable.ic_tab_bike));
		bikeStationsListTab.setContent(new Intent(this, BikeTab.class));
		tabHost.addTab(bikeStationsListTab);
		try {
			tabHost.setCurrentTab(UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_TAB, UserPreferences.PREFS_LCL_TAB_DEFAULT));
		} catch (Exception e) {
			MyLog.w(TAG, "Error while determining the select tab!", e);
		}
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
				RelativeLayout.LayoutParams.FILL_PARENT);
		main.addView(tabHost, layoutParams);
	}

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		MyLog.v(TAG, "onWindowFocusChanged(%s)", hasFocus);
		// IF the activity just regained the focus DO
		if (!this.hasFocus && hasFocus) {
			onResumeWithFocus();
		}
		this.hasFocus = hasFocus;
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		// IF the activity has the focus DO
		if (this.hasFocus) {
			onResumeWithFocus();
		}
		super.onResume();
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
	}

	// @Override
	// protected void onNewIntent(Intent intent) {
	// MyLog.v(TAG, "onNewIntent()");
	// if (TwitterUtils.isTwitterCallback(intent)) {
	// TwitterUtils.getInstance().login(this, intent.getData());
	// }
	// super.onNewIntent(intent);
	// }
}
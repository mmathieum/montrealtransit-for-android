package org.montrealtransit.android.activity;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * This class is the first screen displayed by the application. It contains, 4 tabs.
 * @author Mathieu Méa
 */
// TODO offer the options to just show a list of the "tabs".
public class MainScreen extends TabActivity {

	/**
	 * The log tag.
	 */
	private static final String TAG = MainScreen.class.getSimpleName();
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

		TabHost mTabHost = getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec(TAB_FAV).setIndicator(getResources().getString(R.string.favorite),
		        getResources().getDrawable(R.drawable.tab_starred)).setContent(new Intent(this, FavListTab.class)));
		mTabHost.addTab(mTabHost.newTabSpec(TAB_STOP_CODE).setIndicator(getResources().getString(R.string.stop_code),
		        getResources().getDrawable(R.drawable.tab_stop_code)).setContent(new Intent(this, BusStopCodeTab.class)));
		mTabHost.addTab(mTabHost.newTabSpec(TAB_BUS).setIndicator(getResources().getString(R.string.bus), getResources().getDrawable(R.drawable.tab_bus))
		        .setContent(new Intent(this, BusLineListTab.class)));
		mTabHost.addTab(mTabHost.newTabSpec(TAB_SUBWAY).setIndicator(getResources().getString(R.string.subway),
		        getResources().getDrawable(R.drawable.tab_subway)).setContent(new Intent(this, SubwayLinesListTab.class)));
		try {
			if (Utils.getCursorSize(DataManager.findAllFavs(this.getContentResolver())) == 0) {
				mTabHost.setCurrentTab(1); // show bus stop code search tab
			} else {
				mTabHost.setCurrentTab(0); // show favorite tab
			}
		} catch (Exception e) {
			MyLog.w(TAG, "Error while determing the select tab", e);
		}
	}
}
package org.montrealtransit.android.activity.v4;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.activity.UserPreferences;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * The bus tab activity.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class BusTab extends FragmentActivity {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Buses";
	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_tab);

		showAll();
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
		UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_TAB, 2);
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// resume fragments
		Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + FRAGMENT_CLOSEST_STOPS_INDEX);
		if (f != null) {
			BusTabClosestStopsFragment df = (BusTabClosestStopsFragment) f;
			df.onResumeWithFocus(this);
		}
	}

	@SuppressWarnings("deprecation")
	private void showAll() {
		MyLog.v(TAG, "showAll()");
		this.viewPager = (ViewPager) findViewById(R.id.viewpager);
		this.viewPager.setAdapter(new BusTabFragmentAdapter(getSupportFragmentManager()));
		this.viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				UserPreferences.savePrefLcl(BusTab.this, UserPreferences.PREFS_LCL_BUS_TAB, position);
			}
		});
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... params) {
				try {
					return UserPreferences.getPrefLcl(BusTab.this, UserPreferences.PREFS_LCL_BUS_TAB, UserPreferences.PREFS_LCL_BUS_TAB_DEFAULT);
				} catch (Throwable t) {
					MyLog.w(TAG, t, "Error while loading default bus tab!");
					return 0;
				}
			}

			@Override
			protected void onPostExecute(Integer result) {
				BusTab.this.viewPager.setCurrentItem(result);
			};

		}.execute();
	}

	/**
	 * Show closest bus stops.
	 */
	public void showClosest(View v) {
		MyLog.v(TAG, "showClosest()");
		this.viewPager.setCurrentItem(FRAGMENT_CLOSEST_STOPS_INDEX);
	}

	/**
	 * Show bus lines.
	 */
	public void showGrid(View v) {
		MyLog.v(TAG, "showGrid()");
		this.viewPager.setCurrentItem(FRAGMENT_BUS_LINES_INDEX);
	}

	private static final int FRAGMENT_BUS_LINES_INDEX = 0;
	private static final int FRAGMENT_CLOSEST_STOPS_INDEX = 1;

	private class BusTabFragmentAdapter extends FragmentPagerAdapter {

		public BusTabFragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			// MyLog.v(TAG, "getItem(%s)", position);
			switch (position) {
			case FRAGMENT_CLOSEST_STOPS_INDEX:
				return BusTabClosestStopsFragment.newInstance();
			case FRAGMENT_BUS_LINES_INDEX:
			default: // shall never ever happen
				return BusTabLinesGridFragment.newInstance();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.main_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuUtils.handleCommonMenuActions(this, item);
	}
}

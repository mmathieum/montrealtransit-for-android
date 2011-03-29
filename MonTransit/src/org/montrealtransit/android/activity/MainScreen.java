package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.StmDbHelper;

import android.app.ActivityGroup;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

/**
 * This class is the first screen displayed by the application. It contains, 4 tabs.
 * @author Mathieu Méa
 */
// TODO offer the options to just show a list of the "tabs" == Dashboard UI
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
	 * The progress dialog to show while initializing the app.
	 */
	private ProgressDialog progressDialog;

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

		// IF the local database need to be initialized DO
		if (!StmDbHelper.isDbExist(this)) {
			// show a progress dialog
			this.progressDialog = new ProgressDialog(this);
			this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.progressDialog.setCancelable(false);
			this.progressDialog.setIndeterminate(true);
			// this.progressDialog.setTitle(getString(R.string.init_dialog_title));
			this.progressDialog.setMessage(getString(R.string.init_dialog_message));
			this.progressDialog.show();
			// initialize the database
			new InitializationTask().execute();
		} else {
			// just finish the onCreate
			onCreateFinish();
		}
	}

	/**
	 * Finish the create of the activity (after initialization).
	 */
	private void onCreateFinish() {
		MyLog.v(TAG, "onCreateFinish()");
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
			if (Utils.getListSize(DataManager.findAllFavsList(this.getContentResolver())) > 0) {
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

	/**
	 * This task initialize the application.
	 * @author Mathieu Méa
	 */
	public class InitializationTask extends AsyncTask<String, String, String> {

		/**
		 * The log tag.
		 */
		private final String TAG = InitializationTask.class.getSimpleName();

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected String doInBackground(String... arg0) {
			MyLog.v(TAG, "doInBackground()");
			new StmDbHelper(MainScreen.this, this);
			return null;
		}

		/**
		 * Initialized the progress bar with max value.
		 * @param maxValue the max value
		 */
		public void initProgressBar(int maxValue) {
			MyLog.v(TAG, "initProgressBar(%s)", maxValue);
			MainScreen.this.progressDialog.setIndeterminate(false);
			MainScreen.this.progressDialog.setMax(maxValue);
		}

		/**
		 * Set the progress bar progress to the new progress.
		 * @param value the new progress
		 */
		public void incrementProgressBar(int value) {
			// MyLog.v(TAG, "incrementProgressBar(%s)", value);
			MainScreen.this.progressDialog.setProgress(value);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(String result) {
			MyLog.v(TAG, "onPostExecute()", result);
			super.onPostExecute(result);
			MainScreen.this.progressDialog.dismiss();
			onCreateFinish();
		}
	}
}
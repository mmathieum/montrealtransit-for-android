package org.montrealtransit.android.activity;

import java.text.NumberFormat;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.StmDbHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is the first screen displayed by the application.
 * @author Mathieu Méa
 */
// TODO add version checking here (start asynchronously and show a notification to the user).
public class SplashScreen extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = SplashScreen.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SplashScreen";
	/**
	 * True if forcing DB reset.
	 */
	private static final boolean DB_RESET = false;
	/**
	 * The progress bar title message.
	 */
	private TextView progressBarMessageTitle;
	/**
	 * The progress bar description message.
	 */
	private TextView progressBarMessageDesc;
	/**
	 * The progress bar.
	 */
	private ProgressBar progressBar;
	/**
	 * The progress bar percent.
	 */
	private TextView progressBarPercent;
	/**
	 * The progress bar number.
	 */
	private TextView progressBarNumber;
	/**
	 * The progress bar percent format.
	 */
	private NumberFormat progressPercentFormat;
	/**
	 * The progress bar number format.
	 */
	private String progressNumberFormat;
	/**
	 * The progress bar update handler (from AOSP {@link ProgressDialog}).
	 */
	private Handler progressBarUpdateHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utils.logAppVersion(this);
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// deploy/update DB if necessary. TODO do in background
		if (DB_RESET) {
			deploy();
			return;
		}
		// CHECK DB for initialize/update
		int currentDeployedStmDbVersion = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_STM_DB_VERSION, 0);
		switch (currentDeployedStmDbVersion) {
		case StmDbHelper.DB_VERSION:
			showMainScreen();
			break;
		default:
			deploy();
			break;
		}
	}

	/**
	 * @param context the context
	 * @return true if an update is required
	 */
	public static boolean isUpdateRequired(Context context) {
		return UserPreferences.getPrefLcl(context, UserPreferences.PREFS_LCL_STM_DB_VERSION, 0) != StmDbHelper.DB_VERSION;
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	@Override
	public void onBackPressed() { // API Level 5 - 2.0+
		MyLog.v(TAG, "onBackPressed()");
		new AlertDialog.Builder(this).setMessage(R.string.confirm_exit).setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						SplashScreen.this.finish();
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}).create().show();
	}

	/**
	 * Deploy or update the STM DB.
	 */
	private void deploy() {
		MyLog.v(TAG, "deploy()");
		showSplashScreen();

		// 1 - check for free space in user space /data/data/org.montrealtransit.android/...
		if (Utils.getAvailableSize() < StmDbHelper.getRequiredSize(this)) {
			// show dialog => exit
			Toast.makeText(this, R.string.update_not_enough_free_space, Toast.LENGTH_LONG).show();
			// MyLog.d(TAG, "available size: %s (required size: %s)", Utils.getAvailableSize(), StmDbHelper.getRequiredSize(this));
			finish();
		}
		if (!DB_RESET && !StmDbHelper.isDbExist(this)) { // initialize
			addProgressBar();
			// show a progress dialog
			this.progressBar.setIndeterminate(true);
			this.progressBarMessageTitle.setVisibility(View.VISIBLE);
			this.progressBarMessageTitle.setText(R.string.init_dialog_title);
			this.progressBarMessageDesc.setVisibility(View.VISIBLE);
			this.progressBarMessageDesc.setText(getString(R.string.init_dialog_message));
			// initialize the database
			new InitializationTask().execute(false);
		} else {
			// update favorites
			checkForBusLineUpdates();
			// IF update available DO
			if (!DB_RESET && updateAvailable()) { // update
				addProgressBar();
				// show a progress dialog
				this.progressBar.setIndeterminate(true);
				this.progressBarMessageTitle.setVisibility(View.VISIBLE);
				this.progressBarMessageTitle.setText(R.string.update_dialog_title);
				this.progressBarMessageDesc.setVisibility(View.VISIBLE);
				this.progressBarMessageDesc.setText(R.string.update_dialog_message);
				// initialize the database
				new InitializationTask().execute(true);
			} else { // force re-initialize
				addProgressBar();
				// show a progress dialog
				this.progressBar.setIndeterminate(true);
				this.progressBarMessageTitle.setVisibility(View.VISIBLE);
				this.progressBarMessageTitle.setText(R.string.reset_dialog_title);
				this.progressBarMessageDesc.setVisibility(View.VISIBLE);
				this.progressBarMessageDesc.setText(R.string.reset_dialog_message);
				// initialize the database
				new InitializationTask().execute(true);
			}
		}
	}

	/**
	 * @return true if update available
	 */
	public boolean updateAvailable() {
		boolean updateAvailable = false;
		StmDbHelper tmp = null;
		try {
			tmp = new StmDbHelper(this, null);
			tmp.getReadableDatabase();
			updateAvailable = tmp.isUpdateAvailable();
		} catch (Exception e) {
			MyLog.w(TAG, e, "Can't check DB for update availability!");
		} finally {
			if (tmp != null) {
				tmp.close();
			}
		}
		return updateAvailable;
	}

	/**
	 * Check if a favorite update is needed due to bus line number changes for example.
	 */
	private void checkForBusLineUpdates() {
		int currentDeployedStmDbVersion = UserPreferences.getPrefLcl(this, UserPreferences.PREFS_LCL_STM_DB_VERSION, 0);
		if (currentDeployedStmDbVersion == 9 && StmDbHelper.DB_VERSION >= 10) {
			// update favorites (January 2012)
			Utils.updateFavoritesJan2012(getContentResolver());
			SupportFactory.getInstance(this).backupManagerDataChanged();
		}
	}

	/**
	 * Show the progress bar.
	 */
	private void addProgressBar() {
		this.progressBarMessageTitle = (TextView) findViewById(R.id.message_title);
		this.progressBarMessageDesc = (TextView) findViewById(R.id.message_desc);

		this.progressBar = (ProgressBar) findViewById(R.id.progress);

		this.progressBarPercent = (TextView) findViewById(R.id.progress_percent);
		this.progressPercentFormat = NumberFormat.getPercentInstance();
		this.progressPercentFormat.setMaximumFractionDigits(0);

		this.progressBarNumber = (TextView) findViewById(R.id.progress_number);
		this.progressNumberFormat = "%d/%d";

		this.progressBarUpdateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				/* Update the number and percent */
				int progress = SplashScreen.this.progressBar.getProgress();
				int max = SplashScreen.this.progressBar.getMax();
				double percent = (double) progress / (double) max;
				String format = SplashScreen.this.progressNumberFormat;
				SplashScreen.this.progressBarNumber.setText(String.format(format, progress, max));
				SpannableString tmp = new SpannableString(SplashScreen.this.progressPercentFormat.format(percent));
				tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tmp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				SplashScreen.this.progressBarPercent.setText(tmp);
			}
		};

		findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
	}

	/**
	 * Show the splash screen.
	 */
	private void showSplashScreen() {
		MyLog.v(TAG, "showSplashScreen()");
		// Show splash screen
		setContentView(R.layout.splash_screen);
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(Constant.PKG, 0);
			String versionName = packageInfo.versionName;
			String versionCode = String.valueOf(packageInfo.versionCode);
			TextView versionTv = (TextView) findViewById(R.id.app_version);
			versionTv.setText(getString(R.string.about_version, versionName, versionCode));
		} catch (NameNotFoundException e) {
		}
	}

	/**
	 * Show the main screen.
	 */
	private void showMainScreen() {
		MyLog.v(TAG, "showMainScreen()");
		startActivity(new Intent(this, MainScreen.class));
		this.finish();
	}

	/**
	 * This task initialize the application.
	 */
	public class InitializationTask extends AsyncTask<Boolean, String, String> {

		/**
		 * The log tag.
		 */
		private final String TAG = InitializationTask.class.getSimpleName();

		@Override
		protected String doInBackground(Boolean... arg0) {
			MyLog.v(TAG, "doInBackground()");
			StmDbHelper db = new StmDbHelper(SplashScreen.this, this);
			if (arg0[0]) {
				db.forceReset(SplashScreen.this, this);
				// clean old favorites
				Utils.cleanFavorites(getContentResolver());
				SupportFactory.getInstance(SplashScreen.this).backupManagerDataChanged();
			}
			return null;
		}

		/**
		 * Initialized the progress bar with max value.
		 * @param maxValue the max value
		 */
		public void initProgressBar(int maxValue) {
			MyLog.v(TAG, "initProgressBar(%s)", maxValue);
			SplashScreen.this.progressBar.setIndeterminate(false);
			SplashScreen.this.progressBar.setMax(maxValue);
			SplashScreen.this.progressBarUpdateHandler.sendEmptyMessage(0);
		}

		/**
		 * Set the progress bar progress to the new progress.
		 * @param value the new progress
		 */
		public void incrementProgressBar(int value) {
			// MyLog.v(TAG, "incrementProgressBar(%s)", value);
			SplashScreen.this.progressBar.setProgress(value);
			SplashScreen.this.progressBarUpdateHandler.sendEmptyMessage(0);
		}

		@Override
		protected void onPostExecute(String result) {
			MyLog.v(TAG, "onPostExecute()", result);
			super.onPostExecute(result);
			if (!SplashScreen.this.isFinishing()) { // not showing the main screen if the user left the app
				SplashScreen.this.findViewById(R.id.progress_layout).setVisibility(View.GONE);
				showMainScreen();
			}
		}
	}
}

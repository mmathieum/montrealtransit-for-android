package org.montrealtransit.android.activity;

import java.text.NumberFormat;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.StmDbHelper;

import android.app.Activity;
import android.app.ProgressDialog;
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

/**
 * This class is the first screen displayed by the application.
 * @author Mathieu Méa
 */
// TODO add version checking here (start asynchronously and the send a notification to the user).
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
		// CHECK DB for initialize/update
		int currentDeployedStmDbVersion = Utils.getSharedPreferences(this, UserPreferences.PREFS_STM_DB_VERSION, 0);
		switch (currentDeployedStmDbVersion) {
		case StmDbHelper.DB_VERSION:
			showMainScreen();
			break;
		default:
			deploy();
			break;
		}
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * Deploy or update the STM DB.
	 */
	private void deploy() {
		MyLog.v(TAG, "deploy()");
		if (!StmDbHelper.isDbExist(this)) {
			showSplashScreen();
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
			StmDbHelper tmp = new StmDbHelper(this, null);
			tmp.getReadableDatabase();
			boolean updateAvailable = tmp.isUpdateAvailable();
			tmp.close();
			if (updateAvailable) {
				showSplashScreen();
				// show a progress dialog
				this.progressBar.setIndeterminate(true);
				this.progressBarMessageTitle.setVisibility(View.VISIBLE);
				this.progressBarMessageTitle.setText(R.string.update_dialog_title);
				this.progressBarMessageDesc.setVisibility(View.VISIBLE);
				this.progressBarMessageDesc.setText(R.string.update_dialog_message);
				// initialize the database
				new InitializationTask().execute(true);
			} else {
				// Show main Screen
				showMainScreen();
			}
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

		progressBarUpdateHandler = new Handler() {
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
				tmp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tmp.length(),
				        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
		Intent intent = new Intent(this, MainScreen.class);
		startActivity(intent);
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
			SplashScreen.this.findViewById(R.id.progress_layout).setVisibility(View.GONE);
			showMainScreen();
		}
	}
}

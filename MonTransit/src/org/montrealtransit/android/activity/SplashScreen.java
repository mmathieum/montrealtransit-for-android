package org.montrealtransit.android.activity;

import java.util.concurrent.TimeUnit;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.StmBusScheduleManager;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 * This class is the 1st screen [not] displayed by the application.
 * @author Mathieu Méa
 */
public class SplashScreen extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = SplashScreen.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SplashScreen";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// if (org.montrealtransit.android.BuildConfig.DEBUG) {
		// org.montrealtransit.android.api.SupportFactory.getInstance().enableStrictMode();
		// }
		Utils.logAppVersion(this);
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		Utils.updateBusStopsToRouteStops(this);
		Utils.updateSubwayStationsToRouteStops(this);
		StmBusScheduleManager.wakeUp(getContentResolver());
		showNewAppNotification();
		showMainScreen();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * Show the main screen.
	 */
	private void showMainScreen() {
		MyLog.v(TAG, "showMainScreen()");
		startActivity(new Intent(this, MainScreen.class));
		finish();
	}

	private static final int NEW_APP_AVAILABLE_NOTIFICATION_ID = 20100602;

	private static final String STORE_APP = "market://details?id=";

	private static final String STORE_WWW = "https://play.google.com/store/apps/details?id=";

	private static final String NEW_APP_PACKAGE = "org.mtransit.android";

	private static final long MIGRATION_START_IN_MS = 1427947200000l; // April 2nd, 2015

	private static final String PREFS_LCL_LAST_NEW_APP_NOTIFICATION = "pLclLastNewAppNotification";

	private static final long MIN_DURATION_BETWEEN_NOTIFICATIONS_IN_MS = TimeUnit.DAYS.toMillis(7); // once a week

	@SuppressWarnings("deprecation")
	private void showNewAppNotification() {
		try {
			if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				MyLog.d(TAG, "Not showing new app notification (not compatible)");
				return; // not compatible
			}
			if (Utils.isAppInstalled(this, NEW_APP_PACKAGE)) {
				MyLog.d(TAG, "Not showing new app notification (already installed)");
				return; // already installed
			}
			long nowInMs = System.currentTimeMillis();
			if (nowInMs < MIGRATION_START_IN_MS) {
				MyLog.d(TAG, "Not showing new app notification (too soon)");
				return; // too soon
			}
			long lastNotification = UserPreferences.getPrefLcl(this, PREFS_LCL_LAST_NEW_APP_NOTIFICATION, 0l);
			if (nowInMs - lastNotification < MIN_DURATION_BETWEEN_NOTIFICATIONS_IN_MS) {
				MyLog.d(TAG, "Not showing new app notification (not again, waiting)");
				return; // not again, waiting
			}
			NotificationCompat.Builder noficationBuilder = new NotificationCompat.Builder(this) //
					.setSmallIcon(android.R.drawable.stat_notify_sync) //
					.setContentTitle(getString(R.string.new_app_notification_title)) //
					.setContentText(getString(R.string.new_app_notification_text_short)) //
					.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.new_app_notification_text_long))) //
					.setAutoCancel(true);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_APP + NEW_APP_PACKAGE)); // Google Play Store application
			if (intent.resolveActivity(getPackageManager()) == null) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_WWW + NEW_APP_PACKAGE)); // Google Play Store web site
			}
			noficationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NEW_APP_AVAILABLE_NOTIFICATION_ID, noficationBuilder.build());
			UserPreferences.savePrefLcl(this, PREFS_LCL_LAST_NEW_APP_NOTIFICATION, nowInMs);
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while showing new app notification!");
		}
	}
}

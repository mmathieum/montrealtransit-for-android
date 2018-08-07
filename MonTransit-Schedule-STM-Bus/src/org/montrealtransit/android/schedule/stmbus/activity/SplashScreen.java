package org.montrealtransit.android.schedule.stmbus.activity;

import org.montrealtransit.android.MyLog;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class SplashScreen extends Activity {

	private static final String TAG = SplashScreen.class.getSimpleName();

	private static final String STORE_APP = "market://details?id=";
	private static final String STORE_WWW = "https://play.google.com/store/apps/details?id=";

	private static final String NEW_MAIN_APP_PACKAGE_NAME = "org.mtransit.android";
	private static final String NEW_APP_PACKAGE_NAME = "org.mtransit.android.ca_montreal_stm_bus";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		addLauncherIcon(this); // re-enable launcher icon for migration
		if (Integer.parseInt(Build.VERSION.SDK) < 14) {
			// NEW application not supported
			// => opening STM mobile web site:
			try {
				String url = "https://m.stm.info";
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				startActivity(intent);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Error while opening web url!");
			}
			finish();
			return;
		}
		if (isAppInstalled(NEW_MAIN_APP_PACKAGE_NAME)) {
			if (isAppInstalled(NEW_APP_PACKAGE_NAME)) {
				// open NEW application with STM Bus data
				openApp(NEW_MAIN_APP_PACKAGE_NAME);
			} else {
				// open STM bus data Google Play Store page
				openPlayStore(NEW_APP_PACKAGE_NAME);
			}
		} else { // ELSE IF new main application NOT installed DO
			if (isAppInstalled(NEW_APP_PACKAGE_NAME)) {
				// open NEW application Google Play Store page
				openPlayStore(NEW_MAIN_APP_PACKAGE_NAME);
			} else {
				// open STM bus data Google Play Store page
				openPlayStore(NEW_APP_PACKAGE_NAME);
			}
		}
		finish();
	}

	public static void addLauncherIcon(Context context) {
		MyLog.v(TAG, "addLauncherIcon()");
		try {
			context.getPackageManager().setComponentEnabledSetting(
					new ComponentName("org.montrealtransit.android.schedule.stmbus", "org.montrealtransit.android.schedule.stmbus.activity.SplashScreen"),
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while adding launcher icon!");
		}
	}

	public static void removeLauncherIcon(Context context) {
		MyLog.v(TAG, "removeLauncherIcon()");
		try {
			context.getPackageManager().setComponentEnabledSetting(
					new ComponentName("org.montrealtransit.android.schedule.stmbus", "org.montrealtransit.android.schedule.stmbus.activity.SplashScreen"),
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while removing launcher icon!");
		}
	}

	public void openApp(String pkg) {
		MyLog.v(TAG, "openApp(%s)", pkg);
		try {
			Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
			if (intent == null) {
				throw new PackageManager.NameNotFoundException();
			}
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			startActivity(intent);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while opening the application!");
		}
	}

	private void openPlayStore(String pkg) {
		MyLog.v(TAG, "openPlayStore()");
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_APP + pkg)); // Google Play Store application
			if (intent.resolveActivity(getPackageManager()) == null) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_WWW + pkg)); // Google Play Store web site
			}
			startActivity(intent);
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while opening new app Google Play Store page!");
		}
	}

	private boolean isAppInstalled(String pkg) {
		PackageManager pm = getPackageManager();
		try {
			pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
}

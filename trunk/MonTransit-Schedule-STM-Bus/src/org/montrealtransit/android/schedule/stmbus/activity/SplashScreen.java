package org.montrealtransit.android.schedule.stmbus.activity;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.schedule.stmbus.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

public class SplashScreen extends Activity {

	private static final String TAG = SplashScreen.class.getSimpleName();

	private static final String MAIN_APP_PACKAGE_NAME = "org.montrealtransit.android";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		if (isAppInstalled(MAIN_APP_PACKAGE_NAME)) {
			final Toast toast = Toast.makeText(this, R.string.opening_main_app_and_removing_icon, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			// remove this app icon
			removeLauncherIcon(this);
			// open the main app
			openApp(MAIN_APP_PACKAGE_NAME);
		} else {
			// open google play store
			final Toast toast = Toast.makeText(this, R.string.please_install_main_app, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + MAIN_APP_PACKAGE_NAME)));
		}
		finish();
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

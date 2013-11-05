package org.montrealtransit.android.schedule.stmbus.receiver;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.schedule.stmbus.activity.SplashScreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Not working because the receiver is only started when the application is started.
 */
@Deprecated
public class PackageChangeReceiver extends BroadcastReceiver {

	private static final String TAG = SplashScreen.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		MyLog.v(TAG, "onReceive()");
		// MyLog.d(TAG, "%s", intent);
	}

}

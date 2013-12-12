package org.montrealtransit.android.provider;

import java.util.List;

import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.common.AbstractScheduleManager;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

public class StmBusScheduleManager extends AbstractScheduleManager {

	public static final String TAG = StmBusScheduleManager.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmbus";

	public static final String PROVIDER_APP_PACKAGE = "org.montrealtransit.android.schedule.stmbus";

	public static final Uri CONTENT_URI = Utils.newContentUri(AUTHORITY);

	public static void wakeUp(final ContentResolver contentResolver) {
		AbstractScheduleManager.wakeUp(contentResolver, CONTENT_URI);
	}

	public static void ping(final ContentResolver contentResolver) {
		AbstractScheduleManager.ping(contentResolver, CONTENT_URI);
	}

	public static List<String> findBusScheduleList(ContentResolver contentResolver, int routeId, int tripId, int stopId, String date, String time) {
		return AbstractScheduleManager.findScheduleList(contentResolver, CONTENT_URI, routeId, tripId, stopId, date, time);
	}

	public static boolean isContentProviderAvailable(Context context) {
		return AbstractScheduleManager.isContentProviderAvailable(context, AUTHORITY);
	}

	public static boolean isAppInstalled(Context context) {
		return AbstractScheduleManager.isAppInstalled(context, PROVIDER_APP_PACKAGE);
	}
}

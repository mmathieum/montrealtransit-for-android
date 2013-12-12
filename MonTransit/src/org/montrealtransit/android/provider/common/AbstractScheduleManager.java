package org.montrealtransit.android.provider.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmBusScheduleManager;
import org.montrealtransit.android.provider.StmSubwayManager;
import org.montrealtransit.android.provider.StmSubwayScheduleManager;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

public abstract class AbstractScheduleManager {

	public static final String TAG = AbstractScheduleManager.class.getSimpleName();

	public static Map<String, String> authoritiesToScheduleAuthorities;
	static {
		// dirty: should have been "<schedule_authority>"="<authority>.schedule"
		// need to wait until existing provider data become useless to upgrade
		authoritiesToScheduleAuthorities = new HashMap<String, String>();
		authoritiesToScheduleAuthorities.put(StmBusManager.AUTHORITY, StmBusScheduleManager.AUTHORITY);
		authoritiesToScheduleAuthorities.put(StmSubwayManager.AUTHORITY, StmSubwayScheduleManager.AUTHORITY);
	}

	private static final String[] PROJECTION_SCHEDULE = new String[] { "departure" };

	public static final String PING_CONTENT_DIRECTORY = "ping";
	public static final String DEPARTURE_CONTENT_DIRECTORY = "departure";

	public static final String ROUTE_CONTENT_DIRECTORY = "route"; // mandatory

	private static boolean pingChecked = false;

	public static void wakeUp(final ContentResolver contentResolver, final Uri contentUri) {
		MyLog.v(TAG, "wakeUp()");
		if (pingChecked) {
			return;
		}
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// MyLog.v(TAG, "wakeUp() > doInBackground()");
				ping(contentResolver, contentUri);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// MyLog.v(TAG, "wakeUp() > onPostExecute()");
				super.onPostExecute(result);
			}
		}.execute();
		pingChecked = true;
	}

	public static void ping(final ContentResolver contentResolver, final Uri contentUri) {
		MyLog.v(TAG, "ping()");
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(Uri.withAppendedPath(contentUri, PING_CONTENT_DIRECTORY), null, null, null, null);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<String> findScheduleList(ContentResolver contentResolver, Uri contentUri, int routeId, int tripId, int stopId, String date, String time) {
		MyLog.v(TAG, "findScheduleList(%s, %s, %s, %s, %s)", routeId, tripId, stopId, date, time);
		List<String> result = null;
		Cursor cursor = null;
		try {
			cursor = findSchedule(contentResolver, contentUri, routeId, tripId, stopId, date, time);
			// MyLog.d(TAG, "cursor.getCount(): " + (cursor == null ? "null" : cursor.getCount()));
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<String>();
					do {
						result.add(cursor.getString(0));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	private static Cursor findSchedule(ContentResolver contentResolver, Uri contentUri, int routeId, int tripId, int stopId, String date, String time) {
		MyLog.v(TAG, "findSchedule(%s, %s, %s, %s, %s)", routeId, tripId, stopId, date, time);
		Uri uri = getRouteDepartureUri(contentUri, routeId);
		StringBuilder selectionSb = new StringBuilder();
		selectionSb.append(ScheduleColumns.T_SCHEDULES_K_TRIP_ID).append("=").append(tripId);
		selectionSb.append(" AND ");
		selectionSb.append(ScheduleColumns.T_SCHEDULES_K_STOP_ID).append("=").append(stopId);
		if (!TextUtils.isEmpty(date)) {
			if (selectionSb.length() > 0) {
				selectionSb.append(" AND ");
			}
			selectionSb.append(ServiceDateColumns.T_SERVICE_DATES_K_DATE).append("=").append(date);
		}
		if (!TextUtils.isEmpty(time)) {
			if (selectionSb.length() > 0) {
				selectionSb.append(" AND ");
			}
			selectionSb.append(ScheduleColumns.T_SCHEDULES_K_DEPARTURE).append(">=").append(time);
		}
		return contentResolver.query(uri, PROJECTION_SCHEDULE, selectionSb.toString(), null, null);
	}

	public static Uri getRouteDepartureUri(Uri contentUri, int routeId) {
		final Uri routesUri = Uri.withAppendedPath(contentUri, ROUTE_CONTENT_DIRECTORY);
		final Uri routeUri = Uri.withAppendedPath(routesUri, String.valueOf(routeId));
		return Uri.withAppendedPath(routeUri, DEPARTURE_CONTENT_DIRECTORY);
	}

	public static boolean isContentProviderAvailable(Context context, String authority) {
		return context.getPackageManager().resolveContentProvider(authority, 0) != null;
	}

	public static boolean isAppInstalled(Context context, String providerAppPackage) {
		return Utils.isPackageExists(context, providerAppPackage);
	}
}

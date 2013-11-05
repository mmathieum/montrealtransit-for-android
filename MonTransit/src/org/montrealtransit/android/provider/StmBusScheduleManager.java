package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

public class StmBusScheduleManager {

	public static final String TAG = StmBusScheduleManager.class.getSimpleName();

	private static final String[] PROJECTION_SCHEDULE = new String[] { "departure" };

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmbus";

	public static final String PROVIDER_APP_PACKAGE = "org.montrealtransit.android.schedule.stmbus";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

	public static final String PING_CONTENT_DIRECTORY = "ping";

	public static final String ROUTE_CONTENT_DIRECTORY = "route"; // mandatory
	public static final String TRIP_CONTENT_DIRECTORY = "trip";
	public static final String STOP_CONTENT_DIRECTORY = "stop";
	public static final String DATE_CONTENT_DIRECTORY = "date";
	public static final String TIME_CONTENT_DIRECTORY = "time";

	private static boolean pingChecked = false;

	public static void wakeUp(final ContentResolver contentResolver) {
		MyLog.v(TAG, "wakeUp()");
		if (pingChecked) {
			return;
		}
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// MyLog.v(TAG, "wakeUp() > doInBackground()");
				ping(contentResolver);
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

	public static void ping(final ContentResolver contentResolver) {
		MyLog.v(TAG, "ping()");
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(Uri.withAppendedPath(CONTENT_URI, PING_CONTENT_DIRECTORY), null, null, null, null);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static List<String> findBusScheduleList(ContentResolver contentResolver, String busLineNumber, String stopCode, String date, String time) {
		MyLog.v(TAG, "findBusScheduleList(%s, %s, %s, %s)", busLineNumber, stopCode, date, time);
		List<String> result = null;
		Cursor cursor = null;
		try {
			cursor = findBusSchedule(contentResolver, busLineNumber, stopCode, date, time);
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

	private static Cursor findBusSchedule(ContentResolver contentResolver, String busLineNumber, String stopCode, String date, String time) {
		// MyLog.v(TAG, "findBusSchedule(%s, %s, %s, %s)", busLineNumber, stopCode, date, time);
		Uri uri = Uri.withAppendedPath(Uri.withAppendedPath(CONTENT_URI, ROUTE_CONTENT_DIRECTORY), busLineNumber);
		uri = Uri.withAppendedPath(Uri.withAppendedPath(uri, STOP_CONTENT_DIRECTORY), stopCode);
		if (!TextUtils.isEmpty(date)) {
			uri = Uri.withAppendedPath(Uri.withAppendedPath(uri, DATE_CONTENT_DIRECTORY), date);
		}
		if (!TextUtils.isEmpty(time)) {
			uri = Uri.withAppendedPath(Uri.withAppendedPath(uri, TIME_CONTENT_DIRECTORY), time);
		}
		return contentResolver.query(uri, PROJECTION_SCHEDULE, null, null, null);
	}

	public static boolean isContentProviderAvailable(Context context) {
		return context.getPackageManager().resolveContentProvider(StmBusScheduleManager.AUTHORITY, 0) != null;
	}

	public static boolean isAppInstalled(Context context) {
		return Utils.isPackageExists(context, PROVIDER_APP_PACKAGE);
	}
}

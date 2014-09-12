package org.montrealtransit.android.provider.common;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.StopTimes;
import org.montrealtransit.android.provider.StmBusLiveScheduleManager;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmBusScheduleManager;
import org.montrealtransit.android.provider.StmSubwayManager;
import org.montrealtransit.android.provider.StmSubwayScheduleManager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

public abstract class AbstractScheduleManager {

	public static final String TAG = AbstractScheduleManager.class.getSimpleName();

	public static Map<String, String[]> authoritiesToScheduleAuthorities;
	static {
		// dirty: should have been "<schedule_authority>"="<authority>.schedule"
		// need to wait until existing provider data become useless to upgrade
		authoritiesToScheduleAuthorities = new HashMap<String, String[]>();
		authoritiesToScheduleAuthorities.put(StmBusManager.AUTHORITY, new String[] { StmBusLiveScheduleManager.AUTHORITY, StmBusScheduleManager.AUTHORITY });
		authoritiesToScheduleAuthorities.put(StmSubwayManager.AUTHORITY, new String[] { StmSubwayScheduleManager.AUTHORITY });
	}

	private static final String PING_CONTENT_DIRECTORY = "ping";
	private static final String DEPARTURE_CONTENT_DIRECTORY = "departure";

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

	public static StopTimes findStopTimes(ContentResolver contentResolver, Uri contentUri, RouteTripStop routeTripStop, Long timestamp, Boolean cacheOnly,
			Integer cacheValidityInSec) {
		MyLog.v(TAG, "findStopTimes(%s, %s, %s, %s)", routeTripStop, timestamp, cacheOnly, cacheValidityInSec);
		if (routeTripStop == null) {
			MyLog.w(TAG, "RouteTripStop mandatory!");
			return null;
		}
		StopTimes result = null;
		Cursor cursor = null;
		try {
			JSONObject jSelection = new JSONObject();
			jSelection.put("routeTripStop", routeTripStop.toJSON());
			if (timestamp != null) {
				jSelection.put("timestamp", timestamp);
			}
			if (cacheOnly != null) {
				jSelection.put("cacheOnly", cacheOnly);
			}
			if (cacheValidityInSec != null) {
				jSelection.put("cacheValidityInSec", cacheValidityInSec);
			}
			String selection = jSelection.toString();
			cursor = contentResolver.query(Uri.withAppendedPath(contentUri, DEPARTURE_CONTENT_DIRECTORY), null, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = StopTimes.parseJSON(cursor.getString(cursor.getColumnIndexOrThrow("json")));
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}
}

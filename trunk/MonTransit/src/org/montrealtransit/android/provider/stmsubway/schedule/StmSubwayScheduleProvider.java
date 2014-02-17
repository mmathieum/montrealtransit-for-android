package org.montrealtransit.android.provider.stmsubway.schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.common.AbstractScheduleProvider;
import org.montrealtransit.android.provider.common.ScheduleColumns;
import org.montrealtransit.android.provider.common.ServiceDateColumns;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

public class StmSubwayScheduleProvider extends AbstractScheduleProvider {

	public static final String TAG = StmSubwayScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmsubway";

	public static final String SCHEDULE_SORT_ORDER = StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " ASC";

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	private static final String SCHEDULE_SERVICE_DATE_JOIN = StmSubwayScheduleDbHelper.T_SCHEDULES + " LEFT OUTER JOIN "
			+ StmSubwayScheduleDbHelper.T_SERVICE_DATES + " ON " + StmSubwayScheduleDbHelper.T_SCHEDULES + "."
			+ StmSubwayScheduleDbHelper.T_SCHEDULES_K_SERVICE_ID + "=" + StmSubwayScheduleDbHelper.T_SERVICE_DATES + "."
			+ StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_SERVICE_ID;

	private static StmSubwayScheduleDbHelper stmSubwayScheduleDbHelper;
	private static String stmSubwayScheduleDbHelperRouteId;

	private static Map<String, Integer> currentDbVersion = new HashMap<String, Integer>();

	@Override
	public void ping() {
		// remove this app icon
		// SplashScreen.removeLauncherIcon(getContext());
	}

	@Override
	public String getAUTHORITY() {
		return AUTHORITY;
	}

	@Override
	public UriMatcher getURIMATCHER() {
		return URI_MATCHER;
	}

	private static StmSubwayScheduleDbHelper getDBHelper(Context context, String routeId) {
		MyLog.v(TAG, "getDBHelper(%s)", routeId);
		if (stmSubwayScheduleDbHelperRouteId != null && !stmSubwayScheduleDbHelperRouteId.equals(routeId)) {
			MyLog.d(TAG, "getDBHelper(%s) > Not current DB helper! (was %s)", routeId, stmSubwayScheduleDbHelperRouteId);
			closeDbHelper();
		}
		if (stmSubwayScheduleDbHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			stmSubwayScheduleDbHelper = new StmSubwayScheduleDbHelper(context.getApplicationContext(), routeId);
			stmSubwayScheduleDbHelperRouteId = routeId;
			currentDbVersion.put(routeId, StmSubwayScheduleDbHelper.DB_VERSION);
		} else { // reset
			try {
				if (currentDbVersion.containsKey(routeId) && currentDbVersion.get(routeId).intValue() != StmSubwayScheduleDbHelper.DB_VERSION) {
					MyLog.d(TAG, "Update DB... (deployed version:%s, new version: %s)", currentDbVersion.get(routeId), StmSubwayScheduleDbHelper.DB_VERSION);
					closeDbHelper();
					return getDBHelper(context, routeId);
				}
			} catch (Exception e) {
				// fail if locked, will try again later
				MyLog.d(TAG, e, "Can't check DB version!");
			}
		}
		return stmSubwayScheduleDbHelper;
	}

	private static void closeDbHelper() {
		MyLog.v(TAG, "closeDbHelper()");
		if (stmSubwayScheduleDbHelper != null) {
			stmSubwayScheduleDbHelper.close();
			stmSubwayScheduleDbHelper = null;
			stmSubwayScheduleDbHelperRouteId = null;
		}
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

	@Override
	public Cursor getDeparture(RouteTripStop routeTripStop, Calendar now, Cache cache, String cacheUUID) {
		try {
			List<Long> allTimestamps = new ArrayList<Long>();
			// 1ST - check if YESTERDAY schedule is over (because trips started yesterday end with 240000+ time)
			MyLog.d(TAG, "Checking if yesterday schedule is over...");
			Calendar yesterday = (Calendar) now.clone();
			yesterday.add(Calendar.DATE, -1);
			final String dateYesterday = DATE_FORMAT.format(yesterday.getTime());
			// MyLog.d(TAG, "Date Yesterday: %s", dateYesterday);
			final String timeYesterday = String.valueOf(Integer.valueOf(TIME_FORMAT.format(yesterday.getTime())) + 240000);
			// yesterday.add(Calendar.DATE, +1); TODO ?
			// MyLog.d(TAG, "Time Yesterday: %s", timeYesterday);
			final Set<Long> yesterdayTimestamps = findScheduleList(routeTripStop.route.id, routeTripStop.trip.id, routeTripStop.stop.id, dateYesterday,
					timeYesterday);
			// MyLog.d(TAG, "result: %s", yesterdayTimestamps.size());
			allTimestamps.addAll(yesterdayTimestamps);
			MyLog.d(TAG, "Checking if yesterday schedule is over... DONE");
			// 2ND - check TODAY schedule
			MyLog.d(TAG, "Checking today schedule...");
			final String dateNow = DATE_FORMAT.format(now.getTime());
			// MyLog.d(TAG, "Date Now: %s", dateNow);
			final String timeNow = TIME_FORMAT.format(now.getTime());
			// MyLog.d(TAG, "Time Now: %s", timeNow);
			final Set<Long> todayTimestamps = findScheduleList(routeTripStop.route.id, routeTripStop.trip.id, routeTripStop.stop.id, dateNow, timeNow);
			// MyLog.d(TAG, "result: %s", todayTimestamps.size());
			allTimestamps.addAll(todayTimestamps);
			MyLog.d(TAG, "Checking today schedule... DONE");
			// 3RD - look for last schedule => not necessary, query should ask for now minus the previous duration they want to have
			// 4TH - check TOMORROW schedule
			MyLog.d(TAG, "Checking tomorrow schedule...");
			Calendar tomorrow = (Calendar) now.clone();
			tomorrow.add(Calendar.DATE, +1);
			final String tomorrowDate = DATE_FORMAT.format(tomorrow.getTime());
			// MyLog.d(TAG, "Date tomorrow: %s", tomorrowDate);
			// tomorrow.add(Calendar.DATE, -1);// TODO?
			final String afterMidnightHour = "000000";
			// MyLog.d(TAG, "Date tomorrow: %s", afterMidnightHour);
			final Set<Long> tomorrowTimestamps = findScheduleList(routeTripStop.route.id, routeTripStop.trip.id, routeTripStop.stop.id, tomorrowDate,
					afterMidnightHour);
			// MyLog.d(TAG, "result: %s", tomorrowTimestamps.size());
			allTimestamps.addAll(tomorrowTimestamps);
			MyLog.d(TAG, "Checking tomorrow schedule... DONE");
			// create JSON
			final JSONObject jResult = new JSONObject();
			jResult.put("realtime", false);
			jResult.put("source", getContext().getString(R.string.offline_schedule));
			final JSONArray jTimestamps = new JSONArray();
			Collections.sort(allTimestamps);
			for (Long t : allTimestamps) {
				jTimestamps.put(t);
			}
			jResult.put("timestamps", jTimestamps);
			if (allTimestamps.size() == 0) {
				jResult.put(
						"error",
						getContext().getString(R.string.stop_no_info_and_source, routeTripStop.route.shortName,
								getContext().getString(R.string.offline_schedule)));
			}
			// MyLog.d(TAG, "jResult: %s", jResult);
			// return result
			return getDepartureCursor(jResult);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", routeTripStop);
			return null;
		}
	}

	public static final int CACHE_MAX_VALIDITY_IN_SEC = 24 * 60 * 60; // 1 day

	@Override
	public int getCACHE_MAX_VALIDITY_IN_SEC() {
		return CACHE_MAX_VALIDITY_IN_SEC;
	}

	public static final int CACHE_NOT_REFRESHED_IN_SEC = 6 * 60 * 60; // 6 hours (1/4 day)

	@Override
	public int getCACHE_NOT_REFRESHED_IN_SEC() {
		return CACHE_NOT_REFRESHED_IN_SEC;
	}

	private static final String[] PROJECTION_SCHEDULE = new String[] { "departure" };

	private Set<Long> findScheduleList(int routeId, int tripId, int stopId, String dateS, String timeS) {
		MyLog.v(TAG, "findScheduleList(%s,%s,%s,%s,%s)", routeId, tripId, stopId, dateS, timeS);
		Set<Long> result = new HashSet<Long>();
		Cursor cursor = null;
		try {
			StringBuilder whereSb = new StringBuilder();
			whereSb.append(ScheduleColumns.T_SCHEDULES_K_TRIP_ID).append("=").append(tripId);
			whereSb.append(" AND ");
			whereSb.append(ScheduleColumns.T_SCHEDULES_K_STOP_ID).append("=").append(stopId);
			if (whereSb.length() > 0) {
				whereSb.append(" AND ");
			}
			whereSb.append(ServiceDateColumns.T_SERVICE_DATES_K_DATE).append("=").append(dateS);
			if (whereSb.length() > 0) {
				whereSb.append(" AND ");
			}
			whereSb.append(ScheduleColumns.T_SCHEDULES_K_DEPARTURE).append(">=").append(timeS);
			SQLiteDatabase db = getDBHelper(getContext(), String.valueOf(routeId)).getReadableDatabase();
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			cursor = qb.query(db, PROJECTION_SCHEDULE, whereSb.toString(), null, null, null, SCHEDULE_SORT_ORDER, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final Long timestamp = convertToTimestamp(cursor.getString(0), dateS);
						if (timestamp != null) {
							result.add(timestamp);
						}
					} while (cursor.moveToNext());
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

	public static final SimpleDateFormat TO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd" + "HHmmss");

	private Long convertToTimestamp(String dbDepartureLocalTime, String dateS) {
		try {
			int timeInt = Integer.valueOf(dbDepartureLocalTime);
			return TO_TIMESTAMP_FORMAT.parse(dateS + String.format("%06d", timeInt)).getTime();
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s %s!", dateS, dbDepartureLocalTime);
			return null;
		}
	}

}

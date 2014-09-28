package org.montrealtransit.android.provider.stmsubway.schedule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.common.AbstractScheduleProvider;
import org.montrealtransit.android.provider.common.ServiceDateColumns;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

public class StmSubwayScheduleProvider extends AbstractScheduleProvider {

	public static final String TAG = StmSubwayScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmsubway";

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	private static StmSubwayScheduleDbHelper stmSubwayScheduleDbHelper;

	private static int currentDbVersion = -1;

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

	private StmSubwayScheduleDbHelper getDBHelper(Context context) {
		MyLog.v(TAG, "getDBHelper()");
		if (stmSubwayScheduleDbHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			stmSubwayScheduleDbHelper = new StmSubwayScheduleDbHelper(context.getApplicationContext());
			currentDbVersion = StmSubwayScheduleDbHelper.DB_VERSION;
		} else { // reset
			try {
				if (currentDbVersion != StmSubwayScheduleDbHelper.DB_VERSION) {
					MyLog.d(TAG, "Update DB...");
					stmSubwayScheduleDbHelper.close();
					stmSubwayScheduleDbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				// fail if locked, will try again later
				MyLog.d(TAG, t, "Can't check DB version!");
			}
		}
		return stmSubwayScheduleDbHelper;
	}

	// NOT THREAD SAFE
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	// NOT THREAD SAFE
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
			if (jTimestamps.length() == 0) {
				jResult.put(
						"error",
						getContext().getString(R.string.stop_no_info_and_source, routeTripStop.route.shortName,
								getContext().getString(R.string.offline_schedule)));
			}
			// MyLog.d(TAG, "jResult: %s", jResult);
			// save to cache
			saveToCache(cacheUUID, jResult);
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

	private static final String[] PROJECTION_SERVICE_DATES = new String[] { "service_id" };

	private static final String RAW_FILE_FORMAT = "ca_mtl_stm_subway_schedules_stop_%s";

	private static final int STOP_SCHEDULE_FILE_COL_SERVICE_IDX = 0;
	private static final int STOP_SCHEDULE_FILE_COL_TRIP_IDX = 1;
	private static final int STOP_SCHEDULE_FILE_COL_STOP_IDX = 2;
	private static final int STOP_SCHEDULE_FILE_COL_DEPARTURE_IDX = 3;

	private Set<Long> findScheduleList(int routeId, int tripId, int stopId, String dateS, String timeS) {
		MyLog.v(TAG, "findScheduleList(%s,%s,%s,%s,%s)", routeId, tripId, stopId, dateS, timeS);
		long timeI = Integer.parseInt(timeS);
		Set<Long> result = new HashSet<Long>();
		// 1st find date service(s) in DB
		Set<String> serviceIds = findServices(dateS);
		// MyLog.d(TAG, "findScheduleList() > found %s service(s)", serviceIds.size());
		// 2nd read schedule file
		BufferedReader br = null;
		String line = null;
		String fileName = String.format(RAW_FILE_FORMAT, stopId);
		try {
			br = new BufferedReader(new InputStreamReader(getContext().getResources().openRawResource(
					getContext().getResources().getIdentifier(fileName, "raw", getContext().getPackageName())), "UTF8"), 8192);
			while ((line = br.readLine()) != null) {
				try {
					String[] lineItems = line.split(",");
					if (lineItems.length != 4) {
						MyLog.w(TAG, "Cannot parse schedule '%s'!", line);
						continue;
					}
					final String lineServiceId = lineItems[STOP_SCHEDULE_FILE_COL_SERVICE_IDX].substring(1, lineItems[STOP_SCHEDULE_FILE_COL_SERVICE_IDX].length() - 1);
					if (!serviceIds.contains(lineServiceId)) {
						// MyLog.d(TAG, "Wrong service id '%s' while looking for service ids '%s'!", lineServiceId, serviceIds);
						continue;
					}
					// MyLog.d(TAG, "GOOD service id '%s' while looking for service ids '%s'!", lineServiceId, serviceIds);
					final int lineTripId = Integer.parseInt(lineItems[STOP_SCHEDULE_FILE_COL_TRIP_IDX]);
					if (tripId != lineTripId) { // TODO LATER other trip ID schedule maybe useful in cache ???
						// MyLog.d(TAG, "Wrong trip id '%s' while looking for trip id '%s'!", lineTripId, tripId);
						continue;
					}
					final int lineStopId = Integer.parseInt(lineItems[STOP_SCHEDULE_FILE_COL_STOP_IDX]);
					if (stopId != lineStopId) {
						MyLog.w(TAG, "Wrong stop id '%s' while looking for stop id '%s'!", lineStopId, stopId);
						continue;
					}
					final int lineDeparture = Integer.parseInt(lineItems[STOP_SCHEDULE_FILE_COL_DEPARTURE_IDX]);
					if (lineDeparture > timeI) {
						result.add(convertToTimestamp(lineDeparture, dateS));
						// } else {
						// MyLog.d(TAG, "Too soon '%s' (after:%s)!", lineDeparture, timeI);
					}
				} catch (Exception e) {
					MyLog.w(TAG, e, "Cannot parse schedule '%s' (fileName: %s)!", line, fileName);
				}
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while reading stop time from file! (fileName: %s, line: %s)", fileName, line);
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				MyLog.w(TAG, "ERROR while closing the input stream!", e);
			}
		}
		return result;
	}

	public Set<String> findServices(String dateS) {
		Set<String> serviceIds = new HashSet<String>();
		Cursor cursor = null;
		try {
			StringBuilder whereSb = new StringBuilder();
			whereSb.append(ServiceDateColumns.T_SERVICE_DATES_K_DATE).append("=").append(dateS);
			SQLiteDatabase db = getDBHelper(getContext()).getReadableDatabase();
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(StmSubwayScheduleDbHelper.T_SERVICE_DATES);
			cursor = qb.query(db, PROJECTION_SERVICE_DATES, whereSb.toString(), null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						final String serviceId = cursor.getString(0);
						if (!TextUtils.isEmpty(serviceId)) {
							serviceIds.add(serviceId);
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
		return serviceIds;
	}

	// NOT THREAD SAFE
	public static final SimpleDateFormat TO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd" + "HHmmss");

	private Long convertToTimestamp(int timeInt, String dateS) {
		try {
			return TO_TIMESTAMP_FORMAT.parse(dateS + String.format("%06d", timeInt)).getTime();
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s %s!", dateS, timeInt);
			return null;
		}
	}

}

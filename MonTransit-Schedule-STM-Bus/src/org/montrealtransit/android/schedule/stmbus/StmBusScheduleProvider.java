package org.montrealtransit.android.schedule.stmbus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.ServiceDateColumns;
import org.montrealtransit.android.schedule.stmbus.activity.SplashScreen;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class StmBusScheduleProvider extends ContentProvider {

	public static final String TAG = StmBusScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmbus";

	// @Deprecated
	// private static final int ROUTE_TRIP_STOP = 1;
	// @Deprecated
	// private static final int ROUTE_TRIP_STOP_DATE_TIME = 2;
	// @Deprecated
	// private static final int ROUTE_STOP = 3;
	// @Deprecated
	// private static final int ROUTE_STOP_DATE = 4;
	// @Deprecated
	// private static final int ROUTE_STOP_TIME = 5;
	// @Deprecated
	// private static final int ROUTE_STOP_DATE_TIME = 6;
	// @Deprecated
	// private static final int ROUTE_DEPARTURE = 7;
	private static final int DEPARTURE = 8;
	private static final int PING = 100;

	// private static final HashMap<String, String> SCHEDULE_PROJECTION_MAP;
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "ping", PING);
		URI_MATCHER.addURI(AUTHORITY, "departure", DEPARTURE);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/departure", ROUTE_DEPARTURE);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/trip/#/stop/#", ROUTE_TRIP_STOP);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/trip/#/stop/#/date/#/time/#", ROUTE_TRIP_STOP_DATE_TIME);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#", ROUTE_STOP);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/date/#", ROUTE_STOP_DATE);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/time/#", ROUTE_STOP_TIME);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/date/#/time/#", ROUTE_STOP_DATE_TIME);

		// HashMap<String, String> map = new HashMap<String, String>();
		// map.put(StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE, StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE
		// + " AS " + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE);
		// SCHEDULE_PROJECTION_MAP = map;

	}

	private static StmBusScheduleDbHelper stmBusScheduleDbHelper;

	private static int currentDbVersion = -1;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		ping();
		return true;
	}

	private void ping() {
		// remove this app icon
		SplashScreen.removeLauncherIcon(getContext());
	}

	private StmBusScheduleDbHelper getDBHelper(Context context) {
		MyLog.v(TAG, "getDBHelper()");
		if (stmBusScheduleDbHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			stmBusScheduleDbHelper = new StmBusScheduleDbHelper(context.getApplicationContext());
			currentDbVersion = StmBusScheduleDbHelper.DB_VERSION;
		} else { // reset
			try {
				if (currentDbVersion != StmBusScheduleDbHelper.DB_VERSION) {
					MyLog.d(TAG, "Update DB...");
					stmBusScheduleDbHelper.close();
					stmBusScheduleDbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				// fail if locked, will try again later
				MyLog.d(TAG, t, "Can't check DB version!");
			}
		}
		return stmBusScheduleDbHelper;
	}

	public static final String SCHEDULE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".schedule";
	public static final String DEPARTURE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".departure";

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		// case ROUTE_DEPARTURE:
		// case ROUTE_TRIP_STOP:
		// case ROUTE_TRIP_STOP_DATE_TIME:
		// case ROUTE_STOP:
		// case ROUTE_STOP_DATE:
		// case ROUTE_STOP_TIME:
		// case ROUTE_STOP_DATE_TIME:
		// return SCHEDULE_CONTENT_TYPE;
		case DEPARTURE:
			return DEPARTURE_CONTENT_TYPE;
		case PING:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		MyLog.i(TAG, "[%s]", uri);
		switch (URI_MATCHER.match(uri)) {
		case PING:
			MyLog.v(TAG, "query>PING");
			ping();
			return null;
		case DEPARTURE:
			return getDeparture(selection);
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
		// SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		// String limit = null;
		// final List<String> pathSegments = uri.getPathSegments();
		// if (pathSegments.size() < 1) {
		// MyLog.w(TAG, "Cannot lookup schedule without route ID!");
		// return null;
		// }
		// String routeId = pathSegments.get(1);
		// final Date now = new Date();
		// switch (URI_MATCHER.match(uri)) {
		// case ROUTE_DEPARTURE:
		// MyLog.v(TAG, "query>DEPARTURE");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// break;
		// case ROUTE_TRIP_STOP:
		// MyLog.v(TAG, "query>ROUTE_TRIP_STOP");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(5));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
		// break;
		// case ROUTE_TRIP_STOP_DATE_TIME:
		// MyLog.v(TAG, "query>ROUTE_TRIP_STOP_DATE_TIME");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + routeId);
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(5));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(7));
		// break;
		// case ROUTE_STOP:
		// MyLog.v(TAG, "query>ROUTE_STOP");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// // qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
		// // routeId + "00 AND " + routeId + "99");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
		// break;
		// case ROUTE_STOP_DATE:
		// MyLog.v(TAG, "query>ROUTE_STOP_DATE");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// // qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
		// // routeId + "00 AND " + routeId + "99");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(5));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
		// break;
		// case ROUTE_STOP_TIME:
		// MyLog.v(TAG, "query>ROUTE_STOP_TIME");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// // qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
		// // routeId + "00 AND " + routeId + "99");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(5));
		// break;
		// case ROUTE_STOP_DATE_TIME:
		// MyLog.v(TAG, "query>ROUTE_STOP_DATE_TIME");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// // qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
		// // routeId + "00 AND " + routeId + "99");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(5));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(7));
		// break;
		// default:
		// throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		// }
		// // limit = "7"; // limited to the next 7 passages
		// // If no sort order is specified use the default
		// String orderBy;
		// if (TextUtils.isEmpty(sortOrder)) {
		// switch (URI_MATCHER.match(uri)) {
		// case ROUTE_DEPARTURE:
		// case ROUTE_TRIP_STOP:
		// case ROUTE_TRIP_STOP_DATE_TIME:
		// case ROUTE_STOP:
		// case ROUTE_STOP_DATE:
		// case ROUTE_STOP_TIME:
		// case ROUTE_STOP_DATE_TIME:
		// orderBy = SCHEDULE_SORT_ORDER;
		// break;
		// case PING:
		// return null;
		// default:
		// throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
		// }
		// } else {
		// orderBy = sortOrder;
		// }
		//
		// SQLiteDatabase db = getDBHelper(getContext(), routeId).getReadableDatabase();
		// Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);
		// if (cursor != null) {
		// cursor.setNotificationUri(getContext().getContentResolver(), uri);
		// }
		// // closeDbHelper();
		// return cursor;
	}

	public Cursor getDeparture(String selection) {
		MyLog.d(TAG, "getDeparture(%s)", selection);
		try {
			JSONObject jSelection = new JSONObject(selection);
			// extract values from JSON
			RouteTripStop routeTripStop = RouteTripStop.fromJSON(jSelection.optJSONObject("routeTripStop"));
			long timestamp = jSelection.has("timestamp") ? jSelection.getLong("timestamp") : System.currentTimeMillis();
			boolean cacheOnly = jSelection.has("cacheOnly") ? jSelection.getBoolean("cacheOnly") : false;
			// TODO cache int cacheValidityInSec = jSelection.has("cacheValidityInSec") ? jSelection.getInt("cacheValidityInSec") :
			// getCACHE_MAX_VALIDITY_IN_SEC();
			// TODO cache int cacheNotRefreshedInSec = Math.min(getCACHE_NOT_REFRESHED_IN_SEC(), cacheValidityInSec);
			// read cache
			String cacheUUID = routeTripStop.getUUID() + getAUTHORITY();
			// TODO cache Cache cache = getDataAlreadyInCacheIfStillUseful(cacheUUID);
			Object cache = null;
			// IF cache only DO return cache OR nothing
			if (cacheOnly) {
				JSONObject jResult = null;
				// TODO cache if (cache != null) {
				// TODO cache try {
				// TODO cache jResult = new JSONObject(cache.getObject());
				// TODO cache } catch (JSONException jsone) {
				// TODO cache MyLog.w(TAG, jsone, "Error while parsing JSON from cache!");
				// TODO cache // cache not valid, returning empty
				// TODO cache }
				// TODO cache }
				return getDepartureCursor(jResult);
			}
			// IF cache doesn't have to be refreshed DO return cache
			// TODO cache int tooOld = /* Utils. */currentTimeSec() - cacheNotRefreshedInSec;
			// TODO cacheif (cache != null && tooOld <= cache.getDate()) {
			// TODO cache try {
			// TODO cache return getDepartureCursor(new JSONObject(cache.getObject()));
			// TODO cache } catch (JSONException jsone) {
			// TODO cache MyLog.w(TAG, jsone, "Error while parsing JSON from cache!");
			// TODO cache // cache not valid, loading from www
			// TODO cache }
			// TODO cache}
			final Calendar now = Calendar.getInstance();
			now.setTimeInMillis(timestamp);
			// get departure from content provider
			return getDeparture(routeTripStop, now, cache, cacheUUID);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", selection);
			return null;
		}
	}

	public static final int CACHE_MAX_VALIDITY_IN_SEC = 24 * 60 * 60; // 1 day

	public int getCACHE_MAX_VALIDITY_IN_SEC() {
		return CACHE_MAX_VALIDITY_IN_SEC;
	}

	public static final int CACHE_NOT_REFRESHED_IN_SEC = 6 * 60 * 60; // 6 hours (1/4 day)

	public int getCACHE_NOT_REFRESHED_IN_SEC() {
		return CACHE_NOT_REFRESHED_IN_SEC;
	}

	public static String getAUTHORITY() {
		return AUTHORITY;
	}

	public Cursor getDeparture(RouteTripStop routeTripStop, Calendar now, Object cache, String cacheUUID) { // TODO cache Cache cache
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
			// TODO cache // save to cache
			// TODO cache saveToCache(cacheUUID, jResult);
			// return result
			return getDepartureCursor(jResult);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", routeTripStop);
			return null;
		}
	}

	public Cursor getDepartureCursor(JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "json" });
		matrixCursor.addRow(new Object[] { jsonObject.toString() });
		return matrixCursor;
	}

	private static final String[] PROJECTION_SERVICE_DATES = new String[] { "service_id" };

	private static final String RAW_FILE_FORMAT = "ca_mtl_stm_bus_schedules_stop_%s";

	private Set<Long> findScheduleList(int routeId, int tripId, int stopId, String dateS, String timeS) {
		MyLog.v(TAG, "findScheduleList(%s,%s,%s,%s,%s)", routeId, tripId, stopId, dateS, timeS);
		long timeI = Integer.parseInt(timeS);
		Set<Long> result = new HashSet<Long>();
		Set<String> serviceIds = new HashSet<String>();
		Cursor cursor = null;
		try {
			StringBuilder whereSb = new StringBuilder();
			whereSb.append(ServiceDateColumns.T_SERVICE_DATES_K_DATE).append("=").append(dateS);
			SQLiteDatabase db = getDBHelper(getContext()).getReadableDatabase();
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(StmBusScheduleDbHelper.T_SERVICE_DATES);
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
		// MyLog.d(TAG, "findScheduleList() > found %s service(s)", serviceIds.size());
		// read file
		BufferedReader br = null;
		String line = null;
		String fileName = String.format(RAW_FILE_FORMAT, stopId);
		try {
			br = new BufferedReader(new InputStreamReader(getContext().getResources().openRawResource(
					getContext().getResources().getIdentifier(fileName, "raw", getContext().getPackageName())), "UTF8"), 8192);
			while ((line = br.readLine()) != null) {
				// db.execSQL(String.format(sqlInsert, line));
				try {
					String[] lineItems = line.split(",");
					if (lineItems.length != 4) {
						MyLog.w(TAG, "Cannot parse schedule '%s'!", line);
						continue;
					}
					final String lineServiceId = lineItems[0].substring(1, lineItems[0].length() - 1);
					if (!serviceIds.contains(lineServiceId)) {
						// MyLog.d(TAG, "Wrong service id '%s' while looking for service ids '%s'!", lineServiceId, serviceIds);
						continue;
					}
					// MyLog.d(TAG, "GOOD service id '%s' while looking for service ids '%s'!", lineServiceId, serviceIds);
					final int lineTripId = Integer.parseInt(lineItems[1]);
					if (tripId != lineTripId) { // TODO LATER other trip ID schedule maybe useful in cache ???
						// MyLog.d(TAG, "Wrong trip id '%s' while looking for trip id '%s'!", lineTripId, tripId);
						continue;
					}
					final int lineStopId = Integer.parseInt(lineItems[2]);
					if (stopId != lineStopId) {
						MyLog.w(TAG, "Wrong stop id '%s' while looking for stop id '%s'!", lineStopId, stopId);
						continue;
					}
					final int lineDeparture = Integer.parseInt(lineItems[3]);
					if (lineDeparture > timeI) {
						result.add(convertToTimestamp(lineDeparture, dateS));
						// } else {
						// MyLog.d(TAG, "Too soon '%s' (after:%s)!", lineDeparture, timeI);
					}
				} catch (Exception e) {
					MyLog.w(TAG, e, "Cannot parse schedule '%s' (fileName: %s)!", line, fileName);
				}
			}
			// }
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

	public static final SimpleDateFormat TO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd" + "HHmmss");

	private Long convertToTimestamp(int timeInt, String dateS) {
		try {
			return TO_TIMESTAMP_FORMAT.parse(dateS + String.format("%06d", timeInt)).getTime();
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s %s!", dateS, timeInt);
			return null;
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete()");
		MyLog.w(TAG, "The delete method is not available.");
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "update()");
		MyLog.w(TAG, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		MyLog.v(TAG, "insert()");
		MyLog.w(TAG, "The insert method is not available.");
		return null;
	}

	public static int toTimestampInSeconds(Long timestampInMillis) {
		if (timestampInMillis == null) {
			return 0;
		}
		return (int) (timestampInMillis.longValue() / 1000);
	}

	/**
	 * @return the local system time in seconds
	 */
	public static int currentTimeSec() {
		return toTimestampInSeconds(System.currentTimeMillis());
	}

}

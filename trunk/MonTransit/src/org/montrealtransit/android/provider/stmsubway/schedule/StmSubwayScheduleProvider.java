package org.montrealtransit.android.provider.stmsubway.schedule;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class StmSubwayScheduleProvider extends ContentProvider {

	public static final String TAG = StmSubwayScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmsubway";

	@Deprecated
	private static final int ROUTE_TRIP_STOP = 1;
	@Deprecated
	private static final int ROUTE_TRIP_STOP_DATE_TIME = 2;
	@Deprecated
	private static final int ROUTE_STOP = 3;
	@Deprecated
	private static final int ROUTE_STOP_DATE = 4;
	@Deprecated
	private static final int ROUTE_STOP_TIME = 5;
	@Deprecated
	private static final int ROUTE_STOP_DATE_TIME = 6;
	// @Deprecated
	// private static final int ROUTE_TRIP_STOP_TIME = 7;
	private static final int ROUTE_DEPARTURE = 7;
	private static final int PING = 100;

	private static final HashMap<String, String> SCHEDULE_PROJECTION_MAP;
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "ping", PING);
		URI_MATCHER.addURI(AUTHORITY, "route/#/departure", ROUTE_DEPARTURE);
		URI_MATCHER.addURI(AUTHORITY, "route/#/trip/#/stop/#", ROUTE_TRIP_STOP);
		// URI_MATCHER.addURI(AUTHORITY, "route/#/trip/#/stop/#/time/#", ROUTE_TRIP_STOP_TIME);
		URI_MATCHER.addURI(AUTHORITY, "route/#/trip/#/stop/#/date/#/time/#", ROUTE_TRIP_STOP_DATE_TIME);
		URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#", ROUTE_STOP);
		URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/date/#", ROUTE_STOP_DATE);
		URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/time/#", ROUTE_STOP_TIME);
		URI_MATCHER.addURI(AUTHORITY, "route/#/stop/#/date/#/time/#", ROUTE_STOP_DATE_TIME);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE, StmSubwayScheduleDbHelper.T_SCHEDULES + "."
				+ StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " AS " + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE);
		SCHEDULE_PROJECTION_MAP = map;

	}

	private static final String SCHEDULE_SERVICE_DATE_JOIN = StmSubwayScheduleDbHelper.T_SCHEDULES + " LEFT OUTER JOIN "
			+ StmSubwayScheduleDbHelper.T_SERVICE_DATES + " ON " + StmSubwayScheduleDbHelper.T_SCHEDULES + "."
			+ StmSubwayScheduleDbHelper.T_SCHEDULES_K_SERVICE_ID + "=" + StmSubwayScheduleDbHelper.T_SERVICE_DATES + "."
			+ StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_SERVICE_ID;

	private static StmSubwayScheduleDbHelper stmSubwayScheduleDbHelper;
	private static String stmSubwayScheduleDbHelperRouteId;

	private static Map<String, Integer> currentDbVersion = new HashMap<String, Integer>();

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		// remove this app icon
		// SplashScreen.removeLauncherIcon(getContext());
		return true;
	}

	private static StmSubwayScheduleDbHelper getDBHelper(Context context, String routeId) {
		MyLog.v(TAG, "getDBHelper(%s)", routeId);
		if (/* stmSubwayScheduleDbHelper != null && */stmSubwayScheduleDbHelperRouteId != null && !stmSubwayScheduleDbHelperRouteId.equals(routeId)) {
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

	public static final String SCHEDULE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".schedule";

	public static final String SCHEDULE_SORT_ORDER = StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " ASC";

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case ROUTE_DEPARTURE:
		case ROUTE_TRIP_STOP:
			// case ROUTE_TRIP_STOP_TIME:
		case ROUTE_TRIP_STOP_DATE_TIME:
		case ROUTE_STOP:
		case ROUTE_STOP_DATE:
		case ROUTE_STOP_TIME:
		case ROUTE_STOP_DATE_TIME:
			return SCHEDULE_CONTENT_TYPE;
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
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[%s]", uri);
		switch (URI_MATCHER.match(uri)) {
		case PING:
			MyLog.v(TAG, "query>PING");
			// remove this app icon
			// SplashScreen.removeLauncherIcon(getContext());
			return null;
		}
		String limit = null;
		final List<String> pathSegments = uri.getPathSegments();
		if (pathSegments.size() < 1) {
			MyLog.w(TAG, "Cannot lookup schedule without route ID!");
			return null;
		}
		String routeId = pathSegments.get(1);
		final Date now = new Date();
		switch (URI_MATCHER.match(uri)) {
		case ROUTE_DEPARTURE:
			MyLog.v(TAG, "query>DEPARTURE");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			break;
		case ROUTE_TRIP_STOP:
			MyLog.v(TAG, "query>ROUTE_TRIP_STOP");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		// case ROUTE_TRIP_STOP_TIME:
		// MyLog.v(TAG, "query>ROUTE_TRIP_STOP_TIME");
		// qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
		// qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
		// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + pathSegments.get(3));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(5));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
		// qb.appendWhere(" AND ");
		// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(7));
		// break;
		case ROUTE_TRIP_STOP_DATE_TIME:
			MyLog.v(TAG, "query>ROUTE_TRIP_STOP_DATE_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(7));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(9));
			break;
		case ROUTE_STOP:
			MyLog.v(TAG, "query>ROUTE_STOP");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		case ROUTE_STOP_DATE:
			MyLog.v(TAG, "query>ROUTE_STOP_DATE");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		case ROUTE_STOP_TIME:
			MyLog.v(TAG, "query>ROUTE_STOP_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(5));
			break;
		case ROUTE_STOP_DATE_TIME:
			MyLog.v(TAG, "query>ROUTE_STOP_DATE_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + pathSegments.get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SERVICE_DATES + "." + StmSubwayScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + pathSegments.get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmSubwayScheduleDbHelper.T_SCHEDULES + "." + StmSubwayScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + pathSegments.get(7));
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
		// limit = "7"; // limited to the next 7 passages
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case ROUTE_DEPARTURE:
			case ROUTE_TRIP_STOP:
				// case ROUTE_TRIP_STOP_TIME:
			case ROUTE_TRIP_STOP_DATE_TIME:
			case ROUTE_STOP:
			case ROUTE_STOP_DATE:
			case ROUTE_STOP_TIME:
			case ROUTE_STOP_DATE_TIME:
				orderBy = SCHEDULE_SORT_ORDER;
				break;
			case PING:
				return null;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
			}
		} else {
			orderBy = sortOrder;
		}
		SQLiteDatabase db = getDBHelper(getContext(), routeId).getReadableDatabase();
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);
		if (cursor != null) {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		// closeDbHelper();
		return cursor;
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

}

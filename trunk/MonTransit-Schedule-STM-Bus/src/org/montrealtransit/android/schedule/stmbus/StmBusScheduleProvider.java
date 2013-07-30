package org.montrealtransit.android.schedule.stmbus;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.montrealtransit.android.MyLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class StmBusScheduleProvider extends ContentProvider {

	public static final String TAG = StmBusScheduleProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.schedule.stmbus";

	private static final int ROUTE_TRIP_STOP = 1;
	private static final int ROUTE_TRIP_STOP_DATE_TIME = 2;
	private static final int ROUTE_STOP = 3;
	private static final int ROUTE_STOP_DATE = 4;
	private static final int ROUTE_STOP_TIME = 5;
	private static final int ROUTE_STOP_DATE_TIME = 6;

	private static final HashMap<String, String> SCHEDULE_PROJECTION_MAP;
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/trip/#/stop/#", ROUTE_TRIP_STOP);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/trip/#/stop/#/date/#/time/#", ROUTE_TRIP_STOP_DATE_TIME);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/stop/#", ROUTE_STOP);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/stop/#/date/#", ROUTE_STOP_DATE);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/stop/#/time/#", ROUTE_STOP_TIME);
		URI_MATCHER.addURI(AUTHORITY, "/route/#/stop/#/date/#/time/#", ROUTE_STOP_DATE_TIME);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE, StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE
				+ " AS " + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE);
		SCHEDULE_PROJECTION_MAP = map;

	}

	private static final String SCHEDULE_SERVICE_DATE_JOIN = StmBusScheduleDbHelper.T_SCHEDULES + " LEFT OUTER JOIN " + StmBusScheduleDbHelper.T_SERVICE_DATES
			+ " ON " + StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_SERVICE_ID + "="
			+ StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_SERVICE_ID;

	private static StmBusScheduleDbHelper stmBusScheduleDbHelper;
	private static String stmBusScheduleDbHelperRouteId;

	private static Map<String, Integer> currentDbVersion = new HashMap<String, Integer>();

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	private static StmBusScheduleDbHelper getDBHelper(Context context, String routeId) {
		if (/* stmBusScheduleDbHelper != null && */stmBusScheduleDbHelperRouteId != null && !stmBusScheduleDbHelperRouteId.equals(routeId)) {
			closeDbHelper();
		}
		if (stmBusScheduleDbHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			stmBusScheduleDbHelper = new StmBusScheduleDbHelper(context.getApplicationContext(), routeId);
			stmBusScheduleDbHelperRouteId = routeId;
			currentDbVersion.put(routeId, StmBusScheduleDbHelper.DB_VERSION);
		} else { // reset
			try {
				if (currentDbVersion.containsKey(routeId) || currentDbVersion.get(routeId) != StmBusScheduleDbHelper.DB_VERSION) {
					MyLog.d(TAG, "Update DB...");
					closeDbHelper();
					return getDBHelper(context, routeId);
				}
			} catch (Exception e) {
				// fail if locked, will try again later
				MyLog.d(TAG, "Can't check DB version!", e);
			}
		}
		return stmBusScheduleDbHelper;
	}

	private static void closeDbHelper() {
		if (stmBusScheduleDbHelper != null) {
			stmBusScheduleDbHelper.close();
			stmBusScheduleDbHelper = null;
			stmBusScheduleDbHelperRouteId = null;
		}
	}

	public static final String SCHEDULE_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".schedule";

	public static final String SCHEDULE_SORT_ORDER = StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " ASC";

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case ROUTE_TRIP_STOP:
		case ROUTE_TRIP_STOP_DATE_TIME:
		case ROUTE_STOP:
		case ROUTE_STOP_DATE:
		case ROUTE_STOP_TIME:
		case ROUTE_STOP_DATE_TIME:
			return SCHEDULE_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): " + uri + ""));
		}
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[%s]", uri);
		String limit = null;
		String routeId = uri.getPathSegments().get(1);
		final Date now = new Date();
		switch (URI_MATCHER.match(uri)) {
		case ROUTE_TRIP_STOP:
			MyLog.v(TAG, "query>ROUTE_TRIP_STOP");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		case ROUTE_TRIP_STOP_DATE_TIME:
			MyLog.v(TAG, "query>ROUTE_TRIP_STOP_DATE_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " = " + routeId);
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + uri.getPathSegments().get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + uri.getPathSegments().get(7));
			break;
		case ROUTE_STOP:
			MyLog.v(TAG, "query>ROUTE_STOP");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		case ROUTE_STOP_DATE:
			MyLog.v(TAG, "query>ROUTE_STOP_DATE");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + uri.getPathSegments().get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + TIME_FORMAT.format(now));
			break;
		case ROUTE_STOP_TIME:
			MyLog.v(TAG, "query>ROUTE_STOP_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + DATE_FORMAT.format(now));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + uri.getPathSegments().get(5));
			break;
		case ROUTE_STOP_DATE_TIME:
			MyLog.v(TAG, "query>ROUTE_STOP_DATE_TIME");
			qb.setTables(SCHEDULE_SERVICE_DATE_JOIN);
			qb.setProjectionMap(SCHEDULE_PROJECTION_MAP);
			// qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " BETWEEN " +
			// routeId + "00 AND " + routeId + "99");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_TRIP_ID + " LIKE '" + routeId + "%'");
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_STOP_ID + " = " + uri.getPathSegments().get(3));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SERVICE_DATES + "." + StmBusScheduleDbHelper.T_SERVICE_DATES_K_DATE + " = " + uri.getPathSegments().get(5));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmBusScheduleDbHelper.T_SCHEDULES + "." + StmBusScheduleDbHelper.T_SCHEDULES_K_DEPARTURE + " >= " + uri.getPathSegments().get(7));
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): %s", uri));
		}
		limit = "7"; // limited to the next 7 passages
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case ROUTE_TRIP_STOP:
			case ROUTE_TRIP_STOP_DATE_TIME:
			case ROUTE_STOP:
			case ROUTE_STOP_DATE:
			case ROUTE_STOP_TIME:
			case ROUTE_STOP_DATE_TIME:
				orderBy = SCHEDULE_SORT_ORDER;
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (order): %s", uri));
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

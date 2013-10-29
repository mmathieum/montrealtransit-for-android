package org.montrealtransit.android.provider.stmbus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.common.AbstractDbHelper;
import org.montrealtransit.android.provider.common.RouteColumns;
import org.montrealtransit.android.provider.common.RouteTripColumns;
import org.montrealtransit.android.provider.common.RouteTripStopColumns;
import org.montrealtransit.android.provider.common.SqlUtils;
import org.montrealtransit.android.provider.common.StopColumns;
import org.montrealtransit.android.provider.common.TripColumns;
import org.montrealtransit.android.provider.common.TripStopColumns;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

// TODO extract non STM-Bus specific into abstract provider
public class StmBusProvider extends ContentProvider {

	public static final String TAG = StmBusProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.stmbus";

	public static final String ROUTE_SORT_ORDER = AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " ASC";
	public static final String TRIP_SORT_ORDER = AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " ASC";
	public static final String TRIP_STOPS_SORT_ORDER = AbstractDbHelper.T_TRIP_STOPS + "." + AbstractDbHelper.T_TRIP_STOPS_K_ID + " ASC";
	public static final String STOP_SORT_ORDER = AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " ASC";
	public static final String ROUTE_TRIP_STOP_SORT_ORDER = ROUTE_SORT_ORDER + ", " + TRIP_SORT_ORDER + ", " + STOP_SORT_ORDER;
	public static final String ROUTE_TRIP_SORT_ORDER = ROUTE_SORT_ORDER + ", " + TRIP_SORT_ORDER;
	public static final String TRIP_STOP_SORT_ORDER = TRIP_SORT_ORDER + ", " + STOP_SORT_ORDER;

	public static final String ROUTE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".route";
	public static final String ROUTE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".route";
	public static final String TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".trip";
	public static final String TRIP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".trip";
	public static final String STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".stop";
	public static final String STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".stop";
	public static final String ROUTE_TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".routetripstop";
	public static final String ROUTE_TRIP_STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".routetripstop";
	public static final String TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".tripstop";
	public static final String TRIP_STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".tripstop";
	public static final String ROUTE_TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".routetrip";
	public static final String ROUTE_TRIP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".routetrip";

	private static final int ROUTES = 1;
	private static final int STOPS = 2;
	private static final int TRIPS = 3;
	private static final int ROUTES_TRIPS_STOPS = 4;
	private static final int ROUTES_TRIPS_STOPS_SEARCH = 5;
	private static final int ROUTES_TRIPS = 6;
	private static final int TRIPS_STOPS = 7;
	private static final int SEARCH_NO_KEYWORD = 8;
	private static final int SEARCH_WITH_KEYWORD = 9;
	private static final int DB_VERSION = 100;
	private static final int DB_DEPLOYED = 101;
	private static final int DB_LABEL = 102;
	private static final int DB_SETUP_REQUIRED = 103;

	private static final Map<String, String> ROUTE_PROJECTION_MAP;
	private static final Map<String, String> TRIP_PROJECTION_MAP;
	private static final Map<String, String> STOP_PROJECTION_MAP;
	private static final Map<String, String> ROUTE_TRIP_STOP_PROJECTION_MAP;
	private static final Map<String, String> ROUTE_TRIP_PROJECTION_MAP;
	private static final Map<String, String> TRIP_STOP_PROJECTION_MAP;
	private static final Map<String, String> SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP;
	private static final UriMatcher URI_MATCHER;

	private static final String UID_SEPARATOR = "-";
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "version", DB_VERSION);
		URI_MATCHER.addURI(AUTHORITY, "deployed", DB_DEPLOYED);
		URI_MATCHER.addURI(AUTHORITY, "label", DB_LABEL);
		URI_MATCHER.addURI(AUTHORITY, "setuprequired", DB_SETUP_REQUIRED);
		URI_MATCHER.addURI(AUTHORITY, "route", ROUTES);
		URI_MATCHER.addURI(AUTHORITY, "trip", TRIPS);
		URI_MATCHER.addURI(AUTHORITY, "stop", STOPS);
		URI_MATCHER.addURI(AUTHORITY, "route/trip/stop", ROUTES_TRIPS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "route/trip/stop/*", ROUTES_TRIPS_STOPS_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "route/trip", ROUTES_TRIPS);
		URI_MATCHER.addURI(AUTHORITY, "trip/stop", TRIPS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_NO_KEYWORD);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_WITH_KEYWORD);

		HashMap<String, String> map;

		map = new HashMap<String, String>();
		map.put(RouteColumns.T_ROUTE_K_ID, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID + " AS " + RouteColumns.T_ROUTE_K_ID);
		map.put(RouteColumns.T_ROUTE_K_SHORT_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteColumns.T_ROUTE_K_LONG_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_LONG_NAME + " AS " + RouteColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteColumns.T_ROUTE_K_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_COLOR + " AS " + RouteColumns.T_ROUTE_K_COLOR);
		map.put(RouteColumns.T_ROUTE_K_TEXT_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(TripColumns.T_TRIP_K_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + " AS " + TripColumns.T_TRIP_K_ID);
		map.put(TripColumns.T_TRIP_K_HEADSIGN_TYPE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ TripColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(TripColumns.T_TRIP_K_HEADSIGN_VALUE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ TripColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(TripColumns.T_TRIP_K_ROUTE_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ROUTE_ID + " AS " + TripColumns.T_TRIP_K_ROUTE_ID);
		TRIP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(StopColumns.T_STOP_K_ID, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + " AS " + StopColumns.T_STOP_K_ID);
		map.put(StopColumns.T_STOP_K_CODE, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + " AS " + StopColumns.T_STOP_K_CODE);
		map.put(StopColumns.T_STOP_K_NAME, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_NAME + " AS " + StopColumns.T_STOP_K_NAME);
		map.put(StopColumns.T_STOP_K_LAT, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LAT + " AS " + StopColumns.T_STOP_K_LAT);
		map.put(StopColumns.T_STOP_K_LNG, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LNG + " AS " + StopColumns.T_STOP_K_LNG);
		STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(RouteTripStopColumns.T_STOP_K_ID, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + " AS " + RouteTripStopColumns.T_STOP_K_ID);
		map.put(RouteTripStopColumns.T_STOP_K_CODE, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + " AS " + RouteTripStopColumns.T_STOP_K_CODE);
		map.put(RouteTripStopColumns.T_STOP_K_NAME, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_NAME + " AS " + RouteTripStopColumns.T_STOP_K_NAME);
		map.put(RouteTripStopColumns.T_STOP_K_LAT, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LAT + " AS " + RouteTripStopColumns.T_STOP_K_LAT);
		map.put(RouteTripStopColumns.T_STOP_K_LNG, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LNG + " AS " + RouteTripStopColumns.T_STOP_K_LNG);
		map.put(RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE + " AS "
				+ RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		map.put(RouteTripStopColumns.T_TRIP_K_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + " AS " + RouteTripStopColumns.T_TRIP_K_ID);
		map.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(RouteTripStopColumns.T_TRIP_K_ROUTE_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ROUTE_ID + " AS "
				+ RouteTripStopColumns.T_TRIP_K_ROUTE_ID);
		map.put(RouteTripStopColumns.T_ROUTE_K_ID, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID + " AS " + RouteTripStopColumns.T_ROUTE_K_ID);
		map.put(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteTripStopColumns.T_ROUTE_K_LONG_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_LONG_NAME + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteTripStopColumns.T_ROUTE_K_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_COLOR + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_COLOR);
		map.put(RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_TRIP_STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		// TODO use real UID (needs trip ID)
		// map.put(BaseColumns._ID, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID + "||'" + UID_SEPARATOR + "'||" + StmBusDbHelper.T_TRIP + "."
		// + StmBusDbHelper.T_TRIP_K_ID + "||'" + UID_SEPARATOR + "'||" + StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + " AS "
		// + BaseColumns._ID);
		// map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID + "||'" + UID_SEPARATOR + "'||"
		// + StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + "||'" + UID_SEPARATOR + "'||" + StmBusDbHelper.T_STOP + "."
		// + StmBusDbHelper.T_STOP_K_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(BaseColumns._ID, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + "||'" + UID_SEPARATOR + "'||" + StmBusDbHelper.T_ROUTE + "."
				+ StmBusDbHelper.T_ROUTE_K_SHORT_NAME + " AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + "||'" + UID_SEPARATOR + "'||"
				+ StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_SHORT_NAME + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + "||' - '||" + StmBusDbHelper.T_ROUTE + "."
				+ StmBusDbHelper.T_ROUTE_K_SHORT_NAME + "||' '||" + StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(RouteTripColumns.T_TRIP_K_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + " AS " + RouteTripColumns.T_TRIP_K_ID);
		map.put(RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(RouteTripColumns.T_TRIP_K_ROUTE_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ROUTE_ID + " AS "
				+ RouteTripColumns.T_TRIP_K_ROUTE_ID);
		map.put(RouteTripColumns.T_ROUTE_K_ID, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID + " AS " + RouteTripColumns.T_ROUTE_K_ID);
		map.put(RouteTripColumns.T_ROUTE_K_SHORT_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteTripColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteTripColumns.T_ROUTE_K_LONG_NAME, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_LONG_NAME + " AS "
				+ RouteTripColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteTripColumns.T_ROUTE_K_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_COLOR + " AS " + RouteTripColumns.T_ROUTE_K_COLOR);
		map.put(RouteTripColumns.T_ROUTE_K_TEXT_COLOR, StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteTripColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_TRIP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(TripStopColumns.T_STOP_K_ID, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + " AS " + TripStopColumns.T_STOP_K_ID);
		map.put(TripStopColumns.T_STOP_K_CODE, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_CODE + " AS " + TripStopColumns.T_STOP_K_CODE);
		map.put(TripStopColumns.T_STOP_K_NAME, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_NAME + " AS " + TripStopColumns.T_STOP_K_NAME);
		map.put(TripStopColumns.T_STOP_K_LAT, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LAT + " AS " + TripStopColumns.T_STOP_K_LAT);
		map.put(TripStopColumns.T_STOP_K_LNG, StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_LNG + " AS " + TripStopColumns.T_STOP_K_LNG);
		map.put(TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE + " AS "
				+ TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		map.put(TripStopColumns.T_TRIP_K_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + " AS " + TripStopColumns.T_TRIP_K_ID);
		map.put(TripStopColumns.T_TRIP_K_HEADSIGN_TYPE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ TripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(TripStopColumns.T_TRIP_K_HEADSIGN_VALUE, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ TripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(TripStopColumns.T_TRIP_K_ROUTE_ID, StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ROUTE_ID + " AS " + TripStopColumns.T_TRIP_K_ROUTE_ID);
		TRIP_STOP_PROJECTION_MAP = map;

	}

	@SuppressWarnings("unused")
	private static final String TRIP_STOPS_STOP_JOIN = StmBusDbHelper.T_TRIP_STOPS + SqlUtils.INNER_JOIN + StmBusDbHelper.T_STOP + " ON "
			+ StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_STOP_ID + "=" + StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID;

	private static final String ROUTE_TRIP_TRIP_STOPS_STOP_JOIN = StmBusDbHelper.T_STOP + SqlUtils.INNER_JOIN + StmBusDbHelper.T_TRIP_STOPS + " ON "
			+ StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + "=" + StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_STOP_ID
			+ SqlUtils.INNER_JOIN + StmBusDbHelper.T_TRIP + " ON " + StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_TRIP_ID + "="
			+ StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID + SqlUtils.INNER_JOIN + StmBusDbHelper.T_ROUTE + " ON " + StmBusDbHelper.T_TRIP + "."
			+ StmBusDbHelper.T_TRIP_K_ROUTE_ID + "=" + StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID;

	private static final String TRIP_TRIP_STOPS_STOP_JOIN = StmBusDbHelper.T_STOP + SqlUtils.INNER_JOIN + StmBusDbHelper.T_TRIP_STOPS + " ON "
			+ StmBusDbHelper.T_STOP + "." + StmBusDbHelper.T_STOP_K_ID + "=" + StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_STOP_ID
			+ SqlUtils.INNER_JOIN + StmBusDbHelper.T_TRIP + " ON " + StmBusDbHelper.T_TRIP_STOPS + "." + StmBusDbHelper.T_TRIP_STOPS_K_TRIP_ID + "="
			+ StmBusDbHelper.T_TRIP + "." + StmBusDbHelper.T_TRIP_K_ID;

	private static final String ROUTE_TRIP_JOIN = StmBusDbHelper.T_TRIP + SqlUtils.INNER_JOIN + StmBusDbHelper.T_ROUTE + " ON " + StmBusDbHelper.T_TRIP + "."
			+ StmBusDbHelper.T_TRIP_K_ROUTE_ID + "=" + StmBusDbHelper.T_ROUTE + "." + StmBusDbHelper.T_ROUTE_K_ID;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	/**
	 * The SQLite open helper object.
	 */
	private static StmBusDbHelper mOpenHelper;
	/**
	 * Stores the current database version.
	 */
	private static int currentDbVersion = -1;

	/**
	 * @return the database helper
	 */
	private static StmBusDbHelper getDBHelper(Context context) {
		if (mOpenHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			mOpenHelper = new StmBusDbHelper(context.getApplicationContext());
			currentDbVersion = StmBusDbHelper.DB_VERSION;
		} else { // reset
			try {
				if (currentDbVersion != StmBusDbHelper.DB_VERSION) {
					MyLog.d(TAG, "Update DB...");
					mOpenHelper.close();
					mOpenHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) {
				// fail if locked, will try again later
				MyLog.d(TAG, e, "Can't check DB version!");
			}
		}
		return mOpenHelper;
	}

	private static final String SEARCH_SPLIT_ON = "[\\s\\W]";

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		// MyLog.i(TAG, "[%s]", uri);
		String limit = null;
		switch (URI_MATCHER.match(uri)) {
		case DB_VERSION:
			MyLog.v(TAG, "query>DB_VERSION");
			return getDbVersion();
		case DB_LABEL:
			MyLog.v(TAG, "query>DB_LABEL");
			return getDbLabel();
		case DB_DEPLOYED:
			MyLog.v(TAG, "query>DB_DEPLOYED");
			return isDbDeployed();
		case DB_SETUP_REQUIRED:
			MyLog.v(TAG, "query>DB_SETUP_REQUIRED");
			return isDbSetupRequired();
		case ROUTES:
			MyLog.v(TAG, "query>ROUTES");
			qb.setTables(AbstractDbHelper.T_ROUTE);
			qb.setProjectionMap(ROUTE_PROJECTION_MAP);
			break;
		case TRIPS:
			MyLog.v(TAG, "query>TRIPS");
			qb.setTables(AbstractDbHelper.T_TRIP);
			qb.setProjectionMap(TRIP_PROJECTION_MAP);
			break;
		case STOPS:
			MyLog.v(TAG, "query>STOPS");
			qb.setTables(AbstractDbHelper.T_STOP);
			qb.setProjectionMap(STOP_PROJECTION_MAP);
			break;
		case ROUTES_TRIPS_STOPS:
			MyLog.v(TAG, "query>ROUTES_TRIPS_STOPS");
			qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
			qb.setProjectionMap(ROUTE_TRIP_STOP_PROJECTION_MAP);
			break;
		case ROUTES_TRIPS_STOPS_SEARCH:
			MyLog.v(TAG, "query>ROUTES_TRIPS_STOPS_SEARCH");
			qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
			qb.setProjectionMap(ROUTE_TRIP_STOP_PROJECTION_MAP);
			appendRouteTripStopSearch(uri, qb);
			break;
		case ROUTES_TRIPS:
			MyLog.v(TAG, "query>ROUTES_TRIPS");
			qb.setTables(ROUTE_TRIP_JOIN);
			qb.setProjectionMap(ROUTE_TRIP_PROJECTION_MAP);
			break;
		case TRIPS_STOPS:
			MyLog.v(TAG, "query>TRIPS_STOPS");
			qb.setTables(TRIP_TRIP_STOPS_STOP_JOIN);
			qb.setProjectionMap(TRIP_STOP_PROJECTION_MAP);
			break;
		case SEARCH_NO_KEYWORD:
			MyLog.v(TAG, "query>SEARCH_NO_KEYWORD");
			// TODO store & show most recent
			// TODO show more than just bus stops
			qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
			qb.setProjectionMap(SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP);
			limit = "7";
			break;
		case SEARCH_WITH_KEYWORD:
			MyLog.v(TAG, "query>SEARCH_WITH_KEYWORD");
			// TODO show more than just bus stops
			qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
			qb.setProjectionMap(SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP);
			appendRouteTripStopSearch(uri, qb);
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
		// If no sort order is specified use the default
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case ROUTES:
				sortOrder = ROUTE_SORT_ORDER;
				break;
			case TRIPS:
				sortOrder = TRIP_SORT_ORDER;
				break;
			case STOPS:
				sortOrder = STOP_SORT_ORDER;
				break;
			case ROUTES_TRIPS_STOPS:
			case ROUTES_TRIPS_STOPS_SEARCH:
				sortOrder = ROUTE_TRIP_STOP_SORT_ORDER;
				break;
			case TRIPS_STOPS:
				sortOrder = TRIP_STOP_SORT_ORDER;
				break;
			case ROUTES_TRIPS:
				sortOrder = ROUTE_TRIP_SORT_ORDER;
				break;
			case SEARCH_NO_KEYWORD:
			case SEARCH_WITH_KEYWORD:
			case DB_DEPLOYED:
			case DB_LABEL:
			case DB_VERSION:
			case DB_SETUP_REQUIRED:
				sortOrder = null;
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (order): '%s'", uri));
			}
		}
		// MyLog.d(TAG, "sortOrder: " + sortOrder);
		Cursor cursor = qb.query(getDBHelper(getContext()).getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder, limit);
		if (cursor != null) {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		// closeDbHelper();
		// MyLog.d(TAG, "query(%s, %s, %s, %s, %s) DONE", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		return cursor;
	}

	private Cursor getDbVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
		matrixCursor.addRow(new Object[] { StmBusDbHelper.DB_VERSION });
		return matrixCursor;
	}

	private Cursor getDbLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "label" });
		matrixCursor.addRow(new Object[] { getContext().getString(StmBusDbHelper.LABEL) });
		return matrixCursor;
	}

	private Cursor isDbDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "deployed" });
		matrixCursor.addRow(new Object[] { StmBusDbHelper.isDbExist(getContext(), StmBusDbHelper.DB_NAME) ? 1 : 0 });
		return matrixCursor;
	}

	private Cursor isDbSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "setuprequired" });
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != StmBusDbHelper.DB_VERSION) {
			// live update required => update
			setupRequired = true;
		} else if (!StmBusDbHelper.isDbExist(getContext(), StmBusDbHelper.DB_NAME)) {
			// not deployed => initialization
			setupRequired = true;
		} else if (StmBusDbHelper.getCurrentDbVersion(getContext(), StmBusDbHelper.DB_NAME) != StmBusDbHelper.DB_VERSION) {
			// update required => update
			setupRequired = true;
		}
		matrixCursor.addRow(new Object[] { setupRequired ? 1 : 0 });
		return matrixCursor;
	}

	private void appendRouteTripStopSearch(Uri uri, SQLiteQueryBuilder qb) {
		String search = uri.getLastPathSegment().toLowerCase();
		if (!TextUtils.isEmpty(search)) {
			String[] keywords = search.split(SEARCH_SPLIT_ON);
			StringBuilder inWhere = new StringBuilder();
			for (String keyword : keywords) {
				if (inWhere.length() > 0) {
					inWhere.append(" AND ");
				}
				inWhere.append("(");
				if (TextUtils.isDigitsOnly(keyword)) {
					// TODO setting for this ?
					// IF the keyword start with 5 or 6 OR the keyword length is more than 3 DO
					if (keyword.startsWith("5") || keyword.startsWith("6") || keyword.length() > 3) {
						// BUS STOP CODE
						inWhere.append(StmBusDbHelper.T_STOP).append(".").append(StmBusDbHelper.T_STOP_K_CODE).append(" LIKE '%").append(keyword).append("%'");
						inWhere.append(" OR ");
					}
					// BUS STOP LINE NUMBER
					inWhere.append(StmBusDbHelper.T_ROUTE).append(".").append(StmBusDbHelper.T_ROUTE_K_SHORT_NAME).append(" LIKE '%").append(keyword)
							.append("%'");
					inWhere.append(" OR ");
				} else {
					// BUS STOP LINE NAME
					inWhere.append(StmBusDbHelper.T_ROUTE).append(".").append(StmBusDbHelper.T_ROUTE_K_LONG_NAME).append(" LIKE '%").append(keyword)
							.append("%'");
					inWhere.append(" OR ");
				}
				// BUS STOP PLACE
				inWhere.append(StmBusDbHelper.T_STOP).append(".").append(StmBusDbHelper.T_STOP_K_NAME).append(" LIKE '%").append(keyword).append("%'");
				inWhere.append(")");
			}
			qb.appendWhere(inWhere);
		}
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case ROUTES:
			return ROUTE_CONTENT_TYPE;
		case TRIPS:
			return TRIP_CONTENT_TYPE;
		case STOPS:
			return STOP_CONTENT_TYPE;
		case ROUTES_TRIPS_STOPS:
		case ROUTES_TRIPS_STOPS_SEARCH:
			return ROUTE_TRIP_STOP_CONTENT_TYPE;
		case ROUTES_TRIPS:
			return ROUTE_TRIP_CONTENT_TYPE;
		case TRIPS_STOPS:
			return TRIP_STOP_CONTENT_TYPE;
		case SEARCH_NO_KEYWORD:
		case SEARCH_WITH_KEYWORD:
			return SearchManager.SUGGEST_MIME_TYPE;
		case DB_DEPLOYED:
		case DB_LABEL:
		case DB_VERSION:
		case DB_SETUP_REQUIRED:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
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
}

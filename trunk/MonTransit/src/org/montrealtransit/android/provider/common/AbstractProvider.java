package org.montrealtransit.android.provider.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.MyLog;

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

public abstract class AbstractProvider extends ContentProvider {

	public static final String TAG = AbstractProvider.class.getSimpleName();

	public static final String ROUTE_SORT_ORDER = AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " ASC";
	public static final String TRIP_SORT_ORDER = AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " ASC";
	public static final String TRIP_STOPS_SORT_ORDER = AbstractDbHelper.T_TRIP_STOPS + "." + AbstractDbHelper.T_TRIP_STOPS_K_ID + " ASC";
	public static final String STOP_SORT_ORDER = AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " ASC";
	public static final String ROUTE_TRIP_STOP_SORT_ORDER = ROUTE_SORT_ORDER + ", " + TRIP_SORT_ORDER + ", " + STOP_SORT_ORDER;
	public static final String ROUTE_TRIP_SORT_ORDER = ROUTE_SORT_ORDER + ", " + TRIP_SORT_ORDER;
	public static final String TRIP_STOP_SORT_ORDER = TRIP_SORT_ORDER + ", " + STOP_SORT_ORDER;

	protected static final int ROUTES = 1;
	protected static final int STOPS = 2;
	protected static final int TRIPS = 3;
	protected static final int ROUTES_TRIPS_STOPS = 4;
	protected static final int ROUTES_TRIPS_STOPS_SEARCH = 5;
	protected static final int ROUTES_TRIPS = 6;
	protected static final int TRIPS_STOPS = 7;
	protected static final int SEARCH_NO_KEYWORD = 8;
	protected static final int SEARCH_WITH_KEYWORD = 9;
	protected static final int DB_VERSION = 100;
	protected static final int DB_DEPLOYED = 101;
	protected static final int DB_LABEL = 102;
	protected static final int DB_SETUP_REQUIRED = 103;

	private static final Map<String, String> ROUTE_PROJECTION_MAP;
	private static final Map<String, String> TRIP_PROJECTION_MAP;
	private static final Map<String, String> STOP_PROJECTION_MAP;
	private static final Map<String, String> ROUTE_TRIP_STOP_PROJECTION_MAP;
	private static final Map<String, String> ROUTE_TRIP_PROJECTION_MAP;
	private static final Map<String, String> TRIP_STOP_PROJECTION_MAP;
	private static final Map<String, String> SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP;

	public static final String GLOBAL_AUTHORITY = "org.montrealtransit.android";

	public static final String ROUTE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".route";
	public static final String ROUTE_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".route";
	public static final String TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".trip";
	public static final String TRIP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".trip";
	public static final String STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".stop";
	public static final String STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".stop";
	public static final String ROUTE_TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".routetripstop";
	public static final String ROUTE_TRIP_STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".routetripstop";
	public static final String TRIP_STOP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".tripstop";
	public static final String TRIP_STOP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".tripstop";
	public static final String ROUTE_TRIP_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".routetrip";
	public static final String ROUTE_TRIP_CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".routetrip";

	private static final String UID_SEPARATOR = "-";
	static {

		HashMap<String, String> map;

		map = new HashMap<String, String>();
		map.put(RouteColumns.T_ROUTE_K_ID, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " AS " + RouteColumns.T_ROUTE_K_ID);
		map.put(RouteColumns.T_ROUTE_K_SHORT_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteColumns.T_ROUTE_K_LONG_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_LONG_NAME + " AS "
				+ RouteColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteColumns.T_ROUTE_K_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_COLOR + " AS " + RouteColumns.T_ROUTE_K_COLOR);
		map.put(RouteColumns.T_ROUTE_K_TEXT_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(TripColumns.T_TRIP_K_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " AS " + TripColumns.T_TRIP_K_ID);
		map.put(TripColumns.T_TRIP_K_HEADSIGN_TYPE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ TripColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(TripColumns.T_TRIP_K_HEADSIGN_VALUE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ TripColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(TripColumns.T_TRIP_K_ROUTE_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + " AS " + TripColumns.T_TRIP_K_ROUTE_ID);
		TRIP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(StopColumns.T_STOP_K_ID, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " AS " + StopColumns.T_STOP_K_ID);
		map.put(StopColumns.T_STOP_K_CODE, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_CODE + " AS " + StopColumns.T_STOP_K_CODE);
		map.put(StopColumns.T_STOP_K_NAME, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_NAME + " AS " + StopColumns.T_STOP_K_NAME);
		map.put(StopColumns.T_STOP_K_LAT, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LAT + " AS " + StopColumns.T_STOP_K_LAT);
		map.put(StopColumns.T_STOP_K_LNG, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LNG + " AS " + StopColumns.T_STOP_K_LNG);
		STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(RouteTripStopColumns.T_STOP_K_ID, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " AS " + RouteTripStopColumns.T_STOP_K_ID);
		map.put(RouteTripStopColumns.T_STOP_K_CODE, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_CODE + " AS "
				+ RouteTripStopColumns.T_STOP_K_CODE);
		map.put(RouteTripStopColumns.T_STOP_K_NAME, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_NAME + " AS "
				+ RouteTripStopColumns.T_STOP_K_NAME);
		map.put(RouteTripStopColumns.T_STOP_K_LAT, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LAT + " AS " + RouteTripStopColumns.T_STOP_K_LAT);
		map.put(RouteTripStopColumns.T_STOP_K_LNG, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LNG + " AS " + RouteTripStopColumns.T_STOP_K_LNG);
		map.put(RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, AbstractDbHelper.T_TRIP_STOPS + "." + AbstractDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE + " AS "
				+ RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		map.put(RouteTripStopColumns.T_TRIP_K_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " AS " + RouteTripStopColumns.T_TRIP_K_ID);
		map.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(RouteTripStopColumns.T_TRIP_K_ROUTE_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + " AS "
				+ RouteTripStopColumns.T_TRIP_K_ROUTE_ID);
		map.put(RouteTripStopColumns.T_ROUTE_K_ID, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " AS " + RouteTripStopColumns.T_ROUTE_K_ID);
		map.put(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteTripStopColumns.T_ROUTE_K_LONG_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_LONG_NAME + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteTripStopColumns.T_ROUTE_K_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_COLOR + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_COLOR);
		map.put(RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_TRIP_STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		// TODO use real UID (needs trip ID)
		// map.put(BaseColumns._ID, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + "||'" + UID_SEPARATOR + "'||" + AbstractDbHelper.T_TRIP +
		// "."
		// + AbstractDbHelper.T_TRIP_K_ID + "||'" + UID_SEPARATOR + "'||" + AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " AS "
		// + BaseColumns._ID);
		// map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + "||'" + UID_SEPARATOR + "'||"
		// + AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + "||'" + UID_SEPARATOR + "'||" + AbstractDbHelper.T_STOP + "."
		// + AbstractDbHelper.T_STOP_K_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(BaseColumns._ID, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + "||'" + UID_SEPARATOR + "'||" + AbstractDbHelper.T_ROUTE + "."
				+ AbstractDbHelper.T_ROUTE_K_ID + " AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + "||'" + UID_SEPARATOR + "'||"
				+ AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_NAME + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_CODE + "||' '||" + AbstractDbHelper.T_ROUTE
				+ "." + AbstractDbHelper.T_ROUTE_K_SHORT_NAME + "||' '||" + AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(RouteTripColumns.T_TRIP_K_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " AS " + RouteTripColumns.T_TRIP_K_ID);
		map.put(RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(RouteTripColumns.T_TRIP_K_ROUTE_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + " AS "
				+ RouteTripColumns.T_TRIP_K_ROUTE_ID);
		map.put(RouteTripColumns.T_ROUTE_K_ID, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID + " AS " + RouteTripColumns.T_ROUTE_K_ID);
		map.put(RouteTripColumns.T_ROUTE_K_SHORT_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_SHORT_NAME + " AS "
				+ RouteTripColumns.T_ROUTE_K_SHORT_NAME);
		map.put(RouteTripColumns.T_ROUTE_K_LONG_NAME, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_LONG_NAME + " AS "
				+ RouteTripColumns.T_ROUTE_K_LONG_NAME);
		map.put(RouteTripColumns.T_ROUTE_K_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_COLOR + " AS " + RouteTripColumns.T_ROUTE_K_COLOR);
		map.put(RouteTripColumns.T_ROUTE_K_TEXT_COLOR, AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_TEXT_COLOR + " AS "
				+ RouteTripColumns.T_ROUTE_K_TEXT_COLOR);
		ROUTE_TRIP_PROJECTION_MAP = map;

		map = new HashMap<String, String>();
		map.put(TripStopColumns.T_STOP_K_ID, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + " AS " + TripStopColumns.T_STOP_K_ID);
		map.put(TripStopColumns.T_STOP_K_CODE, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_CODE + " AS " + TripStopColumns.T_STOP_K_CODE);
		map.put(TripStopColumns.T_STOP_K_NAME, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_NAME + " AS " + TripStopColumns.T_STOP_K_NAME);
		map.put(TripStopColumns.T_STOP_K_LAT, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LAT + " AS " + TripStopColumns.T_STOP_K_LAT);
		map.put(TripStopColumns.T_STOP_K_LNG, AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_LNG + " AS " + TripStopColumns.T_STOP_K_LNG);
		map.put(TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, AbstractDbHelper.T_TRIP_STOPS + "." + AbstractDbHelper.T_TRIP_STOPS_K_STOP_SEQUENCE + " AS "
				+ TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE);
		map.put(TripStopColumns.T_TRIP_K_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + " AS " + TripStopColumns.T_TRIP_K_ID);
		map.put(TripStopColumns.T_TRIP_K_HEADSIGN_TYPE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_TYPE + " AS "
				+ TripStopColumns.T_TRIP_K_HEADSIGN_TYPE);
		map.put(TripStopColumns.T_TRIP_K_HEADSIGN_VALUE, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_HEADSIGN_VALUE + " AS "
				+ TripStopColumns.T_TRIP_K_HEADSIGN_VALUE);
		map.put(TripStopColumns.T_TRIP_K_ROUTE_ID, AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + " AS "
				+ TripStopColumns.T_TRIP_K_ROUTE_ID);
		TRIP_STOP_PROJECTION_MAP = map;

	}

	@SuppressWarnings("unused")
	private static final String TRIP_STOPS_STOP_JOIN = AbstractDbHelper.T_TRIP_STOPS + SqlUtils.INNER_JOIN + AbstractDbHelper.T_STOP + " ON "
			+ AbstractDbHelper.T_TRIP_STOPS + "." + AbstractDbHelper.T_TRIP_STOPS_K_STOP_ID + "=" + AbstractDbHelper.T_STOP + "."
			+ AbstractDbHelper.T_STOP_K_ID;

	private static final String ROUTE_TRIP_TRIP_STOPS_STOP_JOIN = AbstractDbHelper.T_STOP + SqlUtils.INNER_JOIN + AbstractDbHelper.T_TRIP_STOPS + " ON "
			+ AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + "=" + AbstractDbHelper.T_TRIP_STOPS + "."
			+ AbstractDbHelper.T_TRIP_STOPS_K_STOP_ID + SqlUtils.INNER_JOIN + AbstractDbHelper.T_TRIP + " ON " + AbstractDbHelper.T_TRIP_STOPS + "."
			+ AbstractDbHelper.T_TRIP_STOPS_K_TRIP_ID + "=" + AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID + SqlUtils.INNER_JOIN
			+ AbstractDbHelper.T_ROUTE + " ON " + AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + "=" + AbstractDbHelper.T_ROUTE + "."
			+ AbstractDbHelper.T_ROUTE_K_ID;

	private static final String TRIP_TRIP_STOPS_STOP_JOIN = AbstractDbHelper.T_STOP + SqlUtils.INNER_JOIN + AbstractDbHelper.T_TRIP_STOPS + " ON "
			+ AbstractDbHelper.T_STOP + "." + AbstractDbHelper.T_STOP_K_ID + "=" + AbstractDbHelper.T_TRIP_STOPS + "."
			+ AbstractDbHelper.T_TRIP_STOPS_K_STOP_ID + SqlUtils.INNER_JOIN + AbstractDbHelper.T_TRIP + " ON " + AbstractDbHelper.T_TRIP_STOPS + "."
			+ AbstractDbHelper.T_TRIP_STOPS_K_TRIP_ID + "=" + AbstractDbHelper.T_TRIP + "." + AbstractDbHelper.T_TRIP_K_ID;

	private static final String ROUTE_TRIP_JOIN = AbstractDbHelper.T_TRIP + SqlUtils.INNER_JOIN + AbstractDbHelper.T_ROUTE + " ON " + AbstractDbHelper.T_TRIP
			+ "." + AbstractDbHelper.T_TRIP_K_ROUTE_ID + "=" + AbstractDbHelper.T_ROUTE + "." + AbstractDbHelper.T_ROUTE_K_ID;

	/**
	 * The SQLite open helper object.
	 */
	private static AbstractDbHelper dbHelper;
	/**
	 * Stores the current database version.
	 */
	private static int currentDbVersion = -1;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	/**
	 * @return the database helper
	 */
	private AbstractDbHelper getDBHelper(Context context) {
		if (dbHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					MyLog.d(TAG, "Update DB...");
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				// fail if locked, will try again later
				MyLog.d(TAG, t, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			// MyLog.i(TAG, "[%s]", uri);
			String limit = null;
			switch (getURIMATCHER().match(uri)) {
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
				// TODO show more than just stops
				qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
				qb.setProjectionMap(SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP);
				limit = "7";
				break;
			case SEARCH_WITH_KEYWORD:
				MyLog.v(TAG, "query>SEARCH_WITH_KEYWORD");
				// TODO show more than just stops
				qb.setTables(ROUTE_TRIP_TRIP_STOPS_STOP_JOIN);
				qb.setProjectionMap(SEARCH_ROUTE_TRIP_STOP_PROJECTION_MAP);
				appendRouteTripStopSearch(uri, qb);
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
			}
			// If no sort order is specified use the default
			if (TextUtils.isEmpty(sortOrder)) {
				switch (getURIMATCHER().match(uri)) {
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
		} catch (Throwable t) {
			MyLog.w(TAG, "Error while resolving query %s!", uri);
			return null;
		}
	}

	private Cursor getDbVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
		matrixCursor.addRow(new Object[] { getCurrentDbVersion() });
		return matrixCursor;
	}

	private Cursor getDbLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "label" });
		matrixCursor.addRow(new Object[] { getContext().getString(getLabel()) });
		return matrixCursor;
	}

	private Cursor isDbDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "deployed" });
		matrixCursor.addRow(new Object[] { AbstractDbHelper.isDbExist(getContext(), getDbName()) ? 1 : 0 });
		return matrixCursor;
	}

	private Cursor isDbSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "setuprequired" });
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			// live update required => update
			setupRequired = true;
		} else if (!AbstractDbHelper.isDbExist(getContext(), getDbName())) {
			// not deployed => initialization
			setupRequired = true;
		} else if (AbstractDbHelper.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			// update required => update
			setupRequired = true;
		}
		matrixCursor.addRow(new Object[] { setupRequired ? 1 : 0 });
		return matrixCursor;
	}

	private static final String SEARCH_SPLIT_ON = "[\\s\\W]";

	private void appendRouteTripStopSearch(Uri uri, SQLiteQueryBuilder qb) {
		String search = uri.getLastPathSegment().toLowerCase(Locale.ENGLISH);
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
						// STOP CODE
						inWhere.append(AbstractDbHelper.T_STOP).append(".").append(AbstractDbHelper.T_STOP_K_CODE).append(" LIKE '%").append(keyword)
								.append("%'");
						inWhere.append(" OR ");
					}
					// STOP ROUTE SHORT NAME
					inWhere.append(AbstractDbHelper.T_ROUTE).append(".").append(AbstractDbHelper.T_ROUTE_K_SHORT_NAME).append(" LIKE '%").append(keyword)
							.append("%'");
					inWhere.append(" OR ");
				} else {
					// STOP ROUTE LONG NAME
					inWhere.append(AbstractDbHelper.T_ROUTE).append(".").append(AbstractDbHelper.T_ROUTE_K_LONG_NAME).append(" LIKE '%").append(keyword)
							.append("%'");
					inWhere.append(" OR ");
				}
				// STOP NAME
				inWhere.append(AbstractDbHelper.T_STOP).append(".").append(AbstractDbHelper.T_STOP_K_NAME).append(" LIKE '%").append(keyword).append("%'");
				inWhere.append(")");
			}
			qb.appendWhere(inWhere);
		}
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (getURIMATCHER().match(uri)) {
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

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, "version", DB_VERSION);
		URI_MATCHER.addURI(authority, "deployed", DB_DEPLOYED);
		URI_MATCHER.addURI(authority, "label", DB_LABEL);
		URI_MATCHER.addURI(authority, "setuprequired", DB_SETUP_REQUIRED);
		URI_MATCHER.addURI(authority, "route", ROUTES);
		URI_MATCHER.addURI(authority, "trip", TRIPS);
		URI_MATCHER.addURI(authority, "stop", STOPS);
		URI_MATCHER.addURI(authority, "route/trip/stop", ROUTES_TRIPS_STOPS);
		URI_MATCHER.addURI(authority, "route/trip/stop/*", ROUTES_TRIPS_STOPS_SEARCH);
		URI_MATCHER.addURI(authority, "route/trip", ROUTES_TRIPS);
		URI_MATCHER.addURI(authority, "trip/stop", TRIPS_STOPS);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_NO_KEYWORD);
		URI_MATCHER.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_WITH_KEYWORD);
		return URI_MATCHER;
	}

	public abstract UriMatcher getURIMATCHER();

	public abstract String getAUTHORITY();

	public abstract int getCurrentDbVersion();

	public abstract int getLabel();

	public abstract String getDbName();

	public abstract AbstractDbHelper getNewDbHelper(Context context);

}

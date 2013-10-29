package org.montrealtransit.android.provider;

import java.util.Arrays;
import java.util.HashMap;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * This data provider contains static informations about bus stops, bus lines, subway lines, subway stations.
 * @author Mathieu Méa
 */
public class StmProvider extends ContentProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmProvider.class.getSimpleName();

	/**
	 * The content provider authority as described in the AndroidManifest.
	 */
	public static final String AUTHORITY = Constant.PKG + ".stm";

	/**
	 * The content URIs matcher IDs.
	 */
	private static final int SUBWAY_LINES = 5;
	private static final int SUBWAY_LINE_ID = 6;
	private static final int SUBWAY_STATIONS = 7;
	private static final int SUBWAY_STATION_ID = 8;
	private static final int SUBWAY_LINE_ID_STATIONS = 9;
	private static final int SUBWAY_STATION_ID_LINES = 25;
	private static final int SUBWAY_STATIONS_IDS = 24;
	private static final int SUBWAY_LINE_ID_STATIONS_SEARCH = 26;
	private static final int SUBWAY_LINES_SEARCH = 29;
	private static final int SUBWAY_STATION_ID_LINES_OTHER = 32;
	private static final int SUBWAY_STATION_ID_DIRECTION_ID_DAY = 34;
	private static final int SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY = 35;
	private static final int SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID = 36;
	private static final int SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID = 37;
	private static final int SUBWAY_STATIONS_AND_LINES = 38;
	private static final int SUBWAY_STATIONS_LOC_LAT_LNG = 43;
	private static final int DB_VERSION = 100;
	private static final int DB_DEPLOYED = 101;
	private static final int DB_LABEL = 102;
	private static final int DB_SETUP_REQUIRED = 103;

	/**
	 * Projection for subway station.
	 */
	private static final HashMap<String, String> sSubwayStationsProjectionMap;
	/**
	 * Projection for subway line.
	 */
	private static final HashMap<String, String> sSubwayLinesProjectionMap;
	/**
	 * Projection for subway lines and stations.
	 */
	private static final HashMap<String, String> sSubwayLinesStationsProjectionMap;
	/**
	 * Projection for the first and last hour of a subway station.
	 */
	private static final HashMap<String, String> sSubwayStationHourProjectionMap;

	/**
	 * The URI marcher.
	 */
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "version", DB_VERSION);
		URI_MATCHER.addURI(AUTHORITY, "deployed", DB_DEPLOYED);
		URI_MATCHER.addURI(AUTHORITY, "label", DB_LABEL);
		URI_MATCHER.addURI(AUTHORITY, "setuprequired", DB_SETUP_REQUIRED);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines", SUBWAY_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/search/*", SUBWAY_LINES_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#", SUBWAY_LINE_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#/subwaystations", SUBWAY_LINE_ID_STATIONS);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#/subwaystations/search/*", SUBWAY_LINE_ID_STATIONS_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations", SUBWAY_STATIONS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/subwaylines", SUBWAY_STATIONS_AND_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#", SUBWAY_STATION_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/directions/#", SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/directions/#/*", SUBWAY_STATION_ID_DIRECTION_ID_DAY);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/subwaylines", SUBWAY_STATION_ID_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/subwaylines/other", SUBWAY_STATION_ID_LINES_OTHER);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/*", SUBWAY_STATIONS_IDS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystationsloc/*", SUBWAY_STATIONS_LOC_LAT_LNG);
		URI_MATCHER.addURI(AUTHORITY, "subwaydirections/#/days/*/hours/*", SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaydirections/#/hours/*", SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS " + BaseColumns._ID);
		map.put(StmStore.SubwayStation.STATION_ID, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS "
				+ StmStore.SubwayStation.STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS "
				+ StmStore.SubwayStation.STATION_NAME);
		map.put(StmStore.SubwayStation.STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT + " AS "
				+ StmStore.SubwayStation.STATION_LAT);
		map.put(StmStore.SubwayStation.STATION_LNG, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG + " AS "
				+ StmStore.SubwayStation.STATION_LNG);
		sSubwayStationsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " AS " + BaseColumns._ID);
		map.put(StmStore.SubwayLine.LINE_NUMBER, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " AS "
				+ StmStore.SubwayLine.LINE_NUMBER);
		map.put(StmStore.SubwayLine.LINE_NAME, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME + " AS " + StmStore.SubwayLine.LINE_NAME);
		sSubwayLinesProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "||'-'||" + StmDbHelper.T_SUBWAY_STATIONS + "."
				+ StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS " + BaseColumns._ID);
		map.put(StmStore.SubwayLine.LINE_NUMBER, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " AS "
				+ StmStore.SubwayLine.LINE_NUMBER);
		map.put(StmStore.SubwayLine.LINE_NAME, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME + " AS " + StmStore.SubwayLine.LINE_NAME);
		map.put(StmStore.SubwayStation.STATION_ID, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS "
				+ StmStore.SubwayStation.STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS "
				+ StmStore.SubwayStation.STATION_NAME);
		map.put(StmStore.SubwayStation.STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT + " AS "
				+ StmStore.SubwayStation.STATION_LAT);
		map.put(StmStore.SubwayStation.STATION_LNG, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG + " AS "
				+ StmStore.SubwayStation.STATION_LNG);
		sSubwayLinesStationsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + " AS " + BaseColumns._ID);
		map.put(StmStore.HOUR, "strftime('%Hh%M'," + StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_HOUR + ") AS " + StmStore.HOUR);
		map.put(StmStore.FIRST_LAST, StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_FIRST_LAST + " AS " + StmStore.FIRST_LAST);
		sSubwayStationHourProjectionMap = map;
	}

	private static final String SUBWAY_LINE_STATIONS_JOIN = StmDbHelper.T_SUBWAY_LINES + " LEFT OUTER JOIN " + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + " ON "
			+ StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
			+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID + " LEFT OUTER JOIN " + StmDbHelper.T_SUBWAY_STATIONS + " ON "
			+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID + "=" + StmDbHelper.T_SUBWAY_STATIONS
			+ "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;

	private static final String SUBWAY_STATIONS_LINE_JOIN = StmDbHelper.T_SUBWAY_LINES + " JOIN " + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + " ON "
			+ StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
			+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID;

	private static final String SUBWAY_STATIONS_LINE_DIRECTION_JOIN = StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + " JOIN " + StmDbHelper.T_SUBWAY_LINES + " ON "
			+ StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
			+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID + " JOIN " + StmDbHelper.T_SUBWAY_STATIONS + " ON " + StmDbHelper.T_SUBWAY_STATIONS + "."
			+ StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + "=" + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
			+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	/**
	 * The SQLite open helper object.
	 */
	private static StmDbHelper mOpenHelper;
	/**
	 * Stores the current database version.
	 */
	private static int currentDbVersion = 0;

	/**
	 * @return the database helper
	 */
	private static StmDbHelper getDBHelper(Context context) {
		if (mOpenHelper == null) { // initialize
			MyLog.d(TAG, "Initialize DB...");
			mOpenHelper = new StmDbHelper(context.getApplicationContext());
			currentDbVersion = StmDbHelper.DB_VERSION;
		} else { // reset
			try {
				if (currentDbVersion != StmDbHelper.DB_VERSION) {
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
		MyLog.i(TAG, "[%s]", uri);
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
		case SUBWAY_LINES:
			MyLog.v(TAG, "query>SUBWAY_LINES");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			qb.setProjectionMap(sSubwayLinesProjectionMap);
			break;
		case SUBWAY_LINES_SEARCH:
			MyLog.v(TAG, "query>SUBWAY_LINES_SEARCH");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			String search = Uri.decode(uri.getPathSegments().get(2));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
				String inWhere = "";
				for (String keyword : keywords) {
					if (inWhere.length() > 0) {
						inWhere += " AND ";
					}
					inWhere += "(" + StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " LIKE '%" + keyword + "%'";
					inWhere += " OR ";
					inWhere += StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME + " LIKE '%" + keyword + "%')";
				}
				qb.appendWhere(inWhere);
			}
			break;
		case SUBWAY_STATION_ID_LINES:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_LINES");
			qb.setTables(SUBWAY_STATIONS_LINE_JOIN);
			qb.setProjectionMap(sSubwayLinesProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID + "="
					+ uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATIONS_AND_LINES:
			MyLog.v(TAG, "query>SUBWAY_STATIONS_AND_LINES");
			qb.setTables(SUBWAY_STATIONS_LINE_DIRECTION_JOIN);
			qb.setProjectionMap(sSubwayLinesStationsProjectionMap);
			break;
		case SUBWAY_STATION_ID_LINES_OTHER:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_LINES_OTHER");
			qb.setTables(SUBWAY_STATIONS_LINE_DIRECTION_JOIN);
			qb.setProjectionMap(sSubwayLinesStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID + "!="
					+ uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
					+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID + " IN (" + "SELECT " + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
					+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID + " FROM " + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + " WHERE "
					+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID + "=" + uri.getPathSegments().get(1)
					+ ")");
			break;
		case SUBWAY_LINE_ID:
			MyLog.v(TAG, "query>SUBWAY_LINE_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			qb.setProjectionMap(sSubwayLinesProjectionMap);
			String subwayLineId = uri.getPathSegments().get(1);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + subwayLineId);
			break;
		case SUBWAY_LINE_ID_STATIONS:
			MyLog.v(TAG, "query>SUBWAY_LINE_ID_STATIONS");
			qb.setTables(SUBWAY_LINE_STATIONS_JOIN);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case SUBWAY_LINE_ID_STATIONS_SEARCH:
			MyLog.v(TAG, "query>SUBWAY_LINE_ID_STATIONS_SEARCH");
			qb.setTables(SUBWAY_LINE_STATIONS_JOIN);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + uri.getPathSegments().get(1));
			search = Uri.decode(uri.getPathSegments().get(4));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
				for (String keyword : keywords) {
					qb.appendWhere(" AND ");
					qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " LIKE '%" + keyword + "%'");
				}
			}
			break;
		case SUBWAY_STATIONS:
			MyLog.v(TAG, "query>SUBWAY_STATIONS");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			break;
		case SUBWAY_STATIONS_IDS:
			MyLog.v(TAG, "query>SUBWAY_STATIONS_IDS");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			String[] subwayStationIds = uri.getPathSegments().get(1).split("\\+");
			qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " IN (");
			for (int i = 0; i < subwayStationIds.length; i++) {
				if (i > 0) {
					qb.appendWhere(",");
				}
				qb.appendWhere(subwayStationIds[i]);
			}
			qb.appendWhere(")");
			break;
		case SUBWAY_STATIONS_LOC_LAT_LNG:
			MyLog.v(TAG, "query>SUBWAY_STATIONS_LOC_LAT_LNG");
			qb.setTables(SUBWAY_STATIONS_LINE_DIRECTION_JOIN);
			qb.setProjectionMap(sSubwayLinesStationsProjectionMap);
			String[] latlng = uri.getPathSegments().get(1).split("\\+");
			qb.appendWhere(LocationUtils.genAroundWhere(latlng[0], latlng[1],
					StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "."
							+ StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG));
			break;
		case SUBWAY_STATION_ID:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + "=" + uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY");
			qb.setProjectionMap(sSubwayStationHourProjectionMap);
			qb.setTables(StmDbHelper.T_SUBWAY_HOUR);
			qb.appendWhere(StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_STATION_ID + "=" + uri.getPathSegments().get(1) + " AND "
					+ StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + "=" + uri.getPathSegments().get(3) + " AND "
					+ StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DAY + "=''");
			break;
		case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_DIRECTION_ID_DAY");
			qb.setTables(StmDbHelper.T_SUBWAY_HOUR);
			qb.setProjectionMap(sSubwayStationHourProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_STATION_ID + "=" + uri.getPathSegments().get(1) + " AND "
					+ StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + "=" + uri.getPathSegments().get(3) + " AND "
					+ StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DAY + "='" + uri.getPathSegments().get(4) + "'");
			break;

		case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
			MyLog.v(TAG, "query>SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_FREQUENCES);
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(StmStore.FREQUENCY, StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_FREQUENCE + " AS " + StmStore.FREQUENCY);
			qb.setProjectionMap(map);
			qb.appendWhere(StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DIRECTION + "=" + uri.getPathSegments().get(1) + " AND "
					+ StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY + "='' AND time(" + StmDbHelper.T_SUBWAY_FREQUENCES + "."
					+ StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + ", '-2 hour') <= '" + uri.getPathSegments().get(3) + "'");
			limit = "1";
			sortOrder = StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + " DESC";

			break;
		case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
			MyLog.v(TAG, "query>SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_FREQUENCES);
			HashMap<String, String> map2 = new HashMap<String, String>();
			map2.put(StmStore.FREQUENCY, StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_FREQUENCE + " AS " + StmStore.FREQUENCY);
			qb.setProjectionMap(map2);
			qb.appendWhere(StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DIRECTION + "=" + uri.getPathSegments().get(1) + " AND "
					+ StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY + "='" + uri.getPathSegments().get(3) + "' AND time("
					+ StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + ", '-2 hour') <= '" + uri.getPathSegments().get(5) + "'");
			limit = "1";
			sortOrder = StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + " DESC";
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): %s", uri));
		}
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case SUBWAY_LINES:
			case SUBWAY_LINES_SEARCH:
			case SUBWAY_STATION_ID_LINES:
			case SUBWAY_LINE_ID:
				orderBy = StmStore.SubwayLine.DEFAULT_SORT_ORDER;
				break;
			case SUBWAY_STATIONS:
			case SUBWAY_STATION_ID:
			case SUBWAY_LINE_ID_STATIONS:
			case SUBWAY_LINE_ID_STATIONS_SEARCH:
			case SUBWAY_STATION_ID_LINES_OTHER:
			case SUBWAY_STATIONS_AND_LINES:
			case SUBWAY_STATIONS_LOC_LAT_LNG:
			case SUBWAY_STATIONS_IDS:
				orderBy = StmStore.SubwayStation.DEFAULT_SORT_ORDER;
				break;
			case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
			case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
			case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
			case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
			case DB_DEPLOYED:
			case DB_LABEL:
			case DB_VERSION:
			case DB_SETUP_REQUIRED:
				// case SEARCH:
				orderBy = null;
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (order): %s", uri));
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = getDBHelper(getContext()).getReadableDatabase();
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);
		if (cursor != null) {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	private Cursor getDbVersion() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "version" });
		matrixCursor.addRow(new Object[] { StmDbHelper.DB_VERSION });
		return matrixCursor;
	}

	private Cursor getDbLabel() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "label" });
		matrixCursor.addRow(new Object[] { getContext().getString(R.string.ca_mtl_stm_subway_label) });
		return matrixCursor;
	}

	private Cursor isDbDeployed() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "deployed" });
		matrixCursor.addRow(new Object[] { StmDbHelper.isDbExist(getContext()) ? 1 : 0 });
		return matrixCursor;
	}

	private Cursor isDbSetupRequired() {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "setuprequired" });
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != StmDbHelper.DB_VERSION) {
			// live update required => update
			setupRequired = true;
		} else if (!StmDbHelper.isDbExist(getContext())) {
			// not deployed => initialization
			setupRequired = true;
		} else if (StmDbHelper.getCurrentDbVersion(getContext()) != StmDbHelper.DB_VERSION) {
			// update required => update
			setupRequired = true;
		}
		matrixCursor.addRow(new Object[] { setupRequired ? 1 : 0 });
		return matrixCursor;
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case SUBWAY_STATION_ID_LINES:
		case SUBWAY_LINES:
		case SUBWAY_LINES_SEARCH:
			return StmStore.SubwayLine.CONTENT_TYPE;
		case SUBWAY_LINE_ID:
			return StmStore.SubwayLine.CONTENT_ITEM_TYPE;
		case SUBWAY_STATIONS:
		case SUBWAY_STATIONS_IDS:
		case SUBWAY_LINE_ID_STATIONS:
		case SUBWAY_LINE_ID_STATIONS_SEARCH:
		case SUBWAY_STATIONS_LOC_LAT_LNG:
			return StmStore.SubwayStation.CONTENT_TYPE;
		case SUBWAY_STATION_ID:
			return StmStore.SubwayStation.CONTENT_ITEM_TYPE;
		case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
		case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
		case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
		case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
		case SUBWAY_STATION_ID_LINES_OTHER:
		case SUBWAY_STATIONS_AND_LINES:
		case DB_DEPLOYED:
		case DB_LABEL:
		case DB_VERSION:
		case DB_SETUP_REQUIRED:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): %s", uri));
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

package org.montrealtransit.android.provider;

import java.util.Arrays;
import java.util.HashMap;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
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
	public static final String AUTHORITY = Constant.PKG;

	/**
	 * The content URIs matcher IDs.
	 */
	private static final int BUS_LINES = 1;
	private static final int BUS_LINE_ID = 2;
	private static final int BUS_STOPS = 3;
	private static final int BUS_STOP_ID = 4;
	private static final int SUBWAY_LINES = 5;
	private static final int SUBWAY_LINE_ID = 6;
	private static final int SUBWAY_STATIONS = 7;
	private static final int SUBWAY_STATION_ID = 8;
	private static final int SUBWAY_LINE_ID_STATIONS = 9;
	private static final int SUBWAY_STATION_ID_LINES = 25;
	private static final int BUS_LINE_ID_DIRECTIONS = 11;
	private static final int BUS_LINE_ID_DIRECTION_ID_STOPS = 12;
	private static final int BUS_LINE_ID_STOPS = 14;
	private static final int BUS_LINE_ID_STOP_ID = 15;
	private static final int BUS_LINE_DIRECTIONS = 16;
	private static final int BUS_LINE_DIRECTION_ID = 17;
	private static final int BUS_STOP_ID_BUS_LINES = 18;
	private static final int BUS_LINES_IDS = 19;
	private static final int BUS_STOPS_IDS = 20;
	private static final int BUS_STOPS_LIVE_FOLDER = 21;
	private static final int SUBWAY_STATION_ID_BUS_LINES = 22;
	private static final int SUBWAY_STATION_ID_BUS_STOPS = 23;
	private static final int SUBWAY_STATIONS_IDS = 24;
	private static final int SUBWAY_LINE_ID_STATIONS_SEARCH = 26;
	private static final int BUS_LINES_SEARCH = 27;
	private static final int BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH = 28;
	private static final int SUBWAY_LINES_SEARCH = 29;

	/**
	 * The URI marcher.
	 */
	private static final UriMatcher URI_MATCHER;
	/**
	 * Projection for subway station.
	 */
	private static final HashMap<String, String> sSubwayStationsProjectionMap;
	/**
	 * Projection for subway line.
	 */
	private static final HashMap<String, String> sSubwayLinesProjectionMap;
	/**
	 * Projection for bus line direction.
	 */
	private static final HashMap<String, String> sBusLineDirectionsProjectionMap;
	/**
	 * Projection for bus lines.
	 */
	private static final HashMap<String, String> sBusLinesProjectionMap;
	/**
	 * Projection for the live folder.
	 */
	private static final HashMap<String, String> sBusStopsLiveFolderProjectionMap;
	/**
	 * Projection for bus stops extended (with bus lines info).
	 */
	private static final HashMap<String, String> sBusStopsExtendedProjectionMap;
	/**
	 * Projection for bus stops.
	 */
	private static final HashMap<String, String> sBusStopsProjectionMap;
	/**
	 * Projection for bus stops with subway line name.
	 */
	private static final HashMap<String, String> sBusStopsWithSubwayStationProjectionMap;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "buslines", BUS_LINES);
		URI_MATCHER.addURI(AUTHORITY, "buslines/search/*", BUS_LINES_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#", BUS_LINE_ID);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/busstops", BUS_LINE_ID_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/busstops/#", BUS_LINE_ID_STOP_ID);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections", BUS_LINE_ID_DIRECTIONS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections/*/busstops", BUS_LINE_ID_DIRECTION_ID_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections/*/busstops/search/*", BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "buslines/*", BUS_LINES_IDS);
		URI_MATCHER.addURI(AUTHORITY, "busstops", BUS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "busstopslivefolder/*", BUS_STOPS_LIVE_FOLDER);
		URI_MATCHER.addURI(AUTHORITY, "busstops/#", BUS_STOP_ID);
		URI_MATCHER.addURI(AUTHORITY, "busstops/#/buslines", BUS_STOP_ID_BUS_LINES);
		URI_MATCHER.addURI(AUTHORITY, "busstops/*", BUS_STOPS_IDS);
		URI_MATCHER.addURI(AUTHORITY, "buslinedirections", BUS_LINE_DIRECTIONS);
		URI_MATCHER.addURI(AUTHORITY, "buslinedirections/*", BUS_LINE_DIRECTION_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines", SUBWAY_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/search/*", SUBWAY_LINES_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#", SUBWAY_LINE_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#/subwaystations", SUBWAY_LINE_ID_STATIONS);
		URI_MATCHER.addURI(AUTHORITY, "subwaylines/#/subwaystations/search/*", SUBWAY_LINE_ID_STATIONS_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations", SUBWAY_STATIONS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#", SUBWAY_STATION_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/subwaylines", SUBWAY_STATION_ID_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/busstops", SUBWAY_STATION_ID_BUS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/buslines", SUBWAY_STATION_ID_BUS_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/*", SUBWAY_STATIONS_IDS);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(StmStore.SubwayStation._ID, StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS "
		        + StmStore.SubwayStation._ID);
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
		map.put(StmStore.SubwayLine._ID, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " AS " + StmStore.SubwayLine._ID);
		map.put(StmStore.SubwayLine.LINE_NUMBER, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " AS "
		        + StmStore.SubwayLine.LINE_NUMBER);
		map.put(StmStore.SubwayLine.LINE_NAME, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME + " AS " + StmStore.SubwayLine.LINE_NAME);
		sSubwayLinesProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusLineDirection._ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + " AS "
		        + StmStore.BusLineDirection._ID);
		map.put(StmStore.BusLineDirection.DIRECTION_ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + " AS "
		        + StmStore.BusLineDirection.DIRECTION_ID);
		map.put(StmStore.BusLineDirection.DIRECTION_LINE_ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_LINE_ID + " AS "
		        + StmStore.BusLineDirection.DIRECTION_LINE_ID);
		map.put(StmStore.BusLineDirection.DIRECTION_NAME, StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_NAME + " AS "
		        + StmStore.BusLineDirection.DIRECTION_NAME);
		sBusLineDirectionsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusLine._ID, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS " + StmStore.BusLine._ID);
		map.put(StmStore.BusLine.LINE_NUMBER, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS " + StmStore.BusLine.LINE_NUMBER);
		map.put(StmStore.BusLine.LINE_NAME, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " AS " + StmStore.BusLine.LINE_NAME);
		map.put(StmStore.BusLine.LINE_HOURS, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_HOURS + " AS " + StmStore.BusLine.LINE_HOURS);
		map.put(StmStore.BusLine.LINE_TYPE, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_TYPE + " AS " + StmStore.BusLine.LINE_TYPE);
		sBusLinesProjectionMap = map;

		map = new HashMap<String, String>();
		// TODO bus stop code + bus line number is NOT an UID for a bus stop.
		// Need to add bus line direction ?
		map.put(LiveFolders._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "||" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + LiveFolders._ID);
		map.put(LiveFolders.NAME, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS " + LiveFolders.NAME);
		map.put(LiveFolders.DESCRIPTION, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "||\" \"||" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_CODE + " AS " + LiveFolders.DESCRIPTION);
		sBusStopsLiveFolderProjectionMap = map;
		
		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS " + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS " + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS " + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, "substr(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + ",length(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "))" + " AS " + StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS "
		        + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		sBusStopsProjectionMap = map;
		
		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, "substr(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + ",length(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "))" + " AS " + StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS " + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS " + StmStore.SubwayStation.STATION_NAME);
		sBusStopsWithSubwayStationProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS " + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS " + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS " + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID, "substr(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + ",length(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "))" + " AS " + StmStore.BusStop.STOP_SIMPLE_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.LINE_NAME, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " AS " + StmStore.BusStop.LINE_NAME);
		map.put(StmStore.BusStop.LINE_TYPE, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_TYPE + " AS " + StmStore.BusStop.LINE_TYPE);
		map.put(StmStore.BusStop.LINE_HOURS, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_HOURS + " AS " + StmStore.BusStop.LINE_HOURS);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS "
		        + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		sBusStopsExtendedProjectionMap = map;
	}

	private StmDbHelper mOpenHelper;

	private static final String SUBWAY_LINE_STATIONS_JOIN = StmDbHelper.T_SUBWAY_LINES + " LEFT OUTER JOIN "
	        + StmDbHelper.T_SUBWAY_DIRECTIONS + " ON " + StmDbHelper.T_SUBWAY_LINES + "."
	        + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_DIRECTIONS + "."
	        + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID + " LEFT OUTER JOIN " + StmDbHelper.T_SUBWAY_STATIONS
	        + " ON " + StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID
	        + "=" + StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;

	private static final String BUS_STOP_LINES_JOIN = StmDbHelper.T_BUS_LINES + " JOIN " + StmDbHelper.T_BUS_STOPS
	        + " ON " + StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + "=" + StmDbHelper.T_BUS_STOPS
	        + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER;

	private static final String SUBWAY_STATIONS_LINE_JOIN = StmDbHelper.T_SUBWAY_LINES + " JOIN "
	        + StmDbHelper.T_SUBWAY_DIRECTIONS + " ON " + StmDbHelper.T_SUBWAY_LINES + "."
	        + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_DIRECTIONS + "."
	        + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID;

	private static final String BUS_STOP_SUBWAY_STATION_JOIN = StmDbHelper.T_BUS_STOPS + " LEFT OUTER JOIN "
	        + StmDbHelper.T_SUBWAY_STATIONS + " ON " + StmDbHelper.T_BUS_STOPS + "."
	        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "=" + StmDbHelper.T_SUBWAY_STATIONS + "."
	        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		mOpenHelper = new StmDbHelper(getContext());
		// close the DbHelper to be sure that the stm.db is accessible.
		mOpenHelper.close();
		// reopen the database
		mOpenHelper.open();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(" + uri.getPath() + ", " + Arrays.toString(projection) + ", " + selection + ", " + Arrays.toString(selectionArgs) + ", "
		        + sortOrder + " )");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[" + uri + "]");
		switch (URI_MATCHER.match(uri)) {
		case BUS_LINES:
			MyLog.v(TAG, "query>BUS_LINES");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			break;
		case BUS_LINES_SEARCH:
			MyLog.v(TAG, "query>BUS_LINES_SEARCH");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			if (!TextUtils.isEmpty(uri.getPathSegments().get(2))) {
				String[] keywords = uri.getPathSegments().get(2).split(" ");
				String inWhere = "";
				for (String keyword : keywords) {
					if (inWhere.length() > 0) {
						inWhere += " AND ";
					}
					inWhere += "(" + StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " LIKE '%"
					        + keyword + "%'";
					inWhere += " OR ";
					inWhere += StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " LIKE '%" + keyword
					        + "%')";
				}
				qb.appendWhere(inWhere);
			}
			break;
		case BUS_LINES_IDS:
			MyLog.v(TAG, "query>BUS_LINES_IDS");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			String[] busLineIds = uri.getPathSegments().get(1).split("\\+");
			qb.appendWhere(StmDbHelper.T_BUS_LINES_K_NUMBER + " IN (");
			for (int i = 0; i < busLineIds.length; i++) {
				if (i > 0) {
					qb.appendWhere(",");
				}
				qb.appendWhere(busLineIds[i]);
			}
			qb.appendWhere(")");
			break;
		case BUS_LINE_ID:
			MyLog.v(TAG, "query>BUS_LINE_ID");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			qb.appendWhere(StmDbHelper.T_BUS_LINES_K_NUMBER + "=" + uri.getPathSegments().get(1));
			break;
		case BUS_LINE_ID_STOPS:
			MyLog.v(TAG, "query>BUS_LINE_ID_STOPS");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case BUS_LINE_ID_STOP_ID:
			MyLog.v(TAG, "query>BUS_LINE_ID_STOP_ID");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "=");
			String busLineNumber = uri.getPathSegments().get(1);
			qb.appendWhere(busLineNumber);
			qb.appendWhere(" AND ");
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "=");
			String busStopCode = uri.getPathSegments().get(3);
			qb.appendWhere(busStopCode);
			break;
		case BUS_STOP_ID_BUS_LINES:
			MyLog.v(TAG, "BUS_STOP_ID_BUS_LINES");
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setProjectionMap(sBusLinesProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "=");
			busStopCode = uri.getPathSegments().get(1);
			qb.appendWhere(busStopCode);
			break;
		case BUS_LINE_ID_DIRECTIONS:
			MyLog.v(TAG, "query>BUS_LINE_DIRECTIONS");
			qb.setTables(StmDbHelper.T_BUS_LINE_DIRECTIONS);
			qb.setProjectionMap(sBusLineDirectionsProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_LINE_ID + "=");
			String busLineNumber2 = uri.getPathSegments().get(1);
			qb.appendWhere(busLineNumber2);
			break;
		case BUS_LINE_ID_DIRECTION_ID_STOPS:
			MyLog.v(TAG, "query>BUS_LINE_DIRECTION_STOPS");
			qb.setTables(BUS_STOP_SUBWAY_STATION_JOIN);
			qb.setProjectionMap(sBusStopsWithSubwayStationProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "=");
			String lineId = uri.getPathSegments().get(1);
			qb.appendWhere(lineId);
			qb.appendWhere(" AND ");
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "=");
			String directionId = uri.getPathSegments().get(3);
			qb.appendWhere("\"" + directionId + "\"");
			break;
		case BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH:
			MyLog.v(TAG, "query>BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH");
			qb.setTables(BUS_STOP_SUBWAY_STATION_JOIN);
			qb.setProjectionMap(sBusStopsWithSubwayStationProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "="
			        + uri.getPathSegments().get(1));
			qb.appendWhere(" AND ");
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "=\""
			        + uri.getPathSegments().get(3) + "\"");
			if (!TextUtils.isEmpty(uri.getPathSegments().get(6))) {
				String[] keywords = uri.getPathSegments().get(6).split(" ");
				for (String keyword : keywords) {
					qb.appendWhere(" AND ");
					qb.appendWhere("(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " LIKE '%"
					        + keyword + "%'");
					qb.appendWhere(" OR ");
					qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " LIKE '%"
					        + keyword + "%'");
					qb.appendWhere(" OR ");
					qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME
					        + " LIKE '%" + keyword + "%')");
				}
			}
			break;
		case BUS_STOPS:
			MyLog.v(TAG, "query>BUS_STOPS");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			break;
		case BUS_STOPS_IDS:
			MyLog.v(TAG, "query>BUS_STOPS_IDS");
			qb.setDistinct(true);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setProjectionMap(sBusStopsExtendedProjectionMap);
			String[] tmp = new String[2];
			String[] favIds = uri.getPathSegments().get(1).split("\\+");
			for (int i = 0; i < favIds.length; i++) {
				tmp = favIds[i].split("-");
				if (i > 0) {
					qb.appendWhere("OR ");
				}
				qb.appendWhere("(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " = " + tmp[0] + " AND " + StmDbHelper.T_BUS_STOPS + "."
				        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " = " + tmp[1] + ") ");
			}
			break;
		case BUS_STOPS_LIVE_FOLDER:
			MyLog.v(TAG, "query>BUS_STOPS_LIVE_FOLDER");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.setProjectionMap(sBusStopsLiveFolderProjectionMap);
			tmp = new String[2];
			favIds = uri.getPathSegments().get(1).split("\\+");
			for (int i = 0; i < favIds.length; i++) {
				tmp = favIds[i].split("-");
				if (i > 0) {
					qb.appendWhere("OR ");
				}
				qb.appendWhere("(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " = " + tmp[0] + " AND " + StmDbHelper.T_BUS_STOPS + "."
				        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " = " + tmp[1] + ") ");
			}
			break;
		case BUS_STOP_ID:
			MyLog.v(TAG, "query>BUS_STOP_ID");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS_K_CODE + "=" + uri.getPathSegments().get(1));
			break;
		case BUS_LINE_DIRECTIONS:
			MyLog.v(TAG, "query>BUS_LINE_DIRECTIONS");
			qb.setTables(StmDbHelper.T_BUS_LINE_DIRECTIONS);
			break;
		case BUS_LINE_DIRECTION_ID:
			MyLog.v(TAG, "query>BUS_LINE_DIRECTIONS");
			qb.setTables(StmDbHelper.T_BUS_LINE_DIRECTIONS);
			qb.setProjectionMap(sBusLineDirectionsProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + "=\"" + uri.getPathSegments().get(1) + "\"");
			break;
		case SUBWAY_LINES:
			MyLog.v(TAG, "query>SUBWAY_LINES");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			break;
		case SUBWAY_LINES_SEARCH:
			MyLog.v(TAG, "query>SUBWAY_LINES_SEARCH");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			if (!TextUtils.isEmpty(uri.getPathSegments().get(2))) {
				String[] keywords = uri.getPathSegments().get(2).split(" ");
				String inWhere = "";
				for (String keyword : keywords) {
					if (inWhere.length() > 0) {
						inWhere += " AND ";
					}
					inWhere += "(" + StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER
					        + " LIKE '%" + keyword + "%'";
					inWhere += " OR ";
					inWhere += StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME + " LIKE '%"
					        + keyword + "%')";
				}
				qb.appendWhere(inWhere);
			}
			break;
		case SUBWAY_STATION_ID_LINES:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_LINES");
			qb.setTables(SUBWAY_STATIONS_LINE_JOIN);
			qb.setProjectionMap(sSubwayLinesProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID + "=" + uri.getPathSegments().get(1));
			break;
		case SUBWAY_LINE_ID:
			MyLog.v(TAG, "query>SUBWAY_LINE_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			String subwayLineId = uri.getPathSegments().get(1);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + subwayLineId);
			break;
		case SUBWAY_LINE_ID_STATIONS:
			MyLog.v(TAG, "query>SUBWAY_LINE_STATIONS");
			qb.setTables(SUBWAY_LINE_STATIONS_JOIN);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case SUBWAY_LINE_ID_STATIONS_SEARCH:
			MyLog.v(TAG, "query>SUBWAY_LINE_ID_STATIONS_SEARCH");
			qb.setTables(SUBWAY_LINE_STATIONS_JOIN);
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "="
			        + uri.getPathSegments().get(1));
			if (!TextUtils.isEmpty(uri.getPathSegments().get(4))) {
				String[] keywords = uri.getPathSegments().get(4).split(" ");
				for (String keyword : keywords) {
					qb.appendWhere(" AND ");
					qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME
					        + " LIKE '%" + keyword + "%'");
				}
			}
			break;
		case SUBWAY_STATIONS:
			MyLog.v(TAG, "query>SUBWAY_STATIONS");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
			break;
		case SUBWAY_STATIONS_IDS:
			MyLog.v(TAG, "query>SUBWAY_STATIONS_IDS");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
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
		case SUBWAY_STATION_ID:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_STATIONS);
			qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + "=" + uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_BUS_LINES:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_BUS_LINES");
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setDistinct(true);
			qb.setProjectionMap(sBusLinesProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_BUS_STOPS:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_BUS_STOPS");
			qb.setDistinct(true);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setProjectionMap(sBusStopsExtendedProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case BUS_LINES:
			case BUS_LINES_SEARCH:
			case SUBWAY_STATION_ID_BUS_STOPS:
			case SUBWAY_STATION_ID_BUS_LINES:
			case BUS_LINES_IDS:
			case BUS_STOP_ID_BUS_LINES:
			case BUS_LINE_ID:
				orderBy = StmStore.BusLine.DEFAULT_SORT_ORDER;
				break;
			case BUS_LINE_ID_DIRECTION_ID_STOPS:
			case BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH:
			case BUS_STOPS:
			case BUS_STOPS_IDS:
			case BUS_LINE_ID_STOP_ID:
			case BUS_STOP_ID:
				orderBy = StmStore.BusStop.DEFAULT_SORT_ORDER;
				break;
			case BUS_LINE_ID_DIRECTIONS:
			case BUS_LINE_DIRECTIONS:
			case BUS_LINE_DIRECTION_ID:
				orderBy = StmStore.BusLineDirection.DEFAULT_SORT_ORDER;
				break;
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
				orderBy = StmStore.SubwayStation.DEFAULT_SORT_ORDER;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(" + uri.getPath() + ")");
		switch (URI_MATCHER.match(uri)) {
		case BUS_STOP_ID_BUS_LINES:
		case BUS_LINES_IDS:
		case SUBWAY_STATION_ID_BUS_LINES:
		case BUS_LINES:
		case BUS_LINES_SEARCH:
			return StmStore.BusLine.CONTENT_TYPE;
		case BUS_LINE_ID:
			return StmStore.BusLine.CONTENT_ITEM_TYPE;
		case BUS_STOPS_LIVE_FOLDER:
			return StmStore.BusStop.CONTENT_TYPE_LIVE_FOLDER;
		case BUS_LINE_ID_DIRECTION_ID_STOPS:
		case BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH:
		case BUS_STOPS_IDS:
		case SUBWAY_STATION_ID_BUS_STOPS:
		case BUS_STOPS:
			return StmStore.BusStop.CONTENT_TYPE;
		case BUS_STOP_ID:
			return StmStore.BusStop.CONTENT_ITEM_TYPE;
		case BUS_LINE_ID_DIRECTIONS:
		case BUS_LINE_DIRECTIONS:
			return StmStore.BusLineDirection.CONTENT_TYPE;
		case BUS_LINE_DIRECTION_ID:
			return StmStore.BusLineDirection.CONTENT_ITEM_TYPE;
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
			return StmStore.SubwayStation.CONTENT_TYPE;
		case SUBWAY_STATION_ID:
			return StmStore.SubwayStation.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete()");
		MyLog.w(TAG, "The delete method is not available.");
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "update()");
		MyLog.w(TAG, "The update method is not available.");
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		MyLog.v(TAG, "insert()");
		MyLog.w(TAG, "The insert method is not available.");
		return null;
	}
}

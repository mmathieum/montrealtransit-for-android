package org.montrealtransit.android.provider;

import java.util.Arrays;
import java.util.HashMap;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.LiveFolders;
import android.text.TextUtils;

/**
 * This data provider contains static informations about bus stops, bus lines, subway lines, subway stations.
 * @author Mathieu Méa
 */
@SuppressWarnings("deprecation") //TODO use App Widgets (Android 3.0+)
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
	private static final int SEARCH = 30;
	private static final int BUS_STOPS_SEARCH = 31;
	private static final int SUBWAY_STATION_ID_LINES_OTHER = 32;
	private static final int SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS = 33;
	private static final int SUBWAY_STATION_ID_DIRECTION_ID_DAY = 34;
	private static final int SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY = 35;
	private static final int SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID = 36;
	private static final int SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID = 37;
	private static final int SUBWAY_STATIONS_AND_LINES = 38;
	private static final int BUS_LINES_NUMBER = 39;
	private static final int BUS_STOPS_CODE = 40;

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
	 * Projection for bus line direction.
	 */
	private static final HashMap<String, String> sBusLineDirectionsProjectionMap;
	/**
	 * Projection for bus lines.
	 */
	private static final HashMap<String, String> sBusLinesProjectionMap;
	/**
	 * Projection for bus lines numbers.
	 */
	private static final HashMap<String, String> sBusLinesNumbersProjectionMap;
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
	 * Projection for bus stops codes.
	 */
	private static final HashMap<String, String> sBusStopsCodeProjectionMap;
	/**
	 * Projection for bus stops with subway line name.
	 */
	private static final HashMap<String, String> sBusStopsWithSubwayStationProjectionMap;
	/**
	 * Projection for the search (bus stop, bus line, subway station).
	 */
	private static final HashMap<String, String> sSearchProjectionMap;
	/**
	 * Projection for the simple search (bus stop only).
	 */
	private static final HashMap<String, String> sSearchSimpleProjectionMap;
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
		URI_MATCHER.addURI(AUTHORITY, "buslines", BUS_LINES);
		URI_MATCHER.addURI(AUTHORITY, "buslines/" + StmStore.BusLine.LINE_NUMBER, BUS_LINES_NUMBER);
		URI_MATCHER.addURI(AUTHORITY, "buslines/search/*", BUS_LINES_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#", BUS_LINE_ID);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/busstops", BUS_LINE_ID_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/busstops/#", BUS_LINE_ID_STOP_ID);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections", BUS_LINE_ID_DIRECTIONS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections/*/busstops", BUS_LINE_ID_DIRECTION_ID_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "buslines/#/buslinedirections/*/busstops/search/*",
		        BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH);
		URI_MATCHER.addURI(AUTHORITY, "buslines/*", BUS_LINES_IDS);
		URI_MATCHER.addURI(AUTHORITY, "busstops", BUS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "busstops/" + StmStore.BusStop.STOP_CODE, BUS_STOPS_CODE);
		URI_MATCHER.addURI(AUTHORITY, "busstopslivefolder/*", BUS_STOPS_LIVE_FOLDER);
		URI_MATCHER.addURI(AUTHORITY, "busstopssearch/*", BUS_STOPS_SEARCH);
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
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/subwaylines", SUBWAY_STATIONS_AND_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#", SUBWAY_STATION_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/directions/#", SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/directions/#/*", SUBWAY_STATION_ID_DIRECTION_ID_DAY);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/subwaylines", SUBWAY_STATION_ID_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/subwaylines/other", SUBWAY_STATION_ID_LINES_OTHER);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/buslines", SUBWAY_STATION_ID_BUS_LINES);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/buslines/#/busstops", SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/#/busstops", SUBWAY_STATION_ID_BUS_STOPS);
		URI_MATCHER.addURI(AUTHORITY, "subwaystations/*", SUBWAY_STATIONS_IDS);
		URI_MATCHER.addURI(AUTHORITY, "subwaydirections/#/days/*/hours/*", SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID);
		URI_MATCHER.addURI(AUTHORITY, "subwaydirections/#/hours/*", SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(StmStore.SubwayStation._ID, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS " + StmStore.SubwayStation._ID);
		map.put(StmStore.SubwayStation.STATION_ID, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS " + StmStore.SubwayStation.STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS " + StmStore.SubwayStation.STATION_NAME);
		map.put(StmStore.SubwayStation.STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT + " AS " + StmStore.SubwayStation.STATION_LAT);
		map.put(StmStore.SubwayStation.STATION_LNG, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG + " AS " + StmStore.SubwayStation.STATION_LNG);
		sSubwayStationsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.SubwayLine._ID, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER
		        + " AS " + StmStore.SubwayLine._ID);
		map.put(StmStore.SubwayLine.LINE_NUMBER, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER
		        + " AS " + StmStore.SubwayLine.LINE_NUMBER);
		map.put(StmStore.SubwayLine.LINE_NAME, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME
		        + " AS " + StmStore.SubwayLine.LINE_NAME);
		sSubwayLinesProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "||'-'||"
		        + StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS "
		        + BaseColumns._ID);
		map.put(StmStore.SubwayLine.LINE_NUMBER, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER
		        + " AS " + StmStore.SubwayLine.LINE_NUMBER);
		map.put(StmStore.SubwayLine.LINE_NAME, StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NAME
		        + " AS " + StmStore.SubwayLine.LINE_NAME);
		map.put(StmStore.SubwayStation.STATION_ID, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + " AS " + StmStore.SubwayStation.STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS " + StmStore.SubwayStation.STATION_NAME);
		map.put(StmStore.SubwayStation.STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT + " AS " + StmStore.SubwayStation.STATION_LAT);
		map.put(StmStore.SubwayStation.STATION_LNG, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG + " AS " + StmStore.SubwayStation.STATION_LNG);
		sSubwayLinesStationsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusLineDirection._ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "."
		        + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + " AS " + StmStore.BusLineDirection._ID);
		map.put(StmStore.BusLineDirection.DIRECTION_ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "."
		        + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + " AS " + StmStore.BusLineDirection.DIRECTION_ID);
		map.put(StmStore.BusLineDirection.DIRECTION_LINE_ID, StmDbHelper.T_BUS_LINE_DIRECTIONS + "."
		        + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_LINE_ID + " AS " + StmStore.BusLineDirection.DIRECTION_LINE_ID);
		map.put(StmStore.BusLineDirection.DIRECTION_NAME, StmDbHelper.T_BUS_LINE_DIRECTIONS + "."
		        + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_NAME + " AS " + StmStore.BusLineDirection.DIRECTION_NAME);
		sBusLineDirectionsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusLine._ID, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS "
		        + StmStore.BusLine._ID);
		map.put(StmStore.BusLine.LINE_NUMBER, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS "
		        + StmStore.BusLine.LINE_NUMBER);
		map.put(StmStore.BusLine.LINE_NAME, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " AS "
		        + StmStore.BusLine.LINE_NAME);
		map.put(StmStore.BusLine.LINE_TYPE, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_TYPE + " AS "
		        + StmStore.BusLine.LINE_TYPE);
		sBusLinesProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusLine._ID, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS "
		        + StmStore.BusLine._ID);
		map.put(StmStore.BusLine.LINE_NUMBER, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " AS "
		        + StmStore.BusLine.LINE_NUMBER);
		sBusLinesNumbersProjectionMap = map;

		map = new HashMap<String, String>();
		// TODO bus stop code + bus line number is NOT an UID for a bus stop.
		// Need to add bus line direction ?
		map.put(LiveFolders._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "||"
		        + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + LiveFolders._ID);
		map.put(LiveFolders.NAME, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + LiveFolders.NAME);
		map.put(LiveFolders.DESCRIPTION, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
		        + "||\" \"||" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + LiveFolders.DESCRIPTION);
		sBusStopsLiveFolderProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_DIRECTION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + " AS " + StmStore.BusStop.STOP_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS " + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		sBusStopsProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop.STOP_CODE);
		sBusStopsCodeProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_DIRECTION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + " AS " + StmStore.BusStop.STOP_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS " + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		map.put(StmStore.SubwayStation.STATION_NAME, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " AS " + StmStore.SubwayStation.STATION_NAME);
		map.put(StmStore.SubwayStation.STATION_LAT, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT + " AS " + StmStore.SubwayStation.STATION_LAT);
		map.put(StmStore.SubwayStation.STATION_LNG, StmDbHelper.T_SUBWAY_STATIONS + "."
		        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG + " AS " + StmStore.SubwayStation.STATION_LNG);
		sBusStopsWithSubwayStationProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(StmStore.BusStop._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop._ID);
		map.put(StmStore.BusStop.STOP_CODE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " AS "
		        + StmStore.BusStop.STOP_CODE);
		map.put(StmStore.BusStop.STOP_PLACE, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + StmStore.BusStop.STOP_PLACE);
		map.put(StmStore.BusStop.STOP_DIRECTION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + " AS " + StmStore.BusStop.STOP_DIRECTION_ID);
		map.put(StmStore.BusStop.STOP_LINE_NUMBER, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + StmStore.BusStop.STOP_LINE_NUMBER);
		map.put(StmStore.BusStop.LINE_NAME, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " AS "
		        + StmStore.BusStop.LINE_NAME);
		map.put(StmStore.BusStop.LINE_TYPE, StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_TYPE + " AS "
		        + StmStore.BusStop.LINE_TYPE);
		map.put(StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + " AS " + StmStore.BusStop.STOP_SUBWAY_STATION_ID);
		sBusStopsExtendedProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + " AS "
		        + BaseColumns._ID);
		map.put(StmStore.HOUR, "strftime('%Hh%M'," + StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_HOUR
		        + ") AS " + StmStore.HOUR);
		map.put(StmStore.FIRST_LAST, StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_FIRST_LAST + " AS "
		        + StmStore.FIRST_LAST);
		sSubwayStationHourProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "||'"
		        + BusStop.UID_SEPARATOR + "'||" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
		        + " AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_CODE + "||'" + BusStop.UID_SEPARATOR + "'||" + StmDbHelper.T_BUS_STOPS
		        + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE
		        + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE
		        + "||' - '||" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "||' '||"
		        + "substr(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + ",length("
		        + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "))" + " AS "
		        + SearchManager.SUGGEST_COLUMN_TEXT_2);
		sSearchSimpleProjectionMap = map;

		map = new HashMap<String, String>();
		map.put(BaseColumns._ID, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "||'"
		        + BusStop.UID_SEPARATOR + "'||" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
		        + " AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_CODE + "||'" + BusStop.UID_SEPARATOR + "'||" + StmDbHelper.T_BUS_STOPS
		        + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE
		        + "||' '||" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " AS "
		        + SearchManager.SUGGEST_COLUMN_TEXT_1);
		// TODO add subway station to text 1 (only when there is a subway station)
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "||' '||" + StmDbHelper.T_BUS_LINES + "."
		        + StmDbHelper.T_BUS_LINES_K_NAME + "||' '||" + "substr(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + ",length(" + StmDbHelper.T_BUS_STOPS + "."
		        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "))" + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2);
		sSearchProjectionMap = map;
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

	private static final String SUBWAY_STATIONS_LINE_DIRECTION_JOIN = StmDbHelper.T_SUBWAY_DIRECTIONS + " JOIN "
	        + StmDbHelper.T_SUBWAY_LINES + " ON " + StmDbHelper.T_SUBWAY_LINES + "."
	        + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "=" + StmDbHelper.T_SUBWAY_DIRECTIONS + "."
	        + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID + " JOIN " + StmDbHelper.T_SUBWAY_STATIONS + " ON "
	        + StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + "="
	        + StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID;

	private static final String BUS_STOP_SUBWAY_STATION_JOIN = StmDbHelper.T_BUS_STOPS + " LEFT OUTER JOIN "
	        + StmDbHelper.T_SUBWAY_STATIONS + " ON " + StmDbHelper.T_BUS_STOPS + "."
	        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "=" + StmDbHelper.T_SUBWAY_STATIONS + "."
	        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;

	private static final String BUS_STOP_ALL_JOIN = StmDbHelper.T_BUS_STOPS + " LEFT OUTER JOIN "
	        + StmDbHelper.T_BUS_LINES + " ON " + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
	        + "=" + StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER + " LEFT OUTER JOIN "
	        + StmDbHelper.T_SUBWAY_STATIONS + " ON " + StmDbHelper.T_BUS_STOPS + "."
	        + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "=" + StmDbHelper.T_SUBWAY_STATIONS + "."
	        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	/**
	 * Excluded bus lines.
	 */
	private static final String EXCLUDED_BUS_LINES = StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NUMBER
	        + " NOT IN(767,769,777)";

	private static final String SEARCH_SPLIT_ON = "[\\s\\W]";

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection,
		        Arrays.toString(selectionArgs), sortOrder);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[%s]", uri);
		String limit = null;
		switch (URI_MATCHER.match(uri)) {
		case BUS_LINES:
			MyLog.v(TAG, "query>BUS_LINES");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			qb.appendWhere(EXCLUDED_BUS_LINES);
			break;
		case BUS_LINES_NUMBER:
			MyLog.v(TAG, "query>BUS_LINES_NUMBER");
			qb.setDistinct(true);
			qb.setTables(StmDbHelper.T_BUS_LINES);
			qb.setProjectionMap(sBusLinesNumbersProjectionMap);
			qb.appendWhere(EXCLUDED_BUS_LINES);
			break;
		case BUS_LINES_SEARCH:
			MyLog.v(TAG, "query>BUS_LINES_SEARCH");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			String search = Uri.decode(uri.getPathSegments().get(2));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
				String inWhere = EXCLUDED_BUS_LINES;
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
			qb.appendWhere(" AND " + EXCLUDED_BUS_LINES);
			break;
		case BUS_LINE_ID:
			MyLog.v(TAG, "query>BUS_LINE_ID");
			qb.setTables(StmDbHelper.T_BUS_LINES);
			qb.appendWhere(StmDbHelper.T_BUS_LINES_K_NUMBER + "=" + uri.getPathSegments().get(1));
			qb.appendWhere(" AND " + EXCLUDED_BUS_LINES);
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
			MyLog.v(TAG, "query>BUS_LINE_ID_DIRECTIONS");
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
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_BUS_STOPS + "."
			        + StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID + "=\"" + uri.getPathSegments().get(3) + "\"");
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
			search = Uri.decode(uri.getPathSegments().get(6));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
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
		case BUS_STOPS_CODE:
			MyLog.v(TAG, "query>BUS_STOPS_CODE");
			qb.setDistinct(true);
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.setProjectionMap(sBusStopsCodeProjectionMap);
			break;
		case BUS_STOPS_IDS:
			MyLog.v(TAG, "query>BUS_STOPS_IDS");
			qb.setDistinct(true);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setProjectionMap(sBusStopsExtendedProjectionMap);
			String[] favIds = uri.getPathSegments().get(1).split("\\+");
			for (int i = 0; i < favIds.length; i++) {
				if (i > 0) {
					qb.appendWhere("OR ");
				}
				qb.appendWhere("(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " = "
				        + BusStop.getCodeFromUID(favIds[i]) + " AND " + StmDbHelper.T_BUS_STOPS + "."
				        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " = " + BusStop.getLineNumberFromUID(favIds[i])
				        + ") ");
			}
			break;
		case BUS_STOPS_SEARCH:
			MyLog.v(TAG, "query>BUS_STOPS_SEARCH");
			qb.setDistinct(true);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.setProjectionMap(sBusStopsExtendedProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "!=''");
			if (uri.getPathSegments().size() > 1) {
				search = Uri.decode(uri.getPathSegments().get(1));
				if (!TextUtils.isEmpty(search)) {
					String[] keywords = search.split(SEARCH_SPLIT_ON);
					String inWhere = "";
					for (String keyword : keywords) {
						inWhere += " AND ";
						inWhere += "(";
						if (TextUtils.isDigitsOnly(keyword)) {
							// BUS STOP CODE
							inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " LIKE '%"
							        + keyword + "%'";
							inWhere += " OR ";
							// BUS STOP LINE NUMBER
							inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
							        + " LIKE '%" + keyword + "%'";
							inWhere += " OR ";
						}
						// BUS STOP PLACE
						inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " LIKE '%"
						        + keyword + "%'";
						if (!TextUtils.isDigitsOnly(keyword)) {
							// inWhere += " OR ";
							// TODO ? BUS STOP SUBWAY STATION NAME
							// inWhere += StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME
							// + " LIKE '%" + keyword + "%'";
							inWhere += " OR ";
							// BUS STOP LINE NAME
							inWhere += StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " LIKE '%"
							        + keyword + "%'";
						}
						inWhere += ")";
					}
					qb.appendWhere(inWhere);
				}
			}
			break;
		case BUS_STOPS_LIVE_FOLDER:
			MyLog.v(TAG, "query>BUS_STOPS_LIVE_FOLDER");
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.setProjectionMap(sBusStopsLiveFolderProjectionMap);
			favIds = uri.getPathSegments().get(1).split("\\+");
			for (int i = 0; i < favIds.length; i++) {
				if (i > 0) {
					qb.appendWhere("OR ");
				}
				qb.appendWhere("(" + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " = "
				        + BusStop.getCodeFromUID(favIds[i]) + " AND " + StmDbHelper.T_BUS_STOPS + "."
				        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + " = " + BusStop.getLineNumberFromUID(favIds[i])
				        + ") ");
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
			MyLog.v(TAG, "query>BUS_LINE_DIRECTION_ID");
			qb.setTables(StmDbHelper.T_BUS_LINE_DIRECTIONS);
			qb.setProjectionMap(sBusLineDirectionsProjectionMap);
			qb.appendWhere(StmDbHelper.T_BUS_LINE_DIRECTIONS + "." + StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID + "=\""
			        + uri.getPathSegments().get(1) + "\"");
			break;
		case SUBWAY_LINES:
			MyLog.v(TAG, "query>SUBWAY_LINES");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			qb.setProjectionMap(sSubwayLinesProjectionMap);
			break;
		case SUBWAY_LINES_SEARCH:
			MyLog.v(TAG, "query>SUBWAY_LINES_SEARCH");
			qb.setTables(StmDbHelper.T_SUBWAY_LINES);
			search = Uri.decode(uri.getPathSegments().get(2));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
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
			qb.appendWhere(StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID
			        + "=" + uri.getPathSegments().get(1));
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
			qb.appendWhere(StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID
			        + "!=" + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_DIRECTIONS + "."
			        + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID + " IN (" + "SELECT "
			        + StmDbHelper.T_SUBWAY_DIRECTIONS + "." + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID
			        + " FROM " + StmDbHelper.T_SUBWAY_DIRECTIONS + " WHERE " + StmDbHelper.T_SUBWAY_DIRECTIONS + "."
			        + StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID + "=" + uri.getPathSegments().get(1) + ")");
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
			qb.appendWhere(StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + "="
			        + uri.getPathSegments().get(1));
			search = Uri.decode(uri.getPathSegments().get(4));
			if (!TextUtils.isEmpty(search)) {
				String[] keywords = search.split(SEARCH_SPLIT_ON);
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
			qb.setProjectionMap(sSubwayStationsProjectionMap);
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
			qb.setProjectionMap(sSubwayStationsProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID + "=" + uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_BUS_LINES:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_BUS_LINES");
			qb.setDistinct(true);
			qb.setProjectionMap(sBusLinesProjectionMap);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "="
			        + uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS");
			qb.setDistinct(true);
			qb.setProjectionMap(sBusStopsProjectionMap);
			qb.setTables(StmDbHelper.T_BUS_STOPS);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_BUS_STOPS + "."
			        + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + "=" + uri.getPathSegments().get(3) + " AND "
			        + StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "!=''");
			break;
		case SUBWAY_STATION_ID_BUS_STOPS:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_BUS_STOPS");
			qb.setDistinct(true);
			qb.setProjectionMap(sBusStopsExtendedProjectionMap);
			qb.setTables(BUS_STOP_LINES_JOIN);
			qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID + "="
			        + uri.getPathSegments().get(1));
			break;
		case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY");
			qb.setProjectionMap(sSubwayStationHourProjectionMap);
			qb.setTables(StmDbHelper.T_SUBWAY_HOUR);
			qb.appendWhere(StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_STATION_ID + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_HOUR + "."
			        + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + "=" + uri.getPathSegments().get(3) + " AND "
			        + StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DAY + "=''");
			break;
		case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
			MyLog.v(TAG, "query>SUBWAY_STATION_ID_DIRECTION_ID_DAY");
			qb.setTables(StmDbHelper.T_SUBWAY_HOUR);
			qb.setProjectionMap(sSubwayStationHourProjectionMap);
			qb.appendWhere(StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_STATION_ID + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_HOUR + "."
			        + StmDbHelper.T_SUBWAY_HOUR_K_DIRECTION_ID + "=" + uri.getPathSegments().get(3) + " AND "
			        + StmDbHelper.T_SUBWAY_HOUR + "." + StmDbHelper.T_SUBWAY_HOUR_K_DAY + "='"
			        + uri.getPathSegments().get(4) + "'");
			break;

		case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
			MyLog.v(TAG, "query>SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_FREQUENCES);
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(StmStore.FREQUENCY, StmDbHelper.T_SUBWAY_FREQUENCES + "."
			        + StmDbHelper.T_SUBWAY_FREQUENCES_K_FREQUENCE + " AS " + StmStore.FREQUENCY);
			qb.setProjectionMap(map);
			qb.appendWhere(StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DIRECTION + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_FREQUENCES + "."
			        + StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY + "='' AND time(" + StmDbHelper.T_SUBWAY_FREQUENCES + "."
			        + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + ", '-2 hour') <= '" + uri.getPathSegments().get(3) + "'");
			limit = "1";
			sortOrder = StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + " DESC";

			break;
		case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
			MyLog.v(TAG, "query>SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID");
			qb.setTables(StmDbHelper.T_SUBWAY_FREQUENCES);
			HashMap<String, String> map2 = new HashMap<String, String>();
			map2.put(StmStore.FREQUENCY, StmDbHelper.T_SUBWAY_FREQUENCES + "."
			        + StmDbHelper.T_SUBWAY_FREQUENCES_K_FREQUENCE + " AS " + StmStore.FREQUENCY);
			qb.setProjectionMap(map2);
			qb.appendWhere(StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_DIRECTION + "="
			        + uri.getPathSegments().get(1) + " AND " + StmDbHelper.T_SUBWAY_FREQUENCES + "."
			        + StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY + "='" + uri.getPathSegments().get(3) + "' AND time("
			        + StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR
			        + ", '-2 hour') <= '" + uri.getPathSegments().get(5) + "'");
			limit = "1";
			sortOrder = StmDbHelper.T_SUBWAY_FREQUENCES + "." + StmDbHelper.T_SUBWAY_FREQUENCES_K_HOUR + " DESC";
			break;
		case SEARCH:
			MyLog.v(TAG, "query>SEARCH");
			// IF simple search DO
			if (UserPreferences.getPrefDefault(getContext(), UserPreferences.PREFS_SEARCH,
			        UserPreferences.PREFS_SEARCH_DEFAULT).equals(UserPreferences.PREFS_SEARCH_SIMPLE)) {
				qb.setTables(StmDbHelper.T_BUS_STOPS);
				qb.setProjectionMap(sSearchSimpleProjectionMap);
				qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "!=''");
				if (uri.getPathSegments().size() > 1) {
					search = Uri.decode(uri.getPathSegments().get(1));
					if (!TextUtils.isEmpty(search)) {
						String[] keywords = search.split(SEARCH_SPLIT_ON);
						String inWhere = "";
						for (String keyword : keywords) {
							inWhere += " AND ";
							inWhere += "(";
							if (TextUtils.isDigitsOnly(keyword)) {
								// TODO setting for this ?
								// IF the keyword start with 5 or 6 OR the keyword length is more than 3 DO
								if (keyword.startsWith("5") || keyword.startsWith("6") || keyword.length() > 3) {
									// BUS STOP CODE
									inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE
									        + " LIKE '%" + keyword + "%'";
									inWhere += " OR ";
								}
								// BUS STOP LINE NUMBER
								inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
								        + " LIKE '%" + keyword + "%'";
								inWhere += " OR ";
							}
							// BUS STOP PLACE
							inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " LIKE '%"
							        + keyword + "%'";

							inWhere += ")";
						}
						qb.appendWhere(inWhere);
					}
				}
				// ELSE IF extended search DO
			} else {
				qb.setDistinct(true);
				qb.setTables(BUS_STOP_ALL_JOIN);
				qb.setProjectionMap(sSearchProjectionMap);
				qb.appendWhere(StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + "!=''");
				if (uri.getPathSegments().size() > 1) {
					search = Uri.decode(uri.getPathSegments().get(1));
					if (!TextUtils.isEmpty(search)) {
						String[] keywords = search.split(SEARCH_SPLIT_ON);
						String inWhere = "";
						for (String keyword : keywords) {
							inWhere += " AND ";
							inWhere += "(";
							if (TextUtils.isDigitsOnly(keyword)) {
								// BUS STOP CODE
								inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_CODE + " LIKE '%"
								        + keyword + "%'";
								inWhere += " OR ";
								// BUS STOP LINE NUMBER
								inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER
								        + " LIKE '%" + keyword + "%'";
								inWhere += " OR ";
							}
							// BUS STOP PLACE
							inWhere += StmDbHelper.T_BUS_STOPS + "." + StmDbHelper.T_BUS_STOPS_K_PLACE + " LIKE '%"
							        + keyword + "%'";
							if (!TextUtils.isDigitsOnly(keyword)) {
								inWhere += " OR ";
								// BUS STOP LINE NAME
								inWhere += StmDbHelper.T_BUS_LINES + "." + StmDbHelper.T_BUS_LINES_K_NAME + " LIKE '%"
								        + keyword + "%'";
								inWhere += " OR ";
								// BUS STOP SUBWAY STATION NAME
								inWhere += StmDbHelper.T_SUBWAY_STATIONS + "."
								        + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " LIKE '%" + keyword + "%'";
							}
							inWhere += ")";
						}
						qb.appendWhere(inWhere);
					}
				}
			}
			if (uri.getPathSegments().size() > 1 && uri.getPathSegments().get(1).length() == 0) {
				limit = String.valueOf(Constant.NB_SEARCH_RESULT);
			}
			break;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): %s", uri));
		}
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case BUS_LINES:
			case BUS_LINES_SEARCH:
			case SUBWAY_STATION_ID_BUS_LINES:
			case BUS_LINES_IDS:
			case BUS_STOP_ID_BUS_LINES:
			case BUS_LINE_ID:
			case BUS_LINES_NUMBER:
				orderBy = StmStore.BusLine.DEFAULT_SORT_ORDER;
				break;
			case BUS_LINE_ID_DIRECTION_ID_STOPS:
			case BUS_LINE_ID_DIRECTION_ID_STOPS_SEARCH:
			case BUS_STOPS:
			case BUS_STOPS_IDS:
			case BUS_STOPS_SEARCH:
			case BUS_LINE_ID_STOP_ID:
			case SUBWAY_STATION_ID_BUS_STOPS:
			case SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS:
			case BUS_STOP_ID:
				orderBy = StmStore.BusStop.DEFAULT_SORT_ORDER;
				break;
			case BUS_STOPS_CODE:
				orderBy = StmStore.BusStop.ORDER_BY_CODE;
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
			case SUBWAY_STATION_ID_LINES_OTHER:
			case SUBWAY_STATIONS_AND_LINES:
				orderBy = StmStore.SubwayStation.DEFAULT_SORT_ORDER;
				break;
			case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
			case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
			case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
			case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
			case SEARCH:
				orderBy = null;
				break;
			default:
				throw new IllegalArgumentException(String.format("Unknown URI (order): %s", uri));
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = getDBHelper().getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy, limit);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * @return the database helper
	 */
	private SQLiteOpenHelper getDBHelper() {
		if (this.mOpenHelper == null) {
			this.mOpenHelper = new StmDbHelper(getContext(), null);
		}
		return this.mOpenHelper;
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
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
		case BUS_STOPS_SEARCH:
		case SUBWAY_STATION_ID_BUS_STOPS:
		case SUBWAY_STATION_ID_BUS_LINE_ID_BUS_STOPS:
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
		case SUBWAY_STATION_ID:
			return StmStore.SubwayStation.CONTENT_ITEM_TYPE;
		case SEARCH:
			return SearchManager.SUGGEST_MIME_TYPE;
		case SUBWAY_STATION_ID_DIRECTION_ID_DAY:
		case SUBWAY_STATION_ID_DIRECTION_ID_WEEK_DAY:
		case SUBWAY_DIRECTION_ID_DAY_ID_HOUR_ID:
		case SUBWAY_DIRECTION_ID_WEEK_DAY_HOUR_ID:
		case SUBWAY_STATION_ID_LINES_OTHER:
		case SUBWAY_STATIONS_AND_LINES:
		case BUS_LINES_NUMBER:
		case BUS_STOPS_CODE:
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

package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * This manager provide methods to access STM static information about bus stops, bus lines, subway lines, subway stations. Use the content provider
 * {@link StmProvider}
 * @author Mathieu Méa
 */
public class StmManager {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmManager.class.getSimpleName();

	/**
	 * Represents the fields the content provider will return for a subway station.
	 */
	private static final String[] PROJECTION_SUBWAY_STATION = new String[] { StmStore.SubwayStation._ID,
	        StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_NAME, StmStore.SubwayStation.STATION_LNG,
	        StmStore.SubwayStation.STATION_LAT };

	/**
	 * Represents the fields the content provider will return for a bus stop.
	 */
	private static final String[] PROJECTION_BUS_STOP = new String[] { StmStore.BusStop._ID,
	        StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_DIRECTION_ID,
	        StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.STOP_SUBWAY_STATION_ID };

	/**
	 * Represents the fields the content provider will return for an extended bus stop (including bus line info).
	 */
	private static final String[] PROJECTION_BUS_STOP_EXTENDED = new String[] { StmStore.BusStop._ID,
	        StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_DIRECTION_ID,
	        StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.LINE_NAME, StmStore.BusStop.LINE_TYPE,
	        StmStore.BusStop.STOP_SUBWAY_STATION_ID };

	/**
	 * Represents the fields the content provider will return for an extended bus stop (including subway station name).
	 */
	private static final String[] PROJECTION_BUS_STOP_AND_SUBWAY_STATION = new String[] { StmStore.BusStop._ID,
	        StmStore.BusStop.STOP_CODE, StmStore.BusStop.STOP_PLACE, StmStore.BusStop.STOP_DIRECTION_ID,
	        StmStore.BusStop.STOP_LINE_NUMBER, StmStore.BusStop.STOP_SUBWAY_STATION_ID, StmStore.BusStop.STATION_NAME,
	        StmStore.BusStop.STATION_LAT, StmStore.BusStop.STATION_LNG };

	/**
	 * Represents the fields the content provider will return for a bus line direction.
	 */
	private static final String[] PROJECTION_BUS_LINE_DIRECTION = new String[] { StmStore.BusLineDirection._ID,
	        StmStore.BusLineDirection.DIRECTION_ID, StmStore.BusLineDirection.DIRECTION_LINE_ID,
	        StmStore.BusLineDirection.DIRECTION_NAME };

	/**
	 * Represents the fields the content provider will return for a subway line.
	 */
	private static final String[] PROJECTION_SUBWAY_LINE = new String[] { StmStore.SubwayLine._ID,
	        StmStore.SubwayLine.LINE_NUMBER, StmStore.SubwayLine.LINE_NAME };

	/**
	 * Represents the fields the content provider will return for a subway line + subway station.
	 */
	private static final String[] PROJECTION_SUBWAY_LINES_STATIONS = new String[] { BaseColumns._ID,
	        StmStore.SubwayLine.LINE_NUMBER, StmStore.SubwayLine.LINE_NAME, StmStore.SubwayStation.STATION_ID,
	        StmStore.SubwayStation.STATION_NAME, StmStore.SubwayStation.STATION_LNG, StmStore.SubwayStation.STATION_LAT };

	/**
	 * Represents the fields the content provider will return for a bus line.
	 */
	private static final String[] PROJECTION_BUS_LINE = new String[] { StmStore.BusLine._ID,
	        StmStore.BusLine.LINE_NUMBER, StmStore.BusLine.LINE_NAME, StmStore.BusLine.LINE_TYPE };

	/**
	 * Find a subway station from it URI.
	 * @param contentResolver the content resolver
	 * @param uri the subway station URI
	 * @return the subway station or <b>NULL</b>
	 */
	public static StmStore.SubwayStation findSubwayStation(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findSubwayStation(%s)", uri.getPath());
		StmStore.SubwayStation subwayStation = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					subwayStation = StmStore.SubwayStation.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return subwayStation;
	}

	/**
	 * Find the subway station from its ID.
	 * @see {@link StmManager#findSubwayLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the subway station
	 */
	public static StmStore.SubwayStation findSubwayStation(ContentResolver contentResolver, String subwayStationId) {
		return findSubwayStation(contentResolver,
		        Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId));
	}

	/**
	 * Find a bus line stop.
	 * @param contentResolver the content resolver.
	 * @param uri the content URI
	 * @return the bus stop
	 */
	public static StmStore.BusStop findBusStop(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusStop(%s)", uri.getPath());
		StmStore.BusStop busStop = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busStop = StmStore.BusStop.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busStop;
	}

	/**
	 * <b>WARNING</b> May not return the expected bus stop since multiple bus stop can share a stop code.
	 * @see {@link StmManager#findBusStop(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busStopCode the bus stop code.
	 * @return the first bus stop found with this stop code.
	 */
	public static StmStore.BusStop findBusStop(ContentResolver contentResolver, String busStopCode) {
		return findBusStop(contentResolver, Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopCode));
	}

	/**
	 * Find a bus stop matching the bus stop code and the bus line number.
	 * @see {@link StmManager#findBusStop(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busStopCode the bus stop code
	 * @param busLineNumber the bus line number
	 * @return a bus stop
	 */
	public static StmStore.BusStop findBusLineStop(ContentResolver contentResolver, String busStopCode,
	        String busLineNumber) {
		// MyTrace.v(TAG, "findBusLineStop("+busStopCode+", "+busLineNumber+")");
		Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
		Uri busLineStopsUri = Uri.withAppendedPath(busLineUri, StmStore.BusLine.BusStops.CONTENT_DIRECTORY);
		Uri busLineStopUri = Uri.withAppendedPath(busLineStopsUri, busStopCode);
		return findBusStop(contentResolver, busLineStopUri);
	}

	/**
	 * Find a subway line.
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findSubwayLine(%s)", uri.getPath());
		StmStore.SubwayLine subwayLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					subwayLine = StmStore.SubwayLine.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return subwayLine;
	}

	/**
	 * Find a bus line
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return a bus line
	 */
	public static StmStore.BusLine findBusLine(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusLine(%s)", uri.getPath());
		StmStore.BusLine busLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busLine = StmStore.BusLine.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busLine;
	}

	/**
	 * Find a bus line direction.
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return the bus line direction
	 */
	public static StmStore.BusLineDirection findBusLineDirection(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBusLineDirection(%s)", uri.getPath());
		StmStore.BusLineDirection busLine = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, PROJECTION_BUS_LINE_DIRECTION, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					busLine = StmStore.BusLineDirection.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return busLine;
	}

	/**
	 * Find bus line direction.
	 * @see {@link StmManager#findBusLineDirection(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busLineDirectionId the bus line direction ID
	 * @return the bus line direction
	 */
	public static StmStore.BusLineDirection findBusLineDirection(ContentResolver contentResolver,
	        String busLineDirectionId) {
		// MyTrace.v(TAG, "findBusLineDirection("+busLineDirectionId+")");
		return findBusLineDirection(contentResolver,
		        Uri.withAppendedPath(StmStore.BusLineDirection.CONTENT_URI, busLineDirectionId));
	}

	/**
	 * Find a subway line
	 * @see {@link StmManager#findSubwayLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(ContentResolver contentResolver, int subwayLineId) {
		// MyTrace.v(TAG, "findSubwayLine("+subwayLineId+")");
		return findSubwayLine(contentResolver,
		        ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineId));
	}

	/**
	 * Find a bus lines list
	 * @param contentResolver the content resolver
	 * @param busLineIds the bus lines IDs
	 * @return the bus lines list
	 */
	public static Cursor findBusLines(ContentResolver contentResolver, List<String> busLineIds) {
		MyLog.v(TAG, "findBusLine(%s)", busLineIds.size());
		String busLineIdsS = "";
		for (String busLineId : busLineIds) {
			if (busLineIdsS.length() > 0) {
				busLineIdsS += "+";
			}
			busLineIdsS += busLineId;
		}
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineIdsS),
		        PROJECTION_BUS_LINE, null, null, StmStore.BusLine.DEFAULT_SORT_ORDER);
	}

	/**
	 * Find distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops
	 */
	public static Cursor findBusStops(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStops(%s)", busStopIdsString);
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopIdsString),
		        PROJECTION_BUS_STOP, null, null, StmStore.BusStop.ORDER_BY_LINE_CODE);
	}

	/**
	 * Find a list of distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findBusStopsList(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsList(%s)", busStopIdsString);
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findBusStops(contentResolver, busStopIdsString);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No result found for bus stops '%s'", busStopIdsString);
				}
			} else {
				MyLog.w(TAG, "No result found for bus stops '%s'", busStopIdsString);
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find distinct (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops
	 */
	private static Cursor findBusStopsExtended(ContentResolver contentResolver, String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsExtended(%s)", busStopIdsString);
		return contentResolver.query(Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, busStopIdsString),
		        PROJECTION_BUS_STOP_EXTENDED, null, null, StmStore.BusStop.ORDER_BY_LINE_CODE);
	}

	/**
	 * Find a list of <b>distinct<b> (group by bus line number) extended (with bus lines info) bus stops matching the bus stop IDs.
	 * @param contentResolver the content resolver
	 * @param busStopIdsString the bus stop IDs
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findBusStopsExtendedList(ContentResolver contentResolver,
	        String busStopIdsString) {
		MyLog.v(TAG, "findBusStopsExtendedList(%s)", busStopIdsString);
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findBusStopsExtended(contentResolver, busStopIdsString);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						BusStop newBusStop = StmStore.BusStop.fromCursor(c);
						boolean alreadyInTheList = false;
						for (BusStop busStop : result) {
							if (busStop.getCode().equals(newBusStop.getCode())
							        && busStop.getLineNumber().equals(newBusStop.getLineNumber())) {
								alreadyInTheList = true;
							}
						}
						if (!alreadyInTheList) {
							result.add(newBusStop);
						}
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No result found for bus stops '%s'", busStopIdsString);
				}
			} else {
				MyLog.w(TAG, "No result found for bus stops '%s'", busStopIdsString);
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return the bus stops URI for the live folder.
	 * @param favList the favorite list
	 * @return the bus stops URI
	 */
	public static Uri getBusStopsFavUri(List<DataStore.Fav> favList) {
		MyLog.v(TAG, "getBusStopsFavUri(%s)", favList.size());
		String favIdsS = "";
		for (DataStore.Fav favId : favList) {
			if (favIdsS.length() > 0) {
				favIdsS += "+";
			}
			favIdsS += BusStop.getUID(favId.getFkId(), favId.getFkId2());
		}
		return Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI_FAV, favIdsS);
	}

	/**
	 * Find a bus line
	 * @see {@link StmManager#findBusLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param busLineId the bus line ID
	 * @return the bus line
	 */
	public static StmStore.BusLine findBusLine(ContentResolver contentResolver, String busLineId) {
		MyLog.v(TAG, "findBusLine(%s)", busLineId);
		return findBusLine(contentResolver, Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineId));
	}

	/**
	 * Find bus line directions for a bus line.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @return the bus line directions
	 */
	public static List<StmStore.BusLineDirection> findBusLineDirections(ContentResolver contentResolver,
	        String busLineNumber) {
		MyLog.v(TAG, "findBusLineDirections(%s)", busLineNumber);
		List<StmStore.BusLineDirection> result = null;
		Cursor c = null;
		try {
			c = contentResolver.query(Uri.withAppendedPath(
			        Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber),
			        StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY), PROJECTION_BUS_LINE_DIRECTION, null, null,
			        StmStore.BusLine.DEFAULT_SORT_ORDER);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLineDirection>();
					do {
						result.add(StmStore.BusLineDirection.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "Bus Line Directions is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "Bus Line Directions is EMPTY !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway line first station in this order
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway line
	 */
	public static StmStore.SubwayStation findSubwayLineFirstSubwayStation(ContentResolver contentResolver,
	        String subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineFirstSubwayStation(%s)", subwayLineId);
		StmStore.SubwayStation subwayStation = null;
		Cursor c = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = Uri.withAppendedPath(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri,
			        StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			// MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			c = contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					subwayStation = StmStore.SubwayStation.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return subwayStation;
	}

	/**
	 * Find the last subway line station in this order.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway station
	 */
	public static StmStore.SubwayStation findSubwayLineLastSubwayStation(ContentResolver contentResolver,
	        int subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineLastSubwayStation(%s)", subwayLineId);
		StmStore.SubwayStation subwayStation = null;
		Cursor c = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri,
			        StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			// MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			c = contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (c.getCount() > 0) {
				if (c.moveToLast()) {
					subwayStation = StmStore.SubwayStation.fromCursor(c);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return subwayStation;
	}

	/**
	 * Return a cursor containing all subway lines.
	 * @param contentResolver the content resolver
	 * @return the cursor
	 */
	public static Cursor findAllSubwayLines(ContentResolver contentResolver) {
		return contentResolver.query(StmStore.SubwayLine.CONTENT_URI, PROJECTION_SUBWAY_LINE, null, null, null);
	}

	/**
	 * Find all subway lines.
	 * @param contentResolver the content resolver
	 * @return the subway line list
	 */
	public static List<StmStore.SubwayLine> findAllSubwayLinesList(ContentResolver contentResolver) {
		MyLog.v(TAG, "findAllSubwayLinesList()");
		List<StmStore.SubwayLine> result = null;
		Cursor c = null;
		try {
			c = findAllSubwayLines(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayLine>();
					do {
						result.add(StmStore.SubwayLine.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return a cursor containing all subway lines.
	 * @param contentResolver the content resolver
	 * @return the cursor
	 */
	public static Cursor findAllSubwayStations(ContentResolver contentResolver) {
		return contentResolver.query(StmStore.SubwayStation.CONTENT_URI, PROJECTION_SUBWAY_STATION, null, null, null);
	}

	/**
	 * Find all subway stations.
	 * @param contentResolver the content resolver
	 * @return the subway stations list
	 */
	public static List<StmStore.SubwayStation> findAllSubwayStationsList(ContentResolver contentResolver) {
		MyLog.v(TAG, "findAllSubwayStationsList()");
		List<StmStore.SubwayStation> result = null;
		Cursor c = null;
		try {
			c = findAllSubwayStations(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayStation>();
					do {
						result.add(StmStore.SubwayStation.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Search all subway lines.
	 * @param contentResolver the content resolver
	 * @param search the keywords
	 * @return the cursor
	 */
	public static Cursor searchAllSubwayLines(ContentResolver contentResolver, String search) {
		MyLog.v(TAG, "searchAllSubwayLines(%s)", search);
		if (!TextUtils.isEmpty(search)) {
			Uri searchUri = Uri.withAppendedPath(
			        Uri.withAppendedPath(StmStore.SubwayLine.CONTENT_URI, StmStore.SEARCH_URI), Uri.encode(search));
			return contentResolver.query(searchUri, PROJECTION_SUBWAY_LINE, null, null, null);
		} else {
			return findAllSubwayLines(contentResolver);
		}
	}

	/**
	 * Find a bus stop line. <b>WARNING</b> Is this working?
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus line
	 */
	public static StmStore.BusLine findBusStopLine(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLine(%s)", stopCode);
		Uri busStopsUri = Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, stopCode);
		Uri busLinesUri = Uri.withAppendedPath(busStopsUri, StmStore.BusStop.BusLines.CONTENT_DIRECTORY);
		return findBusLine(contentResolver, busLinesUri);
	}

	/**
	 * Find bus stop lines.
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus lines cursor
	 */
	public static Cursor findBusStopLines(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLines(%s)", stopCode);
		Uri busStopsUri = Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI, stopCode);
		Uri busLinesUri = Uri.withAppendedPath(busStopsUri, StmStore.BusStop.BusLines.CONTENT_DIRECTORY);
		return contentResolver.query(busLinesUri, PROJECTION_BUS_LINE, null, null, null);
	}

	/**
	 * Return the list of the bus lines for the bus stop.
	 * @param contentResolver the content resolver
	 * @param stopCode the bus stop code
	 * @return the bus lines list
	 */
	public static List<StmStore.BusLine> findBusStopLinesList(ContentResolver contentResolver, String stopCode) {
		MyLog.v(TAG, "findBusStopLinesList(%s)", stopCode);
		List<StmStore.BusLine> result = null;
		Cursor c = null;
		try {
			c = findBusStopLines(contentResolver, stopCode);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLine>();
					do {
						result.add(StmStore.BusLine.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "No bus lines found for bus stop '%s'", stopCode);
				}
			} else {
				MyLog.w(TAG, "No bus lines found for bus stop '%s'", stopCode);
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find subway station lines list.
	 * @param contentResolver the content resolver
	 * @param stationId the subway station ID
	 * @return the subway lines
	 */
	public static List<StmStore.SubwayLine> findSubwayStationLinesList(ContentResolver contentResolver, String stationId) {
		MyLog.v(TAG, "findSubwayStationLinesList(%s)", stationId);
		List<StmStore.SubwayLine> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationLines(contentResolver, stationId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayLine>();
					do {
						result.add(StmStore.SubwayLine.fromCursor(c));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "SubwayLines is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "SubwayLines.SIZE = 0 !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return a cursor containing subway station lines.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the subway lines
	 */
	public static Cursor findSubwayStationLines(ContentResolver contentResolver, String subwayStationId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationUri,
		        StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "subwayLinesUri>" + subwayLinesUri.getPath());
		return contentResolver.query(subwayLinesUri, PROJECTION_SUBWAY_LINE, null, null, null);
	}

	/**
	 * Find all subway stations and subway lines.
	 * @param contentResolver the content resolver
	 * @return the subway stations and subway lines pair list
	 */
	public static List<Pair<SubwayLine, SubwayStation>> findSubwayStationsAndLinesList(ContentResolver contentResolver) {
		MyLog.v(TAG, "findSubwayStationsAndLinesList()");
		List<Pair<SubwayLine, SubwayStation>> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationsAndLines(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<Pair<SubwayLine, SubwayStation>>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(c);
						SubwayLine line = StmStore.SubwayLine.fromCursor(c);
						result.add(new Pair<SubwayLine, SubwayStation>(line, station));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find subway stations and subway lines pairs.
	 * @param contentResolver the content resolver
	 * @return the subway stations and subway line pairs
	 */
	public static Cursor findSubwayStationsAndLines(ContentResolver contentResolver) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationsUri,
		        StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		return contentResolver.query(subwayLinesUri, PROJECTION_SUBWAY_LINES_STATIONS, null, null, null);
	}

	/**
	 * Find subway line stations other lines.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line
	 * @return the subway line + stations
	 */
	public static List<Pair<SubwayLine, SubwayStation>> findSubwayLineStationsWithOtherLinesList(
	        ContentResolver contentResolver, int subwayLineId) {
		MyLog.v(TAG, "findSubwayStationLinesList(%s)", subwayLineId);
		List<Pair<SubwayLine, SubwayStation>> result = null;
		Cursor c = null;
		try {
			c = findSubwayLineStationsWithOtherLines(contentResolver, subwayLineId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<Pair<SubwayLine, SubwayStation>>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(c);
						SubwayLine line = StmStore.SubwayLine.fromCursor(c);
						result.add(new Pair<SubwayLine, SubwayStation>(line, station));
					} while (c.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Return a cursor with subway line stations other lines.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @return the subway line + stations
	 */
	public static Cursor findSubwayLineStationsWithOtherLines(ContentResolver contentResolver, int subwayLineId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, String.valueOf(subwayLineId));
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationUri,
		        StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		Uri otherUri = Uri.withAppendedPath(subwayLinesUri, "other");
		// MyLog.v(TAG, "otherUri>" + otherUri.getPath());
		return contentResolver.query(otherUri, PROJECTION_SUBWAY_LINES_STATIONS, null, null, null);
	}

	/**
	 * Return the subway line stations in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	public static Cursor findSubwayLineStations(ContentResolver contentResolver, int subwayLineNumber, String order) {
		Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
		Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineNumber);
		Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri,
		        StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "subwayLineStationsUri>" + subwayLineStationsUri.getPath());
		return contentResolver.query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
	}

	/**
	 * Return the subway line stations matching the subway line number and the search (subway line name).
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @param search the search
	 * @return the subway stations
	 */
	public static Cursor searchSubwayLineStations(ContentResolver contentResolver, int subwayLineNumber, String order,
	        String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri subwayLineUri = ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineNumber);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri,
			        StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			Uri searchSubwayLineStationsUri = Uri.withAppendedPath(
			        Uri.withAppendedPath(subwayLineStationsUri, StmStore.SEARCH_URI), Uri.encode(search));
			// MyLog.v(TAG, "searchSubwayLineStationsUri>" + searchSubwayLineStationsUri.getPath());
			return contentResolver.query(searchSubwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
		} else {
			return findSubwayLineStations(contentResolver, subwayLineNumber, order);
		}
	}

	/**
	 * Return the subway line stations list in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	public static List<StmStore.SubwayStation> findSubwayLineStationsList(ContentResolver contentResolver,
	        int subwayLineNumber, String order) {
		List<StmStore.SubwayStation> result = null;
		Cursor c = null;
		try {
			c = findSubwayLineStations(contentResolver, subwayLineNumber, order);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayStation>();
					do {
						result.add(StmStore.SubwayStation.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway stations with those IDs
	 * @param contentResolver the content resolver
	 * @param subwayStationIds the subway station IDs
	 * @return the subway stations
	 */
	public static Cursor findSubwayStations(ContentResolver contentResolver, List<String> subwayStationIds) {
		MyLog.v(TAG, "findSubwayStations(%s)", subwayStationIds.size());
		String subwayStationIdsS = "";
		for (String subwayStationId : subwayStationIds) {
			if (subwayStationIdsS.length() > 0) {
				subwayStationIdsS += "+";
			}
			subwayStationIdsS += subwayStationId;
		}
		Uri uri = Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationIdsS);
		return contentResolver.query(uri, PROJECTION_SUBWAY_STATION, null, null, null);
	}

	/**
	 * Find bus line stops matching the bus line number and the direction ID.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @param directionId the direction ID
	 * @return the bus stops
	 */
	public static Cursor findBusLineStops(ContentResolver contentResolver, String busLineNumber, String directionId) {
		Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
		Uri busLineDirectionsUri = Uri.withAppendedPath(busLineUri,
		        StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY);
		Uri busLineDirectionUri = Uri.withAppendedPath(busLineDirectionsUri, directionId);
		Uri busStopsUri = Uri.withAppendedPath(busLineDirectionUri,
		        StmStore.BusLine.BusLineDirections.BusStops.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "URI: " + busStopsUri.getPath());
		return contentResolver.query(busStopsUri, PROJECTION_BUS_STOP_AND_SUBWAY_STATION, null, null, null);
	}

	/**
	 * Find bus line stops matching the bus line number and the direction ID.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @param directionId the direction ID
	 * @return the bus stops
	 */
	public static List<StmStore.BusStop> findBusLineStopsList(ContentResolver contentResolver, String busLineNumber,
	        String directionId) {
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findBusLineStops(contentResolver, busLineNumber, directionId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Search bus line stops matching the bus line number and the direction ID.
	 * @param contentResolver the content resolver
	 * @param busLineNumber the bus line number
	 * @param directionId the direction ID
	 * @param search the search
	 * @return the bus stops
	 */
	public static Cursor searchBusLineStops(ContentResolver contentResolver, String busLineNumber, String directionId,
	        String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri busLineUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, busLineNumber);
			Uri busLineDirectionsUri = Uri.withAppendedPath(busLineUri,
			        StmStore.BusLine.BusLineDirections.CONTENT_DIRECTORY);
			Uri busLineDirectionUri = Uri.withAppendedPath(busLineDirectionsUri, directionId);
			Uri busStopsUri = Uri.withAppendedPath(busLineDirectionUri,
			        StmStore.BusLine.BusLineDirections.BusStops.CONTENT_DIRECTORY);
			Uri searchUri = Uri.withAppendedPath(Uri.withAppendedPath(busStopsUri, StmStore.SEARCH_URI),
			        Uri.encode(search));
			// MyLog.v(TAG, "URI: " + searchUri.getPath());
			return contentResolver.query(searchUri, PROJECTION_BUS_STOP_AND_SUBWAY_STATION, null, null, null);
		} else {
			return findBusLineStops(contentResolver, busLineNumber, directionId);
		}
	}

	/**
	 * Find subway station bus lines
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the bus lines
	 */
	public static Cursor findSubwayStationBusLines(ContentResolver contentResolver, String subwayStationId) {
		Uri subwayStationUri = Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId);
		Uri busLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.BusLines.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "busLinesUri>" + busLinesUri.getPath());
		return contentResolver.query(busLinesUri, PROJECTION_BUS_LINE, null, null, null);
	}

	/**
	 * Find subway station bus lines list
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the bus lines list
	 */
	public static List<StmStore.BusLine> findSubwayStationBusLinesList(ContentResolver contentResolver,
	        String subwayStationId) {
		List<StmStore.BusLine> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationBusLines(contentResolver, subwayStationId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLine>();
					do {
						result.add(StmStore.BusLine.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway station bus line stops.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @param busLineNumber the bus line number
	 * @return the bus stops
	 */
	private static Cursor findSubwayStationBusLineStops(ContentResolver contentResolver, String subwayStationId,
	        String busLineNumber) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri busLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.BusLines.CONTENT_DIRECTORY);
		Uri busLineUri = Uri.withAppendedPath(busLinesUri, busLineNumber);
		Uri busStopsUri = Uri.withAppendedPath(busLineUri, StmStore.SubwayStation.BusStops.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "URI>" + busStopsUri.getPath());
		return contentResolver.query(busStopsUri, PROJECTION_BUS_STOP, null, null, null);
	}

	/**
	 * Find the subway station bus line stops.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @param busLineNumber the bus line number
	 * @return the bus stops list
	 */
	public static List<StmStore.BusStop> findSubwayStationBusLineStopsList(ContentResolver contentResolver,
	        String subwayStationId, String busLineNumber) {
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationBusLineStops(contentResolver, subwayStationId, busLineNumber);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						BusStop newBusStop = StmStore.BusStop.fromCursor(c);
						boolean alreadyInTheList = false;
						for (BusStop busStop : result) {
							if (busStop.getCode().equals(newBusStop.getCode())
							        && busStop.getLineNumber().equals(newBusStop.getLineNumber())) {
								alreadyInTheList = true;
							}
						}
						if (!alreadyInTheList) {
							result.add(newBusStop);
						}
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway station extended bus stops (with bus line info)
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the extended bus stops
	 */
	public static Cursor findSubwayStationBusStopsExtended(ContentResolver contentResolver, String subwayStationId) {
		MyLog.v(TAG, "findSubwayStationBusStopsExtended(%s)", subwayStationId);
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri busStopsUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.BusStops.CONTENT_DIRECTORY);
		return contentResolver.query(busStopsUri, PROJECTION_BUS_STOP_EXTENDED, null, null,
		        StmStore.SubwayStation.BusStops.DEFAULT_SORT_ORDER);
	}

	/**
	 * Find subway station extended bus stops (with bus line info) list.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the extended bus stops list
	 */
	public static List<StmStore.BusStop> findSubwayStationBusStopsExtendedList(ContentResolver contentResolver,
	        String subwayStationId) {
		List<StmStore.BusStop> result = null;
		Cursor c = null;
		try {
			c = findSubwayStationBusStopsExtended(contentResolver, subwayStationId);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusStop>();
					do {
						result.add(StmStore.BusStop.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find all bus lines.
	 * @param contentResolver the content resolver
	 * @return the bus lines
	 */
	public static Cursor findAllBusLines(ContentResolver contentResolver) {
		return contentResolver.query(StmStore.BusLine.CONTENT_URI, PROJECTION_BUS_LINE, null, null, null);
	}

	/**
	 * Search from all bus lines.
	 * @param contentResolver the content resolver
	 * @param search
	 * @return the bus lines
	 */
	public static Cursor searchAllBusLines(ContentResolver contentResolver, String search) {
		if (!TextUtils.isEmpty(search)) {
			Uri busLineSearchUri = Uri.withAppendedPath(StmStore.BusLine.CONTENT_URI, StmStore.SEARCH_URI);
			Uri searchUri = Uri.withAppendedPath(busLineSearchUri, search);
			// MyLog.v(TAG, "searchUri>" + searchUri.getPath());
			return contentResolver.query(searchUri, PROJECTION_BUS_LINE, null, null, null);
		} else {
			return findAllBusLines(contentResolver);
		}
	}

	/**
	 * Find all bus lines list.
	 * @param contentResolver the content resolver
	 * @return the bus lines list
	 */
	public static List<StmStore.BusLine> findAllBusLinesList(ContentResolver contentResolver) {
		List<StmStore.BusLine> result = null;
		Cursor c = null;
		try {
			c = findAllBusLines(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<StmStore.BusLine>();
					do {
						result.add(StmStore.BusLine.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Reproduce the standard SEARCH.
	 * @param contentResolver the content resolver
	 * @param searchTerm the search term
	 * @return the cursor with the search results
	 */
	public static Cursor search(ContentResolver contentResolver, String searchTerm) {
		MyLog.v(TAG, "search(%s)", searchTerm);
		Uri searchQuery = Uri.withAppendedPath(StmStore.GLOBAL_SEARCH_URI, Uri.encode(searchTerm));
		return contentResolver.query(searchQuery, null, null, null, null);
	}

	/**
	 * Find distinct extended bus stop matching the search terms.
	 * @param contentResolver the content resolver
	 * @param searchTerm the search term
	 * @return the extended bus stops matching the search term
	 */
	public static Cursor searchResult(ContentResolver contentResolver, String searchTerm) {
		MyLog.v(TAG, "searchResult(%s)", searchTerm);
		Uri searchQuery = Uri.withAppendedPath(StmStore.BusStop.CONTENT_URI_SEARCH, Uri.encode(searchTerm));
		return contentResolver.query(searchQuery, PROJECTION_BUS_STOP_EXTENDED, null, null,
		        StmStore.BusStop.ORDER_BY_LINE_CODE);
	}

	/**
	 * Find the first and the last departure for a subway station.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @param directionId the direction ID
	 * @param dayOfTheWeek the day of the week - 2 hours
	 * @return the first and the last departure
	 */
	public static Pair<String, String> findSubwayStationDepartures(ContentResolver contentResolver,
	        String subwayStationId, String directionId, String dayOfTheWeek) {
		MyLog.v(TAG, "findSubwayStationDepartures(%s, %s, %s)", subwayStationId, directionId, dayOfTheWeek);
		Pair<String, String> result = null;
		Cursor c = null;
		try {
			Uri subwayStation = Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId);
			Uri subwayStationDirections = Uri.withAppendedPath(subwayStation, StmStore.DIRECTION_URI);
			Uri subwayStationDirectionId = Uri.withAppendedPath(subwayStationDirections, directionId);
			Uri subwayStationDirectionDay = Uri.withAppendedPath(subwayStationDirectionId, dayOfTheWeek);
			c = contentResolver.query(subwayStationDirectionDay, null, null, null, null);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					String first = "";
					String last = "";
					do {
						String first_last = c.getString(c.getColumnIndexOrThrow(StmStore.FIRST_LAST));
						if (first_last.equals(StmDbHelper.T_SUBWAY_HOUR_K_FIRST)) {
							first = c.getString(c.getColumnIndexOrThrow(StmStore.HOUR));
						} else {
							last = c.getString(c.getColumnIndexOrThrow(StmStore.HOUR));
						}
					} while (c.moveToNext());
					result = new Pair<String, String>(first, last);
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * Find the subway direction frequency
	 * @param contentResolver the content resolver
	 * @param directionId the direction ID
	 * @param dayOfTheWeek the day of the week - 2 hours
	 * @param hourOfTheDay the hour of the day - 2 hours
	 * @return the frequency
	 */
	public static String findSubwayDirectionFrequency(ContentResolver contentResolver, String directionId,
	        String dayOfTheWeek, String hourOfTheDay) {
		MyLog.v(TAG, "findSubwayDirectionFrequency(%s, %s, %s)", directionId, dayOfTheWeek, hourOfTheDay);
		String result = null;
		Cursor c = null;
		try {
			Uri subwayDirection = ContentUris.withAppendedId(StmStore.SUBWAY_DIRECTION_URI,
			        Integer.valueOf(directionId));
			if (dayOfTheWeek.length() > 0) {
				Uri subwayDirDays = Uri.withAppendedPath(subwayDirection, StmStore.DAY_URI);
				subwayDirection = Uri.withAppendedPath(subwayDirDays, dayOfTheWeek);
			}
			Uri subwayDirDayHours = Uri.withAppendedPath(subwayDirection, StmStore.HOUR_URI);
			Uri subwayDirDayHour = Uri.withAppendedPath(subwayDirDayHours, hourOfTheDay);

			c = contentResolver.query(subwayDirDayHour, null, null, null, null);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = c.getString(c.getColumnIndexOrThrow(StmStore.FREQUENCY));
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}
}

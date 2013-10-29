package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

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
	private static final String[] PROJECTION_SUBWAY_STATION = new String[] { StmStore.SubwayStation._ID, StmStore.SubwayStation.STATION_ID,
			StmStore.SubwayStation.STATION_NAME, StmStore.SubwayStation.STATION_LNG, StmStore.SubwayStation.STATION_LAT };

	/**
	 * Represents the fields the content provider will return for a subway line.
	 */
	private static final String[] PROJECTION_SUBWAY_LINE = new String[] { StmStore.SubwayLine._ID, StmStore.SubwayLine.LINE_NUMBER,
			StmStore.SubwayLine.LINE_NAME };

	/**
	 * Represents the fields the content provider will return for a subway line + subway station.
	 */
	private static final String[] PROJECTION_SUBWAY_LINES_STATIONS = new String[] { BaseColumns._ID, StmStore.SubwayLine.LINE_NUMBER,
			StmStore.SubwayLine.LINE_NAME, StmStore.SubwayStation.STATION_ID, StmStore.SubwayStation.STATION_NAME, StmStore.SubwayStation.STATION_LNG,
			StmStore.SubwayStation.STATION_LAT };

	private static boolean setupRequiredChecked = false;

	private static void showSetupRequiredIfNecessary(final Context context) {
		if (setupRequiredChecked) {
			return;
		}
		// 1st - check if the DB is deployed ASAP before any other call
		final boolean dbDeployed = isDbDeployed(context);
		// 2nd - check if a DB setup is required
		final boolean dbSetupRequired = isDbSetupRequired(context);
		new AsyncTask<Boolean, Void, Pair<Boolean, Boolean>>() {
			@Override
			protected Pair<Boolean, Boolean> doInBackground(Boolean... params) {
				return new Pair<Boolean, Boolean>(params[0], params[1]);
			}

			@Override
			protected void onPostExecute(Pair<Boolean, Boolean> result) {
				if (result.second) {
					final String label = findDbLabel(context);
					final boolean dbDeployed = result.first;
					// MyLog.d(TAG, "dbDeployed: " + dbDeployed);
					String messageId = context.getString(dbDeployed ? R.string.update_message_starting_and_label : R.string.init_message_starting_and_label,
							label);
					Toast toast = Toast.makeText(context, messageId, Toast.LENGTH_LONG); // need to run on UI thread with Activity or Application context
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			};
		}.execute(dbDeployed, dbSetupRequired);
		setupRequiredChecked = true;
	}

	public static boolean isDbSetupRequired(Context context) {
		MyLog.v(TAG, "isDbSetupRequired()");
		boolean result = false;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(StmStore.DB_SETUP_REQUIRED_URI, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getInt(0) > 0;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static int findDbVersion(Context context) {
		MyLog.v(TAG, "findDbVersion()");
		int result = -1;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(StmStore.DB_VERSION_URI, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getInt(0);
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static String findDbLabel(Context context) {
		MyLog.v(TAG, "findDbLabel()");
		String result = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(StmStore.DB_LABEL_URI, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(0);
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static boolean isDbDeployed(Context context) {
		MyLog.v(TAG, "isDbDeployed()");
		boolean result = false;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(StmStore.DB_DEPLOYED_URI, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getInt(0) > 0;
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	/**
	 * Find a subway station from it URI.
	 * @param contentResolver the content resolver
	 * @param uri the subway station URI
	 * @return the subway station or <b>NULL</b>
	 */
	public static StmStore.SubwayStation findSubwayStation(Context context, Uri uri) {
		MyLog.v(TAG, "findSubwayStation(%s)", uri.getPath());
		showSetupRequiredIfNecessary(context);
		StmStore.SubwayStation subwayStation = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
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
	public static StmStore.SubwayStation findSubwayStation(Context context, String subwayStationId) {
		showSetupRequiredIfNecessary(context);
		return findSubwayStation(context, Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId));
	}

	/**
	 * Find a subway line.
	 * @param contentResolver the content resolver
	 * @param uri the content URI
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(Context context, Uri uri) {
		MyLog.v(TAG, "findSubwayLine(%s)", uri.getPath());
		showSetupRequiredIfNecessary(context);
		StmStore.SubwayLine subwayLine = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
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
	 * Find a subway line
	 * @see {@link StmManager#findSubwayLine(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @return a subway line
	 */
	public static StmStore.SubwayLine findSubwayLine(Context context, int subwayLineId) {
		// MyLog.v(TAG, "findSubwayLine("+subwayLineId+")");
		showSetupRequiredIfNecessary(context);
		return findSubwayLine(context, ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineId));
	}

	/**
	 * Find the subway line first station in this order
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway line
	 */
	public static StmStore.SubwayStation findSubwayLineFirstSubwayStation(Context context, String subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineFirstSubwayStation(%s)", subwayLineId);
		showSetupRequiredIfNecessary(context);
		StmStore.SubwayStation subwayStation = null;
		Cursor cursor = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = Uri.withAppendedPath(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			// MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			cursor = context.getContentResolver().query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (cursor != null && cursor.getCount() > 0) {
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
	 * Find the last subway line station in this order.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @param sortOrder the sort order
	 * @return the subway station
	 */
	public static StmStore.SubwayStation findSubwayLineLastSubwayStation(Context context, int subwayLineId, String sortOrder) {
		MyLog.v(TAG, "findSubwayLineLastSubwayStation(%s)", subwayLineId);
		showSetupRequiredIfNecessary(context);
		StmStore.SubwayStation subwayStation = null;
		Cursor cursor = null;
		try {
			Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
			Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineId);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			// MyLog.v(TAG, "subwayLineStationUri>" + subwayLineStationsUri.getPath());
			cursor = context.getContentResolver().query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, sortOrder);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToLast()) {
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
	 * Return a cursor containing all subway lines.
	 * @param contentResolver the content resolver
	 * @return the cursor
	 */
	private static Cursor findAllSubwayLines(Context context) {
		return context.getContentResolver().query(StmStore.SubwayLine.CONTENT_URI, PROJECTION_SUBWAY_LINE, null, null, null);
	}

	/**
	 * Find all subway lines.
	 * @param contentResolver the content resolver
	 * @return the subway line list
	 */
	public static List<StmStore.SubwayLine> findAllSubwayLinesList(Context context) {
		MyLog.v(TAG, "findAllSubwayLinesList()");
		showSetupRequiredIfNecessary(context);
		List<StmStore.SubwayLine> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllSubwayLines(context);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayLine>();
					do {
						result.add(StmStore.SubwayLine.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Return a cursor containing all subway lines.
	 * @param contentResolver the content resolver
	 * @return the cursor
	 */
	private static Cursor findAllSubwayStations(Context context) {
		return context.getContentResolver().query(StmStore.SubwayStation.CONTENT_URI, PROJECTION_SUBWAY_STATION, null, null, null);
	}

	/**
	 * Find all subway stations.
	 * @param contentResolver the content resolver
	 * @return the subway stations list
	 */
	public static List<StmStore.SubwayStation> findAllSubwayStationsList(Context context) {
		MyLog.v(TAG, "findAllSubwayStationsList()");
		showSetupRequiredIfNecessary(context);
		List<StmStore.SubwayStation> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllSubwayStations(context);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayStation>();
					do {
						result.add(StmStore.SubwayStation.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Search all subway lines.
	 * @param contentResolver the content resolver
	 * @param search the keywords
	 * @return the cursor
	 */
	@SuppressWarnings("unused")
	private static Cursor searchAllSubwayLines(Context context, String search) {
		MyLog.v(TAG, "searchAllSubwayLines(%s)", search);
		if (!TextUtils.isEmpty(search)) {
			Uri searchUri = Uri.withAppendedPath(Uri.withAppendedPath(StmStore.SubwayLine.CONTENT_URI, StmStore.SEARCH_URI), Uri.encode(search));
			return context.getContentResolver().query(searchUri, PROJECTION_SUBWAY_LINE, null, null, null);
		} else {
			return findAllSubwayLines(context);
		}
	}

	/**
	 * Find subway station lines list.
	 * @param contentResolver the content resolver
	 * @param stationId the subway station ID
	 * @return the subway lines
	 */
	public static List<StmStore.SubwayLine> findSubwayStationLinesList(Context context, String stationId) {
		MyLog.v(TAG, "findSubwayStationLinesList(%s)", stationId);
		showSetupRequiredIfNecessary(context);
		List<StmStore.SubwayLine> result = null;
		Cursor cursor = null;
		try {
			cursor = findSubwayStationLines(context, stationId);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayLine>();
					do {
						result.add(StmStore.SubwayLine.fromCursor(cursor));
					} while (cursor.moveToNext());
				} else {
					MyLog.w(TAG, "SubwayLines is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "SubwayLines.SIZE = 0 !!!");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Return a cursor containing subway station lines.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @return the subway lines
	 */
	private static Cursor findSubwayStationLines(Context context, String subwayStationId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, subwayStationId);
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "subwayLinesUri>" + subwayLinesUri.getPath());
		return context.getContentResolver().query(subwayLinesUri, PROJECTION_SUBWAY_LINE, null, null, null);
	}

	/**
	 * Find all subway stations and subway lines.
	 * @param contentResolver the content resolver
	 * @return the subway stations and subway lines pair list
	 */
	public static List<Pair<SubwayLine, SubwayStation>> findSubwayStationsAndLinesList(Context context) {
		MyLog.v(TAG, "findSubwayStationsAndLinesList()");
		showSetupRequiredIfNecessary(context);
		List<Pair<SubwayLine, SubwayStation>> result = null;
		Cursor cursor = null;
		try {
			cursor = findSubwayStationsAndLines(context);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Pair<SubwayLine, SubwayStation>>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(cursor);
						SubwayLine line = StmStore.SubwayLine.fromCursor(cursor);
						result.add(new Pair<SubwayLine, SubwayStation>(line, station));
					} while (cursor.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<SubwayStation> findSubwayStationsList(Context context, String subwayStationsIds) {
		MyLog.v(TAG, "findSubwayStationsList(%s)", subwayStationsIds);
		showSetupRequiredIfNecessary(context);
		List<SubwayStation> result = null;
		Cursor cursor = null;
		try {
			cursor = findSubwayStations(context, subwayStationsIds);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<SubwayStation>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(cursor);
						result.add(station);
					} while (cursor.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Find subway stations and subway lines pairs.
	 * @param contentResolver the content resolver
	 * @return the subway stations and subway line pairs
	 */
	private static Cursor findSubwayStationsAndLines(Context context) {
		return context.getContentResolver().query(
				Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY),
				PROJECTION_SUBWAY_LINES_STATIONS, null, null, null);
	}

	/**
	 * @param contentResolver content resolver
	 * @param location the location
	 * @return all bus stops w/ location close to a location
	 */
	private static Cursor findAllSubwayStationsAndLinesLocation(Context context, double lat, double lng) {
		MyLog.v(TAG, "findAllSubwayStationsAndLinesLocation(%s, %s)", lat, lng);
		return context.getContentResolver().query(Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI_LOC, lat + "+" + lng),
				PROJECTION_SUBWAY_LINES_STATIONS, null, null, null);
	}

	/**
	 * @param contentResolver content resolver
	 * @param location the location
	 * @return all bus stops w/ location list close to a location
	 */
	public static List<Pair<SubwayLine, SubwayStation>> findAllSubwayStationsAndLinesLocationList(Context context, double lat, double lng) {
		// MyLog.v(TAG, "findAllSubwayStationsAndLinesLocationList(%s, %s)", lat, lng);
		showSetupRequiredIfNecessary(context);
		List<Pair<SubwayLine, SubwayStation>> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllSubwayStationsAndLinesLocation(context, lat, lng);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Pair<SubwayLine, SubwayStation>>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(cursor);
						SubwayLine line = StmStore.SubwayLine.fromCursor(cursor);
						result.add(new Pair<SubwayLine, SubwayStation>(line, station));
					} while (cursor.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Find subway line stations other lines.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line
	 * @return the subway line + stations
	 */
	public static List<Pair<SubwayLine, SubwayStation>> findSubwayLineStationsWithOtherLinesList(Context context, int subwayLineId) {
		MyLog.v(TAG, "findSubwayStationLinesList(%s)", subwayLineId);
		showSetupRequiredIfNecessary(context);
		List<Pair<SubwayLine, SubwayStation>> result = null;
		Cursor cursor = null;
		try {
			cursor = findSubwayLineStationsWithOtherLines(context, subwayLineId);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Pair<SubwayLine, SubwayStation>>();
					do {
						SubwayStation station = StmStore.SubwayStation.fromCursor(cursor);
						SubwayLine line = StmStore.SubwayLine.fromCursor(cursor);
						result.add(new Pair<SubwayLine, SubwayStation>(line, station));
					} while (cursor.moveToNext());
				} else {
					MyLog.w(TAG, "cursor is EMPTY !!!");
				}
			} else {
				MyLog.w(TAG, "cursor.SIZE = 0 !!!");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Return a cursor with subway line stations other lines.
	 * @param contentResolver the content resolver
	 * @param subwayLineId the subway line ID
	 * @return the subway line + stations
	 */
	private static Cursor findSubwayLineStationsWithOtherLines(Context context, int subwayLineId) {
		Uri subwayStationsUri = StmStore.SubwayStation.CONTENT_URI;
		Uri subwayStationUri = Uri.withAppendedPath(subwayStationsUri, String.valueOf(subwayLineId));
		Uri subwayLinesUri = Uri.withAppendedPath(subwayStationUri, StmStore.SubwayStation.SubwayLines.CONTENT_DIRECTORY);
		Uri otherUri = Uri.withAppendedPath(subwayLinesUri, "other");
		// MyLog.v(TAG, "otherUri>" + otherUri.getPath());
		return context.getContentResolver().query(otherUri, PROJECTION_SUBWAY_LINES_STATIONS, null, null, null);
	}

	/**
	 * Return the subway line stations in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	private static Cursor findSubwayLineStations(Context context, int subwayLineNumber, String order) {
		Uri subwayLinesUri = StmStore.SubwayLine.CONTENT_URI;
		Uri subwayLineUri = ContentUris.withAppendedId(subwayLinesUri, subwayLineNumber);
		Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
		// MyLog.v(TAG, "subwayLineStationsUri>" + subwayLineStationsUri.getPath());
		return context.getContentResolver().query(subwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
	}

	/**
	 * Return the subway line stations matching the subway line number and the search (subway line name).
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @param search the search
	 * @return the subway stations
	 */
	@SuppressWarnings("unused")
	private static Cursor searchSubwayLineStations(Context context, int subwayLineNumber, String order, String search) {
		showSetupRequiredIfNecessary(context);
		if (!TextUtils.isEmpty(search)) {
			Uri subwayLineUri = ContentUris.withAppendedId(StmStore.SubwayLine.CONTENT_URI, subwayLineNumber);
			Uri subwayLineStationsUri = Uri.withAppendedPath(subwayLineUri, StmStore.SubwayLine.SubwayStations.CONTENT_DIRECTORY);
			Uri searchSubwayLineStationsUri = Uri.withAppendedPath(Uri.withAppendedPath(subwayLineStationsUri, StmStore.SEARCH_URI), Uri.encode(search));
			// MyLog.v(TAG, "searchSubwayLineStationsUri>" + searchSubwayLineStationsUri.getPath());
			return context.getContentResolver().query(searchSubwayLineStationsUri, PROJECTION_SUBWAY_STATION, null, null, order);
		} else {
			return findSubwayLineStations(context, subwayLineNumber, order);
		}
	}

	/**
	 * Return the subway line stations list in the specified order.
	 * @param contentResolver the content resolver
	 * @param subwayLineNumber the subway line number
	 * @param order the order
	 * @return the subway stations
	 */
	public static List<StmStore.SubwayStation> findSubwayLineStationsList(Context context, int subwayLineNumber, String order) {
		showSetupRequiredIfNecessary(context);
		List<StmStore.SubwayStation> result = null;
		Cursor cursor = null;
		try {
			cursor = findSubwayLineStations(context, subwayLineNumber, order);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<StmStore.SubwayStation>();
					do {
						result.add(StmStore.SubwayStation.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * Find the subway stations with those IDs
	 * @param contentResolver the content resolver
	 * @param subwayStationIds the subway station IDs
	 * @return the subway stations
	 */
	@SuppressWarnings("unused")
	private static Cursor findSubwayStations(Context context, List<String> subwayStationIds) {
		MyLog.v(TAG, "findSubwayStations(%s)", subwayStationIds.size());
		showSetupRequiredIfNecessary(context);
		String subwayStationIdsS = "";
		for (String subwayStationId : subwayStationIds) {
			if (subwayStationIdsS.length() > 0) {
				subwayStationIdsS += "+";
			}
			subwayStationIdsS += subwayStationId;
		}
		return findSubwayStations(context, subwayStationIdsS);
	}

	private static Cursor findSubwayStations(Context context, String subwayStationIds) {
		Uri uri = Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationIds);
		return context.getContentResolver().query(uri, PROJECTION_SUBWAY_STATION, null, null, null);
	}

	/**
	 * Find the first and the last departure for a subway station.
	 * @param contentResolver the content resolver
	 * @param subwayStationId the subway station ID
	 * @param directionId the direction ID
	 * @param dayOfTheWeek the day of the week - 2 hours
	 * @return the first and the last departure
	 */
	public static Pair<String, String> findSubwayStationDepartures(Context context, String subwayStationId, String directionId, String dayOfTheWeek) {
		MyLog.v(TAG, "findSubwayStationDepartures(%s, %s, %s)", subwayStationId, directionId, dayOfTheWeek);
		showSetupRequiredIfNecessary(context);
		Pair<String, String> result = null;
		Cursor cursor = null;
		try {
			Uri subwayStation = Uri.withAppendedPath(StmStore.SubwayStation.CONTENT_URI, subwayStationId);
			Uri subwayStationDirections = Uri.withAppendedPath(subwayStation, StmStore.DIRECTION_URI);
			Uri subwayStationDirectionId = Uri.withAppendedPath(subwayStationDirections, directionId);
			Uri subwayStationDirectionDay = Uri.withAppendedPath(subwayStationDirectionId, dayOfTheWeek);
			cursor = context.getContentResolver().query(subwayStationDirectionDay, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					String first = "";
					String last = "";
					do {
						String first_last = cursor.getString(cursor.getColumnIndexOrThrow(StmStore.FIRST_LAST));
						if (first_last.equals(StmDbHelper.T_SUBWAY_HOUR_K_FIRST)) {
							first = cursor.getString(cursor.getColumnIndexOrThrow(StmStore.HOUR));
						} else {
							last = cursor.getString(cursor.getColumnIndexOrThrow(StmStore.HOUR));
						}
					} while (cursor.moveToNext());
					result = new Pair<String, String>(first, last);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
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
	public static String findSubwayDirectionFrequency(Context context, String directionId, String dayOfTheWeek, String hourOfTheDay) {
		MyLog.v(TAG, "findSubwayDirectionFrequency(%s, %s, %s)", directionId, dayOfTheWeek, hourOfTheDay);
		showSetupRequiredIfNecessary(context);
		String result = null;
		Cursor cursor = null;
		try {
			Uri subwayDirection = ContentUris.withAppendedId(StmStore.SUBWAY_DIRECTION_URI, Integer.valueOf(directionId));
			if (dayOfTheWeek.length() > 0) {
				Uri subwayDirDays = Uri.withAppendedPath(subwayDirection, StmStore.DAY_URI);
				subwayDirection = Uri.withAppendedPath(subwayDirDays, dayOfTheWeek);
			}
			Uri subwayDirDayHours = Uri.withAppendedPath(subwayDirection, StmStore.HOUR_URI);
			Uri subwayDirDayHour = Uri.withAppendedPath(subwayDirDayHours, hourOfTheDay);
			cursor = context.getContentResolver().query(subwayDirDayHour, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = cursor.getString(cursor.getColumnIndexOrThrow(StmStore.FREQUENCY));
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

}

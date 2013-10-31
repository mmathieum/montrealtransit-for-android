package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.common.RouteColumns;
import org.montrealtransit.android.provider.common.RouteTripColumns;
import org.montrealtransit.android.provider.common.RouteTripStopColumns;
import org.montrealtransit.android.provider.common.StopColumns;
import org.montrealtransit.android.provider.common.TripColumns;
import org.montrealtransit.android.provider.common.TripStopColumns;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

public class StmBusManager {

	public static final String TAG = StmBusManager.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.stmbus";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

	private static final Uri DB_VERSION_URI = Uri.withAppendedPath(CONTENT_URI, "version");
	private static final Uri DB_DEPLOYED_URI = Uri.withAppendedPath(CONTENT_URI, "deployed");
	private static final Uri DB_LABEL_URI = Uri.withAppendedPath(CONTENT_URI, "label");
	private static final Uri DB_SETUP_REQUIRED_URI = Uri.withAppendedPath(CONTENT_URI, "setuprequired");

	public static final String ROUTE_CONTENT_DIRECTORY = "route";

	public static final String TRIP_CONTENT_DIRECTORY = "trip";

	public static final String STOP_CONTENT_DIRECTORY = "stop";

	private static final Uri STOP_URI = Uri.withAppendedPath(CONTENT_URI, STOP_CONTENT_DIRECTORY);

	private static final Uri TRIP_URI = Uri.withAppendedPath(CONTENT_URI, TRIP_CONTENT_DIRECTORY);

	private static final Uri TRIP_STOP_URI = Uri.withAppendedPath(TRIP_URI, STOP_CONTENT_DIRECTORY);

	private static final Uri ROUTE_URI = Uri.withAppendedPath(CONTENT_URI, ROUTE_CONTENT_DIRECTORY);

	private static final Uri ROUTE_TRIP_URI = Uri.withAppendedPath(ROUTE_URI, TRIP_CONTENT_DIRECTORY);

	private static final Uri ROUTE_TRIP_STOP_URI = Uri.withAppendedPath(ROUTE_TRIP_URI, STOP_CONTENT_DIRECTORY);

	public static final String LOCATION_CONTENT_DIRECTORY = "location";

	private static final String[] PROJECTION_ROUTE = new String[] { RouteColumns.T_ROUTE_K_ID, RouteColumns.T_ROUTE_K_SHORT_NAME,
			RouteColumns.T_ROUTE_K_LONG_NAME, RouteColumns.T_ROUTE_K_COLOR, RouteColumns.T_ROUTE_K_TEXT_COLOR };

	private static final String[] PROJECTION_TRIP = new String[] { TripColumns.T_TRIP_K_ID, TripColumns.T_TRIP_K_HEADSIGN_TYPE,
			TripColumns.T_TRIP_K_HEADSIGN_VALUE, TripColumns.T_TRIP_K_ROUTE_ID };

	private static final String[] PROJECTION_STOP = new String[] { StopColumns.T_STOP_K_ID, StopColumns.T_STOP_K_CODE, StopColumns.T_STOP_K_NAME,
			StopColumns.T_STOP_K_LAT, StopColumns.T_STOP_K_LNG };

	private static final String[] PROJECTION_STOP_CODE = new String[] { StopColumns.T_STOP_K_CODE };

	private static final String[] PROJECTION_ROUTE_SHORT_NAME = new String[] { RouteColumns.T_ROUTE_K_SHORT_NAME };

	private static final String[] PROJECTION_ROUTE_TRIP_STOP = new String[] { RouteTripStopColumns.T_ROUTE_K_ID, RouteTripStopColumns.T_ROUTE_K_SHORT_NAME,
			RouteTripStopColumns.T_ROUTE_K_LONG_NAME, RouteTripStopColumns.T_ROUTE_K_COLOR, RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR,
			RouteTripStopColumns.T_TRIP_K_ID, RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE, RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE,
			RouteTripStopColumns.T_TRIP_K_ROUTE_ID, RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE, RouteTripStopColumns.T_STOP_K_ID,
			RouteTripStopColumns.T_STOP_K_CODE, RouteTripStopColumns.T_STOP_K_NAME, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG };

	private static final String[] PROJECTION_ROUTE_TRIP = new String[] { RouteTripColumns.T_ROUTE_K_ID, RouteTripColumns.T_ROUTE_K_SHORT_NAME,
			RouteTripColumns.T_ROUTE_K_LONG_NAME, RouteTripColumns.T_ROUTE_K_COLOR, RouteTripColumns.T_ROUTE_K_TEXT_COLOR, RouteTripColumns.T_TRIP_K_ID,
			RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE, RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE, RouteTripColumns.T_TRIP_K_ROUTE_ID };

	private static final String[] PROJECTION_TRIP_STOP = new String[] { TripStopColumns.T_TRIP_K_ID, TripStopColumns.T_TRIP_K_HEADSIGN_TYPE,
			TripStopColumns.T_TRIP_K_HEADSIGN_VALUE, TripStopColumns.T_TRIP_K_ROUTE_ID, TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE,
			TripStopColumns.T_STOP_K_ID, TripStopColumns.T_STOP_K_CODE, TripStopColumns.T_STOP_K_NAME, TripStopColumns.T_STOP_K_LAT,
			TripStopColumns.T_STOP_K_LNG };

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
			cursor = context.getContentResolver().query(DB_SETUP_REQUIRED_URI, null, null, null, null);
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
			cursor = context.getContentResolver().query(DB_VERSION_URI, null, null, null, null);
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
			cursor = context.getContentResolver().query(DB_LABEL_URI, null, null, null, null);
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
			cursor = context.getContentResolver().query(DB_DEPLOYED_URI, null, null, null, null);
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

	public static List<Route> findAllRoutesList(Context context) {
		MyLog.v(TAG, "findAllRoutesList()");
		showSetupRequiredIfNecessary(context);
		List<Route> result = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(ROUTE_URI, PROJECTION_ROUTE, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Route>();
					do {
						result.add(Route.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static Route findRoute(Context context, String routeId) {
		MyLog.v(TAG, "findRoute(%s)", routeId);
		showSetupRequiredIfNecessary(context);
		Route route = null;
		Cursor cursor = null;
		try {
			String selection = RouteColumns.T_ROUTE_K_ID + " = " + routeId;
			cursor = context.getContentResolver().query(ROUTE_URI, PROJECTION_ROUTE, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					route = Route.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return route;
	}

	public static List<TripStop> findStopsWithTripIdList(Context context, String tripId) {
		MyLog.v(TAG, "findStopsWithTripIdList(%s)", tripId);
		showSetupRequiredIfNecessary(context);
		List<TripStop> result = null;
		Cursor cursor = null;
		try {
			String selection = TripStopColumns.T_TRIP_K_ID + " = " + tripId;
			cursor = context.getContentResolver().query(TRIP_STOP_URI, PROJECTION_TRIP_STOP, selection, null,
					TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<TripStop>();
					do {
						result.add(TripStop.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<Trip> findTripsWithRouteIdList(Context context, String routeId) {
		MyLog.v(TAG, "findTripsWithRouteIdList(%s)", routeId);
		showSetupRequiredIfNecessary(context);
		List<Trip> result = null;
		Cursor cursor = null;
		try {
			String selection = TripColumns.T_TRIP_K_ROUTE_ID + " = " + routeId;
			cursor = context.getContentResolver().query(TRIP_URI, PROJECTION_TRIP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Trip>();
					do {
						result.add(Trip.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<RouteTripStop> findRouteTripStopsWithLocationList(Context context, Location location) {
		// MyLog.v(TAG, "findRouteTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		try {
			String selection = LocationUtils.genAroundWhere(location, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG);
			cursor = context.getContentResolver().query(ROUTE_TRIP_STOP_URI, PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<TripStop> findTripStopsWithLocationList(Context context, Location location) {
		// MyLog.v(TAG, "findTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		List<TripStop> result = null;
		try {
			String selection = LocationUtils.genAroundWhere(location, StopColumns.T_STOP_K_LAT, StopColumns.T_STOP_K_LNG);
			cursor = context.getContentResolver().query(TRIP_STOP_URI, PROJECTION_TRIP_STOP, selection, null,
					TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<TripStop>();
					do {
						result.add(TripStop.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<Stop> findStopsWithLocationList(Context context, Location location) {
		// MyLog.v(TAG, "findStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		List<Stop> result = null;
		try {
			String selection = LocationUtils.genAroundWhere(location, StopColumns.T_STOP_K_LAT, StopColumns.T_STOP_K_LNG);
			cursor = context.getContentResolver().query(STOP_URI, PROJECTION_STOP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Stop>();
					do {
						result.add(Stop.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<RouteTripStop> findRouteTripStopsWithLatLngList(Context context, double lat, double lng) {
		MyLog.v(TAG, "findRouteTripStopsWithLatLngList(%s,%s)", lat, lng);
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		try {
			String selection = LocationUtils.genAroundWhere(lat, lng, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG);
			cursor = context.getContentResolver().query(ROUTE_TRIP_STOP_URI, PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * @return ALL routes+trips+stops !WARNING!
	 */
	public static List<RouteTripStop> findRouteTripStopsList(Context context) {
		MyLog.v(TAG, "findRouteTripStopsList()");
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(ROUTE_TRIP_STOP_URI, PROJECTION_ROUTE_TRIP_STOP, null, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<RouteTripStop> findRouteTripStopWithStopCodeList(Context context, String stopCode) { // TODO stopID
		MyLog.v(TAG, "findRouteTripStopWithStopCodeList(%s)", stopCode);
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_CODE + " = " + stopCode;
			cursor = context.getContentResolver().query(ROUTE_TRIP_STOP_URI, PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_ROUTE_K_SHORT_NAME + ", " + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	/**
	 * @deprecated NOT WORKING
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private static Cursor findRouteTripsWithStopCode(Context context, String stopCode) {
		MyLog.v(TAG, "findRouteTripsWithStopCode(%s)", stopCode);
		showSetupRequiredIfNecessary(context);
		final Uri stopUri = Uri.withAppendedPath(STOP_URI, stopCode); // NOT WORKING!
		final Uri stopRoutesUri = Uri.withAppendedPath(stopUri, ROUTE_CONTENT_DIRECTORY);
		final Uri stopRouteTripsUri = Uri.withAppendedPath(stopRoutesUri, TRIP_CONTENT_DIRECTORY);
		return context.getContentResolver().query(stopRouteTripsUri, PROJECTION_ROUTE_TRIP, null, null, null);
	}

	public static Stop findStopWithCode(Context context, String code) {
		MyLog.v(TAG, "findStopWithCode(%s)", code);
		showSetupRequiredIfNecessary(context);
		Stop stop = null;
		Cursor cursor = null;
		try {
			String selection = StopColumns.T_STOP_K_CODE + " = " + code;
			cursor = context.getContentResolver().query(STOP_URI, PROJECTION_STOP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = Stop.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return stop;
	}

	public static Stop findStopWithId(Context context, int id) {
		showSetupRequiredIfNecessary(context);
		MyLog.v(TAG, "findStopWithId(%s)", id);
		Stop stop = null;
		Cursor cursor = null;
		try {
			String selection = StopColumns.T_STOP_K_ID + " = " + id;
			cursor = context.getContentResolver().query(STOP_URI, PROJECTION_STOP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = Stop.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return stop;
	}

	public static RouteTripStop findRouteTripStop(Context context, String stopCode, String routeShortName) { // TODO stop ID, route ID ... trip
																											 // ID ?
		MyLog.v(TAG, "findRouteTripStop(%s, %s)", stopCode, routeShortName);
		showSetupRequiredIfNecessary(context);
		RouteTripStop stop = null;
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_CODE + " = " + stopCode + " AND " + RouteTripStopColumns.T_ROUTE_K_SHORT_NAME + " = "
					+ routeShortName;
			cursor = context.getContentResolver().query(ROUTE_TRIP_STOP_URI, PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = RouteTripStop.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return stop;
	}

	public static Trip findTrip(Context context, String tripId) {
		MyLog.v(TAG, "findTrip(%s)", tripId);
		showSetupRequiredIfNecessary(context);
		Trip trip = null;
		Cursor cursor = null;
		try {
			String selection = TripColumns.T_TRIP_K_ID + " = " + tripId;
			cursor = context.getContentResolver().query(TRIP_URI, PROJECTION_TRIP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					trip = Trip.fromCursor(cursor);
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return trip;
	}

	public static List<String> findAllStopsCodeList(Context context) {
		MyLog.v(TAG, "findAllStopsCodeList()");
		showSetupRequiredIfNecessary(context);
		List<String> result = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(STOP_URI, PROJECTION_STOP_CODE, null, null, StopColumns.T_STOP_K_CODE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(cursor.getString(0));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<String> findAllRoutesShortNameList(Context context) {
		MyLog.v(TAG, "findAllStopsCodeList()");
		showSetupRequiredIfNecessary(context);
		List<String> result = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(ROUTE_URI, PROJECTION_ROUTE_SHORT_NAME, null, null, RouteColumns.T_ROUTE_K_SHORT_NAME + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						result.add(cursor.getString(0));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	public static List<RouteTripStop> findRouteTripStops(Context context, List<Fav> tripStopFavs) {
		MyLog.v(TAG, "findRouteTripStopsList()");
		showSetupRequiredIfNecessary(context);
		Cursor cursor = null;
		try {
			StringBuilder selection = null;
			for (Fav tripStopFav : tripStopFavs) {
				if (selection == null) {
					selection = new StringBuilder();
				}
				if (selection.length() > 0) {
					selection.append(" OR ");
				}
				selection.append("(");
				selection.append(RouteTripStopColumns.T_STOP_K_CODE).append(" = ").append(tripStopFav.getFkId());
				selection.append(" AND ");
				selection.append(RouteTripStopColumns.T_TRIP_K_ROUTE_ID).append(" = ").append(tripStopFav.getFkId2());
				selection.append(")");
			}
			cursor = context.getContentResolver().query(
					ROUTE_TRIP_STOP_URI,
					PROJECTION_ROUTE_TRIP_STOP,
					selection == null ? null : selection.toString(),
					null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_STOP_K_CODE + ", " + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE
							+ " ASC");
			return getRouteTripStops(cursor);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	private static List<RouteTripStop> getRouteTripStops(Cursor cursor) {
		MyLog.v(TAG, "getRouteTripStops()");
		List<RouteTripStop> result = null;
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				result = new ArrayList<RouteTripStop>();
				Set<String> uids = new HashSet<String>();
				do {
					final RouteTripStop fromCursor = RouteTripStop.fromCursor(cursor);
					if (uids.contains(fromCursor.getUID())) {
						continue; // remove duplicates (same stop + route but different trip ID)
					}
					result.add(fromCursor);
					uids.add(fromCursor.getUID());
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static Cursor search(Context context, String searchTerm) {
		showSetupRequiredIfNecessary(context);
		Uri searchQuery = Uri.withAppendedPath(Uri.withAppendedPath(CONTENT_URI, SearchManager.SUGGEST_URI_PATH_QUERY), Uri.encode(searchTerm));
		return context.getContentResolver().query(searchQuery, null, null, null, null);
	}

	public static List<RouteTripStop> searchRouteTripStopList(Context context, String searchTerm) {
		MyLog.v(TAG, "searchRouteTripStopList(%s)", searchTerm);
		showSetupRequiredIfNecessary(context);
		List<RouteTripStop> result = null;
		Cursor cursor = null;
		try {
			Uri searchQuery = Uri.withAppendedPath(ROUTE_TRIP_STOP_URI, Uri.encode(searchTerm));
			cursor = context.getContentResolver().query(searchQuery, PROJECTION_ROUTE_TRIP_STOP, null, null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_STOP_K_CODE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<RouteTripStop>();
					do {
						result.add(RouteTripStop.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

}

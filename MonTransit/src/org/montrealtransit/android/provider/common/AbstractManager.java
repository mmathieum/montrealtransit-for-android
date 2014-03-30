package org.montrealtransit.android.provider.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteStop;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmSubwayManager;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

public class AbstractManager {

	public static final String TAG = AbstractManager.class.getSimpleName();

	public static final String[] AUTHORITIES = new String[] { StmSubwayManager.AUTHORITY, StmBusManager.AUTHORITY };

	public static final String ROUTE_CONTENT_DIRECTORY = "route";

	public static final String TRIP_CONTENT_DIRECTORY = "trip";

	public static final String STOP_CONTENT_DIRECTORY = "stop";

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

	private static void showSetupRequiredIfNecessary(final Context context, final Uri contentUri) {
		if (setupRequiredChecked) {
			return;
		}
		// 1st - check if the DB is deployed ASAP before any other call
		final boolean dbDeployed = isDbDeployed(context, contentUri);
		// 2nd - check if a DB setup is required
		final boolean dbSetupRequired = isDbSetupRequired(context, contentUri);
		new AsyncTask<Boolean, Void, Pair<Boolean, Boolean>>() {
			@Override
			protected Pair<Boolean, Boolean> doInBackground(Boolean... params) {
				return new Pair<Boolean, Boolean>(params[0], params[1]);
			}

			@Override
			protected void onPostExecute(Pair<Boolean, Boolean> result) {
				if (result.second) {
					final String label = findDbLabel(context, contentUri);
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

	public static boolean isDbSetupRequired(Context context, Uri contentUri) {
		MyLog.v(TAG, "isDbSetupRequired()");
		boolean result = false;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Uri.withAppendedPath(contentUri, "setuprequired"), null, null, null, null);
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

	public static int findDbVersion(Context context, Uri contentUri) {
		MyLog.v(TAG, "findDbVersion()");
		int result = -1;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Uri.withAppendedPath(contentUri, "version"), null, null, null, null);
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

	public static String findDbLabel(Context context, Uri contentUri) {
		MyLog.v(TAG, "findDbLabel()");
		String result = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Uri.withAppendedPath(contentUri, "label"), null, null, null, null);
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

	public static boolean isDbDeployed(Context context, Uri contentUri) {
		MyLog.v(TAG, "isDbDeployed()");
		boolean result = false;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Uri.withAppendedPath(contentUri, "deployed"), null, null, null, null);
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

	public static List<Route> findAllRoutesList(Context context, Uri contentUri) {
		MyLog.v(TAG, "findAllRoutesList()");
		showSetupRequiredIfNecessary(context, contentUri);
		List<Route> result = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(getRouteUri(contentUri), PROJECTION_ROUTE, null, null, null);
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

	public static List<Route> findRoutesWithStopIdList(Context context, Uri contentUri, int stopId) {
		MyLog.v(TAG, "findRoutesWithStopIdList(%s)", stopId);
		showSetupRequiredIfNecessary(context, contentUri);
		List<Route> result = null;
		Cursor cursor = null;
		try {
			final String selection = RouteTripStopColumns.T_STOP_K_ID + "=" + stopId;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Route>();
					do {
						result.add(RouteTripStop.fromCursor(cursor, contentUri.getAuthority()).route);
					} while (cursor.moveToNext());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	public static Uri getRouteUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, ROUTE_CONTENT_DIRECTORY);
	}

	public static Route findRoute(Context context, Uri contentUri, Integer routeId) {
		MyLog.v(TAG, "findRoute(%s)", routeId);
		showSetupRequiredIfNecessary(context, contentUri);
		Route route = null;
		Cursor cursor = null;
		try {
			String selection = RouteColumns.T_ROUTE_K_ID + " = " + routeId;
			cursor = context.getContentResolver().query(getRouteUri(contentUri), PROJECTION_ROUTE, selection, null, null);
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

	public static Route findRouteWithShortName(Context context, Uri contentUri, String routeShortName) {
		MyLog.v(TAG, "findRouteWithShortName(%s)", routeShortName);
		showSetupRequiredIfNecessary(context, contentUri);
		Route route = null;
		Cursor cursor = null;
		try {
			String selection = RouteColumns.T_ROUTE_K_SHORT_NAME + " = " + routeShortName;
			cursor = context.getContentResolver().query(getRouteUri(contentUri), PROJECTION_ROUTE, selection, null, null);
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

	public static List<TripStop> findStopsWithTripIdList(Context context, Uri contentUri, Integer tripId) {
		MyLog.v(TAG, "findStopsWithTripIdList(%s)", tripId);
		showSetupRequiredIfNecessary(context, contentUri);
		List<TripStop> result = null;
		Cursor cursor = null;
		try {
			String selection = TripStopColumns.T_TRIP_K_ID + " = " + tripId;
			cursor = context.getContentResolver().query(getTripStopUri(contentUri), PROJECTION_TRIP_STOP, selection, null,
					TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<TripStop>();
					do {
						result.add(TripStop.fromCursor(cursor, contentUri.getAuthority()));
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

	public static Uri getTripStopUri(Uri contentUri) {
		return Uri.withAppendedPath(getTripUri(contentUri), STOP_CONTENT_DIRECTORY);
	}

	public static List<Trip> findTripsWithRouteIdList(Context context, Uri contentUri, Integer routeId) {
		MyLog.v(TAG, "findTripsWithRouteIdList(%s)", routeId);
		showSetupRequiredIfNecessary(context, contentUri);
		List<Trip> result = null;
		Cursor cursor = null;
		try {
			String selection = TripColumns.T_TRIP_K_ROUTE_ID + " = " + routeId;
			cursor = context.getContentResolver().query(getTripUri(contentUri), PROJECTION_TRIP, selection, null, null);
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

	public static Uri getTripUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, TRIP_CONTENT_DIRECTORY);
	}

	public static List<RouteTripStop> findRouteTripStopsWithLocationList(Context context, Uri contentUri, Location location, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		List<RouteTripStop> routeTripStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteTripStopsWithLocationList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeTripStops = findRouteTripStopsWithLocationList(context, contentUri, location, aroundDiff, filterByUID);
			// update route trip stops distance
			LocationUtils.updateDistance(routeTripStops, location.getLatitude(), location.getLongitude());
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(location.getLatitude(), location.getLongitude(), aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeTripStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeTripStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteTripStopsWithLocationList() > result count: %s", (routeTripStops == null ? null : routeTripStops.size()));
		return routeTripStops;
	}

	private static List<RouteTripStop> findRouteTripStopsWithLocationList(Context context, Uri contentUri, Location location, double aroundDiff,
			boolean filterByUID) {
		// MyLog.v(TAG, "findRouteTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			String selection = LocationUtils.genAroundWhere(location, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG, aroundDiff);
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<RouteStop> findRouteStopsWithLocationList(Context context, Uri contentUri, Location location, boolean filterByUID) {
		MyLog.v(TAG, "findRouteStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		List<RouteStop> routeStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteStopsWithLocationList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeStops = findRouteStopsWithLocationList(context, contentUri, location, aroundDiff, filterByUID);
			// update route trip stops distance
			LocationUtils.updateDistance(routeStops, location.getLatitude(), location.getLongitude());
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(location.getLatitude(), location.getLongitude(), aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteStopsWithLocationList() > result count: %s", (routeStops == null ? null : routeStops.size()));
		return routeStops;
	}

	private static List<RouteStop> findRouteStopsWithLocationList(Context context, Uri contentUri, Location location, double aroundDiff, boolean filterByUID) {
		MyLog.v(TAG, "findRouteStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			final String selection = LocationUtils.genAroundWhere(location, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG, aroundDiff);
			final String sortOrder = /* RouteTripColumns.T_ROUTE_K_ID + ", " + */RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC";
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null, sortOrder);
			return getRouteStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static Uri getRouteTripStopUri(Uri contentUri) {
		return Uri.withAppendedPath(Uri.withAppendedPath(getRouteUri(contentUri), TRIP_CONTENT_DIRECTORY), STOP_CONTENT_DIRECTORY);
	}

	// private static List<TripStop> findTripStopsWithLocationList(Context context, Uri contentUri, Location location, double aroundDiff) {
	// // MyLog.v(TAG, "findTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
	// showSetupRequiredIfNecessary(context, contentUri);
	// Cursor cursor = null;
	// List<TripStop> result = null;
	// try {
	// String selection = LocationUtils.genAroundWhere(location, StopColumns.T_STOP_K_LAT, StopColumns.T_STOP_K_LNG, aroundDiff);
	// cursor = context.getContentResolver().query(getTripStopUri(contentUri), PROJECTION_TRIP_STOP, selection, null,
	// TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
	// if (cursor != null && cursor.getCount() > 0) {
	// if (cursor.moveToFirst()) {
	// result = new ArrayList<TripStop>();
	// do {
	// result.add(TripStop.fromCursor(cursor, contentUri.getAuthority()));
	// } while (cursor.moveToNext());
	// }
	// }
	// } catch (Throwable t) {
	// MyLog.w(TAG, t, "Error!");
	// } finally {
	// if (cursor != null)
	// cursor.close();
	// }
	// return result;
	// }

	// private static List<Stop> findStopsWithLocationList(Context context, Uri contentUri, Location location, double aroundDiff) {
	// // MyLog.v(TAG, "findStopsWithLocationList(%s)", LocationUtils.locationToString(location));
	// showSetupRequiredIfNecessary(context, contentUri);
	// Cursor cursor = null;
	// List<Stop> result = null;
	// try {
	// String selection = LocationUtils.genAroundWhere(location, StopColumns.T_STOP_K_LAT, StopColumns.T_STOP_K_LNG, aroundDiff);
	// cursor = context.getContentResolver().query(getStopUri(contentUri), PROJECTION_STOP, selection, null, null);
	// if (cursor != null && cursor.getCount() > 0) {
	// if (cursor.moveToFirst()) {
	// result = new ArrayList<Stop>();
	// do {
	// result.add(Stop.fromCursor(cursor));
	// } while (cursor.moveToNext());
	// }
	// }
	// } catch (Throwable t) {
	// MyLog.w(TAG, t, "Error!");
	// } finally {
	// if (cursor != null)
	// cursor.close();
	// }
	// return result;
	// }

	private static Uri getStopUri(Uri contentUri) {
		return Uri.withAppendedPath(contentUri, STOP_CONTENT_DIRECTORY);
	}

	public static List<RouteStop> findRouteStopsWithLocationList(Context context, String[] authorities, Location location, boolean filterByUID) {
		MyLog.v(TAG, "findRouteStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		List<RouteStop> routeStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteStopsWithLocationList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeStops = new ArrayList<RouteStop>();
			for (String authority : authorities) {
				final List<RouteStop> newRouteStops = findRouteStopsWithLocationList(context, Utils.newContentUri(authority), location, aroundDiff, filterByUID);
				if (newRouteStops != null) {
					routeStops.addAll(newRouteStops);
				}
			}
			// update route trip stops distance
			LocationUtils.updateDistance(routeStops, location.getLatitude(), location.getLongitude());
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(location.getLatitude(), location.getLongitude(), aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteStopsWithLocationList() > result count: %s", (routeStops == null ? null : routeStops.size()));
		return routeStops;
	}

	public static List<RouteStop> findRouteStopsWithLatLngList(Context context, String[] authorities, double lat, double lng, boolean filterByUID) {
		MyLog.v(TAG, "findRouteStopsWithLatLngList(%s,%s)", lat, lng);
		List<RouteStop> routeStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteStopsWithLocationList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeStops = new ArrayList<RouteStop>();
			for (String authority : authorities) {
				final List<RouteStop> newRouteStops = findRouteStopsWithLatLngList(context, Utils.newContentUri(authority), lat, lng, aroundDiff, filterByUID);
				if (newRouteStops != null) {
					routeStops.addAll(newRouteStops);
				}
			}
			// update route trip stops distance
			LocationUtils.updateDistance(routeStops, lat, lng);
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(lat, lng, aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteStopsWithLocationList() > result count: %s", (routeStops == null ? null : routeStops.size()));
		return routeStops;
	}

	private static List<RouteStop> findRouteStopsWithLatLngList(Context context, Uri contentUri, double lat, double lng, double aroundDiff, boolean filterByUID) {
		// MyLog.v(TAG, "findRouteStopsWithLatLngList(%s,%s)", lat, lng);
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			String selection = LocationUtils.genAroundWhere(lat, lng, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG, aroundDiff);
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<RouteTripStop> findRouteTripStopsWithLocationList(Context context, String[] authorities, Location location, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsWithLocationList(%s)", LocationUtils.locationToString(location));
		List<RouteTripStop> routeTripStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteTripStopsWithLocationList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeTripStops = new ArrayList<RouteTripStop>();
			for (String authority : authorities) {
				final List<RouteTripStop> newRouteTripStops = findRouteTripStopsWithLocationList(context, Utils.newContentUri(authority), location, aroundDiff,
						filterByUID);
				if (newRouteTripStops != null) {
					routeTripStops.addAll(newRouteTripStops);
				}
			}
			// update route trip stops distance
			LocationUtils.updateDistance(routeTripStops, location.getLatitude(), location.getLongitude());
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(location.getLatitude(), location.getLongitude(), aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeTripStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeTripStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteTripStopsWithLocationList() > result count: %s", (routeTripStops == null ? null : routeTripStops.size()));
		return routeTripStops;
	}

	public static List<RouteTripStop> findRouteTripStopsWithLatLngList(Context context, String[] authorities, double lat, double lng, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsWithLatLngList(%s,%s)", lat, lng);
		List<RouteTripStop> routeTripStops = null;
		double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		do {
			// MyLog.d(TAG, "findRouteTripStopsWithLatLngList() > try with: %s", aroundDiff);
			// load route trip stops with location (square)
			routeTripStops = new ArrayList<RouteTripStop>();
			for (String authority : authorities) {
				final List<RouteTripStop> newRouteTripStops = findRouteTripStopsWithLatLngList(context, Utils.newContentUri(authority), lat, lng, aroundDiff,
						filterByUID);
				if (newRouteTripStops != null) {
					routeTripStops.addAll(newRouteTripStops);
				}
			}
			// update route trip stops distance
			LocationUtils.updateDistance(routeTripStops, lat, lng);
			// find maximum distance covered (circle)
			float maxDistance = LocationUtils.getAroundCoveredDistance(lat, lng, aroundDiff);
			// remove items out of the covered circle
			LocationUtils.removeTooFar(routeTripStops, maxDistance);
			// increment around for next try
			aroundDiff += LocationUtils.INC_AROUND_DIFF;
		} while (Utils.getCollectionSize(routeTripStops) <= Utils.MIN_NEARBY_LIST && aroundDiff < LocationUtils.MAX_AROUND_DIFF);
		// MyLog.d(TAG, "findRouteTripStopsWithLatLngList() > result count: %s", (routeTripStops == null ? null : routeTripStops.size()));
		return routeTripStops;
	}

	private static List<RouteTripStop> findRouteTripStopsWithLatLngList(Context context, Uri contentUri, double lat, double lng, double aroundDiff,
			boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsWithLatLngList(%s,%s)", lat, lng);
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			final String selection = LocationUtils.genAroundWhere(lat, lng, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG, aroundDiff);
			final String sortOrder = RouteTripStopColumns.T_TRIP_K_ROUTE_ID + "," + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC";
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null, sortOrder);
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	// private static List<RouteStop> findRouteStopsWithLatLngList(Context context, Uri contentUri, double lat, double lng, double AROUND_DIFF, boolean
	// filterByUID) {
	// MyLog.v(TAG, "findRouteStopsWithLatLngList(%s,%s)", lat, lng);
	// showSetupRequiredIfNecessary(context, contentUri);
	// Cursor cursor = null;
	// try {
	// String selection = LocationUtils.genAroundWhere(lat, lng, RouteTripStopColumns.T_STOP_K_LAT, RouteTripStopColumns.T_STOP_K_LNG, AROUND_DIFF);
	// cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
	// RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
	// return getRouteStops(cursor, contentUri.getAuthority(), filterByUID);
	// } catch (Throwable t) {
	// MyLog.w(TAG, t, "Error!");
	// return null;
	// } finally {
	// if (cursor != null)
	// cursor.close();
	// }
	// }

	/**
	 * @return ALL routes+trips+stops !WARNING!
	 */
	public static List<RouteTripStop> findRouteTripStopsList(Context context, Uri contentUri, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsList()");
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, null, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	@Deprecated
	public static List<RouteTripStop> findRouteTripStopWithStopCodeList(Context context, Uri contentUri, String stopCode, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopWithStopCodeList(%s)", stopCode);
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_CODE + " = " + stopCode;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			// should be RouteTripStopColumns.T_ROUTE_K_SHORT_NAME but text sorting doesn't work well with integers
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<RouteTripStop> findRouteTripStopWithStopIdList(Context context, Uri contentUri, Integer stopId, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopWithStopIdList(%s)", stopId);
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_ID + " = " + stopId;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			// TODO ERROR: Cannot perform this operation because the connection pool has been closed.
			// should be RouteTripStopColumns.T_ROUTE_K_SHORT_NAME but text sorting doesn't work well with integers
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public static List<RouteTripStop> findRouteTripStopWithStopIdList(Context context, Uri contentUri, Integer stopId, Integer routeId, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopWithStopIdList(%s, %s)", stopId, routeId);
		showSetupRequiredIfNecessary(context, contentUri);
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_ID + " = " + stopId + " AND " + RouteTripStopColumns.T_ROUTE_K_ID + " = " + routeId;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			// should be RouteTripStopColumns.T_ROUTE_K_SHORT_NAME but text sorting doesn't work well with integers
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
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
	private static Cursor findRouteTripsWithStopCode(Context context, Uri contentUri, String stopCode) {
		MyLog.v(TAG, "findRouteTripsWithStopCode(%s)", stopCode);
		showSetupRequiredIfNecessary(context, contentUri);
		final Uri stopUri = Uri.withAppendedPath(getStopUri(contentUri), stopCode); // NOT WORKING!
		final Uri stopRoutesUri = Uri.withAppendedPath(stopUri, ROUTE_CONTENT_DIRECTORY);
		final Uri stopRouteTripsUri = Uri.withAppendedPath(stopRoutesUri, TRIP_CONTENT_DIRECTORY);
		return context.getContentResolver().query(stopRouteTripsUri, PROJECTION_ROUTE_TRIP, null, null, null);
	}

	public static Stop findStopWithCode(Context context, Uri contentUri, String code) {
		MyLog.v(TAG, "findStopWithCode(%s)", code);
		showSetupRequiredIfNecessary(context, contentUri);
		Stop stop = null;
		Cursor cursor = null;
		try {
			String selection = StopColumns.T_STOP_K_CODE + " = " + code;
			cursor = context.getContentResolver().query(getStopUri(contentUri), PROJECTION_STOP, selection, null, null);
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

	public static Stop findStopWithId(Context context, Uri contentUri, Integer stopId) {
		MyLog.v(TAG, "findStopWithId(%s)", stopId);
		showSetupRequiredIfNecessary(context, contentUri);
		Stop stop = null;
		Cursor cursor = null;
		try {
			String selection = StopColumns.T_STOP_K_ID + " = " + stopId;
			cursor = context.getContentResolver().query(getStopUri(contentUri), PROJECTION_STOP, selection, null, null);
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

	public static TripStop findTripLastTripStop(Context context, Uri contentUri, Integer tripId) {
		MyLog.v(TAG, "findTripLastTripStop(%s)", tripId);
		showSetupRequiredIfNecessary(context, contentUri);
		TripStop tripStop = null;
		Cursor cursor = null;
		try {
			String selection = TripStopColumns.T_TRIP_K_ID + " = " + tripId;
			String sortOrder = TripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " DESC";
			cursor = context.getContentResolver().query(getTripStopUri(contentUri), PROJECTION_TRIP_STOP, selection, null, sortOrder);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					tripStop = TripStop.fromCursor(cursor, contentUri.getAuthority());
				}
			}
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return tripStop;
	}

	// public static Stop findStopWithId(Context context, Uri contentUri, int id) {
	// showSetupRequiredIfNecessary(context, contentUri);
	// MyLog.v(TAG, "findStopWithId(%s)", id);
	// Stop stop = null;
	// Cursor cursor = null;
	// try {
	// String selection = StopColumns.T_STOP_K_ID + " = " + id;
	// cursor = context.getContentResolver().query(getStopUri(contentUri), PROJECTION_STOP, selection, null, null);
	// if (cursor != null && cursor.getCount() > 0) {
	// if (cursor.moveToFirst()) {
	// stop = Stop.fromCursor(cursor);
	// }
	// }
	// } catch (Throwable t) {
	// MyLog.w(TAG, t, "Error!");
	// } finally {
	// if (cursor != null)
	// cursor.close();
	// }
	// return stop;
	// }

	@Deprecated
	public static RouteTripStop findRouteTripStop(Context context, Uri contentUri, String stopCode, String routeShortName) {
		MyLog.v(TAG, "findRouteTripStop(%s, %s)", stopCode, routeShortName);
		showSetupRequiredIfNecessary(context, contentUri);
		RouteTripStop stop = null;
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_CODE + " = " + stopCode + " AND " + RouteTripStopColumns.T_ROUTE_K_SHORT_NAME + " = "
					+ routeShortName;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = RouteTripStop.fromCursor(cursor, contentUri.getAuthority());
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

	public static RouteTripStop findRouteTripStop(Context context, Uri contentUri, Integer stopId, Integer routeId) {
		MyLog.v(TAG, "findRouteTripStop(%s, %s)", stopId, routeId);
		showSetupRequiredIfNecessary(context, contentUri);
		RouteTripStop stop = null;
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_ID + " = " + stopId + " AND " + RouteTripStopColumns.T_ROUTE_K_ID + " = " + routeId;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = RouteTripStop.fromCursor(cursor, contentUri.getAuthority());
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

	public static RouteTripStop findRouteTripStop(Context context, Uri contentUri, Integer stopId, Integer routeId, Integer tripId) {
		MyLog.v(TAG, "findRouteTripStop(%s, %s, %s)", stopId, routeId, tripId);
		showSetupRequiredIfNecessary(context, contentUri);
		RouteTripStop stop = null;
		Cursor cursor = null;
		try {
			String selection = RouteTripStopColumns.T_STOP_K_ID + " = " + stopId + " AND " + RouteTripStopColumns.T_ROUTE_K_ID + " = " + routeId + " AND "
					+ RouteTripStopColumns.T_TRIP_K_ID + " = " + tripId;
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP, selection, null,
					RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC");
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					stop = RouteTripStop.fromCursor(cursor, contentUri.getAuthority());
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

	public static Trip findTrip(Context context, Uri contentUri, Integer tripId) {
		MyLog.v(TAG, "findTrip(%s)", tripId);
		showSetupRequiredIfNecessary(context, contentUri);
		Trip trip = null;
		Cursor cursor = null;
		try {
			String selection = TripColumns.T_TRIP_K_ID + " = " + tripId;
			cursor = context.getContentResolver().query(getTripUri(contentUri), PROJECTION_TRIP, selection, null, null);
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

	public static List<String> findAllStopsCodeList(Context context, Uri contentUri) {
		MyLog.v(TAG, "findAllStopsCodeList()");
		showSetupRequiredIfNecessary(context, contentUri);
		List<String> result = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(getStopUri(contentUri), PROJECTION_STOP_CODE, null, null, StopColumns.T_STOP_K_CODE + " ASC");
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

	public static List<String> findAllRoutesShortNameList(Context context, Uri contentUri) {
		MyLog.v(TAG, "findAllStopsCodeList()");
		showSetupRequiredIfNecessary(context, contentUri);
		List<String> result = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(getRouteUri(contentUri), PROJECTION_ROUTE_SHORT_NAME, null, null,
					RouteColumns.T_ROUTE_K_SHORT_NAME + " ASC");
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

	public static List<RouteTripStop> findRouteTripStops(Context context, Uri contentUri, List<Fav> tripStopFavs, boolean filterByUID) {
		MyLog.v(TAG, "findRouteTripStopsList()");
		showSetupRequiredIfNecessary(context, contentUri);
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
				final int stopId = TripStop.getStopIdFromUID(tripStopFav.getFkId());
				final int routeId = TripStop.getRouteIdFromUID(tripStopFav.getFkId());
				if (stopId < 0 || routeId < 0) {
					continue; // invalid favorite
				}
				selection.append("(");
				selection.append(RouteTripStopColumns.T_STOP_K_ID).append(" = ").append(stopId);
				selection.append(" AND ");
				selection.append(RouteTripStopColumns.T_TRIP_K_ROUTE_ID).append(" = ").append(routeId);
				selection.append(")");
			}
			if (selection == null || selection.length() == 0) {
				return null; // no favorites
			}
			final String sortOrder = RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_STOP_K_CODE + ", "
			// + RouteTripStopColumns.T_STOP_K_NAME + ", "
					+ RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE + " ASC";
			cursor = context.getContentResolver().query(getRouteTripStopUri(contentUri), PROJECTION_ROUTE_TRIP_STOP,
					selection == null ? null : selection.toString(), null, sortOrder);
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private static List<RouteTripStop> getRouteTripStops(Cursor cursor, String authority, boolean filterByUID) {
		MyLog.v(TAG, "getRouteTripStops()");
		List<RouteTripStop> result = new ArrayList<RouteTripStop>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				Set<String> uids = new HashSet<String>();
				do {
					final RouteTripStop fromCursor = RouteTripStop.fromCursor(cursor, authority);
					if (filterByUID && uids.contains(fromCursor.getUID())) {
						continue; // remove duplicates (same stop + route but different trip ID)
					}
					result.add(fromCursor);
					uids.add(fromCursor.getUID());
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	private static List<RouteStop> getRouteStops(Cursor cursor, String authority, boolean filterByUID) {
		MyLog.v(TAG, "getRouteStops()");
		List<RouteStop> result = new ArrayList<RouteStop>();
		if (cursor != null && cursor.getCount() > 0) {
			if (cursor.moveToFirst()) {
				result = new ArrayList<RouteStop>();
				Set<String> uids = new HashSet<String>();
				do {
					final RouteStop fromCursor = RouteStop.fromCursor(cursor, authority);
					if (filterByUID && uids.contains(fromCursor.getUID())) {
						continue; // remove duplicates (same stop + route but different trip ID)
					}
					result.add(fromCursor);
					uids.add(fromCursor.getUID());
				} while (cursor.moveToNext());
			}
		}
		return result;
	}

	public static Cursor search(Context context, Uri contentUri, String searchTerm) {
		showSetupRequiredIfNecessary(context, contentUri);
		Uri searchQuery = Uri.withAppendedPath(Uri.withAppendedPath(contentUri, SearchManager.SUGGEST_URI_PATH_QUERY), Uri.encode(searchTerm));
		return context.getContentResolver().query(searchQuery, null, null, null, null);
	}

	/**
	 * From all {@link AbstractManager#AUTHORITIES}!
	 */
	public static List<RouteTripStop> searchRouteTripStopList(Context context, String searchTerm, boolean filterByUID) {
		MyLog.v(TAG, "searchRouteTripStopList(%s)", searchTerm);
		List<RouteTripStop> result = new ArrayList<RouteTripStop>();
		for (String authority : AUTHORITIES) {
			result.addAll(searchRouteTripStopList(context, Utils.newContentUri(authority), searchTerm, filterByUID));
		}
		return result;
	}

	public static List<RouteTripStop> searchRouteTripStopList(Context context, Uri contentUri, String searchTerm, boolean filterByUID) {
		MyLog.v(TAG, "searchRouteTripStopList(%s)", searchTerm);
		showSetupRequiredIfNecessary(context, contentUri);
		List<RouteTripStop> result = null;
		Cursor cursor = null;
		try {
			Uri searchQuery = Uri.withAppendedPath(getRouteTripStopUri(contentUri), Uri.encode(searchTerm));
			cursor = context.getContentResolver().query(searchQuery, PROJECTION_ROUTE_TRIP_STOP, null, null,
					RouteTripStopColumns.T_ROUTE_K_ID + ", " + RouteTripStopColumns.T_STOP_K_CODE + " ASC");
			return getRouteTripStops(cursor, contentUri.getAuthority(), filterByUID);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}
}

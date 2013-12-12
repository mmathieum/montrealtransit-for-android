package org.montrealtransit.android.provider;

import java.util.List;

import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.content.Context;
import android.location.Location;
import android.net.Uri;

/**
 * Bus stops are grouped by route (trip ignored, usually just 1 per stop).
 */
public class StmBusManager extends AbstractManager {

	public static final String TAG = StmBusManager.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.stmbus";

	public static final Uri CONTENT_URI = Utils.newContentUri(AUTHORITY);

	public static List<TripStop> findStopsWithTripIdList(Context context, Integer tripId) {
		return findStopsWithTripIdList(context, CONTENT_URI, tripId);
	}

	public static Route findRoute(Context context, Integer routeId) {
		return findRoute(context, CONTENT_URI, routeId);
	}

	public static Route findRouteWithShortName(Context context, String routeShortName) {
		return findRouteWithShortName(context, CONTENT_URI, routeShortName);
	}

	public static List<Route> findRoutesWithStopIdList(Context context, int stopId) {
		return findRoutesWithStopIdList(context, CONTENT_URI, stopId);
	}

	public static List<Trip> findTripsWithRouteIdList(Context context, Integer routeId) {
		return findTripsWithRouteIdList(context, CONTENT_URI, routeId);
	}

	public static List<Route> findAllRoutesList(Context context) {
		return findAllRoutesList(context, CONTENT_URI);
	}

	public static Trip findTrip(Context context, Integer tripId) {
		return findTrip(context, CONTENT_URI, tripId);
	}

	public static List<String> findAllStopsCodeList(Context context) {
		return findAllStopsCodeList(context, CONTENT_URI);
	}

	public static List<String> findAllRoutesShortNameList(Context context) {
		return findAllRoutesShortNameList(context, CONTENT_URI);
	}

	@Deprecated
	public static List<RouteTripStop> findRouteTripStopWithStopCodeList(Context context, String stopCode, boolean filterByUID) {
		return findRouteTripStopWithStopCodeList(context, CONTENT_URI, stopCode, filterByUID);
	}

	public static Stop findStopWithCode(Context context, String code) {
		return findStopWithCode(context, CONTENT_URI, code);
	}

	@Deprecated
	public static RouteTripStop findRouteTripStop(Context context, String stopCode, String routeShortName) {
		return findRouteTripStop(context, CONTENT_URI, stopCode, routeShortName);
	}

	public static List<RouteTripStop> findRouteTripStops(Context context, List<Fav> tripStopFavs, boolean filterByUID) {
		return findRouteTripStops(context, CONTENT_URI, tripStopFavs, filterByUID);
	}

	public static List<RouteTripStop> searchRouteTripStopList(Context context, String searchTerm, boolean filterByUID) {
		return searchRouteTripStopList(context, CONTENT_URI, searchTerm, filterByUID);
	}

	public static List<RouteTripStop> findRouteTripStopsWithLocationList(Context context, Location location, boolean filterByUID) {
		return findRouteTripStopsWithLocationList(context, CONTENT_URI, location, filterByUID);
	}

	public static List<RouteTripStop> findRouteTripStopsList(Context context, boolean filterByUID) {
		return findRouteTripStopsList(context, CONTENT_URI, filterByUID);
	}

}

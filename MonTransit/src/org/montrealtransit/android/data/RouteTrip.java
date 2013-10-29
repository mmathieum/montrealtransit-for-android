package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.RouteTripColumns;

import android.database.Cursor;

public class RouteTrip {

	public Route route;

	public Trip trip;

	public RouteTrip(Route route, Trip trip) {
		this.route = route;
		this.trip = trip;
	}

	public static RouteTrip fromCursor(Cursor c) {
		final Route route = new Route();
		route.id = c.getInt(c.getColumnIndexOrThrow(RouteTripColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteTripColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteTripColumns.T_ROUTE_K_LONG_NAME));
		route.color = c.getString(c.getColumnIndexOrThrow(RouteTripColumns.T_ROUTE_K_COLOR));
		route.textColor = c.getString(c.getColumnIndexOrThrow(RouteTripColumns.T_ROUTE_K_TEXT_COLOR));
		final Trip trip = new Trip();
		trip.id = c.getInt(c.getColumnIndexOrThrow(RouteTripColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(RouteTripColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(RouteTripColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(RouteTripColumns.T_TRIP_K_ROUTE_ID));
		return new RouteTrip(route, trip);
	}
}

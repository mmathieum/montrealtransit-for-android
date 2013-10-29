package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.RouteTripStopColumns;

import android.database.Cursor;

public class RouteTripStop extends TripStop {

	public Route route;

	public RouteTripStop(Route route, Trip trip, Stop stop) {
		super(trip, stop);
		this.route = route;
	}

	public RouteTripStop(RouteTrip routeTrip, Stop stop) {
		this(routeTrip.route, routeTrip.trip, stop);
	}

	public static RouteTripStop fromCursor(Cursor c) {
		final Route route = new Route();
		route.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_LONG_NAME));
		route.color = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_COLOR));
		route.textColor = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR));
		final Trip trip = new Trip();
		trip.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_TRIP_K_ROUTE_ID));
		final Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LNG));
		return new RouteTripStop(route, trip, stop);
	}

}

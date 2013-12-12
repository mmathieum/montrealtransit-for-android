package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.RouteTripStopColumns;

import android.database.Cursor;

/**
 * A {@link RouteTripStop} without {@link Trip}.
 */
public class RouteStop extends TripStop {

	public Route route;

	public RouteStop(String authority, Route route, Trip trip, Stop stop) {
		super(authority, null, stop);
		this.route = route;
	}

	public RouteStop(String authority, RouteTrip routeTrip, Stop stop) {
		this(authority, routeTrip.route, null, stop);
	}

	public static RouteStop fromCursor(Cursor c, String authority) {
		final Route route = new Route();
		route.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_LONG_NAME));
		route.color = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_COLOR));
		route.textColor = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_ROUTE_K_TEXT_COLOR));
		final Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(RouteTripStopColumns.T_STOP_K_LNG));
		return new RouteStop(authority, route, null, stop);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(RouteStop.class.getSimpleName()).append(":[") //
				.append("authority:").append(authority).append(',') //
				.append(route).append(',') //
				.append(trip).append(',') //
				.append(stop) //
				.append(']').toString();
	}

	@Override
	public String getUID() {
		return getUID(authority, stop.id, route.id);
	}
}

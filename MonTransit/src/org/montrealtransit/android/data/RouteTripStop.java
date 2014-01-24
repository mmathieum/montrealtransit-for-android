package org.montrealtransit.android.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.common.RouteTripStopColumns;

import android.database.Cursor;

public class RouteTripStop extends TripStop {

	public Route route;

	public RouteTripStop(String authority, Route route, Trip trip, Stop stop) {
		super(authority, trip, stop);
		this.route = route;
	}

	public RouteTripStop(String authority, RouteTrip routeTrip, Stop stop) {
		this(authority, routeTrip.route, routeTrip.trip, stop);
	}

	public static RouteTripStop fromCursor(Cursor c, String authority) {
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
		return new RouteTripStop(authority, route, trip, stop);
	}

	public boolean equals(int routeId, int tripId, int stopId) {
		return route.id == routeId && trip.id == tripId && stop.id == stopId;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(RouteTripStop.class.getSimpleName()).append(":[") //
				.append("authority:").append(authority).append(',') //
				.append(route).append(',') //
				.append(trip).append(',') //
				.append(stop) //
				.append(']').toString();
	}

	public JSONObject toJSON() {
		return toJSON(this);
	}

	public static JSONObject toJSON(RouteTripStop routeTripStop) {
		try {
			return new JSONObject() //
					.put("authority", routeTripStop.authority) //
					.put("route", Route.toJSON(routeTripStop.route)) //
					.put("trip", Trip.toJSON(routeTripStop.trip)) //
					.put("stop", Stop.toJSON(routeTripStop.stop));
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while converting to JSON (%s)!", routeTripStop);
			return null;
		}
	}

	public static RouteTripStop fromJSON(String json) {
		try {
			return fromJSON(new JSONObject(json));
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", json);
			return null;
		}
	}

	public static RouteTripStop fromJSON(JSONObject jRouteTripStop) {
		try {
			return new RouteTripStop( //
					jRouteTripStop.getString("authority"),//
					Route.fromJSON(jRouteTripStop.getJSONObject("route")), //
					Trip.fromJSON(jRouteTripStop.getJSONObject("trip")), //
					Stop.fromJSON(jRouteTripStop.getJSONObject("stop")));
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jRouteTripStop);
			return null;
		}
	}

	@Override
	public String getUID() {
		return getUID(authority, stop.id, route.id);
	}

}

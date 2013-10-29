package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.TripStopColumns;

import android.database.Cursor;
import android.text.TextUtils;

public class TripStop implements POI {

	public Trip trip;
	public Stop stop;

	public static final String UID_SEPARATOR = "-";

	public TripStop(Trip trip, Stop stop) {
		this.trip = trip;
		this.stop = stop;
	}

	public static TripStop fromCursor(Cursor c) {
		final Trip trip = new Trip();
		trip.id = c.getInt(c.getColumnIndexOrThrow(TripStopColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(TripStopColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(TripStopColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(TripStopColumns.T_TRIP_K_ROUTE_ID));
		final Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(TripStopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(TripStopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(TripStopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(TripStopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(TripStopColumns.T_STOP_K_LNG));
		return new TripStop(trip, stop);
	}

	@Override
	public String getUID() {
		// TODO include agency
		// TOOD use Trip ID?
		return getUID(stop.code, String.valueOf(trip.routeId));
	}

	/**
	 * @param stopCode stop code (should be stop ID)
	 * @param routeShortName route short name (TODO should be route ID? trip ID?)
	 */
	public static String getUID(String stopCode, String routeShortName) {
		// TODO include agency
		// TOOD use Trip ID? Route ID?
		return stopCode + UID_SEPARATOR + routeShortName;
	}

	public static String getStopCodeFromUID(String uid) {
		if (TextUtils.isEmpty(uid)) {
			return null;
		}
		final String[] split = uid.split(UID_SEPARATOR);
		if (split.length < 1) {
			return null;
		}
		return split[0];
	}

	public static String getRouteShortNameFromUID(String uid) {
		if (TextUtils.isEmpty(uid)) {
			return null;
		}
		final String[] split = uid.split(UID_SEPARATOR);
		if (split.length < 2) {
			return null;
		}
		return split[1];
	}

	@Override
	public void setDistanceString(String distanceString) {
		stop.setDistanceString(distanceString);
	}

	@Override
	public Double getLat() {
		return stop.getLat();
	}

	@Override
	public Double getLng() {
		return stop.getLng();
	}

	@Override
	public boolean hasLocation() {
		return stop.hasLocation();
	}

	@Override
	public String getDistanceString() {
		return stop.getDistanceString();
	}

	@Override
	public void setDistance(float distance) {
		stop.setDistance(distance);
	}

	@Override
	public float getDistance() {
		return stop.getDistance();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_BUS;
	}
}

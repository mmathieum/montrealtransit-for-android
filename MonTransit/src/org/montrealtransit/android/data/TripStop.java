package org.montrealtransit.android.data;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.common.TripStopColumns;

import android.database.Cursor;
import android.text.TextUtils;

public class TripStop implements POI {

	public static final String TAG = TripStop.class.getSimpleName();

	// TODO store ContentUri ?
	public String authority;

	public Trip trip;
	public Stop stop;

	public static final String UID_SEPARATOR = "-";

	public TripStop(String authority, Trip trip, Stop stop) {
		this.authority = authority;
		this.trip = trip;
		this.stop = stop;
	}

	public static TripStop fromCursor(Cursor c, String authority) {
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
		return new TripStop(authority, trip, stop);
	}

	@Override
	public String toString() {
		return new StringBuilder().append(TripStop.class.getSimpleName()).append(":[") //
				.append("authority:").append(authority).append(',') //
				.append(trip).append(',') //
				.append(stop) //
				.append(']').toString();
	}

	@Override
	public String getUID() {
		// TODO include agency
		return getUID(authority, stop.id, trip.routeId);
	}

	/**
	 * @param stopCode stop code (should be stop ID)
	 * @param routeShortName route short name (TODO should be route ID? trip ID?)
	 */
	public static String getUID(String authority, int stopId, int routeId) {
		// TODO include agency
		return authority + UID_SEPARATOR + stopId + UID_SEPARATOR + routeId;
	}

	public static String getAuthorityFromUID(String uid) {
		if (TextUtils.isEmpty(uid)) {
			return null;
		}
		final String[] split = uid.split(UID_SEPARATOR);
		if (split.length < 1) {
			return null;
		}
		return split[0];
	}

	public static int getStopIdFromUID(String uid) {
		try {
			if (TextUtils.isEmpty(uid)) {
				return -1;
			}
			final String[] split = uid.split(UID_SEPARATOR);
			if (split.length < 2) {
				return -1;
			}
			return Integer.valueOf(split[1]);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while extracting stop ID from UID '%s'!", uid);
			return -1;
		}
	}

	public static int getRouteIdFromUID(String uid) {
		try {
			if (TextUtils.isEmpty(uid)) {
				return -1;
			}
			final String[] split = uid.split(UID_SEPARATOR);
			if (split.length < 3) {
				return -1;
			}
			return Integer.valueOf(split[2]);
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while extracting stop ID from UID '%s'!", uid);
			return -1;
		}
	}

	@Override
	public void setDistanceString(CharSequence distanceString) {
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
	public CharSequence getDistanceString() {
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
		return POI.ITEM_VIEW_TYPE_STOP;
	}
}

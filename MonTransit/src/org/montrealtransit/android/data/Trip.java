package org.montrealtransit.android.data;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.provider.common.TripColumns;

import android.content.Context;
import android.database.Cursor;

public class Trip {

	private static final String TAG = Trip.class.getSimpleName();

	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	public static final int HEADSIGN_TYPE_STOP_ID = 3;

	public int id;
	public int headsignType = HEADSIGN_TYPE_STRING; // 0 = String, 1 = direction, 2= inbound, 3=stopId
	public String headsignValue = "";
	public int routeId; // 10

	public static Trip fromCursor(Cursor c) {
		final Trip trip = new Trip();
		trip.id = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_ID));
		trip.headsignType = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_HEADSIGN_TYPE));
		trip.headsignValue = c.getString(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_HEADSIGN_VALUE));
		trip.routeId = c.getInt(c.getColumnIndexOrThrow(TripColumns.T_TRIP_K_ROUTE_ID));
		return trip;
	}

	private String heading = null;

	public String getHeading(Context context) {
		if (heading == null) {
			heading = getNewHeading(context);
		}
		return heading;
	}

	private String getNewHeading(Context context) {
		switch (headsignType) {
		case HEADSIGN_TYPE_STRING:
			return headsignValue;
		case HEADSIGN_TYPE_DIRECTION:
			if ("E".equals(headsignValue)) {
				return context.getString(R.string.east);
			} else if ("N".equals(headsignValue)) {
				return context.getString(R.string.north);
			} else if ("W".equals(headsignValue)) {
				return context.getString(R.string.west);
			} else if ("S".equals(headsignValue)) {
				return context.getString(R.string.south);
			}
			// TODO HEADSIGN_TYPE_INBOUND
			// TODO HEADSIGN_TYPE_STOP_ID
		default:
			break;
		}
		MyLog.w(TAG, "Unknown trip heading type: %s | value: %s !", headsignType, headsignValue);
		return context.getString(R.string.ellipsis);
	}

}

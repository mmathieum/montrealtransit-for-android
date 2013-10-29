package org.montrealtransit.android.provider.common;

import android.provider.BaseColumns;

public class TripStopColumns {

	private static final String T_TRIP = "trip";
	public static final String T_TRIP_K_ID = T_TRIP + BaseColumns._ID;
	public static final String T_TRIP_K_HEADSIGN_TYPE = T_TRIP + "_" + "headsign_type";
	public static final String T_TRIP_K_HEADSIGN_VALUE = T_TRIP + "_" + "headsign_value";
	public static final String T_TRIP_K_ROUTE_ID = T_TRIP + "_" + "route_id";

	private static final String T_STOP = "stop";
	public static final String T_STOP_K_ID = T_STOP + BaseColumns._ID;
	public static final String T_STOP_K_CODE = T_STOP + "_" + "code";
	public static final String T_STOP_K_NAME = T_STOP + "_" + "name";
	public static final String T_STOP_K_LAT = T_STOP + "_" + "lat";
	public static final String T_STOP_K_LNG = T_STOP + "_" + "lng";

	private static final String T_TRIP_STOPS = "trip_stops";
	public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = T_TRIP_STOPS + "_" + "stop_sequence";
	// TODO other T_TRIP_STOPS columns?

}
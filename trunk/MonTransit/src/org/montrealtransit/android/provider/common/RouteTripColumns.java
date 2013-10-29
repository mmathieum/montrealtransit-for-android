package org.montrealtransit.android.provider.common;

import android.provider.BaseColumns;

public class RouteTripColumns {

	private static final String T_ROUTE = "route";
	public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
	public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
	public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
	public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
	public static final String T_ROUTE_K_TEXT_COLOR = T_ROUTE + "_" + "text_color";

	private static final String T_TRIP = "trip";
	public static final String T_TRIP_K_ID = T_TRIP + BaseColumns._ID;
	public static final String T_TRIP_K_HEADSIGN_TYPE = T_TRIP + "_" + "headsign_type";
	public static final String T_TRIP_K_HEADSIGN_VALUE = T_TRIP + "_" + "headsign_value";
	public static final String T_TRIP_K_ROUTE_ID = T_TRIP + "_" + "route_id";

}
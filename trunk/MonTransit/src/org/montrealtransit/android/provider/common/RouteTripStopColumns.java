package org.montrealtransit.android.provider.common;

import android.provider.BaseColumns;

public class RouteTripStopColumns extends TripStopColumns {

	private static final String T_ROUTE = "route";
	public static final String T_ROUTE_K_ID = T_ROUTE + BaseColumns._ID;
	public static final String T_ROUTE_K_SHORT_NAME = T_ROUTE + "_" + "short_name";
	public static final String T_ROUTE_K_LONG_NAME = T_ROUTE + "_" + "long_name";
	public static final String T_ROUTE_K_COLOR = T_ROUTE + "_" + "color";
	public static final String T_ROUTE_K_TEXT_COLOR = T_ROUTE + "_" + "text_color";

}
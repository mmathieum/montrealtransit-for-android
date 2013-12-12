package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.RouteColumns;

import android.database.Cursor;

public class Route {

	public int id;
	public String shortName;
	public String longName;

	public String color;
	public String textColor;

	public static Route fromCursor(Cursor c) {
		final Route route = new Route();
		route.id = c.getInt(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_ID));
		route.shortName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_SHORT_NAME));
		route.longName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_LONG_NAME));
		route.color = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_COLOR));
		route.textColor = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_TEXT_COLOR));
		return route;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Route.class.getSimpleName()).append(":[") //
				.append("id:").append(id).append(',') //
				.append("shortName:").append(shortName).append(',') //
				.append("longName:").append(longName).append(',') //
				.append("color:").append(color).append(',') //
				.append("textColor:").append(textColor) //
				.append(']').toString();
	}

}

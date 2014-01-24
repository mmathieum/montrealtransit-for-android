package org.montrealtransit.android.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;

public class Route {

	public static final String TAG = Route.class.getSimpleName();

	public int id;
	public String shortName;
	public String longName;

	public String color;
	public String textColor;

	// public static Route fromCursor(Cursor c) {
	// final Route route = new Route();
	// route.id = c.getInt(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_ID));
	// route.shortName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_SHORT_NAME));
	// route.longName = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_LONG_NAME));
	// route.color = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_COLOR));
	// route.textColor = c.getString(c.getColumnIndexOrThrow(RouteColumns.T_ROUTE_K_TEXT_COLOR));
	// return route;
	// }

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

	public static JSONObject toJSON(Route route) {
		try {
			return new JSONObject() //
					.put("id", route.id) //
					.put("shortName", route.shortName) //
					.put("longName", route.longName) //
					.put("color", route.color) //
					.put("textColor", route.textColor);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while converting to JSON (%s)!", route);
			return null;
		}
	}

	public static Route fromJSON(JSONObject jRoute) {
		try {
			final Route route = new Route();
			route.id = jRoute.getInt("id");
			route.shortName = jRoute.getString("shortName");
			route.longName = jRoute.getString("longName");
			route.color = jRoute.getString("color");
			route.textColor = jRoute.getString("textColor");
			return route;
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", jRoute);
			return null;
		}
	}

}

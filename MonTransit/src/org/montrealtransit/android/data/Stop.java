package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.common.StopColumns;

import android.database.Cursor;

public class Stop implements POI {

	public int id;

	public String code;
	public String name;

	public double lat;
	public double lng;

	private CharSequence distanceString = null;

	private float distance = -1;

	public Stop() {
	}

	public Stop(Stop stop) {
		id = stop.id;
		code = stop.code;
		name = stop.name;
		lat = stop.lat;
		lng = stop.lng;
	}

	public static Stop fromCursor(Cursor c) {
		final Stop stop = new Stop();
		stop.id = c.getInt(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_ID));
		stop.code = c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_CODE));
		stop.name = c.getString(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_NAME));
		stop.lat = c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LAT));
		stop.lng = c.getDouble(c.getColumnIndexOrThrow(StopColumns.T_STOP_K_LNG));
		return stop;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(Stop.class.getSimpleName()).append(":[") //
				.append("id:").append(id).append(',') //
				.append("code:").append(code).append(',') //
				.append("name:").append(name).append(',') //
				.append("lat:").append(lat).append(',') //
				.append("lng:").append(lng) //
				.append(']').toString();
	}

	@Override
	public Double getLat() {
		return this.lat;
	}

	@Override
	public Double getLng() {
		return this.lng;
	}

	@Override
	public boolean hasLocation() {
		return true;
	}

	@Override
	public CharSequence getDistanceString() {
		return distanceString;
	}

	@Override
	public void setDistanceString(CharSequence distanceString) {
		this.distanceString = distanceString;
	}

	@Override
	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Override
	public float getDistance() {
		return this.distance;
	}

	@Deprecated
	@Override
	public String getUID() {
		return String.valueOf(this.id);
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_STOP;
	}

}

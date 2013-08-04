package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

/**
 * Represent list of close Point Of Interest and the location (+address).
 * @author Mathieu Méa
 */
public class ClosestPOI<T> {

	/**
	 * The closest Point Of Interest list.
	 */
	private List<T> poiList;
	/**
	 * The error message.
	 */
	private String errorMessage;
	private double lat;
	private double lng;

	public ClosestPOI(Location location) {
		this.lat = location.getLatitude();
		this.lng = location.getLongitude();
	}

	public ClosestPOI(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	/**
	 * @return the Point Of Interest list (initialize if null)
	 */
	public List<T> getPoiList() {
		if (this.poiList == null) {
			this.poiList = new ArrayList<T>();
		}
		return this.poiList;
	}

	/**
	 * @return the Point Of Interest list or null
	 */
	public List<T> getPoiListOrNull() {
		return this.poiList;
	}

	/**
	 * @return the Point Of Interest list size (null-safe)
	 */
	public int getPoiListSize() {
		return this.poiList == null ? 0 : this.poiList.size();
	}

	/**
	 * @param poiList the new Point Of Interest list
	 */
	public void setPoiList(List<T> poiList) {
		this.poiList = poiList;
	}

	/**
	 * @param errorMessage the new error message
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}

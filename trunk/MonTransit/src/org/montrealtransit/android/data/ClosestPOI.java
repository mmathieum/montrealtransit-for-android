package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.LocationUtils;

import android.content.Context;
import android.location.Address;
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
	 * The location.
	 */
	private Location location;
	/**
	 * The location address.
	 */
	private Address locationAddress;
	/**
	 * The error message.
	 */
	private String errorMessage;

	/**
	 * Default constructor.
	 */
	public ClosestPOI() {
	}

	/**
	 * Set the location and LOAD the location address.
	 * @param location the location
	 * @param context the context
	 */
	@Deprecated
	public void setLocationAndAddress(Location location, Context context) {
		this.location = location;
		this.locationAddress = LocationUtils.getLocationAddress(context, this.location);
	}

	/**
	 * @param location the new location
	 */
	public void setLocation(Location location) {
		this.location = location;
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
	 * @return the location
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * @return the location address
	 */
	public Address getLocationAddress() {
		return this.locationAddress;
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

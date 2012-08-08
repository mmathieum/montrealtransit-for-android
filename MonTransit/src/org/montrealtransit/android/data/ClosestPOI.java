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
	
	private String errorMessage;

	/**
	 * The default constructor.
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
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
}

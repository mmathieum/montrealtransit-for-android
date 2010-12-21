package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.LocationUtils;

import android.content.Context;
import android.location.Address;
import android.location.Location;

/**
 * Represent list of close {@link ASubwayStation} and the location (+address).
 * @author Mathieu Méa
 */
public class ClosestSubwayStations {

	/**
	 * The closest stations.
	 */
	private List<ASubwayStation> stations;
	/**
	 * The location.
	 */
	private Location location;
	/**
	 * The location address.
	 */
	private Address locationAddress;

	/**
	 * The default constructor.
	 */
	public ClosestSubwayStations() {
	}

	/**
	 * Set the location and the location address.
	 * @param location the location
	 * @param context the context
	 */
	public void setLocationAndAddress(Location location, Context context) {
		this.location = location;
		this.locationAddress = LocationUtils.getLocationAddress(context, this.location);
	}

	/**
	 * @return the stations (initialize if null)
	 */
	public List<ASubwayStation> getStations() {
		if (this.stations == null) {
			this.stations = new ArrayList<ASubwayStation>();
		}
		return this.stations;
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
}

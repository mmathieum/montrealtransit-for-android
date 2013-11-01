package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.BixiStore.BikeStation;

/**
 * A {@link BikeStation} with a distance.
 * @author Mathieu Méa
 */
public class ABikeStation extends BikeStation implements POI {

	/**
	 * The distance string.
	 */
	private CharSequence distanceString = null;
	/**
	 * The distance in meter.
	 */
	private float distance = -1;

	/**
	 * The default constructor.
	 */
	public ABikeStation() {
	}

	/**
	 * A constructor initializing the properties with a {@link BikeStation} object.
	 * @param subwayStation {@link BikeStation} object.
	 */
	public ABikeStation(BikeStation bikeStation) {
		setId(bikeStation.getId());
		setName(bikeStation.getName());
		setTerminalName(bikeStation.getTerminalName());
		setLat(bikeStation.getLat());
		setLng(bikeStation.getLng());
		setInstalled(bikeStation.isInstalled());
		setLocked(bikeStation.isLocked());
		setInstallDate(bikeStation.getInstallDate());
		setRemovalDate(bikeStation.getRemovalDate());
		setTemporary(bikeStation.isTemporary());
		setNbBikes(bikeStation.getNbBikes());
		setNbEmptyDocks(bikeStation.getNbEmptyDocks());
		setLatestUpdateTime(bikeStation.getLatestUpdateTime());
	}

	/**
	 * @param distanceString the new distance string
	 */
	public void setDistanceString(CharSequence distanceString) {
		this.distanceString = distanceString;
	}

	/**
	 * @return the distance string
	 */
	public CharSequence getDistanceString() {
		return distanceString;
	}

	/**
	 * @param distance the new distance
	 */
	public void setDistance(float distance) {
		this.distance = distance;
	}

	/**
	 * @return the distance or null
	 */
	public float getDistance() {
		return distance;
	}

	@Override
	public String getUID() {
		return getTerminalName();
	}

	@Override
	public int getType() {
		return POI.ITEM_VIEW_TYPE_BIKE;
	}
}

package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.BixiStore.BikeStation;

import android.graphics.Matrix;

/**
 * A {@link BikeStation} with a distance.
 * @author Mathieu Méa
 */
public class ABikeStation extends BikeStation {

	/**
	 * The distance string.
	 */
	private String distanceString;
	/**
	 * The distance in meter.
	 */
	private Float distance;
	
	/**
	 * The compass rotation matrix or null.
	 */
	private Matrix compassMatrix;

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
	public void setDistanceString(String distanceString) {
		this.distanceString = distanceString;
	}

	/**
	 * @return the distance string
	 */
	public String getDistanceString() {
		return distanceString;
	}

	/**
	 * @param distance the new distance
	 */
	public void setDistance(Float distance) {
		this.distance = distance;
	}

	/**
	 * @return the distance or null
	 */
	public Float getDistance() {
		return distance;
	}

	/**
	 * @param matrix the new compass rotation matrix
	 */
	public void setCompassMatrix(Matrix matrix) {
		this.compassMatrix = matrix;
	}

	/**
	 * @return the compass rotation matrix
	 */
	public Matrix getCompassMatrix() {
		return compassMatrix;
	}

}

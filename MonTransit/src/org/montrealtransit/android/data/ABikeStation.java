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
	private float distance = -1;

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
	public void setDistance(float distance) {
		this.distance = distance;
	}

	/**
	 * @return the distance or null
	 */
	public float getDistance() {
		return distance;
	}

	/**
	 * @param matrix the new compass rotation matrix
	 */
	public void setCompassMatrix(Matrix matrix) {
		this.compassMatrix = matrix;
	}

	/**
	 * @return the compass rotation matrix (not null)
	 */
	public Matrix getCompassMatrix() {
		if (this.compassMatrix == null) {
			this.compassMatrix = new Matrix();
		}
		return compassMatrix;
	}

	/**
	 * @return the compass rotation matrix or null
	 */
	public Matrix getCompassMatrixOrNull() {
		return compassMatrix;
	}
}

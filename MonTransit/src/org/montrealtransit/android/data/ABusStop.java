package org.montrealtransit.android.data;

import org.montrealtransit.android.provider.StmStore.BusStop;

import android.graphics.Matrix;

/**
 * A {@link BusStop} with a distance.
 * @author Mathieu Méa
 */
public class ABusStop extends BusStop {

	/**
	 * The distance string.
	 */
	private String distanceString;
	/**
	 * The distance in meter.
	 */
	private float distance;

	/**
	 * The compass rotation matrix.
	 */
	private Matrix compassMatrix;

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
	 * @return the distance
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
	 * @return the compass rotation matrix
	 */
	public Matrix getCompassMatrix() {
		return compassMatrix;
	}
}

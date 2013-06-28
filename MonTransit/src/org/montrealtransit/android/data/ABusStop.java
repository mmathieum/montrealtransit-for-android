package org.montrealtransit.android.data;

import org.montrealtransit.android.LocationUtils.POI;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.graphics.Matrix;

/**
 * A {@link BusStop} with a distance.
 * @author Mathieu Méa
 */
public class ABusStop extends BusStop implements POI {

	/**
	 * The distance string.
	 */
	private String distanceString;
	/**
	 * The distance in meter.
	 */
	private float distance = -1;

	/**
	 * The compass rotation matrix.
	 */
	private Matrix compassMatrix;

	/**
	 * Default constructor.
	 */
	public ABusStop() {
	}

	/**
	 * A constructor initializing the properties with a {@link BusStop} object.
	 * @param busStop {@link BusStop} object.
	 */
	public ABusStop(BusStop busStop) {
		setCode(busStop.getCode());
		setDirectionId(busStop.getDirectionId());
		setPlace(busStop.getPlace());
		setSubwayStationId(busStop.getSubwayStationId());
		setSubwayStationName(busStop.getSubwayStationNameOrNull());
		setSubwayStationLat(busStop.getSubwayStationLatOrNull());
		setSubwayStationLng(busStop.getSubwayStationLngOrNull());
		setLineNumber(busStop.getLineNumber());
		setLineName(busStop.getLineNameOrNull());
		setLineType(busStop.getLineTypeOrNull());
		setLat(busStop.getLat());
		setLng(busStop.getLng());
	}

	@Override
	public void setDistanceString(String distanceString) {
		this.distanceString = distanceString;
	}

	@Override
	public String getDistanceString() {
		return distanceString;
	}

	@Override
	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Override
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

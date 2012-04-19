package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.graphics.Matrix;

/**
 * Represents a localized subway station.
 * @author Mathieu Méa
 */
public class ASubwayStation extends SubwayStation {

	/**
	 * The current main subway line ID.
	 */
	private Integer lineId;
	/**
	 * the other subway lines ID.
	 */
	private List<Integer> otherLinesId;
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
	 * The default constructor.
	 */
	public ASubwayStation() {
	}

	/**
	 * A constructor initializing the properties with a subway station object.
	 * @param subwayStation the subway station object.
	 */
	public ASubwayStation(SubwayStation subwayStation) {
		setId(subwayStation.getId());
		this.setId(subwayStation.getId());
		this.setName(subwayStation.getName());
		this.setLat(subwayStation.getLat());
		this.setLng(subwayStation.getLng());
	}

	/**
	 * @param lineId the new current subway line ID
	 */
	public void setLineId(int lineId) {
		this.lineId = lineId;
	}

	/**
	 * @return the current subway line ID.
	 */
	public int getLineId() {
		return lineId;
	}

	/**
	 * @param newLineNumber a new other subway line ID
	 */
	public void addOtherLineId(Integer newLineNumber) {
		if (!this.getOtherLinesId().contains(newLineNumber)) {
			this.getOtherLinesId().add(newLineNumber);
		}
	}

	/**
	 * @param newLinesNumber the new other subway lines ID
	 */
	public void addOtherLinesId(Set<Integer> newLinesNumber) {
		for (Integer newLineNumber : newLinesNumber) {
			addOtherLineId(newLineNumber);
		}
	}

	/**
	 * @return the other subway lines ID
	 */
	public List<Integer> getOtherLinesId() {
		if (this.otherLinesId == null) {
			this.otherLinesId = new ArrayList<Integer>();
		}
		return this.otherLinesId;
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
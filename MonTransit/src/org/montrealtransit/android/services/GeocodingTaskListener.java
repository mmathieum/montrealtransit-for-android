package org.montrealtransit.android.services;

import java.util.List;

import android.location.Address;

/**
 * Interface implemented by all the class that need to process the result of {@link GeocodingTask}.
 * @author Mathieu Méa
 */
public interface GeocodingTaskListener {

	/**
	 * Call when the task is complete to process the data.
	 * @param addresses the new addresses.
	 */
	void processLocation(List<Address> addresses);
}

package org.montrealtransit.android.api;

import android.annotation.TargetApi;

/**
 * Features available for Android 2.0 Eclair (API Level 5) and higher.
 * @author Mathieu Méa
 */
@TargetApi(5)
public class EclairSupport extends DonutSupport {

	/**
	 * The default constructor.
	 */
	public EclairSupport() {
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return 25;
	}

}

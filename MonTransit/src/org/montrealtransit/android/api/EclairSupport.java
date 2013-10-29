package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.res.Configuration;

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
	
	@Override
	public boolean isScreenHeightSmall(Configuration configuration) {
		final int sizeMask = getScreenLayoutSize(configuration);
		final boolean smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL;
		return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && smallScreen;
	}
}

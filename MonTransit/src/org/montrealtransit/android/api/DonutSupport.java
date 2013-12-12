package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;

/**
 * Features available for Android 1.6 Donut (API Level 4) and higher.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class DonutSupport extends CupcakeSupport {

	/**
	 * The default constructor.
	 */
	public DonutSupport() {
	}

	@Override
	public String getBuildManufacturer() {
		return Build.MANUFACTURER;
	}

	@Override
	public int getScreenLayoutSize(Configuration configuration) {
		return configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
	}

	@Override
	public Class<?> getRouteInfoClass() {
		return org.montrealtransit.android.activity.v4.RouteInfo.class;
	}

	@Override
	public Class<?> getBusTabClass() {
		return org.montrealtransit.android.activity.v4.BusTab.class;
	}
}

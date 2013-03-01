package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;
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
	 * @param context the context
	 */
	public DonutSupport(Context context) {
		super(context);
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
	public boolean isScreenHeightSmall(Configuration configuration) {
		final int sizeMask = getScreenLayoutSize(configuration);
		final boolean smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL;
		return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && smallScreen;
	}

	@Override
	public Class<?> getBusLineInfoClass() {
		return org.montrealtransit.android.activity.v4.BusLineInfo.class;
	}

	@Override
	public Class<?> getSubwayLineInfoClass() {
		return org.montrealtransit.android.activity.v4.SubwayLineInfo.class;
	}

	@Override
	public Class<?> getBusTabClass() {
		return org.montrealtransit.android.activity.v4.BusTab.class;
	}
}

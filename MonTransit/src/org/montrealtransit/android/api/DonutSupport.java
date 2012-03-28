package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;
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
}

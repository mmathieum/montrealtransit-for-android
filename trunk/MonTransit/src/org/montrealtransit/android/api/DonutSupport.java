package org.montrealtransit.android.api;

import android.content.Context;
import android.os.Build;

public class DonutSupport extends CupcakeSupport {

	public DonutSupport(Context context) {
		super(context);
	}

	@Override
	public String getBuildManufacturer() {
		return Build.MANUFACTURER;
	}
}

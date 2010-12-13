package org.montrealtransit.android.api;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class SupportFactory {

	private static final String TAG = SupportFactory.class.getSimpleName();

	public static SupportUtil getInstance(Context context) {
		String className = SupportFactory.class.getPackage().getName() + ".";
		int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion < Build.VERSION_CODES.CUPCAKE) {
			Log.d(TAG, "Unknow API Level: " + sdkVersion);
			className += "CupcakeSupport"; // unsupported version
		} else if (sdkVersion == Build.VERSION_CODES.CUPCAKE) {
			className += "CupcakeSupport"; // 3
		} else if (sdkVersion == Build.VERSION_CODES.DONUT) {
			className += "DonutSupport"; // 4
		} else if (sdkVersion == Build.VERSION_CODES.ECLAIR
				|| sdkVersion == Build.VERSION_CODES.ECLAIR_0_1
				|| sdkVersion == Build.VERSION_CODES.ECLAIR_MR1) {
			className += "EclairSupport"; // 5 6 7
		} else if (sdkVersion == Build.VERSION_CODES.FROYO) {
			className += "FroyoSupport"; // 8
		} else if (sdkVersion == Build.VERSION_CODES.GINGERBREAD) {
			className += "GingerBreadSupport"; // 9
		} else if (sdkVersion > Build.VERSION_CODES.GINGERBREAD) {
			Log.d(TAG, "Unknow API Level: " + Build.VERSION.SDK);
			className += "GingerBreadSupport"; // default for newer SDK
		}

		try {
			Class<?> detectorClass = Class.forName(className);
			return (SupportUtil) detectorClass.getConstructor(Context.class)
					.newInstance(context);
		} catch (Exception e) {
			Log.e(TAG, "INTERNAL ERROR!", e);
			throw new RuntimeException(e);
		}
	}
}

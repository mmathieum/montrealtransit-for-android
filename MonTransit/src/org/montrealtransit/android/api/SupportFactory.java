package org.montrealtransit.android.api;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.os.Build;

public class SupportFactory {

	private static final String TAG = SupportFactory.class.getSimpleName();

	public static SupportUtil getInstance(Context context) {
		String className = SupportFactory.class.getPackage().getName();
		int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion < Build.VERSION_CODES.CUPCAKE) {
			MyLog.d(TAG, "Unknow API Level: " + sdkVersion);
			className += ".CupcakeSupport"; // unsupported version
		} else if (sdkVersion == Build.VERSION_CODES.CUPCAKE) {
			className += ".CupcakeSupport"; // 3
		} else if (sdkVersion == Build.VERSION_CODES.DONUT) {
			className += ".DonutSupport"; // 4
		} else if (sdkVersion == Build.VERSION_CODES.ECLAIR
				|| sdkVersion == Build.VERSION_CODES.ECLAIR_0_1
				|| sdkVersion == Build.VERSION_CODES.ECLAIR_MR1) {
			className += ".EclairSupport"; // 5 6 7
		} else if (sdkVersion == Build.VERSION_CODES.FROYO) {
			className += ".FroyoSupport"; // 8
		} else if (sdkVersion == Build.VERSION_CODES.GINGERBREAD) {
			className += ".GingerbreadSupport"; // 9
		} else if (sdkVersion > Build.VERSION_CODES.GINGERBREAD) {
			MyLog.w(TAG, "Unknow API Level: %s",  Build.VERSION.SDK);
			className += ".GingerbreadSupport"; // default for newer SDK
		}

		try {
			Class<?> detectorClass = Class.forName(className);
			return (SupportUtil) detectorClass.getConstructor(Context.class)
					.newInstance(context);
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR!", e);
			throw new RuntimeException(e);
		}
	}
}

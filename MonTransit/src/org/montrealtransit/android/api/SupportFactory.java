package org.montrealtransit.android.api;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.os.Build;

/**
 * This class binds SDK versions with available features.
 * @author Mathieu Méa
 */
public class SupportFactory {

	/**
	 * The log tag.
	 */
	private static final String TAG = SupportFactory.class.getSimpleName();

	/**
	 * @param context the context
	 * @return the support instance
	 */
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
		} else if (sdkVersion == Build.VERSION_CODES.ECLAIR || sdkVersion == Build.VERSION_CODES.ECLAIR_0_1
		        || sdkVersion == Build.VERSION_CODES.ECLAIR_MR1) {
			className += ".EclairSupport"; // 5 6 7
		} else if (sdkVersion == Build.VERSION_CODES.FROYO) {
			className += ".FroyoSupport"; // 8
		} else if (sdkVersion == Build.VERSION_CODES.GINGERBREAD || sdkVersion == Build.VERSION_CODES.GINGERBREAD_MR1) {
			className += ".GingerbreadSupport"; // 9 10
		} else if (sdkVersion == Build.VERSION_CODES.HONEYCOMB || sdkVersion == Build.VERSION_CODES.HONEYCOMB_MR1
		        || sdkVersion == Build.VERSION_CODES.HONEYCOMB_MR2) {
			className += ".HoneycombSupport"; // 11 12 13
		} else if (sdkVersion == Build.VERSION_CODES.ICE_CREAM_SANDWICH
		        || sdkVersion == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			className += ".IceCreamSandwichSupport"; // 14 15
		} else if (sdkVersion > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			MyLog.w(TAG, "Unknow API Level: %s", Build.VERSION.SDK);
			className += ".IceCreamSandwichSupport"; // default for newer SDK
		}

		try {
			Class<?> detectorClass = Class.forName(className);
			return (SupportUtil) detectorClass.getConstructor(Context.class).newInstance(context);
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR!");
			throw new RuntimeException(e);
		}
	}
}

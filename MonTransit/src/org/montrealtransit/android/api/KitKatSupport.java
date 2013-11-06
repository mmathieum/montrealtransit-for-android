package org.montrealtransit.android.api;

import android.annotation.TargetApi;

/**
 * Features available for Android 4.4 KitKat (API Level 19) and higher.
 * @author Mathieu Méa
 */
@TargetApi(19)
public class KitKatSupport extends JellyBeanSupportMR2 {

	/**
	 * The log tag.
	 */
	public static final String TAG = KitKatSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public KitKatSupport() {
	}
	
}

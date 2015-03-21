package org.montrealtransit.android.api;

import android.annotation.TargetApi;

/**
 * Features available for Android 5.1 Lollipop (API Level 22) and higher.
 * @author Mathieu Méa
 */
@TargetApi(22)
public class LollipopMR1Support extends KitKatSupport {

	/**
	 * The log tag.
	 */
	public static final String TAG = LollipopMR1Support.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public LollipopMR1Support() {
	}

}

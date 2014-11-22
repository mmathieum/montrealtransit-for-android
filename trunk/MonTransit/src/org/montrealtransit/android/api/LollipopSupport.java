package org.montrealtransit.android.api;

import android.annotation.TargetApi;

/**
 * Features available for Android 5.0 Lollipop (API Level 21) and higher.
 * @author Mathieu Méa
 */
@TargetApi(21)
public class LollipopSupport extends KitKatSupport {

	/**
	 * The log tag.
	 */
	public static final String TAG = LollipopSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public LollipopSupport() {
	}

}

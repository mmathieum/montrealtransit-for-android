package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;

/**
 * Features available for Android 4.1 Jelly Bean (API Level 16) and higher.
 * @author Mathieu Méa
 */
@TargetApi(16)
public class JellyBeanSupport extends IceCreamSandwichSupport {

	/**
	 * The log tag.
	 */
	// private static final String TAG = JellyBeanSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public JellyBeanSupport(Context context) {
		super(context);
	}
}
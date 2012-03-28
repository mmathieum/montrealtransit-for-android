package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;

/**
 * Features available for Android 3.0 Honeycomb (API Level 11) and higher.
 * @author Mathieu Méa
 */
@TargetApi(11)
public class HoneycombSupport extends GingerbreadSupport {

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public HoneycombSupport(Context context) {
		super(context);
	}
}

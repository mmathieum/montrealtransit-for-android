package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;

/**
 * Features available for Android 2.0 Eclair (API Level 5) and higher.
 * @author Mathieu Méa
 */
@TargetApi(5)
public class EclairSupport extends DonutSupport {

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public EclairSupport(Context context) {
		super(context);
	}

}

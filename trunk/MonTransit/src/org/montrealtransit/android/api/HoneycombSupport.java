package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;

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
	
	@Override
	public void enableStrictMode() {
		ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build();
		// or .detectAll() for all detectable problems
		StrictMode.setThreadPolicy(threadPolicy);
		VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build();
		StrictMode.setVmPolicy(vmPolicy);
	}
}

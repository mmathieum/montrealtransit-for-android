package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;

/**
 * Features available for Android 2.3 Gingerbread (API Level 9) and higher.
 * @author Mathieu Méa
 */
@TargetApi(9)
public class GingerbreadSupport extends FroyoSupport {

	/**
	 * The log tag.
	 */
	@SuppressWarnings("unused")
	private static final String TAG = GingerbreadSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public GingerbreadSupport(Context context) {
		super(context);
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		editor.apply();
		backupManagerDataChanged();
	}

	@Override
	public void enableStrictMode() {
		ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build();
		// or .detectAll() for all detectable problems
		StrictMode.setThreadPolicy(threadPolicy);
		VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build();
		StrictMode.setVmPolicy(vmPolicy);
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return 100;
	}
}

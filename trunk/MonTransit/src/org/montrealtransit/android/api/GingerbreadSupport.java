package org.montrealtransit.android.api;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import android.annotation.TargetApi;
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
	 */
	public GingerbreadSupport() {
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		editor.apply();
	}

	@Override
	public void enableStrictMode() {
		ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
		// or .detectAll() for all detectable problems
		StrictMode.setThreadPolicy(threadPolicy);
		VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
		StrictMode.setVmPolicy(vmPolicy);
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return super.getNbClosestPOIDisplay(); //100;
	}

	@Override
	public BlockingQueue<Runnable> getNewBlockingQueue() {
		return new LinkedBlockingDeque<Runnable>(7);
	}
}

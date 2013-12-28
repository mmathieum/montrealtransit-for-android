package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.view.ViewTreeObserver;

/**
 * Features available for Android 4.1 Jelly Bean (API Level 16) and higher.
 * @author Mathieu Méa
 */
@TargetApi(16)
public class JellyBeanSupport extends IceCreamSandwichSupport {

	/**
	 * The log tag.
	 */
	public static final String TAG = JellyBeanSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public JellyBeanSupport() {
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return 100; // super.getNbClosestPOIDisplay();
	}
	
	@Override
	public void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
		viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
	}
}

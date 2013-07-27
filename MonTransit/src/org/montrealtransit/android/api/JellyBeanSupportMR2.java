package org.montrealtransit.android.api;

import android.annotation.TargetApi;
import android.os.StatFs;

/**
 * Features available for Android 4.3 Jelly Bean (API Level 18) and higher.
 * @author Mathieu Méa
 */
@TargetApi(18)
public class JellyBeanSupportMR2 extends JellyBeanSupport {

	/**
	 * The log tag.
	 */
	public static final String TAG = JellyBeanSupportMR2.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public JellyBeanSupportMR2() {
	}
	
	@Override
	public long getStatFsAvailableBlocksLong(StatFs statFs) {
		return statFs.getAvailableBlocksLong();
	}
	
	@Override
	public long getStatFsBlockSizeLong(StatFs statFs) {
		return statFs.getBlockSizeLong();
	}
}

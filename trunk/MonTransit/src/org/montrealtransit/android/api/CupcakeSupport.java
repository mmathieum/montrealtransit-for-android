package org.montrealtransit.android.api;

import org.montrealtransit.android.services.NfcListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.view.Surface;

/**
 * Features available for Android 1.5 Cupcake (API Level 3) and higher.
 * @author Mathieu Méa
 */
@TargetApi(3)
public class CupcakeSupport implements SupportUtil {

	/**
	 * The context.
	 */
	protected Context context;

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public CupcakeSupport(Context context) {
		this.context = context;
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		editor.commit();
	}

	@Override
	public String getBuildManufacturer() {
		return "unknown";
	}

	@Override
	public int getASyncTaskCapacity() {
		return 10;
	}

	@Override
	public void backupManagerDataChanged() {
		// not supported until Froyo (API Level 8)
	}

	@Override
	public void registerNfcCallback(Activity activity, NfcListener listener, String mimeType) {
		// not supported until Ice Cream Sandwich (API Level 14)
	}

	@Override
	public boolean isNfcIntent(Intent intent) {
		// not supported until Gingerbread (API Level 9)
		return false;
	}

	@Override
	public void processNfcIntent(Intent intent, NfcListener listener) {
		// not supported until Gingerbread (API Level 9)
	}

	@Override
	public void setOnNdefPushCompleteCallback(Activity activity, NfcListener listener) {
		// not supported until Gingerbread (API Level 9)
	}

	@Override
	public void enableNfcForegroundDispatch(Activity activity) {
		// not supported until Gingerbread (API Level 9)
	}

	@Override
	public void disableNfcForegroundDispatch(Activity activity) {
		// not supported until Gingerbread (API Level 9)
	}

	@Override
	public float getDisplayRotation(Context context) {
		// does not handle other orientation
		if (Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation) {
			return -90f;
		}
		return 0f;
	}

	@Override
	public int getSurfaceRotation(Context context) {
		if (Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation) {
			return Surface.ROTATION_90;
		}
		return Surface.ROTATION_0;
	}

	@Override
	public void enableStrictMode() {
		// not supported until Gingerbread (API Level 9)
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return 10;
	}

	@Override
	public int getScreenLayoutSize(Configuration configuration) {
		return 0x02; // Configuration.SCREENLAYOUT_SIZE_NORMAL
	}

	@Override
	public boolean isScreenHeightSmall(Configuration configuration) {
		return true; // old device anyway
	}

	@Override
	public Class<?> getBusLineInfoClass() {
		return org.montrealtransit.android.activity.BusLineInfo.class;
	}

	@Override
	public Class<?> getSubwayLineInfoClass() {
		return org.montrealtransit.android.activity.SubwayLineInfo.class;
	}
}

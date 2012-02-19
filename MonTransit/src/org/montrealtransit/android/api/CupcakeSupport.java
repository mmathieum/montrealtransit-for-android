package org.montrealtransit.android.api;

import org.montrealtransit.android.services.NfcListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;

/**
 * Features available for Android 1.5 Cupcake (API Level 3) and higher.
 * @author Mathieu Méa
 */
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
}

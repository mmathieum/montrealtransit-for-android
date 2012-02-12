package org.montrealtransit.android.api;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public class CupcakeSupport implements SupportUtil {

	protected Context context;

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
		// not supported until Froyo?
	}
}

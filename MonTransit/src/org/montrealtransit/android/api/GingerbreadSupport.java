package org.montrealtransit.android.api;

import android.content.Context;
import android.content.SharedPreferences.Editor;

public class GingerbreadSupport extends FroyoSupport {

	public GingerbreadSupport(Context context) {
		super(context);
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		editor.apply();
		backupManagerDataChanged();
	}
}

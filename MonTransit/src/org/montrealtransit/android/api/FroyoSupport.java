package org.montrealtransit.android.api;

import org.montrealtransit.android.MyLog;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences.Editor;

public class FroyoSupport extends EclairSupport {

	private static final String TAG = FroyoSupport.class.getSimpleName();

	public FroyoSupport(Context context) {
		super(context);
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		backupManagerDataChanged();
		super.applySharedPreferencesEditor(editor);
	}

	@Override
	public void backupManagerDataChanged() {
		new BackupManager(context).dataChanged();
		MyLog.d(TAG, "SharedPreferences changed. BackupManager.dataChanged() fired.");
	}
}

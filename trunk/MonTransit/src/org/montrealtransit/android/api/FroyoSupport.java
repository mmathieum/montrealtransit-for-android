package org.montrealtransit.android.api;

import org.montrealtransit.android.MyLog;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences.Editor;

/**
 * Features available for Android 2.2 Froyo (API Level 8) and higher.
 * @author Mathieu Méa
 */
@TargetApi(8)
public class FroyoSupport extends EclairSupport {

	/**
	 * The log tag.
	 */
	private static final String TAG = FroyoSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 * @param context the context
	 */
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

package org.montrealtransit.android.services;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.activity.UserPreferences;

import android.annotation.TargetApi;
import android.app.backup.SharedPreferencesBackupHelper;
import android.preference.PreferenceManager;

/**
 * The backup agent defining the various preferences helper.
 * <ul>
 * <li>{@link SharedPreferencesBackupHelper}</li>
 * <li>{@link FavoritesBackupHelper}</li>
 * </ul>
 * @author Mathieu Méa
 */
@TargetApi(8)
public class BackupAgentHelper extends android.app.backup.BackupAgentHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = BackupAgentHelper.class.getSimpleName();

	/**
	 * The {@link SharedPreferencesBackupHelper} backup key for the {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}.
	 */
	private static final String PREFS_BACKUP_KEY = "H_prefs";
	/**
	 * The {@link FavoritesBackupHelper} backup key.
	 */
	private static final String FAVS_BACKUP_KEY = "H_favs";

	@Override
	public void onCreate() {
		MyLog.v(TAG, "onCreate()");
		addHelper(PREFS_BACKUP_KEY, new SharedPreferencesBackupHelper(this, UserPreferences.DEFAULT_PREF_NAME));
		addHelper(FAVS_BACKUP_KEY, new FavoritesBackupHelper(this));
	}
}

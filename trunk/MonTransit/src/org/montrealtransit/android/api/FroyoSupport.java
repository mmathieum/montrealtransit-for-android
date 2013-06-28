package org.montrealtransit.android.api;

import org.montrealtransit.android.MyLog;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ListView;

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
	 */
	public FroyoSupport() {
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		super.applySharedPreferencesEditor(editor);
	}

	@Override
	public void backupManagerDataChanged(Context context) {
		new BackupManager(context).dataChanged();
		MyLog.d(TAG, "SharedPreferences changed. BackupManager.dataChanged() fired.");
	}

	@Override
	public float getDisplayRotation(Context context) {
		switch (((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
		case Surface.ROTATION_90:
			return -90f;
		case Surface.ROTATION_180:
			return 0f; // TODO TEST
		case Surface.ROTATION_270:
			return 90f;
		case Surface.ROTATION_0:
		default:
			return 0f;
		}
	}

	@Override
	public int getSurfaceRotation(Context context) {
		return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return 50;
	}

	@Override
	public void listViewScrollTo(ListView listView, int position, int offset) {
		// int firstVisible = listView.getFirstVisiblePosition();
		// int lastVisible = listView.getLastVisiblePosition();
		// if (position < firstVisible) {
		// listView.smoothScrollToPosition(position);
		// } else {
		// listView.smoothScrollToPosition(position + lastVisible - firstVisible
		// - 2);
		// }
		// listView.smoothScrollToPosition(0);
		super.listViewScrollTo(listView, position, offset);
	}
}

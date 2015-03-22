package org.montrealtransit.android.api;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.montrealtransit.android.services.NfcListener;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.StatFs;
import android.view.Surface;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * The methods that all platforms version need to implement.
 * @author Mathieu Méa
 */
public interface SupportUtil {

	/**
	 * Apply shared preferences editor.
	 * @param editor shared preferences editor
	 */
	void applySharedPreferencesEditor(SharedPreferences.Editor editor);

	/**
	 * @return the build manufacturer
	 */
	String getBuildManufacturer();

	/**
	 * @return the {@link AsyncTask} capacity.
	 */
	int getASyncTaskCapacity();

	/**
	 * Notify the {@link BackupManager} that the data has changed.
	 */
	void backupManagerDataChanged(Context context);

	/**
	 * @param activity the activity
	 * @param listener the listener
	 * @param mimeType the MIME type
	 */
	void registerNfcCallback(Activity activity, NfcListener listener, String mimeType);

	/**
	 * @param intent the intent
	 * @return true if the intent is an NFC intent
	 */
	boolean isNfcIntent(Intent intent);

	/**
	 * @param intent the intent
	 * @param listener the NFC listener
	 */
	void processNfcIntent(Intent intent, NfcListener listener);

	/**
	 * @param activity the activity
	 * @param listener the listener
	 */
	void setOnNdefPushCompleteCallback(Activity activity, NfcListener listener);

	/**
	 * Enable NFC foreground dispatch.
	 * @param activity the activity
	 */
	void enableNfcForegroundDispatch(Activity activity);

	/**
	 * Disable NFC foreground dispatch.
	 * @param activity the activity
	 */
	void disableNfcForegroundDispatch(Activity activity);

	/**
	 * @return the display rotation
	 */
	float getDisplayRotation(Context context);

	/**
	 * @return the display rotation {@link Surface#ROTATION_0} ...
	 */
	int getSurfaceRotation(Context context);

	/**
	 * Enable Strict Mode if supported.
	 */
	void enableStrictMode();

	/**
	 * @return the number of the closest POI displayed
	 */
	int getNbClosestPOIDisplay();

	/**
	 * @param configuration the configuration
	 * @return the screen layout size (small, normal, large, xlarge)
	 */
	int getScreenLayoutSize(Configuration configuration);

	/**
	 * @param configuration the configuration
	 * @return true if the screen is small (hide ad, hide non-necessary stuff...)
	 */
	boolean isScreenHeightSmall(Configuration configuration);

	/**
	 * @return the route info class
	 */
	Class<?> getRouteInfoClass();

	/**
	 * @return the bus tab class
	 */
	Class<?> getBusTabClass();

	SimpleCursorAdapter newSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags);

	/**
	 * Scroll list view to a position with offset.
	 * @param listView the list view
	 * @param position the position
	 * @param offset the offset
	 */
	void listViewScrollTo(ListView listView, int position, int offset);

	@SuppressWarnings("rawtypes")
	void executeOnExecutor(AsyncTask task, Executor executor);

	BlockingQueue<Runnable> getNewBlockingQueue();

	void rotateImageView(ImageView img, float rotation, Activity activity);

	long getStatFsAvailableBlocksLong(StatFs statFs);

	long getStatFsBlockSizeLong(StatFs statFs);

	void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, OnGlobalLayoutListener onGlobalLayoutListener);

	void showNewAppNotification(Context context);

	boolean isGeocoderPresent();
}

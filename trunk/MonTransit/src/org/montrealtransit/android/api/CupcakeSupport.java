package org.montrealtransit.android.api;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;
import org.montrealtransit.android.services.NfcListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Matrix;
import android.os.StatFs;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Features available for Android 1.5 Cupcake (API Level 3) and higher.
 * @author Mathieu Méa
 */
@TargetApi(3)
public class CupcakeSupport implements SupportUtil {

	/**
	 * The default constructor.
	 */
	public CupcakeSupport() {
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
	public void backupManagerDataChanged(Context context) {
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

	@Override
	public Class<?> getBusTabClass() {
		return org.montrealtransit.android.activity.BusTab.class;
	}

	@SuppressWarnings("deprecation")
	@Override
	public SimpleCursorAdapter newSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		return new SimpleCursorAdapter(context, layout, c, from, to);
	}

	@Override
	public void listViewScrollTo(ListView listView, int position, int offset) {
		listView.setSelectionFromTop(position, offset);
	}

	@Override
	public void executeOnExecutor(LoadNextBusStopIntoCacheTask task, Executor executor) {
		task.execute();
	}

	@Override
	public BlockingQueue<Runnable> getNewBlockingQueue() {
		return new ArrayBlockingQueue<Runnable>(5);
	}

	@Override
	public void rotateImageView(ImageView img, float rotation, Activity activity) {
		Matrix compassMatrix = new Matrix();
		compassMatrix.postRotate(rotation, SensorUtils.getRotationPx(activity), SensorUtils.getRotationPy(activity));
		img.setImageMatrix(compassMatrix);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public long getStatFsAvailableBlocksLong(StatFs statFs) {
		return (long) statFs.getAvailableBlocks();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public long getStatFsBlockSizeLong(StatFs statFs) {
		return (long) statFs.getBlockSize();
	}
}

package org.montrealtransit.android.api;

import java.util.concurrent.Executor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Features available for Android 3.0 Honeycomb (API Level 11) and higher.
 * @author Mathieu Méa
 */
@TargetApi(11)
public class HoneycombSupport extends GingerbreadSupport {

	/**
	 * The default constructor.
	 */
	public HoneycombSupport() {
	}

	@Override
	public void enableStrictMode() {
		ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyFlashScreen().build();
		// or .detectAll() for all detectable problems
		StrictMode.setThreadPolicy(threadPolicy);
		VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
		StrictMode.setVmPolicy(vmPolicy);
	}

	@Override
	public SimpleCursorAdapter newSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		return new SimpleCursorAdapter(context, layout, c, from, to, flags);
	}

	@Override
	public void listViewScrollTo(ListView listView, int position, int offset) {
		// listView.smoothScrollToPositionFromTop(position, offset);
		super.listViewScrollTo(listView, position, offset);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void executeOnExecutor(AsyncTask task, Executor executor) {
		task.executeOnExecutor(executor);
	}

	@Override
	public void rotateImageView(ImageView img, float rotation, Activity activity) {
		img.setRotation(rotation);
	}
}

package org.montrealtransit.android.api;

import java.util.concurrent.Executor;

import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
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

	@Override
	public void executeOnExecutor(LoadNextBusStopIntoCacheTask task, Executor executor) {
		task.executeOnExecutor(executor);
	}
}

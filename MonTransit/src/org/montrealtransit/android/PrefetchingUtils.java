package org.montrealtransit.android;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.api.SupportFactory;

import android.content.Context;
import android.net.ConnectivityManager;

public class PrefetchingUtils {

	public static final String TAG = PrefetchingUtils.class.getSimpleName();

	public static final boolean PREFETCHING_DISABLED = false; // true to disable all pre-fetching

	private static Boolean prefetching = null;

	private static Boolean prefetchingWiFiOnly = null;

	private static Executor executor;

	public static Executor getExecutor() {
		if (executor == null) {
			int numCore = 1;
			try {
				numCore = getNumCores();
			} catch (Throwable t) {
				MyLog.w(TAG, t, "Error while reading number of core!");
			}
			if (numCore > 1) {
				numCore--; // keep 1 core for the rest of the app
			}
			executor = new ThreadPoolExecutor(1, numCore, 0, TimeUnit.SECONDS, SupportFactory.get().getNewBlockingQueue(), new RejectedExecutionHandler() {

				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					MyLog.d(TAG, "Prefetching task rejected!");
					// too bad
				}
			});
		}
		return executor;
	}

	public static boolean isPrefetching(Context context) {
		if (PREFETCHING_DISABLED) {
			return false;
		}
		if (PrefetchingUtils.prefetching == null) {
			prefetching = UserPreferences.getPrefLcl(context, UserPreferences.PREFS_PREFETCHING, UserPreferences.PREFS_PREFETCHING_DEFAULT);
		}
		return PrefetchingUtils.prefetching;
	}

	public static boolean isPrefetchingWiFiOnly(Context context) {
		if (PREFETCHING_DISABLED) {
			return false;
		}
		if (PrefetchingUtils.prefetchingWiFiOnly == null) {
			prefetchingWiFiOnly = UserPreferences.getPrefLcl(context, UserPreferences.PREFS_PREFETCHING_WIFI_ONLY, true);
		}
		return PrefetchingUtils.prefetchingWiFiOnly;
	}

	/**
	 * Gets the number of cores available in this device, across all processors. Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
	 * @return The number of cores, or 1 if failed to get result
	 */
	private static int getNumCores() {
		// Private Class to display only CPU devices in the directory listing
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				// Check if filename is "cpu", followed by a single digit number
				if (Pattern.matches("cpu[0-9]", pathname.getName())) {
					return true;
				}
				return false;
			}
		}
		try {
			// Get directory containing CPU info
			File dir = new File("/sys/devices/system/cpu/");
			// Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			// Return the number of cores (virtual CPU devices)
			return files.length;
		} catch (Exception e) {
			// Default to return 1 core
			return 1;
		}
	}

	public static boolean isConnectedToWifi(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

	public static void setPrefetching(Boolean prefetching) {
		PrefetchingUtils.prefetching = prefetching;
	}

	public static void setPrefetchingWiFiOnly(Boolean prefetchingWiFiOnly) {
		PrefetchingUtils.prefetchingWiFiOnly = prefetchingWiFiOnly;
	}

}

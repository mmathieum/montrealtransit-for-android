package org.montrealtransit.android;

import org.montrealtransit.android.api.SupportFactory;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * This class contains useful methods to interact with Google Analytics API.
 * @author Mathieu Méa
 */
public class AnalyticsUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = AnalyticsUtils.class.getSimpleName();

	/**
	 * Use this boolean to disable the tracker.
	 */
	private static boolean TRACKING = true;

	// /**
	// * Event category "show".
	// */
	// public static final String CAT_SHOW = "show";
	//
	// /**
	// * Event action "view".
	// */
	// public static final String ACT_VIEW = "view";

	/**
	 * Custom variables scope levels.
	 */
	public static final int SCOPE_VISITOR_LEVEL = 1;
	public static final int SCOPE_SESSION_LEVEL = 2;
	public static final int SCOPE_PAGE_LEVEL = 3;

	/**
	 * The instance.
	 */
	private static AnalyticsUtils instance;

	/**
	 * @return the instance
	 */
	public static AnalyticsUtils getInstance() {
		MyLog.v(TAG, "getInstance()");
		if (instance == null) {
			instance = new AnalyticsUtils();
		}
		return instance;
	}

	/**
	 * The Google Analytics tracker.
	 */
	private static GoogleAnalyticsTracker tracker;

	/**
	 * True if the tracker is started.
	 */
	private static boolean trackerStarted = false;

	/**
	 * @param context the context
	 * @return the Google Analytics tracker
	 */
	private static GoogleAnalyticsTracker getTracker(Context context) {
		MyLog.v(TAG, "getTracker()");
		if (tracker == null) {
			MyLog.v(TAG, "Initializing the Google Analytics tracker...");
			tracker = GoogleAnalyticsTracker.getInstance();
			// initTrackerWithUserData(context);
			MyLog.v(TAG, "Initializing the Google Analytics tracker... DONE");
		}
		if (!trackerStarted) {
			MyLog.v(TAG, "Starting the Google Analytics tracker...");
			tracker.start(context.getString(R.string.google_analytics_id), context);
			trackerStarted = true;
			MyLog.v(TAG, "Starting the Google Analytics tracker... DONE");
		}
		return tracker;
	}

	/**
	 * Initialize the Google Analytics tracker with user device properties.
	 * @param context the context
	 */
	private static void initTrackerWithUserData(Context context) {
		// only 5 Custom Variables allowed!
		// 1 - Application version
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			String appVersion = packageInfo.versionName;
			// MyLog.v(TAG, "app_version: '%s'.", appVersion);
			getTracker(context).setCustomVar(1, "app_version", appVersion, SCOPE_VISITOR_LEVEL);
			//getTracker(context).setProductVersion("MonTransit", appVersion);
		} catch (NameNotFoundException e) {
		}
		// 2 - Android version
		String androidVersion = Build.VERSION.RELEASE;
		// MyLog.v(TAG, "os_rel: '%s'.", androidVersion);
		getTracker(context).setCustomVar(1, "os_rel", androidVersion, SCOPE_VISITOR_LEVEL);
		// 3 - Android SDK
		String sdk = Build.VERSION.SDK;
		// MyLog.v(TAG, "sdk_version: '%s'.", sdk);
		getTracker(context).setCustomVar(1, "sdk_version", sdk, SCOPE_VISITOR_LEVEL);
		// 4 - Device
		String device = "";
		if (Integer.parseInt(Build.VERSION.SDK) >= Build.VERSION_CODES.DONUT) {
			device += SupportFactory.getInstance(context).getBuildManufacturer() + " ";
		}
		device += Build.MODEL;
		// MyLog.v(TAG, "device: '%s'.", device);
		getTracker(context).setCustomVar(1, "device", device, SCOPE_VISITOR_LEVEL);
		// already provided by Analytics:
		// user language + country
		// Mobile carrier
	}

	/**
	 * Track an event.
	 * @param context the context
	 * @param category the event category
	 * @param action the event action
	 * @param label the event label
	 * @param value the event value
	 */
	public static void trackEvent(Context context, String category, String action, String label, int value) {
		MyLog.v(TAG, "trackEvent()");
		if (TRACKING) {
			initTrackerWithUserData(context);
			getTracker(context).trackEvent(category, action, label, value);
		}
	}

	/**
	 * Track a page view.
	 * @param context the context
	 * @param page the viewed page.
	 */
	public static void trackPageView(Context context, String page) {
		MyLog.v(TAG, "trackPageView(%s)", page);
		if (TRACKING) {
			try {
				initTrackerWithUserData(context);
				getTracker(context).trackPageView(page);
			} catch (Throwable t) {
				MyLog.w(TAG, t, "Error while tracing view.");
			}
		}
	}

	/**
	 * Dispatch the Google Analytics tracker content.
	 * @param context the context
	 */
	public static void dispatch(Context context) {
		MyLog.v(TAG, "dispatch()");
		if (TRACKING) {
			try {
				getTracker(context).dispatch();
			} catch (Throwable t) {
				MyLog.w(TAG, t, "Error while dispatching analytics data.");
			}
		}
	}

	/**
	 * Stop the Google Analytics tracker
	 * @param context the context
	 */
	public static void stop(Context context) {
		MyLog.v(TAG, "stop()");
		if (TRACKING) {
			getTracker(context).stop();
			trackerStarted = false;
		}
	}
}

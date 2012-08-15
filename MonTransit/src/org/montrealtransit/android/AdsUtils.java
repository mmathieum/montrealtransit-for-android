package org.montrealtransit.android;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.montrealtransit.android.activity.UserPreferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.view.View;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdView;

/**
 * This class contains useful methods to interact with AdMob SDK.
 * @author Mathieu Méa
 */
public class AdsUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = AdsUtils.class.getSimpleName();

	/**
	 * True if currently debugging ads.
	 */
	private static boolean DEBUG = false;

	/**
	 * True if showing ads.
	 */
	private static Boolean showingAds = null;

	/**
	 * True if the user donate (bought a Donate app from the Market).
	 */
	private static Boolean generousUser = null;

	/**
	 * Set to false to disable ads.
	 */
	public static final boolean AD_ENABLED = true;

	/**
	 * Ads keywords.
	 */
	private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(new String[] { "montreal", "transit",
	        "STM", "bus", "subway", "metro", "taxi", "quebec", "canada" }));

	/**
	 * The donate apps package name.
	 */
	private static final String DONATE_PACKAGES_START_WITH = "org.montrealtransit.android.donate";

	/**
	 * Setup the ad in the activity.
	 * @param activity the activity
	 */
	public static void setupAd(final Activity activity) {
		MyLog.v(TAG, "setupAd()");

		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return AD_ENABLED && isShowingAds(activity);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				View adLayout = activity.findViewById(R.id.ad_layout);
				if (result) {
					// show ads
					if (adLayout != null) {
						adLayout.setVisibility(View.VISIBLE);
						AdView adView = (AdView) adLayout.findViewById(R.id.ad);
						// IF the ad view is present in the layout AND not already loaded DO
						if (adView != null && !adView.isReady()) {
							adView.setVisibility(View.VISIBLE);
							AdRequest adRequest = new AdRequest();
							adRequest.setLocation(LocationUtils.getBestLastKnownLocation(activity));
							adRequest.setKeywords(KEYWORDS);
							if (DEBUG) {
								adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
								adRequest.addTestDevice(activity.getString(R.string.admob_test_device_id));
								adView.setAdListener(new AdListener() {
									@Override
									public void onDismissScreen(Ad ad) {
										MyLog.v(TAG, "onDismissScreen()");
									}

									@Override
									public void onFailedToReceiveAd(Ad ad, ErrorCode errorCode) {
										MyLog.v(TAG, "onFailedToReceiveAd()");
										MyLog.w(TAG, "Failed to received ad! Error code: '%s'.", errorCode);
									}

									@Override
									public void onLeaveApplication(Ad ad) {
										MyLog.v(TAG, "onLeaveApplication()");
									}

									@Override
									public void onPresentScreen(Ad ad) {
										MyLog.v(TAG, "onPresentScreen()");
									}

									@Override
									public void onReceiveAd(Ad ad) {
										MyLog.v(TAG, "onReceiveAd()");
									}
								});
							}
							adView.loadAd(adRequest);
						}
					}
				} else {
					// hide ads
					if (adLayout != null) {
						adLayout.setVisibility(View.GONE);
						AdView adView = (AdView) adLayout.findViewById(R.id.ad);
						if (adView != null) {
							adView.setVisibility(View.GONE);
						}
					}
				}
			};
		}.execute();
	}

	/**
	 * Destroy the ad in the activity.
	 * @param activity the activity
	 */
	public static void destroyAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.stopLoading();
					adView.destroy();
				}
			}
		}
	}

	/**
	 * @param context the context
	 * @return true if showing ads
	 */
	public static boolean isShowingAds(Context context) {
		MyLog.v(TAG, "isShowingAds()");
		if (AdsUtils.showingAds == null) {
			// IF the user is generous DO
			if (isGenerousUser(context)) {
				// the user has the right to choose not to display ads
				AdsUtils.showingAds = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_ADS,
				        UserPreferences.PREFS_ADS_DEFAULT);
			} else {
				AdsUtils.showingAds = true;
				UserPreferences.savePrefDefault(context, UserPreferences.PREFS_ADS, AdsUtils.showingAds);
			}
		}
		return AD_ENABLED ? AdsUtils.showingAds : AD_ENABLED;
	}

	/**
	 * @param showingAds set showing ads
	 */
	public static void setShowingAds(Boolean showingAds) {
		AdsUtils.showingAds = showingAds;
	}

	/**
	 * @param context the context
	 * @return true if the user is generous
	 */
	public static boolean isGenerousUser(Context context) {
		MyLog.v(TAG, "isGenerousUser()");
		if (AdsUtils.generousUser == null) {
			generousUser = false;
			final PackageManager packageManager = context.getPackageManager();
			for (PackageInfo pkg : packageManager.getInstalledPackages(0)) {
				if (pkg.packageName.startsWith(DONATE_PACKAGES_START_WITH)) {
					AdsUtils.generousUser = true;
					// TODO check that the app was bought (Google Play Store Licensing)
					break;
				}
			}
		}
		return AdsUtils.generousUser;
	}

	/**
	 * @param generousUser set if the user is generous
	 */
	public static void setGenerousUser(Boolean generousUser) {
		AdsUtils.generousUser = generousUser;
	}
}

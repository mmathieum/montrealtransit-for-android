package org.montrealtransit.android;

import org.montrealtransit.android.activity.UserPreferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;

/**
 * This class contains useful methods to interact with AdMob SDK.
 * @author Mathieu Méa
 */
public class AdsUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = UserPreferences.class.getSimpleName();

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
	private static final String KEYWORDS = "montreal transit STM bus subway metro taxi quebec canada";

	/**
	 * The donate apps package name.
	 */
	private static final String DONATE_PACKAGES_START_WITH = "org.montrealtransit.android.donate";

	public static void setupAd(Activity context) {
		MyLog.v(TAG, "setupAd()");

		if (DEBUG) {
			AdManager.setTestDevices(new String[] { AdManager.TEST_EMULATOR, "BF250C0623034125E60A4E9584BA488F",
			        "8D352C9046AC598F81D532CC8DB1F170" });
		}

		View adLayout = context.findViewById(R.id.ad_layout);
		if (AD_ENABLED && isShowingAds(context)) {
			// show ads
			if (adLayout != null) {
				adLayout.setVisibility(View.VISIBLE);
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.setVisibility(View.VISIBLE);
					adView.setKeywords(KEYWORDS);
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
	}

	public static boolean isShowingAds(Context context) {
		MyLog.v(TAG, "isShowingAds()");
		if (AdsUtils.showingAds == null) {
			// IF the user is generous DO
			if (isGenerousUser(context)) {
				// the user has the right to choose not to display ads
				AdsUtils.showingAds = Utils.getSharedPreferences(context, UserPreferences.PREFS_ADS,
				        UserPreferences.PREFS_ADS_DEFAULT);
			} else {
				AdsUtils.showingAds = true;
				Utils.saveSharedPreferences(context, UserPreferences.PREFS_ADS, AdsUtils.showingAds);
			}
		}
		return AD_ENABLED ? AdsUtils.showingAds : AD_ENABLED;
	}

	public static void setShowingAds(Boolean showingAds) {
		AdsUtils.showingAds = showingAds;
	}

	public static boolean isGenerousUser(Context context) {
		MyLog.v(TAG, "isGenerousUser()");
		if (AdsUtils.generousUser == null) {
			generousUser = false;
			final PackageManager packageManager = context.getPackageManager();
			for (PackageInfo pkg : packageManager.getInstalledPackages(0)) {
				if (pkg.packageName.startsWith(DONATE_PACKAGES_START_WITH)) {
					AdsUtils.generousUser = true;
					// TODO check that the app was bought (Android Market Licensing)
					break;
				}
			}
		}
		return AdsUtils.generousUser;
	}

	public static void setGenerousUser(Boolean generousUser) {
		AdsUtils.generousUser = generousUser;
	}

}

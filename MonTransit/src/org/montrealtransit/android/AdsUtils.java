package org.montrealtransit.android;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.montrealtransit.android.activity.UserPreferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * This class contains useful methods to interact with Google Mobile Ads SDK.
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
	private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(new String[] { "montreal", "transit", "STM", "bus", "subway", "metro",
			"taxi", "quebec", "canada", "bixi", "bike", "sharing", "velo" }));

	/**
	 * The donate application package name.
	 */
	private static final String DONATE_PACKAGES_START_WITH = "org.montrealtransit.android.donate";

	/**
	 * Setup the ad in the activity.
	 * @param activity the activity
	 */
	@SuppressWarnings("deprecation")
	public static void setupAd(final Activity activity) {
		MyLog.v(TAG, "setupAd()");
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.GINGERBREAD) {
			return; // no ads before 2.3 (limited support)
		}
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return AD_ENABLED && isShowingAds(activity);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					// show ads
					View adLayout = activity.findViewById(R.id.ad_layout);
					if (adLayout != null) {
						AdView adView = (AdView) adLayout.findViewById(R.id.ad);
						// IF the ad view is present in the layout AND not already loaded DO
						if (adView != null) {
							AdRequest.Builder adRequestBd = new AdRequest.Builder();
							adRequestBd.setLocation(LocationUtils.getBestLastKnownLocation(activity));
							for (String keyword : KEYWORDS) {
								adRequestBd.addKeyword(keyword);
							}
							adView.setAdListener(new AdListener() {

								@Override
								public void onAdFailedToLoad(int errorCode) {
									MyLog.v(TAG, "onAdFailedToLoad(%s)", errorCode);
									super.onAdFailedToLoad(errorCode);
									// print error code
									switch (errorCode) {
									case AdRequest.ERROR_CODE_INTERNAL_ERROR:
										MyLog.w(TAG, "Failed to received ad! Internal error code: '%s'.", errorCode);
										break;
									case AdRequest.ERROR_CODE_INVALID_REQUEST:
										MyLog.w(TAG, "Failed to received ad! Invalid request error code: '%s'.", errorCode);
										break;
									case AdRequest.ERROR_CODE_NETWORK_ERROR:
										MyLog.w(TAG, "Failed to received ad! Network error code: '%s'.", errorCode);
										break;
									case AdRequest.ERROR_CODE_NO_FILL:
										MyLog.w(TAG, "Failed to received ad! No fill error code: '%s'.", errorCode);
										break;
									default:
										MyLog.w(TAG, "Failed to received ad! Error code: '%s'.", errorCode);
									}
									// hide ads
									hideAds(activity);
								}

								@Override
								public void onAdLoaded() {
									MyLog.v(TAG, "onAdLoaded()");
									super.onAdLoaded();
									// show ads
									showAds(activity);
								}

								@Override
								public void onAdClosed() {
									MyLog.v(TAG, "onAdClosed()");
									super.onAdClosed();
								}

								@Override
								public void onAdLeftApplication() {
									MyLog.v(TAG, "onAdLeftApplication()");
									super.onAdLeftApplication();
								}

								@Override
								public void onAdOpened() {
									MyLog.v(TAG, "onAdOpened()");
									super.onAdOpened();
								}
							});
							if (DEBUG) {
								adRequestBd.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
								adRequestBd.addTestDevice(activity.getString(R.string.admob_test_device_id));
							}
							adView.loadAd(adRequestBd.build()); // StrictModeDiskReadViolation
						}
					}
				} else {
					// hide ads
					hideAds(activity);
				}
			};
		}.execute();
	}

	/**
	 * Show the ads in the activity.
	 * @param activity the activity
	 */
	public static void showAds(Activity activity) {
		MyLog.v(TAG, "showAds()");
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.VISIBLE);
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Hide the ads in the activity.
	 * @param activity the activity
	 */
	public static void hideAds(Activity activity) {
		MyLog.v(TAG, "hideAds()");
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.GONE);
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * Pause the ad in the activity.
	 * @param activity the activity
	 */
	public static void pauseAd(Activity activity) {
		MyLog.v(TAG, "pauseAd()");
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.pause();
				}
			}
		}
	}

	/**
	 * Resume the ad in the activity.
	 * @param activity the activity
	 */
	public static void resumeAd(Activity activity) {
		MyLog.v(TAG, "resumeAd()");
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.resume();
				}
			}
		}
	}

	/**
	 * Destroy the ad in the activity.
	 * @param activity the activity
	 */
	public static void destroyAd(Activity activity) {
		MyLog.v(TAG, "destroyAd()");
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.removeAllViews();
					adView.destroy();
				}
			}
		}
	}

	/**
	 * @param context the context
	 * @return true if showing ads
	 */
	@SuppressWarnings("deprecation")
	public static boolean isShowingAds(Context context) {
		MyLog.v(TAG, "isShowingAds()");
		if (!AD_ENABLED) {
			return false;
		}
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.GINGERBREAD) {
			return false; // no ads before 2.3 (limited support)
		}
		if (AdsUtils.showingAds == null) {
			// IF the user is generous DO
			if (isGenerousUser(context)) {
				// the user has the right to choose not to display ads
				AdsUtils.showingAds = UserPreferences.getPrefDefault(context, UserPreferences.PREFS_ADS, UserPreferences.PREFS_ADS_DEFAULT);
			} else {
				AdsUtils.showingAds = true;
				UserPreferences.savePrefDefault(context, UserPreferences.PREFS_ADS, AdsUtils.showingAds);
			}
		}
		return AdsUtils.showingAds;
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
			for (PackageInfo pkg : context.getPackageManager().getInstalledPackages(0)) {
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

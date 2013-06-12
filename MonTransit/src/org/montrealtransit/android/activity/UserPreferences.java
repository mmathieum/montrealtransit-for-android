package org.montrealtransit.android.activity;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.DataManager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

/**
 * The user preferences activity.
 * @author Mathieu Méa
 */
public class UserPreferences extends PreferenceActivity {

	/**
	 * The log tag.
	 */
	private static final String TAG = UserPreferences.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Preferences";

	/**
	 * The name of the <b>default</b> {@link SharedPreferences} according to {@link PreferenceManager#getDefaultSharedPreferences(android.content.Context)}
	 * source code.
	 */
	public static final String DEFAULT_PREF_NAME = Constant.PKG + "_preferences";

	/**
	 * The name of the local preferences (no backup).
	 */
	public static final String LCL_PREF_NAME = "lcl";

	/**
	 * The preference key for the bus lines list display.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY = "pBusLineListGroupBy";
	/**
	 * The preference value for the bus lines list display without group by.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP = "no";
	/**
	 * The preference value for the bus lines list display group by line number.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_NUMBER = "number";
	/**
	 * The preference value for the bus lines list display group by line type.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_TYPE = "type";
	/**
	 * The preference value for the bus lines list display group by line day/night.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_DAY_NIGHT = "daynight";
	/**
	 * The default value for the bus lines display.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_DEFAULT = PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP;

	/**
	 * The preference key for the next stop provider V2.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER = "pNextStopProvider2";
	/**
	 * The preference value for automatic next stop provider.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER_AUTO = "auto";
	/**
	 * The preference value for the next stop provider stm.info.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER_STM_INFO = "stminfo";
	/**
	 * The preference value for the next stop provider m.stm.info.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER_STM_MOBILE = "stmmobile";
	/**
	 * The default value for the bus lines display.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER_DEFAULT = PREFS_NEXT_STOP_PROVIDER_AUTO;

	/**
	 * The preference key for the search.
	 */
	public static final String PREFS_SEARCH = "pSearch";
	/**
	 * The preference value for the search simple.
	 */
	public static final String PREFS_SEARCH_SIMPLE = "simple";
	/**
	 * The preference value for the search extended.
	 */
	public static final String PREFS_SEARCH_EXTENDED = "extended";
	/**
	 * The default value for the search.
	 */
	public static final String PREFS_SEARCH_DEFAULT = PREFS_SEARCH_SIMPLE;

	/**
	 * The preference key for the distance display.
	 */
	public static final String PREFS_DISTANCE = "pDistanceDisplay";
	/**
	 * The preference value for the simple distance display (example: < 1 km).
	 */
	public static final String PREFS_DISTANCE_SIMPLE = "simple";
	/**
	 * The preference value for the detailed distance display (example: 0.8-1 km).
	 */
	public static final String PREFS_DISTANCE_DETAILED = "detailed";
	/**
	 * The default value for the distance display.
	 */
	public static final String PREFS_DISTANCE_DEFAULT = PREFS_DISTANCE_SIMPLE;

	/**
	 * The preference key for the distance unit.
	 */
	public static final String PREFS_DISTANCE_UNIT = "pDistanceUnit";
	/**
	 * The preference value for the meter unit.
	 */
	public static final String PREFS_DISTANCE_UNIT_METER = "meter";
	/**
	 * The preference value for the imperial unit.
	 */
	public static final String PREFS_DISTANCE_UNIT_IMPERIAL = "imperial";
	/**
	 * The default value for the distance display.
	 */
	public static final String PREFS_DISTANCE_UNIT_DEFAULT = PREFS_DISTANCE_UNIT_METER;

	/**
	 * The preference key for clearing the cache.
	 */
	public static final String PREFS_CLEAR_CACHE = "pClearCache";

	/**
	 * The preference key for the subway line stations display order. <b>WARNING:</b> To be used with the subway line number at the end. Use
	 * {@link UserPreferences#getPrefsSubwayStationsOrder(int)} to get the key.
	 */
	private static final String PREFS_SUBWAY_STATIONS_ORDER = "pSubwayStationOrder";
	/**
	 * The preference value for the natural order 1.
	 */
	public static final String PREFS_SUBWAY_STATIONS_ORDER_NATURAL = "asc";
	/**
	 * The preference value for the natural order 2.
	 */
	public static final String PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC = "desc";
	/**
	 * The default value for the subway stations order.
	 */
	public static final String PREFS_SUBWAY_STATIONS_ORDER_DEFAULT = PREFS_SUBWAY_STATIONS_ORDER_NATURAL;

	/**
	 * The preference key for pre-fetching.
	 */
	public static final String PREFS_PREFETCHING = "pPrefetching";
	/**
	 * The default value for the pre-fetching.
	 */
	public static final boolean PREFS_PREFETCHING_DEFAULT = true;
	/**
	 * The preference key for pre-fetching over WiFi only.
	 */
	public static final String PREFS_PREFETCHING_WIFI_ONLY = "pPrefetchingWifiOnly";
	/**
	 * The default value for the pre-fetching over WiFi only.
	 */
	public static final boolean PREFS_PREFETCHING_WIFI_ONLY_DEFAULT = true;

	/**
	 * The preference key for ads.
	 */
	public static final String PREFS_ADS = "pAds";
	/**
	 * The default value for the ads.
	 */
	public static final boolean PREFS_ADS_DEFAULT = true;

	/**
	 * The preference key for the presence of favorite.
	 */
	public static final String PREFS_LCL_IS_FAV = "pFav";
	/**
	 * Default value for favorite.
	 */
	public static final boolean PREFS_LCL_IS_FAV_DEFAULT = false;

	/**
	 * The preference key for the opening tab.
	 */
	public static final String PREFS_LCL_TAB = "pTab";
	/**
	 * Default value for the opening tab (stop code).
	 */
	public static final int PREFS_LCL_TAB_DEFAULT = 1;

	/**
	 * The preference key for the opening bus tab.
	 */
	public static final String PREFS_LCL_BUS_TAB = "pBusTab";
	/**
	 * Default value for the opening bus tab (stop code).
	 */
	public static final int PREFS_LCL_BUS_TAB_DEFAULT = 0;

	/**
	 * The latest version of the STM DB successfully deployed.
	 */
	public static final String PREFS_LCL_STM_DB_VERSION = "pStmDbVersion";

	/**
	 * The latest update of the Bixi database.
	 */
	public static final String PREFS_LCL_BIXI_LAST_UPDATE = "pBixiLastUpdate";

	/**
	 * The ads check box.
	 */
	private CheckBoxPreference adsCheckBox;

	private CheckBoxPreference prefetchCb;

	private CheckBoxPreference prefetchWiFiOnlyCb;

	/**
	 * The number of click on the version.
	 */
	private int versionCount;

	@SuppressWarnings("deprecation")
	// TODO use PreferenceActivity
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.userpreferences);

		// // prefetch
		// this.prefetchCb = (CheckBoxPreference) findPreference(PREFS_PREFETCHING);
		// this.prefetchWiFiOnlyCb = (CheckBoxPreference) findPreference(PREFS_PREFETCHING_WIFI_ONLY);
		// this.prefetchCb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		//
		// @Override
		// public boolean onPreferenceClick(Preference preference) {
		// Toast.makeText(UserPreferences.this, "prefetching changed " + UserPreferences.this.prefetchCb.isChecked(), Toast.LENGTH_SHORT).show();
		// // boolean isNowChecked = UserPreferences.getPrefLcl(UserPreferences.this, PREFS_PREFETCHING, PREFS_PREFETCHING_DEFAULT);
		// UserPreferences.savePrefLcl(UserPreferences.this, PREFS_PREFETCHING_WIFI_ONLY, UserPreferences.this.prefetchCb.isChecked());
		// UserPreferences.this.prefetchWiFiOnlyCb.setEnabled(UserPreferences.this.prefetchCb.isChecked());
		// PrefetchingUtils.setPrefetching(null); // force value refresh
		// return true;
		// }
		// });
		// this.prefetchWiFiOnlyCb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		//
		// @Override
		// public boolean onPreferenceClick(Preference preference) {
		// Toast.makeText(UserPreferences.this, "prefetching wifi only changed " + UserPreferences.this.prefetchWiFiOnlyCb.isChecked(), Toast.LENGTH_SHORT)
		// .show();
		// UserPreferences.savePrefLcl(UserPreferences.this, PREFS_PREFETCHING_WIFI_ONLY, UserPreferences.this.prefetchWiFiOnlyCb.isChecked());
		// PrefetchingUtils.setPrefetchingWiFiOnly(null); // force value refresh
		// return true;
		// }
		// });

		// ads dialog
		this.adsCheckBox = (CheckBoxPreference) findPreference("pAds");
		this.adsCheckBox.setChecked(AdsUtils.isShowingAds(this));
		this.adsCheckBox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				boolean isNowChecked = UserPreferences.getPrefDefault(UserPreferences.this, PREFS_ADS, PREFS_ADS_DEFAULT);
				// IF the user tries to disable ads AND the user didn't donate
				// DO
				if (!isNowChecked && !AdsUtils.isGenerousUser(UserPreferences.this)) {
					// TODO show a dialog explaining that, to remove ads, the
					// user need to:
					// block ads system wide on rooted device
					// download the code of the app an block ads in the source
					// code
					// or donate to support the development of the application
					Utils.notifyTheUserLong(UserPreferences.this, UserPreferences.this.getString(R.string.donate_to_remove_ads));
					UserPreferences.this.adsCheckBox.setChecked(true);

					Uri appMarketURI = Uri.parse("market://search?q=pub:\"Mathieu Méa\"");
					Intent appMarketIntent = new Intent(Intent.ACTION_VIEW).setData(appMarketURI);
					UserPreferences.this.startActivity(appMarketIntent);
					AdsUtils.setGenerousUser(null); // reset generous user
					return true;
				} else {
					AdsUtils.setShowingAds(isNowChecked);
					return false;
				}
			}
		});

		// clear cache //TODO confirmation dialog
		((PreferenceScreen) findPreference(PREFS_CLEAR_CACHE)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				DataManager.deleteAllCache(getContentResolver());
				Utils.notifyTheUser(getApplicationContext(), UserPreferences.this.getString(R.string.clear_cache_complete));
				setClearCachePref();
				return false;
			}
		});

		// donate
		((PreferenceScreen) findPreference("pDonate")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				// Google Play Store
				Utils.notifyTheUser(UserPreferences.this, getString(R.string.donate_opening_market));
				Uri appMarketURI = Uri.parse("market://details?id=" + Constant.DONATE_PKG);
				Intent appMarketIntent = new Intent(Intent.ACTION_VIEW).setData(appMarketURI);
				startActivity(appMarketIntent);
				return false;
			}
		});
		// about dialog
		((PreferenceScreen) findPreference("pAbout")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Utils.showAboutDialog(UserPreferences.this);
				return false;
			}
		});
		// version
		String versionName = "";
		String versionCode = "";
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(Constant.PKG, 0);
			versionName = packageInfo.versionName;
			versionCode = String.valueOf(packageInfo.versionCode);
		} catch (NameNotFoundException e) {
		}
		PreferenceScreen versionPS = (PreferenceScreen) findPreference("pVersion");
		versionPS.setSummary(getString(R.string.version_pref_summary, versionName, versionCode));
		versionPS.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				UserPreferences.this.versionCount++;
				if (UserPreferences.this.versionCount >= 5) {
					// show dialog
					new AlertDialog.Builder(UserPreferences.this).setTitle(R.string.demo_title).setMessage(R.string.demo_message)
							.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Utils.setDemoMode(UserPreferences.this);
								}
							}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							}).setCancelable(true).create().show();

					UserPreferences.this.versionCount = 0; // reset counter
				}
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);

		setClearCachePref();

		super.onResume();
	}

	/**
	 * Setup the clear cache preference.
	 */
	private void setClearCachePref() {
		@SuppressWarnings("deprecation")
		// TODO use PreferenceActivity
		PreferenceScreen clearCachePref = (PreferenceScreen) findPreference(PREFS_CLEAR_CACHE);
		clearCachePref.setEnabled(DataManager.findAllCacheList(getContentResolver()) != null);
		clearCachePref.setSummary(clearCachePref.isEnabled() ? R.string.clear_cache_pref_summary : R.string.clear_cache_pref_summary_disabled);
	}

	/**
	 * @param number the subway line number
	 * @return the PREFS_SUBWAY_STATIONS_ORDER+number key.
	 */
	public static String getPrefsSubwayStationsOrder(int number) {
		return PREFS_SUBWAY_STATIONS_ORDER + number;
	}

	/**
	 * Save {@link String} {@link SharedPreferences} in {@link UserPreferences#DEFAULT_PREF_NAME}.
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value
	 */
	public static void savePrefDefault(Context context, String prefKey, String newValue) {
		MyLog.v(TAG, "savePrefDefault(%s,%s)", prefKey, newValue);
		savePref(context, PreferenceManager.getDefaultSharedPreferences(context), prefKey, newValue);
		SupportFactory.getInstance(context).backupManagerDataChanged();
	}

	/**
	 * Save {@link String} {@link SharedPreferences} in {@link UserPreferences#LCL_PREF_NAME}.
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value
	 */
	public static void savePrefLcl(Context context, String prefKey, String newValue) {
		savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	@SuppressLint("CommitPrefEdits")
	private static void savePref(Context context, SharedPreferences sharedPreferences, String prefKey, String newValue) {
		// MyLog.v(TAG, "savePref(%s, %s)", prefKey, newValue);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(prefKey, newValue);
		SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
	}

	/**
	 * Save {@link Boolean} {@link SharedPreferences} in {@link UserPreferences#DEFAULT_PREF_NAME}.
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value
	 */
	public static void savePrefDefault(Context context, String prefKey, boolean newValue) {
		MyLog.v(TAG, "savePrefDefault(%s,%s)", prefKey, newValue);
		savePref(context, PreferenceManager.getDefaultSharedPreferences(context), prefKey, newValue);
		SupportFactory.getInstance(context).backupManagerDataChanged();
	}

	/**
	 * Save {@link Boolean} {@link SharedPreferences} in {@link UserPreferences#LCL_PREF_NAME}.
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value
	 */
	public static void savePrefLcl(Context context, String prefKey, boolean newValue) {
		savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	@SuppressLint("CommitPrefEdits")
	private static void savePref(Context context, SharedPreferences sharedPreferences, String prefKey, boolean newValue) {
		// MyLog.v(TAG, "savePref(%s, %s)", prefKey, newValue);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(prefKey, newValue);
		SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
	}

	/**
	 * Save {@link Integer} {@link SharedPreferences} in {@link UserPreferences#DEFAULT_PREF_NAME}.
	 * 
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value.
	 */
	public static void savePrefDefault(Context context, String prefKey, int newValue) {
		MyLog.v(TAG, "savePrefDefault(%s,%s)", prefKey, newValue);
		savePref(context, PreferenceManager.getDefaultSharedPreferences(context), prefKey, newValue);
		SupportFactory.getInstance(context).backupManagerDataChanged();
	}

	/**
	 * Save {@link Integer} {@link SharedPreferences} in {@link UserPreferences#LCL_PREF_NAME}.
	 * @param context the context
	 * @param prefKey the preference key
	 * @param newValue the new value
	 */
	public static void savePrefLcl(Context context, String prefKey, int newValue) {
		savePref(context, context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, newValue);
	}

	/**
	 * Save a new preference value.
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param newValue the new preference value
	 */
	@SuppressLint("CommitPrefEdits")
	private static void savePref(Context context, SharedPreferences sharedPreferences, String prefKey, int newValue) {
		// MyLog.v(TAG, "savePref(%s, %s)", prefKey, newValue);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putInt(prefKey, newValue);
		SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link String} {@link SharedPreferences} from {@link UserPreferences#DEFAULT_PREF_NAME}
	 */
	public static String getPrefDefault(Context context, String prefKey, String defaultValue) {
		if (context == null) {
			MyLog.d(TAG, "Context null, using default value '%s' for preference '%s'!", defaultValue, prefKey);
			return defaultValue;
		}
		return getPref(PreferenceManager.getDefaultSharedPreferences(context), prefKey, defaultValue);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link String} {@link SharedPreferences} from {@link UserPreferences#LCL_PREF_NAME}
	 */
	public static String getPrefLcl(Context context, String prefKey, String defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	/**
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value
	 * @return the preference value
	 */
	private static String getPref(SharedPreferences sharedPreferences, String prefKey, String defaultValue) {
		// MyLog.v(TAG, "getPref(%s, %s)", prefKey, defaultValue);
		return sharedPreferences.getString(prefKey, defaultValue);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link Boolean} {@link SharedPreferences} from {@link UserPreferences#DEFAULT_PREF_NAME}
	 */
	public static boolean getPrefDefault(Context context, String prefKey, boolean defaultValue) {
		return getPref(PreferenceManager.getDefaultSharedPreferences(context), prefKey, defaultValue);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link Boolean} {@link SharedPreferences} from {@link UserPreferences#LCL_PREF_NAME}
	 */
	public static boolean getPrefLcl(Context context, String prefKey, boolean defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	/**
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value
	 * @return the preference value
	 */
	private static boolean getPref(SharedPreferences sharedPreferences, String prefKey, boolean defaultValue) {
		// MyLog.v(TAG, "getPref(%s, %s)", prefKey, defaultValue);
		return sharedPreferences.getBoolean(prefKey, defaultValue);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link Integer} {@link SharedPreferences} from {@link UserPreferences#DEFAULT_PREF_NAME}
	 */
	public static int getPrefDefault(Context context, String prefKey, int defaultValue) {
		return getPref(PreferenceManager.getDefaultSharedPreferences(context), prefKey, defaultValue);
	}

	/**
	 * @param context the context
	 * @param prefKey the preference key
	 * @param defaultValue the default value
	 * @return the {@link Integer} {@link SharedPreferences} from {@link UserPreferences#LCL_PREF_NAME}
	 */
	public static int getPrefLcl(Context context, String prefKey, int defaultValue) {
		return getPref(context.getSharedPreferences(LCL_PREF_NAME, Context.MODE_PRIVATE), prefKey, defaultValue);
	}

	/**
	 * @param context the context calling the method
	 * @param prefKey the preference key
	 * @param defaultValue the default value if no value
	 * @return the preference value
	 */
	private static int getPref(SharedPreferences sharedPreferences, String prefKey, int defaultValue) {
		// MyLog.v(TAG, "getPref(%s, %s)", prefKey, defaultValue);
		return sharedPreferences.getInt(prefKey, defaultValue);
	}

}

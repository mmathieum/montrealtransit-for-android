package org.montrealtransit.android.activity;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
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
	 * The default value for the bus lines display.
	 */
	public static final String PREFS_BUS_LINE_LIST_GROUP_BY_DEFAULT = PREFS_BUS_LINE_LIST_GROUP_BY_NO_GROUP;

	/**
	 * The preference key for the next stop provider.
	 */
	public static final String PREFS_NEXT_STOP_PROVIDER = "pNextStopProvider";
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
	public static final String PREFS_NEXT_STOP_PROVIDER_DEFAULT = PREFS_NEXT_STOP_PROVIDER_STM_MOBILE;

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
	 * The preference key for the subway line stations display order. <b>WARNING:</b> To be used with the subway line number at the end. Use
	 * {@link UserPreferences#getPrefsSubwayStationsOrder(int)} to get the key.
	 */
	private static final String PREFS_SUBWAY_STATIONS_ORDER = "pSubwayStationOrder";
	/**
	 * The preference value for A-Z order.
	 */
	public static final String PREFS_SUBWAY_STATIONS_ORDER_AZ = "az";
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
	public static final String PREFS_SUBWAY_STATIONS_ORDER_DEFAULT = PREFS_SUBWAY_STATIONS_ORDER_AZ;

	/**
	 * The preference key for the presence of favorite.
	 */
	public static final String PREFS_IS_FAV = "pFav";

	/**
	 * Default value for favorite.
	 */
	public static final boolean PREFS_IS_FAV_DEFAULT = false;

	/**
	 * The preference key for ads.
	 */
	public static final String PREFS_ADS = "pAds";
	/**
	 * The default value for the ads.
	 */
	public static final boolean PREFS_ADS_DEFAULT = true;

	/**
	 * The ads check box.
	 */
	private CheckBoxPreference adsCheckBox;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.userpreferences);

		// ads dialog
		this.adsCheckBox = (CheckBoxPreference) findPreference("pAds");
		this.adsCheckBox.setChecked(AdsUtils.isShowingAds(this));
		this.adsCheckBox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				boolean isNowChecked = Utils.getSharedPreferences(UserPreferences.this, PREFS_ADS, PREFS_ADS_DEFAULT);
				// IF the user tries to disable ads AND the user didn't donate DO
				if (!isNowChecked && !AdsUtils.isGenerousUser(UserPreferences.this)) {
					// TODO show a dialog explaining that, to remove ads, the user need to:
					// block ads system wide on rooted device
					// download the code of the app an block ads in the source code
					// or donate to support the development of the application
					Utils.notifyTheUserLong(UserPreferences.this, UserPreferences.this.getString(R.string.donate_to_remove_ads));
					UserPreferences.this.adsCheckBox.setChecked(true);

					Uri appMarketURI = Uri.parse("market://search?q=pub:\"Mathieu Méa\"");
					Intent appMarketIntent = new Intent(Intent.ACTION_VIEW).setData(appMarketURI);
					UserPreferences.this.startActivity(appMarketIntent);

					AdsUtils.setGenerousUser(null);// reset generous user

					return true;
				} else {
					AdsUtils.setShowingAds(isNowChecked);
					return false;
				}
			}
		});

		// donate dialog
		((PreferenceScreen) findPreference("pDonate")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				UserPreferences.this.startActivity(new Intent(UserPreferences.this, DonateActivity.class));
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * @param number the subway line number
	 * @return the PREFS_SUBWAY_STATIONS_ORDER+number key.
	 */
	public static String getPrefsSubwayStationsOrder(int number) {
		return PREFS_SUBWAY_STATIONS_ORDER + number;
	}

}

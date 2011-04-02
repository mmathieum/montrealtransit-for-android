package org.montrealtransit.android;

import org.montrealtransit.android.activity.UserPreferences;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Contains useful methods for menus.
 * @author Mathieu Méa
 */
public class MenuUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = MenuUtils.class.getSimpleName();

	/**
	 * Handles 'Search' and 'Preferences' menu.
	 * @param activity the current activity
	 * @param item the menu item
	 * @return false to allow normal menu processing to proceed, true to consume it here.
	 */
	public static boolean handleCommonMenuActions(Activity activity, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search:
			return activity.onSearchRequested();
		case R.id.preferences:
			activity.startActivity(new Intent(activity, UserPreferences.class));
			return true;
		default:
			MyLog.d(TAG, "Unknown option menu action: %s.", item.getItemId());
			return false;
		}
	}

	/**
	 * Create the main menu (with 'Search' and 'Preferences').
	 * @param activity the activity
	 * @param menu the menu
	 * @return true if the menu creation was handled
	 */
	public static boolean createMainMenu(Activity activity, Menu menu) {
		return inflateMenu(activity, menu, R.menu.main_menu);
	}

	/**
	 * Inflate a menu into the activity menu.
	 * @param activity the activity
	 * @param menu the menu
	 * @param menuId the menu ID
	 * @return true if the menu was inflated
	 */
	public static boolean inflateMenu(Activity activity, Menu menu, int menuId) {
		activity.getMenuInflater().inflate(menuId, menu);
		return true;
	}

}

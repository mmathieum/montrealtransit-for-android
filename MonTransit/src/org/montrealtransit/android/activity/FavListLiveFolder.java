package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

/**
 * This class manage the favorite live folder.
 * @author Mathieu Méa
 */
public class FavListLiveFolder extends Activity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
			List<DataStore.Fav> favList = DataManager.findAllFavsList(this.getContentResolver());
			if (Utils.getListSize(favList) > 0) {
				setResult(RESULT_OK, createLiveFolder(this, StmManager.getBusStopsFavUri(favList), getResources().getString(
				        R.string.favorite_live_folder_name), R.drawable.fav_live_folder_icon));
			} else {
				Utils.notifyTheUser(this, getResources().getString(R.string.favorite_live_folder_need_to_add_fav));
				setResult(RESULT_CANCELED);
			}
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	/**
	 * Create the live folder
	 * @param context the context
	 * @param uri the bus stops URI
	 * @param name the folder name
	 * @param icon the folder icon
	 * @return
	 */
	private static Intent createLiveFolder(Context context, Uri uri, String name, int icon) {
		final Intent intent = new Intent();
		intent.setData(uri);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, name);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, Intent.ShortcutIconResource.fromContext(context, icon));
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, new Intent(Intent.ACTION_VIEW, StmStore.BusStop.CONTENT_URI_FAV));
		return intent;
	}
}
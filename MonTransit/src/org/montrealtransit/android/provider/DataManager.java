package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.DataStore.History;
import org.montrealtransit.android.provider.DataStore.TwitterApi;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

/**
 * This manager provide methods to access the user data. This class query the content provider {@link DataProvider}
 * @author Mathieu Méa
 */
public class DataManager {

	/**
	 * The log tag.
	 */
	private static final String TAG = DataManager.class.getSimpleName();

	/**
	 * Delete a favorite entry
	 * @param contentResolver the content resolver
	 * @param favId the favorite ID
	 * @return true if one (or more) favorite have been deleted.
	 */
	public static boolean deleteFav(ContentResolver contentResolver, int favId) {
		int count = contentResolver.delete(Uri.withAppendedPath(DataStore.Fav.CONTENT_URI, String.valueOf(favId)),
		        null, null);
		return count > 0;
	}

	/**
	 * Delete a history entry
	 * @param contentResolver the content resolver
	 * @param favId the favorite ID
	 * @return true if one (or more) favorite have been deleted.
	 */
	public static boolean deleteHistory(ContentResolver contentResolver) {
		int count = contentResolver.delete(DataStore.History.CONTENT_URI, null, null);
		return count > 0;
	}

	/**
	 * Delete a Twitter API entry
	 * @param contentResolver the content resolver
	 * @param favId the favorite ID
	 * @return true if one (or more) favorite have been deleted.
	 */
	public static boolean deleteTwitterAPI(ContentResolver contentResolver) {
		int count = contentResolver.delete(DataStore.TwitterApi.CONTENT_URI, null, null);
		return count > 0;
	}

	/**
	 * Represents the fields the content provider will return for a favorite entry.
	 */
	private static final String[] PROJECTION_FAVS = new String[] { DataStore.Fav._ID, DataStore.Fav.FAV_FK_ID,
	        DataStore.Fav.FAV_FK_ID2, DataStore.Fav.FAV_TYPE };
	
	/**
	 * Represents the fields the content provider will return for a Twitter API entry.
	 */
	private static final String[] PROJECTION_TWITTER_APIS = new String[] { DataStore.TwitterApi._ID, DataStore.TwitterApi.TOKEN, DataStore.TwitterApi.TOKEN_SECRET};

	/**
	 * Represents the fields the content provider will return for an history entry.
	 */
	private static final String[] PROJECTION_HISTORY = new String[] { DataStore.History._ID, DataStore.History.VALUE};

	/**
	 * @param contentResolver the content resolver
	 * @return all favorite entries
	 */
	public static Cursor findAllFavs(ContentResolver contentResolver) {
		return contentResolver.query(DataStore.Fav.CONTENT_URI, PROJECTION_FAVS, null, null, null);
	}
	
	/**
	 * @param contentResolver the content resolver
	 * @return all Twitter API entries
	 */
	public static Cursor findAllTwitterApis(ContentResolver contentResolver) {
		return contentResolver.query(DataStore.TwitterApi.CONTENT_URI, PROJECTION_TWITTER_APIS, null, null, null);
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all the history entries
	 */
	public static Cursor findAllHistory(ContentResolver contentResolver) {
		return contentResolver.query(DataStore.History.CONTENT_URI, PROJECTION_HISTORY, null, null, null);
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all the history entries in a list
	 * @see DataManager#findAllHistory(ContentResolver)
	 */
	public static List<String> findAllHistoryList(ContentResolver contentResolver) {
		List<String> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllHistory(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<String>();
					do {
						result.add(DataStore.History.fromCursor(cursor).getValue());
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all the favorite entries in a list
	 * @see DataManager#findAllFavs(ContentResolver)
	 */
	public static List<DataStore.Fav> findAllFavsList(ContentResolver contentResolver) {
		List<DataStore.Fav> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllFavs(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<DataStore.Fav>();
					do {
						result.add(DataStore.Fav.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}
	
	/**
	 * @param contentResolver the content resolver
	 * @return all Twitter API entries a list
	 * @see DataManager#findAllTwitterApis(ContentResolver)
	 */
	public static List<DataStore.TwitterApi> findAllTwitterApisList(ContentResolver contentResolver) {
		List<DataStore.TwitterApi> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllTwitterApis(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<DataStore.TwitterApi>();
					do {
						result.add(DataStore.TwitterApi.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	/**
	 * @param contentResolver the content resolver
	 * @param uri the favorite entry URI
	 * @return the favorite or <b>NULL</b>
	 */
	private static DataStore.Fav findFav(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findFav(%s)", uri.getPath());
		DataStore.Fav fav = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					fav = DataStore.Fav.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return fav;
	}

	/**
	 * @param contentResolver the content resolver
	 * @param uri the history entry URI
	 * @return the history entry or <b>NULL</b>
	 */
	private static DataStore.History findHistory(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findHistory(%s)", uri.getPath());
		DataStore.History history = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					history = DataStore.History.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return history;
	}

	/**
	 * @param contentResolver the content resolver
	 * @param uri the history entry URI
	 * @return the Twitter API or <b>NULL</b>
	 */
	private static DataStore.TwitterApi findTwitterApi(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findTwitterApi(%s)", uri.getPath());
		DataStore.TwitterApi twitterApi = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					twitterApi = DataStore.TwitterApi.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return twitterApi;
	}
	
	/**
	 * @param contentResolver the content resolver
	 * @param type the favorite entry type
	 * @param fkId the favorite entry FK ID
	 * @param fkId2 the favorite FK_ID2 or <b>NULL</b> if N/A
	 * @return the favorite entry matching the parameter.
	 */
	public static DataStore.Fav findFav(ContentResolver contentResolver, int type, String fkId, String fkId2) {
		MyLog.v(TAG, "findFav(%s, %s, %s)", type, fkId,fkId2);
		DataStore.Fav fav = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(DataStore.Fav.CONTENT_URI, fkId + "+" + fkId2 + "+" + type);
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					fav = DataStore.Fav.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return fav;
	}

	/**
	 * Find the favorite for given favorite type.
	 * @param contentResolver the content resolver
	 * @param type the favorite type
	 * @return the favorites
	 */
	public static List<DataStore.Fav> findFavsByTypeList(ContentResolver contentResolver, int type) {
		MyLog.v(TAG, "findFavsByTypeList(%s)", type);
		List<DataStore.Fav> result = null;
		Cursor cursor = null;
		try {
			Uri favTypeUri = ContentUris.withAppendedId(DataStore.Fav.CONTENT_URI, type);
			Uri uri = Uri.withAppendedPath(favTypeUri, DataStore.Fav.URI_TYPE);

			cursor = contentResolver.query(uri, PROJECTION_FAVS, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<DataStore.Fav>();
					do {
						result.add(DataStore.Fav.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;

	}

	/**
	 * Add a favorite to the content provider
	 * @param contentResolver the content resolver
	 * @param newFav the new favorite entry to add
	 * @return the added favorite entry or <b>NULL</b>
	 */
	public static DataStore.Fav addFav(ContentResolver contentResolver, Fav newFav) {
		final Uri uri = contentResolver.insert(DataStore.Fav.CONTENT_URI, newFav.getContentValues());
		if (uri != null) {
			return findFav(contentResolver, uri);
		} else {
			return null;
		}
	}

	/**
	 * Add an history entry to the content provider
	 * @param contentResolver the content resolver
	 * @param newHistory the new history entry
	 * @return the added history entry or <b>NULL</b>
	 */
	public static DataStore.History addHistory(ContentResolver contentResolver, History newHistory) {
		final Uri uri = contentResolver.insert(DataStore.History.CONTENT_URI, newHistory.getContentValues());
		if (uri != null) {
			return findHistory(contentResolver, uri);
		} else {
			return null;
		}
	}

	/**
	 * Add a Twitter API entry to the content provider
	 * @param contentResolver the content resolver
	 * @param newTwitterApi the new Twitter API entry
	 * @return the added Twitter API entry or <b>NULL</b>
	 */
	public static DataStore.TwitterApi addTwitterApi(ContentResolver contentResolver, TwitterApi newTwitterApi) {
		final Uri uri = contentResolver.insert(DataStore.TwitterApi.CONTENT_URI, newTwitterApi.getContentValues());
		if (uri != null) {
			return findTwitterApi(contentResolver, uri);
		} else {
			return null;
		}
	}
}

package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.DataStore.History;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

/**
 * This manager provide methods to access the user data.
 * This class query the content provider {@link DataProvider}
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
		int count = contentResolver.delete(Uri.withAppendedPath(DataStore.Fav.CONTENT_URI, String.valueOf(favId)), null, null);
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
	 * Represents the fields the content provider will return for a favorite entry.
	 */
	private static final String[] PROJECTION_FAVS = new String[] { DataStore.Fav._ID, DataStore.Fav.FAV_FK_ID, DataStore.Fav.FAV_FK_ID2, DataStore.Fav.FAV_TYPE };

	/**
	 * Represents the fields the content provider will return for an history entry.
	 */
	private static final String[] PROJECTION_HISTORY = new String[] { DataStore.History._ID, DataStore.History.VALUE, };

	/**
	 * @param contentResolver the content resolver
	 * @return all the favorite entries
	 */
	public static Cursor findAllFavs(ContentResolver contentResolver) {
		return contentResolver.query(DataStore.Fav.CONTENT_URI, PROJECTION_FAVS, null, null, null);
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
		Cursor c = null;
		try {
			c = findAllHistory(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<String>();
					do {
						result.add(DataStore.History.fromCursor(c).getValue());
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
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
		Cursor c = null;
		try {
			c = findAllFavs(contentResolver);
			if (c.getCount() > 0) {
				if (c.moveToFirst()) {
					result = new ArrayList<DataStore.Fav>();
					do {
						result.add(DataStore.Fav.fromCursor(c));
					} while (c.moveToNext());
				}
			}
		} finally {
			if (c != null)
				c.close();
		}
		return result;
	}

	/**
	 * @param contentResolver the content resolver
	 * @param uri the favorite entry URI
	 * @return the favorite or <b>NULL</b>
	 */
	private static DataStore.Fav findFav(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findFav(" + uri.getPath() + ")");
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
		MyLog.v(TAG, "findHistory(" + uri.getPath() + ")");
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
	 * @param type the favorite entry type
	 * @param fkId the favorite entry FK ID
	 * @param fkId2 the favorite FK_ID
	 * @return the favorite entry matching the parameter.
	 */
	public static DataStore.Fav findFav(ContentResolver contentResolver, int type, String fkId, String fkId2) {
		DataStore.Fav fav = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(Uri.withAppendedPath(DataStore.Fav.CONTENT_URI, fkId + "+" + fkId2 + "+" + type), // FavsStore.Fav.CONTENT_URI
			        null, null, null, /* new String[]{fkId, fkId2, String.valueOf(type)} */
			        null);
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
}

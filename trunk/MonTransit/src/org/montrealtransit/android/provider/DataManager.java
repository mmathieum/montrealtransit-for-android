package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.DataStore.History;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
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
	 * Represents the fields the content provider will return for a favorite entry.
	 */
	private static final String[] PROJECTION_FAVS = new String[] { DataStore.Fav._ID, DataStore.Fav.FAV_FK_ID,
	        DataStore.Fav.FAV_FK_ID2, DataStore.Fav.FAV_TYPE };

	/**
	 * Represents the fields the content provider will return for a Twitter API entry.
	 */
	private static final String[] PROJECTION_TWITTER_APIS = new String[] { DataStore.TwitterApi._ID,
	        DataStore.TwitterApi.TOKEN, DataStore.TwitterApi.TOKEN_SECRET };

	/**
	 * Represents the fields the content provider will return for an history entry.
	 */
	private static final String[] PROJECTION_HISTORY = new String[] { DataStore.History._ID, DataStore.History.VALUE };

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
	 * Delete ALL history entries
	 * @param contentResolver the content resolver
	 * @return true if one (or more) history entries have been deleted.
	 */
	public static boolean deleteAllHistory(ContentResolver contentResolver) {
		int count = contentResolver.delete(DataStore.History.CONTENT_URI, null, null);
		return count > 0;
	}

	/**
	 * Delete ALL Twitter API entries
	 * @param contentResolver the content resolver
	 * @return true if one (or more) Twitter API entries have been deleted.
	 */
	public static boolean deleteAllTwitterAPI(ContentResolver contentResolver) {
		int count = contentResolver.delete(DataStore.TwitterApi.CONTENT_URI, null, null);
		return count > 0;
	}

	/**
	 * Delete ALL service status entries
	 * @param contentResolver the content resolver
	 * @return true if one (or more) service status entries have been deleted.
	 */
	public static boolean deleteAllServiceStatus(ContentResolver contentResolver) {
		int count = contentResolver.delete(DataStore.ServiceStatus.CONTENT_URI, null, null);
		return count > 0;
	}

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
	 * @return all the history entries in a list or <b>NULL</b>
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
	 * @return all the favorite entries in a list OR <b>NULL</b>
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
	 * @param uri the Twitter API entry URI
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
	 * @param uri the service status entry URI
	 * @return the service status or <b>NULL</b>
	 */
	private static DataStore.ServiceStatus findServiceStatus(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findServiceStatus(%s)", uri.getPath());
		DataStore.ServiceStatus serviceStatus = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					serviceStatus = DataStore.ServiceStatus.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return serviceStatus;
	}

	/**
	 * @param contentResolver the content resolver
	 * @param type the favorite entry type
	 * @param fkId the favorite entry FK ID
	 * @param fkId2 the favorite FK_ID2 or <b>NULL</b> if N/A
	 * @return the favorite entry matching the parameter.
	 */
	public static DataStore.Fav findFav(ContentResolver contentResolver, int type, String fkId, String fkId2) {
		MyLog.v(TAG, "findFav(%s, %s, %s)", type, fkId, fkId2);
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
	 * Find the latest service status.
	 * @param contentResolver the content resolver
	 * @param lang the language
	 * @return the latest service status or NULL
	 */
	public static DataStore.ServiceStatus findLatestServiceStatus(ContentResolver contentResolver, String lang) {
		MyLog.v(TAG, "findLatestServiceStatus(%s)", lang);
		DataStore.ServiceStatus serviceStatus = null;
		Cursor cursor = null;
		try {
			Uri uri = DataStore.ServiceStatus.CONTENT_URI;
			String selection = DataDbHelper.T_SERVICE_STATUS_K_LANGUAGE + "='" + lang + "'";
			cursor = contentResolver
			        .query(uri, null, selection, null, DataStore.ServiceStatus.ORDER_BY_LATEST_PUB_DATE);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					serviceStatus = DataStore.ServiceStatus.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return serviceStatus;
	}

	/**
	 * Find the favorite for given favorite type.
	 * @param contentResolver the content resolver
	 * @param type the favorite type
	 * @return the favorites (never NULL)
	 */
	public static List<DataStore.Fav> findFavsByTypeList(ContentResolver contentResolver, int type) {
		MyLog.v(TAG, "findFavsByTypeList(%s)", type);
		List<DataStore.Fav> result = new ArrayList<DataStore.Fav>();
		Cursor cursor = null;
		try {
			Uri favTypeUri = ContentUris.withAppendedId(DataStore.Fav.CONTENT_URI, type);
			Uri uri = Uri.withAppendedPath(favTypeUri, DataStore.Fav.URI_TYPE);

			cursor = contentResolver.query(uri, PROJECTION_FAVS, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
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

	/**
	 * Add a service status entry to the content provider
	 * @param contentResolver the content resolver
	 * @param newServiceStatus the new service status entry
	 * @return the added service status entry or <b>NULL</b>
	 */
	public static DataStore.ServiceStatus addServiceStatus(ContentResolver contentResolver,
	        ServiceStatus newServiceStatus) {
		final Uri uri = contentResolver
		        .insert(DataStore.ServiceStatus.CONTENT_URI, newServiceStatus.getContentValues());
		if (uri != null) {
			return findServiceStatus(contentResolver, uri);
		} else {
			return null;
		}
	}

	/**
	 * Add a new cache entry to the content provider.
	 * @param contentResolver the content resolver
	 * @param newCache the new cache
	 * @return the new added cache object or <b>NULL</b> if something wrong happen
	 */
	public static Cache addCache(ContentResolver contentResolver, Cache newCache) {
		MyLog.v(TAG, "addCache(%s)", newCache.getObject());
		final Uri uri = contentResolver.insert(Cache.CONTENT_URI, newCache.getContentValues());
		if (uri != null) {
			return findCache(contentResolver, uri);
		} else {
			return null;
		}
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all cache entries
	 */
	public static Cursor findAllCache(ContentResolver contentResolver) {
		return contentResolver.query(Cache.CONTENT_URI, null, null, null, null);
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all the cache entries in a list or <b>NULL</b>
	 * @see {@link DataManager#findAllCache(ContentResolver)}
	 */
	public static List<Cache> findAllCacheList(ContentResolver contentResolver) {
		List<Cache> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllCache(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<Cache>();
					do {
						result.add(DataStore.Cache.fromCursor(cursor));
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
	 * Find a cache object.
	 * @param contentResolver the content resolver
	 * @param uri the cache object URI
	 * @return the cache object
	 */
	private static Cache findCache(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findCache(%s)", uri.getPath());
		Cache cache = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = Cache.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return cache;
	}

	/**
	 * Find a cache object in the content provider.
	 * @param contentResolver the content resolver
	 * @param type the cache object type
	 * @param fkId the cache object FK ID
	 * @return the cache object or <b>NULL</b>
	 */
	public static Cache findCache(ContentResolver contentResolver, int type, String fkId) {
		MyLog.v(TAG, "findCache(%s, %s)", type, fkId);
		Cache cache = null;
		Cursor cursor = null;
		try {
			Uri uri = Uri.withAppendedPath(Cache.CONTENT_URI, fkId + "+" + type);
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					cache = Cache.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return cache;
	}

	/**
	 * Find all cache entries of this type matching the regex.
	 * @param contentResolver the content resolver
	 * @param type the cache type
	 * @param fkIdRegex the cache FK ID regex
	 * @return the cache entries
	 */
	public static Set<Cache> findCacheRegex(ContentResolver contentResolver, int type, String fkIdRegex) {
		MyLog.v(TAG, "findCacheRegex(%s, %s)", type, fkIdRegex);
		Set<Cache> caches = new HashSet<DataStore.Cache>();
		Cache cache = null;
		Cursor cursor = null;
		try {
			// find all values
			cursor = contentResolver.query(Cache.CONTENT_URI, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						cache = Cache.fromCursor(cursor);
						if (Pattern.matches(fkIdRegex, cache.getFkId())) {
							caches.add(cache);
						}
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return caches;
	}

	/**
	 * Delete a cache object from the content provider.
	 * @param contentResolver the content resolver
	 * @param id the cache object ID
	 * @return true if one or more object was removed
	 */
	public static boolean deleteCache(ContentResolver contentResolver, int id) {
		int count = contentResolver.delete(Uri.withAppendedPath(Cache.CONTENT_URI, String.valueOf(id)), null, null);
		return count > 0;
	}

	/**
	 * Try to delete a cache object from the content provider if it exist.
	 * @param contentResolver the content resolver
	 * @param type the cache object type
	 * @param fkId the cache object FJ ID
	 * @return true if one or more objects were deleted
	 */
	public static boolean deleteCacheIfExist(ContentResolver contentResolver, int type, String fkId) {
		MyLog.v(TAG, "deleteCacheIfExist(%s, %s)", type, fkId);
		Cache oldCache = findCache(contentResolver, type, fkId);
		if (oldCache != null) {
			return deleteCache(contentResolver, oldCache.getId());
		}
		return false;
	}

	/**
	 * Delete all cache entries older than the specified date from the content provider
	 * @param contentResolver the content resolver
	 * @param date the date in seconds
	 * @return true if one or more cache entries were deleted
	 */
	public static boolean deleteCacheOlderThan(ContentResolver contentResolver, int date) {
		MyLog.v(TAG, "deleteCacheOlderThan(%s)", date);
		Uri dateUri = Uri.withAppendedPath(Cache.CONTENT_URI, String.valueOf(date));
		Uri uri = Uri.withAppendedPath(dateUri, Cache.URI_DATE);
		int count = contentResolver.delete(uri, null, null);
		return count > 0;
	}

	/**
	 * Delete all cache entries from the content provider.
	 * @param contentResolver the content resolver
	 * @return true if one or more cache entries were deleted
	 */
	public static boolean deleteAllCache(ContentResolver contentResolver) {
		MyLog.v(TAG, "deleteAllCache()");
		int count = contentResolver.delete(Cache.CONTENT_URI, null, null);
		return count > 0;
	}
}

package org.montrealtransit.android.provider;

import java.util.Arrays;
import java.util.HashMap;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;

/**
 * This provider give information about the user.
 * @author Mathieu Méa
 */
@SuppressWarnings("deprecation")
// TODO use App Widgets (Android 3.0+)
public class DataProvider extends ContentProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = DataProvider.class.getSimpleName();

	/**
	 * The content provider authority as described in the AndroidManifest.
	 */
	public static final String AUTHORITY = Constant.PKG + ".data";

	private static final int LIVE_FOLDER_FAVS = 1;
	private static final int FAVS = 2;
	private static final int FAV_ID = 3;
	private static final int FAVS_IDS = 4;
	private static final int HISTORY = 5;
	private static final int HISTORY_ID = 6;
	private static final int HISTORY_IDS = 7;
	private static final int FAVS_TYPE_ID = 8;
	private static final int TWITTER_API = 9;
	private static final int TWITTER_API_ID = 10;
	private static final int SERVICE_STATUS = 11;
	private static final int SERVICE_STATUS_ID = 12;
	private static final int CACHE = 13;
	private static final int CACHE_ID = 14;
	private static final int CACHE_FKID = 15;
	private static final int CACHE_DATE = 16;

	/**
	 * The URI matcher filter the content URI calls.
	 */
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "cache", CACHE);
		URI_MATCHER.addURI(AUTHORITY, "cache/#", CACHE_ID);
		URI_MATCHER.addURI(AUTHORITY, "cache/*", CACHE_FKID);
		URI_MATCHER.addURI(AUTHORITY, "cache/#/date", CACHE_DATE);
		URI_MATCHER.addURI(AUTHORITY, "favs", FAVS);
		URI_MATCHER.addURI(AUTHORITY, "favs/#", FAV_ID);
		URI_MATCHER.addURI(AUTHORITY, "favs/*", FAVS_IDS);
		URI_MATCHER.addURI(AUTHORITY, "favs/#/type", FAVS_TYPE_ID);
		URI_MATCHER.addURI(AUTHORITY, "live_folder_favs", LIVE_FOLDER_FAVS); // TODO useless ?
		URI_MATCHER.addURI(AUTHORITY, "history", HISTORY);
		URI_MATCHER.addURI(AUTHORITY, "history/#", HISTORY_ID);
		URI_MATCHER.addURI(AUTHORITY, "history/*", HISTORY_IDS);
		URI_MATCHER.addURI(AUTHORITY, "twitterapi", TWITTER_API);
		URI_MATCHER.addURI(AUTHORITY, "twitterapi/#", TWITTER_API_ID);
		URI_MATCHER.addURI(AUTHORITY, "servicestatus", SERVICE_STATUS);
		URI_MATCHER.addURI(AUTHORITY, "servicestatus/#", SERVICE_STATUS_ID);
	}

	/**
	 * Use to convert a favorite object in live folder object.
	 */
	private static final HashMap<String, String> LIVE_FOLDER_PROJECTION_MAP;
	static {
		LIVE_FOLDER_PROJECTION_MAP = new HashMap<String, String>();
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders._ID, DataDbHelper.T_FAVS_K_ID + " AS " + LiveFolders._ID);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.NAME, DataDbHelper.T_FAVS_K_FK_ID + " AS " + LiveFolders.NAME);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.DESCRIPTION, DataDbHelper.T_FAVS_K_FK_ID_2 + " AS " + LiveFolders.DESCRIPTION);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[%s]", uri);
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
			MyLog.v(TAG, "FAVS");
			qb.setTables(DataDbHelper.T_FAVS);
			break;
		case FAV_ID:
			MyLog.v(TAG, "FAV_ID");
			qb.setTables(DataDbHelper.T_FAVS);
			qb.appendWhere(DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_ID + "=" + uri.getPathSegments().get(1));
			break;
		case FAVS_IDS:
			MyLog.v(TAG, "FAVS_IDS");
			qb.setTables(DataDbHelper.T_FAVS);
			String[] ids = uri.getPathSegments().get(1).split("\\+");
			String fkId = ids[0];
			String fkId2 = ids[1];
			int favType = Integer.valueOf(ids[2]);
			String where = null;
			if (favType == DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP) {
				where = DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS + "."
						+ DataDbHelper.T_FAVS_K_FK_ID_2 + "=" + fkId2 + " AND " + DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_TYPE + "=" + favType;
			} else if (favType == DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION) {
				where = DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_TYPE
						+ "=" + favType;
			} else if (favType == DataStore.Fav.KEY_TYPE_VALUE_BIKE_STATIONS) {
				where = DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_TYPE
						+ "=" + favType;
			}
			qb.appendWhere(where);
			break;
		case FAVS_TYPE_ID:
			MyLog.v(TAG, "FAVS_TYPE_ID");
			qb.setTables(DataDbHelper.T_FAVS);
			qb.appendWhere(DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_TYPE + "=" + uri.getPathSegments().get(1));
			break;
		case LIVE_FOLDER_FAVS:
			qb.setTables(DataDbHelper.T_FAVS);
			qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);
			break;
		case HISTORY:
			MyLog.v(TAG, "HISTORY");
			qb.setTables(DataDbHelper.T_HISTORY);
			break;
		case HISTORY_ID:
			MyLog.v(TAG, "HISTORY_ID");
			qb.setTables(DataDbHelper.T_HISTORY);
			qb.appendWhere(DataDbHelper.T_HISTORY + "." + DataDbHelper.T_HISTORY_K_ID + "=" + uri.getPathSegments().get(1));
			break;
		case TWITTER_API:
			MyLog.v(TAG, "TWITTER_API");
			qb.setTables(DataDbHelper.T_TWITTER_API);
			break;
		case TWITTER_API_ID:
			MyLog.v(TAG, "TWITTER_API_ID");
			qb.setTables(DataDbHelper.T_TWITTER_API);
			qb.appendWhere(DataDbHelper.T_TWITTER_API + "." + DataDbHelper.T_TWITTER_API_K_ID + "=" + uri.getPathSegments().get(1));
			break;
		case SERVICE_STATUS:
			MyLog.v(TAG, "SERVICE_STATUS");
			qb.setTables(DataDbHelper.T_SERVICE_STATUS);
			break;
		case SERVICE_STATUS_ID:
			MyLog.v(TAG, "SERVICE_STATUS_ID");
			qb.setTables(DataDbHelper.T_SERVICE_STATUS);
			qb.appendWhere(DataDbHelper.T_SERVICE_STATUS + "." + DataDbHelper.T_SERVICE_STATUS_K_ID + "=" + uri.getPathSegments().get(1));
			break;
		case CACHE:
			MyLog.v(TAG, "CACHE");
			qb.setTables(DataDbHelper.T_CACHE);
			break;
		case CACHE_ID:
			MyLog.v(TAG, "CACHE_ID");
			qb.setTables(DataDbHelper.T_CACHE);
			qb.appendWhere(DataDbHelper.T_CACHE + "." + DataDbHelper.T_CACHE_K_ID + "=" + uri.getPathSegments().get(1));
			break;
		case CACHE_FKID:
			MyLog.v(TAG, "CACHE_FKID");
			qb.setTables(DataDbHelper.T_CACHE);
			ids = uri.getPathSegments().get(1).split("\\+");
			fkId = ids[0];
			int type = Integer.valueOf(ids[1]);
			where = null;
			if (type == DataStore.Cache.KEY_TYPE_VALUE_BUS_STOP) {
				where = DataDbHelper.T_CACHE + "." + DataDbHelper.T_CACHE_K_FK_ID + "='" + fkId + "' AND " + DataDbHelper.T_CACHE + "."
						+ DataDbHelper.T_CACHE_K_TYPE + "=" + type;
			}
			qb.appendWhere(where);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI (query) :" + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case FAVS:
			case FAV_ID:
			case FAVS_IDS:
			case FAVS_TYPE_ID:
			case LIVE_FOLDER_FAVS:
				orderBy = DataStore.Fav.DEFAULT_SORT_ORDER;
				break;
			case HISTORY:
			case HISTORY_ID:
			case HISTORY_IDS:
				orderBy = DataStore.History.DEFAULT_SORT_ORDER;
				break;
			case TWITTER_API:
			case TWITTER_API_ID:
				orderBy = DataStore.TwitterApi.DEFAULT_SORT_ORDER;
				break;
			case SERVICE_STATUS:
			case SERVICE_STATUS_ID:
				orderBy = DataStore.ServiceStatus.DEFAULT_SORT_ORDER;
				break;
			case CACHE:
			case CACHE_ID:
			case CACHE_FKID:
				orderBy = DataStore.Cache.DEFAULT_SORT_ORDER;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI (order) :" + uri);
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = getDBHelper().getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		if (c != null) {
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
		case FAVS_IDS:
		case FAVS_TYPE_ID:
			return DataStore.Fav.CONTENT_TYPE;
		case FAV_ID:
			return DataStore.Fav.CONTENT_ITEM_TYPE;
		case HISTORY:
		case HISTORY_IDS:
			return DataStore.History.CONTENT_TYPE;
		case HISTORY_ID:
			return DataStore.History.CONTENT_ITEM_TYPE;
		case TWITTER_API:
			return DataStore.TwitterApi.CONTENT_TYPE;
		case TWITTER_API_ID:
			return DataStore.TwitterApi.CONTENT_ITEM_TYPE;
		case SERVICE_STATUS:
			return DataStore.ServiceStatus.CONTENT_TYPE;
		case SERVICE_STATUS_ID:
			return DataStore.ServiceStatus.CONTENT_ITEM_TYPE;
		case CACHE:
		case CACHE_DATE:
			return DataStore.Cache.CONTENT_TYPE;
		case CACHE_ID:
		case CACHE_FKID:
			return DataStore.Cache.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI (type) :" + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete(%s, %s, %s)", uri.getPath(), selection, Arrays.toString(selectionArgs));
		SQLiteDatabase db = getDBHelper().getWritableDatabase();

		int count = 0;
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
			MyLog.v(TAG, "DELETE>FAVS");
			String fkId = selectionArgs[0];
			String fkId2 = selectionArgs[1];
			int favType = Integer.valueOf(selectionArgs[2]);
			String whereClause = null;
			if (favType == DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP) {
				whereClause = DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS_K_FK_ID_2 + "=" + fkId2 + " AND "
						+ DataDbHelper.T_FAVS_K_TYPE + "=" + favType;
			} else if (favType == DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION) {
				whereClause = DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS_K_TYPE + "=" + favType;
			}
			count = db.delete(DataDbHelper.T_FAVS, whereClause, null);
			break;
		case FAV_ID:
			MyLog.v(TAG, "DELETE>FAV_ID");
			count = db.delete(DataDbHelper.T_FAVS, DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_ID + "=" + uri.getPathSegments().get(1), null);
			break;
		case HISTORY:
			MyLog.v(TAG, "DELETE>HISTORY");
			count = db.delete(DataDbHelper.T_HISTORY, selection, null);
			break;
		case TWITTER_API:
			MyLog.v(TAG, "DELETE>TWITTER_API");
			count = db.delete(DataDbHelper.T_TWITTER_API, selection, null);
			break;
		case SERVICE_STATUS:
			MyLog.v(TAG, "DELETE>SERVICE_STATUS");
			count = db.delete(DataDbHelper.T_SERVICE_STATUS, selection, null);
			break;
		case CACHE:
			MyLog.v(TAG, "DELETE>CACHE");
			count = db.delete(DataDbHelper.T_CACHE, selection, null);
			break;
		case CACHE_ID:
			MyLog.v(TAG, "DELETE>CACHE_ID");
			count = db.delete(DataDbHelper.T_CACHE, DataDbHelper.T_CACHE + "." + DataDbHelper.T_CACHE_K_ID + "=" + uri.getPathSegments().get(1), null);
			break;
		case CACHE_DATE:
			MyLog.v(TAG, "DELETE>CACHE_DATE");
			count = db.delete(DataDbHelper.T_CACHE, DataDbHelper.T_CACHE + "." + DataDbHelper.T_CACHE_K_DATE + " < " + uri.getPathSegments().get(1), null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI (delete): " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		MyLog.v(TAG, "insert(%s, %s)", uri, initialValues.size());
		ContentValues values = new ContentValues(initialValues);
		// if (initialValues != null) {
		// values = new ContentValues(initialValues);
		// } else {
		// values = new ContentValues();
		// }
		SQLiteDatabase db = getDBHelper().getWritableDatabase();
		Uri insertUri = null;
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
			long rowId = db.insert(DataDbHelper.T_FAVS, DataDbHelper.T_FAVS_K_TITLE, values);
			if (rowId > 0) {
				insertUri = ContentUris.withAppendedId(DataStore.Fav.CONTENT_URI, rowId);
			}
			break;
		case HISTORY:
			long historyId = db.insert(DataDbHelper.T_HISTORY, DataDbHelper.T_HISTORY_K_VALUE, values);
			if (historyId > 0) {
				insertUri = ContentUris.withAppendedId(DataStore.History.CONTENT_URI, historyId);
			}
			break;
		case TWITTER_API:
			long twitterApiId = db.insert(DataDbHelper.T_TWITTER_API, DataDbHelper.T_TWITTER_API_K_TOKEN, values);
			if (twitterApiId > 0) {
				insertUri = ContentUris.withAppendedId(DataStore.TwitterApi.CONTENT_URI, twitterApiId);
			}
			break;
		case SERVICE_STATUS:
			long serviceStatusId = db.insert(DataDbHelper.T_SERVICE_STATUS, DataDbHelper.T_SERVICE_STATUS_K_MESSAGE, values);
			if (serviceStatusId > 0) {
				insertUri = ContentUris.withAppendedId(DataStore.ServiceStatus.CONTENT_URI, serviceStatusId);
			}
			break;
		case CACHE:
			long cacheId = db.insert(DataDbHelper.T_CACHE, DataDbHelper.T_CACHE_K_OBJECT, values);
			if (cacheId > 0) {
				insertUri = ContentUris.withAppendedId(DataStore.Cache.CONTENT_URI, cacheId);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI (insert) " + uri);
		}
		if (insertUri == null) {
			throw new SQLException("Failed to insert row into " + uri);
		} else {
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
	}

	/**
	 * The SQLite open helper object.
	 */
	private DataDbHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}

	/**
	 * @return the database helper
	 */
	private DataDbHelper getDBHelper() {
		if (this.mOpenHelper == null) {
			// initialize
			this.mOpenHelper = new DataDbHelper(getContext());
		} else if (this.mOpenHelper.getReadableDatabase().getVersion() != DataDbHelper.DATABASE_VERSION) {
			// reset
			this.mOpenHelper.close();
			this.mOpenHelper = new DataDbHelper(getContext());
		}
		return this.mOpenHelper;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		MyLog.v(TAG, "update()");
		MyLog.w(TAG, "The update method is not available.");
		return 0;
	}

}

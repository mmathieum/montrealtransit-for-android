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
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;

/**
 * This provider give information about the user.
 * @author Mathieu Méa
 */
public class DataProvider extends ContentProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = DataProvider.class.getSimpleName();

	/**
	 * The content provider authority as described in the AndroidManifest.
	 */
	public static final String AUTHORITY = Constant.PKG + ".data";

	/**
	 * The content URI ID for live folder.
	 */
	private static final int LIVE_FOLDER_FAVS = 4;
	/**
	 * The content URI ID for all the favorite entries.
	 */
	private static final int FAVS = 2;
	/**
	 * The content URI ID for a favorite entry.
	 */
	private static final int FAV_ID = 3;
	/**
	 * The content URI ID for some favorite entries.
	 */
	private static final int FAVS_IDS = 5;
	/**
	 * The content URI ID for all history entries.
	 */
	private static final int HISTORY = 6;
	/**
	 * The content URI ID for one history entry.
	 */
	private static final int HISTORY_ID = 7;
	/**
	 * The content URI ID for some history entries.
	 */
	private static final int HISTORY_IDS = 8;

	/**
	 * The URI matcher filter the content URI calls.
	 */
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "favs", FAVS);
		URI_MATCHER.addURI(AUTHORITY, "favs/#", FAV_ID);
		URI_MATCHER.addURI(AUTHORITY, "favs/*", FAVS_IDS);
		URI_MATCHER.addURI(AUTHORITY, "live_folder_favs", LIVE_FOLDER_FAVS); // TODO useless ?
		URI_MATCHER.addURI(AUTHORITY, "history", HISTORY);
		URI_MATCHER.addURI(AUTHORITY, "history/#", HISTORY_ID);
		URI_MATCHER.addURI(AUTHORITY, "history/*", HISTORY_IDS);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(" + uri + ", " + Arrays.toString(projection) + ", " + selection + ", " + Arrays.toString(selectionArgs) + ", " + sortOrder + ")");
		MyLog.i(TAG, "[" + uri + "]");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

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
			String[] ids = uri.getPathSegments().get(1).split("\\+");
			String fkId = ids[0];
			String fkId2 = ids[1];
			String favType = ids[2];
			String mSlection = DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS + "."
			        + DataDbHelper.T_FAVS_K_FK_ID_2 + "=" + fkId2 + " AND " + DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_TYPE + "=" + favType;
			qb.setTables(DataDbHelper.T_FAVS);
			qb.appendWhere(mSlection);
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
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case FAVS:
			case FAV_ID:
			case FAVS_IDS:
			case LIVE_FOLDER_FAVS:
				orderBy = DataStore.Fav.DEFAULT_SORT_ORDER;
				break;
			case HISTORY:
			case HISTORY_ID:
			case HISTORY_IDS:
				orderBy = DataStore.History.DEFAULT_SORT_ORDER;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(" + uri.getPath() + ")");
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
		case FAVS_IDS:
			return DataStore.Fav.CONTENT_TYPE;
		case FAV_ID:
			return DataStore.Fav.CONTENT_ITEM_TYPE;
		case HISTORY:
		case HISTORY_IDS:
			return DataStore.History.CONTENT_TYPE;
		case HISTORY_ID:
			return DataStore.History.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete(" + uri.getPath() + "," + selection + "," + Arrays.toString(selectionArgs) + ")");
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		int count = 0;
		switch (URI_MATCHER.match(uri)) {
		case FAVS:
			MyLog.v(TAG, "DELETE>FAVS");
			String fkId = selectionArgs[0];
			String fkId2 = selectionArgs[1];
			String favType = selectionArgs[2];
			String mSlection = DataDbHelper.T_FAVS_K_FK_ID + "=" + fkId + " AND " + DataDbHelper.T_FAVS_K_FK_ID_2 + "=" + fkId2 + " AND "
			        + DataDbHelper.T_FAVS_K_TYPE + "=" + favType;
			break;
		case FAV_ID:
			MyLog.v(TAG, "DELETE>FAV_ID");
			String favId = uri.getPathSegments().get(1);
			count = db.delete(DataDbHelper.T_FAVS, DataDbHelper.T_FAVS + "." + DataDbHelper.T_FAVS_K_ID + "=" + favId, null);
			break;
		case HISTORY:
			MyLog.v(TAG, "DELETE>HISTORY");
			count = db.delete(DataDbHelper.T_HISTORY, null, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		MyLog.v(TAG, "insert(" + uri + ", " + initialValues.size() + ")");
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
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
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
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
	 * @see DataDbHelper
	 */
	private SQLiteOpenHelper mOpenHelper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		mOpenHelper = new DataDbHelper(getContext());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		MyLog.v(TAG, "update()");
		MyLog.w(TAG, "The update method is not available.");
		return 0;
	}

}

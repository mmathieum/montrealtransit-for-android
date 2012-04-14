package org.montrealtransit.android.provider;

import java.util.Arrays;

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
import android.text.TextUtils;

public class BixiProvider extends ContentProvider {

	/**
	 * The log tag.
	 */
	private static final String TAG = BixiProvider.class.getSimpleName();

	/**
	 * The content provider authority as described in the AndroidManifest.
	 */
	public static final String AUTHORITY = Constant.PKG + ".bixi";

	private static final int BIKE_STATION = 1;
	private static final int BIKE_STATION_ID = 2;
	private static final int BIKE_STATION_IDS = 3;

	/**
	 * The URI matcher filter the content URI calls.
	 */
	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "bikestations", BIKE_STATION);
		URI_MATCHER.addURI(AUTHORITY, "bikestations/#", BIKE_STATION_ID);
		URI_MATCHER.addURI(AUTHORITY, "bikestations/*", BIKE_STATION_IDS);
	}

	/**
	 * The SQLite open helper object.
	 */
	private BixiDbHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		MyLog.v(TAG, "onCreate()");
		return true;
	}
	
	/**
	 * @return the database helper
	 */
	private BixiDbHelper getDBHelper() {
		if (this.mOpenHelper == null) {
			// initialize
			this.mOpenHelper = new BixiDbHelper(getContext());
		} else if (this.mOpenHelper.getReadableDatabase().getVersion() != BixiDbHelper.DATABASE_VERSION) {
			// reset
			this.mOpenHelper.close();
			this.mOpenHelper = new BixiDbHelper(getContext());
		}
		return this.mOpenHelper;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		MyLog.i(TAG, "[%s]", uri);
		switch (URI_MATCHER.match(uri)) {
		case BIKE_STATION:
			MyLog.d(TAG, "BIKE_STATION");
			qb.setTables(BixiDbHelper.T_BIKE_STATIONS);
			break;
		case BIKE_STATION_ID:
			MyLog.d(TAG, "BIKE_STATION_ID");
			qb.setTables(BixiDbHelper.T_BIKE_STATIONS);
			qb.appendWhere(BixiDbHelper.T_BIKE_STATIONS + "." + BixiDbHelper.T_BIKE_STATIONS_K_TERMINAL_NAME + "=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI (query) :" + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			switch (URI_MATCHER.match(uri)) {
			case BIKE_STATION:
			case BIKE_STATION_ID:
			case BIKE_STATION_IDS:
				orderBy = BixiStore.BikeStation.DEFAULT_SORT_ORDER;
				break;
			default:
				throw new IllegalArgumentException("Unknown URI (order) :" + uri);
			}
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = getDBHelper().getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (URI_MATCHER.match(uri)) {
		case BIKE_STATION:
		case BIKE_STATION_IDS:
			return BixiStore.BikeStation.CONTENT_TYPE;
		case BIKE_STATION_ID:
			return BixiStore.BikeStation.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI (type) :" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		MyLog.v(TAG, "insert(%s, %s)", uri, values.size());
		SQLiteDatabase db = getDBHelper().getWritableDatabase();
		Uri insertUri = null;
		switch (URI_MATCHER.match(uri)) {
		case BIKE_STATION:
			MyLog.d(TAG, "INSERT>BIKE_STATION");
			long bikeStationId = db.insert(BixiDbHelper.T_BIKE_STATIONS, BixiDbHelper.T_BIKE_STATIONS_K_ID, values);
			if (bikeStationId > 0) {
				insertUri = ContentUris.withAppendedId(BixiStore.BikeStation.CONTENT_URI, bikeStationId);
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

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete(%s, %s, %s)", uri.getPath(), selection, Arrays.toString(selectionArgs));
		SQLiteDatabase db = getDBHelper().getWritableDatabase();
		int count = 0;
		switch (URI_MATCHER.match(uri)) {
		case BIKE_STATION:
			MyLog.d(TAG, "DELETE>BIKE_STATION");
			count = db.delete(BixiDbHelper.T_BIKE_STATIONS, selection, null);
			break;
		case BIKE_STATION_ID:
			MyLog.d(TAG, "DELETE>BIKE_STATION_ID");
			db.delete(BixiDbHelper.T_BIKE_STATIONS, BixiDbHelper.T_BIKE_STATIONS + "." + BixiDbHelper.T_BIKE_STATIONS_K_TERMINAL_NAME + "=" + uri.getPathSegments().get(1), null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI (delete): " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "update(%s, %s, %s)", uri.getPath(), selection, Arrays.toString(selectionArgs));
		MyLog.d(TAG, "The update method is not available!");
		return 0;
		// TODO
		// SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		// int count = 0;
		// switch (URI_MATCHER.match(uri)) {
		// case BIKE_STATION:
		// MyLog.v(TAG, "UPDATE>BIKE_STATION");
		// count = db.update(BixiDbHelper.T_BIKE_STATIONS, values, BixiDbHelper.T_BIKE_STATIONS_K_ID + "="
		// + uri.getPathSegments().get(1), null);
		// break;
		// default:
		// throw new IllegalArgumentException("Unknown URI (update): " + uri);
		// }
		// getContext().getContentResolver().notifyChange(uri, null);
		// return count;
	}

}

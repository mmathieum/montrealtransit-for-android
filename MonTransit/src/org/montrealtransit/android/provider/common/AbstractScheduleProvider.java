package org.montrealtransit.android.provider.common;

import java.util.Arrays;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Cache;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public abstract class AbstractScheduleProvider extends ContentProvider {

	public static final String TAG = AbstractScheduleProvider.class.getSimpleName();

	public static final String GLOBAL_AUTHORITY = "org.montrealtransit.android";

	public static final String DEPARTURE_CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + GLOBAL_AUTHORITY + ".departure";

	private static final int DEPARTURE = 1;
	private static final int PING = 99;

	// protected static final int VERSION = 100;
	// protected static final int DEPLOYED = 101;
	// protected static final int LABEL = 102;
	// protected static final int SETUP_REQUIRED = 103;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(authority, "departure", DEPARTURE);
		URI_MATCHER.addURI(authority, "ping", PING);
		// URI_MATCHER.addURI(authority, "version", VERSION);
		// URI_MATCHER.addURI(authority, "deployed", DEPLOYED);
		// URI_MATCHER.addURI(authority, "label", LABEL);
		// URI_MATCHER.addURI(authority, "setuprequired", SETUP_REQUIRED);
		return URI_MATCHER;
	}

	@Override
	public boolean onCreate() {
		ping();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MyLog.v(TAG, "query(%s, %s, %s, %s, %s)", uri.getPath(), Arrays.toString(projection), selection, Arrays.toString(selectionArgs), sortOrder);
		MyLog.i(TAG, "[%s]", uri);
		switch (getURIMATCHER().match(uri)) {
		case PING:
			MyLog.v(TAG, "query>PING");
			ping();
			return null;
		case DEPARTURE:
			return getDeparture(selection);
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
	}

	@Override
	public String getType(Uri uri) {
		MyLog.v(TAG, "getType(%s)", uri.getPath());
		switch (getURIMATCHER().match(uri)) {
		case DEPARTURE:
			return DEPARTURE_CONTENT_TYPE;
		case PING:
			return null;
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
		}
	}

	public Cursor getDeparture(String selection) {
		MyLog.d(TAG, "getDeparture(%s)", selection);
		try {
			JSONObject jSelection = new JSONObject(selection);
			// extract values from JSON
			RouteTripStop routeTripStop = RouteTripStop.fromJSON(jSelection.optJSONObject("routeTripStop"));
			long timestamp = jSelection.has("timestamp") ? jSelection.getLong("timestamp") : System.currentTimeMillis();
			boolean cacheOnly = jSelection.has("cacheOnly") ? jSelection.getBoolean("cacheOnly") : false;
			int cacheValidityInSec = jSelection.has("cacheValidityInSec") ? jSelection.getInt("cacheValidityInSec") : getCACHE_MAX_VALIDITY_IN_SEC();
			int cacheNotRefreshedInSec = Math.min(getCACHE_NOT_REFRESHED_IN_SEC(), cacheValidityInSec);
			// read cache
			String cacheUUID = routeTripStop.getUUID() + getAUTHORITY();
			Cache cache = getDataAlreadyInCacheIfStillUseful(cacheUUID);
			// IF cache only DO return cache OR nothing
			if (cacheOnly) {
				JSONObject jResult = null;
				if (cache != null) {
					try {
						jResult = new JSONObject(cache.getObject());
					} catch (JSONException jsone) {
						MyLog.w(TAG, jsone, "Error while parsing JSON from cache!");
						// cache not valid, returning empty
					}
				}
				// MyLog.d(TAG, "getDeparture() > use cache (only)");
				return getDepartureCursor(jResult);
			}
			// IF cache doesn't have to be refreshed DO return cache
			int tooOld = Utils.currentTimeSec() - cacheNotRefreshedInSec;
			if (cache != null && tooOld <= cache.getDate()) {
				try {
					// MyLog.d(TAG, "getDeparture() > use cache");
					return getDepartureCursor(new JSONObject(cache.getObject()));
				} catch (JSONException jsone) {
					MyLog.w(TAG, jsone, "Error while parsing JSON from cache!");
					// cache not valid, loading from www
				}
			}
			final Calendar now = Calendar.getInstance();
			now.setTimeInMillis(timestamp);
			// get departure from content provider
			// MyLog.d(TAG, "getDeparture() > NOT use cache, use content provider");
			return getDeparture(routeTripStop, now, cache, cacheUUID);
		} catch (JSONException jsone) {
			MyLog.w(TAG, jsone, "Error while parsing JSON '%s'!", selection);
			return null;
		}
	}

	public Cursor getDepartureCursor(JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "json" });
		matrixCursor.addRow(new Object[] { jsonObject.toString() });
		return matrixCursor;
	}

	private Cache getDataAlreadyInCacheIfStillUseful(String uuid) {
		MyLog.v(TAG, "getDataAlreadyInCacheIfStillUseful(%s)", uuid);
		// load cache from database
		Cache cache = DataManager.findCache(getContext().getContentResolver(), Cache.KEY_TYPE_VALUE_AUTHORITY_ROUTE_TRIP_STOP_JSON, uuid);
		// compute the too old date
		int tooOld = Utils.currentTimeSec() - getCACHE_MAX_VALIDITY_IN_SEC();
		// IF the cache is too old DO
		if (cache != null && tooOld >= cache.getDate()) {
			// don't use the cache
			cache = null;
			// delete all too old cache
			try {
				DataManager.deleteCacheOlderThan(getContext().getContentResolver(), tooOld);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Can't clean the cache!");
			}
		}
		return cache;
	}

	public void saveToCache(String uuid, JSONObject jResult) {
		MyLog.v(TAG, "saveToCache(%s)", uuid);
		final JSONArray jTimestamps = jResult == null ? null : jResult.optJSONArray("timestamps");
		if (jTimestamps == null || jTimestamps.length() == 0) {
			// MyLog.d(TAG, "saveToCache(%s,%s) > skipped because no timestamp", uuid, jResult);
			return;
		}
		Cache newCache = new Cache(Cache.KEY_TYPE_VALUE_AUTHORITY_ROUTE_TRIP_STOP_JSON, uuid, jResult.toString());
		// remove existing cache for this bus stop
		DataManager.deleteCacheIfExist(getContext().getContentResolver(), Cache.KEY_TYPE_VALUE_AUTHORITY_ROUTE_TRIP_STOP_JSON, uuid);
		// save the new value to cache
		DataManager.addCache(getContext().getContentResolver(), newCache);
	}

	public abstract void ping();

	public abstract Cursor getDeparture(RouteTripStop routeTripStop, Calendar now, Cache cache, String cacheUUID);

	public abstract UriMatcher getURIMATCHER();

	public abstract String getAUTHORITY();

	public abstract int getCACHE_NOT_REFRESHED_IN_SEC();

	public abstract int getCACHE_MAX_VALIDITY_IN_SEC();

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "delete()");
		MyLog.w(TAG, "The delete method is not available.");
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MyLog.v(TAG, "update()");
		MyLog.w(TAG, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		MyLog.v(TAG, "insert()");
		MyLog.w(TAG, "The insert method is not available.");
		return null;
	}

}

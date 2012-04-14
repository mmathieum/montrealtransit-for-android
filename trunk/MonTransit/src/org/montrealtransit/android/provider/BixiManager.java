package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.provider.BixiStore.BikeStation;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

/**
 * This manager provide methods to access the Bixi data. This class query the content provider {@link BixiProvider}
 * @author Mathieu Méa
 */
public class BixiManager {

	/**
	 * The log tag.
	 */
	private static final String TAG = BixiManager.class.getSimpleName();

	/**
	 * Delete <b>ALL</b> bike station entries
	 * @param contentResolver the content resolver
	 * @return true if 1 (or more) bike stations entries have been deleted
	 */
	public static boolean deleteAllBikeStations(ContentResolver contentResolver) {
		return contentResolver.delete(BixiStore.BikeStation.CONTENT_URI, null, null) > 0;
	}
	
	/**
	 * Delete a bike station.
	 * @param contentResolver the content resolver
	 * @param terminalName the terminal name of the bike station to delete
	 * @return true if 1 (or more) bike stations entries have been deleted
	 */
	public static boolean deleteBikeStation(ContentResolver contentResolver, String terminalName) {
		return contentResolver.delete(Uri.withAppendedPath(BikeStation.CONTENT_URI, terminalName), null, null) > 0;
	}

	/**
	 * Add a bike stations entry to the content provider
	 * @param contentResolver the content resolver
	 * @param newBikeStation the new bike station entry
	 * @param findNew true if finding a returning the newly created bike station entry
	 * @return the added bike station entry or <b>NULL</b> TODO necessary?
	 */
	public static BikeStation addBikeStation(ContentResolver contentResolver, BikeStation newBikeStation, boolean findNew) {
		final Uri uri = contentResolver.insert(BixiStore.BikeStation.CONTENT_URI, newBikeStation.getContentValues());
		if (findNew && uri != null) {
			return findBikeStation(contentResolver, uri);
		} else {
			return null;
		}
	}
	
	/**
	 * Update a bike station.
	 * @param contentResolver the content resolver
	 * @param newBikeStation the new bike station
	 * @param terminal the new bike station terminal name (ID)
	 * @return true if 1 (or more) bike stations entries have been updated
	 * @deprecated NOT WORKING use {@link #deleteBikeStation(ContentResolver, String)} and {@link #addBikeStation(ContentResolver, BikeStation, boolean)} for now
	 */
	@Deprecated
	public static boolean updateBikeStation(ContentResolver contentResolver, BikeStation newBikeStation, String terminalName) {
		MyLog.v(TAG, "updateBikeStation(%s)", terminalName);
		return contentResolver.update(Uri.withAppendedPath(BikeStation.CONTENT_URI, terminalName), newBikeStation.getContentValues(), null, null)  > 0;
	}

	/**
	 * Add a bike stations entry to the content provider
	 * @param contentResolver the content resolver
	 * @param newBikeStations the new bike stations entries
	 */
	public static void addBikeStations(ContentResolver contentResolver, List<BikeStation> newBikeStations) {
		MyLog.v(TAG, "addBikeStations(%s)", newBikeStations.size());
		for (BikeStation newBikeStation : newBikeStations) {
			contentResolver.insert(BixiStore.BikeStation.CONTENT_URI, newBikeStation.getContentValues());
		}
	}

	/**
	 * @param contentResolver the content resolver
	 * @param uri the bike station entry URI
	 * @return the bike station entry or <b>NULL</b>
	 */
	private static BikeStation findBikeStation(ContentResolver contentResolver, Uri uri) {
		MyLog.v(TAG, "findBikeStation(%s)", uri.getPath());
		BikeStation bikeStation = null;
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(uri, null, null, null, null);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					bikeStation = BixiStore.BikeStation.fromCursor(cursor);
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return bikeStation;
	}
	
	/**
	 * @see {@link StmManager#findBikeStation(ContentResolver, Uri)}
	 * @param contentResolver the content resolver
	 * @param terminalName the bike station terminal name
	 * @return the bike station with this terminal name or null
	 */
	public static BikeStation findBikeStation(ContentResolver contentResolver, String terminalName) {
		return findBikeStation(contentResolver, Uri.withAppendedPath(BikeStation.CONTENT_URI, terminalName));
	}

	/**
	 * @param contentResolver the content resolver
	 * @return all the bike stations entries
	 */
	public static Cursor findAllBikeStations(ContentResolver contentResolver) {
		MyLog.v(TAG, "findAllBikeStations()");
		return contentResolver.query(BixiStore.BikeStation.CONTENT_URI, null, null, null, null);
	}

	/**
	 * @param contentResolver the content resolver
	 * @param includeNotInstalled true if including {@link BikeStation#isInstalled()}
	 * @return all the bike stations entries in a list or <b>NULL</b>
	 * @see #findAllBikeStations(ContentResolver)
	 */
	public static List<BikeStation> findAllBikeStationsList(ContentResolver contentResolver, boolean includeNotInstalled) {
		MyLog.v(TAG, "findAllBikeStationsList(%s)", includeNotInstalled);
		List<BikeStation> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllBikeStations(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new ArrayList<BikeStation>();
					do {
						BikeStation fromCursor = BixiStore.BikeStation.fromCursor(cursor);
						if (includeNotInstalled || fromCursor.isInstalled()) {
							result.add(fromCursor);
						}
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
	 * Find all bike stations (Terminal Name => Bike Station).
	 * @param contentResolver the content resolver
	 * @param includeNotInstalled true if including not installed bike station in the result
	 * @return all bike stations (Terminal Name => Bike Station)
	 */
	public static Map<String, BikeStation> findAllBikeStationsMap(ContentResolver contentResolver, boolean includeNotInstalled) {
		MyLog.v(TAG, "findAllBikeStationsMap(%s)", includeNotInstalled);
		Map<String, BikeStation> result = null;
		Cursor cursor = null;
		try {
			cursor = findAllBikeStations(contentResolver);
			if (cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					result = new HashMap<String, BikeStation>();
					do {
						BikeStation fromCursor = BixiStore.BikeStation.fromCursor(cursor);
						if (includeNotInstalled || fromCursor.isInstalled()) {
							result.put(fromCursor.getTerminalName(), fromCursor);
						}
					} while (cursor.moveToNext());
				}
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}
}

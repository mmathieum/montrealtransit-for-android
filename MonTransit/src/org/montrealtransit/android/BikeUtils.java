package org.montrealtransit.android;

/**
 * Some useful method for bikes.
 * @author Mathieu Méa
 */
public final class BikeUtils {
	
	/**
	 * The validity of the cache (in seconds).
	 */
	public static final int CACHE_TOO_OLD_IN_SEC = 2 * 60; // 2 minutes
	/**
	 * The cache is too old to be useful, don't display it.
	 */
	public static final int CACHE_NOT_USEFUL_IN_SEC = 10 * 60; // 10 minutes
	/**
	 * No DDOS on the server!
	 */
	public static final int CACHE_TOO_FRESH_IN_SEC = 1 * 60; // 1 minute

	
	private BikeUtils() {
	}
}

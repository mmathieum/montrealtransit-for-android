package org.montrealtransit.android.services;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.PrefetchingUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.services.nextstop.IStmInfoTask;
import org.montrealtransit.android.services.nextstop.NextStopListener;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;

/**
 * Load next bus stops into cache in not in cache.
 */
public class LoadNextBusStopIntoCacheTask extends AsyncTask<Void, Void, Void> implements NextStopListener {

	public static final String TAG = LoadNextBusStopIntoCacheTask.class.getSimpleName();

	private Context context;

	private BusStop busStop;

	private WeakReference<NextStopListener> from;

	private boolean prefetch;

	private boolean force;

	public LoadNextBusStopIntoCacheTask(Context context, BusStop busStop, NextStopListener from, boolean prefetch, boolean force) {
		MyLog.d(TAG, "LoadNextBusStopIntoCacheTask()");
		this.context = context;
		this.busStop = busStop;
		this.from = new WeakReference<NextStopListener>(from);
		this.prefetch = prefetch;
		this.force = force;
	}

	@Override
	protected Void doInBackground(Void... params) {
		MyLog.d(TAG, "doInBackground()");
		if (prefetch) {
			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
			if (!PrefetchingUtils.isPrefetching(context)) {
				MyLog.d(TAG, "prefetching disabled");
				return null; // disabled
			}
			if (!PrefetchingUtils.isConnectedToWifi(context) && PrefetchingUtils.isPrefetchingWiFiOnly(context)) {
				MyLog.d(TAG, "prefetching over Wifi only");
				return null; // over WiFi only
			}
		}
		if (!force && recentDataAlreadyInCache()) {
			MyLog.d(TAG, "bus stop already in cache (%s)", this.busStop.getUID());
			return null;
		}
		MyLog.d(TAG, "Prefetching bus stop %s data...", this.busStop.getUID());
		new IStmInfoTask(context, this, busStop).execute();
		return null;
	}

	private boolean recentDataAlreadyInCache() {
		// load cache from database
		Cache cache = DataManager.findCache(context.getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP, busStop.getUID());
		// compute the too old date
		int tooOld = Utils.currentTimeSec() - BusUtils.CACHE_TOO_OLD_IN_SEC;
		// IF the cache is too old DO
		if (cache != null && tooOld >= cache.getDate()) {
			// don't use the cache
			cache = null;
			// delete all too old cache
			try {
				DataManager.deleteCacheOlderThan(context.getContentResolver(), tooOld);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Can't clean the cache!");
			}
		}
		return cache != null;
	}

	@Override
	public void onNextStopsProgress(String progress) {
		MyLog.v(TAG, "onNextStopsProgress(%s)", progress);
		NextStopListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onNextStopsProgress(progress);
		}
	}

	@Override
	public void onNextStopsLoaded(final Map<String, BusStopHours> results) {
		MyLog.v(TAG, "onNextStopsLoaded()");
		if (results == null || results.size() <= 0) {
			MyLog.d(TAG, "onNextStopsLoaded() > no result to store in cache!");
			return; // no result
		}
		// IF valid result or the last result DO
		NextStopListener fromWR = this.from == null ? null : this.from.get();
		if (fromWR != null) {
			fromWR.onNextStopsLoaded(results);
		}
		// if (containResult) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				// MyLog.d(TAG, "onNextStopsLoaded() > %s results to store in cache.", results.size());
				// store in cache
				for (String lineNumber : results.keySet()) {
					BusStopHours busStopHours = results.get(lineNumber);
					// MyLog.d(TAG, "onNextStopsLoaded() > %s result hours to store in cache.", (busStopHours == null ? null :
					// busStopHours.getSHours().size()));
					if (busStopHours != null && busStopHours.getSHours().size() > 0) {
						saveToCache(LoadNextBusStopIntoCacheTask.this.busStop.getCode(), lineNumber, busStopHours);
					}
				}
				return null;
			}

		}.execute();
	}

	private void saveToCache(String stopCode, String lineNumber, BusStopHours busStopHours) {
		// MyLog.v(TAG, "saveToCache(%s,%s)", stopCode, lineNumber);
		Cache newCache = new Cache(Cache.KEY_TYPE_VALUE_BUS_STOP, BusStop.getUID(stopCode, lineNumber), busStopHours.serialized());
		// remove existing cache for this bus stop
		DataManager.deleteCacheIfExist(context.getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP, BusStop.getUID(stopCode, lineNumber));
		// save the new value to cache
		DataManager.addCache(context.getContentResolver(), newCache);
	}

}

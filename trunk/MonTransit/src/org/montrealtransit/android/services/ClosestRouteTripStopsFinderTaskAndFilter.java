package org.montrealtransit.android.services;

import java.util.List;
import java.util.ListIterator;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.content.Context;

public class ClosestRouteTripStopsFinderTaskAndFilter extends ClosestRouteTripStopsFinderTask {

	private static final String TAG = ClosestRouteTripStopsFinderTaskAndFilter.class.getSimpleName();

	private RouteTripStop routeTripStop;

	public ClosestRouteTripStopsFinderTaskAndFilter(ClosestRouteTripStopsFinderListener from, Context context, String[] authorities, int maxResult,
			RouteTripStop routeTripStop) {
		super(from, context, authorities, maxResult);
		this.routeTripStop = routeTripStop;
	}

	@Override
	protected ClosestPOI<RouteTripStop> doInBackground(Double... params) {
		MyLog.v(TAG, "doInBackground()");
		ClosestPOI<RouteTripStop> result = super.doInBackground(params);
		List<RouteTripStop> routeTripStops = result.getPoiListOrNull();
		if (routeTripStops != null) {
			ListIterator<RouteTripStop> routeTripStopsIt = routeTripStops.listIterator();
			while (routeTripStopsIt.hasNext()) {
				RouteTripStop rts = routeTripStopsIt.next();
				// IF same stop DO
				if (rts.stop.id == this.routeTripStop.stop.id) {
					routeTripStopsIt.remove();
					continue;
				}
			}
			// remove last trip stops now (because all nearby list takes too long)
			ListIterator<RouteTripStop> resultIt = routeTripStops.listIterator();
			while (resultIt.hasNext()) {
				final RouteTripStop rts = resultIt.next();
				if (rts == null) {
					// MyLog.d(TAG, "refreshNearby()>Null or not Route Trip Stop! %s", poi);
					continue;
				}
				// IF last stop of the trip DO
				final TripStop lastTripStop = AbstractManager.findTripLastTripStop(this.context, Utils.newContentUri(rts.authority), rts.trip.id);
				if (lastTripStop != null && rts.stop.id == lastTripStop.stop.id) {
					resultIt.remove();
					// MyLog.d(TAG, "refreshNearby()>REMOVED %s", routeTripStop.stop);
					continue;
				}
				// MyLog.d(TAG, "refreshNearby()>NOT REMOVED");
			}
		}
		result.setPoiList(routeTripStops);
		return result;
	}

}

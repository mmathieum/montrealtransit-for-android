package org.montrealtransit.android.services.nextstop;

import java.util.HashMap;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.StopTimes;
import org.montrealtransit.android.provider.common.AbstractScheduleManager;

import android.content.Context;
import android.os.Environment;

public class ScheduleTask extends AbstractNextStopProvider {

	public static final String TAG = ScheduleTask.class.getSimpleName();
	private boolean force;

	public ScheduleTask(Context context, NextStopListener from, RouteTripStop stop, String scheduleAuthority, boolean force) {
		super(context, from, stop, scheduleAuthority);
		this.force = force;
	}

	@Override
	protected Map<String, StopTimes> doInBackground(Void... params) {
		MyLog.v(TAG, "doInBackground()");
		if (this.routeTripStop == null) {
			MyLog.w(TAG, "No stop available!");
			return null;
		}
		if (!Utils.isContentProviderAvailable(this.context, this.scheduleAuthority)) {
			String errorMessage;
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				errorMessage = this.context.getString(R.string.no_offline_schedule_or_sd_card_not_mounted);
			} else {
				errorMessage = this.context.getString(R.string.no_offline_schedule);
			}
			MyLog.w(TAG, "Content provider '%s' not available!", this.scheduleAuthority);
			Map<String, StopTimes> stopTimes = new HashMap<String, StopTimes>();
			stopTimes.put(this.routeTripStop.getUUID(), new StopTimes(this.context.getString(R.string.offline_schedule), false, errorMessage));
			return stopTimes;
		}
		publishProgress(this.context.getString(R.string.downloading_data_from_and_source, getSourceName()));
		Integer cacheValidityInSec = force ? -1 : null;
		final StopTimes stopTime = AbstractScheduleManager.findStopTimes(this.context.getContentResolver(), Utils.newContentUri(this.scheduleAuthority),
				routeTripStop, Utils.recentTimeMillis(), false, cacheValidityInSec);
		// MyLog.d(TAG, "stopTime: %s", stopTime);
		if (stopTime == null) {
			MyLog.w(TAG, "No stop times found for stop '%s' in provider '%s'!", this.routeTripStop, this.scheduleAuthority);
			// stopTime = new StopTimes(null, false, this.context.getString(R.string.error));
		}
		Map<String, StopTimes> stopTimes = new HashMap<String, StopTimes>();
		stopTimes.put(this.routeTripStop.getUUID(), stopTime);
		return stopTimes;
	}

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String getSourceName() {
		return context.getString(R.string.offline_schedule);
	}
}

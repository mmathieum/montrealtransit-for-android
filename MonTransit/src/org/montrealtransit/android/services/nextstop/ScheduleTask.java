package org.montrealtransit.android.services.nextstop;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.StopTimes;
import org.montrealtransit.android.provider.common.AbstractScheduleManager;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public class ScheduleTask extends AbstractNextStopProvider {

	public static final String TAG = ScheduleTask.class.getSimpleName();

	public ScheduleTask(Context context, NextStopListener from, RouteTripStop stop) {
		super(context, from, stop);
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

	@Override
	protected Map<String, StopTimes> doInBackground(Void... params) {
		MyLog.v(TAG, "doInBackground()");
		if (this.routeTripStop == null) {
			MyLog.w(TAG, "No stop available!");
			return null;
		}
		final String authority = AbstractScheduleManager.authoritiesToScheduleAuthorities.get(this.routeTripStop.authority);
		if (!AbstractScheduleManager.isContentProviderAvailable(this.context, authority)) {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				publishProgress(this.context.getString(R.string.no_offline_schedule_or_sd_card_not_mounted));
			} else {
				publishProgress(this.context.getString(R.string.no_offline_schedule));
			}
			MyLog.w(TAG, "No content provider available!");
			return null;
		}
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		final String loadingFrom = this.context.getString(R.string.downloading_data_from_and_source, getSourceName());
		final String processingData = this.context.getResources().getString(R.string.processing_data);
		Map<String, StopTimes> stopTimes = new HashMap<String, StopTimes>();
		try {
			StopTimes stopTime = new StopTimes(getSourceName());
			final Uri contentUri = Utils.newContentUri(authority);
			final Calendar now = Calendar.getInstance();
			// now.set(Calendar.HOUR_OF_DAY, 23); // TEST
			// now.set(Calendar.MINUTE, 59); // TEST
			// now.set(Calendar.DAY_OF_MONTH, 21); // TEST
			// now.set(Calendar.MONTH, Calendar.DECEMBER); // TEST
			// now.set(Calendar.YEAR, 2013); // TEST
			// MyLog.d(TAG, "Now: %s", now);
			// 1st - check if yesterday schedule is over (based on user device date/time)
			MyLog.d(TAG, "Checking if yesterday schedule is over...");
			Calendar cal = now;
			cal.add(Calendar.DATE, -1);
			final String dateYesterday = DATE_FORMAT.format(cal.getTime());
			// MyLog.d(TAG, "Date Yesterday: %s", dateYesterday);
			final String timeYesterday = String.valueOf(Integer.valueOf(TIME_FORMAT.format(cal.getTime())) + 240000);
			cal.add(Calendar.DATE, +1);
			// MyLog.d(TAG, "Time Yesterday: %s", timeYesterday);
			publishProgress(loadingFrom);
			List<String> sHours = AbstractScheduleManager.findScheduleList(context.getContentResolver(), contentUri, routeTripStop.route.id,
					routeTripStop.trip.id, routeTripStop.stop.id, dateYesterday, timeYesterday);
			// MyLog.d(TAG, "provider result: " + sHours);
			publishProgress(processingData);
			appendToStopTimes(sHours, stopTime);
			MyLog.d(TAG, "Checking if yesterday schedule is over... DONE");
			// 2nd - check today schedule (provider date/time)
			MyLog.d(TAG, "Checking today schedule...");
			publishProgress(loadingFrom);
			final String dateNow = DATE_FORMAT.format(now.getTime()); // null;
			// MyLog.d(TAG, "Date Now: %s", dateNow);
			final String timeNow = TIME_FORMAT.format(now.getTime()); // null;
			// MyLog.d(TAG, "Time Now: %s", timeNow);
			sHours = AbstractScheduleManager.findScheduleList(context.getContentResolver(), contentUri, routeTripStop.route.id, routeTripStop.trip.id,
					routeTripStop.stop.id, dateNow, timeNow);
			// MyLog.d(TAG, "provider result: " + sHours);
			publishProgress(processingData);
			appendToStopTimes(sHours, stopTime);
			MyLog.d(TAG, "Checking today schedule... DONE");
			// 4th - look for last schedule
			MyLog.d(TAG, "Checking last schedule...");
			cal.add(Calendar.HOUR, -1);
			final String oneHourAgoDate = DATE_FORMAT.format(cal.getTime()); // null;
			// MyLog.d(TAG, "Date 1 hour ago: %s", oneHourAgoDate);
			final String oneHourAgoTime = TIME_FORMAT.format(cal.getTime());
			// MyLog.d(TAG, "Hour 1 hour ago: %s", oneHourAgoTime);
			cal.add(Calendar.HOUR, +1);
			sHours = AbstractScheduleManager.findScheduleList(context.getContentResolver(), contentUri, routeTripStop.route.id, routeTripStop.trip.id,
					routeTripStop.stop.id, oneHourAgoDate, oneHourAgoTime);
			// MyLog.d(TAG, "provider result: " + sHours);
			if (sHours != null && sHours.size() > 0) {
				for (int i = sHours.size() - 1; i >= 0; i--) {
					final String formattedHour = formatHour(sHours.get(i));
					// MyLog.d(TAG, "fHour : " + formattedHour);
					if (!stopTime.getSTimes().contains(formattedHour)) {
						// MyLog.d(TAG, "fHour previous : " + formattedHour);
						stopTime.setPreviousTime(formattedHour);
						break;
					}
				}
			}
			MyLog.d(TAG, "Checking last schedule... DONE");
			// 3rd - check tomorrow schedule
			// if (stopHours.getSHours().size() < 7) {
			MyLog.d(TAG, "Checking tomorrow schedule...");
			cal.add(Calendar.DATE, +1);
			final String tomorrowDate = DATE_FORMAT.format(cal.getTime());
			// MyLog.d(TAG, "Date tomorrow: %s", tomorrowDate);
			cal.add(Calendar.DATE, -1);
			final String afterMidnightHour = "000000";
			// MyLog.d(TAG, "Date tomorrow: %s", afterMidnightHour);
			publishProgress(loadingFrom);
			sHours = AbstractScheduleManager.findScheduleList(context.getContentResolver(), contentUri, routeTripStop.route.id, routeTripStop.trip.id,
					routeTripStop.stop.id, tomorrowDate, afterMidnightHour);
			// MyLog.d(TAG, "provider result: " + sHours);
			publishProgress(processingData);
			appendToStopTimes(sHours, stopTime);
			MyLog.d(TAG, "Checking tomorrow schedule... DONE");
			// }
			if (stopTime.hasSTimes()) {
				stopTimes.put(this.routeTripStop.getUUID(), stopTime);
			} else {
				// no information
				errorMessage = this.context.getString(R.string.stop_no_info_and_source, this.routeTripStop.route.shortName, getSourceName());
				publishProgress(errorMessage);
				stopTimes.put(this.routeTripStop.getUUID(), new StopTimes(getSourceName(), errorMessage));
				// AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_STOP_REMOVED, this.stop.getUID(),
				// context.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
			}
			return stopTimes;
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(errorMessage);
			stopTimes.put(this.routeTripStop.getUUID(), new StopTimes(getSourceName(), this.context.getString(R.string.error)));
			return stopTimes;
		}
	}

	private void appendToStopTimes(List<String> sHours, StopTimes stopTimes) {
		MyLog.v(TAG, "appendToStopTimes(%s)", sHours);
		if (sHours != null) {
			for (String sHour : sHours) {
				final String formattedHour = formatHour(sHour);
				// MyLog.d(TAG, "fHour : %s", formattedHour);
				stopTimes.addSTime(formattedHour);
			}
		}
	}

	private String formatHour(String time) {
		// MyLog.v(TAG, "formatHour(%s)", time);
		try {
			int timeInt = Integer.valueOf(time);
			if (timeInt >= 240000) {
				timeInt -= 240000;
			}
			// MyLog.d(TAG, "time: " + time);
			return OUTPUT_FORMAT.format(SOURCE_L_FORMAT.parse(String.format("%06d", timeInt)));
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while parsing time %s!", time);
			return time;
		}
	}

	public static final SimpleDateFormat SOURCE_S_FORMAT = new SimpleDateFormat("Hmmss");
	public static final SimpleDateFormat SOURCE_L_FORMAT = new SimpleDateFormat("HHmmss");
	public static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("HH'h'mm");

	@Override
	public String getTag() {
		return TAG;
	}

	@Override
	public String getSourceName() {
		return context.getString(R.string.offline_schedule);
	}
}

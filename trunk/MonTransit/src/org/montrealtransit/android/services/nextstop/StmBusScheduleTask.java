package org.montrealtransit.android.services.nextstop;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.StmBusScheduleManager;

import android.content.Context;
import android.os.Environment;

public class StmBusScheduleTask extends AbstractNextStopProvider {

	public static final String TAG = StmBusScheduleTask.class.getSimpleName();

	public StmBusScheduleTask(Context context, NextStopListener from, RouteTripStop busStop) {
		super(context, from, busStop);
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");

	@Override
	protected Map<String, BusStopHours> doInBackground(Void... params) {
		if (this.routeTripStop == null) {
			return null;
		}
		if (!StmBusScheduleManager.isContentProviderAvailable(this.context)) {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				publishProgress(this.context.getString(R.string.no_offline_schedule_or_sd_card_not_mounted));
			} else {
				publishProgress(this.context.getString(R.string.no_offline_schedule));
			}
			return null;
		}
		String errorMessage = this.context.getString(R.string.error); // set the default error message
		final String loadingFrom = context.getString(R.string.downloading_data_from_and_source, getSourceName());
		final String processingData = this.context.getResources().getString(R.string.processing_data);
		Map<String, BusStopHours> hours = new HashMap<String, BusStopHours>();
		try {
			BusStopHours busStopHours = new BusStopHours(getSourceName());
			// 1st - check if yesterday schedule is over (based on user device date/time)
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			String yesterday = DATE_FORMAT.format(cal.getTime());
			String timeYesterday = String.valueOf(Integer.valueOf(TIME_FORMAT.format(cal.getTime())) + 240000);
			cal.add(Calendar.DATE, +1);
			// MyLog.d(TAG, "timeYesterday: " + timeYesterday);
			publishProgress(loadingFrom);
			List<String> sHours = StmBusScheduleManager.findBusScheduleList(context.getContentResolver(), routeTripStop.route.shortName, routeTripStop.stop.code,
					yesterday, timeYesterday);
			// MyLog.d(TAG, "provider result: " + sHours);
			publishProgress(processingData);
			appendToBusStopHours(sHours, busStopHours);
			// 2nd - check today schedule (provider date/time)
			publishProgress(loadingFrom);
			sHours = StmBusScheduleManager.findBusScheduleList(context.getContentResolver(), routeTripStop.route.shortName, routeTripStop.stop.code, null, null);
			// MyLog.d(TAG, "provider result: " + sHours);
			publishProgress(processingData);
			appendToBusStopHours(sHours, busStopHours);
			// 3rd - check tomorrow schedule
			if (busStopHours.getSHours().size() < 7) {
				cal.add(Calendar.DATE, +1);
				String tomorrow = DATE_FORMAT.format(cal.getTime());
				cal.add(Calendar.DATE, -1);
				String afterMidnight = "000000";
				publishProgress(loadingFrom);
				sHours = StmBusScheduleManager.findBusScheduleList(context.getContentResolver(), routeTripStop.route.shortName, routeTripStop.stop.code, tomorrow,
						afterMidnight);
				// MyLog.d(TAG, "provider result: " + sHours);
				publishProgress(processingData);
				appendToBusStopHours(sHours, busStopHours);
			}
			// 4th - look for last schedule
			cal.add(Calendar.HOUR, -1);
			String oneHourAgo = TIME_FORMAT.format(cal.getTime());
			cal.add(Calendar.HOUR, +1);
			sHours = StmBusScheduleManager.findBusScheduleList(context.getContentResolver(), routeTripStop.route.shortName, routeTripStop.stop.code, null, oneHourAgo);
			// MyLog.d(TAG, "provider result: " + sHours);
			if (sHours != null && sHours.size() > 0) {
				for (int i = sHours.size() - 1; i >= 0; i--) {
					final String formattedHour = formatHour(sHours.get(i));
					// MyLog.d(TAG, "fHour : " + formattedHour);
					if (!busStopHours.getSHours().contains(formattedHour)) {
						// MyLog.d(TAG, "fHour previous : " + formattedHour);
						busStopHours.setPreviousHour(formattedHour);
						break;
					}
				}
			}
			hours.put(routeTripStop.route.shortName, busStopHours);
			if (hours.size() == 0) {
				// no information
				errorMessage = this.context.getString(R.string.bus_stop_no_info_and_source, this.routeTripStop.route.shortName, getSourceName());
				publishProgress(errorMessage);
				hours.put(this.routeTripStop.route.shortName, new BusStopHours(getSourceName(), errorMessage));
				// AnalyticsUtils.trackEvent(context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_BUS_STOP_REMOVED, this.busStop.getUID(),
				// context.getPackageManager().getPackageInfo(Constant.PKG, 0).versionCode);
			}
			return hours;
		} catch (Exception e) {
			MyLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			publishProgress(errorMessage);
			hours.put(this.routeTripStop.route.shortName, new BusStopHours(getSourceName(), this.context.getString(R.string.error)));
			return hours;
		}
	}

	private void appendToBusStopHours(List<String> sHours, BusStopHours busStopHours) {
		MyLog.v(TAG, "appendToBusStopHours(%s)", sHours);
		if (sHours != null && sHours.size() > 0) {
			for (int i = 0; i < sHours.size() && busStopHours.getSHours().size() < 7; i++) {
				final String formattedHour = formatHour(sHours.get(i));
				// MyLog.d(TAG, "fHour : " + formattedHour);
				busStopHours.addSHour(formattedHour);
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

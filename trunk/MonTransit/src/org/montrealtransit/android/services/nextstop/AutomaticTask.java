package org.montrealtransit.android.services.nextstop;

import java.util.Map;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.content.Context;

/**
 * Automatically get the next passages from the best source available.
 * 
 * <ul>
 * <li>see {@link StmMobileTask}</li>
 * <li>see {@link StmInfoTask}</li>
 * </ul>
 * @author Mathieu Méa
 */
public class AutomaticTask extends AbstractNextStopProvider implements NextStopListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = AutomaticTask.class.getSimpleName();
	/**
	 * The {@link StmMobileTask}.
	 */
	private StmMobileTask taskStmMobile;
	/**
	 * The {@link StmInfoTask}.
	 */
	private StmInfoTask taskStmInfo;
	/**
	 * The bus stop.
	 */
	private BusStop busStop;

	/**
	 * @see AbstractNextStopProvider#AbstractNextStopProvider(NextStopListener, Context)
	 */
	public AutomaticTask(NextStopListener from, Context context) {
		super(from, context);
	}

	@Override
	protected Map<String, BusStopHours> doInBackground(BusStop... busStops) {
		MyLog.v(TAG, "doInBackground()");
		// TODO ask the most reliable 1st and after 5 seconds, if no response, ask the other one(s)
		this.busStop = busStops[0];

		this.taskStmMobile = new StmMobileTask(this, context);
		this.taskStmMobile.execute(this.busStop);

		this.taskStmInfo = new StmInfoTask(this, context);
		this.taskStmInfo.execute(this.busStop);

		return null;
	}

	@Override
	public void onNextStopsProgress(String progress) {
		MyLog.v(getTag(), "onNextStopsProgress(%s)", progress);
		this.from.onNextStopsProgress(progress);
	}

	@Override
	public void onNextStopsLoaded(Map<String, BusStopHours> results) {
		MyLog.v(TAG, "onNextStopsLoaded()");
		boolean containResult = results != null && results.get(this.busStop.getLineNumber()).getSHours().size() > 0;
		if (containResult) {
			// cancel/stop all other tasks now
			stopAllTasks();
		}
		// IF valid result or the last result DO
		if (containResult || countRunningTask() == 0) {
			this.from.onNextStopsLoaded(results);
		}
	}

	/**
	 * @return the number of running task
	 */
	private int countRunningTask() {
		MyLog.v(TAG, "countRunningTask()");
		int nbRunning = 0;
		if (this.taskStmInfo != null && this.taskStmInfo.getStatus() != Status.FINISHED) {
			nbRunning++;
		}
		if (this.taskStmMobile != null && this.taskStmMobile.getStatus() != Status.FINISHED) {
			nbRunning++;
		}
		return nbRunning;
	}

	/**
	 * Stops all {@link Status#RUNNING} {@link Status#PENDING} tasks.
	 */
	private void stopAllTasks() {
		MyLog.v(TAG, "stopAllTasks()");
		if (this.taskStmInfo != null && this.taskStmInfo.getStatus() != Status.FINISHED) {
			this.taskStmInfo.cancel(true);
			this.taskStmInfo = null;
		}
		if (this.taskStmMobile != null && this.taskStmMobile.getStatus() != Status.FINISHED) {
			this.taskStmMobile.cancel(true);
			this.taskStmMobile = null;
		}
	}

	@Override
	public String getTag() {
		return TAG;
	}
}

package org.montrealtransit.android.services;

import java.util.Arrays;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.StmInfoStatus;
import org.montrealtransit.android.data.StmInfoStatuses;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.TwitterApi;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import android.content.Context;
import android.os.AsyncTask;

/**
 * This task get the STM info status from twitter.com/stminfo
 * @author Mathieu Méa
 */
public class StmInfoStatusReader extends AsyncTask<String, String, StmInfoStatuses> {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoStatusReader.class.getSimpleName();

	/**
	 * The source string.
	 */
	public static final String SOURCE = "twitter.com/stminfo";

	/**
	 * The context executing the task.
	 */
	private Context context;

	/**
	 * The class that will handle the answer.
	 */
	private StmInfoStatusReaderListener from;

	/**
	 * The default constructor.
	 * @param from the class that will handle the answer
	 * @param context context executing the task
	 */
	public StmInfoStatusReader(StmInfoStatusReaderListener from, Context context) {
		this.from = from;
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected StmInfoStatuses doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground(%s)", Arrays.asList(params));
		try {
			List<TwitterApi> twitterApis = DataManager.findAllTwitterApisList(this.context.getContentResolver());
			Twitter twitter;
			// IF user is authenticated with Twitter DO
			if (TwitterUtils.isConnected(twitterApis)) {
				MyLog.d(TAG, "Connecting to Twitter using authentication...");
				publishProgress();
				TwitterApi twitterUserAccount = twitterApis.get(0);;
				String consumerKey = TwitterUtils.getConsumerKey(this.context);
				String consumerSecret = TwitterUtils.getConsumerSecret(this.context);
				AccessToken accessToken = new AccessToken(twitterUserAccount.getToken(), twitterUserAccount.getTokenSecret());
				twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);
			} else {
				MyLog.d(TAG, "Connecting to Twitter anonymously...");
				twitter = new TwitterFactory().getInstance(); // Anonymous
			}
			StmInfoStatuses statuses = new StmInfoStatuses();
			boolean french = false;
			if (Utils.getUserLocale().equals("fr")) {
				french = true;
			}
			ResponseList<twitter4j.Status> userTimeline = twitter.getUserTimeline("stminfo");
			for (twitter4j.Status status : userTimeline) {
				String message = status.getText();
				if ((french && message.endsWith("F")) || (!french && message.endsWith("E"))) {
					StmInfoStatus stmStatus = new StmInfoStatus(status.getText());
					stmStatus.setDate(status.getCreatedAt());
					statuses.add(stmStatus);
				}
			}
			return statuses;
		} catch (TwitterException e) {
			MyLog.e(TAG, "Twitter Error!", e);
			if (e.isCausedByNetworkIssue()) {
				publishProgress(this.context.getString(R.string.no_internet));
				return new StmInfoStatuses(this.context.getString(R.string.no_internet));
			} else if (e.exceededRateLimitation()) {
				//TODO handle twitter API error // RateLimitStatus
				if (e.getRateLimitStatus() != null) {
					MyLog.d(TAG, "RateLimitStatus:");
					MyLog.d(TAG, "getHourlyLimit():" + e.getRateLimitStatus().getHourlyLimit());
					MyLog.d(TAG, "getRemainingHits():" + e.getRateLimitStatus().getRemainingHits());
					MyLog.d(TAG, "getResetTimeInSeconds():" + e.getRateLimitStatus().getResetTimeInSeconds());
					MyLog.d(TAG, "getSecondsUntilReset():" + e.getRateLimitStatus().getSecondsUntilReset());
					MyLog.d(TAG, "getResetTime():" + e.getRateLimitStatus().getResetTime());
				} else {
					MyLog.d(TAG, "NO RateLimitStatus!");
				}
				publishProgress(this.context.getString(R.string.error_http_400_twitter));
				return new StmInfoStatuses(this.context.getString(R.string.error_http_400_twitter));
			} else {
				publishProgress(this.context.getString(R.string.error_http_400_twitter));
				return new StmInfoStatuses(this.context.getString(R.string.error_http_400_twitter));
			}
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
			publishProgress(this.context.getString(R.string.error));
			return new StmInfoStatuses(this.context.getString(R.string.error));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(StmInfoStatuses result) {
		from.onStmInfoStatusesLoaded(result);
		super.onPostExecute(result);
	}
}

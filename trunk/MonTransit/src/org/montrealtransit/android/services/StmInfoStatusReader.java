package org.montrealtransit.android.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.TwitterUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.ServiceStatus;
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
public class StmInfoStatusReader extends AsyncTask<String, String, String> {

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
	protected String doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground(%s)", Arrays.asList(params));
		boolean isConnected = false;
		try {
			List<TwitterApi> twitterApis = DataManager.findAllTwitterApisList(this.context.getContentResolver());
			Twitter twitter;
			// IF user is authenticated with Twitter DO
			if (TwitterUtils.isConnected(twitterApis)) {
				MyLog.d(TAG, "Connecting to Twitter using authentication...");
				isConnected = true;
				publishProgress();
				TwitterApi twitterUserAccount = twitterApis.get(0);
				String consumerKey = TwitterUtils.getConsumerKey(this.context);
				String consumerSecret = TwitterUtils.getConsumerSecret(this.context);
				AccessToken accessToken = new AccessToken(twitterUserAccount.getToken(),
				        twitterUserAccount.getTokenSecret());
				twitter = new TwitterFactory().getOAuthAuthorizedInstance(consumerKey, consumerSecret, accessToken);
			} else {
				MyLog.d(TAG, "Connecting to Twitter anonymously...");
				twitter = new TwitterFactory().getInstance(); // Anonymous
			}
			ResponseList<twitter4j.Status> userTimeline = twitter.getUserTimeline("stminfo");

			List<ServiceStatus> allServiceStatus = new ArrayList<ServiceStatus>();
			// FOR each status DO
			for (twitter4j.Status twitterStatus : userTimeline) {
				// extract the info from the Twitter message
				ServiceStatus serviceStatus = new ServiceStatus();
				String statusText = twitterStatus.getText();
				// extract the service status from the code
				String statusChar = statusText.substring(statusText.length() - 2, statusText.length() - 1);
				if (statusChar.equals("V")) {
					serviceStatus.setType(ServiceStatus.STATUS_TYPE_GREEN);
				} else if (statusChar.equals("J")) {
					serviceStatus.setType(ServiceStatus.STATUS_TYPE_YELLOW);
				} else if (statusChar.equals("R")) {
					serviceStatus.setType(ServiceStatus.STATUS_TYPE_RED);
				} else {
					serviceStatus.setType(ServiceStatus.STATUS_TYPE_DEFAULT);
				}
				// clean message (remove ' #STM XY')
				serviceStatus.setMessage(statusText.substring(0, statusText.length() - 8));
				// set language
				if (statusText.endsWith("F")) {
					serviceStatus.setLanguage(ServiceStatus.STATUS_LANG_FRENCH);
				} else if (statusText.endsWith("E")) {
					serviceStatus.setLanguage(ServiceStatus.STATUS_LANG_ENGLISH);
				} else {
					serviceStatus.setLanguage(ServiceStatus.STATUS_LANG_UNKNOWN);
				}
				// dates
				int pubDate = (int) (twitterStatus.getCreatedAt().getTime() / 1000);
				serviceStatus.setPubDate(pubDate);
				int readDate = (int) (System.currentTimeMillis() / 1000);
				serviceStatus.setReadDate(readDate);
				// source name
				serviceStatus.setSourceName("stminfo");
				// source link
				serviceStatus.setSourceLink(TwitterUtils.getTwitterStatusURL("stminfo", twitterStatus.getId()));
				// add the status to the list
				allServiceStatus.add(serviceStatus);
			}
			// delete existing status
			DataManager.deleteAllServiceStatus(this.context.getContentResolver());
			// add new status
			for (ServiceStatus serviceStatus : allServiceStatus) {
				DataManager.addServiceStatus(context.getContentResolver(), serviceStatus);
			}
			return null;
		} catch (TwitterException e) {
			MyLog.d(TAG, "Twitter Error!", e);
			if (e.isCausedByNetworkIssue()) {
				// no Internet
				publishProgress(this.context.getString(R.string.no_internet));
				return this.context.getString(R.string.no_internet);
			} else if (e.exceededRateLimitation()) {
				// Twitter rate limit exceeded.
				return handleTwitterError(isConnected, e);
			} else {
				// Unknown Twitter error
				publishProgress(this.context.getString(R.string.twitter_error));
				return this.context.getString(R.string.twitter_error);
			}
		} catch (Exception e) {
			// Unknown error
			MyLog.e(TAG, "INTERNAL ERROR: Unknown Exception", e);
			publishProgress(this.context.getString(R.string.error));
			return this.context.getString(R.string.error);
		}
	}

	/**
	 * Handle Twitter error
	 * @param isConnected true if the user is authenticated
	 * @param e the twitter error
	 * @return the error message
	 */
	private String handleTwitterError(boolean isConnected, TwitterException e) {
	    String loginString = context.getString(R.string.menu_twitter_login);
	    if (e.getRateLimitStatus() != null) {
	    	CharSequence readTime = Utils.formatSameDayDate(e.getRateLimitStatus().getResetTime());
	    	if (isConnected) {
	    		String message = context.getString(R.string.twitter_error_http_400_auth_and_time, readTime,
	    		        loginString);
	    		publishProgress(message);
	    		return message;
	    	} else {
	    		String message = context.getString(R.string.twitter_error_http_400_anonymous_and_time,
	    		        readTime, loginString);
	    		publishProgress(message);
	    		return message;
	    	}
	    } else {
	    	if (isConnected) {
	    		String message = context.getString(R.string.twitter_error_http_400_auth, loginString);
	    		publishProgress(message);
	    		return message;
	    	} else {
	    		String message = context.getString(R.string.twitter_error_http_400_anonymous, loginString);
	    		publishProgress(message);
	    		return message;
	    	}
	    }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(String errorMessage) {
		from.onStmInfoStatusesLoaded(errorMessage);
		super.onPostExecute(errorMessage);
	}
}

package org.montrealtransit.android;

import java.util.List;

import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.signature.HmacSha1MessageSigner;

import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.TwitterApi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

/**
 * This class contains useful methods to interact with the Twitter API including the login/logout process.
 * @author Mathieu Méa
 */
public class TwitterUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = TwitterUtils.class.getSimpleName();

	/**
	 * The Twitter API Callback URL.
	 */
	public static final String CALLBACK_URL = "montransit://twitt";

	/**
	 * The preferences use to store the request token and secret during the logging process.
	 */
	public static final String PREFS_NAME = "TwitterTmp";
	public static final String REQUEST_TOKEN = "request_token";
	public static final String REQUEST_SECRET = "request_secret";

	/**
	 * The Twitter API OAuth URLs.
	 */
	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	/**
	 * The instance.
	 */
	private static TwitterUtils instance;

	/**
	 * The Twitter API provider.
	 */
	private static CommonsHttpOAuthProvider provider;
	/**
	 * The Twitter API consumer.
	 */
	private static CommonsHttpOAuthConsumer consumer;

	/**
	 * Private constructor.
	 */
	private TwitterUtils() {
	}

	/**
	 * @return the instance
	 */
	public static TwitterUtils getInstance() {
		if (instance == null) {
			instance = new TwitterUtils();
		}
		return instance;
	}

	/**
	 * @return the provider (initialize on the first call)
	 */
	public static CommonsHttpOAuthProvider getProvider() {
		if (provider == null) {
			provider = new CommonsHttpOAuthProvider(TWITTER_REQUEST_TOKEN_URL, TWITTER_ACCESS_TOKEN_URL,
			        TWITTER_AUTHORIZE_URL);
		}
		return provider;
	}

	/**
	 * @param context the context
	 * @return the consumer (initialize on the first call)
	 */
	public static CommonsHttpOAuthConsumer getConsumer(Context context) {
		if (consumer == null) {
			consumer = new CommonsHttpOAuthConsumer(getConsumerKey(context), getConsumerSecret(context));
			consumer.setMessageSigner(new HmacSha1MessageSigner());
		}
		return consumer;
	}

	/**
	 * @param context the context
	 * @return the consumer key from the resource file.
	 */
	public static String getConsumerKey(Context context) {
		return context.getString(R.string.twitter_api_consumer_key);
	}

	/**
	 * @param context the context
	 * @return the consumer secret from the resource file.
	 */
	public static String getConsumerSecret(Context context) {
		return context.getString(R.string.twitter_api_consumer_secret);
	}

	/**
	 * @param context the context
	 * @return true if the first of Twitter API settings is an authenticated user.
	 */
	public static boolean isConnected(List<TwitterApi> twitterApis) {
		return twitterApis != null && twitterApis.size() >= 1;
	}

	/**
	 * @param context the context
	 * @return true if the user is authenticated with the Twitter API.
	 */
	public static boolean isConnected(Context context) {
		return isConnected(DataManager.findAllTwitterApisList(context.getContentResolver()));
	}

	/**
	 * Start the login process. Launch the Twitter authentication page URL.
	 * @param context the context
	 */
	public void startLoginProcess(Context context) {
		MyLog.v(TAG, "startLoginProcess()");
		try {
			// TODO show a dialog with more information who/what/why/how
			Utils.notifyTheUserLong(context, context.getString(R.string.twitter_pre_auth));
			getProvider().setOAuth10a(true);
			String authUrl = getProvider().retrieveRequestToken(getConsumer(context), CALLBACK_URL);
			// saving the token
			SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(REQUEST_TOKEN, getConsumer(context).getToken());
			editor.putString(REQUEST_SECRET, getConsumer(context).getTokenSecret());
			SupportFactory.getInstance(context).applySharedPreferencesEditor(editor);
			AnalyticsUtils.dispatch(context); // while we are connected, sent the analytics data
			// launching the browser
			context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
		} catch (Exception e) {
			MyLog.e(TAG, e, "Error while trying to launch Twitter Authentication!");
		}
	}

	/**
	 * @param intent the intent
	 * @return return true if the intent data URI match with the Twitter API Callback URL
	 */
	public static boolean isTwitterCallback(Intent intent) {
		MyLog.v(TAG, "isTwitterCallback()");
		Uri uri = intent.getData();
		return uri != null && uri.toString().startsWith(CALLBACK_URL);
	}

	/**
	 * Log the user in Twitter using the Callback URL.
	 * @param context
	 * @param uri
	 */
	public void login(Context context, Uri uri) {
		MyLog.v(TAG, "login()");
		try {
			MyLog.d(TAG, "Fetching access token from Twitter...");
			// read the request token and secret from the app settings
			SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
			String token = settings.getString(REQUEST_TOKEN, null);
			String secret = settings.getString(REQUEST_SECRET, null);
			// apply the request token and secret to the consumer
			getConsumer(context).setTokenWithSecret(token, secret);
			// read the OAuth verifier and token from the Twitter Callback URL
			String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
			String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);
			// check if the token returned by the Twitter Callback URL matches the sent token
			if (!token.equals(otoken)) {
				MyLog.w(TAG, "Twitter error: returned token differs from sent token!");
			}
			// retrieve the access token from the consumer and the OAuth verifier returner by the Twitter Callback URL
			getProvider().retrieveAccessToken(getConsumer(context), oauth_verifier);
			// saving the Twitter user account token and secret
			TwitterApi newTwitterApi = new TwitterApi();
			newTwitterApi.setToken(getConsumer(context).getToken());
			newTwitterApi.setTokenSecret(getConsumer(context).getTokenSecret());
			DataManager.addTwitterApi(context.getContentResolver(), newTwitterApi);
			// notify the user of the success
			Utils.notifyTheUser(context, context.getString(R.string.twitter_auth_success));
		} catch (OAuthException oae) {
			Utils.notifyTheUser(context, context.getString(R.string.twitter_auth_failed));
			MyLog.w(TAG, oae, "Twitter OAuth error!");
		}
	}

	/**
	 * Logout the user from Twitter.
	 * @param context the context
	 */
	public static void logout(Context context) {
		MyLog.v(TAG, "logout()");
		try {
			DataManager.deleteAllTwitterAPI(context.getContentResolver());
			Utils.notifyTheUser(context, context.getString(R.string.twitter_logout_success));
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while disconnecting!");
		}
	}

	/**
	 * @param username the Twitter user name
	 * @param id the Twitter status id
	 * @return the Twitter status URL
	 */
	public static String getTwitterStatusURL(String username, long id) {
		// Twitter Status Link: http://twitter.com/<username>/statuses/<id>
		return String.format("http://twitter.com/%s/statuses/%s", username, id);
	}
}

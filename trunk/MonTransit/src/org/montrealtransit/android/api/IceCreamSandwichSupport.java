package org.montrealtransit.android.api;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.services.NfcListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

/**
 * Features available for Android 4.0 Ice Cream Sandwich (API Level 14) and higher.
 * @author Mathieu Méa
 */
@TargetApi(14)
public class IceCreamSandwichSupport extends HoneycombSupport {

	/**
	 * The log tag.
	 */
	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 */
	public IceCreamSandwichSupport() {
		super();
	}

	@Override
	public boolean isNfcIntent(Intent intent) {
		return NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());
	}

	@Override
	public void processNfcIntent(Intent intent, NfcListener listener) {
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage ndefMessage = (NdefMessage) rawMsgs[0];
		List<String> stringRecords = new ArrayList<String>();
		for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
			stringRecords.add(new String(ndefRecord.getPayload()));
		}
		listener.processNfcRecords(stringRecords.toArray(new String[] {}));
	}

	@Override
	public void enableNfcForegroundDispatch(Activity activity) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (nfcAdapter != null) {
			PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
			try {
				// handles all MIME based dispatches (TODO should specify only the ones that you need)
				ndef.addDataType("*/*");
			} catch (MalformedMimeTypeException e) {
				MyLog.w(TAG, "Error while constructing the NDEF filter!", e);
			}
			IntentFilter[] filters = new IntentFilter[] { ndef, };
			String[][] techListsArray = new String[][] { new String[] { NfcF.class.getName() } };
			nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techListsArray);
		}
	}

	@Override
	public void disableNfcForegroundDispatch(Activity activity) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (nfcAdapter != null) {
			nfcAdapter.disableForegroundDispatch(activity);
		}
	}

	@Override
	public void registerNfcCallback(Activity activity, final NfcListener listener, final String mimeType) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (nfcAdapter != null) {
			nfcAdapter.setNdefPushMessageCallback(new NfcAdapter.CreateNdefMessageCallback() {

				@Override
				public NdefMessage createNdefMessage(NfcEvent event) {
					MyLog.v(TAG, "createNdefMessage()");
					byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
					List<NdefRecord> ndefRecords = new ArrayList<NdefRecord>();
					// add records
					for (String record : listener.getNfcMimeMessages()) {
						ndefRecords.add(new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], record.getBytes()));
					}
					// add Android Application Record (AAR)
					ndefRecords.add(NdefRecord.createApplicationRecord(Constant.PKG));
					return new NdefMessage(ndefRecords.toArray(new NdefRecord[] {}));
				}

			}, activity);
		}
	}

	@Override
	public void setOnNdefPushCompleteCallback(final Activity activity, final NfcListener listener) {
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (nfcAdapter != null) {
			nfcAdapter.setOnNdefPushCompleteCallback(new NfcAdapter.OnNdefPushCompleteCallback() {

				@Override
				public void onNdefPushComplete(NfcEvent event) {
					MyLog.v(TAG, "onNdefPushComplete()");
					listener.onNfcPushComplete();
				}
			}, activity);
		}
	}

	@Override
	public int getNbClosestPOIDisplay() {
		return super.getNbClosestPOIDisplay();
	}

	private static final int NEW_APP_AVAILABLE_NOTIFICATION_ID = 20100602;

	private static final String STORE_APP = "market://details?id=";

	private static final String STORE_WWW = "https://play.google.com/store/apps/details?id=";

	private static final String NEW_APP_PACKAGE = "org.mtransit.android";

	private static final long MIGRATION_START_IN_MS = 1427947200000l; // April 2nd, 2015

	private static final String PREFS_LCL_LAST_NEW_APP_NOTIFICATION = "pLclLastNewAppNotification";

	private static final long MIN_DURATION_BETWEEN_NOTIFICATIONS_IN_MS = TimeUnit.DAYS.toMillis(7); // once a week

	@SuppressWarnings("deprecation")
	@Override
	public void showNewAppNotification(Context context) {
		try {
			if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				MyLog.d(TAG, "Not showing new app notification (not compatible)");
				return; // not compatible
			}
			if (Utils.isAppInstalled(context, NEW_APP_PACKAGE)) {
				MyLog.d(TAG, "Not showing new app notification (already installed)");
				return; // already installed
			}
			long nowInMs = System.currentTimeMillis();
			if (nowInMs < MIGRATION_START_IN_MS) {
				MyLog.d(TAG, "Not showing new app notification (too soon)");
				return; // too soon
			}
			long lastNotification = UserPreferences.getPrefLcl(context, PREFS_LCL_LAST_NEW_APP_NOTIFICATION, 0l);
			if (nowInMs - lastNotification < MIN_DURATION_BETWEEN_NOTIFICATIONS_IN_MS) {
				MyLog.d(TAG, "Not showing new app notification (not again, waiting)");
				return; // not again, waiting
			}
			NotificationCompat.Builder noficationBuilder = new NotificationCompat.Builder(context) //
					.setSmallIcon(android.R.drawable.stat_notify_sync) //
					.setContentTitle(context.getString(R.string.new_app_notification_title)) //
					.setContentText(context.getString(R.string.new_app_notification_text_short)) //
					.setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.new_app_notification_text_long))) //
					.setAutoCancel(true);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_APP + NEW_APP_PACKAGE)); // Google Play Store application
			if (intent.resolveActivity(context.getPackageManager()) == null) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_WWW + NEW_APP_PACKAGE)); // Google Play Store web site
			}
			noficationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NEW_APP_AVAILABLE_NOTIFICATION_ID, noficationBuilder.build());
			UserPreferences.savePrefLcl(context, PREFS_LCL_LAST_NEW_APP_NOTIFICATION, nowInMs);
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while showing new app notification!");
		}
	}
}

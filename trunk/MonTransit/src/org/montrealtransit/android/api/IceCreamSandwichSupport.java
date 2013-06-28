package org.montrealtransit.android.api;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.services.NfcListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Parcelable;

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
		return super.getNbClosestPOIDisplay(); // 0; // ClosestBikeStationsFinderTask.NO_LIMIT;
	}
}

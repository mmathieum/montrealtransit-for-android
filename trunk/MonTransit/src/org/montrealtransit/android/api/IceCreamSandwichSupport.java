package org.montrealtransit.android.api;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.services.NfcListener;

import android.app.Activity;
import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;

/**
 * Features available for Android 4.0 Ice Cream Sandwich (API Level 14) and higher.
 * @author Mathieu Méa
 */
public class IceCreamSandwichSupport extends HoneycombSupport {

	/**
	 * The log tag.
	 */
	private static final String TAG = IceCreamSandwichSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public IceCreamSandwichSupport(Context context) {
		super(context);
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

}

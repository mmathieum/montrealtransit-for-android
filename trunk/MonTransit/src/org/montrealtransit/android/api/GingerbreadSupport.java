package org.montrealtransit.android.api;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.services.NfcListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences.Editor;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Parcelable;

/**
 * Features available for Android 2.3 Gingerbread (API Level 9) and higher.
 * @author Mathieu Méa
 */
public class GingerbreadSupport extends FroyoSupport {

	/**
	 * The log tag.
	 */
	private static final String TAG = GingerbreadSupport.class.getSimpleName();

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public GingerbreadSupport(Context context) {
		super(context);
	}

	@Override
	public void applySharedPreferencesEditor(Editor editor) {
		editor.apply();
		backupManagerDataChanged();
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
}

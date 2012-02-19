package org.montrealtransit.android.api;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.services.NfcListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

/**
 * Features available for Android 2.3 Gingerbread (API Level 9) and higher.
 * @author Mathieu Méa
 */
public class GingerbreadSupport extends FroyoSupport {

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
}

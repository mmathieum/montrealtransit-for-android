package org.montrealtransit.android.services;

/**
 * The methods required by any activity with NFC capability.
 * @author Mathieu Méa
 */
public interface NfcListener {

	/**
	 * @return the MIME messages
	 */
	String[] getNfcMimeMessages();

	/**
	 * @param stringRecords process the string records from the NFC message
	 */
	void processNfcRecords(String[] stringRecords);

	/**
	 * Fired when the NFC push is completed.
	 */
	void onNfcPushComplete();
}

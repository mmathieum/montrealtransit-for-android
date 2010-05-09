package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.BusStopHours;
import org.xml.sax.SAXException;

/**
 * This class handle m.stm.info page parsing.
 * @author Mathieu Méa
 */
public class StmMobileHandler extends AbstractXHTMLHandler {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmMobileHandler.class.getSimpleName();

	/**
	 * The bus stop hours.
	 */
	private BusStopHours hours = new BusStopHours(StmMobileTask.SOURCE_NAME);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startDocument() throws SAXException {
		hours.clear();
		super.startDocument();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String string = new String(ch, start, length).trim();
		if (string.length() > 0) {
			//MyLog.v(TAG, "" + getTagTable() + ">" + string/* +"("+lineNumber+")." */);
			if (isInInterestedArea()) {
				// check if it's an hour
				try {
					Integer.valueOf(string.trim().substring(0,2));
					// considering 00h00 the standard (instead of the 00:00 provided by m.stm.info in English)
					string = string.replaceAll(":", "h");
					hours.addSHour(string);
					MyLog.v(TAG, "new hour:"+string+".");
				} catch (NumberFormatException nfe) {
					// it's not an hour!
					MyLog.d(TAG,"'"+string+"' is not an hour.");
				}
			}
			if (isInStmErrorMessage1Area()) {
				hours.addMessageString(string);
				MyLog.d(TAG, "message1:"+string);
			} else if (isInStmErrorMessage2Area()) {
				hours.addMessage2String(string);
				MyLog.d(TAG, "message2:"+string);
			} else if (isInStmErrorMessage3Area()) {
				hours.addMessageString(string);
				MyLog.d(TAG, "message3:"+string);
			}
		}
		super.characters(ch, start, length);
	}

	/**
	 * @return true is the parser is currently in the part of document containing the hours.
	 */
	private boolean isInInterestedArea() {
		return nb_div == 1 && id_div == 6 && nb_span == 1;
	}
	
	/**
	 * @return true if the parser is currently in the part of the document containing the first message.
	 */
	private boolean isInStmErrorMessage1Area(){
		return nb_div == 1 && id_div == 5 && nb_span == 1;
	}
	
	/**
	 * @return true if the parser is currently in the part of the document containing the second message.
	 */
	private boolean isInStmErrorMessage2Area(){
		return nb_div == 1 && id_div == 7 && nb_span == 1;
	}
	
	/**
	 * @return true if the parser is currently in the part of the document containing the m.stm.info service unavailable message.
	 */
	private boolean isInStmErrorMessage3Area(){
		return nb_div == 1 && id_div == 3 && nb_span == 1 && id_span == 2;
	}

	/**
	 * @return the hours.
	 */
	public BusStopHours getHours() {
		return hours;
	}
}
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
	private BusStopHours hours = new BusStopHours();

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
				hours.addSHour(string);
				MyLog.v(TAG, "new hour:"+string+".");
			}
		}
		super.characters(ch, start, length);
	}

	/**
	 * @return true is the parser is currently in the interesting part of the document.
	 */
	private boolean isInInterestedArea() {
		return nb_div == 1 && id_div == 6 && nb_span == 1;
	}

	/**
	 * @return the hours.
	 */
	public BusStopHours getHours() {
		return hours;
	}
}
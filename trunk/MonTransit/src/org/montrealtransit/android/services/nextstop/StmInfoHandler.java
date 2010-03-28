package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.data.BusStopHours;
import org.xml.sax.SAXException;

/**
 * This class handle stm.info page parsing.
 * @author Mathieu Méa
 */
public class StmInfoHandler extends AbstractXHTMLHandler {
	
	/**
	 * The log tag.
	 */
	private static final String TAG = StmInfoHandler.class.getSimpleName();
	
	/**
	 * The bus stop hours.
	 */
	private BusStopHours hours = new BusStopHours();
	/**
	 * The bus line number to match.
	 */
	private String lineNumber;
	/**
	 * Use to know if the parser is in the interesting area of the file.
	 */
	private boolean isIn = false;
	/**
	 * The id of the HTML table tag.
	 */
	private int int_id_table;
	/**
	 * The id of the HTML tr tag.
	 */
	private int int_id_tr;
	
	/**
	 * The default constructor.
	 * @param lineNumber the bus line number to match.
	 */
	public StmInfoHandler(String lineNumber) {
		this.lineNumber = lineNumber;
	}

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
		if (string.length()>0) {
			//MyLog.v(TAG, ""+getTagTable()+">"+string/*+"("+lineNumber+")."*/);
			if (string.equalsIgnoreCase(lineNumber)) {
				isIn = true;
				this.int_id_table = id_table;
				this.int_id_tr = id_tr;
			}
			if (isIn & isInInterestedArea()) {
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
		return nb_table==1&&id_table==int_id_table && nb_tr==1&&id_tr==int_id_tr && nb_td==1 && nb_b==0;
	}
	
	/**
	 * @return the hours.
	 */
	public BusStopHours getHours() {
		return hours;
	}
}
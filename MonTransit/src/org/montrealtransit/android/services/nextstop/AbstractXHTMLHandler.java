package org.montrealtransit.android.services.nextstop;

import org.montrealtransit.android.MyLog;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The abstract default handler for XHTML file.
 * @author Mathieu Méa
 */
public abstract class AbstractXHTMLHandler extends DefaultHandler implements ContentHandler {

	/**
	 * The log tag.
	 */
	private static final String TAG = AbstractXHTMLHandler.class.getSimpleName();

	/**
	 * The HTML tags.
	 */
	protected static final String HTML = "html";
	protected static final String BODY = "body";
	protected static final String DIV = "div";
	protected static final String CENTER = "center";
	protected static final String TABLE = "table";
	protected static final String TR = "tr";
	protected static final String TD = "td";
	protected static final String UL = "ul";
	protected static final String LI = "li";
	protected static final String A = "a";
	protected static final String B = "b";
	protected static final String IMG = "img";

	/**
	 * The HTML NUMBER & ID value.
	 */
	protected int nb_html = 0;
	protected int id_html = 0;
	protected int nb_body = 0;
	protected int id_body = 0;
	protected int nb_div = 0;
	protected int id_div = 0;
	protected int nb_center = 0;
	protected int id_center = 0;
	protected int nb_table = 0;
	protected int id_table = 0;
	protected int nb_tr = 0;
	protected int id_tr = 0;
	protected int nb_td = 0;
	protected int id_td = 0;
	protected int nb_ul = 0;
	protected int id_ul = 0;
	protected int nb_li = 0;
	protected int id_li = 0;
	protected int nb_a = 0;
	protected int id_a = 0;
	protected int nb_b = 0;
	protected int id_b = 0;
	protected int nb_img = 0;
	protected int id_img = 0;

	protected int pIndex = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
		pIndex++;
		if (isTag(HTML, localName)) {
			nb_html++;
			id_html++;
		} else if (isTag(BODY, localName)) {
			nb_body++;
			id_body++;
		} else if (isTag(DIV, localName)) {
			nb_div++;
			id_div++;
		} else if (isTag(CENTER, localName)) {
			nb_center++;
			id_center++;
		} else if (isTag(TABLE, localName)) {
			nb_table++;
			id_table++;
		} else if (isTag(TR, localName)) {
			nb_tr++;
			id_tr++;
		} else if (isTag(TD, localName)) {
			nb_td++;
			id_td++;
		} else if (isTag(UL, localName)) {
			nb_ul++;
			id_ul++;
		} else if (isTag(LI, localName)) {
			nb_li++;
			id_li++;
		} else if (isTag(A, localName)) {
			nb_a++;
			id_a++;
		} else if (isTag(B, localName)) {
			nb_b++;
			id_b++;
		} else if (isTag(IMG, localName)) {
			nb_img++;
			id_img++;
		} else {
			MyLog.d(TAG, "localName+" + localName + ".");
		}
	}

	/**
	 * @return a string representing the current parser status.
	 */
	protected String getTagTable() {
		String result = "";
		result += "[";
		result += HTML + ":" + nb_html + "." + id_html;
		result += "|";
		result += BODY + ":" + nb_body + "." + id_body;
		result += "|";
		result += DIV + ":" + nb_div + "." + id_div;
		result += "|";
		result += CENTER + ":" + nb_center + "." + id_center;
		result += "|";
		result += TABLE + ":" + nb_table + "." + id_table;
		result += "|";
		result += TR + ":" + nb_tr + "." + id_tr;
		result += "|";
		result += TD + ":" + nb_td + "." + id_td;
		result += "|";
		result += UL + ":" + nb_ul + "." + id_ul;
		result += "|";
		result += LI + ":" + nb_li + "." + id_li;
		result += "|";
		result += A + ":" + nb_a + "." + id_a;
		result += "|";
		result += B + ":" + nb_b + "." + id_b;
		result += "|";
		result += IMG + ":" + nb_img + "." + id_img;
		result += "]";
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		pIndex--;
		if (isTag(HTML, localName)) {
			nb_html--;
		} else if (isTag(BODY, localName)) {
			nb_body--;
		} else if (isTag(DIV, localName)) {
			nb_div--;
		} else if (isTag(CENTER, localName)) {
			nb_center--;
		} else if (isTag(TABLE, localName)) {
			nb_table--;
		} else if (isTag(TR, localName)) {
			nb_tr--;
		} else if (isTag(TD, localName)) {
			nb_td--;
		} else if (isTag(UL, localName)) {
			nb_ul--;
		} else if (isTag(LI, localName)) {
			nb_li--;
		} else if (isTag(A, localName)) {
			nb_a--;
		} else if (isTag(B, localName)) {
			nb_b--;
		} else if (isTag(IMG, localName)) {
			nb_img--;
		} else {
			MyLog.d(TAG, "localName-" + localName + ".");
		}
	}

	/**
	 * @param tag the HTML tag
	 * @param localName the local name
	 * @return true if this the same tag.
	 */
	protected boolean isTag(String tag, String localName) {
		return localName.equalsIgnoreCase(tag);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startDocument() throws SAXException {
		MyLog.v(TAG, "startDocument()");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endDocument() throws SAXException {
		MyLog.v(TAG, "endDocument()");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void error(SAXParseException exception) throws SAXException {
		MyLog.v(TAG, "error()");
		exception.printStackTrace();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		MyLog.v(TAG, "fatalError()");
		exception.printStackTrace();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		MyLog.v(TAG, "warning()");
		exception.printStackTrace();
	}
}

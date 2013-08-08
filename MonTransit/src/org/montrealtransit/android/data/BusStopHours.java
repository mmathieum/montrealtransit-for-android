package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.content.Context;

/**
 * Represent the bus stops hours.
 * @author Mathieu Méa
 */
public class BusStopHours {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusStopHours.class.getSimpleName();

	/**
	 * The source name to display.
	 */
	private String sourceName;
	/**
	 * The bus stop hours.
	 */
	private List<String> sHours;
	// TODO use a list of message
	/**
	 * The first message from the STM.
	 */
	private String message = "";
	/**
	 * The second message from the STM.
	 */
	private String message2 = "";
	/**
	 * The error message.
	 */
	private String error = null;

	/**
	 * The private constructor.
	 */
	private BusStopHours() {
	}

	/**
	 * The default constructor.
	 * @param sourceName the source name.
	 */
	public BusStopHours(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * An other constructor to specify if there was an error.
	 * @param sourceName the source name
	 * @param error the error message (<b>null</b> if no error)
	 */
	public BusStopHours(String sourceName, String error) {
		this.sourceName = sourceName;
		this.error = error;
	}

	public BusStopHours(String sourceName, String message, String message2) {
		this.sourceName = sourceName;
		this.message = message;
		this.message2 = message2;
	}

	/**
	 * @return the bus stop hours
	 */
	public List<String> getSHours() {
		if (this.sHours == null) {
			this.sHours = new ArrayList<String>();
		}
		return this.sHours;
	}

	/**
	 * Clear the bus stop hours list.
	 */
	public void clear() {
		this.message = "";
		this.message2 = "";
		this.getSHours().clear();
	}

	/**
	 * Add an hour to the list.
	 * @param newHour the new hour.
	 */
	public void addSHour(String newHour) {
		if (!getSHours().contains(newHour)) {
			this.getSHours().add(newHour);
		}
	}

	/**
	 * @param context the context on which to format the hours
	 * @return the formatted hours.
	 */
	public List<String> getFormattedHours(Context context) {
		List<String> result = new ArrayList<String>();
		for (String shour : this.getSHours()) {
			result.add(Utils.formatHours(context, shour));
		}
		return result;
	}

	/**
	 * Add the string to the message 1.
	 * @param string the string to add
	 */
	public void addMessageString(String string) {
		this.message += string;
	}

	/**
	 * Add the string to the message 2.
	 * @param string the string to add.
	 */
	public void addMessage2String(String string) {
		this.message2 += string;
	}

	private static final String SOURCE = "source";
	private static final String HOURS = "hours";
	private static final String MESSAGE = "message";
	private static final String MESSAGE2 = "message2";
	private static final String ERROR = "error";

	/**
	 * @return the serialized version of this object
	 */
	public String serialized() {
		MyLog.v(TAG, "serialized()");
		String result = "";
		result += "${" + SOURCE + ":" + getSourceName() + "},";
		for (String sHour : this.getSHours()) {
			result += "${" + HOURS + ":" + sHour + "},";
		}
		result += "${" + MESSAGE + ":" + getMessage() + "},";
		result += "${" + MESSAGE2 + ":" + getMessage2() + "},";
		result += "${" + ERROR + ":" + getError() + "}";
		return result;
	}

	/**
	 * The regex use to match the serialized object properties.
	 */
	private static final Pattern PATTERN_REGEX_FOR_PROPERTIES = Pattern.compile("\\$\\{[^:]*:[^}]*\\}*");

	/**
	 * @param serializedHours the serialized hour object
	 * @return the hour the hour object
	 */
	public static BusStopHours deserialized(String serializedHours) {
		MyLog.v(TAG, "deserialized(%s)", serializedHours);
		BusStopHours result = new BusStopHours();
		Matcher matcher = PATTERN_REGEX_FOR_PROPERTIES.matcher(serializedHours);
		while (matcher.find()) {
			int separator = matcher.group().indexOf(":");
			String property = matcher.group().substring(2, separator);
			String value = matcher.group().substring(separator + 1, matcher.group().length() - 1);
			if (SOURCE.equals(property)) {
				result.setSourceName(value);
			} else if (HOURS.equals(property)) {
				result.addSHour(value);
			} else if (MESSAGE.equals(property)) {
				result.setMessage(String.valueOf(value));
			} else if (MESSAGE2.equals(property)) {
				result.setMessage2(String.valueOf(value));
			} else if (ERROR.equals(property)) {
				result.setError(String.valueOf(value));
			}
		}
		return result;
	}

	/**
	 * @return the message 1.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the new message
	 */
	private void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the message 2.
	 */
	public String getMessage2() {
		return message2;
	}

	/**
	 * @param message2 the new message2
	 */
	private void setMessage2(String message2) {
		this.message2 = message2;
	}

	/**
	 * @return the source name.
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * @param sourceName the new source name
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * @return the error message
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the new error message
	 */
	public void setError(String error) {
		this.error = error;
	}
}

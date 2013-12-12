package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.content.Context;
import android.text.TextUtils;

/**
 * Represent the stops times.
 * @author Mathieu Méa
 */
public class StopTimes {

	/**
	 * The log tag.
	 */
	private static final String TAG = StopTimes.class.getSimpleName();

	/**
	 * The source name to display.
	 */
	private String sourceName;
	/**
	 * The previous time if any.
	 */
	private String previousTime;
	/**
	 * The stop times.
	 */
	private List<String> sTimes;
	// TODO use a list of message
	/**
	 * The first message from the STM.
	 */
	private String message;
	/**
	 * The second message from the STM.
	 */
	private String message2;
	/**
	 * The error message.
	 */
	private String error;

	/**
	 * The private constructor.
	 */
	private StopTimes() {
	}

	/**
	 * The default constructor.
	 * @param sourceName the source name.
	 */
	public StopTimes(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * An other constructor to specify if there was an error.
	 * @param sourceName the source name
	 * @param error the error message (<b>null</b> if no error)
	 */
	public StopTimes(String sourceName, String error) {
		this.sourceName = sourceName;
		this.error = error;
	}

	public StopTimes(String sourceName, String message, String message2) {
		this.sourceName = sourceName;
		this.message = message;
		this.message2 = message2;
	}

	/**
	 * @return the stop times
	 */
	public List<String> getSTimes() {
		if (this.sTimes == null) {
			this.sTimes = new ArrayList<String>();
		}
		return this.sTimes;
	}

	public void setPreviousTime(String previousTime) {
		this.previousTime = previousTime;
	}

	public String getPreviousTime() {
		return previousTime;
	}

	public boolean hasPreviousTime() {
		return !TextUtils.isEmpty(this.previousTime);
	}

	/**
	 * Add an time to the list.
	 * @param newTime the new time.
	 */
	public void addSTime(String newTime) {
		if (!getSTimes().contains(newTime)) {
			this.getSTimes().add(newTime);
		}
	}

	/**
	 * @param context the context on which to format the times
	 * @return the formatted times.
	 */
	public List<String> getFormattedTimes(Context context) {
		List<String> result = new ArrayList<String>();
		for (String stime : this.getSTimes()) {
			result.add(Utils.formatTimes(context, stime));
		}
		return result;
	}

	/**
	 * Add the string to the message 1.
	 * @param string the string to add
	 */
	public void addMessageString(String string) {
		if (this.message == null) {
			this.message = "";
		}
		this.message += string;
	}

	/**
	 * Add the string to the message 2.
	 * @param string the string to add.
	 */
	public void addMessage2String(String string) {
		if (this.message2 == null) {
			this.message2 = "";
		}
		this.message2 += string;
	}

	private static final String SOURCE = "source";
	private static final String TIMES = "hours";
	private static final String MESSAGE = "message";
	private static final String MESSAGE2 = "message2";
	private static final String ERROR = "error";
	private static final String PREVIOUS_TIME = "previous";

	/**
	 * @return the serialized version of this object
	 */
	public String serialized() {
		MyLog.v(TAG, "serialized()");
		String result = "";
		result += "${" + SOURCE + ":" + getSourceName() + "},";
		for (String sTime : this.getSTimes()) {
			result += "${" + TIMES + ":" + sTime + "},";
		}
		if (!TextUtils.isEmpty(getPreviousTime())) {
			result += "${" + PREVIOUS_TIME + ":" + getPreviousTime() + "}";
		}
		if (!TextUtils.isEmpty(getMessage())) {
			result += "${" + MESSAGE + ":" + getMessage() + "},";
		}
		if (!TextUtils.isEmpty(getMessage2())) {
			result += "${" + MESSAGE2 + ":" + getMessage2() + "},";
		}
		if (!TextUtils.isEmpty(getError())) {
			result += "${" + ERROR + ":" + getError() + "}";
		}
		return result;
	}

	/**
	 * The regex use to match the serialized object properties.
	 */
	private static final Pattern PATTERN_REGEX_FOR_PROPERTIES = Pattern.compile("\\$\\{[^:]*:[^}]*\\}*");

	/**
	 * @param serializedTimes the serialized time object
	 * @return the time the time object
	 */
	public static StopTimes deserialized(String serializedTimes) {
		MyLog.v(TAG, "deserialized(%s)", serializedTimes);
		StopTimes result = new StopTimes();
		Matcher matcher = PATTERN_REGEX_FOR_PROPERTIES.matcher(serializedTimes);
		while (matcher.find()) {
			int separator = matcher.group().indexOf(":");
			String property = matcher.group().substring(2, separator);
			String value = matcher.group().substring(separator + 1, matcher.group().length() - 1);
			if (SOURCE.equals(property)) {
				result.setSourceName(value);
			} else if (PREVIOUS_TIME.equals(property)) {
				result.setPreviousTime(value);
			} else if (TIMES.equals(property)) {
				result.addSTime(value);
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(StopTimes.class.getSimpleName()).append(':').append('{');
		sb.append(SOURCE).append(':').append(this.sourceName).append(',');
		sb.append(PREVIOUS_TIME).append(':').append(this.previousTime).append(',');
		sb.append(TIMES).append(':').append('[');
		for (String sTime : getSTimes()) {
			sb.append(sTime).append(',');
		}
		sb.append(']').append(',');
		sb.append(MESSAGE).append(':').append(this.message).append(',');
		sb.append(MESSAGE2).append(':').append(this.message2);
		sb.append('}');
		return sb.toString();
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

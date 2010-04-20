package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.Utils;

import android.content.Context;

/**
 * Represent the bus stops hours.
 * @author Mathieu Méa
 */
public class BusStopHours {
	
	/**
	 * The bus stop hours.
	 */
	private List<String> sHours;
	/**
	 * The first message from the STM.
	 */
	private String message;
	/**
	 * The second message from the STM.
	 */
	private String message2;

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
		this.getSHours().add(newHour);
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
	
	/**
	 * @return the message 1.
	 */
	public String getMessage() {
	    return message;
    }
	
	/**
	 * @return the message 2.
	 */
	public String getMessage2(){
		return message2;
	}
}

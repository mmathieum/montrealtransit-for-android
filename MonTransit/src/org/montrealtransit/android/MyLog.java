package org.montrealtransit.android;

import android.content.Context;
import android.util.Log;

/**
 * This class is used to customize and optimize the log.
 * @author Mathieu Méa
 */
public class MyLog {

	/**
	 * Use this boolean to enable full LOG (VERBOSE)
	 */
	private static boolean DEBUG = false;
	
	/**
	 * Use this to enable location display.
	 */
	public static final boolean SHOW_LOCATION = false;

	/**
	 * @see Log#v(String, String)
	 * @param tag the class tag
	 * @param msg the message
	 */
	public static void v(String tag, String msg) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.VERBOSE)) {
			Log.v(Constant.MAIN_TAG, tag + ">" + msg);
		}
	}

	/**
	 * @see MyLog#v(String, String)
	 * @param the class tag
	 * @param msg the message
	 * @param context the context
	 * @param show true to show the log with a toast
	 */
	public static void v(String tag, String msg, Context context, boolean show) {
	   if (DEBUG && show) {
		   Utils.notifyTheUser(context, msg);
	   }
	   v(tag,msg);
    }

	/**
	 * @see Log#w(String, String)
	 * @param tag the class tag
	 * @param msg the message
	 */
	public static void w(String tag, String msg) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.WARN)) {
			Log.w(Constant.MAIN_TAG, tag + ">" + msg);
		}
	}

	/**
	 * @see Log#w(String, String, Throwable)
	 * @param tag the class tag
	 * @param msg the message
	 * @param e the error
	 */
	public static void w(String tag, String msg, Exception e) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.WARN)) {
			Log.w(Constant.MAIN_TAG, tag + ">" + msg, e);
		}
	}

	/**
	 * @see Log#d(String, String)
	 * @param tag the class tag
	 * @param msg the message
	 */
	public static void d(String tag, String msg) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.DEBUG)) {
			Log.d(Constant.MAIN_TAG, tag + ">" + msg);
		}
	}

	/**
	 * @see Log#e(String, String, Throwable)
	 * @param tag the class tag
	 * @param msg the message
	 * @param e the error
	 */
	public static void e(String tag, String msg, Exception e) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.ERROR)) {
			Log.e(Constant.MAIN_TAG, tag + ">" + msg, e);
		}
	}

	/**
	 * @see Log#i(String, String)
	 * @param tag the class log
	 * @param msg the message
	 */
	public static void i(String tag, String msg) {
		if (DEBUG || Log.isLoggable(Constant.MAIN_TAG, Log.INFO)) {
			Log.i(Constant.MAIN_TAG, tag + ">" + msg);
		}
	}
}

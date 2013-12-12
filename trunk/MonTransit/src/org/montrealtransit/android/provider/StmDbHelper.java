package org.montrealtransit.android.provider;

import java.util.Arrays;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This STM Subway database helper.
 * @author Mathieu Méa
 */
@Deprecated
public class StmDbHelper extends SQLiteOpenHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmDbHelper.class.getSimpleName();

	/**
	 * The database file name.
	 */
	public static final String DB_NAME = "stm.db";

	/**
	 * The database version use to manage database changes.
	 */
	public static final int DB_VERSION = 26;

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public StmDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmDbHelper(%s, %s)", DB_NAME, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		MyLog.v(TAG, "onCreate()");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		MyLog.v(TAG, "onUpgrade(%s, %s)", oldVersion, newVersion);
		MyLog.d(TAG, "Upgrading database from version %s to %s.", oldVersion, newVersion);
		switch (oldVersion) {
		default:
			MyLog.d(TAG, "Old data destroyed!");
			db.execSQL("DROP TABLE IF EXISTS lignes_autobus");
			db.execSQL("DROP TABLE IF EXISTS directions_autobus");
			db.execSQL("DROP TABLE IF EXISTS arrets_autobus");
			db.execSQL("DROP TABLE IF EXISTS arrets_autobus_loc");
			db.execSQL("DROP TABLE IF EXISTS frequences_metro");
			db.execSQL("DROP TABLE IF EXISTS horaire_metro");
			db.execSQL("DROP TABLE IF EXISTS lignes_metro");
			db.execSQL("DROP TABLE IF EXISTS directions_metro");
			db.execSQL("DROP TABLE IF EXISTS stations_metro");
			break;
		}
	}

	/**
	 * Check if the database already exist.
	 * @param context the context
	 * @return true if the DB exists, false if it doesn't
	 */
	public static boolean isDbExist(Context context) {
		return Arrays.asList(context.databaseList()).contains(DB_NAME);
	}

	/**
	 * @return current DB version w/o creating/upgrading or -1
	 */
	public static int getCurrentDbVersion(Context context) {
		MyLog.v(TAG, "getCurrentDbVersion()");
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);
			return db.getVersion();
		} catch (Throwable t) {
			MyLog.w(TAG, t, "Error while reading current DB version!");
			return -1;
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	@Override
	public synchronized void close() {
		try {
			super.close();
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while closing the databases!");
		}
	}

}

package org.montrealtransit.android.provider;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * This database helper is used to access the Bixi data.
 * @author Mathieu Méa
 */
public class BixiDbHelper extends SQLiteOpenHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = BixiDbHelper.class.getSimpleName();
	/**
	 * The database file.
	 */
	private static final String DATABASE_NAME = "bixi.db";
	/**
	 * The database version use to manage database changes.
	 */
	public static final int DATABASE_VERSION = 1;

	/**
	 * The bike stations table.
	 */
	public static final String T_BIKE_STATIONS = "bike_stations";
	/**
	 * The bike stations ID field.
	 */
	public static final String T_BIKE_STATIONS_K_ID = BaseColumns._ID;
	/**
	 * The bike stations name field.
	 */
	public static final String T_BIKE_STATIONS_K_NAME = "name";
	/**
	 * The bike stations terminal name field.
	 */
	public static final String T_BIKE_STATIONS_K_TERMINAL_NAME = "terminal_name";
	/**
	 * The bike stations latitude field.
	 */
	public static final String T_BIKE_STATIONS_K_LAT = "lat";
	/**
	 * The bike stations longitude field.
	 */
	public static final String T_BIKE_STATIONS_K_LNG = "lng";
	/**
	 * The bike stations installed field (0 = false).
	 */
	public static final String T_BIKE_STATIONS_K_INSTALLED = "installed";
	/**
	 * The bike stations installed field (0 = false).
	 */
	public static final String T_BIKE_STATIONS_K_LOCKED = "locked";
	/**
	 * The bike stations install date.
	 */
	public static final String T_BIKE_STATIONS_K_INSTALL_DATE = "install_date";
	/**
	 * The bike stations removal date.
	 */
	public static final String T_BIKE_STATIONS_K_REMOVE_DATE = "removal_date";
	/**
	 * The bike stations temporary field (0 = false).
	 */
	public static final String T_BIKE_STATIONS_K_TEMPORARY = "temporary";
	/**
	 * The bike stations number of available bike.
	 */
	public static final String T_BIKE_STATIONS_K_NB_BIKES = "nb_bikes";
	/**
	 * The bike stations number of empty docks.
	 */
	public static final String T_BIKE_STATIONS_K_NB_EMPTY_DOCKS = "nb_empty_docks";
	/**
	 * The bike stations latest update time.
	 */
	public static final String T_BIKE_STATIONS_K_LATEST_UPDATE_TIME = "latest_update_time";

	/**
	 * Database creation SQL statement for the bike stations table.
	 */
	private static final String DATABASE_CREATE_T_BIKE_STATIONS = "create table " + T_BIKE_STATIONS + " (" + T_BIKE_STATIONS_K_ID
			+ " integer primary key autoincrement, " + T_BIKE_STATIONS_K_NAME + " text, " + T_BIKE_STATIONS_K_TERMINAL_NAME + " text, " + T_BIKE_STATIONS_K_LAT
			+ " real, " + T_BIKE_STATIONS_K_LNG + " real, " + T_BIKE_STATIONS_K_INSTALLED + " integer, " + T_BIKE_STATIONS_K_LOCKED + " integer, "
			+ T_BIKE_STATIONS_K_INSTALL_DATE + " integer, " + T_BIKE_STATIONS_K_REMOVE_DATE + " integer, " + T_BIKE_STATIONS_K_TEMPORARY + " integer, "
			+ T_BIKE_STATIONS_K_NB_BIKES + " integer, " + T_BIKE_STATIONS_K_NB_EMPTY_DOCKS + " integer, " + T_BIKE_STATIONS_K_LATEST_UPDATE_TIME + " integer);";

	/**
	 * Default constructor.
	 */
	public BixiDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		MyLog.v(TAG, "BixiDbHelper()");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		MyLog.v(TAG, "onCreate()");
		db.execSQL(DATABASE_CREATE_T_BIKE_STATIONS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		MyLog.v(TAG, "onUpgrade(%s, %s)", oldVersion, newVersion);
		MyLog.d(TAG, "Upgrading database from version %s to %s, which may destroy all old data!", oldVersion, newVersion);
		switch (oldVersion) {
		// case 1:
		// MyLog.v(TAG, "add the X table");
		// // just create the X table
		// db.execSQL(DATABASE_CREATE_T_X);
		// case 2:
		// MyLog.v(TAG, "add the Y table");
		// // just create the Y table
		// db.execSQL(DATABASE_CREATE_T_Y);
		// break;
		default:
			MyLog.w(TAG, "Old user data destroyed!");
			db.execSQL("DROP TABLE IF EXISTS " + T_BIKE_STATIONS);
			onCreate(db);
			break;
		}
	}

	@Override
	public void close() {
		MyLog.v(TAG, "close()");
		super.close();
	}

}

package org.montrealtransit.android.provider;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This database helper is used to access the user data.
 * @author Mathieu Méa
 */
public class DataDbHelper extends SQLiteOpenHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = DataDbHelper.class.getSimpleName();
	/**
	 * The SQLite database.
	 */
	private SQLiteDatabase myDataBase;
	/**
	 * The database file.
	 */
	private static final String DATABASE_NAME = "data.db";
	/**
	 * The database version use to manage database changes.
	 */
	private static final int DATABASE_VERSION = 2;

	/**
	 * The favorites table.
	 */
	public static final String T_FAVS = "favs";
	/**
	 * The favorite ID field.
	 */
	public static final String T_FAVS_K_ID = "_id";
	/**
	 * The favorite FK_ID field.
	 */
	public static final String T_FAVS_K_FK_ID = "fk_id";
	/**
	 * The favorite FK_ID2 field.
	 */
	public static final String T_FAVS_K_FK_ID_2 = "fk_id2";
	/**
	 * The favorite title field.
	 */
	public static final String T_FAVS_K_TITLE = "title";
	/**
	 * The favorite type field.
	 */
	public static final String T_FAVS_K_TYPE = "type";

	/**
	 * The favorite type value for bus stops.
	 */
	public static final int KEY_TYPE_VALUE_BUS_STOP = 1;

	/**
	 * The history table.
	 */
	public static final String T_HISTORY = "history";
	/**
	 * The history ID field.
	 */
	public static final String T_HISTORY_K_ID = "_id";
	/**
	 * The history value field.
	 */
	public static final String T_HISTORY_K_VALUE = "value";

	/**
	 * Constant for opening writable database.
	 */
	//public static final String OPEN_WRITABLE = "W";
	/**
	 * Constant for opening read only database.
	 */
	//public static final String OPEN_READABLE = "R";

	/**
	 * Database creation SQL statement for the favorite table.
	 */
	private static final String DATABASE_CREATE_T_FAVS = "create table " + T_FAVS + " (" + T_FAVS_K_ID + " integer primary key autoincrement, " + T_FAVS_K_TYPE
	        + " integer, " + T_FAVS_K_FK_ID + " text, " + T_FAVS_K_FK_ID_2 + " text, " + T_FAVS_K_TITLE + " text);";
	/**
	 * Database creation SQL statement for the history table.
	 */
	private static final String DATABASE_CREATE_T_HISTORY = "create table " + T_HISTORY + " (" + T_HISTORY_K_ID + " integer primary key autoincrement, "
	        + T_HISTORY_K_VALUE + " text);";

	public DataDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE_T_FAVS);
		db.execSQL(DATABASE_CREATE_T_HISTORY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		MyLog.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which may destroy all old data");
		if (oldVersion == 1 && newVersion == 2) {
			MyLog.v(TAG, "old data not destroyed, just create the history table");
			// just create the history database
			db.execSQL(DATABASE_CREATE_T_HISTORY);
		} else {
			MyLog.v(TAG, "old data destroyed");
			db.execSQL("DROP TABLE IF EXISTS " + T_FAVS);
			db.execSQL("DROP TABLE IF EXISTS " + T_HISTORY);
			onCreate(db);
		}
	}

	/*public synchronized void open(String mode) {
		MyTrace.v(TAG, "open()");
		try {
			if (mode.equals(DataDbHelper.OPEN_WRITABLE)) {
				myDataBase = this.getWritableDatabase();
			} else if (mode.equals(DataDbHelper.OPEN_READABLE)) {
				myDataBase = this.getReadableDatabase();
			}
		} catch (NullPointerException npe) {
			MyTrace.e(TAG, "Null Pointer Exception", npe);
		} catch (SQLException sqle) {
			MyTrace.e(TAG, "SQL Exception", sqle);
			throw sqle;
		}
	}*/

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		MyLog.v(TAG, "close()");
		if (myDataBase != null) {
			myDataBase.close();
		}
		super.close();
	}
}

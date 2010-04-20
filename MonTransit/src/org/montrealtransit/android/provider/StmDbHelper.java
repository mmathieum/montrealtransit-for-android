package org.montrealtransit.android.provider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This SQLite database helper is used to access the STM database.
 * @author Mathieu Méa
 */
public class StmDbHelper extends SQLiteOpenHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = StmDbHelper.class.getSimpleName();

	/**
	 * The path of the database file.
	 */
	private static String DB_PATH = "/data/data/" + Constant.PKG + "/databases/";

	/**
	 * The database file name.
	 */
	private static String DB_NAME = "stm.db";
	
	/**
	 * The database version use to manage database changes.
	 */
	private static final int DB_VERSION = 2;

	/**
	 * The database object.
	 */
	private SQLiteDatabase myDataBase;

	// BUS LINE
	public static final String T_BUS_LINES = "lignes_autobus";
	public static final String T_BUS_LINES_K_NUMBER = "_id";
	public static final String T_BUS_LINES_K_NAME = "name";
	public static final String T_BUS_LINES_K_HOURS = "schedule";
	public static final String T_BUS_LINES_K_TYPE = "type";

	public static final String BUS_LINE_TYPE_REGULAR_SERVICE = "J";
	public static final String BUS_LINE_TYPE_RUSH_HOUR_SERVICE = "P";
	public static final String BUS_LINE_TYPE_METROBUS_SERVICE = "M";
	public static final String BUS_LINE_TYPE_TRAINBUS = "T";
	public static final String BUS_LINE_TYPE_NIGHT_SERVICE = "N";
	public static final String BUS_LINE_TYPE_EXPRESS_SERVICE = "E";
	public static final String BUS_LINE_TYPE_RESERVED_LANE_SERVICE = "R";

	// BUS LINE DIRECTIONS
	public static final String T_BUS_LINE_DIRECTIONS = "directions_autobus";
	public static final String T_BUS_LINE_DIRECTIONS_K_ID = "direction_id";
	public static final String T_BUS_LINE_DIRECTIONS_K_LINE_ID = "ligne_id";
	public static final String T_BUS_LINE_DIRECTIONS_K_NAME = "name";

	// BUS STOP
	public static final String T_BUS_STOPS = "arrets_autobus";
	public static final String T_BUS_STOPS_K_CODE = "_id";
	public static final String T_BUS_STOPS_K_PLACE = "lieu";
	public static final String T_BUS_STOPS_K_LINE_NUMBER = "ligne_id";
	public static final String T_BUS_STOPS_K_DIRECTION_ID = "direction_id";
	public static final String T_BUS_STOPS_K_SUBWAY_STATION_ID = "station_id";
	public static final String T_BUS_STOPS_K_STOPS_ORDER = "arret_order";
	public static final String T_BUS_STOPS_A_SIMPLE_DIRECTION_ID = "simple_direction_id";

	// SUBWAY LINE
	public static final String T_SUBWAY_LINES = "lignes_metro";
	public static final String T_SUBWAY_LINES_K_NUMBER = "_id";
	public static final String T_SUBWAY_LINES_K_NAME = "name";

	// SUBWAY FREQUENCES
	public static final String T_SUBWAY_FREQUENCES = "frequences_metro";
	public static final String T_SUBWAY_FREQUENCES_K_DIRECTION = "direction";
	public static final String T_SUBWAY_FREQUENCES_K_HOUR = "heure";
	public static final String T_SUBWAY_FREQUENCES_K_FREQUENCE = "frequence";
	public static final String T_SUBWAY_FREQUENCES_K_DAY = "day";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_WEEK = "";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_SUNDAY = "d";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_SATURDAY = "s";

	// SUBWAY DIRECTIONS
	public static final String T_SUBWAY_DIRECTIONS = "directions_metro";
	public static final String T_SUBWAY_DIRECTIONS_K_SUBWAY_LINE_ID = "ligne_id";
	public static final String T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ORDER = "station_order";
	public static final String T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ID = "station_id";

	public static final String SUBWAY_DIRECTION_ID = "DIRECTION_ID";
	public static final String SUBWAY_DIRECTION_1 = "ASC";
	public static final String SUBWAY_DIRECTION_2 = "DESC";

	// SUBWAY HOURS
	public static final String T_SUBWAY_HOUR = "horaire_metro";
	public static final String T_SUBWAY_HOUR_K_STATION_ID = "station_id";
	public static final String T_SUBWAY_HOUR_K_DIRECTION_ID = "direction_id";
	public static final String T_SUBWAY_HOUR_K_HOUR = "heure";
	public static final String T_SUBWAY_HOUR_K_FIRST_LAST = "premier_dernier";
	public static final String T_SUBWAY_HOUR_K_FIRST = "premier";
	public static final String T_SUBWAY_HOUR_K_LAST = "dernier";
	public static final String T_SUBWAY_HOUR_K_DAY = "day";
	public static final String T_SUBWAY_HOUR_K_DAY_WEEK = "";
	public static final String T_SUBWAY_HOUR_K_DAY_SUNDAY = "d";
	public static final String T_SUBWAY_HOUR_K_DAY_SATURDAY = "s";

	// SUBWAY STATIONS
	public static final String T_SUBWAY_STATIONS = "stations_metro";
	public static final String T_SUBWAY_STATIONS_K_STATION_ID = "_id";
	public static final String T_SUBWAY_STATIONS_K_STATION_NAME = "name";
	public static final String T_SUBWAY_STATIONS_K_STATION_LAT = "lat";
	public static final String T_SUBWAY_STATIONS_K_STATION_LNG = "lng";

	/**
	 * Constructor takes and keeps a reference of the passed context in order to access to the application assets and resources.
	 */
	public StmDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmDbHelper("+DB_NAME+", "+DB_VERSION+")");
		createDbIfNecessary(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		//MyLog.v(TAG, "onCreate()");
		// DO NOTHING
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//MyLog.v(TAG, "onUpgrade("+oldVersion+", "+newVersion+")");
		// DO NOTHING
	}

	/**
	 * Create the database if necessary.
	 * @param context the context used to create the database
	 */
	private void createDbIfNecessary(Context context){
		MyLog.v(TAG, "createDbIfNecessary()");
		// IF DB doesn't exist DO
		if (!isDbExist()) {
			MyLog.d(TAG, "DB NOT EXIST");
			// By calling this method, an empty database will be created into the default system path of your application
			// so we are going to be able to overwrite that database with our database.
			try {
				this.getReadableDatabase();
			} catch (SQLiteException e) {
				MyLog.d(TAG,"SQLite exception while getReadableDatabase().");
			}
			try {
				MyLog.i(TAG, "Initialization of the STM database...");
				copyDataBase(context);
				MyLog.i(TAG, "Initialization of the STM database... OK");
			} catch (IOException e) {
				MyLog.e(TAG, "Error while copying database", e);
			}
		// IF DB exist DO
		} else {
			MyLog.d(TAG, "DB EXIST");
			// check version
			int currentVersion = getExistingDbVersion();
			if (currentVersion!= DB_VERSION) {
				MyLog.d(TAG, "VERSION DIFF");
				// upgrade
				if (currentVersion== 1 && DB_VERSION==2) {
					MyLog.d(TAG, "UPGRADING...");
					// close the db
					close();
					// remove the existing db
					if (context.deleteDatabase(DB_NAME)) {
						// copy the new one
						createDbIfNecessary(context);
						Utils.notifyTheUser(context, context.getResources().getString(R.string.update_stm_db_ok));
					} else {
						MyLog.w(TAG,"Can't delete the current database.");
						// notify the user that he need to remove and re-install the application
						Utils.notifyTheUserLong(context, context.getResources().getString(R.string.update_stm_db_error));
						Utils.notifyTheUserLong(context, context.getResources().getString(R.string.update_stm_db_error_next));
					}
				} else {
					MyLog.w(TAG,"Trying to upgrade the db from version '"+currentVersion+"' to version '"+DB_VERSION+"'.");
				}
			}
		}
	}
	
	private int getExistingDbVersion() {
		int result = 0;
		if (myDataBase!= null) {
			result = myDataBase.getVersion();
		} else {
			SQLiteDatabase checkDB = null;
			try {
				// open/close the database to check the version.
				checkDB = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
				result = checkDB.getVersion();
				checkDB.close();
			} catch (SQLiteException e) {
				MyLog.w(TAG, "Can't find the current DB version.");
			}
		}
		return result;
    }

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * @return true if it exists, false if it doesn't
	 */
	private boolean isDbExist() {
		SQLiteDatabase checkDB = null;
		try {
			// open/close the database, if an SQLite exception is raised, the DB doesn't exist.
			checkDB = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
			checkDB.close();
			return true;
		} catch (SQLiteException e) {
			MyLog.i(TAG, "The STM database doesn't exist yet.");
			return false;
		}
	}

	/**
	 * Copies the database from the local assets-folder to the just created empty database in the system folder, from where it can be accessed and handled.
	 * This is done by transferring byte stream.
	 * @param context the context use to open the input stream
	 * */
	private void copyDataBase(Context context) throws IOException {
		MyLog.v(TAG, "copyDataBase()");
		// Open your local DB as the input stream
		InputStream myInput = context.getAssets().open(DB_NAME);
		// Path to the just created empty DB
		String outFileName = DB_PATH + DB_NAME;
		// Open the empty DB as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);
		// transfer bytes from the input file to the output file
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}
		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}
	
	/**
	 * Open the database.
	 */
	public void open() {
		MyLog.v(TAG, "open()");
		try {
			myDataBase = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLException sqle) {
			MyLog.w(TAG, "Error while opening the database.", sqle);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void close() {
		if (myDataBase != null)
			myDataBase.close();
		super.close();
	}
}

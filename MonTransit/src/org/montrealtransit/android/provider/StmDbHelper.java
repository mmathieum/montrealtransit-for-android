package org.montrealtransit.android.provider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.montrealtransit.android.Constant;
import org.montrealtransit.android.MyLog;

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
		super(context, DB_NAME, null, 1);
		createDbIfNecessary(context);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// DO NOTHING
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// DO NOTHING
	}

	/**
	 * Open the database.
	 */
	public void open() {
		// MyTrace.v(TAG, "open()");
		try {
			myDataBase = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLException sqle) {
			MyLog.e(TAG, "Can't open the databse.", sqle);
		}
	}

	/**
	 * Create the database if necessary.
	 * @param context the context used to create the database
	 */
	private void createDbIfNecessary(Context context){
		MyLog.v(TAG, "createDbIfNecessary()");
		if (!isDbExist()) {
			// By calling this method, an empty database will be created into the default system path of your application
			// so we are going to be able to overwrite that database with our database.
			this.getReadableDatabase();
			try {
				MyLog.i(TAG, "Initialization of the STM database..");
				copyDataBase(context);
			} catch (IOException e) {
				MyLog.e(TAG, "Error while copying database", e);
			}
		}
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
		MyLog.v(TAG, "copyDataBase() ...");
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
}

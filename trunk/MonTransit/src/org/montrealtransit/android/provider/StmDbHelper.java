package org.montrealtransit.android.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.MainScreen.InitializationTask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

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
	 * The database file name.
	 */
	private static String DB_NAME = "stm.db";

	/**
	 * The database version use to manage database changes.
	 */
	private static final int DB_VERSION = 4;

	/**
	 * The list of SQL dump files.
	 */
	private static final int[] DUMP_FILES = new int[] { R.raw.stm_db_arrets_autobus_p1, R.raw.stm_db_arrets_autobus_p2,
	        R.raw.stm_db_directions_autobus, R.raw.stm_db_directions_metro, R.raw.stm_db_frequences_metro,
	        R.raw.stm_db_horaire_metro, R.raw.stm_db_lignes_autobus, R.raw.stm_db_lignes_metro,
	        R.raw.stm_db_stations_lignes, R.raw.stm_db_stations_metro };

	// BUS LINE
	public static final String T_BUS_LINES = "lignes_autobus";
	public static final String T_BUS_LINES_K_NUMBER = BaseColumns._ID;
	public static final String T_BUS_LINES_K_NAME = "name";
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
	public static final String T_BUS_STOPS_K_CODE = BaseColumns._ID;
	public static final String T_BUS_STOPS_K_PLACE = "lieu";
	public static final String T_BUS_STOPS_K_LINE_NUMBER = "ligne_id";
	public static final String T_BUS_STOPS_K_DIRECTION_ID = "direction_id";
	public static final String T_BUS_STOPS_K_SUBWAY_STATION_ID = "station_id";
	public static final String T_BUS_STOPS_K_STOPS_ORDER = "arret_order";
	public static final String T_BUS_STOPS_A_SIMPLE_DIRECTION_ID = "simple_direction_id";

	// SUBWAY LINE
	public static final String T_SUBWAY_LINES = "lignes_metro";
	public static final String T_SUBWAY_LINES_K_NUMBER = BaseColumns._ID;
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
	public static final String T_SUBWAY_STATIONS_K_STATION_ID = BaseColumns._ID;
	public static final String T_SUBWAY_STATIONS_K_STATION_NAME = "name";
	public static final String T_SUBWAY_STATIONS_K_STATION_LAT = "lat";
	public static final String T_SUBWAY_STATIONS_K_STATION_LNG = "lng";

	private boolean upgrade = false;

	/**
	 * The default constructor.
	 * @param context the context
	 * @param task the initialization task or <b>NULL</b>
	 */
	public StmDbHelper(Context context, InitializationTask task) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmDbHelper(%s, %s)", DB_NAME, DB_VERSION);
		createDbIfNecessary(context, task);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		MyLog.v(TAG, "onCreate()");
		// DO NOTHING
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		MyLog.v(TAG, "onUpgrade(%s, %s)", oldVersion, newVersion);
		this.upgrade = (oldVersion != newVersion);
		MyLog.d(TAG, "upgrade:" + upgrade);
		// // upgrade
		// if (oldVersion < newVersion) {
		// MyLog.d(TAG, "UPGRADING FROM '%s' to '%s' ...", oldVersion, DB_VERSION);
		// // close the database
		// close();
		// // remove the existing database
		// if (context.deleteDatabase(DB_NAME)) {
		// // copy the new one
		// createDbIfNecessary();
		// Utils.notifyTheUser(context, context.getString(R.string.update_stm_db_ok));
		// } else {
		// MyLog.w(TAG, "Can't delete the current database.");
		// // notify the user that he need to remove and re-install the application
		// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error));
		// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error_next));
		// }
		// } else {
		// MyLog.w(TAG, "Trying to upgrade the db from version '%s' to version '%s'.", oldVersion,
		// newVersion);
		// }
	}

	/**
	 * Create the database if necessary.
	 * @param context the context used to create the database
	 * @param task the initialization task or <b>NULL</b>
	 */
	public void createDbIfNecessary(Context context, InitializationTask task) {
		MyLog.v(TAG, "createDbIfNecessary()");
		// IF DB doesn't exist DO
		if (!isDbExist(context)) {
			MyLog.d(TAG, "DB DOES NOT EXIST");
			try {
				MyLog.i(TAG, "Initialization of the STM database...");
				initDataBase(context, task);
				MyLog.i(TAG, "Initialization of the STM database... DONE");
			} catch (IOException ioe) {
				MyLog.e(TAG, ioe, "Error while initializating of the STM database!");
			}
			// } else {
			// MyLog.d(TAG, "DB EXIST");
			// // check version
			// // int currentVersion = this.getReadableDatabase().getVersion();
			// // always true!if (currentVersion != DB_VERSION) {
			// if (this.upgrade) {
			// // MyLog.d(TAG, "VERSION DIFF");
			// // upgrade
			// // if (currentVersion < DB_VERSION) {
			// MyLog.d(TAG, "UPGRADING ...");
			// // close the database
			// // this.close();
			// MyLog.d(TAG, "DATABASE CLOSED.");
			// // remove the existing database
			// if (context.deleteDatabase(DB_NAME)) {
			// MyLog.d(TAG, "DATABASE DELETED.");
			// // copy the new one
			// createDbIfNecessary(context, task);
			// Utils.notifyTheUser(context, context.getString(R.string.update_stm_db_ok));
			// } else {
			// MyLog.w(TAG, "Can't delete the current database.");
			// // notify the user that he need to remove and re-install the application
			// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error));
			// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error_next));
			// }
			// // } else {
			// // MyLog.w(TAG, "Trying to upgrade the db from version '%s' to version '%s'.", currentVersion,
			// // DB_VERSION);
			// // }
			// }
		}
	}

	/**
	 * Force the database reset (delete DB + {@link StmDbHelper#createDbIfNecessary(Context, InitializationTask)}.
	 * @param context the context
	 * @param task the initialization task (or NULL)
	 */
	public void forceReset(Context context, InitializationTask task) {
		if (context.deleteDatabase(DB_NAME)) {
			MyLog.d(TAG, "DATABASE DELETED.");
			// copy the new one
			createDbIfNecessary(context, task);
			// Utils.notifyTheUser(context, context.getString(R.string.update_stm_db_ok));
		} else {
			MyLog.w(TAG, "Can't delete the current database.");
			// notify the user that he need to remove and re-install the application
			// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error));
			// Utils.notifyTheUserLong(context, context.getString(R.string.update_stm_db_error_next));
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

	public Boolean isUpdateAvailable() {
		MyLog.v(TAG, "isUpdateAvailable()");
		MyLog.d(TAG, "upgrade:" + upgrade);
		return upgrade;
	}

	/**
	 * Initialize the database from the SQL dump.
	 * @param context the context use to open the input stream
	 * @param task the initialize task or <b>NULL</b>
	 */
	private void initDataBase(Context context, InitializationTask task) throws IOException {
		MyLog.v(TAG, "initDataBase()");
		// count the number of line of the SQL dump files
		int nbLine = 0;
		try {
			for (int fileId : DUMP_FILES) {
				nbLine += Utils.countNumberOfLine(context.getResources().openRawResource(fileId));
			}
			if (task != null) {
				task.initProgressBar(nbLine);
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while counting nb line!");
		}
		BufferedReader br = null;
		SQLiteDatabase dataBase = null;
		try {
			// open the database RW
			dataBase = this.getWritableDatabase();
			// global settings
			dataBase.execSQL("PRAGMA foreign_keys=OFF;");
			dataBase.execSQL("PRAGMA synchronous=OFF;");
			dataBase.execSQL("PRAGMA auto_vacuum=NONE;");
			// starting the transaction
			dataBase.beginTransaction();
			int lineNumber = 0;
			String line;
			for (int fileId : DUMP_FILES) {
				br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(fileId), "UTF8"));
				while ((line = br.readLine()) != null) {
					dataBase.execSQL(line);
					if (nbLine > 0 && task != null) {
						task.incrementProgressBar(++lineNumber);
					}
				}
			}
			// mark the transaction as successful
			dataBase.setTransactionSuccessful();
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while copying the database file!");
		} finally {
			try {
				if (dataBase != null) {
					// end the transaction
					dataBase.endTransaction();
					dataBase.close();
				}
			} catch (Exception e) {
				MyLog.w(TAG, e, "ERROR while closing the new database!");
			}
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				MyLog.w(TAG, e, "ERROR while closing the input stream!");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void close() {
		try {
			// this.close();
			super.close();
		} catch (Exception e) {
			MyLog.w(TAG, "Error while closing the databases!", e);
		}
	}
}

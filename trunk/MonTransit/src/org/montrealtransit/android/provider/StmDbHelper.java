package org.montrealtransit.android.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.activity.UserPreferences;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.widget.Toast;

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
	private static final String DB_NAME = "stm.db";

	/**
	 * The database version use to manage database changes.
	 */
	public static final int DB_VERSION = 21; // TODO 22

	/**
	 * The list of SQL dump files.
	 */
	private static final int[] DUMP_FILES = new int[] { R.raw.stm_db_directions_autobus, R.raw.stm_db_directions_metro, R.raw.stm_db_frequences_metro,
			R.raw.stm_db_horaire_metro, R.raw.stm_db_lignes_autobus, R.raw.stm_db_lignes_metro, R.raw.stm_db_stations_metro, R.raw.stm_db_arrets_autobus_loc,
			R.raw.stm_db_arrets_autobus_0, R.raw.stm_db_arrets_autobus_1, R.raw.stm_db_arrets_autobus_2, R.raw.stm_db_arrets_autobus_3,
			R.raw.stm_db_arrets_autobus_4, R.raw.stm_db_arrets_autobus_7 };

	// BUS LINES
	public static final String T_BUS_LINES = "lignes_autobus";
	public static final String T_BUS_LINES_K_NUMBER = BaseColumns._ID;
	public static final String T_BUS_LINES_K_NAME = "name";
	public static final String T_BUS_LINES_K_TYPE = "type";

	public static final String BUS_LINE_TYPE_REGULAR_SERVICE = "J";
	public static final String BUS_LINE_TYPE_RUSH_HOUR_SERVICE = "P";
	public static final String BUS_LINE_TYPE_NIGHT_SERVICE = "N";
	public static final String BUS_LINE_TYPE_EXPRESS_SERVICE = "E";

	// BUS LINES DIRECTIONS
	public static final String T_BUS_LINES_DIRECTIONS = "directions_autobus";
	public static final String T_BUS_LINES_DIRECTIONS_K_ID = "direction_id";
	public static final String T_BUS_LINES_DIRECTIONS_K_LINE_ID = "ligne_id";
	public static final String T_BUS_LINES_DIRECTIONS_K_NAME = "name";

	// BUS STOPS
	public static final String T_BUS_STOPS = "arrets_autobus";
	public static final String T_BUS_STOPS_K_CODE = BaseColumns._ID;
	public static final String T_BUS_STOPS_K_PLACE = "lieu";
	public static final String T_BUS_STOPS_K_LINE_NUMBER = "ligne_id";
	public static final String T_BUS_STOPS_K_DIRECTION_ID = "direction_id";
	public static final String T_BUS_STOPS_K_SUBWAY_STATION_ID = "station_id";
	public static final String T_BUS_STOPS_K_STOPS_ORDER = "arret_order";

	// BUS STOPS LOCATION
	public static final String T_BUS_STOPS_LOC = "arrets_autobus_loc";
	public static final String T_BUS_STOPS_LOC_K_CODE = BaseColumns._ID;
	public static final String T_BUS_STOPS_LOC_K_PLACE = "lieu";
	public static final String T_BUS_STOPS_LOC_K_STOP_LAT = "lat";
	public static final String T_BUS_STOPS_LOC_K_STOP_LNG = "lng";

	// SUBWAY FREQUENCES
	public static final String T_SUBWAY_FREQUENCES = "frequences_metro";
	public static final String T_SUBWAY_FREQUENCES_K_DIRECTION = "direction";
	public static final String T_SUBWAY_FREQUENCES_K_HOUR = "heure";
	public static final String T_SUBWAY_FREQUENCES_K_FREQUENCE = "frequence";
	public static final String T_SUBWAY_FREQUENCES_K_DAY = "day";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_WEEK = "";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_SUNDAY = "d";
	public static final String T_SUBWAY_FREQUENCES_K_DAY_SATURDAY = "s";

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

	// SUBWAY LINES
	public static final String T_SUBWAY_LINES = "lignes_metro";
	public static final String T_SUBWAY_LINES_K_NUMBER = BaseColumns._ID;
	public static final String T_SUBWAY_LINES_K_NAME = "name";

	// SUBWAY LINES DIRECTIONS
	public static final String T_SUBWAY_LINES_DIRECTIONS = "directions_metro";
	public static final String T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID = "ligne_id";
	public static final String T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ORDER = "station_order";
	public static final String T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID = "station_id";

	public static final String SUBWAY_DIRECTION_ID = "DIRECTION_ID";
	public static final String SUBWAY_DIRECTION_1 = "ASC";
	public static final String SUBWAY_DIRECTION_2 = "DESC";

	// SUBWAY STATIONS
	public static final String T_SUBWAY_STATIONS = "stations_metro";
	public static final String T_SUBWAY_STATIONS_K_STATION_ID = BaseColumns._ID;
	public static final String T_SUBWAY_STATIONS_K_STATION_NAME = "name";
	public static final String T_SUBWAY_STATIONS_K_STATION_LAT = "lat";
	public static final String T_SUBWAY_STATIONS_K_STATION_LNG = "lng";

	/**
	 * Database creation SQL statement for the bus lines table.
	 */
	// TODO remove 'schedule' (no i18n)
	private static final String DATABASE_CREATE_T_BUS_LINES = "CREATE TABLE IF NOT EXISTS " + T_BUS_LINES + " (" + T_BUS_LINES_K_NUMBER
			+ " integer PRIMARY KEY, " + T_BUS_LINES_K_NAME + " text, " + T_BUS_LINES_K_TYPE + " text, schedule text);";

	/**
	 * Database creation SQL statement for the bus line directions table.
	 */
	// TODO remove 'name' (no i18n)
	private static final String DATABASE_CREATE_T_BUS_LINES_DIRECTIONS = "CREATE TABLE IF NOT EXISTS " + T_BUS_LINES_DIRECTIONS + " ("
			+ T_BUS_LINES_DIRECTIONS_K_LINE_ID + " integer, " + T_BUS_LINES_DIRECTIONS_K_ID + " text, " + T_BUS_LINES_DIRECTIONS_K_NAME + " text);";

	/**
	 * Database creation SQL statement for the bus stops table.
	 */
	// FIXME remove 'directive' (no i18n)
	private static final String DATABASE_CREATE_T_BUS_STOPS = "CREATE TABLE IF NOT EXISTS " + T_BUS_STOPS + " (" + T_BUS_STOPS_K_CODE + " integer, "
			+ T_BUS_STOPS_K_LINE_NUMBER + " integer, " + T_BUS_STOPS_K_DIRECTION_ID + " text, " + T_BUS_STOPS_K_PLACE + " text, "
			+ T_BUS_STOPS_K_SUBWAY_STATION_ID + " integer, directive text, " + T_BUS_STOPS_K_STOPS_ORDER + " integer);";

	/**
	 * Database creation SQL statement for the bus stop locations table.
	 */
	private static final String DATABASE_CREATE_T_BUS_STOPS_LOC = "CREATE TABLE IF NOT EXISTS " + T_BUS_STOPS_LOC + " (" + T_BUS_STOPS_LOC_K_CODE
			+ " integer PRIMARY KEY, " + T_BUS_STOPS_LOC_K_PLACE + " text, " + T_BUS_STOPS_LOC_K_STOP_LAT + " real, " + T_BUS_STOPS_LOC_K_STOP_LNG + " real);";

	/**
	 * Database creation SQL statement for the subway frequencies table.
	 */
	private static final String DATABASE_CREATE_T_SUBWAY_FREQUENCES = "CREATE TABLE IF NOT EXISTS " + T_SUBWAY_FREQUENCES + " ("
			+ T_SUBWAY_FREQUENCES_K_DIRECTION + " integer, " + T_SUBWAY_FREQUENCES_K_DAY + " text, " + T_SUBWAY_FREQUENCES_K_HOUR + " real, "
			+ T_SUBWAY_FREQUENCES_K_FREQUENCE + " integer);";

	/**
	 * Database creation SQL statement for the subway hours table.
	 */
	private static final String DATABASE_CREATE_T_SUBWAY_HOUR = "CREATE TABLE IF NOT EXISTS " + T_SUBWAY_HOUR + " (" + T_SUBWAY_HOUR_K_STATION_ID
			+ " integer, " + T_SUBWAY_HOUR_K_DIRECTION_ID + " integer, " + T_SUBWAY_HOUR_K_DAY + " text, " + T_SUBWAY_HOUR_K_HOUR + " real, "
			+ T_SUBWAY_HOUR_K_FIRST_LAST + " text);";

	/**
	 * Database creation SQL statement for the subway lines table.
	 */
	private static final String DATABASE_CREATE_T_SUBWAY_LINES = "CREATE TABLE IF NOT EXISTS " + T_SUBWAY_LINES + " (" + T_SUBWAY_LINES_K_NUMBER
			+ " integer PRIMARY KEY, " + T_SUBWAY_LINES_K_NAME + " text);";

	/**
	 * Database creation SQL statement for the subway line directions table.
	 */
	private static final String DATABASE_CREATE_T_SUBWAY_LINES_DIRECTIONS = "CREATE TABLE IF NOT EXISTS " + T_SUBWAY_LINES_DIRECTIONS + " ("
			+ T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_LINE_ID + " integer, " + T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ID + " integer, "
			+ T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ORDER + " integer);";

	/**
	 * Database creation SQL statement for the subway stations table.
	 */
	private static final String DATABASE_CREATE_T_SUBWAY_STATIONS = "CREATE TABLE IF NOT EXISTS " + T_SUBWAY_STATIONS + " (" + T_SUBWAY_STATIONS_K_STATION_ID
			+ " integer PRIMARY KEY, " + T_SUBWAY_STATIONS_K_STATION_NAME + " text, " + T_SUBWAY_STATIONS_K_STATION_LAT + " real, "
			+ T_SUBWAY_STATIONS_K_STATION_LNG + " real);";

	/**
	 * Database drop SQL statement for the bus lines table.
	 */
	private static final String DATABASE_DROP_T_BUS_LINES = "DROP TABLE IF EXISTS " + T_BUS_LINES;
	/**
	 * Database drop SQL statement for the bus line directions table.
	 */
	private static final String DATABASE_DROP_T_BUS_LINES_DIRECTIONS = "DROP TABLE IF EXISTS " + T_BUS_LINES_DIRECTIONS;
	/**
	 * Database drop SQL statement for the bus stops table.
	 */
	private static final String DATABASE_DROP_T_BUS_STOPS = "DROP TABLE IF EXISTS " + T_BUS_STOPS;
	/**
	 * Database drop SQL statement for the bus stop locations table.
	 */
	private static final String DATABASE_DROP_T_BUS_STOPS_LOC = "DROP TABLE IF EXISTS " + T_BUS_STOPS_LOC;
	/**
	 * Database drop SQL statement for the subway frequencies table.
	 */
	private static final String DATABASE_DROP_T_SUBWAY_FREQUENCES = "DROP TABLE IF EXISTS " + T_SUBWAY_FREQUENCES;
	/**
	 * Database drop SQL statement for the subway hours table.
	 */
	private static final String DATABASE_DROP_T_SUBWAY_HOUR = "DROP TABLE IF EXISTS " + T_SUBWAY_HOUR;
	/**
	 * Database drop SQL statement for the subway lines table.
	 */
	private static final String DATABASE_DROP_T_SUBWAY_LINES = "DROP TABLE IF EXISTS " + T_SUBWAY_LINES;
	/**
	 * Database drop SQL statement for the subway lines directions table.
	 */
	private static final String DATABASE_DROP_T_SUBWAY_LINES_DIRECTIONS = "DROP TABLE IF EXISTS " + T_SUBWAY_LINES_DIRECTIONS;
	/**
	 * Database drop SQL statement for the subway stations table.
	 */
	private static final String DATABASE_DROP_T_SUBWAY_STATIONS = "DROP TABLE IF EXISTS " + T_SUBWAY_STATIONS;

	/**
	 * The context.
	 */
	private Context context;

	/**
	 * The default constructor.
	 * @param context the context
	 */
	public StmDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmDbHelper(%s, %s)", DB_NAME, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		MyLog.v(TAG, "onCreate()");
		initAllDbTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		MyLog.v(TAG, "onUpgrade(%s, %s)", oldVersion, newVersion);
		MyLog.d(TAG, "Upgrading database from version %s to %s.", oldVersion, newVersion);
		switch (oldVersion) {
		// case 17: TODO incremental
		// MyLog.v(TAG, "add the History table");
		// // just create the History table
		// db.execSQL(DATABASE_CREATE_T_HISTORY);
		// case 18:
		// MyLog.v(TAG, "add the Twitter API table");
		// // just create the Twitter API table
		// db.execSQL(DATABASE_CREATE_T_TWITTER_API);
		// break;
		default:
			MyLog.d(TAG, "Old data destroyed!");
			db.execSQL(DATABASE_DROP_T_BUS_LINES);
			db.execSQL(DATABASE_DROP_T_BUS_LINES_DIRECTIONS);
			db.execSQL(DATABASE_DROP_T_BUS_STOPS);
			db.execSQL(DATABASE_DROP_T_BUS_STOPS_LOC);
			db.execSQL(DATABASE_DROP_T_SUBWAY_FREQUENCES);
			db.execSQL(DATABASE_DROP_T_SUBWAY_HOUR);
			db.execSQL(DATABASE_DROP_T_SUBWAY_LINES);
			db.execSQL(DATABASE_DROP_T_SUBWAY_LINES_DIRECTIONS);
			db.execSQL(DATABASE_DROP_T_SUBWAY_STATIONS);
			initAllDbTables(db);
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
	 * @param context the context
	 * @return true if an update is required
	 */
	public static boolean isUpdateRequired(Context context) {
		return UserPreferences.getPrefLcl(context, UserPreferences.PREFS_LCL_STM_DB_VERSION, 0) != DB_VERSION;
	}

	public static void showUpdateRequiredIfNecessary(final Context context) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return isUpdateRequired(context);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					int messageId = StmDbHelper.isDbExist(context) ? R.string.update_message_starting : R.string.init_message_starting;
					Toast toast = Toast.makeText(context, messageId, Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			};
		}.execute();
	}

	/**
	 * Initialize the database from the SQL dump.
	 * @param dataBase the writable database.
	 */
	@Deprecated
	protected boolean initDataBase(SQLiteDatabase dataBase) {
		MyLog.v(TAG, "initDataBase()");
		BufferedReader br = null;
		try {
			// global settings
			dataBase.execSQL("PRAGMA foreign_keys=OFF;");
			// dataBase.execSQL("PRAGMA synchronous=OFF;");
			dataBase.execSQL("PRAGMA auto_vacuum=NONE;");
			// starting the transaction
			dataBase.beginTransaction();
			MyLog.d(TAG, "create tables");
			// create tables
			dataBase.execSQL(DATABASE_CREATE_T_BUS_LINES);
			dataBase.execSQL(DATABASE_CREATE_T_BUS_LINES_DIRECTIONS);
			dataBase.execSQL(DATABASE_CREATE_T_BUS_STOPS);
			dataBase.execSQL(DATABASE_CREATE_T_BUS_STOPS_LOC);
			dataBase.execSQL(DATABASE_CREATE_T_SUBWAY_FREQUENCES);
			dataBase.execSQL(DATABASE_CREATE_T_SUBWAY_HOUR);
			dataBase.execSQL(DATABASE_CREATE_T_SUBWAY_LINES);
			dataBase.execSQL(DATABASE_CREATE_T_SUBWAY_LINES_DIRECTIONS);
			dataBase.execSQL(DATABASE_CREATE_T_SUBWAY_STATIONS);
			// deploy data
			String line;
			for (int fileId : DUMP_FILES) {
				MyLog.d(TAG, "deploy data from " + fileId);
				br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(fileId), "UTF8"), 8192);
				while ((line = br.readLine()) != null) {
					dataBase.execSQL(line);
				}
			}
			// mark the transaction as successful
			dataBase.setTransactionSuccessful();
			UserPreferences.savePrefLcl(this.context, UserPreferences.PREFS_LCL_STM_DB_VERSION, DB_VERSION);
			return true;
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while copying the database file!");
			AnalyticsUtils
					.trackEvent(this.context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_DB_INIT_FAIL, e.getClass().getSimpleName(), DB_VERSION);
			// TODO handles no space left on the device
			return false;
		} finally {
			try {
				if (dataBase != null) {
					// end the transaction
					dataBase.endTransaction();
					// dataBase.close();
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
	 * Initialize all database tables to the latest version.
	 * @param dataBase the database
	 */
	private void initAllDbTables(SQLiteDatabase dataBase) {
		MyLog.v(TAG, "initAllDbTables()");
		// global settings
		dataBase.execSQL("PRAGMA foreign_keys=OFF;");
		// dataBase.execSQL("PRAGMA synchronous=OFF;");
		dataBase.execSQL("PRAGMA auto_vacuum=NONE;");
		// buses
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_BUS_LINES, DATABASE_DROP_T_BUS_LINES, new int[] { R.raw.stm_db_lignes_autobus });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_BUS_LINES_DIRECTIONS, DATABASE_DROP_T_BUS_LINES_DIRECTIONS,
				new int[] { R.raw.stm_db_directions_autobus });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_BUS_STOPS, DATABASE_DROP_T_BUS_STOPS, new int[] { R.raw.stm_db_arrets_autobus_0,
				R.raw.stm_db_arrets_autobus_1, R.raw.stm_db_arrets_autobus_2, R.raw.stm_db_arrets_autobus_3, R.raw.stm_db_arrets_autobus_4,
				R.raw.stm_db_arrets_autobus_7 });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_BUS_STOPS_LOC, DATABASE_DROP_T_BUS_STOPS_LOC, new int[] { R.raw.stm_db_arrets_autobus_loc });
		// subways
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_SUBWAY_FREQUENCES, DATABASE_DROP_T_SUBWAY_FREQUENCES, new int[] { R.raw.stm_db_frequences_metro });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_SUBWAY_HOUR, DATABASE_DROP_T_SUBWAY_HOUR, new int[] { R.raw.stm_db_horaire_metro });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_SUBWAY_LINES, DATABASE_DROP_T_SUBWAY_LINES, new int[] { R.raw.stm_db_lignes_metro });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_SUBWAY_LINES_DIRECTIONS, DATABASE_DROP_T_SUBWAY_LINES_DIRECTIONS,
				new int[] { R.raw.stm_db_directions_metro });
		initDbTableWithRetry(dataBase, DATABASE_CREATE_T_SUBWAY_STATIONS, DATABASE_DROP_T_SUBWAY_STATIONS, new int[] { R.raw.stm_db_stations_metro });
		UserPreferences.savePrefLcl(this.context, UserPreferences.PREFS_LCL_STM_DB_VERSION, DB_VERSION);
	}

	/**
	 * Initialize the database table (retry forever).
	 * @param dataBase the database
	 * @param createSQL the create SQL query
	 * @param dropSQL the drop SQL query
	 * @param fileIds the file(s) to deploy
	 */
	private void initDbTableWithRetry(SQLiteDatabase dataBase, String createSQL, String dropSQL, int[] fileIds) {
		MyLog.v(TAG, "initDbTableWithRetry(%s)", createSQL);
		boolean success = false;
		do {
			try {
				success = initDbTable(dataBase, createSQL, dropSQL, fileIds);
				MyLog.d(TAG, "DB deployed: " + success);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Error while deploying DB!");
				success = false;
			}
		} while (!success);
	}

	/**
	 * Initialize the database table.
	 * @paramdataBase the database
	 * @param createSQL the create SQL query
	 * @param dropSQL the drop SQL query
	 * @param fileIds the file(s) to deploy
	 * @return true if everything went well.
	 */
	private boolean initDbTable(SQLiteDatabase dataBase, String createSQL, String dropSQL, int[] fileIds) {
		MyLog.v(TAG, "initDbTable(%s)", createSQL);
		BufferedReader br = null;
		String line = null;
		try {
			dataBase.beginTransaction();
			// MyLog.d(TAG, "create tables");
			// create tables
			dataBase.execSQL(dropSQL); // drop if exists
			dataBase.execSQL(createSQL); // create if not exists
			// deploy data
			for (int fileId : fileIds) {
				// MyLog.d(TAG, "deploy data from " + fileId);
				br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(fileId), "UTF8"), 8192);
				while ((line = br.readLine()) != null) {
					dataBase.execSQL(line);
				}
			}
			// mark the transaction as successful
			dataBase.setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while copying the database file! (line:%s)", line);
			AnalyticsUtils
					.trackEvent(this.context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_DB_INIT_FAIL, e.getClass().getSimpleName(), DB_VERSION);
			// TODO handles no space left on the device
			return false;
		} finally {
			try {
				if (dataBase != null) {
					// end the transaction
					dataBase.endTransaction();
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

	@Override
	public synchronized void close() {
		try {
			super.close();
		} catch (Exception e) {
			MyLog.w(TAG, "Error while closing the databases!", e);
		}
	}

	/**
	 * @return return the required free space required to deploy the DB
	 */
	public static long getRequiredSize(Context context) {
		MyLog.v(TAG, "getRequiredSize()");
		long size = 0;
		for (int rawFileId : DUMP_FILES) {
			try {
				size += context.getResources().openRawResource(rawFileId).available();
			} catch (IOException ioe) {
				size += 1000000; // default size
			}
		}
		return size * 2; // twice the size to be sure
	}
}

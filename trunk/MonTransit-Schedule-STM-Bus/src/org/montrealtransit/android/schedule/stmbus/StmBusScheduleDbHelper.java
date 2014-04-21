package org.montrealtransit.android.schedule.stmbus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StmBusScheduleDbHelper extends SQLiteOpenHelper {

	public static final String TAG = StmBusScheduleDbHelper.class.getSimpleName();

	public static final String DB_NAME_FORMAT = "stmbusschedule_route_%s.db";

	private static final String RAW_FILE_FORMAT = "ca_mtl_stm_bus_schedules_route_%s";

	public static final int DB_VERSION = 7; // 2014-04-21

	public static final String T_SCHEDULES = "schedules";
	// public static final String T_SCHEDULES_K_ID = BaseColumns._ID;
	public static final String T_SCHEDULES_K_SERVICE_ID = "service_id";
	public static final String T_SCHEDULES_K_TRIP_ID = "trip_id";
	public static final String T_SCHEDULES_K_STOP_ID = "stop_id";
	public static final String T_SCHEDULES_K_DEPARTURE = "departure";

	public static final String T_SERVICE_DATES = "service_dates";
	// public static final String T_SERVICE_DATES_K_ID = BaseColumns._ID;
	public static final String T_SERVICE_DATES_K_SERVICE_ID = "service_id";
	public static final String T_SERVICE_DATES_K_DATE = "date";

	private static final String DATABASE_CREATE_T_SCHEDULES = "CREATE TABLE IF NOT EXISTS " + T_SCHEDULES + " (" //
			// + T_SCHEDULES_K_ID + " integer PRIMARY KEY AUTOINCREMENT, " //
			+ T_SCHEDULES_K_SERVICE_ID + " text, " //
			+ T_SCHEDULES_K_TRIP_ID + " integer, " //
			+ T_SCHEDULES_K_STOP_ID + " integer, " //
			+ T_SCHEDULES_K_DEPARTURE + " integer" //
			+ ");";

	private static final String DATABASE_CREATE_T_SERVICE_DATES = "CREATE TABLE IF NOT EXISTS " + T_SERVICE_DATES + " (" //
			// + T_SERVICE_DATES_K_ID + " integer PRIMARY KEY AUTOINCREMENT, " //
			+ T_SERVICE_DATES_K_SERVICE_ID + " text, " //
			+ T_SERVICE_DATES_K_DATE + " integer" //
			+ ");";

	public static final String T_SCHEDULES_SQL_INSERT = "INSERT INTO " + T_SCHEDULES + " (" + T_SCHEDULES_K_SERVICE_ID + "," + T_SCHEDULES_K_TRIP_ID + ","
			+ T_SCHEDULES_K_STOP_ID + "," + T_SCHEDULES_K_DEPARTURE + ") VALUES(%s)";

	public static final String T_SERVICE_DATES_SQL_INSERT = "INSERT INTO " + T_SERVICE_DATES + " (" + T_SERVICE_DATES_K_SERVICE_ID + ","
			+ T_SERVICE_DATES_K_DATE + ") VALUES(%s)";

	private static final String DATABASE_DROP_T_SCHEDULES = "DROP TABLE IF EXISTS " + T_SCHEDULES;

	private static final String DATABASE_DROP_T_SERVICE_DATES = "DROP TABLE IF EXISTS " + T_SERVICE_DATES;

	private Context context;

	private String routeId;

	private boolean deployingData = false;

	public StmBusScheduleDbHelper(Context context, String routeId) {
		super(context, String.format(DB_NAME_FORMAT, routeId), null, DB_VERSION);
		MyLog.v(TAG, "StmBusScheduleDbHelper(%s, %s)", String.format(DB_NAME_FORMAT, routeId), DB_VERSION);
		this.context = context;
		this.routeId = routeId;
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
		// case X: TODO incremental
		default:
			MyLog.d(TAG, "Old data destroyed!");
			db.execSQL(DATABASE_DROP_T_SCHEDULES);
			db.execSQL(DATABASE_DROP_T_SERVICE_DATES);
			initAllDbTables(db);
			break;
		}
	}

	@Override
	public synchronized void close() {
		try {
			// wait until the data is deployed
			while (this.deployingData) {
				MyLog.d(TAG, "waiting 1 second before closing route %s DB...", routeId);
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException ie) {
				}
			}
			super.close();
		} catch (Exception e) {
			MyLog.w(TAG, "Error while closing the databases!", e);
		}
	}

	private void initAllDbTables(SQLiteDatabase db) {
		MyLog.v(TAG, "initAllDbTables()");
		this.deployingData = true;
		// global settings
		db.execSQL("PRAGMA foreign_keys=OFF;");
		// dataBase.execSQL("PRAGMA synchronous=OFF;");
		db.execSQL("PRAGMA auto_vacuum=NONE;");
		// service dates
		initDbTableWithRetry(db, T_SERVICE_DATES, DATABASE_CREATE_T_SERVICE_DATES, T_SERVICE_DATES_SQL_INSERT, DATABASE_DROP_T_SERVICE_DATES,
				new String[] { "ca_mtl_stm_bus_service_dates" });
		final String startingWith = String.format(RAW_FILE_FORMAT, this.routeId);
		List<String> rawFileNames = new ArrayList<String>();
		try {
			Field[] fields = R.raw.class.getFields();
			for (Field f : fields) {
				if (f.getName().startsWith(startingWith)) {
					rawFileNames.add(f.getName());
				}
			}
		} catch (Exception e) {
			MyLog.w(TAG, e, "Error while listing raw files for route %s!", this.routeId);
		}
		initDbTableWithRetry(db, T_SCHEDULES, DATABASE_CREATE_T_SCHEDULES, T_SCHEDULES_SQL_INSERT, DATABASE_DROP_T_SCHEDULES,
				rawFileNames.toArray(new String[] {}));
		this.deployingData = false;
		MyLog.v(TAG, "initAllDbTables() - DONE (route: %s)", routeId);
	}

	private void initDbTableWithRetry(SQLiteDatabase dataBase, String table, String createSQL, String insertSQL, String dropSQL, String[] fileNames) {
		MyLog.v(TAG, "initDbTableWithRetry(%s)", table);
		boolean success = false;
		do {
			try {
				success = initDbTable(dataBase, table, createSQL, insertSQL, dropSQL, fileNames);
				MyLog.d(TAG, "DB deployed: %s", success);
			} catch (Exception e) {
				MyLog.w(TAG, "Error while deploying DB!", e);
				success = false;
			}
		} while (!success);
	}

	private boolean initDbTable(SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, String[] fileNames) {
		MyLog.v(TAG, "initDbTable(%s)", table);
		BufferedReader br = null;
		String line = null;
		try {
			db.beginTransaction();
			// MyLog.d(TAG, "create tables");
			// create tables
			db.execSQL(sqlDrop); // drop if exists
			db.execSQL(sqlCreate); // create if not exists
			// deploy data
			for (String fileName : fileNames) {
				// MyLog.d(TAG, "deploy data from " + fileId);
				// InputStream in = getResources().openRawResource(resourceName);
				br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(
						this.context.getResources().getIdentifier(fileName, "raw", this.context.getPackageName())), "UTF8"), 8192);
				while ((line = br.readLine()) != null) {
					db.execSQL(String.format(sqlInsert, line));
				}
			}
			// mark the transaction as successful
			db.setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while copying the database file! (line: %s)", line);
			// AnalyticsUtils
			// .trackEvent(this.context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_DB_INIT_FAIL, e.getClass().getSimpleName(), DB_VERSION);
			// TODO handles no space left on the device
			return false;
		} finally {
			try {
				if (db != null) {
					// end the transaction
					db.endTransaction();
				}
			} catch (Exception e) {
				MyLog.w(TAG, "ERROR while closing the new database!", e);
			}
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
				MyLog.w(TAG, "ERROR while closing the input stream!", e);
			}
		}
	}
}

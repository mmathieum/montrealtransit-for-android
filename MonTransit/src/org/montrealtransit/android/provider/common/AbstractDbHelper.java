package org.montrealtransit.android.provider.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.montrealtransit.android.MyLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public abstract class AbstractDbHelper extends SQLiteOpenHelper {

	private static final String TAG = AbstractDbHelper.class.getSimpleName();

	public static final String T_ROUTE = "route";
	public static final String T_ROUTE_K_ID = BaseColumns._ID;
	public static final String T_ROUTE_K_SHORT_NAME = "short_name";
	public static final String T_ROUTE_K_LONG_NAME = "long_name";
	public static final String T_ROUTE_K_COLOR = "color";
	public static final String T_ROUTE_K_TEXT_COLOR = "text_color";
	public static final String T_ROUTE_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_ROUTE + " (" //
			+ T_ROUTE_K_ID + SqlUtils.INT_PK + ", "//
			+ T_ROUTE_K_SHORT_NAME + SqlUtils.TXT + ", "//
			+ T_ROUTE_K_LONG_NAME + SqlUtils.TXT + ", " //
			+ T_ROUTE_K_COLOR + SqlUtils.TXT + ", "//
			+ T_ROUTE_K_TEXT_COLOR + SqlUtils.TXT + ")";
	public static final String T_ROUTE_SQL_INSERT = "INSERT INTO " + T_ROUTE + " (" + T_ROUTE_K_ID + "," + T_ROUTE_K_SHORT_NAME + "," + T_ROUTE_K_LONG_NAME
			+ "," + T_ROUTE_K_COLOR + "," + T_ROUTE_K_TEXT_COLOR + ") VALUES(%s)";
	public static final String T_ROUTE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE);

	public static final String T_TRIP = "trip";
	public static final String T_TRIP_K_ID = BaseColumns._ID;
	public static final String T_TRIP_K_HEADSIGN_TYPE = "headsign_type";
	public static final String T_TRIP_K_HEADSIGN_VALUE = "headsign_value"; // really?
	public static final String T_TRIP_K_ROUTE_ID = "route_id";
	public static final String T_TRIP_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_TRIP + " (" //
			+ T_TRIP_K_ID + SqlUtils.INT_PK + ", " //
			+ T_TRIP_K_HEADSIGN_TYPE + SqlUtils.INT + ", " //
			+ T_TRIP_K_HEADSIGN_VALUE + SqlUtils.TXT + ", " //
			+ T_TRIP_K_ROUTE_ID + SqlUtils.INT + "," //
			+ SqlUtils.getSQLForeignKey(T_TRIP_K_ROUTE_ID, T_ROUTE, T_ROUTE_K_ID) + ")";
	public static final String T_TRIP_SQL_INSERT = "INSERT INTO " + T_TRIP + " (" + T_TRIP_K_ID + "," + T_TRIP_K_HEADSIGN_TYPE + "," + T_TRIP_K_HEADSIGN_VALUE
			+ "," + T_TRIP_K_ROUTE_ID + ") VALUES(%s)";
	public static final String T_TRIP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP);

	public static final String T_STOP = "stop";
	public static final String T_STOP_K_ID = BaseColumns._ID;
	public static final String T_STOP_K_CODE = "code"; // optional
	public static final String T_STOP_K_NAME = "name";
	public static final String T_STOP_K_LAT = "lat";
	public static final String T_STOP_K_LNG = "lng";
	public static final String T_STOP_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_STOP + " (" //
			+ T_STOP_K_ID + SqlUtils.INT_PK + ", "//
			+ T_STOP_K_CODE + SqlUtils.TXT + ", " //
			+ T_STOP_K_NAME + SqlUtils.TXT + ", "//
			+ T_STOP_K_LAT + SqlUtils.REAL + ", " //
			+ T_STOP_K_LNG + SqlUtils.REAL + ")";
	public static final String T_STOP_SQL_INSERT = "INSERT INTO " + T_STOP + " (" + T_STOP_K_ID + "," + T_STOP_K_CODE + "," + T_STOP_K_NAME + ","
			+ T_STOP_K_LAT + "," + T_STOP_K_LNG + ") VALUES(%s)";
	public static final String T_STOP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STOP);

	public static final String T_TRIP_STOPS = "trip_stops";
	public static final String T_TRIP_STOPS_K_ID = BaseColumns._ID;
	public static final String T_TRIP_STOPS_K_TRIP_ID = "trip_id";
	public static final String T_TRIP_STOPS_K_STOP_ID = "stop_id";
	public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = "stop_sequence";
	public static final String T_TRIP_STOPS_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_TRIP_STOPS + "(" //
			+ T_TRIP_STOPS_K_ID + SqlUtils.INT_PK_AUTO + ", "// TODO SqlUtils.INT_PK ?
			+ T_TRIP_STOPS_K_TRIP_ID + SqlUtils.INT + ", "//
			+ T_TRIP_STOPS_K_STOP_ID + SqlUtils.INT + ", "//
			+ T_TRIP_STOPS_K_STOP_SEQUENCE + SqlUtils.INT + ", "//
			+ SqlUtils.getSQLForeignKey(T_TRIP_STOPS_K_TRIP_ID, T_TRIP, T_TRIP_K_ID) + ", "//
			+ SqlUtils.getSQLForeignKey(T_TRIP_STOPS_K_STOP_ID, T_STOP, T_STOP_K_ID) + ")";
	public static final String T_TRIP_STOPS_SQL_INSERT = "INSERT INTO " + T_TRIP_STOPS + " (" /* + T_TRIP_STOPS_K_ID + "," */+ T_TRIP_STOPS_K_TRIP_ID + ","
			+ T_TRIP_STOPS_K_STOP_ID + "," + T_TRIP_STOPS_K_STOP_SEQUENCE + ") VALUES(%s)";
	public static final String T_TRIP_STOPS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP_STOPS);

	private Context context;

	public AbstractDbHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		MyLog.v(TAG, "AbstractDbHelper(%s, %s)", name, version);
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
		MyLog.d(TAG, "Upgrading database '%s' from version %s to %s.", getDbName(), oldVersion, newVersion);
		// no custom updater => removing everything
		MyLog.d(TAG, "Using generic updater (reset).");
		db.execSQL(T_TRIP_STOPS_SQL_DROP);
		db.execSQL(T_STOP_SQL_DROP);
		db.execSQL(T_TRIP_SQL_DROP);
		db.execSQL(T_ROUTE_SQL_DROP);
		initAllDbTables(db);
	}

	/**
	 * @return current DB version w/o creating/upgrading or -1
	 */
	public static int getCurrentDbVersion(Context context, String dbName) {
		MyLog.v(TAG, "getCurrentDbVersion()");
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).getPath(), null, SQLiteDatabase.OPEN_READONLY);
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

	public static boolean isDbExist(Context context, String dbName) {
		return Arrays.asList(context.databaseList()).contains(dbName);
	}

	public boolean isDbExist(Context context) {
		return isDbExist(context, getDbName());
	}

	private void initAllDbTables(SQLiteDatabase db) {
		MyLog.v(TAG, "initAllDbTables()");
		// global settings
		// TODO FK support? db.execSQL("PRAGMA foreign_keys=OFF;");
		// db.execSQL("PRAGMA synchronous=OFF;");
		db.execSQL("PRAGMA auto_vacuum=NONE;");
		initDbTableWithRetry(db, T_ROUTE, T_ROUTE_SQL_CREATE, T_ROUTE_SQL_INSERT, T_ROUTE_SQL_DROP, getRouteFiles());
		initDbTableWithRetry(db, T_TRIP, T_TRIP_SQL_CREATE, T_TRIP_SQL_INSERT, T_TRIP_SQL_DROP, getTripFiles());
		initDbTableWithRetry(db, T_STOP, T_STOP_SQL_CREATE, T_STOP_SQL_INSERT, T_STOP_SQL_DROP, getStopFiles());
		initDbTableWithRetry(db, T_TRIP_STOPS, T_TRIP_STOPS_SQL_CREATE, T_TRIP_STOPS_SQL_INSERT, T_TRIP_STOPS_SQL_DROP, getTripStopsFiles());
	}

	private void initDbTableWithRetry(SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
		MyLog.v(TAG, "initDbTableWithRetry(%s)", table);
		boolean success = false;
		do {
			try {
				success = initDbTable(db, table, sqlCreate, sqlInsert, sqlDrop, files);
				MyLog.d(TAG, "DB table %s deployed: %s", table, success);
			} catch (Exception e) {
				MyLog.w(TAG, e, "Error while deploying DB table %s!", table);
				success = false;
			}
		} while (!success);
	}

	private boolean initDbTable(SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
		MyLog.v(TAG, "initDbTable(%s)", table);
		BufferedReader br = null;
		try {
			db.beginTransaction();
			// MyLog.d(TAG, "create tables");
			// create tables
			db.execSQL(sqlDrop); // drop if exists
			db.execSQL(sqlCreate); // create if not exists
			// deploy data
			String line;
			for (int file : files) {
				// MyLog.d(TAG, "deploy data from " + fileId);
				br = new BufferedReader(new InputStreamReader(this.context.getResources().openRawResource(file), "UTF8"), 8192);
				while ((line = br.readLine()) != null) {
					// db.execSQL(line);
					db.execSQL(String.format(sqlInsert, line));
				}
			}
			// mark the transaction as successful
			db.setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			MyLog.w(TAG, e, "ERROR while copying the database '%s' table '%s' file!", getDbName(), table);
			// AnalyticsUtils
			// .trackEvent(this.context, AnalyticsUtils.CATEGORY_ERROR, AnalyticsUtils.ACTION_DB_INIT_FAIL, e.getClass().getSimpleName(), DB_VERSION);
			// TODO handles no space left on the device
			return false;
		} finally {
			try {
				if (db != null) {
					db.endTransaction(); // end the transaction
				}
			} catch (Exception e) {
				MyLog.w(TAG, e, "ERROR while closing the new database '%s'!", getDbName());
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

	public abstract String getDbName();

	public abstract int getDbVersion();

	public abstract String getUID();

	public abstract int[] getRouteFiles();

	public abstract int[] getTripFiles();

	public abstract int[] getStopFiles();

	public abstract int[] getTripStopsFiles();

	public abstract int getLabel();
}

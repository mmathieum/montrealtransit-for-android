package org.montrealtransit.android.provider.stmbus;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.provider.common.AbstractDbHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class StmBusDbHelper extends AbstractDbHelper {

	private static final String TAG = StmBusDbHelper.class.getSimpleName();

	public static final String DB_NAME = "stmbus.db";

	public static final int DB_VERSION = 5; // 2014-03-07
	
	public static final int LABEL = R.string.ca_mtl_stm_bus_label;

	public StmBusDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmBusDbHelper(%s, %s)", DB_NAME, DB_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		super.onUpgrade(db, oldVersion, newVersion);
		// TODO custom (faster) upgrade ?
	}

	@Override
	public int getDbVersion() {
		return DB_VERSION;
	}
	
	@Override
	public String getUID() {
		return StmBusProvider.AUTHORITY; // TODO "ca_mtl_stm_bus";
	}

	@Override
	public String getDbName() {
		return DB_NAME;
	}
	
	@Override
	public int getLabel() {
		return LABEL;
	}

	@Override
	public int[] getRouteFiles() {
		return new int[] { R.raw.ca_mtl_stm_bus_routes };
	}

	@Override
	public int[] getStopFiles() {
		return new int[] { R.raw.ca_mtl_stm_bus_stops };
	}

	@Override
	public int[] getTripFiles() {
		return new int[] { R.raw.ca_mtl_stm_bus_trips };
	}

	@Override
	public int[] getTripStopsFiles() {
		return new int[] { R.raw.ca_mtl_stm_bus_trip_stops };
	}

}

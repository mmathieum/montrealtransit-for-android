package org.montrealtransit.android.provider.stmsubway;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.provider.common.AbstractDbHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class StmSubwayDbHelper extends AbstractDbHelper {

	private static final String TAG = StmSubwayDbHelper.class.getSimpleName();

	public static final String DB_NAME = "stmsubway.db";

	public static final int DB_VERSION = 7; // 2015-05-03

	public static final int LABEL = R.string.ca_mtl_stm_subway_label;

	public StmSubwayDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		MyLog.v(TAG, "StmSubwayDbHelper(%s, %s)", DB_NAME, DB_VERSION);
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

	// @Override
	// public String getUID() {
	// return StmSubwayProvider.AUTHORITY; // TODO "ca_mtl_stm_subway";
	// }

	@Override
	public String getDbName() {
		return DB_NAME;
	}

	// @Override
	// public int getLabel() {
	// return LABEL;
	// }

	@Override
	public int[] getRouteFiles() {
		return new int[] { R.raw.ca_mtl_stm_subway_routes };
	}

	@Override
	public int[] getStopFiles() {
		return new int[] { R.raw.ca_mtl_stm_subway_stops };
	}

	@Override
	public int[] getTripFiles() {
		return new int[] { R.raw.ca_mtl_stm_subway_trips };
	}

	@Override
	public int[] getTripStopsFiles() {
		return new int[] { R.raw.ca_mtl_stm_subway_trip_stops };
	}

}

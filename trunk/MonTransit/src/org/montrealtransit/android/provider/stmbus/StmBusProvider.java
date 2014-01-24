package org.montrealtransit.android.provider.stmbus;

import org.montrealtransit.android.provider.common.AbstractDbHelper;
import org.montrealtransit.android.provider.common.AbstractProvider;

import android.content.Context;
import android.content.UriMatcher;

public class StmBusProvider extends AbstractProvider {

	public static final String TAG = StmBusProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.stmbus";

	private static final UriMatcher URI_MATCHER = getNewUriMatcher(AUTHORITY);

	@Override
	public UriMatcher getURIMATCHER() {
		return URI_MATCHER;
	}

	@Override
	public String getAUTHORITY() {
		return AUTHORITY;
	}

	@Override
	public int getCurrentDbVersion() {
		return StmBusDbHelper.DB_VERSION;
	}

	@Override
	public int getProviderLabel() {
		return StmBusDbHelper.LABEL;
	}

	@Override
	public String getDbName() {
		return StmBusDbHelper.DB_NAME;
	}

	@Override
	public AbstractDbHelper getNewDbHelper(Context context) {
		return new StmBusDbHelper(context.getApplicationContext());
	}
}

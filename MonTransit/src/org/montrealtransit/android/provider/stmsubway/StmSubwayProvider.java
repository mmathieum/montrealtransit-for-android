package org.montrealtransit.android.provider.stmsubway;

import org.montrealtransit.android.provider.common.AbstractDbHelper;
import org.montrealtransit.android.provider.common.AbstractProvider;

import android.content.Context;
import android.content.UriMatcher;

public class StmSubwayProvider extends AbstractProvider {

	public static final String TAG = StmSubwayProvider.class.getSimpleName();

	public static final String AUTHORITY = "org.montrealtransit.android.stmsubway";

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
		return StmSubwayDbHelper.DB_VERSION;
	}

	@Override
	public int getProviderLabel() {
		return StmSubwayDbHelper.LABEL;
	}

	@Override
	public String getDbName() {
		return StmSubwayDbHelper.DB_NAME;
	}

	@Override
	public AbstractDbHelper getNewDbHelper(Context context) {
		return new StmSubwayDbHelper(context.getApplicationContext());
	}
}

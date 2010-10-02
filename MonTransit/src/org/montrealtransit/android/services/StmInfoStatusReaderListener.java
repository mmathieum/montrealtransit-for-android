package org.montrealtransit.android.services;

import org.montrealtransit.android.data.StmInfoStatuses;

public interface StmInfoStatusReaderListener {

	void onStmInfoStatusesLoaded(StmInfoStatuses statuses);

}

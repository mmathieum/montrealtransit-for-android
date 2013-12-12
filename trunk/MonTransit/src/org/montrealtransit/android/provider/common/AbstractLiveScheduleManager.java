package org.montrealtransit.android.provider.common;

import java.util.HashMap;
import java.util.Map;

import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.provider.StmSubwayManager;

public class AbstractLiveScheduleManager {
	
	public static Map<String, String> authoritiesToLiveScheduleAuthorities;
	static {
		// dirty: should have been "<schedule_authority>"="<authority>.schedule"
		// need to wait until existing provider data become useless to upgrade
		authoritiesToLiveScheduleAuthorities = new HashMap<String, String>();
		authoritiesToLiveScheduleAuthorities.put(StmBusManager.AUTHORITY, "available"); // TODO burk!
		authoritiesToLiveScheduleAuthorities.put(StmSubwayManager.AUTHORITY, null); // Not Available
	}

}

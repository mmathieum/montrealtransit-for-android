package org.montrealtransit.android.dialog;

import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;

public interface RouteSelectTripDialogListener {

	void showNewRouteTrip(String authority, Route route, Trip trip);

}
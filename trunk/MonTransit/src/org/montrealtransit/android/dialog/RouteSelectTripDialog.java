package org.montrealtransit.android.dialog;

import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.RouteInfo;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;

public class RouteSelectTripDialog implements RouteSelectTripDialogListener {

	public static final String TAG = RouteSelectTripDialog.class.getSimpleName();

	private Context context;
	private Route route;
	private Integer selectedTripId;
	private String authority;
	private RouteSelectTripDialogListener listener;

	public RouteSelectTripDialog(Context context, String authority, Route route, Integer selectedTripId, RouteSelectTripDialogListener listener) {
		MyLog.v(TAG, "RouteSelectTripDialog(%s)", route, selectedTripId);
		this.context = context;
		this.authority = authority;
		this.route = route;
		this.selectedTripId = selectedTripId;
		if (listener == null) {
			this.listener = this;
		} else {
			this.listener = listener;
		}
	}

	public void showDialog() {
		MyLog.v(TAG, "showDialog()");
		final List<Trip> routeTrips = AbstractManager.findTripsWithRouteIdList(this.context, Utils.newContentUri(this.authority), this.route.id);
		final int count = routeTrips == null ? 0 : routeTrips.size();
		// MyLog.d(TAG, "showDialog() route trips count: %s", count);
		String[] tripHeadings = new String[count];
		int[] tripIds = new int[count];
		int selectedTripIndex = -1; // TODO persist preferred trip into preferences
		if (routeTrips != null) {
			for (int i = 0; i < count; i++) {
				final Trip trip = routeTrips.get(i);
				tripIds[i] = trip.id;
				tripHeadings[i] = trip.getHeading(this.context);
				if (this.selectedTripId != null && trip.id == this.selectedTripId.intValue()) {
					selectedTripIndex = i;
				}
			}
		}
		StringBuilder routeNameSb = new StringBuilder();
		if (!TextUtils.isEmpty(route.shortName)) {
			routeNameSb.append(route.shortName);
		}
		if (!TextUtils.isEmpty(route.longName)) {
			if (routeNameSb.length() > 0) {
				routeNameSb.append(' ');
			}
			routeNameSb.append(route.longName);
		}
		final String dialogTitle = this.context.getString(R.string.select_route_trip_and_route_name, routeNameSb.toString());
		new AlertDialog.Builder(this.context).setTitle(dialogTitle)
				.setSingleChoiceItems(tripHeadings, selectedTripIndex, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyLog.v(TAG, "onClick(%s)", which);
						// show the route trips
						Trip selectedTrip = routeTrips.get(which);
						if (selectedTrip != null) {
							RouteSelectTripDialog.this.listener.showNewRouteTrip(RouteSelectTripDialog.this.authority, RouteSelectTripDialog.this.route,
									selectedTrip);
							dialog.dismiss(); // CLOSE
						}
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel(); // CANCEL
					}
				}).create().show();

	}

	@Override
	public void showNewRouteTrip(String authority, Route route, Trip trip) {
		RouteSelectTripDialog.this.context.startActivity(RouteInfo.newInstance(RouteSelectTripDialog.this.context, authority, route, trip.id, null));
	}

}

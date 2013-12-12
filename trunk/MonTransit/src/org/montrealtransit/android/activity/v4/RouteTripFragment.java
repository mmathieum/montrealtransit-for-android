package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.StopInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.common.AbstractManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This fragment shows the stops for a specific route trip.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class RouteTripFragment extends Fragment implements AdapterView.OnItemClickListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = RouteTripFragment.class.getSimpleName();

	/**
	 * The stops list adapter.
	 */
	private POIArrayAdapter adapter;
	/**
	 * The selected stop code
	 */
	private String currentStopUID;

	public static Fragment newInstance(String authority, Integer routeId, Integer tripId, Integer stopId) {
		MyLog.v(TAG, "newInstance(%s,%s,%s, %s)", authority, routeId, tripId, stopId);
		RouteTripFragment f = new RouteTripFragment();

		Bundle args = new Bundle();
		args.putString(RouteInfo.EXTRA_AUTHORITY, authority);
		args.putInt(RouteInfo.EXTRA_ROUTE_ID, routeId);
		args.putInt(RouteInfo.EXTRA_TRIP_ID, tripId);
		if (stopId != null) {
			args.putInt(RouteInfo.EXTRA_STOP_ID, stopId);
		}
		f.setArguments(args);

		return f;
	}

	public static Fragment newInstance(String authority, Route route, Trip trip, Integer stopId) {
		MyLog.v(TAG, "newInstance(%s,%s,%s,%s)", authority, route, trip, stopId);
		RouteTripFragment f = new RouteTripFragment();

		Bundle args = new Bundle();
		args.putString(RouteInfo.EXTRA_AUTHORITY, authority);
		args.putInt(RouteInfo.EXTRA_ROUTE_ID, route.id);
		args.putInt(RouteInfo.EXTRA_TRIP_ID, trip.id);
		if (stopId != null) {
			args.putInt(RouteInfo.EXTRA_STOP_ID, stopId);
		}
		f.setArguments(args);

		return f;
	}

	@Override
	public void onAttach(Activity activity) {
		MyLog.v(TAG, "onAttach()");
		super.onAttach(activity);
		this.lastActivity = activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.lastActivity = null;
	}

	private Activity lastActivity;

	private Activity getLastActivity() {
		FragmentActivity newActivity = getActivity();
		if (newActivity != null) {
			this.lastActivity = newActivity;
		}
		return this.lastActivity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.route_info_stops_list, container, false);
		((ListView) v.findViewById(R.id.list)).setEmptyView(v.findViewById(R.id.list_empty));
		this.lastView = v;
		return v;
	}

	private View lastView;

	private Uri contentUri;

	private View getLastView() {
		View newView = getView();
		if (newView != null) {
			this.lastView = newView;
		}
		return this.lastView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		MyLog.v(TAG, "onActivityCreated()");
		super.onActivityCreated(savedInstanceState);
		showAll();
	}

	private void showAll() {
		String authority = getArguments().getString(RouteInfo.EXTRA_AUTHORITY);
		Integer tripId = getArguments().getInt(RouteInfo.EXTRA_TRIP_ID);
		Integer currentStopId = getArguments().getInt(RouteInfo.EXTRA_STOP_ID);
		ListView listView = (ListView) getLastView().findViewById(R.id.list);
		this.adapter = new POIArrayAdapter(getLastActivity());
		this.adapter.setShakeEnabled(true);
		this.adapter.setPois(null);
		this.adapter.setIntentExtras(StopInfo.newInstanceExtra(getRouteInfoActivity().getRoute()));
		this.adapter.setListView(listView);
		listView.setOnItemClickListener(this);
		refreshStopListFromDB(authority, tripId, currentStopId);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s,%s)", position, id);
		if (this.adapter != null && position < this.adapter.getPoisCount() && this.adapter.getPoi(position) != null) {
			// IF last bus stop, show descent only
			if (position + 1 == this.adapter.getPoisCount()) {
				Toast toast = Toast.makeText(getLastActivity(), R.string.descent_only, Toast.LENGTH_SHORT);
				// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
				toast.show();
				// return; why not?
			}
			this.adapter.onItemClick(parent, view, position, id);
		}
	}

	public void onResumeWithFocus(RouteInfo activity) {
		MyLog.v(TAG, "onResumeWithFocus()");
		this.adapter.setLocation(activity.getLocation());
		refreshFavoriteStopCodesFromDB(activity.getContentResolver());
	}

	@Override
	public void onPause() {
		MyLog.v(TAG, "onPause()");
		this.adapter.onPause();
		super.onPause();
	}

	/**
	 * Refresh the stops list UI.
	 */
	private void refreshStopListFromDB(String authority, Integer tripId, Integer stopId) { // TODO extract?
		MyLog.v(TAG, "refreshStopListFromDB(%s,%s,%s)", authority, tripId, stopId);
		this.contentUri = Utils.newContentUri(authority);
		new AsyncTask<Integer, Void, Void>() {
			@Override
			protected Void doInBackground(Integer... params) {
				// MyLog.d(TAG, "refreshStopListFromDB() > doInBackground()");
				Integer tripId = params[0], stopId = params[1];
				RouteInfo activity = RouteTripFragment.this.getRouteInfoActivity();
				List<TripStop> stopList = AbstractManager.findStopsWithTripIdList(activity, RouteTripFragment.this.contentUri, tripId);
				if (stopList != null) {
					// creating the list of the subways stations object
					for (TripStop tripStop : stopList) {
						if (tripStop.stop.id == stopId) {
							RouteTripFragment.this.currentStopUID = tripStop.getUID();
						}
					}
					RouteTripFragment.this.adapter.setPois(stopList);
				} else {
					RouteTripFragment.this.adapter.setPois(new ArrayList<TripStop>()); // TODO what's wrong with null?
				}
				// force update all stops with location
				// MyLog.d(TAG, "activity.getLocation(): " + activity.getLocation());
				RouteTripFragment.this.adapter.updateDistancesNow(activity.getLocation());
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				MyLog.d(TAG, "refreshStopListFromDB() > onPostExecute()");
				RouteInfo activity = RouteTripFragment.this.getRouteInfoActivity();
				View view = RouteTripFragment.this.getLastView();
				if (activity == null || view == null) {
					return; // too late, the parent activity is gone
				}
				refreshFavoriteStopCodesFromDB(activity.getContentResolver());
				ListView listView = (ListView) view.findViewById(R.id.list);
				int index = selectedStopIndex();
				// MyLog.d(TAG, "refreshStopListFromDB() > index: " + index);
				if (index > 0) {
					index--; // show 1 more stop on top of the list
				}
				SupportFactory.get().listViewScrollTo(listView, index, 50);
				RouteTripFragment.this.adapter.updateCompassNow();
			}

		}.execute(tripId, stopId);
	}

	/**
	 * @return the current stop index
	 */
	private int selectedStopIndex() {
		// MyLog.v(TAG, "selectedStopIndex()");
		if (this.adapter.getPois() != null) {
			for (int i = 0; i < this.adapter.getPoisCount(); i++) {
				if (this.currentStopUID != null) {
					if (this.adapter.getPoi(i).getUID().equals(this.currentStopUID)) {
						return i;
					}
				} else if (this.adapter.hasClosestPOI()) {
					if (this.adapter.isClosestPOI(i)) {
						return i;
					}
				}
			}
		}
		// MyLog.d(TAG, "selectedStopIndex() > not found!");
		return 0;
	}

	protected void setLocation(Location currentLocation) {
		this.adapter.setLocation(currentLocation);
	}

	/**
	 * @return see {@link #getLastActivity()}
	 */
	private RouteInfo getRouteInfoActivity() {
		return (RouteInfo) getLastActivity();
	}

	/**
	 * Show the closest stop (if possible).
	 * @return true if action performed
	 */
	protected boolean showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.adapter.hasClosestPOI()) {
			Toast.makeText(getLastActivity(), R.string.shake_closest_stop_selected, Toast.LENGTH_SHORT).show();
			return this.adapter.showClosestPOI();
		}
		return false;
	}

	/**
	 * Find favorites stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB(final ContentResolver contentResolver) {
		MyLog.v(TAG, "refreshFavoriteStopCodesFromDB()");
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(contentResolver, Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				RouteTripFragment.this.adapter.setFavs(result);
			};
		}.execute();
	}

}

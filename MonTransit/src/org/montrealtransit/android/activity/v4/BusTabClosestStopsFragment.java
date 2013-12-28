package org.montrealtransit.android.activity.v4;

import java.util.List;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.services.ClosestRouteTripStopsFinderTask;
import org.montrealtransit.android.services.ClosestRouteTripStopsFinderTask.ClosestRouteTripStopsFinderListener;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ListView;
import android.widget.TextView;

@TargetApi(4)
public class BusTabClosestStopsFragment extends Fragment implements LocationListener, ClosestRouteTripStopsFinderListener {
	/**
	 * The log tag.
	 */
	private static final String TAG = BusTabClosestStopsFragment.class.getSimpleName();

	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Store the device location.
	 */
	private Location location;

	/**
	 * The bus stops list adapter.
	 */
	private POIArrayAdapter adapter;

	/**
	 * The task used to find the closest stations.
	 */
	private ClosestRouteTripStopsFinderTask closestStopsTask;
	/**
	 * The location used to generate the closest stops.
	 */
	private Location closestStopsLocation;
	/**
	 * The location address used to generate the closest stops.
	 */
	protected String closestStopsLocationAddress;

	private boolean paused = false;

	/**
	 * @return the fragment
	 */
	public static Fragment newInstance() {
		MyLog.v(TAG, "newInstance()");
		return new BusTabClosestStopsFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		MyLog.v(TAG, "onAttach()");
		super.onAttach(activity);
		this.lastActivity = activity;
	}

	private Activity lastActivity;

	private Activity getLastActivity() {
		Activity newActivity = getActivity();
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
		View v = inflater.inflate(R.layout.bus_tab_closest_stops, container, false);
		setupView(v);
		this.lastView = v;
		return v;
	}

	private View lastView;

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

	private void setupView(View v) {
		v.findViewById(R.id.title_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshClosestStops(v);
			}
		});
	}

	private void showAll() {
		MyLog.v(TAG, "showAll()");
		View view = getLastView();
		Activity activity = getLastActivity();
		if (view.findViewById(R.id.closest_stops) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.closest_stops_stub)).inflate(); // inflate
		}
		this.adapter = new POIArrayAdapter(activity);
		ListView closestStopsListView = (ListView) view.findViewById(R.id.closest_stops);
		this.adapter.setListView(closestStopsListView);
		if (this.adapter.getPois() == null) {
			showClosestStops(view, activity);
			return;
		}
		if (view.findViewById(R.id.closest_stops) != null) { // IF present/inflated DO
			view.findViewById(R.id.closest_stops).setVisibility(View.VISIBLE); // show
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(activity, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Show the closest stops UI.
	 */
	public void showClosestStops(View view, Activity activity) {
		MyLog.v(TAG, "showClosestStops()");
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(activity, this, this.locationUpdatesEnabled, this.paused);
		// IF there is no closest stops DO
		if (this.adapter.getPois() == null) {
			// look for the closest stops
			refreshClosestStops();
		} else {
			// show the closest stops
			showNewClosestStops(view, activity, false);
			// IF the latest location is too old DO
			if (LocationUtils.isTooOld(this.closestStopsLocation)) {
				// start refreshing
				refreshClosestStops();
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
	}

	/**
	 * Start the refresh closest stops tasks if necessary.
	 */
	private void refreshClosestStops() {
		MyLog.v(TAG, "refreshClosestStops()");
		// IF the task is NOT already running DO
		if (this.closestStopsTask == null || !this.closestStopsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setClosestStopsLoading(null);
			// IF location found DO
			Location currentLocation = getLocation();
			if (currentLocation == null) {
				// MyLog.d(TAG, "no location yet...");
				return;
			}
			// find the closest stations
			this.closestStopsTask = new ClosestRouteTripStopsFinderTask(this, getLastActivity(), new String[] { StmBusManager.AUTHORITY }, SupportFactory.get()
					.getNbClosestPOIDisplay());
			this.closestStopsTask.execute(currentLocation.getLatitude(), currentLocation.getLongitude());
			this.closestStopsLocation = currentLocation;
			new AsyncTask<Location, Void, String>() {

				@Override
				protected String doInBackground(Location... locations) {
					Address address = LocationUtils.getLocationAddress(BusTabClosestStopsFragment.this.getLastActivity(), locations[0]);
					if (address == null || BusTabClosestStopsFragment.this.closestStopsLocation == null) {
						return null;
					}
					return LocationUtils.getLocationString(BusTabClosestStopsFragment.this.getLastActivity(), R.string.closest_bus_stops, address,
							BusTabClosestStopsFragment.this.closestStopsLocation.getAccuracy());
				}

				@Override
				protected void onPostExecute(String result) {
					boolean refreshRequired = BusTabClosestStopsFragment.this.closestStopsLocationAddress == null;
					BusTabClosestStopsFragment.this.closestStopsLocationAddress = result;
					if (refreshRequired) {
						showNewClosestStopsTitle(getLastView(), getLastActivity());
					}
				}

			}.execute(this.closestStopsLocation);
			// ELSE wait for location...
		}
	}

	@Override
	public void onClosestStopsProgress(String message) {
		MyLog.v(TAG, "onClosestStopsProgress(%s)", message);
		setClosestStopsLoading(message);
	}

	@Override
	public void onClosestStopsDone(ClosestPOI<RouteTripStop> result) {
		MyLog.v(TAG, "onClosestStopsDone(%s)", result == null ? null : result.getPoiListSize());
		View view = getLastView();
		Activity activity = getLastActivity();
		if (view == null || activity == null) {
			return;
		}
		if (result == null || result.getPoiListOrNull() == null) {
			// show the error
			setClosestStopsError(view, activity);
		} else {
			// get the result
			this.adapter.setPois(result.getPoiList());
			this.adapter.updateDistancesNow(this.location);
			this.adapter.prefetchClosests();
			refreshFavoriteUIDsFromDB();
			// show the result
			showNewClosestStops(view, activity, LocationUtils.areTheSame(this.closestStopsLocation, result.getLat(), result.getLng()));
			setClosestStopsNotLoading(view);
		}
	}

	/**
	 * Show new closest stops title.
	 */
	public void showNewClosestStopsTitle(View view, Context context) {
		if (this.closestStopsLocationAddress != null && this.closestStopsLocation != null && context != null && view != null) {
			View closestStopsLayout = view.findViewById(R.id.closest_stops_layout);
			((TextView) closestStopsLayout.findViewById(R.id.title_text)).setText(this.closestStopsLocationAddress);
		}
	}

	/**
	 * Show the new closest stops.
	 */
	private void showNewClosestStops(View view, Context activity, boolean scroll) {
		MyLog.v(TAG, "showNewClosestStops(%s)", scroll);
		if (this.adapter.getPois() != null && view != null && activity != null) {
			// set the closest stop title
			showNewClosestStopsTitle(view, activity);
			// hide loading
			if (view.findViewById(R.id.closest_stops_loading) != null) { // IF inflated/present DO
				view.findViewById(R.id.closest_stops_loading).setVisibility(View.GONE); // hide
			}
			if (view.findViewById(R.id.closest_stops) == null) { // IF NOT present/inflated DO
				((ViewStub) view.findViewById(R.id.closest_stops_stub)).inflate(); // inflate
			}
			// show stops list
			this.adapter.notifyDataSetChanged(true);
			ListView closestStopsListView = (ListView) view.findViewById(R.id.closest_stops);
			if (scroll) {
				SupportFactory.get().listViewScrollTo(closestStopsListView, 0, 0);
			}
			closestStopsListView.setVisibility(View.VISIBLE);
			setClosestStopsNotLoading(view);
		}
	}

	/**
	 * Find favorites bus stops UIDs.
	 */
	private void refreshFavoriteUIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getLastActivity().getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				BusTabClosestStopsFragment.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStopsError(View view, Context activity) {
		MyLog.v(TAG, "setClosestStopsError()");
		// IF there are already stations DO
		if (this.adapter.getPois() != null) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUser(activity, getString(R.string.closest_bus_stops_error));
		} else {
			// show the BIG message
			TextView cancelMsgTv = new TextView(activity);
			cancelMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			cancelMsgTv.setText(getString(R.string.closest_bus_stops_error));
			// ((ListView) findViewById(R.id.closest_stops)).addView(cancelMsgTv);
			// hide loading
			view.findViewById(R.id.closest_stops_loading).setVisibility(View.GONE);
			view.findViewById(R.id.closest_stops).setVisibility(View.VISIBLE);
		}
		setClosestStopsNotLoading(view);
	}

	/**
	 * Set the closest stations as not loading.
	 */
	private void setClosestStopsNotLoading(View view) {
		MyLog.v(TAG, "setClosestStopsNotLoading()");
		View closestStopsLayout = view.findViewById(R.id.closest_stops_layout);
		// show refresh icon instead of loading
		closestStopsLayout.findViewById(R.id.title_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		closestStopsLayout.findViewById(R.id.title_progress_bar).setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the closest stops as loading.
	 */
	private void setClosestStopsLoading(String detailMsg) {
		MyLog.v(TAG, "setClosestStationsLoading(%s)", detailMsg);
		View closestStopsLayout = getLastView().findViewById(R.id.closest_stops_layout);
		if (this.adapter.getPois() == null) {
			// set the BIG loading message
			// remove last location from the list divider
			((TextView) closestStopsLayout.findViewById(R.id.title_text)).setText(R.string.closest_bus_stops);
			if (getLastView().findViewById(R.id.closest_stops) != null) { // IF inflated/present DO
				// hide the list
				getLastView().findViewById(R.id.closest_stops).setVisibility(View.GONE);
				// clean the list (useful ?)
				// ((ListView) findViewById(R.id.closest_stops)).removeAllViews();
			}
			// show loading
			if (getLastView().findViewById(R.id.closest_stops_loading) == null) { // IF NOT inflated/present DO
				((ViewStub) getLastView().findViewById(R.id.closest_stops_loading_stub)).inflate(); // inflate
			}
			getLastView().findViewById(R.id.closest_stops_loading).setVisibility(View.VISIBLE);
			// show waiting for location
			TextView detailMsgTv = (TextView) getLastView().findViewById(R.id.closest_stops_loading).findViewById(R.id.detail_msg);
			if (detailMsg == null) { // show waiting for location
				detailMsg = getString(R.string.waiting_for_location_fix);
			}
			detailMsgTv.setText(detailMsg);
			detailMsgTv.setVisibility(View.VISIBLE);
			// } else { just notify the user ?
		}
		// show stop icon instead of refresh
		closestStopsLayout.findViewById(R.id.title_refresh).setVisibility(View.GONE);
		// show progress bar
		closestStopsLayout.findViewById(R.id.title_progress_bar).setVisibility(View.VISIBLE);
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		// MyLog.v(TAG, "getLocation()");
		if (this.location == null) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					return LocationUtils.getBestLastKnownLocation(BusTabClosestStopsFragment.this.getLastActivity());
				}

				@Override
				protected void onPostExecute(Location result) {
					if (result != null) {
						setLocation(result);
					}
					// enable location updates if necessary
					BusTabClosestStopsFragment.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(
							BusTabClosestStopsFragment.this.getLastActivity(), BusTabClosestStopsFragment.this,
							BusTabClosestStopsFragment.this.locationUpdatesEnabled, BusTabClosestStopsFragment.this.paused);
				}

			}.execute();
		}
		return this.location;
	}

	/**
	 * @param newLocation the new location
	 */
	private void setLocation(Location newLocation) {
		if (newLocation != null) {
			// MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.adapter.setLocation(this.location);
				if (this.adapter.getPois() == null) {
					// start refreshing if not running.
					refreshClosestStops();
				}
			}
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		// MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	@Override
	public void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(getLastActivity(), this, this.locationUpdatesEnabled);
		this.adapter.onPause();
		super.onPause();
	}

	@Override
	public void onResume() {
		MyLog.v(TAG, "onResume()");
		this.paused = false;
		super.onResume();
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus(BusTab activity) {
		MyLog.v(TAG, "onResumeWithFocus()");
		// IF location updates should be enabled DO
		if (!this.locationUpdatesEnabled) {
			new AsyncTask<Void, Void, Location>() {
				@Override
				protected Location doInBackground(Void... params) {
					return LocationUtils.getBestLastKnownLocation(BusTabClosestStopsFragment.this.getLastActivity());
				}

				@Override
				protected void onPostExecute(Location result) {
					// IF there is a valid last know location DO
					if (result != null) {
						if (BusTabClosestStopsFragment.this.closestStopsLocation != null) {
							if (LocationUtils.isMoreRelevant(BusTabClosestStopsFragment.this.closestStopsLocation, result,
									LocationUtils.SIGNIFICANT_ACCURACY_IN_METERS, Utils.CLOSEST_POI_LIST_PREFER_ACCURACY_OVER_TIME)
									&& LocationUtils.isTooOld(BusTabClosestStopsFragment.this.closestStopsLocation, Utils.CLOSEST_POI_LIST_TIMEOUT)) {
								BusTabClosestStopsFragment.this.adapter.setPois(null); // force refresh
							}
						}
						// set the new distance
						setLocation(result);
					}
					// re-enable
					BusTabClosestStopsFragment.this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(
							BusTabClosestStopsFragment.this.getLastActivity(), BusTabClosestStopsFragment.this,
							BusTabClosestStopsFragment.this.locationUpdatesEnabled, BusTabClosestStopsFragment.this.paused);

				};

			}.execute();
		}
		refreshFavoriteUIDsFromDB();
	}

	/**
	 * Refresh or stop refresh the closest stops depending if running.
	 * @param v a view (not used)
	 */
	public void refreshOrStopRefreshClosestStops(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshClosestStops()");
		// IF the task is running DO
		if (this.closestStopsTask != null && this.closestStopsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.closestStopsTask.cancel(true);
			this.closestStopsTask = null;
		} else {
			refreshClosestStops();
		}
	}

}

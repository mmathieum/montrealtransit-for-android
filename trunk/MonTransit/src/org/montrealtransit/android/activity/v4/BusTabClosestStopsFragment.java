package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.PrefetchingUtils;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.services.ClosestBusStopsFinderTask;
import org.montrealtransit.android.services.ClosestBusStopsFinderTask.ClosestBusStopsFinderListener;
import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

@TargetApi(4)
public class BusTabClosestStopsFragment extends Fragment implements LocationListener, ClosestBusStopsFinderListener, OnItemClickListener, OnScrollListener,
		SensorEventListener, CompassListener {
	/**
	 * The log tag.
	 */
	private static final String TAG = BusTabClosestStopsFragment.class.getSimpleName();

	/**
	 * The closest stops.
	 */
	private List<ABusStop> closestStops;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the compass updates enabled?
	 */
	private boolean compassUpdatesEnabled = false;
	/**
	 * Store the device location.
	 */
	private Location location;

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
		ListView closestStopsListView = (ListView) view.findViewById(R.id.closest_stops);
		closestStopsListView.setOnItemClickListener(this);
		closestStopsListView.setOnScrollListener(this);
		this.closestStopsAdapter = new BusStopArrayAdapter(activity, R.layout.bus_tab_closest_stops_list_item);
		closestStopsListView.setAdapter(this.closestStopsAdapter);
		if (this.closestStops == null) {
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
		if (this.closestStops == null) {
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
		updateDistancesWithNewLocation();
	}

	/**
	 * Update the bike stations distances with the new location.
	 */
	private void updateDistancesWithNewLocation() {
		MyLog.v(TAG, "updateDistancesWithNewLocation()");
		// IF no closest bike stations AND new location DO
		Location currentLocation = getLocation();
		if (this.closestStops == null && currentLocation != null) {
			// start refreshing if not running.
			refreshClosestStops();
			return;
		}
		// ELSE IF there are closest stations AND new location DO
		if (this.closestStops != null && currentLocation != null) {
			LocationUtils.updateDistance(getLastActivity(), BusTabClosestStopsFragment.this.closestStops, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					notifyDataSetChanged(false);
				};
			});
		}
	}

	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<ABusStop> closestStopsAdapter;

	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;
	/**
	 * The list scroll state.
	 */
	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.closestStopsAdapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > Utils.ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.closestStopsAdapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	/**
	 * The task used to find the closest stations.
	 */
	private ClosestBusStopsFinderTask closestStopsTask;
	/**
	 * The location used to generate the closest stops.
	 */
	private Location closestStopsLocation;
	/**
	 * The location address used to generate the closest stops.
	 */
	protected String closestStopsLocationAddress;

	/**
	 * Start the refresh closest stops tasks if necessary.
	 */
	private void refreshClosestStops() {
		MyLog.v(TAG, "refreshClosestStops()");
		// IF the task is NOT already running DO
		if (this.closestStopsTask == null || !this.closestStopsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setClosestStopsLoading();
			// IF location found DO
			Location currentLocation = getLocation();
			if (currentLocation == null) {
				// MyLog.d(TAG, "no location yet...");
				return;
			}
			// find the closest stations
			this.closestStopsTask = new ClosestBusStopsFinderTask(this, getLastActivity(), SupportFactory.get().getNbClosestPOIDisplay());
			this.closestStopsTask.execute(currentLocation);
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

	/**
	 * Show new closest stops title.
	 */
	public void showNewClosestStopsTitle(View view, Context context) {
		if (this.closestStopsLocationAddress != null && this.closestStopsLocation != null && context != null && view != null) {
			View closestStopsLayout = view.findViewById(R.id.closest_stops_layout);
			((TextView) closestStopsLayout.findViewById(R.id.title_text)).setText(this.closestStopsLocationAddress);
		}
	}

	@Override
	public void onClosestStopsProgress(String message) {
		MyLog.v(TAG, "onClosestStopsProgress(%s)", message);
		// do nothing
	}

	@Override
	public void onClosestStopsDone(ClosestPOI<ABusStop> result) {
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
			this.closestStops = result.getPoiList();
			// generateOrderedStopCodes();
			prefetchClosestStops();
			refreshFavoriteUIDsFromDB();
			// show the result
			showNewClosestStops(view, activity, LocationUtils.areTheSame(this.closestStopsLocation, result.getLat(), result.getLng()));
			setClosestStopsNotLoading(view);
		}
	}

	private void prefetchClosestStops() {
		if (this.closestStops == null) {
			return;
		}
		for (int i = 0; i < this.closestStops.size() && i < 5; i++) {
			SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(getLastActivity(), this.closestStops.get(i), null, true, false),
					PrefetchingUtils.getExecutor());
		}
	}

	/**
	 * Show the new closest stops.
	 */
	private void showNewClosestStops(View view, Context activity, boolean scroll) {
		MyLog.v(TAG, "showNewClosestStops(%s)", scroll);
		if (this.closestStops != null && view != null && activity != null) {
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
			notifyDataSetChanged(true);
			ListView closestStopsListView = (ListView) view.findViewById(R.id.closest_stops);
			if (scroll) {
				SupportFactory.get().listViewScrollTo(closestStopsListView, 0, 0);
			}
			closestStopsListView.setVisibility(View.VISIBLE);
			setClosestStopsNotLoading(view);
		}
	}

	static class ViewHolder {
		TextView stopCodeTv;
		TextView labelTv;
		TextView lineNumberTv;
		TextView lineDirectionTv;
		TextView distanceTv;
		ImageView favImg;
		ImageView compassImg;
	}

	/**
	 * A custom array adapter with custom {@link BusStopArrayAdapter#getView(int, View, ViewGroup)}
	 */
	private class BusStopArrayAdapter extends ArrayAdapter<ABusStop> {

		/**
		 * The layout inflater.
		 */
		private LayoutInflater layoutInflater;
		/**
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 */
		public BusStopArrayAdapter(Context context, int viewId) {
			super(context, viewId);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return BusTabClosestStopsFragment.this.closestStops == null ? 0 : BusTabClosestStopsFragment.this.closestStops.size();
		}

		@Override
		public int getPosition(ABusStop item) {
			return BusTabClosestStopsFragment.this.closestStops == null ? 0 : BusTabClosestStopsFragment.this.closestStops.indexOf(item);
		}

		@Override
		public ABusStop getItem(int position) {
			return BusTabClosestStopsFragment.this.closestStops == null ? null : BusTabClosestStopsFragment.this.closestStops.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
				holder.labelTv = (TextView) convertView.findViewById(R.id.label);
				holder.lineNumberTv = (TextView) convertView.findViewById(R.id.line_number);
				holder.lineDirectionTv = (TextView) convertView.findViewById(R.id.line_direction);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ABusStop stop = getItem(position);
			if (stop != null) {
				// bus stop code
				holder.stopCodeTv.setText(stop.getCode());
				// bus stop place
				holder.labelTv.setText(BusUtils.cleanBusStopPlace(stop.getPlace()));
				// bus stop line number
				holder.lineNumberTv.setText(stop.getLineNumber());
				holder.lineNumberTv.setBackgroundColor(BusUtils.getBusLineTypeBgColor(stop.getLineTypeOrNull(), stop.getLineNumber()));
				// bus stop line direction
				int busLineDirection = BusUtils.getBusLineSimpleDirection(stop.getDirectionId());
				holder.lineDirectionTv.setText(getString(busLineDirection).toUpperCase(Locale.getDefault()));
				// distance
				if (!TextUtils.isEmpty(stop.getDistanceString())) {
					holder.distanceTv.setText(stop.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE);
					holder.distanceTv.setText(null);
				}
				// compass
				if (location != null && lastCompassInDegree != 0 && location.getAccuracy() <= stop.getDistance()) {
					float compassRotation = SensorUtils.getCompassRotationInDegree(location, stop, lastCompassInDegree, locationDeclination);
					SupportFactory.get().rotateImageView(holder.compassImg, compassRotation, getLastActivity());
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE);
				}
				// favorite
				if (BusTabClosestStopsFragment.this.favUIDs != null && BusTabClosestStopsFragment.this.favUIDs.contains(stop.getUID())) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// // closest bike station
				// int index = -1;
				// if (BikeTab.this.orderedStationsIds != null) {
				// index = BikeTab.this.orderedStationsIds.indexOf(bikeStation.getTerminalName());
				// }
				// switch (index) {
				// case 0:
				// ((TextView) convertView.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT_BOLD);
				// distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
				// distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
				// holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
				// break;
				// default:
				// ((TextView) convertView.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT);
				// distanceTv.setTypeface(Typeface.DEFAULT);
				// distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
				// holder.compassImg.setImageResource(R.drawable.heading_arrow);
				// break;
				// }
			}
			return convertView;
		}
	}

	/**
	 * The favorites bus stops UIDs.
	 */
	private List<String> favUIDs;

	private float locationDeclination;

	/**
	 * Find favorites bus stops UIDs.
	 */
	private void refreshFavoriteUIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getLastActivity().getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false; // don't trigger update if favorites are the same
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BusTabClosestStopsFragment.this.favUIDs)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavUIDs = new ArrayList<String>();
				for (Fav busStopFav : result) {
					String UID = BusStop.getUID(busStopFav.getFkId(), busStopFav.getFkId2());
					if (BusTabClosestStopsFragment.this.favUIDs == null || !BusTabClosestStopsFragment.this.favUIDs.contains(UID)) {
						newFav = true; // new favorite
					}
					newfavUIDs.add(UID); // store UID
				}
				BusTabClosestStopsFragment.this.favUIDs = newfavUIDs;
				// trigger change if necessary
				if (newFav) {
					notifyDataSetChanged(true);
				}
			};
		}.execute();
	}

	/**
	 * Set the closest stations as error.
	 */
	private void setClosestStopsError(View view, Context activity) {
		MyLog.v(TAG, "setClosestStopsError()");
		// IF there are already stations DO
		if (this.closestStops != null) {
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
	private void setClosestStopsLoading() {
		MyLog.v(TAG, "setClosestStationsLoading()");
		View closestStopsLayout = getLastView().findViewById(R.id.closest_stops_layout);
		if (this.closestStops == null) {
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
			detailMsgTv.setText(R.string.waiting_for_location_fix);
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
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerShakeAndCompassListener(getLastActivity(), this);
					// SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
					// this.shakeHandled = false;
				}
			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForCompass(getLastActivity(), se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];

	/**
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		if (this.closestStops == null) {
			// MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							BusTabClosestStopsFragment.this.lastCompassInDegree = (int) orientation;
							BusTabClosestStopsFragment.this.lastCompassChanged = now;
							// update the view
							notifyDataSetChanged(false);
						}
					}
				});
	}

	private boolean paused = false;

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (getLastView() != null && view == getLastView().findViewById(R.id.list)) {
			this.scrollState = scrollState;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s, %s,%s,%s)", parent.getId(), view.getId(), position, id);
		if (this.closestStops != null && position < this.closestStops.size() && this.closestStops.get(position) != null) {
			Intent intent = new Intent(getLastActivity(), BusStopInfo.class);
			BusStop selectedStop = this.closestStops.get(position);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, selectedStop.getCode());
			intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, selectedStop.getPlace());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, selectedStop.getLineNumber());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, selectedStop.getLineNameOrNull());
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, selectedStop.getLineTypeOrNull());
			startActivity(intent);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
	public void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
	}

	@Override
	public void onPause() {
		MyLog.v(TAG, "onPause()");
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(getLastActivity(), this, this.locationUpdatesEnabled);
		SensorUtils.unregisterSensorListener(getLastActivity(), this);
		this.compassUpdatesEnabled = false;
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
								BusTabClosestStopsFragment.this.closestStops = null; // force refresh
							}
						}
						// set the new distance
						setLocation(result);
						updateDistancesWithNewLocation();
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
			// refreshSubwayStatus();
			refreshClosestStops();
		}
	}

	@Override
	public void onStop() {
		MyLog.v(TAG, "onStop()");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		MyLog.v(TAG, "onDetach()");
		super.onDetach();
	}

}

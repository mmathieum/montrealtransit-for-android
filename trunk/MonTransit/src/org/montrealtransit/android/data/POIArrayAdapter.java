package org.montrealtransit.android.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.montrealtransit.android.BikeUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BikeStationInfo;
import org.montrealtransit.android.activity.RouteInfo;
import org.montrealtransit.android.activity.StopInfo;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class POIArrayAdapter extends ArrayAdapter<POI> implements CompassListener, OnItemClickListener, OnItemLongClickListener, SensorEventListener,
		OnScrollListener {

	public static final String TAG = POIArrayAdapter.class.getSimpleName();

	private LayoutInflater layoutInflater;

	private List<? extends POI> pois;

	private SparseArray<Set<String>> typeFavUIDs;

	private Activity activity;

	private Location location;

	private int lastCompassInDegree = -1;

	private float locationDeclination;

	private Pair<Integer, String> closestPOI;

	private int lastSuccessfulRefresh = -1;

	private float[] accelerometerValues = new float[3];

	private float[] magneticFieldValues = new float[3];

	private Bundle intentExtras;

	private boolean showData = false;

	private ViewGroup manualLayout;

	private long lastNotifyDataSetChanged = -1;

	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

	private ScrollView manualScrollView;

	private boolean compassUpdatesEnabled = false;

	private boolean shakeEnabled = false;

	private long lastCompassChanged = -1;

	public POIArrayAdapter(Activity activity) {
		super(activity, R.layout.loading_small_layout);
		MyLog.v(TAG, "POIArrayAdapter()");
		this.activity = activity;
		this.layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void setManualLayout(ViewGroup manualLayout) {
		this.manualLayout = manualLayout;
	}

	public void setIntentExtras(Bundle intentExtras) {
		this.intentExtras = intentExtras;
	}

	public void setShowData(boolean showData) {
		this.showData = showData;
	}

	@Override
	public int getItemViewType(int position) {
		// MyLog.v(TAG, "getItemViewType(%s)", position);
		return getItemViewType(getItem(position));
	}

	public int getItemViewType(POI poi) {
		// MyLog.v(TAG, "getItemViewType(%s)", position);
		if (poi == null) {
			MyLog.d(TAG, "Cannot find type for object null!");
			return -1;
		}
		return poi.getType();
	}

	@Override
	public int getCount() {
		return pois == null ? 0 : pois.size();
	}

	@Override
	public int getPosition(POI item) {
		return pois == null ? 0 : pois.indexOf(item);
	}

	@Override
	public POI getItem(int position) {
		return pois == null ? null : pois.get(position);
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getView(%s)", position);
		switch (getItemViewType(position)) {
		case POI.ITEM_VIEW_TYPE_STOP:
			return getRouteTripStopView(position, convertView, parent);
		case POI.ITEM_VIEW_TYPE_BIKE:
			return getBikeView(position, convertView, parent);
		default:
			MyLog.w(TAG, "Unknow view type at position %s!", position);
			return null; // FIXME CRASH!!!
		}
	}

	public View updateView(int position, View convertView) {
		// MyLog.v(TAG, "updateView(%s)", position);
		switch (getItemViewType(position)) {
		case POI.ITEM_VIEW_TYPE_STOP:
			return updateRouteTripStopView(position, convertView);
		case POI.ITEM_VIEW_TYPE_BIKE:
			return updateBikeView(position, convertView);
		default:
			MyLog.w(TAG, "Unknow view type at position %s!", position);
			return null; // FIXME CRASH!!!
		}
	}

	public void updateCommonView(int position, View convertView) {
		// MyLog.v(TAG, "updateCommonView(%s)", position);
		if (convertView == null || convertView.getTag() == null || !(convertView.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) convertView.getTag();
		POI poi = getPoi(position);
		updateCommonView(holder, poi);
		return;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemClick(%s,%s)", position, id);
		showPoiViewerActivity(position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		MyLog.v(TAG, "onItemLongClick(%s,%s)", position, id);
		return showPoiMenu(position);
	}

	public boolean showClosestPOI() {
		if (!hasClosestPOI()) {
			return false;
		}
		return showPoiViewerActivity(indexOfPoi(this.closestPOI.second));
	}

	public boolean showPoiViewerActivity(int position) {
		MyLog.v(TAG, "showPoiViewerActivity(%s)", position);
		if (this.pois != null && position < this.pois.size() && this.pois.get(position) != null) {
			return showPoiViewerActivity(this.pois.get(position));
		}
		return false;
	}

	public boolean showPoiMenu(int position) {
		MyLog.v(TAG, "showPoiMenu(%s)", position);
		if (this.pois != null && position < this.pois.size() && this.pois.get(position) != null) {
			return showPoiMenu(this.pois.get(position));
		}
		return false;
	}

	public boolean showPoiViewerActivity(final POI poi) {
		MyLog.v(TAG, "showPoiViewerActivity(%s)", poi);
		if (poi == null) {
			return false;
		}
		final Intent intent;
		switch (getItemViewType(poi)) {
		case POI.ITEM_VIEW_TYPE_STOP:
			if (poi instanceof RouteTripStop) {
				intent = StopInfo.newInstance(this.activity, (RouteTripStop) poi);
			} else if (poi instanceof RouteStop) {
				intent = StopInfo.newInstance(this.activity, (RouteStop) poi);
			} else { // if (poi instanceof TripStop) {
				intent = StopInfo.newInstance(this.activity, (TripStop) poi);
				// } else {
				// intent = StopInfo.newInstance(this.activity, (Stop) poi);
			}
			break;
		case POI.ITEM_VIEW_TYPE_BIKE:
			intent = BikeStationInfo.newInstance(this.activity, (BikeStation) poi);
			break;
		default:
			MyLog.w(TAG, "Unknow view type for poi %s!", poi);
			return false;
		}
		if (this.intentExtras != null) {
			intent.putExtras(this.intentExtras);
		}
		this.activity.startActivity(intent);
		return true;
	}

	public boolean showPoiMenu(final POI poi) {
		MyLog.v(TAG, "showPoiMenu(%s)", poi);
		if (poi == null) {
			return false;
		}
		switch (getItemViewType(poi)) {
		case POI.ITEM_VIEW_TYPE_STOP:
			final Route route;
			final Trip trip;
			final Stop stop;
			final String authority;
			if (poi instanceof RouteTripStop) {
				final RouteTripStop routeTripStop = (RouteTripStop) poi;
				route = routeTripStop.route;
				trip = routeTripStop.trip;
				stop = routeTripStop.stop;
				authority = routeTripStop.authority;
			} else if (poi instanceof RouteStop) {
				final RouteStop routeStop = (RouteStop) poi;
				route = routeStop.route;
				trip = routeStop.trip; // already null
				stop = routeStop.stop;
				authority = routeStop.authority;
			} else if (poi instanceof TripStop) {
				final TripStop tripStop = (TripStop) poi;
				route = null;
				trip = tripStop.trip;
				stop = tripStop.stop;
				authority = tripStop.authority;
			} else {
				return false;
			}
			StringBuilder title = new StringBuilder(stop.name);
			if (!TextUtils.isEmpty(stop.code)) {
				title.append(" (").append(stop.code).append(")");
			}
			final Integer routeId = route == null ? (trip == null ? null : trip.routeId) : route.id;
			final Integer tripId = trip == null ? null : trip.id;
			final Integer stopId = stop == null ? null : stop.id;
			final Fav findFav = DataManager.findFav(this.activity.getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP, poi.getUID());
			final boolean isFav = findFav != null;
			new AlertDialog.Builder(this.activity)
					.setTitle(title)
					.setItems(
							new CharSequence[] { this.activity.getString(R.string.view_stop), this.activity.getString(R.string.view_stop_route),
									isFav ? this.activity.getString(R.string.remove_fav) : this.activity.getString(R.string.add_fav) },
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									MyLog.v(TAG, "onClick(%s)", item);
									switch (item) {
									case 0:
										showPoiViewerActivity(poi);
										break;
									case 1:
										POIArrayAdapter.this.activity.startActivity(RouteInfo.newInstance(POIArrayAdapter.this.activity, authority, routeId,
												tripId, stopId));
										break;
									case 2:
										if (isFav) { // remove favorite
											boolean success = DataManager.deleteFav(POIArrayAdapter.this.activity.getContentResolver(), findFav.getId());
											if (success) {
												Utils.notifyTheUser(POIArrayAdapter.this.activity,
														POIArrayAdapter.this.activity.getString(R.string.favorite_removed));
											} else {
												MyLog.w(TAG, "Favorite not deleted!");
											}
										} else { // add favorite
											Fav newFav = new Fav();
											newFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
											newFav.setFkId(poi.getUID());
											boolean success = DataManager.addFav(POIArrayAdapter.this.activity.getContentResolver(), newFav) != null;
											if (success) {
												Utils.notifyTheUser(POIArrayAdapter.this.activity,
														POIArrayAdapter.this.activity.getString(R.string.favorite_added));
												UserPreferences.savePrefLcl(POIArrayAdapter.this.activity, UserPreferences.PREFS_LCL_IS_FAV, true);
											} else {
												MyLog.w(TAG, "Favorite not added!");
											}
										}
										SupportFactory.get().backupManagerDataChanged(POIArrayAdapter.this.activity);
										notifyDataSetChanged(true);
										break;
									default:
										break;
									}
								}
							}).create().show();
			return true;
		default:
			MyLog.w(TAG, "Unknow view type for poi %s!", poi);
			return false;
		}
	}

	public void setPois(List<? extends POI> pois) {
		MyLog.v(TAG, "setPois(%s)", pois == null ? null : pois.size());
		this.pois = pois;
	}

	public List<? extends POI> getPois() {
		return pois;
	}

	public POI getPoi(int position) {
		if (this.pois == null) {
			return null;
		}
		if (position >= this.pois.size()) {
			MyLog.d(TAG, "getPoi(%s) > no item at this position!", position);
			return null;
		}
		return this.pois.get(position);
	}

	public int getPoisCount() {
		if (pois == null) {
			return 0;
		}
		return pois.size();
	}

	public boolean hasPois() {
		return getPoisCount() > 0;
	}

	private void updateClosestPoi() {
		MyLog.v(TAG, "updateClosestPoi()");
		if (!this.shakeEnabled) {
			this.closestPOI = null;
			// MyLog.d(TAG, "updateClosestPoi() > shake not enabled!");
			return;
		}
		if (getPoisCount() == 0) {
			this.closestPOI = null;
			// MyLog.d(TAG, "updateClosestPoi() > 0 poi!");
			return;
		}
		List<POI> orderedPois = new ArrayList<POI>(this.pois);
		// order the POIs list by distance (closest first)
		Collections.sort(orderedPois, POI.POI_DISTANCE_COMPARATOR);
		if (orderedPois.get(0).getDistance() > 0) {
			// MyLog.d(TAG, "updateClosestPoi() > found (%s)", orderedPois.get(0).getUID());
			this.closestPOI = new Pair<Integer, String>(getItemViewType(orderedPois.get(0)), orderedPois.get(0).getUID());
		} else {
			MyLog.d(TAG, "updateClosestPoi() > no distance! (%s)", orderedPois.get(0).getDistance());
			this.closestPOI = null;
		}
	}

	public boolean hasClosestPOI() {
		return closestPOI != null && closestPOI.first != null && closestPOI.second != null;
	}

	public boolean isClosestPOI(int position) {
		if (this.closestPOI == null) {
			return false;
		}
		final POI poi = getPoi(position);
		if (poi == null) {
			return false;
		}
		return this.closestPOI.first.equals(getItemViewType(poi)) && this.closestPOI.second.equals(poi.getUID());
	}

	public POI findPoi(String uid) {
		if (this.pois != null) {
			for (final POI poi : this.pois) {
				if (poi.getUID().equals(uid)) {
					return poi;
				}
			}
		}
		return null;
	}

	public int indexOfPoi(String uid) {
		if (this.pois != null) {
			for (int i = 0; i < pois.size(); i++) {
				if (pois.get(i).getUID().equals(uid)) {
					return i;
				}
			}
		}
		return -1;
	}

	@Deprecated
	public void prefetchClosests() {
		if (this.pois == null) {
			return;
		}
		// TODO for (int i = 0; i < this.closestStops.size() && i < 5; i++) {
		// SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(getLastActivity(), this.closestStops.get(i), null, true, false),
		// PrefetchingUtils.getExecutor());
		// }
	}

	@Deprecated
	public void prefetchFavorites() {
		if (this.pois == null || this.typeFavUIDs == null) {
			return;
		}
		// TODO for (String code : this.favStopCodes) {
		// RouteTripStop routeTripStop = new RouteTripStop(null, null, null);
		// routeTripStop.stop.code = code;
		// routeTripStop.route.shortName = this.busLineNumber;
		// SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(getLastActivity(), busStop, null, true, false),
		// PrefetchingUtils.getExecutor());
		// }
	}

	public void setLocationDeclination(float locationDeclination) {
		this.locationDeclination = locationDeclination;
	}

	private void updateDistances(Location currentLocation) {
		MyLog.v(TAG, "updateDistances()");
		// MyLog.d(TAG, "updateDistances() > currentLocation: %s", currentLocation);
		if (this.pois != null && currentLocation != null) {
			LocationUtils.updateDistanceWithString(this.activity, this.pois, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					MyLog.v(TAG, "updateDistances() > onLocationTaskCompleted()");
					final Pair<Integer, String> previousClosest = POIArrayAdapter.this.closestPOI;
					updateClosestPoi();
					final boolean newClosest = POIArrayAdapter.this.closestPOI == null ? false : POIArrayAdapter.this.closestPOI.equals(previousClosest);
					notifyDataSetChanged(newClosest);
					prefetchClosests();
				}
			});
		}
	}

	public void updateDistancesNow(Location currentLocation) {
		MyLog.v(TAG, "updateDistancesNow()");
		// MyLog.d(TAG, "updateDistancesNow() > location: %s", this.location);
		// MyLog.d(TAG, "updateDistancesNow() > compassUpdatesEnabled: %s", this.compassUpdatesEnabled);
		if (this.pois != null && currentLocation != null) {
			LocationUtils.updateDistanceWithString(this.activity, this.pois, currentLocation);
			updateClosestPoi();
		}
		if (this.location == null) { // TODO always?
			setLocation(currentLocation);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		setScrollState(scrollState);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	public void setScrollState(int scrollState) {
		// MyLog.v(TAG, "setScrollState(%s)", scrollState);
		this.scrollState = scrollState;
	}

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.scrollState == OnScrollListener.SCROLL_STATE_IDLE && (force || (now - this.lastNotifyDataSetChanged) > Utils.ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			notifyDataSetChanged();
			notifyDataSetChangedManual();
			this.lastNotifyDataSetChanged = now;
		}
	}

	private void notifyDataSetChangedManual() {
		// MyLog.v(TAG, "notifyDataSetChangedManual()");
		if (this.manualLayout != null && hasPois()) {
			int position = 0;
			for (int i = 0; i < this.manualLayout.getChildCount(); i++) {
				View view = this.manualLayout.getChildAt(i);
				Object tag = view.getTag();
				if (tag != null && tag instanceof CommonViewHolder) {
					updateCommonView(position, view);
					position++;
				}
			}
		}
	}

	public void setListView(ListView listView) {
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setOnScrollListener(this);
		listView.setAdapter(this);
	}

	public void initManual() {
		MyLog.v(TAG, "initManual()");
		if (this.manualLayout != null && hasPois()) {
			// clear the previous list
			this.manualLayout.removeAllViews();
			// show stations list
			for (int i = 0; i < getPoisCount(); i++) {
				// list view divider
				if (this.manualLayout.getChildCount() > 0) {
					this.manualLayout.addView(this.layoutInflater.inflate(R.layout.list_view_divider, this.manualLayout, false));
				}
				// create view
				View view = getView(i, null, this.manualLayout);
				// add click listener
				final int position = i;
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick(%s)", position);
						showPoiViewerActivity(position);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						MyLog.v(TAG, "onLongClick(%s)", position);
						return showPoiMenu(position);
					}
				});
				this.manualLayout.addView(view);
			}
		}
	}

	public void scrollManualScrollViewTo(int x, int y) {
		if (this.manualScrollView != null) {
			this.manualScrollView.scrollTo(x, y);
		}
	}

	public void setManualScrollView(ScrollView scrollView) {
		this.manualScrollView = scrollView;
		if (scrollView == null) {
			return;
		}
		scrollView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// MyLog.v(TAG, "onTouch(%s)", event);
				switch (event.getAction()) {
				case MotionEvent.ACTION_SCROLL:
				case MotionEvent.ACTION_MOVE:
					setScrollState(OnScrollListener.SCROLL_STATE_FLING);
					break;
				case MotionEvent.ACTION_DOWN:
					setScrollState(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					// scroll view can still by flying
					setScrollState(OnScrollListener.SCROLL_STATE_IDLE);
					break;
				default:
					MyLog.v(TAG, "Unexpected event %s", event);
				}
				return false;
			}
		});
	}

	public void setShakeEnabled(boolean shakeEnabled) {
		this.shakeEnabled = shakeEnabled;
	}

	public void setLocation(Location newLocation) {
		// MyLog.v(TAG, "setLocation()");
		if (newLocation != null) {
			MyLog.d(TAG, "new location: %s.", LocationUtils.locationToString(newLocation));
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					// shake handled on the activity level (1 shake / activity)
					SensorUtils.registerCompassListener(this.activity, this);
					this.compassUpdatesEnabled = true;
				}
				updateDistances(this.location);
			}
		}
	}

	public void onPause() {
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this.activity, this);
			this.compassUpdatesEnabled = false;
		}
	}

	public void onResume() {
		if (!this.compassUpdatesEnabled) {
			// shake handled on the activity level (1 shake / activity)
			SensorUtils.registerCompassListener(this.activity, this);
			this.compassUpdatesEnabled = true;
		}
	}

	// public void setLastCompassInDegree(int lastCompassInDegree) {
	// MyLog.v(TAG, "setLastCompassInDegree(%s)", lastCompassInDegree);
	// this.lastCompassInDegree = lastCompassInDegree;
	// }

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		if (this.pois == null) {
			// MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, this.location, orientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							POIArrayAdapter.this.lastCompassInDegree = (int) orientation;
							POIArrayAdapter.this.lastCompassChanged = now;
							// update the view
							notifyDataSetChanged(false);
						}
					}
				});
	}

	public void updateCompassNow() {
		notifyDataSetChanged(true); // TODO really?
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForCompass(this.activity, se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged(%s)", accuracy);
	}

	private View getBikeView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getBikeView(%s)", position);
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.poi_list_bike_station_item, parent, false);
			BikeViewHolder holder = new BikeViewHolder();
			holder.uid = null;
			holder.stopNameTv = (TextView) convertView.findViewById(R.id.station_name);
			holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
			holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
			holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
			holder.progressBar = (ProgressBar) convertView.findViewById(R.id.availability).findViewById(R.id.progress_bar);
			holder.dockTv = (TextView) convertView.findViewById(R.id.availability).findViewById(R.id.progress_dock);
			holder.bikeTv = (TextView) convertView.findViewById(R.id.availability).findViewById(R.id.progress_bike);
			convertView.setTag(holder);
		}
		updateBikeView(position, convertView);
		return convertView;
	}

	private View updateBikeView(int position, View convertView) {
		// MyLog.v(TAG, "updateBikeView(%s)", position);
		if (convertView == null) {
			return convertView;
		}
		BikeViewHolder holder = (BikeViewHolder) convertView.getTag();
		final POI poi = getItem(position);
		if (poi != null) {
			ABikeStation bikeStation = (ABikeStation) poi;
			// bike station name
			holder.stopNameTv.setText(Utils.cleanBikeStationName(bikeStation.getName()));
			// status (not installed, locked..)
			if (!bikeStation.isInstalled() || bikeStation.isLocked()) {
				holder.stopNameTv.setTextColor(Utils.getTextColorSecondary(getContext()));
			} else {
				holder.stopNameTv.setTextColor(Utils.getTextColorPrimary(getContext()));
			}
			// availability
			if (showData) {
				final boolean dataUseful;
				if (this.lastSuccessfulRefresh < 0) {
					// MyLog.d(TAG, "updateBikeView(%s) > no last successful data!", position);
					dataUseful = false;
				} else {
					// final int lastUpdate = this.lastSuccessfulRefresh > 0 ? this.lastSuccessfulRefresh : getLastUpdateTime();
					int waitFor = Utils.currentTimeSec() - BikeUtils.CACHE_NOT_USEFUL_IN_SEC - this.lastSuccessfulRefresh;
					dataUseful = waitFor < 0;
				}
				if (dataUseful) {
					if (!bikeStation.isInstalled()) {
						holder.bikeTv.setText(R.string.bike_station_not_installed);
						holder.bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
						holder.dockTv.setVisibility(View.GONE);
					} else if (bikeStation.isLocked()) {
						holder.bikeTv.setText(R.string.bike_station_locked);
						holder.bikeTv.setTypeface(Typeface.DEFAULT_BOLD);
						holder.dockTv.setVisibility(View.GONE);
					} else {
						// bikes #
						holder.bikeTv
								.setText(activity.getResources().getQuantityString(R.plurals.bikes_nb, bikeStation.getNbBikes(), bikeStation.getNbBikes()));
						holder.bikeTv.setTypeface(bikeStation.getNbBikes() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
						holder.bikeTv.setVisibility(View.VISIBLE);
						// dock #
						holder.dockTv.setText(activity.getResources().getQuantityString(R.plurals.docks_nb, bikeStation.getNbEmptyDocks(),
								bikeStation.getNbEmptyDocks()));
						holder.dockTv.setTypeface(bikeStation.getNbEmptyDocks() <= 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
						holder.dockTv.setVisibility(View.VISIBLE);
					}
					// progress bar
					holder.progressBar.setIndeterminate(false);
					holder.progressBar.setMax(bikeStation.getNbTotalDocks());
					holder.progressBar.setProgress(bikeStation.getNbBikes());
					holder.progressBar.setVisibility(View.VISIBLE);
				} else {
					// TODO trigger refresh?
					holder.bikeTv.setText(R.string.ellipsis);
					holder.bikeTv.setTypeface(Typeface.DEFAULT);
					holder.bikeTv.setVisibility(View.VISIBLE);
					holder.dockTv.setText(R.string.ellipsis);
					holder.dockTv.setTypeface(Typeface.DEFAULT);
					holder.dockTv.setVisibility(View.VISIBLE);
					holder.progressBar.setIndeterminate(true);
					holder.progressBar.setVisibility(View.VISIBLE);
				}
			} else {
				holder.bikeTv.setVisibility(View.GONE);
				holder.dockTv.setVisibility(View.GONE);
				holder.progressBar.setVisibility(View.GONE);
			}
			// setup distance, compass, favorite, closest
			updateCommonView(holder, poi);
		}
		return convertView;
	}

	public void setLastSuccessfulRefresh(int lastSuccessfulRefresh) {
		MyLog.v(TAG, "setLastSuccessfulRefresh(%s)", lastSuccessfulRefresh);
		this.lastSuccessfulRefresh = lastSuccessfulRefresh;
	}

	public int getLastSuccessfulRefresh() {
		return lastSuccessfulRefresh;
	}

	private View getRouteTripStopView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getRouteTripStopView(%s)", position);
		RouteTripStopViewHolder holder;
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.poi_list_route_trip_stop_item, parent, false);
			holder = new RouteTripStopViewHolder();
			holder.uid = null;
			holder.stopNameTv = (TextView) convertView.findViewById(R.id.stop_name);
			holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
			holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
			holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
			holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
			holder.routeFL = convertView.findViewById(R.id.route);
			holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
			holder.routeTypeImg = (ImageView) convertView.findViewById(R.id.route_type_img);
			holder.tripHeadingTv = (TextView) convertView.findViewById(R.id.trip_heading);
			convertView.setTag(holder);
		}
		updateRouteTripStopView(position, convertView);
		return convertView;
	}

	private View updateRouteTripStopView(int position, View convertView) {
		// MyLog.v(TAG, "updateRouteTripStopView(%s)", position);
		if (convertView == null) {
			return convertView;
		}
		RouteTripStopViewHolder holder = (RouteTripStopViewHolder) convertView.getTag();
		final POI poi = getItem(position);
		if (poi != null) {
			final boolean isSubway;
			final Route route;
			final Trip trip;
			final Stop stop;
			if (poi instanceof RouteTripStop) {
				final RouteTripStop routeTripStop = (RouteTripStop) poi;
				route = routeTripStop.route;
				trip = routeTripStop.trip;
				stop = routeTripStop.stop;
				isSubway = routeTripStop.authority.contains("subway");
			} else if (poi instanceof RouteStop) {
				final RouteStop routeStop = (RouteStop) poi;
				route = routeStop.route;
				trip = routeStop.trip; // already null
				stop = routeStop.stop;
				isSubway = routeStop.authority.contains("subway");
			} else if (poi instanceof TripStop) {
				final TripStop tripStop = (TripStop) poi;
				route = null;
				trip = tripStop.trip;
				stop = tripStop.stop;
				isSubway = tripStop.authority.contains("subway");
			} else {
				route = null;
				trip = null;
				stop = (Stop) poi;
				isSubway = false;
			}
			// bus stop code
			if (TextUtils.isEmpty(stop.code)) {
				holder.stopCodeTv.setVisibility(View.GONE);
			} else {
				holder.stopCodeTv.setText(stop.code);
				holder.stopCodeTv.setVisibility(View.VISIBLE);
			}
			// bus stop place
			holder.stopNameTv.setText(BusUtils.cleanBusStopPlace(stop.name));
			// bus stop line number
			if (route == null) {
				holder.routeFL.setVisibility(View.GONE);
				holder.tripHeadingTv.setVisibility(View.GONE);
			} else {
				if (TextUtils.isEmpty(route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.GONE);
					holder.routeTypeImg.setVisibility(View.VISIBLE);
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(route.shortName);
					holder.routeShortNameTv.setTextColor(Utils.parseColor(route.textColor));
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setBackgroundColor(Utils.parseColor(route.color));
				holder.routeFL.setVisibility(View.VISIBLE);
				if (trip == null || isSubway) {
					holder.tripHeadingTv.setVisibility(View.GONE);
				} else {
					holder.tripHeadingTv.setText(trip.getHeading(this.activity).toUpperCase(Locale.getDefault()));
					holder.tripHeadingTv.setVisibility(View.VISIBLE);
				}
			}
			// setup distance, compass, favorite, closest
			updateCommonView(holder, poi);
		}
		return convertView;
	}

	private void updateCommonView(CommonViewHolder holder, POI poi) {
		// MyLog.v(TAG, "updateCommonView(%s,%s,%s)", type, uid, poi);
		if (poi == null) {
			return;
		}
		holder.uid = poi.getUID();
		// // distance
		if (!TextUtils.isEmpty(poi.getDistanceString())) {
			if (!poi.getDistanceString().equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(poi.getDistanceString());
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		// compass
		if (this.location != null && this.lastCompassInDegree != 0 && this.location.getAccuracy() <= poi.getDistance()) {
			float compassRotation = SensorUtils.getCompassRotationInDegree(location, poi, this.lastCompassInDegree, this.locationDeclination);
			SupportFactory.get().rotateImageView(holder.compassImg, compassRotation, this.activity);
			holder.compassImg.setVisibility(View.VISIBLE);
		} else {
			holder.compassImg.setVisibility(View.GONE);
		}
		// favorite
		if (this.typeFavUIDs != null && this.typeFavUIDs.get(poi.getType()) != null && this.typeFavUIDs.get(poi.getType()).contains(poi.getUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
		// closest POI
		if (this.shakeEnabled) {
			final int index;
			if (this.closestPOI != null && this.closestPOI.first == poi.getType() && !TextUtils.isEmpty(this.closestPOI.second)
					&& this.closestPOI.second.equals(poi.getUID())) {
				index = 0;
			} else {
				index = -1;
			}
			switch (index) {
			case 0:
				holder.stopNameTv.setTypeface(Typeface.DEFAULT_BOLD);
				holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
				holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
				holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
				break;
			default:
				holder.stopNameTv.setTypeface(Typeface.DEFAULT);
				holder.distanceTv.setTypeface(Typeface.DEFAULT);
				holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
				holder.compassImg.setImageResource(R.drawable.heading_arrow);
				break;
			}
		}
	}

	public void setFavs(List<Fav> favs) {
		MyLog.v(TAG, "setFavs(%s)", favs == null ? null : favs.size());
		boolean newFav = false; // don't trigger update if favorites are the same
		if (Utils.getCollectionSize(favs) != Utils.getCollectionSize(this.typeFavUIDs)) {
			newFav = true; // different size => different favorites
		}
		SparseArray<Set<String>> newTypeFavUIDs = new SparseArray<Set<String>>();
		if (favs != null) {
			for (Fav fav : favs) {
				switch (fav.getType()) {
				case Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP:
					if (newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_STOP) == null) {
						newTypeFavUIDs.put(POI.ITEM_VIEW_TYPE_STOP, new HashSet<String>());
					}
					final String uid = fav.getFkId();
					if (this.typeFavUIDs != null && this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_STOP) != null
							&& !this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_STOP).contains(uid)) {
						newFav = true;
					}
					newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_STOP).add(uid);
					break;
				case Fav.KEY_TYPE_VALUE_BIKE_STATIONS:
					if (newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_BIKE) == null) {
						newTypeFavUIDs.put(POI.ITEM_VIEW_TYPE_BIKE, new HashSet<String>());
					}
					if (this.typeFavUIDs != null && this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_BIKE) != null
							&& !this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_BIKE).contains(fav.getFkId())) {
						newFav = true;
					}
					newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_BIKE).add(fav.getFkId());
					break;
				default:
					MyLog.w(TAG, "Unknown favorite type ID %s!", fav.getType());
					break;
				}
			}
		}
		this.typeFavUIDs = newTypeFavUIDs;
		// trigger change if necessary
		if (newFav) {
			notifyDataSetChanged(true);
			prefetchFavorites();
		}
	}

	public static class RouteTripStopViewHolder extends CommonViewHolder {
		TextView stopCodeTv;
		TextView routeShortNameTv;
		View routeFL;
		ImageView routeTypeImg;
		TextView tripHeadingTv;
	}

	@Deprecated
	public static class SubwayViewHolder extends CommonViewHolder {
		ImageView subwayImg1;
		ImageView subwayImg2;
		ImageView subwayImg3;
	}

	public static class BikeViewHolder extends CommonViewHolder {
		ProgressBar progressBar;
		TextView dockTv;
		TextView bikeTv;
	}

	public static class CommonViewHolder {
		String uid;
		TextView stopNameTv;
		TextView distanceTv;
		ImageView favImg;
		ImageView compassImg;

		// @Override
		// public boolean equals(Object o) {
		// MyLog.d(TAG, "equals()");
		// if (o == null) {
		// return false;
		// }
		// CommonViewHolder vh = (CommonViewHolder) o;
		// MyLog.d(TAG, "equals() %s vs %s", uid, vh.uid);
		// if (uid != null && uid.equals(vh.uid)) {
		// return true;
		// }
		// return super.equals(o);
		// }
	}
}

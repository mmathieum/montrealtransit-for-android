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
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BikeStationInfo;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.activity.SubwayStationInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

public class POIArrayAdapter extends ArrayAdapter<POI> implements CompassListener, OnItemClickListener, SensorEventListener, OnScrollListener {

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
		return 3;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getView(%s)", position);
		switch (getItemViewType(position)) {
		case POI.ITEM_VIEW_TYPE_BUS:
			return getBusView(position, convertView, parent);
		case POI.ITEM_VIEW_TYPE_SUBWAY:
			return getSubwayView(position, convertView, parent);
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
		case POI.ITEM_VIEW_TYPE_BUS:
			return updateBusView(position, convertView);
		case POI.ITEM_VIEW_TYPE_SUBWAY:
			return updateSubwayView(position, convertView);
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

	public boolean showPoiViewerActivity(final POI poi) {
		MyLog.v(TAG, "showPoiViewerActivity(%s)", poi);
		if (poi == null) {
			return false;
		}
		final Intent intent;
		switch (getItemViewType(poi)) {
		case POI.ITEM_VIEW_TYPE_BUS:
			if (poi instanceof RouteTripStop) {
				intent = BusStopInfo.newInstance(this.activity, (RouteTripStop) poi);
			} else {
				intent = BusStopInfo.newInstance(this.activity, (TripStop) poi);
			}
			break;
		case POI.ITEM_VIEW_TYPE_SUBWAY:
			intent = SubwayStationInfo.newInstance(this.activity, (SubwayStation) poi);
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

	public void setPois(List<? extends POI> pois) {
		MyLog.v(TAG, "setPois(%s)", pois == null ? null : pois.size());
		this.pois = pois;
	}

	public List<? extends POI> getPois() {
		return pois;
	}

	public POI getPoi(int location) {
		if (this.pois == null) {
			return null;
		}
		if (location >= this.pois.size()) {
			MyLog.d(TAG, "getPoi(%s) > no item at this position!", location);
			return null;
		}
		return this.pois.get(location);
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

	public void updateClosestPoi() {
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
		Collections.sort(orderedPois, new POI.POIDistanceComparator());
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

	public boolean isClosestPOI(int location) {
		if (this.closestPOI == null) {
			return false;
		}
		final POI poi = getPoi(location);
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

	public void prefetchClosests() {
		if (this.pois == null) {
			return;
		}
		// TODO for (int i = 0; i < this.closestStops.size() && i < 5; i++) {
		// SupportFactory.get().executeOnExecutor(new LoadNextBusStopIntoCacheTask(getLastActivity(), this.closestStops.get(i), null, true, false),
		// PrefetchingUtils.getExecutor());
		// }
	}

	public void prefetchFavorites() {
		if (this.pois == null || this.typeFavUIDs == null) {
			return;
		}
		// TODO for (String code : this.favStopCodes) {
		// RouteTripStop busStop = new RouteTripStop(null, null, null);
		// busStop.stop.code = code;
		// busStop.route.shortName = this.busLineNumber;
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
			LocationUtils.updateDistance(this.activity, this.pois, currentLocation, new LocationTaskCompleted() {

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
		if (this.pois != null && currentLocation != null) {
			LocationUtils.updateDistanceWithString(this.activity, this.pois, currentLocation);
			updateClosestPoi();
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		setScrollState(this.scrollState);
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
						MyLog.v(TAG, "onClick(%s)", v.getId());
						// showNewBikeStation(terminalName, name);
						showPoiViewerActivity(position);
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

	public void setLastCompassInDegree(int lastCompassInDegree) {
		this.lastCompassInDegree = lastCompassInDegree;
	}

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
			holder.labelTv = (TextView) convertView.findViewById(R.id.station_name);
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
			holder.labelTv.setText(Utils.cleanBikeStationName(bikeStation.getName()));
			// status (not installed, locked..)
			if (!bikeStation.isInstalled() || bikeStation.isLocked()) {
				holder.labelTv.setTextColor(Utils.getTextColorSecondary(getContext()));
			} else {
				holder.labelTv.setTextColor(Utils.getTextColorPrimary(getContext()));
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

	private View getSubwayView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getSubwayView(%s)", position);
		SubwayViewHolder holder;
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.poi_list_subway_station_item, parent, false);
			holder = new SubwayViewHolder();
			holder.uid = null;
			holder.labelTv = (TextView) convertView.findViewById(R.id.station_name);
			holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
			holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
			holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
			holder.subwayImg1 = (ImageView) convertView.findViewById(R.id.subway_img_1);
			holder.subwayImg2 = (ImageView) convertView.findViewById(R.id.subway_img_2);
			holder.subwayImg3 = (ImageView) convertView.findViewById(R.id.subway_img_3);
			convertView.setTag(holder);
		}
		updateSubwayView(position, convertView);
		return convertView;
	}

	private View updateSubwayView(int position, View convertView) {
		// MyLog.v(TAG, "updateSubwayView(%s)", position);
		if (convertView == null) {
			return convertView;
		}
		SubwayViewHolder holder = (SubwayViewHolder) convertView.getTag();
		POI poi = getItem(position);
		if (poi != null) {
			ASubwayStation subwayStation = (ASubwayStation) poi;
			// station name
			holder.labelTv.setText(subwayStation.getName());
			// station lines color
			// TODO manage main route ID and other routes ... or not ?
			if (subwayStation.getOtherLinesId() != null && subwayStation.getOtherLinesId().size() > 0) {
				int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(subwayStation.getOtherLinesId().get(0));
				holder.subwayImg1.setVisibility(View.VISIBLE);
				holder.subwayImg1.setImageResource(subwayLineImg1);
				if (subwayStation.getOtherLinesId().size() > 1) {
					int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(subwayStation.getOtherLinesId().get(1));
					holder.subwayImg2.setVisibility(View.VISIBLE);
					holder.subwayImg2.setImageResource(subwayLineImg2);
					if (subwayStation.getOtherLinesId().size() > 2) {
						int subwayLineImg3 = SubwayUtils.getSubwayLineImgId(subwayStation.getOtherLinesId().get(2));
						holder.subwayImg3.setVisibility(View.VISIBLE);
						holder.subwayImg3.setImageResource(subwayLineImg3);
					} else {
						holder.subwayImg3.setVisibility(View.GONE);
					}
				} else {
					holder.subwayImg2.setVisibility(View.GONE);
					holder.subwayImg3.setVisibility(View.GONE);
				}
			} else {
				holder.subwayImg1.setVisibility(View.GONE);
				holder.subwayImg2.setVisibility(View.GONE);
				holder.subwayImg3.setVisibility(View.GONE);
			}
			// setup distance, compass, favorite, closest
			updateCommonView(holder, poi);
		}
		return convertView;
	}

	private View getBusView(int position, View convertView, ViewGroup parent) {
		// MyLog.v(TAG, "getBusView(%s)", position);
		BusStopViewHolder holder;
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.poi_list_bus_stop_item, parent, false);
			holder = new BusStopViewHolder();
			holder.uid = null;
			holder.labelTv = (TextView) convertView.findViewById(R.id.label);
			holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
			holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
			holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
			holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
			holder.lineNumberTv = (TextView) convertView.findViewById(R.id.line_number);
			holder.lineDirectionTv = (TextView) convertView.findViewById(R.id.line_direction);
			convertView.setTag(holder);
		}
		updateBusView(position, convertView);
		return convertView;
	}

	private View updateBusView(int position, View convertView) {
		// MyLog.v(TAG, "updateBusView(%s)", position);
		if (convertView == null) {
			return convertView;
		}
		BusStopViewHolder holder = (BusStopViewHolder) convertView.getTag();
		final POI poi = getItem(position);
		if (poi != null) {
			final Route route;
			final Trip trip;
			final Stop stop;
			if (poi instanceof RouteTripStop) {
				final RouteTripStop routeTripStop = (RouteTripStop) poi;
				route = routeTripStop.route;
				trip = routeTripStop.trip;
				stop = routeTripStop.stop;
			} else if (poi instanceof TripStop) {
				final TripStop tripStop = (TripStop) poi;
				route = null;
				trip = tripStop.trip;
				stop = tripStop.stop;
			} else {
				route = null;
				trip = null;
				stop = (Stop) poi;
			}
			// bus stop code
			holder.stopCodeTv.setText(stop.code);
			// bus stop place
			holder.labelTv.setText(BusUtils.cleanBusStopPlace(stop.name));
			// bus stop line number
			if (route == null || trip == null) {
				holder.lineNumberTv.setVisibility(View.GONE);
				holder.lineDirectionTv.setVisibility(View.GONE);
			} else {
				holder.lineNumberTv.setText(route.shortName);
				holder.lineNumberTv.setBackgroundColor(Utils.parseColor(route.color));
				holder.lineNumberTv.setTextColor(Utils.parseColor(route.textColor));
				holder.lineDirectionTv.setText(trip.getHeading(this.activity).toUpperCase(Locale.getDefault()));
				holder.lineNumberTv.setVisibility(View.VISIBLE);
				holder.lineDirectionTv.setVisibility(View.VISIBLE);
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
				holder.labelTv.setTypeface(Typeface.DEFAULT_BOLD);
				holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
				holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
				holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
				break;
			default:
				holder.labelTv.setTypeface(Typeface.DEFAULT);
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
				case Fav.KEY_TYPE_VALUE_BUS_STOP:
					if (newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_BUS) == null) {
						newTypeFavUIDs.put(POI.ITEM_VIEW_TYPE_BUS, new HashSet<String>());
					}
					final String uid = TripStop.getUID(fav.getFkId(), fav.getFkId2());
					if (this.typeFavUIDs != null && this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_BUS) != null
							&& !this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_BUS).contains(uid)) {
						newFav = true;
					}
					newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_BUS).add(uid);
					break;
				case Fav.KEY_TYPE_VALUE_SUBWAY_STATION:
					if (newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_SUBWAY) == null) {
						newTypeFavUIDs.put(POI.ITEM_VIEW_TYPE_SUBWAY, new HashSet<String>());
					}
					if (this.typeFavUIDs != null && this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_SUBWAY) != null
							&& !this.typeFavUIDs.get(POI.ITEM_VIEW_TYPE_SUBWAY).contains(fav.getFkId())) {
						newFav = true;
					}
					newTypeFavUIDs.get(POI.ITEM_VIEW_TYPE_SUBWAY).add(fav.getFkId());
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

	public static class BusStopViewHolder extends CommonViewHolder {
		TextView stopCodeTv;
		TextView lineNumberTv;
		TextView lineDirectionTv;
	}

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
		TextView labelTv;
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

package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.dialog.SubwayLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The subway line info activity.
 * @author Mathieu Méa
 */
@TargetApi(3)
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, LocationListener, SensorEventListener, ShakeListener,
		CompassListener, OnScrollListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/SubwayLine";

	/**
	 * The extra for the subway line number.
	 */
	public static final String EXTRA_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra for the subway station display order.
	 */
	public static final String EXTRA_ORDER_PREF = "extra_order_pref";
	/**
	 * The subway line.
	 */
	private StmStore.SubwayLine subwayLine;
	/**
	 * The array adapter.
	 */
	private ArrayAdapter<ASubwayStation> adapter;
	/**
	 * The subway stations.
	 */
	private List<ASubwayStation> stations;
	/**
	 * The closest subway line station ID.
	 */
	private String closestStationId;
	/**
	 * The favorite subway line stations IDs.
	 */
	private List<String> favStationsIds;
	/**
	 * The subway station list order ID.
	 */
	private String orderPref;
	/**
	 * Is the location updates should be enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * The acceleration apart from gravity.
	 */
	private float lastSensorAcceleration = 0.00f;
	/**
	 * The last acceleration including gravity.
	 */
	private float lastSensorAccelerationIncGravity = SensorManager.GRAVITY_EARTH;
	/**
	 * The last sensor update time-stamp.
	 */
	private long lastSensorUpdate = -1;
	/**
	 * True if the share was already handled (should be reset in {@link #onResume()}).
	 */
	private boolean shakeHandled = false;
	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass value.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The list scroll state.
	 */
	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_info);

		setupList((ListView) findViewById(R.id.list));

		Integer lineNumber = Integer.valueOf(Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_LINE_NUMBER));
		String newOrderPref = Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_ORDER_PREF);
		showNewSubway(lineNumber, newOrderPref);
	}

	/**
	 * Setup the subway line stations list.
	 * @param list the stations list
	 */
	private void setupList(ListView list) {
		list.setEmptyView(findViewById(R.id.list_empty));
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s, %s, %s, %s)", l.getId(), v.getId(), position, id);
				if (SubwayLineInfo.this.stations != null && position < SubwayLineInfo.this.stations.size()
						&& SubwayLineInfo.this.stations.get(position) != null) {
					// IF last subway station, show descent only
					if (position + 1 == SubwayLineInfo.this.stations.size()) {
						Toast toast = Toast.makeText(SubwayLineInfo.this, R.string.subway_station_descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						return;
					}
					Intent intent = new Intent(SubwayLineInfo.this, SubwayStationInfo.class);
					String subwayStationId = SubwayLineInfo.this.stations.get(position).getId();
					String subwayStationName = SubwayLineInfo.this.stations.get(position).getName();
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStationName);
					startActivity(intent);
				}
			}
		});
		list.setOnScrollListener(this);
	}

	@Override
	public void showNewSubway(int newLineNumber, final String newOrderPref) {
		MyLog.v(TAG, "showNewSubway(%s, %s)", newLineNumber, newOrderPref);
		if ((this.subwayLine == null || this.subwayLine.getNumber() != newLineNumber) || (this.orderPref != null && !this.orderPref.equals(newOrderPref))) {
			// temporary show the subway line name
			((TextView) findViewById(R.id.line_name)).setText(SubwayUtils.getSubwayLineName(newLineNumber));
			((ImageView) findViewById(R.id.subway_img)).setImageResource(SubwayUtils.getSubwayLineImgId(newLineNumber));
			// show loading layout
			((ListView) findViewById(R.id.list)).setAdapter(null);
			new AsyncTask<Integer, Void, SubwayLine>() {
				@Override
				protected SubwayLine doInBackground(Integer... params) {
					if (SubwayLineInfo.this.subwayLine != null && SubwayLineInfo.this.subwayLine.getNumber() == params[0].intValue()) {
						return SubwayLineInfo.this.subwayLine;
					}
					return StmManager.findSubwayLine(getContentResolver(), params[0]);
				}

				@Override
				protected void onPostExecute(SubwayLine result) {
					SubwayLineInfo.this.subwayLine = result;
					if (newOrderPref == null) {
						SubwayLineInfo.this.orderPref = UserPreferences.getPrefDefault(SubwayLineInfo.this,
								UserPreferences.getPrefsSubwayStationsOrder(result.getNumber()), UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
					} else {
						UserPreferences.savePrefDefault(SubwayLineInfo.this, UserPreferences.getPrefsSubwayStationsOrder(result.getNumber()), newOrderPref);
						SubwayLineInfo.this.orderPref = newOrderPref;
					}
					refreshAll();
				};
			}.execute(newLineNumber);
		}
	}

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		MyLog.v(TAG, "onWindowFocusChanged(%s)", hasFocus);
		// IF the activity just regained the focus DO
		if (!this.hasFocus && hasFocus) {
			onResumeWithFocus();
		}
		this.hasFocus = hasFocus;
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		// IF the activity has the focus DO
		if (this.hasFocus) {
			onResumeWithFocus();
		}
		super.onResume();
	}

	/**
	 * {@link #onResume()} when activity has the focus
	 */
	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		// IF location updates should be enabled DO
		if (!this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// refresh favorites
		refreshFavoriteStationIdsFromDB();
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		LocationUtils.disableLocationUpdates(this, this);
		this.locationUpdatesEnabled = false;
		SensorUtils.unregisterSensorListener(this, this);
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(se, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
		// SensorUtils.checkForCompass(event, this.accelerometerValues, this.magneticFieldValues, this);
		checkForCompass(se, this);
	}

	/**
	 * @see SensorUtils#checkForCompass(SensorEvent, float[], float[], CompassListener)
	 */
	public void checkForCompass(SensorEvent event, CompassListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerValues = event.values;
			if (magneticFieldValues != null) {
				listener.onCompass();
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magneticFieldValues = event.values;
			if (accelerometerValues != null) {
				listener.onCompass();
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged()");
	}

	@Override
	public void onShake() {
		MyLog.v(TAG, "onShake()");
		showClosestStation();
	}

	/**
	 * Show the closest subway line station (if possible).
	 */
	private void showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		if (this.hasFocus && !this.shakeHandled && !TextUtils.isEmpty(this.closestStationId)) {
			Toast.makeText(this, R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.closestStationId);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, findStationName(this.closestStationId));
			startActivity(intent);
			this.shakeHandled = true;
		}
	}

	/**
	 * @param stationId a subway station ID
	 * @return a subway station name or null
	 */
	private String findStationName(String stationId) {
		if (this.stations == null) {
			return null;
		}
		for (SubwayStation subwayStation : this.stations) {
			if (subwayStation.getId().equals(stationId)) {
				return subwayStation.getName();
			}
		}
		return null;
	}

	@Override
	public void onCompass() {
		// MyLog.v(TAG, "onCompass()");
		if (this.accelerometerValues != null && this.magneticFieldValues != null) {
			updateCompass(SensorUtils.calculateOrientation(this, this.accelerometerValues, this.magneticFieldValues));
		}
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	private void updateCompass(float[] orientation) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update closest bike stations compass
				if (this.stations != null) {
					for (ASubwayStation subwayStation : this.stations) {
						subwayStation.getCompassMatrix().reset();
						subwayStation.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this, currentLocation, subwayStation.getLocation(), orientation,
										getLocationDeclination()), getArrowDim().first / 2, getArrowDim().second / 2);
					}
					// update the view
					notifyDataSetChanged(false);
				}
			}
		}
	}

	/**
	 * The minimum between 2 {@link ArrayAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	private static final int ADAPTER_NOTIFY_THRESOLD = 150; // 0.15 seconds

	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	private Pair<Integer, Integer> arrowDim;
	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (view == findViewById(R.id.list)) {
			this.scrollState = scrollState;
		}
	}

	/**
	 * Refresh all the UI based on the subway line.
	 */
	private void refreshAll() {
		refreshSubwayLineInfo();
		refreshSubwayStationsListFromDB();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the list
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistancesWithNewLocation();
		}
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Refresh the subway stations list.
	 */
	private void refreshSubwayStationsListFromDB() {
		new AsyncTask<Integer, Void, List<ASubwayStation>>() {
			@Override
			protected List<ASubwayStation> doInBackground(Integer... params) {
				int lineNumber = params[0];
				String orderId = getSortOrderFromOrderPref(lineNumber);
				List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(SubwayLineInfo.this.getContentResolver(), lineNumber, orderId);
				// preparing other stations lines data
				Map<String, Set<Integer>> stationsWithOtherLines = new HashMap<String, Set<Integer>>();
				for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayLineStationsWithOtherLinesList(
						SubwayLineInfo.this.getContentResolver(), lineNumber)) {
					int subwayLineNumber = lineStation.first.getNumber();
					String subwayStationId = lineStation.second.getId();
					if (stationsWithOtherLines.get(subwayStationId) == null) {
						stationsWithOtherLines.put(subwayStationId, new HashSet<Integer>());
					}
					stationsWithOtherLines.get(subwayStationId).add(subwayLineNumber);
				}
				// creating the list of the subways stations object
				List<ASubwayStation> stations = new ArrayList<ASubwayStation>();
				for (SubwayStation station : subwayStationsList) {
					ASubwayStation aStation = new ASubwayStation(station);
					aStation.setLineId(lineNumber);
					// add other subway lines
					if (stationsWithOtherLines.containsKey(aStation.getId())) {
						aStation.addOtherLinesId(stationsWithOtherLines.get(aStation.getId()));
					}
					stations.add(aStation);
				}
				return stations;
			}

			@Override
			protected void onPostExecute(List<ASubwayStation> result) {
				SubwayLineInfo.this.stations = result;
				generateOrderedStationsIds();
				refreshFavoriteStationIdsFromDB();
				SubwayLineInfo.this.adapter = new ArrayAdapterWithCustomView(SubwayLineInfo.this, R.layout.subway_line_info_stations_list_item);
				updateDistancesWithNewLocation();
				((ListView) findViewById(R.id.list)).setAdapter(SubwayLineInfo.this.adapter);
			};
		}.execute(this.subwayLine.getNumber());
	}

	/**
	 * Find favorites subway station IDs.
	 */
	private void refreshFavoriteStationIdsFromDB() {
		this.favStationsIds = new ArrayList<String>(); // clear list
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false;
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(SubwayLineInfo.this.favStationsIds)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavStationsIds = new ArrayList<String>();
				for (Fav subwayStationFav : result) {
					if (SubwayLineInfo.this.favStationsIds == null || !SubwayLineInfo.this.favStationsIds.contains(subwayStationFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavStationsIds.add(subwayStationFav.getFkId()); // store stop code
				}
				SubwayLineInfo.this.favStationsIds = newfavStationsIds;
				// trigger change
				if (newFav) {
					notifyDataSetChanged(true);
				}
			};
		}.execute();
	}

	/**
	 * Refresh the subway line info.
	 */
	private void refreshSubwayLineInfo() {
		// subway line name
		((TextView) findViewById(R.id.line_name)).setText(SubwayUtils.getSubwayLineName(this.subwayLine.getNumber()));
		((ImageView) findViewById(R.id.subway_img)).setImageResource(SubwayUtils.getSubwayLineImgId(this.subwayLine.getNumber()));

		// subway line direction
		new AsyncTask<String, Void, SubwayStation>() {
			@Override
			protected SubwayStation doInBackground(String... params) {
				return StmManager.findSubwayLineLastSubwayStation(getContentResolver(), SubwayLineInfo.this.subwayLine.getNumber(), params[0]);
			}

			@Override
			protected void onPostExecute(SubwayStation result) {
				((TextView) findViewById(R.id.subway_line_station_string)).setText(getString(R.string.stations_and_order, getDirectionText(result)));
				findViewById(R.id.subway_line_station_string).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showSelectDirectionDialog(v);
					}
				});
			};
		}.execute(getSortOrderFromOrderPref(this.subwayLine.getNumber()));
	}

	/**
	 * Show the subway line select direction dialog.
	 * @param v the view (not used)
	 */
	public void showSelectDirectionDialog(View v) {
		// MyLog.v(TAG, "showSelectDirectionDialog()");
		// TODO use single choice items to show the current direction
		new SubwayLineSelectDirection(this, this.subwayLine.getNumber(), this).showDialog();
	}

	/**
	 * @return the sort order from the order preference
	 */
	private String getSortOrderFromOrderPref(int subwayLineNumber) {
		String prefsSubwayStationsOrder = UserPreferences.getPrefsSubwayStationsOrder(subwayLineNumber);
		String sharedPreferences = UserPreferences.getPrefDefault(this, prefsSubwayStationsOrder, UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT);
		if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER;
		} else if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC;
		} else {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER; // DEFAULT (ASC)
		}
	}

	/**
	 * @param lastSubwayStation the last subway station
	 * @return the direction test (the direction(station) or the A-Z order)
	 */
	private String getDirectionText(SubwayStation lastSubwayStation) {
		MyLog.v(TAG, "getDirectionText(%s)", lastSubwayStation.getName());
		return getString(R.string.direction_and_string, lastSubwayStation.getName());
	}

	/**
	 * Update the distances with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		if (getLocation() != null && this.stations != null) {
			float accuracyInMeters = getLocation().getAccuracy();
			boolean isDetailed = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
					UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(this, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			for (ASubwayStation station : this.stations) {
				station.setDistance(getLocation().distanceTo(station.getLocation()));
				station.setDistanceString(Utils.getDistanceString(station.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			String previousClosest = this.closestStationId;
			generateOrderedStationsIds();
			notifyDataSetChanged(this.closestStationId == null ? false : this.closestStationId.equals(previousClosest));
		}
	}

	/**
	 * Generate the ordered subway line station IDs.
	 */
	public void generateOrderedStationsIds() {
		List<ASubwayStation> orderedStations = new ArrayList<ASubwayStation>(this.stations);
		// order the stations list by distance (closest first)
		Collections.sort(orderedStations, new Comparator<ASubwayStation>() {
			@Override
			public int compare(ASubwayStation lhs, ASubwayStation rhs) {
				float d1 = lhs.getDistance();
				float d2 = rhs.getDistance();
				if (d1 > d2) {
					return +1;
				} else if (d1 < d2) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		this.closestStationId = orderedStations.get(0).getId();
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	private class ArrayAdapterWithCustomView extends ArrayAdapter<ASubwayStation> {

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
		public ArrayAdapterWithCustomView(Context context, int viewId) {
			super(context, viewId, stations);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return SubwayLineInfo.this.stations == null ? 0 : SubwayLineInfo.this.stations.size();
		}

		@Override
		public int getPosition(ASubwayStation item) {
			return SubwayLineInfo.this.stations.indexOf(item);
		}

		@Override
		public ASubwayStation getItem(int position) {
			return SubwayLineInfo.this.stations.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(" + position + ")");
			View view;
			if (convertView == null) {
				view = this.layoutInflater.inflate(viewId, parent, false);
			} else {
				view = convertView;
			}
			ASubwayStation station = getItem(position);
			if (station != null) {
				// station name
				final TextView nameTv = (TextView) view.findViewById(R.id.station_name);
				nameTv.setText(station.getName());
				// station lines color
				List<Integer> otherLines = station.getOtherLinesId();
				// 1 - find the station line image
				int subwayLineImgId = SubwayUtils.getSubwayLineImgId(station.getLineId());
				if (position == 0) {
					subwayLineImgId = SubwayUtils.getSubwayLineImgListTopId(station.getLineId());
				} else if (position == getCount() - 1) {
					subwayLineImgId = SubwayUtils.getSubwayLineImgListBottomId(station.getLineId());
				} else {
					subwayLineImgId = SubwayUtils.getSubwayLineImgListMiddleId(station.getLineId());
				}
				// 2 - set the images to the right image view
				// color 1 (on the right, closer to the text)
				if (otherLines.size() == 0) {
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImgId);
				} else {
					int lastIndex = otherLines.size() - 1;
					int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(lastIndex));
					((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(otherLineImg);
				}
				// color 2 (on the middle)
				if (otherLines.size() < 1) {
					view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.subway_img_2).setVisibility(View.VISIBLE);
					if (otherLines.size() == 1) {
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(0));
						((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(otherLineImg);
					}
				}
				// color 3 (on the left, closer to the border)
				if (otherLines.size() < 2) {
					view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.subway_img_3).setVisibility(View.VISIBLE);
					if (otherLines.size() == 2) {
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(1));
						((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(otherLineImg);
					}
				}
				// favorite
				if (SubwayLineInfo.this.favStationsIds != null && SubwayLineInfo.this.favStationsIds.contains(station.getId())) {
					view.findViewById(R.id.fav_img).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.fav_img).setVisibility(View.GONE);
				}
				// station distance
				TextView distanceTv = (TextView) view.findViewById(R.id.distance);
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					distanceTv.setText(station.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				} else {
					distanceTv.setVisibility(View.GONE);
				}
				// station compass
				ImageView compassImg = (ImageView) view.findViewById(R.id.compass);
				if (station.getCompassMatrixOrNull() != null) {
					compassImg.setImageMatrix(station.getCompassMatrix());
					compassImg.setVisibility(View.VISIBLE);
				} else {
					compassImg.setVisibility(View.GONE);
				}
				// set style for closest subway station
				int index = -1;
				if (!TextUtils.isEmpty(SubwayLineInfo.this.closestStationId)) {
					index = station.getId().equals(SubwayLineInfo.this.closestStationId) ? 0 : 999;
				}
				switch (index) {
				case 0:
					nameTv.setTypeface(Typeface.DEFAULT_BOLD);
					distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					break;
				default:
					nameTv.setTypeface(Typeface.DEFAULT);
					distanceTv.setTypeface(Typeface.DEFAULT);
					distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					break;
				}
			}
			return view;
		}
	}

	// TODO enable list filtering
	// @Override
	// public Cursor runQuery(CharSequence constraint) {
	// MyLog.v(TAG, "runQuery(" + constraint + ")");
	// int lineNumber = this.subwayLine.getNumber();
	// String orderId = getSortOrderFromOrderPref(lineNumber);
	// return StmManager.searchSubwayLineStations(getContentResolver(), lineNumber, orderId, constraint.toString());
	// }

	/**
	 * Store the device location.
	 */
	private Location location;

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <b>NULL</b>
	 */
	private Location getLocation() {
		if (this.location == null) {
			Location bestLastKnownLocationOrNull = LocationUtils.getBestLastKnownLocation(this);
			if (bestLastKnownLocationOrNull != null) {
				this.setLocation(bestLastKnownLocationOrNull);
			}
			// enable location updates if necessary
			if (!this.locationUpdatesEnabled) {
				LocationUtils.enableLocationUpdates(this, this);
				this.locationUpdatesEnabled = true;
			}
		}
		return this.location;
	}

	/**
	 * @param newLocation the new location
	 */
	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
				SensorUtils.registerShakeAndCompassListener(this, this);
				this.shakeHandled = false;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistancesWithNewLocation();
	}

	@Override
	public void onProviderEnabled(String provider) {
		MyLog.v(TAG, "onProviderEnabled(%s)", provider);
	}

	@Override
	public void onProviderDisabled(String provider) {
		MyLog.v(TAG, "onProviderDisabled(%s)", provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.subway_line_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.direction:
			showSelectDirectionDialog(null);
			return true;
		default:
			return MenuUtils.handleCommonMenuActions(this, item);
		}
	}
}

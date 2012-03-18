package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.montrealtransit.android.SensorUtils.ShakeListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.SubwayLineSelectDirection;
import org.montrealtransit.android.dialog.SubwayLineSelectDirectionDialogListener;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
public class SubwayLineInfo extends Activity implements SubwayLineSelectDirectionDialogListener, LocationListener, SensorEventListener, ShakeListener {

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
	private ASubwayStation[] stations;
	/**
	 * The subway line stations IDs ordered by distance (closest first).
	 */
	private List<String> orderedStationsIds;
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
	private boolean shakeHandled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.subway_line_info);

		setupList((ListView) findViewById(R.id.list));

		showNewSubway(Integer.valueOf(Utils.getSavedStringValue(getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_LINE_NUMBER)),
				Utils.getSavedStringValue(getIntent(), savedInstanceState, SubwayLineInfo.EXTRA_ORDER_PREF));
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
				if (SubwayLineInfo.this.stations != null && position < SubwayLineInfo.this.stations.length && SubwayLineInfo.this.stations[position] != null) {
					Intent intent = new Intent(SubwayLineInfo.this, SubwayStationInfo.class);
					String subwayStationId = SubwayLineInfo.this.stations[position].getId();
					String subwayStationName = SubwayLineInfo.this.stations[position].getName();
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStationName);
					startActivity(intent);
				}
			}
		});
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

	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		LocationUtils.disableLocationUpdates(this, this);
		super.onStop();
	}

	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		// IF location updates should be enabled DO
		if (this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistancesWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onRestart();
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		// refresh favorites
		refreshFavoriteStationIdsFromDB();
		SensorUtils.registerShakeListener(this, this);
		this.shakeHandled = false;
		super.onResume();
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForShake(se.values, this.lastSensorUpdate, this.lastSensorAccelerationIncGravity, this.lastSensorAcceleration, this);
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
		if (!this.shakeHandled && this.orderedStationsIds != null && this.orderedStationsIds.size() > 0) {
			Toast.makeText(this, R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.orderedStationsIds.get(0));
			startActivity(intent);
			this.shakeHandled = true;
		}
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		SensorUtils.unregisterShakeListener(this, this);
		super.onPause();
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
		new AsyncTask<Integer, Void, ASubwayStation[]>() {
			@Override
			protected ASubwayStation[] doInBackground(Integer... params) {
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
				ASubwayStation[] stations = new ASubwayStation[subwayStationsList.size()];
				int i = 0;
				for (SubwayStation station : subwayStationsList) {
					ASubwayStation aStation = new ASubwayStation();
					aStation.setId(station.getId());
					aStation.setName(station.getName());
					aStation.setLat(station.getLat());
					aStation.setLng(station.getLng());
					aStation.setLineId(lineNumber);
					// add other subway lines
					if (stationsWithOtherLines.containsKey(aStation.getId())) {
						aStation.addOtherLinesId(stationsWithOtherLines.get(aStation.getId()));
					}
					stations[i++] = aStation;
				}
				return stations;
			}

			@Override
			protected void onPostExecute(ASubwayStation[] result) {
				SubwayLineInfo.this.stations = result;
				refreshFavoriteStationIdsFromDB();
				SubwayLineInfo.this.adapter = new ArrayAdapterWithCustomView(SubwayLineInfo.this, R.layout.subway_line_info_stations_list_item, stations);
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
				for (Fav subwayStationFav : result) {
					// keep all subway stations favorites (can't be that much!)
					SubwayLineInfo.this.favStationsIds.add(subwayStationFav.getFkId()); // store stop code
				}
				// trigger change
				if (SubwayLineInfo.this.adapter != null) {
					SubwayLineInfo.this.adapter.notifyDataSetChanged();
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
			return StmStore.SubwayStation.DEFAULT_SORT_ORDER; // DEFAULT (A-Z order)
		}
	}

	/**
	 * @param lastSubwayStation the last subway station
	 * @return the direction test (the direction(station) or the A-Z order)
	 */
	private String getDirectionText(SubwayStation lastSubwayStation) {
		MyLog.v(TAG, "getDirectionText(%s)", lastSubwayStation.getName());
		MyLog.d(TAG, "this.orderPref: " + this.orderPref);
		if (this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)
				|| this.orderPref.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			MyLog.d(TAG, "direction");
			return getString(R.string.direction_and_string, lastSubwayStation.getName());
		} else {
			MyLog.d(TAG, "default A-Z");
			// DEFAULT: A-Z order
			// this.orderPref = UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_DEFAULT;
			return getString(R.string.alphabetical_order);
		}
	}

	/**
	 * Update the distances with the latest device location.
	 */
	private void updateDistancesWithNewLocation() {
		if (getLocation() != null && this.stations != null) {
			float accuracyInMeters = getLocation().getAccuracy();
			for (ASubwayStation station : this.stations) {
				station.setDistance(getLocation().distanceTo(LocationUtils.getNewLocation(station.getLat(), station.getLng())));
				station.setDistanceString(Utils.getDistanceString(this, station.getDistance(), accuracyInMeters));
			}
			generateOrderedStationsIds();
			if (this.adapter != null) {
				this.adapter.notifyDataSetChanged();
			}
		}
	}

	/**
	 * Generate the ordered subway line station IDs.
	 */
	public void generateOrderedStationsIds() {
		List<ASubwayStation> orderedStations = new ArrayList<ASubwayStation>(Arrays.asList(this.stations));
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
		this.orderedStationsIds = new ArrayList<String>();
		for (ASubwayStation orderedStation : orderedStations) {
			this.orderedStationsIds.add(orderedStation.getId());
		}
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
		 * The stations.
		 */
		private ASubwayStation[] stations;
		/**
		 * The view ID.
		 */
		private int viewId;

		/**
		 * The default constructor.
		 * @param context the context
		 * @param viewId the the view ID
		 * @param objects the stations
		 */
		public ArrayAdapterWithCustomView(Context context, int viewId, ASubwayStation[] stations) {
			super(context, viewId, stations);
			this.viewId = viewId;
			this.stations = stations;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			ASubwayStation station = this.stations[position];
			if (station != null) {
				// station name
				((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
				// station lines color
				List<Integer> otherLines = station.getOtherLinesId();
				// 1 - find the station line image
				int subwayLineImgId = SubwayUtils.getSubwayLineImgId(station.getLineId());
				if (SubwayLineInfo.this.getSortOrderFromOrderPref(station.getLineId()).equals(StmStore.SubwayStation.DEFAULT_SORT_ORDER)) {
					subwayLineImgId = SubwayUtils.getSubwayLineImgListId(station.getLineId());
				} else {
					if (position == 0) {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListTopId(station.getLineId());
					} else if (position == this.stations.length - 1) {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListBottomId(station.getLineId());
					} else {
						subwayLineImgId = SubwayUtils.getSubwayLineImgListMiddleId(station.getLineId());
					}
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
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					((TextView) view.findViewById(R.id.distance)).setText(station.getDistanceString());
					view.findViewById(R.id.distance).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.distance).setVisibility(View.GONE);
					((TextView) view.findViewById(R.id.distance)).setText(null);
				}
				// set style for closest bus stop
				int index = -1;
				if (SubwayLineInfo.this.orderedStationsIds != null) {
					index = SubwayLineInfo.this.orderedStationsIds.indexOf(station.getId());
				}
				switch (index) {
				case 0:
					((TextView) view.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT_BOLD);
					((TextView) view.findViewById(R.id.distance)).setTypeface(Typeface.DEFAULT_BOLD);
					((TextView) view.findViewById(R.id.distance)).setTextColor(Utils.getTextColorPrimary(getContext()));
					break;
				default:
					((TextView) view.findViewById(R.id.station_name)).setTypeface(Typeface.DEFAULT);
					((TextView) view.findViewById(R.id.distance)).setTypeface(Typeface.DEFAULT);
					((TextView) view.findViewById(R.id.distance)).setTextColor(Utils.getTextColorSecondary(getContext()));
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

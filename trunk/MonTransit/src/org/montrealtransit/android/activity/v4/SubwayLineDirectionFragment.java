package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.SubwayStationInfo;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.data.ASubwayStation;
import org.montrealtransit.android.data.Pair;
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
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
 * This fragment shows the subway line stations for a specific direction.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class SubwayLineDirectionFragment extends Fragment implements OnScrollListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = SubwayLineDirectionFragment.class.getSimpleName();
	/**
	 * The minimum between 2 {@link ArrayAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	private static final int ADAPTER_NOTIFY_THRESOLD = 150; // 0.15 seconds

	/**
	 * The subway stations list adapter.
	 */
	private ArrayAdapter<ASubwayStation> adapter;
	/**
	 * The closest subway line station ID.
	 */
	private String closestStationId;
	/**
	 * The list of subway stations.
	 */
	private List<ASubwayStation> stations;
	/**
	 * The favorite subway line stations IDs.
	 */
	private List<String> favStationsIds;
	/**
	 * The subway line number.
	 */
	private int subwayLineNumber;
	/**
	 * The subway line direction ID.
	 */
	private String subwayLineDirectionId;
	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;
	/**
	 * The scroll state of the list.
	 */
	private int scrollState = SCROLL_STATE_IDLE;

	/**
	 * @param subwayLineNumber subway line number
	 * @param directionId direction ID
	 * @return the fragment
	 */
	public static Fragment newInstance(int subwayLineNumber, String directionId) {
		MyLog.v(TAG, "newInstance(%s,%s)", subwayLineNumber, directionId);
		SubwayLineDirectionFragment f = new SubwayLineDirectionFragment();
		Bundle args = new Bundle();
		args.putString(SubwayLineInfo.EXTRA_LINE_NUMBER, String.valueOf(subwayLineNumber));
		args.putString(SubwayLineInfo.EXTRA_ORDER_PREF, directionId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Activity activity) {
		MyLog.v(TAG, "onAttach()");
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreateView()");
		int lineNumber = Integer.valueOf(getArguments().getString(SubwayLineInfo.EXTRA_LINE_NUMBER)).intValue();
		String lineDirectionId = getArguments().getString(SubwayLineInfo.EXTRA_ORDER_PREF);
		showSubwayLineDirectionStations(lineNumber, lineDirectionId);
		View v = inflater.inflate(R.layout.subway_line_info_stations_list, container, false);
		setupList((ListView) v.findViewById(R.id.list), v.findViewById(R.id.list_empty));
		return v;
	}

	/**
	 * Setup list.
	 * @param list the list
	 * @param emptyView the empty view
	 */
	private void setupList(ListView list, View emptyView) {
		MyLog.v(TAG, "setupList()");
		list.setEmptyView(emptyView);
		list.setOnScrollListener(SubwayLineDirectionFragment.this);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s,%s)", position, id);
				if (SubwayLineDirectionFragment.this.stations != null && position < SubwayLineDirectionFragment.this.stations.size()
						&& SubwayLineDirectionFragment.this.stations.get(position) != null) {
					// IF last subway station, show descent only
					if (position + 1 == SubwayLineDirectionFragment.this.stations.size()) {
						Toast toast = Toast.makeText(SubwayLineDirectionFragment.this.getActivity(), R.string.subway_station_descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						return;
					}
					Intent intent = new Intent(SubwayLineDirectionFragment.this.getActivity(), SubwayStationInfo.class);
					String subwayStationId = SubwayLineDirectionFragment.this.stations.get(position).getId();
					String subwayStationName = SubwayLineDirectionFragment.this.stations.get(position).getName();
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
					intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, subwayStationName);
					startActivity(intent);
				}
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		MyLog.v(TAG, "onActivityCreated()");
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
	}

	@Override
	public void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
	}

	public void onResumeWithFocus() {
		MyLog.v(TAG, "onResumeWithFocus()");
		updateDistancesWithNewLocation();
		refreshFavoriteStationIdsFromDB();
	}

	@Override
	public void onPause() {
		MyLog.v(TAG, "onPause()");
		super.onPause();
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

	/**
	 * Show new subway line direction stations.
	 * @param newSubwayLineNumber new subway line number
	 * @param newSubwayLineDirectionId new subway line direction ID
	 */
	private void showSubwayLineDirectionStations(int newSubwayLineNumber, String newSubwayLineDirectionId) {
		MyLog.v(TAG, "showSubwayLineDirectionStations(%s,%s)", newSubwayLineNumber, newSubwayLineDirectionId);
		// if ((this.subwayLineDirectionId == null)
		// || (this.subwayLineNumber != newSubwayLineNumber || !this.subwayLineDirectionId.equals(newSubwayLineDirectionId))) {
		this.subwayLineNumber = newSubwayLineNumber;
		this.subwayLineDirectionId = newSubwayLineDirectionId;
		this.stations = null;
		this.adapter = null;
		refreshSubwayStationListFromDB();
		// }
	}

	/**
	 * Refresh the subway station list UI.
	 */
	private void refreshSubwayStationListFromDB() { // TODO extract?
		MyLog.v(TAG, "refreshSubwayStationListFromDB()");
		new AsyncTask<Integer, Void, List<ASubwayStation>>() {
			@Override
			protected List<ASubwayStation> doInBackground(Integer... params) {
				int lineNumber = params[0];
				String orderId = getSortOrderFromOrderPref(SubwayLineDirectionFragment.this.subwayLineDirectionId);
				List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(SubwayLineDirectionFragment.this.getActivity()
						.getContentResolver(), lineNumber, orderId);
				// preparing other stations lines data
				Map<String, Set<Integer>> stationsWithOtherLines = new HashMap<String, Set<Integer>>();
				for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayLineStationsWithOtherLinesList(SubwayLineDirectionFragment.this
						.getActivity().getContentResolver(), lineNumber)) {
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
				SubwayLineDirectionFragment.this.stations = result;
				generateOrderedStationsIds();
				refreshFavoriteStationIdsFromDB();
				SubwayLineDirectionFragment.this.adapter = new ArrayAdapterWithCustomView(SubwayLineDirectionFragment.this.getActivity(),
						R.layout.subway_line_info_stations_list_item);
				updateDistancesWithNewLocation();
				((ListView) SubwayLineDirectionFragment.this.getView().findViewById(R.id.list)).setAdapter(SubwayLineDirectionFragment.this.adapter);
			};

		}.execute(this.subwayLineNumber);
	}

	/**
	 * @param sharedPreferences preference
	 * @return the actual {@link StmStore} sort order
	 */
	private String getSortOrderFromOrderPref(String sharedPreferences) {
		if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER;
		} else if (sharedPreferences.equals(UserPreferences.PREFS_SUBWAY_STATIONS_ORDER_NATURAL_DESC)) {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER_DESC;
		} else {
			return StmStore.SubwayStation.NATURAL_SORT_ORDER; // DEFAULT (ASC)
		}
	}

	/**
	 * Update the distances with the latest device location.
	 */
	protected void updateDistancesWithNewLocation() {
		// MyLog.v(TAG, "updateDistancesWithNewLocation()");
		if (this.stations == null || getSubwayLineInfoActivity() == null) {
			return;
		}
		Location currentLocation = getSubwayLineInfoActivity().getLocation();
		if (currentLocation != null) {
			float accuracyInMeters = currentLocation.getAccuracy();
			boolean isDetailed = UserPreferences.getPrefDefault(this.getActivity(), UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT)
					.equals(UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(this.getActivity(), UserPreferences.PREFS_DISTANCE_UNIT,
					UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			for (ASubwayStation station : this.stations) {
				// IF the subway station location is known DO
				station.setDistance(currentLocation.distanceTo(station.getLocation()));
				station.setDistanceString(Utils.getDistanceString(station.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			String previousClosest = this.closestStationId;
			generateOrderedStationsIds();
			notifyDataSetChanged(this.closestStationId == null ? false : this.closestStationId.equals(previousClosest));
		}
	}

	/**
	 * @return see {@link #getActivity()}
	 */
	private SubwayLineInfo getSubwayLineInfoActivity() {
		return (SubwayLineInfo) getActivity();
	}

	/**
	 * Show the closest subway line station (if possible).
	 * @return true if action performed
	 */
	protected boolean showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		if (!TextUtils.isEmpty(this.closestStationId)) {
			Toast.makeText(this.getActivity(), R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(this.getActivity(), SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, this.closestStationId);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, findStationName(this.closestStationId));
			startActivity(intent);
			return true;
		}
		return false;
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

	/**
	 * The last compass (in degree).
	 */
	private int lastCompassInDegree = -1;

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	protected void updateCompass(float[] orientation) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		Location currentLocation = getSubwayLineInfoActivity() == null ? null : getSubwayLineInfoActivity().getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update closest subway stations compass
				if (this.stations != null) {
					for (ASubwayStation station : this.stations) {
						station.getCompassMatrix().reset();
						station.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(this.getActivity(), currentLocation, station.getLocation(), orientation,
										getLocationDeclination()), getArrowDim().first / 2, getArrowDim().second / 2);
					}
					// update the view
					notifyDataSetChanged(false);
				}
			}
		}
	}

	private Pair<Integer, Integer> arrowDim;
	private Float locationDeclination;

	private float getLocationDeclination() {
		Location currentLocation = getSubwayLineInfoActivity() == null ? null : getSubwayLineInfoActivity().getLocation();
		if (this.locationDeclination == null && currentLocation != null) {
			this.locationDeclination = new GeomagneticField((float) currentLocation.getLatitude(), (float) currentLocation.getLongitude(),
					(float) currentLocation.getAltitude(), currentLocation.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(this.getActivity(), R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	/**
	 * Generate the ordered subway line stations IDs.
	 */
	public void generateOrderedStationsIds() {
		if (this.stations.size() == 0) {
			return;
		}
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
	 * Find favorites subway station IDs.
	 */
	private void refreshFavoriteStationIdsFromDB() {
		this.favStationsIds = new ArrayList<String>(); // clear list
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(getActivity().getContentResolver(), DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false;
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(SubwayLineDirectionFragment.this.favStationsIds)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavStationIds = new ArrayList<String>();
				for (Fav subwayStationFav : result) {
					if (SubwayLineDirectionFragment.this.favStationsIds == null
							|| !SubwayLineDirectionFragment.this.favStationsIds.contains(subwayStationFav.getFkId())) {
						newFav = true; // new favorite
					}
					newfavStationIds.add(subwayStationFav.getFkId()); // store station ID
				}
				SubwayLineDirectionFragment.this.favStationsIds = newfavStationIds;
				// trigger change
				if (newFav) {
					notifyDataSetChanged(true);
				}
			};
		}.execute();
	}

	/**
	 * @param force true to force notify {@link ArrayAdapter#notifyDataSetChanged()} if necessary
	 */
	public void notifyDataSetChanged(boolean force) {
		// MyLog.v(TAG, "notifyDataSetChanged(%s)", force);
		long now = System.currentTimeMillis();
		if (this.adapter != null && this.scrollState == OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > ADAPTER_NOTIFY_THRESOLD)) {
			// MyLog.d(TAG, "Notify data set changed");
			this.adapter.notifyDataSetChanged();
			this.lastNotifyDataSetChanged = now;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (view == getView().findViewById(R.id.list)) {
			this.scrollState = scrollState;// == OnScrollListener.SCROLL_STATE_FLING);
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
			return SubwayLineDirectionFragment.this.stations == null ? 0 : SubwayLineDirectionFragment.this.stations.size();
		}

		@Override
		public int getPosition(ASubwayStation item) {
			return SubwayLineDirectionFragment.this.stations.indexOf(item);
		}

		@Override
		public ASubwayStation getItem(int position) {
			return SubwayLineDirectionFragment.this.stations.get(position);
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
				// }
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
				if (SubwayLineDirectionFragment.this.favStationsIds != null && SubwayLineDirectionFragment.this.favStationsIds.contains(station.getId())) {
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
				if (!TextUtils.isEmpty(SubwayLineDirectionFragment.this.closestStationId)) {
					index = station.getId().equals(SubwayLineDirectionFragment.this.closestStationId) ? 0 : 999;
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
}

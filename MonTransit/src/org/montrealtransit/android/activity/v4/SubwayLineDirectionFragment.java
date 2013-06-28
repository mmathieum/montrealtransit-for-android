package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.SubwayStationInfo;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.api.SupportFactory;
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
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
	 * The current station ID (or null).
	 */
	private String currentStationId;
	/**
	 * The list of subway stations.
	 */
	private List<ASubwayStation> stations;
	/**
	 * The favorite subway line stations IDs.
	 */
	private List<String> favStationsIds;
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
	public static Fragment newInstance(int subwayLineNumber, String directionId, String stationId) {
		MyLog.v(TAG, "newInstance(%s,%s,%s)", subwayLineNumber, directionId, stationId);
		SubwayLineDirectionFragment f = new SubwayLineDirectionFragment();
		Bundle args = new Bundle();
		args.putString(SubwayLineInfo.EXTRA_LINE_NUMBER, String.valueOf(subwayLineNumber));
		args.putString(SubwayLineInfo.EXTRA_ORDER_PREF, directionId);
		args.putString(SubwayLineInfo.EXTRA_STATION_ID, stationId);
		f.setArguments(args);
		return f;
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
		View v = inflater.inflate(R.layout.subway_line_info_stations_list, container, false);
		setupList((ListView) v.findViewById(R.id.list), v.findViewById(R.id.list_empty));
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

	private void showAll() {
		String lineNumber = getArguments().getString(SubwayLineInfo.EXTRA_LINE_NUMBER);
		String lineDirectionId = getArguments().getString(SubwayLineInfo.EXTRA_ORDER_PREF);
		String stationId = getArguments().getString(SubwayLineInfo.EXTRA_STATION_ID);
		this.subwayLineDirectionId = lineDirectionId;
		this.stations = null;
		ListView listView = (ListView) getLastView().findViewById(R.id.list);
		this.adapter = new ArrayAdapterWithCustomView(getLastActivity(), R.layout.subway_line_info_stations_list_item);
		listView.setAdapter(SubwayLineDirectionFragment.this.adapter);
		refreshSubwayStationListFromDB(lineNumber, stationId);
	}

	@Override
	public void onStart() {
		MyLog.v(TAG, "onStart()");
		super.onStart();
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
				Activity activity = SubwayLineDirectionFragment.this.getLastActivity();
				if (SubwayLineDirectionFragment.this.stations != null && position < SubwayLineDirectionFragment.this.stations.size()
						&& SubwayLineDirectionFragment.this.stations.get(position) != null && activity != null) {
					// IF last subway station, show descent only
					if (position + 1 == SubwayLineDirectionFragment.this.stations.size()) {
						Toast toast = Toast.makeText(activity, R.string.subway_station_descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						return;
					}
					Intent intent = new Intent(activity, SubwayStationInfo.class);
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
	public void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
	}

	public void onResumeWithFocus(SubwayLineInfo activity) {
		MyLog.v(TAG, "onResumeWithFocus()");
		updateDistancesWithNewLocation(activity);
		refreshFavoriteStationIdsFromDB(activity.getContentResolver());
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
	 * Refresh the subway station list UI.
	 */
	private void refreshSubwayStationListFromDB(String lineNumber, String stationId) { // TODO extract?
		MyLog.v(TAG, "refreshSubwayStationListFromDB()");
		new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground(String... params) {
				int lineNumber = Integer.valueOf(params[0]);
				String stationId = params[1];
				String orderId = getSortOrderFromOrderPref(SubwayLineDirectionFragment.this.subwayLineDirectionId);
				SubwayLineInfo activity = SubwayLineDirectionFragment.this.getSubwayLineInfoActivity();
				List<SubwayStation> subwayStationsList = StmManager.findSubwayLineStationsList(activity.getContentResolver(), lineNumber, orderId);
				// preparing other stations lines data
				Map<String, Set<Integer>> stationsWithOtherLines = new HashMap<String, Set<Integer>>();
				for (Pair<SubwayLine, SubwayStation> lineStation : StmManager.findSubwayLineStationsWithOtherLinesList(activity.getContentResolver(),
						lineNumber)) {
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
					if (aStation.getId().equals(stationId)) {
						SubwayLineDirectionFragment.this.currentStationId = aStation.getId();
					}
					stations.add(aStation);
				}
				SubwayLineDirectionFragment.this.stations = stations;
				// force update all subway stations with location
				LocationUtils.updateDistance(activity, SubwayLineDirectionFragment.this.stations, activity.getLocation());
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				MyLog.v(TAG, "refreshSubwayStationListFromDB() > onPostExecute()");
				SubwayLineInfo activity = SubwayLineDirectionFragment.this.getSubwayLineInfoActivity();
				View view = SubwayLineDirectionFragment.this.getLastView();
				if (activity == null || view == null) {
					return;
				}
				generateOrderedStationsIds();
				refreshFavoriteStationIdsFromDB(activity.getContentResolver());
				notifyDataSetChanged(true);
				int index = selectedStationIndex();
				MyLog.d(TAG, "refreshSubwayStationListFromDB() > index: " + index);
				if (index > 0) {
					index--; // show one more stop on top
				}
				SupportFactory.get().listViewScrollTo((ListView) view.findViewById(R.id.list), index, 50);
			};

		}.execute(lineNumber, stationId);
	}

	/**
	 * @return the current station index
	 */
	private int selectedStationIndex() {
		// MyLog.v(TAG, "selectedStationIndex()");
		if (this.stations != null) {
			for (int i = 0; i < this.stations.size(); i++) {
				if (this.currentStationId != null) {
					if (this.stations.get(i).getId().equals(this.currentStationId)) {
						return i;
					}
				} else if (this.closestStationId != null) {
					if (this.stations.get(i).getId().equals(this.closestStationId)) {
						return i;
					}
				}
			}
		}
		return 0;
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
	protected void updateDistancesWithNewLocation(SubwayLineInfo activity) {
		// MyLog.v(TAG, "updateDistancesWithNewLocation()");
		if (this.stations == null || activity == null) {
			return;
		}
		Location currentLocation = activity.getLocation();
		if (currentLocation == null) {
			return;
		}
		LocationUtils.updateDistance(activity, this.stations, currentLocation, new LocationTaskCompleted() {

			@Override
			public void onLocationTaskCompleted() {
				String previousClosest = SubwayLineDirectionFragment.this.closestStationId;
				generateOrderedStationsIds();
				notifyDataSetChanged(SubwayLineDirectionFragment.this.closestStationId == null ? false : SubwayLineDirectionFragment.this.closestStationId
						.equals(previousClosest));
			}
		});
	}

	/**
	 * @return see {@link #getLastActivity()}
	 */
	private SubwayLineInfo getSubwayLineInfoActivity() {
		return (SubwayLineInfo) getLastActivity();
	}

	/**
	 * Show the closest subway line station (if possible).
	 * @return true if action performed
	 */
	protected boolean showClosestStation() {
		MyLog.v(TAG, "showClosestStation()");
		Activity activity = getLastActivity();
		if (activity != null && !TextUtils.isEmpty(this.closestStationId)) {
			Toast.makeText(activity, R.string.shake_closest_subway_line_station_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(activity, SubwayStationInfo.class);
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
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	protected void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		Location currentLocation = getSubwayLineInfoActivity() == null ? null : getSubwayLineInfoActivity().getLocation();
		if (currentLocation == null || orientation == 0 || this.stations == null) {
			// MyLog.d(TAG, "updateCompass() > no location or no POI");
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(getLastActivity(), this.stations, force, currentLocation, orientation, now, this.scrollState, this.lastCompassChanged,
				this.lastCompassInDegree, R.drawable.heading_arrow, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							SubwayLineDirectionFragment.this.lastCompassInDegree = (int) orientation;
							SubwayLineDirectionFragment.this.lastCompassChanged = now;
							// update the view
							notifyDataSetChanged(false);
						}

					}
				});

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
		this.closestStationId = orderedStations.get(0).getDistance() > 0 ? orderedStations.get(0).getId() : null;
	}

	/**
	 * Find favorites subway station IDs.
	 */
	private void refreshFavoriteStationIdsFromDB(final ContentResolver contentResolver) {
		this.favStationsIds = new ArrayList<String>(); // clear list
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findFavsByTypeList(contentResolver, DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
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
		View v = getLastView();
		if (v != null && view == v.findViewById(R.id.list)) {
			this.scrollState = scrollState;// == OnScrollListener.SCROLL_STATE_FLING);
		}
	}

	static class ViewHolder {
		TextView placeTv;
		TextView stationNameTv;
		TextView distanceTv;
		ImageView subwayImg1;
		ImageView subwayImg2;
		ImageView subwayImg3;
		ImageView favImg;
		ImageView compassImg;
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
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(viewId, parent, false);
				holder = new ViewHolder();
				holder.placeTv = (TextView) convertView.findViewById(R.id.place);
				holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				holder.subwayImg1 = (ImageView) convertView.findViewById(R.id.subway_img_1);
				holder.subwayImg2 = (ImageView) convertView.findViewById(R.id.subway_img_2);
				holder.subwayImg3 = (ImageView) convertView.findViewById(R.id.subway_img_3);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ASubwayStation station = getItem(position);
			if (station != null) {
				// station name
				holder.stationNameTv.setText(station.getName());
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
					holder.subwayImg1.setImageResource(subwayLineImgId);
				} else {
					int lastIndex = otherLines.size() - 1;
					int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(lastIndex));
					holder.subwayImg1.setImageResource(otherLineImg);
				}
				// color 2 (on the middle)
				if (otherLines.size() < 1) {
					holder.subwayImg2.setVisibility(View.GONE);
				} else {
					holder.subwayImg2.setVisibility(View.VISIBLE);
					if (otherLines.size() == 1) {
						holder.subwayImg2.setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(0));
						holder.subwayImg2.setImageResource(otherLineImg);
					}
				}
				// color 3 (on the left, closer to the border)
				if (otherLines.size() < 2) {
					holder.subwayImg3.setVisibility(View.GONE);
				} else {
					holder.subwayImg3.setVisibility(View.VISIBLE);
					if (otherLines.size() == 2) {
						holder.subwayImg3.setImageResource(subwayLineImgId);
					} else {
						int otherLineImg = SubwayUtils.getSubwayLineImgListId(otherLines.get(1));
						holder.subwayImg3.setImageResource(otherLineImg);
					}
				}
				// favorite
				if (SubwayLineDirectionFragment.this.favStationsIds != null && SubwayLineDirectionFragment.this.favStationsIds.contains(station.getId())) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// station distance
				if (!TextUtils.isEmpty(station.getDistanceString())) {
					holder.distanceTv.setText(station.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.INVISIBLE);
				}
				// station compass
				if (station.getCompassMatrixOrNull() != null) {
					holder.compassImg.setImageMatrix(station.getCompassMatrix());
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.INVISIBLE);
				}
				// set style for closest subway station
				int index = -1;
				if (!TextUtils.isEmpty(SubwayLineDirectionFragment.this.closestStationId)) {
					index = station.getId().equals(SubwayLineDirectionFragment.this.closestStationId) ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow_light);
					break;
				default:
					holder.stationNameTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					holder.compassImg.setImageResource(R.drawable.heading_arrow);
					break;
				}
			}
			return convertView;
		}
	}
}

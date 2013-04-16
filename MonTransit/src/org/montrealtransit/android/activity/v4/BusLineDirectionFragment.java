package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.data.ABusStop;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.BusStop;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
 * This fragment shows the bus line stops for a specific direction.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class BusLineDirectionFragment extends Fragment implements OnScrollListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineDirectionFragment.class.getSimpleName();
	/**
	 * The minimum between 2 {@link ArrayAdapter#notifyDataSetChanged()} in milliseconds.
	 */
	private static final int ADAPTER_NOTIFY_THRESOLD = 150; // 0.15 seconds

	/**
	 * The bus stops list adapter.
	 */
	private ArrayAdapter<ABusStop> adapter;
	/**
	 * The closest bus line stop code by distance.
	 */
	private String closestStopCode;
	/**
	 * The list of bus stops.
	 */
	private List<ABusStop> busStops = new ArrayList<ABusStop>();
	/**
	 * The favorite bus stops codes.
	 */
	private List<String> favStopCodes;
	/**
	 * The bus line number.
	 */
	private String busLineNumber;
	/**
	 * The bus line direction ID.
	 */
	private String busLineDirectionId;
	/**
	 * The last {@link ArrayAdapter#notifyDataSetChanged() time-stamp in milliseconds.
	 */
	private long lastNotifyDataSetChanged = -1;
	/**
	 * The scroll state of the list.
	 */
	private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * @param busLineNumber bus line number
	 * @param directionId direction ID
	 * @return the fragment
	 */
	public static Fragment newInstance(String busLineNumber, String directionId) {
		MyLog.v(TAG, "newInstance(%s,%s)", busLineNumber, directionId);
		BusLineDirectionFragment f = new BusLineDirectionFragment();

		Bundle args = new Bundle();
		args.putString(BusLineInfo.EXTRA_LINE_NUMBER, busLineNumber);
		args.putString(BusLineInfo.EXTRA_LINE_DIRECTION_ID, directionId);
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
		View v = inflater.inflate(R.layout.bus_line_info_stops_list, container, false);
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
		String lineNumber = getArguments().getString(BusLineInfo.EXTRA_LINE_NUMBER);
		String lineDirectionId = getArguments().getString(BusLineInfo.EXTRA_LINE_DIRECTION_ID);
		this.busLineNumber = lineNumber;
		this.busLineDirectionId = lineDirectionId;
		this.busStops = null;
		this.adapter = null;
		refreshBusStopListFromDB();
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
		list.setOnScrollListener(BusLineDirectionFragment.this);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s,%s)", position, id);
				if (BusLineDirectionFragment.this.busStops != null && position < BusLineDirectionFragment.this.busStops.size()
						&& BusLineDirectionFragment.this.busStops.get(position) != null
						&& !TextUtils.isEmpty(BusLineDirectionFragment.this.busStops.get(position).getCode())) {
					// IF last bus stop, show descent only
					if (position + 1 == BusLineDirectionFragment.this.busStops.size()) {
						Toast toast = Toast.makeText(BusLineDirectionFragment.this.getLastActivity(), R.string.bus_stop_descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						return;
					}
					ABusStop selectedBusStop = BusLineDirectionFragment.this.busStops.get(position);
					Intent intent = new Intent(BusLineDirectionFragment.this.getLastActivity(), BusStopInfo.class);
					intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, selectedBusStop.getCode());
					intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, selectedBusStop.getPlace());
					intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, BusLineDirectionFragment.this.busLineNumber);
					if (getBusLineInfoActivity() != null && getBusLineInfoActivity().getBusLine() != null) {
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, getBusLineInfoActivity().getBusLine().getName());
						intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, getBusLineInfoActivity().getBusLine().getType());
					}
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

	public void onResumeWithFocus(BusLineInfo activity) {
		MyLog.v(TAG, "onResumeWithFocus()");
		updateDistancesWithNewLocation(activity);
		refreshFavoriteStopCodesFromDB(activity.getContentResolver());
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
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopListFromDB() { // TODO extract?
		new AsyncTask<Void, Void, ABusStop[]>() {
			@Override
			protected ABusStop[] doInBackground(Void... params) {
				List<BusStop> busStopList = StmManager.findBusLineStopsList(BusLineDirectionFragment.this.getLastActivity().getContentResolver(),
						BusLineDirectionFragment.this.busLineNumber, BusLineDirectionFragment.this.busLineDirectionId);
				// creating the list of the subways stations object
				ABusStop[] busStops = new ABusStop[busStopList.size()];
				int i = 0;
				for (BusStop busStop : busStopList) {
					ABusStop aBusStop = new ABusStop();
					aBusStop.setCode(busStop.getCode());
					aBusStop.setDirectionId(busStop.getDirectionId());
					aBusStop.setPlace(BusUtils.cleanBusStopPlace(busStop.getPlace()));
					aBusStop.setSubwayStationId(busStop.getSubwayStationId());
					aBusStop.setSubwayStationName(busStop.getSubwayStationNameOrNull());
					aBusStop.setLineNumber(busStop.getLineNumber());
					aBusStop.setLineNumber(busStop.getLineNameOrNull());
					aBusStop.setLineType(busStop.getLineTypeOrNull());
					aBusStop.setLat(busStop.getLat());
					aBusStop.setLng(busStop.getLng());
					busStops[i] = aBusStop;
					i++;
				}
				return busStops;
			}

			@Override
			protected void onPostExecute(ABusStop[] result) {
				BusLineInfo activity = BusLineDirectionFragment.this.getBusLineInfoActivity();
				View view = BusLineDirectionFragment.this.getLastView();
				if (activity == null || view == null) {
					return; // too late, the parent activity is gone
				}
				BusLineDirectionFragment.this.busStops = Arrays.asList(result);
				generateOrderedStopCodes();
				refreshFavoriteStopCodesFromDB(activity.getContentResolver());
				BusLineDirectionFragment.this.adapter = new ArrayAdapterWithCustomView(activity, R.layout.bus_line_info_stops_list_item);
				((ListView) view.findViewById(R.id.list)).setAdapter(BusLineDirectionFragment.this.adapter);
				updateDistancesWithNewLocation(activity); // force update all bus stops with location
			};

		}.execute();
	}

	/**
	 * Update the distances with the latest device location.
	 */
	protected void updateDistancesWithNewLocation(BusLineInfo activity) {
		// MyLog.v(TAG, "updateDistancesWithNewLocation()");
		// BusLineInfo activity = getBusLineInfoActivity();
		if (this.busStops == null || activity == null) {
			return;
		}
		Location currentLocation = activity.getLocation();
		if (currentLocation != null) {
			float accuracyInMeters = currentLocation.getAccuracy();
			boolean isDetailed = UserPreferences.getPrefDefault(activity, UserPreferences.PREFS_DISTANCE, UserPreferences.PREFS_DISTANCE_DEFAULT).equals(
					UserPreferences.PREFS_DISTANCE_DETAILED);
			String distanceUnit = UserPreferences.getPrefDefault(activity, UserPreferences.PREFS_DISTANCE_UNIT, UserPreferences.PREFS_DISTANCE_UNIT_DEFAULT);
			for (ABusStop busStop : this.busStops) {
				// IF the bus stop location is known DO
				busStop.setDistance(currentLocation.distanceTo(busStop.getLocation()));
				busStop.setDistanceString(Utils.getDistanceString(busStop.getDistance(), accuracyInMeters, isDetailed, distanceUnit));
			}
			String previousClosest = this.closestStopCode;
			generateOrderedStopCodes();
			notifyDataSetChanged(this.closestStopCode == null ? false : this.closestStopCode.equals(previousClosest));
		}
	}

	/**
	 * @return see {@link #getLastActivity()}
	 */
	private BusLineInfo getBusLineInfoActivity() {
		return (BusLineInfo) getLastActivity();
	}

	/**
	 * Show the closest bus line stop (if possible).
	 * @return true if action performed
	 */
	protected boolean showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (!TextUtils.isEmpty(this.closestStopCode)) {
			Toast.makeText(getLastActivity(), R.string.shake_closest_bus_line_stop_selected, Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(getLastActivity(), BusStopInfo.class);
			intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, this.closestStopCode);
			intent.putExtra(BusStopInfo.EXTRA_STOP_PLACE, findStopPlace(this.closestStopCode));
			intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, this.busLineNumber);
			if (getBusLineInfoActivity() != null && getBusLineInfoActivity().getBusLine() != null) {
				intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NAME, getBusLineInfoActivity().getBusLine().getName());
				intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_TYPE, getBusLineInfoActivity().getBusLine().getType());
			}
			startActivity(intent);
			return true;
		}
		return false;
	}

	/**
	 * @param stopCode a bus stop code
	 * @return a bus stop place or null
	 */
	private String findStopPlace(String stopCode) {
		if (this.busStops == null) {
			return null;
		}
		for (BusStop busStop : this.busStops) {
			if (busStop.getCode().equals(stopCode)) {
				return busStop.getPlace();
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
		Location currentLocation = getBusLineInfoActivity() == null ? null : getBusLineInfoActivity().getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update closest bus stops compass
				if (this.busStops != null) {
					for (ABusStop busStop : this.busStops) {
						busStop.getCompassMatrix().reset();
						busStop.getCompassMatrix().postRotate(
								SensorUtils.getCompassRotationInDegree(getLastActivity(), currentLocation, busStop.getLocation(), orientation,
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
		Location currentLocation = getBusLineInfoActivity() == null ? null : getBusLineInfoActivity().getLocation();
		if (this.locationDeclination == null && currentLocation != null) {
			this.locationDeclination = new GeomagneticField((float) currentLocation.getLatitude(), (float) currentLocation.getLongitude(),
					(float) currentLocation.getAltitude(), currentLocation.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	public Pair<Integer, Integer> getArrowDim() {
		if (this.arrowDim == null) {
			this.arrowDim = SensorUtils.getResourceDimension(getLastActivity(), R.drawable.heading_arrow);
		}
		return this.arrowDim;
	}

	/**
	 * Generate the ordered bus line stops codes.
	 */
	public void generateOrderedStopCodes() {
		if (this.busStops.size() == 0) {
			return;
		}
		List<ABusStop> orderedStops = new ArrayList<ABusStop>(this.busStops);
		// order the stations list by distance (closest first)
		Collections.sort(orderedStops, new Comparator<ABusStop>() {
			@Override
			public int compare(ABusStop lhs, ABusStop rhs) {
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
		this.closestStopCode = orderedStops.get(0).getCode();
	}

	/**
	 * Find favorites bus stop codes.
	 */
	private void refreshFavoriteStopCodesFromDB(final ContentResolver contentResolver) {
		MyLog.v(TAG, "refreshFavoriteStopCodesFromDB()");
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				// TODO filter by fkid2 (bus line number)
				// Activity activity = getLastActivity();
				// if (activity == null) {
				// // MyLog.d(TAG, "refreshFavoriteStopCodesFromDB() > activity is null!");
				// return null;
				// }
				return DataManager.findFavsByTypeList(contentResolver, DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				boolean newFav = false;
				if (Utils.getCollectionSize(result) != Utils.getCollectionSize(BusLineDirectionFragment.this.favStopCodes)) {
					newFav = true; // different size => different favorites
				}
				List<String> newfavStopCodes = new ArrayList<String>();
				for (Fav busStopFav : result) {
					if (BusLineDirectionFragment.this.busLineNumber.equals(busStopFav.getFkId2())) { // check lin number
						if (BusLineDirectionFragment.this.favStopCodes == null || !BusLineDirectionFragment.this.favStopCodes.contains(busStopFav.getFkId())) {
							newFav = true; // new favorite
						}
						newfavStopCodes.add(busStopFav.getFkId()); // store stop code
					}
				}
				BusLineDirectionFragment.this.favStopCodes = newfavStopCodes;
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
		TextView stopCodeTv;
		TextView placeTv;
		TextView stationNameTv;
		TextView distanceTv;
		ImageView subwayImg;
		ImageView favImg;
		ImageView compassImg;
	}

	/**
	 * A custom array adapter with custom {@link ArrayAdapterWithCustomView#getView(int, View, ViewGroup)}
	 */
	private class ArrayAdapterWithCustomView extends ArrayAdapter<ABusStop> {

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
			super(context, viewId);
			this.viewId = viewId;
			this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return BusLineDirectionFragment.this.busStops == null ? 0 : BusLineDirectionFragment.this.busStops.size();
		}

		@Override
		public int getPosition(ABusStop item) {
			return BusLineDirectionFragment.this.busStops.indexOf(item);
		}

		@Override
		public ABusStop getItem(int position) {
			return BusLineDirectionFragment.this.busStops.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// MyLog.v(TAG, "getView(%s)", position);
			ViewHolder holder;
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(this.viewId, parent, false);
				holder = new ViewHolder();
				holder.stopCodeTv = (TextView) convertView.findViewById(R.id.stop_code);
				holder.placeTv = (TextView) convertView.findViewById(R.id.place);
				holder.stationNameTv = (TextView) convertView.findViewById(R.id.station_name);
				holder.subwayImg = (ImageView) convertView.findViewById(R.id.subway_img);
				holder.favImg = (ImageView) convertView.findViewById(R.id.fav_img);
				holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
				holder.compassImg = (ImageView) convertView.findViewById(R.id.compass);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			ABusStop busStop = getItem(position);
			if (busStop != null) {
				// bus stop code
				holder.stopCodeTv.setText(busStop.getCode());
				// bus stop place
				holder.placeTv.setText(busStop.getPlace());
				// bus stop subway station
				if (!TextUtils.isEmpty(busStop.getSubwayStationId()) && !TextUtils.isEmpty(busStop.getSubwayStationNameOrNull())) {
					holder.subwayImg.setVisibility(View.VISIBLE);
					holder.stationNameTv.setText(busStop.getSubwayStationNameOrNull());
					holder.stationNameTv.setVisibility(View.VISIBLE);
				} else {
					holder.subwayImg.setVisibility(View.GONE);
					holder.stationNameTv.setVisibility(View.GONE);
				}
				// favorite
				if (BusLineDirectionFragment.this.favStopCodes != null && BusLineDirectionFragment.this.favStopCodes.contains(busStop.getCode())) {
					holder.favImg.setVisibility(View.VISIBLE);
				} else {
					holder.favImg.setVisibility(View.GONE);
				}
				// bus stop distance
				if (!TextUtils.isEmpty(busStop.getDistanceString())) {
					holder.distanceTv.setText(busStop.getDistanceString());
					holder.distanceTv.setVisibility(View.VISIBLE);
				} else {
					holder.distanceTv.setVisibility(View.GONE);
				}
				// bus stop compass
				if (busStop.getCompassMatrixOrNull() != null) {
					holder.compassImg.setImageMatrix(busStop.getCompassMatrix());
					holder.compassImg.setVisibility(View.VISIBLE);
				} else {
					holder.compassImg.setVisibility(View.GONE);
				}
				// set style for closest bus stop
				int index = -1;
				if (!TextUtils.isEmpty(BusLineDirectionFragment.this.closestStopCode)) {
					index = busStop.getCode().equals(BusLineDirectionFragment.this.closestStopCode) ? 0 : 999;
				}
				switch (index) {
				case 0:
					holder.placeTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
					holder.distanceTv.setTextColor(Utils.getTextColorPrimary(getContext()));
					break;
				default:
					holder.placeTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTypeface(Typeface.DEFAULT);
					holder.distanceTv.setTextColor(Utils.getTextColorSecondary(getContext()));
					break;
				}
			}
			return convertView;
		}
	}
}

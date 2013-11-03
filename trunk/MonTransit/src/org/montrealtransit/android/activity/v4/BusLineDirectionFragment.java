package org.montrealtransit.android.activity.v4;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.activity.BusStopInfo;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.location.Location;
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
 * This fragment shows the bus line stops for a specific direction.
 * @author Mathieu Méa
 */
@TargetApi(4)
public class BusLineDirectionFragment extends Fragment {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusLineDirectionFragment.class.getSimpleName();

	/**
	 * The bus stops list adapter.
	 */
	private POIArrayAdapter adapter;
	/**
	 * The selected bus stop code
	 */
	private String currentStopUID;

	/**
	 * @param busLineNumber bus line number
	 * @param directionId direction ID
	 * @return the fragment
	 */
	public static Fragment newInstance(String busLineNumber, String directionId, String stopCode) {
		// MyLog.v(TAG, "newInstance(%s,%s,%s)", busLineNumber, directionId, stopCode);
		BusLineDirectionFragment f = new BusLineDirectionFragment();

		Bundle args = new Bundle();
		args.putString(BusLineInfo.EXTRA_ROUTE_SHORT_NAME, busLineNumber);
		args.putString(BusLineInfo.EXTRA_LINE_TRIP_ID, directionId);
		args.putString(BusLineInfo.EXTRA_LINE_STOP_CODE, stopCode);
		f.setArguments(args);

		return f;
	}

	public static Fragment newInstance(Route route, Trip trip, String stopCode) {
		// MyLog.v(TAG, "newInstance(%s,%s)", trip, stopCode);
		BusLineDirectionFragment f = new BusLineDirectionFragment();

		Bundle args = new Bundle();
		args.putString(BusLineInfo.EXTRA_ROUTE_SHORT_NAME, route.shortName);
		args.putString(BusLineInfo.EXTRA_LINE_TRIP_ID, String.valueOf(trip.id));
		args.putString(BusLineInfo.EXTRA_LINE_STOP_CODE, stopCode);
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
		String lineDirectionId = getArguments().getString(BusLineInfo.EXTRA_LINE_TRIP_ID);
		String currentStopCode = getArguments().getString(BusLineInfo.EXTRA_LINE_STOP_CODE);
		ListView listView = (ListView) getLastView().findViewById(R.id.list);
		this.adapter = new POIArrayAdapter(getLastActivity());
		this.adapter.setShakeEnabled(true);
		this.adapter.setPois(null);
		this.adapter.setIntentExtras(BusStopInfo.newInstanceExtra(getBusLineInfoActivity().getBusLine()));
		listView.setAdapter(this.adapter);
		refreshBusStopListFromDB(lineDirectionId, currentStopCode);
	}

	/**
	 * Setup list.
	 * @param list the list
	 * @param emptyView the empty view
	 */
	private void setupList(ListView list, View emptyView) {
		MyLog.v(TAG, "setupList()");
		list.setEmptyView(emptyView);
		list.setOnScrollListener(this.adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				MyLog.v(TAG, "onItemClick(%s,%s)", position, id);
				if (BusLineDirectionFragment.this.adapter != null && position < BusLineDirectionFragment.this.adapter.getPoisCount()
						&& BusLineDirectionFragment.this.adapter.getPoi(position) != null) {
					// IF last bus stop, show descent only
					if (position + 1 == BusLineDirectionFragment.this.adapter.getPoisCount()) {
						Toast toast = Toast.makeText(BusLineDirectionFragment.this.getLastActivity(), R.string.descent_only, Toast.LENGTH_SHORT);
						// toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
						// return; why not?
					}
					BusLineDirectionFragment.this.adapter.onItemClick(parent, view, position, id);
				}
			}
		});
	}

	public void onResumeWithFocus(BusLineInfo activity) {
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
	 * Refresh the bus stops list UI.
	 */
	private void refreshBusStopListFromDB(String lineDirectionId, String stopCode) { // TODO extract?
		MyLog.v(TAG, "refreshBusStopListFromDB(%s,%s)", lineDirectionId, stopCode);
		new AsyncTask<String, Void, Void>() {
			@Override
			protected Void doInBackground(String... params) {
				// MyLog.d(TAG, "refreshBusStopListFromDB() > doInBackground()");
				String lineDirectionId = params[0], stopCode = params[1];
				BusLineInfo activity = BusLineDirectionFragment.this.getBusLineInfoActivity();
				List<TripStop> busStopList = StmBusManager.findStopsWithTripIdList(activity, lineDirectionId);
				if (busStopList != null) {
					// creating the list of the subways stations object
					for (TripStop busStop : busStopList) {
						if (busStop.stop.code.equals(stopCode)) {
							BusLineDirectionFragment.this.currentStopUID = busStop.getUID();
						}
					}
					BusLineDirectionFragment.this.adapter.setPois(busStopList);
				} else {
					BusLineDirectionFragment.this.adapter.setPois(new ArrayList<TripStop>()); // TODO what's wrong with null?
				}
				// force update all bus stops with location
				// MyLog.d(TAG, "activity.getLocation(): " + activity.getLocation());
				BusLineDirectionFragment.this.adapter.updateDistancesNow(activity.getLocation());
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				MyLog.d(TAG, "refreshBusStopListFromDB() > onPostExecute()");
				BusLineInfo activity = BusLineDirectionFragment.this.getBusLineInfoActivity();
				View view = BusLineDirectionFragment.this.getLastView();
				if (activity == null || view == null) {
					return; // too late, the parent activity is gone
				}
				refreshFavoriteStopCodesFromDB(activity.getContentResolver());
				ListView listView = (ListView) view.findViewById(R.id.list);
				int index = selectedStopIndex();
				// MyLog.d(TAG, "refreshBusStopListFromDB() > index: " + index);
				if (index > 0) {
					index--; // show 1 more stop on top of the list
				}
				SupportFactory.get().listViewScrollTo(listView, index, 50);
				BusLineDirectionFragment.this.adapter.updateCompassNow();
			}

		}.execute(lineDirectionId, stopCode);
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
	private BusLineInfo getBusLineInfoActivity() {
		return (BusLineInfo) getLastActivity();
	}

	/**
	 * Show the closest bus line stop (if possible).
	 * @return true if action performed
	 */
	protected boolean showClosestStop() {
		MyLog.v(TAG, "showClosestStop()");
		if (this.adapter.hasClosestPOI()) {
			Toast.makeText(getLastActivity(), R.string.shake_closest_bus_line_stop_selected, Toast.LENGTH_SHORT).show();
			return this.adapter.showClosestPOI();
		}
		return false;
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	protected void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation);
		this.adapter.updateCompass(orientation, force);
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
				BusLineDirectionFragment.this.adapter.setFavs(result);
			};
		}.execute();
	}

}

package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.LocationUtils.LocationTaskCompleted;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.data.POI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmBusManager;
import org.montrealtransit.android.services.ClosestSubwayStationsFinderTask;
import org.montrealtransit.android.services.GeocodingTask;
import org.montrealtransit.android.services.GeocodingTaskListener;
import org.montrealtransit.android.services.LoadNextBusStopIntoCacheTask;
import org.montrealtransit.android.services.NfcListener;
import org.montrealtransit.android.services.nextstop.IStmInfoTask;
import org.montrealtransit.android.services.nextstop.NextStopListener;
import org.montrealtransit.android.services.nextstop.StmBusScheduleTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements LocationListener, DialogInterface.OnClickListener, NfcListener, SensorEventListener, CompassListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = BusStopInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/BusStop";
	/**
	 * The extra ID for the bus stop code.
	 */
	private static final String EXTRA_STOP_CODE = "extra_stop_code";
	/**
	 * The extra ID for the bus stop place (not required).
	 */
	private static final String EXTRA_STOP_NAME = "extra_stop_name";
	/**
	 * The extra ID for the bus line number.
	 */
	private static final String EXTRA_BUS_LINE_SHORT_NAME = "extra_line_number";

	private static final String EXTRA_ROUTE_COLOR = "extra_line_color";

	private static final String EXTRA_ROUTE_TEXT_COLOR = "extra_line_text_color";

	private static final String EXTRA_LINE_DIRECTION = "extra_trip_headsign";

	/**
	 * The NFC MIME type.
	 */
	private static final String MIME_TYPE = "application/org.montrealtransit.android.bus.stop";
	/**
	 * The bus stop.
	 */
	private RouteTripStop routeTripStop;
	/**
	 * Store the current hours (including messages).
	 */
	private BusStopHours hours;
	/**
	 * The cache for the current bus stop (code+line number).
	 */
	private Cache memCache;
	/**
	 * The task used to load the next bus stops.
	 */
	private LoadNextBusStopIntoCacheTask wwwTask;
	/**
	 * The other bus stop lines.
	 */
	protected List<RouteTripStop> otherBusStopLines;
	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues = new float[3];
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues = new float[3];
	/**
	 * The last compass degree.
	 */
	private int lastCompassInDegree = -1;
	/**
	 * The last {@link #updateCompass(float[])} time-stamp in milliseconds.
	 */
	private long lastCompassChanged = -1;
	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;
	/**
	 * Is the location updates enabled?
	 */
	private boolean compassUpdatesEnabled = false;

	private POIArrayAdapter adapter;

	public static Intent newInstance(Context context, Stop stop, Route route) {
		Intent intent = new Intent(context, BusStopInfo.class);
		intent.putExtras(newInstanceExtra(stop));
		intent.putExtras(newInstanceExtra(route));
		return intent;
	}

	public static Intent newInstance(Context context, RouteTripStop routeTripStop) {
		Intent intent = new Intent(context, BusStopInfo.class);
		intent.putExtras(newInstanceExtra(routeTripStop.stop));
		intent.putExtras(newInstanceExtra(routeTripStop.trip, context));
		intent.putExtras(newInstanceExtra(routeTripStop.route));
		return intent;
	}

	public static Intent newInstance(Context context, TripStop tripStop) {
		Intent intent = new Intent(context, BusStopInfo.class);
		intent.putExtras(newInstanceExtra(tripStop.stop));
		intent.putExtras(newInstanceExtra(tripStop.trip, context));
		if (tripStop.trip != null) { // TODO actually not the route short name !
			intent.putExtra(EXTRA_BUS_LINE_SHORT_NAME, String.valueOf(tripStop.trip.routeId));
		}
		return intent;
	}

	public static Intent newInstance(Context context, Stop stop) {
		Intent intent = new Intent(context, BusStopInfo.class);
		intent.putExtras(newInstanceExtra(stop));
		return intent;
	}

	public static Intent newInstance(Context context, String stopCode) {
		return newInstance(context, stopCode, null);
	}

	public static Intent newInstance(Context context, String stopCode, String lineNumber) {
		Intent intent = new Intent(context, BusStopInfo.class);
		intent.putExtra(EXTRA_STOP_CODE, stopCode);
		if (!TextUtils.isEmpty(lineNumber)) {
			intent.putExtra(EXTRA_BUS_LINE_SHORT_NAME, lineNumber);
		}
		return intent;
	}

	public static Bundle newInstanceExtra(Route route) {
		Bundle extras = new Bundle();
		if (route != null) {
			extras.putString(EXTRA_BUS_LINE_SHORT_NAME, route.shortName);
			// extras.putString(EXTRA_BUS_LINE_LONG_NAME, route.longName);
			extras.putString(EXTRA_ROUTE_COLOR, route.color);
			extras.putString(EXTRA_ROUTE_TEXT_COLOR, route.textColor);
		}
		return extras;
	}

	public static Bundle newInstanceExtra(Trip trip, Context context) {
		Bundle extras = new Bundle();
		if (trip != null) {
			extras.putString(EXTRA_LINE_DIRECTION, trip.getHeading(context).toUpperCase(Locale.getDefault()));
		}
		return extras;
	}

	public static Bundle newInstanceExtra(Stop stop) {
		Bundle extras = new Bundle();
		if (stop != null) {
			extras.putString(EXTRA_STOP_CODE, stop.code);
			extras.putString(EXTRA_STOP_NAME, stop.name);
		}
		return extras;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_info);

		this.adapter = new POIArrayAdapter(this);
		this.adapter.setManualLayout((ViewGroup) findViewById(R.id.nearby_list));
		this.adapter.setManualScrollView((ScrollView) findViewById(R.id.scrollview));

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		SupportFactory.get().registerNfcCallback(this, this, MIME_TYPE);
		SupportFactory.get().setOnNdefPushCompleteCallback(this, this);
	}

	/**
	 * onCreate() method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		findViewById(R.id.next_stops_refresh).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshOrStopRefreshNextStops(v);
			}
		});
		findViewById(R.id.next_stops_section_info).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showNextStopsInfoDialog(v);
			}
		});
		findViewById(R.id.star).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addOrRemoveFavorite(v);
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		setIntent(intent);
	}

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;
	private float locationDeclination;

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		MyLog.v(TAG, "onWindowFocusChanged(%s)", hasFocus);
		// IF the activity just regained the focus DO
		if (!this.hasFocus && hasFocus) {
			// try to enable everything but may fail if the activity is not yet resumed
			try {
				onResumeWithFocus();
			} catch (Throwable t) {
				// not that bad, will do it when the activity #onResume() method is called
			}
		}
		this.hasFocus = hasFocus;
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		this.paused = false;
		super.onResume();
		// IF the activity has the focus DO
		if (this.hasFocus) {
			onResumeWithFocus();
		}
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
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		refreshFavoriteIDsFromDB();
		setBusStopFromIntent(getIntent(), null);
		setIntent(null); // set intent as processed
		SupportFactory.get().enableNfcForegroundDispatch(this);
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		SupportFactory.get().disableNfcForegroundDispatch(this);
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.compassUpdatesEnabled = false;
		}
		super.onPause();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged(%s)", accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		SensorUtils.checkForCompass(this, event, this.accelerometerValues, this.magneticFieldValues, this);
	}

	/**
	 * Update the compass image(s).
	 * @param orientation the new orientation
	 */
	@Override
	public void updateCompass(final float orientation, boolean force) {
		// MyLog.v(TAG, "updateCompass(%s)", orientation[0]);
		if (this.routeTripStop == null) {
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							BusStopInfo.this.lastCompassInDegree = (int) orientation;
							BusStopInfo.this.lastCompassChanged = now;
							// update the view
							ImageView compassImg = (ImageView) findViewById(R.id.compass);
							if (location.getAccuracy() <= routeTripStop.getDistance()) {
								float compassRotation = SensorUtils.getCompassRotationInDegree(location, routeTripStop, lastCompassInDegree,
										locationDeclination);
								SupportFactory.get().rotateImageView(compassImg, compassRotation, BusStopInfo.this);
								compassImg.setVisibility(View.VISIBLE);
							} else {
								compassImg.setVisibility(View.INVISIBLE);
							}
						}
					}
				});
	}

	/**
	 * Retrieve the bus stop from the Intent or from the Bundle.
	 * @param intent the intent
	 * @param savedInstanceState the saved instance state (Bundle)
	 */
	private void setBusStopFromIntent(Intent intent, Bundle savedInstanceState) {
		MyLog.v(TAG, "setBusStopFromIntent()");
		if (intent != null) {
			String stopCode;
			String stopPlace = null;
			String lineNumber;
			String lineColor = null;
			String lineTextColor = null;
			String lineDirection = null;
			if (SupportFactory.get().isNfcIntent(intent)) {
				SupportFactory.get().processNfcIntent(intent, this);
				return;
			} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				String pathSegment = intent.getData().getPathSegments().get(1);
				stopCode = pathSegment.substring(0, 5);
				lineNumber = pathSegment.substring(5);
			} else {
				stopCode = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_CODE);
				stopPlace = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_NAME);
				lineNumber = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_BUS_LINE_SHORT_NAME);
				lineColor = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_ROUTE_COLOR);
				lineTextColor = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
				lineDirection = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_LINE_DIRECTION);
			}
			showNewBusStop(stopCode, stopPlace, lineNumber, lineColor, lineTextColor, lineDirection);
		}
	}

	@Override
	public String[] getNfcMimeMessages() {
		MyLog.v(TAG, "getNfcMimeMessages()");
		List<String> msg = new ArrayList<String>();
		// add bus stop code
		msg.add(this.routeTripStop.stop.code);
		// add bus line number
		msg.add(this.routeTripStop.route.shortName);
		// add next stops if loaded
		if (this.hours != null) {
			BusStopHours tmp = this.hours;
			tmp.setSourceName(getString(R.string.nfc));
			msg.add(tmp.serialized());
		}
		return msg.toArray(new String[] {});
	}

	@Override
	public void onNfcPushComplete() {
		// MyLog.v(TAG, "onNfcPushComplete()");
	}

	@Override
	public void processNfcRecords(String[] stringRecords) {
		MyLog.v(TAG, "processNfcRecords(%s)", stringRecords.length);
		// extract bus stop code
		String stopCode = stringRecords[0];
		// extract bus line number
		String lineNumber = stringRecords[1];
		// extract next stops if provided
		if (stringRecords.length > 2) {
			try {
				BusStopHours tmp = BusStopHours.deserialized(stringRecords[2]);
				if (tmp != null && tmp.getSHours().size() != 0) {
					saveToMemCache(stopCode, lineNumber, tmp);
					this.hours = tmp;
					// } else {
					// MyLog.d(TAG, "No bus stop hours from the NFC record!");
				}
			} catch (Exception e) {
				MyLog.w(TAG, e, "Something went wrong while parsing the bus stop hours!");
			}
		}
		// show the bus stop
		showNewBusStop(stopCode, null, lineNumber, null, null, null);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// save the current hours
		return this.hours != null ? this.hours : null;
	}

	/**
	 * Show the next stops info dialog
	 * @param v useless - can be null
	 */
	public void showNextStopsInfoDialog(View v) {
		MyLog.v(TAG, "showNextStopsInfoDialog()");
		String message;
		if (this.hours != null) {
			message = getString(R.string.next_bus_stops_message_and_source, IStmInfoTask.SOURCE_NAME);
		} else {
			message = getString(R.string.next_bus_stops_message_and_source, IStmInfoTask.SOURCE_NAME);
		}
		new AlertDialog.Builder(this).setTitle(getString(R.string.next_bus_stops)).setIcon(R.drawable.ic_btn_info_details).setMessage(message)
				.setPositiveButton(getString(android.R.string.ok), null).setCancelable(true).create().show();
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		// try to find the existing favorite
		new AsyncTask<Void, Void, Fav>() {
			@Override
			protected Fav doInBackground(Void... params) {
				return DataManager.findFav(BusStopInfo.this.getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP,
						String.valueOf(BusStopInfo.this.routeTripStop.stop.code), BusStopInfo.this.routeTripStop.route.shortName);
			}

			protected void onPostExecute(Fav result) {
				final CheckBox starCb = (CheckBox) findViewById(R.id.star);
				starCb.setChecked(result != null);
				starCb.setVisibility(View.VISIBLE);
			};
		}.execute();
	}

	/**
	 * Set the bus stop info basic UI.
	 */
	private void refreshBusStopInfo() {
		MyLog.v(TAG, "refreshBusStopInfo()");
		((TextView) findViewById(R.id.stop_code)).setText(this.routeTripStop.stop.code);
		// set bus stop place name
		((TextView) findViewById(R.id.bus_stop_place)).setText(BusUtils.cleanBusStopPlace(this.routeTripStop.stop.name));
		// set the favorite icon
		setTheStar();
		// set bus line number & direction
		TextView lineNumberTv = (TextView) findViewById(R.id.line_number);
		lineNumberTv.setText(this.routeTripStop.route.shortName);
		lineNumberTv.setBackgroundColor(Utils.parseColor(this.routeTripStop.route.color));
		lineNumberTv.setTextColor(Utils.parseColor(this.routeTripStop.route.textColor));
		((TextView) findViewById(R.id.line_direction)).setText(this.routeTripStop.trip.getHeading(this).toUpperCase(Locale.getDefault()));
		// set listener
		findViewById(R.id.line).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// MyLog.v(TAG, "onView()");
				Intent mIntent = BusLineInfo.newInstance(BusStopInfo.this, BusStopInfo.this.routeTripStop.route,
						String.valueOf(BusStopInfo.this.routeTripStop.trip.id), BusStopInfo.this.routeTripStop.stop.code);
				startActivity(mIntent);
			}
		});
		// display location if known
		updateDistanceWithNewLocation(getLocation());
	}

	private void refreshNearby() {
		MyLog.v(TAG, "refreshNearby()");
		findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.VISIBLE);
		new AsyncTask<String, Void, List<POI>>() {
			@Override
			protected List<POI> doInBackground(String... params) {
				MyLog.v(TAG, "refreshNearby()>doInBackground()");
				List<POI> result = new ArrayList<POI>();
				final Double lat = BusStopInfo.this.routeTripStop.getLat();
				final Double lng = BusStopInfo.this.routeTripStop.getLng();
				// TODO asynchronous from all content providers
				List<RouteTripStop> routeTripStops = StmBusManager.findRouteTripStopsWithLatLngList(BusStopInfo.this, lat, lng);
				ListIterator<RouteTripStop> it = routeTripStops.listIterator();
				while (it.hasNext()) {
					RouteTripStop routeTripStop = it.next();
					if (routeTripStop.stop.id == BusStopInfo.this.routeTripStop.stop.id) {
						it.remove();
					}
				}
				result.addAll(routeTripStops);
				result.addAll(ClosestSubwayStationsFinderTask.getAllStationsWithLines(BusStopInfo.this, lat, lng));
				// TODO ? result.addAll(ClosestBikeStationsFinderTask.getABikeStations(BixiManager.findAllBikeStationsLocationList(contentResolver, lat, lng)));
				LocationUtils.updateDistance(result, lat, lng);
				Collections.sort(result, new POI.POIDistanceComparator());
				if (Utils.NB_NEARBY_LIST > 0 && result.size() > Utils.NB_NEARBY_LIST) {
					return result.subList(0, Utils.NB_NEARBY_LIST);
				} else {
					return result;
				}
			}

			@Override
			protected void onPostExecute(List<POI> result) {
				MyLog.v(TAG, "refreshNearby()>onPostExecute()");
				BusStopInfo.this.adapter.setPois(result);
				BusStopInfo.this.adapter.updateDistancesNow(getLocation());
				refreshFavoriteIDsFromDB();
				// show the result
				BusStopInfo.this.adapter.initManual();
				findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
				findViewById(R.id.nearby_loading).setVisibility(View.GONE);
				findViewById(R.id.nearby_list).setVisibility(View.VISIBLE);
			}

		}.execute();
	}

	private void refreshFavoriteIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findAllFavsList(getContentResolver());
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				BusStopInfo.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * Set the other bus lines using this bus stop.
	 */
	private void refreshOtherBusLinesInfo() {
		MyLog.v(TAG, "refreshOtherBusLinesInfo()");
		if (this.otherBusStopLines != null) {
			refreshOtherBusLinesUI();
			return;
		}
		new AsyncTask<String, Void, List<RouteTripStop>>() {
			@Override
			protected List<RouteTripStop> doInBackground(String... params) {
				MyLog.v(TAG, "refreshOtherBusLinesInfo()>doInBackground(%s)", params[0]);
				List<RouteTripStop> result = StmBusManager.findRouteTripStopWithStopCodeList(BusStopInfo.this, params[0]);

				// remove all bus lines with the same line number
				ListIterator<RouteTripStop> it = result.listIterator();
				while (it.hasNext()) {
					RouteTripStop busStop = it.next();
					// IF same trip DO
					if (busStop.trip.id == BusStopInfo.this.routeTripStop.trip.id) {
						it.remove();
						continue;
					}
					// IF same route DO // TODO really?
					if (busStop.trip.routeId == BusStopInfo.this.routeTripStop.trip.routeId) {
						it.remove();
						continue;
					}
				}
				return result;
			}

			@Override
			protected void onPostExecute(List<RouteTripStop> result) {
				MyLog.v(TAG, "refreshOtherBusLinesInfo()>onPostExecute()");
				BusStopInfo.this.otherBusStopLines = result;
				refreshOtherBusLinesUI();
				refreshNearby();
			}
		}.execute(this.routeTripStop.stop.code);
	}

	/**
	 * Refresh other bus lines UI.
	 */
	private void refreshOtherBusLinesUI() {
		MyLog.v(TAG, "refreshOtherBusLinesUI()");
		LinearLayout otherBusLinesLayout = (LinearLayout) findViewById(R.id.other_bus_line_list);
		otherBusLinesLayout.removeAllViews();
		if (this.otherBusStopLines != null && this.otherBusStopLines.size() > 0) {
			for (RouteTripStop routeTrip : this.otherBusStopLines) {
				// the view
				View view = getLayoutInflater().inflate(R.layout.bus_stop_info_bus_line_list_item, otherBusLinesLayout, false);
				TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
				final String lineNumber = routeTrip.route.shortName;
				final String lineName = routeTrip.route.longName;
				final String lineColor = routeTrip.route.color;
				final String lineTextColor = routeTrip.route.textColor;
				lineNumberTv.setText(lineNumber);
				int color = Utils.parseColor(routeTrip.route.color);
				lineNumberTv.setBackgroundColor(color);
				int textColor = Utils.parseColor(routeTrip.route.textColor);
				lineNumberTv.setTextColor(textColor);
				// line direction
				final String currentDirectionId = String.valueOf(routeTrip.trip.id);
				final String lineDirection = routeTrip.trip.getHeading(this).toUpperCase(Locale.getDefault());
				((TextView) view.findViewById(R.id.line_direction)).setText(lineDirection);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick()");
						// TODO just switch RouteTripStop and update next passages and other bus lines
						showNewBusStop(BusStopInfo.this.routeTripStop.stop.code, BusStopInfo.this.routeTripStop.stop.name, lineNumber, /* lineName, */
								lineColor, lineTextColor, lineDirection);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						new BusLineSelectDirection(BusStopInfo.this, lineNumber, lineName, lineColor, lineTextColor, currentDirectionId).showDialog();
						return true;
					}
				});
				otherBusLinesLayout.addView(view);
			}
			findViewById(R.id.other_bus_line_title).setVisibility(View.VISIBLE);
			otherBusLinesLayout.setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.other_bus_line_title).setVisibility(View.GONE);
			otherBusLinesLayout.setVisibility(View.GONE);
		}
	};

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newStopPlace the new bus stop place or null (optional)
	 * @param newLineNumber the new bus line number (optional)
	 * @param newLineType the new bus line type or null (optional)
	 */
	public void showNewBusStop(String newStopCode, String newStopPlace, String newLineNumber, /* String newLineName, */String newLineColor,
			String newLineTextColor, String lineDirection) {
		MyLog.v(TAG, "showNewBusStop(%s, %s, %s, %s, %s, %s)", newStopCode, newStopPlace, newLineNumber, /* newLineName, */newLineColor, newLineTextColor,
				lineDirection);
		// temporary set UI
		((TextView) findViewById(R.id.stop_code)).setText(newStopCode);
		if (!TextUtils.isEmpty(newStopPlace)) {
			((TextView) findViewById(R.id.bus_stop_place)).setText(BusUtils.cleanBusStopPlace(newStopPlace));
		}
		TextView busLineNumberTv = (TextView) findViewById(R.id.line_number);
		busLineNumberTv.setText(newLineNumber);
		if (!TextUtils.isEmpty(newLineColor)) {
			busLineNumberTv.setBackgroundColor(Utils.parseColor(newLineColor));
		}
		if (!TextUtils.isEmpty(newLineTextColor)) {
			busLineNumberTv.setTextColor(Utils.parseColor(newLineTextColor));
		}
		if (TextUtils.isEmpty(lineDirection)) {
			((TextView) findViewById(R.id.line_direction)).setText(getString(R.string.ellipsis));
		} else {
			((TextView) findViewById(R.id.line_direction)).setText(lineDirection);
		}
		// set as loading
		((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops));
		// hide unknown data
		findViewById(R.id.distance).setVisibility(View.INVISIBLE);
		findViewById(R.id.compass).setVisibility(View.INVISIBLE);
		findViewById(R.id.other_bus_line_title).setVisibility(View.GONE);
		findViewById(R.id.other_bus_line_list).setVisibility(View.GONE);
		findViewById(R.id.nearby_title).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.GONE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		// setNearbyListAsLoading();
		this.adapter.setPois(null);
		findViewById(R.id.star).setVisibility(View.INVISIBLE);
		this.hours = null;
		setNextStopsLoading();

		new AsyncTask<String, Void, Void>() {
			private boolean isNewBusStop;
			private String newStopCode;
			private String newLineNumber;

			@Override
			protected Void doInBackground(String... params) {
				newStopCode = params[0];
				newLineNumber = params[1];

				isNewBusStop = BusStopInfo.this.routeTripStop == null || !BusStopInfo.this.routeTripStop.stop.code.equals(newStopCode)
						|| !BusStopInfo.this.routeTripStop.route.shortName.equals(newLineNumber);

				// IF no bus stop OR new bus stop DO
				if (BusStopInfo.this.routeTripStop == null || !BusStopInfo.this.routeTripStop.stop.code.equals(newStopCode)) {
					// MyLog.v(TAG, "New bus stop '%s' line '%s'.", newStopCode, newLineNumber);
					if (StmBusManager.findStopWithCode(BusStopInfo.this, newStopCode) == null) {
						showAlertDialog(newStopCode);
					}
				}
				if (newLineNumber == null) {
					newLineNumber = findBusLineNumberFromStopCode(newStopCode);
				}
				if (newStopCode != null && newLineNumber != null) {

					if (isNewBusStop) {
						// TODO retry now that we have more data?
						// DOESN'T WORK because bus stop direction need to be read from the Bus Stop entry in the DB
						// if (BusStopInfo.this.busStop != null && BusStopInfo.this.busLine != null && Utils.getCollectionSize(BusStopInfo.this.otherBusLines) >
						// 0
						// && BusStopInfo.this.busStop.getCode().equals(newStopCode)) {
						// if (BusStopInfo.this.busStop != null && BusStopInfo.this.busLine != null &&
						// Utils.getCollectionSize(BusStopInfo.this.otherBusStopLines) > 0
						// && BusStopInfo.this.busStop.getCode().equals(newStopCode)) {
						// // just switching bus line
						// // BusStopInfo.this.otherBusLines.add(BusStopInfo.this.busLine);
						// // make sure bus stop has bus line type
						// BusStopInfo.this.busStop.setLineType(BusStopInfo.this.busLine.getType());
						// BusStopInfo.this.otherBusStopLines.add(BusStopInfo.this.busStop);
						// // BusLine newBusLine = null;
						// BusStop newBusStop = null;
						// // ListIterator<BusLine> it = BusStopInfo.this.otherBusLines.listIterator();
						// ListIterator<BusStop> it = BusStopInfo.this.otherBusStopLines.listIterator();
						// while (it.hasNext()) {
						// // BusLine otherBusLine = it.next();
						// BusStop otherBusStop = it.next();
						// // if (otherBusLine.getNumber().equals(newLineNumber)) {
						// if (otherBusStop.getLineNumber().equals(newLineNumber)) {
						// // newBusLine = otherBusLine;
						// newBusStop = otherBusStop;
						// it.remove(); // remove new bus stop from other bus lines stop
						// break;
						// }
						// }
						// // BusStopInfo.this.busLine = newBusLine;
						// BusStopInfo.this.busLine = newBusStop.getBusLine();
						// // BusStopInfo.this.busStop.setLineNumber(newLineNumber);
						// BusStopInfo.this.busStop = newBusStop;
						// BusStopInfo.this.busStopDirection = BusLineDirection.create(BusStopInfo.this.busStop.getLineNumber(),
						// BusStopInfo.this.busStop.getDirectionId());
						// } else {
						// load bus stop from DB if necessary
						BusStopInfo.this.routeTripStop = StmBusManager.findRouteTripStop(BusStopInfo.this, newStopCode, newLineNumber);
						BusStopInfo.this.otherBusStopLines = null;
						// BusStopInfo.this.busStopDirection = StmManager.findBusLineDirection(BusStopInfo.this.getContentResolver(),
						// BusStopInfo.this.busStop.getDirectionId());
						// }
						// BusStopInfo.this.busStopDirection = StmManager.findBusLineDirection(BusStopInfo.this.getContentResolver(),
						// BusStopInfo.this.busStop.getDirectionId());
					}

				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// IF no bus stop OR new bus stop DO
				if (isNewBusStop) {
					// MyLog.v(TAG, "New bus stop '%s' line '%s'.", newStopCode, newLineNumber);
					((TextView) findViewById(R.id.stop_code)).setText(newStopCode);
				}
				if (newStopCode != null && newLineNumber != null) {
					// IF new bus stop code DO
					if (isNewBusStop) {
						BusStopInfo.this.hours = null; // clear current bus stop hours
					}
					// TODO ? // IF new bus stop code and line number DO
					// if (isNewBusLine) {
					BusStopInfo.this.memCache = null; // clear the cache for the new bus stop
					// }
					if (BusStopInfo.this.wwwTask != null) {
						BusStopInfo.this.wwwTask.cancel(true);
						BusStopInfo.this.wwwTask = null;
					}
					if (BusStopInfo.this.localTask != null) {
						BusStopInfo.this.localTask.cancel(true);
						BusStopInfo.this.localTask = null;
					}
				}
				setUpUI();
			};
		}.execute(newStopCode, newLineNumber);
	}

	private String findBusLineNumberFromStopCode(String newStopCode) {
		// MyLog.v(TAG, "findBusLineNumberFromStopCode(%s)", newStopCode);
		// get the bus lines for this bus stop
		List<RouteTripStop> busLines = StmBusManager.findRouteTripStopWithStopCodeList(this, newStopCode);
		if (busLines == null) {
			// no bus line found
			// TODO handle unknown bus stop code
			Utils.notifyTheUser(this, getString(R.string.wrong_stop_code_and_code, newStopCode));
			this.finish();
			return null;
		} else {
			// at least 1 bus line found
			// always use the first now for now
			return busLines.get(0).route.shortName;
			// TODO show a bus line selector to the user
		}
	}

	/**
	 * Show the dialog about the unknown bus stop id.
	 * @param wrongStopCode
	 */
	private void showAlertDialog(String wrongStopCode) {
		MyLog.v(TAG, "showAlertDialog()");
		MyLog.w(TAG, "Wrong bus stop code '%s'?", wrongStopCode);
		String message = getString(R.string.wrong_stop_code_and_code, wrongStopCode) + "\n" + getString(R.string.wrong_stop_code_internet);
		new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.warning).setMessage(message)
				.setPositiveButton(R.string.yes, this).setNegativeButton(R.string.no, this).create().show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(%s)", which);
		if (which == -2) {
			dialog.dismiss(); // CANCEL
			this.finish(); // close the activity.
		} else {
			// try to load the next stop from the web.
			loadNextStopsFromWeb();
		}
	}

	/**
	 * Setup all the UI (based on the bus stop).
	 */
	private void setUpUI() {
		refreshBusStopInfo();
		showNextBusStops();
		refreshOtherBusLinesInfo();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Show the next bus stops (or launch refresh next bus stops task).
	 */
	private void showNextBusStops() {
		MyLog.v(TAG, "showNextBusStops()");
		new AsyncTask<Void, Void, Void>() {

			private boolean refreshAsync = false;

			@Override
			protected Void doInBackground(Void... params) {
				if (BusStopInfo.this.hours == null) {
					// check cache
					// IF no local cache DO
					if (BusStopInfo.this.memCache == null) {
						// load cache from database
						BusStopInfo.this.memCache = DataManager.findCache(getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP,
								TripStop.getUID(BusStopInfo.this.routeTripStop.stop.code, String.valueOf(BusStopInfo.this.routeTripStop.trip.routeId)));
					}
					if (BusStopInfo.this.memCache != null) {
						// IF the cache is too old DO
						final int tooOld = Utils.currentTimeSec() - BusUtils.CACHE_NOT_USEFUL_IN_SEC;
						if (tooOld >= BusStopInfo.this.memCache.getDate()) {
							// don't use the cache
							BusStopInfo.this.memCache = null;
							// delete all too old cache
							try {
								DataManager.deleteCacheOlderThan(getContentResolver(), tooOld);
							} catch (Exception e) {
								MyLog.w(TAG, e, "Can't clean the cache!");
							}
						} else if (Utils.currentTimeSec() - BusUtils.CACHE_TOO_OLD_IN_SEC >= BusStopInfo.this.memCache.getDate()) {
							refreshAsync = true;
						}
					}
					if (BusStopInfo.this.memCache != null) {
						// use cache
						BusStopInfo.this.hours = BusStopHours.deserialized(BusStopInfo.this.memCache.getObject());
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (BusStopInfo.this.hours == null) {
					// try to load from local schedule
					loadNextStopsFromLocalSchedule();
					// load from the web
					loadNextStopsFromWeb();
				} else {
					showNewNextStops();
					setNextStopsNotLoading();
					if (refreshAsync) {
						loadNextStopsFromWeb();
					}
				}
			};

		}.execute();
	}

	private StmBusScheduleTask localTask;
	private boolean wwwTaskRunning;

	private void loadNextStopsFromLocalSchedule() {
		MyLog.v(TAG, "loadNextStopsFromLocalSchedule()");
		if (this.localTask != null && this.localTask.getStatus() == Status.RUNNING) {
			this.localTask.cancel(true);
			this.localTask = null;
		}
		this.localTask = new StmBusScheduleTask(this, new NextStopListener() {

			@Override
			public void onNextStopsProgress(String progress) {
				// MyLog.v(TAG, "loadNextStopsFromLocalSchedule()>onNextStopsProgress(%s)", progress);
				// IF the task was cancelled DO
				if (BusStopInfo.this.localTask == null || BusStopInfo.this.localTask.isCancelled()) {
					// MyLog.d(TAG, "Task cancelled!");
					return; // stop here
				}
				BusStopInfo.this.onNextStopsProgress(progress);
			}

			@Override
			public void onNextStopsLoaded(Map<String, BusStopHours> results) {
				MyLog.v(TAG, "loadNextStopsFromLocalSchedule()>onNextStopsLoaded(%s)", results == null ? null : results.size());
				if (BusStopInfo.this.localTask == null || BusStopInfo.this.localTask.isCancelled()) {
					return; // task cancelled
				}
				if (BusStopInfo.this.hours != null && BusStopInfo.this.hours.getSourceName().equals(IStmInfoTask.SOURCE_NAME)) {
					MyLog.d(TAG, "Local DB too late");
					if (!BusStopInfo.this.wwwTaskRunning) {
						setNextStopsNotLoading(); // www task completed
					}
					return;
				}
				if (results == null) {
					MyLog.d(TAG, "Local DB no result");
					if (!BusStopInfo.this.wwwTaskRunning) {
						setNextStopsNotLoading(); // www task completed
					}
					return;
				}
				if (!results.containsKey(BusStopInfo.this.routeTripStop.route.shortName)) {
					MyLog.d(TAG, "Local DB no result for this line number");
					if (!BusStopInfo.this.wwwTaskRunning) {
						setNextStopsNotLoading(); // www task completed
					}
					return;
				}
				// IF error DO
				BusStopHours result = results.get(BusStopInfo.this.routeTripStop.route.shortName);
				if (result == null || result.getSHours().size() <= 0) {
					MyLog.d(TAG, "Local DB no hours in result");
					// process the error
					if (BusStopInfo.this.wwwTask != null && BusStopInfo.this.wwwTask.getStatus() == Status.RUNNING) {
						if (result != null && !TextUtils.isEmpty(result.getError())) {
							BusStopInfo.this.onNextStopsProgress(result.getError());
						} else if (result != null && !TextUtils.isEmpty(result.getMessage())) {
							BusStopInfo.this.onNextStopsProgress(result.getMessage());
						} else if (result != null && !TextUtils.isEmpty(result.getMessage2())) {
							BusStopInfo.this.onNextStopsProgress(result.getMessage2());
						}
					} else {
						setNextStopsError(result);
					}
					if (!BusStopInfo.this.wwwTaskRunning) {
						setNextStopsNotLoading(); // www task completed
					}
					return;
				}
				// get the result
				if (BusStopInfo.this.hours == null) {
					BusStopInfo.this.hours = result;
					// show the result
					showNewNextStops();
				}
				if (!BusStopInfo.this.wwwTaskRunning) {
					setNextStopsNotLoading(); // www task completed
				}
			}
		}, this.routeTripStop);
		this.localTask.execute();
	}

	/**
	 * Show the new next bus stops.
	 * @param hours the new next bus stops
	 */
	private void showNewNextStops() {
		MyLog.v(TAG, "showNewNextStops()");
		if (this.hours != null) {
			// set next stop header with source name
			((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops_and_source, this.hours.getSourceName()));
			// IF there next stops found DO
			if (this.hours.getSHours().size() > 0) {
				// hide loading + messages
				findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
				TextView messageTv = (TextView) findViewById(R.id.next_stops_message_text);
				messageTv.setVisibility(View.GONE);
				// show next bus stop group
				HorizontalScrollView stopsHScrollv = (HorizontalScrollView) findViewById(R.id.next_stops_group);
				if (stopsHScrollv.getVisibility() != View.VISIBLE) {
					stopsHScrollv.smoothScrollTo(0, 0); // reset scroll
					stopsHScrollv.setVisibility(View.VISIBLE);
				}
				List<String> fHours = this.hours.getFormattedHours(this);
				// show the next bus stops
				SpannableStringBuilder nextStopsSb = new SpannableStringBuilder();
				// previous stop
				int startPreviousStop = nextStopsSb.length();
				if (this.hours.hasPreviousHour()) {
					nextStopsSb.append(Utils.formatHours(this, this.hours.getPreviousHour())).append(' ');
				}
				int endPreviousStop = nextStopsSb.length();
				// 1st stop
				int startFirstStop = nextStopsSb.length();
				nextStopsSb.append(fHours.get(0));
				int endFirstStop = startFirstStop + fHours.get(0).length();
				int startSecondStop = 0, endSecondStop = 0, startOtherStops = 0, endOtherStops = 0;
				if (fHours.size() > 1) {
					// 2nd stops
					nextStopsSb.append(' ').append(' ').append(' ');
					startSecondStop = nextStopsSb.length();
					nextStopsSb.append(fHours.get(1));
					endSecondStop = startSecondStop + fHours.get(1).length();
					if (fHours.size() > 2) {
						// other stops
						startOtherStops = nextStopsSb.length();
						endOtherStops = startOtherStops;
						for (int i = 2; i < fHours.size(); i++) {
							if (nextStopsSb.length() > 0) {
								nextStopsSb.append(' ');
								endOtherStops += 1;
							}
							nextStopsSb.append(fHours.get(i));
							endOtherStops += fHours.get(i).length();
						}
					}
				}
				if (startPreviousStop != endPreviousStop) {
					nextStopsSb.setSpan(new TextAppearanceSpan(this, android.R.style.TextAppearance_Medium), startPreviousStop, endPreviousStop,
							Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					nextStopsSb.setSpan(new RelativeSizeSpan(2.0f), startPreviousStop, endPreviousStop, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (startFirstStop != endFirstStop) {
					nextStopsSb.setSpan(new TextAppearanceSpan(this, android.R.style.TextAppearance_Large), startFirstStop, endFirstStop,
							Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					nextStopsSb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startFirstStop, endFirstStop, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					nextStopsSb.setSpan(new RelativeSizeSpan(2.0f), startFirstStop, endFirstStop, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (startSecondStop != endSecondStop) {
					nextStopsSb.setSpan(new TextAppearanceSpan(this, android.R.style.TextAppearance_Medium), startSecondStop, endSecondStop,
							Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					nextStopsSb.setSpan(new RelativeSizeSpan(2.0f), startSecondStop, endSecondStop, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (startOtherStops != endOtherStops) {
					nextStopsSb.setSpan(new TextAppearanceSpan(this, android.R.style.TextAppearance_Small), startOtherStops, endOtherStops,
							Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					nextStopsSb.setSpan(new RelativeSizeSpan(2.0f), startOtherStops, endOtherStops, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				String word = nextStopsSb.toString().toLowerCase(Locale.ENGLISH);
				for (int index = word.indexOf("am"); index >= 0; index = word.indexOf("am", index + 1)) {
					if (index < 0) {
						break;
					}
					nextStopsSb.setSpan(new RelativeSizeSpan(0.1f), index - 1, index, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // remove space hack
					nextStopsSb.setSpan(new RelativeSizeSpan(0.25f), index, index + 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					index += 2;
				}
				for (int index = word.indexOf("pm"); index >= 0; index = word.indexOf("pm", index + 1)) {
					if (index < 0) {
						break;
					}
					nextStopsSb.setSpan(new RelativeSizeSpan(0.1f), index - 1, index, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // remove space hack
					nextStopsSb.setSpan(new RelativeSizeSpan(0.25f), index, index + 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				((TextView) findViewById(R.id.next_stops)).setText(nextStopsSb);
				// show messages
				StringBuilder messageSb = getMessageSb(this.hours);
				if (messageSb.length() > 0) {
					messageTv.setText(messageSb);
					messageTv.setVisibility(View.VISIBLE);
				} else {
					messageTv.setText(null);
					messageTv.setVisibility(View.GONE);
				}
			}
		}
	}

	/**
	 * Start the next bus stops refresh task if not running.
	 */
	private void loadNextStopsFromWeb() {
		MyLog.v(TAG, "loadNextStopsFromWeb()");
		// IF the task is already running DO
		if (this.wwwTask != null && this.wwwTask.getStatus() == Status.RUNNING) {
			return; // skip
		}
		setNextStopsLoading();
		// find the next bus stop
		this.wwwTask = new LoadNextBusStopIntoCacheTask(this, this.routeTripStop, new NextStopListener() {

			@Override
			public void onNextStopsProgress(String progress) {
				// MyLog.v(TAG, "loadNextStopsFromWeb()>onNextStopsProgress(%s)", progress);
				// IF the task was cancelled DO
				if (BusStopInfo.this.wwwTask == null || BusStopInfo.this.wwwTask.isCancelled()) {
					// MyLog.d(TAG, "Task cancelled!");
					return; // stop here
				}
				BusStopInfo.this.onNextStopsProgress(progress);
			}

			@Override
			public void onNextStopsLoaded(Map<String, BusStopHours> results) {
				MyLog.v(TAG, "loadNextStopsFromWeb()>onNextStopsLoaded(%s)", results == null ? null : results.size());
				if (BusStopInfo.this.wwwTask == null || BusStopInfo.this.wwwTask.isCancelled()) {
					return; // task cancelled
				}
				if (results != null) {
					for (String lineNumber : results.keySet()) {
						BusStopHours busStopHours = results.get(lineNumber);
						if (busStopHours != null && busStopHours.getSHours().size() > 0) {
							saveToMemCache(BusStopInfo.this.routeTripStop.stop.code, lineNumber, busStopHours);
						}
					}
				}
				// IF error DO
				BusStopHours result = results == null ? null : results.get(BusStopInfo.this.routeTripStop.route.shortName);
				if (result == null || result.getSHours().size() <= 0) {
					// process the error
					if (BusStopInfo.this.localTask != null && BusStopInfo.this.localTask.getStatus() == Status.RUNNING) {
						if (result != null && !TextUtils.isEmpty(result.getError())) {
							BusStopInfo.this.onNextStopsProgress(result.getError());
						} else if (result != null && !TextUtils.isEmpty(result.getMessage())) {
							BusStopInfo.this.onNextStopsProgress(result.getMessage());
						} else if (result != null && !TextUtils.isEmpty(result.getMessage2())) {
							BusStopInfo.this.onNextStopsProgress(result.getMessage2());
						}
					} else {
						setNextStopsError(result);
					}
					if (BusStopInfo.this.localTask == null || BusStopInfo.this.localTask.getStatus() != Status.RUNNING) {
						setNextStopsNotLoading();
					}
					BusStopInfo.this.wwwTaskRunning = false;
					return;
				}
				// get the result
				BusStopInfo.this.hours = result;
				// show the result
				showNewNextStops();
				setNextStopsNotLoading();
				BusStopInfo.this.wwwTaskRunning = false;
				cancelLocalTask();
			}
		}, false, true);
		this.wwwTask.execute();
		this.wwwTaskRunning = true;
	}

	private void onNextStopsProgress(String progress) {
		if (TextUtils.isEmpty(progress)) {
			return;
		}
		if (this.hours != null) {
			// notify the user ?
			return;
		}
		// update the BIG message
		TextView detailMsgTv = (TextView) findViewById(R.id.detail_msg);
		detailMsgTv.setText(progress);
		detailMsgTv.setVisibility(View.VISIBLE);
	}

	/**
	 * Refresh or stop refresh the next bus stop depending on the current status of the task.
	 * @param v the view (not used)
	 */
	public void refreshOrStopRefreshNextStops(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshNextStops()");
		// IF the task is running DO
		if (this.wwwTask != null && this.wwwTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.wwwTask.cancel(true);
			this.wwwTask = null;
			setNextStopsCancelled();
		} else {
			loadNextStopsFromLocalSchedule();
			loadNextStopsFromWeb();
		}
	}

	/**
	 * Set the next stops view as loading.
	 */
	private void setNextStopsLoading() {
		MyLog.v(TAG, "setNextStopsLoading()");
		if (this.hours == null) {
			// set the BIG loading message
			findViewById(R.id.next_stops_group).setVisibility(View.GONE);
			findViewById(R.id.next_stops_message_text).setVisibility(View.GONE);
			findViewById(R.id.next_stops_loading).setVisibility(View.VISIBLE);
			// } else { // notify the user ?
		}
		// hide refresh icon
		findViewById(R.id.next_stops_refresh).setVisibility(View.INVISIBLE);
		// show progress bar
		findViewById(R.id.progress_bar_next_stop).setVisibility(View.VISIBLE);

	}

	/**
	 * Set the next stop view as not loading.
	 */
	private void setNextStopsNotLoading() {
		MyLog.v(TAG, "setNextStopsNotLoading()");
		// show refresh icon instead of loading
		findViewById(R.id.next_stops_refresh).setVisibility(View.VISIBLE);
		// hide progress bar
		findViewById(R.id.progress_bar_next_stop).setVisibility(View.INVISIBLE);
	}

	/**
	 * Set the next stops view as cancelled.
	 */
	private void setNextStopsCancelled() {
		MyLog.v(TAG, "setClosestStationsCancelled()");
		if (this.hours != null) {
			// notify the user but keep showing the old stations
			Utils.notifyTheUser(this, getString(R.string.next_bus_stop_load_cancelled));
		} else {
			// show the BIG cancel message
			// hide loading + message 2 + next stops group
			findViewById(R.id.next_stops_group).setVisibility(View.GONE);
			findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
			// show message 1
			TextView messageTv = (TextView) findViewById(R.id.next_stops_message_text);
			messageTv.setText(R.string.next_bus_stop_load_cancelled);
			messageTv.setVisibility(View.VISIBLE);
		}
		setNextStopsNotLoading();
	}

	/**
	 * Set the next stops view as error.
	 * @param hours the {@link BusStopHours} object containing the error or <b>null</b>
	 */
	private void setNextStopsError(BusStopHours hours) {
		MyLog.v(TAG, "setNextStopsError()");
		// IF there are hours to show DO
		if (this.hours != null) {
			// notify the user but keep showing the old stations
			if (hours != null && !TextUtils.isEmpty(hours.getError())) {
				Utils.notifyTheUser(this, hours.getError());
			} else if (hours != null && !TextUtils.isEmpty(hours.getMessage())) {
				Utils.notifyTheUser(this, hours.getMessage());
			} else if (hours != null && !TextUtils.isEmpty(hours.getMessage2())) {
				Utils.notifyTheUser(this, hours.getMessage2());
			} else {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", this.routeTripStop.stop.code, this.routeTripStop.route.shortName);
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, this.routeTripStop.route.shortName);
				Utils.notifyTheUser(this, defaultMessage);
			}
		} else {
			// set next stop header with source name
			if (hours == null || TextUtils.isEmpty(hours.getSourceName())) {
				((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops));
			} else {
				((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops_and_source, hours.getSourceName()));
			}
			// show the BIG cancel message
			TextView messageTv = (TextView) findViewById(R.id.next_stops_message_text);
			// hide loading + message 2 + next stops group
			findViewById(R.id.next_stops_group).setVisibility(View.GONE);
			findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
			messageTv.setVisibility(View.GONE);
			// Show messages
			StringBuilder messageSb = getMessageSb(hours);
			if (messageSb.length() == 0) {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", this.routeTripStop.stop.code, this.routeTripStop.route.shortName);
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, this.routeTripStop.route.shortName);
				messageSb.append(defaultMessage);
			}
			messageTv.setText(messageSb);
			messageTv.setVisibility(View.VISIBLE);
		}
		setNextStopsNotLoading();
	}

	private StringBuilder getMessageSb(BusStopHours hours) {
		MyLog.v(TAG, "getMessageSb()");
		StringBuilder messageSb = new StringBuilder();
		if (hours != null && !TextUtils.isEmpty(hours.getError())) {
			MyLog.d(TAG, "getMessageSb() > hours.getError(): " + hours.getError());
			if (messageSb.length() > 0) {
				messageSb.append('\n');
			}
			messageSb.append(hours.getError().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
		}
		if (hours != null && !TextUtils.isEmpty(hours.getMessage())) {
			MyLog.d(TAG, "getMessageSb() > hours.getMessage(): " + hours.getMessage());
			if (messageSb.length() > 0) {
				messageSb.append('\n');
			}
			messageSb.append(hours.getMessage().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
		}
		if (hours != null && !TextUtils.isEmpty(hours.getMessage2())) {
			MyLog.d(TAG, "getMessageSb() > hours.getMessage2(): " + hours.getMessage2());
			if (messageSb.length() > 0) {
				messageSb.append('\n');
			}
			messageSb.append(hours.getMessage2().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
		}
		MyLog.d(TAG, "getMessageSb() > messageSb: " + messageSb);
		return messageSb;
	}

	/**
	 * Save the bus stop hours for the line number into the local cache.
	 * @param lineNumber the bus stop line number
	 * @param busStopHours the stop hours
	 */
	private void saveToMemCache(String stopCode, String lineNumber, BusStopHours busStopHours) {
		// MyLog.v(TAG, "saveToMemCache(%s,%s)", stopCode, lineNumber);
		Cache newCache = new Cache(Cache.KEY_TYPE_VALUE_BUS_STOP, TripStop.getUID(stopCode, lineNumber), busStopHours.serialized());
		// remove existing cache for this bus stop
		if (this.routeTripStop != null && lineNumber.equals(this.routeTripStop.route.shortName)) {
			this.memCache = newCache;
		}
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistanceWithNewLocation(Location currentLocation) {
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", currentLocation);
		if (currentLocation != null && this.routeTripStop != null && this.routeTripStop.hasLocation()) {
			LocationUtils.updateDistanceWithString(this, this.routeTripStop, currentLocation, new LocationTaskCompleted() {

				@Override
				public void onLocationTaskCompleted() {
					TextView distanceTv = (TextView) findViewById(R.id.distance);
					distanceTv.setText(BusStopInfo.this.routeTripStop.getDistanceString());
					distanceTv.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	/**
	 * Initialize the location updates if necessary.
	 * @return the location or <B>NULL</b>
	 */
	private Location getLocation() {
		if (this.location == null) {
			Location bestLastKnownLocationOrNull = LocationUtils.getBestLastKnownLocation(this);
			if (bestLastKnownLocationOrNull != null) {
				this.setLocation(bestLastKnownLocationOrNull);
			}
			// enable location updates if necessary
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
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
				this.adapter.setLocation(this.location);
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(this, this);
					this.compassUpdatesEnabled = true;
				}
				updateDistanceWithNewLocation(this.location);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
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
		// MyLog.v(TAG, "onStatusChanged(%s, %s)", provider, status);
	}

	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		if (this.routeTripStop == null) {
			return;
		}
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP, this.routeTripStop.stop.code, this.routeTripStop.route.shortName);
		// IF the favorite exist DO
		if (findFav != null) {
			// delete the favorite
			DataManager.deleteFav(getContentResolver(), findFav.getId());
			Utils.notifyTheUser(this, getString(R.string.favorite_removed));
		} else {
			// add the favorite
			Fav newFav = new Fav();
			newFav.setType(Fav.KEY_TYPE_VALUE_BUS_STOP);
			newFav.setFkId(this.routeTripStop.stop.code);
			newFav.setFkId2(this.routeTripStop.route.shortName);
			DataManager.addFav(getContentResolver(), newFav);
			Utils.notifyTheUser(this, getString(R.string.favorite_added));
			UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_IS_FAV, true);
		}
		SupportFactory.get().backupManagerDataChanged(this);
		setTheStar(); // TODO is remove useless?
	}

	/**
	 * Show the bus stop in radar-enabled application.
	 * @param v the view (not used).
	 */
	public void showStopInRadar(View v) {
		// IF the a radar activity is available DO
		if (!Utils.isIntentAvailable(this, "com.google.android.radar.SHOW_RADAR")) {
			// tell the user he needs to install a radar library.
			new NoRadarInstalled(this).showDialog();
		} else {
			// Finding the location of the bus stop
			new GeocodingTask(this, 1, true, new GeocodingTaskListener() {
				@Override
				public void processLocation(List<Address> addresses) {
					if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
						float lat = (float) addresses.get(0).getLatitude();
						float lng = (float) addresses.get(0).getLongitude();
						// Launch the radar activity
						Intent intent = new Intent("com.google.android.radar.SHOW_RADAR");
						intent.putExtra("latitude", (float) lat);
						intent.putExtra("longitude", (float) lng);
						try {
							startActivity(intent);
						} catch (ActivityNotFoundException ex) {
							MyLog.w(TAG, "Radar activity not found.");
							new NoRadarInstalled(BusStopInfo.this).showDialog();
						}
					} else {
						Utils.notifyTheUser(BusStopInfo.this, getString(R.string.bus_stop_location_not_found));
					}
				}

			}).execute(this.routeTripStop.stop.name);
		}
	}

	/**
	 * Show the stop in maps-enabled application.
	 * @param v (not used)
	 */
	public void showStopLocationInMaps(View v) {
		// Finding the location of the bus stop
		new GeocodingTask(this, 1, true, new GeocodingTaskListener() {
			@Override
			public void processLocation(List<Address> addresses) {
				if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
					double lat = addresses.get(0).getLatitude();
					double lng = addresses.get(0).getLongitude();
					// Launch the map activity
					Uri uri = Uri.parse(String.format("geo:%s,%s", lat, lng));
					startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
				} else {
					Utils.notifyTheUser(BusStopInfo.this, getString(R.string.bus_stop_location_not_found));
				}
			}

		}).execute(this.routeTripStop.stop.name);
	}

	// /**
	// * Show m.stm.info page for the current bus stop.
	// * @param v the view (not used).
	// */
	// public void showSTMInfoPage(View v) {
	// startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StmMobileTask.getUrlString(this.busStop.getCode()))));
	// }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_stop_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.stm_mobile:
		// showSTMInfoPage(null);
		// return true;
		case R.id.map:
			showStopLocationInMaps(null);
			return true;
		case R.id.radar:
			showStopInRadar(null);
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.wwwTask != null) {
			this.wwwTask.cancel(true);
			this.wwwTask = null;
		}
		cancelLocalTask();
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}

	private void cancelLocalTask() {
		if (this.localTask != null) {
			this.localTask.cancel(true);
			this.localTask = null;
		}
	}
}

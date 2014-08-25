package org.montrealtransit.android.activity;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.montrealtransit.android.data.ClosestPOI;
import org.montrealtransit.android.data.POIArrayAdapter;
import org.montrealtransit.android.data.Route;
import org.montrealtransit.android.data.RouteStop;
import org.montrealtransit.android.data.RouteTripStop;
import org.montrealtransit.android.data.Stop;
import org.montrealtransit.android.data.StopTimes;
import org.montrealtransit.android.data.Trip;
import org.montrealtransit.android.data.TripStop;
import org.montrealtransit.android.dialog.RouteSelectTripDialog;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.common.AbstractManager;
import org.montrealtransit.android.provider.common.AbstractScheduleManager;
import org.montrealtransit.android.services.ClosestRouteTripStopsFinderTask.ClosestRouteTripStopsFinderListener;
import org.montrealtransit.android.services.ClosestRouteTripStopsFinderTaskAndFilter;
import org.montrealtransit.android.services.nextstop.NextStopListener;
import org.montrealtransit.android.services.nextstop.ScheduleTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This activity show information about a stop.
 * @author Mathieu Méa
 */
public class StopInfo extends Activity implements LocationListener, DialogInterface.OnClickListener, /* NfcListener, */SensorEventListener, CompassListener,
		ClosestRouteTripStopsFinderListener, NextStopListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = StopInfo.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/Stop";

	/**
	 * The extra ID for the authority (required).
	 */
	public static final String EXTRA_AUTHORITY = "extra_authority";
	/**
	 * The extra ID for the stop ID (required).
	 */
	public static final String EXTRA_STOP_ID = "extra_stop_id";
	/**
	 * The extra ID for the stop code (not required).
	 */
	private static final String EXTRA_STOP_CODE = "extra_stop_code";
	/**
	 * The extra ID for the stop place (not required).
	 */
	private static final String EXTRA_STOP_NAME = "extra_stop_name";
	/**
	 * The extra ID for the route ID.
	 */
	private static final String EXTRA_ROUTE_ID = "extra_route_id";
	/**
	 * The extra ID for the route short name.
	 */
	private static final String EXTRA_ROUTE_SHORT_NAME = "extra_route_short_name";

	private static final String EXTRA_ROUTE_LONG_NAME = "extra_route_long_name";

	private static final String EXTRA_ROUTE_COLOR = "extra_route_color";

	private static final String EXTRA_ROUTE_TEXT_COLOR = "extra_route_text_color";

	private static final String EXTRA_TRIP_ID = "extra_trip_id";

	private static final String EXTRA_TRIP_HEADSIGN = "extra_trip_headsign";

	// /**
	// * The NFC MIME type.
	// */
	// TODO private static final String MIME_TYPE = "application/org.montrealtransit.android.stop";
	/**
	 * The stop.
	 */
	private RouteTripStop routeTripStop;
	/**
	 * Store the current hours (including messages).
	 */
	private StopTimes stopTimes;
	/**
	 * The cache for the current stop (stop ID + route ID).
	 */
	private Map<String, StopTimes> memCache = new HashMap<String, StopTimes>();
	/**
	 * The other route trip at this stop.
	 */
	protected List<RouteTripStop> otherRouteTrips;
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

	private Uri contentUri;

	private int nbTaskRunning = 0;

	/**
	 * True if the activity has the focus, false otherwise.
	 */
	private boolean hasFocus = true;
	private boolean paused = false;
	private float locationDeclination;
	private ClosestRouteTripStopsFinderTaskAndFilter nearbyTask;

	public static Intent newInstance(Context context, String authority, Stop stop, Route route) {
		MyLog.v(TAG, "newInstance(%s,%s,%s)", authority, stop, route);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, authority);
		intent.putExtras(newInstanceExtra(stop));
		intent.putExtras(newInstanceExtra(route));
		return intent;
	}

	public static Intent newInstance(Context context, RouteTripStop routeTripStop) {
		MyLog.v(TAG, "newInstance(%s)", routeTripStop);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, routeTripStop.authority);
		intent.putExtras(newInstanceExtra(routeTripStop.stop));
		intent.putExtras(newInstanceExtra(routeTripStop.trip, context));
		intent.putExtras(newInstanceExtra(routeTripStop.route));
		return intent;
	}

	public static Intent newInstance(Context context, RouteStop routeStop) {
		MyLog.v(TAG, "newInstance(%s)", routeStop);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, routeStop.authority);
		intent.putExtras(newInstanceExtra(routeStop.route));
		intent.putExtras(newInstanceExtra(routeStop.stop));
		intent.putExtras(newInstanceExtra(routeStop.trip, context));
		return intent;
	}

	public static Intent newInstance(Context context, TripStop tripStop) {
		MyLog.v(TAG, "newInstance(%s)", tripStop);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, tripStop.authority);
		intent.putExtras(newInstanceExtra(tripStop.stop));
		intent.putExtras(newInstanceExtra(tripStop.trip, context));
		return intent;
	}

	public static Intent newInstance(Context context, String authority, Stop stop) {
		MyLog.v(TAG, "newInstance(%s,%s)", authority, stop);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, authority);
		intent.putExtras(newInstanceExtra(stop));
		return intent;
	}

	public static Intent newInstance(Context context, String authority, Integer stopId) {
		MyLog.v(TAG, "newInstance(%s,%s)", authority, stopId);
		return newInstance(context, authority, stopId, null);
	}

	public static Intent newInstance(Context context, String authority, Integer stopId, Integer routeId) {
		MyLog.v(TAG, "newInstance(%s,%s,%s)", authority, stopId, routeId);
		Intent intent = new Intent(context, StopInfo.class);
		intent.putExtra(EXTRA_AUTHORITY, authority);
		intent.putExtra(EXTRA_STOP_ID, stopId);
		if (routeId != null) {
			intent.putExtra(EXTRA_ROUTE_ID, routeId);
		}
		return intent;
	}

	public static Bundle newInstanceExtra(Route route) {
		Bundle extras = new Bundle();
		if (route != null) {
			extras.putInt(EXTRA_ROUTE_ID, route.id);
			extras.putString(EXTRA_ROUTE_SHORT_NAME, route.shortName);
			extras.putString(EXTRA_ROUTE_LONG_NAME, route.longName);
			extras.putString(EXTRA_ROUTE_COLOR, route.color);
			extras.putString(EXTRA_ROUTE_TEXT_COLOR, route.textColor);
		}
		return extras;
	}

	public static Bundle newInstanceExtra(Trip trip, Context context) {
		Bundle extras = new Bundle();
		if (trip != null) {
			extras.putInt(EXTRA_TRIP_ID, trip.id);
			extras.putString(EXTRA_TRIP_HEADSIGN, trip.getHeading(context).toUpperCase(Locale.getDefault()));
			extras.putInt(EXTRA_ROUTE_ID, trip.routeId);
		}
		return extras;
	}

	public static Bundle newInstanceExtra(Stop stop) {
		Bundle extras = new Bundle();
		if (stop != null) {
			extras.putInt(EXTRA_STOP_ID, stop.id);
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
		setContentView(R.layout.stop_info);

		this.contentUri = Utils.newContentUri(Utils.getSavedStringValue(getIntent(), savedInstanceState, EXTRA_AUTHORITY));

		AbstractScheduleManager.wakeUp(getContentResolver(), this.contentUri);

		this.adapter = new POIArrayAdapter(this);
		this.adapter.setManualLayout((ViewGroup) findViewById(R.id.nearby_list));
		this.adapter.setManualScrollView((ScrollView) findViewById(R.id.scrollview));

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		// TODO NFC SupportFactory.get().registerNfcCallback(this, this, MIME_TYPE);
		// TODO NFC SupportFactory.get().setOnNdefPushCompleteCallback(this, this);
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
			final Location bestLastKnownLocationOrNull = LocationUtils.getBestLastKnownLocation(this);
			if (bestLastKnownLocationOrNull != null) {
				// set the new distance
				this.location = null; // force reset
				setLocation(bestLastKnownLocationOrNull);
			}
			// re-enable
			this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		AdsUtils.resumeAd(this);
		refreshFavoriteIDsFromDB();
		setStopFromIntent(getIntent(), null);
		setIntent(null); // set intent as processed
		// TODO NFC SupportFactory.get().enableNfcForegroundDispatch(this);
		// if (this.adapter != null) {
		// this.adapter.onResume();
		// }
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		// TODO NFC SupportFactory.get().disableNfcForegroundDispatch(this);
		this.paused = true;
		this.locationUpdatesEnabled = LocationUtils.disableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled);
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(this, this);
			this.compassUpdatesEnabled = false;
		}
		this.adapter.onPause();
		AdsUtils.pauseAd(this);
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
		// MyLog.v(TAG, "updateCompass(%s, %s)", orientation, force);
		if (this.routeTripStop == null) {
			return;
		}
		final long now = System.currentTimeMillis();
		SensorUtils.updateCompass(force, getLocation(), orientation, now, OnScrollListener.SCROLL_STATE_IDLE, this.lastCompassChanged,
				this.lastCompassInDegree, new SensorUtils.SensorTaskCompleted() {

					@Override
					public void onSensorTaskCompleted(boolean result) {
						if (result) {
							lastCompassInDegree = (int) orientation;
							lastCompassChanged = now;
							// update the view
							ImageView compassImg = (ImageView) findViewById(R.id.compass);
							if (location.getAccuracy() <= routeTripStop.getDistance()) {
								float compassRotation = SensorUtils.getCompassRotationInDegree(location, routeTripStop, lastCompassInDegree,
										locationDeclination);
								SupportFactory.get().rotateImageView(compassImg, compassRotation, StopInfo.this);
								compassImg.setVisibility(View.VISIBLE);
							} else {
								compassImg.setVisibility(View.INVISIBLE);
							}
							// } else {
							// MyLog.d(TAG, "updateCompass()>onSensorTaskCompleted()> no result!");
						}
					}
				});
	}

	/**
	 * Retrieve the stop from the Intent or from the Bundle.
	 * @param intent the intent
	 * @param savedInstanceState the saved instance state (Bundle)
	 */
	private void setStopFromIntent(Intent intent, Bundle savedInstanceState) {
		MyLog.v(TAG, "setStopFromIntent()");
		if (intent != null) {
			String authority;
			Integer stopId;
			String stopCode = null;
			String stopName = null;
			Integer routeId;
			String routeShortName = null;
			String routeColor = null;
			String routeTextColor = null;
			Integer tripId;
			String tripHeadsign = null;
			// TODO NFC if (SupportFactory.get().isNfcIntent(intent)) {
			// TODO NFC SupportFactory.get().processNfcIntent(intent, this);
			// TODO NFC return;
			// TODO NFC } else
			if (Intent.ACTION_VIEW.equals(intent.getAction())) { // TODO
				String pathSegment = intent.getData().getPathSegments().get(1); // TODO
				stopId = Integer.valueOf(pathSegment.substring(0, 5)); // TODO
				routeId = Integer.valueOf(pathSegment.substring(5)); // TODO
				tripId = null; // TODO
				authority = org.montrealtransit.android.provider.StmBusManager.AUTHORITY; // TODO
			} else {
				authority = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_AUTHORITY);
				stopId = Utils.getSavedIntValue(intent, savedInstanceState, EXTRA_STOP_ID);
				stopCode = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_CODE);
				stopName = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_NAME);
				routeId = Utils.getSavedIntValue(intent, savedInstanceState, EXTRA_ROUTE_ID);
				routeShortName = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_ROUTE_SHORT_NAME);
				routeColor = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_ROUTE_COLOR);
				routeTextColor = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_ROUTE_TEXT_COLOR);
				tripId = Utils.getSavedIntValue(intent, savedInstanceState, EXTRA_TRIP_ID);
				tripHeadsign = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_TRIP_HEADSIGN);
			}
			showNewStop(authority, stopId, stopCode, stopName, routeId, routeShortName, routeColor, routeTextColor, tripId, tripHeadsign);
		}
	}

	// @Override
	// TODO NFC public String[] getNfcMimeMessages() {
	// MyLog.v(TAG, "getNfcMimeMessages()");
	// List<String> msg = new ArrayList<String>();
	// // TODO authority
	// // add stop code => ID
	// msg.add(this.routeTripStop.stop.code);
	// // add route short name => ID
	// msg.add(this.routeTripStop.route.shortName);
	// // add next stops if loaded
	// if (this.hours != null) {
	// StopHours tmp = this.hours;
	// tmp.setSourceName(getString(R.string.nfc));
	// msg.add(tmp.serialized());
	// }
	// return msg.toArray(new String[] {});
	// }

	// @Override
	// TODO NFC public void onNfcPushComplete() {
	// // MyLog.v(TAG, "onNfcPushComplete()");
	// }
	//
	// @Override
	// TODO NFC public void processNfcRecords(String[] stringRecords) {
	// MyLog.v(TAG, "processNfcRecords(%s)", stringRecords.length);
	// TODO authority
	// // extract stop code => ID
	// String stopCode = stringRecords[0];
	// // extract route short name => ID
	// String lineNumber = stringRecords[1];
	// // extract next stops if provided
	// if (stringRecords.length > 2) {
	// try {
	// StopHours tmp = StopHours.deserialized(stringRecords[2]);
	// if (tmp != null && tmp.getSHours().size() != 0) {
	// saveToMemCache(Integer.valueOf(stopCode), Integer.valueOf(lineNumber), tmp);
	// this.hours = tmp;
	// // } else {
	// // MyLog.d(TAG, "No stop hours from the NFC record!");
	// }
	// } catch (Exception e) {
	// MyLog.w(TAG, e, "Something went wrong while parsing the stop hours!");
	// }
	// }
	// // show the stop
	// // TODO showNewStop(stopCode, null, lineNumber, null, null, null);
	// }

	@Override
	public Object onRetainNonConfigurationInstance() {
		// save the current hours
		return this.stopTimes != null ? this.stopTimes : null;
	}

	/**
	 * Show the next stops info dialog
	 * @param v useless - can be null
	 */
	public void showNextStopsInfoDialog(View v) {
		MyLog.v(TAG, "showNextStopsInfoDialog()");
		String message;
		if (this.stopTimes != null && this.stopTimes.isRealtime()) {
			message = getString(R.string.next_stops_message_and_source, this.stopTimes.getSourceName());
		} else {
			message = getString(R.string.next_stops_message);
		}
		new AlertDialog.Builder(this).setTitle(getString(R.string.next_stops)).setIcon(R.drawable.ic_btn_info_details).setMessage(message)
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
				return DataManager.findFav(StopInfo.this.getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP, StopInfo.this.routeTripStop.getUID());
			}

			protected void onPostExecute(Fav result) {
				final CheckBox starCb = (CheckBox) findViewById(R.id.star);
				starCb.setChecked(result != null);
				starCb.setVisibility(View.VISIBLE);
			};
		}.execute();
	}

	/**
	 * Set the stop info basic UI.
	 */
	private void refreshStopInfo() {
		MyLog.v(TAG, "refreshStopInfo()");
		((TextView) findViewById(R.id.stop_code)).setText(this.routeTripStop.stop.code);
		// set stop place name
		((TextView) findViewById(R.id.stop_name)).setText(BusUtils.cleanBusStopPlace(this.routeTripStop.stop.name));
		// set the favorite icon
		setTheStar();
		// set route short name & trip heading
		final TextView routeShortNameTv = (TextView) findViewById(R.id.route_short_name);
		if (TextUtils.isEmpty(this.routeTripStop.route.shortName)) {
			routeShortNameTv.setVisibility(View.GONE);
			findViewById(R.id.route_type_img).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.route_type_img).setVisibility(View.GONE);
			routeShortNameTv.setText(this.routeTripStop.route.shortName);
			routeShortNameTv.setTextColor(Utils.parseColor(this.routeTripStop.route.textColor));
			routeShortNameTv.setVisibility(View.VISIBLE);
		}
		findViewById(R.id.banner).findViewById(R.id.route).setBackgroundColor(Utils.parseColor(this.routeTripStop.route.color));
		setTripHeading(this.routeTripStop.trip.getHeading(this));
		// set listener
		findViewById(R.id.route_trip).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// MyLog.v(TAG, "onView()");
				startActivity(RouteInfo.newInstance(StopInfo.this, StopInfo.this.contentUri.getAuthority(), StopInfo.this.routeTripStop));
			}
		});
		// display location if known
		updateDistanceWithNewLocation(getLocation());
	}

	private void refreshNearby() {
		MyLog.v(TAG, "refreshNearby()");
		if (this.nearbyTask != null && this.nearbyTask.getStatus() == Status.RUNNING) {
			this.nearbyTask.cancel(true);
			this.nearbyTask = null;
		}
		setNearbyAsLoading();
		this.nearbyTask = new ClosestRouteTripStopsFinderTaskAndFilter(this, this, AbstractManager.AUTHORITIES, Utils.NB_NEARBY_LIST, this.routeTripStop);
		this.nearbyTask.execute(this.routeTripStop.getLat(), this.routeTripStop.getLng());
	}

	public void setNearbyAsLoading() {
		findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.VISIBLE);
	}

	@Override
	public void onClosestStopsProgress(String message) {
		MyLog.v(TAG, "onClosestStopsProgress(%s)", message);
	}

	@Override
	public void onClosestStopsDone(ClosestPOI<RouteTripStop> result) {
		MyLog.v(TAG, "onClosestStopsDone(%s)", (result == null ? null : result.getPoiListSize()));
		List<RouteTripStop> routeTripStops = result.getPoiListOrNull();
		if (routeTripStops != null) {
			ListIterator<RouteTripStop> routeTripStopsIt = routeTripStops.listIterator();
			while (routeTripStopsIt.hasNext()) {
				RouteTripStop routeTripStop = routeTripStopsIt.next();
				// IF same stop DO
				if (routeTripStop.stop.id == this.routeTripStop.stop.id) {
					routeTripStopsIt.remove();
					continue;
				}
				// TODO slow // IF last stop of the trip DO
				// final TripStop lastTripStop = AbstractManager.findTripLastTripStop(StopInfo.this, Utils.newContentUri(authority),
				// routeTripStop.trip.id);
				// if (lastTripStop != null && routeTripStop.stop.id == lastTripStop.stop.id) {
				// it.remove();
				// continue;
				// }
			}
			// remove last trip stops now (because all nearby list takes too long)
			ListIterator<RouteTripStop> resultIt = routeTripStops.listIterator();
			while (resultIt.hasNext()) {
				final RouteTripStop routeTripStop = resultIt.next();
				if (routeTripStop == null) { // || !(poi instanceof RouteTripStop)) {
					// MyLog.d(TAG, "refreshNearby()>Null or not Route Trip Stop! %s", poi);
					continue;
				}
				// IF last stop of the trip DO
				final TripStop lastTripStop = AbstractManager.findTripLastTripStop(this, Utils.newContentUri(routeTripStop.authority), routeTripStop.trip.id);
				if (lastTripStop != null && routeTripStop.stop.id == lastTripStop.stop.id) {
					resultIt.remove();
					// MyLog.d(TAG, "refreshNearby()>REMOVED %s", routeTripStop.stop);
					continue;
				}
				// MyLog.d(TAG, "refreshNearby()>NOT REMOVED");
			}
		}
		// MyLog.d(TAG, "refreshNearby()> result size: %s", result.size());
		this.adapter.setPois(routeTripStops);
		this.adapter.updateDistancesNow(this.location);
		refreshFavoriteIDsFromDB();
		// show the result
		this.adapter.initManual();
		setNearbyAsNotLoading();

	}

	public void setNearbyAsNotLoading() {
		findViewById(R.id.nearby_title).setVisibility(View.VISIBLE);
		findViewById(R.id.nearby_loading).setVisibility(View.GONE);
		findViewById(R.id.nearby_list).setVisibility(View.VISIBLE);
	}

	private void refreshFavoriteIDsFromDB() {
		new AsyncTask<Void, Void, List<Fav>>() {
			@Override
			protected List<Fav> doInBackground(Void... params) {
				return DataManager.findAllFavsList(getContentResolver());
			}

			@Override
			protected void onPostExecute(List<Fav> result) {
				StopInfo.this.adapter.setFavs(result);
			};
		}.execute();
	}

	/**
	 * Set the other route trips using this stop.
	 */
	private void refreshOtherRouteTripsInfo() {
		MyLog.v(TAG, "refreshOtherRouteTripsInfo()");
		if (this.otherRouteTrips != null) {
			refreshOtherRouteTripsUI();
			return;
		}
		new AsyncTask<Integer, Void, List<RouteTripStop>>() {
			@Override
			protected List<RouteTripStop> doInBackground(Integer... params) {
				MyLog.v(TAG, "refreshOtherRouteTripsInfo()>doInBackground(%s)", params[0]);
				List<RouteTripStop> result = AbstractManager.findRouteTripStopWithStopIdList(StopInfo.this, StopInfo.this.contentUri, params[0], false);
				if (result != null) {
					// remove all routes with the same route ID
					ListIterator<RouteTripStop> it = result.listIterator();
					while (it.hasNext()) {
						final RouteTripStop routeTripStop = it.next();
						// IF same trip DO
						if (routeTripStop.trip.id == StopInfo.this.routeTripStop.trip.id) {
							it.remove();
							continue;
						}
						// // IF same route DO // TODO really?
						// if (routeTripStop.trip.routeId == StopInfo.this.routeTripStop.trip.routeId) {
						// it.remove();
						// continue;
						// }
						// IF last stop of the trip DO
						final TripStop lastTripStop = AbstractManager.findTripLastTripStop(StopInfo.this, StopInfo.this.contentUri, routeTripStop.trip.id);
						if (lastTripStop != null && routeTripStop.stop.id == lastTripStop.stop.id) {
							it.remove();
							continue;
						}
					}
				}
				return result;
			}

			@Override
			protected void onPostExecute(List<RouteTripStop> result) {
				MyLog.v(TAG, "refreshOtherRouteTripsInfo()>onPostExecute(%s)", (result == null ? null : result.size()));
				StopInfo.this.otherRouteTrips = result;
				refreshOtherRouteTripsUI();
				refreshNearby();
			}
		}.execute(this.routeTripStop.stop.id);
	}

	/**
	 * Refresh other route trips UI.
	 */
	private void refreshOtherRouteTripsUI() {
		MyLog.v(TAG, "refreshOtherRouteTripsUI()");
		LinearLayout otherRouteTripsLayout = (LinearLayout) findViewById(R.id.other_route_trip_list);
		otherRouteTripsLayout.removeAllViews();
		final View otherRouteTripsTitle = findViewById(R.id.other_route_trip_title);
		if (this.otherRouteTrips == null || this.otherRouteTrips.size() == 0) {
			otherRouteTripsTitle.setVisibility(View.GONE);
			otherRouteTripsLayout.setVisibility(View.GONE);
			return;
		}
		if (this.routeTripStop.authority.contains("subway")) {
			((ImageView) otherRouteTripsTitle.findViewById(R.id.type_logo)).setImageResource(R.drawable.ic_btn_subway);
		} else {
			((ImageView) otherRouteTripsTitle.findViewById(R.id.type_logo)).setImageResource(R.drawable.ic_btn_bus);
		}
		for (final RouteTripStop routeTripStop : this.otherRouteTrips) {
			// MyLog.d(TAG, "refreshOtherRouteTripsUI()> routeTrip: %s", routeTrip);
			// the view
			final View view = getLayoutInflater().inflate(R.layout.stop_info_route_trip_list_item, otherRouteTripsLayout, false);
			final TextView routeShortNameTv = (TextView) view.findViewById(R.id.route_short_name);
			if (TextUtils.isEmpty(routeTripStop.route.shortName)) {
				routeShortNameTv.setVisibility(View.GONE);
				view.findViewById(R.id.route_type_img).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.route_type_img).setVisibility(View.GONE);
				routeShortNameTv.setText(routeTripStop.route.shortName);
				routeShortNameTv.setTextColor(Utils.parseColor(routeTripStop.route.textColor));
				routeShortNameTv.setVisibility(View.VISIBLE);
			}
			view.findViewById(R.id.route).setBackgroundColor(Utils.parseColor(routeTripStop.route.color));
			// line direction
			((TextView) view.findViewById(R.id.trip_heading)).setText(routeTripStop.trip.getHeading(this).toUpperCase(Locale.getDefault()));
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyLog.v(TAG, "onClick()");
					switchToOtherStop(routeTripStop);
				}
			});
			view.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					new RouteSelectTripDialog(StopInfo.this, routeTripStop.authority, routeTripStop.route, routeTripStop.trip.id, null).showDialog();
					return true;
				}
			});
			otherRouteTripsLayout.addView(view);
		}
		otherRouteTripsTitle.setVisibility(View.VISIBLE);
		otherRouteTripsLayout.setVisibility(View.VISIBLE);
	};

	private void switchToOtherStop(RouteTripStop otherStop) {
		MyLog.v(TAG, "switchToOtherStop(%s)", otherStop);
		cancelScheduleTasks();
		this.stopTimes = null;
		this.otherRouteTrips.remove(otherStop);
		this.otherRouteTrips.add(this.routeTripStop);
		Collections.sort(this.otherRouteTrips, new Comparator<RouteTripStop>() {
			@Override
			public int compare(RouteTripStop lhs, RouteTripStop rhs) {
				if (lhs == null || rhs == null) {
					return lhs == null ? +1 : -1;
				}
				try {
					return Integer.valueOf(lhs.route.shortName) - Integer.valueOf(rhs.route.shortName);
				} catch (NumberFormatException nfe) {
					// compare route short name as string
					return lhs.route.shortName.compareTo(rhs.route.shortName);
				}
			}
		});
		this.routeTripStop = otherStop;
		refreshStopInfo();
		refreshOtherRouteTripsUI();
		((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_stops));
		setNextStopsLoading();
		showNextStopsFromCacheAndStartLoading();
	}

	private void setTripHeading(String newTripHeading) {
		MyLog.v(TAG, "setTripHeading(%s)", newTripHeading);
		final TextView tripHeadingTv = (TextView) findViewById(R.id.trip_heading);
		if (TextUtils.isEmpty(newTripHeading)) {
			tripHeadingTv.setText(getString(R.string.ellipsis));
			return;
		}
		final String tripHeadingStr = newTripHeading.toUpperCase(Locale.ENGLISH);
		// MyLog.d(TAG, "setTripHeading() > new string ? : " + tripHeadingStr + " == " + tripHeadingTv.getText());
		if (!tripHeadingStr.equals(tripHeadingTv.getText())) {
			// MyLog.d(TAG, "setTripHeading() > NEW STRING!");
			tripHeadingTv.setText(tripHeadingStr);
			// MyLog.d(TAG, "setTripHeading() > tripHeadingTv.getLayout() null ? " + (tripHeadingTv.getLayout() == null));
			if (tripHeadingTv.getLayout() != null) {
				// MyLog.d(TAG, "setTripHeading() > ELLIPSIZE:" + tripHeadingTv.getLayout().getEllipsisCount(0));
				if (tripHeadingTv.getLayout().getEllipsisCount(0) > 0) {
					tripHeadingTv.setGravity(Gravity.CENTER_VERTICAL);
				} else { // no "..."
					tripHeadingTv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
				}
			} else { // wait for layout
				tripHeadingTv.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						// TextView tripHeadingTv = (TextView) findViewById(R.id.trip_heading);
						// MyLog.d(TAG, "setTripHeading() > onGlobalLayout() > ELLIPSIZE:" + tripHeadingTv.getLayout().getEllipsisCount(0));
						if (tripHeadingTv.getLayout().getEllipsisCount(0) > 0) {
							tripHeadingTv.setGravity(Gravity.CENTER_VERTICAL);
						} else { // no "..."
							tripHeadingTv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
						}
						SupportFactory.get().removeOnGlobalLayoutListener(tripHeadingTv.getViewTreeObserver(), this);
					}
				});
			}
		}
	}

	/**
	 * Show the new stop information.
	 */
	public void showNewStop(final String newAuthority, final Integer newStopId, String newStopCode, String newStopName, final Integer newRouteId,
			String newRouteShortName, String newRouteColor, String newRouteTextColor, final Integer newTripId, String newTripHeading) {
		MyLog.v(TAG, "showNewStop(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", newAuthority, newStopId, newStopCode, newStopName, newRouteId, newRouteShortName,
				newRouteColor, newRouteTextColor, newTripId, newTripHeading);
		// temporary set UI
		((TextView) findViewById(R.id.stop_code)).setText(newStopCode);
		if (!TextUtils.isEmpty(newStopName)) {
			((TextView) findViewById(R.id.stop_name)).setText(BusUtils.cleanBusStopPlace(newStopName));
		}
		TextView routeShortNameTv = (TextView) findViewById(R.id.route_short_name);
		if (!TextUtils.isEmpty(newAuthority)) {
			if (newAuthority.contains("subway")) {
				routeShortNameTv.setVisibility(View.GONE);
				findViewById(R.id.route_type_img).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.route_type_img).setVisibility(View.GONE);
				routeShortNameTv.setVisibility(View.VISIBLE);
				routeShortNameTv.setText(newRouteShortName);
				if (!TextUtils.isEmpty(newRouteTextColor)) {
					routeShortNameTv.setTextColor(Utils.parseColor(newRouteTextColor));
				}
			}
		}
		if (!TextUtils.isEmpty(newRouteColor)) {
			findViewById(R.id.banner).findViewById(R.id.route).setBackgroundColor(Utils.parseColor(newRouteColor));
		}
		setTripHeading(newTripHeading);
		// set as loading
		((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_stops));
		// hide unknown data
		findViewById(R.id.distance).setVisibility(View.INVISIBLE);
		findViewById(R.id.compass).setVisibility(View.INVISIBLE);
		findViewById(R.id.other_route_trip_title).setVisibility(View.GONE);
		findViewById(R.id.other_route_trip_list).setVisibility(View.GONE);
		findViewById(R.id.nearby_title).setVisibility(View.GONE);
		findViewById(R.id.nearby_loading).setVisibility(View.GONE);
		findViewById(R.id.nearby_list).setVisibility(View.GONE);
		// setNearbyListAsLoading();
		this.adapter.setPois(null);
		findViewById(R.id.star).setVisibility(View.INVISIBLE);
		this.stopTimes = null;
		setNextStopsLoading();

		new AsyncTask<String, Void, Boolean>() {
			private boolean isNewStop;

			@Override
			protected Boolean doInBackground(String... params) {
				isNewStop = StopInfo.this.routeTripStop == null || StopInfo.this.routeTripStop.stop == null || StopInfo.this.routeTripStop.stop.id != newStopId
						|| StopInfo.this.routeTripStop.route == null || StopInfo.this.routeTripStop.route.id != newRouteId
						|| StopInfo.this.routeTripStop.trip == null || StopInfo.this.routeTripStop.trip.id != newTripId;
				if (isNewStop) {
					RouteTripStop newRouteTripStop = null; // reset
					Uri contentUri = Utils.newContentUri(newAuthority);
					if (newStopId != null) {
						if (newRouteId != null) {
							if (newTripId != null) {
								// user specific stop ID & route ID & tripID
								newRouteTripStop = AbstractManager.findRouteTripStop(StopInfo.this, contentUri, newStopId, newRouteId, newTripId);
							}
							if (newRouteTripStop == null) { // trip ID invalid
								// use any trip with this stop ID & route ID
								final List<RouteTripStop> stopRouteTrips = AbstractManager.findRouteTripStopWithStopIdList(StopInfo.this, contentUri,
										newStopId, newRouteId, false);
								if (stopRouteTrips != null && stopRouteTrips.size() > 0) {
									newRouteTripStop = stopRouteTrips.get(0);
								}
							}
						}
						if (newRouteTripStop == null) { // route ID & trip ID invalid
							// use any route trip with this stop ID
							final List<RouteTripStop> stopRoutes = AbstractManager.findRouteTripStopWithStopIdList(StopInfo.this, contentUri, newStopId, false);
							if (stopRoutes != null && stopRoutes.size() > 0) {
								newRouteTripStop = stopRoutes.get(0);
							}
						}
					}
					if (newRouteTripStop == null) {
						return false; // no stop found
					}
					StopInfo.this.contentUri = Utils.newContentUri(newAuthority);
					StopInfo.this.routeTripStop = newRouteTripStop;
					StopInfo.this.otherRouteTrips = null; // reset
					StopInfo.this.stopTimes = null; // clear current stop hours
					// StopInfo.this.memCache = null; // clear the cache for the new stop
					cancelScheduleTasks();
					cancelNearbyTask();
					return true; // new stop found
				} else {
					return true; // same stop
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (Boolean.FALSE.equals(result)) {
					stopNotFound(String.valueOf(newStopId));
				} else {
					setUpUI();
				}
			};
		}.execute();
	}

	private void stopNotFound(String newStopCode) {
		if (this.routeTripStop != null) {
			// just notify
			Utils.notifyTheUser(this, getString(R.string.wrong_stop_code_and_code, newStopCode));
		} else {
			// quit
			Utils.notifyTheUser(this, getString(R.string.wrong_stop_code_and_code, newStopCode));
			finish();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(%s)", which);
		if (which == -2) {
			dialog.dismiss(); // CANCEL
			this.finish(); // close the activity.
		} else {
			// try to load the next stop from the web.
			loadNextStopsFromSchedule(false);
		}
	}

	/**
	 * Setup all the UI (based on the stop).
	 */
	private void setUpUI() {
		refreshStopInfo();
		showNextStopsFromCacheAndStartLoading();
		// refreshOtherRouteTripsInfo();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
		}
		// IF location updates are not already enabled DO
		this.locationUpdatesEnabled = LocationUtils.enableLocationUpdatesIfNecessary(this, this, this.locationUpdatesEnabled, this.paused);
	}

	/**
	 * Show the next stops (or launch refresh next stops task).
	 */
	private void showNextStopsFromCacheAndStartLoading() {
		MyLog.v(TAG, "showNextStopsFromCacheAndStartLoading()");
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				MyLog.v(TAG, "showNextStops()>doInBackground()");
				if (StopInfo.this.stopTimes == null) {
					// check cache
					final String uuid = StopInfo.this.routeTripStop.getUUID();
					// IF not yet in memory cache DO
					if (!StopInfo.this.memCache.containsKey(uuid)) {
						// try loading cache from database
						final String[] scheduleAuthorities = AbstractScheduleManager.authoritiesToScheduleAuthorities.get(StopInfo.this.contentUri
								.getAuthority());
						if (scheduleAuthorities != null) {
							for (String scheduleAuthority : scheduleAuthorities) {
								final StopTimes stopTime = AbstractScheduleManager.findStopTimes(StopInfo.this.getContentResolver(),
										Utils.newContentUri(scheduleAuthority), StopInfo.this.routeTripStop, Utils.recentTimeMillis(), true, null);
								if (stopTime != null) {
									StopInfo.this.memCache.put(uuid, stopTime);
									break; // first result is good enough, will check all provider later
								}
							}
						}
					}
					StopTimes cache = StopInfo.this.memCache.get(uuid);
					if (cache != null) {
						StopInfo.this.stopTimes = cache;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (StopInfo.this.stopTimes != null) {
					showNewNextStops();
					refreshOtherRouteTripsInfo();
				}
				loadNextStopsFromSchedule(false);
			}

		}.execute();
	}

	private Map<String, ScheduleTask> scheduleTasks = new HashMap<String, ScheduleTask>();

	private void loadNextStopsFromSchedule(boolean force) {
		MyLog.v(TAG, "loadNextStopsFromSchedule(%s)", force);
		// 1st - cancel all current tasks
		cancelScheduleTasks();
		setNextStopsLoading();
		// 2nd - start a new loading task for each available provider
		final String[] scheduleAuthorities = AbstractScheduleManager.authoritiesToScheduleAuthorities.get(this.contentUri.getAuthority());
		if (scheduleAuthorities != null) {
			for (String scheduleAuthority : scheduleAuthorities) {
				ScheduleTask scheduleTask = new ScheduleTask(this, this, this.routeTripStop, scheduleAuthority, force);
				scheduleTask.execute();
				this.scheduleTasks.put(scheduleAuthority, scheduleTask);
				this.nbTaskRunning++;
			}
		}
	}

	private void cancelScheduleTasks() {
		MyLog.v(TAG, "cancelScheduleTasks()");
		if (this.scheduleTasks != null) {
			for (ScheduleTask scheduleTask : this.scheduleTasks.values()) {
				if (scheduleTask != null && scheduleTask.getStatus() == Status.RUNNING) {
					scheduleTask.cancel(true);
				}
			}
			this.scheduleTasks.clear();
		}
		this.nbTaskRunning = 0;
	}

	@Override
	public void onNextStopsProgress(String scheduleAuthority, String progress) {
		MyLog.v(TAG, "onNextStopsProgress(%s,%s)", scheduleAuthority, progress);
		ScheduleTask scheduleTask = this.scheduleTasks == null ? null : this.scheduleTasks.get(scheduleAuthority);
		// IF the task was cancelled DO
		if (scheduleTask == null || scheduleTask.isCancelled()) {
			// MyLog.d(TAG, "Task cancelled!");
			return; // stop here
		}
		if (TextUtils.isEmpty(progress)) {
			return;
		}
		if (this.stopTimes != null) {
			// notify the user ?
			return;
		}
		// update the BIG message
		TextView detailMsgTv = (TextView) findViewById(R.id.detail_msg);
		detailMsgTv.setText(progress);
		detailMsgTv.setVisibility(View.VISIBLE);
	}

	@Override
	public void onNextStopsLoaded(String scheduleAuthority, Map<String, StopTimes> results) {
		MyLog.v(TAG, "onNextStopsLoaded(%s)", results == null ? null : results.size());
		ScheduleTask scheduleTask = this.scheduleTasks == null ? null : this.scheduleTasks.get(scheduleAuthority);
		if (scheduleTask == null || scheduleTask.isCancelled()) {
			MyLog.d(TAG, "Task cancelled! (%s)", scheduleAuthority);
			setTaskAsCompleted();
			return;
		}
		if (this.stopTimes != null && this.stopTimes.isRealtime()) {
			MyLog.d(TAG, "Task too late (%s)", scheduleAuthority);
			setTaskAsCompleted();
			return;
		}
		if (results == null) {
			MyLog.d(TAG, "No result! (%s)", scheduleAuthority);
			setTaskAsCompleted();
			return;
		}
		// MyLog.d(TAG, "%s:%s", results.keySet(), results.values());
		StopTimes result = results.get(this.routeTripStop.getUUID());
		if (result == null) {
			MyLog.d(TAG, "No result for this trip! (%s)", scheduleAuthority);
			setTaskAsCompleted();
			return;
		}
		// MyLog.d(TAG, "result:%s", result);
		// IF error DO
		if (result == null || result.getSTimes().size() <= 0) {
			MyLog.d(TAG, "Local DB no hours in result");
			// process the error
			// if (this.wwwTaskRunning) {
			if (this.nbTaskRunning > 1) { // 1?
				if (result != null && !TextUtils.isEmpty(result.getError())) {
					onNextStopsProgress(scheduleAuthority, result.getError());
				} else if (result != null && !TextUtils.isEmpty(result.getMessage())) {
					onNextStopsProgress(scheduleAuthority, result.getMessage());
				} else if (result != null && !TextUtils.isEmpty(result.getMessage2())) {
					onNextStopsProgress(scheduleAuthority, result.getMessage2());
				}
			} else {
				setNextStopsError(result);
			}
			setTaskAsCompleted();
			return;
		}
		// show the result
		if (result.isRealtime()) {
			cancelScheduleTasks(); // cancel other schedule tasks
			this.stopTimes = result;
			showNewNextStops();
		} else { // not real-time
			if (this.stopTimes == null || this.stopTimes.getSourceName().equals(result.getSourceName())) {
				saveToMemCache(this.routeTripStop.getUUID(), result);
				this.stopTimes = result;
				showNewNextStops();
			} else {
				MyLog.d(TAG, "Results from '%s' ignored because hours already known from '%s'.", result.getSourceName(), this.stopTimes.getSourceName());
			}
		}
		setTaskAsCompleted();
	}

	public void setTaskAsCompleted() {
		MyLog.v(TAG, "setLocalTaskAsCompleted()");
		this.nbTaskRunning--;
		if (this.nbTaskRunning <= 0) {
			setNextStopsNotLoading();
			refreshOtherRouteTripsInfo();
		} else if (this.stopTimes != null) {
			refreshOtherRouteTripsInfo();
		}
	}

	/**
	 * Show the new next bus stops.
	 * @param stopTimes the new next bus stops
	 */
	private void showNewNextStops() {
		MyLog.v(TAG, "showNewNextStops()");
		final StopTimes currentStopTimes = this.stopTimes;
		if (currentStopTimes != null) {
			// set next stop header with source name
			((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops_and_source, currentStopTimes.getSourceName()));
			// IF there next stops found DO
			if (currentStopTimes.getSTimes().size() > 0) {
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
				List<String> fHours = currentStopTimes.getFormattedTimes(this);
				// show the next bus stops
				SpannableStringBuilder nextStopsSb = new SpannableStringBuilder();
				// previous stop
				int startPreviousStop = nextStopsSb.length();
				if (currentStopTimes.hasPreviousTime()) {
					nextStopsSb.append(Utils.formatTimes(this, currentStopTimes.getPreviousTime())).append(' ');
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
					if (index <= 0) {
						break;
					}
					nextStopsSb.setSpan(new RelativeSizeSpan(0.1f), index - 1, index, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // remove space hack
					nextStopsSb.setSpan(new RelativeSizeSpan(0.25f), index, index + 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
					index += 2;
				}
				for (int index = word.indexOf("pm"); index >= 0; index = word.indexOf("pm", index + 1)) {
					if (index <= 0) {
						break;
					}
					nextStopsSb.setSpan(new RelativeSizeSpan(0.1f), index - 1, index, Spannable.SPAN_INCLUSIVE_INCLUSIVE); // remove space hack
					nextStopsSb.setSpan(new RelativeSizeSpan(0.25f), index, index + 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				((TextView) findViewById(R.id.next_stops)).setText(nextStopsSb);
				// show messages
				SpannableStringBuilder messageSb = getMessageSb(currentStopTimes);
				if (messageSb.length() > 0) {
					// Linkify.addLinks(messageSb, Linkify.WEB_URLS);
					messageTv.setText(Html.fromHtml(messageSb.toString()));
					messageTv.setMovementMethod(LinkMovementMethod.getInstance());
					messageTv.setVisibility(View.VISIBLE);
				} else {
					messageTv.setText(null);
					messageTv.setVisibility(View.GONE);
				}
			}
		}
	}

	/**
	 * Refresh or stop refresh the next bus stop depending on the current status of the task.
	 * @param v the view (not used)
	 */
	public void refreshOrStopRefreshNextStops(View v) {
		MyLog.v(TAG, "refreshOrStopRefreshNextStops()");
		// IF the task is running DO
		if (this.nbTaskRunning > 0) {
			// stopping the task
			cancelScheduleTasks();
			setNextStopsCancelled();
		} else {
			this.stopTimes = null; // TODO really?
			loadNextStopsFromSchedule(true);
		}
	}

	/**
	 * Set the next stops view as loading.
	 */
	private void setNextStopsLoading() {
		MyLog.v(TAG, "setNextStopsLoading()");
		if (this.stopTimes == null) {
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
		if (this.stopTimes != null) {
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
	 * @param hours the {@link StopTimes} object containing the error or <b>null</b>
	 */
	private void setNextStopsError(StopTimes hours) {
		MyLog.v(TAG, "setNextStopsError(%s)", hours);
		// IF there are hours to show DO
		if (this.stopTimes != null) {
			// notify the user but keep showing the old stations
			if (hours != null && !TextUtils.isEmpty(hours.getError())) {
				Utils.notifyTheUserTop(this, hours.getError());
			} else if (hours != null && !TextUtils.isEmpty(hours.getMessage())) {
				Utils.notifyTheUserTop(this, hours.getMessage());
			} else if (hours != null && !TextUtils.isEmpty(hours.getMessage2())) {
				Utils.notifyTheUserTop(this, hours.getMessage2());
			} else {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", this.routeTripStop.stop.code, this.routeTripStop.route.shortName);
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, this.routeTripStop.route.shortName);
				Utils.notifyTheUserTop(this, defaultMessage);
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
			SpannableStringBuilder messageSb = getMessageSb(hours);
			if (messageSb == null || messageSb.length() == 0) {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", this.routeTripStop.stop.code, this.routeTripStop.route.shortName);
				// DEFAULT MESSAGE > no more bus stop for this bus line
				final String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, this.routeTripStop.route.shortName);
				messageSb.append(defaultMessage);
			}
			// Linkify.addLinks(messageSb, Linkify.WEB_URLS);
			messageTv.setText(Html.fromHtml(messageSb.toString()));
			messageTv.setMovementMethod(LinkMovementMethod.getInstance());
			messageTv.setVisibility(View.VISIBLE);
		}
	}

	private SpannableStringBuilder getMessageSb(StopTimes hours) {
		MyLog.v(TAG, "getMessageSb()");
		SpannableStringBuilder messageSb = new SpannableStringBuilder();
		if (hours != null) {
			if (!TextUtils.isEmpty(hours.getError())) {
				// MyLog.d(TAG, "getMessageSb() > hours.getError(): " + hours.getError());
				if (messageSb.length() > 0) {
					messageSb.append('\n');
				}
				messageSb.append(hours.getError().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
			}
			if (!TextUtils.isEmpty(hours.getMessage())) {
				// MyLog.d(TAG, "getMessageSb() > hours.getMessage(): " + hours.getMessage());
				if (messageSb.length() > 0) {
					messageSb.append('\n');
				}
				messageSb.append(hours.getMessage().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
			}
			if (!TextUtils.isEmpty(hours.getMessage2())) {
				// MyLog.d(TAG, "getMessageSb() > hours.getMessage2(): " + hours.getMessage2());
				if (messageSb.length() > 0) {
					messageSb.append('\n');
				}
				messageSb.append(hours.getMessage2().replaceAll("\\.\\ ", ".\n").replaceAll("\\:\\ ", ":\n"));
			}
		}
		// MyLog.d(TAG, "getMessageSb() > messageSb: " + messageSb);
		return messageSb;
	}

	/**
	 * Save the stop times for the UUID into the local cache.
	 * @param uuid the stop UUID
	 * @param stopTimes the stop hours
	 */
	private void saveToMemCache(String uuid, StopTimes stopTimes) {
		// MyLog.v(TAG, "saveToMemCache(%s,%s)", uuid, stopTimes);
		if (stopTimes == null || stopTimes.getSTimes().size() == 0) {
			return;
		}
		this.memCache.put(uuid, stopTimes);
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
					distanceTv.setText(StopInfo.this.routeTripStop.getDistanceString());
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
				setLocation(bestLastKnownLocationOrNull);
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
		setLocation(location);
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
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP, this.routeTripStop.getUID());
		// IF the favorite exist DO
		if (findFav != null) {
			// delete the favorite
			boolean success = DataManager.deleteFav(getContentResolver(), findFav.getId());
			// WARNING DANGEROUS! DataManager.deleteFav(getContentResolver(), this.routeTripStop.getUID(), null, Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
			if (success) {
				Utils.notifyTheUser(this, getString(R.string.favorite_removed));
			} else {
				MyLog.w(TAG, "Favorite not removed!");
			}
		} else {
			// add the favorite
			Fav newFav = new Fav();
			newFav.setType(Fav.KEY_TYPE_VALUE_AUTHORITY_ROUTE_STOP);
			newFav.setFkId(this.routeTripStop.getUID());
			boolean success = DataManager.addFav(getContentResolver(), newFav) != null;
			if (success) {
				Utils.notifyTheUser(this, getString(R.string.favorite_added));
				UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_IS_FAV, true);
			} else {
				MyLog.w(TAG, "Favorite not added!");
			}
		}
		SupportFactory.get().backupManagerDataChanged(this);
		setTheStar(); // TODO is remove useless?
	}

	/**
	 * Show the bus stop in radar-enabled application.
	 * @param v the view (not used).
	 */
	public void showStopInRadar(View v) {
		LocationUtils.showPOILocationInRadar(this, this.routeTripStop);
	}

	/**
	 * Show the stop in maps-enabled application.
	 * @param v (not used)
	 */
	public void showStopLocationInMaps(View v) {
		LocationUtils.showPOILocationInMap(this, this.routeTripStop);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_stop_info_menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
		cancelScheduleTasks();
		cancelNearbyTask();
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}

	public void cancelNearbyTask() {
		if (this.nearbyTask != null) {
			this.nearbyTask.cancel(true);
			this.nearbyTask = null;
		}
	}
}

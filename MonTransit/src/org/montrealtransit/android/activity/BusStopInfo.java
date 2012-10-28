package org.montrealtransit.android.activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.montrealtransit.android.AdsUtils;
import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MenuUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SensorUtils;
import org.montrealtransit.android.SensorUtils.CompassListener;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.data.Pair;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmDbHelper;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;
import org.montrealtransit.android.services.GeocodingTask;
import org.montrealtransit.android.services.GeocodingTaskListener;
import org.montrealtransit.android.services.NfcListener;
import org.montrealtransit.android.services.nextstop.AutomaticTask;
import org.montrealtransit.android.services.nextstop.NextStopListener;
import org.montrealtransit.android.services.nextstop.StmInfoTask;
import org.montrealtransit.android.services.nextstop.StmMobileTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements LocationListener, NextStopListener, DialogInterface.OnClickListener, NfcListener, SensorEventListener,
		CompassListener {

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
	public static final String EXTRA_STOP_CODE = "extra_stop_code";
	/**
	 * The extra ID for the bus stop place (not required).
	 */
	public static final String EXTRA_STOP_PLACE = "extra_stop_place";
	/**
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_STOP_LINE_NUMBER = "extra_line_number";
	/**
	 * The extra ID for the bus line name (not required).
	 */
	public static final String EXTRA_STOP_LINE_NAME = "extra_line_name";
	/**
	 * The extra ID for the bus line type (not required).
	 */
	public static final String EXTRA_STOP_LINE_TYPE = "extra_line_type";
	/**
	 * The validity of the cache (in seconds).
	 */
	private static final int CACHE_TOO_OLD_IN_SEC = 30 * 60; // 30 minutes

	/**
	 * The NFC MIME type.
	 */
	private static final String MIME_TYPE = "application/org.montrealtransit.android.bus.stop";
	/**
	 * The bus stop.
	 */
	private StmStore.BusStop busStop;
	/**
	 * The bus line.
	 */
	private StmStore.BusLine busLine;
	/**
	 * The subway station or <b>null</b>.
	 */
	private SubwayStation subwayStation;
	/**
	 * Store the current hours (including messages).
	 */
	private BusStopHours hours;
	/**
	 * The cache for the current bus stop (code+line number).
	 */
	private Cache cache;
	/**
	 * The task used to load the next bus stops.
	 */
	private AsyncTask<StmStore.BusStop, String, Map<String, BusStopHours>> task;
	/**
	 * The other bus stop lines.
	 */
	protected List<BusStop> otherBusStopLines;
	/**
	 * The {@link Sensor#TYPE_ACCELEROMETER} values.
	 */
	private float[] accelerometerValues;
	/**
	 * The {@link Sensor#TYPE_MAGNETIC_FIELD} values.
	 */
	private float[] magneticFieldValues;
	/**
	 * The last compass degree.
	 */
	private int lastCompassInDegree = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		checkForUpdateRequired();
		// set the UI
		setContentView(R.layout.bus_stop_info);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		SupportFactory.getInstance(this).registerNfcCallback(this, this, MIME_TYPE);
		SupportFactory.getInstance(this).setOnNdefPushCompleteCallback(this, this);
	}

	/**
	 * Check if an update is required since the activity can be launched directly.
	 */
	public void checkForUpdateRequired() {
		if (StmDbHelper.isUpdateRequired(this)) {
			Toast.makeText(this, R.string.update_required, Toast.LENGTH_SHORT).show();
		}
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
		findViewById(R.id.the_subway_station).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubwayStation(v);
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
		if (this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistanceWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		setBusStopFromIntent(getIntent(), null);
		setIntent(null); // set intent as processed
		SupportFactory.getInstance(this).enableNfcForegroundDispatch(this);
	}

	@Override
	protected void onPause() {
		MyLog.v(TAG, "onPause()");
		SupportFactory.getInstance(this).disableNfcForegroundDispatch(this);
		LocationUtils.disableLocationUpdates(this, this);
		SensorUtils.unregisterSensorListener(this, this);
		super.onPause();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// MyLog.v(TAG, "onAccuracyChanged(%s)", accuracy);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// MyLog.v(TAG, "onSensorChanged()");
		// SensorUtils.checkForCompass(event, this.accelerometerValues, this.magneticFieldValues, this);
		checkForCompass(event, this);
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
		}
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
		// MyLog.v(TAG, "updateCompass(%s)", orientation[0]);
		Location currentLocation = getLocation();
		if (currentLocation != null) {
			int io = (int) orientation[0];
			if (io != 0 && Math.abs(this.lastCompassInDegree - io) > SensorUtils.LIST_VIEW_COMPASS_DEGREE_UPDATE_THRESOLD) {
				this.lastCompassInDegree = io;
				// update bike station compass
				if (this.busStop != null) {
					Matrix matrix = new Matrix();
					matrix.postRotate(
							SensorUtils.getCompassRotationInDegree(this, currentLocation, this.busStop.getLocation(), orientation, getLocationDeclination()),
							getArrowDimLight().first / 2, getArrowDimLight().second / 2);
					ImageView compassImg = (ImageView) findViewById(R.id.compass);
					compassImg.setImageMatrix(matrix);
					compassImg.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private Float locationDeclination;

	private float getLocationDeclination() {
		if (this.locationDeclination == null && this.location != null) {
			this.locationDeclination = new GeomagneticField((float) this.location.getLatitude(), (float) this.location.getLongitude(),
					(float) this.location.getAltitude(), this.location.getTime()).getDeclination();
		}
		return this.locationDeclination;
	}

	private Pair<Integer, Integer> arrowDimLight;

	public Pair<Integer, Integer> getArrowDimLight() {
		if (this.arrowDimLight == null) {
			this.arrowDimLight = SensorUtils.getResourceDimension(this, R.drawable.heading_arrow_light);
		}
		return this.arrowDimLight;
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
			String lineName = null;
			String lineType = null;
			if (SupportFactory.getInstance(this).isNfcIntent(intent)) {
				SupportFactory.getInstance(this).processNfcIntent(intent, this);
				return;
			} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				String pathSegment = intent.getData().getPathSegments().get(1);
				stopCode = pathSegment.substring(0, 5);
				lineNumber = pathSegment.substring(5);
			} else {
				stopCode = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_CODE);
				stopPlace = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_PLACE);
				lineNumber = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_LINE_NUMBER);
				lineName = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_LINE_NAME);
				lineType = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_LINE_TYPE);
			}
			showNewBusStop(stopCode, stopPlace, lineNumber, lineName, lineType);
		}
	}

	@Override
	public String[] getNfcMimeMessages() {
		MyLog.v(TAG, "getNfcMimeMessages()");
		List<String> msg = new ArrayList<String>();
		// add bus stop code
		msg.add(this.busStop.getCode());
		// add bus line number
		msg.add(this.busStop.getLineNumber());
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
					saveToCache(stopCode, lineNumber, tmp);
					this.hours = tmp;
					// } else {
					// MyLog.d(TAG, "No bus stop hours from the NFC record!");
				}
			} catch (Exception e) {
				MyLog.w(TAG, e, "Something went wrong while parsing the bus stop hours!");
			}
		}
		// show the bus stop
		showNewBusStop(stopCode, null, lineNumber, null, null);
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
			message = getString(R.string.next_bus_stops_message_and_source, this.hours.getSourceName());
		} else {
			String provider = getProviderFromPref();
			if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				message = getString(R.string.next_bus_stops_message_and_source, StmInfoTask.SOURCE_NAME);
			} else if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				message = getString(R.string.next_bus_stops_message_and_source, StmMobileTask.SOURCE_NAME);
			} else if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_AUTO)) {
				message = getString(R.string.next_bus_stops_message_auto);
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", provider);
				message = getString(R.string.next_bus_stops_message_auto); // default Auto
			}
		}
		new AlertDialog.Builder(this).setTitle(getString(R.string.next_bus_stops)).setIcon(R.drawable.ic_btn_info_details).setMessage(message)
				.setPositiveButton(getString(android.R.string.ok), null).setCancelable(true).create().show();
	}

	/**
	 * @return the bus list "group by" preference.
	 */
	private String getProviderFromPref() {
		return UserPreferences.getPrefDefault(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER, UserPreferences.PREFS_NEXT_STOP_PROVIDER_DEFAULT);
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(this.getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(), this.busStop.getLineNumber());
		((CheckBox) findViewById(R.id.star)).setChecked(findFav != null);
		findViewById(R.id.star).setVisibility(View.VISIBLE);
	}

	/**
	 * Set the bus stop info basic UI.
	 */
	private void refreshBusStopInfo() {
		MyLog.v(TAG, "refreshBusStopInfo()");
		((TextView) findViewById(R.id.stop_code)).setText(this.busStop.getCode());
		// set bus stop place name
		((TextView) findViewById(R.id.bus_stop_place)).setText(BusUtils.cleanBusStopPlace(this.busStop.getPlace()));
		// set the favorite icon
		setTheStar();
		// set bus line number & direction
		TextView lineNumberTv = (TextView) findViewById(R.id.line_number);
		lineNumberTv.setText(this.busLine.getNumber());
		lineNumberTv.setBackgroundColor(BusUtils.getBusLineTypeBgColorFromType(this.busLine.getType()));
		((TextView) findViewById(R.id.line_direction)).setText(getString(BusUtils.getBusLineSimpleDirection(this.busStop.getDirectionId())).toUpperCase());
		// set listener
		findViewById(R.id.line).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// MyLog.v(TAG, "onView()");
				Intent mIntent = new Intent(BusStopInfo.this, SupportFactory.getInstance(BusStopInfo.this).getBusLineInfoClass());
				mIntent.putExtra(BusLineInfo.EXTRA_LINE_NUMBER, BusStopInfo.this.busLine.getNumber());
				mIntent.putExtra(BusLineInfo.EXTRA_LINE_NAME, BusStopInfo.this.busLine.getName());
				mIntent.putExtra(BusLineInfo.EXTRA_LINE_TYPE, BusStopInfo.this.busLine.getType());
				startActivity(mIntent);
			}
		});
	}

	/**
	 * Set the bus stop subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		if (TextUtils.isEmpty(this.busStop.getSubwayStationId())) {
			findViewById(R.id.subway_station).setVisibility(View.GONE);
			findViewById(R.id.the_subway_station).setVisibility(View.GONE);
			return;
		}
		new AsyncTask<String, Void, List<SubwayLine>>() {
			@Override
			protected List<SubwayLine> doInBackground(String... params) {
				MyLog.v(TAG, "doInBackground(%s)", params[0]);
				// load the subway station line(s) color(s)
				return StmManager.findSubwayStationLinesList(getContentResolver(), params[0]);
			}

			@Override
			protected void onPostExecute(List<SubwayLine> result) {
				findViewById(R.id.subway_station).setVisibility(View.VISIBLE);
				findViewById(R.id.the_subway_station).setVisibility(View.VISIBLE);
				// set subway station name
				((TextView) findViewById(R.id.station_name)).setText(BusStopInfo.this.subwayStation.getName());
				if (Utils.getCollectionSize(result) > 0) {
					((ImageView) findViewById(R.id.subway_img_1)).setImageResource(SubwayUtils.getSubwayLineImgId(result.get(0).getNumber()));
					if (result.size() > 1) {
						((ImageView) findViewById(R.id.subway_img_2)).setImageResource(SubwayUtils.getSubwayLineImgId(result.get(1).getNumber()));
						if (result.size() > 2) {
							((ImageView) findViewById(R.id.subway_img_3)).setImageResource(SubwayUtils.getSubwayLineImgId(result.get(2).getNumber()));
						} else {
							findViewById(R.id.subway_img_3).setVisibility(View.GONE);
						}
					} else {
						findViewById(R.id.subway_img_2).setVisibility(View.GONE);
						findViewById(R.id.subway_img_3).setVisibility(View.GONE);
					}
				} else {
					findViewById(R.id.subway_img_1).setVisibility(View.GONE);
					findViewById(R.id.subway_img_2).setVisibility(View.GONE);
					findViewById(R.id.subway_img_3).setVisibility(View.GONE);
				}
			};
		}.execute(this.busStop.getSubwayStationId());
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
		new AsyncTask<String, Void, List<BusStop>>() {
			@Override
			protected List<BusStop> doInBackground(String... params) {
				return StmManager.findBusStopsWithLineInfoList(BusStopInfo.this.getContentResolver(), params[0]);
			}

			@Override
			protected void onPostExecute(List<BusStop> result) {
				// MyLog.v(TAG, "onPostExecute(%s)", Utils.getCollectionSize(result));
				// remove all bus lines with the same line number
				ListIterator<BusStop> it = result.listIterator();
				Set<String> busLinesNumberDirection = new HashSet<String>();
				while (it.hasNext()) {
					BusStop busStop = it.next();
					// IF same bus stop and bus line DO
					if (busStop.getUID().equals(BusStopInfo.this.busStop.getUID())) {
						it.remove();
						continue;
					}
					// check if same bus line number & direction not already in the list
					if (busLinesNumberDirection.contains(busStop.getLineNumber() + BusUtils.getBusLineSimpleDirection(busStop.getDirectionId()))) {
						it.remove();
						continue;
					}
					// keep trace of processed bus lines number & directions
					busLinesNumberDirection.add(busStop.getLineNumber() + BusUtils.getBusLineSimpleDirection(busStop.getDirectionId()));
				}
				BusStopInfo.this.otherBusStopLines = result;
				refreshOtherBusLinesUI();
			}
		}.execute(this.busStop.getCode());
	}

	/**
	 * Refresh other bus lines UI.
	 */
	private void refreshOtherBusLinesUI() {
		MyLog.v(TAG, "refreshOtherBusLinesUI()");
		LinearLayout otherBusLinesLayout = (LinearLayout) findViewById(R.id.other_bus_line_list);
		otherBusLinesLayout.removeAllViews();
		if (this.otherBusStopLines.size() > 0) {
			findViewById(R.id.other_bus_line).setVisibility(View.VISIBLE);
			otherBusLinesLayout.setVisibility(View.VISIBLE);
			for (StmStore.BusStop busStop : this.otherBusStopLines) {
				// the view
				View view = getLayoutInflater().inflate(R.layout.bus_stop_info_bus_line_list_item, null);
				TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
				final String lineNumber = busStop.getLineNumber();
				final String lineName = busStop.getLineNameOrNull();
				final String lineType = busStop.getLineTypeOrNull();
				lineNumberTv.setText(lineNumber);
				int color = BusUtils.getBusLineTypeBgColorFromType(busStop.getLineTypeOrNull());
				lineNumberTv.setBackgroundColor(color);
				// line direction
				final String currentDirectionId = busStop.getDirectionId();
				int busLineDirection = BusUtils.getBusLineSimpleDirection(currentDirectionId);
				((TextView) view.findViewById(R.id.line_direction)).setText(getString(busLineDirection).toUpperCase());
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick()");
						showNewBusStop(BusStopInfo.this.busStop.getCode(), BusStopInfo.this.busStop.getPlace(), lineNumber, lineName, lineType);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						new BusLineSelectDirection(BusStopInfo.this, lineNumber, lineName, lineType, currentDirectionId).showDialog();
						return true;
					}
				});
				otherBusLinesLayout.addView(view);
			}
		} else {
			findViewById(R.id.other_bus_line).setVisibility(View.GONE);
			otherBusLinesLayout.setVisibility(View.GONE);
		}
	};

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newStopPlace the new bus stop place or null (optional)
	 * @param newLineNumber the new bus line number (optional)
	 * @param newLineName the new bus line name or null (optional)
	 * @param newLineType the new bus line type or null (optional)
	 */
	public void showNewBusStop(String newStopCode, String newStopPlace, String newLineNumber, String newLineName, String newLineType) {
		MyLog.v(TAG, "showNewBusStop(%s, %s, %s, %s, %s)", newStopCode, newStopPlace, newLineNumber, newLineName, newLineType);

		// temporary set UI
		((TextView) findViewById(R.id.stop_code)).setText(newStopCode);
		if (!TextUtils.isEmpty(newStopPlace)) {
			((TextView) findViewById(R.id.bus_stop_place)).setText(BusUtils.cleanBusStopPlace(newStopPlace));
		}
		TextView busLineNumberTv = (TextView) findViewById(R.id.line_number);
		busLineNumberTv.setText(newLineNumber);
		if (!TextUtils.isEmpty(newLineType)) {
			int color = BusUtils.getBusLineTypeBgColorFromType(newLineType);
			busLineNumberTv.setBackgroundColor(color);
		}
		// hide other bus lines
		findViewById(R.id.other_bus_line_list).setVisibility(View.INVISIBLE);
		findViewById(R.id.star).setVisibility(View.INVISIBLE);

		new AsyncTask<String, Void, Void>() {
			private boolean isNewBusStop;
			private boolean isNewBusLine;
			private String newStopCode;
			private String newLineNumber;

			@Override
			protected Void doInBackground(String... params) {
				newStopCode = params[0];
				newLineNumber = params[1];

				isNewBusStop = BusStopInfo.this.busStop == null || !BusStopInfo.this.busStop.getCode().equals(newStopCode)
						|| !BusStopInfo.this.busStop.getLineNumber().equals(newLineNumber);
				isNewBusLine = isNewBusStop || BusStopInfo.this.busLine == null || !BusStopInfo.this.busLine.getNumber().equals(newLineNumber);

				// IF no bus stop OR new bus stop DO
				if (BusStopInfo.this.busStop == null || !BusStopInfo.this.busStop.getCode().equals(newStopCode)) {
					// MyLog.v(TAG, "New bus stop '%s' line '%s'.", newStopCode, newLineNumber);
					if (StmManager.findBusStop(BusStopInfo.this.getContentResolver(), newStopCode) == null) {
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
						BusStopInfo.this.busStop = StmManager.findBusLineStopExt(BusStopInfo.this.getContentResolver(), newStopCode, newLineNumber);
						BusStopInfo.this.otherBusStopLines = null;
						// set subways station
						if (!TextUtils.isEmpty(BusStopInfo.this.busStop.getSubwayStationId())) {
							BusStopInfo.this.subwayStation = new StmStore.SubwayStation();
							BusStopInfo.this.subwayStation.setId(BusStopInfo.this.busStop.getSubwayStationId());
							BusStopInfo.this.subwayStation.setName(BusStopInfo.this.busStop.getSubwayStationNameOrNull());
							BusStopInfo.this.subwayStation.setLat(BusStopInfo.this.busStop.getSubwayStationLatOrNull());
							BusStopInfo.this.subwayStation.setLng(BusStopInfo.this.busStop.getSubwayStationLngOrNull());
						} else {
							BusStopInfo.this.subwayStation = null;
						}
						// load bus line from DB if necessary
						if (isNewBusLine) {
							BusStopInfo.this.busLine = StmManager.findBusLine(BusStopInfo.this.getContentResolver(), BusStopInfo.this.busStop.getLineNumber());
						}
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
					// IF new bus stop code and line number DO
					if (isNewBusLine) {
						BusStopInfo.this.cache = null; // clear the cache for the new bus stop
					}
					if (BusStopInfo.this.task != null) {
						BusStopInfo.this.task.cancel(true);
						BusStopInfo.this.task = null;
					}
				}
				setUpUI();
			};
		}.execute(newStopCode, newLineNumber);
	}

	private String findBusLineNumberFromStopCode(String newStopCode) {
		// get the bus lines for this bus stop
		List<BusLine> busLines = StmManager.findBusStopLinesList(getContentResolver(), newStopCode);
		if (busLines == null) {
			// no bus line found
			// TODO handle unknown bus stop code
			Utils.notifyTheUser(this, getString(R.string.wrong_stop_code_and_code, newStopCode));
			this.finish();
			return null;
		} else {
			// at least 1 bus line found
			// always use the first now for now
			return busLines.get(0).getNumber();
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
			refreshNextStops();
		}
	}

	/**
	 * Setup all the UI (based on the bus stop).
	 */
	private void setUpUI() {
		refreshBusStopInfo();
		showNextBusStops();
		refreshOtherBusLinesInfo();
		refreshSubwayStationInfo();
		// IF there is a valid last know location DO
		if (LocationUtils.getBestLastKnownLocation(this) != null) {
			// set the distance before showing the station
			setLocation(LocationUtils.getBestLastKnownLocation(this));
			updateDistanceWithNewLocation();
		}
		// IF location updates are not already enabled DO
		if (!this.locationUpdatesEnabled) {
			// enable
			LocationUtils.enableLocationUpdates(this, this);
			this.locationUpdatesEnabled = true;
		}
	}

	/**
	 * Show the next bus stops (or launch refresh next bus stops task).
	 */
	private void showNextBusStops() {
		MyLog.v(TAG, "showNextBusStops()");
		if (this.hours == null) {
			// check cache
			// IF no local cache DO
			if (this.cache == null) {
				// load cache from database
				this.cache = DataManager.findCache(getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP, busStop.getUID());
			}
			// compute the too old date
			int tooOld = Utils.currentTimeSec() - CACHE_TOO_OLD_IN_SEC;
			// IF the cache is too old DO
			if (this.cache != null && tooOld >= this.cache.getDate()) {
				// don't use the cache
				this.cache = null;
				// delete all too old cache
				try {
					DataManager.deleteCacheOlderThan(getContentResolver(), tooOld);
				} catch (Exception e) {
					MyLog.w(TAG, e, "Can't clean the cache!");
				}
			}
			if (this.cache != null) {
				// use cache
				this.hours = BusStopHours.deserialized(this.cache.getObject());
			}
		}

		if (this.hours == null) {
			// load from the web
			refreshNextStops();
		} else {
			showNewNextStops();
		}

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
				TextView message1Tv = (TextView) findViewById(R.id.next_stops_msg);
				TextView message2Tv = (TextView) findViewById(R.id.next_stops_msg2);
				findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
				message1Tv.setVisibility(View.GONE);
				message2Tv.setVisibility(View.GONE);
				// show next bus stop group
				findViewById(R.id.next_stops_group).setVisibility(View.VISIBLE);
				List<String> fHours = this.hours.getFormattedHours(this);
				// clear the last values
				TextView theSecondNextStopTv = (TextView) findViewById(R.id.the_second_next_stop);
				theSecondNextStopTv.setText(null);
				TextView otherNextStopsTv = (TextView) findViewById(R.id.other_stops);
				otherNextStopsTv.setText(null);
				// show the next bus stops
				((TextView) findViewById(R.id.the_next_stop)).setText(fHours.get(0));
				if (fHours.size() > 1) {
					theSecondNextStopTv.setText(fHours.get(1));
					if (fHours.size() > 2) {
						String hoursS = "";
						for (int i = 2; i < fHours.size(); i++) {
							if (hoursS.length() > 0) {
								hoursS += " ";
							}
							hoursS += fHours.get(i);
						}
						otherNextStopsTv.setText(hoursS);
					}
				}
				// show messages
				if (!TextUtils.isEmpty(this.hours.getMessage())) {
					message1Tv.setVisibility(View.VISIBLE);
					message1Tv.setText(this.hours.getMessage());
					Linkify.addLinks(message1Tv, Linkify.ALL);
				}
				if (!TextUtils.isEmpty(this.hours.getMessage2())) {
					message2Tv.setVisibility(View.VISIBLE);
					message2Tv.setText(this.hours.getMessage2());
					Linkify.addLinks(message2Tv, Linkify.ALL);
				}
			}
		}
	}

	/**
	 * Start the next bus stops refresh task if not running.
	 */
	private void refreshNextStops() {
		MyLog.v(TAG, "refreshNextStops()");
		// IF the task is NOT already running DO
		if (this.task == null || !this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
			setNextStopsLoading();
			// find the next bus stop
			String provider = getProviderFromPref();
			if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				this.task = new StmInfoTask(this, this);
				this.task.execute(this.busStop);
			} else if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				this.task = new StmMobileTask(this, this);
				this.task.execute(this.busStop);
			} else if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_AUTO)) {
				this.task = new AutomaticTask(this, this);
				this.task.execute(this.busStop);
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", provider);
				this.task = new AutomaticTask(this, this); // default Auto
				this.task.execute(this.busStop);
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
		if (this.task != null && this.task.getStatus().equals(AsyncTask.Status.RUNNING)) {
			// stopping the task
			this.task.cancel(true);
			this.task = null;
			setNextStopsCancelled();
		} else {
			refreshNextStops();
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
			findViewById(R.id.next_stops_msg).setVisibility(View.GONE);
			findViewById(R.id.next_stops_msg2).setVisibility(View.GONE);
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
			findViewById(R.id.next_stops_msg2).setVisibility(View.GONE);
			findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
			// show message 1
			TextView message1Tv = (TextView) findViewById(R.id.next_stops_msg);
			message1Tv.setVisibility(View.VISIBLE);
			message1Tv.setText(R.string.next_bus_stop_load_cancelled);
			message1Tv.setVisibility(View.VISIBLE);
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
				MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
				Utils.notifyTheUser(this, defaultMessage);
			}
		} else {
			// show the BIG cancel message
			TextView message1Tv = (TextView) findViewById(R.id.next_stops_msg);
			TextView message2Tv = (TextView) findViewById(R.id.next_stops_msg2);
			// hide loading + message 2 + next stops group
			findViewById(R.id.next_stops_group).setVisibility(View.GONE);
			message2Tv.setVisibility(View.GONE);
			findViewById(R.id.next_stops_loading).setVisibility(View.GONE);
			// set next stop header with source name
			if (hours == null || TextUtils.isEmpty(hours.getSourceName())) {
				((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops));
			} else {
				((TextView) findViewById(R.id.next_stops_string)).setText(getString(R.string.next_bus_stops_and_source, hours.getSourceName()));
			}
			// show message 1
			message1Tv.setVisibility(View.VISIBLE);
			// IF an error occurs during the process DO
			if (hours != null && !TextUtils.isEmpty(hours.getError())) {
				message1Tv.setText(hours.getError());
			} else {
				// IF there is a secondary message from the STM DO
				if (hours != null && !TextUtils.isEmpty(hours.getMessage2())) {
					message1Tv.setText(hours.getMessage2());
					Linkify.addLinks(message1Tv, Linkify.ALL);
					// IF there is also an error message from the STM DO
					if (hours != null && !TextUtils.isEmpty(hours.getMessage())) {
						message2Tv.setVisibility(View.VISIBLE);
						message2Tv.setText(hours.getMessage());
						Linkify.addLinks(message2Tv, Linkify.ALL);
					}
					// ELSE IF there is only an error message from the STM DO
				} else if (hours != null && !TextUtils.isEmpty(hours.getMessage())) {
					message1Tv.setText(hours.getMessage());
					Linkify.addLinks(message1Tv, Linkify.ALL);
					// ELSE
				} else {
					MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
					// DEFAULT MESSAGE > no more bus stop for this bus line
					String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
					message1Tv.setText(defaultMessage);
				}
			}
		}
		setNextStopsNotLoading();
	}

	@Override
	public void onNextStopsProgress(String progress) {
		MyLog.v(TAG, "onNextStopsProgress(%s)", progress);
		// IF the task was cancelled DO
		if (this.task == null || this.task.isCancelled()) {
			// MyLog.d(TAG, "Task cancelled!");
			return; // stop here
		}
		if (!TextUtils.isEmpty(progress)) {
			if (this.hours != null) {
				// notify the user ?
			} else {
				// update the BIG message
				TextView detailMsgTv = (TextView) findViewById(R.id.detail_msg);
				detailMsgTv.setText(progress);
				detailMsgTv.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onNextStopsLoaded(Map<String, BusStopHours> results) {
		MyLog.v(TAG, "onNextStopsLoaded(%s)", results.size());
		// Save loaded next stops to cache
		for (String lineNumber : results.keySet()) {
			BusStopHours busStopHours = results.get(lineNumber);
			if (busStopHours != null && busStopHours.getSHours().size() > 0) {
				saveToCache(this.busStop.getCode(), lineNumber, busStopHours);
			}
		}
		// IF error DO
		BusStopHours result = results.get(this.busStop.getLineNumber());
		if (result == null || result.getSHours().size() <= 0) {
			// process the error
			setNextStopsError(result);
		} else {
			// get the result
			this.hours = result;
			// show the result
			showNewNextStops();
			setNextStopsNotLoading();
		}
	}

	/**
	 * Save the bus stop hours for the line number into the local cache.
	 * @param lineNumber the bus stop line number
	 * @param busStopHours the stop hours
	 */
	private void saveToCache(String stopCode, String lineNumber, BusStopHours busStopHours) {
		Cache newCache = new Cache(Cache.KEY_TYPE_VALUE_BUS_STOP, BusStop.getUID(/* this.busStop.getCode() */stopCode, lineNumber), busStopHours.serialized());
		// remove existing cache for this bus stop
		if (this.busStop != null && lineNumber.equals(this.busStop.getLineNumber())) {
			if (this.cache != null) {
				DataManager.deleteCache(getContentResolver(), this.cache.getId());
			}
			this.cache = newCache;
		} else {
			DataManager.deleteCacheIfExist(getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP,
					BusStop.getUID(/* this.busStop.getCode() */stopCode, lineNumber));
		}
		// save the new value to cache
		DataManager.addCache(getContentResolver(), newCache);
	}

	/**
	 * Update the distance with the latest device location.
	 */
	private void updateDistanceWithNewLocation() {
		Location currentLocation = getLocation();
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", currentLocation);
		TextView distanceTv = (TextView) findViewById(R.id.distance);
		if (currentLocation != null && this.busStop != null && this.busStop.getLat() != null && this.busStop.getLng() != null) {
			distanceTv.setText(Utils.getDistanceStringUsingPref(this, currentLocation.distanceTo(this.busStop.getLocation()), currentLocation.getAccuracy()));
			distanceTv.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Store the device location.
	 */
	private Location location;
	/**
	 * Is the location updates enabled?
	 */
	private boolean locationUpdatesEnabled = false;

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
				SensorUtils.registerCompassListener(this, this);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		MyLog.v(TAG, "onLocationChanged()");
		this.setLocation(location);
		updateDistanceWithNewLocation();
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

	/**
	 * Switch the favorite status.
	 * @param v the view (not used)
	 */
	public void addOrRemoveFavorite(View v) {
		if (this.busStop == null) {
			return;
		}
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(), this.busStop.getLineNumber());
		// IF the favorite exist DO
		if (findFav != null) {
			// delete the favorite
			DataManager.deleteFav(getContentResolver(), findFav.getId());
			Utils.notifyTheUser(this, getString(R.string.favorite_removed));
		} else {
			// add the favorite
			Fav newFav = new Fav();
			newFav.setType(Fav.KEY_TYPE_VALUE_BUS_STOP);
			newFav.setFkId(this.busStop.getCode());
			newFav.setFkId2(this.busLine.getNumber());
			DataManager.addFav(getContentResolver(), newFav);
			Utils.notifyTheUser(this, getString(R.string.favorite_added));
			UserPreferences.savePrefLcl(this, UserPreferences.PREFS_LCL_IS_FAV, true);
		}
		SupportFactory.getInstance(this).backupManagerDataChanged();
		setTheStar(); // TODO is remove useless?
	}

	/**
	 * Show the subway station.
	 * @param v a view (not used)
	 */
	public void showSubwayStation(View v) {
		if (this.busStop == null) {
			return;
		}
		// IF there is a subway station DO
		String subwayStationId = BusStopInfo.this.busStop.getSubwayStationId();
		if (!TextUtils.isEmpty(subwayStationId)) {
			// show subway station info
			Intent intent = new Intent(BusStopInfo.this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_NAME, BusStopInfo.this.busStop.getSubwayStationNameOrNull());
			startActivity(intent);
		}
	}

	/**
	 * Switch to www.stm.info provider.
	 * @param v the view (not used)
	 */
	public void switchToStmInfoProvider(View v) {
		switchProvider(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO);
	}

	/**
	 * Switch to m.stm.info provider.
	 * @param v the view (not used)
	 */
	public void switchToStmMobileProvider(View v) {
		switchProvider(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE);
	}

	/**
	 * Switch to 'Auto' provider.
	 * @param v the view (not used)
	 */
	public void switchToAutoProvider(View v) {
		switchProvider(UserPreferences.PREFS_NEXT_STOP_PROVIDER_AUTO);
	}

	/**
	 * Switch to a new provider.
	 * @param providerPref the new provider (preferences key)
	 */
	private void switchProvider(String providerPref) {
		if (!getProviderFromPref().equals(providerPref)) {
			UserPreferences.savePrefDefault(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER, providerPref);
		}
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

			}).execute(this.busStop.getPlace());
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

		}).execute(this.busStop.getPlace());
	}

	/**
	 * Show m.stm.info page for the current bus stop.
	 * @param v the view (not used).
	 */
	public void showSTMInfoPage(View v) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StmMobileTask.getUrlString(this.busStop.getCode()))));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_stop_info_menu);
	}

	// @Override
	// public boolean onPrepareOptionsMenu(Menu menu) {
	// MyLog.v(TAG, "onPrepareOptionsMenu()");
	// if (super.onPrepareOptionsMenu(menu)) {
	// // PROVIDERs
	// String provider = getProviderFromPref();
	// if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
	// menu.findItem(R.id.provider_stm_mobile).setChecked(true);
	// } else if (provider.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
	// menu.findItem(R.id.provider_stm_info).setChecked(true);
	// } else {
	// menu.findItem(R.id.provider_auto).setChecked(true); // default = Auto
	// }
	// return true;
	// } else {
	// MyLog.w(TAG, "Error in onPrepareOptionsMenu().");
	// return false;
	// }
	// }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stm_mobile:
			showSTMInfoPage(null);
			return true;
		case R.id.map:
			showStopLocationInMaps(null);
			return true;
		case R.id.radar:
			showStopInRadar(null);
			return true;
			// case R.id.provider_stm_info:
			// switchToStmInfoProvider(null);
			// return true;
			// case R.id.provider_stm_mobile:
			// switchToStmMobileProvider(null);
			// return true;
			// case R.id.provider_auto:
			// switchToAutoProvider(null);
			// return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.task != null) {
			this.task.cancel(true);
			this.task = null;
		}
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}
}

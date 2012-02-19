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
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.BusStopHours;
import org.montrealtransit.android.dialog.BusLineSelectDirection;
import org.montrealtransit.android.dialog.NoRadarInstalled;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Cache;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore;
import org.montrealtransit.android.provider.StmStore.BusLine;
import org.montrealtransit.android.provider.StmStore.BusLineDirection;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.services.GeocodingTask;
import org.montrealtransit.android.services.GeocodingTaskListener;
import org.montrealtransit.android.services.NfcListener;
import org.montrealtransit.android.services.nextstop.NextStopListener;
import org.montrealtransit.android.services.nextstop.StmInfoTask;
import org.montrealtransit.android.services.nextstop.StmMobileTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This activity show information about a bus stop.
 * @author Mathieu Méa
 */
public class BusStopInfo extends Activity implements LocationListener, NextStopListener,
        DialogInterface.OnClickListener, OnSharedPreferenceChangeListener, NfcListener {

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
	 * The extra ID for the bus line number.
	 */
	public static final String EXTRA_STOP_LINE_NUMBER = "extra_line_number";
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
	 * The refresh/stop refresh image.
	 */
	private ImageView nextStopsRefreshOrStopRefreshImg;
	/**
	 * The favorite check box.
	 */
	private CheckBox starCb;
	/**
	 * The subway station layout.
	 */
	private RelativeLayout theSubwayStationLayout;
	/**
	 * The loading view.
	 */
	private View loadingView;
	/**
	 * The first message text view.
	 */
	private TextView message1Tv;
	/**
	 * The second message text view.
	 */
	private TextView message2Tv;
	/**
	 * The next stop group view.
	 */
	private View nextStopsGroupView;
	/**
	 * The progress bar view.
	 */
	private View progressBarView;
	/**
	 * The bus stop type image.
	 */
	private ImageView busTypeImg;
	/**
	 * The bus stop place.
	 */
	private TextView busStopPlaceTv;
	/**
	 * The bus stop number.
	 */
	private TextView busLineNumberTv;
	/**
	 * The bus line name.
	 */
	private TextView busLineNameTv;
	/**
	 * The bus line direction.
	 */
	private TextView busLineDirectionTv;
	/**
	 * The bus stop code.
	 */
	private TextView busStopCodeTv;
	/**
	 * The subway station view.
	 */
	private View subwayStationView;
	/**
	 * The subway station name.
	 */
	private TextView subwayStationNameTv;
	/**
	 * The subway station line color image 1.
	 */
	private ImageView subwayStationImg1;
	/**
	 * The subway station line color image 2.
	 */
	private ImageView subwayStationImg2;
	/**
	 * The subway station line color image 3.
	 */
	private ImageView subwayStationImg3;
	/**
	 * The other bus lines view.
	 */
	private View otherBusLinesView;
	/**
	 * The other bus lines layout.
	 */
	private LinearLayout otherBusLinesLayout;
	/**
	 * The next bus stops title.
	 */
	private TextView nextStopStringTv;
	/**
	 * THE next bus stop.
	 */
	private TextView theNextStopTv;
	/**
	 * The second next bus stops.
	 */
	private TextView theSecondNextStopTv;
	/**
	 * The other next bus stops.
	 */
	private TextView otherNextStopsTv;
	/**
	 * The distance text view.
	 */
	private TextView distanceTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.bus_stop_info);
		// find the views
		this.nextStopsRefreshOrStopRefreshImg = (ImageView) findViewById(R.id.next_stops_refresh);
		this.starCb = (CheckBox) findViewById(R.id.star);
		this.distanceTv = (TextView) findViewById(R.id.distance);
		this.theSubwayStationLayout = (RelativeLayout) findViewById(R.id.the_subway_station);
		this.loadingView = findViewById(R.id.next_stops_loading);
		this.message1Tv = (TextView) findViewById(R.id.next_stops_msg);
		this.message2Tv = (TextView) findViewById(R.id.next_stops_msg2);
		this.nextStopsGroupView = findViewById(R.id.next_stops_group);
		this.progressBarView = findViewById(R.id.next_stops_title).findViewById(R.id.progress_bar_next_stop);
		this.busTypeImg = (ImageView) findViewById(R.id.line_type);
		this.busStopPlaceTv = (TextView) findViewById(R.id.bus_stop_place);
		this.busLineNumberTv = (TextView) findViewById(R.id.line_number);
		this.busLineNameTv = (TextView) findViewById(R.id.line_name);
		this.busLineDirectionTv = (TextView) findViewById(R.id.direction_main);
		this.busStopCodeTv = (TextView) findViewById(R.id.stop_code);
		this.subwayStationView = findViewById(R.id.subway_station);
		this.subwayStationNameTv = (TextView) findViewById(R.id.station_name);
		this.subwayStationImg1 = (ImageView) findViewById(R.id.subway_img_1);
		this.subwayStationImg2 = (ImageView) findViewById(R.id.subway_img_2);
		this.subwayStationImg3 = (ImageView) findViewById(R.id.subway_img_3);
		this.otherBusLinesView = findViewById(R.id.other_bus_line);
		this.otherBusLinesLayout = (LinearLayout) findViewById(R.id.other_bus_line_list);
		this.nextStopStringTv = (TextView) findViewById(R.id.next_stops_string);
		this.theNextStopTv = (TextView) findViewById(R.id.the_next_stop);
		this.theSecondNextStopTv = (TextView) findViewById(R.id.the_second_next_stop);
		this.otherNextStopsTv = (TextView) findViewById(R.id.other_stops);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}
		SupportFactory.getInstance(this).registerNfcCallback(this, this, MIME_TYPE);
		SupportFactory.getInstance(this).setOnNdefPushCompleteCallback(this, this);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * onCreate() method only for Android versions older than 1.6.
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		this.nextStopsRefreshOrStopRefreshImg.setOnClickListener(new View.OnClickListener() {
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
		this.starCb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addOrRemoveFavorite(v);
			}
		});
		this.theSubwayStationLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubwayStation(v);
			}
		});
	}

	@Override
	protected void onStop() {
		MyLog.v(TAG, "onStop()");
		if (isShowingBusStopLocation()) {
			LocationUtils.disableLocationUpdates(this, this);
		}
		super.onStop();
	}

	@Override
	protected void onRestart() {
		MyLog.v(TAG, "onRestart()");
		// IF location updates should be enabled DO
		if (isShowingBusStopLocation() && this.locationUpdatesEnabled) {
			// IF there is a valid last know location DO
			if (LocationUtils.getBestLastKnownLocation(this) != null) {
				// set the new distance
				setLocation(LocationUtils.getBestLastKnownLocation(this));
				updateDistanceWithNewLocation();
			}
			// re-enable
			LocationUtils.enableLocationUpdates(this, this);
		}
		super.onRestart();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		MyLog.v(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		super.onResume();
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		AdsUtils.setupAd(this);
		setBusStopFromIntent(getIntent(), null);
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
			String lineNumber;
			if (SupportFactory.getInstance(this).isNfcIntent(intent)) {
				SupportFactory.getInstance(this).processNfcIntent(intent, this);
				return;
			} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				String pathSegment = intent.getData().getPathSegments().get(1);
				stopCode = pathSegment.substring(0, 5);
				lineNumber = pathSegment.substring(5);
			} else {
				stopCode = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_CODE);
				lineNumber = Utils.getSavedStringValue(intent, savedInstanceState, EXTRA_STOP_LINE_NUMBER);
			}
			showNewBusStop(stopCode, lineNumber);
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
				if (tmp != null && tmp.getSHours().size() == 0) {
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
		showNewBusStop(stopCode, lineNumber);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.next_bus_stops));
		builder.setIcon(R.drawable.ic_btn_info_details);
		String source;
		if (this.hours != null) {
			source = this.hours.getSourceName();
		} else {
			if (getProviderFromPref().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				source = StmInfoTask.SOURCE_NAME;
			} else if (getProviderFromPref().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				source = StmMobileTask.SOURCE_NAME;
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", getProviderFromPref());
				source = StmMobileTask.SOURCE_NAME; // default STM mobile
			}
		}
		builder.setMessage(getString(R.string.next_bus_stops_message_and_source, source));
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.setCancelable(true);

		builder.create();
		builder.show();
	}

	/**
	 * @return the bus list "group by" preference.
	 */
	private String getProviderFromPref() {
		return UserPreferences.getPrefDefault(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER,
		        UserPreferences.PREFS_NEXT_STOP_PROVIDER_DEFAULT);
	}

	/**
	 * Set the favorite star (UI).
	 */
	private void setTheStar() {
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(this.getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP,
		        this.busStop.getCode(), this.busStop.getLineNumber());
		this.starCb.setChecked(findFav != null);
	}

	/**
	 * Set the bus stop info basic UI.
	 */
	private void refreshBusStopInfo() {
		MyLog.v(TAG, "refreshBusStopInfo()");
		this.busStopCodeTv.setText(this.busStop.getCode());
		// set bus stop place name
		this.busStopPlaceTv.setText(BusUtils.cleanBusStopPlace(this.busStop.getPlace()));
		// set the favorite icon
		setTheStar();
		// set bus line number
		this.busLineNumberTv.setText(this.busLine.getNumber());
		int color = BusUtils.getBusLineTypeBgColorFromType(this.busLine.getType());
		this.busLineNumberTv.setBackgroundColor(color);
		// set bus line name
		this.busLineNameTv.setText(this.busLine.getName());
		this.busLineNameTv.requestFocus();
		// set listener
		BusLineSelectDirection busLineSelectDirection = new BusLineSelectDirection(this, this.busLine.getNumber());
		this.busLineNumberTv.setOnClickListener(busLineSelectDirection);
		this.busLineNameTv.setOnClickListener(busLineSelectDirection);
		// bus line type
		this.busTypeImg.setImageResource(BusUtils.getBusLineTypeImgFromType(this.busLine.getType()));
		// set bus line direction
		String directionId = this.busStop.getDirectionId();
		BusLineDirection busLineDirection = StmManager.findBusLineDirection(this.getContentResolver(), directionId);
		int busLineDirectionIds = BusUtils.getBusLineSimpleDirection(busLineDirection.getId());
		String directionText = getString(R.string.direction_and_string, getString(busLineDirectionIds));
		this.busLineDirectionTv.setText(directionText);
	}

	/**
	 * Set the bus stop subway station info.
	 */
	private void refreshSubwayStationInfo() {
		MyLog.v(TAG, "refreshSubwayStationInfo()");
		if (!TextUtils.isEmpty(this.busStop.getSubwayStationId())) {
			this.subwayStationView.setVisibility(View.VISIBLE);
			this.theSubwayStationLayout.setVisibility(View.VISIBLE);
			StmStore.SubwayStation subwayStation = StmManager.findSubwayStation(getContentResolver(),
			        this.busStop.getSubwayStationId());
			// set subway station name
			this.subwayStationNameTv.setText(subwayStation.getName());
			// set subway station line(s) color(s)
			List<SubwayLine> subwayLines = StmManager.findSubwayStationLinesList(getContentResolver(),
			        subwayStation.getId());
			if (subwayLines != null && subwayLines.size() > 0) {
				int subwayLineImg0 = SubwayUtils.getSubwayLineImgId(subwayLines.get(0).getNumber());
				this.subwayStationImg1.setImageResource(subwayLineImg0);
				if (subwayLines.size() > 1) {
					int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(subwayLines.get(1).getNumber());
					this.subwayStationImg2.setImageResource(subwayLineImg1);
					if (subwayLines.size() > 2) {
						int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(subwayLines.get(2).getNumber());
						this.subwayStationImg3.setImageResource(subwayLineImg2);
					} else {
						this.subwayStationImg3.setVisibility(View.GONE);
					}
				} else {
					this.subwayStationImg2.setVisibility(View.GONE);
					this.subwayStationImg3.setVisibility(View.GONE);
				}
			} else {
				this.subwayStationImg1.setVisibility(View.GONE);
				this.subwayStationImg2.setVisibility(View.GONE);
				this.subwayStationImg3.setVisibility(View.GONE);
			}
		} else {
			this.subwayStationView.setVisibility(View.GONE);
			this.theSubwayStationLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Set the other bus lines using this bus stop.
	 */
	private void refreshOtherBusLinesInfo() {
		MyLog.v(TAG, "refreshOtherBusLinesInfo()");
		List<BusLine> allBusLines = StmManager.findBusStopLinesList(this.getContentResolver(), this.busStop.getCode());
		// remove all bus lines with the same line number
		ListIterator<BusLine> it = allBusLines.listIterator();
		Set<String> buslinesNumber = new HashSet<String>();
		while (it.hasNext()) {
			BusLine busLine = it.next();
			// IF the bus line number is the same as the current bus stop DO
			if (busLine.getNumber().equals(this.busLine.getNumber())) {
				it.remove();
				// ELSE IF the bus line number is already in the list DO
			} else if (buslinesNumber.contains(busLine.getNumber())) {
				it.remove();
			} else {
				buslinesNumber.add(busLine.getNumber());
			}
		}
		this.otherBusLinesLayout.removeAllViews();
		if (allBusLines.size() > 0) {
			this.otherBusLinesView.setVisibility(View.VISIBLE);
			this.otherBusLinesLayout.setVisibility(View.VISIBLE);
			for (StmStore.BusLine busLine : allBusLines) {
				// the view
				View view = getLayoutInflater().inflate(R.layout.bus_stop_info_bus_line_list_item, null);
				TextView lineNumberTv = (TextView) view.findViewById(R.id.line_number);
				final String lineNumber = busLine.getNumber();
				lineNumberTv.setText(lineNumber);
				int color = BusUtils.getBusLineTypeBgColorFromType(busLine.getType());
				lineNumberTv.setBackgroundColor(color);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MyLog.v(TAG, "onClick()");
						showNewBusStop(BusStopInfo.this.busStop.getCode(), lineNumber);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						new BusLineSelectDirection(BusStopInfo.this, lineNumber).showDialog();
						return true;
					}
				});
				this.otherBusLinesLayout.addView(view);
			}
		} else {
			this.otherBusLinesView.setVisibility(View.GONE);
			this.otherBusLinesLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Show the new bus stop information.
	 * @param newStopCode the new bus stop code MANDATORY
	 * @param newLineNumber the new bus line number (optional)
	 */
	public void showNewBusStop(String newStopCode, String newLineNumber) {
		MyLog.v(TAG, "showNewBusStop(%s, %s)", newStopCode, newLineNumber);
		boolean newBusStopCode = this.busStop == null || !this.busStop.getCode().equals(newStopCode)
		        || !this.busStop.getLineNumber().equals(newLineNumber);
		boolean newBusStopCodeAndLineNumber = newBusStopCode || this.busLine == null
		        || !this.busLine.getNumber().equals(newLineNumber);
		// MyLog.d(TAG, "Is there bus stop : " + (this.busStop != null));
		// MyLog.d(TAG, "Is there bus line : " + (this.busLine != null));
		// MyLog.d(TAG, "New bus stop code ? " + newBusStopCode);
		// MyLog.d(TAG, "New bus stop code and line number ? " + newBusStopCodeAndLineNumber);
		// IF no bus stop OR new bus stop DO
		if (newBusStopCode) {
			// MyLog.v(TAG, "New bus stop '%s' line '%s'.", newStopCode, newLineNumber);
			this.busStopCodeTv.setText(newStopCode);
			checkStopCode(newStopCode);
		}
		if (newLineNumber == null) {
			newLineNumber = findBusLineNumnberFromStopCode(newStopCode);
		}
		if (newStopCode != null && newLineNumber != null) {
			if (isNewBusStop(newStopCode, newLineNumber)) {
				// MyLog.d(TAG, "starting NEW activity...");
				// force starting a new instance of the activity
				Intent newIntent = new Intent(this, BusStopInfo.class);
				// newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // ??
				newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				// newIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
				newIntent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, newLineNumber);
				newIntent.putExtra(BusStopInfo.EXTRA_STOP_CODE, newStopCode);
				startActivity(newIntent);
				return;
			}
			// MyLog.d(TAG, "Using the SAME activity");
			// load bus stop from DB if necessary
			if (newBusStopCode) {
				this.busStop = StmManager.findBusLineStop(this.getContentResolver(), newStopCode, newLineNumber);
			}
			// load bus line from DB if necessary
			if (newBusStopCodeAndLineNumber) {
				this.busLine = StmManager.findBusLine(this.getContentResolver(), this.busStop.getLineNumber());
			}
			// IF new bus stop code DO
			if (newBusStopCode) {
				findBusStopLocation(); // start the asynchronous task ASAP
				this.hours = null;
			}
			// IF new bus stop code and line number DO
			if (newBusStopCodeAndLineNumber) {
				// clear the cache for the new bus stop
				this.cache = null;
			}
			if (this.task != null) {
				this.task.cancel(true);
				this.task = null;
			}
			setUpUI();
		}
		// }
	}

	/**
	 * @param newStopCode the bus stop code
	 * @param newLineNumber the bus line number
	 * @return false if the {@link #getIntent()} doesn't match the current bus stop code
	 */
	private boolean isNewBusStop(String newStopCode, String newLineNumber) {
		MyLog.v(TAG, "isNewBusStop(%s,%s)", newStopCode, newLineNumber);
		if (this.busStop == null || this.busLine == null) {
			// MyLog.d(TAG, "No existing bus stop/line => NOT NEW!");
			return false;
		}
		if (!this.busStop.getCode().equals(newStopCode)) {
			// MyLog.d(TAG, "New bus stop code => NEW!");
			return true;
		}
		// MyLog.d(TAG, "Same bus stop code & bus line number => NOT NEW!");
		return false;
	}

	private String findBusLineNumnberFromStopCode(String newStopCode) {
		// get the bus lines for this bus stop
		List<BusLine> busLines = StmManager.findBusStopLinesList(getContentResolver(), newStopCode);
		if (busLines == null) {
			// no bus line found
			// TODO handle unknown bus stop code
			String message = getString(R.string.wrong_stop_code_and_code, newStopCode);
			Utils.notifyTheUser(this, message);
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
	 * Check if the bus stop is in the current application DB.
	 * @param stopCode the bus stop code
	 */
	private void checkStopCode(String stopCode) {
		if (StmManager.findBusStop(this.getContentResolver(), stopCode) == null) {
			showAlertDialog(stopCode);
		}
	}

	/**
	 * Show the dialog about the unknown bus stop id.
	 * @param wrongStopCode
	 */
	private void showAlertDialog(String wrongStopCode) {
		MyLog.v(TAG, "showAlertDialog()");
		MyLog.w(TAG, "Wrong bus stop code '%s'?", wrongStopCode);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.warning);
		String message = getString(R.string.wrong_stop_code_and_code, wrongStopCode) + "\n"
		        + getString(R.string.wrong_stop_code_internet);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.yes, this);
		builder.setNegativeButton(R.string.no, this);
		builder.create().show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(%s)", which);
		if (which == -2) {
			dialog.dismiss();// CANCEL
			this.finish(); // close the activity.
		} else {
			// try to load the next stop from the web.
			refreshNextStops();
		}
	}

	/**
	 * @return the bus stop location preference.
	 */
	private boolean isShowingBusStopLocation() {
		return UserPreferences.getPrefDefault(this, UserPreferences.PREFS_BUS_STOP_LOCATION,
		        UserPreferences.PREFS_BUS_STOP_LOCATION_DEFAULT);
	}

	/**
	 * Setup all the UI (based on the bus stop).
	 */
	private void setUpUI() {
		showNextBusStops();
		refreshBusStopInfo();
		refreshOtherBusLinesInfo();
		refreshSubwayStationInfo();
		if (isShowingBusStopLocation()) {
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
	}

	/**
	 * Find the bus stop location.
	 */
	private void findBusStopLocation() {
		if (isShowingBusStopLocation()) {
			new GeocodingTask(this, 1, false, new GeocodingTaskListener() {
				@Override
				public void processLocation(List<Address> addresses) {
					MyLog.v(TAG, "processLocation()");
					if (addresses != null && addresses.size() > 0 && addresses.get(0) != null) {
						BusStopInfo.this.busStop.setLat(addresses.get(0).getLatitude());
						BusStopInfo.this.busStop.setLng(addresses.get(0).getLongitude());
						updateDistanceWithNewLocation(); // force update
					} else if (!TextUtils.isEmpty(BusStopInfo.this.busStop.getSubwayStationId())
					        && BusStopInfo.this.busStop.getSubwayStationLatOrNull() != null
					        && BusStopInfo.this.busStop.getSubwayStationLngOrNull() != null) {
						BusStopInfo.this.busStop.setLat(BusStopInfo.this.busStop.getSubwayStationLatOrNull());
						BusStopInfo.this.busStop.setLng(BusStopInfo.this.busStop.getSubwayStationLngOrNull());
						updateDistanceWithNewLocation(); // force update
					}
				}
			}).execute(this.busStop.getPlace());
		} else {
			// clearing bus stop location
			this.busStop.setLat(null);
			this.busStop.setLng(null);
			updateDistanceWithNewLocation(); // force update
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
				this.cache = DataManager.findCache(getContentResolver(), Cache.KEY_TYPE_VALUE_BUS_STOP,
				        busStop.getUID());
			}
			// compute the too old date
			int tooOld = (int) (System.currentTimeMillis() / 1000) - CACHE_TOO_OLD_IN_SEC;
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
			// MyLog.d(TAG, "next stops: %s", this.hours.serialized());
			// set next stop header with source name
			this.nextStopStringTv.setText(getString(R.string.next_bus_stops_and_source, this.hours.getSourceName()));
			// IF there next stops found DO
			if (this.hours.getSHours().size() > 0) {
				// hide loading + messages
				this.loadingView.setVisibility(View.GONE);
				this.message1Tv.setVisibility(View.GONE);
				this.message2Tv.setVisibility(View.GONE);
				// show next bus stop group
				this.nextStopsGroupView.setVisibility(View.VISIBLE);
				List<String> fHours = this.hours.getFormattedHours(this);
				// clear the last values
				this.theSecondNextStopTv.setText(null);
				this.otherNextStopsTv.setText(null);
				// show the next bus stops
				this.theNextStopTv.setText(fHours.get(0));
				if (fHours.size() > 1) {
					this.theSecondNextStopTv.setText(fHours.get(1));
					if (fHours.size() > 2) {
						String hoursS = "";
						for (int i = 2; i < fHours.size(); i++) {
							if (hoursS.length() > 0) {
								hoursS += " ";
							}
							hoursS += fHours.get(i);
						}
						this.otherNextStopsTv.setText(hoursS);
					}
				}
				// show messages
				if (!TextUtils.isEmpty(this.hours.getMessage())) {
					this.message1Tv.setVisibility(View.VISIBLE);
					this.message1Tv.setText(this.hours.getMessage());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
				}
				if (!TextUtils.isEmpty(this.hours.getMessage2())) {
					this.message2Tv.setVisibility(View.VISIBLE);
					this.message2Tv.setText(this.hours.getMessage2());
					Linkify.addLinks(this.message2Tv, Linkify.ALL);
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
			if (getProviderFromPref().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				this.task = new StmInfoTask(this, this);
				this.task.execute(this.busStop);
			} else if (getProviderFromPref().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_MOBILE)) {
				this.task = new StmMobileTask(this, this);
				this.task.execute(this.busStop);
			} else {
				MyLog.w(TAG, "Unknow next stop provider '%s'", getProviderFromPref());
				this.task = new StmMobileTask(this, this); // default STM mobile
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
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message1Tv.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.VISIBLE);
			// } else { // notify the user ?
		}
		// show stop icon instead of refresh
		this.nextStopsRefreshOrStopRefreshImg.setVisibility(View.INVISIBLE);
		// show progress bar
		this.progressBarView.setVisibility(View.VISIBLE);

	}

	/**
	 * Set the next stop view as not loading.
	 */
	private void setNextStopsNotLoading() {
		MyLog.v(TAG, "setNextStopsNotLoading()");
		// show refresh icon instead of loading
		this.nextStopsRefreshOrStopRefreshImg.setVisibility(View.VISIBLE);
		// hide progress bar
		this.progressBarView.setVisibility(View.INVISIBLE);
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
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.GONE);
			// show message 1
			this.message1Tv.setVisibility(View.VISIBLE);
			this.message1Tv.setText(R.string.next_bus_stop_load_cancelled);
			this.message1Tv.setVisibility(View.VISIBLE);
		}
		setNextStopsNotLoading();
	}

	/**
	 * Set the next stops view as error.
	 * @param hours the error
	 */
	private void setNextStopsError(BusStopHours hours) {
		MyLog.v(TAG, "setNextStopsError()");
		// IF there are hours to show DO
		if (this.hours != null) {
			// notify the user but keep showing the old stations
			if (!TextUtils.isEmpty(hours.getError())) {
				Utils.notifyTheUser(this, hours.getError());
			} else if (!TextUtils.isEmpty(hours.getMessage())) {
				Utils.notifyTheUser(this, hours.getMessage());
			} else if (!TextUtils.isEmpty(hours.getMessage2())) {
				Utils.notifyTheUser(this, hours.getMessage2());
			} else {
				MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
				// DEFAULT MESSAGE > no more bus stop for this bus line
				String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
				Utils.notifyTheUser(this, defaultMessage);
			}
		} else {
			// show the BIG cancel message
			// hide loading + message 2 + next stops group
			this.nextStopsGroupView.setVisibility(View.GONE);
			this.message2Tv.setVisibility(View.GONE);
			this.loadingView.setVisibility(View.GONE);
			// set next stop header with source name
			this.nextStopStringTv.setText(getString(R.string.next_bus_stops_and_source, hours.getSourceName()));
			// show message 1
			this.message1Tv.setVisibility(View.VISIBLE);
			// IF an error occurs during the process DO
			if (!TextUtils.isEmpty(hours.getError())) {
				this.message1Tv.setText(hours.getError());
			} else {
				// IF there is a secondary message from the STM DO
				if (!TextUtils.isEmpty(hours.getMessage2())) {
					this.message1Tv.setText(hours.getMessage2());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
					// IF there is also an error message from the STM DO
					if (!TextUtils.isEmpty(hours.getMessage())) {
						this.message2Tv.setVisibility(View.VISIBLE);
						Linkify.addLinks(this.message2Tv, Linkify.ALL);
					}
					// ELSE IF there is only an error message from the STM DO
				} else if (!TextUtils.isEmpty(hours.getMessage())) {
					this.message1Tv.setText(hours.getMessage());
					Linkify.addLinks(this.message1Tv, Linkify.ALL);
					// ELSE
				} else {
					MyLog.w(TAG, "no next stop or message or error for %s %s!", busStop.getCode(), busLine.getNumber());
					// DEFAULT MESSAGE > no more bus stop for this bus line
					String defaultMessage = getString(R.string.no_more_stops_for_this_bus_line, busLine.getNumber());
					this.message1Tv.setText(defaultMessage);
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
				TextView detailMsgTv = (TextView) this.loadingView.findViewById(R.id.detail_msg);
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
		Cache newCache = new Cache(Cache.KEY_TYPE_VALUE_BUS_STOP, BusStop.getUID(/* this.busStop.getCode() */stopCode,
		        lineNumber), busStopHours.serialized());
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
		MyLog.v(TAG, "updateDistanceWithNewLocation(%s)", getLocation());
		if (!isShowingBusStopLocation()) {
			// clearing
			this.distanceTv.setVisibility(View.GONE);
			this.distanceTv.setText(null);
			return;
		}
		if (getLocation() != null && this.busStop != null && this.busStop.getLat() != null
		        && this.busStop.getLng() != null) {
			// distance & accuracy
			Location busStopLocation = LocationUtils.getNewLocation(this.busStop.getLat(), this.busStop.getLng());
			float distanceInMeters = getLocation().distanceTo(busStopLocation);
			float accuracyInMeters = getLocation().getAccuracy();
			String distanceString = Utils.getDistanceString(this, distanceInMeters, accuracyInMeters);
			this.distanceTv.setVisibility(View.VISIBLE);
			this.distanceTv.setText(distanceString);
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
		if (isShowingBusStopLocation() && this.location == null) {
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
		if (isShowingBusStopLocation() && newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(this.location, newLocation)) {
				this.location = newLocation;
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
		// try to find the existing favorite
		Fav findFav = DataManager.findFav(getContentResolver(), Fav.KEY_TYPE_VALUE_BUS_STOP, this.busStop.getCode(),
		        this.busStop.getLineNumber());
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
		// IF there is a subway station DO
		String subwayStationId = BusStopInfo.this.busStop.getSubwayStationId();
		if (!TextUtils.isEmpty(subwayStationId)) {
			// show subway station info
			Intent intent = new Intent(BusStopInfo.this, SubwayStationInfo.class);
			intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, subwayStationId);
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
	 * Switch to a new provider.
	 * @param providerPref the new provider (preferences key)
	 */
	private void switchProvider(String providerPref) {
		if (!getProviderFromPref().equals(providerPref)) {
			UserPreferences.savePrefDefault(this, UserPreferences.PREFS_NEXT_STOP_PROVIDER, providerPref);
			// reloadNextBusStops();
		}
	}

	/**
	 * Show the bus stop in radar-enabled app.
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
		String url = StmMobileTask.getUrlString(this.busStop.getCode());
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return MenuUtils.inflateMenu(this, menu, R.menu.bus_stop_info_menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MyLog.v(TAG, "onPrepareOptionsMenu()");
		if (super.onPrepareOptionsMenu(menu)) {
			// PROVIDERs
			if (getProviderFromPref().equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER_STM_INFO)) {
				menu.findItem(R.id.provider_stm_info).setChecked(true);
			} else {
				menu.findItem(R.id.provider_stm_mobile).setChecked(true);
			}
			return true;
		} else {
			MyLog.w(TAG, "Error in onPrepareOptionsMenu().");
			return false;
		}
	}

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
		case R.id.provider_stm_info:
			switchToStmInfoProvider(null);
			return true;
		case R.id.provider_stm_mobile:
			switchToStmMobileProvider(null);
			return true;
		}
		return MenuUtils.handleCommonMenuActions(this, item);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(UserPreferences.PREFS_NEXT_STOP_PROVIDER)) {
			// TODO reload if error?
		} else if (key.equals(UserPreferences.PREFS_BUS_STOP_LOCATION)) {
			findBusStopLocation();
		}
	}

	@Override
	protected void onDestroy() {
		MyLog.v(TAG, "onDestroy()");
		if (this.task != null) {
			this.task.cancel(true);
			this.task = null;
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		AdsUtils.destroyAd(this);
		super.onDestroy();
	}
}

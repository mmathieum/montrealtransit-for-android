package org.montrealtransit.android.activity;

import java.util.List;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.BusUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.SubwayUtils;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore;
import org.montrealtransit.android.provider.DataStore.Fav;
import org.montrealtransit.android.provider.StmManager;
import org.montrealtransit.android.provider.StmStore.BusStop;
import org.montrealtransit.android.provider.StmStore.SubwayLine;
import org.montrealtransit.android.provider.StmStore.SubwayStation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This activity list the favorite bus stops.
 * @author Mathieu Méa
 */
public class FavListTab extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = FavListTab.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/FavList";

	/**
	 * The favorite subway stations list.
	 */
	private List<DataStore.Fav> lastSubwayStationFavList;

	/**
	 * The favorite favorite bus stops list.
	 */
	private List<DataStore.Fav> lastBusStopFavList;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.fav_list_tab);
		refreshAll();
	}

	/**
	 * Refresh all the UI.
	 */
	private void refreshAll() {
		refreshBusStops();
		refreshSubwayStations();
	}

	/**
	 * Refresh the favorite bus stops UI.
	 */
	private void refreshBusStops() {
		MyLog.v(TAG, "refreshBusStops()");
		List<DataStore.Fav> busStopFavList = DataManager.findFavsByTypeList(getContentResolver(),
		        DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP);
		if (this.lastBusStopFavList == null || this.lastBusStopFavList.size() != busStopFavList.size()) {
			LinearLayout busStopsLayout = (LinearLayout) findViewById(R.id.bus_stops_list);
			busStopsLayout.removeAllViews();
			// IF there is one or more favorite bus stops DO
			if (busStopFavList != null && busStopFavList.size() > 0) {
				List<BusStop> busStops = StmManager.findBusStopsExtendedList(this.getContentResolver(),
				        Utils.extractBusStopIDsFromFavList(busStopFavList));
				// FOR EACH bus stop DO
				for (final BusStop busStop : busStops) {
					// list view divider
					if (busStopsLayout.getChildCount() > 0) {
						busStopsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
					}
					// create view
					View view = getLayoutInflater().inflate(R.layout.fav_list_tab_bus_stop_item, null);
					// bus stop code
					((TextView) view.findViewById(R.id.stop_code)).setText(busStop.getCode());
					// bus stop place
					String busStopPlace = BusUtils.cleanBusStopPlace(busStop.getPlace());
					((TextView) view.findViewById(R.id.label)).setText(busStopPlace);
					// bus stop line number
					((TextView) view.findViewById(R.id.line_number)).setText(busStop.getLineNumber());
					// bus stop line name
					((TextView) view.findViewById(R.id.line_name)).setText(busStop.getLineNameOrNull());
					// bus stop line direction
					Integer busLineDirection = BusUtils.getBusLineDirectionStringIdFromId(
					        busStop.getSimpleDirectionId()).get(0);
					((TextView) view.findViewById(R.id.line_direction)).setText(busLineDirection);
					// add click listener
					view.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							MyLog.v(TAG, "onClick(%s)", v.getId());
							Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
							intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
							intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
							startActivity(intent);
						}
					});
					// add context menu
					view.setOnLongClickListener(new View.OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							MyLog.v(TAG, "onLongClick(%s)", v.getId());
							AlertDialog.Builder builder = new AlertDialog.Builder(FavListTab.this);
							builder.setTitle(getString(R.string.bus_stop_and_line_short, busStop.getCode(),
							        busStop.getLineNumber()));
							CharSequence[] items = { getString(R.string.view_bus_stop), getString(R.string.remove_fav) };
							builder.setItems(items, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									MyLog.v(TAG, "onClick(%s)", item);
									switch (item) {
									case VIEW_CONTEXT_MENU_INDEX:
										Intent intent = new Intent(FavListTab.this, BusStopInfo.class);
										intent.putExtra(BusStopInfo.EXTRA_STOP_LINE_NUMBER, busStop.getLineNumber());
										intent.putExtra(BusStopInfo.EXTRA_STOP_CODE, busStop.getCode());
										startActivity(intent);
										break;
									case DELETE_CONTEXT_MENU_INDEX:
										// find the favorite to delete
										Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
										        DataStore.Fav.KEY_TYPE_VALUE_BUS_STOP, busStop.getCode(),
										        busStop.getLineNumber());
										// delete the favorite
										/* boolean status = */
										DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
										// MyLog.d(TAG, "delete fav: " + status);
										// refresh the UI
										FavListTab.this.lastBusStopFavList = null;
										refreshBusStops();
										break;
									default:
										break;
									}
								}
							});
							AlertDialog alert = builder.create();
							alert.show();
							return true;
						}
					});
					busStopsLayout.addView(view);
				}
			} else {
				// show the noFav message
				TextView noFavTv = new TextView(this);
				noFavTv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				noFavTv.setTextAppearance(this, android.R.attr.textAppearanceMedium);
				noFavTv.setText(R.string.no_fav_bus_stop_message);
				busStopsLayout.addView(noFavTv);
			}
		}
	}

	/**
	 * Refresh the favorite subway stations UI.
	 */
	private void refreshSubwayStations() {
		List<DataStore.Fav> subwayFavList = DataManager.findFavsByTypeList(getContentResolver(),
		        DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION);
		if (this.lastSubwayStationFavList == null || this.lastSubwayStationFavList.size() != subwayFavList.size()) {
			LinearLayout subwayStationsLayout = (LinearLayout) findViewById(R.id.subway_stations_list);
			subwayStationsLayout.removeAllViews();
			// IF there is one or more bus stops DO
			if (subwayFavList != null && subwayFavList.size() > 0) {
				// FOR EACH favorite subway DO
				for (Fav subwayFav : subwayFavList) {
					final SubwayStation station = StmManager.findSubwayStation(getContentResolver(), subwayFav.getFkId());
					if (station != null) {
						List<SubwayLine> otherLinesId = StmManager.findSubwayStationLinesList(getContentResolver(),
						        station.getId());
						// list view divider
						if (subwayStationsLayout.getChildCount() > 0) {
							subwayStationsLayout.addView(getLayoutInflater().inflate(R.layout.list_view_divider, null));
						}
						// create view
						View view = getLayoutInflater().inflate(R.layout.fav_list_tab_subway_station_item, null);
						// subway station name
						((TextView) view.findViewById(R.id.station_name)).setText(station.getName());
						// station lines color
						if (otherLinesId != null && otherLinesId.size() > 0) {
							int subwayLineImg1 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(0).getNumber());
							((ImageView) view.findViewById(R.id.subway_img_1)).setImageResource(subwayLineImg1);
							if (otherLinesId.size() > 1) {
								int subwayLineImg2 = SubwayUtils.getSubwayLineImgId(otherLinesId.get(1).getNumber());
								((ImageView) view.findViewById(R.id.subway_img_2)).setImageResource(subwayLineImg2);
								if (otherLinesId.size() > 2) {
									int subwayLineImg3 = SubwayUtils
									        .getSubwayLineImgId(otherLinesId.get(2).getNumber());
									((ImageView) view.findViewById(R.id.subway_img_3)).setImageResource(subwayLineImg3);
								} else {
									view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
								}
							} else {
								view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
								view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
							}
						} else {
							view.findViewById(R.id.subway_img_1).setVisibility(View.GONE);
							view.findViewById(R.id.subway_img_2).setVisibility(View.GONE);
							view.findViewById(R.id.subway_img_3).setVisibility(View.GONE);
						}
						// add click listener
						view.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								MyLog.v(TAG, "onClick(%s)", v.getId());
								Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
								intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, station.getId());
								startActivity(intent);
							}
						});
						// add context menu
						view.setOnLongClickListener(new View.OnLongClickListener() {
							@Override
							public boolean onLongClick(View v) {
								MyLog.v(TAG, "onLongClick(%s)", v.getId());
								AlertDialog.Builder builder = new AlertDialog.Builder(FavListTab.this);
								builder.setTitle(getString(R.string.subway_station_with_name_short, station.getName()));
								CharSequence[] items = { getString(R.string.view_subway_station),
								        getString(R.string.remove_fav) };
								builder.setItems(items, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int item) {
										MyLog.v(TAG, "onClick(%s)", item);
										switch (item) {
										case VIEW_CONTEXT_MENU_INDEX:
											Intent intent = new Intent(FavListTab.this, SubwayStationInfo.class);
											intent.putExtra(SubwayStationInfo.EXTRA_STATION_ID, station.getId());
											startActivity(intent);
											break;
										case DELETE_CONTEXT_MENU_INDEX:
											// find the favorite to delete
											Fav findFav = DataManager.findFav(FavListTab.this.getContentResolver(),
											        DataStore.Fav.KEY_TYPE_VALUE_SUBWAY_STATION, station.getId(), null);
											// delete the favorite
											/* boolean status = */
											DataManager.deleteFav(FavListTab.this.getContentResolver(), findFav.getId());
											// MyLog.d(TAG, "delete fav: " + status);
											// refresh the UI
											FavListTab.this.lastSubwayStationFavList = null;
											refreshSubwayStations();
											break;
										default:
											break;
										}
									}
								});
								AlertDialog alert = builder.create();
								alert.show();
								return true;
							}
						});
						subwayStationsLayout.addView(view);
					} else {
						MyLog.w(TAG, "Can't find the favorite subway station (ID:%s)", subwayFav.getFkId());
					}
				}
			} else {
				// show the noFav message
				TextView noFavTv = new TextView(this);
				noFavTv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				noFavTv.setTextAppearance(this, android.R.attr.textAppearanceMedium);
				noFavTv.setText(R.string.no_fav_subway_station_message);
				subwayStationsLayout.addView(noFavTv);
			}
		}
	}

	/**
	 * Context menu to view the favorite.
	 */
	private static final int VIEW_CONTEXT_MENU_INDEX = 0;
	/**
	 * Context menu to delete the favorite.
	 */
	private static final int DELETE_CONTEXT_MENU_INDEX = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		refreshAll();
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
		super.onResume();
	}

	/**
	 * The menu used to show the search UI.
	 */
	private static final int MENU_SEARCH = Menu.FIRST;
	/**
	 * The menu used to show the user preferences.
	 */
	private static final int MENU_PREFERENCES = Menu.FIRST + 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuSearch = menu.add(0, MENU_SEARCH, Menu.NONE, R.string.menu_search);
		menuSearch.setIcon(android.R.drawable.ic_menu_search);
		MenuItem menuPref = menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
		menuPref.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SEARCH:
			return this.onSearchRequested();
		case MENU_PREFERENCES:
			startActivity(new Intent(this, UserPreferences.class));
			break;
		default:
			MyLog.d(TAG, "Unknown option menu action: %s.", item.getItemId());
		}
		return true;
	}
}

package org.montrealtransit.android.provider;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class contains all the info about the data return by the {@link StmProvider}
 * @author Mathieu Méa
 */
public class StmStore {

	/**
	 * The log tag
	 */
	private static final String TAG = StmStore.class.getSimpleName();
	/**
	 * The provider authority.
	 */
	public static final String AUTHORITY = StmProvider.AUTHORITY;

	/**
	 * Represent a bus line.
	 * @author Mathieu Méa
	 */
	public static class BusLine implements BaseColumns, BusLinesColumns, BusStopsColumns {
		/**
		 * The content URI for bus line.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/buslines");
		/**
		 * The default sort order for displaying the bus lines.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_BUS_LINES_K_NUMBER + " ASC";
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of bus lines.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.buslines";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single bus line.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.buslines";

		/**
		 * The different bus line types values.
		 */
		public static final String LINE_TYPE_REGULAR_SERVICE = StmDbHelper.BUS_LINE_TYPE_REGULAR_SERVICE;
		public static final String LINE_TYPE_RUSH_HOUR_SERVICE = StmDbHelper.BUS_LINE_TYPE_RUSH_HOUR_SERVICE;
		public static final String LINE_TYPE_NIGHT_SERVICE = StmDbHelper.BUS_LINE_TYPE_NIGHT_SERVICE;
		public static final String LINE_TYPE_METROBUS_SERVICE = StmDbHelper.BUS_LINE_TYPE_METROBUS_SERVICE;
		public static final String LINE_TYPE_TRAINBUS = StmDbHelper.BUS_LINE_TYPE_TRAINBUS;
		public static final String LINE_TYPE_EXPRESS_SERVICE = StmDbHelper.BUS_LINE_TYPE_EXPRESS_SERVICE;
		public static final String LINE_TYPE_RESERVED_LANE_SERVICE = StmDbHelper.BUS_LINE_TYPE_RESERVED_LANE_SERVICE;

		/**
		 * The bus line number.
		 */
		private String number;
		/**
		 * The bus line name.
		 */
		private String name;
		/**
		 * The bus lines hours.
		 */
		private String hours;
		/**
		 * The bus line type.
		 */
		private String type;

		/**
		 * @param c the cursor containing bus line entries.
		 * @return a bus line object from the current cursor position
		 */
		public static BusLine fromCursor(Cursor c) {
			final BusLine busLine = new BusLine();
			busLine.number = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_NUMBER));
			busLine.name = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_NAME));
			busLine.hours = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_HOURS));
			busLine.type = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_TYPE));
			return busLine;
		}

		/**
		 * @param number the new bus line number
		 */
		public void setNumber(String number) {
			this.number = number;
		}

		/**
		 * @return the bus line number.
		 */
		public String getNumber() {
			return number;
		}

		/**
		 * @param name the new line name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the line name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param hours the new hours
		 */
		public void setHours(String hours) {
			this.hours = hours;
		}

		/**
		 * @return the line hours
		 */
		public String getHours() {
			return hours;
		}

		/**
		 * @param type the line type
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * @return
		 */
		public String getType() {
			return type;
		}

		/**
		 * A sub directory of a single bus line that contains all of their directions.
		 */
		public static final class BusLineDirections implements BaseColumns, BusLineDirectionsColumns, BusLinesColumns {
			private BusLineDirections() {
			}

			public static final String CONTENT_DIRECTORY = "buslinedirections";
			public static final String DEFAULT_SORT_ORDER = BusLineDirectionsColumns.DIRECTION_ID + " ASC";

			/**
			 * A sub directory of a single bus line that contains all of their stops.
			 */
			public static final class BusStops implements BaseColumns, BusStopsColumns, BusLinesColumns {
				private BusStops() {
				}

				public static final String CONTENT_DIRECTORY = "busstops";
				public static final String DEFAULT_SORT_ORDER = BusStopsColumns.STOPS_ORDER + " ASC";
			}
		}

		/**
		 * A sub directory of a single bus line that contains all of their stops.
		 */
		public static final class BusStops implements BaseColumns, BusStopsColumns, BusLinesColumns {
			private BusStops() {
			}

			public static final String CONTENT_DIRECTORY = "busstops";
			public static final String DEFAULT_SORT_ORDER = BusStopsColumns.STOPS_ORDER + " ASC";
		}

	}

	/**
	 * The bus line columns
	 * @author Mathieu Méa
	 */
	public interface BusLinesColumns {
		public static final String LINE_NUMBER = StmDbHelper.T_BUS_LINES_K_NUMBER;
		public static final String LINE_NAME = StmDbHelper.T_BUS_LINES_K_NAME;
		public static final String LINE_HOURS = StmDbHelper.T_BUS_LINES_K_HOURS;
		public static final String LINE_TYPE = StmDbHelper.T_BUS_LINES_K_TYPE;
	}

	/**
	 * Represent a bus line direction.
	 * @author Mathieu Méa
	 */
	public static class BusLineDirection implements BaseColumns, BusLineDirectionsColumns {
		/**
		 * The content URI for bus line directions.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/buslinedirections");
		/**
		 * The default sort order for bus line directions.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_BUS_LINES_K_NUMBER + " ASC";
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of bus line directions.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.buslinedirections";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single bus line direction.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.buslinedirections";

		/**
		 * The line direction ID.
		 */
		private String id;
		/**
		 * The bus line ID.
		 */
		private String lineId;
		/**
		 * The line direction name.
		 */
		private String name;

		/**
		 * @param c the cursor
		 * @return a bus line direction object.
		 */
		public static BusLineDirection fromCursor(Cursor c) {
			final BusLineDirection busLineDirection = new BusLineDirection();
			busLineDirection.id = c.getString(c.getColumnIndexOrThrow(BusLineDirectionsColumns.DIRECTION_ID));
			busLineDirection.lineId = c.getString(c.getColumnIndexOrThrow(BusLineDirectionsColumns.DIRECTION_LINE_ID));
			busLineDirection.name = c.getString(c.getColumnIndexOrThrow(BusLineDirectionsColumns.DIRECTION_NAME));
			return busLineDirection;
		}

		/**
		 * @return the direction ID
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the bus line ID
		 */
		public String getLineId() {
			return lineId;
		}

		/**
		 * @return the direction name
		 */
		public String getName() {
			return name;
		}
	}

	/**
	 * The bus line direction columns
	 * @author Mathieu Méa
	 */
	public interface BusLineDirectionsColumns {
		public static final String DIRECTION_ID = StmDbHelper.T_BUS_LINE_DIRECTIONS_K_ID;
		public static final String DIRECTION_LINE_ID = StmDbHelper.T_BUS_LINE_DIRECTIONS_K_LINE_ID;
		public static final String DIRECTION_NAME = StmDbHelper.T_BUS_LINE_DIRECTIONS_K_NAME;
	}

	/**
	 * A bus line stop.
	 * @author Mathieu Méa
	 */
	public static class BusStop implements BaseColumns, BusLinesColumns, BusStopsColumns {
		/**
		 * The content URI for a bus stop.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/busstops");
		/**
		 * The content URI for the live folder.
		 */
		public static final Uri CONTENT_URI_FAV = Uri.parse("content://" + AUTHORITY + "/busstopslivefolder");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of bus stops.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.busstops";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single bus stop.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.busstops";
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of bus stops for the live folder.
		 */
		public static final String CONTENT_TYPE_LIVE_FOLDER = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.busstopslivefolder";
		/**
		 * The default sort order.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_BUS_STOPS_K_STOPS_ORDER + " ASC";
		/**
		 * The order by bus line number and bus stop code.
		 */
		public static final String ORDER_BY_LINE_CODE = StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER + ", " + StmDbHelper.T_BUS_STOPS_K_CODE + " ASC";
		/**
		 * The bus stop code
		 */
		private String code;
		/**
		 * The bus line direction ID
		 */
		private String directionId;
		/**
		 * The bus stop place
		 */
		private String place;
		/**
		 * The subway station ID
		 */
		private String subwayStationId;
		/**
		 * The bus line number.
		 */
		private String lineNumber;
		/**
		 * The bus line name.
		 */
		private String lineName;
		/**
		 * The bus line hours.
		 */
		private String lineHours;
		/**
		 * The bus line type.
		 */
		private String lineType;

		public static BusStop fromCursor(Cursor c) {
			final BusStop busStop = new BusStop();
			busStop.code = c.getString(c.getColumnIndexOrThrow(BusStopsColumns.STOP_CODE));
			busStop.directionId = c.getString(c.getColumnIndexOrThrow(BusStopsColumns.STOP_DIRECTION_ID));
			busStop.lineNumber = c.getString(c.getColumnIndexOrThrow(BusStopsColumns.STOP_LINE_NUMBER));
			busStop.place = c.getString(c.getColumnIndexOrThrow(BusStopsColumns.STOP_PLACE));
			busStop.subwayStationId = c.getString(c.getColumnIndexOrThrow(BusStopsColumns.STOP_SUBWAY_STATION_ID));
			// set the line name if the data is available
			if (c.getColumnIndex(BusLinesColumns.LINE_NAME) != -1) {
				busStop.lineName = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_NAME));
			}
			if (c.getColumnIndex(BusLinesColumns.LINE_HOURS) != -1) {
				busStop.lineHours = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_HOURS));
			}
			if (c.getColumnIndex(BusLinesColumns.LINE_TYPE) != -1) {
				busStop.lineType = c.getString(c.getColumnIndexOrThrow(BusLinesColumns.LINE_TYPE));
			}
			return busStop;
		}

		/**
		 * @return the bus stop line name or <b>NULL</b>
		 */
		public String getLineNameOrNull() {
			return lineName;
		}

		/**
		 * @return the bus stop line hours or <b>NULL</b>
		 */
		public String getLineHoursOrNull() {
			return lineHours;
		}

		/**
		 * @return the bus stop line type or <b>NULL</b>
		 */
		public String getLineTypeOrNull() {
			return lineType;
		}

		/**
		 * @return the bus stop code
		 */
		public String getCode() {
			return code;
		}

		/**
		 * @return the direction ID
		 */
		public String getDirectionId() {
			return directionId;
		}

		/**
		 * @return the bus stop place
		 */
		public String getPlace() {
			return place;
		}

		/**
		 * @return the bus line number
		 */
		public String getLineNumber() {
			return lineNumber;
		}

		/**
		 * @return the subway station ID
		 */
		public String getSubwayStationId() {
			return subwayStationId;
		}

		/**
		 * A sub directory of a single bus stop that contains all of their bus lines.
		 */
		public static final class BusLines implements BaseColumns, BusLinesColumns, BusStopsColumns {
			private BusLines() {
			}

			public static final String CONTENT_DIRECTORY = "buslines";
			public static final String DEFAULT_SORT_ORDER = BusLinesColumns.LINE_NUMBER + " ASC";
		}
	}

	/**
	 * The bus stop columns.
	 * @author Mathieu Méa
	 */
	public interface BusStopsColumns {
		public static final String STOP_CODE = StmDbHelper.T_BUS_STOPS_K_CODE;
		public static final String STOP_PLACE = StmDbHelper.T_BUS_STOPS_K_PLACE;
		public static final String STOP_DIRECTION_ID = StmDbHelper.T_BUS_STOPS_K_DIRECTION_ID;
		public static final String STOP_LINE_NUMBER = StmDbHelper.T_BUS_STOPS_K_LINE_NUMBER;
		public static final String STOP_SUBWAY_STATION_ID = StmDbHelper.T_BUS_STOPS_K_SUBWAY_STATION_ID;
		public static final String STOPS_ORDER = StmDbHelper.T_BUS_STOPS_K_STOPS_ORDER;
	}

	/**
	 * A subway line
	 * @author Mathieu Méa
	 */
	public static class SubwayLine implements BaseColumns, SubwayLinesColumns, SubwayStationsColumns {
		/**
		 * The content URI for a subway line
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/subwaylines");
		/**
		 * The frequency days.
		 */
		public static final String FREQUENCES_K_DAY_SATURDAY = StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY_SATURDAY;
		public static final String FREQUENCES_K_DAY_SUNDAY = StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY_SUNDAY;
		public static final String FREQUENCES_K_DAY_WEEK = StmDbHelper.T_SUBWAY_FREQUENCES_K_DAY_WEEK;
		/**
		 * The subway line directions.
		 */
		public static final String DIRECTION_1 = StmDbHelper.SUBWAY_DIRECTION_1;
		public static final String DIRECTION_2 = StmDbHelper.SUBWAY_DIRECTION_2;

		/**
		 * Subway line numbers.
		 */
		public static final int GREEN_LINE_NUMBER = 1;
		public static final int ORANGE_LINE_NUMBER = 2;
		public static final int YELLOW_LINE_NUMBER = 4;
		public static final int BLUE_LINE_NUMBER = 5;

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of subway lines.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.subwaylines";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single subway line.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.subwaylines";

		/**
		 * The default sort order.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_SUBWAY_LINES_K_NAME + " ASC";

		/**
		 * Order subway line by the real world order 1.
		 */
		public static final String NATURAL_SORT_ORDER = StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ORDER + " ASC";
		/**
		 * Order subway line by the real world order 2.
		 */
		public static final String NATURAL_SORT_ORDER_DESC = StmDbHelper.T_SUBWAY_DIRECTIONS_K_SUBWAY_STATION_ORDER + " DESC";

		/**
		 * The subway line number.
		 */
		private int number;
		/**
		 * The subway line name.
		 */
		private String name;

		/**
		 * @param c the cursor
		 * @return a subway line
		 */
		public static SubwayLine fromCursor(Cursor c) {
			final SubwayLine subwayLine = new SubwayLine();
			subwayLine.number = c.getInt(c.getColumnIndexOrThrow(SubwayLinesColumns.LINE_NUMBER));
			subwayLine.name = c.getString(c.getColumnIndexOrThrow(SubwayLinesColumns.LINE_NAME));
			return subwayLine;
		}

		/**
		 * @deprecated Use {@link Utils#getSubwayLineName(String)} instead (localized).
		 */
		@Deprecated
		public String getName() {
			MyLog.w(TAG, "Use of the deprecated subwayStation.getName() method");
			return name;
		}

		/**
		 * @return the subway line number
		 */
		public int getNumber() {
			return number;
		}

		/**
		 * A sub directory of a single subwayLine that contains all of their stations.
		 */
		public static final class SubwayStations implements BaseColumns, SubwayStationsColumns, SubwayLinesColumns {
			private SubwayStations() {
			}

			public static final String CONTENT_DIRECTORY = "subwaystations";
			public static final String DEFAULT_SORT_ORDER = SubwayStationsColumns.STATION_NAME + " ASC";
		}
	}

	/**
	 * The subway line columns
	 * @author Mathieu Méa
	 */
	public interface SubwayLinesColumns {
		public static final String LINE_NUMBER = StmDbHelper.T_SUBWAY_LINES_K_NUMBER;
		public static final String LINE_NAME = StmDbHelper.T_SUBWAY_LINES_K_NAME;
	}

	/**
	 * The subway station.
	 * @author Mathieu Méa
	 */
	public static class SubwayStation implements BaseColumns, SubwayStationsColumns, SubwayLinesColumns {
		/**
		 * The content URI for subway station.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/subwaystations");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of subway stations.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.subwaystations";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single subway station.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.subwaystations";
		/**
		 * The default sort order.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " ASC";
		/**
		 * The subway station ID.
		 */
		private String id;
		/**
		 * The subway station name.
		 */
		private String name;
		/**
		 * The subway station latitude.
		 */
		private double lat;
		/**
		 * The subway station longitude.
		 */
		private double lng;

		/**
		 * @param c the cursor
		 * @return a subway station
		 */
		public static SubwayStation fromCursor(Cursor c) {
			final SubwayStation subwayStation = new SubwayStation();
			subwayStation.id = c.getString(c.getColumnIndexOrThrow(SubwayStationsColumns.STATION_ID));
			subwayStation.name = c.getString(c.getColumnIndexOrThrow(SubwayStationsColumns.STATION_NAME));
			subwayStation.lat = c.getDouble(c.getColumnIndexOrThrow(SubwayStationsColumns.STATION_LAT));
			subwayStation.lng = c.getDouble(c.getColumnIndexOrThrow(SubwayStationsColumns.STATION_LNG));
			return subwayStation;
		}

		/**
		 * @return the subway station name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the subway station ID
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the subway station GPS latitude
		 */
		public double getLat() {
			return lat;
		}

		/**
		 * @return the subway station GPS longitude
		 */
		public double getLng() {
			return lng;
		}

		/**
		 * A sub directory of a single subway station that contains all of their bus lines.
		 * @author Mathieu Méa
		 */
		public static final class BusLines implements BaseColumns, BusLinesColumns, SubwayStationsColumns {
			private BusLines() {
			}

			public static final String CONTENT_DIRECTORY = "buslines";
			public static final String DEFAULT_SORT_ORDER = BusLinesColumns.LINE_NUMBER + " ASC";
		}

		/**
		 * A sub directory of a single subway station that contains all of their bus stops.
		 * @author Mathieu Méa
		 */
		public static final class BusStops implements BaseColumns, BusStopsColumns, SubwayStationsColumns {
			private BusStops() {
			}

			public static final String CONTENT_DIRECTORY = "busstops";
			public static final String DEFAULT_SORT_ORDER = BusStopsColumns.STOP_LINE_NUMBER + " ASC";
		}

		/**
		 * A sub directory of a single subway station that contains all of their subway lines.
		 * @author Mathieu Méa
		 */
		public static final class SubwayLines implements BaseColumns, SubwayLinesColumns, SubwayStationsColumns {
			private SubwayLines() {
			}

			public static final String CONTENT_DIRECTORY = "subwaylines";
			public static final String DEFAULT_SORT_ORDER = SubwayLinesColumns.LINE_NUMBER + " ASC";
		}
	}

	/**
	 * The subway lines columns
	 * @author Mathieu Méa
	 */
	public interface SubwayStationsColumns {
		public static final String STATION_ID = StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;
		public static final String STATION_NAME = StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME;
		public static final String STATION_LAT = StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT;
		public static final String STATION_LNG = StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG;
	}
}

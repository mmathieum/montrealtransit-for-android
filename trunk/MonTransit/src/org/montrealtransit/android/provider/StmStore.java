package org.montrealtransit.android.provider;

import org.montrealtransit.android.LocationUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.location.Location;
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
	 * The content URI for bus line.
	 */
	public static final String SEARCH_URI = "search";
	/**
	 * The content URI for a direction.
	 */
	public static final String DIRECTION_URI = "directions";
	/**
	 * The content URI for a day.
	 */
	public static final String DAY_URI = "days";
	/**
	 * The content URI for an hour.
	 */
	public static final String HOUR_URI = "hours";
	/**
	 * A frequency column name.
	 */
	public static final String FREQUENCY = "frequency";
	/**
	 * An hour column name.
	 */
	public static final String HOUR = "hour";
	/**
	 * A first/last column name.
	 */
	public static final String FIRST_LAST = "first_last";
	/**
	 * The content URI for a subway directions.
	 */
	private static final String SUBWAY_DIRECTIONS_URI = "subwaydirections";
	/**
	 * The subway directions URI.
	 */
	public static final Uri SUBWAY_DIRECTION_URI = Uri.parse("content://" + AUTHORITY + "/" + SUBWAY_DIRECTIONS_URI);

	public static final Uri DB_VERSION_URI = Uri.parse("content://" + AUTHORITY + "/version");

	public static final Uri DB_DEPLOYED_URI = Uri.parse("content://" + AUTHORITY + "/deployed");

	public static final Uri DB_LABEL_URI = Uri.parse("content://" + AUTHORITY + "/label");

	public static final Uri DB_SETUP_REQUIRED_URI = Uri.parse("content://" + AUTHORITY + "/setuprequired");

	/**
	 * A subway line
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
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".provider.subwaylines";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single subway line.
		 */
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".provider.subwaylines";

		/**
		 * The default sort order.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_SUBWAY_LINES + "." + StmDbHelper.T_SUBWAY_LINES_K_NUMBER + " ASC";

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
	 */
	public interface SubwayLinesColumns {
		public static final String LINE_NUMBER = StmDbHelper.T_SUBWAY_LINES + "_" + StmDbHelper.T_SUBWAY_LINES_K_NUMBER;
		public static final String LINE_NAME = StmDbHelper.T_SUBWAY_LINES + "_" + StmDbHelper.T_SUBWAY_LINES_K_NAME;
	}

	/**
	 * The subway station.
	 */
	public static class SubwayStation implements BaseColumns, SubwayStationsColumns, SubwayLinesColumns {
		/**
		 * The content URI for subway station.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/subwaystations");
		/**
		 * The content URI for the location.
		 */
		public static final Uri CONTENT_URI_LOC = Uri.parse("content://" + AUTHORITY + "/subwaystationsloc");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of subway stations.
		 */
		public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".provider.subwaystations";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single subway station.
		 */
		public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".provider.subwaystations";
		/**
		 * The default sort order.
		 */
		public static final String DEFAULT_SORT_ORDER = StmDbHelper.T_SUBWAY_STATIONS + "." + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME + " ASC";
		/**
		 * Order subway line by the real world order 1.
		 */
		public static final String NATURAL_SORT_ORDER = StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
				+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ORDER + " ASC";
		/**
		 * Order subway line by the real world order 2.
		 */
		public static final String NATURAL_SORT_ORDER_DESC = StmDbHelper.T_SUBWAY_LINES_DIRECTIONS + "."
				+ StmDbHelper.T_SUBWAY_LINES_DIRECTIONS_K_SUBWAY_STATION_ORDER + " DESC";
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
		 * The subway station location or null.
		 */
		private Location location;

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
		 * @param name the new subway station name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the subway station ID
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param id the new subway station ID
		 */
		public void setId(String id) {
			this.id = id;
		}

		/**
		 * @return the subway station GPS latitude
		 */
		public Double getLat() {
			return lat;
		}

		/**
		 * @param lat the new subway station GPS latitude
		 */
		public void setLat(double lat) {
			this.lat = lat;
		}

		/**
		 * @return the subway station GPS longitude
		 */
		public Double getLng() {
			return lng;
		}

		/**
		 * @param lng the new subway station GPS longitude
		 */
		public void setLng(double lng) {
			this.lng = lng;
		}

		/**
		 * @return the subway station location (not null)
		 */
		@Deprecated
		public Location getLocation() {
			if (this.location == null) {
				this.location = LocationUtils.getNewLocation(this.lat, this.lng);
			}
			return this.location;
		}

		public boolean hasLocation() {
			return true;
		}

		/**
		 * A sub directory of a single subway station that contains all of their subway lines.
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
	 */
	public interface SubwayStationsColumns {
		public static final String STATION_ID = StmDbHelper.T_SUBWAY_STATIONS + "_" + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_ID;
		public static final String STATION_NAME = StmDbHelper.T_SUBWAY_STATIONS + "_" + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_NAME;
		public static final String STATION_LAT = StmDbHelper.T_SUBWAY_STATIONS + "_" + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LAT;
		public static final String STATION_LNG = StmDbHelper.T_SUBWAY_STATIONS + "_" + StmDbHelper.T_SUBWAY_STATIONS_K_STATION_LNG;
	}
}

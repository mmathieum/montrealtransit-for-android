package org.montrealtransit.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class contains all the info about the data return by the {@link BixiProvider}
 * @author Mathieu Méa
 */
public class BixiStore {

	/**
	 * The log tag
	 */
	@SuppressWarnings("unused")
	private static final String TAG = BixiStore.class.getSimpleName();
	/**
	 * The provider authority.
	 */
	public static final String AUTHORITY = BixiProvider.AUTHORITY;

	/**
	 * Represent a bike station.
	 */
	public static class BikeStation implements BaseColumns, BikeStationColumns {
		/**
		 * The content URI for bike stations.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/bikestations");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of bike station entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.bikestations";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single bike station entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.bikestations";
		/**
		 * The default sort order for bike stations.
		 */
		public static final String DEFAULT_SORT_ORDER = BixiDbHelper.T_BIKE_STATIONS_K_TERMINAL_NAME + " ASC";

		/**
		 * The bike station ID.
		 */
		private int id;
		/**
		 * The bike station name.
		 */
		private String name;
		/**
		 * The bike station terminal name.
		 */
		private String terminalName;
		/**
		 * The bike station latitude.
		 */
		private double lat;
		/**
		 * The bike station longitude.
		 */
		private double lng;
		/**
		 * True if the bike station is installed.
		 */
		private boolean installed;
		/**
		 * True if the bike station is locked.
		 */
		private boolean locked;
		/**
		 * The bike station install date.
		 */
		private int installDate;
		/**
		 * The bike station removal date.
		 */
		private int removalDate;
		/**
		 * True if the bike station is temporary.
		 */
		private boolean temporary;
		/**
		 * The bike stations number of available bike.
		 */
		private int nbBikes;
		/**
		 * The bike stations number of empty docks.
		 */
		private int nbEmptyDocks;
		/**
		 * The bike stations latest update time.
		 */
		private int latestUpdateTime;

		/**
		 * @param c the cursor
		 * @return a bike station object from the cursor.
		 */
		public static BikeStation fromCursor(Cursor c) {
			// MyLog.v(TAG, "fromCursor()");
			final BikeStation bikeStation = new BikeStation();
			bikeStation.id = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.ID));
			bikeStation.name = c.getString(c.getColumnIndexOrThrow(BikeStationColumns.NAME));
			bikeStation.terminalName = c.getString(c.getColumnIndexOrThrow(BikeStationColumns.TERMINAL_NAME));
			bikeStation.lat = c.getDouble(c.getColumnIndexOrThrow(BikeStationColumns.LAT));
			bikeStation.lng = c.getDouble(c.getColumnIndexOrThrow(BikeStationColumns.LNG));
			bikeStation.installed = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.INSTALLED)) > 0;
			bikeStation.locked = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.LOCKED)) > 0;
			bikeStation.installDate = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.INSTALL_DATE));
			bikeStation.removalDate = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.REMOVAL_DATE));
			bikeStation.temporary = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.TEMPORARY)) > 0;
			bikeStation.nbBikes = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.NB_BIKES));
			bikeStation.nbEmptyDocks = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.NB_EMPTY_DOCKS));
			bikeStation.latestUpdateTime = c.getInt(c.getColumnIndexOrThrow(BikeStationColumns.LATEST_UPDATE_TIME));
			return bikeStation;
		}

		/**
		 * @param id the new ID
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * @return the ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * @param id the new name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param terminalName the new terminal name
		 */
		public void setTerminalName(String terminalName) {
			this.terminalName = terminalName;
		}

		/**
		 * @return the terminal name
		 */
		public String getTerminalName() {
			return terminalName;
		}

		/**
		 * @param the new latitude
		 */
		public void setLat(double lat) {
			this.lat = lat;
		}

		/**
		 * @return the latitude
		 */
		public double getLat() {
			return lat;
		}

		/**
		 * @param lng the new longitude
		 */
		public void setLng(double lng) {
			this.lng = lng;
		}

		/**
		 * @return the longitude
		 */
		public double getLng() {
			return lng;
		}

		/**
		 * @return true if installed
		 */
		public boolean isInstalled() {
			return installed;
		}

		/**
		 * @param installed true if installed
		 */
		public void setInstalled(boolean installed) {
			this.installed = installed;
		}

		/**
		 * @return true if locked
		 */
		public boolean isLocked() {
			return locked;
		}

		/**
		 * @param locked true if locked
		 */
		public void setLocked(boolean locked) {
			this.locked = locked;
		}

		/**
		 * @return the install date
		 */
		public int getInstallDate() {
			return installDate;
		}

		/**
		 * @param installDate the new install date
		 */
		public void setInstallDate(int installDate) {
			this.installDate = installDate;
		}

		/**
		 * @return the removal date
		 */
		public int getRemovalDate() {
			return removalDate;
		}

		/**
		 * @param removalDate the new removal date
		 */
		public void setRemovalDate(int removalDate) {
			this.removalDate = removalDate;
		}

		/**
		 * @return true if temporary
		 */
		public boolean isTemporary() {
			return temporary;
		}

		/**
		 * @param temporary true if temporary
		 */
		public void setTemporary(boolean temporary) {
			this.temporary = temporary;
		}

		/**
		 * @return the number of available bikes
		 */
		public int getNbBikes() {
			return nbBikes;
		}

		/**
		 * @param nbBikes the new number of available bikes
		 */
		public void setNbBikes(int nbBikes) {
			this.nbBikes = nbBikes;
		}

		/**
		 * @return the number of empty docks
		 */
		public int getNbEmptyDocks() {
			return nbEmptyDocks;
		}

		/**
		 * @param nbEmptyDocks the new number of empty docks
		 */
		public void setNbEmptyDocks(int nbEmptyDocks) {
			this.nbEmptyDocks = nbEmptyDocks;
		}
		
		/**
		 * @return the total number of docks
		 * @see {@link #getNbEmptyDocks()}
		 * @see {@link #getNbBikes()}
		 */
		public int getNbTotalDocks() {
			return getNbBikes() + getNbEmptyDocks();
		}

		/**
		 * @return the latest update time
		 */
		public int getLatestUpdateTime() {
			return latestUpdateTime;
		}

		/**
		 * @param latestUpdateTime the new latest update time
		 */
		public void setLatestUpdateTime(int latestUpdateTime) {
			this.latestUpdateTime = latestUpdateTime;
		}

		/**
		 * @return the content values representing this bike station entry
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			// values.put(BikeStationColumns.ID, getId()); // auto-increment
			values.put(BikeStationColumns.NAME, getName());
			values.put(BikeStationColumns.TERMINAL_NAME, getTerminalName());
			values.put(BikeStationColumns.LAT, getLat());
			values.put(BikeStationColumns.LNG, getLng());
			values.put(BikeStationColumns.INSTALLED, isInstalled()); // TODO boolean?
			values.put(BikeStationColumns.LOCKED, isLocked()); // TODO boolean?
			values.put(BikeStationColumns.INSTALL_DATE, getInstallDate());
			values.put(BikeStationColumns.REMOVAL_DATE, getRemovalDate());
			values.put(BikeStationColumns.TEMPORARY, isTemporary()); // TODO boolean?
			values.put(BikeStationColumns.NB_BIKES, getNbBikes());
			values.put(BikeStationColumns.NB_EMPTY_DOCKS, getNbEmptyDocks());
			values.put(BikeStationColumns.LATEST_UPDATE_TIME, getLatestUpdateTime());
			return values;
		}

		@Override
		public String toString() {
			return new StringBuilder().append(getName()).append('-').append(getTerminalName()).toString();
		}

		/**
		 * @param bs1 bike station
		 * @param bs2 bike station
		 * @return true if the 2 bike stations are considered as equals
		 */
		public static boolean equals(BikeStation bs1, BikeStation bs2) {
			// MyLog.v(TAG, "equals()");
			if (bs1 == null && bs2 == null) {
				return true; // both null
			}
			if (bs1 == null || bs2 == null) {
				return false; // only 1 is null
			}
			if (!bs1.getTerminalName().equals(bs2.getTerminalName())) {
				return false;
			}
			// 2 bike stations with same terminal name at this point
			// checking bikes, empty dock
			if (bs1.getNbBikes() != bs2.getNbBikes() || bs1.getNbEmptyDocks() != bs2.getNbEmptyDocks()) {
				return false;
			}
			// checking last update time
			if (bs1.getLatestUpdateTime() != bs2.getLatestUpdateTime()) {
				return false;
			}
			// TODO what else?
			return true;
		}
	}

	/**
	 * The bike station columns.
	 */
	public interface BikeStationColumns {
		public static final String ID = BixiDbHelper.T_BIKE_STATIONS_K_ID;
		public static final String NAME = BixiDbHelper.T_BIKE_STATIONS_K_NAME;
		public static final String TERMINAL_NAME = BixiDbHelper.T_BIKE_STATIONS_K_TERMINAL_NAME;
		public static final String LAT = BixiDbHelper.T_BIKE_STATIONS_K_LAT;
		public static final String LNG = BixiDbHelper.T_BIKE_STATIONS_K_LNG;
		public static final String INSTALLED = BixiDbHelper.T_BIKE_STATIONS_K_INSTALLED;
		public static final String LOCKED = BixiDbHelper.T_BIKE_STATIONS_K_LOCKED;
		public static final String INSTALL_DATE = BixiDbHelper.T_BIKE_STATIONS_K_INSTALL_DATE;
		public static final String REMOVAL_DATE = BixiDbHelper.T_BIKE_STATIONS_K_REMOVE_DATE;
		public static final String TEMPORARY = BixiDbHelper.T_BIKE_STATIONS_K_TEMPORARY;
		public static final String NB_BIKES = BixiDbHelper.T_BIKE_STATIONS_K_NB_BIKES;
		public static final String NB_EMPTY_DOCKS = BixiDbHelper.T_BIKE_STATIONS_K_NB_EMPTY_DOCKS;
		public static final String LATEST_UPDATE_TIME = BixiDbHelper.T_BIKE_STATIONS_K_LATEST_UPDATE_TIME;
	}
}

package org.montrealtransit.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The data store contains information about the objects from the {@link DataProvider}.
 * @author Mathieu Méa
 */
public class DataStore {

	/**
	 * The content URI provider.
	 * @see DataProvider
	 */
	public static final String AUTHORITY = DataProvider.AUTHORITY;

	/**
	 * This class represent a favorite entry.
	 * @author Mathieu Méa
	 */
	public static class Fav implements BaseColumns, FavColumns {
		/**
		 * The content URI for favorite.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favs");
		/**
		 * Content URI for favorite type.
		 */
		public static final String URI_TYPE = "type";
		/**
		 * The content URI for favorite in the live folder.
		 */
		public static final Uri CONTENT_URI_LIVE_FOLDER = Uri.parse("content://" + AUTHORITY + "/live_folder_favs");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of favorite entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.favs";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single favorite entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.favs";
		/**
		 * The default sort order for favorite.
		 */
		public static final String DEFAULT_SORT_ORDER = DataDbHelper.T_FAVS_K_FK_ID + ", " + DataDbHelper.T_FAVS_K_FK_ID_2 + " ASC";
		/**
		 * The favorite type value for bus stops.
		 */
		public static final int KEY_TYPE_VALUE_BUS_STOP = DataDbHelper.KEY_TYPE_VALUE_BUS_STOP;
		/**
		 * The favorite type value for subway stations.
		 */
		public static final int KEY_TYPE_VALUE_SUBWAY_STATION = DataDbHelper.KEY_TYPE_VALUE_SUBWAY_STATION;
		/**
		 * The favorite ID.
		 */
		private int id;
		/**
		 * The favorite FK ID.
		 */
		private String fkId;
		/**
		 * The favorite FK ID2.
		 */
		private String fkId2;
		/**
		 * The favorite type.
		 */
		private int type;

		/**
		 * @param c the cursor
		 * @return a favorite object from the cursor.
		 */
		public static Fav fromCursor(Cursor c) {
			final Fav fav = new Fav();
			fav.id = c.getInt(c.getColumnIndexOrThrow(FavColumns.FAV_ID));
			fav.fkId = c.getString(c.getColumnIndexOrThrow(FavColumns.FAV_FK_ID));
			fav.fkId2 = c.getString(c.getColumnIndexOrThrow(FavColumns.FAV_FK_ID2));
			fav.type = c.getInt(c.getColumnIndexOrThrow(FavColumns.FAV_TYPE));
			return fav;
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
		 * @param fkId the new FK ID
		 */
		public void setFkId(String fkId) {
			this.fkId = fkId;
		}

		/**
		 * @return the FK ID
		 */
		public String getFkId() {
			return fkId;
		}

		/**
		 * @param fkId2 the new FK ID2
		 */
		public void setFkId2(String fkId2) {
			this.fkId2 = fkId2;
		}

		/**
		 * @return the FK ID
		 */
		public String getFkId2() {
			return fkId2;
		}

		/**
		 * @param type the new type
		 */
		public void setType(int type) {
			this.type = type;
		}

		/**
		 * @return the type
		 */
		public int getType() {
			return type;
		}

		/**
		 * @return the content values representing this favorite.
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			values.put(FavColumns.FAV_FK_ID, getFkId());
			values.put(FavColumns.FAV_FK_ID2, getFkId2());
			values.put(FavColumns.FAV_TYPE, getType());
			return values;
		}
	}

	/**
	 * The column associated with a favorite entries.
	 * @author Mathieu Méa
	 */
	public interface FavColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String FAV_ID = DataDbHelper.T_FAVS_K_ID;
		public static final String FAV_FK_ID = DataDbHelper.T_FAVS_K_FK_ID;
		public static final String FAV_FK_ID2 = DataDbHelper.T_FAVS_K_FK_ID_2;
		public static final String FAV_TYPE = DataDbHelper.T_FAVS_K_TYPE;
	}

	/**
	 * This class represent an history entry.
	 * @author Mathieu Méa
	 */
	public static class History implements BaseColumns, HistoryColumns {
		/**
		 * The content URI for history.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/history");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of history entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.history";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single history entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.history";
		/**
		 * The default order for history.
		 */
		public static final String DEFAULT_SORT_ORDER = DataDbHelper.T_HISTORY_K_ID + " DESC";
		/**
		 * The history ID.
		 */
		private int id;
		/**
		 * The history value.
		 */
		private String value;

		/**
		 * @param c the cursor
		 * @return an history entry from the cursor values.
		 */
		public static History fromCursor(Cursor c) {
			final History history = new History();
			history.id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
			history.value = c.getString(c.getColumnIndexOrThrow(HistoryColumns.VALUE));
			return history;
		}

		/**
		 * @return the ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * @param value the new value
		 */
		public void setValue(String value) {
			this.value = value;
		}

		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @return the content values representing this history entry.
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			values.put(HistoryColumns.VALUE, getValue());
			return values;
		}
	}

	/**
	 * The history columns
	 * @author Mathieu Méa
	 */
	public interface HistoryColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String VALUE = DataDbHelper.T_HISTORY_K_VALUE;
	}

}

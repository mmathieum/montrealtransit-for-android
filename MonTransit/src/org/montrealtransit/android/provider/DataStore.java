package org.montrealtransit.android.provider;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

/**
 * The data store contains information about the objects from the {@link DataProvider}.
 * @author Mathieu Méa
 */
public class DataStore {

	/**
	 * The log tag.
	 */
	private static final String TAG = DataStore.class.getSimpleName();

	/**
	 * The content URI provider.
	 * @see DataProvider
	 */
	public static final String AUTHORITY = DataProvider.AUTHORITY;

	/**
	 * This class represent a favorite entry.
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
		public static final int KEY_TYPE_VALUE_BUS_STOP = DataDbHelper.KEY_FAVS_TYPE_VALUE_BUS_STOP;
		/**
		 * The favorite type value for subway stations.
		 */
		public static final int KEY_TYPE_VALUE_SUBWAY_STATION = DataDbHelper.KEY_FAVS_TYPE_VALUE_SUBWAY_STATION;
		/**
		 * The favorite type value for bike stations.
		 */
		public static final int KEY_TYPE_VALUE_BIKE_STATIONS = DataDbHelper.KEY_FAVS_TYPE_VALUE_BIKE_STATION;
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

		/**
		 * @param fav the favorite object
		 * @return the serialized favorite
		 */
		public static String serializeFav(Fav fav) {
			// MyLog.v(TAG, "serializeFav()");
			String result = ""; // TODO use StringBuilder!
			result += fav.getType();
			result += "-";
			result += fav.getFkId();
			if (!TextUtils.isEmpty(fav.getFkId2())) {
				result += "-";
				result += fav.getFkId2();
			}
			return result;
		}

		/**
		 * @param line the serialized favorite
		 * @return the favorite object OR <b>NULL</b> if invalid
		 */
		public static Fav extractFav(String line) {
			// MyLog.v(TAG, "extractFav(%s)", line);
			try {
				Fav fav = new Fav();
				String[] keys = line.split("-");
				int type = Integer.valueOf(keys[0]);
				fav.setType(type);
				switch (fav.getType()) {
				case Fav.KEY_TYPE_VALUE_BUS_STOP:
					if (keys.length < 3) {
						return null; // we need the fkID2 for bus stops!
					}
					fav.setFkId2(keys[2]);
				case Fav.KEY_TYPE_VALUE_SUBWAY_STATION:
				case Fav.KEY_TYPE_VALUE_BIKE_STATIONS:
					fav.setFkId(keys[1]);
				}
				return fav;
			} catch (Exception e) {
				MyLog.w(TAG, "Error while reading favorite!", e);
				return null;
			}
		}

		/**
		 * @param favoritesS the serialized favorites
		 * @return the list of favorites
		 */
		public static List<Fav> extractFavs(String favoritesS) {
			// MyLog.v(TAG, "extractFavs(%s)", favoritesS);
			List<Fav> favs = new ArrayList<Fav>();
			String[] allFavS = favoritesS.split("\\+");
			for (String favS : allFavS) {
				Fav extractFav = Fav.extractFav(favS);
				if (extractFav != null) {
					favs.add(extractFav);
				}
			}
			return favs;
		}

		/**
		 * @param favs the favorites list
		 * @param fav the favorite to find
		 * @return true if the favorite is present in the list
		 */
		public static boolean contains(List<Fav> favs, Fav fav) {
			// MyLog.v(TAG, "contains()");
			if (favs == null || favs.size() == 0) {
				return false; // not in the list
			}
			for (Fav currentFav : favs) {
				if (fav.getType() == currentFav.getType() && fav.getFkId().equals(currentFav.getFkId())) {
					if (fav.getType() == Fav.KEY_TYPE_VALUE_BUS_STOP) {
						if (fav.getFkId2() != null && fav.getFkId2().equals(currentFav.getFkId2())) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * @param favs1 a favorites list (not null)
		 * @param favs2 a favorite list (not null)
		 * @return true if they don't contain
		 */
		public static boolean listEquals(List<Fav> favs1, List<Fav> favs2) {
			if (favs1.size() != favs2.size()) {
				return false;
			}
			for (Fav fav1 : favs1) {
				if (!Fav.contains(favs2, fav1)) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Serialize the favorites list.
		 * @param favs the favorites list
		 * @return the serialized favorites
		 */
		public static String serializeFavs(List<Fav> favs) {
			// MyLog.v(TAG, "serializeFavs()");
			String favS = "";
			if (favs != null) {
				for (Fav currentFav : favs) {
					if (favS.length() > 0) {
						favS += "+";
					}
					favS += Fav.serializeFav(currentFav);
				}
			}
			return favS;
		}
	}

	/**
	 * The column associated with a favorite entry.
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
		 * The Default constructor.
		 */
		private History() {
		}

		/**
		 * Create an history with a value
		 * @param value the value
		 */
		public History(String value) {
			this.value = value;
		}

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
	 */
	public interface HistoryColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String VALUE = DataDbHelper.T_HISTORY_K_VALUE;
	}

	/**
	 * This class represent an Twitter API entry.
	 */
	public static class TwitterApi implements BaseColumns, TwitterApiColumns {
		/**
		 * The content URI for history.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/twitterapi");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of Twitter API entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.twitterapi";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single Twitter API entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.twitterapi";
		/**
		 * The default order for Twitter API.
		 */
		public static final String DEFAULT_SORT_ORDER = DataDbHelper.T_TWITTER_API_K_ID + " DESC";
		/**
		 * The Twitter API ID.
		 */
		private int id;
		/**
		 * The Twitter API token.
		 */
		private String token;
		/**
		 * The Twitter API token secret.
		 */
		private String tokenSecret;

		/**
		 * @param c the cursor
		 * @return an history entry from the cursor values.
		 */
		public static TwitterApi fromCursor(Cursor c) {
			final TwitterApi twitterApi = new TwitterApi();
			twitterApi.id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
			twitterApi.token = c.getString(c.getColumnIndexOrThrow(TwitterApiColumns.TOKEN));
			twitterApi.tokenSecret = c.getString(c.getColumnIndexOrThrow(TwitterApiColumns.TOKEN_SECRET));
			return twitterApi;
		}

		/**
		 * @return the ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * @param token the new token
		 */
		public void setToken(String token) {
			this.token = token;
		}

		/**
		 * @return the token
		 */
		public String getToken() {
			return token;
		}

		/**
		 * @param token the new token secret
		 */
		public void setTokenSecret(String tokenSecret) {
			this.tokenSecret = tokenSecret;
		}

		/**
		 * @return the token secret
		 */
		public String getTokenSecret() {
			return tokenSecret;
		}

		/**
		 * @return the content values representing this Twitter API entry.
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			values.put(TwitterApiColumns.TOKEN, getToken());
			values.put(TwitterApiColumns.TOKEN_SECRET, getTokenSecret());
			return values;
		}
	}

	/**
	 * The Twitter API columns
	 */
	public interface TwitterApiColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String TOKEN = DataDbHelper.T_TWITTER_API_K_TOKEN;
		public static final String TOKEN_SECRET = DataDbHelper.T_TWITTER_API_K_TOKEN_SECRET;
	}

	/**
	 * This class represent a service status entry.
	 */
	public static class ServiceStatus implements BaseColumns, ServiceStatusColumns {
		/**
		 * The content URI for service status.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/servicestatus");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of service status entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.servicestatus";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single service status entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.servicestatus";
		/**
		 * The default order for service status.
		 */
		public static final String DEFAULT_SORT_ORDER = DataDbHelper.T_SERVICE_STATUS_K_ID + " DESC";

		/**
		 * The order status from the latest to the oldest.
		 */
		public static final String ORDER_BY_LATEST_PUB_DATE = DataDbHelper.T_SERVICE_STATUS_K_PUB_DATE + " DESC";

		/**
		 * The different service types values.
		 */
		public static final int STATUS_TYPE_DEFAULT = DataDbHelper.SERVICE_STATUS_TYPE_DEFAULT;
		public static final int STATUS_TYPE_GREEN = DataDbHelper.SERVICE_STATUS_TYPE_GREEN;
		public static final int STATUS_TYPE_YELLOW = DataDbHelper.SERVICE_STATUS_TYPE_YELLOW;
		public static final int STATUS_TYPE_RED = DataDbHelper.SERVICE_STATUS_TYPE_RED;

		/**
		 * The different service language values.
		 */
		public static final String STATUS_LANG_UNKNOWN = DataDbHelper.SERVICE_STATUS_LANG_UNKNOWN;
		public static final String STATUS_LANG_ENGLISH = DataDbHelper.SERVICE_STATUS_LANG_ENGLISH;
		public static final String STATUS_LANG_FRENCH = DataDbHelper.SERVICE_STATUS_LANG_FRENCH;

		/**
		 * The service status ID.
		 */
		private int id;
		/**
		 * The service status message.
		 */
		private String message;
		/**
		 * The service status publish date in seconds.
		 */
		private int pubDate;
		/**
		 * The service status read date in seconds.
		 */
		private int readDate;
		/**
		 * The service status type.
		 */
		private int type;
		/**
		 * The service status language.
		 */
		private String language;
		/**
		 * The service status source name.
		 */
		private String sourceName;
		/**
		 * The service status source link.
		 */
		private String sourceLink;

		/**
		 * @param c the cursor
		 * @return an history entry from the cursor values.
		 */
		public static ServiceStatus fromCursor(Cursor c) {
			final ServiceStatus serviceStatus = new ServiceStatus();
			serviceStatus.id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
			serviceStatus.message = c.getString(c.getColumnIndexOrThrow(ServiceStatusColumns.MESSAGE));
			serviceStatus.pubDate = c.getInt(c.getColumnIndexOrThrow(ServiceStatusColumns.PUB_DATE));
			serviceStatus.readDate = c.getInt(c.getColumnIndexOrThrow(ServiceStatusColumns.READ_DATE));
			serviceStatus.type = c.getInt(c.getColumnIndexOrThrow(ServiceStatusColumns.TYPE));
			serviceStatus.language = c.getString(c.getColumnIndexOrThrow(ServiceStatusColumns.LANGUAGE));
			serviceStatus.sourceName = c.getString(c.getColumnIndexOrThrow(ServiceStatusColumns.SOURCE_NAME));
			serviceStatus.sourceLink = c.getString(c.getColumnIndexOrThrow(ServiceStatusColumns.SOURCE_LINK));
			return serviceStatus;
		}

		/**
		 * @return the ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * @param message the new message
		 */
		public void setMessage(String message) {
			this.message = message;
		}

		/**
		 * @return the pub date
		 */
		public int getPubDate() {
			return pubDate;
		}

		/**
		 * @param pubDate the new pub date
		 */
		public void setPubDate(int pubDate) {
			this.pubDate = pubDate;
		}

		/**
		 * @return read date in seconds
		 */
		public int getReadDate() {
			return readDate;
		}

		/**
		 * @return read date in milliseconds
		 */
		public long getReadDateInMs() {
			return ((long) getReadDate()) * 1000;
		}

		/**
		 * @param readDate the new read date in seconds.
		 */
		public void setReadDate(int readDate) {
			this.readDate = readDate;
		}

		/**
		 * @return the type
		 */
		public int getType() {
			return type;
		}

		/**
		 * @param type the new type
		 */
		public void setType(int type) {
			this.type = type;
		}

		/**
		 * @return the language
		 */
		public String getLanguage() {
			return language;
		}

		/**
		 * @param language the new language
		 */
		public void setLanguage(String language) {
			this.language = language;
		}

		/**
		 * @return the source name
		 */
		public String getSourceName() {
			return sourceName;
		}

		/**
		 * @param sourceName the new source name
		 */
		public void setSourceName(String sourceName) {
			this.sourceName = sourceName;
		}

		/**
		 * @return the source link
		 */
		public String getSourceLink() {
			return sourceLink;
		}

		/**
		 * @param sourceLink the new source link
		 */
		public void setSourceLink(String sourceLink) {
			this.sourceLink = sourceLink;
		}

		/**
		 * @return the content values representing this Twitter API entry.
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			values.put(ServiceStatusColumns.MESSAGE, getMessage());
			values.put(ServiceStatusColumns.PUB_DATE, getPubDate());
			values.put(ServiceStatusColumns.READ_DATE, getReadDate());
			values.put(ServiceStatusColumns.TYPE, getType());
			values.put(ServiceStatusColumns.LANGUAGE, getLanguage());
			values.put(ServiceStatusColumns.SOURCE_NAME, getSourceName());
			values.put(ServiceStatusColumns.SOURCE_LINK, getSourceLink());
			return values;
		}
	}

	/**
	 * The Service Status columns
	 */
	public interface ServiceStatusColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String MESSAGE = DataDbHelper.T_SERVICE_STATUS_K_MESSAGE;
		public static final String PUB_DATE = DataDbHelper.T_SERVICE_STATUS_K_PUB_DATE;
		public static final String READ_DATE = DataDbHelper.T_SERVICE_STATUS_K_READ_DATE;
		public static final String TYPE = DataDbHelper.T_SERVICE_STATUS_K_TYPE;
		public static final String LANGUAGE = DataDbHelper.T_SERVICE_STATUS_K_LANGUAGE;
		public static final String SOURCE_NAME = DataDbHelper.T_SERVICE_STATUS_K_SOURCE;
		public static final String SOURCE_LINK = DataDbHelper.T_SERVICE_STATUS_K_LINK;
	}

	/**
	 * This class represent a cache entry.
	 */
	public static class Cache implements BaseColumns, CacheColumns {
		/**
		 * The content URI for cache.
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/cache");
		/**
		 * Content URI for cache date.
		 */
		public static final String URI_DATE = "date";
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of cache entries.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + ".provider.cache";
		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single cache entry.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + ".provider.cache";
		/**
		 * The default sort order for cache.
		 */
		public static final String DEFAULT_SORT_ORDER = DataDbHelper.T_CACHE_K_DATE + " DESC";
		/**
		 * The cache type value for bus stops.
		 */
		public static final int KEY_TYPE_VALUE_BUS_STOP = DataDbHelper.KEY_CACHE_TYPE_VALUE_BUS_STOP;
		/**
		 * The cache ID.
		 */
		private int id;
		/**
		 * The cache date in seconds.
		 */
		private int date;
		/**
		 * The cache type.
		 */
		private int type;
		/**
		 * The cache FK ID.
		 */
		private String fkId;
		/**
		 * The cache object.
		 */
		private String object;

		/**
		 * The Default Constructor.
		 */
		public Cache() {
		}

		/**
		 * Create a new cache with date = now.
		 * @param type the cache type
		 * @param fkId the cache FK ID
		 * @param object the cache object
		 */
		public Cache(int type, String fkId, String object) {
			this.date = Utils.currentTimeSec();
			this.type = type;
			this.fkId = fkId;
			this.object = object;
		}

		/**
		 * @param c the cursor
		 * @return a cache object from the cursor
		 */
		public static Cache fromCursor(Cursor c) {
			final Cache cache = new Cache();
			cache.id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
			cache.date = c.getInt(c.getColumnIndexOrThrow(CacheColumns.CACHE_DATE));
			cache.type = c.getInt(c.getColumnIndexOrThrow(CacheColumns.CACHE_TYPE));
			cache.fkId = c.getString(c.getColumnIndexOrThrow(CacheColumns.CACHE_FK_ID));
			cache.object = c.getString(c.getColumnIndexOrThrow(CacheColumns.CACHE_OBJECT));
			return cache;
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
		 * @return the date
		 */
		public int getDate() {
			return date;
		}

		/**
		 * @param date the new date
		 */
		public void setDate(int date) {
			this.date = date;
		}

		/**
		 * @return the object
		 */
		public String getObject() {
			return object;
		}

		/**
		 * @param object the new object
		 */
		public void setObject(String object) {
			this.object = object;
		}

		/**
		 * @return the content values representing this favorite.
		 */
		public ContentValues getContentValues() {
			final ContentValues values = new ContentValues();
			values.put(CacheColumns.CACHE_DATE, getDate());
			values.put(CacheColumns.CACHE_TYPE, getType());
			values.put(CacheColumns.CACHE_FK_ID, getFkId());
			values.put(CacheColumns.CACHE_OBJECT, getObject());
			return values;
		}
	}

	/**
	 * The column associated with a cache entry.
	 */
	public interface CacheColumns {
		// TODO release this columns from their link to the real DB column name. Should use projection.
		public static final String CACHE_DATE = DataDbHelper.T_CACHE_K_DATE;
		public static final String CACHE_TYPE = DataDbHelper.T_CACHE_K_TYPE;
		public static final String CACHE_FK_ID = DataDbHelper.T_CACHE_K_FK_ID;
		public static final String CACHE_OBJECT = DataDbHelper.T_CACHE_K_OBJECT;
	}

}

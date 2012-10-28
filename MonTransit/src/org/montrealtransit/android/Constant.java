package org.montrealtransit.android;

/**
 * Contains all the constants use by the application.
 * @author Mathieu Méa
 */
public class Constant {

	/**
	 * The application package from the Manifest.
	 */
	public static final String PKG = "org.montrealtransit.android";
	/**
	 * The donation package.
	 */
	public static final String DONATE_PKG = "org.montrealtransit.android.donate.mm.onetrip";

	public static final String PLACE_CHAR_DE = "de ";
	public static final int PLACE_CHAR_DE_LENGTH = PLACE_CHAR_DE.length();

	// public static final String PLACE_CHAR_DE_LA = "de la ";
	// public static final int PLACE_CHAR_DE_LA_LENGTH = PLACE_CHAR_DE_LA.length();

	public static final String PLACE_CHAR_DES = "des ";
	public static final int PLACE_CHAR_DES_LENGTH = PLACE_CHAR_DES.length();

	public static final String PLACE_CHAR_DU = "du ";
	public static final int PLACE_CHAR_DU_LENGTH = PLACE_CHAR_DU.length();

	public static final String PLACE_CHAR_LA = "la ";
	public static final int PLACE_CHAR_LA_LENGTH = PLACE_CHAR_LA.length();

	public static final String PLACE_CHAR_L = "l'";
	public static final int PLACE_CHAR_L_LENGTH = PLACE_CHAR_L.length();

	public static final String PLACE_CHAR_D = "d'";
	public static final int PLACE_CHAR_D_LENGTH = PLACE_CHAR_D.length();

	public static final String PLACE_CHAR_IN = "/ ";

	public static final String PLACE_CHAR_IN_DE = PLACE_CHAR_IN + PLACE_CHAR_DE;

	// public static final String PLACE_CHAR_IN_DE_LA = PLACE_CHAR_IN + PLACE_CHAR_DE_LA;

	public static final String PLACE_CHAR_IN_DES = PLACE_CHAR_IN + PLACE_CHAR_DES;

	public static final String PLACE_CHAR_IN_DU = PLACE_CHAR_IN + PLACE_CHAR_DU;

	public static final String PLACE_CHAR_IN_LA = PLACE_CHAR_IN + PLACE_CHAR_LA;

	public static final String PLACE_CHAR_IN_L = PLACE_CHAR_IN + PLACE_CHAR_L;

	public static final String PLACE_CHAR_IN_D = PLACE_CHAR_IN + PLACE_CHAR_D;

	public static final String PLACE_CHAR_PARENTHESE = "(";

	public static final String PLACE_CHAR_PARENTHESE_STATION = PLACE_CHAR_PARENTHESE + "station ";
	public static final int PLACE_CHAR_PARENTHESE_STATION_LENGTH = PLACE_CHAR_PARENTHESE_STATION.length();
	public static final String PLACE_CHAR_PARENTHESE_STATION_BIG = PLACE_CHAR_PARENTHESE + "Station ";
	public static final int PLACE_CHAR_PARENTHESE_STATION_BIG_LENGTH = PLACE_CHAR_PARENTHESE_STATION_BIG.length();

	/**
	 * Montreal location (source:wikipedia.org)
	 */
	public static final double MONTREAL_LAT = 45.5;
	public static final double MONTREAL_LNG = -73.666667;

	/**
	 * STM coverage area.
	 */
	public static final double STM_UPPER_RIGHT_LAT = 45.7278;
	public static final double STM_UPPER_RIGHT_LNG = -73.4738;
	public static final double STM_LOWER_LEFT_LNG = 45.4038;
	public static final double STM_LOWER_LEFT_LAT = -73.9943;

	/**
	 * The max number of search results.
	 */
	public static final int NB_SEARCH_RESULT = 7;

	/**
	 * The shortcut for STM Mobile link.
	 */
	public static final char SHORTCUT_STM_MOBILE = 's';
	/**
	 * The shortcut for refresh data.
	 */
	public static final char SHORTCUT_REFRESH = 'r';
	/**
	 * The shortcut for maps.
	 */
	public static final char SHORTCUT_MAPS = 'm';

	/**
	 * The number of feet necessary to make 1 meter.
	 */
	public static float FEET_PER_M = 3.2808399f;
	/**
	 * The number of feet necessary to make 1 mile.
	 */
	public static float FEET_PER_MILE = 5280;
	/**
	 * The number of meter necessary to make 1 KM.
	 */
	public static float METER_PER_KM = 1000f;
}

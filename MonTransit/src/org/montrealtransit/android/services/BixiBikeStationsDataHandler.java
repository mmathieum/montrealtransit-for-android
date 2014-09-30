package org.montrealtransit.android.services;

import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.provider.BixiStore.BikeStation;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Handler for <a href="https://montreal.bixi.com/data/bikeStations.xml">https://montreal.bixi.com/data/bikeStations.xml</a>.
 */
public class BixiBikeStationsDataHandler extends DefaultHandler implements ContentHandler {

	/**
	 * The log tag.
	 */
	private static final String TAG = BixiBikeStationsDataHandler.class.getSimpleName();

	/**
	 * XML tags.
	 */
	private static final String STATIONS = "stations";
	// private static final String STATIONS_LAST_UPDATE = "lastUpdate";
	private static final String STATIONS_VERSION = "version"; // 2.0
	private static final String STATION = "station";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String TERMINAL_NAME = "terminalName";
	private static final String LAST_COMM_WITH_SERVER = "lastCommWithServer";
	private static final String LAT = "lat";
	private static final String LONG = "long";
	private static final String INSTALLED = "installed";
	private static final String LOCKED = "locked";
	private static final String INSTALL_DATE = "installDate";
	private static final String REMOVAL_DATE = "removalDate";
	private static final String TEMPORARY = "temporary";
	private static final String PUBLIC = "public";
	private static final String NB_BIKES = "nbBikes";
	private static final String NB_EMPTY_DOCKS = "nbEmptyDocks";
	private static final String LATEST_UPDATE_TIME = "latestUpdateTime";

	private static final String SUPPORTED_VERSIONS = "2.0";

	/**
	 * The current element name.
	 */
	private String currentLocalName = STATIONS;

	/**
	 * The bike stations.
	 */
	private List<BikeStation> bikeStations = new ArrayList<BikeStation>();
	// /**
	// * The bike stations list last update;
	// */
	// private int lastUpdate = -1;

	/**
	 * The current bike station or null.
	 */
	private BikeStation currentBikeStation = null;

	/**
	 * @return the list of bike stations
	 */
	public List<BikeStation> getBikeStations() {
		return this.bikeStations;
	}

	// /**
	// * @return the last update
	// */
	// public int getLastUpdate() {
	// return lastUpdate;
	// }

	// @Override
	// public void startDocument() throws SAXException {
	// MyLog.v(TAG, "startDocument()");
	// }

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// MyLog.v(TAG, "startElement(%s,%s,%s)", uri, localName, qName);
		this.currentLocalName = localName;
		if (STATIONS.equals(localName)) {
			// read version attribute
			String version = attributes.getValue(STATIONS_VERSION);
			if (version == null || !SUPPORTED_VERSIONS.equals(version)) {
				// unknown version
				MyLog.w(TAG, "XML version '%s' not supported!", version);
				// TODO abort parsing ?
			}
			// // read last update attribute
			// this.lastUpdate = Utils.toTimestampInSeconds(Long.valueOf(attributes.getValue(STATIONS_LAST_UPDATE)));
		} else if (STATION.equals(localName)) {
			this.currentBikeStation = new BikeStation();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// MyLog.v(TAG, "characters(%s)", new String(ch, start, length));
		if (this.currentBikeStation != null) {
			try {
				String string = new String(ch, start, length).trim();
				if (ID.equals(this.currentLocalName)) {
					// do not store source ID as it only represents the current position in the XML list
					// this.currentBikeStation.setId(Integer.valueOf(string));
				} else if (NAME.equals(this.currentLocalName)) {
					this.currentBikeStation.setName(string);
				} else if (TERMINAL_NAME.equals(this.currentLocalName)) {
					this.currentBikeStation.setTerminalName(string);
				} else if (LAST_COMM_WITH_SERVER.equals(this.currentLocalName)) {
					this.currentBikeStation.setLastCommWithServer(Utils.toTimestampInSeconds(Long.valueOf(string)));
				} else if (LAT.equals(this.currentLocalName)) {
					this.currentBikeStation.setLat(Double.valueOf(string));
				} else if (LONG.equals(this.currentLocalName)) {
					this.currentBikeStation.setLng(Double.valueOf(string));
				} else if (INSTALLED.equals(this.currentLocalName)) {
					this.currentBikeStation.setInstalled(Boolean.valueOf(string));
				} else if (LOCKED.equals(this.currentLocalName)) {
					this.currentBikeStation.setLocked(Boolean.valueOf(string));
				} else if (INSTALL_DATE.equals(this.currentLocalName)) {
					this.currentBikeStation.setInstallDate(Utils.toTimestampInSeconds(Long.valueOf(string)));
				} else if (REMOVAL_DATE.equals(this.currentLocalName)) {
					this.currentBikeStation.setRemovalDate(Utils.toTimestampInSeconds(Long.valueOf(string)));
				} else if (TEMPORARY.equals(this.currentLocalName)) {
					this.currentBikeStation.setTemporary(Boolean.valueOf(string));
				} else if (PUBLIC.equals(this.currentLocalName)) {
					this.currentBikeStation.setPublicStation(Boolean.valueOf(string));
				} else if (NB_BIKES.equals(this.currentLocalName)) {
					this.currentBikeStation.setNbBikes(Integer.valueOf(string));
				} else if (NB_EMPTY_DOCKS.equals(this.currentLocalName)) {
					this.currentBikeStation.setNbEmptyDocks(Integer.valueOf(string));
				} else if (LATEST_UPDATE_TIME.equals(this.currentLocalName)) {
					this.currentBikeStation.setLatestUpdateTime(Utils.toTimestampInSeconds(Long.valueOf(string)));
				}
			} catch (Exception e) {
				MyLog.w(TAG, "Error while parsing '%s'!", e);
			}
		}
		// super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// MyLog.v(TAG, "endElement(%s,%s,%s)", uri, localName, qName);
		if (STATION.equals(localName)) {
			this.bikeStations.add(this.currentBikeStation);
			this.currentBikeStation = null;
		}
	}

	// @Override
	// public void endDocument() throws SAXException {
	// MyLog.v(TAG, "endDocument()");
	// }

	@Override
	public void error(SAXParseException exception) throws SAXException {
		// MyLog.v(TAG, "error()");
		MyLog.e(TAG, exception, "Error while parsing XML!");
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		// MyLog.v(TAG, "fatalError()");
		MyLog.e(TAG, exception, "Fatal error while parsing XML!");
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		// MyLog.v(TAG, "warning()");
		MyLog.w(TAG, exception, "Warning while parsing XML!");
	}

}
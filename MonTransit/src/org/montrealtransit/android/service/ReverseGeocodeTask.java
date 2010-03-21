package org.montrealtransit.android.service;

import java.io.IOException;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

/**
 * This task obtain the GPS location of the location name.
 * @author Mathieu Méa
 */
public class ReverseGeocodeTask extends AsyncTask<String, String, List<Address>> {

	/**
	 * The log tag.
	 */
	private static final String TAG = ReverseGeocodeTask.class.getSimpleName();

	/**
	 * The activity context calling the dialog.
	 */
	private Context context;

	/**
	 * The max number of possible location match to return.
	 */
	private int maxResults;

	/**
	 * This class will process the result.
	 */
	private ReverseGeocodeTaskListener listener;

	/**
	 * Default constructor.
	 * @param context the activity
	 * @param maxResults the max number of results necessary
	 * @param listener the class that will process the result
	 */
	public ReverseGeocodeTask(Context context, int maxResults, ReverseGeocodeTaskListener listener) {
		this.context = context;
		this.maxResults = maxResults;
		this.listener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Address> doInBackground(String... params) {
		MyLog.v(TAG, "doInBackground()");
		try {
			String locationName = params[0];
			publishProgress(this.context.getResources().getString(R.string.reverse_geocoding_1) + " " + locationName
			        + this.context.getResources().getString(R.string.ellipsis));
			MyLog.v(TAG, "Reverse geocode: " + locationName + ".");
			Geocoder geocoder = new Geocoder(this.context);
			return geocoder.getFromLocationName(locationName, this.maxResults);
		} catch (IOException e) {
			MyLog.e(TAG, "INTERNAL ERROR: the network is unavailable or any other I/O problem occurs", e);
		} catch (Exception e) {
			MyLog.e(TAG, "INTERNAL ERROR: unknown problem", e);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onProgressUpdate(String... values) {
		if (values != null && values.length > 0 && values[0] != null) {
			Utils.notifyTheUser(this.context, values[0]);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(List<Address> result) {
		MyLog.v(TAG, "onPostExecute()");
		this.listener.processLocation(result);
	}
}

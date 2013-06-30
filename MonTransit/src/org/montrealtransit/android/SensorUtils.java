package org.montrealtransit.android;

import org.montrealtransit.android.LocationUtils.POI;
import org.montrealtransit.android.api.SupportFactory;
import org.montrealtransit.android.data.Pair;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Surface;
import android.widget.AbsListView.OnScrollListener;

/**
 * This class provide some tools to handle {@link SensorEvent}.
 * @author Mathieu Méa
 */
public final class SensorUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = SensorUtils.class.getSimpleName();

	/**
	 * The shake threshold acceleration.
	 */
	public static final float SHAKE_THRESHOLD_ACCELERATION = 17.00f;

	/**
	 * The shake threshold minimum duration between 2 checks (in milliseconds).
	 */
	public static final int SHAKE_THRESHOLD_PERIOD = 1000; // 1 second

	/**
	 * The minimum degree change for a list view to be updated.
	 */
	public static final int COMPASS_DEGREE_UPDATE_THRESOLD = 18;
	/**
	 * The minimum between 2 {@link #updateCompass(float[])} in milliseconds.
	 */
	public static final int COMPASS_UPDATE_THRESOLD = 150; // 0.15 seconds

	/**
	 * Utility class.
	 */
	private SensorUtils() {
	}

	/**
	 * @see #registerShakeListener(Context, SensorEventListener)
	 * @see #registerCompassListener(Context, SensorEventListener)
	 */
	public static void registerShakeAndCompassListener(Context context, SensorEventListener listener) {
		MyLog.v(TAG, "registerShakeAndCompassListener()");
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, getAccelerometerSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(listener, getMagneticFieldSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * Register the listener for the shake sensor service.
	 * @param context the context
	 * @param listener the listener
	 */
	public static void registerShakeListener(Context context, SensorEventListener listener) {
		MyLog.v(TAG, "registerShakeListener()");
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, getAccelerometerSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * Check for a shake event (and update the last values for next check).
	 * @param values the {@link SensorEvent} values
	 * @param lastSensorUpdate the last sensor updates (in milliseconds)
	 * @param lastAccelIncGravity the last acceleration (including gravity)
	 * @param lastAccel the last acceleration
	 * @param listener the {@link ShakeListener}
	 */
	public static void checkForShake(SensorEvent event, long lastSensorUpdate, float lastAccelIncGravity, float lastAccel, ShakeListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			long now = System.currentTimeMillis();
			if ((now - lastSensorUpdate) > SHAKE_THRESHOLD_PERIOD) {
				lastSensorUpdate = now; // save last update
				float currentAccelIncGravity = extractAcceleration(event.values);
				lastAccel = lastAccel * 0.9f + (currentAccelIncGravity - lastAccelIncGravity); // perform low-cut filter
				if (lastAccel > SHAKE_THRESHOLD_ACCELERATION) {
					listener.onShake();
				}
				// save last sensor acceleration
				lastAccelIncGravity = currentAccelIncGravity;
			}
			break;
		}
	}

	/**
	 * @param values the {@link SensorEvent} values
	 * @return the acceleration (including gravity)
	 */
	public static float extractAcceleration(float[] values) {
		float x = values[0];
		float y = values[1];
		float z = values[2];
		return FloatMath.sqrt((x * x + y * y + z * z));
	}

	/**
	 * The shake listener.
	 */
	public interface ShakeListener {

		/**
		 * Called when a shake is detected.
		 */
		void onShake();
	}

	/**
	 * Register the listener for the compass sensor service.
	 * @param context the context
	 * @param listener the listener
	 */
	public static void registerCompassListener(Context context, SensorEventListener listener) {
		MyLog.v(TAG, "registerCompassListener()");
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, getAccelerometerSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(listener, getMagneticFieldSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * @param mSensorManager the sensor manager
	 * @return the default magnetic field sensor or null
	 */
	public static Sensor getMagneticFieldSensor(SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		// List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		// MyLog.d(TAG, "MagneticFieldSensor: " + list.size());
		// if (list.size() > 0) {
		// return list.get(0);
		// }
		// return null;
	}

	/**
	 * @param mSensorManager the sensor manager
	 * @return the default accelerometer sensor or null
	 */
	public static Sensor getAccelerometerSensor(SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		// List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		// MyLog.d(TAG, "AccelerometerSensor: " + list.size());
		// if (list.size() > 0) {
		// return list.get(0);
		// }
		// return null;
	}

	/**
	 * @param activity the activity
	 * @param currentLocation the current location
	 * @param destinationLocation the destination location
	 * @param orientation the orientation from {@link SensorManager}
	 * @param sourceWidth the image source width
	 * @param sourceHeight the image source height
	 * @return the compass rotation matrix
	 */
	@Deprecated
	public static Matrix getCompassRotationMatrix(Location currentLocation, Location destinationLocation, float orientation, int sourceWidth, int sourceHeight,
			float declination) {
		Matrix matrix = new Matrix();
		matrix.postRotate(
				getCompassRotationInDegree(currentLocation.getLatitude(), currentLocation.getLongitude(), destinationLocation.getLatitude(),
						destinationLocation.getLongitude(), orientation, declination), sourceWidth / 2, sourceHeight / 2);
		return matrix;
	}

	/**
	 * @param activity the activity
	 * @param currentLocation the current device location
	 * @param destinationLocation the destination location
	 * @param orientation the sensor orientation
	 * @param declination align with true north
	 * @return compass rotation in degree
	 */
	public static float getCompassRotationInDegree(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float orientation,
			float declination) {
		return LocationUtils.bearTo(startLatitude, startLongitude, endLatitude, endLongitude) - (orientation + declination);
	}

	public static float getCompassRotationInDegree(Location start, POI poi, float orientation, float declination) {
		return getCompassRotationInDegree(start.getLatitude(), start.getLongitude(), poi.getLat(), poi.getLng(), orientation, declination);
	}

	/**
	 * @param accelerometerValues the {@link Sensor#TYPE_ACCELEROMETER} values
	 * @param magneticFieldValues the {@link Sensor#TYPE_MAGNETIC_FIELD} values
	 * @return the orientation
	 */
	public static float calculateOrientation(Context context, float[] accelerometerValues, float[] magneticFieldValues) {
		if (accelerometerValues == null || magneticFieldValues == null) {
			MyLog.w(TAG, "accelerometer and magnetic field values are required!");
			return 0;
		}
		float[] R = new float[9];
		SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
		float[] outR = new float[9];

		int x_axis = SensorManager.AXIS_X;
		int y_axis = SensorManager.AXIS_Y;
		int rotation = SupportFactory.get().getSurfaceRotation(context);
		switch (rotation) {
		case Surface.ROTATION_0:
			break;
		case Surface.ROTATION_90:
			x_axis = SensorManager.AXIS_Y;
			y_axis = SensorManager.AXIS_MINUS_X;
			break;
		case Surface.ROTATION_180:
			y_axis = SensorManager.AXIS_MINUS_Y;
			break;
		case Surface.ROTATION_270:
			x_axis = SensorManager.AXIS_MINUS_Y;
			y_axis = SensorManager.AXIS_X;
			break;
		}
		SensorManager.remapCoordinateSystem(R, x_axis, y_axis, outR);

		float[] values = new float[3];
		SensorManager.getOrientation(outR, values);

		// Convert from Radians to Degrees.
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);

		return values[0];
	}

	/**
	 * @param activity the activity
	 * @param resId the resource ID
	 * @return the resource dimensions (width, height) in pixel
	 */
	public static Pair<Integer, Integer> getResourceDimension(Activity activity, int resId) {
		Bitmap bitmapOrg = BitmapFactory.decodeResource(activity.getResources(), resId);
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		return new Pair<Integer, Integer>(bitmapOrg.getWidth(), bitmapOrg.getHeight());
	}

	/**
	 * Set the accelerometer and magnetic field values.
	 * @param event the sensor event
	 * @param accelerometerValues the {@link Sensor#TYPE_ACCELEROMETER} values
	 * @param magneticFieldValues the {@link Sensor#TYPE_MAGNETIC_FIELD} values
	 * @param listener the listener
	 * @deprecated not working yet
	 */
	@Deprecated
	public static void checkForCompass(SensorEvent event, float[] accelerometerValues, float[] magneticFieldValues, CompassListener listener) {
		// MyLog.v(TAG, "checkForCompass(%s,%s,%s)", event.sensor.getType(), accelerometerValues, magneticFieldValues);
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerValues = event.values;
			if (magneticFieldValues != null) {
				listener.onCompass();
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magneticFieldValues = event.values;
			if (accelerometerValues != null) {
				listener.onCompass();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * The compass listener.
	 */
	public interface CompassListener {

		/**
		 * Called when the compass change.
		 */
		void onCompass();
	}

	/**
	 * Unregister the listener for the context sensor service.
	 * @param context the context
	 * @param listener the listener
	 */
	public static void unregisterSensorListener(Context context, SensorEventListener listener) {
		MyLog.v(TAG, "unregisterSensorListener()");
		((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(listener);
	}

	@Deprecated
	public static void registerOrientationListener(Context context, SensorEventListener listener) {
		MyLog.v(TAG, "registerOrientationListener()");
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
	}

	@Deprecated
	public static Matrix getRotationMatrixFromOrientation(Activity activity, Location currentLocation, Location destinationLocation, float[] orientation,
			int sourceWidth, int sourceHeight) {
		float bearTo = currentLocation.bearingTo(destinationLocation);
		float orientationFix = SupportFactory.get().getDisplayRotation(activity);
		float declination = new GeomagneticField(Double.valueOf(currentLocation.getLatitude()).floatValue(), Double.valueOf(currentLocation.getLongitude())
				.floatValue(), Double.valueOf(currentLocation.getAltitude()).floatValue(), System.currentTimeMillis()).getDeclination();
		float rotationDegrees = bearTo - (orientation[0] + declination) + orientationFix;
		Matrix matrix = new Matrix();
		matrix.postRotate(rotationDegrees, sourceWidth / 2, sourceHeight / 2);
		return matrix;
	}

	@Deprecated
	public static void checkForOrientation(SensorEvent event, float[] orientationFieldValues, CompassListener listener) {
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			orientationFieldValues = event.values;
			listener.onCompass();
		}
	}

	// public static void updateCompass(final boolean force, final Location currentLocation, final float orientation, final long now, final int scrollState,
	// final long lastCompassChanged, final int lastCompassInDegree, final SensorTaskCompleted callback) {
	// if (currentLocation == null /* || poi == null */|| orientation == 0) {
	// // MyLog.d(TAG, "updateCompass() > no location (%s) or no POI (%s) or no orientation (%s)", (currentLocation == null), (poi == null), orientation);
	// callback.onSensorTaskCompleted(false);
	// return;
	// }
	// if (!force) {
	// // if (io == 0) {
	// // // MyLog.d(TAG, "updateCompass() > no orientation");
	// // callback.onSensorTaskCompleted(false);
	// // return;
	// // }
	// if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
	// // MyLog.d(TAG, "updateCompass() > scrolling");
	// callback.onSensorTaskCompleted(false);
	// return;
	// }
	// final boolean tooSoon = (now - lastCompassChanged) <= COMPASS_UPDATE_THRESOLD;
	// if (tooSoon) {
	// // MyLog.d(TAG, "updateCompass() > too soon");
	// callback.onSensorTaskCompleted(false);
	// return;
	// }
	// final boolean notDifferentEnough = Math.abs(lastCompassInDegree - orientation) <= COMPASS_DEGREE_UPDATE_THRESOLD;
	// if (notDifferentEnough) {
	// // MyLog.d(TAG, "updateCompass() > not different enough");
	// callback.onSensorTaskCompleted(false);
	// return;
	// }
	// }
	// callback.onSensorTaskCompleted(true);
	// // new AsyncTask<Void, Void, Boolean>() {
	// //
	// // @Override
	// // protected Boolean doInBackground(Void... params) {
	// // return updateCompass(activity, poi, force, currentLocation, orientation, now, scrollState, lastCompassChanged, lastCompassInDegree,
	// // headingArrowResId);
	// // }
	// //
	// // @Override
	// // protected void onPostExecute(Boolean result) {
	// // callback.onSensorTaskCompleted(result);
	// // }
	// // }.execute();
	// }

	public static void updateCompass(final boolean force, final Location currentLocation, final float orientation, final long now, final int scrollState,
			final long lastCompassChanged, final int lastCompassInDegree, final SensorTaskCompleted callback) {
		if (currentLocation == null /* || pois == null */|| orientation == 0) {
			// MyLog.d(TAG, "updateCompass() > no location (%s) or no POI (%s) or no orientation (%s)", (currentLocation == null), (pois == null), orientation);
			callback.onSensorTaskCompleted(false);
			return;
		}
		if (!force) {
			// if (io == 0) {
			// // MyLog.d(TAG, "updateCompass() > no orientation");
			// callback.onSensorTaskCompleted(false);
			// return;
			// }
			if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				// MyLog.d(TAG, "updateCompass() > scrolling");
				callback.onSensorTaskCompleted(false);
				return;
			}
			final boolean tooSoon = (now - lastCompassChanged) <= COMPASS_UPDATE_THRESOLD;
			if (tooSoon) {
				// MyLog.d(TAG, "updateCompass() > too soon");
				callback.onSensorTaskCompleted(false);
				return;
			}
			final boolean notDifferentEnough = Math.abs(lastCompassInDegree - orientation) <= COMPASS_DEGREE_UPDATE_THRESOLD;
			if (notDifferentEnough) {
				// MyLog.d(TAG, "updateCompass() > not different enough");
				callback.onSensorTaskCompleted(false);
				return;
			}
		}
		callback.onSensorTaskCompleted(true);
		// new AsyncTask<Void, Void, Boolean>() {
		//
		// @Override
		// protected Boolean doInBackground(Void... params) {
		// return updateCompass(activity, pois, force, currentLocation, orientation, now, scrollState, lastCompassChanged, lastCompassInDegree,
		// headingArrowResId);
		// }
		//
		// @Override
		// protected void onPostExecute(Boolean result) {
		// callback.onSensorTaskCompleted(result);
		// }
		// }.execute();
	}

	// public static boolean updateCompass(Activity activity, List<? extends POI> pois, boolean force, Location currentLocation, float orientation, long now,
	// int scrollState, long lastCompassChanged, int lastCompassInDegree, int headingArrowResId) {
	// if (currentLocation == null || pois == null || orientation == 0) {
	// // MyLog.d(TAG, "updateCompass() > no location (%s) or no POI (%s) or no orientation (%s)", (currentLocation== null), (pois == null), orientation);
	// return false;
	// }
	// int io = (int) orientation;
	// if (!force) {
	// // if (io == 0) {
	// // // MyLog.d(TAG, "updateCompass() > no orientation");
	// // return false;
	// // }
	// if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
	// // MyLog.d(TAG, "updateCompass() > scrolling");
	// }
	// final boolean tooSoon = (now - lastCompassChanged) <= COMPASS_UPDATE_THRESOLD;
	// if (tooSoon) {
	// // MyLog.d(TAG, "updateCompass() > too soon");
	// return false;
	// }
	// final boolean notDifferentEnough = Math.abs(lastCompassInDegree - io) <= COMPASS_DEGREE_UPDATE_THRESOLD;
	// if (notDifferentEnough) {
	// // MyLog.d(TAG, "updateCompass() > not different enough");
	// return false;
	// }
	// }
	// float locationDeclination = new GeomagneticField((float) currentLocation.getLatitude(), (float) currentLocation.getLongitude(),
	// (float) currentLocation.getAltitude(), currentLocation.getTime()).getDeclination();
	// Pair<Integer, Integer> arrowDim = SensorUtils.getResourceDimension(activity, headingArrowResId);
	// // update closest bike stations compass
	// for (POI poi : pois) {
	// poi.getCompassMatrix().reset();
	// final float compassRotationInDegree = getCompassRotationInDegree(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(),
	// poi.getLng(), orientation, locationDeclination);
	// poi.getCompassMatrix().postRotate(compassRotationInDegree, arrowDim.first / 2, arrowDim.second / 2);
	// }
	// return true;
	// }

	// public static boolean updateCompass(Activity activity, POI poi, boolean force, Location currentLocation, float orientation, long now, int scrollState,
	// long lastCompassChanged, int lastCompassInDegree, int headingArrowResId) {
	// if (currentLocation == null || poi == null || orientation == 0) {
	// // MyLog.d(TAG, "updateCompass() > no location (%s) or no POI (%s) or no orientation (%s)", (currentLocation== null), (pois == null), orientation);
	// return false;
	// }
	// int io = (int) orientation;
	// if (!force) {
	// // if (io == 0) {
	// // // MyLog.d(TAG, "updateCompass() > no orientation");
	// // return false;
	// // }
	// if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
	// // MyLog.d(TAG, "updateCompass() > scrolling");
	// }
	// final boolean tooSoon = (now - lastCompassChanged) <= COMPASS_UPDATE_THRESOLD;
	// if (tooSoon) {
	// // MyLog.d(TAG, "updateCompass() > too soon");
	// return false;
	// }
	// final boolean notDifferentEnough = Math.abs(lastCompassInDegree - io) <= COMPASS_DEGREE_UPDATE_THRESOLD;
	// if (notDifferentEnough) {
	// // MyLog.d(TAG, "updateCompass() > not different enough");
	// return false;
	// }
	// }
	// float locationDeclination = new GeomagneticField((float) currentLocation.getLatitude(), (float) currentLocation.getLongitude(),
	// (float) currentLocation.getAltitude(), currentLocation.getTime()).getDeclination();
	// Pair<Integer, Integer> arrowDim = SensorUtils.getResourceDimension(activity, headingArrowResId /* R.drawable.heading_arrow */);
	// // update closest bike stations compass
	// poi.getCompassMatrix().reset();
	// poi.getCompassMatrix().postRotate(
	// getCompassRotationInDegree(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng(), orientation,
	// locationDeclination), arrowDim.first / 2, arrowDim.second / 2);
	// return true;
	// }

	public interface SensorTaskCompleted {
		void onSensorTaskCompleted(boolean result);
	}

	private static int rotationPx = -1;
	private static int rotationPy = -1;

	public static int getRotationPx(Activity activity) {
		if (rotationPx < 0) {
			initRotation(activity);
		}
		return rotationPx;
	}

	public static int getRotationPy(Activity activity) {
		if (rotationPy < 0) {
			initRotation(activity);
		}
		return rotationPy;
	}

	private static void initRotation(Activity activity) {
		MyLog.v(TAG, "initRotation()");
		Pair<Integer, Integer> arrowDim = getResourceDimension(activity, R.drawable.heading_arrow);
		rotationPx = arrowDim.first / 2;
		rotationPy = arrowDim.second / 2;
	}

	public static float getLocationDeclination(Location location) {
		return new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(), (float) location.getAltitude(), location.getTime())
				.getDeclination();
	}

}

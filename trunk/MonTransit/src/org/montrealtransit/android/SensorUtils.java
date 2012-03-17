package org.montrealtransit.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * This class provide some tools to handle {@link SensorEvent}.
 * @author Mathieu Méa
 */
public final class SensorUtils {

	/**
	 * The shake threshold acceleration.
	 */
	public static final float SHAKE_THRESHOLD_ACCELERATION = 10.00f;

	/**
	 * The shake threshold minimum duration between 2 checks (in milliseconds).
	 */
	public static final int SHAKE_THRESHOLD_PERIOD = 1000; // 1 second

	/**
	 * Utility class.
	 */
	private SensorUtils() {
	}

	/**
	 * Register the listener for the context sensor service.
	 * @param context the context
	 * @param listener the listener
	 */
	public static void registerShakeListener(Context context, SensorEventListener listener) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * Unregister the listener for the context sensor service.
	 * @param context the context
	 * @param listener the listener
	 */
	public static void unregisterShakeListener(Context context, SensorEventListener listener) {
		((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(listener);
	}

	/**
	 * Check for a shake event (and update the last values for next check).
	 * @param values the {@link SensorEvent} values
	 * @param lastSensorUpdate the last sensor updates (in milliseconds)
	 * @param lastAccelIncGravity the last acceleration (including gravity)
	 * @param lastAccel the last acceleration
	 * @param listener the {@link ShakeListener}
	 */
	public static void checkForShake(float[] values, long lastSensorUpdate, float lastAccelIncGravity, float lastAccel, ShakeListener listener) {
		long now = System.currentTimeMillis();
		if ((now - lastSensorUpdate) > SHAKE_THRESHOLD_PERIOD) {
			lastSensorUpdate = now; // save last update
			float currentAccelIncGravity = extractAcceleration(values);
			lastAccel = lastAccel * 0.9f + (currentAccelIncGravity - lastAccelIncGravity); // perform low-cut filter
			if (lastAccel > SHAKE_THRESHOLD_ACCELERATION) {
				listener.onShake();
			}
			// save last sensor acceleration
			lastAccelIncGravity = currentAccelIncGravity;
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
		return (float) Math.sqrt((double) (x * x + y * y + z * z));
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
}

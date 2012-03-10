package org.montrealtransit.android;

import java.math.BigDecimal;
import java.util.Locale;

import android.content.Context;

import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalPayment;

/**
 * This class contains useful methods to interact with the PayPal API.
 * @author Mathieu Méa
 */
public class PayPalUtils {

	/**
	 * The log tag.
	 */
	private static final String TAG = PayPalUtils.class.getSimpleName();

	/**
	 * The PayPal object.
	 */
	private static PayPal instance;

	/**
	 * @param context the context
	 * @return the PayPal object (initialize if necessary)
	 */
	public static PayPal getInstance(Context context) {
		if (instance == null) {
			MyLog.v(TAG, "initWithAppID()");
			instance = PayPal.initWithAppID(context, context.getString(R.string.paypal_app_id), PayPal.ENV_LIVE);
			MyLog.v(TAG, "initWithAppID... DONE");
			AnalyticsUtils.dispatch(context); // while we are connected, send the analytics data
			instance.setShippingEnabled(false);
			instance.setLanguage(Locale.getDefault().toString()); // en_CA / fr_CA
			// TODO check that language is PayPal valid
		}
		return instance;
	}

	/**
	 * The PayPal dialog request code.
	 */
	public static final int paypalRequestCode = 10;

	/**
	 * @param context the context
	 * @param devName the developer name
	 * @param devEmail the developer email
	 * @param amount the amount
	 * @return the PayPal payment object
	 */
	public static PayPalPayment getNewPayPalPayment(Context context, String devName, String devEmail, double amount) {
		MyLog.v(TAG, "getNewPayPalPayment(%s)", amount);
		PayPalPayment newPayment = new PayPalPayment();
		newPayment.setSubtotal(new BigDecimal(amount)); // 10.00
		newPayment.setCurrencyType("CAD"); // Canadian Dollars
		newPayment.setMerchantName(String.format(context.getString(R.string.donate_montransit_dev), devName));
		newPayment.setRecipient(devEmail);
		return newPayment;
	}

	/**
	 * @param context the context
	 * @return the PayPal checkout button
	 */
	public static CheckoutButton getCheckoutButton(Context context) {
		MyLog.v(TAG, "getCheckoutButton()");
		AnalyticsUtils.dispatch(context); // while we are connected, send the analytics data
		return getInstance(context).getCheckoutButton(context, PayPal.BUTTON_294x45, CheckoutButton.TEXT_PAY);
	}

}

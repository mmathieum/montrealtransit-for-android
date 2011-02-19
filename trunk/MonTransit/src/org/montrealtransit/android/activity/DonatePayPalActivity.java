package org.montrealtransit.android.activity;

import org.montrealtransit.android.AnalyticsUtils;
import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.PayPalUtils;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPalActivity;
import com.paypal.android.MEP.PayPalPayment;

/**
 * The activity showing the options to donate using the PayPal API.
 * @author Mathieu Méa
 */
public class DonatePayPalActivity extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = DonatePayPalActivity.class.getSimpleName();
	/**
	 * The tracker tag.
	 */
	private static final String TRACKER_TAG = "/DonatePayPal";

	/**
	 * The activity content.
	 */
	private LinearLayout content;

	/**
	 * The cancel button.
	 */
	private Button cancelBt;

	/**
	 * The progress dialog used while loading the PayPal checkout button.
	 */
	private ProgressDialog progressDialog;

	/**
	 * The donation amount spinner.
	 */
	public Spinner amountSpinner;

	/**
	 * The list of available amount (matching the spinner texts).
	 */
	private static final float[] amounts = new float[] { 3.00f, 5.50f, 14.25f, 22.50f, 22.00f, 72.75f };
	/**
	 * The list of the developers name.
	 */
	private static final int[] devsNames = new int[] { R.string.paypal_recipient_name_mathieu_mea };
	/**
	 * The list of the developers email.
	 */
	private static final int[] devsEmails = new int[] { R.string.paypal_recipient_email_mathieu_mea };

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		// load PayPal button
		this.progressDialog = ProgressDialog.show(this, "PayPal", getString(R.string.donate_loading_paypal), true);
		new LoadPayPalButtonTask(this).execute();
	}

	/**
	 * Finish the create of the activity with the PayPal button.
	 */
	private void onCreateAfterLoading() {
	    // set the UI
		setContentView(R.layout.donate_paypal_activity);
		// get UI elements
		this.content = (LinearLayout) findViewById(R.id.content);
		this.cancelBt = (Button) findViewById(R.id.cancel_bt);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreateAfterLoadingPreDonut();
		}
    }

	/**
	 * {@link DonateActivity#onCreateAfterLoadingPreDonut(Bundle)} additional code for devices running Android version < 1.6
	 */
	private void onCreateAfterLoadingPreDonut() {
		// since 'android:onClick' requires API Level 4
		this.cancelBt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel(v);
			}
		});
	}

	/**
	 * Show the PayPal button.
	 * @param button
	 */
	private void showPayPalButtonDialog(CheckoutButton button) {
		this.progressDialog.dismiss();
		// finish loading the UI component
		onCreateAfterLoading();
		// add the text message
		TextView paypalMsgTv = new TextView(this, null, android.R.attr.textAppearanceLarge);
		paypalMsgTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		paypalMsgTv.setText(R.string.donate_paypal_button_message);
		this.content.addView(paypalMsgTv);
		// add an empty space
		View view = new View(this);
		view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
		this.content.addView(view);
		// add amount text view + spinner
		TextView amountTv = new TextView(this, null, android.R.attr.textAppearanceMedium);
		amountTv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		amountTv.setText(R.string.donate_amount);
		this.content.addView(amountTv);
		this.amountSpinner = new Spinner(this);
		this.amountSpinner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.amountSpinner.setPrompt(getString(R.string.donate_select_amount));
		ArrayAdapter<CharSequence> amountAdapter = ArrayAdapter.createFromResource(DonatePayPalActivity.this,
		        R.array.donate_amounts, android.R.layout.simple_spinner_item);
		amountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.amountSpinner.setAdapter(amountAdapter);
		this.content.addView(this.amountSpinner);
		// add PayPal button
		//button.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.content.addView(button);
		// add empty space
		View view2 = new View(this);
		view2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
		this.content.addView(view2);

		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MyLog.v(TAG, "onClick()");
				// amount
				final float amount = amounts[amountSpinner.getSelectedItemPosition()];
				// // dev
				final String devName = getString(devsNames[0]); // TODO when multiple devs
				final String devEmail = getString(devsEmails[0]);// TODO when multiple devs
				Utils.notifyTheUser(DonatePayPalActivity.this,
				        DonatePayPalActivity.this.getString(R.string.donate_loading_paypal));
				PayPalPayment payPalPayment = PayPalUtils.getNewPayPalPayment(DonatePayPalActivity.this, devName,
				        devEmail, amount);
				Intent paypalIntent = PayPalUtils.getInstance(DonatePayPalActivity.this).checkout(payPalPayment,
				        DonatePayPalActivity.this);
				DonatePayPalActivity.this.startActivityForResult(paypalIntent, PayPalUtils.paypalRequestCode);
				// DonatePayPalActivity.this.buttonDialog.dismiss(); // close the dialog
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		MyLog.v(TAG, "onResume()");
		AnalyticsUtils.trackPageView(this, TRACKER_TAG);
	    super.onResume();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		MyLog.v(TAG, "onActivityResult(%s, %s)", requestCode, resultCode);
		switch (requestCode) {
		case PayPalUtils.paypalRequestCode:
			switch (resultCode) {
			case Activity.RESULT_OK:
				MyLog.v(TAG, "Paypal payment succeeded");
				// String payKey = data.getStringExtra(PayPalActivity.EXTRA_PAY_KEY);
				// Tell the user their payment succeeded
				Utils.notifyTheUser(this, this.getString(R.string.donate_payment_completed));
				this.finish();
				break;
			case Activity.RESULT_CANCELED:
				MyLog.v(TAG, "PayPal payment was cancelled.");
				// Tell the user their payment was canceled
				Utils.notifyTheUser(this, this.getString(R.string.donate_payment_cancelled));
				break;
			case PayPalActivity.RESULT_FAILURE:
				MyLog.v(TAG, "Paypal: failure.");
				String errorID = data.getStringExtra(PayPalActivity.EXTRA_ERROR_ID);
				String errorMessage = data.getStringExtra(PayPalActivity.EXTRA_ERROR_MESSAGE);
				MyLog.w(TAG, "PayPal Error Id: " + errorID);
				MyLog.w(TAG, "PayPal Error Message: " + errorMessage);
				// Tell the user their payment was failed.
				Utils.notifyTheUser(this, this.getString(R.string.donate_payment_failure));
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	/**
	 * Cancel the PayPal donation (close the activity).
	 * @param v not used
	 */
	public void cancel(View v) {
		this.finish();
	}

	/**
	 * This task load the PayPal button.
	 * @author Mathieu Méa
	 */
	public class LoadPayPalButtonTask extends AsyncTask<String, String, CheckoutButton> {

		/**
		 * The context.
		 */
		private Context context;

		/**
		 * Default constructor.
		 * @param context the context
		 */
		public LoadPayPalButtonTask(Context context) {
			this.context = context;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected CheckoutButton doInBackground(String... params) {
			MyLog.d(TAG, "Getting checkout button...");
			return PayPalUtils.getCheckoutButton(this.context);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void onPostExecute(CheckoutButton result) {
			super.onPostExecute(result);
			MyLog.d(TAG, "Getting checkout button... DONE");
			showPayPalButtonDialog(result);
		}
	}
}

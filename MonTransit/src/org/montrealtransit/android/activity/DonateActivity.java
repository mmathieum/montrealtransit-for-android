package org.montrealtransit.android.activity;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;
import org.montrealtransit.android.Utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

/**
 * The first donate activity.
 * Currently giving choice of payment method (Android Market / PayPal).
 * May let the user choose the developer when multiple developers.
 * @author Mathieu Méa
 */
public class DonateActivity extends Activity {

	/**
	 * The log tag.
	 */
	private static final String TAG = DonateActivity.class.getSimpleName();

	/**
	 * The payment method spinner.
	 */
	public Spinner methodSpinner;
	/**
	 * The next button.
	 */
	public Button nextBt;

	/**
	 * The cancel button.
	 */
	private Button cancelBt;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		MyLog.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		// set the UI
		setContentView(R.layout.donate_activity);
		// get UI elements
		this.methodSpinner = (Spinner) findViewById(R.id.method_spinner);
		this.nextBt = (Button) findViewById(R.id.next_bt);
		this.cancelBt = (Button) findViewById(R.id.back_bt);

		if (Utils.isVersionOlderThan(Build.VERSION_CODES.DONUT)) {
			onCreatePreDonut();
		}

		// set up the payment methods spinner
		ArrayAdapter<CharSequence> methodAdapter = ArrayAdapter.createFromResource(
		        DonateActivity.this, R.array.donate_methods, android.R.layout.simple_spinner_item);
		methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.methodSpinner.setAdapter(methodAdapter);
	}
	
	/**
	 * {@link DonateActivity#onCreate(Bundle)} additional code for devices running Android version < 1.6
	 */
	private void onCreatePreDonut() {
		// since 'android:onClick' requires API Level 4
		this.cancelBt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancel(v);
			}
		});
		this.nextBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				next(v);
			}
		});
	}

	/**
	 * Show the next step depending on the selected method payment.
	 * @param v not used
	 */
	public void next(View v) {
		// method
		switch (methodSpinner.getSelectedItemPosition()) {
		case 0:
			// Android Market
			Utils.notifyTheUser(DonateActivity.this, DonateActivity.this.getString(R.string.donate_opening_market));
			Uri appMarketURI = Uri.parse("market://search?q=pub:\"Mathieu Méa\"");
			Intent appMarketIntent = new Intent(Intent.ACTION_VIEW).setData(appMarketURI);
			DonateActivity.this.startActivity(appMarketIntent);
			break;
		case 1:
			// PayPal
			Intent intent = new Intent(DonateActivity.this, DonatePayPalActivity.class);
			DonateActivity.this.startActivity(intent);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Cancel the donation (close the activity).
	 * @param v not used
	 */
	public void cancel(View v) {
		this.finish();
	}
}

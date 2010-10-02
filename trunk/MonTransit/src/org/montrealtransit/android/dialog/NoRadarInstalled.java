package org.montrealtransit.android.dialog;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;

/**
 * This dialog is used to ask the user to install an activity capable of displaying a radar (compass).
 * @author Mathieu Méa
 */
public class NoRadarInstalled implements OnClickListener {

	/**
	 * The log tag.
	 */
	private static final String TAG = NoRadarInstalled.class.getSimpleName();

	/**
	 * The activity context calling the dialog.
	 */
	private Context context;

	/**
	 * Default constructor.
	 * @param context the caller context
	 */
	public NoRadarInstalled(Context context) {
		this.context = context;
	}

	/**
	 * Actually show the dialog.
	 */
	public void showDialog() {
		getAlertDialog().show();
	}

	/**
	 * Return the alert dialog.
	 * @return the alert dialog
	 */
	private AlertDialog getAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
		builder.setTitle(R.string.no_radar_title);
		builder.setIcon(android.R.drawable.ic_dialog_info);
		String[] items = { this.context.getString(R.string.download_radar_app),
		        this.context.getString(R.string.search_for_other_radar_app) };
		builder.setItems(items, this);
		builder.setNegativeButton(R.string.cancel, this);
		AlertDialog alert = builder.create();
		return alert;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		MyLog.v(TAG, "onClick(" + which + ")");
		switch (which) {
		case -2:
			dialog.dismiss(); // CANCEL
			break;
		case 0:
			// install the free radar library com.google.android.radar
			Uri radarAppMarketUri = Uri.parse("market://search?q=pname:com.google.android.radar");
			Intent radarAppMarketIntent = new Intent(Intent.ACTION_VIEW).setData(radarAppMarketUri);
			this.context.startActivity(radarAppMarketIntent);
			break;
		case 1:
			// search for another radar app
			Uri radarSearchMarketUri = Uri.parse("market://search?q=radar");
			Intent radarSearchMarketIntent = new Intent(Intent.ACTION_VIEW).setData(radarSearchMarketUri);
			this.context.startActivity(radarSearchMarketIntent);
			break;
		default:
			break;
		}
	}
}

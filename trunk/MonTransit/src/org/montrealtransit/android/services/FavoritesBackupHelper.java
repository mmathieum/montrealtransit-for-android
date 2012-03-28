package org.montrealtransit.android.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.montrealtransit.android.MyLog;
import org.montrealtransit.android.Utils;
import org.montrealtransit.android.activity.UserPreferences;
import org.montrealtransit.android.provider.DataManager;
import org.montrealtransit.android.provider.DataStore.Fav;

import android.annotation.TargetApi;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;

/**
 * The {@link Fav} backup helper.
 * @author Mathieu Méa
 */
@TargetApi(8)
public class FavoritesBackupHelper implements BackupHelper {

	/**
	 * The log tag.
	 */
	private static final String TAG = FavoritesBackupHelper.class.getSimpleName();

	/**
	 * The favorites entity header.
	 */
	private static final String FAVS_ENTITY = "B_favs";

	/**
	 * The context.
	 */
	private Context context;
	/**
	 * The list of current favorites.
	 */
	private List<Fav> currentFavs;

	/**
	 * The default constructor.
	 * @param context
	 */
	public FavoritesBackupHelper(Context context) {
		MyLog.v(TAG, "FavoritesBackupHelper()");
		this.context = context;
	}

	@Override
	public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
		MyLog.v(TAG, "performBackup()");
		MyLog.v(TAG, "Checking if favorites(s) backup is necessary...");
		// load the current favorites
		loadCurrentFavorites();
		// check if a backup is needed
		// MyLog.d(TAG, oldState == null ? "No old state, do backup." : "Old state, comparing state...");
		boolean doBackup = (oldState == null); // no state => backup
		if (!doBackup) {
			doBackup = stateChanged(oldState); // state changed => backup
			// MyLog.d(TAG, doBackup ? "State changed." : "Same state.");
		}
		// IF the backup is needed DO
		if (doBackup) {
			MyLog.v(TAG, "Favorites(s) backup necessary.");
			try {
				backup(data);
				MyLog.i(TAG, "Favorites(s) backup completed.");
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while performing the Favorite(s) backup!", ioe);
				return; // don't save current status
			}
		}
		// write the state of the backup
		writeNewStateDescription(newState);
	}

	@Override
	public void restoreEntity(BackupDataInputStream data) {
		MyLog.v(TAG, "restoreEntity(%s,%s)", data.getKey(), data.size());
		try {
			if (!FAVS_ENTITY.equals(data.getKey())) {
				try {// still read it to fix Backup Agent error
					readData(data); // TODO still necessary?
				} catch (IOException ioe) {
					MyLog.v(TAG, "Error while reading the backup data to restore!", ioe);
				}
				MyLog.d(TAG, "Unknown backup entity '" + data.getKey() + "'!");
				return;
			}
			MyLog.v(TAG, "Restoring Favorite(s) from backup...");
			String dataS = "";
			try {
				dataS = readData(data);
			} catch (IOException ioe) {
				MyLog.w(TAG, "Error while reading the backup Favorite(s) to restore!", ioe);
				return;
			}
			loadCurrentFavorites();
			// MyLog.d(TAG, "dataS: " + dataS);
			restore(dataS);
			MyLog.i(TAG, "Favorite(s) restored from backup.");
		} catch (Throwable t) {
			MyLog.w(TAG, "Unkown error while restoring backup!", t);
		}
	}

	@Override
	public void writeNewStateDescription(ParcelFileDescriptor newState) {
		MyLog.v(TAG, "writeNewStateDescription()");
		try {
			MyLog.v(TAG, "Saving backup state...");
			writeStateFile(newState);
			MyLog.v(TAG, "Backup state saved.");
		} catch (Exception e) {
			MyLog.w(TAG, "Failed to write new backup state!", e);
		}
	}

	/**
	 * Read data from the input stream
	 * @param data the input stream
	 * @return the data
	 * @throws IOException I/O error
	 */
	private String readData(BackupDataInputStream data) throws IOException {
		String dataS;
		byte[] buf = new byte[data.size()];
		data.read(buf, 0, buf.length);
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		DataInputStream dis = new DataInputStream(bais);
		dataS = dis.readUTF();
		dis.close();
		bais.close();
		return dataS;
	}

	/**
	 * Loads the current favorites.
	 */
	private void loadCurrentFavorites() {
		MyLog.v(TAG, "loadCurrentFavorites()");
		this.currentFavs = DataManager.findAllFavsList(context.getContentResolver());
	}

	/**
	 * Backup the current favorites.
	 * @param data the output data
	 * @throws IOException I/O error
	 */
	private void backup(BackupDataOutput data) throws IOException {
		MyLog.v(TAG, "backup()");
		byte[] allFavs = serializeAllFavs().toByteArray();
		// MyLog.d(TAG, "backup byte length: " + allFavs.length);
		data.writeEntityHeader(FAVS_ENTITY, allFavs.length);
		data.writeEntityData(allFavs, allFavs.length);
	}

	/**
	 * Restore the backup Favorite(s)
	 * @param dataS the backup Favorite(s)
	 */
	private void restore(String dataS) {
		List<Fav> restoredFavs = Fav.extractFavs(dataS);
		// remove old favorites
		if (this.currentFavs != null) {
			for (Fav fav : this.currentFavs) {
				if (!Fav.contains(restoredFavs, fav)) {
					DataManager.deleteFav(this.context.getContentResolver(), fav.getId());
				}
			}
		}
		// add restored favorites
		for (Fav fav : restoredFavs) {
			if (!Fav.contains(this.currentFavs, fav)) {
				DataManager.addFav(this.context.getContentResolver(), fav);
			}
		}
		UserPreferences.savePrefLcl(context, UserPreferences.PREFS_LCL_IS_FAV, restoredFavs.size() > 0);
	}

	/**
	 * Serialize the current favorites.
	 * @return the output stream with the current favorites
	 * @throws IOException I/O error
	 */
	private ByteArrayOutputStream serializeAllFavs() throws IOException {
		MyLog.v(TAG, "serializeAllFavs()");
		ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
		DataOutputStream outWriter = new DataOutputStream(bufStream);
		outWriter.writeUTF(Fav.serializeFavs(this.currentFavs));
		return bufStream;
	}

	/**
	 * Write state file of the current favorites.
	 * @param stateFile the state file
	 * @throws IOException I/O error
	 */
	private void writeStateFile(ParcelFileDescriptor stateFile) throws IOException {
		MyLog.v(TAG, "writeStateFile()");
		FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
		DataOutputStream out = new DataOutputStream(outstream);
		out.writeUTF(Fav.serializeFavs(this.currentFavs));
	}

	/**
	 * Compare the old state at the current list of favorites.
	 * @param oldState the old state from {@link BackupHelper#performBackup(ParcelFileDescriptor, BackupDataOutput, ParcelFileDescriptor)}
	 * @return true if the old state favorites are exactly the same as the current favorites, false otherwise.
	 */
	private boolean stateChanged(ParcelFileDescriptor oldState) {
		MyLog.v(TAG, "stateChanged()");
		if (oldState == null) {
			return true;
		}
		List<Fav> oldFavs = extractFavs(new DataInputStream(new FileInputStream(oldState.getFileDescriptor())));
		// MyLog.d(TAG, "oldFavs: " + Utils.getListSize(oldFavs));
		// MyLog.d(TAG, "currentFavs: " + Utils.getListSize(this.currentFavs));
		// IF old favorites number != current favorites number DO
		if (Utils.getCollectionSize(oldFavs) != Utils.getCollectionSize(this.currentFavs)) {
			return true; // different size
		}
		// same size
		if (Utils.getCollectionSize(this.currentFavs) == 0) {
			return false; // no favorite
		}
		for (Fav oldFav : oldFavs) {
			if (!Fav.contains(this.currentFavs, oldFav)) {
				return true; // new favorite
			}
		}
		return false;
	}

	/**
	 * @param oldSateDIS the data input stream containing favorites.
	 * @return the favorites from data input stream OR an empty list
	 */
	private List<Fav> extractFavs(DataInputStream oldSateDIS) {
		MyLog.v(TAG, "extractFavs()");
		try {
			return Fav.extractFavs(oldSateDIS.readUTF());
		} catch (IOException ioe) {
			return new ArrayList<Fav>();
		}
	}
}

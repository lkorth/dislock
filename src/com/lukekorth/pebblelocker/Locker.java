package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class Locker {
	
	private Context mContext;
	private SharedPreferences mPrefs;
	private Logger mLogger;
	private String mUniq;
	private DevicePolicyManager mDPM;
	
	public Locker(Context context, String tag) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context);
		mUniq = tag;
		mDPM = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}
	
	public boolean isActiveAdmin() {
		return mDPM.isAdminActive(new ComponentName(mContext, CustomDeviceAdminReceiver.class));
	}
	
	public void lockIfEnabled() {
		lockIfEnabled(true);
	}

	public void lockIfEnabled(boolean forceLock) {
		if (isActiveAdmin()) {
			if (mPrefs.getBoolean("key_enable_locker", false)) {
				if (!connectedToDeviceOrWifi()) {
					mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).commit();
					
					mLogger.log(mUniq, "Locked!");

					if (forceLock && mPrefs.getBoolean("key_force_lock", false))
						mDPM.lockNow();
				}
			} else {
				mLogger.log(mUniq, "key_enable_locker is false");
			}
		} else {
			mLogger.log(mUniq, "Not an active admin");
		}
	}

	public void unlockIfEnabled() {
		if (isActiveAdmin()) {
			if (mPrefs.getBoolean("key_enable_locker", false)) {
				if(connectedToDeviceOrWifi()) {
					mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, false).commit();
					
					mLogger.log(mUniq, "Unlocked!");	
				}
			} else {
				mLogger.log(mUniq, "key_enable_locker is false");
			}
		} else {
			mLogger.log(mUniq, "Not an active admin");
		}
	}
	
	private boolean connectedToDeviceOrWifi() {		
		boolean pebble = false;
		boolean bluetooth = false;
		boolean wifi = false;
		
		if(mPrefs.getBoolean("pebble", true))
			pebble = isWatchConnected();
		else
			mLogger.log(mUniq, "Unlock via any Pebble is not enabled");

		String bluetoothAddress = mPrefs.getString("bluetooth", "");
		mLogger.log(mUniq, "Connected bluetooth address: " + bluetoothAddress);
		if(mPrefs.getBoolean(bluetoothAddress, false))
			bluetooth = true;

		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if(wifiInfo != null) {
			if(wifiInfo.getSSID() != null) {
				if(mPrefs.getBoolean(Base64.encodeToString(wifiInfo.getSSID().getBytes(), Base64.DEFAULT).trim(), false))
					wifi = true;
			} else {
				mLogger.log(mUniq, "wifiInfo.getSSID is null");
			}
		} else {
			mLogger.log(mUniq, "wifiInfo is null");
		}
		
		mLogger.log(mUniq, "Pebble: " + pebble + " Bluetooth: " + bluetooth + " Wifi: " + wifi);
		
		return (pebble || bluetooth || wifi);
	}

	/**
	 * Copied from PebbleKit https://github.com/pebble/pebblekit
	 * 
	 * Synchronously query the Pebble application to see if an active Bluetooth
	 * connection to a watch currently exists.
	 * 
	 * @param context
	 *            The Android context used to perform the query.
	 *            <p/>
	 *            <em>Protip:</em> You probably want to use your
	 *            ApplicationContext here.
	 * 
	 * @return true if an active connection to the watch currently exists,
	 *         otherwise false. This method will also return false if the Pebble
	 *         application is not installed on the user's handset.
	 */
	public boolean isWatchConnected() {
		Cursor c = null;
		try {
			c = mContext.getApplicationContext().getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
		} catch (Exception e) {
			mLogger.log(mUniq, "Exception getting Pebble connection status: " + e);
		}
		
		if (c == null)
			return false;
		
		if(!c.moveToNext()) {
			c.close();
			return false;
		}

		boolean connected = (c.getInt(0) == 1);
		c.close();
		return connected;
	}
}

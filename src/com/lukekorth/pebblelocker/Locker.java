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
import android.util.Log;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class Locker {
	
	public static final String TAG = "pebble-locker";
	
	private Context mContext;
	private SharedPreferences mPrefs;
	private DevicePolicyManager mDpm;
	
	public Locker(Context context) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mDpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}
	
	public boolean isActiveAdmin() {
		return mDpm.isAdminActive(new ComponentName(mContext, CustomDeviceAdminReceiver.class));
	}
	
	public void lockIfEnabled() {
		lockIfEnabled(true);
	}

	public void lockIfEnabled(boolean forceLock) {
		if (isActiveAdmin()) {
			if (mPrefs.getBoolean("key_enable_locker", false)) {
				if (!connectedToDeviceOrWifi()) {
					mDpm.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					mPrefs.edit().putBoolean("locked", true).commit();
					
					Log.i(TAG, "Locked!");

					if (forceLock && mPrefs.getBoolean("key_force_lock", false))
						mDpm.lockNow();
				}
			}
		} else {
			Log.i(TAG, "Not an active admin");
		}
	}

	public void unlockIfEnabled() {
		if (isActiveAdmin()) {			
			if (mPrefs.getBoolean("key_enable_locker", false)) {
				if(connectedToDeviceOrWifi()) {
					mDpm.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					mPrefs.edit().putBoolean("locked", false).commit();
					
					Log.i(TAG, "Unlocked!");	
				}
			}
		} else {
			Log.i(TAG, "Not an active admin");
		}
	}
	
	private boolean connectedToDeviceOrWifi() {		
		boolean pebble = false;
		boolean bluetooth = false;
		boolean wifi = false;
		
		if(mPrefs.getBoolean("pebble", true))
			pebble = Locker.isWatchConnected(mContext);

		if(mPrefs.getBoolean(mPrefs.getString("bluetooth", ""), false))
			bluetooth = true;

		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if(wifiInfo != null && mPrefs.getBoolean(Base64.encodeToString(wifiInfo.getSSID().getBytes(), Base64.DEFAULT), false))
			wifi = true;
		
		Log.i(TAG, "Pebble: " + pebble + " Bluetooth: " + bluetooth + " Wifi: " + wifi);
		
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
	public static boolean isWatchConnected(Context context) {
		Cursor c = context.getApplicationContext().getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
		if (c == null)
			return false;
		
		if(!c.moveToNext()) {
			c.close();
			return false;
		}

		boolean connected = c.getInt(0) == 1;
		c.close();
		return connected;
	}
}

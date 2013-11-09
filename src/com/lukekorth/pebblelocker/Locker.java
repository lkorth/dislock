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
import android.util.Log;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class Locker {
	
	public static final String TAG = "pebble-locker";
	
	public static void lockIfEnabled(Context context) {
		Locker.lockIfEnabled(context, true);
	}

	public static void lockIfEnabled(Context context, boolean forceLock) {
		DevicePolicyManager dpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));

		if (dpm.isAdminActive(new ComponentName(context, CustomDeviceAdminReceiver.class))) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			if (prefs.getBoolean("key_enable_locker", false)) {
				if (!Locker.connectedToDeviceOrWifi(context)) {
					dpm.resetPassword(prefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					prefs.edit().putBoolean("locked", true).commit();
					
					Log.i(TAG, "Locked!");

					if (forceLock && prefs.getBoolean("key_force_lock", false))
						dpm.lockNow();
				}
			}
		} else {
			Log.i(TAG, "Not an active admin");
		}
	}

	public static void unlockIfEnabled(Context context) {
		DevicePolicyManager dpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));

		if (dpm.isAdminActive(new ComponentName(context, CustomDeviceAdminReceiver.class))) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			if (prefs.getBoolean("key_enable_locker", false)) {
				if(Locker.connectedToDeviceOrWifi(context)) {
					dpm.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
					prefs.edit().putBoolean("locked", false).commit();
					
					Log.i(TAG, "Unlocked!");	
				}
			}
		} else {
			Log.i(TAG, "Not an active admin");
		}
	}
	
	private static boolean connectedToDeviceOrWifi(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean pebble = false;
		boolean bluetooth = false;
		boolean wifi = false;
		
		if(prefs.getBoolean("pebble", true))
			pebble = Locker.isWatchConnected(context);

		if(prefs.getBoolean(prefs.getString("bluetooth", ""), false))
			bluetooth = true;

		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if(wifiInfo != null && prefs.getBoolean(WiFiNetworks.stripQuotes(wifiInfo.getSSID()), false))
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
	public static boolean isWatchConnected(final Context context) {
		Cursor c = context.getApplicationContext().getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
		if (c == null || !c.moveToNext()) {
			return false;
		}
		return c.getInt(0) == 1;
	}
}

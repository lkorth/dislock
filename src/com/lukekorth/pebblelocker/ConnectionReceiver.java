package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConnectionReceiver extends BroadcastReceiver {
	
	private static final String PEBBLE_CONNECTED       = "com.getpebble.action.pebble_connected";
	private static final String PEBBLE_DISCONNECTED    = "com.getpebble.action.pebble_disconnected";
	private static final String BLUETOOTH_CONNECTED    = "android.bluetooth.device.action.acl_connected";
	private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";
	private static final String CONNECTIVITY_CHANGE    = "android.net.conn.connectivity_change";
	private static final String USER_PRESENT           = "android.intent.action.user_present";
	private static final String UNLOCK                 = "unlock";
	
	private Context mContext;
	private SharedPreferences mPrefs;
	private String mAction;

	@SuppressLint("DefaultLocale")
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mAction = intent.getAction().toLowerCase();
		
		Log.i(Locker.TAG, "ConnectionReceiver: " + mAction);
		
		checkForBluetoothDevice(((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
		
		if((mAction.equals(PEBBLE_CONNECTED) || mAction.equals(BLUETOOTH_CONNECTED) || isWifiConnected()) && isLocked(true)) {
			if(isScreenOn()) {
				mPrefs.edit().putBoolean(UNLOCK, true).commit();
				Log.i(Locker.TAG, "Screen is on, setting unlock true for future unlock");
			} else {
				mPrefs.edit().putBoolean(UNLOCK, false).commit();
				Log.i(Locker.TAG, "Attempting unlock");
				Locker.unlockIfEnabled(context);
			}
		} else if ((mAction.equals(PEBBLE_DISCONNECTED) || mAction.equals(BLUETOOTH_DISCONNECTED) || !isWifiConnected()) && !isLocked(false)) {
			mPrefs.edit().putBoolean(UNLOCK, false).commit();
			Log.i(Locker.TAG, "Attempting lock");
			Locker.lockIfEnabled(context);
		} else if (mAction.equals(USER_PRESENT) && needToUnlock()) {
			mPrefs.edit().putBoolean(UNLOCK, false).commit();
			Log.i(Locker.TAG, "User present and need to unlock...attempting to unlock");
			Locker.unlockIfEnabled(context);
		}
	}
	
	public boolean isLocked(boolean defaultValue) {
		return mPrefs.getBoolean("locked", defaultValue);
	}
	
	public boolean isScreenOn() {
		return ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).isScreenOn();
	}
	
	public boolean needToUnlock() {
		return mPrefs.getBoolean(UNLOCK, true);
	}
	
	public void checkForBluetoothDevice(BluetoothDevice device) {
		if(mAction.equals(BLUETOOTH_CONNECTED)) {
			mPrefs.edit().putString("bluetooth", device.getAddress()).commit();
			Log.i(Locker.TAG, "Bluetooth device connected: " + device.getName());
		} else if(mAction.equals(BLUETOOTH_DISCONNECTED)) {
			mPrefs.edit().putString("bluetooth", "").commit();
			Log.i(Locker.TAG, "Bluetooth device disconnected: " + device.getName());
		}
	}
	
	public boolean isWifiConnected() {
		if(mAction.equals(CONNECTIVITY_CHANGE)) {
			NetworkInfo netInfo =  ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {	            
	            Log.i(Locker.TAG, "Wifi network connected: " + 
	            		((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID());
	            
	            return true;
			}
		}
		
		return false;
	}
}
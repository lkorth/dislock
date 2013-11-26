	package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Base64;

public class ConnectionReceiver extends BroadcastReceiver {
	
	private static final String PEBBLE_CONNECTED       = "com.getpebble.action.pebble_connected";
	private static final String PEBBLE_DISCONNECTED    = "com.getpebble.action.pebble_disconnected";
	private static final String BLUETOOTH_CONNECTED    = "android.bluetooth.device.action.acl_connected";
	private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";
	private static final String CONNECTIVITY_CHANGE    = "android.net.conn.connectivity_change";
	private static final String USER_PRESENT           = "android.intent.action.user_present";
	public static final String LOCKED				   = "locked";
	private static final String UNLOCK                 = "unlock";
	
	private Context mContext;
	private SharedPreferences mPrefs;
	private Logger mLogger;
	private String mAction;

	@SuppressLint("DefaultLocale")
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context);
		mAction = intent.getAction().toLowerCase();
		
		mLogger.log("ConnectionReceiver: " + mAction);
		
		checkForBluetoothDevice(((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
		
		if((mAction.equals(PEBBLE_CONNECTED) || mAction.equals(BLUETOOTH_CONNECTED) || isWifiConnected()) && isLocked(true)) {
			if(isScreenOn()) {
				mPrefs.edit().putBoolean(UNLOCK, true).commit();
				mLogger.log("Screen is on, setting unlock true for future unlock");
			} else {
				mPrefs.edit().putBoolean(UNLOCK, false).commit();
				mLogger.log("Attempting unlock");
				new Locker(context).unlockIfEnabled();
			}
		} else if ((mAction.equals(PEBBLE_DISCONNECTED) || mAction.equals(BLUETOOTH_DISCONNECTED) || !isWifiConnected()) && !isLocked(false)) {
			mPrefs.edit().putBoolean(UNLOCK, false).commit();
			mLogger.log("Attempting lock");
			new Locker(context).lockIfEnabled();
		} else if (mAction.equals(USER_PRESENT) && needToUnlock()) {
			mPrefs.edit().putBoolean(UNLOCK, false).commit();
			mLogger.log("User present and need to unlock...attempting to unlock");
			new Locker(context).unlockIfEnabled();
		}
	}
	
	public boolean isLocked(boolean defaultValue) {
		boolean locked = mPrefs.getBoolean(LOCKED, defaultValue);
		mLogger.log("Locked: " + locked);
		
		return locked;
	}
	
	public boolean isScreenOn() {
		return ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).isScreenOn();
	}
	
	public boolean needToUnlock() {
		boolean needToUnlock = mPrefs.getBoolean(UNLOCK, true);
		mLogger.log("Need to unlock: " + needToUnlock);
		
		return needToUnlock;
	}
	
	public void checkForBluetoothDevice(BluetoothDevice device) {
		if(mAction.equals(BLUETOOTH_CONNECTED)) {
			mPrefs.edit().putString("bluetooth", device.getAddress()).commit();
			mLogger.log("Bluetooth device connected: " + device.getName());
		} else if(mAction.equals(BLUETOOTH_DISCONNECTED)) {
			mPrefs.edit().putString("bluetooth", "").commit();
			mLogger.log("Bluetooth device disconnected: " + device.getName());
		}
	}
	
	public boolean isWifiConnected() {
		if(mAction.equals(CONNECTIVITY_CHANGE)) {
			WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			if(wifiInfo != null) {
				String ssid = wifiInfo.getSSID();
				
				if(ssid != null) {
					mLogger.log("Wifi network " + ssid + " connected: " + Base64.encodeToString(ssid.getBytes(), Base64.DEFAULT).trim());
					return true;
				} else {
					mLogger.log("ConnectionReceiver: wifiInfo.getSSID is null");
				}
			} else {
				mLogger.log("ConnectionReceiver: wifiInfo is null");
			}
		}
		
		return false;
	}
}
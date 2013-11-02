package com.lukekorth.pebblelocker;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class ConnectionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String action = intent.getAction();
		
		if(action.equalsIgnoreCase("android.bluetooth.device.action.ACL_CONNECTED")) {
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			prefs.edit().putString("bluetooth", device.getAddress()).commit();
		} else if(action.equalsIgnoreCase("android.bluetooth.device.action.ACL_DISCONNECTED"))
			prefs.edit().putString("bluetooth", "").commit();		
		
		boolean wifi = false;
		if(action.equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE")) {
			NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo(); 
	    
			if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) 
	            wifi = true;
		}
				
		if(action.equalsIgnoreCase("com.getpebble.action.PEBBLE_CONNECTED") ||
				action.equalsIgnoreCase("android.bluetooth.device.action.ACL_CONNECTED") || (action.equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE") && wifi) && prefs.getBoolean("locked", true)) {
			if(((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn())
				prefs.edit().putBoolean("unlock", true).commit();
			else {
				prefs.edit().putBoolean("unlock", false).commit();
				Locker.unlockIfEnabled(context);
			}
		} else if (action.equalsIgnoreCase("com.getpebble.action.PEBBLE_DISCONNECTED") ||
				   	action.equalsIgnoreCase("android.bluetooth.device.action.ACL_DISCONNECTED") || action.equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE") && !prefs.getBoolean("locked", false)) {
			Locker.lockIfEnabled(context);
		} else if (action.equalsIgnoreCase("android.intent.action.USER_PRESENT") && 
				prefs.getBoolean("unlock", false)) {
			prefs.edit().putBoolean("unlock", false).commit();
			Locker.unlockIfEnabled(context);
		}
	}
}
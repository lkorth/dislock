package com.lukekorth.pebblelocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class PebbleConnectionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
				
		if(action.equalsIgnoreCase("com.getpebble.action.PEBBLE_CONNECTED"))
			PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("unlock", true).commit();
		else if (action.equalsIgnoreCase("com.getpebble.action.PEBBLE_DISCONNECTED"))
			Locker.lockIfEnabled(context);
		else if (action.equalsIgnoreCase("android.intent.action.USER_PRESENT") && 
				PreferenceManager.getDefaultSharedPreferences(context).getBoolean("unlock", false) &&
				Locker.isWatchConnected(context)) {
			PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("unlock", false).commit();
			Locker.unlockIfEnabled(context);
		}
	}

}

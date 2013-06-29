package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class PebbleConnectionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		DevicePolicyManager dpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
		ComponentName deviceAdmin = new ComponentName(context, CustomDeviceAdminReceiver.class);
		
		if(dpm.isAdminActive(deviceAdmin) && action.equals("com.getpebble.action.PEBBLE_CONNECTED")) {
			dpm.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		} else if (dpm.isAdminActive(deviceAdmin) && action.equals("com.getpebble.action.PEBBLE_DISCONNECTED")) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			dpm.resetPassword(prefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
			
			if(prefs.getBoolean("key_force_lock", false))
				dpm.lockNow();
		}
	}

}

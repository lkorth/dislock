package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class Locker {
	
	public static void lockIfEnabled(Context context) {
		DevicePolicyManager dpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
		
		if (dpm.isAdminActive(new ComponentName(context, CustomDeviceAdminReceiver.class))) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			if(prefs.getBoolean("key_enable_locker", false)) {			
				dpm.resetPassword(prefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
				
				if(prefs.getBoolean("key_force_lock", false))
					dpm.lockNow();
			}
		}
	}
}

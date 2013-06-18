package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		DevicePolicyManager dpm = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
		
		if (dpm.isAdminActive(new ComponentName(context, CustomDeviceAdminReceiver.class))) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			dpm.resetPassword(prefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
			
			if(prefs.getBoolean("key_force_lock", false))
				dpm.lockNow();
		}
	}

}

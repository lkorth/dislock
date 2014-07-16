package com.lukekorth.pebblelocker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.services.LockerService;

public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";
    private static final String TAG = "[PHONE-STATE]";

	@Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
		new Logger(context).log(TAG, "Received a PhoneState BroadcastIntent: " + intent.getAction());

        if (action.equals(BOOT_ACTION)) {
            Intent lockerIntent = new Intent(context, LockerService.class);
            lockerIntent.putExtra(LockerService.TAG, TAG);
            lockerIntent.putExtra(LockerService.WITH_DELAY, false);
            context.startService(lockerIntent);
        }
	}
}

package com.lukekorth.pebblelocker.receivers;

import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.services.LockerService;

public class PhoneStateReceiver extends BaseBroadcastReceiver {

    private static final String BOOT_ACTION = "android.intent.action.boot_completed";
    private static final String TAG = "[PHONE-STATE]";

	@Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
		new Logger(context).log(TAG, "Received a PhoneState BroadcastIntent: " + intent.getAction());

        if (action.equalsIgnoreCase(BOOT_ACTION)) {
            Intent lockerIntent = new Intent(context, LockerService.class);
            lockerIntent.putExtra(LockerService.TAG, TAG);
            lockerIntent.putExtra(LockerService.WITH_DELAY, false);
            BaseBroadcastReceiver.startWakefulService(context, lockerIntent);
        }
	}
}

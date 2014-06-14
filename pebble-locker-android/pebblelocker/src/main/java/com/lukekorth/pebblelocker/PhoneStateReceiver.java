package com.lukekorth.pebblelocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PhoneStateReceiver extends BroadcastReceiver {

    private final String SHUTDOWN_ACTION = "android.intent.action.ACTION_SHUTDOWN";
    private final String TAG = "[PHONE-STATE]";

	@Override
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
		new Logger(context).log(TAG, "Received a PhoneState BroadcastIntent: " + intent.getAction());

        if (action.equals(SHUTDOWN_ACTION)) {
            new Locker(context, TAG).lock(false);
        } else {
            new Locker(context, TAG).handleLocking();
        }
	}
}

package com.lukekorth.pebblelocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PhoneStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		new Logger(context).log("[PHONESTATE]", "Received a PhoneState BroadcastIntent: " + intent.getAction());
		new Locker(context, "[PHONESTATE]").handleLocking();
	}
}

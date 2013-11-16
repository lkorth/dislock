package com.lukekorth.pebblelocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(Locker.TAG, "Received a PhoneState BroadcastIntent: " + intent.getAction());
		
		new Locker(context).lockIfEnabled();
	}

}

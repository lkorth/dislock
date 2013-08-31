package com.lukekorth.pebblelocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PebbleConnectionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
				
		if(action.equalsIgnoreCase("com.getpebble.action.PEBBLE_CONNECTED"))
			Locker.unlock(context);
		else if (action.equalsIgnoreCase("com.getpebble.action.PEBBLE_DISCONNECTED"))
			Locker.lockIfEnabled(context);
	}

}

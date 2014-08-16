package com.lukekorth.pebblelocker.receivers;

import com.lukekorth.pebblelocker.helpers.PebbleHelper;

public class PebbleConnectionReceiver extends BaseBroadcastReceiver {

    protected static final String PEBBLE_CONNECTED = "com.getpebble.action.pebble_connected";
    protected static final String PEBBLE_DISCONNECTED = "com.getpebble.action.pebble_disconnected";

    @Override
    protected void handle() {
        if (new PebbleHelper(mContext, mLogger).isEnabled()) {
            if (mAction.equals(PEBBLE_CONNECTED)) {
                mLogger.log("Pebble connected, attempting unlock");
                handleLocking();
            } else if (mAction.equals(PEBBLE_DISCONNECTED)) {
                mLogger.log("Pebble disconnected, attempting lock with delay");
                lockWithDelay();
            }
        }
    }

}

package com.lukekorth.pebblelocker.receivers;

import com.lukekorth.pebblelocker.helpers.PebbleHelper;

public class PebbleConnectionReceiver extends BaseBroadcastReceiver {

    protected static final String PEBBLE_CONNECTED = "com.getpebble.action.pebble_connected";
    protected static final String PEBBLE_DISCONNECTED = "com.getpebble.action.pebble_disconnected";

    @Override
    protected void handle() {
        if (new PebbleHelper(mContext, mTag).isEnabled()) {
            if (mAction.equals(PEBBLE_CONNECTED)) {
                mLogger.debug("Pebble connected, attempting unlock");
                handleLocking();
            } else if (mAction.equals(PEBBLE_DISCONNECTED)) {
                mLogger.debug("Pebble disconnected, attempting lock with delay");
                lockWithDelay();
            }
        }
    }

}

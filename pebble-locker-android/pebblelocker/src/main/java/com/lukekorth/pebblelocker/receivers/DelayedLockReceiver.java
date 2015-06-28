package com.lukekorth.pebblelocker.receivers;

public class DelayedLockReceiver extends BaseBroadcastReceiver {

    protected static final String DELAYED_LOCK = "com.lukekorth.pebblelocker.delayed_lock";

    @Override
    protected void handle() {
        if (mAction.equals(DELAYED_LOCK)) {
            mLogger.debug("Delayed lock broadcast, handling locking");
            handleLocking();
        }
    }
}

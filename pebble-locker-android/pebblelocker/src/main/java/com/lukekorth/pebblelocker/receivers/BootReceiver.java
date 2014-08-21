package com.lukekorth.pebblelocker.receivers;

import com.lukekorth.pebblelocker.Locker;

public class BootReceiver extends BaseBroadcastReceiver {

    private static final String BOOT_ACTION = "android.intent.action.boot_completed";
    private static final String ACTION_SHUTDOWN = "android.intent.action.action_shutdown";

    @Override
    protected void handle() {
        if (mAction.equals(ACTION_SHUTDOWN)) {
            mLogger.debug("Shutting down, locking without checking trusted devices");
            new Locker(mContext, mTag).lock(false);
        } else if (mAction.equals(BOOT_ACTION)) {
            mLogger.debug("Boot complete, handling locking");
            handleLocking();
        }
    }

}

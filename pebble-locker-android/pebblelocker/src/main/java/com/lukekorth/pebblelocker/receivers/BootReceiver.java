package com.lukekorth.pebblelocker.receivers;

public class BootReceiver extends BaseBroadcastReceiver {

    private static final String BOOT_ACTION = "android.intent.action.boot_completed";
    private static final String ACTION_SHUTDOWN = "android.intent.action.action_shutdown";

    @Override
    protected void handle() {
        if (mAction.equals(BOOT_ACTION)) {
            mLogger.debug("Boot complete, setting alarm to handle locking in 10 seconds");
            setDelayedLockAlarm(mContext, 10000);
        }
    }

}

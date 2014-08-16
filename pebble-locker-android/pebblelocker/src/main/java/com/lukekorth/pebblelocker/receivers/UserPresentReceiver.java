package com.lukekorth.pebblelocker.receivers;

public class UserPresentReceiver extends BaseBroadcastReceiver {

    private static final String USER_PRESENT = "android.intent.action.user_present";

    @Override
    protected void handle() {
        if (mAction.equals(USER_PRESENT) && mDeviceHelper.isUnlockNeeded()) {
            mLogger.log("User present and need to unlock, handling locking");
            handleLocking();
        }
    }

}

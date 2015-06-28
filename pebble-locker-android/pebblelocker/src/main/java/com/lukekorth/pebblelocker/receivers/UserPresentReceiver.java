package com.lukekorth.pebblelocker.receivers;

import android.app.ActivityManager;
import android.content.Context;

import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.services.LockerService;

public class UserPresentReceiver extends BaseBroadcastReceiver {

    private static final String USER_PRESENT = "android.intent.action.user_present";

    @Override
    protected void handle() {
        if (mAction.equals(USER_PRESENT) && Settings.isUnlockNeeded(mContext)) {
            mLogger.debug("User present and need to unlock, handling locking");
            handleLocking();
        } else if (mAction.equals(USER_PRESENT) && !Settings.isLocked(mContext) &&
                !isLockerServiceRunning()) {
            mLogger.debug("State is unlocked and service is not running, handling locking");
            handleLocking();
        }
    }

    private boolean isLockerServiceRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LockerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

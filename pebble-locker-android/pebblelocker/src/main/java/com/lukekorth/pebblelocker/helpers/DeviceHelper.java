package com.lukekorth.pebblelocker.helpers;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;
import com.lukekorth.pebblelocker.logging.Logger;

public class DeviceHelper {

    public static final String NEED_TO_UNLOCK_KEY = "unlock";

    private Context mContext;
    private Logger mLogger;

    public DeviceHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public boolean isOnLockscreen() {
        boolean keyguard = ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE))
                .inKeyguardRestrictedInputMode();
        mLogger.log("Keyguard is showing: " + keyguard);
        return keyguard;
    }

    public boolean isScreenOn() {
        boolean screen = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                .isScreenOn();
        mLogger.log("Screen is on: " + screen);
        return screen;
    }

    public boolean isLocked(boolean defaultState) {
        boolean locked = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(ConnectionReceiver.LOCKED, defaultState);
        mLogger.log("Locked: " + locked);
        return locked;
    }

    public boolean isUnlockNeeded() {
        boolean needToUnlock = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(NEED_TO_UNLOCK_KEY, true);
        mLogger.log("Need to unlock: " + needToUnlock);

        return needToUnlock;
    }

    public static void sendLockStatusChangedBroadcast(Context context) {
        context.sendBroadcast(new Intent(ConnectionReceiver.STATUS_CHANGED_INTENT));
    }

    public void sendLockStatusChangedBroadcast() {
        DeviceHelper.sendLockStatusChangedBroadcast(mContext);
    }

}

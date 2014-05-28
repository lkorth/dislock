package com.lukekorth.pebblelocker.helpers;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.lukekorth.pebblelocker.ConnectionReceiver;
import com.lukekorth.pebblelocker.Logger;

public class DeviceHelper {

    private Context mContext;
    private Logger mLogger;

    public DeviceHelper(Context context) {
        mContext = context;
    }

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

    public void sendLockStatusChangedBroadcast() {
        LocalBroadcastManager.getInstance(mContext)
                .sendBroadcast(new Intent(ConnectionReceiver.STATUS_CHANGED_INTENT));
    }
}

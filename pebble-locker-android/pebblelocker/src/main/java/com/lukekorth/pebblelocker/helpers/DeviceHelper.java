package com.lukekorth.pebblelocker.helpers;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.BaseBroadcastReceiver;

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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            int screen = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getState();
            if (screen == Display.STATE_ON || screen == Display.STATE_DOZING) {
                mLogger.log("Screen is on or dozing. " + screen);
                return true;
            } else {
                mLogger.log("Screen is off or unknown. " + screen);
                return false;
            }
        } else {
            boolean screen = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                    .isScreenOn();
            mLogger.log("Screen is on: " + screen);
            return screen;
        }
    }

    public boolean isLocked(boolean defaultState) {
        boolean locked = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(BaseBroadcastReceiver.LOCKED, defaultState);
        mLogger.log("Locked: " + locked);
        return locked;
    }

    public boolean isUnlockNeeded() {
        boolean needToUnlock = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(NEED_TO_UNLOCK_KEY, true);
        mLogger.log("Need to unlock: " + needToUnlock);

        return needToUnlock;
    }

    public void sendLockStatusChangedEvent() {
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

}

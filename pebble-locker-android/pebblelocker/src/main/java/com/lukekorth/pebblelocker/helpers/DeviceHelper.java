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
import com.lukekorth.pebblelocker.receivers.BaseBroadcastReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceHelper {

    public static final String NEED_TO_UNLOCK_KEY = "unlock";

    private Context mContext;
    private Logger mLogger;

    public DeviceHelper(Context context, String tag) {
        mContext = context;
        mLogger = LoggerFactory.getLogger(tag);
    }

    public boolean isOnLockscreen() {
        boolean keyguard = ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE))
                .inKeyguardRestrictedInputMode();
        mLogger.debug("Keyguard is showing: " + keyguard);
        return keyguard;
    }

    public boolean isScreenOn() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            int screen = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getState();
            if (screen == Display.STATE_ON || screen == Display.STATE_DOZING) {
                mLogger.debug("Screen is on or dozing. " + screen);
                return true;
            } else {
                mLogger.debug("Screen is off or unknown. " + screen);
                return false;
            }
        } else {
            boolean screen = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                    .isScreenOn();
            mLogger.debug("Screen is on: " + screen);
            return screen;
        }
    }

    public boolean isLocked(boolean defaultState) {
        boolean locked = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(BaseBroadcastReceiver.LOCKED, defaultState);
        mLogger.debug("Locked: " + locked);
        return locked;
    }

    public boolean isUnlockNeeded() {
        boolean needToUnlock = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(NEED_TO_UNLOCK_KEY, true);
        mLogger.debug("Need to unlock: " + needToUnlock);

        return needToUnlock;
    }

    public void sendLockStatusChangedEvent() {
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

}

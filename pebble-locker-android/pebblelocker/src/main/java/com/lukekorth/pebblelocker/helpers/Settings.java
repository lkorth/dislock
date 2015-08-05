package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.models.LockState;

public class Settings {

    public static final String REAUTHENTICATION_REQUIRED = "key_require_password_on_reconnect";
    public static final String PURCHASED = "purchased";
    public static final String LOCKED = "locked";
    public static final String NEED_TO_UNLOCK = "need_to_unlock";
    public static final String LOCK_STATE = "lock_state";
    public static final String GRACE_PERIOD = "key_grace_period";
    public static final String ONGOING_NOTIFICATION = "key_notification";
    public static final String PEBBLE_ENABLED = "pebble_enabled";

    private static SharedPreferences sPrefs;

    public static boolean isReauthenticationRequired(Context context) {
        return getPreferences(context).getBoolean(REAUTHENTICATION_REQUIRED, false);
    }

    public static boolean hasPurchased(Context context) {
        return BuildConfig.DEBUG || getPreferences(context).getBoolean(PURCHASED, false);
    }

    public static void setPurchased(Context context, boolean purchased) {
        getPreferences(context).edit().putBoolean(PURCHASED, purchased).apply();
    }

    public static boolean isLocked(Context context) {
        return getPreferences(context).getBoolean(LOCKED, true);
    }

    public static void setLocked(Context context, boolean locked) {
        getPreferences(context).edit().putBoolean(LOCKED, locked).apply();
    }

    public static boolean isUnlockNeeded(Context context) {
        return getPreferences(context).getBoolean(NEED_TO_UNLOCK, false);
    }

    public static void setNeedToUnlock(Context context, boolean needToUnlock) {
        getPreferences(context).edit().putBoolean(NEED_TO_UNLOCK, needToUnlock).apply();
    }

    public static int getLockState(Context context) {
        return getPreferences(context).getInt(LOCK_STATE, LockState.AUTO.getState());
    }

    public static void setLockState(Context context, LockState state) {
        getPreferences(context).edit().putInt(LOCK_STATE, state.getState()).apply();
    }

    public static String getGracePeriod(Context context) {
        return getPreferences(context).getString(GRACE_PERIOD, "2");
    }

    public static boolean getOngoingNotification(Context context) {
        return getPreferences(context).getBoolean(ONGOING_NOTIFICATION, true);
    }

    public static boolean isPebbleEnabled(Context context) {
        return getPreferences(context).getBoolean(PEBBLE_ENABLED, false);
    }

    public static void setPebbleEnabled(Context context, boolean enabled) {
        getPreferences(context).edit().putBoolean(PEBBLE_ENABLED, enabled).apply();
    }

    public static SharedPreferences getPreferences(Context context) {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return sPrefs;
    }
}

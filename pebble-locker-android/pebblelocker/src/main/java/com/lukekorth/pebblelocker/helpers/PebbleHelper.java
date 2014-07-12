package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.logging.Logger;

public class PebbleHelper {

    public static final String ENABLED_KEY = "pebble";

    private Context mContext;
    private Logger mLogger;

    public PebbleHelper(Context context) {
        mContext = context;
    }

    public PebbleHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public boolean isEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ENABLED_KEY, true);
    }

    public boolean isConnected() {
        Cursor c = null;
        try {
            c = mContext.getApplicationContext().getContentResolver()
                    .query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
        } catch (Exception e) {
            mLogger.log("Exception getting Pebble connection status " + e.getClass() + ": " +
                e.getMessage());
        }

        if (c == null) {
            return false;
        }

        if (!c.moveToNext()) {
            c.close();
            return false;
        }

        boolean connected = (c.getInt(0) == 1);
        c.close();
        return connected;
    }

    public boolean isEnabledAndConnected() {
        if (isEnabled()) {
            return isConnected();
        } else {
            mLogger.log("Unlock via any Pebble is not enabled");
            return false;
        }
    }

    public boolean isPebbleAppInstalled() {
        PackageManager packageManager = mContext.getPackageManager();
        try {
            packageManager.getPackageInfo("com.getpebble.android", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String getConnectionStatus() {
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ENABLED_KEY, true)) {
            if (isConnected()) {
                return "Pebble watch connected";
            } else {
                return "Pebble watch disconnected";
            }
        }

        return "";
    }
}

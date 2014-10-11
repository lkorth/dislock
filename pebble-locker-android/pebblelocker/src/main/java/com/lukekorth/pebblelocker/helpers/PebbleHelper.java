package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PebbleHelper {

    public static final String ENABLED_KEY = "pebble";

    private Context mContext;
    private Logger mLogger;

    public PebbleHelper(Context context, String tag) {
        mContext = context;
        mLogger = LoggerFactory.getLogger(tag);
    }

    public boolean isEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ENABLED_KEY, false);
    }

    public boolean isConnected() {
        Cursor c = null;
        try {
            c = mContext.getApplicationContext().getContentResolver()
                    .query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
        } catch (Exception e) {
            mLogger.error("Exception getting Pebble connection status " + e.getClass() + ": " +
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
            mLogger.debug("Unlock via any Pebble is not enabled");
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
        if (isPebbleAppInstalled() && isEnabled() && isConnected()) {
            return "Pebble watch connected";
        }

        return null;
    }
}

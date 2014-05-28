package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.Logger;

public class PebbleHelper {

    private static final String ENABLED_KEY = "pebble";

    private Context mContext;
    private Logger mLogger;

    public PebbleHelper(Context context) {
        mContext = context;
    }

    public PebbleHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public boolean isEnabledAndConnected() {
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ENABLED_KEY, true)) {
            return isConnected();
        } else {
            mLogger.log("Unlock via any Pebble is not enabled");
            return false;
        }
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

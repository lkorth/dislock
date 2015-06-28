package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import com.lukekorth.pebblelocker.R;

public class PebbleHelper {

    private Context mContext;

    public PebbleHelper(Context context) {
        mContext = context;
    }

    public boolean isConnected() {
        Cursor c = null;
        try {
            c = mContext.getApplicationContext().getContentResolver()
                    .query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
        } catch (Exception e) {
            return false;
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
        return (Settings.isPebbleEnabled(mContext) && isConnected());
    }

    public String getConnectionStatus() {
        if (PebbleHelper.isPebbleAppInstalled(mContext) && isEnabledAndConnected()) {
            return mContext.getString(R.string.pebble_watch_connected);
        }

        return null;
    }

    public static boolean isPebbleAppInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo("com.getpebble.android", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}

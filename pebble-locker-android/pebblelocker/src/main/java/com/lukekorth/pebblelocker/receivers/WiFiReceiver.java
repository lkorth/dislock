package com.lukekorth.pebblelocker.receivers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.WifiHelper;

public class WiFiReceiver extends BaseBroadcastReceiver {

    private static final String CONNECTIVITY_CHANGE = "android.net.conn.connectivity_change";

    @Override
    protected void handle() {
        checkForMalformedPassword();

        boolean trustedWifiConnected = new WifiHelper(mContext, mTag).isTrustedWifiConnected();
        if (mAction.equals(CONNECTIVITY_CHANGE) && trustedWifiConnected) {
            mLogger.debug("Wifi connected, attempting unlock");
            handleLocking();
        } else if (mAction.equals(CONNECTIVITY_CHANGE) && !trustedWifiConnected) {
            mLogger.debug("Wifi disconnected, attempting lock with delay");
            lockWithDelay();
        }
    }

    private void checkForMalformedPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String originalPassword = prefs.getString("key_password", "");
        String trimmedPassword = originalPassword.trim();
        if (!originalPassword.equals(trimmedPassword)) {
            prefs.edit().putString("key_password", trimmedPassword).apply();
            new Locker(mContext, "Malformed-Password").lock(false);
        }
    }
}

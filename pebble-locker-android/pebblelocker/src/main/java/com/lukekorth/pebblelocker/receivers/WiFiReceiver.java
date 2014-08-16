package com.lukekorth.pebblelocker.receivers;

import com.lukekorth.pebblelocker.helpers.WifiHelper;

public class WiFiReceiver extends BaseBroadcastReceiver {

    private static final String CONNECTIVITY_CHANGE = "android.net.conn.connectivity_change";

    @Override
    protected void handle() {
        boolean trustedWifiConnected = new WifiHelper(mContext, mLogger).isTrustedWifiConnected();
        if (mAction.equals(CONNECTIVITY_CHANGE) && trustedWifiConnected) {
            mLogger.log("Wifi connected, attempting unlock");
            handleLocking();
        } else if (mAction.equals(CONNECTIVITY_CHANGE) && !trustedWifiConnected) {
            mLogger.log("Wifi disconnected, attempting lock with delay");
            lockWithDelay();
        }
    }

}

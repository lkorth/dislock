package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiHelper {

    private Context mContext;
    private Logger mLogger;

    public WifiHelper(Context context, String tag) {
        mContext = context;
        mLogger = LoggerFactory.getLogger(tag);
    }

    /**
     * @return a map of printable SSIDs to a base64 encoded representation of the SSID
     */
    public Map<String, String> getPrintableNetworks() {
        List<WifiConfiguration> storedNetworks = getStoredNetworks();

        Map<String, String> printableNetworks = new HashMap<String, String>();
        for (WifiConfiguration wifiConfiguration : storedNetworks) {
            String ssid = stripQuotes(wifiConfiguration.SSID);
            if (!TextUtils.isEmpty(ssid)) {
                printableNetworks.put(ssid, base64Encode(ssid));
            }
        }

        return printableNetworks;
    }

    /**
     * @return a list of {@link android.net.wifi.WifiConfiguration}s or null if Wifi is disabled
     * or there was a failure getting networks
     */
    public List<WifiConfiguration> getStoredNetworks() {
        return ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).getConfiguredNetworks();
    }

    public boolean isTrustedWifiConnected() {
        String ssid = getConnectedNetworkSsid();
        String encodedSsid = base64Encode(ssid);

        mLogger.debug("Wifi network " + ssid + " is connected: " + encodedSsid);

        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(encodedSsid, false)) {
            return true;
        }

        return false;
    }

    public String getConnectionStatus() {
        if(isTrustedWifiConnected()) {
            return getConnectedNetworkSsid() + " connected";
        }

        return null;
    }

    private String getConnectedNetworkSsid() {
        WifiInfo wifiInfo = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
        if (wifiInfo != null) {
            if (wifiInfo.getSSID() != null) {
                return stripQuotes(wifiInfo.getSSID());
            } else {
                mLogger.debug("wifiInfo.getSSID is null");
            }
        } else {
            mLogger.debug("wifiInfo is null");
        }

        return "";
    }

    public static String base64Encode(String input) {
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT).trim();
    }

    private String stripQuotes(String input) {
        if (input != null && input.startsWith("\"") && input.endsWith("\""))
            return input.substring(1, input.length() - 1);
        else
            return input;
    }

}

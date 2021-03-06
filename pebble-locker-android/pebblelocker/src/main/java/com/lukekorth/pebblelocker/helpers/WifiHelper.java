package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiHelper {

    private Context mContext;

    public WifiHelper(Context context) {
        mContext = context;
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

        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(encodedSsid, false);
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
            }
        }

        return "";
    }

    public static String base64Encode(String input) {
        if (input == null)
            return "";
        else
            return Base64.encodeToString(input.getBytes(), Base64.DEFAULT).trim();
    }

    private String stripQuotes(String input) {
        if (input != null && input.startsWith("\"") && input.endsWith("\""))
            return input.substring(1, input.length() - 1);
        else
            return input;
    }
}

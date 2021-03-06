package com.lukekorth.pebblelocker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.models.WifiNetworks;
import com.lukekorth.pebblelocker.services.LockerService;

import java.util.List;
import java.util.Map;

public class WiFiNetworks extends PremiumFeaturesActivity implements Preference.OnPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
        inlinePrefCat.setTitle("WiFi Networks");
        root.addPreference(inlinePrefCat);

        WifiHelper wifiHelper = new WifiHelper(this);

        List<WifiConfiguration> networks = wifiHelper.getStoredNetworks();
        if (networks == null) {
            new AlertDialog.Builder(this)
                .setMessage("Wifi must be enabled to get a list of saved networks, please enable it before using this feature")
                .setCancelable(false)
                .setPositiveButton("Open Wifi Settings", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WiFiNetworks.this.finish();
                    }
                })
                .show();
        } else {
            migrateNetworks();

            Map<String, String> printableNetworks = wifiHelper.getPrintableNetworks();
            for (Map.Entry<String, String> entry : printableNetworks.entrySet()) {
                // Checkbox preference
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(entry.getValue());
                checkboxPref.setTitle(entry.getKey());
                checkboxPref.setOnPreferenceChangeListener(this);
                inlinePrefCat.addPreference(checkboxPref);
            }

            setPreferenceScreen(root);
        }
    }

    private void migrateNetworks() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!prefs.getBoolean("migratedWifi", false)) {
            WifiHelper wifiHelper = new WifiHelper(this);
            List<WifiConfiguration> networks = wifiHelper.getStoredNetworks();

            WifiNetworks networkModel;
            for (WifiConfiguration network : networks) {
                networkModel = new WifiNetworks();
                networkModel.ssid = network.SSID;
                networkModel.trusted = prefs.getBoolean(WifiHelper.base64Encode(network.SSID), false);
            }
            prefs.edit().putBoolean("migratedWifi", true).apply();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean trusted = Boolean.parseBoolean(newValue.toString());
        if (!trusted || !isPurchaseRequired()) {
            WifiNetworks.setNetworkTrusted(preference.getTitle().toString(), trusted);

            Intent intent = new Intent(this, LockerService.class)
                    .putExtra(LockerService.EXTRA_FORCE_LOCK, false);
            startService(intent);
            getEnabledDevices();
            return true;
        }
        return false;
    }
}

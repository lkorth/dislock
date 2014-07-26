package com.lukekorth.pebblelocker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.services.LockerService;

import java.util.List;
import java.util.Map;

public class WiFiNetworks extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Inline preferences
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
                .setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WiFiNetworks.this.finish();
                    }
                })
                .show();
        } else {
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (LockState.getCurrentState(this) == LockState.AUTO) {
            Intent intent = new Intent(this, LockerService.class);
            intent.putExtra(LockerService.TAG, "[WIFI-ACTIVITY]");
            intent.putExtra(LockerService.WITH_DELAY, false);
            intent.putExtra(LockerService.FORCE_LOCK, false);
            startService(intent);
        }
        return true;
    }
}

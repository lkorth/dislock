package com.lukekorth.pebblelocker;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.lukekorth.pebblelocker.helpers.BluetoothHelper;

import java.util.Set;

public class BluetoothDevices extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Any Pebble
        PreferenceCategory pebble = new PreferenceCategory(this);
        pebble.setTitle("Pebble");
        root.addPreference(pebble);
        CheckBoxPreference pebblePref = new CheckBoxPreference(this);
        pebblePref.setKey("pebble");
        pebblePref.setTitle("Allow any Pebble to unlock");
        pebblePref.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pebble", true));
        pebble.addPreference(pebblePref);

        // Inline preferences
        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
        inlinePrefCat.setTitle("Bluetooth Devices");
        root.addPreference(inlinePrefCat);

        Set<BluetoothDevice> pairedDevices = new BluetoothHelper(this).getPairedDevices();
        if (pairedDevices.size() == 0) {
            new AlertDialog.Builder(this)
                .setMessage("Bluetooth is turned off or you do not have any paired devices. " +
                    "Please enable it and/or pair a device before using this feature")
                .setCancelable(false)
                .setPositiveButton("Open Bluetooth Settings", new OnClickListener() {
                @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothDevices.this.finish();
                    }
                })
                .show();
        } else {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Checkbox preference
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(device.getAddress());
                checkboxPref.setTitle(device.getName());
                inlinePrefCat.addPreference(checkboxPref);
            }

            setPreferenceScreen(root);
        }
    }
}
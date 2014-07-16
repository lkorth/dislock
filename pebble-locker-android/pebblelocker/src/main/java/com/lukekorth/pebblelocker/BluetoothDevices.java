package com.lukekorth.pebblelocker;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;

import java.util.Map;
import java.util.Set;

public class BluetoothDevices extends PreferenceActivity implements AndroidWearHelper.Listener {

    private PreferenceCategory mAndroidWear;
    private Preference mAndroidWearStatus;
    private PreferenceCategory mBluetooth;
    private Preference mBluetoothStatus;

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

        // Android Wear
        mAndroidWear = new PreferenceCategory(this);
        mAndroidWear.setTitle("Android Wear - Beta");
        root.addPreference(mAndroidWear);

        // Bluetooth
        mBluetooth = new PreferenceCategory(this);
        mBluetooth.setTitle("Bluetooth Devices");
        root.addPreference(mBluetooth);

        setPreferenceScreen(root);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAndroidWearDevices();
        getBluetoothDevices();
    }

    private void getBluetoothDevices() {
        if (mBluetoothStatus == null) {
            mBluetoothStatus = new Preference(this);
            mBluetoothStatus.setKey("bluetooth_status");
            mBluetoothStatus.setTitle("Bluetooth unavailable");
            mBluetoothStatus.setSummary("Bluetooth is turned off or you do not have any paired " +
                    "devices. Please enable and/or pair a device.");
        }

        mBluetooth.removeAll();

        Set<BluetoothDevice> pairedDevices = new BluetoothHelper(this).getPairedDevices();
        if (pairedDevices.size() == 0) {
            mBluetooth.addPreference(mBluetoothStatus);
        } else {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Checkbox preference
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(device.getAddress());
                checkboxPref.setTitle(device.getName());
                mBluetooth.addPreference(checkboxPref);
            }
        }
    }

    private void getAndroidWearDevices() {
        if (mAndroidWearStatus == null) {
            mAndroidWearStatus = new Preference(this);
            mAndroidWearStatus.setKey("android_wear_status");
        }
        mAndroidWearStatus.setTitle("Scanning...");

        mAndroidWear.removeAll();
        mAndroidWear.addPreference(mAndroidWearStatus);
        new AndroidWearHelper(this).getKnownDevices(this);
    }

    @Override
    public void onKnownDevicesLoaded(Map<String, String> devices) {
        mAndroidWear.removeAll();

        if (devices.size() > 0) {
            for(String key : devices.keySet()) {
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(key);
                checkboxPref.setTitle(devices.get(key));
                mAndroidWear.addPreference(checkboxPref);
            }
        } else {
            mAndroidWearStatus.setTitle("No devices found");
            mAndroidWear.addPreference(mAndroidWearStatus);
        }
    }

}
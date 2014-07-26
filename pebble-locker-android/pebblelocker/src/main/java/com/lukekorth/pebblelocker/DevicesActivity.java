package com.lukekorth.pebblelocker;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.activeandroid.query.Select;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;

import java.util.List;
import java.util.Set;

public class DevicesActivity extends PreferenceActivity implements AndroidWearHelper.Listener {

    private PreferenceCategory mAndroidWear;
    private Preference mAndroidWearStatus;
    private AndroidWearHelper mAndroidWearHelper;
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
        mAndroidWearHelper = new AndroidWearHelper(this);

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

    Preference.OnPreferenceChangeListener bluetoothPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            BluetoothDevices device = new Select()
                    .from(BluetoothDevices.class)
                    .where("address = ?", preference.getKey())
                    .executeSingle();

            if (device == null) {
                device = new BluetoothDevices();
                device.name = preference.getTitle().toString();
                device.address = preference.getKey();
                device.connected = false;
            }

            device.trusted = Boolean.parseBoolean(newValue.toString());
            device.save();
            return true;
        }
    };

    Preference.OnPreferenceChangeListener androidWearPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            mAndroidWearHelper.setDeviceTrusted(preference.getTitle().toString(), preference.getKey(),
                    Boolean.parseBoolean(newValue.toString()));
            return true;
        }
    };

    private void getBluetoothDevices() {
        if (mBluetoothStatus == null) {
            mBluetoothStatus = new Preference(this);
            mBluetoothStatus.setKey("bluetooth_status");
            mBluetoothStatus.setTitle("Bluetooth unavailable");
            mBluetoothStatus.setSummary("Bluetooth is turned off or you do not have any paired " +
                    "devices. Please enable and/or pair a device.");
        }

        mBluetooth.removeAll();

        Set<BluetoothDevice> pairedDevices = BluetoothDevices.getPairedDevices();
        if (pairedDevices.size() == 0) {
            mBluetooth.addPreference(mBluetoothStatus);
        } else {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Checkbox preference
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(device.getAddress());
                checkboxPref.setTitle(device.getName());
                checkboxPref.setOnPreferenceChangeListener(bluetoothPreferenceListener);
                mBluetooth.addPreference(checkboxPref);
            }
        }
    }

    private void getAndroidWearDevices() {
        if (mAndroidWearStatus == null) {
            mAndroidWearStatus = new Preference(this);
            mAndroidWearStatus.setKey("android_wear_status");
        }

        mAndroidWear.removeAll();
        mAndroidWearStatus.setTitle("Scanning...");
        mAndroidWear.addPreference(mAndroidWearStatus);

        new AndroidWearHelper(this).getConnectedDevices(this, true);
    }

    @Override
    public void onKnownDevicesLoaded(List<AndroidWearDevices> devices) {
        mAndroidWear.removeAll();

        if (devices.size() > 0) {
            for(AndroidWearDevices device : devices) {
                CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
                checkboxPref.setKey(device.deviceId);
                checkboxPref.setTitle(device.name);
                checkboxPref.setOnPreferenceChangeListener(androidWearPreferenceListener);
                mAndroidWear.addPreference(checkboxPref);
            }
        } else {
            mAndroidWearStatus.setTitle("No devices found");
            mAndroidWear.addPreference(mAndroidWearStatus);
        }
    }

}
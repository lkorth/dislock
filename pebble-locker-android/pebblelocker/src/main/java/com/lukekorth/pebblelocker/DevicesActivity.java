package com.lukekorth.pebblelocker;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.activeandroid.query.Select;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.services.LockingIntentService;

import java.util.List;
import java.util.Set;

public class DevicesActivity extends PremiumFeaturesActivity {

    private PreferenceScreen mPreferenceScreen;

    private PreferenceCategory mAndroidWear;
    private Preference mAndroidWearStatus;
    private PreferenceCategory mBluetooth;
    private Preference mBluetoothStatus;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferenceScreen = getPreferenceManager().createPreferenceScreen(this);

        addPebbleOption();

        mAndroidWear = new PreferenceCategory(this);
        mAndroidWear.setTitle(R.string.android_wear);
        mPreferenceScreen.addPreference(mAndroidWear);

        mBluetooth = new PreferenceCategory(this);
        mBluetooth.setTitle(R.string.bluetooth_devices_preference_category);
        mPreferenceScreen.addPreference(mBluetooth);

        setPreferenceScreen(mPreferenceScreen);
    }

    @Override
    public void onResume() {
        super.onResume();
        getAndroidWearDevices();
        getBluetoothDevices();
    }

    private void addPebbleOption() {
        PreferenceCategory pebble = new PreferenceCategory(this);
        pebble.setTitle(R.string.pebble);
        mPreferenceScreen.addPreference(pebble);
        CheckBoxPreference pref = new CheckBoxPreference(this);
        pref.setKey("pebble");
        pref.setTitle(R.string.any_pebble);
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean trusted = Boolean.parseBoolean(newValue.toString());
                if (!trusted || !isPurchaseRequired()) {
                    handleLocking();
                    getEnabledDevices();
                    return true;
                }
                return false;
            }
        });
        pref.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pebble", false));
        pebble.addPreference(pref);
    }

    private void getBluetoothDevices() {
        mBluetooth.removeAll();

        Set<BluetoothDevice> pairedDevices = BluetoothDevices.getPairedDevices();
        if (pairedDevices.size() == 0) {
            if (mBluetoothStatus == null) {
                mBluetoothStatus = new Preference(this);
                mBluetoothStatus.setKey("bluetooth_status");
                mBluetoothStatus.setTitle("Bluetooth unavailable");
                mBluetoothStatus.setSummary("Bluetooth is turned off or you do not have any paired " +
                        "devices. Please enable and/or pair a device.");
            }

            mBluetooth.addPreference(mBluetoothStatus);
        } else {
            for (BluetoothDevice device : pairedDevices) {
                CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setKey(device.getAddress());
                pref.setTitle(device.getName());
                pref.setOnPreferenceChangeListener(bluetoothPreferenceListener);
                mBluetooth.addPreference(pref);
            }
        }
    }

    private void getAndroidWearDevices() {
        mAndroidWear.removeAll();

        List<AndroidWearDevices> devices = AndroidWearDevices.getDevices();
        if (devices.size() == 0) {
            if (mAndroidWearStatus == null) {
                mAndroidWearStatus = new Preference(this);
                mAndroidWearStatus.setKey("android_wear_status");
                mAndroidWearStatus.setTitle("No devices found");
            }

            mAndroidWear.addPreference(mAndroidWearStatus);
        } else {
            for(AndroidWearDevices device : devices) {
                CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setKey(device.deviceId);
                pref.setTitle(device.name);
                pref.setOnPreferenceChangeListener(androidWearPreferenceListener);
                mAndroidWear.addPreference(pref);
            }
        }
    }

    Preference.OnPreferenceChangeListener bluetoothPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean trusted = Boolean.parseBoolean(newValue.toString());
            if (!trusted || !isPurchaseRequired()) {
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

                device.trusted = trusted;
                device.save();
                handleLocking();
                getEnabledDevices();
                return true;
            }
            return false;
        }
    };

    Preference.OnPreferenceChangeListener androidWearPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean trusted = Boolean.parseBoolean(newValue.toString());
            if (!trusted || !isPurchaseRequired()) {
                AndroidWearDevices device = new Select()
                        .from(AndroidWearDevices.class)
                        .where("deviceId = ?", preference.getKey())
                        .executeSingle();

                device.trusted = trusted;
                device.save();
                handleLocking();
                getEnabledDevices();
                return true;
            }
            return false;
        }
    };

    private void handleLocking() {
        startService(new Intent(this, LockingIntentService.class));
    }

}
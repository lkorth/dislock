package com.lukekorth.pebblelocker.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ApplicationTestCase;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;

import org.mockito.MockitoAnnotations;

public class BaseApplicationTestCase extends ApplicationTestCase<PebbleLockerApplication> {

    protected SharedPreferences mPrefs;

    public BaseApplicationTestCase() {
        super(PebbleLockerApplication.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);
        PebbleLockerApplication.sIsRunningInTestHarness = true;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        createApplication();
    }

    @Override
    public void tearDown() throws Exception {
        mPrefs.edit().clear().apply();
        getContext().deleteDatabase("pebble_locker.db");
        getContext().deleteDatabase("pebble-locker-logger");
        super.tearDown();
    }

    protected AndroidWearDevices createAndroidWearDevice(String name, String id, boolean connected,
                                                         boolean trusted) {
        AndroidWearDevices device = new AndroidWearDevices();
        device.name = name;
        device.deviceId = id;
        device.connected = connected;
        device.trusted = trusted;
        device.save();
        return device;
    }

    protected BluetoothDevices createBluetoothDevice(String name, String address, boolean connected, boolean trusted) {
        BluetoothDevices device = new BluetoothDevices();
        device.name = name;
        device.address = address;
        device.connected = connected;
        device.trusted = trusted;
        device.save();
        return device;
    }

}

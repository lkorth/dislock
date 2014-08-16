package com.lukekorth.pebblelocker.models;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Table(name = "BluetoothDevices")
public class BluetoothDevices extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "address")
    public String address;

    @Column(name = "connected")
    public boolean connected;

    @Column(name = "trusted")
    public boolean trusted;

    public BluetoothDevices() {
        super();
    }

    public static Set<BluetoothDevice> getPairedDevices() {
        Set<BluetoothDevice> pairedDevices;
        try {
            pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        } catch (NullPointerException e) {
            pairedDevices = null;
        }

        if (pairedDevices == null) {
            pairedDevices = new HashSet<BluetoothDevice>();
        }

        return pairedDevices;
    }

    public static boolean isADeviceTrusted() {
        return new Select()
                .from(BluetoothDevices.class)
                .where("trusted = ?", true)
                .exists();
    }

    public static boolean isTrustedDeviceConnected() {
        return new Select()
                .from(BluetoothDevices.class)
                .where("connected = ?", true)
                .where("trusted = ?", true)
                .exists();
    }

    public static List<BluetoothDevices> getConnectedDevices() {
        return new Select()
                .from(BluetoothDevices.class)
                .where("connected = ?", true)
                .execute();
    }

    public static void setDeviceConnected(BluetoothDevice device, boolean connected) {
        BluetoothDevices persistedDevice = new Select()
                .from(BluetoothDevices.class)
                .where("address = ?", device.getAddress())
                .executeSingle();

        if (persistedDevice == null) {
            persistedDevice = new BluetoothDevices();
            persistedDevice.name = device.getName();
            persistedDevice.address = device.getAddress();
            persistedDevice.trusted = false;
        }

        persistedDevice.connected = connected;
        persistedDevice.save();
    }

    public static String getConnectionStatus() {
        String status = "";
        if(BluetoothDevices.isTrustedDeviceConnected()) {
            for(BluetoothDevices device : BluetoothDevices.getConnectedDevices()) {
                status += device.name + " connected\n";
            }
        }

        return status.trim();
    }
}

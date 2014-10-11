package com.lukekorth.pebblelocker.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.google.android.gms.wearable.Node;

import java.util.List;

@Table(name = "AndroidWearDevices")
public class AndroidWearDevices extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "deviceId")
    public String deviceId;

    @Column(name = "connected")
    public boolean connected;

    @Column(name = "trusted")
    public boolean trusted;

    public AndroidWearDevices() {
        super();
    }

    public static List<AndroidWearDevices> getDevices() {
        return new Select()
                .from(AndroidWearDevices.class)
                .execute();
    }

    public static boolean isTrustedDeviceConnected() {
        return new Select()
                .from(AndroidWearDevices.class)
                .where("connected = ?", true)
                .where("trusted = ?", true)
                .exists();
    }

    public static int countTrustedDevices() {
        return new Select()
                .from(AndroidWearDevices.class)
                .where("trusted = ?", true)
                .count();
    }

    public static void setDeviceConnected(Node node, boolean connected) {
        AndroidWearDevices persistedDevice = new Select()
                .from(AndroidWearDevices.class)
                .where("deviceId = ?", node.getId())
                .executeSingle();

        if (persistedDevice == null) {
            persistedDevice = new AndroidWearDevices();
            persistedDevice.name = node.getDisplayName();
            persistedDevice.deviceId = node.getId();
            persistedDevice.trusted = false;
        }

        persistedDevice.connected = connected;
        persistedDevice.save();
    }

    public static String getConnectionStatus() {
        if (AndroidWearDevices.isTrustedDeviceConnected()) {
            return "Android Wear Connected";
        }

        return null;
    }

}

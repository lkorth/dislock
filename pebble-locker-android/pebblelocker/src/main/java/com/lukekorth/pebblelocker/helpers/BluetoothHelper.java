package com.lukekorth.pebblelocker.helpers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothHelper {

    private Context mContext;
    private Logger mLogger;

    public BluetoothHelper(Context context) {
        mContext = context;
        mLogger = new Logger(context, "[BLUETOOTH-HELPER]");
    }

    public BluetoothHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    }

    public boolean isTrustedDeviceConnected() {
        ArrayList<String> connectedBluetoothDevices = getConnectedDevices();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        for (String address : connectedBluetoothDevices) {
            mLogger.log("Connected bluetooth address: " + address);

            if (prefs.getBoolean(address, false)) {
                return true;
            }
        }

        return false;
    }

    public void setDeviceStatus(String name, String address, boolean connected) {
        ContentValues cv = new ContentValues();
        cv.put("address", address);
        cv.put("connected", connected ? 1 : 0);

        SQLiteDatabase db = new BluetoothDatabase(mContext).getWritableDatabase();
        db.insertWithOnConflict(BluetoothDatabase.BLUETOOTH_DEVICES, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        mLogger.log("Bluetooth device " + (connected ? "connected" : "disconnected") + ": " + name +
                " " + address);
    }

    public ArrayList<String> getConnectedDevices() {
        SQLiteDatabase db = new BluetoothDatabase(mContext).getReadableDatabase();
        Cursor cursor = db.query(BluetoothDatabase.BLUETOOTH_DEVICES, new String[] { "address" },
                "connected == ?", new String[] { "1" }, null, null, null);

        ArrayList<String> connectedDevices = new ArrayList<String>();
        while(cursor.moveToNext()) {
            connectedDevices.add(cursor.getString(cursor.getColumnIndex("address")));
        }

        cursor.close();
        db.close();

        return connectedDevices;
    }

    public String getConnectionStatus() {
        if(isTrustedDeviceConnected()) {
            return "Bluetooth device(s) connected \n\t" + getConnectedDeviceNames();
        }

        return null;
    }

    private String getConnectedDeviceNames() {
        ArrayList<String> connectedBluetoothDevices = getConnectedDevices();
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        String deviceNames = "";
        if (connectedBluetoothDevices.size() > 0 && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (connectedBluetoothDevices.contains(device.getAddress())) {
                    deviceNames += device.getName() + ",";
                }
            }

            if(deviceNames.length() > 0)
                deviceNames = deviceNames.substring(0, deviceNames.length() - 1) + ")";
        }

        return deviceNames;
    }

    private class BluetoothDatabase extends SQLiteOpenHelper {

        private static final String BLUETOOTH_DEVICES = "connectedBluetoothDevices";

        public BluetoothDatabase(Context context) {
            super(context, "pebble-locker", null, BuildConfig.VERSION_CODE);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + BLUETOOTH_DEVICES + " (address TEXT PRIMARY KEY, connected INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS log");
            onCreate(db);
        }
    }
}

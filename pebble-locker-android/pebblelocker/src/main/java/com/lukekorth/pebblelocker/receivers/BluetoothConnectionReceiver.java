package com.lukekorth.pebblelocker.receivers;

import android.bluetooth.BluetoothDevice;

import com.lukekorth.pebblelocker.models.BluetoothDevices;

public class BluetoothConnectionReceiver extends BaseBroadcastReceiver {

    private static final String BLUETOOTH_CONNECTED = "android.bluetooth.device.action.acl_connected";
    private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";

    @Override
    protected void onReceive() {
        BluetoothDevice device = mIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (mAction.equals(BLUETOOTH_CONNECTED) && device != null) {
            mLogger.debug("Setting bluetooth device " + device.getName() + " " + device.getAddress() +
                    " connected");
            BluetoothDevices.setDeviceConnected(device, true);
        } else if (mAction.equals(BLUETOOTH_DISCONNECTED) && device != null) {
            mLogger.debug("Setting bluetooth device " + device.getName() + " " + device.getAddress() +
                    " disconnected");
            BluetoothDevices.setDeviceConnected(device, false);
        }
    }

    @Override
    protected void handle() {
        if (BluetoothDevices.isADeviceTrusted()) {
            if (mAction.equals(BLUETOOTH_CONNECTED)) {
                mLogger.debug("Bluetooth connected, attempting unlock");
                handleLocking();
            } else if (mAction.equals(BLUETOOTH_DISCONNECTED)) {
                mLogger.debug("Bluetooth disconnected, attempting lock with delay");
                lockWithDelay();
            }
        }
    }

}

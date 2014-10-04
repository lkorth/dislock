package com.lukekorth.pebblelocker.receivers;

import android.bluetooth.BluetoothAdapter;

import com.lukekorth.pebblelocker.models.BluetoothDevices;

public class BluetoothReceiver extends BaseBroadcastReceiver {

    private static final String BLUETOOTH_STATE_CHANGED = "android.bluetooth.adapter.action.state_changed";

    private boolean mDisconnecting;

    @Override
    protected void onReceive() {
        checkIntent();
        if (mDisconnecting) {
            BluetoothDevices.setAllDevicesDisconnected();
        }
    }

    @Override
    protected void handle() {
        if (mDisconnecting) {
            handleLocking();
        }
    }

    private void checkIntent() {
        mDisconnecting = mAction.equals(BLUETOOTH_STATE_CHANGED);
        int state = mIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        mDisconnecting = mDisconnecting &&
                (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF);
    }

}

package com.lukekorth.pebblelocker.receivers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.models.BluetoothDevices;

public class ConnectionReceiver extends BaseBroadcastReceiver {

    private static final String BOOT_ACTION            = "android.intent.action.boot_completed";
	private static final String PEBBLE_CONNECTED       = "com.getpebble.action.pebble_connected";
	private static final String PEBBLE_DISCONNECTED    = "com.getpebble.action.pebble_disconnected";
	private static final String BLUETOOTH_CONNECTED    = "android.bluetooth.device.action.acl_connected";
	private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";
	public static final String ANDROID_WEAR_CONNECTED  = "com.lukekorth.pebblelocker.android_wear_connected";
    public static final String ANDROID_WEAR_DISCONNECTED = "com.lukekorth.pebblelocker.android_wear_disconnected";
    private static final String CONNECTIVITY_CHANGE    = "android.net.conn.connectivity_change";
	private static final String USER_PRESENT           = "android.intent.action.user_present";
	public  static final String LOCKED                 = "locked";

	private String mAction;
    private Intent mIntent;
    private DeviceHelper mDeviceHelper;
    private boolean mTrustedWifiConnected;

	@Override
	public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
	    mAction = intent.getAction().toLowerCase();
        mIntent = intent;
        mDeviceHelper = new DeviceHelper(context, mLogger);
        mTrustedWifiConnected = new WifiHelper(context, mLogger).isTrustedWifiConnected();

        mLogger.log("ConnectionReceiver: " + mAction);

        checkForBluetoothDevice();

        mDeviceHelper.sendLockStatusChangedEvent();

        LockState lockState = LockState.getCurrentState(mContext);
        if (lockState == LockState.AUTO) {
            if (mAction.equals(BOOT_ACTION)) {
                mLogger.log("Phone completed booting, running locking...");
                new Locker(context, mTag).handleLocking(false, true);
            } else if (isUnlockScheduled()) {
                mLogger.log("User present and need to unlock...attempting to unlock");
                new Locker(context, mTag).handleLocking(false, true);
            } else if (isUnlockNeeded()) {
                mLogger.log("Attempting unlock");
                new Locker(context, mTag).handleLocking(false, true);
            } else if (isLockNeeded()) {
                mLogger.log("Attempting lock");
                new Locker(context, mTag).handleLocking(true, true);
            }
        } else {
            mLogger.log("Lock state was manually set to " + lockState.getDisplayName());
        }
	}

	private void checkForBluetoothDevice() {
        BluetoothDevice device = mIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		if (mAction.equals(BLUETOOTH_CONNECTED) && device != null) {
            mLogger.log("Setting bluetooth device " + device.getName() + " " + device.getAddress() +
                " connected");
            BluetoothDevices.setDeviceConnected(device, true);
		} else if (mAction.equals(BLUETOOTH_DISCONNECTED) && device != null) {
            mLogger.log("Setting bluetooth device " + device.getName() + " " + device.getAddress() +
                    " disconnected");
            BluetoothDevices.setDeviceConnected(device, false);
		}
	}

    private boolean isUnlockScheduled() {
        return (mAction.equals(USER_PRESENT) && mDeviceHelper.isUnlockNeeded());
    }

    private boolean isUnlockNeeded() {
        return ((mAction.equals(PEBBLE_CONNECTED) ||
                 mAction.equals(BLUETOOTH_CONNECTED) ||
                 mAction.equals(ANDROID_WEAR_CONNECTED) ||
                (mAction.equals(CONNECTIVITY_CHANGE) && mTrustedWifiConnected))
              && mDeviceHelper.isLocked(true));
    }

    private boolean isLockNeeded() {
        return ((mAction.equals(PEBBLE_DISCONNECTED) ||
                 mAction.equals(BLUETOOTH_DISCONNECTED) ||
                 mAction.equals(ANDROID_WEAR_DISCONNECTED) ||
                (mAction.equals(CONNECTIVITY_CHANGE) && !mTrustedWifiConnected))
             && !mDeviceHelper.isLocked(false));
    }

}

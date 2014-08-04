package com.lukekorth.pebblelocker.receivers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.services.LockerService;

public class ConnectionReceiver extends BaseBroadcastReceiver {

	private static final String PEBBLE_CONNECTED       = "com.getpebble.action.pebble_connected";
	private static final String PEBBLE_DISCONNECTED    = "com.getpebble.action.pebble_disconnected";
	private static final String BLUETOOTH_CONNECTED    = "android.bluetooth.device.action.acl_connected";
	private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";
	public static final String ANDROID_WEAR_CONNECTED = "com.lukekorth.pebblelocker.android_wear_connected";
    public static final String ANDROID_WEAR_DISCONNECTED = "com.lukekorth.pebblelocker.android_wear_disconnected";
    private static final String CONNECTIVITY_CHANGE    = "android.net.conn.connectivity_change";
	private static final String USER_PRESENT           = "android.intent.action.user_present";
	public  static final String LOCKED                 = "locked";

	private String mAction;
    private Intent mIntent;
    private DeviceHelper mDeviceHelper;
    private boolean mTrustedWifiConnected;

    public ConnectionReceiver(Context context, Intent intent, String tag, Logger logger,
                              DeviceHelper deviceHelper, boolean trustedWifiConnected) {
        mContext = context;
        mTag = tag;
        mLogger = logger;
        mAction = intent.getAction().toLowerCase();
        mIntent = intent;
        mDeviceHelper = deviceHelper;
        mTrustedWifiConnected = trustedWifiConnected;
    }

	@Override
	public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
	    mAction = intent.getAction().toLowerCase();
        mIntent = intent;
        mDeviceHelper = new DeviceHelper(context, mLogger);
        mTrustedWifiConnected = new WifiHelper(context, mLogger).isTrustedWifiConnected();

        mLogger.log("ConnectionReceiver: " + mAction);

        Intent actionIntent = handleBroadcast();
        if (actionIntent != null) {
            BaseBroadcastReceiver.startWakefulService(mContext, actionIntent);
        }
	}

    public Intent handleBroadcast() {
        checkForBluetoothDevice();

        mDeviceHelper.sendLockStatusChangedEvent();

        Intent actionIntent = null;
        LockState lockState = LockState.getCurrentState(mContext);
        if (lockState == LockState.AUTO) {
            if (isUnlockScheduled()) {
                mLogger.log("User present and need to unlock...attempting to unlock");
                actionIntent = getActionIntent().putExtra(LockerService.WITH_DELAY, false);
            } else if (isUnlockNeeded()) {
                mLogger.log("Attempting unlock");
                actionIntent = getActionIntent().putExtra(LockerService.WITH_DELAY, false);
            } else if (isLockNeeded()) {
                mLogger.log("Attempting lock");
                actionIntent = getActionIntent();
            }
        } else {
            mLogger.log("Lock state was manually set to " + lockState.getDisplayName());
        }

        return actionIntent;
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

    public Intent getActionIntent() {
        Intent intent = new Intent(mContext, LockerService.class);
        intent.putExtra(LockerService.TAG, mTag);
        return intent;
    }

    public boolean isUnlockScheduled() {
        return (mAction.equals(USER_PRESENT) && mDeviceHelper.isUnlockNeeded());
    }

    public boolean isUnlockNeeded() {
        return ((mAction.equals(PEBBLE_CONNECTED) ||
                 mAction.equals(BLUETOOTH_CONNECTED) ||
                 mAction.equals(ANDROID_WEAR_CONNECTED) ||
                (mAction.equals(CONNECTIVITY_CHANGE) && mTrustedWifiConnected))
              && mDeviceHelper.isLocked(true));
    }

    public boolean isLockNeeded() {
        return ((mAction.equals(PEBBLE_DISCONNECTED) ||
                 mAction.equals(BLUETOOTH_DISCONNECTED) ||
                 mAction.equals(ANDROID_WEAR_DISCONNECTED) ||
                (mAction.equals(CONNECTIVITY_CHANGE) && !mTrustedWifiConnected))
             && !mDeviceHelper.isLocked(false));
    }

}

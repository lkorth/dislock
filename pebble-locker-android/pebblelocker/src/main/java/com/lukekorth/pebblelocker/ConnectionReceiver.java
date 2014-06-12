package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;

public class ConnectionReceiver extends BaseBroadcastReceiver {

	private static final String PEBBLE_CONNECTED       = "com.getpebble.action.pebble_connected";
	private static final String PEBBLE_DISCONNECTED    = "com.getpebble.action.pebble_disconnected";
	private static final String BLUETOOTH_CONNECTED    = "android.bluetooth.device.action.acl_connected";
	private static final String BLUETOOTH_DISCONNECTED = "android.bluetooth.device.action.acl_disconnected";
	private static final String CONNECTIVITY_CHANGE    = "android.net.conn.connectivity_change";
	private static final String USER_PRESENT           = "android.intent.action.user_present";
	public  static final String STATUS_CHANGED_INTENT  = "com.lukekorth.pebblelocker.STATUS_CHANGED";
	public  static final String LOCKED                 = "locked";
	public  static final String UNLOCK                 = "unlock";
	public  static final String LOCK_STATE             = "state";

	private SharedPreferences mPrefs;
	private String mAction;

	@SuppressLint("DefaultLocale")
	@Override
	public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        acquireWakeLock();

        new DeviceHelper(context, mLogger).sendLockStatusChangedBroadcast();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mAction = intent.getAction().toLowerCase();

		mLogger.log("ConnectionReceiver: " + mAction);

		LockState lockState =
                LockState.getInstance(mPrefs.getInt(LOCK_STATE, LockState.AUTO.getState()));
		if (lockState == LockState.AUTO) {
			checkForBluetoothDevice(((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
			boolean isWifiConnected = isWifiConnected();

			if (mAction.equals(USER_PRESENT) && needToUnlock()) {
				mLogger.log("User present and need to unlock...attempting to unlock");
				new Locker(context, mTag).handleLocking();
			} else if ((mAction.equals(PEBBLE_CONNECTED) || mAction.equals(BLUETOOTH_CONNECTED) || (mAction.equals(CONNECTIVITY_CHANGE) && isWifiConnected)) && isLocked(true)) {
				mLogger.log("Attempting unlock");
				new Locker(context, mTag).handleLocking();
			} else if ((mAction.equals(PEBBLE_DISCONNECTED) || mAction.equals(BLUETOOTH_DISCONNECTED) || (mAction.equals(CONNECTIVITY_CHANGE) && !isWifiConnected)) && !isLocked(false)) {
				mLogger.log("Attempting lock");
                lockWithDelay();
			}
		} else {
			mLogger.log("Lock state was manually set to " + lockState.getDisplayName());
		}

        releaseWakeLock();
	}

    private void lockWithDelay() {
        int delay = Integer.parseInt(mPrefs.getString("key_grace_period", "2"));

        if (delay != 0) {
            mLogger.log("Sleeping for " + delay + " seconds");
            SystemClock.sleep(delay * 1000);
        }

        mLogger.log("Locking...");
        new Locker(mContext, mTag).handleLocking();
    }

    private boolean isLocked(boolean defaultValue) {
		boolean locked = mPrefs.getBoolean(LOCKED, defaultValue);
		mLogger.log("Locked: " + locked);

		return locked;
	}

	private boolean needToUnlock() {
		boolean needToUnlock = mPrefs.getBoolean(UNLOCK, true);
		mLogger.log("Need to unlock: " + needToUnlock);

		return needToUnlock;
	}

	private void checkForBluetoothDevice(BluetoothDevice device) {
		if (mAction.equals(BLUETOOTH_CONNECTED)) {
			new BluetoothHelper(mContext, mLogger).setDeviceStatus(device.getName(), device.getAddress(), true);
		} else if (mAction.equals(BLUETOOTH_DISCONNECTED)) {
			new BluetoothHelper(mContext, mLogger).setDeviceStatus(device.getName(), device.getAddress(), false);
		}
	}

	private boolean isWifiConnected() {
		NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo != null) {
			if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				return true;
			} else {
				mLogger.log("Network is not connected to wifi");
			}
		} else {
			mLogger.log("NetworkInfo is null");
		}

		return false;
	}
}

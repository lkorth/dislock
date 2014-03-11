package com.lukekorth.pebblelocker;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

public class ConnectionReceiver extends BroadcastReceiver {

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
	public  static final int    AUTO                   = 0;
	public  static final int    MANUAL_LOCKED          = 1;
	public  static final int    MANUAL_UNLOCKED        = 2;

	private Context mContext;
	private SharedPreferences mPrefs;
	private Logger mLogger;
	private String mUniq;
	private String mAction;

	@SuppressLint("DefaultLocale")
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context);
		mUniq = "[" + UUID.randomUUID().toString().split("-")[1] + "]";
		mAction = intent.getAction().toLowerCase();

		mLogger.log(mUniq, "ConnectionReceiver: " + mAction);

		int lockState = mPrefs.getInt(LOCK_STATE, AUTO);
		if (lockState == AUTO) {
			checkForBluetoothDevice(((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
			boolean isWifiConnected = isWifiConnected();

			if (mAction.equals(USER_PRESENT) && needToUnlock()) {
				mLogger.log(mUniq, "User present and need to unlock...attempting to unlock");
				new Locker(context, mUniq).handleLocking();
			} else if ((mAction.equals(PEBBLE_CONNECTED) || mAction.equals(BLUETOOTH_CONNECTED) || (mAction.equals(CONNECTIVITY_CHANGE) && isWifiConnected)) && isLocked(true)) {
				mLogger.log(mUniq, "Attempting unlock");
				new Locker(context, mUniq).handleLocking();
			} else if ((mAction.equals(PEBBLE_DISCONNECTED) || mAction.equals(BLUETOOTH_DISCONNECTED) || (mAction.equals(CONNECTIVITY_CHANGE) && !isWifiConnected)) && !isLocked(false)) {
				mLogger.log(mUniq, "Attempting lock");
				new Locker(context, mUniq).handleLocking();
			}
		} else {
			mLogger.log(mUniq, "Lock state was manually set to " + lockState);
		}
	}

	private boolean isLocked(boolean defaultValue) {
		boolean locked = mPrefs.getBoolean(LOCKED, defaultValue);
		mLogger.log(mUniq, "Locked: " + locked);

		return locked;
	}

	private boolean needToUnlock() {
		boolean needToUnlock = mPrefs.getBoolean(UNLOCK, true);
		mLogger.log(mUniq, "Need to unlock: " + needToUnlock);

		return needToUnlock;
	}

	private void checkForBluetoothDevice(BluetoothDevice device) {
		if (mAction.equals(BLUETOOTH_CONNECTED)) {
			new DatabaseHelper(mContext).setStatus(device.getAddress(), true);
			mLogger.log(mUniq, "Bluetooth device connected: " + device.getName() + " " + device.getAddress());
		} else if (mAction.equals(BLUETOOTH_DISCONNECTED)) {
			new DatabaseHelper(mContext).setStatus(device.getAddress(), false);
			mLogger.log(mUniq, "Bluetooth device disconnected: " + device.getName() + " " + device.getAddress());
		}
	}

	private boolean isWifiConnected() {
		NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (networkInfo != null) {
			if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				return true;
			} else {
				mLogger.log(mUniq, "Network is not connected to wifi");
			}
		} else {
			mLogger.log(mUniq, "NetworkInfo is null");
		}

		return false;
	}
}

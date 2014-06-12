package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;

public class Locker {

	private Context mContext;
	private SharedPreferences mPrefs;

    private Logger mLogger;
    private DeviceHelper mDeviceHelper;
    private WifiHelper mWifiHelper;
    private BluetoothHelper mBluetoothHelper;
    private PebbleHelper mPebbleHelper;
	private DevicePolicyManager mDPM;

	public Locker(Context context, String tag) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context, tag);
        mDeviceHelper = new DeviceHelper(context, mLogger);
        mWifiHelper = new WifiHelper(context, mLogger);
        mBluetoothHelper = new BluetoothHelper(context, mLogger);
        mPebbleHelper = new PebbleHelper(context, mLogger);
		mDPM = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}

    public Locker(Context context, String tag, DeviceHelper deviceHelper, WifiHelper wifiHelper,
                  BluetoothHelper bluetoothHelper, PebbleHelper pebbleHelper,
                  DevicePolicyManager dpm) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mLogger = new Logger(context, tag);
        mDeviceHelper = deviceHelper;
        mWifiHelper = wifiHelper;
        mBluetoothHelper = bluetoothHelper;
        mPebbleHelper = pebbleHelper;
        mDPM = dpm;
    }

	public void handleLocking() {
		handleLocking(true);
	}

	public void handleLocking(boolean forceLock) {
        boolean connectedToDeviceOrWifi = isConnectedToDeviceOrWifi();
		if (connectedToDeviceOrWifi && mDeviceHelper.isLocked(true)) {
            unlock();
        } else if (!connectedToDeviceOrWifi && !mDeviceHelper.isLocked(false)) {
            lock(forceLock);
        }
	}

	public void lock() {
		lock(true);
	}

	public void lock(boolean forceLock) {
		if (!enabled())
			return;

		boolean passwordChanged = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).putBoolean(ConnectionReceiver.UNLOCK, false).commit();

		mLogger.log("Successfully locked: " + passwordChanged);

		if (forceLock && mPrefs.getBoolean("key_force_lock", false))
			mDPM.lockNow();

        mDeviceHelper.sendLockStatusChangedBroadcast();
	}

	public void unlock() {
		if (!enabled())
			return;

		if (mDeviceHelper.isOnLockscreen() && mDeviceHelper.isScreenOn()) {
			mPrefs.edit().putBoolean(ConnectionReceiver.UNLOCK, true).commit();
			mLogger.log("Screen is on lockscreen, setting unlock true for future unlock");
		} else {
			boolean passwordChanged = false;
            boolean screen = mDeviceHelper.isScreenOn();

			try {
				passwordChanged = mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
				mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, false).commit();
			} catch (IllegalArgumentException e) {
                boolean passwordReset = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).commit();
                mLogger.log("There was an exception when setting the password to blank, setting it back. Successfully reset: " + passwordReset + " " + Log.getStackTraceString(e));
            }

            if(!screen && mDeviceHelper.isScreenOn() && passwordChanged) {
                mDPM.lockNow();
            }

			mPrefs.edit().putBoolean(ConnectionReceiver.UNLOCK, false).commit();

			mLogger.log("Successfully unlocked: " + passwordChanged);
		}

        mDeviceHelper.sendLockStatusChangedBroadcast();
	}

	public boolean enabled() {
		boolean activeAdmin = mDPM.isAdminActive(new ComponentName(mContext, CustomDeviceAdminReceiver.class));
		boolean enabled = mPrefs.getBoolean("key_enable_locker", false);
        boolean password = !(mPrefs.getString("key_password", "").equals(""));

		if (!activeAdmin) {
            mLogger.log("Not an active admin");
        }
		if (!enabled) {
            mLogger.log("key_enable_locker is false");
        }
        if (!password) {
            mLogger.log("User's password is empty");
        }

		return activeAdmin && enabled && password;
	}

	public boolean isConnectedToDeviceOrWifi() {
		boolean pebble = mPebbleHelper.isEnabledAndConnected();
		boolean bluetooth = mBluetoothHelper.isTrustedDeviceConnected();
		boolean wifi = mWifiHelper.isTrustedWifiConnected();

		mLogger.log("Pebble: " + pebble + " Bluetooth: " + bluetooth + " Wifi: " + wifi);

		return (pebble || bluetooth || wifi);
	}
}

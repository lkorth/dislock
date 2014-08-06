package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.lukekorth.pebblelocker.helpers.CustomDeviceAdminReceiver;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;

public class Locker {

	private Context mContext;
	private SharedPreferences mPrefs;

    private Logger mLogger;
    private DeviceHelper mDeviceHelper;
    private WifiHelper mWifiHelper;
    private PebbleHelper mPebbleHelper;
	private DevicePolicyManager mDPM;

	public Locker(Context context, String tag) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context, tag);
        mDeviceHelper = new DeviceHelper(context, mLogger);
        mWifiHelper = new WifiHelper(context, mLogger);
        mPebbleHelper = new PebbleHelper(context, mLogger);
		mDPM = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}

    public Locker(Context context, String tag, DeviceHelper deviceHelper, WifiHelper wifiHelper,
                  PebbleHelper pebbleHelper, DevicePolicyManager dpm) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mLogger = new Logger(context, tag);
        mDeviceHelper = deviceHelper;
        mWifiHelper = wifiHelper;
        mPebbleHelper = pebbleHelper;
        mDPM = dpm;
    }

	public void handleLocking(boolean withDelay, boolean forceLock) {
        if (withDelay) {
            int delay = Integer.parseInt(mPrefs.getString("key_grace_period", "2"));

            if (delay != 0) {
                mLogger.log("Sleeping for " + delay + " seconds");
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException e) {
                    mLogger.log("Sleep was interrupted: " + e.getMessage());
                }
            }

            mLogger.log("Grace period is over, handling locking...");
        }

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
		if (!enabled()) {
            return;
        }

		boolean passwordChanged = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false).apply();

		mLogger.log("Successfully locked: " + passwordChanged);

		if (forceLock && mPrefs.getBoolean("key_force_lock", false)) {
            mDPM.lockNow();
        }

        mDeviceHelper.sendLockStatusChangedEvent();
	}

	public void unlock() {
		if (!enabled()) {
            return;
        }

		if (mDeviceHelper.isOnLockscreen() && mDeviceHelper.isScreenOn()) {
			mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, true).apply();
			mLogger.log("Screen is on lock screen, setting unlock true for future unlock");
		} else if (mPrefs.getBoolean("key_require_password_on_reconnect", false) && !mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false)) {
            mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, true).apply();
            mLogger.log("Requiring user to re-authenticate once before unlocking");
        } else {
			boolean passwordChanged = false;
            boolean screen = mDeviceHelper.isScreenOn();

			try {
				passwordChanged = mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
				mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, false).apply();
			} catch (IllegalArgumentException e) {
                boolean passwordReset = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).apply();
                mLogger.log("There was an exception when setting the password to blank, setting it back. Successfully reset: " + passwordReset + " " + Log.getStackTraceString(e));
            }

            if(!screen && mDeviceHelper.isScreenOn() && passwordChanged) {
                mDPM.lockNow();
            }

			mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false).apply();

			mLogger.log("Successfully unlocked: " + passwordChanged);
		}

        mDeviceHelper.sendLockStatusChangedEvent();
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
        boolean wear = AndroidWearDevices.isTrustedDeviceConnected();
		boolean bluetooth = BluetoothDevices.isTrustedDeviceConnected();
		boolean wifi = mWifiHelper.isTrustedWifiConnected();

		mLogger.log("Pebble: " + pebble + " Wear: " + wear + " Bluetooth: " + bluetooth + " Wifi: " + wifi);

		return (pebble || wear || bluetooth || wifi);
	}

}

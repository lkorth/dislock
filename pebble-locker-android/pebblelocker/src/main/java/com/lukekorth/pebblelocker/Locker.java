package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.lukekorth.pebblelocker.helpers.CustomDeviceAdminReceiver;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.receivers.BaseBroadcastReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		mLogger = LoggerFactory.getLogger(tag);
        mDeviceHelper = new DeviceHelper(context, tag);
        mWifiHelper = new WifiHelper(context, tag);
        mPebbleHelper = new PebbleHelper(context, tag);
		mDPM = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}

    public Locker(Context context, String tag, DeviceHelper deviceHelper, WifiHelper wifiHelper,
                  PebbleHelper pebbleHelper, DevicePolicyManager dpm) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mLogger = LoggerFactory.getLogger(tag);
        mDeviceHelper = deviceHelper;
        mWifiHelper = wifiHelper;
        mPebbleHelper = pebbleHelper;
        mDPM = dpm;
    }

	public void handleLocking(boolean forceLock) {
        boolean connectedToDeviceOrWifi = isConnectedToDeviceOrWifi();
		if (connectedToDeviceOrWifi && mDeviceHelper.isLocked(true)) {
            unlock();
        } else if (!connectedToDeviceOrWifi && !mDeviceHelper.isLocked(false)) {
            lock(forceLock);
        }
	}

	public void lock(boolean forceLock) {
		if (!enabled()) {
            return;
        }

		boolean passwordChanged = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		mPrefs.edit().putBoolean(BaseBroadcastReceiver.LOCKED, true).putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false).apply();

		mLogger.debug("Successfully locked: " + passwordChanged);

		if (forceLock && mPrefs.getBoolean("key_force_lock", false)) {
            mDPM.lockNow();
        }

        mDeviceHelper.sendLockStatusChangedEvent();
	}

	public void unlock() {
		if (!enabled()) {
            return;
        }

        boolean needToTurnOffScreen = false;
		if (mDeviceHelper.isOnLockscreen() && mDeviceHelper.isScreenOn()) {
			mLogger.debug("Screen is on and on lock screen, turning off to unlock and then turning back on");
            needToTurnOffScreen = true;
	    }

        if (mPrefs.getBoolean("key_require_password_on_reconnect", false) && !mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false)) {
            mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, true).apply();
            mLogger.debug("Requiring user to re-authenticate once before unlocking");
        } else {
            if (needToTurnOffScreen) {
                mLogger.debug("Turning off screen");
                mDPM.lockNow();
            }

			boolean passwordChanged = false;
            boolean screen = mDeviceHelper.isScreenOn();
            mLogger.debug("Screen is currently on: " + screen);

			try {
				passwordChanged = mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
				mPrefs.edit().putBoolean(BaseBroadcastReceiver.LOCKED, false).apply();
			} catch (IllegalArgumentException e) {
                handleUnlockException(e);
            } catch (RuntimeException e) {
                handleUnlockException(e);
            }

            if(!screen && mDeviceHelper.isScreenOn() && passwordChanged && !needToTurnOffScreen) {
                mLogger.debug("Turning off screen because it turned on after unlock");
                mDPM.lockNow();
            }

            if (needToTurnOffScreen) {
                mLogger.debug("Waking screen because it was on when unlock started");
                PowerManager.WakeLock wakeLock = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "PebbleLocker-Locker");
                wakeLock.acquire();
                wakeLock.release();
            }

			mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false).apply();

			mLogger.debug("Successfully unlocked: " + passwordChanged);
		}

        mDeviceHelper.sendLockStatusChangedEvent();
	}

    private void handleUnlockException(Throwable throwable) {
        boolean passwordReset = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        mPrefs.edit().putBoolean(BaseBroadcastReceiver.LOCKED, true).apply();
        mLogger.error("There was an exception when setting the password to blank, setting it back. Successfully reset: " + passwordReset);
        mLogger.error("Error: " + Log.getStackTraceString(throwable));
    }

	public boolean enabled() {
		boolean activeAdmin = mDPM.isAdminActive(new ComponentName(mContext, CustomDeviceAdminReceiver.class));
	    boolean isSlide = (ScreenLockType.getCurrent(mContext) == ScreenLockType.SLIDE);
        boolean password = !(mPrefs.getString("key_password", "").equals(""));

		if (!activeAdmin) {
            mLogger.error("Not an active admin");
        }
		if (isSlide) {
            mLogger.error("ScreenLockType is set to SLIDE");
        }
        if (!password) {
            mLogger.error("User's password is empty");
        }

		return activeAdmin && !isSlide && password;
	}

	public boolean isConnectedToDeviceOrWifi() {
		boolean pebble = mPebbleHelper.isEnabledAndConnected();
        boolean wear = AndroidWearDevices.isTrustedDeviceConnected();
		boolean bluetooth = BluetoothDevices.isTrustedDeviceConnected();
		boolean wifi = mWifiHelper.isTrustedWifiConnected();

		mLogger.debug("Pebble: " + pebble + " Wear: " + wear + " Bluetooth: " + bluetooth + " Wifi: " + wifi);

		return (pebble || wear || bluetooth || wifi);
	}

}

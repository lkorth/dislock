package com.lukekorth.pebblelocker.services;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.helpers.ThreadBus;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.models.LockState;
import com.lukekorth.pebblelocker.receivers.DislockDeviceAdminReciever;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockerService extends Service {

    public static final String EXTRA_FORCE_LOCK = "com.lukekorth.pebblelocker.FORCE_LOCK";

    private Logger mLogger;
    private ThreadBus mBus;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private boolean mForceLock;
    private boolean mTriggeringLock = false;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();

        mLogger = LoggerFactory.getLogger("LockerServices");
        mLogger.debug("onCreate");
        mBus = PebbleLockerApplication.getBus();
        mKeyguardLock = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE))
                .newKeyguardLock("Dislock:LockerService");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mLogger.warn("Received onTrimMemory call with level " + level);
    }

    @Override
    public void onDestroy() {
        lock();
        mLogger.debug("onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        mLogger.debug("onStartCommand");

        mForceLock = intent == null || intent.getBooleanExtra(EXTRA_FORCE_LOCK, true);

        handleLocking();
        return START_STICKY;
    }

    private void handleLocking() {
        switch (LockState.getCurrentState(this)) {
            case AUTO:
                if (hasTrustedConnection()) {
                    unlock();
                } else {
                    mTriggeringLock = true;
                    stopSelf(); // triggers onDestroy which calls lock()
                }
                break;
            case MANUAL_LOCKED:
                mTriggeringLock = true;
                stopSelf(); // triggers onDestroy which calls lock()
                break;
            case MANUAL_UNLOCKED:
                unlock();
                break;
        }
    }

    private void lock() {
        mKeyguardLock.reenableKeyguard();
        Settings.setLocked(this, true);
        mBus.post(new StatusChangedEvent());

        mLogger.debug("Locked");

        if (mForceLock && mTriggeringLock) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName deviceAdmin = new ComponentName(this, DislockDeviceAdminReciever.class);
            if (devicePolicyManager.isAdminActive(deviceAdmin)) {
                devicePolicyManager.lockNow();
                mLogger.debug("Force lock requested and device admin active, turning screen off");
            }
        }
    }

    private void unlock() {
        mKeyguardLock.disableKeyguard();
        Settings.setLocked(this, false);
        Settings.setNeedToUnlock(this, false);
        mBus.post(new StatusChangedEvent());

        mLogger.debug("Unlocked");
    }

    private boolean hasTrustedConnection() {
        boolean pebble = new PebbleHelper(this).isEnabledAndConnected();
        boolean wear = AndroidWearDevices.isTrustedDeviceConnected();
        boolean bluetooth = BluetoothDevices.isTrustedDeviceConnected();
        boolean wifi = new WifiHelper(this).isTrustedWifiConnected();

        mLogger.debug("Pebble: " + pebble + " Wear: " + wear + " Bluetooth: " + bluetooth + " Wifi: " + wifi);

        return (pebble || wear || bluetooth || wifi);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

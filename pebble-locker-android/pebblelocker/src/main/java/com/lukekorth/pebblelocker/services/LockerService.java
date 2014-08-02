package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;

public class LockerService extends IntentService {

    public static final String TAG = "tag";
    public static final String WITH_DELAY = "with_delay";
    public static final String FORCE_LOCK = "force_lock";

    private static final String WAKE_LOCK_TAG = "LockerServiceWakeLock";

    public LockerService() {
        super("LockerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String tag = intent.getStringExtra(TAG);
        Logger logger = new Logger(this, tag);
        logger.log("Initializing service and acquiring wake lock to run locking");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire();

        logger.log("Wake lock acquired, removing BroadcastReceiver wake lock and running locking...");

        boolean wakeLockRemoved = BaseBroadcastReceiver.completeWakefulIntent(this, intent);
        logger.log("BroadcastReceiver wake lock removed: " + wakeLockRemoved);

        Locker locker = new Locker(this, tag);
        locker.handleLocking(intent.getBooleanExtra(WITH_DELAY, true),
                intent.getBooleanExtra(FORCE_LOCK, true));

        DeviceHelper.sendLockStatusChangedBroadcast(this);

        logger.log("Done locking, releasing wake lock");
        wakeLock.release();
    }
}

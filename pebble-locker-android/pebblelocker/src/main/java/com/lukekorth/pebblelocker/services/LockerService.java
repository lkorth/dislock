package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;

public class LockerService extends IntentService {

    public static final String TAG = "tag";
    public static final String WITH_DELAY = "with_delay";
    public static final String FORCE_LOCK = "force_lock";

    public LockerService() {
        super("LockerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String tag = intent.getStringExtra(TAG);
        Logger logger = new Logger(this, tag);

        logger.log("Starting service to run locking");

        Locker locker = new Locker(this, tag);
        locker.handleLocking(intent.getBooleanExtra(WITH_DELAY, true),
                intent.getBooleanExtra(FORCE_LOCK, true));

        DeviceHelper.sendLockStatusChangedBroadcast(this);

        boolean wakeLockRemoved = WakefulBroadcastReceiver.completeWakefulIntent(intent);
        logger.log("WakeLock removed: " + wakeLockRemoved);
    }
}

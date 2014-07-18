package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;

public class LockerService extends IntentService {

    public static final String TAG = "tag";
    public static final String WITH_DELAY = "with_delay";
    public static final String FORCE_LOCK = "force_lock";

    public LockerService() {
        super("LockerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Locker locker = new Locker(this, intent.getStringExtra(TAG));
        locker.handleLocking(intent.getBooleanExtra(WITH_DELAY, true),
                intent.getBooleanExtra(FORCE_LOCK, true));

        DeviceHelper.sendLockStatusChangedBroadcast(this);

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}

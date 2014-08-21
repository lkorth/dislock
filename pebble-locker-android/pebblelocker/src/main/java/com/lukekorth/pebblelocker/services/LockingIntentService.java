package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;

public class LockingIntentService extends IntentService {

    public static final String LOCK = "lock_key";

    private static final String TAG = "Locking_Intent_Service";

    public LockingIntentService() {
        super("LockStateIntentService");
    }

    public LockingIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(LOCK, false) &&
                new DeviceHelper(this, TAG).isLocked(false)) {
            new Locker(this, TAG).lock(false);
        } else if (LockState.getCurrentState(this) == LockState.AUTO) {
            new Locker(this, TAG).handleLocking(false);
            PebbleLockerApplication.getBus().post(new StatusChangedEvent());
        }
    }

}

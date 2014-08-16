package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;

public class LockingIntentService extends IntentService {

    public static final String LOCK = "lock_key";

    private static final String TAG = "[LOCKING-INTENT-SERVICE]";

    public LockingIntentService() {
        super("LockStateIntentService");
    }

    public LockingIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(LOCK, false) &&
                new DeviceHelper(this, new Logger(this, TAG)).isLocked(false)) {
            new Locker(this, TAG).lock(false);
        } else if (LockState.getCurrentState(this) == LockState.AUTO) {
            new Locker(this, TAG).handleLocking(false);
            PebbleLockerApplication.getBus().post(new StatusChangedEvent());
        }
    }

}

package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;

public class LockingIntentService extends IntentService {

    public LockingIntentService() {
        super("LockStateIntentService");
    }

    public LockingIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (LockState.getCurrentState(this) == LockState.AUTO) {
            new Locker(this, "[LOCKING-INTENT-SERVICE]").handleLocking(false);
            PebbleLockerApplication.getBus().post(new StatusChangedEvent());
        }
    }

}

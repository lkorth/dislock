package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;

import org.slf4j.LoggerFactory;

public class LockStateIntentService extends IntentService {

    public LockStateIntentService() {
        super("LockStateIntentService");
    }

    public LockStateIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String tag = PebbleLockerApplication.getUniqueTag();
        LoggerFactory.getLogger(tag).debug("Got intent to switch to the next lock state");
        LockState.switchToNextState(this, tag, false);
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }
}

package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.logging.Logger;

public class LockStateIntentService extends IntentService {

    public LockStateIntentService() {
        super("LockStateIntentService");
    }

    public LockStateIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger logger = new Logger(this, PebbleLockerApplication.getUniqueTag());
        logger.log("Got intent to switch to the next lock state");
        LockState.switchToNextState(this, logger, false);
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }
}

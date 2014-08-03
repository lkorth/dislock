package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.BaseBroadcastReceiver;
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
        logger.log("Initializing LockerService and running locking");

        Locker locker = new Locker(this, tag);
        locker.handleLocking(intent.getBooleanExtra(WITH_DELAY, true),
                intent.getBooleanExtra(FORCE_LOCK, true));

        PebbleLockerApplication.getBus().post(new StatusChangedEvent());

        boolean wakeLockRemoved = BaseBroadcastReceiver.completeWakefulIntent(this, intent);
        logger.log("Done locking, releasing BroadcastReceiver wake lock: " + wakeLockRemoved);
    }

}

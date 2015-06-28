package com.lukekorth.pebblelocker.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.models.LockState;
import com.lukekorth.pebblelocker.services.LockerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseBroadcastReceiver extends BroadcastReceiver {

    protected static final int DELAYED_LOCK_REQUEST_CODE = 3439393;

    protected Context mContext;
    protected String mTag;
    protected Logger mLogger;
    protected String mAction;
    protected Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mTag = PebbleLockerApplication.getUniqueTag();
        mLogger = LoggerFactory.getLogger(mTag);
        mAction = intent.getAction().toLowerCase();
        mIntent = intent;

        mLogger.debug("BroadcastReceiver action: " + mAction);

        onReceive();

        LockState lockState = LockState.getCurrentState(mContext);
        if (lockState == LockState.AUTO) {
            handle();
        } else {
            mLogger.debug("Lock state was manually set to " +
                    mContext.getString(lockState.getDisplayName(mContext)));
        }

        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

    protected void handleLocking() {
        BaseBroadcastReceiver.handleLocking(mContext);
    }

    protected static void handleLocking(Context context) {
        if (Settings.isLocked(context) && Settings.isReauthenticationRequired(context) &&
                !Settings.isUnlockNeeded(context)) {
            Settings.setNeedToUnlock(context, true);
            LoggerFactory.getLogger("BaseBroadcastReceiver")
                    .debug("Reauthentication required, waiting until next unlock before unlocking");
        } else {
            context.startService(new Intent(context, LockerService.class));
        }
    }

    protected void lockWithDelay() {
        BaseBroadcastReceiver.lockWithDelay(mContext, mTag);
    }

    protected static void lockWithDelay(Context context, String tag) {
        Logger logger = LoggerFactory.getLogger(tag);

        int delay = Integer.parseInt(Settings.getGracePeriod(context));
        if (delay == 0) {
            logger.debug("No delay, handling locking now");
            BaseBroadcastReceiver.handleLocking(context);
        } else {
            logger.debug("Delay of " + delay + " seconds, setting alarm");
            BaseBroadcastReceiver.setDelayedLockAlarm(context, (delay * 1000));
        }
    }

    protected static void setDelayedLockAlarm(Context context, int delay) {
        long wakeupTime = System.currentTimeMillis() + delay;
        PendingIntent wakeupIntent = PendingIntent.getBroadcast(context,
                DELAYED_LOCK_REQUEST_CODE,
                new Intent(DelayedLockReceiver.DELAYED_LOCK),
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeupTime, wakeupIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, wakeupTime, wakeupIntent);
        }
    }

    protected void onReceive() {}
    protected abstract void handle();

}

package com.lukekorth.pebblelocker.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;

public abstract class BaseBroadcastReceiver extends BroadcastReceiver {

    public static final String LOCKED = "locked";
    protected static final int DELAYED_LOCK_REQUEST_CODE = 3439393;

    protected Context mContext;
    protected String mTag;
    protected Logger mLogger;
    protected DeviceHelper mDeviceHelper;
    protected String mAction;
    protected Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mTag = PebbleLockerApplication.getUniqueTag();
        mLogger = new Logger(context, mTag);
        mDeviceHelper = new DeviceHelper(context, mLogger);
        mAction = intent.getAction().toLowerCase();
        mIntent = intent;

        mLogger.log("BroadcastReceiver action: " + mAction);

        onReceive();

        LockState lockState = LockState.getCurrentState(mContext);
        if (lockState == LockState.AUTO) {
            handle();
        } else {
            mLogger.log("Lock state was manually set to " + lockState.getDisplayName());
        }
        mDeviceHelper.sendLockStatusChangedEvent();
    }

    protected void handleLocking() {
        BaseBroadcastReceiver.handleLocking(mContext, mTag);
    }

    protected static void handleLocking(Context context, String tag) {
        new Locker(context, tag).handleLocking(true);
    }

    protected void lockWithDelay() {
        BaseBroadcastReceiver.lockWithDelay(mContext, mLogger);
    }

    protected static void lockWithDelay(Context context, Logger logger) {
        int delay = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("key_grace_period", "2"));

        if (delay == 0) {
            logger.log("No delay, handling locking now");
            BaseBroadcastReceiver.handleLocking(context, logger.getTag());
        } else {
            logger.log("Delay of " + delay + "seconds, setting alarm");

            long wakeupTime = System.currentTimeMillis() +  (delay * 1000);
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
    }

    protected void onReceive() {}
    protected abstract void handle();

}

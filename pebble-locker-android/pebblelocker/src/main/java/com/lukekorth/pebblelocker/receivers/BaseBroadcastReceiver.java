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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        mLogger = LoggerFactory.getLogger(mTag);
        mDeviceHelper = new DeviceHelper(context, mTag);
        mAction = intent.getAction().toLowerCase();
        mIntent = intent;

        mLogger.debug("BroadcastReceiver action: " + mAction);

        onReceive();

        LockState lockState = LockState.getCurrentState(mContext);
        if (lockState == LockState.AUTO) {
            handle();
        } else {
            mLogger.debug("Lock state was manually set to " + lockState.getDisplayName());
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
        BaseBroadcastReceiver.lockWithDelay(mContext, mTag);
    }

    protected static void lockWithDelay(Context context, String tag) {
        Logger logger = LoggerFactory.getLogger(tag);
        int delay = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("key_grace_period", "2"));

        if (delay == 0) {
            logger.debug("No delay, handling locking now");
            BaseBroadcastReceiver.handleLocking(context, tag);
        } else {
            logger.debug("Delay of " + delay + "seconds, setting alarm");

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

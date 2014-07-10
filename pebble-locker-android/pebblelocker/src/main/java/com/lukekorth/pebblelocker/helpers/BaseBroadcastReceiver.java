package com.lukekorth.pebblelocker.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.lukekorth.pebblelocker.logging.Logger;

import java.util.UUID;

public class BaseBroadcastReceiver extends BroadcastReceiver {

    protected Context mContext;
    protected String mTag;
    protected Logger mLogger;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mTag = "[" + UUID.randomUUID().toString().split("-")[1] + "]";
        mLogger = new Logger(context, mTag);
    }

    protected void acquireWakeLock() {
        mWakeLock = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PebbleLockerReceiver");

        mLogger.log("Acquiring wakelock");

        mWakeLock.acquire();
    }

    protected void releaseWakeLock() {
        mLogger.log("Releasing wakelock");
        mWakeLock.release();
    }
}

package com.lukekorth.pebblelocker.helpers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.lukekorth.pebblelocker.logging.Logger;

import java.util.UUID;

public class BaseBroadcastReceiver extends WakefulBroadcastReceiver {

    protected Context mContext;
    protected String mTag;
    protected Logger mLogger;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mTag = "[" + UUID.randomUUID().toString().split("-")[1] + "]";
        mLogger = new Logger(context, mTag);
    }

}

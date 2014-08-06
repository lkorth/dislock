package com.lukekorth.pebblelocker.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.logging.Logger;

import java.util.UUID;

public class BaseBroadcastReceiver extends BroadcastReceiver {

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

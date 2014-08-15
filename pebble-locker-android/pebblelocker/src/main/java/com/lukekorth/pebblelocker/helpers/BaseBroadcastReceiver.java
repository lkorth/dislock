package com.lukekorth.pebblelocker.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.logging.Logger;

public class BaseBroadcastReceiver extends BroadcastReceiver {

    protected Context mContext;
    protected String mTag;
    protected Logger mLogger;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mTag = PebbleLockerApplication.getUniqueTag();
        mLogger = new Logger(context, mTag);
    }

}

package com.lukekorth.pebblelocker.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;

public class LockStatePreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final String TAG = "[LOCK-STATE-PREFERENCE]";

    private Context mContext;
    private Logger mLogger;
    private BroadcastReceiver mStatusReceiver;

    public LockStatePreference(Context context) {
        super(context);
        init(context);
    }

    public LockStatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LockStatePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mLogger = new Logger(mContext, TAG);

        setOnPreferenceClickListener(this);

        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
    }

    private void update() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        LockState lockState = LockState.getCurrentState(mContext);
        String title = "";

        if (lockState == LockState.AUTO) {
            if (prefs.getBoolean(ConnectionReceiver.LOCKED, false)) {
                title = "Locked (Automatic)";
            } else {
                title = "Unlocked (Automatic)";
            }
        } else {
            title = lockState.getDisplayName();
        }

        setTitle(title);
    }

    public void registerListener() {
        mContext.registerReceiver(mStatusReceiver,
                new IntentFilter(ConnectionReceiver.STATUS_CHANGED_INTENT));
        update();
    }

    public void unregisterListener() {
        mContext.unregisterReceiver(mStatusReceiver);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        LockState.switchToNextState(mContext, new Logger(mContext, TAG), false);
        return true;
    }

}

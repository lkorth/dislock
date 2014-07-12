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
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;

public class Status extends Preference implements Preference.OnPreferenceClickListener {

    private Context mContext;
    private Logger mLogger;
    private BroadcastReceiver mStatusReceiver;

    public Status(Context context) {
        super(context);
        init(context);
    }

    public Status(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Status(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mLogger = new Logger(mContext, "[STATUS-PREFERENCE]");
        setOnPreferenceClickListener(this);

        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        registerListener();
    }

    private void update() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        LockState lockState = LockState.getCurrentState(mContext);
        String statusMessage = "";

        if (lockState == LockState.AUTO) {
            if(prefs.getBoolean(ConnectionReceiver.LOCKED, false)) {
                statusMessage = "Locked (Automatic)";
            } else {
                statusMessage = "Unlocked (Automatic)";
            }
        } else {
            statusMessage = lockState.getDisplayName();
        }

        StringBuilder connectionStatusBuilder = new StringBuilder();

        PebbleHelper pebbleHelper = new PebbleHelper(mContext, mLogger);
        if (pebbleHelper.isPebbleAppInstalled() && pebbleHelper.isEnabled()) {
            connectionStatusBuilder.append(pebbleHelper.getConnectionStatus());
        }

        if(prefs.getBoolean("donated", false)) {
            connectionStatusBuilder.append(new BluetoothHelper(mContext, mLogger).getConnectionStatus());
            connectionStatusBuilder.append(new WifiHelper(mContext, mLogger).getConnectionStatus());
        }

        connectionStatusBuilder.append("\nClick to change");

        setTitle(statusMessage);
        setSummary(connectionStatusBuilder.toString());
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
        LockState.switchToNextState(mContext, new Logger(mContext, "[STATUS_PREFERENCE]"), false);
        update();
        return true;
    }

}

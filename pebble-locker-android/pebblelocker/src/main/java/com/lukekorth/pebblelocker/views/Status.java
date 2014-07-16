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
import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;

import java.util.Map;

public class Status extends Preference implements Preference.OnPreferenceClickListener, AndroidWearHelper.Listener {

    private static final String TAG = "[STATUS-PREFERENCE]";

    private Context mContext;
    private Logger mLogger;
    private BroadcastReceiver mStatusReceiver;
    private StringBuilder mSummary;
    private AndroidWearHelper mAndroidWearHelper;

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

        mSummary = new StringBuilder();
        mSummary.append("Click to change\n");
        updateSummary();
    }

    private void updateSummary() {
        PebbleHelper pebbleHelper = new PebbleHelper(mContext, mLogger);
        if (pebbleHelper.isPebbleAppInstalled() && pebbleHelper.isEnabled()) {
            mSummary.append("\n" + pebbleHelper.getConnectionStatus());
        }

        if (PremiumFeaturesActivity.hasPurchased(mContext)) {
            mSummary.append("\n" + new WifiHelper(mContext, mLogger).getConnectionStatus());
            mSummary.append("\n" + new BluetoothHelper(mContext, mLogger).getConnectionStatus());
            getConnectedAndroidWears();
        }

        setSummary(mSummary.toString());
    }

    private void getConnectedAndroidWears() {
        if (mAndroidWearHelper == null) {
            mAndroidWearHelper = new AndroidWearHelper(mContext, mLogger);
        }

        mAndroidWearHelper.getConnectedDevices(this);
    }

    @Override
    public void onKnownDevicesLoaded(Map<String, String> devices) {
        if (devices.size() > 0) {
            mSummary.append("\nAndroid Wear connected");
        } else {
            mSummary.append("\nNo Android Wear connected");
        }

        setSummary(mSummary.toString());
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

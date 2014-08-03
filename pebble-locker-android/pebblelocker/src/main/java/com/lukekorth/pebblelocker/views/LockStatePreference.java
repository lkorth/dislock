package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;
import com.squareup.otto.Subscribe;

public class LockStatePreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final String TAG = "[LOCK-STATE-PREFERENCE]";

    private Context mContext;

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
        PebbleLockerApplication.getBus().register(this);
        update(new StatusChangedEvent());
        setOnPreferenceClickListener(this);
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Subscribe
    public void update(StatusChangedEvent event) {
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        LockState.switchToNextState(mContext, new Logger(mContext, TAG), false);
        return true;
    }

}

package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.receivers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.services.LockStateIntentService;
import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockStatePreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final String TAG = "Lock_State_Preference";

    private Logger mLogger;

    public LockStatePreference(Context context) {
        super(context);
        init();
    }

    public LockStatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LockStatePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mLogger = LoggerFactory.getLogger(TAG);
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        LockState lockState = LockState.getCurrentState(getContext());
        String title = "";

        if (lockState == LockState.AUTO) {
            if (prefs.getBoolean(BaseBroadcastReceiver.LOCKED, false)) {
                title = "Locked (Automatic)";
            } else {
                title = "Unlocked (Automatic)";
            }
        } else {
            title = lockState.getDisplayName();
        }

        setTitle(title);
        setSummary(R.string.click_to_change);

        mLogger.debug("Updating status to " + title);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        getContext().startService(new Intent(getContext(), LockStateIntentService.class));

        setTitle(R.string.please_wait);
        setSummary("");

        return true;
    }

}

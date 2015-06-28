package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.models.LockState;
import com.squareup.otto.Subscribe;

public class LockStatePreference extends Preference implements Preference.OnPreferenceClickListener {

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
        PebbleLockerApplication.getBus().register(this);
        update();
        setOnPreferenceClickListener(this);
    }

    @Override
    public void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Subscribe
    public void onResume(ActivityResumedEvent event) {
        update();
    }

    @Subscribe
    public void onStatusChanged(StatusChangedEvent event) {
        update();
    }

    private void update() {
        LockState lockState = LockState.getCurrentState(getContext());
        setTitle(lockState.getDisplayName(getContext()));
        setSummary(R.string.click_to_change);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        LockState.switchToNextState(getContext());
        return true;
    }
}

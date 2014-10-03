package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.AuthenticationActivity;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.ScreenLockType;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.events.AuthenticationRequestEvent;
import com.squareup.otto.Subscribe;

public class ScreenLockTypePreference extends ListPreference {

    private ScreenLockType mCurrentType;

    public ScreenLockTypePreference(Context context) {
        super(context);
        init();
    }

    public ScreenLockTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        PebbleLockerApplication.getBus().register(this);
        update(new ActivityResumedEvent());
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Subscribe
    public void update(ActivityResumedEvent event) {
        mCurrentType = ScreenLockType.getCurrent(getContext());
        setSummary(mCurrentType.getDescription());
        notifyChanged();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            ScreenLockType newType = ScreenLockType.getCurrent(getContext());
            if (!newType.equals(mCurrentType)) {
                ScreenLockType.setCurrent(getContext(), mCurrentType);
            }

            Intent intent = new Intent(getContext(), AuthenticationActivity.class);
            if (newType == ScreenLockType.SLIDE) {
                intent.putExtra(AuthenticationActivity.AUTHENTICATION_TYPE_KEY,
                        AuthenticationActivity.CHANGE_TO_SLIDE);
            } else if (newType == ScreenLockType.PIN) {
                intent.putExtra(AuthenticationActivity.AUTHENTICATION_TYPE_KEY,
                        AuthenticationActivity.CHANGE_TO_PIN);
            } else if (newType == ScreenLockType.PASSWORD) {
                intent.putExtra(AuthenticationActivity.AUTHENTICATION_TYPE_KEY,
                        AuthenticationActivity.CHANGE_TO_PASSWORD);
            }

            PebbleLockerApplication.getBus().post(new AuthenticationRequestEvent(intent));
        }
    }

}

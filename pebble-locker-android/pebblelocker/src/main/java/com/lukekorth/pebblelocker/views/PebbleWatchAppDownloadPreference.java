package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.squareup.otto.Subscribe;

public class PebbleWatchAppDownloadPreference extends Preference implements Preference.OnPreferenceClickListener {

    public PebbleWatchAppDownloadPreference(Context context) {
        super(context);
        init();
    }

    public PebbleWatchAppDownloadPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PebbleWatchAppDownloadPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        PebbleLockerApplication.getBus().register(this);
        refresh(null);
        setOnPreferenceClickListener(this);
    }

    @Subscribe
    public void refresh(ActivityResumedEvent event) {
        setEnabled(PebbleHelper.isPebbleAppInstalled(getContext()));
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("pebble://appstore/5386a0646189a1be8200007a")));
        return true;
    }
}

package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.squareup.otto.Subscribe;

public class PebbleWatchAppDownloadPreference extends PremiumFeaturesPreference {

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
        refresh(new ActivityResumedEvent());
    }

    @Subscribe
    public void refresh(ActivityResumedEvent event) {
        setEnabled(new PebbleHelper(getContext(), new Logger(getContext(), "[PEBBLE-WATCH-DOWNLOAD]"))
                .isPebbleAppInstalled());
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Override
    public Intent getActionIntent() {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("pebble://appstore/5386a0646189a1be8200007a"));
    }

}

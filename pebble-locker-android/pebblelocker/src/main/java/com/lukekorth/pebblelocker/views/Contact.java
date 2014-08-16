package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.logging.LogReporting;

public class Contact extends Preference implements Preference.OnPreferenceClickListener {

    public Contact(Context context) {
        super(context);
        init();
    }

    public Contact(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Contact(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new LogReporting(getContext()).collectAndSendLogs();
        return true;
    }

}

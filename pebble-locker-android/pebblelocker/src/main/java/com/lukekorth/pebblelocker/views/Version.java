package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.BuildConfig;

public class Version extends Preference {

    public Version(Context context) {
        super(context);
        setSummary(BuildConfig.VERSION_NAME);
    }

    public Version(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSummary(BuildConfig.VERSION_NAME);
    }

    public Version(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSummary(BuildConfig.VERSION_NAME);
    }

}

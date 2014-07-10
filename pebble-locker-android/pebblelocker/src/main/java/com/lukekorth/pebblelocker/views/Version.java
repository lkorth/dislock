package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.R;

public class Version extends Preference {

    public Version(Context context) {
        super(context);
        init();
    }

    public Version(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Version(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setTitle(R.string.version);
        setSummary(BuildConfig.VERSION_NAME);
    }

}

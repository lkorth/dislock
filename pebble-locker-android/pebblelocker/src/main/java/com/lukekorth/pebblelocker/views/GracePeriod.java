package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class GracePeriod extends ListPreference implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "key_grace_period";
    private static final String DEFAULT = "2";

    public GracePeriod(Context context) {
        super(context);
        init(context);
    }

    public GracePeriod(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setCustomSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, DEFAULT));
        setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setCustomSummary(newValue.toString());
        return true;
    }

    private void setCustomSummary(String length) {
        String time;
        if (length.equals("0")) {
            time = "instantly";
        } else {
            time = length + " seconds";
        }

        setSummary("Lock " + time + " after disconnection");
    }

}

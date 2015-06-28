package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.helpers.Settings;

public class GracePeriod extends ListPreference implements Preference.OnPreferenceChangeListener {

    public GracePeriod(Context context) {
        super(context);
        init();
    }

    public GracePeriod(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setCustomSummary(Settings.getGracePeriod(getContext()));
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
